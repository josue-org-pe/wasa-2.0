package com.chatlocal.backend.network;

import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.service.FileTransferManager;

import java.io.*;
import java.net.Socket;

/**
 * Gestiona una conexión de socket abierta con el par remoto.
 * Proporciona métodos thread-safe para enviar texto, archivos, notas de voz y stickers,
 * y ejecuta un hilo de escucha continuo para deserializar paquetes entrantes.
 */
public class SocketConnection {

    public interface ConnectionCallback {
        void onTextMessageReceived(ChatMessage message);
        void onFileReceived(ChatMessage message);
        void onAudioReceived(ChatMessage message);
        void onPongReceived();
        void onConnectionLost(String reason);
    }

    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    private final ConnectionCallback callback;
    private volatile boolean active = true;

    public SocketConnection(Socket socket, ConnectionCallback callback) throws IOException {
        this.socket = socket;
        this.callback = callback;
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        Thread listenThread = new Thread(this::listenLoop, "chat-socket-listener");
        listenThread.setDaemon(true);
        listenThread.start();
    }

    private void listenLoop() {
        try {
            while (active) {
                byte type = in.readByte();
                switch (type) {
                    case ProtocolConstants.TYPE_TEXT -> {
                        String sender = in.readUTF();
                        int color = in.readInt();
                        String text = in.readUTF();
                        ChatMessage message = ChatMessage.createTextMessage(sender, color, text, false, "general");
                        callback.onTextMessageReceived(message);
                    }
                    case ProtocolConstants.TYPE_FILE -> handleIncomingFile();
                    case ProtocolConstants.TYPE_AUDIO -> handleIncomingAudio();
                    case ProtocolConstants.TYPE_STICKER -> {
                        String sender = in.readUTF();
                        int color = in.readInt();
                        String sticker = in.readUTF();
                        ChatMessage message = ChatMessage.createStickerMessage(sender, color, sticker, false, "general");
                        callback.onTextMessageReceived(message);
                    }
                    case ProtocolConstants.TYPE_PING -> sendPong();
                    case ProtocolConstants.TYPE_PONG -> callback.onPongReceived();
                    default -> {
                        // Tipo de paquete desconocido, ignorar
                    }
                }
            }
        } catch (IOException e) {
            if (active) {
                active = false;
                callback.onConnectionLost(e.getMessage() != null ? e.getMessage() : "Conexión cerrada por el extremo remoto");
            }
        }
    }

    private void handleIncomingFile() throws IOException {
        String sender = in.readUTF();
        int color = in.readInt();
        String fileName = in.readUTF();
        long fileSize = in.readLong();

        File folder = FileTransferManager.getReceivedFilesFolder();
        File targetFile = resolveCollision(folder, fileName);

        readFileStream(targetFile, fileSize);

        ChatMessage message = ChatMessage.createFileMessage(sender, color, fileName, fileSize, targetFile.getAbsolutePath(), false, "general");
        callback.onFileReceived(message);
    }

    private void handleIncomingAudio() throws IOException {
        String sender = in.readUTF();
        int color = in.readInt();
        String fileName = in.readUTF();
        long fileSize = in.readLong();
        int durationSecs = in.readInt();

        File folder = FileTransferManager.getReceivedFilesFolder();
        File targetFile = resolveCollision(folder, fileName);

        readFileStream(targetFile, fileSize);

        ChatMessage message = ChatMessage.createAudioMessage(sender, color, targetFile.getAbsolutePath(), durationSecs, false, "general");
        callback.onAudioReceived(message);
    }

    private void readFileStream(File destination, long fileSize) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] buffer = new byte[ProtocolConstants.BUFFER_SIZE];
            long bytesRemaining = fileSize;
            while (bytesRemaining > 0) {
                int toRead = (int) Math.min(buffer.length, bytesRemaining);
                int bytesRead = in.read(buffer, 0, toRead);
                if (bytesRead == -1) {
                    throw new EOFException("Fin inesperado del stream al recibir el archivo");
                }
                bos.write(buffer, 0, bytesRead);
                bytesRemaining -= bytesRead;
            }
            bos.flush();
        }
    }

    private File resolveCollision(File folder, String fileName) {
        File targetFile = new File(folder, fileName);
        if (targetFile.exists()) {
            String base = fileName;
            String extension = "";
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex != -1) {
                base = fileName.substring(0, dotIndex);
                extension = fileName.substring(dotIndex);
            }
            targetFile = new File(folder, base + "_" + System.currentTimeMillis() + extension);
        }
        return targetFile;
    }

    public synchronized void sendTextMessage(String sender, int color, String text) throws IOException {
        out.writeByte(ProtocolConstants.TYPE_TEXT);
        out.writeUTF(sender);
        out.writeInt(color);
        out.writeUTF(text);
        out.flush();
    }

    public synchronized void sendText(String text) throws IOException {
        sendTextMessage("Usuario", 0x6366F1, text);
    }

    public synchronized void sendAudio(String sender, int color, File audioFile, int durationSecs) throws IOException {
        if (!audioFile.exists() || !audioFile.isFile()) {
            throw new FileNotFoundException("El archivo de audio no existe.");
        }

        out.writeByte(ProtocolConstants.TYPE_AUDIO);
        out.writeUTF(sender);
        out.writeInt(color);
        out.writeUTF(audioFile.getName());
        out.writeLong(audioFile.length());
        out.writeInt(durationSecs);

        writeFileStream(audioFile);
    }

    public synchronized void sendSticker(String sender, int color, String stickerText) throws IOException {
        out.writeByte(ProtocolConstants.TYPE_STICKER);
        out.writeUTF(sender);
        out.writeInt(color);
        out.writeUTF(stickerText);
        out.flush();
    }

    public synchronized void sendFile(String sender, int color, File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("El archivo especificado no existe o es un directorio.");
        }

        out.writeByte(ProtocolConstants.TYPE_FILE);
        out.writeUTF(sender);
        out.writeInt(color);
        out.writeUTF(file.getName());
        out.writeLong(file.length());

        writeFileStream(file);
    }

    public synchronized void sendFile(File file) throws IOException {
        sendFile("Usuario", 0x6366F1, file);
    }

    private void writeFileStream(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[ProtocolConstants.BUFFER_SIZE];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        out.flush();
    }

    public synchronized void sendPing() throws IOException {
        out.writeByte(ProtocolConstants.TYPE_PING);
        out.flush();
    }

    private synchronized void sendPong() throws IOException {
        out.writeByte(ProtocolConstants.TYPE_PONG);
        out.flush();
    }

    public synchronized void close() {
        active = false;
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {}
        try {
            if (in != null) in.close();
        } catch (IOException ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return active && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public Socket getSocket() {
        return socket;
    }
}

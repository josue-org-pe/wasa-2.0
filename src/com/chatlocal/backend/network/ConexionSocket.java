package com.chatlocal.backend.network;

import com.chatlocal.backend.model.MensajeChat;
import com.chatlocal.backend.service.GestorTransferenciaArchivos;

import java.io.*;
import java.net.Socket;

// conexion por socket para mandar texto, archivos, audios y llamadas
public class ConexionSocket {

    public interface ConnectionCallback {
        void onTextMessageReceived(MensajeChat message);
        void onFileReceived(MensajeChat message);
        void onAudioReceived(MensajeChat message);
        void onPongReceived();
        void onConnectionLost(String reason);
        default void onAckReceived(String messageId) {}
        default void onHandshakeReceived(String username, int colorHex, String avatarPath) {}
        default void onCallRequestReceived(String callerName, String peerIp, int mediaPort) {}
        default void onCallAcceptReceived(String acceptorName, String peerIp, int mediaPort) {}
        default void onCallRejectReceived(String reason) {}
        default void onCallEndReceived(String reason) {}
    }

    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    private final ConnectionCallback callback;
    private volatile boolean active = true;

    public ConexionSocket(Socket socket, ConnectionCallback callback) throws IOException {
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
                    case ConstantesProtocolo.TYPE_TEXT -> {
                        String id = in.readUTF();
                        String sender = in.readUTF();
                        int color = in.readInt();
                        String roomId = in.readUTF();
                        String recipient = in.readUTF();
                        String avatarPath = in.readUTF();
                        String text = in.readUTF();
                        MensajeChat message = new MensajeChat(id, sender, color, text, com.chatlocal.backend.model.TipoMensaje.TEXT, false, null, 0, null, 0, roomId, avatarPath, recipient);
                        sendAck(id);
                        callback.onTextMessageReceived(message);
                    }
                    case ConstantesProtocolo.TYPE_FILE -> handleIncomingFile();
                    case ConstantesProtocolo.TYPE_AUDIO -> handleIncomingAudio();
                    case ConstantesProtocolo.TYPE_STICKER -> {
                        String id = in.readUTF();
                        String sender = in.readUTF();
                        int color = in.readInt();
                        String roomId = in.readUTF();
                        String recipient = in.readUTF();
                        String avatarPath = in.readUTF();
                        String sticker = in.readUTF();
                        MensajeChat message = new MensajeChat(id, sender, color, sticker, com.chatlocal.backend.model.TipoMensaje.STICKER, false, null, 0, null, 0, roomId, avatarPath, recipient);
                        sendAck(id);
                        callback.onTextMessageReceived(message);
                    }
                    case ConstantesProtocolo.TYPE_ACK -> {
                        String messageId = in.readUTF();
                        callback.onAckReceived(messageId);
                    }
                    case ConstantesProtocolo.TYPE_HANDSHAKE -> {
                        String username = in.readUTF();
                        int color = in.readInt();
                        String avatarPath = in.readUTF();
                        callback.onHandshakeReceived(username, color, avatarPath);
                    }
                    case ConstantesProtocolo.TYPE_PING -> sendPong();
                    case ConstantesProtocolo.TYPE_PONG -> callback.onPongReceived();
                    case ConstantesProtocolo.TYPE_CALL_REQUEST -> {
                        String caller = in.readUTF();
                        int mediaPort = in.readInt();
                        String peerIp = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "127.0.0.1";
                        callback.onCallRequestReceived(caller, peerIp, mediaPort);
                    }
                    case ConstantesProtocolo.TYPE_CALL_ACCEPT -> {
                        String acceptor = in.readUTF();
                        int mediaPort = in.readInt();
                        String peerIp = socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "127.0.0.1";
                        callback.onCallAcceptReceived(acceptor, peerIp, mediaPort);
                    }
                    case ConstantesProtocolo.TYPE_CALL_REJECT -> {
                        String reason = in.readUTF();
                        callback.onCallRejectReceived(reason);
                    }
                    case ConstantesProtocolo.TYPE_CALL_END -> {
                        String reason = in.readUTF();
                        callback.onCallEndReceived(reason);
                    }
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
        String id = in.readUTF();
        String sender = in.readUTF();
        int color = in.readInt();
        String roomId = in.readUTF();
        String recipient = in.readUTF();
        String avatarPath = in.readUTF();
        String fileName = in.readUTF();
        long fileSize = in.readLong();

        File folder = GestorTransferenciaArchivos.getReceivedFilesFolder();
        File targetFile = resolveCollision(folder, fileName);

        readFileStream(targetFile, fileSize);

        MensajeChat message = new MensajeChat(id, sender, color, "Has recibido un archivo", com.chatlocal.backend.model.TipoMensaje.FILE, false, fileName, fileSize, targetFile.getAbsolutePath(), 0, roomId, avatarPath, recipient);
        sendAck(id);
        callback.onFileReceived(message);
    }

    private void handleIncomingAudio() throws IOException {
        String id = in.readUTF();
        String sender = in.readUTF();
        int color = in.readInt();
        String roomId = in.readUTF();
        String recipient = in.readUTF();
        String avatarPath = in.readUTF();
        String fileName = in.readUTF();
        long fileSize = in.readLong();
        int durationSecs = in.readInt();

        File folder = GestorTransferenciaArchivos.getReceivedFilesFolder();
        File targetFile = resolveCollision(folder, fileName);

        readFileStream(targetFile, fileSize);

        MensajeChat message = new MensajeChat(id, sender, color, "Nota de voz (" + durationSecs + "s)", com.chatlocal.backend.model.TipoMensaje.AUDIO, false, fileName, fileSize, targetFile.getAbsolutePath(), durationSecs, roomId, avatarPath, recipient);
        sendAck(id);
        callback.onAudioReceived(message);
    }

    private void readFileStream(File destination, long fileSize) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] buffer = new byte[ConstantesProtocolo.BUFFER_SIZE];
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

    public synchronized void sendTextMessage(String id, String sender, int color, String text, String roomId, String recipient, String avatarPath) throws IOException {
        out.writeByte(ConstantesProtocolo.TYPE_TEXT);
        out.writeUTF(id != null ? id : "");
        out.writeUTF(sender != null ? sender : "Usuario");
        out.writeInt(color);
        out.writeUTF(roomId != null ? roomId : "general");
        out.writeUTF(recipient != null ? recipient : "");
        out.writeUTF(avatarPath != null ? avatarPath : "");
        out.writeUTF(text != null ? text : "");
        out.flush();
    }

    public synchronized void sendTextMessage(String sender, int color, String text) throws IOException {
        sendTextMessage(java.util.UUID.randomUUID().toString(), sender, color, text, "general", "", "");
    }

    public synchronized void sendText(String text) throws IOException {
        sendTextMessage("Usuario", 0x6366F1, text);
    }

    public synchronized void sendAudio(String id, String sender, int color, File audioFile, int durationSecs, String roomId, String recipient, String avatarPath) throws IOException {
        if (!audioFile.exists() || !audioFile.isFile()) {
            throw new FileNotFoundException("El archivo de audio no existe.");
        }

        out.writeByte(ConstantesProtocolo.TYPE_AUDIO);
        out.writeUTF(id != null ? id : "");
        out.writeUTF(sender != null ? sender : "Usuario");
        out.writeInt(color);
        out.writeUTF(roomId != null ? roomId : "general");
        out.writeUTF(recipient != null ? recipient : "");
        out.writeUTF(avatarPath != null ? avatarPath : "");
        out.writeUTF(audioFile.getName());
        out.writeLong(audioFile.length());
        out.writeInt(durationSecs);

        writeFileStream(audioFile);
    }

    public synchronized void sendAudio(String sender, int color, File audioFile, int durationSecs) throws IOException {
        sendAudio(java.util.UUID.randomUUID().toString(), sender, color, audioFile, durationSecs, "general", "", "");
    }

    public synchronized void sendSticker(String id, String sender, int color, String stickerText, String roomId, String recipient, String avatarPath) throws IOException {
        out.writeByte(ConstantesProtocolo.TYPE_STICKER);
        out.writeUTF(id != null ? id : "");
        out.writeUTF(sender != null ? sender : "Usuario");
        out.writeInt(color);
        out.writeUTF(roomId != null ? roomId : "general");
        out.writeUTF(recipient != null ? recipient : "");
        out.writeUTF(avatarPath != null ? avatarPath : "");
        out.writeUTF(stickerText != null ? stickerText : "");
        out.flush();
    }

    public synchronized void sendSticker(String sender, int color, String stickerText) throws IOException {
        sendSticker(java.util.UUID.randomUUID().toString(), sender, color, stickerText, "general", "", "");
    }

    public synchronized void sendFile(String id, String sender, int color, File file, String roomId, String recipient, String avatarPath) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("El archivo especificado no existe o es un directorio.");
        }

        out.writeByte(ConstantesProtocolo.TYPE_FILE);
        out.writeUTF(id != null ? id : "");
        out.writeUTF(sender != null ? sender : "Usuario");
        out.writeInt(color);
        out.writeUTF(roomId != null ? roomId : "general");
        out.writeUTF(recipient != null ? recipient : "");
        out.writeUTF(avatarPath != null ? avatarPath : "");
        out.writeUTF(file.getName());
        out.writeLong(file.length());

        writeFileStream(file);
    }

    public synchronized void sendFile(String sender, int color, File file) throws IOException {
        sendFile(java.util.UUID.randomUUID().toString(), sender, color, file, "general", "", "");
    }

    public synchronized void sendFile(File file) throws IOException {
        sendFile("Usuario", 0x6366F1, file);
    }

    public synchronized void sendAck(String messageId) {
        if (!active) return;
        try {
            out.writeByte(ConstantesProtocolo.TYPE_ACK);
            out.writeUTF(messageId != null ? messageId : "");
            out.flush();
        } catch (IOException ignored) {}
    }

    public synchronized void sendHandshake(String username, int colorHex, String avatarPath) throws IOException {
        out.writeByte(ConstantesProtocolo.TYPE_HANDSHAKE);
        out.writeUTF(username != null ? username : "Usuario");
        out.writeInt(colorHex);
        out.writeUTF(avatarPath != null ? avatarPath : "");
        out.flush();
    }

    private void writeFileStream(File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[ConstantesProtocolo.BUFFER_SIZE];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        out.flush();
    }

    public synchronized void sendPing() throws IOException {
        out.writeByte(ConstantesProtocolo.TYPE_PING);
        out.flush();
    }

    private synchronized void sendPong() throws IOException {
        out.writeByte(ConstantesProtocolo.TYPE_PONG);
        out.flush();
    }

    public synchronized void sendCallRequest(String callerName, int mediaPort) throws IOException {
        out.writeByte(ConstantesProtocolo.TYPE_CALL_REQUEST);
        out.writeUTF(callerName != null ? callerName : "Usuario");
        out.writeInt(mediaPort);
        out.flush();
    }

    public synchronized void sendCallAccept(String acceptorName, int mediaPort) throws IOException {
        out.writeByte(ConstantesProtocolo.TYPE_CALL_ACCEPT);
        out.writeUTF(acceptorName != null ? acceptorName : "Usuario");
        out.writeInt(mediaPort);
        out.flush();
    }

    public synchronized void sendCallReject(String reason) throws IOException {
        out.writeByte(ConstantesProtocolo.TYPE_CALL_REJECT);
        out.writeUTF(reason != null ? reason : "Llamada rechazada");
        out.flush();
    }

    public synchronized void sendCallEnd(String reason) throws IOException {
        out.writeByte(ConstantesProtocolo.TYPE_CALL_END);
        out.writeUTF(reason != null ? reason : "Llamada finalizada");
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

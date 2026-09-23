package com.chatlocal.backend.service;

import com.chatlocal.backend.event.ConnectionListener;
import com.chatlocal.backend.event.MessageListener;
import com.chatlocal.backend.model.*;
import com.chatlocal.backend.network.ProtocolConstants;
import com.chatlocal.backend.network.SocketConnection;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementación robusta del servicio de chat con soporte multicliente, salas,
 * notas de voz, stickers, bloqueo de usuarios y verificación Ping/Pong.
 */
public class ChatServiceImpl implements ChatService, SocketConnection.ConnectionCallback {

    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();

    private volatile ConnectionState currentState = ConnectionState.DISCONNECTED;
    private volatile ConnectionRole currentRole = null;
    private volatile String peerAddress = "";
    private volatile int activePort = ProtocolConstants.DEFAULT_PORT;

    private volatile UserProfile localUserProfile = new UserProfile("Usuario", ConnectionRole.CLIENT, "127.0.0.1", ProtocolConstants.DEFAULT_PORT);
    private final List<UserProfile> connectedUsers = new CopyOnWriteArrayList<>();
    private final Set<String> blockedUsers = ConcurrentHashMap.newKeySet();

    // Salas y reuniones
    private final List<ChatRoom> rooms = new CopyOnWriteArrayList<>();
    private volatile String activeRoomId = "general";

    // Grabador de voz
    private final AudioRecorderService audioRecorder = new AudioRecorderService();

    // Manejo de conexiones multicliente (Servidor / Host)
    private final List<SocketConnection> clientConnections = new CopyOnWriteArrayList<>();

    // Conexión individual (Cliente)
    private SocketConnection clientToServerConnection;

    private ServerSocket serverSocket;
    private Thread connectionWorker;
    private final AtomicBoolean pongReceived = new AtomicBoolean(false);

    public ChatServiceImpl() {
        // Inicializar sala general por defecto
        rooms.add(ChatRoom.createDefaultGeneralRoom());
    }

    @Override
    public void setLocalUserProfile(UserProfile profile) {
        if (profile != null) {
            this.localUserProfile = profile;
        }
    }

    @Override
    public UserProfile getLocalUserProfile() {
        return localUserProfile;
    }

    @Override
    public List<UserProfile> getConnectedUsers() {
        return connectedUsers;
    }

    @Override
    public int getConnectedUserCount() {
        return (currentRole == ConnectionRole.HOST) ? clientConnections.size() : (clientToServerConnection != null && clientToServerConnection.isConnected() ? 1 : 0);
    }

    // --- Gestión de Salas ---

    @Override
    public List<ChatRoom> getRooms() {
        return rooms;
    }

    @Override
    public void createRoom(String name, String topic, String accessCode, boolean isMeeting) {
        ChatRoom room = new ChatRoom(name, topic, accessCode, isMeeting, localUserProfile.getUsername());
        rooms.add(room);
        this.activeRoomId = room.getId();
    }

    @Override
    public ChatRoom getActiveRoom() {
        for (ChatRoom r : rooms) {
            if (r.getId().equals(activeRoomId)) return r;
        }
        return rooms.isEmpty() ? ChatRoom.createDefaultGeneralRoom() : rooms.get(0);
    }

    @Override
    public void setActiveRoom(String roomId) {
        if (roomId != null) {
            this.activeRoomId = roomId;
            ChatRoom room = getActiveRoom();
            if (room != null) room.resetUnread();
        }
    }

    // --- Bloqueo de Contactos ---

    @Override
    public void blockUser(String username) {
        if (username != null && !username.trim().isEmpty()) {
            blockedUsers.add(username.trim().toLowerCase());
        }
    }

    @Override
    public void unblockUser(String username) {
        if (username != null) {
            blockedUsers.remove(username.trim().toLowerCase());
        }
    }

    @Override
    public boolean isUserBlocked(String username) {
        if (username == null) return false;
        return blockedUsers.contains(username.trim().toLowerCase());
    }

    @Override
    public List<String> getBlockedUsers() {
        return new ArrayList<>(blockedUsers);
    }

    @Override
    public AudioRecorderService getAudioRecorder() {
        return audioRecorder;
    }

    // --- Red y Conexiones ---

    @Override
    public void startHost(int port) {
        disconnect();
        this.currentRole = ConnectionRole.HOST;
        this.activePort = port;
        this.pongReceived.set(false);
        this.connectedUsers.clear();
        this.clientConnections.clear();

        updateState(ConnectionState.LISTENING, "Servidor activo. Esperando conexiones en puerto " + port + "...");

        connectionWorker = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(port));

                while (currentState == ConnectionState.LISTENING || currentState == ConnectionState.CONNECTED) {
                    try {
                        Socket acceptedSocket = serverSocket.accept();
                        handleIncomingClient(acceptedSocket);
                    } catch (IOException e) {
                        if (serverSocket == null || serverSocket.isClosed()) break;
                    }
                }
            } catch (IOException e) {
                if (currentState != ConnectionState.DISCONNECTED) {
                    notifyError("No se pudo iniciar el servidor en el puerto " + port + ": " + e.getMessage());
                    updateState(ConnectionState.ERROR, "Fallo al iniciar servidor: " + e.getMessage());
                }
            }
        }, "chat-multihost-worker");
        connectionWorker.setDaemon(true);
        connectionWorker.start();
    }

    private void handleIncomingClient(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        int clientPort = socket.getPort();

        try {
            SocketConnection conn = new SocketConnection(socket, new SocketConnection.ConnectionCallback() {
                @Override
                public void onTextMessageReceived(ChatMessage message) {
                    if (isUserBlocked(message.getSender())) return;
                    notifyMessageReceived(message);
                    broadcastMessage(message, socket);
                }

                @Override
                public void onFileReceived(ChatMessage message) {
                    if (isUserBlocked(message.getSender())) return;
                    notifyFileReceived(message);
                }

                @Override
                public void onAudioReceived(ChatMessage message) {
                    if (isUserBlocked(message.getSender())) return;
                    notifyAudioReceived(message);
                }

                @Override
                public void onPongReceived() {
                    pongReceived.set(true);
                }

                @Override
                public void onConnectionLost(String reason) {
                    clientConnections.removeIf(c -> c.getSocket() == socket);
                    connectedUsers.removeIf(u -> u.getIpAddress().equals(clientIp) && u.getPort() == clientPort);
                    updateState(currentState, "Un usuario se ha desconectado (" + clientIp + ")");
                }
            });

            clientConnections.add(conn);
            peerAddress = clientIp;

            // Registrar usuario detectado
            UserProfile newUser = new UserProfile(clientIp + ":" + clientPort, ConnectionRole.CLIENT, clientIp, clientPort);
            connectedUsers.add(newUser);

            // Enviar Ping de confirmación
            conn.sendPing();

            if (currentState != ConnectionState.CONNECTED) {
                updateState(ConnectionState.CONNECTED, "Cliente conectado desde " + clientIp + ":" + clientPort);
            } else {
                updateState(ConnectionState.CONNECTED, "Nuevo usuario en sala: " + clientIp + " (Total: " + clientConnections.size() + ")");
            }

        } catch (IOException e) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void broadcastMessage(ChatMessage message, Socket excludeSocket) {
        for (SocketConnection conn : clientConnections) {
            if (conn.isConnected() && conn.getSocket() != excludeSocket) {
                try {
                    conn.sendTextMessage(message.getSender(), message.getSenderColorHex(), message.getContent());
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public void connectToHost(String hostIp, int port) {
        disconnect();
        this.currentRole = ConnectionRole.CLIENT;
        this.peerAddress = hostIp;
        this.activePort = port;
        this.pongReceived.set(false);

        updateState(ConnectionState.CONNECTING, "Conectando con el servidor " + hostIp + ":" + port + "...");

        connectionWorker = new Thread(() -> {
            try {
                Socket clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(hostIp, port), 5000);

                updateState(ConnectionState.VERIFYING, "Conectado. Verificando canal de comunicación...");

                clientToServerConnection = new SocketConnection(clientSocket, this);
                clientToServerConnection.sendPing();

                long deadline = System.currentTimeMillis() + ProtocolConstants.PING_TIMEOUT_MS;
                while (!pongReceived.get() && System.currentTimeMillis() < deadline && clientToServerConnection.isConnected()) {
                    Thread.sleep(100);
                }

                if (pongReceived.get()) {
                    updateState(ConnectionState.CONNECTED, "Conexión verificada con éxito.");
                } else if (clientToServerConnection.isConnected()) {
                    updateState(ConnectionState.CONNECTED, "Conectado a la sala.");
                } else {
                    updateState(ConnectionState.ERROR, "El servidor cerró la conexión durante la verificación.");
                }
            } catch (Exception e) {
                if (currentState != ConnectionState.DISCONNECTED) {
                    notifyError("No se pudo conectar a " + hostIp + ":" + port + ". " + e.getMessage());
                    updateState(ConnectionState.ERROR, "Fallo al conectar: " + e.getMessage());
                }
            }
        }, "chat-client-worker");
        connectionWorker.setDaemon(true);
        connectionWorker.start();
    }

    @Override
    public void sendTextMessage(String text) throws IOException {
        String sender = (localUserProfile != null) ? localUserProfile.getUsername() : "Yo";
        int color = (localUserProfile != null) ? localUserProfile.getAvatarColorHex() : 0x6366F1;
        ChatMessage msg = ChatMessage.createTextMessage(sender, color, text, true, activeRoomId);

        if (currentRole == ConnectionRole.HOST) {
            if (clientConnections.isEmpty()) {
                throw new IOException("No hay usuarios conectados en la sala actualmente.");
            }
            for (SocketConnection conn : clientConnections) {
                if (conn.isConnected()) {
                    conn.sendTextMessage(sender, color, text);
                }
            }
            notifyMessageSent(msg);
        } else {
            if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                throw new IOException("No hay conexión con el servidor.");
            }
            clientToServerConnection.sendTextMessage(sender, color, text);
            notifyMessageSent(msg);
        }
    }

    @Override
    public void sendAudioMessage(File audioFile, int durationSeconds) throws IOException {
        String sender = (localUserProfile != null) ? localUserProfile.getUsername() : "Yo";
        int color = (localUserProfile != null) ? localUserProfile.getAvatarColorHex() : 0x6366F1;
        ChatMessage msg = ChatMessage.createAudioMessage(sender, color, audioFile.getAbsolutePath(), durationSeconds, true, activeRoomId);

        if (currentRole == ConnectionRole.HOST) {
            for (SocketConnection conn : clientConnections) {
                if (conn.isConnected()) {
                    conn.sendAudio(sender, color, audioFile, durationSeconds);
                }
            }
            notifyMessageSent(msg);
        } else {
            if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                throw new IOException("No hay conexión con el servidor.");
            }
            clientToServerConnection.sendAudio(sender, color, audioFile, durationSeconds);
            notifyMessageSent(msg);
        }
    }

    @Override
    public void sendStickerMessage(String stickerText) throws IOException {
        String sender = (localUserProfile != null) ? localUserProfile.getUsername() : "Yo";
        int color = (localUserProfile != null) ? localUserProfile.getAvatarColorHex() : 0x6366F1;
        ChatMessage msg = ChatMessage.createStickerMessage(sender, color, stickerText, true, activeRoomId);

        if (currentRole == ConnectionRole.HOST) {
            for (SocketConnection conn : clientConnections) {
                if (conn.isConnected()) {
                    conn.sendSticker(sender, color, stickerText);
                }
            }
            notifyMessageSent(msg);
        } else {
            if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                throw new IOException("No hay conexión con el servidor.");
            }
            clientToServerConnection.sendSticker(sender, color, stickerText);
            notifyMessageSent(msg);
        }
    }

    @Override
    public void sendFile(File file) throws IOException {
        String sender = (localUserProfile != null) ? localUserProfile.getUsername() : "Yo";
        int color = (localUserProfile != null) ? localUserProfile.getAvatarColorHex() : 0x6366F1;
        ChatMessage msg = ChatMessage.createFileMessage(sender, color, file.getName(), file.length(), file.getAbsolutePath(), true, activeRoomId);

        if (currentRole == ConnectionRole.HOST) {
            for (SocketConnection conn : clientConnections) {
                if (conn.isConnected()) {
                    conn.sendFile(sender, color, file);
                }
            }
            notifyMessageSent(msg);
        } else {
            if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                throw new IOException("No hay conexión con el servidor.");
            }
            clientToServerConnection.sendFile(sender, color, file);
            notifyMessageSent(msg);
        }
    }

    @Override
    public synchronized void disconnect() {
        if (connectionWorker != null && connectionWorker.isAlive()) {
            connectionWorker.interrupt();
        }
        closeServerSocket();

        for (SocketConnection conn : clientConnections) {
            conn.close();
        }
        clientConnections.clear();
        connectedUsers.clear();

        if (clientToServerConnection != null) {
            clientToServerConnection.close();
            clientToServerConnection = null;
        }

        if (currentState != ConnectionState.DISCONNECTED) {
            updateState(ConnectionState.DISCONNECTED, "Desconectado.");
        }
    }

    private void closeServerSocket() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
            serverSocket = null;
        }
    }

    private void updateState(ConnectionState newState, String details) {
        this.currentState = newState;
        SwingUtilities.invokeLater(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onConnectionStateChanged(newState, details);
            }
        });
    }

    private void notifyError(String error) {
        SwingUtilities.invokeLater(() -> {
            for (ConnectionListener listener : connectionListeners) {
                listener.onConnectionError(error);
            }
        });
    }

    private void notifyMessageSent(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            for (MessageListener listener : messageListeners) {
                listener.onMessageSent(message);
            }
        });
    }

    private void notifyMessageReceived(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            for (MessageListener listener : messageListeners) {
                listener.onMessageReceived(message);
            }
        });
    }

    private void notifyFileReceived(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            for (MessageListener listener : messageListeners) {
                listener.onFileReceived(message);
            }
        });
    }

    private void notifyAudioReceived(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            for (MessageListener listener : messageListeners) {
                listener.onAudioReceived(message);
            }
        });
    }

    // --- Callbacks de SocketConnection.ConnectionCallback (para modo Cliente) ---

    @Override
    public void onTextMessageReceived(ChatMessage message) {
        if (isUserBlocked(message.getSender())) return;
        notifyMessageReceived(message);
    }

    @Override
    public void onFileReceived(ChatMessage message) {
        if (isUserBlocked(message.getSender())) return;
        notifyFileReceived(message);
    }

    @Override
    public void onAudioReceived(ChatMessage message) {
        if (isUserBlocked(message.getSender())) return;
        notifyAudioReceived(message);
    }

    @Override
    public void onPongReceived() {
        pongReceived.set(true);
    }

    @Override
    public void onConnectionLost(String reason) {
        updateState(ConnectionState.DISCONNECTED, "Conexión perdida con el servidor: " + reason);
    }

    // --- Getters y Registro de Listeners ---

    @Override
    public ConnectionState getConnectionState() {
        return currentState;
    }

    @Override
    public ConnectionRole getCurrentRole() {
        return currentRole;
    }

    @Override
    public String getPeerAddress() {
        return peerAddress;
    }

    @Override
    public int getActivePort() {
        return activePort;
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null) connectionListeners.add(listener);
    }

    @Override
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    @Override
    public void addMessageListener(MessageListener listener) {
        if (listener != null) messageListeners.add(listener);
    }

    @Override
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }
}

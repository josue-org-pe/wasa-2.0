package com.chatlocal.backend.service;

import com.chatlocal.backend.event.ConexionDeEscucha;
import com.chatlocal.backend.event.MensajeDeEscucha;
import com.chatlocal.backend.model.*;
import com.chatlocal.backend.network.ConstantesProtocolo;
import com.chatlocal.backend.network.ConexionSocket;
import com.chatlocal.backend.service.videocall.ServicioVideollamada;
import com.chatlocal.backend.service.videocall.ServicioVideollamadaImpl;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

// implementacion del servicio de chat con sockets, salas, notas de voz y llamadas
public class ServicioChatImpl implements ServicioChat, ConexionSocket.ConnectionCallback {

    private final List<ConexionDeEscucha> connectionListeners = new CopyOnWriteArrayList<>();
    private final List<MensajeDeEscucha> messageListeners = new CopyOnWriteArrayList<>();

    private volatile EstadoConexion currentState = EstadoConexion.DISCONNECTED;
    private volatile RolConexion currentRole = null;
    private volatile String peerAddress = "";
    private volatile int activePort = ConstantesProtocolo.DEFAULT_PORT;

    private volatile UsuarioPerfil localUserProfile = new UsuarioPerfil("Usuario", RolConexion.CLIENT, "127.0.0.1", ConstantesProtocolo.DEFAULT_PORT);
    private final List<UsuarioPerfil> connectedUsers = new CopyOnWriteArrayList<>();
    private final Set<String> blockedUsers = ConcurrentHashMap.newKeySet();

    // Salas y reuniones
    private final List<SalaChat> rooms = new CopyOnWriteArrayList<>();
    private volatile String activeRoomId = "general";

    // Historiales de conversación por sala ("room_<id>") o chat privado ("private_<usuario>")
    private final Map<String, List<MensajeChat>> conversationHistories = new ConcurrentHashMap<>();
    private final Map<ConexionSocket, UsuarioPerfil> connectionProfiles = new ConcurrentHashMap<>();
    private volatile String activeConversationId = "room_general";
    private volatile UsuarioPerfil activePrivateUser = null;
    private volatile String chatWallpaperPath = null;

    // Grabador de voz
    private final ServicioGrabadorAudio audioRecorder = new ServicioGrabadorAudio();

    // Manejo de conexiones multicliente (Servidor / Host)
    private final List<ConexionSocket> clientConnections = new CopyOnWriteArrayList<>();

    // Conexión individual (Cliente)
    private ConexionSocket clientToServerConnection;

    private ServerSocket serverSocket;
    private Thread connectionWorker;
    private final AtomicBoolean pongReceived = new AtomicBoolean(false);

    private final ServicioVideollamadaImpl videoCallService;

    public ServicioChatImpl() {
        // Inicializar sala general por defecto
        rooms.add(SalaChat.createDefaultGeneralRoom());

        this.videoCallService = new ServicioVideollamadaImpl(localUserProfile, new ServicioVideollamadaImpl.CallSignalingHandler() {
            @Override
            public void sendCallRequest(String callerName, int mediaPort) throws IOException {
                if (currentRole == RolConexion.HOST) {
                    for (ConexionSocket conn : clientConnections) {
                        if (conn.isConnected()) conn.sendCallRequest(callerName, mediaPort);
                    }
                } else if (clientToServerConnection != null && clientToServerConnection.isConnected()) {
                    clientToServerConnection.sendCallRequest(callerName, mediaPort);
                }
            }

            @Override
            public void sendCallAccept(String acceptorName, int mediaPort) throws IOException {
                if (currentRole == RolConexion.HOST) {
                    for (ConexionSocket conn : clientConnections) {
                        if (conn.isConnected()) conn.sendCallAccept(acceptorName, mediaPort);
                    }
                } else if (clientToServerConnection != null && clientToServerConnection.isConnected()) {
                    clientToServerConnection.sendCallAccept(acceptorName, mediaPort);
                }
            }

            @Override
            public void sendCallReject(String reason) throws IOException {
                if (currentRole == RolConexion.HOST) {
                    for (ConexionSocket conn : clientConnections) {
                        if (conn.isConnected()) conn.sendCallReject(reason);
                    }
                } else if (clientToServerConnection != null && clientToServerConnection.isConnected()) {
                    clientToServerConnection.sendCallReject(reason);
                }
            }

            @Override
            public void sendCallEnd(String reason) throws IOException {
                if (currentRole == RolConexion.HOST) {
                    for (ConexionSocket conn : clientConnections) {
                        if (conn.isConnected()) conn.sendCallEnd(reason);
                    }
                } else if (clientToServerConnection != null && clientToServerConnection.isConnected()) {
                    clientToServerConnection.sendCallEnd(reason);
                }
            }
        });
    }

    @Override
    public void setLocalUserProfile(UsuarioPerfil profile) {
        if (profile != null) {
            this.localUserProfile = profile;
        }
    }

    @Override
    public UsuarioPerfil getLocalUserProfile() {
        return localUserProfile;
    }

    @Override
    public List<UsuarioPerfil> getConnectedUsers() {
        return connectedUsers;
    }

    @Override
    public int getConnectedUserCount() {
        return (currentRole == RolConexion.HOST) ? clientConnections.size() : (clientToServerConnection != null && clientToServerConnection.isConnected() ? 1 : 0);
    }

    // --- Gestión de Salas ---

    @Override
    public List<SalaChat> getRooms() {
        return rooms;
    }

    @Override
    public void createRoom(String name, String topic, String accessCode, boolean isMeeting) {
        SalaChat room = new SalaChat(name, topic, accessCode, isMeeting, localUserProfile.getUsername());
        rooms.add(room);
        this.activeRoomId = room.getId();
    }

    @Override
    public SalaChat getActiveRoom() {
        for (SalaChat r : rooms) {
            if (r.getId().equals(activeRoomId)) return r;
        }
        return rooms.isEmpty() ? SalaChat.createDefaultGeneralRoom() : rooms.get(0);
    }

    @Override
    public void setActiveRoom(String roomId) {
        if (roomId != null) {
            this.activeRoomId = roomId;
            this.activePrivateUser = null;
            this.activeConversationId = "room_" + roomId;
            SalaChat room = getActiveRoom();
            if (room != null) room.resetUnread();
        }
    }

    @Override
    public String getActiveConversationId() {
        return activeConversationId;
    }

    @Override
    public void setActiveConversation(String conversationId, String displayName) {
        if (conversationId != null) {
            this.activeConversationId = conversationId;
            if (conversationId.startsWith("room_")) {
                this.activeRoomId = conversationId.substring("room_".length());
                this.activePrivateUser = null;
            }
        }
    }

    @Override
    public void setActivePrivateUser(UsuarioPerfil user) {
        this.activePrivateUser = user;
        if (user != null) {
            this.activeConversationId = "private_" + user.getUsername();
        } else {
            this.activeConversationId = "room_" + activeRoomId;
        }
    }

    @Override
    public UsuarioPerfil getActivePrivateUser() {
        return activePrivateUser;
    }

    @Override
    public List<MensajeChat> getConversationMessages(String conversationId) {
        List<MensajeChat> list = conversationHistories.get(conversationId);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public void storeMessage(String conversationId, MensajeChat msg) {
        if (conversationId == null || msg == null) return;
        conversationHistories.computeIfAbsent(conversationId, k -> new CopyOnWriteArrayList<>()).add(msg);
    }

    @Override
    public String getChatWallpaperPath() {
        return chatWallpaperPath;
    }

    @Override
    public void setChatWallpaperPath(String path) {
        this.chatWallpaperPath = path;
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
    public ServicioGrabadorAudio getAudioRecorder() {
        return audioRecorder;
    }

    // --- Red y Conexiones ---

    @Override
    public void startHost(int port) {
        disconnect();
        this.currentRole = RolConexion.HOST;
        this.activePort = port;
        this.pongReceived.set(false);
        this.connectedUsers.clear();
        this.clientConnections.clear();

        updateState(EstadoConexion.LISTENING, "Servidor activo. Esperando conexiones en puerto " + port + "...");

        connectionWorker = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(port));

                while (currentState == EstadoConexion.LISTENING || currentState == EstadoConexion.CONNECTED) {
                    try {
                        Socket acceptedSocket = serverSocket.accept();
                        handleIncomingClient(acceptedSocket);
                    } catch (IOException e) {
                        if (serverSocket == null || serverSocket.isClosed()) break;
                    }
                }
            } catch (IOException e) {
                if (currentState != EstadoConexion.DISCONNECTED) {
                    notifyError("No se pudo iniciar el servidor en el puerto " + port + ": " + e.getMessage());
                    updateState(EstadoConexion.ERROR, "Fallo al iniciar servidor: " + e.getMessage());
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
            UsuarioPerfil newUser = new UsuarioPerfil(clientIp, RolConexion.CLIENT, clientIp, clientPort);
            connectedUsers.add(newUser);

            ConexionSocket conn = new ConexionSocket(socket, new ConexionSocket.ConnectionCallback() {
                @Override
                public void onTextMessageReceived(MensajeChat message) {
                    if (isUserBlocked(message.getSender())) return;
                    handleIncomingMessageHost(message, socket);
                }

                @Override
                public void onFileReceived(MensajeChat message) {
                    if (isUserBlocked(message.getSender())) return;
                    handleIncomingFileHost(message, socket);
                }

                @Override
                public void onAudioReceived(MensajeChat message) {
                    if (isUserBlocked(message.getSender())) return;
                    handleIncomingAudioHost(message, socket);
                }

                @Override
                public void onAckReceived(String messageId) {
                    handleAck(messageId);
                }

                @Override
                public void onHandshakeReceived(String username, int colorHex, String avatarPath) {
                    newUser.setUsername(username);
                    newUser.setAvatarColorHex(colorHex);
                    newUser.setAvatarImagePath(avatarPath);
                    updateState(currentState, "Usuario conectado: " + username + " (" + clientIp + ")");
                }

                @Override
                public void onPongReceived() {
                    pongReceived.set(true);
                }

                @Override
                public void onCallRequestReceived(String callerName, String peerIp, int mediaPort) {
                    if (isUserBlocked(callerName)) return;
                    videoCallService.handleIncomingCall(callerName, peerIp, mediaPort);
                }

                @Override
                public void onCallAcceptReceived(String acceptorName, String peerIp, int mediaPort) {
                    videoCallService.handleCallAccepted(acceptorName, peerIp, mediaPort);
                }

                @Override
                public void onCallRejectReceived(String reason) {
                    videoCallService.handleCallRejected(reason);
                }

                @Override
                public void onCallEndReceived(String reason) {
                    videoCallService.handleCallEnded(reason);
                }

                @Override
                public void onConnectionLost(String reason) {
                    clientConnections.removeIf(c -> c.getSocket() == socket);
                    connectedUsers.remove(newUser);
                    for (Map.Entry<ConexionSocket, UsuarioPerfil> e : connectionProfiles.entrySet()) {
                        if (e.getKey().getSocket() == socket) {
                            connectionProfiles.remove(e.getKey());
                            break;
                        }
                    }
                    updateState(currentState, "Usuario desconectado (" + clientIp + ")");
                }
            });

            clientConnections.add(conn);
            connectionProfiles.put(conn, newUser);
            peerAddress = clientIp;

            // Enviar Handshake del Host al Cliente
            try {
                conn.sendHandshake(localUserProfile.getUsername(), localUserProfile.getAvatarColorHex(), localUserProfile.getAvatarImagePath());
            } catch (IOException ignored) {}

            conn.sendPing();

            if (currentState != EstadoConexion.CONNECTED) {
                updateState(EstadoConexion.CONNECTED, "Cliente conectado desde " + clientIp);
            } else {
                updateState(EstadoConexion.CONNECTED, "Nuevo usuario en red: " + clientIp + " (Total: " + clientConnections.size() + ")");
            }

        } catch (IOException e) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleIncomingMessageHost(MensajeChat message, Socket sourceSocket) {
        if (message.isPrivate()) {
            String recipient = message.getRecipient();
            boolean isForHost = recipient == null || recipient.isEmpty() ||
                    recipient.equalsIgnoreCase(localUserProfile.getUsername()) ||
                    recipient.equalsIgnoreCase("Yo") ||
                    recipient.equalsIgnoreCase("Host");

            if (isForHost) {
                String convKey = "private_" + message.getSender();
                storeMessage(convKey, message);
                if (activeConversationId.equals(convKey)) {
                    notifyMessageReceived(message);
                }
            } else {
                // Reenviar exclusivamente al cliente destino
                for (Map.Entry<ConexionSocket, UsuarioPerfil> entry : connectionProfiles.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().getUsername().equalsIgnoreCase(recipient)) {
                        try {
                            entry.getKey().sendTextMessage(message.getId(), message.getSender(), message.getSenderColorHex(),
                                    message.getContent(), message.getRoomId(), message.getRecipient(), message.getSenderAvatarPath());
                        } catch (IOException ignored) {}
                        break;
                    }
                }
            }
        } else {
            String convKey = "room_" + message.getRoomId();
            storeMessage(convKey, message);
            if (activeConversationId.equals(convKey)) {
                notifyMessageReceived(message);
            }
            broadcastMessage(message, sourceSocket);
        }
    }

    private void handleIncomingFileHost(MensajeChat message, Socket sourceSocket) {
        if (message.isPrivate()) {
            String recipient = message.getRecipient();
            boolean isForHost = recipient == null || recipient.isEmpty() ||
                    recipient.equalsIgnoreCase(localUserProfile.getUsername()) ||
                    recipient.equalsIgnoreCase("Yo") ||
                    recipient.equalsIgnoreCase("Host");

            if (isForHost) {
                String convKey = "private_" + message.getSender();
                storeMessage(convKey, message);
                if (activeConversationId.equals(convKey)) {
                    notifyFileReceived(message);
                }
            }
        } else {
            String convKey = "room_" + message.getRoomId();
            storeMessage(convKey, message);
            if (activeConversationId.equals(convKey)) {
                notifyFileReceived(message);
            }
        }
    }

    private void handleIncomingAudioHost(MensajeChat message, Socket sourceSocket) {
        if (message.isPrivate()) {
            String recipient = message.getRecipient();
            boolean isForHost = recipient == null || recipient.isEmpty() ||
                    recipient.equalsIgnoreCase(localUserProfile.getUsername()) ||
                    recipient.equalsIgnoreCase("Yo") ||
                    recipient.equalsIgnoreCase("Host");

            if (isForHost) {
                String convKey = "private_" + message.getSender();
                storeMessage(convKey, message);
                if (activeConversationId.equals(convKey)) {
                    notifyAudioReceived(message);
                }
            }
        } else {
            String convKey = "room_" + message.getRoomId();
            storeMessage(convKey, message);
            if (activeConversationId.equals(convKey)) {
                notifyAudioReceived(message);
            }
        }
    }

    private void handleAck(String messageId) {
        if (messageId == null || messageId.isEmpty()) return;
        for (List<MensajeChat> list : conversationHistories.values()) {
            for (MensajeChat m : list) {
                if (messageId.equals(m.getId())) {
                    m.setStatus(EstadoMensaje.DELIVERED);
                    SwingUtilities.invokeLater(() -> {
                        for (MensajeDeEscucha listener : messageListeners) {
                            listener.onMessageStatusChanged(messageId, EstadoMensaje.DELIVERED);
                        }
                    });
                    return;
                }
            }
        }
    }

    private ConexionSocket findConnectionForUser(UsuarioPerfil target) {
        if (target == null) return null;
        for (Map.Entry<ConexionSocket, UsuarioPerfil> entry : connectionProfiles.entrySet()) {
            UsuarioPerfil u = entry.getValue();
            if (u != null && (u.getUsername().equalsIgnoreCase(target.getUsername()) ||
                    (u.getIpAddress().equals(target.getIpAddress()) && u.getPort() == target.getPort()))) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void broadcastMessage(MensajeChat message, Socket excludeSocket) {
        for (ConexionSocket conn : clientConnections) {
            if (conn.isConnected() && conn.getSocket() != excludeSocket) {
                try {
                    conn.sendTextMessage(message.getId(), message.getSender(), message.getSenderColorHex(),
                            message.getContent(), message.getRoomId(), "", message.getSenderAvatarPath());
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public void connectToHost(String hostIp, int port) {
        disconnect();
        this.currentRole = RolConexion.CLIENT;
        this.peerAddress = hostIp;
        this.activePort = port;
        this.pongReceived.set(false);

        updateState(EstadoConexion.CONNECTING, "Conectando con el servidor " + hostIp + ":" + port + "...");

        connectionWorker = new Thread(() -> {
            try {
                Socket clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(hostIp, port), 5000);

                updateState(EstadoConexion.VERIFYING, "Conectado. Verificando canal de comunicación...");

                clientToServerConnection = new ConexionSocket(clientSocket, this);

                // Enviar Handshake inicial con perfil local
                try {
                    clientToServerConnection.sendHandshake(localUserProfile.getUsername(), localUserProfile.getAvatarColorHex(), localUserProfile.getAvatarImagePath());
                } catch (IOException ignored) {}

                clientToServerConnection.sendPing();

                long deadline = System.currentTimeMillis() + ConstantesProtocolo.PING_TIMEOUT_MS;
                while (!pongReceived.get() && System.currentTimeMillis() < deadline && clientToServerConnection.isConnected()) {
                    Thread.sleep(100);
                }

                if (pongReceived.get()) {
                    updateState(EstadoConexion.CONNECTED, "Conexión verificada con éxito.");
                } else if (clientToServerConnection.isConnected()) {
                    updateState(EstadoConexion.CONNECTED, "Conectado a la sala.");
                } else {
                    updateState(EstadoConexion.ERROR, "El servidor cerró la conexión durante la verificación.");
                }
            } catch (Exception e) {
                if (currentState != EstadoConexion.DISCONNECTED) {
                    notifyError("No se pudo conectar a " + hostIp + ":" + port + ". " + e.getMessage());
                    updateState(EstadoConexion.ERROR, "Fallo al conectar: " + e.getMessage());
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
        String avatarPath = (localUserProfile != null) ? localUserProfile.getAvatarImagePath() : "";
        String msgId = UUID.randomUUID().toString();

        if (activePrivateUser != null) {
            String recipient = activePrivateUser.getUsername();
            MensajeChat msg = new MensajeChat(msgId, sender, color, text, TipoMensaje.TEXT, true, null, 0, null, 0, "private", avatarPath, recipient);
            storeMessage("private_" + recipient, msg);

            if (currentRole == RolConexion.HOST) {
                ConexionSocket conn = findConnectionForUser(activePrivateUser);
                if (conn != null && conn.isConnected()) {
                    conn.sendTextMessage(msgId, sender, color, text, "private", recipient, avatarPath);
                } else {
                    throw new IOException("El usuario " + recipient + " no está conectado.");
                }
            } else {
                if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                    throw new IOException("No hay conexión con el servidor.");
                }
                clientToServerConnection.sendTextMessage(msgId, sender, color, text, "private", recipient, avatarPath);
            }
            notifyMessageSent(msg);
        } else {
            MensajeChat msg = new MensajeChat(msgId, sender, color, text, TipoMensaje.TEXT, true, null, 0, null, 0, activeRoomId, avatarPath, null);
            storeMessage("room_" + activeRoomId, msg);

            if (currentRole == RolConexion.HOST) {
                if (clientConnections.isEmpty()) {
                    throw new IOException("No hay otros usuarios en la sala actualmente.");
                }
                for (ConexionSocket conn : clientConnections) {
                    if (conn.isConnected()) {
                        conn.sendTextMessage(msgId, sender, color, text, activeRoomId, "", avatarPath);
                    }
                }
            } else {
                if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                    throw new IOException("No hay conexión con el servidor.");
                }
                clientToServerConnection.sendTextMessage(msgId, sender, color, text, activeRoomId, "", avatarPath);
            }
            notifyMessageSent(msg);
        }
    }

    @Override
    public void sendAudioMessage(File audioFile, int durationSeconds) throws IOException {
        String sender = (localUserProfile != null) ? localUserProfile.getUsername() : "Yo";
        int color = (localUserProfile != null) ? localUserProfile.getAvatarColorHex() : 0x6366F1;
        String avatarPath = (localUserProfile != null) ? localUserProfile.getAvatarImagePath() : "";
        String msgId = UUID.randomUUID().toString();

        if (activePrivateUser != null) {
            String recipient = activePrivateUser.getUsername();
            MensajeChat msg = new MensajeChat(msgId, sender, color, "Nota de voz (" + durationSeconds + "s)", TipoMensaje.AUDIO, true, "audio_nota.wav", 0, audioFile.getAbsolutePath(), durationSeconds, "private", avatarPath, recipient);
            storeMessage("private_" + recipient, msg);

            if (currentRole == RolConexion.HOST) {
                ConexionSocket conn = findConnectionForUser(activePrivateUser);
                if (conn != null && conn.isConnected()) {
                    conn.sendAudio(msgId, sender, color, audioFile, durationSeconds, "private", recipient, avatarPath);
                } else {
                    throw new IOException("El usuario " + recipient + " no está conectado.");
                }
            } else {
                if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                    throw new IOException("No hay conexión con el servidor.");
                }
                clientToServerConnection.sendAudio(msgId, sender, color, audioFile, durationSeconds, "private", recipient, avatarPath);
            }
            notifyMessageSent(msg);
        } else {
            MensajeChat msg = new MensajeChat(msgId, sender, color, "Nota de voz (" + durationSeconds + "s)", TipoMensaje.AUDIO, true, "audio_nota.wav", 0, audioFile.getAbsolutePath(), durationSeconds, activeRoomId, avatarPath, null);
            storeMessage("room_" + activeRoomId, msg);

            if (currentRole == RolConexion.HOST) {
                for (ConexionSocket conn : clientConnections) {
                    if (conn.isConnected()) {
                        conn.sendAudio(msgId, sender, color, audioFile, durationSeconds, activeRoomId, "", avatarPath);
                    }
                }
            } else {
                if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                    throw new IOException("No hay conexión con el servidor.");
                }
                clientToServerConnection.sendAudio(msgId, sender, color, audioFile, durationSeconds, activeRoomId, "", avatarPath);
            }
            notifyMessageSent(msg);
        }
    }

    @Override
    public void sendStickerMessage(String stickerText) throws IOException {
        String sender = (localUserProfile != null) ? localUserProfile.getUsername() : "Yo";
        int color = (localUserProfile != null) ? localUserProfile.getAvatarColorHex() : 0x6366F1;
        String avatarPath = (localUserProfile != null) ? localUserProfile.getAvatarImagePath() : "";
        String msgId = UUID.randomUUID().toString();

        if (activePrivateUser != null) {
            String recipient = activePrivateUser.getUsername();
            MensajeChat msg = new MensajeChat(msgId, sender, color, stickerText, TipoMensaje.STICKER, true, null, 0, null, 0, "private", avatarPath, recipient);
            storeMessage("private_" + recipient, msg);

            if (currentRole == RolConexion.HOST) {
                ConexionSocket conn = findConnectionForUser(activePrivateUser);
                if (conn != null && conn.isConnected()) {
                    conn.sendSticker(msgId, sender, color, stickerText, "private", recipient, avatarPath);
                } else {
                    throw new IOException("El usuario " + recipient + " no está conectado.");
                }
            } else {
                if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                    throw new IOException("No hay conexión con el servidor.");
                }
                clientToServerConnection.sendSticker(msgId, sender, color, stickerText, "private", recipient, avatarPath);
            }
            notifyMessageSent(msg);
        } else {
            MensajeChat msg = new MensajeChat(msgId, sender, color, stickerText, TipoMensaje.STICKER, true, null, 0, null, 0, activeRoomId, avatarPath, null);
            storeMessage("room_" + activeRoomId, msg);

            if (currentRole == RolConexion.HOST) {
                for (ConexionSocket conn : clientConnections) {
                    if (conn.isConnected()) {
                        conn.sendSticker(msgId, sender, color, stickerText, activeRoomId, "", avatarPath);
                    }
                }
            } else {
                if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                    throw new IOException("No hay conexión con el servidor.");
                }
                clientToServerConnection.sendSticker(msgId, sender, color, stickerText, activeRoomId, "", avatarPath);
            }
            notifyMessageSent(msg);
        }
    }

    @Override
    public void sendFile(File file) throws IOException {
        String sender = (localUserProfile != null) ? localUserProfile.getUsername() : "Yo";
        int color = (localUserProfile != null) ? localUserProfile.getAvatarColorHex() : 0x6366F1;
        String avatarPath = (localUserProfile != null) ? localUserProfile.getAvatarImagePath() : "";
        String msgId = UUID.randomUUID().toString();

        if (activePrivateUser != null) {
            String recipient = activePrivateUser.getUsername();
            MensajeChat msg = new MensajeChat(msgId, sender, color, "Has enviado un archivo", TipoMensaje.FILE, true, file.getName(), file.length(), file.getAbsolutePath(), 0, "private", avatarPath, recipient);
            storeMessage("private_" + recipient, msg);

            if (currentRole == RolConexion.HOST) {
                ConexionSocket conn = findConnectionForUser(activePrivateUser);
                if (conn != null && conn.isConnected()) {
                    conn.sendFile(msgId, sender, color, file, "private", recipient, avatarPath);
                } else {
                    throw new IOException("El usuario " + recipient + " no está conectado.");
                }
            } else {
                if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                    throw new IOException("No hay conexión con el servidor.");
                }
                clientToServerConnection.sendFile(msgId, sender, color, file, "private", recipient, avatarPath);
            }
            notifyMessageSent(msg);
        } else {
            MensajeChat msg = new MensajeChat(msgId, sender, color, "Has enviado un archivo", TipoMensaje.FILE, true, file.getName(), file.length(), file.getAbsolutePath(), 0, activeRoomId, avatarPath, null);
            storeMessage("room_" + activeRoomId, msg);

            if (currentRole == RolConexion.HOST) {
                for (ConexionSocket conn : clientConnections) {
                    if (conn.isConnected()) {
                        conn.sendFile(msgId, sender, color, file, activeRoomId, "", avatarPath);
                    }
                }
            } else {
                if (clientToServerConnection == null || !clientToServerConnection.isConnected()) {
                    throw new IOException("No hay conexión con el servidor.");
                }
                clientToServerConnection.sendFile(msgId, sender, color, file, activeRoomId, "", avatarPath);
            }
            notifyMessageSent(msg);
        }
    }

    @Override
    public synchronized void disconnect() {
        if (videoCallService != null) {
            videoCallService.endCall();
        }
        if (connectionWorker != null && connectionWorker.isAlive()) {
            connectionWorker.interrupt();
        }
        closeServerSocket();

        for (ConexionSocket conn : clientConnections) {
            conn.close();
        }
        clientConnections.clear();
        connectedUsers.clear();

        if (clientToServerConnection != null) {
            clientToServerConnection.close();
            clientToServerConnection = null;
        }

        if (currentState != EstadoConexion.DISCONNECTED) {
            updateState(EstadoConexion.DISCONNECTED, "Desconectado.");
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

    private void updateState(EstadoConexion newState, String details) {
        this.currentState = newState;
        SwingUtilities.invokeLater(() -> {
            for (ConexionDeEscucha listener : connectionListeners) {
                listener.onConnectionStateChanged(newState, details);
            }
        });
    }

    private void notifyError(String error) {
        SwingUtilities.invokeLater(() -> {
            for (ConexionDeEscucha listener : connectionListeners) {
                listener.onConnectionError(error);
            }
        });
    }

    private void notifyMessageSent(MensajeChat message) {
        SwingUtilities.invokeLater(() -> {
            for (MensajeDeEscucha listener : messageListeners) {
                listener.onMessageSent(message);
            }
        });
    }

    private void notifyMessageReceived(MensajeChat message) {
        SwingUtilities.invokeLater(() -> {
            for (MensajeDeEscucha listener : messageListeners) {
                listener.onMessageReceived(message);
            }
        });
    }

    private void notifyFileReceived(MensajeChat message) {
        SwingUtilities.invokeLater(() -> {
            for (MensajeDeEscucha listener : messageListeners) {
                listener.onFileReceived(message);
            }
        });
    }

    private void notifyAudioReceived(MensajeChat message) {
        SwingUtilities.invokeLater(() -> {
            for (MensajeDeEscucha listener : messageListeners) {
                listener.onAudioReceived(message);
            }
        });
    }

    // --- Callbacks de ConexionSocket.ConnectionCallback (para modo Cliente) ---

    @Override
    public void onTextMessageReceived(MensajeChat message) {
        if (isUserBlocked(message.getSender())) return;
        String convKey = message.isPrivate() ? ("private_" + message.getSender()) : ("room_" + message.getRoomId());
        storeMessage(convKey, message);
        if (activeConversationId.equals(convKey)) {
            notifyMessageReceived(message);
        }
    }

    @Override
    public void onFileReceived(MensajeChat message) {
        if (isUserBlocked(message.getSender())) return;
        String convKey = message.isPrivate() ? ("private_" + message.getSender()) : ("room_" + message.getRoomId());
        storeMessage(convKey, message);
        if (activeConversationId.equals(convKey)) {
            notifyFileReceived(message);
        }
    }

    @Override
    public void onAudioReceived(MensajeChat message) {
        if (isUserBlocked(message.getSender())) return;
        String convKey = message.isPrivate() ? ("private_" + message.getSender()) : ("room_" + message.getRoomId());
        storeMessage(convKey, message);
        if (activeConversationId.equals(convKey)) {
            notifyAudioReceived(message);
        }
    }

    @Override
    public void onAckReceived(String messageId) {
        handleAck(messageId);
    }

    @Override
    public void onHandshakeReceived(String username, int colorHex, String avatarPath) {
        UsuarioPerfil hostProfile = new UsuarioPerfil(username, RolConexion.HOST, peerAddress, activePort);
        hostProfile.setAvatarColorHex(colorHex);
        hostProfile.setAvatarImagePath(avatarPath);
        connectedUsers.removeIf(u -> u.getRole() == RolConexion.HOST);
        connectedUsers.add(0, hostProfile);
        updateState(currentState, "Conectado con " + username);
    }

    @Override
    public void onPongReceived() {
        pongReceived.set(true);
    }

    @Override
    public void onCallRequestReceived(String callerName, String peerIp, int mediaPort) {
        if (isUserBlocked(callerName)) return;
        videoCallService.handleIncomingCall(callerName, peerIp, mediaPort);
    }

    @Override
    public void onCallAcceptReceived(String acceptorName, String peerIp, int mediaPort) {
        videoCallService.handleCallAccepted(acceptorName, peerIp, mediaPort);
    }

    @Override
    public void onCallRejectReceived(String reason) {
        videoCallService.handleCallRejected(reason);
    }

    @Override
    public void onCallEndReceived(String reason) {
        videoCallService.handleCallEnded(reason);
    }

    @Override
    public void onConnectionLost(String reason) {
        updateState(EstadoConexion.DISCONNECTED, "Conexión perdida con el servidor: " + reason);
    }

    // --- Getters y Registro de Listeners ---

    @Override
    public EstadoConexion getConnectionState() {
        return currentState;
    }

    @Override
    public RolConexion getCurrentRole() {
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
    public void addConnectionListener(ConexionDeEscucha listener) {
        if (listener != null) connectionListeners.add(listener);
    }

    @Override
    public void removeConnectionListener(ConexionDeEscucha listener) {
        connectionListeners.remove(listener);
    }

    @Override
    public void addMessageListener(MensajeDeEscucha listener) {
        if (listener != null) messageListeners.add(listener);
    }

    @Override
    public void removeMessageListener(MensajeDeEscucha listener) {
        messageListeners.remove(listener);
    }

    // --- Soporte de Videollamada ---

    @Override
    public ServicioVideollamada getVideoCallService() {
        return videoCallService;
    }

    @Override
    public void initiateVideoCall() {
        if (currentState != EstadoConexion.CONNECTED) {
            JOptionPane.showMessageDialog(null, "Debes estar conectado a una sala o usuario para iniciar una videollamada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String target = "Contacto";
        if (!connectedUsers.isEmpty()) {
            target = connectedUsers.get(0).getUsername();
        } else if (currentRole == RolConexion.CLIENT) {
            target = "Host (" + peerAddress + ")";
        }

        videoCallService.initiateCall(target);
    }
}

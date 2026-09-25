package com.chatlocal.backend.service;

import com.chatlocal.backend.event.ConnectionListener;
import com.chatlocal.backend.event.MessageListener;
import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.ChatRoom;
import com.chatlocal.backend.model.ConnectionRole;
import com.chatlocal.backend.model.ConnectionState;
import com.chatlocal.backend.model.UserProfile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Contrato del servicio de backend para operaciones de red, salas, notas de voz y chat.
 */
public interface ChatService {

    void startHost(int port);

    void connectToHost(String hostIp, int port);

    void sendTextMessage(String text) throws IOException;

    void sendFile(File file) throws IOException;

    void sendAudioMessage(File audioFile, int durationSeconds) throws IOException;

    void sendStickerMessage(String stickerText) throws IOException;

    void disconnect();

    ConnectionState getConnectionState();

    ConnectionRole getCurrentRole();

    String getPeerAddress();

    int getActivePort();

    void setLocalUserProfile(UserProfile profile);

    UserProfile getLocalUserProfile();

    List<UserProfile> getConnectedUsers();

    int getConnectedUserCount();

    // Gestión de Salas y Reuniones
    List<ChatRoom> getRooms();

    void createRoom(String name, String topic, String accessCode, boolean isMeeting);

    ChatRoom getActiveRoom();

    void setActiveRoom(String roomId);

    // Gestión de Conversaciones e Historiales
    String getActiveConversationId();

    void setActiveConversation(String conversationId, String displayName);

    void setActivePrivateUser(UserProfile user);

    UserProfile getActivePrivateUser();

    List<ChatMessage> getConversationMessages(String conversationId);

    // Fondo de pantalla del chat (Wallpaper)
    String getChatWallpaperPath();

    void setChatWallpaperPath(String path);

    // Bloqueo de Contactos
    void blockUser(String username);

    void unblockUser(String username);

    boolean isUserBlocked(String username);

    List<String> getBlockedUsers();

    // Grabador de Audio
    AudioRecorderService getAudioRecorder();

    // Videollamada
    com.chatlocal.backend.service.videocall.VideoCallService getVideoCallService();

    void initiateVideoCall();

    void addConnectionListener(ConnectionListener listener);

    void removeConnectionListener(ConnectionListener listener);

    void addMessageListener(MessageListener listener);

    void removeMessageListener(MessageListener listener);
}

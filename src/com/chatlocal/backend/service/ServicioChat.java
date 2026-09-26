package com.chatlocal.backend.service;

import com.chatlocal.backend.event.ConexionDeEscucha;
import com.chatlocal.backend.event.MensajeDeEscucha;
import com.chatlocal.backend.model.MensajeChat;
import com.chatlocal.backend.model.SalaChat;
import com.chatlocal.backend.model.RolConexion;
import com.chatlocal.backend.model.EstadoConexion;
import com.chatlocal.backend.model.UsuarioPerfil;

import java.io.File;
import java.io.IOException;
import java.util.List;

// interfaz con los metodos principales del servicio de chat
public interface ServicioChat {

    void startHost(int port);

    void connectToHost(String hostIp, int port);

    void sendTextMessage(String text) throws IOException;

    void sendFile(File file) throws IOException;

    void sendAudioMessage(File audioFile, int durationSeconds) throws IOException;

    void sendStickerMessage(String stickerText) throws IOException;

    void disconnect();

    EstadoConexion getConnectionState();

    RolConexion getCurrentRole();

    String getPeerAddress();

    int getActivePort();

    void setLocalUserProfile(UsuarioPerfil profile);

    UsuarioPerfil getLocalUserProfile();

    List<UsuarioPerfil> getConnectedUsers();

    int getConnectedUserCount();

    // Gestión de Salas y Reuniones
    List<SalaChat> getRooms();

    void createRoom(String name, String topic, String accessCode, boolean isMeeting);

    SalaChat getActiveRoom();

    void setActiveRoom(String roomId);

    // Gestión de Conversaciones e Historiales
    String getActiveConversationId();

    void setActiveConversation(String conversationId, String displayName);

    void setActivePrivateUser(UsuarioPerfil user);

    UsuarioPerfil getActivePrivateUser();

    List<MensajeChat> getConversationMessages(String conversationId);

    // Fondo de pantalla del chat (Wallpaper)
    String getChatWallpaperPath();

    void setChatWallpaperPath(String path);

    // Bloqueo de Contactos
    void blockUser(String username);

    void unblockUser(String username);

    boolean isUserBlocked(String username);

    List<String> getBlockedUsers();

    // Grabador de Audio
    ServicioGrabadorAudio getAudioRecorder();

    // Videollamada
    com.chatlocal.backend.service.videocall.ServicioVideollamada getVideoCallService();

    void initiateVideoCall();

    void addConnectionListener(ConexionDeEscucha listener);

    void removeConnectionListener(ConexionDeEscucha listener);

    void addMessageListener(MensajeDeEscucha listener);

    void removeMessageListener(MensajeDeEscucha listener);
}

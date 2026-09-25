package com.chatlocal.backend.service.videocall;

/**
 * Contrato del servicio de videollamadas para gestión del ciclo de llamada,
 * señalización, control de dispositivos (micrófono, cámara, pantalla) y eventos.
 */
public interface VideoCallService {

    void initiateCall(String targetName);

    void handleIncomingCall(String callerName, String peerIp, int mediaPort);

    void handleCallAccepted(String acceptorName, String peerIp, int mediaPort);

    void handleCallRejected(String reason);

    void handleCallEnded(String reason);

    void acceptCall();

    void rejectCall(String reason);

    void endCall();

    CallState getCallState();

    String getPeerName();

    void setMuted(boolean muted);

    boolean isMuted();

    void setVideoEnabled(boolean enabled);

    boolean isVideoEnabled();

    void setVideoSourceType(VideoSourceType type);

    VideoSourceType getVideoSourceType();

    void addListener(VideoCallListener listener);

    void removeListener(VideoCallListener listener);
}

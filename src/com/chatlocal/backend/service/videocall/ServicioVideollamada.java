package com.chatlocal.backend.service.videocall;

// interfaz del servicio de videollamadas (iniciar, contestar, colgar, silenciar)
public interface ServicioVideollamada {

    void initiateCall(String targetName);

    void handleIncomingCall(String callerName, String peerIp, int mediaPort);

    void handleCallAccepted(String acceptorName, String peerIp, int mediaPort);

    void handleCallRejected(String reason);

    void handleCallEnded(String reason);

    void acceptCall();

    void rejectCall(String reason);

    void endCall();

    EstadoLlamada getCallState();

    String getPeerName();

    void setMuted(boolean muted);

    boolean isMuted();

    void setVideoEnabled(boolean enabled);

    boolean isVideoEnabled();

    void setVideoSourceType(TipoFuenteVideo type);

    TipoFuenteVideo getVideoSourceType();

    void addListener(OyenteVideollamada listener);

    void removeListener(OyenteVideollamada listener);
}

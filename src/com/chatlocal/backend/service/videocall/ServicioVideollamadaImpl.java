package com.chatlocal.backend.service.videocall;

import com.chatlocal.backend.model.UsuarioPerfil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// esto maneja las videollamadas
public class ServicioVideollamadaImpl implements ServicioVideollamada, GestorTransmisionMedia.MediaFrameConsumer {

    public interface CallSignalingHandler {
        void sendCallRequest(String callerName, int mediaPort) throws IOException;
        void sendCallAccept(String acceptorName, int mediaPort) throws IOException;
        void sendCallReject(String reason) throws IOException;
        void sendCallEnd(String reason) throws IOException;
    }

    private final CallSignalingHandler signalingHandler;
    private final UsuarioPerfil localUser;
    private final List<OyenteVideollamada> listeners = new CopyOnWriteArrayList<>();

    private volatile EstadoLlamada callState = EstadoLlamada.IDLE;
    private volatile String peerName = "";
    private volatile String peerIp = "127.0.0.1";
    private volatile int peerMediaPort = -1;

    private GestorTransmisionMedia mediaManager;
    private TipoFuenteVideo currentSourceType = TipoFuenteVideo.PHYSICAL_WEBCAM;
    private boolean muted = false;
    private boolean videoEnabled = true;

    public ServicioVideollamadaImpl(UsuarioPerfil localUser, CallSignalingHandler signalingHandler) {
        this.localUser = localUser;
        this.signalingHandler = signalingHandler;
    }

    @Override
    public synchronized void initiateCall(String targetName) {
        if (callState != EstadoLlamada.IDLE && callState != EstadoLlamada.ENDED) {
            return;
        }

        try {
            cleanupMedia();
            this.peerName = (targetName != null && !targetName.isEmpty()) ? targetName : "Contacto";

            FuenteVideo source = createVideoSource(currentSourceType);
            this.mediaManager = new GestorTransmisionMedia(source, this);
            this.mediaManager.setMicMuted(muted);
            this.mediaManager.setVideoMuted(!videoEnabled);

            int localMediaPort = mediaManager.getLocalPort();
            String myName = (localUser != null) ? localUser.getUsername() : "Usuario";

            if (signalingHandler != null) {
                signalingHandler.sendCallRequest(myName, localMediaPort);
            }

            this.callState = EstadoLlamada.OUTGOING_CALL;
            notifyStateChanged(callState, peerName, "Llamando a " + peerName + "...");

            // Iniciar vista previa local mientras suena
            this.mediaManager.start();

        } catch (Exception e) {
            this.callState = EstadoLlamada.ENDED;
            notifyStateChanged(callState, peerName, "Error al iniciar videollamada: " + e.getMessage());
            cleanupMedia();
        }
    }

    @Override
    public synchronized void handleIncomingCall(String callerName, String peerIp, int mediaPort) {
        if (callState == EstadoLlamada.CONNECTED || callState == EstadoLlamada.OUTGOING_CALL) {
            // Ya ocupado en otra llamada
            if (signalingHandler != null) {
                try {
                    signalingHandler.sendCallReject("El usuario está ocupado en otra llamada.");
                } catch (IOException ignored) {}
            }
            return;
        }

        this.peerName = (callerName != null && !callerName.isEmpty()) ? callerName : "Contacto";
        this.peerIp = peerIp;
        this.peerMediaPort = mediaPort;
        this.callState = EstadoLlamada.INCOMING_CALL;

        notifyStateChanged(callState, peerName, "Videollamada entrante de " + peerName);
        for (OyenteVideollamada l : listeners) {
            l.onIncomingCallReceived(callerName, peerIp, mediaPort);
        }
    }

    @Override
    public synchronized void acceptCall() {
        if (callState != EstadoLlamada.INCOMING_CALL) return;

        try {
            cleanupMedia();
            FuenteVideo source = createVideoSource(currentSourceType);
            this.mediaManager = new GestorTransmisionMedia(source, this);
            this.mediaManager.setMicMuted(muted);
            this.mediaManager.setVideoMuted(!videoEnabled);
            this.mediaManager.setRemoteTarget(peerIp, peerMediaPort);

            int localMediaPort = mediaManager.getLocalPort();
            String myName = (localUser != null) ? localUser.getUsername() : "Usuario";

            if (signalingHandler != null) {
                signalingHandler.sendCallAccept(myName, localMediaPort);
            }

            this.mediaManager.start();
            this.callState = EstadoLlamada.CONNECTED;
            notifyStateChanged(callState, peerName, "En llamada con " + peerName);

        } catch (Exception e) {
            rejectCall("Error al iniciar multimedia local: " + e.getMessage());
        }
    }

    @Override
    public synchronized void handleCallAccepted(String acceptorName, String peerIp, int mediaPort) {
        this.peerName = (acceptorName != null && !acceptorName.isEmpty()) ? acceptorName : this.peerName;
        this.peerIp = peerIp;
        this.peerMediaPort = mediaPort;

        if (mediaManager != null) {
            mediaManager.setRemoteTarget(peerIp, mediaPort);
        }

        this.callState = EstadoLlamada.CONNECTED;
        notifyStateChanged(callState, peerName, "Conectado con " + peerName);
    }

    @Override
    public synchronized void rejectCall(String reason) {
        if (signalingHandler != null) {
            try {
                signalingHandler.sendCallReject(reason != null ? reason : "Llamada rechazada");
            } catch (IOException ignored) {}
        }
        this.callState = EstadoLlamada.ENDED;
        notifyStateChanged(callState, peerName, "Llamada rechazada");
        cleanupMedia();
    }

    @Override
    public synchronized void handleCallRejected(String reason) {
        this.callState = EstadoLlamada.ENDED;
        notifyStateChanged(callState, peerName, "Llamada rechazada: " + (reason != null ? reason : ""));
        cleanupMedia();
    }

    @Override
    public synchronized void endCall() {
        if (callState == EstadoLlamada.IDLE) return;

        if (signalingHandler != null) {
            try {
                signalingHandler.sendCallEnd("Llamada finalizada por el usuario");
            } catch (IOException ignored) {}
        }

        this.callState = EstadoLlamada.ENDED;
        notifyStateChanged(callState, peerName, "Videollamada finalizada");
        cleanupMedia();
    }

    @Override
    public synchronized void handleCallEnded(String reason) {
        this.callState = EstadoLlamada.ENDED;
        notifyStateChanged(callState, peerName, "El otro usuario finalizó la videollamada");
        cleanupMedia();
    }

    private void cleanupMedia() {
        if (mediaManager != null) {
            mediaManager.stop();
            mediaManager = null;
        }
    }

    private FuenteVideo createVideoSource(TipoFuenteVideo type) {
        String name = (localUser != null) ? localUser.getUsername() : "Yo";
        if (type == TipoFuenteVideo.PHYSICAL_WEBCAM) {
            return new FuenteCamaraWeb(name);
        } else if (type == TipoFuenteVideo.SCREEN_SHARE) {
            return new FuenteCompartirPantalla(name);
        }
        return new FuenteCamaraVirtual(name);
    }

    @Override
    public EstadoLlamada getCallState() {
        return callState;
    }

    @Override
    public String getPeerName() {
        return peerName;
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        if (mediaManager != null) {
            mediaManager.setMicMuted(muted);
        }
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public void setVideoEnabled(boolean enabled) {
        this.videoEnabled = enabled;
        if (mediaManager != null) {
            mediaManager.setVideoMuted(!enabled);
        }
    }

    @Override
    public boolean isVideoEnabled() {
        return videoEnabled;
    }

    @Override
    public synchronized void setVideoSourceType(TipoFuenteVideo type) {
        if (type != null && type != currentSourceType) {
            this.currentSourceType = type;
            if (mediaManager != null) {
                if (type == TipoFuenteVideo.CAMERA_OFF) {
                    mediaManager.setVideoMuted(true);
                } else {
                    mediaManager.setVideoMuted(false);
                    mediaManager.setVideoSource(createVideoSource(type));
                }
            }
        }
    }

    @Override
    public TipoFuenteVideo getVideoSourceType() {
        return currentSourceType;
    }

    @Override
    public void addListener(OyenteVideollamada listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(OyenteVideollamada listener) {
        listeners.remove(listener);
    }

    private void notifyStateChanged(EstadoLlamada state, String peer, String msg) {
        for (OyenteVideollamada l : listeners) {
            l.onCallStateChanged(state, peer, msg);
        }
    }

    // MediaFrameConsumer callbacks
    @Override
    public void onLocalFrame(BufferedImage frame) {
        for (OyenteVideollamada l : listeners) {
            l.onLocalFrameAvailable(frame);
        }
    }

    @Override
    public void onRemoteFrame(BufferedImage frame) {
        for (OyenteVideollamada l : listeners) {
            l.onRemoteFrameAvailable(frame);
        }
    }

    @Override
    public void onAudioLevels(float localLevel, float remoteLevel) {
        for (OyenteVideollamada l : listeners) {
            l.onAudioLevelsUpdated(localLevel, remoteLevel);
        }
    }
}

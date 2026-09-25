package com.chatlocal.backend.service.videocall;

import com.chatlocal.backend.model.UserProfile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementación del servicio de videollamadas.
 * Orquesta la señalización por sockets TCP y el intercambio de medios por UDP.
 */
public class VideoCallServiceImpl implements VideoCallService, MediaStreamManager.MediaFrameConsumer {

    public interface CallSignalingHandler {
        void sendCallRequest(String callerName, int mediaPort) throws IOException;
        void sendCallAccept(String acceptorName, int mediaPort) throws IOException;
        void sendCallReject(String reason) throws IOException;
        void sendCallEnd(String reason) throws IOException;
    }

    private final CallSignalingHandler signalingHandler;
    private final UserProfile localUser;
    private final List<VideoCallListener> listeners = new CopyOnWriteArrayList<>();

    private volatile CallState callState = CallState.IDLE;
    private volatile String peerName = "";
    private volatile String peerIp = "127.0.0.1";
    private volatile int peerMediaPort = -1;

    private MediaStreamManager mediaManager;
    private VideoSourceType currentSourceType = VideoSourceType.PHYSICAL_WEBCAM;
    private boolean muted = false;
    private boolean videoEnabled = true;

    public VideoCallServiceImpl(UserProfile localUser, CallSignalingHandler signalingHandler) {
        this.localUser = localUser;
        this.signalingHandler = signalingHandler;
    }

    @Override
    public synchronized void initiateCall(String targetName) {
        if (callState != CallState.IDLE && callState != CallState.ENDED) {
            return;
        }

        try {
            cleanupMedia();
            this.peerName = (targetName != null && !targetName.isEmpty()) ? targetName : "Contacto";

            VideoSource source = createVideoSource(currentSourceType);
            this.mediaManager = new MediaStreamManager(source, this);
            this.mediaManager.setMicMuted(muted);
            this.mediaManager.setVideoMuted(!videoEnabled);

            int localMediaPort = mediaManager.getLocalPort();
            String myName = (localUser != null) ? localUser.getUsername() : "Usuario";

            if (signalingHandler != null) {
                signalingHandler.sendCallRequest(myName, localMediaPort);
            }

            this.callState = CallState.OUTGOING_CALL;
            notifyStateChanged(callState, peerName, "Llamando a " + peerName + "...");

            // Iniciar vista previa local mientras suena
            this.mediaManager.start();

        } catch (Exception e) {
            this.callState = CallState.ENDED;
            notifyStateChanged(callState, peerName, "Error al iniciar videollamada: " + e.getMessage());
            cleanupMedia();
        }
    }

    @Override
    public synchronized void handleIncomingCall(String callerName, String peerIp, int mediaPort) {
        if (callState == CallState.CONNECTED || callState == CallState.OUTGOING_CALL) {
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
        this.callState = CallState.INCOMING_CALL;

        notifyStateChanged(callState, peerName, "Videollamada entrante de " + peerName);
        for (VideoCallListener l : listeners) {
            l.onIncomingCallReceived(callerName, peerIp, mediaPort);
        }
    }

    @Override
    public synchronized void acceptCall() {
        if (callState != CallState.INCOMING_CALL) return;

        try {
            cleanupMedia();
            VideoSource source = createVideoSource(currentSourceType);
            this.mediaManager = new MediaStreamManager(source, this);
            this.mediaManager.setMicMuted(muted);
            this.mediaManager.setVideoMuted(!videoEnabled);
            this.mediaManager.setRemoteTarget(peerIp, peerMediaPort);

            int localMediaPort = mediaManager.getLocalPort();
            String myName = (localUser != null) ? localUser.getUsername() : "Usuario";

            if (signalingHandler != null) {
                signalingHandler.sendCallAccept(myName, localMediaPort);
            }

            this.mediaManager.start();
            this.callState = CallState.CONNECTED;
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

        this.callState = CallState.CONNECTED;
        notifyStateChanged(callState, peerName, "Conectado con " + peerName);
    }

    @Override
    public synchronized void rejectCall(String reason) {
        if (signalingHandler != null) {
            try {
                signalingHandler.sendCallReject(reason != null ? reason : "Llamada rechazada");
            } catch (IOException ignored) {}
        }
        this.callState = CallState.ENDED;
        notifyStateChanged(callState, peerName, "Llamada rechazada");
        cleanupMedia();
    }

    @Override
    public synchronized void handleCallRejected(String reason) {
        this.callState = CallState.ENDED;
        notifyStateChanged(callState, peerName, "Llamada rechazada: " + (reason != null ? reason : ""));
        cleanupMedia();
    }

    @Override
    public synchronized void endCall() {
        if (callState == CallState.IDLE) return;

        if (signalingHandler != null) {
            try {
                signalingHandler.sendCallEnd("Llamada finalizada por el usuario");
            } catch (IOException ignored) {}
        }

        this.callState = CallState.ENDED;
        notifyStateChanged(callState, peerName, "Videollamada finalizada");
        cleanupMedia();
    }

    @Override
    public synchronized void handleCallEnded(String reason) {
        this.callState = CallState.ENDED;
        notifyStateChanged(callState, peerName, "El otro usuario finalizó la videollamada");
        cleanupMedia();
    }

    private void cleanupMedia() {
        if (mediaManager != null) {
            mediaManager.stop();
            mediaManager = null;
        }
    }

    private VideoSource createVideoSource(VideoSourceType type) {
        String name = (localUser != null) ? localUser.getUsername() : "Yo";
        if (type == VideoSourceType.PHYSICAL_WEBCAM) {
            return new PhysicalWebcamSource(name);
        } else if (type == VideoSourceType.SCREEN_SHARE) {
            return new ScreenShareVideoSource(name);
        }
        return new VirtualCameraVideoSource(name);
    }

    @Override
    public CallState getCallState() {
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
    public synchronized void setVideoSourceType(VideoSourceType type) {
        if (type != null && type != currentSourceType) {
            this.currentSourceType = type;
            if (mediaManager != null) {
                if (type == VideoSourceType.CAMERA_OFF) {
                    mediaManager.setVideoMuted(true);
                } else {
                    mediaManager.setVideoMuted(false);
                    mediaManager.setVideoSource(createVideoSource(type));
                }
            }
        }
    }

    @Override
    public VideoSourceType getVideoSourceType() {
        return currentSourceType;
    }

    @Override
    public void addListener(VideoCallListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(VideoCallListener listener) {
        listeners.remove(listener);
    }

    private void notifyStateChanged(CallState state, String peer, String msg) {
        for (VideoCallListener l : listeners) {
            l.onCallStateChanged(state, peer, msg);
        }
    }

    // MediaFrameConsumer callbacks
    @Override
    public void onLocalFrame(BufferedImage frame) {
        for (VideoCallListener l : listeners) {
            l.onLocalFrameAvailable(frame);
        }
    }

    @Override
    public void onRemoteFrame(BufferedImage frame) {
        for (VideoCallListener l : listeners) {
            l.onRemoteFrameAvailable(frame);
        }
    }

    @Override
    public void onAudioLevels(float localLevel, float remoteLevel) {
        for (VideoCallListener l : listeners) {
            l.onAudioLevelsUpdated(localLevel, remoteLevel);
        }
    }
}

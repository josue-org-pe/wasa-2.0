package com.chatlocal.backend.service.videocall;

import java.awt.image.BufferedImage;

/**
 * Escuchador de eventos para actualización de la interfaz gráfica durante una videollamada.
 */
public interface VideoCallListener {

    /**
     * Notifica un cambio en el estado de la llamada (IDLE, OUTGOING, INCOMING, CONNECTED, ENDED).
     */
    void onCallStateChanged(CallState newState, String peerName, String message);

    /**
     * Notifica la llegada de una solicitud de videollamada desde un par remoto.
     */
    void onIncomingCallReceived(String callerName, String peerIp, int mediaPort);

    /**
     * Nuevo cuadro de video local generado (para renderizado en PiP o miniatura).
     */
    void onLocalFrameAvailable(BufferedImage frame);

    /**
     * Nuevo cuadro de video remoto recibido y decodificado (para renderizado en vista principal).
     */
    void onRemoteFrameAvailable(BufferedImage frame);

    /**
     * Actualización de niveles de audio RMS (0.0 a 1.0) para indicadores visuales de voz.
     */
    void onAudioLevelsUpdated(float localLevel, float remoteLevel);
}

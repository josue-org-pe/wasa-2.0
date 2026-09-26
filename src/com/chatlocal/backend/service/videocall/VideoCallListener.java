package com.chatlocal.backend.service.videocall;

import java.awt.image.BufferedImage;

// interfaz para recibir avisos de cuando cambia la llamada o llega un frame
public interface VideoCallListener {

    // avisa cuando cambia de estado (llamando, conectado, colgado)
    void onCallStateChanged(CallState newState, String peerName, String message);

    // avisa cuando entra una llamada
    void onIncomingCallReceived(String callerName, String peerIp, int mediaPort);

    // nuevo frame de nuestra camara local
    void onLocalFrameAvailable(BufferedImage frame);

    // nuevo frame de la otra persona
    void onRemoteFrameAvailable(BufferedImage frame);

    // nivel del microfono para la animacion
    void onAudioLevelsUpdated(float localLevel, float remoteLevel);
}

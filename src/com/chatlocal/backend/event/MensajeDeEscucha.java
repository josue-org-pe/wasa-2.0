package com.chatlocal.backend.event;

import com.chatlocal.backend.model.MensajeChat;

// avisa cuando llega un mensaje, archivo o audio nuevo
public interface MensajeDeEscucha {
    void onMessageReceived(MensajeChat message);
    void onFileReceived(MensajeChat message);
    default void onAudioReceived(MensajeChat message) {
        onFileReceived(message);
    }
    void onMessageSent(MensajeChat message);
    default void onMessageStatusChanged(String messageId, com.chatlocal.backend.model.EstadoMensaje status) {}
}

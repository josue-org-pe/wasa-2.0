package com.chatlocal.backend.event;

import com.chatlocal.backend.model.ChatMessage;

/**
 * Escuchador para eventos de recepción y emisión de mensajes.
 */
public interface MessageListener {
    void onMessageReceived(ChatMessage message);
    void onFileReceived(ChatMessage message);
    default void onAudioReceived(ChatMessage message) {
        onFileReceived(message);
    }
    void onMessageSent(ChatMessage message);
    default void onMessageStatusChanged(String messageId, com.chatlocal.backend.model.MessageStatus status) {}
}

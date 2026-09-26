package com.chatlocal.backend.event;

import com.chatlocal.backend.model.ChatMessage;

// avisa cuando llega un mensaje, archivo o audio nuevo
public interface MessageListener {
    void onMessageReceived(ChatMessage message);
    void onFileReceived(ChatMessage message);
    default void onAudioReceived(ChatMessage message) {
        onFileReceived(message);
    }
    void onMessageSent(ChatMessage message);
    default void onMessageStatusChanged(String messageId, com.chatlocal.backend.model.MessageStatus status) {}
}

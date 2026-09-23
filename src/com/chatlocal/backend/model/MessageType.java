package com.chatlocal.backend.model;

/**
 * Tipos de mensajes soportados por el protocolo de comunicación.
 */
public enum MessageType {
    TEXT,
    FILE,
    AUDIO,
    STICKER,
    PING,
    PONG,
    SYSTEM
}

package com.chatlocal.backend.model;

// tipo de mensaje que se envia por el socket
public enum MessageType {
    TEXT,
    FILE,
    AUDIO,
    STICKER,
    PING,
    PONG,
    SYSTEM
}

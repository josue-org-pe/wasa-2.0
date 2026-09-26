package com.chatlocal.backend.service.videocall;

// estados de la videollamada
public enum CallState {
    IDLE,
    OUTGOING_CALL,
    INCOMING_CALL,
    CONNECTED,
    ENDED
}

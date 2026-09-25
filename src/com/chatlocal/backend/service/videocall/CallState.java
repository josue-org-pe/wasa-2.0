package com.chatlocal.backend.service.videocall;

/**
 * Estados del ciclo de vida de una videollamada.
 */
public enum CallState {
    IDLE,
    OUTGOING_CALL,
    INCOMING_CALL,
    CONNECTED,
    ENDED
}

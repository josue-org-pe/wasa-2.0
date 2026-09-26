package com.chatlocal.backend.event;

import com.chatlocal.backend.model.ConnectionState;

// avisa cuando cambia la conexion o hay error
public interface ConnectionListener {
    void onConnectionStateChanged(ConnectionState newState, String details);
    void onConnectionError(String errorMessage);
}

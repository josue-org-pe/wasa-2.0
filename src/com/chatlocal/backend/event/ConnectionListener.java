package com.chatlocal.backend.event;

import com.chatlocal.backend.model.ConnectionState;

/**
 * Escuchador para cambios en el estado de la conexión de red.
 */
public interface ConnectionListener {
    void onConnectionStateChanged(ConnectionState newState, String details);
    void onConnectionError(String errorMessage);
}

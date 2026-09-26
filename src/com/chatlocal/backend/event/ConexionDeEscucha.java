package com.chatlocal.backend.event;

import com.chatlocal.backend.model.EstadoConexion;

// avisa cuando cambia la conexion o hay error
public interface ConexionDeEscucha {
    void onConnectionStateChanged(EstadoConexion newState, String details);
    void onConnectionError(String errorMessage);
}

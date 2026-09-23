package com.chatlocal.backend.model;

/**
 * Estados del ciclo de vida de la conexión.
 */
public enum ConnectionState {
    DISCONNECTED("Desconectado"),
    LISTENING("Esperando conexión entrante..."),
    CONNECTING("Conectando con el anfitrión..."),
    VERIFYING("Verificando protocolo de comunicación..."),
    CONNECTED("Conectado"),
    ERROR("Error de conexión");

    private final String etiqueta;

    ConnectionState(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}

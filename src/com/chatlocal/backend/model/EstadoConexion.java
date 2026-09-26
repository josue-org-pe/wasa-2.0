package com.chatlocal.backend.model;

// estados por los que pasa la conexion del socket
public enum EstadoConexion {
    DISCONNECTED("Desconectado"),
    LISTENING("Esperando conexión entrante..."),
    CONNECTING("Conectando con el anfitrión..."),
    VERIFYING("Verificando protocolo de comunicación..."),
    CONNECTED("Conectado"),
    ERROR("Error de conexión");

    private final String etiqueta;

    EstadoConexion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}

package com.chatlocal.backend.model;

/**
 * Rol que asume la aplicación en la red de pares.
 */
public enum ConnectionRole {
    HOST("Anfitrión (Esperar conexión)"),
    CLIENT("Cliente (Conectarse a anfitrión)");

    private final String descripcion;

    ConnectionRole(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

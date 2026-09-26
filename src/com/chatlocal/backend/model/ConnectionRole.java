package com.chatlocal.backend.model;

// indica si somos anfitrion (servidor) o cliente
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

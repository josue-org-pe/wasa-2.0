package com.chatlocal.backend.model;

// indica si somos anfitrion (servidor) o cliente
public enum RolConexion {
    HOST("Anfitrión (Esperar conexión)"),
    CLIENT("Cliente (Conectarse a anfitrión)");

    private final String descripcion;

    RolConexion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}

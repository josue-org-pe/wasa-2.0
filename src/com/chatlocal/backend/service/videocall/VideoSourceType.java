package com.chatlocal.backend.service.videocall;

/**
 * Fuentes de video seleccionables durante una videollamada.
 */
public enum VideoSourceType {
    PHYSICAL_WEBCAM("Cámara Web Integrada"),
    VIRTUAL_CAMERA("Cámara Virtual con Avatar"),
    SCREEN_SHARE("Compartir Pantalla Completa"),
    CAMERA_OFF("Cámara Apagada");

    private final String displayName;

    VideoSourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

package com.chatlocal.backend.service.videocall;

// tipos de camara que se pueden seleccionar
public enum TipoFuenteVideo {
    PHYSICAL_WEBCAM("Cámara Web Integrada"),
    VIRTUAL_CAMERA("Cámara Virtual con Avatar"),
    SCREEN_SHARE("Compartir Pantalla Completa"),
    CAMERA_OFF("Cámara Apagada");

    private final String displayName;

    TipoFuenteVideo(String displayName) {
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

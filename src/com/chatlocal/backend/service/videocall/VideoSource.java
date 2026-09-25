package com.chatlocal.backend.service.videocall;

import java.awt.image.BufferedImage;

/**
 * Interfaz genérica para generadores de cuadros de video (cámara virtual, pantalla, etc.).
 */
public interface VideoSource {

    /**
     * Genera el siguiente cuadro de video a ser transmitido.
     */
    BufferedImage captureFrame();

    /**
     * Informa el nivel de audio actual (0.0 a 1.0) para efectos reactivos visuales.
     */
    void setAudioLevel(float level);

    /**
     * Asigna el nombre de usuario local para rotulación en video.
     */
    void setUserName(String name);

    /**
     * Libera cualquier recurso asociado a la fuente de video.
     */
    void close();
}

package com.chatlocal.backend.service.videocall;

import java.awt.image.BufferedImage;

// interfaz base para capturar video (webcam, virtual o pantalla)
public interface VideoSource {

    // toma la captura de la imagen actual
    BufferedImage captureFrame();

    // pasa el volumen del audio para animar las barras
    void setAudioLevel(float level);

    // nombre para pintar abajo
    void setUserName(String name);

    // libera la camara o recursos
    void close();
}

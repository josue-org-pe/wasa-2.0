package com.chatlocal.backend.model;

// para saber si el mensaje se envio, se entrego o ya lo vieron (checks)
public enum MessageStatus {
    SENDING("Enviando..."),
    SENT("Enviado al socket"),      // Single check ✓
    DELIVERED("Entregado"),          // Double check ✓✓
    READ("Visto");                   // Double check ✓✓ azul/verde

    private final String description;

    MessageStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

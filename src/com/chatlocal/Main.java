package com.chatlocal;

import com.chatlocal.ui.ChatWindow;

import javax.swing.*;

/**
 * Punto de entrada oficial de ChatLocal.
 * Inicializa propiedades globales de renderizado gráfico de alta calidad
 * y despliega la ventana principal en el hilo de eventos (EDT).
 */
public class Main {

    public static void main(String[] args) {
        // Habilitar anti-aliasing en fuentes de Swing en todo el JVM
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ChatWindow window = new ChatWindow();
            window.setVisible(true);
        });
    }
}

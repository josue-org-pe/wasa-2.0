package com.chatlocal;

import com.chatlocal.ui.ChatWindow;

import javax.swing.*;

// punto de entrada principal de wasa 3.0
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

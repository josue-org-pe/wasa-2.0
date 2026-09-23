package com.chatlocal.ui;

import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.ChatRoom;
import com.chatlocal.backend.model.ConnectionRole;
import com.chatlocal.backend.model.UserProfile;
import com.chatlocal.backend.service.ChatService;
import com.chatlocal.backend.service.ChatServiceImpl;
import com.chatlocal.ui.panels.MainMessengerPanel;
import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;

/**
 * Lanzador de vista previa de alta fidelidad sin necesidad de sockets.
 * Muestra el diseño completo con barra lateral (salas y contactos),
 * notas de voz interactivas, stickers, selector de temas y checks de lectura.
 */
public class UiPreview {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pulse LAN Messenger — Vista Previa de Diseño");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(880, 640);
            frame.setMinimumSize(new Dimension(720, 540));
            frame.setLocationRelativeTo(null);
            frame.getContentPane().setBackground(ThemeManager.getTheme().bgDark);

            ChatService mockService = new ChatServiceImpl();
            mockService.setLocalUserProfile(new UserProfile("Skipper", ConnectionRole.HOST, "192.168.1.15", 5000));

            // Agregar salas de demostración
            mockService.createRoom("Proyecto Final POO", "Entrega viernes 23:59 hrs", "", false);
            mockService.createRoom("Gaming & Chill 🎮", "Charla libre y videojuegos", "", false);
            mockService.createRoom("📹 Reunión Sprint 3", "Revisión de arquitectura modular", "1234", true);

            MainMessengerPanel panel = new MainMessengerPanel(frame, mockService, () -> {
                JOptionPane.showMessageDialog(frame, "Acción de salir pulsada (Modo Preview)");
            });

            // Poblar mensajes de ejemplo de diferentes tipos
            panel.addMessage(ChatMessage.createSystemMessage("¡Bienvenido a la sala! Conexión encriptada localmente."));
            panel.addMessage(ChatMessage.createTextMessage("María González", 0x3B82F6, "¡Hola equipo! ¿Cómo van los avances del proyecto de POO?", false, "general"));
            panel.addMessage(ChatMessage.createTextMessage("Yo", 0x8B5CF6, "¡Todo excelente! Acabamos de implementar las notas de voz y los temas oscuros.", true, "general"));
            panel.addMessage(ChatMessage.createStickerMessage("Carlos Ruiz", 0x10B981, "¡Aprobado con 20! 💯", false, "general"));
            panel.addMessage(ChatMessage.createAudioMessage("María González", 0x3B82F6, null, 14, false, "general"));
            panel.addMessage(ChatMessage.createFileMessage("Yo", 0x8B5CF6, "Diagrama_Clases_UML.pdf", 2450000, null, true, "general"));

            frame.add(panel);
            frame.setVisible(true);

            ThemeManager.addThemeListener(() -> {
                frame.getContentPane().setBackground(ThemeManager.getTheme().bgDark);
                frame.revalidate();
                frame.repaint();
            });
        });
    }
}

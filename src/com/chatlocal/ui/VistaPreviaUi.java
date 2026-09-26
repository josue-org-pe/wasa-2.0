package com.chatlocal.ui;

import com.chatlocal.backend.model.MensajeChat;
import com.chatlocal.backend.model.SalaChat;
import com.chatlocal.backend.model.RolConexion;
import com.chatlocal.backend.model.UsuarioPerfil;
import com.chatlocal.backend.service.ServicioChat;
import com.chatlocal.backend.service.ServicioChatImpl;
import com.chatlocal.ui.panels.PanelMensajeroPrincipal;
import com.chatlocal.ui.theme.GestorTema;

import javax.swing.*;
import java.awt.*;

// clase para probar la interfaz visual rapido sin conectar la red
public class VistaPreviaUi {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("wasa 3.0 — Vista Previa");
            com.chatlocal.ui.theme.Tema.applyAppIcon(frame);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(880, 640);
            frame.setMinimumSize(new Dimension(720, 540));
            frame.setLocationRelativeTo(null);
            frame.getContentPane().setBackground(GestorTema.getTheme().bgDark);

            ServicioChat mockService = new ServicioChatImpl();
            mockService.setLocalUserProfile(new UsuarioPerfil("Skipper", RolConexion.HOST, "192.168.1.15", 5000));

            // Agregar salas de demostración
            mockService.createRoom("Proyecto Final POO", "Entrega viernes 23:59 hrs", "", false);
            mockService.createRoom("Gaming & Chill 🎮", "Charla libre y videojuegos", "", false);
            mockService.createRoom("📹 Reunión Sprint 3", "Revisión de arquitectura modular", "1234", true);

            PanelMensajeroPrincipal panel = new PanelMensajeroPrincipal(frame, mockService, () -> {
                JOptionPane.showMessageDialog(frame, "Acción de salir pulsada (Modo Preview)");
            });

            // Poblar mensajes de ejemplo de diferentes tipos
            panel.addMessage(MensajeChat.createSystemMessage("¡Bienvenido a la sala! Conexión encriptada localmente."));
            panel.addMessage(MensajeChat.createTextMessage("María González", 0x3B82F6, "¡Hola equipo! ¿Cómo van los avances del proyecto de POO?", false, "general"));
            panel.addMessage(MensajeChat.createTextMessage("Yo", 0x8B5CF6, "¡Todo excelente! Acabamos de implementar las notas de voz y los temas oscuros.", true, "general"));
            panel.addMessage(MensajeChat.createStickerMessage("Carlos Ruiz", 0x10B981, "¡Aprobado con 20! 💯", false, "general"));
            panel.addMessage(MensajeChat.createAudioMessage("María González", 0x3B82F6, null, 14, false, "general"));
            panel.addMessage(MensajeChat.createFileMessage("Yo", 0x8B5CF6, "Diagrama_Clases_UML.pdf", 2450000, null, true, "general"));

            frame.add(panel);
            frame.setVisible(true);

            GestorTema.addThemeListener(() -> {
                frame.getContentPane().setBackground(GestorTema.getTheme().bgDark);
                frame.revalidate();
                frame.repaint();
            });
        });
    }
}

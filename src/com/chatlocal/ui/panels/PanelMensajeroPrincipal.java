package com.chatlocal.ui.panels;

import com.chatlocal.backend.model.MensajeChat;
import com.chatlocal.backend.model.SalaChat;
import com.chatlocal.backend.model.UsuarioPerfil;
import com.chatlocal.backend.service.ServicioChat;
import com.chatlocal.ui.theme.GestorTema;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

// contenedor principal que une la barra lateral y el area de chat
public class PanelMensajeroPrincipal extends JPanel {

    public interface MessengerActionCallback {
        void onDisconnectRequested();
    }

    private final ServicioChat chatService;
    private final MessengerActionCallback disconnectCallback;

    private PanelBarraLateral sidebarPanel;
    private PanelAreaChat chatAreaPanel;

    public PanelMensajeroPrincipal(Frame parentFrame, ServicioChat chatService, MessengerActionCallback disconnectCallback) {
        this.chatService = chatService;
        this.disconnectCallback = disconnectCallback;

        setOpaque(true);
        setBackground(GestorTema.getTheme().bgDark);
        setLayout(new BorderLayout());

        buildUI(parentFrame);
    }

    private void buildUI(Frame parentFrame) {
        // Área de chat
        chatAreaPanel = new PanelAreaChat(chatService.getAudioRecorder(), new PanelAreaChat.ChatAreaCallback() {
            @Override
            public void onSendMessage(String text) {
                try {
                    chatService.sendTextMessage(text);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(PanelMensajeroPrincipal.this, "Error al enviar mensaje: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void onSendFile(File file) {
                new Thread(() -> {
                    try {
                        chatService.sendFile(file);
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(PanelMensajeroPrincipal.this, "Error transfiriendo archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                        );
                    }
                }, "file-upload-worker").start();
            }

            @Override
            public void onSendAudio(File audioFile, int durationSecs) {
                new Thread(() -> {
                    try {
                        chatService.sendAudioMessage(audioFile, durationSecs);
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(PanelMensajeroPrincipal.this, "Error enviando nota de voz: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                        );
                    }
                }, "audio-send-worker").start();
            }

            @Override
            public void onSendSticker(String sticker) {
                try {
                    chatService.sendStickerMessage(sticker);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(PanelMensajeroPrincipal.this, "Error al enviar sticker: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void onStartVideoCall() {
                chatService.initiateVideoCall();
            }

            @Override
            public void onDisconnectRequested() {
                if (disconnectCallback != null) {
                    disconnectCallback.onDisconnectRequested();
                }
            }

            @Override
            public void onToggleSidebar() {
                sidebarPanel.setVisible(!sidebarPanel.isVisible());
                revalidate();
                repaint();
            }
        });

        // Barra lateral
        sidebarPanel = new PanelBarraLateral(parentFrame, chatService, new PanelBarraLateral.SidebarCallback() {
            @Override
            public void onRoomSelected(SalaChat room) {
                chatAreaPanel.setRoomInfo(room);
                chatAreaPanel.clearMessages();
                java.util.List<MensajeChat> history = chatService.getConversationMessages("room_" + room.getId());
                for (MensajeChat m : history) {
                    chatAreaPanel.addMessage(m);
                }
            }

            @Override
            public void onUserSelected(UsuarioPerfil user) {
                chatAreaPanel.setDirectUserInfo(user);
                chatAreaPanel.clearMessages();
                java.util.List<MensajeChat> history = chatService.getConversationMessages("private_" + user.getUsername());
                for (MensajeChat m : history) {
                    chatAreaPanel.addMessage(m);
                }
            }

            @Override
            public void onSettingsChanged() {
                setBackground(GestorTema.getTheme().bgDark);
                if (chatService.getChatWallpaperPath() != null) {
                    chatAreaPanel.setBackgroundImagePath(chatService.getChatWallpaperPath());
                }
                revalidate();
                repaint();
            }
        });

        add(sidebarPanel, BorderLayout.WEST);
        add(chatAreaPanel, BorderLayout.CENTER);
    }

    public void addMessage(MensajeChat message) {
        chatAreaPanel.addMessage(message);
    }

    public void updateMessageStatus(String messageId, com.chatlocal.backend.model.EstadoMensaje status) {
        chatAreaPanel.updateMessageStatus(messageId, status);
    }

    public void clearMessages() {
        chatAreaPanel.clearMessages();
    }

    public void refreshSidebar() {
        sidebarPanel.refresh();
        sidebarPanel.updateProfileLabels();
    }
}

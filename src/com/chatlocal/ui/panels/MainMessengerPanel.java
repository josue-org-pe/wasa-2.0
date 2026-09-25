package com.chatlocal.ui.panels;

import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.ChatRoom;
import com.chatlocal.backend.model.UserProfile;
import com.chatlocal.backend.service.ChatService;
import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Contenedor maestro del mensajero activo:
 * - Panel lateral izquierdo (Sidebar con salas y contactos).
 * - Área central de conversación (ChatAreaPanel con feed, notas de voz y emojis).
 */
public class MainMessengerPanel extends JPanel {

    public interface MessengerActionCallback {
        void onDisconnectRequested();
    }

    private final ChatService chatService;
    private final MessengerActionCallback disconnectCallback;

    private SidebarPanel sidebarPanel;
    private ChatAreaPanel chatAreaPanel;

    public MainMessengerPanel(Frame parentFrame, ChatService chatService, MessengerActionCallback disconnectCallback) {
        this.chatService = chatService;
        this.disconnectCallback = disconnectCallback;

        setOpaque(true);
        setBackground(ThemeManager.getTheme().bgDark);
        setLayout(new BorderLayout());

        buildUI(parentFrame);
    }

    private void buildUI(Frame parentFrame) {
        // Área de chat
        chatAreaPanel = new ChatAreaPanel(chatService.getAudioRecorder(), new ChatAreaPanel.ChatAreaCallback() {
            @Override
            public void onSendMessage(String text) {
                try {
                    chatService.sendTextMessage(text);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(MainMessengerPanel.this, "Error al enviar mensaje: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void onSendFile(File file) {
                new Thread(() -> {
                    try {
                        chatService.sendFile(file);
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(MainMessengerPanel.this, "Error transfiriendo archivo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
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
                                JOptionPane.showMessageDialog(MainMessengerPanel.this, "Error enviando nota de voz: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                        );
                    }
                }, "audio-send-worker").start();
            }

            @Override
            public void onSendSticker(String sticker) {
                try {
                    chatService.sendStickerMessage(sticker);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(MainMessengerPanel.this, "Error al enviar sticker: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        sidebarPanel = new SidebarPanel(parentFrame, chatService, new SidebarPanel.SidebarCallback() {
            @Override
            public void onRoomSelected(ChatRoom room) {
                chatAreaPanel.setRoomInfo(room);
                chatAreaPanel.clearMessages();
                java.util.List<ChatMessage> history = chatService.getConversationMessages("room_" + room.getId());
                for (ChatMessage m : history) {
                    chatAreaPanel.addMessage(m);
                }
            }

            @Override
            public void onUserSelected(UserProfile user) {
                chatAreaPanel.setDirectUserInfo(user);
                chatAreaPanel.clearMessages();
                java.util.List<ChatMessage> history = chatService.getConversationMessages("private_" + user.getUsername());
                for (ChatMessage m : history) {
                    chatAreaPanel.addMessage(m);
                }
            }

            @Override
            public void onSettingsChanged() {
                setBackground(ThemeManager.getTheme().bgDark);
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

    public void addMessage(ChatMessage message) {
        chatAreaPanel.addMessage(message);
    }

    public void updateMessageStatus(String messageId, com.chatlocal.backend.model.MessageStatus status) {
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

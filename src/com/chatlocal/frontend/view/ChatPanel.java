package com.chatlocal.frontend.view;

import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.ConnectionState;
import com.chatlocal.frontend.components.ChatBubblePanel;
import com.chatlocal.frontend.components.ModernButton;
import com.chatlocal.frontend.components.ModernTextField;
import com.chatlocal.frontend.components.StatusBadge;
import com.chatlocal.frontend.theme.ThemeColors;
import com.chatlocal.frontend.theme.ThemeFonts;
import com.chatlocal.frontend.theme.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/**
 * Pantalla principal de conversación en tiempo real con cabecera de estado,
 * historial de mensajes con burbujas y barra inferior de envío de texto y archivos.
 */
public class ChatPanel extends JPanel {

    public interface ChatActionListener {
        void onSendMessage(String text);
        void onSendFile(File file);
        void onDisconnectRequested();
    }

    private ChatActionListener listener;

    private final JLabel peerTitleLabel = new JLabel("Contacto Remoto");
    private final StatusBadge statusBadge = new StatusBadge();
    private final JPanel messagesContainer = new JPanel();
    private final JScrollPane scrollPane;
    private final ModernTextField messageInput = new ModernTextField();
    private final ModernButton sendButton = new ModernButton("Enviar", ModernButton.Type.PRIMARY);
    private final ModernButton attachButton = new ModernButton("📎 Adjuntar", ModernButton.Type.SECONDARY);
    private final ModernButton disconnectButton = new ModernButton("Desconectar", ModernButton.Type.DANGER);

    public ChatPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeColors.BG_DARK);

        // Contenedor vertical de burbujas
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setOpaque(false);

        // Panel envoltorio para alinear al fondo
        JPanel messagesWrapper = new JPanel(new BorderLayout());
        messagesWrapper.setOpaque(false);
        messagesWrapper.add(messagesContainer, BorderLayout.NORTH);

        scrollPane = new JScrollPane(messagesWrapper);
        UIUtils.customizeScrollBar(scrollPane);

        buildHeader();
        add(scrollPane, BorderLayout.CENTER);
        buildInputBar();
    }

    public void setChatActionListener(ChatActionListener listener) {
        this.listener = listener;
    }

    private void buildHeader() {
        JPanel header = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UIUtils.applyQualityRendering(g2);
                g2.setColor(ThemeColors.BG_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ThemeColors.BORDER);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        // Info izquierda: Avatar + Título + Badge
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        // Avatar estilizado
        JLabel avatarLabel = new JLabel("💬");
        avatarLabel.setFont(ThemeFonts.title(20));

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        peerTitleLabel.setFont(ThemeFonts.bold(14));
        peerTitleLabel.setForeground(ThemeColors.TEXT_PRIMARY);
        textGroup.add(peerTitleLabel);

        statusBadge.setState(ConnectionState.CONNECTED, "En línea");

        leftPanel.add(avatarLabel);
        leftPanel.add(textGroup);
        leftPanel.add(statusBadge);

        header.add(leftPanel, BorderLayout.WEST);

        // Botón desconectar derecha
        disconnectButton.setFont(ThemeFonts.bold(11));
        disconnectButton.setMargin(new Insets(6, 12, 6, 12));
        disconnectButton.addActionListener(e -> {
            int resp = JOptionPane.showConfirmDialog(this, "¿Deseas finalizar la sesión de chat?",
                    "Confirmar Desconexión", JOptionPane.YES_NO_OPTION);
            if (resp == JOptionPane.YES_OPTION && listener != null) {
                listener.onDisconnectRequested();
            }
        });

        header.add(disconnectButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void buildInputBar() {
        JPanel inputBar = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UIUtils.applyQualityRendering(g2);
                g2.setColor(ThemeColors.BG_SURFACE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ThemeColors.BORDER);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        inputBar.setOpaque(false);
        inputBar.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Botón Adjuntar
        attachButton.setMargin(new Insets(8, 12, 8, 12));
        attachButton.addActionListener(e -> handleAttachFile());
        inputBar.add(attachButton, BorderLayout.WEST);

        // Campo de texto
        messageInput.setPlaceholder("Escribe un mensaje aquí... (Presiona Enter para enviar)");
        messageInput.addActionListener(e -> handleSendMessage());
        inputBar.add(messageInput, BorderLayout.CENTER);

        // Botón Enviar
        sendButton.setPreferredSize(new Dimension(95, 38));
        sendButton.addActionListener(e -> handleSendMessage());
        inputBar.add(sendButton, BorderLayout.EAST);

        add(inputBar, BorderLayout.SOUTH);
    }

    public void setConnectionInfo(String peerAddress, int port) {
        peerTitleLabel.setText("Contacto (" + peerAddress + ":" + port + ")");
        statusBadge.setState(ConnectionState.CONNECTED, "En línea");
    }

    public void setConnectionStatus(ConnectionState state, String text) {
        statusBadge.setState(state, text);
    }

    public void addMessage(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            ChatBubblePanel bubble = new ChatBubblePanel(message);
            messagesContainer.add(bubble);
            messagesContainer.revalidate();
            messagesContainer.repaint();

            // Auto-scroll al fondo
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    public void clearMessages() {
        SwingUtilities.invokeLater(() -> {
            messagesContainer.removeAll();
            messagesContainer.revalidate();
            messagesContainer.repaint();
        });
    }

    private void handleSendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty() || listener == null) return;
        messageInput.setText("");
        listener.onSendMessage(text);
        messageInput.requestFocusInWindow();
    }

    private void handleAttachFile() {
        if (listener == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecciona un archivo para enviar");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            listener.onSendFile(selectedFile);
        }
    }
}

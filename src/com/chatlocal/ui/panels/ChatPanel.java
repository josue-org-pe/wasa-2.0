package com.chatlocal.ui.panels;

import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.ConnectionState;
import com.chatlocal.backend.service.FileTransferManager;
import com.chatlocal.ui.components.*;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.File;

// panel donde se muestran los mensajes y la entrada de texto
public class ChatPanel extends JPanel {

    public interface ChatPanelCallback {
        void onSendMessage(String text);
        void onSendFile(File file);
        void onDisconnectRequested();
    }

    private final ChatPanelCallback callback;

    private JLabel lblPeerName;
    private JLabel lblPeerEndpoint;
    private StatusBadge statusBadge;

    private JPanel messageListPanel;
    private JScrollPane scrollPane;
    private ModernTextField txtInput;
    private ModernButton btnSend;
    private ModernButton btnAttach;

    public ChatPanel(ChatPanelCallback callback) {
        this.callback = callback;

        setOpaque(true);
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        buildUI();
    }

    private void buildUI() {
        // 1. Cabecera superior
        add(createHeaderBar(), BorderLayout.NORTH);

        // 2. Feed central de mensajes
        add(createMessageFeed(), BorderLayout.CENTER);

        // 3. Barra inferior de redacción
        add(createInputBar(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(true);
        bar.setBackground(Theme.BG_PANEL);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Lado izquierdo: Avatar y nombres
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel avatar = new JLabel(Icons.avatar("Contacto", 38, Theme.PRIMARY, Theme.TEXT_PRIMARY));
        left.add(avatar);

        JPanel details = new JPanel(new GridLayout(2, 1, 0, 2));
        details.setOpaque(false);

        lblPeerName = new JLabel("Contacto");
        lblPeerName.setFont(Theme.FONT_TITLE);
        lblPeerName.setForeground(Theme.TEXT_PRIMARY);

        lblPeerEndpoint = new JLabel("Conectado localmente");
        lblPeerEndpoint.setFont(Theme.FONT_CAPTION);
        lblPeerEndpoint.setForeground(Theme.TEXT_MUTED);

        details.add(lblPeerName);
        details.add(lblPeerEndpoint);
        left.add(details);

        // Lado derecho: Status badge y botones de acción
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        statusBadge = new StatusBadge();
        statusBadge.setState(ConnectionState.CONNECTED);
        right.add(statusBadge);

        ModernButton btnFolder = new ModernButton("Carpeta", Icons.folder(15, Theme.TEXT_PRIMARY), ModernButton.Variant.SECONDARY);
        btnFolder.setMargin(new Insets(6, 12, 6, 12));
        btnFolder.setFont(Theme.FONT_CAPTION);
        btnFolder.setToolTipText("Abrir carpeta de archivos recibidos");
        btnFolder.addActionListener(e -> FileTransferManager.openReceivedFolder());
        right.add(btnFolder);

        ModernButton btnDisconnect = new ModernButton("Salir", Icons.power(15, Theme.TEXT_PRIMARY), ModernButton.Variant.DANGER);
        btnDisconnect.setMargin(new Insets(6, 12, 6, 12));
        btnDisconnect.setFont(Theme.FONT_CAPTION);
        btnDisconnect.setToolTipText("Cerrar conexión y volver al inicio");
        btnDisconnect.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "¿Estás seguro de que deseas cerrar la sesión de chat?",
                    "Confirmar desconexión",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION && callback != null) {
                callback.onDisconnectRequested();
            }
        });
        right.add(btnDisconnect);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane createMessageFeed() {
        messageListPanel = new JPanel();
        messageListPanel.setOpaque(true);
        messageListPanel.setBackground(Theme.BG_DARK);
        messageListPanel.setLayout(new BoxLayout(messageListPanel, BoxLayout.Y_AXIS));
        messageListPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        scrollPane = new JScrollPane(messageListPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        return scrollPane;
    }

    private JPanel createInputBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BORDER_SUBTLE);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setOpaque(true);
        bar.setBackground(Theme.BG_PANEL);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 18, 14, 18));

        // Botón para adjuntar archivo
        btnAttach = new ModernButton("", Icons.attach(18, Theme.TEXT_PRIMARY), ModernButton.Variant.SECONDARY);
        btnAttach.setPreferredSize(new Dimension(42, 42));
        btnAttach.setToolTipText("Adjuntar y enviar archivo");
        btnAttach.addActionListener(e -> selectAndSendFile());

        // Campo de entrada
        txtInput = new ModernTextField("Escribe un mensaje aquí... (Enter para enviar)", 20);
        txtInput.addActionListener(e -> submitTextMessage());

        // Botón enviar
        btnSend = new ModernButton("", Icons.send(16, Theme.TEXT_PRIMARY), ModernButton.Variant.PRIMARY);
        btnSend.setPreferredSize(new Dimension(44, 42));
        btnSend.setToolTipText("Enviar mensaje");
        btnSend.addActionListener(e -> submitTextMessage());

        bar.add(btnAttach, BorderLayout.WEST);
        bar.add(txtInput, BorderLayout.CENTER);
        bar.add(btnSend, BorderLayout.EAST);

        return bar;
    }

    public void setPeerInfo(String peerName, String endpointInfo) {
        if (peerName != null) lblPeerName.setText(peerName);
        if (endpointInfo != null) lblPeerEndpoint.setText(endpointInfo);
    }

    public void updateConnectionState(ConnectionState state) {
        statusBadge.setState(state);
    }

    public void addMessage(ChatMessage message) {
        ChatBubblePanel bubble = new ChatBubblePanel(message);
        messageListPanel.add(bubble);
        messageListPanel.add(Box.createVerticalStrut(4));
        messageListPanel.revalidate();
        messageListPanel.repaint();

        // Desplazar suavemente hasta el último mensaje
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public void clearMessages() {
        messageListPanel.removeAll();
        messageListPanel.revalidate();
        messageListPanel.repaint();
    }

    private void submitTextMessage() {
        String text = txtInput.getText().trim();
        if (text.isEmpty()) return;

        txtInput.setText("");
        if (callback != null) {
            callback.onSendMessage(text);
        }
        txtInput.requestFocusInWindow();
    }

    private void selectAndSendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecciona un archivo para transferir");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile != null && selectedFile.exists() && selectedFile.isFile()) {
                if (callback != null) {
                    callback.onSendFile(selectedFile);
                }
            }
        }
    }
}

package com.chatlocal.ui.panels;

import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.ChatRoom;
import com.chatlocal.backend.model.ConnectionState;
import com.chatlocal.backend.service.AudioRecorderService;
import com.chatlocal.backend.service.FileTransferManager;
import com.chatlocal.ui.components.*;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Área principal de conversación y chat:
 * - Cabecera con título de sala, badge de latencia y estado.
 * - Feed de mensajes con burbujas, nombres con colores, notas de voz y checks.
 * - Barra inferior con adjuntos, selector de emojis/stickers, notas de voz y botón enviar.
 */
public class ChatAreaPanel extends JPanel {

    public interface ChatAreaCallback {
        void onSendMessage(String text);
        void onSendFile(File file);
        void onSendAudio(File audioFile, int durationSecs);
        void onSendSticker(String sticker);
        void onDisconnectRequested();
    }

    private final AudioRecorderService audioService;
    private final ChatAreaCallback callback;

    private JLabel lblChatTitle;
    private JLabel lblChatSubtitle;
    private StatusBadge statusBadge;
    private JButton btnMeeting;

    private JPanel messageListPanel;
    private JScrollPane scrollPane;

    private JPanel inputContainer;
    private JPanel standardInputBar;
    private AudioRecorderBar recorderBar;
    private ModernTextField txtInput;
    private JButton btnEmoji;

    public ChatAreaPanel(AudioRecorderService audioService, ChatAreaCallback callback) {
        this.audioService = audioService;
        this.callback = callback;

        setOpaque(true);
        setBackground(ThemeManager.getTheme().bgDark);
        setLayout(new BorderLayout());

        buildUI();
    }

    private void buildUI() {
        add(createHeaderBar(), BorderLayout.NORTH);
        add(createMessageFeed(), BorderLayout.CENTER);
        add(createInputSection(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(ThemeManager.getTheme().borderSubtle);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(true);
        bar.setBackground(ThemeManager.getTheme().bgSidebar);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JLabel avatar = new JLabel(Icons.group(24, ThemeManager.getTheme().primary));
        left.add(avatar);

        JPanel details = new JPanel(new GridLayout(2, 1, 0, 2));
        details.setOpaque(false);

        lblChatTitle = new JLabel("Sala General");
        lblChatTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblChatTitle.setForeground(ThemeManager.getTheme().textPrimary);

        lblChatSubtitle = new JLabel("Canal principal de comunicación local");
        lblChatSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblChatSubtitle.setForeground(ThemeManager.getTheme().textMuted);

        details.add(lblChatTitle);
        details.add(lblChatSubtitle);
        left.add(details);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        // Badge de reunión activa si aplica
        btnMeeting = new JButton("📹 Reunión Activa");
        btnMeeting.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnMeeting.setForeground(Color.WHITE);
        btnMeeting.setBackground(ThemeManager.getTheme().danger);
        btnMeeting.setFocusPainted(false);
        btnMeeting.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        btnMeeting.setVisible(false);
        right.add(btnMeeting);

        statusBadge = new StatusBadge();
        statusBadge.setState(ConnectionState.CONNECTED);
        right.add(statusBadge);

        ModernButton btnFolder = new ModernButton("Carpeta", Icons.folder(14, Color.WHITE), ModernButton.Variant.GHOST);
        btnFolder.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnFolder.setMargin(new Insets(5, 10, 5, 10));
        btnFolder.setToolTipText("Abrir descargas y audios");
        btnFolder.addActionListener(e -> FileTransferManager.openReceivedFolder());
        right.add(btnFolder);

        ModernButton btnExit = new ModernButton("Salir", Icons.power(14, Color.WHITE), ModernButton.Variant.DANGER);
        btnExit.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnExit.setMargin(new Insets(5, 10, 5, 10));
        btnExit.setToolTipText("Cerrar sesión");
        btnExit.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "¿Deseas salir del chat?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION && callback != null) {
                callback.onDisconnectRequested();
            }
        });
        right.add(btnExit);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JScrollPane createMessageFeed() {
        messageListPanel = new JPanel();
        messageListPanel.setOpaque(true);
        messageListPanel.setBackground(ThemeManager.getTheme().bgDark);
        messageListPanel.setLayout(new BoxLayout(messageListPanel, BoxLayout.Y_AXIS));
        messageListPanel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

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

    private JPanel createInputSection() {
        inputContainer = new JPanel(new CardLayout());
        inputContainer.setOpaque(false);

        // 1. Barra estándar de redacción
        standardInputBar = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(ThemeManager.getTheme().borderSubtle);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        standardInputBar.setOpaque(true);
        standardInputBar.setBackground(ThemeManager.getTheme().bgSidebar);
        standardInputBar.setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));

        // Lado izquierdo: Botón de Adjunto (+) y Emojis (😊)
        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftActions.setOpaque(false);

        ModernButton btnAttach = new ModernButton("", Icons.attach(16, Color.WHITE), ModernButton.Variant.GHOST);
        btnAttach.setPreferredSize(new Dimension(38, 38));
        btnAttach.setToolTipText("Adjuntar archivo");
        btnAttach.addActionListener(e -> selectFile());

        btnEmoji = new JButton(Icons.emoji(18, ThemeManager.getTheme().textSecondary));
        btnEmoji.setPreferredSize(new Dimension(38, 38));
        btnEmoji.setContentAreaFilled(false);
        btnEmoji.setBorderPainted(false);
        btnEmoji.setFocusPainted(false);
        btnEmoji.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEmoji.setToolTipText("Insertar Emoji o Sticker");
        btnEmoji.addActionListener(e -> showEmojiPicker());

        leftActions.add(btnAttach);
        leftActions.add(btnEmoji);

        // Campo de texto
        txtInput = new ModernTextField("Escribe un mensaje aquí... (Enter para enviar)", 20);
        txtInput.addActionListener(e -> submitTextMessage());

        // Lado derecho: Botón Micrófono (🎙️) y Enviar (➤)
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightActions.setOpaque(false);

        ModernButton btnMic = new ModernButton("", Icons.mic(18, Color.WHITE), ModernButton.Variant.GHOST);
        btnMic.setPreferredSize(new Dimension(38, 38));
        btnMic.setToolTipText("Grabar Nota de Voz");
        btnMic.addActionListener(e -> startAudioRecording());

        ModernButton btnSend = new ModernButton("", Icons.send(16, Color.WHITE), ModernButton.Variant.PRIMARY);
        btnSend.setPreferredSize(new Dimension(42, 38));
        btnSend.setToolTipText("Enviar");
        btnSend.addActionListener(e -> submitTextMessage());

        rightActions.add(btnMic);
        rightActions.add(btnSend);

        standardInputBar.add(leftActions, BorderLayout.WEST);
        standardInputBar.add(txtInput, BorderLayout.CENTER);
        standardInputBar.add(rightActions, BorderLayout.EAST);

        // 2. Barra de grabación de audio
        recorderBar = new AudioRecorderBar(audioService, new AudioRecorderBar.AudioRecordCallback() {
            @Override
            public void onAudioReady(File audioFile, int durationSeconds) {
                switchInputView(false);
                if (callback != null) callback.onSendAudio(audioFile, durationSeconds);
            }

            @Override
            public void onAudioCancelled() {
                switchInputView(false);
            }
        });
        recorderBar.setVisible(false);

        inputContainer.add(standardInputBar, "standard");
        inputContainer.add(recorderBar, "recording");

        return inputContainer;
    }

    private void startAudioRecording() {
        switchInputView(true);
        recorderBar.startRecording();
    }

    private void switchInputView(boolean isRecording) {
        CardLayout cl = (CardLayout) inputContainer.getLayout();
        cl.show(inputContainer, isRecording ? "recording" : "standard");
    }

    private void showEmojiPicker() {
        EmojiPickerPopup popup = new EmojiPickerPopup(new EmojiPickerPopup.EmojiCallback() {
            @Override
            public void onEmojiSelected(String emoji) {
                txtInput.setText(txtInput.getText() + emoji);
                txtInput.requestFocusInWindow();
            }

            @Override
            public void onStickerSelected(String stickerText) {
                if (callback != null) callback.onSendSticker(stickerText);
            }
        });
        popup.show(btnEmoji, 0, -260);
    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecciona un archivo para transferir");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (f != null && f.exists() && callback != null) {
                callback.onSendFile(f);
            }
        }
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

    public void setRoomInfo(ChatRoom room) {
        if (room != null) {
            lblChatTitle.setText(room.getName());
            lblChatSubtitle.setText(room.getTopic());
            btnMeeting.setVisible(room.isMeeting());
        }
    }

    public void addMessage(ChatMessage message) {
        ChatBubblePanel bubble = new ChatBubblePanel(message, audioService);
        messageListPanel.add(bubble);
        messageListPanel.add(Box.createVerticalStrut(4));
        messageListPanel.revalidate();
        messageListPanel.repaint();

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
}

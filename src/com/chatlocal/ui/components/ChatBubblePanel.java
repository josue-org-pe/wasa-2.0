package com.chatlocal.ui.components;

import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.MessageStatus;
import com.chatlocal.backend.model.MessageType;
import com.chatlocal.backend.service.AudioRecorderService;
import com.chatlocal.backend.service.FileTransferManager;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

// panel que dibuja una burbuja de mensaje con nombre, avatar, audio o archivo
public class ChatBubblePanel extends JPanel {

    private final ChatMessage message;
    private final AudioRecorderService audioService;
    private JButton btnPlayAudio;
    private boolean isAudioPlaying = false;

    public ChatBubblePanel(ChatMessage message) {
        this(message, null);
    }

    public ChatBubblePanel(ChatMessage message, AudioRecorderService audioService) {
        this.message = message;
        this.audioService = audioService;

        setOpaque(false);

        if (message.getType() == MessageType.SYSTEM) {
            buildSystemMessage();
        } else if (message.isSelf()) {
            buildSelfMessage();
        } else {
            buildPeerMessage();
        }
    }

    private void buildSystemMessage() {
        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));

        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getTheme().bubbleSystem);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(ThemeManager.getTheme().borderSubtle);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);

        JLabel label = new JLabel(message.getContent() + " • " + message.getTimestamp());
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setForeground(ThemeManager.getTheme().textMuted);
        pill.add(label);

        add(pill);
    }

    private void buildSelfMessage() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 4));

        JPanel bubble = new JPanel(new BorderLayout(8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getTheme().bubbleSelf);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 14, 6, 14));

        // Contenido del mensaje según tipo
        bubble.add(createMessageContent(true), BorderLayout.CENTER);

        // Fila inferior: Hora y Check de entrega (✓ / ✓✓)
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        footer.setOpaque(false);

        JLabel timeLabel = new JLabel(message.getTimestamp());
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(255, 255, 255, 180));
        footer.add(timeLabel);

        // Checkmark
        Color checkColor = (message.getStatus() == MessageStatus.DELIVERED || message.getStatus() == MessageStatus.READ)
                ? new Color(0x34, 0xD3, 0x99) // Verde/Cyan de confirmación
                : new Color(255, 255, 255, 150); // Grisáceo de envío

        Icon checkIcon = (message.getStatus() == MessageStatus.DELIVERED || message.getStatus() == MessageStatus.READ)
                ? Icons.doubleCheck(14, checkColor)
                : Icons.singleCheck(14, checkColor);

        JLabel checkLabel = new JLabel(checkIcon);
        footer.add(checkLabel);

        bubble.add(footer, BorderLayout.SOUTH);
        add(bubble);
    }

    private void buildPeerMessage() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));

        JPanel rowPanel = new JPanel(new BorderLayout(8, 0));
        rowPanel.setOpaque(false);

        // Avatar del contacto con su color distintivo y foto si tiene
        Color avatarBg = new Color(message.getSenderColorHex());
        JLabel avatarLabel = new JLabel(Icons.avatar(message.getSender(), 34, avatarBg, Color.WHITE, message.getSenderAvatarPath()));
        avatarLabel.setVerticalAlignment(SwingConstants.TOP);
        rowPanel.add(avatarLabel, BorderLayout.WEST);

        // Burbuja
        JPanel bubble = new JPanel(new BorderLayout(8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getTheme().bubblePeer);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(ThemeManager.getTheme().borderSubtle);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubble.setOpaque(false);
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 14, 6, 14));

        // Encabezado con NOMBRE VISIBLE y COLOREADO
        JLabel senderLabel = new JLabel(message.getSender());
        senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        senderLabel.setForeground(new Color(message.getSenderColorHex()));
        bubble.add(senderLabel, BorderLayout.NORTH);

        bubble.add(createMessageContent(false), BorderLayout.CENTER);

        // Hora
        JLabel timeLabel = new JLabel(message.getTimestamp(), SwingConstants.RIGHT);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(ThemeManager.getTheme().textMuted);
        bubble.add(timeLabel, BorderLayout.SOUTH);

        rowPanel.add(bubble, BorderLayout.CENTER);
        add(rowPanel);
    }

    private JComponent createMessageContent(boolean isSelf) {
        if (message.getType() == MessageType.AUDIO) {
            return createAudioPlayerWidget(isSelf);
        } else if (message.getType() == MessageType.STICKER) {
            return createStickerWidget(isSelf);
        } else if (message.getType() == MessageType.FILE) {
            return createFileCardWidget(isSelf);
        } else {
            return createTextWidget(isSelf);
        }
    }

    private JComponent createTextWidget(boolean isSelf) {
        JTextArea area = new JTextArea(message.getContent());
        area.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        area.setForeground(isSelf ? Color.WHITE : ThemeManager.getTheme().textPrimary);
        area.setOpaque(false);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFocusable(false);

        int maxCharWidth = 44;
        if (message.getContent().length() > maxCharWidth) {
            area.setColumns(Math.min(message.getContent().length(), maxCharWidth));
        }
        return area;
    }

    private JComponent createStickerWidget(boolean isSelf) {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setOpaque(false);
        JLabel lblSticker = new JLabel(message.getContent());
        lblSticker.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        lblSticker.setForeground(isSelf ? Color.WHITE : ThemeManager.getTheme().textPrimary);
        panel.add(lblSticker, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createAudioPlayerWidget(boolean isSelf) {
        JPanel card = new JPanel(new BorderLayout(10, 4));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

        btnPlayAudio = new JButton(Icons.play(16, isSelf ? Color.WHITE : ThemeManager.getTheme().primary));
        btnPlayAudio.setPreferredSize(new Dimension(34, 34));
        btnPlayAudio.setContentAreaFilled(false);
        btnPlayAudio.setBorderPainted(false);
        btnPlayAudio.setFocusPainted(false);
        btnPlayAudio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnPlayAudio.addActionListener(e -> {
            if (message.getLocalFilePath() == null) return;
            File audioFile = new File(message.getLocalFilePath());
            AudioRecorderService svc = (audioService != null) ? audioService : new AudioRecorderService();
            if (isAudioPlaying) {
                svc.stopPlayback();
                isAudioPlaying = false;
                btnPlayAudio.setIcon(Icons.play(16, isSelf ? Color.WHITE : ThemeManager.getTheme().primary));
            } else {
                isAudioPlaying = true;
                btnPlayAudio.setIcon(Icons.pause(16, isSelf ? Color.WHITE : ThemeManager.getTheme().primary));
                svc.play(audioFile, () -> {
                    isAudioPlaying = false;
                    btnPlayAudio.setIcon(Icons.play(16, isSelf ? Color.WHITE : ThemeManager.getTheme().primary));
                });
            }
        });

        // Barra de progreso y duración
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);

        JLabel lblAudioTitle = new JLabel("🎙️ Nota de voz (" + message.getFormattedAudioDuration() + ")");
        lblAudioTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAudioTitle.setForeground(isSelf ? Color.WHITE : ThemeManager.getTheme().textPrimary);

        JProgressBar progress = new JProgressBar();
        progress.setPreferredSize(new Dimension(140, 6));
        progress.setValue(100);
        progress.setForeground(isSelf ? Color.WHITE : ThemeManager.getTheme().primary);
        progress.setBackground(new Color(0, 0, 0, 50));
        progress.setBorderPainted(false);

        infoPanel.add(lblAudioTitle);
        infoPanel.add(progress);

        card.add(btnPlayAudio, BorderLayout.WEST);
        card.add(infoPanel, BorderLayout.CENTER);
        return card;
    }

    private JComponent createFileCardWidget(boolean isSelf) {
        JPanel card = new JPanel(new BorderLayout(10, 6));
        card.setOpaque(false);

        JLabel fileIcon = new JLabel(Icons.file(28, isSelf ? Color.WHITE : ThemeManager.getTheme().primary));
        card.add(fileIcon, BorderLayout.WEST);

        JPanel details = new JPanel(new GridLayout(2, 1, 0, 2));
        details.setOpaque(false);

        JLabel nameLabel = new JLabel(message.getFileName() != null ? message.getFileName() : "archivo");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(isSelf ? Color.WHITE : ThemeManager.getTheme().textPrimary);

        JLabel sizeLabel = new JLabel(message.getFormattedFileSize());
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sizeLabel.setForeground(isSelf ? new Color(255, 255, 255, 200) : ThemeManager.getTheme().textMuted);

        details.add(nameLabel);
        details.add(sizeLabel);
        card.add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        actions.setOpaque(false);

        if (message.getLocalFilePath() != null) {
            File targetFile = new File(message.getLocalFilePath());
            ModernButton btnOpen = new ModernButton("Abrir", Icons.file(13, Color.WHITE), ModernButton.Variant.GHOST);
            btnOpen.setMargin(new Insets(3, 8, 3, 8));
            btnOpen.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnOpen.addActionListener(e -> FileTransferManager.openFile(targetFile));
            actions.add(btnOpen);
        }

        ModernButton btnFolder = new ModernButton("Carpeta", Icons.folder(13, Color.WHITE), ModernButton.Variant.GHOST);
        btnFolder.setMargin(new Insets(3, 8, 3, 8));
        btnFolder.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnFolder.addActionListener(e -> FileTransferManager.openReceivedFolder());
        actions.add(btnFolder);

        card.add(actions, BorderLayout.SOUTH);
        return card;
    }
}

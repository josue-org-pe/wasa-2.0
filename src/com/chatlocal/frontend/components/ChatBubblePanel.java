package com.chatlocal.frontend.components;

import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.MessageType;
import com.chatlocal.frontend.theme.ThemeColors;
import com.chatlocal.frontend.theme.ThemeFonts;
import com.chatlocal.frontend.theme.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/**
 * Representa una fila y burbuja de mensaje individual en el historial de chat,
 * adaptando su diseño según si es propio, del contacto, un archivo transferido o aviso del sistema.
 */
public class ChatBubblePanel extends JPanel {

    private final ChatMessage message;

    public ChatBubblePanel(ChatMessage message) {
        this.message = message;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(4, 12, 4, 12));

        if (message.getType() == MessageType.SYSTEM) {
            buildSystemMessage();
        } else {
            buildUserOrFileMessage();
        }
    }

    private void buildSystemMessage() {
        JPanel centerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerWrapper.setOpaque(false);

        JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UIUtils.applyQualityRendering(g2);
                g2.setColor(ThemeColors.BUBBLE_SYSTEM);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(ThemeColors.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(4, 14, 4, 14));

        JLabel label = new JLabel(message.getContent());
        label.setFont(ThemeFonts.regular(11));
        label.setForeground(ThemeColors.TEXT_MUTED);
        pill.add(label);

        centerWrapper.add(pill);
        add(centerWrapper, BorderLayout.CENTER);
    }

    private void buildUserOrFileMessage() {
        int alignment = message.isSelf() ? FlowLayout.RIGHT : FlowLayout.LEFT;
        JPanel rowWrapper = new JPanel(new FlowLayout(alignment, 0, 0));
        rowWrapper.setOpaque(false);

        Color bubbleBg = message.isSelf() ? ThemeColors.BUBBLE_SELF : ThemeColors.BUBBLE_OTHER;
        Color borderColor = message.isSelf() ? new Color(99, 102, 241, 100) : ThemeColors.BORDER;

        JPanel bubble = new JPanel(new BorderLayout(8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UIUtils.applyQualityRendering(g2);
                g2.setColor(bubbleBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Cabecera con emisor (solo si es contacto)
        if (!message.isSelf()) {
            JLabel senderLabel = new JLabel(message.getSender());
            senderLabel.setFont(ThemeFonts.bold(11));
            senderLabel.setForeground(ThemeColors.ACCENT_CYAN);
            bubble.add(senderLabel, BorderLayout.NORTH);
        }

        // Cuerpo: Texto o Archivo
        if (message.getType() == MessageType.FILE) {
            bubble.add(buildFileCard(), BorderLayout.CENTER);
        } else {
            JTextArea textArea = new JTextArea(message.getContent());
            textArea.setFont(ThemeFonts.regular(13));
            textArea.setForeground(ThemeColors.TEXT_PRIMARY);
            textArea.setOpaque(false);
            textArea.setEditable(false);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setBorder(null);

            // Limitar ancho máximo de la burbuja para legibilidad
            int maxWidth = 380;
            FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
            int textWidth = fm.stringWidth(message.getContent());
            if (textWidth > maxWidth || message.getContent().contains("\n")) {
                textArea.setColumns(30);
            }

            bubble.add(textArea, BorderLayout.CENTER);
        }

        // Pie: Hora del mensaje
        JLabel timeLabel = new JLabel(message.getTimestamp());
        timeLabel.setFont(ThemeFonts.regular(10));
        timeLabel.setForeground(message.isSelf() ? new Color(224, 231, 255, 180) : ThemeColors.TEXT_HINT);
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bubble.add(timeLabel, BorderLayout.SOUTH);

        rowWrapper.add(bubble);
        add(rowWrapper, BorderLayout.CENTER);
    }

    private JPanel buildFileCard() {
        JPanel fileCard = new JPanel(new BorderLayout(10, 6));
        fileCard.setOpaque(false);
        fileCard.setBorder(new EmptyBorder(4, 0, 4, 0));

        // Icono de archivo
        JLabel iconLabel = new JLabel("📎");
        iconLabel.setFont(ThemeFonts.title(20));
        iconLabel.setForeground(ThemeColors.ACCENT_CYAN);
        fileCard.add(iconLabel, BorderLayout.WEST);

        // Info del archivo
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(message.getFileName());
        nameLabel.setFont(ThemeFonts.bold(12));
        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel sizeLabel = new JLabel(message.getFormattedFileSize() + (message.isSelf() ? " (Enviado)" : " (Descargado)"));
        sizeLabel.setFont(ThemeFonts.regular(10));
        sizeLabel.setForeground(ThemeColors.TEXT_MUTED);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(2));
        infoPanel.add(sizeLabel);

        fileCard.add(infoPanel, BorderLayout.CENTER);

        // Botón interactivo para abrir carpeta / archivo
        if (message.getLocalFilePath() != null) {
            ModernButton openBtn = new ModernButton("Abrir", ModernButton.Type.SECONDARY);
            openBtn.setFont(ThemeFonts.bold(10));
            openBtn.setCornerRadius(6);
            openBtn.setMargin(new Insets(4, 8, 4, 8));
            openBtn.addActionListener(e -> openFileLocation(message.getLocalFilePath()));
            fileCard.add(openBtn, BorderLayout.EAST);
        }

        return fileCard;
    }

    private void openFileLocation(String filePath) {
        try {
            File f = new File(filePath);
            if (f.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        desktop.open(f);
                        return;
                    }
                }
            }
            // Si no se puede abrir directamente, abrir el directorio contenedor
            File parent = f.getParentFile();
            if (parent != null && parent.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(parent);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo abrir el archivo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

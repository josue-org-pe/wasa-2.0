package com.chatlocal.ui.dialogs;

import com.chatlocal.ui.components.ModernButton;
import com.chatlocal.ui.components.ModernTextField;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.Theme;
import com.chatlocal.ui.theme.ThemeManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Diálogo modal para previsualizar archivos e imágenes antes de enviarlos.
 * Permite verificar el contenido, ver tamaño exacto y añadir un comentario opcional.
 */
public class FilePreviewDialog extends JDialog {

    public interface FilePreviewCallback {
        void onConfirm(String caption);
    }

    private final File file;
    private final FilePreviewCallback callback;
    private ModernTextField txtCaption;

    public FilePreviewDialog(Window parent, File file, FilePreviewCallback callback) {
        super(parent, "Vista previa de archivo", ModalityType.APPLICATION_MODAL);
        this.file = file;
        this.callback = callback;

        setSize(460, 420);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(ThemeManager.getTheme().bgDark);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setOpaque(true);
        root.setBackground(ThemeManager.getTheme().bgDark);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // 1. Título
        JLabel lblTitle = new JLabel("Vista previa de archivo a enviar");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(ThemeManager.getTheme().textPrimary);
        root.add(lblTitle, BorderLayout.NORTH);

        // 2. Tarjeta central de contenido (Imagen o Archivo)
        JPanel centerCard = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.enableQualityRendering(g2);
                g2.setColor(ThemeManager.getTheme().bgCard);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(ThemeManager.getTheme().borderSubtle);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        centerCard.setOpaque(false);
        centerCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        boolean isImage = isImageFile(file);
        if (isImage) {
            BufferedImage previewImg = loadImagePreview(file, 380, 180);
            if (previewImg != null) {
                JLabel lblImg = new JLabel(new ImageIcon(previewImg));
                lblImg.setHorizontalAlignment(SwingConstants.CENTER);
                centerCard.add(lblImg, BorderLayout.CENTER);
            } else {
                centerCard.add(createGenericFileCard(), BorderLayout.CENTER);
            }
        } else {
            centerCard.add(createGenericFileCard(), BorderLayout.CENTER);
        }

        // Metadatos inferiores
        JPanel metaPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        metaPanel.setOpaque(false);

        JLabel lblName = new JLabel(file.getName());
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(ThemeManager.getTheme().textPrimary);

        JLabel lblSize = new JLabel(formatFileSize(file.length()) + " • " + getFileExtension(file.getName()).toUpperCase());
        lblSize.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSize.setForeground(ThemeManager.getTheme().textMuted);

        metaPanel.add(lblName);
        metaPanel.add(lblSize);
        centerCard.add(metaPanel, BorderLayout.SOUTH);

        root.add(centerCard, BorderLayout.CENTER);

        // 3. Fila de entrada de comentario y botones
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        txtCaption = new ModernTextField("Añadir un comentario (opcional)...", 20);
        txtCaption.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttons.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cancelar", ModernButton.Variant.GHOST);
        btnCancel.addActionListener(e -> dispose());

        ModernButton btnSend = new ModernButton("Enviar", Icons.send(14, Color.WHITE), ModernButton.Variant.PRIMARY);
        btnSend.addActionListener(e -> {
            dispose();
            if (callback != null) {
                callback.onConfirm(txtCaption.getText().trim());
            }
        });

        buttons.add(btnCancel);
        buttons.add(btnSend);

        bottomPanel.add(txtCaption);
        bottomPanel.add(Box.createVerticalStrut(8));
        bottomPanel.add(buttons);

        root.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createGenericFileCard() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JLabel iconLabel = new JLabel(Icons.file(48, ThemeManager.getTheme().primary));
        panel.add(iconLabel);
        return panel;
    }

    private boolean isImageFile(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
    }

    private String getFileExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "Archivo";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private BufferedImage loadImagePreview(File file, int maxW, int maxH) {
        try {
            BufferedImage orig = ImageIO.read(file);
            if (orig == null) return null;

            double scale = Math.min((double) maxW / orig.getWidth(), (double) maxH / orig.getHeight());
            int w = (int) (orig.getWidth() * Math.min(1.0, scale));
            int h = (int) (orig.getHeight() * Math.min(1.0, scale));

            BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            Theme.enableQualityRendering(g2);
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));
            g2.drawImage(orig, 0, 0, w, h, null);
            g2.dispose();
            return scaled;
        } catch (Exception e) {
            return null;
        }
    }
}

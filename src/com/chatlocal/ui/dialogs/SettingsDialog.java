package com.chatlocal.ui.dialogs;

import com.chatlocal.backend.model.UserProfile;
import com.chatlocal.backend.service.ChatService;
import com.chatlocal.backend.service.FileTransferManager;
import com.chatlocal.ui.components.ModernButton;
import com.chatlocal.ui.components.ModernTextField;
import com.chatlocal.ui.theme.AppTheme;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Diálogo modal de Configuración y Personalización de la aplicación.
 * Permite cambiar perfil, mensaje de estado, tema visual en caliente y gestionar bloqueos.
 */
public class SettingsDialog extends JDialog {

    private final ChatService chatService;
    private final Runnable onSettingsUpdated;

    private ModernTextField txtUsername;
    private ModernTextField txtStatusMessage;
    private int selectedColorHex;
    private DefaultListModel<String> blockedListModel;

    private static final int[] PALETTE = {
            0x8B5CF6, 0x10B981, 0x3B82F6, 0xF43F5E,
            0xF59E0B, 0x06B6D4, 0xEC4899, 0x14B8A6
    };

    public SettingsDialog(Frame parent, ChatService chatService, Runnable onSettingsUpdated) {
        super(parent, "Configuración y Personalización", true);
        this.chatService = chatService;
        this.onSettingsUpdated = onSettingsUpdated;

        UserProfile current = chatService.getLocalUserProfile();
        this.selectedColorHex = current != null ? current.getAvatarColorHex() : 0x8B5CF6;

        setSize(520, 460);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(ThemeManager.getTheme().bgDark);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(true);
        root.setBackground(ThemeManager.getTheme().bgDark);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.setBackground(ThemeManager.getTheme().bgDark);
        tabs.setForeground(ThemeManager.getTheme().textPrimary);

        tabs.addTab("👤 Mi Perfil", createProfileTab());
        tabs.addTab("🎨 Apariencia", createAppearanceTab());
        tabs.addTab("🚫 Bloqueados", createBlockedTab());
        tabs.addTab("📁 Almacenamiento", createStorageTab());

        root.add(tabs, BorderLayout.CENTER);

        // Barra inferior con botón Guardar
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        footer.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cerrar", ModernButton.Variant.GHOST);
        btnCancel.addActionListener(e -> dispose());

        ModernButton btnSave = new ModernButton("Guardar Cambios", Icons.check(14, Color.WHITE), ModernButton.Variant.PRIMARY);
        btnSave.addActionListener(e -> saveSettings());

        footer.add(btnCancel);
        footer.add(btnSave);
        root.add(footer, BorderLayout.SOUTH);

        add(root);
    }

    private JPanel createProfileTab() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        UserProfile profile = chatService.getLocalUserProfile();

        // Nombre de usuario
        JLabel lblName = new JLabel("Nombre o Alias:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblName.setForeground(ThemeManager.getTheme().textPrimary);
        txtUsername = new ModernTextField("Tu alias...", 15);
        txtUsername.setText(profile != null ? profile.getUsername() : "Usuario");

        panel.add(lblName);
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtUsername);
        panel.add(Box.createVerticalStrut(12));

        // Mensaje de estado
        JLabel lblStatus = new JLabel("Mensaje de Estado:");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(ThemeManager.getTheme().textPrimary);
        txtStatusMessage = new ModernTextField("Disponible, En clase, etc...", 15);
        txtStatusMessage.setText(profile != null ? profile.getStatusMessage() : "En línea");

        panel.add(lblStatus);
        panel.add(Box.createVerticalStrut(4));
        panel.add(txtStatusMessage);
        panel.add(Box.createVerticalStrut(14));

        // Selector de color de avatar
        JLabel lblColor = new JLabel("Color de Avatar:");
        lblColor.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblColor.setForeground(ThemeManager.getTheme().textPrimary);
        panel.add(lblColor);
        panel.add(Box.createVerticalStrut(6));

        JPanel palettePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        palettePanel.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        for (int colorHex : PALETTE) {
            JRadioButton btn = new JRadioButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(colorHex));
                    g2.fillOval(2, 2, 24, 24);
                    if (isSelected()) {
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(2.0f));
                        g2.drawOval(0, 0, 27, 27);
                    }
                    g2.dispose();
                }
            };
            btn.setPreferredSize(new Dimension(28, 28));
            btn.setOpaque(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (colorHex == selectedColorHex) btn.setSelected(true);
            btn.addActionListener(e -> selectedColorHex = colorHex);
            group.add(btn);
            palettePanel.add(btn);
        }
        panel.add(palettePanel);

        return panel;
    }

    private JPanel createAppearanceTab() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel lblTheme = new JLabel("Elige tu paleta de tema visual:");
        lblTheme.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTheme.setForeground(ThemeManager.getTheme().textPrimary);
        panel.add(lblTheme);
        panel.add(Box.createVerticalStrut(12));

        for (AppTheme theme : ThemeManager.getAvailableThemes()) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(true);
            row.setBackground(theme.bgCard);
            row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            JLabel lblName = new JLabel(theme.getName());
            lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblName.setForeground(theme.textPrimary);

            ModernButton btnApply = new ModernButton(
                    ThemeManager.getTheme().getId().equals(theme.getId()) ? "Activo" : "Aplicar",
                    ModernButton.Variant.PRIMARY
            );
            btnApply.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnApply.setMargin(new Insets(3, 10, 3, 10));
            btnApply.setEnabled(!ThemeManager.getTheme().getId().equals(theme.getId()));
            btnApply.addActionListener(e -> {
                ThemeManager.setTheme(theme);
                dispose();
                if (onSettingsUpdated != null) onSettingsUpdated.run();
            });

            row.add(lblName, BorderLayout.CENTER);
            row.add(btnApply, BorderLayout.EAST);

            panel.add(row);
            panel.add(Box.createVerticalStrut(8));
        }

        return panel;
    }

    private JPanel createBlockedTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel lblInfo = new JLabel("Usuarios bloqueados (sus mensajes son ignorados):");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblInfo.setForeground(ThemeManager.getTheme().textMuted);
        panel.add(lblInfo, BorderLayout.NORTH);

        blockedListModel = new DefaultListModel<>();
        for (String u : chatService.getBlockedUsers()) {
            blockedListModel.addElement(u);
        }

        JList<String> list = new JList<>(blockedListModel);
        list.setBackground(ThemeManager.getTheme().bgInput);
        list.setForeground(ThemeManager.getTheme().textPrimary);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        ModernButton btnUnblock = new ModernButton("Desbloquear Seleccionado", ModernButton.Variant.SECONDARY);
        btnUnblock.addActionListener(e -> {
            String selected = list.getSelectedValue();
            if (selected != null) {
                chatService.unblockUser(selected);
                blockedListModel.removeElement(selected);
                JOptionPane.showMessageDialog(this, "Usuario " + selected + " desbloqueado.");
            }
        });
        panel.add(btnUnblock, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStorageTab() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        File folder = FileTransferManager.getReceivedFilesFolder();

        JLabel lblPath = new JLabel("Carpeta de archivos recibidos y notas de voz:");
        lblPath.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPath.setForeground(ThemeManager.getTheme().textPrimary);

        JLabel lblDir = new JLabel(folder.getAbsolutePath());
        lblDir.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblDir.setForeground(ThemeManager.getTheme().textMuted);

        panel.add(lblPath);
        panel.add(Box.createVerticalStrut(6));
        panel.add(lblDir);
        panel.add(Box.createVerticalStrut(14));

        ModernButton btnOpen = new ModernButton("Abrir Carpeta en Windows Explorer", Icons.folder(14, Color.WHITE), ModernButton.Variant.SECONDARY);
        btnOpen.addActionListener(e -> FileTransferManager.openReceivedFolder());
        panel.add(btnOpen);

        return panel;
    }

    private void saveSettings() {
        String newName = txtUsername.getText().trim();
        if (newName.length() < 2) {
            JOptionPane.showMessageDialog(this, "El nombre de usuario debe tener al menos 2 caracteres.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UserProfile profile = chatService.getLocalUserProfile();
        if (profile != null) {
            profile.setUsername(newName);
            profile.setStatusMessage(txtStatusMessage.getText().trim());
            profile.setAvatarColorHex(selectedColorHex);
        }

        dispose();
        if (onSettingsUpdated != null) {
            onSettingsUpdated.run();
        }
    }
}

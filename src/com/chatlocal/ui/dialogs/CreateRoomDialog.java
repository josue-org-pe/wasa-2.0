package com.chatlocal.ui.dialogs;

import com.chatlocal.backend.service.ChatService;
import com.chatlocal.ui.components.ModernButton;
import com.chatlocal.ui.components.ModernTextField;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo modal para crear una nueva sala, grupo temático o reunión local.
 */
public class CreateRoomDialog extends JDialog {

    private final ChatService chatService;
    private final Runnable onRoomCreated;

    private ModernTextField txtRoomName;
    private ModernTextField txtTopic;
    private ModernTextField txtAccessCode;
    private JCheckBox chkMeeting;

    public CreateRoomDialog(Frame parent, ChatService chatService, Runnable onRoomCreated) {
        super(parent, "Crear Nueva Sala / Reunión Local", true);
        this.chatService = chatService;
        this.onRoomCreated = onRoomCreated;

        setSize(440, 380);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(ThemeManager.getTheme().bgDark);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel();
        root.setOpaque(true);
        root.setBackground(ThemeManager.getTheme().bgDark);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel lblTitle = new JLabel("Configura tu nueva Sala o Reunión");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(ThemeManager.getTheme().textPrimary);
        root.add(lblTitle);
        root.add(Box.createVerticalStrut(14));

        // Nombre de la sala
        JLabel lblName = new JLabel("Nombre de la Sala / Grupo:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblName.setForeground(ThemeManager.getTheme().textSecondary);
        txtRoomName = new ModernTextField("Ej: Grupo Estudio POO, Sala General 2...", 15);
        root.add(lblName);
        root.add(Box.createVerticalStrut(4));
        root.add(txtRoomName);
        root.add(Box.createVerticalStrut(12));

        // Tema / Descripción
        JLabel lblTopic = new JLabel("Tema o Propósito:");
        lblTopic.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTopic.setForeground(ThemeManager.getTheme().textSecondary);
        txtTopic = new ModernTextField("Ej: Discusión de clases, entregas...", 15);
        root.add(lblTopic);
        root.add(Box.createVerticalStrut(4));
        root.add(txtTopic);
        root.add(Box.createVerticalStrut(12));

        // Código de acceso opcional
        JLabel lblCode = new JLabel("Código de Acceso / Contraseña (Opcional):");
        lblCode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCode.setForeground(ThemeManager.getTheme().textSecondary);
        txtAccessCode = new ModernTextField("Dejar vacío si es pública...", 10);
        root.add(lblCode);
        root.add(Box.createVerticalStrut(4));
        root.add(txtAccessCode);
        root.add(Box.createVerticalStrut(12));

        // Checkbox reunión
        chkMeeting = new JCheckBox("Marcar como Reunión Local Activa (Destacar con banner)");
        chkMeeting.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkMeeting.setForeground(ThemeManager.getTheme().textPrimary);
        chkMeeting.setOpaque(false);
        root.add(chkMeeting);
        root.add(Box.createVerticalStrut(18));

        // Botones
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cancelar", ModernButton.Variant.GHOST);
        btnCancel.addActionListener(e -> dispose());

        ModernButton btnCreate = new ModernButton("Crear Sala", Icons.plus(14, Color.WHITE), ModernButton.Variant.PRIMARY);
        btnCreate.addActionListener(e -> submit());

        footer.add(btnCancel);
        footer.add(btnCreate);
        root.add(footer);

        add(root);
    }

    private void submit() {
        String name = txtRoomName.getText().trim();
        if (name.length() < 2) {
            JOptionPane.showMessageDialog(this, "El nombre de la sala debe tener al menos 2 caracteres.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String topic = txtTopic.getText().trim();
        String code = txtAccessCode.getText().trim();
        boolean isMeeting = chkMeeting.isSelected();

        chatService.createRoom(name, topic, code, isMeeting);
        dispose();

        if (onRoomCreated != null) {
            onRoomCreated.run();
        }
    }
}

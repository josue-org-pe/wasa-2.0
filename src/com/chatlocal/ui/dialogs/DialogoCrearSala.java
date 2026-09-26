package com.chatlocal.ui.dialogs;

import com.chatlocal.backend.service.ServicioChat;
import com.chatlocal.ui.components.BotonModerno;
import com.chatlocal.ui.components.CampoTextoModerno;
import com.chatlocal.ui.theme.Iconos;
import com.chatlocal.ui.theme.GestorTema;

import javax.swing.*;
import java.awt.*;

// ventana para crear una nueva sala o grupo
public class DialogoCrearSala extends JDialog {

    private final ServicioChat chatService;
    private final Runnable onRoomCreated;

    private CampoTextoModerno txtRoomName;
    private CampoTextoModerno txtTopic;
    private CampoTextoModerno txtAccessCode;
    private JCheckBox chkMeeting;

    public DialogoCrearSala(Frame parent, ServicioChat chatService, Runnable onRoomCreated) {
        super(parent, "Crear Sala", true);
        this.chatService = chatService;
        this.onRoomCreated = onRoomCreated;

        setSize(440, 380);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(GestorTema.getTheme().bgDark);
        com.chatlocal.ui.theme.Tema.applyAppIcon(this);

        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel();
        root.setOpaque(true);
        root.setBackground(GestorTema.getTheme().bgDark);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel lblTitle = new JLabel("Crear nueva sala o grupo");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(GestorTema.getTheme().textPrimary);
        root.add(lblTitle);
        root.add(Box.createVerticalStrut(14));

        // Nombre de la sala
        JLabel lblName = new JLabel("Nombre de la Sala / Grupo:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblName.setForeground(GestorTema.getTheme().textSecondary);
        txtRoomName = new CampoTextoModerno("Ej: Grupo Estudio POO, Sala General 2...", 15);
        root.add(lblName);
        root.add(Box.createVerticalStrut(4));
        root.add(txtRoomName);
        root.add(Box.createVerticalStrut(12));

        // Tema / Descripción
        JLabel lblTopic = new JLabel("Tema o Propósito:");
        lblTopic.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTopic.setForeground(GestorTema.getTheme().textSecondary);
        txtTopic = new CampoTextoModerno("Ej: Discusión de clases, entregas...", 15);
        root.add(lblTopic);
        root.add(Box.createVerticalStrut(4));
        root.add(txtTopic);
        root.add(Box.createVerticalStrut(12));

        // Código de acceso opcional
        JLabel lblCode = new JLabel("Código de Acceso / Contraseña (Opcional):");
        lblCode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCode.setForeground(GestorTema.getTheme().textSecondary);
        txtAccessCode = new CampoTextoModerno("Dejar vacío si es pública...", 10);
        root.add(lblCode);
        root.add(Box.createVerticalStrut(4));
        root.add(txtAccessCode);
        root.add(Box.createVerticalStrut(12));

        // Checkbox reunión
        chkMeeting = new JCheckBox("Marcar como Reunión Local Activa (Destacar con banner)");
        chkMeeting.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkMeeting.setForeground(GestorTema.getTheme().textPrimary);
        chkMeeting.setOpaque(false);
        root.add(chkMeeting);
        root.add(Box.createVerticalStrut(18));

        // Botones
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        BotonModerno btnCancel = new BotonModerno("Cancelar", BotonModerno.Variant.GHOST);
        btnCancel.addActionListener(e -> dispose());

        BotonModerno btnCreate = new BotonModerno("Crear Sala", Iconos.plus(14, Color.WHITE), BotonModerno.Variant.PRIMARY);
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

package com.chatlocal.frontend.view;

import com.chatlocal.backend.model.ConnectionRole;
import com.chatlocal.backend.service.NetworkUtils;
import com.chatlocal.frontend.components.ModernButton;
import com.chatlocal.frontend.components.ModernCard;
import com.chatlocal.frontend.components.ModernTextField;
import com.chatlocal.frontend.theme.ThemeColors;
import com.chatlocal.frontend.theme.ThemeFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Pantalla de configuración inicial: selección de rol (Host / Cliente),
 * detección de IP local y parámetros de conexión.
 */
public class ConfigPanel extends JPanel {

    public interface ConfigListener {
        void onStartHost(int port);
        void onConnectToHost(String hostIp, int port);
    }

    private ConfigListener listener;

    private ConnectionRole selectedRole = ConnectionRole.HOST;
    private final ModernCard hostRoleCard = new ModernCard();
    private final ModernCard clientRoleCard = new ModernCard();

    private final ModernTextField ipField = new ModernTextField("127.0.0.1", 15);
    private final ModernTextField portField = new ModernTextField("5000", 6);
    private final JLabel ipLabel = new JLabel("Dirección IP del anfitrión:");
    private final ModernButton connectButton = new ModernButton("Iniciar como Anfitrión", ModernButton.Type.PRIMARY);

    private final String localIp;

    public ConfigPanel() {
        this.localIp = NetworkUtils.getPrimaryLocalIp();
        setLayout(new BorderLayout());
        setBackground(ThemeColors.BG_DARK);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        buildUI();
    }

    public void setConfigListener(ConfigListener listener) {
        this.listener = listener;
    }

    private void buildUI() {
        // --- 1. Cabecera ---
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Chat Local Sockets");
        titleLabel.setFont(ThemeFonts.title(22));
        titleLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("Comunicación P2P directa entre computadoras en red local");
        subtitleLabel.setFont(ThemeFonts.regular(13));
        subtitleLabel.setForeground(ThemeColors.TEXT_MUTED);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(18));

        add(headerPanel, BorderLayout.NORTH);

        // --- 2. Contenido Central ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Tarjetas de Selección de Rol
        JPanel rolesPanel = new JPanel(new GridLayout(1, 2, 14, 0));
        rolesPanel.setOpaque(false);
        rolesPanel.setMaximumSize(new Dimension(800, 100));

        setupRoleCard(hostRoleCard, "👑 Ser Anfitrión", "Esperar que otra PC se conecte a ti", ConnectionRole.HOST);
        setupRoleCard(clientRoleCard, "🔗 Conectarme", "Unirte a un anfitrión existente", ConnectionRole.CLIENT);

        rolesPanel.add(hostRoleCard);
        rolesPanel.add(clientRoleCard);
        centerPanel.add(rolesPanel);
        centerPanel.add(Box.createVerticalStrut(16));

        // Tarjeta de información de IP local
        ModernCard localIpCard = new ModernCard(new BorderLayout(12, 0));
        localIpCard.setBorder(new EmptyBorder(10, 16, 10, 16));
        localIpCard.setMaximumSize(new Dimension(800, 50));

        JLabel myIpText = new JLabel("Tu IP en la red local:  " + localIp);
        myIpText.setFont(ThemeFonts.bold(12));
        myIpText.setForeground(ThemeColors.TEXT_PRIMARY);

        ModernButton copyIpBtn = new ModernButton("Copiar IP", ModernButton.Type.SECONDARY);
        copyIpBtn.setFont(ThemeFonts.bold(11));
        copyIpBtn.setMargin(new Insets(4, 10, 4, 10));
        copyIpBtn.addActionListener(e -> copyIpToClipboard(copyIpBtn));

        localIpCard.add(myIpText, BorderLayout.CENTER);
        localIpCard.add(copyIpBtn, BorderLayout.EAST);
        centerPanel.add(localIpCard);
        centerPanel.add(Box.createVerticalStrut(16));

        // Formulario de parámetros (IP Remota y Puerto)
        ModernCard formCard = new ModernCard(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        formCard.setMaximumSize(new Dimension(800, 140));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 1: IP Anfitrión
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.4;
        ipLabel.setFont(ThemeFonts.bold(12));
        ipLabel.setForeground(ThemeColors.TEXT_MUTED);
        formCard.add(ipLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.6;
        ipField.setPlaceholder("Ej: 192.168.1.50");
        formCard.add(ipField, gbc);

        // Fila 2: Puerto
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.4;
        JLabel portLabel = new JLabel("Puerto de escucha / destino:");
        portLabel.setFont(ThemeFonts.bold(12));
        portLabel.setForeground(ThemeColors.TEXT_MUTED);
        formCard.add(portLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.6;
        portField.setPlaceholder("5000");
        formCard.add(portField, gbc);

        centerPanel.add(formCard);

        add(centerPanel, BorderLayout.CENTER);

        // --- 3. Pie con Botón de Conexión ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        bottomPanel.setOpaque(false);

        connectButton.setPreferredSize(new Dimension(200, 42));
        connectButton.addActionListener(e -> handleConnectAction());
        bottomPanel.add(connectButton);

        add(bottomPanel, BorderLayout.SOUTH);

        // Refrescar estado visual según el rol por defecto
        updateRoleSelection();
    }

    private void setupRoleCard(ModernCard card, String title, String description, ConnectionRole role) {
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(ThemeFonts.bold(14));
        titleLbl.setForeground(ThemeColors.TEXT_PRIMARY);

        JLabel descLbl = new JLabel(description);
        descLbl.setFont(ThemeFonts.regular(11));
        descLbl.setForeground(ThemeColors.TEXT_MUTED);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(titleLbl);
        content.add(Box.createVerticalStrut(4));
        content.add(descLbl);

        card.add(content, BorderLayout.CENTER);

        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                selectedRole = role;
                updateRoleSelection();
            }
        });
    }

    private void updateRoleSelection() {
        if (selectedRole == ConnectionRole.HOST) {
            hostRoleCard.setBackgroundColor(new Color(40, 52, 75));
            hostRoleCard.setBorderColor(ThemeColors.PRIMARY);

            clientRoleCard.setBackgroundColor(ThemeColors.BG_SURFACE);
            clientRoleCard.setBorderColor(ThemeColors.BORDER);

            ipField.setEnabled(false);
            ipLabel.setForeground(ThemeColors.TEXT_HINT);
            connectButton.setText("Esperar Conexión (Host)");
        } else {
            clientRoleCard.setBackgroundColor(new Color(40, 52, 75));
            clientRoleCard.setBorderColor(ThemeColors.PRIMARY);

            hostRoleCard.setBackgroundColor(ThemeColors.BG_SURFACE);
            hostRoleCard.setBorderColor(ThemeColors.BORDER);

            ipField.setEnabled(true);
            ipLabel.setForeground(ThemeColors.TEXT_PRIMARY);
            connectButton.setText("Conectarme al Host");
        }
        revalidate();
        repaint();
    }

    private void copyIpToClipboard(ModernButton btn) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(localIp), null);
        btn.setText("¡Copiado!");
        Timer timer = new Timer(1500, e -> btn.setText("Copiar IP"));
        timer.setRepeats(false);
        timer.start();
    }

    private void handleConnectAction() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (!NetworkUtils.isValidPort(port)) {
                JOptionPane.showMessageDialog(this, "El puerto debe estar entre 1024 y 65535.",
                        "Puerto Inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor introduce un número de puerto válido.",
                    "Puerto Inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (listener == null) return;

        if (selectedRole == ConnectionRole.HOST) {
            listener.onStartHost(port);
        } else {
            String ip = ipField.getText().trim();
            if (!NetworkUtils.isValidHost(ip)) {
                JOptionPane.showMessageDialog(this, "Por favor especifica una dirección IP o nombre de host válido.",
                        "IP Inválida", JOptionPane.WARNING_MESSAGE);
                return;
            }
            listener.onConnectToHost(ip, port);
        }
    }
}

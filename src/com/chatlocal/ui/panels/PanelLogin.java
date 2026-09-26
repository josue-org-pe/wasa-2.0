package com.chatlocal.ui.panels;

import com.chatlocal.backend.model.RolConexion;
import com.chatlocal.backend.model.UsuarioPerfil;
import com.chatlocal.backend.service.UtilidadesRed;
import com.chatlocal.backend.service.ProbadorPing;
import com.chatlocal.ui.components.*;
import com.chatlocal.ui.theme.Iconos;
import com.chatlocal.ui.theme.Tema;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

// panel para iniciar sesion y conectarse
public class PanelLogin extends JPanel {

    public interface LoginCallback {
        void onLoginSuccess(UsuarioPerfil userProfile, String hostIp, int port);
    }

    private final LoginCallback callback;

    // Componentes de Perfil
    private JLabel lblAvatarPreview;
    private CampoTextoModerno txtUsername;

    // Selector de modo
    private ControlSegmentado segmentedControl;
    private RolConexion currentRole = RolConexion.HOST;

    // Campos de Red
    private JPanel panelHostView;
    private JPanel panelClientView;
    private CampoTextoModerno txtClientHostIp;
    private CampoTextoModerno txtClientPort;
    private CampoTextoModerno txtHostPort;

    // Ping
    private BotonModerno btnTestPing;
    private InsigniaResultadoPing pingBadge;

    public PanelLogin(LoginCallback callback) {
        this.callback = callback;

        setOpaque(true);
        setBackground(Tema.BG_DARK);
        setLayout(new BorderLayout());

        buildUI();
    }

    private void buildUI() {
        // Envoltorio para centrar la tarjeta de Login
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        TarjetaModerna loginCard = new TarjetaModerna();
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.setPreferredSize(new Dimension(480, 560));
        loginCard.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // 1. Cabecera con Avatar Dinámico
        loginCard.add(createProfileHeader());
        loginCard.add(Box.createVerticalStrut(16));

        // 2. Campo de Usuario / Alias
        loginCard.add(createUsernameField());
        loginCard.add(Box.createVerticalStrut(18));

        // 3. Selector de Modo (Pestañas Crear vs Unirse)
        loginCard.add(createModeSelector());
        loginCard.add(Box.createVerticalStrut(16));

        // 4. Panel conmutador (Configuración Host vs Cliente)
        loginCard.add(createNetworkSettingsPanel());
        loginCard.add(Box.createVerticalStrut(14));

        // 5. Botón de Entrar / Iniciar
        loginCard.add(createSubmitButton());

        centerWrapper.add(loginCard);
        add(centerWrapper, BorderLayout.CENTER);
    }

    private JPanel createProfileHeader() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Avatar circular que responde en tiempo real
        lblAvatarPreview = new JLabel(Iconos.avatar("U", 60, Tema.PRIMARY, Tema.TEXT_PRIMARY));
        lblAvatarPreview.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("wasa 3.0");
        lblTitle.setFont(Tema.FONT_TITLE);
        lblTitle.setForeground(Tema.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSubtitle = new JLabel("Mensajería y videollamadas");
        lblSubtitle.setFont(Tema.FONT_CAPTION);
        lblSubtitle.setForeground(Tema.TEXT_MUTED);
        lblSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(lblAvatarPreview);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(3));
        panel.add(lblSubtitle);

        return panel;
    }

    private JPanel createUsernameField() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setOpaque(false);

        JLabel lblLabel = new JLabel("Tu Nombre / Alias:");
        lblLabel.setFont(Tema.FONT_BODY_BOLD);
        lblLabel.setForeground(Tema.TEXT_PRIMARY);

        txtUsername = new CampoTextoModerno("Escribe tu nombre o apodo...", 15);
        txtUsername.setText("Usuario_" + (int)(Math.random() * 900 + 100));

        // Actualizar avatar dinámico mientras el usuario escribe
        txtUsername.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateAvatar(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateAvatar(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateAvatar(); }
        });
        updateAvatar();

        panel.add(lblLabel, BorderLayout.NORTH);
        panel.add(txtUsername, BorderLayout.CENTER);
        return panel;
    }

    private void updateAvatar() {
        String name = txtUsername.getText().trim();
        UsuarioPerfil tempProfile = new UsuarioPerfil(name, currentRole, "", 0);
        Color avatarBg = new Color(tempProfile.getAvatarColorHex());
        lblAvatarPreview.setIcon(Iconos.avatar(tempProfile.getInitials(), 60, avatarBg, Tema.TEXT_PRIMARY));
    }

    private JPanel createModeSelector() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        String[] options = {"Crear Sala (Servidor)", "Unirme a Sala (Cliente)"};
        segmentedControl = new ControlSegmentado(options, (selectedIndex, item) -> {
            currentRole = (selectedIndex == 0) ? RolConexion.HOST : RolConexion.CLIENT;
            panelHostView.setVisible(currentRole == RolConexion.HOST);
            panelClientView.setVisible(currentRole == RolConexion.CLIENT);
            pingBadge.setIdle();
            revalidate();
            repaint();
        });

        panel.add(segmentedControl, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createNetworkSettingsPanel() {
        JPanel container = new JPanel(new CardLayout());
        container.setOpaque(false);

        // --- Vista de Anfitrión ---
        panelHostView = new JPanel();
        panelHostView.setOpaque(false);
        panelHostView.setLayout(new BoxLayout(panelHostView, BoxLayout.Y_AXIS));

        String localIp = UtilidadesRed.getPrimaryLocalIp();

        // Tarjeta informativa de IP con botón Copiar
        JPanel ipRow = new JPanel(new BorderLayout(10, 0));
        ipRow.setOpaque(true);
        ipRow.setBackground(Tema.BG_INPUT);
        ipRow.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel lblIpInfo = new JLabel("Tu IP: " + localIp);
        lblIpInfo.setFont(Tema.FONT_BODY_BOLD);
        lblIpInfo.setForeground(Tema.TEXT_ACCENT);

        BotonModerno btnCopy = new BotonModerno("Copiar", Iconos.copy(14, Tema.TEXT_PRIMARY), BotonModerno.Variant.GHOST);
        btnCopy.setMargin(new Insets(3, 8, 3, 8));
        btnCopy.setFont(Tema.FONT_CAPTION);
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(localIp), null);
            btnCopy.setText("¡Copiada!");
            Timer t = new Timer(1500, ev -> btnCopy.setText("Copiar"));
            t.setRepeats(false);
            t.start();
        });

        ipRow.add(lblIpInfo, BorderLayout.CENTER);
        ipRow.add(btnCopy, BorderLayout.EAST);

        panelHostView.add(ipRow);
        panelHostView.add(Box.createVerticalStrut(10));

        JPanel portRowHost = new JPanel(new BorderLayout(8, 4));
        portRowHost.setOpaque(false);
        JLabel lblPortHost = new JLabel("Puerto de escucha multicliente:");
        lblPortHost.setFont(Tema.FONT_CAPTION);
        lblPortHost.setForeground(Tema.TEXT_SECONDARY);
        txtHostPort = new CampoTextoModerno("5000", 6);
        txtHostPort.setText("5000");
        portRowHost.add(lblPortHost, BorderLayout.NORTH);
        portRowHost.add(txtHostPort, BorderLayout.CENTER);
        panelHostView.add(portRowHost);

        // --- Vista de Cliente ---
        panelClientView = new JPanel();
        panelClientView.setOpaque(false);
        panelClientView.setLayout(new BoxLayout(panelClientView, BoxLayout.Y_AXIS));
        panelClientView.setVisible(false);

        JPanel hostIpRow = new JPanel(new BorderLayout(8, 4));
        hostIpRow.setOpaque(false);
        JLabel lblTargetIp = new JLabel("Dirección IP del Servidor:");
        lblTargetIp.setFont(Tema.FONT_CAPTION);
        lblTargetIp.setForeground(Tema.TEXT_SECONDARY);
        txtClientHostIp = new CampoTextoModerno("192.168.1.X o 127.0.0.1", 15);
        txtClientHostIp.setText("127.0.0.1");
        hostIpRow.add(lblTargetIp, BorderLayout.NORTH);
        hostIpRow.add(txtClientHostIp, BorderLayout.CENTER);
        panelClientView.add(hostIpRow);

        panelClientView.add(Box.createVerticalStrut(8));

        JPanel portRowClient = new JPanel(new BorderLayout(8, 4));
        portRowClient.setOpaque(false);
        JLabel lblPortClient = new JLabel("Puerto del Servidor:");
        lblPortClient.setFont(Tema.FONT_CAPTION);
        lblPortClient.setForeground(Tema.TEXT_SECONDARY);
        txtClientPort = new CampoTextoModerno("5000", 6);
        txtClientPort.setText("5000");
        portRowClient.add(lblPortClient, BorderLayout.NORTH);
        portRowClient.add(txtClientPort, BorderLayout.CENTER);
        panelClientView.add(portRowClient);

        panelClientView.add(Box.createVerticalStrut(10));

        // Botón interactivo de Probar Ping
        JPanel pingActionRow = new JPanel(new BorderLayout(8, 4));
        pingActionRow.setOpaque(false);

        btnTestPing = new BotonModerno("Probar Conexión (Ping)", Iconos.ping(14, Tema.TEXT_PRIMARY), BotonModerno.Variant.SECONDARY);
        btnTestPing.setMargin(new Insets(6, 12, 6, 12));
        btnTestPing.setFont(Tema.FONT_CAPTION);
        btnTestPing.addActionListener(e -> executePingTest());

        pingBadge = new InsigniaResultadoPing();
        pingBadge.setIdle();

        pingActionRow.add(btnTestPing, BorderLayout.WEST);
        pingActionRow.add(pingBadge, BorderLayout.CENTER);
        panelClientView.add(pingActionRow);

        // Contenedor principal de ambos
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(panelHostView, BorderLayout.NORTH);
        wrapper.add(panelClientView, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createSubmitButton() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        BotonModerno btnSubmit = new BotonModerno(
                "Entrar al Chat",
                Iconos.send(16, Tema.TEXT_PRIMARY),
                BotonModerno.Variant.PRIMARY
        );
        btnSubmit.setPreferredSize(new Dimension(380, 44));
        btnSubmit.setFont(Tema.FONT_BODY_BOLD);
        btnSubmit.addActionListener(e -> handleSubmit());

        panel.add(btnSubmit, BorderLayout.CENTER);
        return panel;
    }

    private void executePingTest() {
        String host = txtClientHostIp.getText().trim();
        String portStr = txtClientPort.getText().trim();

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (!UtilidadesRed.isValidPort(port)) {
                JOptionPane.showMessageDialog(this, "Puerto inválido (1024 - 65535)", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Por favor introduce un número de puerto válido", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!UtilidadesRed.isValidHost(host)) {
            JOptionPane.showMessageDialog(this, "La dirección IP no es válida", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnTestPing.setEnabled(false);
        pingBadge.setTesting();

        ProbadorPing.testPingAsync(host, port, 3000, result -> {
            btnTestPing.setEnabled(true);
            pingBadge.setResult(result);
        });
    }

    private void handleSubmit() {
        String username = txtUsername.getText().trim();
        if (username.length() < 2) {
            JOptionPane.showMessageDialog(this, "El nombre de usuario debe tener al menos 2 caracteres.", "Nombre Requerido", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocusInWindow();
            return;
        }

        int port;
        String hostIp = "127.0.0.1";

        if (currentRole == RolConexion.HOST) {
            try {
                port = Integer.parseInt(txtHostPort.getText().trim());
                if (!UtilidadesRed.isValidPort(port)) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Puerto de escucha inválido.", "Dato Inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            hostIp = UtilidadesRed.getPrimaryLocalIp();
        } else {
            try {
                port = Integer.parseInt(txtClientPort.getText().trim());
                if (!UtilidadesRed.isValidPort(port)) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Puerto de conexión inválido.", "Dato Inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            hostIp = txtClientHostIp.getText().trim();
            if (!UtilidadesRed.isValidHost(hostIp)) {
                JOptionPane.showMessageDialog(this, "Dirección IP de servidor no válida.", "Dato Inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        UsuarioPerfil userProfile = new UsuarioPerfil(username, currentRole, hostIp, port);

        if (callback != null) {
            callback.onLoginSuccess(userProfile, hostIp, port);
        }
    }
}

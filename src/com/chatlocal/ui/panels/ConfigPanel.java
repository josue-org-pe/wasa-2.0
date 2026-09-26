package com.chatlocal.ui.panels;

import com.chatlocal.backend.model.ConnectionRole;
import com.chatlocal.backend.service.NetworkUtils;
import com.chatlocal.ui.components.ModernButton;
import com.chatlocal.ui.components.ModernCard;
import com.chatlocal.ui.components.ModernTextField;
import com.chatlocal.ui.components.RoleCard;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Pantalla inicial de configuración de conexión y selección de rol.
 */
public class ConfigPanel extends JPanel {

    public interface ConfigCallback {
        void onStartConnection(ConnectionRole role, String hostIp, int port);
    }

    private final ConfigCallback callback;

    private RoleCard cardHost;
    private RoleCard cardClient;
    private ConnectionRole selectedRole = ConnectionRole.HOST;

    private ModernTextField txtHostIp;
    private ModernTextField txtPort;
    private JLabel lblIpHelp;
    private JPanel panelClientFields;

    public ConfigPanel(ConfigCallback callback) {
        this.callback = callback;
        setOpaque(true);
        setBackground(Theme.BG_DARK);
        setLayout(new BorderLayout());

        buildUI();
    }

    private void buildUI() {
        // Contenedor centrado con ancho restringido para diseño profesional
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel mainContainer = new JPanel();
        mainContainer.setOpaque(false);
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setPreferredSize(new Dimension(540, 520));

        // 1. Cabecera / Hero
        mainContainer.add(createHeaderPanel());
        mainContainer.add(Box.createVerticalStrut(16));

        // 2. IP Local Card con botón de copiar
        mainContainer.add(createLocalIpCard());
        mainContainer.add(Box.createVerticalStrut(18));

        // 3. Selector de Roles (Tarjetas interactivas)
        mainContainer.add(createRolesPanel());
        mainContainer.add(Box.createVerticalStrut(18));

        // 4. Parámetros de Conexión (IP remota y Puerto)
        mainContainer.add(createParametersCard());
        mainContainer.add(Box.createVerticalStrut(20));

        // 5. Botón de Iniciar
        mainContainer.add(createActionPanel());

        centerWrapper.add(mainContainer);
        add(centerWrapper, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("wasa 3.0");
        title.setFont(Theme.FONT_HERO);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Comunicación directa en red local");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(Theme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
        panel.add(subtitle);
        return panel;
    }

    private JPanel createLocalIpCard() {
        ModernCard card = new ModernCard(new BorderLayout(12, 0));
        card.setCornerRadius(12);
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        String localIp = NetworkUtils.getPrimaryLocalIp();

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        left.setOpaque(false);

        JLabel lblTag = new JLabel("Tu dirección IP local:");
        lblTag.setFont(Theme.FONT_BODY);
        lblTag.setForeground(Theme.TEXT_MUTED);

        JLabel lblIp = new JLabel(localIp);
        lblIp.setFont(Theme.FONT_BODY_BOLD);
        lblIp.setForeground(Theme.TEXT_ACCENT);

        left.add(lblTag);
        left.add(lblIp);

        ModernButton btnCopy = new ModernButton("Copiar", Icons.copy(14, Theme.TEXT_PRIMARY), ModernButton.Variant.GHOST);
        btnCopy.setMargin(new Insets(4, 10, 4, 10));
        btnCopy.setFont(Theme.FONT_CAPTION);
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(localIp), null);
            btnCopy.setText("¡Copiado!");
            Timer timer = new Timer(1500, ev -> btnCopy.setText("Copiar"));
            timer.setRepeats(false);
            timer.start();
        });

        card.add(left, BorderLayout.CENTER);
        card.add(btnCopy, BorderLayout.EAST);
        return card;
    }

    private JPanel createRolesPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 14, 0));
        panel.setOpaque(false);

        cardHost = new RoleCard(
                ConnectionRole.HOST,
                "Ser Anfitrión",
                "Esperar conexiones entrantes en esta computadora",
                Icons.host(32, Theme.PRIMARY)
        );
        cardHost.setSelected(true);

        cardClient = new RoleCard(
                ConnectionRole.CLIENT,
                "Conectarme",
                "Unirme a una sesión creada en otra computadora",
                Icons.client(32, Theme.PRIMARY)
        );

        cardHost.setOnSelectCallback(() -> {
            cardClient.setSelected(false);
            selectedRole = ConnectionRole.HOST;
            updateFieldVisibility();
        });

        cardClient.setOnSelectCallback(() -> {
            cardHost.setSelected(false);
            selectedRole = ConnectionRole.CLIENT;
            updateFieldVisibility();
        });

        panel.add(cardHost);
        panel.add(cardClient);
        return panel;
    }

    private JPanel createParametersCard() {
        ModernCard card = new ModernCard(new BorderLayout(12, 10));
        card.setCornerRadius(12);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 1: IP del Anfitrión (solo visible/habilitada para clientes)
        panelClientFields = new JPanel(new BorderLayout(8, 4));
        panelClientFields.setOpaque(false);

        JLabel lblIp = new JLabel("IP del Anfitrión al que deseas conectarte:");
        lblIp.setFont(Theme.FONT_BODY_BOLD);
        lblIp.setForeground(Theme.TEXT_PRIMARY);

        txtHostIp = new ModernTextField("192.168.1.X o 127.0.0.1", 16);
        txtHostIp.setText("127.0.0.1");

        panelClientFields.add(lblIp, BorderLayout.NORTH);
        panelClientFields.add(txtHostIp, BorderLayout.CENTER);

        // Mensaje de ayuda para anfitrión
        lblIpHelp = new JLabel("Como anfitrión, solo necesitas compartir tu IP local para que otros se conecten.");
        lblIpHelp.setFont(Theme.FONT_SUBTITLE);
        lblIpHelp.setForeground(Theme.TEXT_MUTED);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        form.add(panelClientFields, gbc);
        form.add(lblIpHelp, gbc);

        // Fila 2: Puerto
        JPanel panelPort = new JPanel(new BorderLayout(8, 4));
        panelPort.setOpaque(false);

        JLabel lblPort = new JLabel("Puerto de comunicación TCP:");
        lblPort.setFont(Theme.FONT_BODY_BOLD);
        lblPort.setForeground(Theme.TEXT_PRIMARY);

        txtPort = new ModernTextField("5000", 6);
        txtPort.setText("5000");

        panelPort.add(lblPort, BorderLayout.NORTH);
        panelPort.add(txtPort, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        form.add(panelPort, gbc);

        card.add(form, BorderLayout.CENTER);

        updateFieldVisibility();
        return card;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        ModernButton btnStart = new ModernButton(
                "Iniciar Conexión",
                Icons.send(16, Theme.TEXT_PRIMARY),
                ModernButton.Variant.PRIMARY
        );
        btnStart.setPreferredSize(new Dimension(240, 44));
        btnStart.setFont(Theme.FONT_BODY_BOLD);
        btnStart.addActionListener(e -> handleStartAction());

        panel.add(btnStart);
        return panel;
    }

    private void updateFieldVisibility() {
        boolean isClient = (selectedRole == ConnectionRole.CLIENT);
        panelClientFields.setVisible(isClient);
        lblIpHelp.setVisible(!isClient);
        revalidate();
        repaint();
    }

    private void handleStartAction() {
        String portStr = txtPort.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portStr);
            if (!NetworkUtils.isValidPort(port)) {
                showWarningDialog("El puerto debe encontrarse entre 1024 y 65535.");
                return;
            }
        } catch (NumberFormatException e) {
            showWarningDialog("Por favor ingresa un número de puerto válido.");
            return;
        }

        String hostIp = txtHostIp.getText().trim();
        if (selectedRole == ConnectionRole.CLIENT) {
            if (!NetworkUtils.isValidHost(hostIp)) {
                showWarningDialog("Por favor ingresa una dirección IP válida para el anfitrión.");
                return;
            }
        }

        if (callback != null) {
            callback.onStartConnection(selectedRole, hostIp, port);
        }
    }

    private void showWarningDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Dato inválido", JOptionPane.WARNING_MESSAGE);
    }
}

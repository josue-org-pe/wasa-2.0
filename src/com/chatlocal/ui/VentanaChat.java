package com.chatlocal.ui;

import com.chatlocal.backend.event.ConexionDeEscucha;
import com.chatlocal.backend.event.MensajeDeEscucha;
import com.chatlocal.backend.model.MensajeChat;
import com.chatlocal.backend.model.RolConexion;
import com.chatlocal.backend.model.EstadoConexion;
import com.chatlocal.backend.service.ServicioChat;
import com.chatlocal.backend.service.ServicioChatImpl;
import com.chatlocal.backend.service.videocall.EstadoLlamada;
import com.chatlocal.backend.service.videocall.OyenteVideollamada;
import com.chatlocal.ui.dialogs.videocall.DialogoLlamadaEntrante;
import com.chatlocal.ui.dialogs.videocall.DialogoVideollamada;
import com.chatlocal.ui.panels.PanelConectando;
import com.chatlocal.ui.panels.PanelLogin;
import com.chatlocal.ui.panels.PanelMensajeroPrincipal;
import com.chatlocal.ui.theme.Tema;
import com.chatlocal.ui.theme.GestorTema;

import java.awt.image.BufferedImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// ventana principal de wasa
public class VentanaChat extends JFrame implements ConexionDeEscucha, MensajeDeEscucha, OyenteVideollamada {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(cardLayout);

    private final ServicioChat chatService;

    private PanelLogin loginPanel;
    private PanelConectando connectingPanel;
    private PanelMensajeroPrincipal messengerPanel;
    private DialogoVideollamada activeVideoDialog;

    public VentanaChat() {
        super("wasa 3.0");
        Tema.applyAppIcon(this);
        this.chatService = new ServicioChatImpl();
        this.chatService.addConnectionListener(this);
        this.chatService.addMessageListener(this);
        this.chatService.getVideoCallService().addListener(this);

        initWindow();
        initPanels();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                chatService.disconnect();
                System.exit(0);
            }
        });

        // Escuchar cambios de tema para repintar la ventana
        GestorTema.addThemeListener(() -> {
            getContentPane().setBackground(GestorTema.getTheme().bgDark);
            revalidate();
            repaint();
        });
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(880, 640);
        setMinimumSize(new Dimension(720, 540));
        setLocationRelativeTo(null);
        getContentPane().setBackground(GestorTema.getTheme().bgDark);
    }

    private void initPanels() {
        // Vista 1: Login con Ping-Pong y Personalización
        loginPanel = new PanelLogin((userProfile, hostIp, port) -> {
            chatService.setLocalUserProfile(userProfile);
            cardLayout.show(rootPanel, "connecting");
            connectingPanel.start();

            if (userProfile.getRole() == RolConexion.HOST) {
                connectingPanel.setStatus(
                        "Iniciando Sala de Chat...",
                        "Abriendo socket servidor multicliente en puerto " + port + "...",
                        "Comparte tu IP local para que otros se unan"
                );
                chatService.startHost(port);
            } else {
                connectingPanel.setStatus(
                        "Conectando a la Sala...",
                        "Estableciendo socket con " + hostIp + ":" + port + "...",
                        "Destino: " + hostIp + ":" + port
                );
                chatService.connectToHost(hostIp, port);
            }
        });

        // Vista 2: Conexión / Espera
        connectingPanel = new PanelConectando(() -> {
            chatService.disconnect();
            connectingPanel.stop();
            cardLayout.show(rootPanel, "login");
        });

        // Vista 3: Mensajero Maestro (Sidebar + Chat Area)
        messengerPanel = new PanelMensajeroPrincipal(this, chatService, () -> {
            chatService.disconnect();
            cardLayout.show(rootPanel, "login");
        });

        rootPanel.add(loginPanel, "login");
        rootPanel.add(connectingPanel, "connecting");
        rootPanel.add(messengerPanel, "messenger");

        add(rootPanel);
        cardLayout.show(rootPanel, "login");
    }

    // --- Implementación de ConexionDeEscucha ---

    @Override
    public void onConnectionStateChanged(EstadoConexion newState, String details) {
        switch (newState) {
            case LISTENING -> connectingPanel.setStatus(
                    "Sala activa — Esperando clientes...",
                    details,
                    "Puerto local: " + chatService.getActivePort()
            );
            case CONNECTING -> connectingPanel.setStatus(
                    "Conectando al servidor...",
                    details,
                    chatService.getPeerAddress() + ":" + chatService.getActivePort()
            );
            case VERIFYING -> connectingPanel.setStatus(
                    "Verificando comunicación...",
                    details,
                    "Canal de control Ping / Pong en curso"
            );
            case CONNECTED -> {
                connectingPanel.stop();
                messengerPanel.clearMessages();
                messengerPanel.addMessage(MensajeChat.createSystemMessage("¡Conexión verificada con éxito! Ya puedes chatear, enviar audios y stickers."));
                messengerPanel.refreshSidebar();
                cardLayout.show(rootPanel, "messenger");
            }
            case DISCONNECTED -> {
                connectingPanel.stop();
                cardLayout.show(rootPanel, "login");
                if (details != null && !details.isEmpty() && !details.equals("Desconectado.")) {
                    JOptionPane.showMessageDialog(this, details, "Conexión finalizada", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            case ERROR -> {
                connectingPanel.stop();
                cardLayout.show(rootPanel, "login");
            }
        }
    }

    @Override
    public void onConnectionError(String errorMessage) {
        connectingPanel.stop();
        cardLayout.show(rootPanel, "login");
        JOptionPane.showMessageDialog(this, errorMessage, "Error de red", JOptionPane.ERROR_MESSAGE);
    }

    // --- Implementación de MensajeDeEscucha ---

    @Override
    public void onMessageReceived(MensajeChat message) {
        messengerPanel.addMessage(message);
        messengerPanel.refreshSidebar();
    }

    @Override
    public void onFileReceived(MensajeChat message) {
        messengerPanel.addMessage(message);
        messengerPanel.refreshSidebar();
    }

    @Override
    public void onAudioReceived(MensajeChat message) {
        messengerPanel.addMessage(message);
        messengerPanel.refreshSidebar();
    }

    @Override
    public void onMessageSent(MensajeChat message) {
        messengerPanel.addMessage(message);
    }

    @Override
    public void onMessageStatusChanged(String messageId, com.chatlocal.backend.model.EstadoMensaje status) {
        messengerPanel.updateMessageStatus(messageId, status);
    }

    // --- Implementación de OyenteVideollamada ---

    private void openVideoCallDialog(String peerName) {
        if (activeVideoDialog != null && activeVideoDialog.isShowing()) return;
        activeVideoDialog = new DialogoVideollamada(this, chatService.getVideoCallService(), peerName);
        activeVideoDialog.setVisible(true);
    }

    @Override
    public void onCallStateChanged(EstadoLlamada newState, String peerName, String message) {
        SwingUtilities.invokeLater(() -> {
            if (newState == EstadoLlamada.OUTGOING_CALL) {
                openVideoCallDialog(peerName);
            } else if (newState == EstadoLlamada.ENDED) {
                if (activeVideoDialog != null) {
                    activeVideoDialog.dispose();
                    activeVideoDialog = null;
                }
                if (message != null && !message.isEmpty()) {
                    messengerPanel.addMessage(MensajeChat.createSystemMessage("📹 " + message));
                }
            }
        });
    }

    @Override
    public void onIncomingCallReceived(String callerName, String peerIp, int mediaPort) {
        SwingUtilities.invokeLater(() -> {
            DialogoLlamadaEntrante dialog = new DialogoLlamadaEntrante(this, chatService.getVideoCallService(), callerName, () -> {
                openVideoCallDialog(callerName);
            });
            dialog.setVisible(true);
        });
    }

    @Override
    public void onLocalFrameAvailable(BufferedImage frame) {
        // Manejado directamente por DialogoVideollamada
    }

    @Override
    public void onRemoteFrameAvailable(BufferedImage frame) {
        // Manejado directamente por DialogoVideollamada
    }

    @Override
    public void onAudioLevelsUpdated(float localLevel, float remoteLevel) {
        // Manejado directamente por DialogoVideollamada
    }
}

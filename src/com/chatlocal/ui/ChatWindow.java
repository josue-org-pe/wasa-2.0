package com.chatlocal.ui;

import com.chatlocal.backend.event.ConnectionListener;
import com.chatlocal.backend.event.MessageListener;
import com.chatlocal.backend.model.ChatMessage;
import com.chatlocal.backend.model.ConnectionRole;
import com.chatlocal.backend.model.ConnectionState;
import com.chatlocal.backend.service.ChatService;
import com.chatlocal.backend.service.ChatServiceImpl;
import com.chatlocal.backend.service.videocall.CallState;
import com.chatlocal.backend.service.videocall.VideoCallListener;
import com.chatlocal.ui.dialogs.videocall.IncomingCallDialog;
import com.chatlocal.ui.dialogs.videocall.VideoCallDialog;
import com.chatlocal.ui.panels.ConnectingPanel;
import com.chatlocal.ui.panels.LoginPanel;
import com.chatlocal.ui.panels.MainMessengerPanel;
import com.chatlocal.ui.theme.Theme;
import com.chatlocal.ui.theme.ThemeManager;

import java.awt.image.BufferedImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// ventana principal de wasa
public class ChatWindow extends JFrame implements ConnectionListener, MessageListener, VideoCallListener {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(cardLayout);

    private final ChatService chatService;

    private LoginPanel loginPanel;
    private ConnectingPanel connectingPanel;
    private MainMessengerPanel messengerPanel;
    private VideoCallDialog activeVideoDialog;

    public ChatWindow() {
        super("wasa 3.0");
        Theme.applyAppIcon(this);
        this.chatService = new ChatServiceImpl();
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
        ThemeManager.addThemeListener(() -> {
            getContentPane().setBackground(ThemeManager.getTheme().bgDark);
            revalidate();
            repaint();
        });
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(880, 640);
        setMinimumSize(new Dimension(720, 540));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeManager.getTheme().bgDark);
    }

    private void initPanels() {
        // Vista 1: Login con Ping-Pong y Personalización
        loginPanel = new LoginPanel((userProfile, hostIp, port) -> {
            chatService.setLocalUserProfile(userProfile);
            cardLayout.show(rootPanel, "connecting");
            connectingPanel.start();

            if (userProfile.getRole() == ConnectionRole.HOST) {
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
        connectingPanel = new ConnectingPanel(() -> {
            chatService.disconnect();
            connectingPanel.stop();
            cardLayout.show(rootPanel, "login");
        });

        // Vista 3: Mensajero Maestro (Sidebar + Chat Area)
        messengerPanel = new MainMessengerPanel(this, chatService, () -> {
            chatService.disconnect();
            cardLayout.show(rootPanel, "login");
        });

        rootPanel.add(loginPanel, "login");
        rootPanel.add(connectingPanel, "connecting");
        rootPanel.add(messengerPanel, "messenger");

        add(rootPanel);
        cardLayout.show(rootPanel, "login");
    }

    // --- Implementación de ConnectionListener ---

    @Override
    public void onConnectionStateChanged(ConnectionState newState, String details) {
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
                messengerPanel.addMessage(ChatMessage.createSystemMessage("¡Conexión verificada con éxito! Ya puedes chatear, enviar audios y stickers."));
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

    // --- Implementación de MessageListener ---

    @Override
    public void onMessageReceived(ChatMessage message) {
        messengerPanel.addMessage(message);
        messengerPanel.refreshSidebar();
    }

    @Override
    public void onFileReceived(ChatMessage message) {
        messengerPanel.addMessage(message);
        messengerPanel.refreshSidebar();
    }

    @Override
    public void onAudioReceived(ChatMessage message) {
        messengerPanel.addMessage(message);
        messengerPanel.refreshSidebar();
    }

    @Override
    public void onMessageSent(ChatMessage message) {
        messengerPanel.addMessage(message);
    }

    @Override
    public void onMessageStatusChanged(String messageId, com.chatlocal.backend.model.MessageStatus status) {
        messengerPanel.updateMessageStatus(messageId, status);
    }

    // --- Implementación de VideoCallListener ---

    private void openVideoCallDialog(String peerName) {
        if (activeVideoDialog != null && activeVideoDialog.isShowing()) return;
        activeVideoDialog = new VideoCallDialog(this, chatService.getVideoCallService(), peerName);
        activeVideoDialog.setVisible(true);
    }

    @Override
    public void onCallStateChanged(CallState newState, String peerName, String message) {
        SwingUtilities.invokeLater(() -> {
            if (newState == CallState.OUTGOING_CALL) {
                openVideoCallDialog(peerName);
            } else if (newState == CallState.ENDED) {
                if (activeVideoDialog != null) {
                    activeVideoDialog.dispose();
                    activeVideoDialog = null;
                }
                if (message != null && !message.isEmpty()) {
                    messengerPanel.addMessage(ChatMessage.createSystemMessage("📹 " + message));
                }
            }
        });
    }

    @Override
    public void onIncomingCallReceived(String callerName, String peerIp, int mediaPort) {
        SwingUtilities.invokeLater(() -> {
            IncomingCallDialog dialog = new IncomingCallDialog(this, chatService.getVideoCallService(), callerName, () -> {
                openVideoCallDialog(callerName);
            });
            dialog.setVisible(true);
        });
    }

    @Override
    public void onLocalFrameAvailable(BufferedImage frame) {
        // Manejado directamente por VideoCallDialog
    }

    @Override
    public void onRemoteFrameAvailable(BufferedImage frame) {
        // Manejado directamente por VideoCallDialog
    }

    @Override
    public void onAudioLevelsUpdated(float localLevel, float remoteLevel) {
        // Manejado directamente por VideoCallDialog
    }
}

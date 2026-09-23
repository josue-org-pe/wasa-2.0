package com.chatlocal.frontend.view;

import com.chatlocal.frontend.theme.ThemeColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana principal de la aplicación.
 * Administra el intercambio de pantallas mediante CardLayout.
 */
public class MainFrame extends JFrame {

    public static final String VIEW_CONFIG = "CONFIG";
    public static final String VIEW_STATUS = "STATUS";
    public static final String VIEW_CHAT = "CHAT";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel container = new JPanel(cardLayout);

    private final ConfigPanel configPanel;
    private final StatusPanel statusPanel;
    private final ChatPanel chatPanel;

    public interface WindowCloseCallback {
        void onClosing();
    }

    public MainFrame(ConfigPanel configPanel, StatusPanel statusPanel, ChatPanel chatPanel, WindowCloseCallback closeCallback) {
        super("Chat Local — Sockets Java P2P");
        this.configPanel = configPanel;
        this.statusPanel = statusPanel;
        this.chatPanel = chatPanel;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 600);
        setMinimumSize(new Dimension(620, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeColors.BG_DARK);

        container.setOpaque(false);
        container.add(configPanel, VIEW_CONFIG);
        container.add(statusPanel, VIEW_STATUS);
        container.add(chatPanel, VIEW_CHAT);

        add(container);
        showView(VIEW_CONFIG);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (closeCallback != null) {
                    closeCallback.onClosing();
                }
            }
        });
    }

    public void showView(String viewName) {
        SwingUtilities.invokeLater(() -> cardLayout.show(container, viewName));
    }

    public ConfigPanel getConfigPanel() {
        return configPanel;
    }

    public StatusPanel getStatusPanel() {
        return statusPanel;
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }
}

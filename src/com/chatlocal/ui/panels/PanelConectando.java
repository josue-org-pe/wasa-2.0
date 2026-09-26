package com.chatlocal.ui.panels;

import com.chatlocal.ui.components.BotonModerno;
import com.chatlocal.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

// pantalla de espera mientras se conecta el socket y hace ping
public class PanelConectando extends JPanel {

    public interface CancelCallback {
        void onCancelRequested();
    }

    private final CancelCallback cancelCallback;

    private JLabel lblStatusTitle;
    private JLabel lblStatusDetails;
    private JLabel lblEndpointInfo;

    private Timer animationTimer;
    private float pulsePhase = 0.0f;

    public PanelConectando(CancelCallback cancelCallback) {
        this.cancelCallback = cancelCallback;

        setOpaque(true);
        setBackground(Tema.BG_DARK);
        setLayout(new BorderLayout());

        buildUI();
        initAnimation();
    }

    private void buildUI() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setPreferredSize(new Dimension(460, 440));

        // 1. Área de animación de pulso
        JPanel pulseCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Tema.enableQualityRendering(g2);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // Dibujar 3 anillos concéntricos expandiéndose
                for (int i = 0; i < 3; i++) {
                    float ringPhase = (pulsePhase + i * 0.33f) % 1.0f;
                    float radius = 25.0f + ringPhase * 55.0f;
                    int alpha = (int) ((1.0f - ringPhase) * 160.0f);

                    g2.setColor(new Color(Tema.PRIMARY.getRed(), Tema.PRIMARY.getGreen(), Tema.PRIMARY.getBlue(), Math.max(0, alpha)));
                    g2.setStroke(new BasicStroke(2.0f + (1.0f - ringPhase) * 2.0f));
                    g2.drawOval((int) (cx - radius), (int) (cy - radius), (int) (radius * 2), (int) (radius * 2));
                }

                // Círculo central luminoso
                g2.setColor(Tema.PRIMARY);
                g2.fillOval(cx - 20, cy - 20, 40, 40);

                // Brillo interno
                g2.setColor(new Color(255, 255, 255, 220));
                g2.fillOval(cx - 8, cy - 8, 16, 16);

                g2.dispose();
            }
        };
        pulseCanvas.setOpaque(false);
        pulseCanvas.setPreferredSize(new Dimension(200, 160));
        pulseCanvas.setMaximumSize(new Dimension(200, 160));
        pulseCanvas.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(pulseCanvas);

        content.add(Box.createVerticalStrut(20));

        // 2. Título de estado
        lblStatusTitle = new JLabel("Estableciendo conexión...");
        lblStatusTitle.setFont(Tema.FONT_HERO);
        lblStatusTitle.setForeground(Tema.TEXT_PRIMARY);
        lblStatusTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblStatusTitle);

        content.add(Box.createVerticalStrut(8));

        // 3. Detalles de progreso
        lblStatusDetails = new JLabel("Iniciando sockets y preparando protocolo...");
        lblStatusDetails.setFont(Tema.FONT_SUBTITLE);
        lblStatusDetails.setForeground(Tema.TEXT_SECONDARY);
        lblStatusDetails.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblStatusDetails);

        content.add(Box.createVerticalStrut(6));

        // 4. Info de Endpoint
        lblEndpointInfo = new JLabel("");
        lblEndpointInfo.setFont(Tema.FONT_CAPTION);
        lblEndpointInfo.setForeground(Tema.TEXT_MUTED);
        lblEndpointInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblEndpointInfo);

        content.add(Box.createVerticalStrut(28));

        // 5. Botón de cancelar
        BotonModerno btnCancel = new BotonModerno("Cancelar", BotonModerno.Variant.SECONDARY);
        btnCancel.setPreferredSize(new Dimension(140, 38));
        btnCancel.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCancel.addActionListener(e -> {
            if (cancelCallback != null) {
                cancelCallback.onCancelRequested();
            }
        });
        content.add(btnCancel);

        center.add(content);
        add(center, BorderLayout.CENTER);
    }

    private void initAnimation() {
        animationTimer = new Timer(30, e -> {
            pulsePhase = (pulsePhase + 0.02f) % 1.0f;
            repaint();
        });
    }

    public void start() {
        if (animationTimer != null && !animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    public void stop() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }

    public void setStatus(String title, String details, String endpointInfo) {
        lblStatusTitle.setText(title);
        lblStatusDetails.setText(details);
        if (endpointInfo != null) {
            lblEndpointInfo.setText(endpointInfo);
        }
    }
}

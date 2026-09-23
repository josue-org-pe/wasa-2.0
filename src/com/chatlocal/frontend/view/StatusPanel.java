package com.chatlocal.frontend.view;

import com.chatlocal.backend.model.ConnectionState;
import com.chatlocal.frontend.components.ModernButton;
import com.chatlocal.frontend.components.ModernCard;
import com.chatlocal.frontend.theme.ThemeColors;
import com.chatlocal.frontend.theme.ThemeFonts;
import com.chatlocal.frontend.theme.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Pantalla intermedia de estado: muestra el progreso de establecimiento de conexión,
 * verificación con ping/pong y mensajes de error con opción para cancelar o volver.
 */
public class StatusPanel extends JPanel {

    public interface StatusActionCallback {
        void onCancelOrBackRequested();
    }

    private StatusActionCallback callback;

    private final JLabel stateTitleLabel = new JLabel("Estableciendo conexión...");
    private final JLabel stateDetailsLabel = new JLabel("Por favor espera...");
    private final ModernButton actionButton = new ModernButton("Cancelar", ModernButton.Type.SECONDARY);
    private final RadarPulseComponent pulseComponent = new RadarPulseComponent();

    public StatusPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeColors.BG_DARK);
        setBorder(new EmptyBorder(32, 40, 32, 40));

        buildUI();
    }

    public void setActionCallback(StatusActionCallback callback) {
        this.callback = callback;
    }

    private void buildUI() {
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        ModernCard card = new ModernCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(32, 40, 32, 40));
        card.setMaximumSize(new Dimension(500, 340));
        card.setPreferredSize(new Dimension(480, 320));

        pulseComponent.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(pulseComponent);
        card.add(Box.createVerticalStrut(20));

        stateTitleLabel.setFont(ThemeFonts.title(16));
        stateTitleLabel.setForeground(ThemeColors.TEXT_PRIMARY);
        stateTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(stateTitleLabel);
        card.add(Box.createVerticalStrut(8));

        stateDetailsLabel.setFont(ThemeFonts.regular(12));
        stateDetailsLabel.setForeground(ThemeColors.TEXT_MUTED);
        stateDetailsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(stateDetailsLabel);
        card.add(Box.createVerticalStrut(28));

        actionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionButton.setPreferredSize(new Dimension(160, 38));
        actionButton.addActionListener(e -> {
            if (callback != null) callback.onCancelOrBackRequested();
        });
        card.add(actionButton);

        centerWrapper.add(card);
        add(centerWrapper, BorderLayout.CENTER);
    }

    public void updateStatus(ConnectionState state, String details) {
        switch (state) {
            case LISTENING -> {
                pulseComponent.setColor(ThemeColors.WARNING);
                pulseComponent.startAnimation();
                stateTitleLabel.setText("Esperando conexión...");
                stateTitleLabel.setForeground(ThemeColors.TEXT_PRIMARY);
                actionButton.setText("Cancelar");
            }
            case CONNECTING -> {
                pulseComponent.setColor(ThemeColors.PRIMARY);
                pulseComponent.startAnimation();
                stateTitleLabel.setText("Conectando con el Host...");
                stateTitleLabel.setForeground(ThemeColors.TEXT_PRIMARY);
                actionButton.setText("Cancelar");
            }
            case VERIFYING -> {
                pulseComponent.setColor(ThemeColors.ACCENT_CYAN);
                pulseComponent.startAnimation();
                stateTitleLabel.setText("Verificando protocolo...");
                stateTitleLabel.setForeground(ThemeColors.TEXT_PRIMARY);
                actionButton.setText("Cancelar");
            }
            case ERROR -> {
                pulseComponent.setColor(ThemeColors.DANGER);
                pulseComponent.stopAnimation();
                stateTitleLabel.setText("No se pudo conectar");
                stateTitleLabel.setForeground(ThemeColors.DANGER);
                actionButton.setText("Volver a Configuración");
            }
            default -> {
                pulseComponent.stopAnimation();
            }
        }
        stateDetailsLabel.setText("<html><center>" + details + "</center></html>");
        revalidate();
        repaint();
    }

    /**
     * Componente visual animado que dibuja ondas suaves de radar.
     */
    private static class RadarPulseComponent extends JComponent {
        private float angle = 0;
        private Color pulseColor = ThemeColors.PRIMARY;
        private final Timer timer;

        public RadarPulseComponent() {
            setPreferredSize(new Dimension(80, 80));
            setMaximumSize(new Dimension(80, 80));
            timer = new Timer(35, e -> {
                angle = (angle + 0.08f) % ((float) Math.PI * 2);
                repaint();
            });
        }

        public void setColor(Color color) {
            this.pulseColor = color;
            repaint();
        }

        public void startAnimation() {
            if (!timer.isRunning()) timer.start();
        }

        public void stopAnimation() {
            if (timer.isRunning()) timer.stop();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UIUtils.applyQualityRendering(g2);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            // Círculo exterior con halo animado
            int baseRadius = 24;
            float pulseScale = (float) (Math.sin(angle) * 0.5 + 0.5);
            int pulseRadius = baseRadius + (int) (pulseScale * 14);

            g2.setColor(new Color(pulseColor.getRed(), pulseColor.getGreen(), pulseColor.getBlue(), 40));
            g2.fillOval(cx - pulseRadius, cy - pulseRadius, pulseRadius * 2, pulseRadius * 2);

            // Círculo central
            g2.setColor(pulseColor);
            g2.fillOval(cx - baseRadius, cy - baseRadius, baseRadius * 2, baseRadius * 2);

            // Centro blanco brillante
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - 8, cy - 8, 16, 16);

            g2.dispose();
        }
    }
}

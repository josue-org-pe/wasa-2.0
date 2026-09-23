package com.chatlocal.frontend.components;

import com.chatlocal.backend.model.ConnectionState;
import com.chatlocal.frontend.theme.ThemeColors;
import com.chatlocal.frontend.theme.ThemeFonts;
import com.chatlocal.frontend.theme.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Píldora visual para indicar el estado de conexión actual con indicador luminoso.
 */
public class StatusBadge extends JPanel {

    private ConnectionState state = ConnectionState.DISCONNECTED;
    private String customText = null;

    public StatusBadge() {
        setOpaque(false);
        setBorder(new EmptyBorder(4, 12, 4, 12));
    }

    public void setState(ConnectionState state) {
        this.state = state;
        this.customText = null;
        repaint();
    }

    public void setState(ConnectionState state, String customText) {
        this.state = state;
        this.customText = customText;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UIUtils.applyQualityRendering(g2);

        // Fondo de la píldora
        Color pillBg = new Color(20, 27, 40, 200);
        g2.setColor(pillBg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.setColor(ThemeColors.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());

        // Color del punto de estado
        Color dotColor;
        switch (state) {
            case CONNECTED -> dotColor = ThemeColors.SUCCESS;
            case LISTENING, CONNECTING, VERIFYING -> dotColor = ThemeColors.WARNING;
            case ERROR -> dotColor = ThemeColors.DANGER;
            default -> dotColor = ThemeColors.TEXT_HINT;
        }

        int dotSize = 8;
        int dotX = 12;
        int dotY = (getHeight() - dotSize) / 2;

        // Halo suave en el punto
        g2.setColor(new Color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), 60));
        g2.fillOval(dotX - 2, dotY - 2, dotSize + 4, dotSize + 4);

        // Punto sólido
        g2.setColor(dotColor);
        g2.fillOval(dotX, dotY, dotSize, dotSize);

        // Texto
        String label = (customText != null) ? customText : state.getEtiqueta();
        g2.setFont(ThemeFonts.bold(11));
        g2.setColor(ThemeColors.TEXT_PRIMARY);
        FontMetrics fm = g2.getFontMetrics();
        int textX = dotX + dotSize + 8;
        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(label, textX, textY);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        String label = (customText != null) ? customText : state.getEtiqueta();
        FontMetrics fm = getFontMetrics(ThemeFonts.bold(11));
        int textWidth = fm.stringWidth(label);
        return new Dimension(textWidth + 40, 26);
    }
}

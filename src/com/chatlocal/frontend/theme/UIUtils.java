package com.chatlocal.frontend.theme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Utilidades gráficas de renderizado suave y personalización de componentes Swing.
 */
public final class UIUtils {

    private UIUtils() {}

    /**
     * Activa el suavizado de bordes de alta fidelidad y renderizado de texto nítido.
     */
    public static void applyQualityRendering(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * Dibuja un rectángulo con esquinas redondeadas y borde opcional.
     */
    public static void paintRoundedPanel(Graphics2D g2, int width, int height, int radius, Color bg, Color border) {
        applyQualityRendering(g2);
        if (bg != null) {
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, width, height, radius, radius);
        }
        if (border != null) {
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, width - 1, height - 1, radius, radius);
        }
    }

    /**
     * Aplica un diseño moderno y minimalista al scrollbar de un JScrollPane.
     */
    public static void customizeScrollBar(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ThemeColors.BG_DARK);
        scrollPane.getViewport().setBackground(ThemeColors.BG_DARK);

        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setPreferredSize(new Dimension(8, 0));
        verticalBar.setUI(new ModernScrollBarUI());

        JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
        horizontalBar.setPreferredSize(new Dimension(0, 8));
        horizontalBar.setUI(new ModernScrollBarUI());
    }

    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(50, 65, 85);
            this.trackColor = ThemeColors.BG_DARK;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            applyQualityRendering(g2);
            g2.setColor(isThumbRollover() ? ThemeColors.PRIMARY : thumbColor);
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 6, 6);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(trackColor);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
    }
}

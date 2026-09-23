package com.chatlocal.ui.components;

import com.chatlocal.ui.theme.Theme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * ScrollBar moderno, minimalista y oscuro sin botones triangulares toscos.
 */
public class ModernScrollBarUI extends BasicScrollBarUI {

    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = new Color(0x38, 0x3D, 0x4E);
        this.trackColor = Theme.BG_PANEL;
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
        button.setVisible(false);
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Theme.BG_PANEL);
        g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.enableQualityRendering(g2);

        Color color = isThumbRollover() ? new Color(0x52, 0x58, 0x6E) : thumbColor;
        g2.setColor(color);

        int arc = 6;
        int pad = 2;
        g2.fillRoundRect(thumbBounds.x + pad, thumbBounds.y + pad,
                thumbBounds.width - 2 * pad, thumbBounds.height - 2 * pad, arc, arc);

        g2.dispose();
    }
}

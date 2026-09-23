package com.chatlocal.frontend.components;

import com.chatlocal.frontend.theme.ThemeColors;
import com.chatlocal.frontend.theme.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Panel contenedor con elevación visual, esquinas suaves y borde moderno.
 */
public class ModernCard extends JPanel {

    private int cornerRadius = 14;
    private Color backgroundColor = ThemeColors.BG_SURFACE;
    private Color borderColor = ThemeColors.BORDER;

    public ModernCard(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public ModernCard() {
        this(new BorderLayout());
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UIUtils.paintRoundedPanel(g2, getWidth(), getHeight(), cornerRadius, backgroundColor, borderColor);
        g2.dispose();
        super.paintComponent(g);
    }
}

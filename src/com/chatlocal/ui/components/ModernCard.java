package com.chatlocal.ui.components;

import com.chatlocal.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;

// tarjeta con bordes redondeados y fondo oscuro
public class ModernCard extends JPanel {

    private int cornerRadius = 14;
    private Color backgroundColor = Theme.BG_CARD;
    private Color borderColor = Theme.BORDER_SUBTLE;

    public ModernCard() {
        this(new BorderLayout());
    }

    public ModernCard(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
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
        Theme.enableQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();

        // Fondo curvo
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        // Borde fino
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}

package com.chatlocal.frontend.components;

import com.chatlocal.frontend.theme.ThemeColors;
import com.chatlocal.frontend.theme.ThemeFonts;
import com.chatlocal.frontend.theme.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Botón moderno con bordes redondeados, transiciones al hover y estilos temáticos.
 */
public class ModernButton extends JButton {

    public enum Type {
        PRIMARY,
        SECONDARY,
        DANGER,
        SUCCESS
    }

    private final Type type;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 10;

    public ModernButton(String text, Type type) {
        super(text);
        this.type = type;
        init();
    }

    public ModernButton(String text) {
        this(text, Type.PRIMARY);
    }

    private void init() {
        setFont(ThemeFonts.bold(13));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(10, 18, 10, 18));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UIUtils.applyQualityRendering(g2);

        Color bgColor;
        Color fgColor = ThemeColors.TEXT_PRIMARY;
        Color borderColor = null;

        if (!isEnabled()) {
            bgColor = new Color(35, 45, 60);
            fgColor = ThemeColors.TEXT_HINT;
        } else {
            switch (type) {
                case PRIMARY -> {
                    if (isPressed) bgColor = ThemeColors.PRIMARY_PRESSED;
                    else if (isHovered) bgColor = ThemeColors.PRIMARY_HOVER;
                    else bgColor = ThemeColors.PRIMARY;
                }
                case SECONDARY -> {
                    if (isPressed) bgColor = new Color(40, 52, 70);
                    else if (isHovered) bgColor = new Color(33, 44, 60);
                    else bgColor = ThemeColors.BG_SURFACE;
                    borderColor = ThemeColors.BORDER;
                }
                case DANGER -> {
                    if (isPressed) bgColor = new Color(185, 28, 28);
                    else if (isHovered) bgColor = ThemeColors.DANGER_HOVER;
                    else bgColor = ThemeColors.DANGER;
                }
                case SUCCESS -> {
                    if (isPressed) bgColor = new Color(5, 150, 105);
                    else if (isHovered) bgColor = new Color(13, 165, 115);
                    else bgColor = ThemeColors.SUCCESS;
                }
                default -> bgColor = ThemeColors.PRIMARY;
            }
        }

        // Fondo
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // Borde opcional
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }

        // Texto
        g2.setFont(getFont());
        g2.setColor(fgColor);
        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width + 24, 80), Math.max(d.height + 12, 38));
    }
}

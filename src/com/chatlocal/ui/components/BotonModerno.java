package com.chatlocal.ui.components;

import com.chatlocal.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// boton con bordes redondeados y colores segun su tipo
public class BotonModerno extends JButton {

    public enum Variant {
        PRIMARY,
        SECONDARY,
        DANGER,
        SUCCESS,
        GHOST
    }

    private final Variant variant;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 10;

    public BotonModerno(String text, Variant variant) {
        this(text, null, variant);
    }

    public BotonModerno(String text, Icon icon, Variant variant) {
        super(text);
        if (icon != null) setIcon(icon);
        this.variant = variant;

        setFont(Tema.FONT_BODY_BOLD);
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
        Tema.enableQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();

        Color bg;
        Color fg;

        if (!isEnabled()) {
            bg = new Color(0x23, 0x25, 0x30);
            fg = Tema.TEXT_MUTED;
        } else {
            switch (variant) {
                case PRIMARY -> {
                    bg = isPressed ? Tema.PRIMARY_PRESSED : (isHovered ? Tema.PRIMARY_HOVER : Tema.PRIMARY);
                    fg = Tema.TEXT_PRIMARY;
                }
                case DANGER -> {
                    bg = isPressed ? new Color(0xB9, 0x1C, 0x1C) : (isHovered ? Tema.DANGER_HOVER : Tema.DANGER);
                    fg = Tema.TEXT_PRIMARY;
                }
                case SUCCESS -> {
                    bg = isPressed ? new Color(0x04, 0x78, 0x57) : (isHovered ? new Color(0x05, 0x96, 0x69) : Tema.SUCCESS);
                    fg = Color.WHITE;
                }
                case GHOST -> {
                    bg = isPressed ? new Color(0x33, 0x37, 0x46) : (isHovered ? new Color(0x26, 0x2A, 0x36) : new Color(0, 0, 0, 0));
                    fg = isHovered ? Tema.TEXT_PRIMARY : Tema.TEXT_SECONDARY;
                }
                default -> { // SECONDARY
                    bg = isPressed ? new Color(0x28, 0x2B, 0x37) : (isHovered ? Tema.SECONDARY_HOVER : Tema.SECONDARY);
                    fg = Tema.TEXT_PRIMARY;
                }
            }
        }

        // Fondo redondeado
        if (bg.getAlpha() > 0) {
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);
        }

        // Borde sutil para botones GHOST o SECONDARY
        if (variant == Variant.GHOST || variant == Variant.SECONDARY) {
            g2.setColor(Tema.BORDER_SUBTLE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
        }

        setForeground(fg);
        g2.dispose();
        super.paintComponent(g);
    }
}

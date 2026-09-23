package com.chatlocal.ui.components;

import com.chatlocal.backend.model.ConnectionRole;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tarjeta interactiva de selección de rol (Host / Client).
 * Proporciona feedback visual inmediato al pasar el ratón y al seleccionarse.
 */
public class RoleCard extends JPanel {

    private final ConnectionRole role;
    private final String title;
    private final String description;
    private final Icon icon;

    private boolean selected = false;
    private boolean hovered = false;
    private Runnable onSelectCallback;

    public RoleCard(ConnectionRole role, String title, String description, Icon icon) {
        this.role = role;
        this.title = title;
        this.description = description;
        this.icon = icon;

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(280, 110));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!selected) {
                    setSelected(true);
                    if (onSelectCallback != null) {
                        onSelectCallback.run();
                    }
                }
            }
        });
    }

    public ConnectionRole getRole() {
        return role;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public void setOnSelectCallback(Runnable callback) {
        this.onSelectCallback = callback;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.enableQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();
        int r = 12;

        // Fondo dependiente del estado
        Color bg;
        if (selected) {
            bg = new Color(0x27, 0x2B, 0x3A);
        } else if (hovered) {
            bg = Theme.BG_CARD_HOVER;
        } else {
            bg = Theme.BG_CARD;
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, r, r);

        // Borde
        if (selected) {
            g2.setColor(Theme.PRIMARY);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, r, r);
        } else {
            g2.setColor(hovered ? new Color(0x42, 0x48, 0x5C) : Theme.BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
        }

        // Icono a la izquierda
        int iconSize = 36;
        int iconX = 16;
        int iconY = (h - iconSize) / 2;
        if (icon != null) {
            icon.paintIcon(this, g2, iconX, iconY);
        }

        // Título y descripción
        int textX = iconX + iconSize + 14;
        g2.setColor(selected ? Theme.TEXT_PRIMARY : (hovered ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY));
        g2.setFont(Theme.FONT_BODY_BOLD);
        g2.drawString(title, textX, 36);

        g2.setColor(Theme.TEXT_MUTED);
        g2.setFont(Theme.FONT_CAPTION);
        // Permitir dividir la descripción en 2 líneas
        drawStringWrapped(g2, description, textX, 56, w - textX - 32);

        // Indicador de selección a la derecha
        int checkX = w - 28;
        int checkY = 16;
        int checkSize = 16;
        if (selected) {
            g2.setColor(Theme.PRIMARY);
            g2.fillOval(checkX, checkY, checkSize, checkSize);
            Icon checkIcon = Icons.check(12, Theme.TEXT_PRIMARY);
            checkIcon.paintIcon(this, g2, checkX + 2, checkY + 2);
        } else {
            g2.setColor(Theme.BORDER_SUBTLE);
            g2.drawOval(checkX, checkY, checkSize, checkSize);
        }

        g2.dispose();
    }

    private void drawStringWrapped(Graphics2D g2, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int lineY = y;

        for (String word : words) {
            if (fm.stringWidth(currentLine + " " + word) > maxWidth) {
                g2.drawString(currentLine.toString(), x, lineY);
                lineY += fm.getHeight();
                currentLine = new StringBuilder(word);
            } else {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            g2.drawString(currentLine.toString(), x, lineY);
        }
    }
}

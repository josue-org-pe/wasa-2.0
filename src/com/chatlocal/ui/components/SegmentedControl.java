package com.chatlocal.ui.components;

import com.chatlocal.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Control segmentado tipo selector de pestañas / píldora moderna.
 * Permite alternar limpiamente entre opciones con feedback visual táctil.
 */
public class SegmentedControl extends JPanel {

    public interface SelectionListener {
        void onSelectionChanged(int selectedIndex, String item);
    }

    private final String[] items;
    private int selectedIndex = 0;
    private int hoveredIndex = -1;
    private SelectionListener listener;

    public SegmentedControl(String[] items, SelectionListener listener) {
        this.items = items;
        this.listener = listener;

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(380, 42));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = getIndexAt(e.getX());
                if (index >= 0 && index < items.length && index != selectedIndex) {
                    selectedIndex = index;
                    repaint();
                    if (SegmentedControl.this.listener != null) {
                        SegmentedControl.this.listener.onSelectionChanged(selectedIndex, items[selectedIndex]);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredIndex = -1;
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = getIndexAt(e.getX());
                if (index != hoveredIndex) {
                    hoveredIndex = index;
                    repaint();
                }
            }
        });
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < items.length) {
            this.selectedIndex = index;
            repaint();
        }
    }

    private int getIndexAt(int x) {
        if (items.length == 0 || getWidth() <= 0) return -1;
        int itemWidth = getWidth() / items.length;
        return Math.min(items.length - 1, Math.max(0, x / itemWidth));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.enableQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();
        int r = 10;

        // Fondo contenedor
        g2.setColor(Theme.BG_INPUT);
        g2.fillRoundRect(0, 0, w, h, r, r);
        g2.setColor(Theme.BORDER_SUBTLE);
        g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

        if (items.length == 0) {
            g2.dispose();
            return;
        }

        int itemWidth = w / items.length;
        int pad = 3;

        // Dibujar botón seleccionado activo
        int selX = selectedIndex * itemWidth + pad;
        int selW = itemWidth - 2 * pad;
        int selH = h - 2 * pad;

        g2.setColor(Theme.PRIMARY);
        g2.fillRoundRect(selX, pad, selW, selH, r - 2, r - 2);

        // Dibujar etiquetas
        g2.setFont(Theme.FONT_BODY_BOLD);
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < items.length; i++) {
            int cx = i * itemWidth + itemWidth / 2;
            int textW = fm.stringWidth(items[i]);
            int textX = cx - textW / 2;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

            if (i == selectedIndex) {
                g2.setColor(Theme.TEXT_PRIMARY);
            } else if (i == hoveredIndex) {
                g2.setColor(Theme.TEXT_PRIMARY);
            } else {
                g2.setColor(Theme.TEXT_SECONDARY);
            }

            g2.drawString(items[i], textX, textY);
        }

        g2.dispose();
    }
}

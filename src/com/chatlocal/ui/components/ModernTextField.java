package com.chatlocal.ui.components;

import com.chatlocal.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

// caja de texto con placeholder y bordes curvos
public class ModernTextField extends JTextField {

    private String placeholder = "";
    private boolean isFocused = false;
    private int cornerRadius = 10;

    public ModernTextField(String placeholder, int columns) {
        super(columns);
        this.placeholder = placeholder != null ? placeholder : "";

        setFont(Theme.FONT_BODY);
        setForeground(Theme.TEXT_PRIMARY);
        setCaretColor(Theme.TEXT_PRIMARY);
        setBackground(Theme.BG_INPUT);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.enableQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();

        // Fondo redondeado
        g2.setColor(Theme.BG_INPUT);
        g2.fillRoundRect(0, 0, w, h, cornerRadius, cornerRadius);

        // Borde con cambio de estado al enfocar
        if (isFocused) {
            g2.setColor(Theme.BORDER_ACTIVE);
            g2.setStroke(new BasicStroke(1.8f));
            g2.drawRoundRect(1, 1, w - 2, h - 2, cornerRadius, cornerRadius);
        } else {
            g2.setColor(Theme.BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);

        // Dibujar placeholder si está vacío
        if (getText().isEmpty() && !isFocused && placeholder != null && !placeholder.isEmpty()) {
            Graphics2D gPlaceholder = (Graphics2D) g.create();
            Theme.enableQualityRendering(gPlaceholder);
            gPlaceholder.setColor(Theme.TEXT_MUTED);
            gPlaceholder.setFont(getFont());
            FontMetrics fm = gPlaceholder.getFontMetrics();
            int py = (h - fm.getHeight()) / 2 + fm.getAscent();
            Insets insets = getInsets();
            gPlaceholder.drawString(placeholder, insets.left, py);
            gPlaceholder.dispose();
        }
    }
}

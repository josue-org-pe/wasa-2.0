package com.chatlocal.frontend.components;

import com.chatlocal.frontend.theme.ThemeColors;
import com.chatlocal.frontend.theme.ThemeFonts;
import com.chatlocal.frontend.theme.UIUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Campo de texto estilizado con esquinas redondeadas, placeholder y animación de foco.
 */
public class ModernTextField extends JTextField {

    private String placeholder = "";
    private boolean isFocused = false;
    private int cornerRadius = 10;

    public ModernTextField(String text, int columns) {
        super(text, columns);
        init();
    }

    public ModernTextField(String placeholder) {
        this.placeholder = placeholder;
        init();
    }

    public ModernTextField() {
        init();
    }

    private void init() {
        setFont(ThemeFonts.regular(13));
        setForeground(ThemeColors.TEXT_PRIMARY);
        setCaretColor(ThemeColors.PRIMARY);
        setBackground(ThemeColors.BG_INPUT);
        setOpaque(false);
        setBorder(new EmptyBorder(10, 14, 10, 14));

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

    public String getPlaceholder() {
        return placeholder;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UIUtils.applyQualityRendering(g2);

        // Fondo
        g2.setColor(ThemeColors.BG_INPUT);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // Borde
        if (isFocused) {
            g2.setColor(ThemeColors.BORDER_FOCUS);
            g2.setStroke(new BasicStroke(1.5f));
        } else {
            g2.setColor(ThemeColors.BORDER);
            g2.setStroke(new BasicStroke(1f));
        }
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        super.paintComponent(g);

        // Placeholder
        if (getText().isEmpty() && !placeholder.isEmpty() && !isFocused) {
            g2.setColor(ThemeColors.TEXT_HINT);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = getInsets().left;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(placeholder, x, y);
        }

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 40));
    }
}

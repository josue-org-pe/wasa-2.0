package com.chatlocal.ui.components;

import com.chatlocal.backend.service.PingTester;
import com.chatlocal.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Píldora que muestra el resultado visual de la prueba de Ping y latencia.
 */
public class PingResultBadge extends JPanel {

    private enum State {
        IDLE,
        TESTING,
        SUCCESS,
        ERROR
    }

    private State state = State.IDLE;
    private String text = "";
    private long latencyMs = -1;

    public PingResultBadge() {
        setOpaque(false);
        setPreferredSize(new Dimension(360, 32));
    }

    public void setIdle() {
        this.state = State.IDLE;
        this.text = "";
        setVisible(false);
        repaint();
    }

    public void setTesting() {
        this.state = State.TESTING;
        this.text = "Enviando paquete Ping a través de sockets...";
        setVisible(true);
        repaint();
    }

    public void setResult(PingTester.PingResult result) {
        if (result.isSuccess()) {
            this.state = State.SUCCESS;
            this.latencyMs = result.getLatencyMs();
            this.text = result.getFormattedQuality();
        } else {
            this.state = State.ERROR;
            this.text = result.getMessage();
        }
        setVisible(true);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (state == State.IDLE) return;

        Graphics2D g2 = (Graphics2D) g.create();
        Theme.enableQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();
        int r = 10;

        Color bg;
        Color border;
        Color dotColor;
        Color textColor;

        switch (state) {
            case SUCCESS -> {
                bg = new Color(0x13, 0x2A, 0x22);
                border = new Color(0x10, 0xB9, 0x81, 140);
                dotColor = Theme.SUCCESS;
                textColor = Theme.SUCCESS;
            }
            case ERROR -> {
                bg = new Color(0x2C, 0x18, 0x1A);
                border = new Color(0xEF, 0x44, 0x44, 140);
                dotColor = Theme.DANGER;
                textColor = new Color(0xF8, 0x71, 0x71);
            }
            default -> { // TESTING
                bg = new Color(0x27, 0x24, 0x16);
                border = new Color(0xF5, 0x9E, 0x0B, 140);
                dotColor = Theme.WARNING;
                textColor = Theme.WARNING;
            }
        }

        // Fondo curvo
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, r, r);
        g2.setColor(border);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

        // Punto luminoso
        int dotSize = 8;
        int dotX = 12;
        int dotY = (h - dotSize) / 2;

        g2.setColor(new Color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), 70));
        g2.fillOval(dotX - 2, dotY - 2, dotSize + 4, dotSize + 4);

        g2.setColor(dotColor);
        g2.fillOval(dotX, dotY, dotSize, dotSize);

        // Texto
        g2.setColor(textColor);
        g2.setFont(Theme.FONT_CAPTION);
        FontMetrics fm = g2.getFontMetrics();
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

        // Si el texto excede, truncar suavemente
        String display = text;
        int maxW = w - (dotX + dotSize + 20);
        if (fm.stringWidth(display) > maxW) {
            while (display.length() > 4 && fm.stringWidth(display + "...") > maxW) {
                display = display.substring(0, display.length() - 1);
            }
            display += "...";
        }

        g2.drawString(display, dotX + dotSize + 8, textY);
        g2.dispose();
    }
}

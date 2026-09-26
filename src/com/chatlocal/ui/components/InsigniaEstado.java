package com.chatlocal.ui.components;

import com.chatlocal.backend.model.EstadoConexion;
import com.chatlocal.ui.theme.Tema;

import javax.swing.*;
import java.awt.*;

// etiqueta redondeada con punto verde/rojo que indica el estado actual
public class InsigniaEstado extends JPanel {

    private String text = "Desconectado";
    private Color dotColor = Tema.DANGER;

    public InsigniaEstado() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
    }

    public void setState(EstadoConexion state) {
        if (state == null) state = EstadoConexion.DISCONNECTED;
        switch (state) {
            case CONNECTED -> {
                this.dotColor = Tema.SUCCESS;
                this.text = "En línea";
            }
            case LISTENING -> {
                this.dotColor = Tema.WARNING;
                this.text = "Esperando par...";
            }
            case CONNECTING -> {
                this.dotColor = Tema.WARNING;
                this.text = "Conectando...";
            }
            case VERIFYING -> {
                this.dotColor = Tema.PRIMARY;
                this.text = "Verificando...";
            }
            case ERROR -> {
                this.dotColor = Tema.DANGER;
                this.text = "Error de red";
            }
            default -> {
                this.dotColor = Tema.TEXT_MUTED;
                this.text = "Desconectado";
            }
        }
        repaint();
    }

    public void setCustomStatus(String text, Color dotColor) {
        this.text = text;
        this.dotColor = dotColor;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(Tema.FONT_CAPTION);
        int textWidth = fm.stringWidth(text);
        return new Dimension(textWidth + 34, 24);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Tema.enableQualityRendering(g2);

        int w = getWidth();
        int h = getHeight();

        // Píldora de fondo
        g2.setColor(Tema.BG_BADGE);
        g2.fillRoundRect(0, 0, w, h, h, h);
        g2.setColor(Tema.BORDER_SUBTLE);
        g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

        // Punto luminoso
        int dotSize = 8;
        int dotX = 10;
        int dotY = (h - dotSize) / 2;

        // Resplandor exterior suave
        g2.setColor(new Color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), 60));
        g2.fillOval(dotX - 2, dotY - 2, dotSize + 4, dotSize + 4);

        // Punto sólido
        g2.setColor(dotColor);
        g2.fillOval(dotX, dotY, dotSize, dotSize);

        // Texto
        g2.setColor(Tema.TEXT_SECONDARY);
        g2.setFont(Tema.FONT_CAPTION);
        FontMetrics fm = g2.getFontMetrics();
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, dotX + dotSize + 7, textY);

        g2.dispose();
    }
}

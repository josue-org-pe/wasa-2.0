package com.chatlocal.ui.theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

// iconos dibujados con Java2D para no necesitar imagenes externas
public final class Iconos {

    private Iconos() {}

    public static Icon send(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            Path2D path = new Path2D.Float();
            float pad = w * 0.15f;
            // Flecha tipo triángulo / avión de papel
            path.moveTo(pad, pad);
            path.lineTo(w - pad, h / 2.0f);
            path.lineTo(pad, h - pad);
            path.lineTo(pad + (w - 2 * pad) * 0.35f, h / 2.0f);
            path.closePath();
            g2.fill(path);
        });
    }

    public static Icon attach(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Icono de clip de papel estilizado
            float cx = w / 2.0f;
            float cy = h / 2.0f;
            float r = w * 0.22f;
            Path2D p = new Path2D.Float();
            p.moveTo(cx - r * 0.5f, cy + r * 0.6f);
            p.lineTo(cx - r * 0.5f, cy - r * 0.4f);
            p.curveTo(cx - r * 0.5f, cy - r * 1.1f, cx + r * 0.7f, cy - r * 1.1f, cx + r * 0.7f, cy - r * 0.4f);
            p.lineTo(cx + r * 0.7f, cy + r * 0.8f);
            p.curveTo(cx + r * 0.7f, cy + r * 1.5f, cx - r * 1.1f, cy + r * 1.5f, cx - r * 1.1f, cy + r * 0.6f);
            p.lineTo(cx - r * 1.1f, cy - r * 0.6f);
            g2.draw(p);
        });
    }

    public static Icon host(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Servidor / Antena Host
            float m = w * 0.2f;
            g2.drawRoundRect((int) m, (int) (h * 0.3f), (int) (w - 2 * m), (int) (h * 0.22f), 4, 4);
            g2.drawRoundRect((int) m, (int) (h * 0.6f), (int) (w - 2 * m), (int) (h * 0.22f), 4, 4);
            // Luces LED
            g2.fillOval((int) (m + 5), (int) (h * 0.3f + 4), 4, 4);
            g2.fillOval((int) (m + 5), (int) (h * 0.6f + 4), 4, 4);
        });
    }

    public static Icon client(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Pantalla / Dispositivo Cliente
            float m = w * 0.2f;
            g2.drawRoundRect((int) m, (int) (h * 0.22f), (int) (w - 2 * m), (int) (h * 0.45f), 4, 4);
            // Base
            g2.drawLine((int) (w * 0.4f), (int) (h * 0.67f), (int) (w * 0.4f), (int) (h * 0.78f));
            g2.drawLine((int) (w * 0.6f), (int) (h * 0.67f), (int) (w * 0.6f), (int) (h * 0.78f));
            g2.drawLine((int) (w * 0.3f), (int) (h * 0.78f), (int) (w * 0.7f), (int) (h * 0.78f));
        });
    }

    public static Icon file(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float x = w * 0.25f;
            float y = h * 0.15f;
            float fw = w * 0.5f;
            float fh = h * 0.7f;
            float fold = w * 0.18f;

            Path2D p = new Path2D.Float();
            p.moveTo(x, y);
            p.lineTo(x + fw - fold, y);
            p.lineTo(x + fw, y + fold);
            p.lineTo(x + fw, y + fh);
            p.lineTo(x, y + fh);
            p.closePath();
            g2.draw(p);

            // Doblez de la esquina
            g2.drawLine((int) (x + fw - fold), (int) y, (int) (x + fw - fold), (int) (y + fold));
            g2.drawLine((int) (x + fw - fold), (int) (y + fold), (int) (x + fw), (int) (y + fold));

            // Líneas de contenido de archivo
            g2.drawLine((int) (x + fw * 0.25f), (int) (y + fh * 0.45f), (int) (x + fw * 0.75f), (int) (y + fh * 0.45f));
            g2.drawLine((int) (x + fw * 0.25f), (int) (y + fh * 0.65f), (int) (x + fw * 0.75f), (int) (y + fh * 0.65f));
        });
    }

    public static Icon copy(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float s = w * 0.48f;
            // Cuadro trasero
            g2.drawRoundRect((int) (w * 0.32f), (int) (h * 0.16f), (int) s, (int) s, 3, 3);
            // Cuadro frontal
            g2.setColor(Tema.BG_CARD);
            g2.fillRoundRect((int) (w * 0.18f), (int) (h * 0.32f), (int) s, (int) s, 3, 3);
            g2.setColor(color);
            g2.drawRoundRect((int) (w * 0.18f), (int) (h * 0.32f), (int) s, (int) s, 3, 3);
        });
    }

    public static Icon folder(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float x = w * 0.15f;
            float y = h * 0.25f;
            float fw = w * 0.7f;
            float fh = h * 0.55f;

            Path2D p = new Path2D.Float();
            p.moveTo(x, y + fh);
            p.lineTo(x, y);
            p.lineTo(x + fw * 0.4f, y);
            p.lineTo(x + fw * 0.5f, y + fh * 0.2f);
            p.lineTo(x + fw, y + fh * 0.2f);
            p.lineTo(x + fw, y + fh);
            p.closePath();
            g2.draw(p);
        });
    }

    public static Icon check(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            p.moveTo(w * 0.22f, h * 0.52f);
            p.lineTo(w * 0.42f, h * 0.72f);
            p.lineTo(w * 0.8f, h * 0.28f);
            g2.draw(p);
        });
    }

    public static Icon power(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float r = w * 0.32f;
            float cx = w / 2.0f;
            float cy = h * 0.55f;
            // Arco de encendido
            g2.draw(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, 50, 260, Arc2D.OPEN));
            // Línea central
            g2.drawLine((int) cx, (int) (h * 0.18f), (int) cx, (int) cy);
        });
    }

    public static Icon user(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2.0f;
            // Cabeza
            float hr = w * 0.22f;
            g2.drawOval((int) (cx - hr), (int) (h * 0.16f), (int) (hr * 2), (int) (hr * 2));
            // Hombros
            float sr = w * 0.38f;
            g2.draw(new Arc2D.Float(cx - sr, h * 0.48f, sr * 2, sr * 1.6f, 0, 180, Arc2D.OPEN));
        });
    }

    public static Icon ping(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2.0f;
            float cy = h * 0.72f;
            // Punto emisor
            g2.fillOval((int) (cx - 2.5f), (int) (cy - 2.5f), 5, 5);
            // Ondas concéntricas superiores
            g2.draw(new Arc2D.Float(cx - w * 0.22f, cy - w * 0.22f, w * 0.44f, w * 0.44f, 40, 100, Arc2D.OPEN));
            g2.draw(new Arc2D.Float(cx - w * 0.38f, cy - w * 0.38f, w * 0.76f, w * 0.76f, 40, 100, Arc2D.OPEN));
        });
    }

    public static Icon server(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float m = w * 0.16f;
            g2.drawRoundRect((int) m, (int) (h * 0.18f), (int) (w - 2 * m), (int) (h * 0.28f), 4, 4);
            g2.drawRoundRect((int) m, (int) (h * 0.54f), (int) (w - 2 * m), (int) (h * 0.28f), 4, 4);
            // Puntos led
            g2.fillOval((int) (m + 6), (int) (h * 0.18f + 6), 4, 4);
            g2.fillOval((int) (m + 6), (int) (h * 0.54f + 6), 4, 4);
        });
    }

    public static Icon mic(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2.0f;
            // Cápsula
            g2.drawRoundRect((int) (cx - w * 0.14f), (int) (h * 0.16f), (int) (w * 0.28f), (int) (h * 0.44f), (int) (w * 0.28f), (int) (w * 0.28f));
            // Soporte semicircular
            g2.draw(new Arc2D.Float(cx - w * 0.25f, h * 0.32f, w * 0.5f, h * 0.36f, 0, -180, Arc2D.OPEN));
            // Pie
            g2.drawLine((int) cx, (int) (h * 0.68f), (int) cx, (int) (h * 0.82f));
            g2.drawLine((int) (cx - w * 0.18f), (int) (h * 0.82f), (int) (cx + w * 0.18f), (int) (h * 0.82f));
        });
    }

    public static Icon camera(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawRoundRect((int) (w * 0.12f), (int) (h * 0.3f), (int) (w * 0.76f), (int) (h * 0.54f), 5, 5);
            g2.drawOval((int) (w * 0.34f), (int) (h * 0.42f), (int) (w * 0.32f), (int) (h * 0.32f));
            g2.drawRoundRect((int) (w * 0.35f), (int) (h * 0.18f), (int) (w * 0.3f), (int) (h * 0.14f), 2, 2);
        });
    }

    public static Icon play(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            Path2D p = new Path2D.Float();
            p.moveTo(w * 0.32f, h * 0.22f);
            p.lineTo(w * 0.78f, h * 0.5f);
            p.lineTo(w * 0.32f, h * 0.78f);
            p.closePath();
            g2.fill(p);
        });
    }

    public static Icon pause(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            float bw = w * 0.18f;
            float bh = h * 0.56f;
            float y = (h - bh) / 2.0f;
            g2.fillRoundRect((int) (w * 0.26f), (int) y, (int) bw, (int) bh, 3, 3);
            g2.fillRoundRect((int) (w * 0.56f), (int) y, (int) bw, (int) bh, 3, 3);
        });
    }

    public static Icon stop(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            float s = w * 0.5f;
            float p = (w - s) / 2.0f;
            g2.fillRoundRect((int) p, (int) p, (int) s, (int) s, 4, 4);
        });
    }

    public static Icon emoji(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2.0f;
            float cy = h / 2.0f;
            float r = w * 0.38f;
            // Cara
            g2.drawOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
            // Ojos
            g2.fillOval((int) (cx - r * 0.45f), (int) (cy - r * 0.3f), 3, 4);
            g2.fillOval((int) (cx + r * 0.25f), (int) (cy - r * 0.3f), 3, 4);
            // Sonrisa
            g2.draw(new Arc2D.Float(cx - r * 0.5f, cy - r * 0.2f, r, r * 0.8f, 0, -180, Arc2D.OPEN));
        });
    }

    public static Icon doubleCheck(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Primer check
            Path2D p1 = new Path2D.Float();
            p1.moveTo(w * 0.12f, h * 0.52f);
            p1.lineTo(w * 0.32f, h * 0.72f);
            p1.lineTo(w * 0.65f, h * 0.3f);
            g2.draw(p1);
            // Segundo check
            Path2D p2 = new Path2D.Float();
            p2.moveTo(w * 0.35f, h * 0.52f);
            p2.lineTo(w * 0.52f, h * 0.72f);
            p2.lineTo(w * 0.88f, h * 0.3f);
            g2.draw(p2);
        });
    }

    public static Icon singleCheck(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Float();
            p.moveTo(w * 0.22f, h * 0.52f);
            p.lineTo(w * 0.44f, h * 0.74f);
            p.lineTo(w * 0.82f, h * 0.28f);
            g2.draw(p);
        });
    }

    public static Icon gear(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2.0f;
            float cy = h / 2.0f;
            float ro = w * 0.36f;
            float ri = w * 0.16f;
            g2.drawOval((int) (cx - ro), (int) (cy - ro), (int) (ro * 2), (int) (ro * 2));
            g2.drawOval((int) (cx - ri), (int) (cy - ri), (int) (ri * 2), (int) (ri * 2));
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60);
                int x1 = (int) (cx + (ro - 1) * Math.cos(angle));
                int y1 = (int) (cy + (ro - 1) * Math.sin(angle));
                int x2 = (int) (cx + (ro + 3) * Math.cos(angle));
                int y2 = (int) (cy + (ro + 3) * Math.sin(angle));
                g2.drawLine(x1, y1, x2, y2);
            }
        });
    }

    public static Icon search(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float r = w * 0.28f;
            g2.drawOval((int) (w * 0.2f), (int) (h * 0.2f), (int) (r * 2), (int) (r * 2));
            g2.drawLine((int) (w * 0.2f + r * 1.7f), (int) (h * 0.2f + r * 1.7f), (int) (w * 0.82f), (int) (h * 0.82f));
        });
    }

    public static Icon plus(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2.0f;
            float cy = h / 2.0f;
            float l = w * 0.3f;
            g2.drawLine((int) (cx - l), (int) cy, (int) (cx + l), (int) cy);
            g2.drawLine((int) cx, (int) (cy - l), (int) cx, (int) (cy + l));
        });
    }

    public static Icon group(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Usuario frontal
            float cx = w * 0.42f;
            float hr = w * 0.18f;
            g2.drawOval((int) (cx - hr), (int) (h * 0.2f), (int) (hr * 2), (int) (hr * 2));
            g2.draw(new Arc2D.Float(cx - w * 0.32f, h * 0.48f, w * 0.64f, h * 0.4f, 0, 180, Arc2D.OPEN));
            // Usuario trasero
            float cx2 = w * 0.7f;
            float hr2 = w * 0.14f;
            g2.drawOval((int) (cx2 - hr2), (int) (h * 0.14f), (int) (hr2 * 2), (int) (hr2 * 2));
            g2.draw(new Arc2D.Float(cx2 - w * 0.22f, h * 0.38f, w * 0.44f, h * 0.35f, 0, 120, Arc2D.OPEN));
        });
    }

    public static Icon lock(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2.0f;
            // Grillete
            float r = w * 0.2f;
            g2.draw(new Arc2D.Float(cx - r, h * 0.18f, r * 2, r * 2.2f, 0, 180, Arc2D.OPEN));
            // Cuerpo
            g2.drawRoundRect((int) (w * 0.22f), (int) (h * 0.42f), (int) (w * 0.56f), (int) (h * 0.42f), 4, 4);
            g2.fillOval((int) (cx - 2), (int) (h * 0.56f), 4, 6);
        });
    }

    public static Icon video(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cw = w * 0.52f;
            float ch = h * 0.46f;
            float x = w * 0.14f;
            float y = (h - ch) / 2.0f;
            g2.drawRoundRect((int) x, (int) y, (int) cw, (int) ch, 4, 4);

            Path2D p = new Path2D.Float();
            p.moveTo(x + cw, y + ch * 0.25f);
            p.lineTo(w * 0.86f, y + ch * 0.05f);
            p.lineTo(w * 0.86f, y + ch * 0.95f);
            p.lineTo(x + cw, y + ch * 0.75f);
            p.closePath();
            g2.draw(p);
        });
    }

    public static Icon videoOff(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cw = w * 0.52f;
            float ch = h * 0.46f;
            float x = w * 0.14f;
            float y = (h - ch) / 2.0f;
            g2.drawRoundRect((int) x, (int) y, (int) cw, (int) ch, 4, 4);

            Path2D p = new Path2D.Float();
            p.moveTo(x + cw, y + ch * 0.25f);
            p.lineTo(w * 0.86f, y + ch * 0.05f);
            p.lineTo(w * 0.86f, y + ch * 0.95f);
            p.lineTo(x + cw, y + ch * 0.75f);
            p.closePath();
            g2.draw(p);

            // Línea diagonal tachada
            g2.setColor(new Color(239, 68, 68));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) (w * 0.12f), (int) (h * 0.88f), (int) (w * 0.88f), (int) (h * 0.12f));
        });
    }

    public static Icon micOff(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = w / 2.0f;
            // Cápsula
            g2.drawRoundRect((int) (cx - w * 0.14f), (int) (h * 0.16f), (int) (w * 0.28f), (int) (h * 0.44f), (int) (w * 0.28f), (int) (w * 0.28f));
            // Soporte
            g2.draw(new Arc2D.Float(cx - w * 0.25f, h * 0.32f, w * 0.5f, h * 0.36f, 0, -180, Arc2D.OPEN));
            g2.drawLine((int) cx, (int) (h * 0.68f), (int) cx, (int) (h * 0.82f));
            g2.drawLine((int) (cx - w * 0.18f), (int) (h * 0.82f), (int) (cx + w * 0.18f), (int) (h * 0.82f));

            // Línea diagonal tachada
            g2.setColor(new Color(239, 68, 68));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) (w * 0.15f), (int) (h * 0.85f), (int) (w * 0.85f), (int) (h * 0.15f));
        });
    }

    public static Icon screenShare(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Pantalla
            float m = w * 0.14f;
            g2.drawRoundRect((int) m, (int) (h * 0.18f), (int) (w - 2 * m), (int) (h * 0.5f), 4, 4);
            // Pie
            g2.drawLine((int) (w * 0.5f), (int) (h * 0.68f), (int) (w * 0.5f), (int) (h * 0.82f));
            g2.drawLine((int) (w * 0.32f), (int) (h * 0.82f), (int) (w * 0.68f), (int) (h * 0.82f));
            // Flecha hacia arriba centrada
            float cx = w * 0.5f;
            float cy = h * 0.42f;
            g2.drawLine((int) cx, (int) (cy - h * 0.14f), (int) cx, (int) (cy + h * 0.14f));
            g2.drawLine((int) cx, (int) (cy - h * 0.14f), (int) (cx - w * 0.12f), (int) (cy - h * 0.02f));
            g2.drawLine((int) cx, (int) (cy - h * 0.14f), (int) (cx + w * 0.12f), (int) (cy - h * 0.02f));
        });
    }

    public static Icon phone(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Auricular telefónico
            Path2D p = new Path2D.Float();
            p.moveTo(w * 0.28f, h * 0.36f);
            p.curveTo(w * 0.28f, h * 0.22f, w * 0.42f, h * 0.22f, w * 0.48f, h * 0.32f);
            p.lineTo(w * 0.44f, h * 0.44f);
            p.curveTo(w * 0.46f, h * 0.56f, w * 0.56f, h * 0.66f, w * 0.68f, h * 0.68f);
            p.lineTo(w * 0.80f, h * 0.64f);
            p.curveTo(w * 0.90f, h * 0.70f, w * 0.90f, h * 0.84f, w * 0.76f, h * 0.84f);
            p.curveTo(w * 0.46f, h * 0.84f, w * 0.28f, h * 0.66f, w * 0.28f, h * 0.36f);
            p.closePath();
            g2.draw(p);
        });
    }

    public static Icon phoneEnd(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Teléfono invertido horizontal para colgar
            Path2D p = new Path2D.Float();
            float cx = w / 2.0f;
            float cy = h * 0.52f;
            float r = w * 0.36f;
            p.moveTo(cx - r, cy);
            p.curveTo(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy - r * 0.7f, cx + r, cy);
            p.curveTo(cx + r * 0.6f, cy + r * 0.35f, cx + r * 0.35f, cy + r * 0.35f, cx + r * 0.35f, cy);
            p.curveTo(cx + r * 0.2f, cy - r * 0.2f, cx - r * 0.2f, cy - r * 0.2f, cx - r * 0.35f, cy);
            p.curveTo(cx - r * 0.35f, cy + r * 0.35f, cx - r * 0.6f, cy + r * 0.35f, cx - r, cy);
            p.closePath();
            g2.fill(p);
        });
    }

    public static Icon sidebarToggle(int size, Color color) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Tres líneas horizontales modernas con ancho dinámico
            float padX = w * 0.18f;
            g2.drawLine((int) padX, (int) (h * 0.28f), (int) (w - padX), (int) (h * 0.28f));
            g2.drawLine((int) padX, (int) (h * 0.50f), (int) (w - padX * 1.5f), (int) (h * 0.50f));
            g2.drawLine((int) padX, (int) (h * 0.72f), (int) (w - padX), (int) (h * 0.72f));
        });
    }

    public static Icon avatar(String name, int size, Color bg, Color fg) {
        return avatar(name, size, bg, fg, null);
    }

    public static Icon avatar(String name, int size, Color bg, Color fg, String imagePath) {
        return createVectorIcon(size, size, (g2, w, h) -> {
            if (imagePath != null && !imagePath.trim().isEmpty()) {
                java.io.File imgFile = new java.io.File(imagePath);
                if (imgFile.exists() && imgFile.isFile()) {
                    try {
                        BufferedImage raw = javax.imageio.ImageIO.read(imgFile);
                        if (raw != null) {
                            // Clip circular para avatar de foto
                            g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, w, h));
                            g2.drawImage(raw, 0, 0, w, h, null);
                            g2.setClip(null);
                            g2.setColor(new Color(255, 255, 255, 40));
                            g2.setStroke(new BasicStroke(1.5f));
                            g2.drawOval(0, 0, w - 1, h - 1);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
            }

            g2.setColor(bg);
            g2.fillOval(0, 0, w, h);

            String initial = (name != null && !name.trim().isEmpty())
                    ? name.trim().substring(0, 1).toUpperCase()
                    : "?";

            g2.setColor(fg);
            g2.setFont(new Font(Tema.FONT_FAMILY, Font.BOLD, (int) (size * 0.48)));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(initial)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(initial, tx, ty);
        });
    }

    @FunctionalInterface
    public interface VectorDrawer {
        void draw(Graphics2D g2, int width, int height);
    }

    public static ImageIcon createVectorIcon(int width, int height, VectorDrawer drawer) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        Tema.enableQualityRendering(g2);
        drawer.draw(g2, width, height);
        g2.dispose();
        return new ImageIcon(image);
    }
}

package com.chatlocal.backend.service.videocall;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Fuente de video virtual de alta fidelidad:
 * Renderiza una señal de cámara simulada con estética moderna estilo streaming/Discord:
 * - Avatar dinámico con halo reactivo al nivel de micrófono.
 * - Ecualizador gráfico de audio en tiempo real.
 * - Marcadores OSD/HUD: "● EN VIVO", contador de tiempo, badge de usuario y FPS.
 */
public class VirtualCameraVideoSource implements VideoSource {

    private static final int WIDTH = 480;
    private static final int HEIGHT = 360;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String userName = "Usuario";
    private float currentAudioLevel = 0.0f;
    private float smoothedAudioLevel = 0.0f;
    private long frameCount = 0;
    private final long startTime = System.currentTimeMillis();

    public VirtualCameraVideoSource(String userName) {
        this.userName = (userName != null && !userName.isEmpty()) ? userName : "Usuario";
    }

    @Override
    public synchronized void setUserName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.userName = name.trim();
        }
    }

    @Override
    public synchronized void setAudioLevel(float level) {
        this.currentAudioLevel = Math.max(0.0f, Math.min(1.0f, level));
    }

    @Override
    public BufferedImage captureFrame() {
        frameCount++;
        smoothedAudioLevel = smoothedAudioLevel * 0.7f + currentAudioLevel * 0.3f;

        BufferedImage frame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = frame.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        drawBackground(g2);
        drawAvatar(g2);
        drawAudioVisualizer(g2);
        drawHudOverlay(g2);

        g2.dispose();
        return frame;
    }

    private void drawBackground(Graphics2D g2) {
        // Fondo con gradiente oscuro suave y sutil movimiento
        double angle = (frameCount * 0.02) % (2 * Math.PI);
        float shift = (float) Math.sin(angle) * 30.0f;

        Point2D start = new Point2D.Float(WIDTH / 2.0f + shift, 0);
        Point2D end = new Point2D.Float(WIDTH / 2.0f - shift, HEIGHT);
        float[] dist = {0.0f, 0.5f, 1.0f};
        Color[] colors = {
                new Color(18, 24, 38),
                new Color(15, 17, 26),
                new Color(8, 10, 15)
        };
        LinearGradientPaint p = new LinearGradientPaint(start, end, dist, colors);
        g2.setPaint(p);
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        // Grid sutil tecnológico en el fondo
        g2.setColor(new Color(255, 255, 255, 6));
        g2.setStroke(new BasicStroke(1.0f));
        int gridSize = 32;
        for (int x = 0; x < WIDTH; x += gridSize) {
            g2.drawLine(x, 0, x, HEIGHT);
        }
        for (int y = 0; y < HEIGHT; y += gridSize) {
            g2.drawLine(0, y, WIDTH, y);
        }

        // Iluminación ambiental cenital
        RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Float(WIDTH / 2.0f, HEIGHT * 0.42f),
                WIDTH * 0.45f,
                new float[]{0.0f, 1.0f},
                new Color[]{new Color(99, 102, 241, 35), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawAvatar(Graphics2D g2) {
        float cx = WIDTH / 2.0f;
        float cy = HEIGHT * 0.42f;

        // Respiración / animación cíclica
        double breathe = Math.sin(frameCount * 0.08) * 2.5;
        float baseRadius = 54.0f + (float) breathe;

        // Halo de voz reactivo al micrófono
        float voicePulse = smoothedAudioLevel * 28.0f;
        if (voicePulse > 1.5f) {
            float haloRadius = baseRadius + voicePulse;
            RadialGradientPaint halo = new RadialGradientPaint(
                    new Point2D.Float(cx, cy),
                    haloRadius,
                    new float[]{0.6f, 1.0f},
                    new Color[]{
                            new Color(139, 92, 246, (int) Math.min(180, 50 + voicePulse * 4)),
                            new Color(139, 92, 246, 0)
                    }
            );
            g2.setPaint(halo);
            g2.fill(new Ellipse2D.Float(cx - haloRadius, cy - haloRadius, haloRadius * 2, haloRadius * 2));
        }

        // Anillo exterior elegante
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(voicePulse > 3.0f ? new Color(16, 185, 129) : new Color(139, 92, 246, 160));
        g2.draw(new Ellipse2D.Float(cx - baseRadius, cy - baseRadius, baseRadius * 2, baseRadius * 2));

        // Círculo del Avatar
        GradientPaint avatarGrad = new GradientPaint(
                cx - baseRadius, cy - baseRadius, new Color(79, 70, 229),
                cx + baseRadius, cy + baseRadius, new Color(147, 51, 234)
        );
        g2.setPaint(avatarGrad);
        g2.fill(new Ellipse2D.Float(cx - baseRadius + 3, cy - baseRadius + 3, (baseRadius - 3) * 2, (baseRadius - 3) * 2));

        // Inicial del usuario
        String initial = (userName != null && !userName.isEmpty()) ? userName.substring(0, 1).toUpperCase() : "U";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 46));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (int) (cx - fm.stringWidth(initial) / 2.0f);
        int ty = (int) (cy - fm.getHeight() / 2.0f + fm.getAscent());
        g2.setColor(Color.WHITE);
        g2.drawString(initial, tx, ty);
    }

    private void drawAudioVisualizer(Graphics2D g2) {
        int barCount = 14;
        float barWidth = 6.0f;
        float barGap = 4.0f;
        float totalWidth = barCount * barWidth + (barCount - 1) * barGap;
        float startX = (WIDTH - totalWidth) / 2.0f;
        float baseY = HEIGHT * 0.72f;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < barCount; i++) {
            float distFromCenter = Math.abs(i - (barCount - 1) / 2.0f);
            double wavePhase = frameCount * 0.25 + i * 0.45;
            float waveFactor = (float) (Math.sin(wavePhase) * 0.5 + 0.5);

            float height = 5.0f + (smoothedAudioLevel * 38.0f * (1.0f - distFromCenter * 0.08f) * waveFactor);
            height = Math.max(4.0f, Math.min(36.0f, height));

            float x = startX + i * (barWidth + barGap);
            float y = baseY - height / 2.0f;

            Color barColor = (smoothedAudioLevel > 0.05f)
                    ? new Color(16, 185, 129, 210) // Verde activo
                    : new Color(148, 163, 184, 120); // Gris neutro

            g2.setColor(barColor);
            g2.fill(new RoundRectangle2D.Float(x, y, barWidth, height, 4, 4));
        }
    }

    private void drawHudOverlay(Graphics2D g2) {
        // 1. Badge Superior Izquierdo: [● EN VIVO] + Timer
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        long elapsedSecs = (System.currentTimeMillis() - startTime) / 1000;
        String timeStr = String.format("%02d:%02d", elapsedSecs / 60, elapsedSecs % 60);

        // Fondo pill
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(14, 14, 120, 24, 12, 12);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(14, 14, 120, 24, 12, 12);

        // Punto rojo parpadeante
        boolean blink = (frameCount % 30) < 20;
        g2.setColor(blink ? new Color(239, 68, 68) : new Color(150, 30, 30));
        g2.fillOval(23, 22, 8, 8);

        g2.setColor(Color.WHITE);
        g2.drawString("LIVE  " + timeStr, 38, 30);

        // 2. Badge Superior Derecho: Calidad HD
        String qualityText = "HD 720p • 20 FPS";
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        int qw = g2.getFontMetrics().stringWidth(qualityText) + 16;
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(WIDTH - qw - 14, 14, qw, 24, 12, 12);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(WIDTH - qw - 14, 14, qw, 24, 12, 12);
        g2.setColor(new Color(203, 213, 225));
        g2.drawString(qualityText, WIDTH - qw - 6, 30);

        // 3. Badge Inferior Izquierdo: Nombre de Usuario
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        String userBadge = "📷 " + userName;
        int uw = g2.getFontMetrics().stringWidth(userBadge) + 20;
        g2.setColor(new Color(15, 23, 42, 190));
        g2.fillRoundRect(14, HEIGHT - 38, uw, 26, 13, 13);
        g2.setColor(new Color(139, 92, 246, 120));
        g2.drawRoundRect(14, HEIGHT - 38, uw, 26, 13, 13);
        g2.setColor(new Color(248, 250, 252));
        g2.drawString(userBadge, 24, HEIGHT - 21);

        // 4. Marca de agua sutil en esquina inferior derecha
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(new Color(255, 255, 255, 70));
        g2.drawString("ChatLocal Video", WIDTH - 95, HEIGHT - 21);

        // Viñeta oscura suave en los bordes
        Paint oldPaint = g2.getPaint();
        RadialGradientPaint vignette = new RadialGradientPaint(
                new Point2D.Float(WIDTH / 2.0f, HEIGHT / 2.0f),
                WIDTH * 0.65f,
                new float[]{0.7f, 1.0f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 90)}
        );
        g2.setPaint(vignette);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setPaint(oldPaint);
    }

    @Override
    public void close() {
        // Sin recursos nativos retenidos
    }
}

package com.chatlocal.backend.service.videocall;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Fuente de video virtual HD (1280x720 @ 60 FPS) de alta fidelidad:
 * Renderiza una señal de cámara simulada con estética moderna estilo streaming/Discord:
 * - Avatar dinámico con halo reactivo al nivel de micrófono en tiempo real.
 * - Ecualizador gráfico de audio fluido a 60 FPS.
 * - Marcadores OSD/HUD: "● LIVE", contador de tiempo, badge de usuario y "HD 720p • 60 FPS".
 */
public class VirtualCameraVideoSource implements VideoSource {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private String userName = "Usuario";
    private String customSubtitle = null;
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

    public synchronized void setCustomSubtitle(String subtitle) {
        this.customSubtitle = subtitle;
    }

    @Override
    public BufferedImage captureFrame() {
        frameCount++;
        smoothedAudioLevel = smoothedAudioLevel * 0.75f + currentAudioLevel * 0.25f;

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
        double angle = (frameCount * 0.015) % (2 * Math.PI);
        float shift = (float) Math.sin(angle) * 50.0f;

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

        // Grid tecnológico sutil en alta definición
        g2.setColor(new Color(255, 255, 255, 6));
        g2.setStroke(new BasicStroke(1.0f));
        int gridSize = 48;
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
                new Color[]{new Color(99, 102, 241, 40), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(glow);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private void drawAvatar(Graphics2D g2) {
        float cx = WIDTH / 2.0f;
        float cy = HEIGHT * 0.42f;

        // Respiración / animación cíclica a 60 FPS
        double breathe = Math.sin(frameCount * 0.05) * 4.0;
        float baseRadius = 100.0f + (float) breathe;

        // Halo de voz reactivo al micrófono
        float voicePulse = smoothedAudioLevel * 50.0f;
        if (voicePulse > 1.5f) {
            float haloRadius = baseRadius + voicePulse;
            RadialGradientPaint halo = new RadialGradientPaint(
                    new Point2D.Float(cx, cy),
                    haloRadius,
                    new float[]{0.6f, 1.0f},
                    new Color[]{
                            new Color(139, 92, 246, (int) Math.min(190, 60 + voicePulse * 3)),
                            new Color(139, 92, 246, 0)
                    }
            );
            g2.setPaint(halo);
            g2.fill(new Ellipse2D.Float(cx - haloRadius, cy - haloRadius, haloRadius * 2, haloRadius * 2));
        }

        // Anillo exterior elegante
        g2.setStroke(new BasicStroke(3.5f));
        g2.setColor(voicePulse > 4.0f ? new Color(16, 185, 129) : new Color(139, 92, 246, 170));
        g2.draw(new Ellipse2D.Float(cx - baseRadius, cy - baseRadius, baseRadius * 2, baseRadius * 2));

        // Círculo del Avatar
        GradientPaint avatarGrad = new GradientPaint(
                cx - baseRadius, cy - baseRadius, new Color(79, 70, 229),
                cx + baseRadius, cy + baseRadius, new Color(147, 51, 234)
        );
        g2.setPaint(avatarGrad);
        g2.fill(new Ellipse2D.Float(cx - baseRadius + 4, cy - baseRadius + 4, (baseRadius - 4) * 2, (baseRadius - 4) * 2));

        // Inicial del usuario
        String initial = (userName != null && !userName.isEmpty()) ? userName.substring(0, 1).toUpperCase() : "U";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 84));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (int) (cx - fm.stringWidth(initial) / 2.0f);
        int ty = (int) (cy - fm.getHeight() / 2.0f + fm.getAscent());
        g2.setColor(Color.WHITE);
        g2.drawString(initial, tx, ty);

        // Subtítulo de estado (por ejemplo: "Iniciando cámara física...")
        if (customSubtitle != null && !customSubtitle.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            FontMetrics fmSub = g2.getFontMetrics();
            int subW = fmSub.stringWidth(customSubtitle);
            int subX = (int) (cx - subW / 2.0f);
            int subY = (int) (cy + baseRadius + 40);

            g2.setColor(new Color(15, 23, 42, 210));
            g2.fillRoundRect(subX - 16, subY - 20, subW + 32, 28, 14, 14);
            g2.setColor(new Color(251, 191, 36));
            g2.drawString(customSubtitle, subX, subY);
        }
    }

    private void drawAudioVisualizer(Graphics2D g2) {
        int barCount = 22;
        float barWidth = 10.0f;
        float barGap = 6.0f;
        float totalWidth = barCount * barWidth + (barCount - 1) * barGap;
        float startX = (WIDTH - totalWidth) / 2.0f;
        float baseY = HEIGHT * 0.74f;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < barCount; i++) {
            float distFromCenter = Math.abs(i - (barCount - 1) / 2.0f);
            double wavePhase = frameCount * 0.18 + i * 0.4;
            float waveFactor = (float) (Math.sin(wavePhase) * 0.5 + 0.5);

            float height = 8.0f + (smoothedAudioLevel * 60.0f * (1.0f - distFromCenter * 0.05f) * waveFactor);
            height = Math.max(6.0f, Math.min(56.0f, height));

            float x = startX + i * (barWidth + barGap);
            float y = baseY - height / 2.0f;

            Color barColor = (smoothedAudioLevel > 0.05f)
                    ? new Color(16, 185, 129, 220) // Verde activo
                    : new Color(148, 163, 184, 130); // Gris neutro

            g2.setColor(barColor);
            g2.fill(new RoundRectangle2D.Float(x, y, barWidth, height, 6, 6));
        }
    }

    private void drawHudOverlay(Graphics2D g2) {
        // 1. Badge Superior Izquierdo: [● EN VIVO] + Timer
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        long elapsedSecs = (System.currentTimeMillis() - startTime) / 1000;
        String timeStr = String.format("%02d:%02d", elapsedSecs / 60, elapsedSecs % 60);

        // Fondo pill
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(24, 20, 140, 32, 16, 16);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(24, 20, 140, 32, 16, 16);

        // Punto rojo parpadeante (a 60 FPS parpadea suavemente)
        boolean blink = (frameCount % 60) < 40;
        g2.setColor(blink ? new Color(239, 68, 68) : new Color(150, 30, 30));
        g2.fillOval(36, 31, 10, 10);

        g2.setColor(Color.WHITE);
        g2.drawString("LIVE  " + timeStr, 54, 42);

        // 2. Badge Superior Derecho: Calidad HD 60 FPS
        String qualityText = "● HD 720p • 60 FPS";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        int qw = g2.getFontMetrics().stringWidth(qualityText) + 24;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(WIDTH - qw - 24, 20, qw, 32, 16, 16);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(WIDTH - qw - 24, 20, qw, 32, 16, 16);
        g2.setColor(new Color(52, 211, 153));
        g2.drawString(qualityText, WIDTH - qw - 12, 41);

        // 3. Badge Inferior Izquierdo: Nombre de Usuario
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        String userBadge = "📷 " + userName;
        int uw = g2.getFontMetrics().stringWidth(userBadge) + 24;
        g2.setColor(new Color(15, 23, 42, 200));
        g2.fillRoundRect(24, HEIGHT - 52, uw, 32, 16, 16);
        g2.setColor(new Color(139, 92, 246, 140));
        g2.drawRoundRect(24, HEIGHT - 52, uw, 32, 16, 16);
        g2.setColor(new Color(248, 250, 252));
        g2.drawString(userBadge, 36, HEIGHT - 31);

        // 4. Marca de agua sutil en esquina inferior derecha
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(new Color(255, 255, 255, 80));
        g2.drawString("ChatLocal Video HD", WIDTH - 145, HEIGHT - 31);

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

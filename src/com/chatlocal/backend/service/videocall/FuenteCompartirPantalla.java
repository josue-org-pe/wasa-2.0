package com.chatlocal.backend.service.videocall;

import java.awt.*;
import java.awt.image.BufferedImage;

// esto captura la pantalla usando Robot de java
public class FuenteCompartirPantalla implements FuenteVideo {

    private static final int TARGET_WIDTH = 1280;
    private static final int TARGET_HEIGHT = 720;

    private Robot robot;
    private final Rectangle screenRect;
    private String userName = "Usuario";
    private float audioLevel = 0.0f;

    public FuenteCompartirPantalla(String userName) {
        this.userName = (userName != null && !userName.isEmpty()) ? userName : "Usuario";
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.screenRect = new Rectangle(0, 0, (int) screenSize.getWidth(), (int) screenSize.getHeight());

        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            System.err.println("No se pudo inicializar Robot para captura de pantalla: " + e.getMessage());
        }
    }

    @Override
    public synchronized void setUserName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.userName = name.trim();
        }
    }

    @Override
    public synchronized void setAudioLevel(float level) {
        this.audioLevel = level;
    }

    @Override
    public BufferedImage captureFrame() {
        if (robot == null) {
            return createErrorPlaceholder();
        }

        try {
            BufferedImage rawScreen = robot.createScreenCapture(screenRect);
            BufferedImage scaled = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = scaled.createGraphics();

            // Fondo negro para barras si hay diferencia de aspecto
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, TARGET_WIDTH, TARGET_HEIGHT);

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            double screenAspect = (double) rawScreen.getWidth() / rawScreen.getHeight();
            double targetAspect = (double) TARGET_WIDTH / TARGET_HEIGHT;

            int drawW, drawH, drawX, drawY;
            if (screenAspect > targetAspect) {
                drawW = TARGET_WIDTH;
                drawH = (int) (TARGET_WIDTH / screenAspect);
                drawX = 0;
                drawY = (TARGET_HEIGHT - drawH) / 2;
            } else {
                drawH = TARGET_HEIGHT;
                drawW = (int) (TARGET_HEIGHT * screenAspect);
                drawX = (TARGET_WIDTH - drawW) / 2;
                drawY = 0;
            }

            g2.drawImage(rawScreen, drawX, drawY, drawW, drawH, null);

            // Borde verde distintivo de compartir pantalla
            g2.setColor(new Color(16, 185, 129, 200));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRect(1, 1, TARGET_WIDTH - 2, TARGET_HEIGHT - 2);

            // Badge inferior
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            String badgeText = "🖥️ Pantalla de " + userName;
            int bw = g2.getFontMetrics().stringWidth(badgeText) + 16;
            g2.setColor(new Color(15, 23, 42, 210));
            g2.fillRoundRect(12, TARGET_HEIGHT - 32, bw, 22, 10, 10);
            g2.setColor(new Color(16, 185, 129));
            g2.drawRoundRect(12, TARGET_HEIGHT - 32, bw, 22, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString(badgeText, 20, TARGET_HEIGHT - 17);

            g2.dispose();
            return scaled;
        } catch (Exception e) {
            return createErrorPlaceholder();
        }
    }

    private BufferedImage createErrorPlaceholder() {
        BufferedImage img = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(15, 23, 42));
        g2.fillRect(0, 0, TARGET_WIDTH, TARGET_HEIGHT);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString("Captura de pantalla no disponible", 120, 180);
        g2.dispose();
        return img;
    }

    @Override
    public void close() {
        robot = null;
    }
}

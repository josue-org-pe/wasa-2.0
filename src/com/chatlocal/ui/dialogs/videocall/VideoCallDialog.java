package com.chatlocal.ui.dialogs.videocall;

import com.chatlocal.backend.service.videocall.CallState;
import com.chatlocal.backend.service.videocall.VideoCallListener;
import com.chatlocal.backend.service.videocall.VideoCallService;
import com.chatlocal.backend.service.videocall.VideoSourceType;
import com.chatlocal.ui.components.ModernButton;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.Theme;
import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Ventana principal de videollamada estilo Discord / FaceTime / Google Meet:
 * - Renderizado en tiempo real de video remoto a pantalla completa con aspect-ratio mantenido.
 * - Vista local flotante Picture-in-Picture (PiP) interactiva en la esquina superior derecha.
 * - Barra superior con temporizador de duración y badge de calidad HD.
 * - Dock inferior flotante con controles de Micrófono, Cámara, Compartir Pantalla y Colgar.
 */
public class VideoCallDialog extends JDialog implements VideoCallListener {

    private final VideoCallService callService;
    private final String peerName;

    private BufferedImage remoteFrame;
    private BufferedImage localFrame;
    private float remoteAudioLevel = 0.0f;
    private float localAudioLevel = 0.0f;

    private JLabel lblTimer;
    private JLabel lblStatus;
    private ModernButton btnMic;
    private ModernButton btnVideo;
    private ModernButton btnScreen;
    private JComboBox<VideoSourceType> comboSources;

    private JPanel remoteVideoPanel;
    private JPanel localPipPanel;
    private boolean swapViews = false;

    private long callStartTime = 0;
    private javax.swing.Timer durationTimer;

    public VideoCallDialog(Frame parent, VideoCallService callService, String peerName) {
        super(parent, "Videollamada con " + peerName, false);
        this.callService = callService;
        this.peerName = (peerName != null && !peerName.isEmpty()) ? peerName : "Contacto";

        setSize(840, 600);
        setMinimumSize(new Dimension(680, 480));
        setLocationRelativeTo(parent);
        getContentPane().setBackground(ThemeManager.getTheme().bgDark);

        buildUI();

        callService.addListener(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupAndClose();
            }
        });

        startTimer();
    }

    private void buildUI() {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);

        // 1. Panel de Video Remoto (Capa Fondo 0)
        remoteVideoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.enableQualityRendering(g2);

                int w = getWidth();
                int h = getHeight();

                BufferedImage mainImg = swapViews ? localFrame : remoteFrame;
                if (mainImg != null && (swapViews || callService.getCallState() == CallState.CONNECTED)) {
                    drawScaledImage(g2, mainImg, w, h);
                } else {
                    drawWaitingPlaceholder(g2, w, h);
                }

                g2.dispose();
            }
        };
        remoteVideoPanel.setOpaque(true);
        remoteVideoPanel.setBackground(new Color(10, 12, 18));

        // 2. Panel PiP Local (Capa Flotante 1)
        localPipPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.enableQualityRendering(g2);

                int w = getWidth();
                int h = getHeight();

                // Recorte redondeado para PiP
                g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));

                BufferedImage pipImg = swapViews ? remoteFrame : localFrame;
                if (pipImg != null && (swapViews || callService.isVideoEnabled())) {
                    drawScaledImage(g2, pipImg, w, h);
                } else {
                    g2.setColor(new Color(24, 30, 44));
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    String txt = swapViews ? "Esperando remoto..." : "Cámara apagada";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(txt, (w - fm.stringWidth(txt)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
                }

                // Borde elegante
                g2.setClip(null);
                float activeAudio = swapViews ? remoteAudioLevel : localAudioLevel;
                g2.setColor(activeAudio > 0.05f ? new Color(16, 185, 129) : ThemeManager.getTheme().borderActive);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 14, 14);

                // Badge identificador
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.setColor(new Color(0, 0, 0, 160));
                String badgeText = swapViews ? peerName : "Tú";
                FontMetrics bFm = g2.getFontMetrics();
                int bWidth = bFm.stringWidth(badgeText) + 16;
                g2.fillRoundRect(8, h - 24, bWidth, 16, 8, 8);
                g2.setColor(Color.WHITE);
                g2.drawString(badgeText, 16, h - 12);

                g2.dispose();
            }
        };
        localPipPanel.setOpaque(false);
        localPipPanel.setSize(180, 135);
        localPipPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        localPipPanel.setToolTipText("Haz clic para alternar pantalla completa y miniatura");
        localPipPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                swapViews = !swapViews;
                remoteVideoPanel.repaint();
                localPipPanel.repaint();
            }
        });

        // 3. Barra Superior OSD (Capa Flotante 2)
        JPanel topBar = createTopBar();
        topBar.setSize(800, 54);

        // 4. Barra Inferior de Controles (Capa Flotante 3)
        JPanel bottomBar = createControlDock();
        bottomBar.setSize(520, 64);

        layeredPane.add(remoteVideoPanel, Integer.valueOf(0));
        layeredPane.add(localPipPanel, Integer.valueOf(1));
        layeredPane.add(topBar, Integer.valueOf(2));
        layeredPane.add(bottomBar, Integer.valueOf(3));

        // Listener de redimensión para recolocar elementos flotantes
        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int pw = layeredPane.getWidth();
                int ph = layeredPane.getHeight();

                remoteVideoPanel.setBounds(0, 0, pw, ph);
                localPipPanel.setLocation(pw - localPipPanel.getWidth() - 20, 70);
                topBar.setBounds(20, 14, pw - 40, 50);

                int bw = bottomBar.getWidth();
                int bh = bottomBar.getHeight();
                bottomBar.setBounds((pw - bw) / 2, ph - bh - 24, bw, bh);
            }
        });

        setContentPane(layeredPane);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.enableQualityRendering(g2);
                g2.setColor(new Color(15, 23, 42, 190));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(255, 255, 255, 25));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        // Lado izquierdo: Usuario e info
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        left.setOpaque(false);

        JLabel lblAvatar = new JLabel(Icons.avatar(peerName, 32, ThemeManager.getTheme().primary, Color.WHITE));
        left.add(lblAvatar);

        JPanel details = new JPanel(new GridLayout(2, 1, 0, 1));
        details.setOpaque(false);

        JLabel lblName = new JLabel(peerName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(Color.WHITE);

        lblStatus = new JLabel("Conectando...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblStatus.setForeground(new Color(148, 163, 184));

        details.add(lblName);
        details.add(lblStatus);
        left.add(details);

        // Centro: Temporizador de Duración
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        center.setOpaque(false);

        lblTimer = new JLabel("⏱ 00:00");
        lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTimer.setForeground(new Color(52, 211, 153));
        center.add(lblTimer);

        // Lado derecho: Selector de Fuente y Calidad
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 2));
        right.setOpaque(false);

        JLabel lblQuality = new JLabel("● HD 720p 60 FPS");
        lblQuality.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblQuality.setForeground(new Color(52, 211, 153));
        right.add(lblQuality);

        comboSources = new JComboBox<>(VideoSourceType.values());
        comboSources.setSelectedItem(callService.getVideoSourceType());
        comboSources.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        comboSources.setBackground(ThemeManager.getTheme().bgSidebar);
        comboSources.setForeground(ThemeManager.getTheme().textPrimary);
        comboSources.setFocusable(false);
        comboSources.addActionListener(e -> {
            VideoSourceType selected = (VideoSourceType) comboSources.getSelectedItem();
            if (selected != null) {
                callService.setVideoSourceType(selected);
                updateControlButtons();
            }
        });
        right.add(comboSources);

        bar.add(left, BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createControlDock() {
        JPanel dock = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.enableQualityRendering(g2);
                g2.setColor(new Color(15, 23, 42, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        dock.setOpaque(false);
        dock.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));

        // 1. Micrófono
        btnMic = new ModernButton("", Icons.mic(18, Color.WHITE), ModernButton.Variant.GHOST);
        btnMic.setPreferredSize(new Dimension(46, 42));
        btnMic.setToolTipText("Silenciar / Activar Micrófono");
        btnMic.addActionListener(e -> {
            boolean next = !callService.isMuted();
            callService.setMuted(next);
            updateControlButtons();
        });

        // 2. Cámara
        btnVideo = new ModernButton("", Icons.video(18, Color.WHITE), ModernButton.Variant.GHOST);
        btnVideo.setPreferredSize(new Dimension(46, 42));
        btnVideo.setToolTipText("Encender / Apagar Cámara");
        btnVideo.addActionListener(e -> {
            boolean next = !callService.isVideoEnabled();
            callService.setVideoEnabled(next);
            updateControlButtons();
        });

        // 3. Compartir Pantalla
        btnScreen = new ModernButton("", Icons.screenShare(18, Color.WHITE), ModernButton.Variant.GHOST);
        btnScreen.setPreferredSize(new Dimension(46, 42));
        btnScreen.setToolTipText("Compartir Pantalla Completa");
        btnScreen.addActionListener(e -> {
            if (callService.getVideoSourceType() == VideoSourceType.SCREEN_SHARE) {
                callService.setVideoSourceType(VideoSourceType.VIRTUAL_CAMERA);
                comboSources.setSelectedItem(VideoSourceType.VIRTUAL_CAMERA);
            } else {
                callService.setVideoSourceType(VideoSourceType.SCREEN_SHARE);
                comboSources.setSelectedItem(VideoSourceType.SCREEN_SHARE);
            }
            updateControlButtons();
        });

        // 4. Colgar (Rojo)
        ModernButton btnHangup = new ModernButton("", Icons.phoneEnd(20, Color.WHITE), ModernButton.Variant.DANGER);
        btnHangup.setPreferredSize(new Dimension(54, 42));
        btnHangup.setToolTipText("Finalizar llamada");
        btnHangup.addActionListener(e -> cleanupAndClose());

        dock.add(btnMic);
        dock.add(btnVideo);
        dock.add(btnScreen);
        dock.add(btnHangup);

        updateControlButtons();
        return dock;
    }

    private void updateControlButtons() {
        boolean isMuted = callService.isMuted();
        btnMic.setIcon(isMuted ? Icons.micOff(18, Color.WHITE) : Icons.mic(18, Color.WHITE));
        btnMic.setBackground(isMuted ? ThemeManager.getTheme().danger : ThemeManager.getTheme().bgCard);

        boolean isVideoOn = callService.isVideoEnabled();
        btnVideo.setIcon(isVideoOn ? Icons.video(18, Color.WHITE) : Icons.videoOff(18, Color.WHITE));
        btnVideo.setBackground(!isVideoOn ? ThemeManager.getTheme().danger : ThemeManager.getTheme().bgCard);

        boolean isSharing = callService.getVideoSourceType() == VideoSourceType.SCREEN_SHARE;
        btnScreen.setBackground(isSharing ? new Color(16, 185, 129) : ThemeManager.getTheme().bgCard);
    }

    private void drawScaledImage(Graphics2D g2, BufferedImage img, int targetW, int targetH) {
        double imgAspect = (double) img.getWidth() / img.getHeight();
        double targetAspect = (double) targetW / targetH;

        int dw, dh, dx, dy;
        if (imgAspect > targetAspect) {
            dw = targetW;
            dh = (int) (targetW / imgAspect);
            dx = 0;
            dy = (targetH - dh) / 2;
        } else {
            dh = targetH;
            dw = (int) (targetH * imgAspect);
            dx = (targetW - dw) / 2;
            dy = 0;
        }

        g2.drawImage(img, dx, dy, dw, dh, null);
    }

    private void drawWaitingPlaceholder(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(15, 23, 42));
        g2.fillRect(0, 0, w, h);

        int cx = w / 2;
        int cy = h / 2 - 20;

        float r = 52.0f;
        g2.setColor(new Color(139, 92, 246, 70));
        g2.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));

        String initial = (peerName != null && !peerName.isEmpty()) ? peerName.substring(0, 1).toUpperCase() : "?";
        g2.setFont(new Font("Segoe UI", Font.BOLD, 42));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(initial, (int) (cx - fm.stringWidth(initial) / 2.0f), (int) (cy - fm.getHeight() / 2.0f + fm.getAscent()));

        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        String title = (callService.getCallState() == CallState.OUTGOING_CALL)
                ? "Llamando a " + peerName + "..."
                : "Esperando señal de video de " + peerName + "...";
        FontMetrics fm2 = g2.getFontMetrics();
        g2.setColor(new Color(248, 250, 252));
        g2.drawString(title, (w - fm2.stringWidth(title)) / 2, cy + 80);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String sub = "La transmisión iniciará de forma automática en cuanto responda.";
        FontMetrics fm3 = g2.getFontMetrics();
        g2.setColor(new Color(148, 163, 184));
        g2.drawString(sub, (w - fm3.stringWidth(sub)) / 2, cy + 104);
    }

    private void startTimer() {
        callStartTime = System.currentTimeMillis();
        durationTimer = new javax.swing.Timer(1000, e -> {
            if (callService.getCallState() == CallState.CONNECTED) {
                long elapsed = (System.currentTimeMillis() - callStartTime) / 1000;
                lblTimer.setText(String.format("⏱ %02d:%02d", elapsed / 60, elapsed % 60));
            }
        });
        durationTimer.start();
    }

    private void cleanupAndClose() {
        if (durationTimer != null && durationTimer.isRunning()) {
            durationTimer.stop();
        }
        callService.removeListener(this);
        callService.endCall();
        dispose();
    }

    // --- Implementación de VideoCallListener ---

    @Override
    public void onCallStateChanged(CallState newState, String peerName, String message) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(message != null ? message : "");
            if (newState == CallState.CONNECTED) {
                callStartTime = System.currentTimeMillis();
                lblStatus.setText("En videollamada HD");
            } else if (newState == CallState.ENDED) {
                cleanupAndClose();
            }
            remoteVideoPanel.repaint();
        });
    }

    @Override
    public void onIncomingCallReceived(String callerName, String peerIp, int mediaPort) {
        // No aplica dentro de la ventana de llamada activa
    }

    @Override
    public void onLocalFrameAvailable(BufferedImage frame) {
        this.localFrame = frame;
        localPipPanel.repaint();
    }

    @Override
    public void onRemoteFrameAvailable(BufferedImage frame) {
        this.remoteFrame = frame;
        remoteVideoPanel.repaint();
    }

    @Override
    public void onAudioLevelsUpdated(float localLevel, float remoteLevel) {
        this.localAudioLevel = localLevel;
        this.remoteAudioLevel = remoteLevel;
    }
}

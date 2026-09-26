package com.chatlocal.ui.dialogs.videocall;

import com.chatlocal.backend.service.videocall.VideoCallService;
import com.chatlocal.ui.components.ModernButton;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.Theme;
import com.chatlocal.ui.theme.ThemeManager;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// ventana que avisa cuando entra una llamada
public class IncomingCallDialog extends JDialog {

    private final VideoCallService callService;
    private final String callerName;
    private final Runnable onAcceptCallback;

    private volatile boolean ringing = true;
    private Thread ringtoneThread;
    private int pulseFrame = 0;
    private javax.swing.Timer animTimer;

    public IncomingCallDialog(Frame parent, VideoCallService callService, String callerName, Runnable onAcceptCallback) {
        super(parent, "Videollamada Entrante", true);
        this.callService = callService;
        this.callerName = (callerName != null && !callerName.isEmpty()) ? callerName : "Contacto";
        this.onAcceptCallback = onAcceptCallback;

        setUndecorated(true);
        setSize(380, 260);
        setLocationRelativeTo(parent);
        setBackground(new Color(0, 0, 0, 0));
        Theme.applyAppIcon(this);

        buildUI();
        startRingtone();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopRingtone();
                if (callService != null) callService.rejectCall("Llamada no atendida");
            }
        });
    }

    private void buildUI() {
        JPanel card = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.enableQualityRendering(g2);

                // Fondo oscuro con sombra y bordes redondeados
                g2.setColor(ThemeManager.getTheme().bgSidebar);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 18, 18);

                g2.setColor(ThemeManager.getTheme().borderActive);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 18, 18);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

        // Centro: Avatar con halo y textos
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // Panel de avatar con halo pulsante
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.enableQualityRendering(g2);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // Ondas pulsantes de llamada
                double pulse = Math.sin(pulseFrame * 0.15) * 6.0;
                float r1 = 38.0f + (float) pulse;
                g2.setColor(new Color(16, 185, 129, 60));
                g2.fillOval((int) (cx - r1), (int) (cy - r1), (int) (r1 * 2), (int) (r1 * 2));

                float r2 = 32.0f;
                g2.setColor(new Color(16, 185, 129, 140));
                g2.drawOval((int) (cx - r2), (int) (cy - r2), (int) (r2 * 2), (int) (r2 * 2));

                g2.dispose();
            }
        };
        avatarPanel.setOpaque(false);
        avatarPanel.setPreferredSize(new Dimension(84, 84));
        avatarPanel.setMaximumSize(new Dimension(84, 84));
        avatarPanel.setLayout(new GridBagLayout());

        JLabel lblAvatar = new JLabel(Icons.avatar(callerName, 56, ThemeManager.getTheme().primary, Color.WHITE));
        avatarPanel.add(lblAvatar);

        centerPanel.add(avatarPanel);
        centerPanel.add(Box.createVerticalStrut(10));

        JLabel lblTitle = new JLabel("📹 Videollamada Entrante");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(ThemeManager.getTheme().textPrimary);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblCaller = new JLabel(callerName + " te está llamando...");
        lblCaller.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCaller.setForeground(ThemeManager.getTheme().textMuted);
        lblCaller.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(lblTitle);
        centerPanel.add(Box.createVerticalStrut(4));
        centerPanel.add(lblCaller);

        // Barra inferior: Botones de Acción
        JPanel actions = new JPanel(new GridLayout(1, 2, 16, 0));
        actions.setOpaque(false);

        ModernButton btnReject = new ModernButton("Rechazar", Icons.phoneEnd(16, Color.WHITE), ModernButton.Variant.DANGER);
        btnReject.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReject.setPreferredSize(new Dimension(130, 42));
        btnReject.addActionListener(e -> {
            stopRingtone();
            dispose();
            if (callService != null) callService.rejectCall("Rechazada por el usuario");
        });

        ModernButton btnAccept = new ModernButton("Aceptar", Icons.phone(16, Color.WHITE), ModernButton.Variant.SUCCESS);
        btnAccept.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAccept.setPreferredSize(new Dimension(130, 42));
        btnAccept.addActionListener(e -> {
            stopRingtone();
            dispose();
            if (callService != null) callService.acceptCall();
            if (onAcceptCallback != null) onAcceptCallback.run();
        });

        actions.add(btnReject);
        actions.add(btnAccept);

        card.add(centerPanel, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);

        setContentPane(card);

        // Animación de pulso visual (30 FPS)
        animTimer = new javax.swing.Timer(33, e -> {
            pulseFrame++;
            avatarPanel.repaint();
        });
        animTimer.start();
    }

    private void startRingtone() {
        ringtoneThread = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) return;

                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format, 4096);
                line.start();

                // Generar ciclo de notas de timbre agradable (Acordes suaves C5 -> E5 -> G5)
                byte[] chime = createChimeBuffer(16000);

                while (ringing) {
                    line.write(chime, 0, chime.length);
                    // Pausa entre timbrazos
                    for (int i = 0; i < 15 && ringing; i++) {
                        Thread.sleep(100);
                    }
                }

                line.stop();
                line.close();
            } catch (Exception ignored) {}
        }, "incoming-ringtone-worker");
        ringtoneThread.setDaemon(true);
        ringtoneThread.start();
    }

    private byte[] createChimeBuffer(int sampleRate) {
        double durationSecs = 1.2;
        int totalSamples = (int) (sampleRate * durationSecs);
        byte[] buffer = new byte[totalSamples * 2];

        double[] freqs = {523.25, 659.25, 783.99}; // Do5, Mi5, Sol5
        double noteDuration = durationSecs / freqs.length;

        for (int i = 0; i < totalSamples; i++) {
            double t = (double) i / sampleRate;
            int noteIdx = Math.min(freqs.length - 1, (int) (t / noteDuration));
            double noteTime = t - (noteIdx * noteDuration);
            double freq = freqs[noteIdx];

            // Envolvente de decaimiento suave
            double envelope = Math.exp(-noteTime * 5.0);
            double sample = Math.sin(2.0 * Math.PI * freq * t) * envelope * 0.35;

            short s = (short) (sample * 32767);
            buffer[i * 2] = (byte) (s & 0xFF);
            buffer[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return buffer;
    }

    private void stopRingtone() {
        ringing = false;
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        if (ringtoneThread != null) {
            ringtoneThread.interrupt();
            ringtoneThread = null;
        }
    }

    @Override
    public void dispose() {
        stopRingtone();
        super.dispose();
    }
}

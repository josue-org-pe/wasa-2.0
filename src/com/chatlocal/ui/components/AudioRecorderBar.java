package com.chatlocal.ui.components;

import com.chatlocal.backend.service.AudioRecorderService;
import com.chatlocal.ui.theme.Icons;
import com.chatlocal.ui.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

// barra que sale cuando estas grabando una nota de voz
public class AudioRecorderBar extends JPanel {

    public interface AudioRecordCallback {
        void onAudioReady(File audioFile, int durationSeconds);
        void onAudioCancelled();
    }

    private final AudioRecorderService recorder;
    private final AudioRecordCallback callback;

    private JLabel lblTimer;
    private Timer pulseTimer;
    private int elapsedSeconds = 0;
    private float pulseAlpha = 0.5f;
    private boolean alphaIncreasing = true;

    public AudioRecorderBar(AudioRecorderService recorder, AudioRecordCallback callback) {
        this.recorder = recorder;
        this.callback = callback;

        setOpaque(true);
        setBackground(ThemeManager.getTheme().bgCard);
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        buildUI();
    }

    private void buildUI() {
        // Indicador izquierdo con punto rojo parpadeante y texto
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Punto rojo pulsante
                g2.setColor(new Color(1.0f, 0.2f, 0.2f, Math.max(0.2f, Math.min(1.0f, pulseAlpha))));
                g2.fillOval(4, 11, 12, 12);
                g2.dispose();
            }
        };
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(190, 36));

        lblTimer = new JLabel("  Grabando... 00:00");
        lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTimer.setForeground(new Color(0xF8, 0x71, 0x71));
        left.add(lblTimer);

        // Centro: visualización de ondas simples
        JPanel center = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getTheme().primary);
                int h = getHeight();
                int w = getWidth();
                int barCount = Math.max(10, w / 8);
                for (int i = 0; i < barCount; i++) {
                    int barH = (int) (8 + Math.abs(Math.sin((elapsedSeconds * 4 + i) * 0.4)) * (h - 16));
                    int y = (h - barH) / 2;
                    g2.fillRect(i * 8, y, 4, barH);
                }
                g2.dispose();
            }
        };
        center.setOpaque(false);

        // Derecha: Botones de Cancelar y Enviar
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.setOpaque(false);

        ModernButton btnCancel = new ModernButton("Cancelar", ModernButton.Variant.GHOST);
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.setMargin(new Insets(4, 10, 4, 10));
        btnCancel.addActionListener(e -> cancel());

        ModernButton btnSend = new ModernButton("Enviar Audio", Icons.send(14, Color.WHITE), ModernButton.Variant.PRIMARY);
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSend.setMargin(new Insets(6, 12, 6, 12));
        btnSend.addActionListener(e -> finishAndSend());

        right.add(btnCancel);
        right.add(btnSend);

        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    public void startRecording() {
        elapsedSeconds = 0;
        lblTimer.setText("  Grabando... 00:00");
        setVisible(true);

        boolean ok = recorder.startRecording();
        if (!ok) {
            cancel();
            return;
        }

        pulseTimer = new Timer(100, e -> {
            if (alphaIncreasing) {
                pulseAlpha += 0.08f;
                if (pulseAlpha >= 1.0f) alphaIncreasing = false;
            } else {
                pulseAlpha -= 0.08f;
                if (pulseAlpha <= 0.2f) alphaIncreasing = true;
            }

            if (e.getSource() != null) {
                // Cada segundo
                long now = System.currentTimeMillis();
            }
            repaint();
        });
        pulseTimer.start();

        Timer secondsTimer = new Timer(1000, e -> {
            if (recorder.isRecording()) {
                elapsedSeconds++;
                int mins = elapsedSeconds / 60;
                int secs = elapsedSeconds % 60;
                lblTimer.setText(String.format("  Grabando... %02d:%02d", mins, secs));
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        secondsTimer.start();
    }

    private void finishAndSend() {
        if (pulseTimer != null) pulseTimer.stop();
        AudioRecorderService.AudioRecordResult result = recorder.stopRecording();
        setVisible(false);
        if (result != null && callback != null) {
            callback.onAudioReady(result.getFile(), result.getDurationSeconds());
        }
    }

    private void cancel() {
        if (pulseTimer != null) pulseTimer.stop();
        recorder.cancelRecording();
        setVisible(false);
        if (callback != null) {
            callback.onAudioCancelled();
        }
    }
}

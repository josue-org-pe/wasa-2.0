package com.chatlocal.backend.service;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Servicio nativo para grabación y reproducción de notas de voz en formato WAV.
 * Utiliza 100% la Java Sound API estándar (javax.sound.sampled) sin librerías externas.
 */
public class AudioRecorderService {

    private TargetDataLine targetLine;
    private File currentAudioFile;
    private long recordStartTime = 0;
    private volatile boolean isRecording = false;

    private Clip activeClip;

    /**
     * Inicia la captura de audio desde el micrófono en un hilo en segundo plano.
     */
    public synchronized boolean startRecording() {
        if (isRecording) return false;

        try {
            // Formato de voz optimizado: 16kHz, 16-bit mono PCM (calidad nítida y peso ligero)
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Micrófono no soportado en este sistema");
                return false;
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();

            File storageDir = FileTransferManager.getReceivedFilesFolder();
            currentAudioFile = new File(storageDir, "audio_nota_" + System.currentTimeMillis() + ".wav");
            recordStartTime = System.currentTimeMillis();
            isRecording = true;

            Thread recordThread = new Thread(() -> {
                try (AudioInputStream ais = new AudioInputStream(targetLine)) {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, currentAudioFile);
                } catch (IOException e) {
                    System.err.println("Error grabando flujo de audio: " + e.getMessage());
                }
            }, "audio-recorder-thread");
            recordThread.setDaemon(true);
            recordThread.start();

            return true;
        } catch (LineUnavailableException e) {
            System.err.println("Línea de audio no disponible: " + e.getMessage());
            return false;
        }
    }

    /**
     * Detiene la grabación y retorna el archivo generado junto a su duración en segundos.
     */
    public synchronized AudioRecordResult stopRecording() {
        if (!isRecording || targetLine == null) {
            return null;
        }

        isRecording = false;
        long durationMs = System.currentTimeMillis() - recordStartTime;
        int durationSecs = Math.max(1, (int) Math.round(durationMs / 1000.0));

        targetLine.stop();
        targetLine.close();
        targetLine = null;

        // Pequeña pausa para asegurar flush a disco
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}

        if (currentAudioFile != null && currentAudioFile.exists() && currentAudioFile.length() > 0) {
            return new AudioRecordResult(currentAudioFile, durationSecs);
        }
        return null;
    }

    /**
     * Cancela la grabación y elimina el archivo temporal.
     */
    public synchronized void cancelRecording() {
        if (isRecording && targetLine != null) {
            isRecording = false;
            targetLine.stop();
            targetLine.close();
            targetLine = null;
        }
        if (currentAudioFile != null && currentAudioFile.exists()) {
            currentAudioFile.delete();
            currentAudioFile = null;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Reproduce una nota de voz y ejecuta el callback al finalizar.
     */
    public synchronized void play(File audioFile, Runnable onFinished) {
        stopPlayback();
        if (audioFile == null || !audioFile.exists()) return;

        new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile)) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                activeClip = clip;

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        if (onFinished != null) {
                            javax.swing.SwingUtilities.invokeLater(onFinished);
                        }
                    }
                });

                clip.start();
            } catch (Exception e) {
                System.err.println("Error al reproducir nota de voz: " + e.getMessage());
                if (onFinished != null) {
                    javax.swing.SwingUtilities.invokeLater(onFinished);
                }
            }
        }, "audio-player-worker").start();
    }

    public synchronized void stopPlayback() {
        if (activeClip != null) {
            try {
                if (activeClip.isRunning()) activeClip.stop();
                activeClip.close();
            } catch (Exception ignored) {}
            activeClip = null;
        }
    }

    public static class AudioRecordResult {
        private final File file;
        private final int durationSeconds;

        public AudioRecordResult(File file, int durationSeconds) {
            this.file = file;
            this.durationSeconds = durationSeconds;
        }

        public File getFile() {
            return file;
        }

        public int getDurationSeconds() {
            return durationSeconds;
        }
    }
}

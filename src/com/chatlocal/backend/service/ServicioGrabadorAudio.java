package com.chatlocal.backend.service;

import javax.sound.sampled.*;
import java.io.*;

// graba y reproduce los audios de voz en wav
public class ServicioGrabadorAudio {

    private TargetDataLine targetLine;
    private AudioFormat currentFormat;
    private ByteArrayOutputStream pcmBuffer;
    private File currentAudioFile;
    private long recordStartTime = 0;
    private volatile boolean isRecording = false;

    private SourceDataLine activeSourceLine;
    private volatile boolean isPlaying = false;

    /**
     * Inicia la captura de audio desde el micrófono probando formatos estándar compatibles.
     */
    public synchronized boolean startRecording() {
        if (isRecording) return false;

        float[] sampleRates = {16000.0f, 44100.0f, 48000.0f, 8000.0f};
        targetLine = null;

        for (float rate : sampleRates) {
            AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                try {
                    targetLine = (TargetDataLine) AudioSystem.getLine(info);
                    targetLine.open(format, 4096);
                    targetLine.start();
                    currentFormat = format;
                    break;
                } catch (Exception ignored) {}
            }
        }

        if (targetLine == null) {
            System.err.println("Micrófono no soportado o actualmente en uso");
            return false;
        }

        pcmBuffer = new ByteArrayOutputStream();
        File storageDir = GestorTransferenciaArchivos.getReceivedFilesFolder();
        currentAudioFile = new File(storageDir, "audio_nota_" + System.currentTimeMillis() + ".wav");
        recordStartTime = System.currentTimeMillis();
        isRecording = true;

        Thread recordThread = new Thread(() -> {
            byte[] buf = new byte[1024];
            while (isRecording && targetLine != null && targetLine.isOpen()) {
                int read = targetLine.read(buf, 0, buf.length);
                if (read > 0) {
                    synchronized (pcmBuffer) {
                        pcmBuffer.write(buf, 0, read);
                    }
                }
            }
        }, "audio-recorder-thread");
        recordThread.setDaemon(true);
        recordThread.start();

        return true;
    }

    /**
     * Detiene la grabación, finaliza el archivo WAV y retorna el resultado.
     */
    public synchronized AudioRecordResult stopRecording() {
        if (!isRecording) return null;
        isRecording = false;

        if (targetLine != null) {
            try {
                targetLine.stop();
                targetLine.close();
            } catch (Exception ignored) {}
            targetLine = null;
        }

        try { Thread.sleep(60); } catch (InterruptedException ignored) {}

        byte[] pcmData;
        synchronized (pcmBuffer) {
            pcmData = pcmBuffer.toByteArray();
        }

        if (pcmData.length == 0 || currentFormat == null) {
            return null;
        }

        long durationMs = System.currentTimeMillis() - recordStartTime;
        int durationSecs = Math.max(1, (int) Math.round(durationMs / 1000.0));

        // Escribir archivo WAV con cabecera canónica exacta
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
             AudioInputStream ais = new AudioInputStream(bais, currentFormat, pcmData.length / currentFormat.getFrameSize())) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, currentAudioFile);
        } catch (IOException e) {
            System.err.println("Error finalizando archivo WAV: " + e.getMessage());
            return null;
        }

        if (currentAudioFile.exists() && currentAudioFile.length() > 44) {
            return new AudioRecordResult(currentAudioFile, durationSecs);
        }
        return null;
    }

    public synchronized void cancelRecording() {
        isRecording = false;
        if (targetLine != null) {
            try {
                targetLine.stop();
                targetLine.close();
            } catch (Exception ignored) {}
            targetLine = null;
        }
        if (currentAudioFile != null && currentAudioFile.exists()) {
            currentAudioFile.delete();
            currentAudioFile = null;
        }
        if (pcmBuffer != null) {
            pcmBuffer.reset();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Reproduce una nota de voz mediante SourceDataLine y ejecuta el callback al finalizar.
     */
    public synchronized void play(File audioFile, Runnable onFinished) {
        stopPlayback();
        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0) {
            if (onFinished != null) javax.swing.SwingUtilities.invokeLater(onFinished);
            return;
        }

        isPlaying = true;
        new Thread(() -> {
            try (AudioInputStream rawAis = AudioSystem.getAudioInputStream(audioFile)) {
                AudioFormat baseFormat = rawAis.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        baseFormat.getSampleRate(),
                        16,
                        baseFormat.getChannels(),
                        baseFormat.getChannels() * 2,
                        baseFormat.getSampleRate(),
                        false
                );

                try (AudioInputStream dais = AudioSystem.getAudioInputStream(decodedFormat, rawAis)) {
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(decodedFormat);
                    line.start();
                    synchronized (this) {
                        activeSourceLine = line;
                    }

                    byte[] buffer = new byte[4096];
                    int read;
                    while (isPlaying && (read = dais.read(buffer, 0, buffer.length)) != -1) {
                        line.write(buffer, 0, read);
                    }
                    if (isPlaying) {
                        line.drain();
                    }
                    line.stop();
                    line.close();
                }
            } catch (Exception e) {
                System.err.println("Error al reproducir audio: " + e.getMessage());
            } finally {
                synchronized (this) {
                    activeSourceLine = null;
                    isPlaying = false;
                }
                if (onFinished != null) {
                    javax.swing.SwingUtilities.invokeLater(onFinished);
                }
            }
        }, "audio-player-worker").start();
    }

    public synchronized void stopPlayback() {
        isPlaying = false;
        if (activeSourceLine != null) {
            try {
                activeSourceLine.stop();
                activeSourceLine.close();
            } catch (Exception ignored) {}
            activeSourceLine = null;
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

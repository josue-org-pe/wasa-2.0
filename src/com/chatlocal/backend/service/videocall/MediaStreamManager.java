package com.chatlocal.backend.service.videocall;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// aqui transmitimos el audio y video por paquetes udp
public class MediaStreamManager {

    private static final byte PKT_VIDEO = 1;
    private static final byte PKT_AUDIO = 2;
    private static final int CHUNK_SIZE = 1200;
    private static final int TARGET_FPS = 60;
    private static final int FRAME_INTERVAL_MS = 1000 / TARGET_FPS;

    public interface MediaFrameConsumer {
        void onLocalFrame(BufferedImage frame);
        void onRemoteFrame(BufferedImage frame);
        void onAudioLevels(float localLevel, float remoteLevel);
    }

    private final MediaFrameConsumer consumer;
    private DatagramSocket udpSocket;
    private int localPort;

    private InetAddress remoteAddress;
    private int remotePort = -1;

    private volatile boolean running = false;
    private volatile boolean micMuted = false;
    private volatile boolean videoMuted = false;

    private VideoSource currentVideoSource;
    private final AtomicInteger frameIdCounter = new AtomicInteger(1);
    private final AtomicInteger audioSeqCounter = new AtomicInteger(1);

    // Audio lines
    private TargetDataLine targetLine; // Micrófono
    private SourceDataLine sourceLine; // Altavoz
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(16000.0f, 16, 1, true, false);

    private float localRmsLevel = 0.0f;
    private float remoteRmsLevel = 0.0f;

    // Hilos de trabajo
    private Thread videoSendThread;
    private Thread audioSendThread;
    private Thread receiveThread;

    // Ensamblador de cuadros de video UDP
    private static class PartialFrame {
        final int totalChunks;
        final byte[][] chunks;
        int receivedCount = 0;
        final long creationTime = System.currentTimeMillis();

        PartialFrame(int totalChunks) {
            this.totalChunks = totalChunks;
            this.chunks = new byte[totalChunks][];
        }
    }

    private final Map<Integer, PartialFrame> frameAssembler = new ConcurrentHashMap<>();

    public MediaStreamManager(VideoSource initialSource, MediaFrameConsumer consumer) throws SocketException {
        this.consumer = consumer;
        this.currentVideoSource = initialSource;
        // Asignar puerto UDP dinámico libre con búferes ampliados para HD 60 FPS
        this.udpSocket = new DatagramSocket(0);
        try {
            this.udpSocket.setSendBufferSize(4 * 1024 * 1024);
            this.udpSocket.setReceiveBufferSize(8 * 1024 * 1024);
        } catch (Exception ignored) {}
        this.localPort = udpSocket.getLocalPort();
    }

    public int getLocalPort() {
        return localPort;
    }

    public synchronized void setRemoteTarget(String ip, int port) {
        try {
            this.remoteAddress = InetAddress.getByName(ip);
            this.remotePort = port;
        } catch (UnknownHostException e) {
            System.err.println("Error resolviendo IP remota para medios: " + e.getMessage());
        }
    }

    public synchronized void setVideoSource(VideoSource newSource) {
        if (newSource != null) {
            VideoSource old = this.currentVideoSource;
            this.currentVideoSource = newSource;
            if (old != null && old != newSource) {
                old.close();
            }
        }
    }

    public void setMicMuted(boolean muted) {
        this.micMuted = muted;
    }

    public boolean isMicMuted() {
        return micMuted;
    }

    public void setVideoMuted(boolean muted) {
        this.videoMuted = muted;
    }

    public boolean isVideoMuted() {
        return videoMuted;
    }

    public synchronized void start() {
        if (running) return;
        running = true;

        initAudioLines();

        // 1. Hilo de recepción UDP (Video + Audio)
        receiveThread = new Thread(this::receiveLoop, "media-udp-receiver");
        receiveThread.setDaemon(true);
        receiveThread.start();

        // 2. Hilo de envío de Video
        videoSendThread = new Thread(this::videoSendLoop, "media-video-sender");
        videoSendThread.setDaemon(true);
        videoSendThread.start();

        // 3. Hilo de envío de Audio
        audioSendThread = new Thread(this::audioSendLoop, "media-audio-sender");
        audioSendThread.setDaemon(true);
        audioSendThread.start();
    }

    private void initAudioLines() {
        // Inicializar altavoz (SourceDataLine)
        try {
            DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
            if (AudioSystem.isLineSupported(sourceInfo)) {
                sourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
                sourceLine.open(AUDIO_FORMAT, 4096);
                sourceLine.start();
            }
        } catch (Exception e) {
            System.err.println("Aviso: Altavoz no disponible para llamada: " + e.getMessage());
        }

        // Inicializar micrófono (TargetDataLine) con fallback
        float[] sampleRates = {16000.0f, 8000.0f, 44100.0f};
        for (float rate : sampleRates) {
            try {
                AudioFormat testFormat = new AudioFormat(rate, 16, 1, true, false);
                DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, testFormat);
                if (AudioSystem.isLineSupported(targetInfo)) {
                    targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
                    targetLine.open(testFormat, 4096);
                    targetLine.start();
                    break;
                }
            } catch (LineUnavailableException e) {
                // Micrófono en uso por otra instancia local
                break;
            } catch (Exception ignored) {}
        }
    }

    private void videoSendLoop() {
        ImageWriter writer = null;
        ImageWriteParam param = null;
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (writers.hasNext()) {
                writer = writers.next();
                param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.62f);
            }
        } catch (Exception ignored) {}

        while (running) {
            long loopStart = System.currentTimeMillis();
            try {
                if (currentVideoSource != null) {
                    BufferedImage frame = currentVideoSource.captureFrame();
                    if (frame != null && consumer != null) {
                        consumer.onLocalFrame(frame);

                        if (!videoMuted && remoteAddress != null && remotePort > 0 && writer != null) {
                            sendVideoFrame(frame, writer, param);
                        }
                    }
                }
            } catch (Exception e) {
                // Prevenir interrupción del hilo por errores puntuales de compresión
            }

            long elapsed = System.currentTimeMillis() - loopStart;
            long sleepTime = Math.max(1, FRAME_INTERVAL_MS - elapsed);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                break;
            }
        }

        if (writer != null) {
            writer.dispose();
        }
    }

    private void sendVideoFrame(BufferedImage frame, ImageWriter writer, ImageWriteParam param) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(frame, null, null), param);
        }

        byte[] rawBytes = baos.toByteArray();
        int totalChunks = (int) Math.ceil((double) rawBytes.length / CHUNK_SIZE);
        int frameId = frameIdCounter.getAndIncrement();

        for (int i = 0; i < totalChunks && running; i++) {
            int offset = i * CHUNK_SIZE;
            int length = Math.min(CHUNK_SIZE, rawBytes.length - offset);

            ByteArrayOutputStream pktStream = new ByteArrayOutputStream(length + 14);
            DataOutputStream dos = new DataOutputStream(pktStream);
            dos.writeByte(PKT_VIDEO);
            dos.writeInt(frameId);
            dos.writeShort(totalChunks);
            dos.writeShort(i);
            dos.writeInt(length);
            dos.write(rawBytes, offset, length);
            dos.flush();

            byte[] pktData = pktStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(pktData, pktData.length, remoteAddress, remotePort);
            udpSocket.send(packet);
        }
    }

    private void audioSendLoop() {
        byte[] buffer = new byte[640]; // 20ms a 16kHz 16-bit mono (320 muestras * 2 bytes)
        while (running) {
            if (targetLine == null || !targetLine.isOpen()) {
                try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                continue;
            }

            int read = targetLine.read(buffer, 0, buffer.length);
            if (read > 0) {
                localRmsLevel = calculateRms(buffer, read);
                if (currentVideoSource != null) {
                    currentVideoSource.setAudioLevel(localRmsLevel);
                }

                if (consumer != null) {
                    consumer.onAudioLevels(localRmsLevel, remoteRmsLevel);
                }

                if (!micMuted && remoteAddress != null && remotePort > 0) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(read + 9);
                        DataOutputStream dos = new DataOutputStream(baos);
                        dos.writeByte(PKT_AUDIO);
                        dos.writeInt(audioSeqCounter.getAndIncrement());
                        dos.writeInt(read);
                        dos.write(buffer, 0, read);
                        dos.flush();

                        byte[] data = baos.toByteArray();
                        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);
                        udpSocket.send(packet);
                    } catch (IOException ignored) {}
                }
            }
        }
    }

    private void receiveLoop() {
        byte[] receiveBuf = new byte[65535];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(receiveBuf, receiveBuf.length);
                udpSocket.receive(packet);

                if (remoteAddress == null || remotePort <= 0) {
                    remoteAddress = packet.getAddress();
                    remotePort = packet.getPort();
                }

                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
                byte type = dis.readByte();

                if (type == PKT_VIDEO) {
                    int fId = dis.readInt();
                    short total = dis.readShort();
                    short idx = dis.readShort();
                    int len = dis.readInt();
                    byte[] chunkData = new byte[len];
                    dis.readFully(chunkData);

                    handleIncomingVideoChunk(fId, total, idx, chunkData);
                } else if (type == PKT_AUDIO) {
                    int seq = dis.readInt();
                    int len = dis.readInt();
                    byte[] pcm = new byte[len];
                    dis.readFully(pcm);

                    handleIncomingAudio(pcm, len);
                }
            } catch (SocketException e) {
                if (!running) break;
            } catch (Exception e) {
                // Ignorar paquetes corruptos o aislados
            }
        }
    }

    private void handleIncomingVideoChunk(int frameId, int totalChunks, int chunkIndex, byte[] chunkData) {
        // Limpieza de cuadros viejos (más de 1.5s sin completarse)
        long now = System.currentTimeMillis();
        frameAssembler.entrySet().removeIf(entry -> now - entry.getValue().creationTime > 1500);

        PartialFrame pf = frameAssembler.computeIfAbsent(frameId, k -> new PartialFrame(totalChunks));
        if (chunkIndex >= 0 && chunkIndex < totalChunks && pf.chunks[chunkIndex] == null) {
            pf.chunks[chunkIndex] = chunkData;
            pf.receivedCount++;

            if (pf.receivedCount == pf.totalChunks) {
                frameAssembler.remove(frameId);
                // Reensamblar y decodificar
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (byte[] c : pf.chunks) {
                    if (c != null) baos.write(c, 0, c.length);
                }

                try {
                    BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
                    if (decoded != null && consumer != null) {
                        consumer.onRemoteFrame(decoded);
                    }
                } catch (IOException ignored) {}
            }
        }
    }

    private void handleIncomingAudio(byte[] pcmData, int length) {
        remoteRmsLevel = calculateRms(pcmData, length);
        if (sourceLine != null && sourceLine.isOpen()) {
            sourceLine.write(pcmData, 0, length);
        }
    }

    private float calculateRms(byte[] pcm, int length) {
        long sum = 0;
        int samples = length / 2;
        if (samples == 0) return 0.0f;

        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            sum += (long) sample * sample;
        }
        double rms = Math.sqrt((double) sum / samples);
        // Normalizar 0.0 a 1.0
        return (float) Math.min(1.0, rms / 12000.0);
    }

    public synchronized void stop() {
        running = false;

        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }

        if (targetLine != null) {
            try {
                targetLine.stop();
                targetLine.close();
            } catch (Exception ignored) {}
            targetLine = null;
        }

        if (sourceLine != null) {
            try {
                sourceLine.stop();
                sourceLine.close();
            } catch (Exception ignored) {}
            sourceLine = null;
        }

        if (currentVideoSource != null) {
            currentVideoSource.close();
        }

        frameAssembler.clear();
    }
}

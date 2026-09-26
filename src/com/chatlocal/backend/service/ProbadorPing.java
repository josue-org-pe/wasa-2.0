package com.chatlocal.backend.service;

import com.chatlocal.backend.network.ConstantesProtocolo;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

// clase para medir el ping y ver si el otro lado responde rapido
public final class ProbadorPing {

    private ProbadorPing() {}

    public static class PingResult {
        private final boolean success;
        private final long latencyMs;
        private final String message;

        public PingResult(boolean success, long latencyMs, String message) {
            this.success = success;
            this.latencyMs = latencyMs;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getLatencyMs() {
            return latencyMs;
        }

        public String getMessage() {
            return message;
        }

        public String getFormattedQuality() {
            if (!success) return message;
            if (latencyMs < 50) return latencyMs + " ms — Excelente";
            if (latencyMs < 150) return latencyMs + " ms — Buena";
            return latencyMs + " ms — Conexión lenta";
        }
    }

    public interface PingCallback {
        void onPingComplete(PingResult result);
    }

    // manda un ping por el socket y espera el pong
    public static PingResult measurePing(String host, int port, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(timeoutMs);
            socket.connect(new InetSocketAddress(host, port), timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Enviar Ping
            out.writeByte(ConstantesProtocolo.TYPE_PING);
            out.flush();

            // Leer respuesta Pong
            byte response = in.readByte();
            long rtt = System.currentTimeMillis() - startTime;

            if (response == ConstantesProtocolo.TYPE_PONG) {
                return new PingResult(true, Math.max(1, rtt), "Ping y Pong recibidos correctamente.");
            } else if (response == ConstantesProtocolo.TYPE_PING) {
                out.writeByte(ConstantesProtocolo.TYPE_PONG);
                out.flush();
                return new PingResult(true, Math.max(1, rtt), "Servidor activo y canal verificado (Ping / Pong).");
            } else {
                return new PingResult(false, rtt, "Respuesta inesperada del servidor (byte " + response + ")");
            }
        } catch (java.net.ConnectException e) {
            return new PingResult(false, -1, "Conexión rechazada (El servidor no está activo o el puerto está cerrado).");
        } catch (java.net.SocketTimeoutException e) {
            return new PingResult(false, -1, "Tiempo de espera agotado (Timeout).");
        } catch (IOException e) {
            return new PingResult(false, -1, "Error de red: " + (e.getMessage() != null ? e.getMessage() : "Desconocido"));
        }
    }

    // hace el ping en segundo plano para no congelar la pantalla
    public static void testPingAsync(String host, int port, int timeoutMs, PingCallback callback) {
        Thread thread = new Thread(() -> {
            PingResult result = measurePing(host, port, timeoutMs);
            SwingUtilities.invokeLater(() -> {
                if (callback != null) {
                    callback.onPingComplete(result);
                }
            });
        }, "ping-tester-worker");
        thread.setDaemon(true);
        thread.start();
    }
}

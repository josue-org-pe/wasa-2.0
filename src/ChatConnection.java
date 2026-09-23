import java.io.*;
import java.net.*;

/**
 * Envuelve un Socket ya conectado y maneja el envío/recepción
 * de mensajes de texto y archivos de forma simultánea (dúplex).
 * Sirve igual para el lado servidor y el lado cliente: una vez
 * conectados, ambos hablan por el mismo protocolo.
 */
public class ChatConnection {

    public interface Listener {
        void onTextReceived(String texto);
        void onFileReceived(String nombreArchivo, long tamano);
        void onPongReceived();
        void onDisconnected();
    }

    private static final int TIPO_TEXTO = 1;
    private static final int TIPO_ARCHIVO = 2;
    private static final int TIPO_PING = 3;
    private static final int TIPO_PONG = 4;

    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    private final Listener listener;
    private volatile boolean activo = true;

    public ChatConnection(Socket socket, Listener listener) throws IOException {
        this.socket = socket;
        this.listener = listener;
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        Thread hiloEscucha = new Thread(this::bucleEscucha, "chat-listener");
        hiloEscucha.setDaemon(true);
        hiloEscucha.start();
    }

    private void bucleEscucha() {
        try {
            while (activo) {
                int tipo = in.readByte();
                switch (tipo) {
                    case TIPO_TEXTO -> listener.onTextReceived(in.readUTF());
                    case TIPO_ARCHIVO -> recibirArchivo();
                    case TIPO_PING -> enviarPong();
                    case TIPO_PONG -> listener.onPongReceived();
                }
            }
        } catch (IOException e) {
            if (activo) listener.onDisconnected();
        }
    }

    private void recibirArchivo() throws IOException {
        String nombre = in.readUTF();
        long tamano = in.readLong();

        // Se guarda en la carpeta del usuario (no en la carpeta de instalación),
        // porque esa suele requerir permisos de administrador para escribir.
        File carpeta = new File(System.getProperty("user.home"), "ChatLocal" + File.separator + "archivos_recibidos");
        if (!carpeta.exists()) carpeta.mkdirs();
        File destino = new File(carpeta, nombre);

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destino))) {
            byte[] buffer = new byte[4096];
            long recibido = 0;
            while (recibido < tamano) {
                int porLeer = (int) Math.min(buffer.length, tamano - recibido);
                int leidos = in.read(buffer, 0, porLeer);
                if (leidos == -1) break;
                bos.write(buffer, 0, leidos);
                recibido += leidos;
            }
        }
        listener.onFileReceived(nombre, tamano);
    }

    public synchronized void enviarTexto(String texto) throws IOException {
        out.writeByte(TIPO_TEXTO);
        out.writeUTF(texto);
        out.flush();
    }

    public synchronized void enviarArchivo(File archivo) throws IOException {
        out.writeByte(TIPO_ARCHIVO);
        out.writeUTF(archivo.getName());
        out.writeLong(archivo.length());
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archivo))) {
            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = bis.read(buffer)) != -1) {
                out.write(buffer, 0, leidos);
            }
        }
        out.flush();
    }

    public synchronized void enviarPing() throws IOException {
        out.writeByte(TIPO_PING);
        out.flush();
    }

    private synchronized void enviarPong() throws IOException {
        out.writeByte(TIPO_PONG);
        out.flush();
    }

    public void cerrar() {
        activo = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}

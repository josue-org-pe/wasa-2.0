package com.chatlocal.backend.service;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * Gestor del almacenamiento y visualización de archivos transferidos.
 * Administra la carpeta de descargas de ChatLocal y permite abrirlos en el sistema operativo.
 */
public final class FileTransferManager {

    private static final String APP_FOLDER = "ChatLocal";
    private static final String RECEIVED_FOLDER = "archivos_recibidos";

    private FileTransferManager() {}

    /**
     * Retorna la carpeta donde se almacenan los archivos recibidos.
     * Si no existe, la crea de forma segura.
     */
    public static File getReceivedFilesFolder() {
        File userHome = new File(System.getProperty("user.home"));
        File storageDir = new File(userHome, APP_FOLDER + File.separator + RECEIVED_FOLDER);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return storageDir;
    }

    /**
     * Abre un archivo recibido con su aplicación predeterminada del sistema.
     */
    public static boolean openFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(file);
                return true;
            } catch (IOException e) {
                System.err.println("Error al abrir archivo con la aplicación nativa: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Abre la carpeta de descargas en el Explorador de archivos del sistema operativo.
     */
    public static boolean openReceivedFolder() {
        File folder = getReceivedFilesFolder();
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(folder);
                return true;
            } catch (IOException e) {
                System.err.println("Error al abrir carpeta de descargas: " + e.getMessage());
            }
        }
        return false;
    }
}

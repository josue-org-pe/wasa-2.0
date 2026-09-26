package com.chatlocal.backend.service;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

// maneja los archivos que se reciben en el chat
public final class FileTransferManager {

    private static final String APP_FOLDER = "wasa";
    private static final String RECEIVED_FOLDER = "archivos_recibidos";

    private FileTransferManager() {}

    // aqui crea la carpeta de descargas en la ruta del usuario
    public static File getReceivedFilesFolder() {
        File userHome = new File(System.getProperty("user.home"));
        File storageDir = new File(userHome, APP_FOLDER + File.separator + RECEIVED_FOLDER);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return storageDir;
    }

    // abre el archivo con la app predeterminada de la compu
    public static boolean openFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(file);
                return true;
            } catch (IOException e) {
                System.err.println("No se pudo abrir el archivo: " + e.getMessage());
            }
        }
        return false;
    }

    // abre la carpeta de descargas
    public static boolean openDownloadsFolder() {
        return openFile(getReceivedFilesFolder());
    }

    // alias para abrir la carpeta recibida
    public static boolean openReceivedFolder() {
        return openDownloadsFolder();
    }
}

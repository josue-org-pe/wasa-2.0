package com.chatlocal.backend.service.videocall;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Fuente de video para captura de cámara web física (Integrated Camera en Windows).
 * Utiliza el puente nativo de Windows MediaCapture para leer cuadros del hardware real.
 * Si la cámara física no está accesible o se encuentra ocupada por otra aplicación,
 * conmuta automáticamente a la cámara virtual inteligente sin interrupciones.
 */
public class PhysicalWebcamSource implements VideoSource {

    private final String userName;
    private final VirtualCameraVideoSource fallbackVirtual;
    private Process captureProcess;
    private File frameFile;
    private BufferedImage lastGoodFrame;
    private volatile boolean running = true;
    private boolean physicalCameraActive = false;

    public PhysicalWebcamSource(String userName) {
        this.userName = userName;
        this.fallbackVirtual = new VirtualCameraVideoSource(userName);
        initPhysicalCamera();
    }

    private void initPhysicalCamera() {
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "ChatLocalCamera");
            if (!tempDir.exists()) tempDir.mkdirs();
            frameFile = new File(tempDir, "cam_frame_" + System.currentTimeMillis() + ".jpg");

            // Script de captura continua en segundo plano mediante Windows MediaCapture
            String script =
                    "[Windows.Media.Capture.MediaCapture, Windows.Media, ContentType=WindowsRuntime]|Out-Null;" +
                    "[Windows.Storage.StorageFile, Windows.Storage, ContentType=WindowsRuntime]|Out-Null;" +
                    "[Windows.Media.MediaProperties.ImageEncodingProperties, Windows.Media, ContentType=WindowsRuntime]|Out-Null;" +
                    "$c=New-Object Windows.Media.Capture.MediaCapture;" +
                    "$asT=[System.WindowsRuntimeSystemExtensions].GetMethods()|Where-Object{$_.Name -eq 'AsTask' -and -not $_.IsGenericMethod}|Select-Object -First 1;" +
                    "$asT.Invoke($null,@($c.InitializeAsync())).Wait(5000);" +
                    "$p='" + frameFile.getAbsolutePath().replace("\\", "\\\\") + "';" +
                    "New-Item -Path $p -ItemType File -Force|Out-Null;" +
                    "$asTF=([System.WindowsRuntimeSystemExtensions].GetMethods()|Where-Object{$_.Name -eq 'AsTask' -and $_.IsGenericMethod}|Select-Object -First 1).MakeGenericMethod([Windows.Storage.StorageFile]);" +
                    "$f=$asTF.Invoke($null,@([Windows.Storage.StorageFile]::GetFileFromPathAsync($p))).Result;" +
                    "$props=[Windows.Media.MediaProperties.ImageEncodingProperties]::CreateJpeg();" +
                    "while($true){" +
                    "  $asT.Invoke($null,@($c.CapturePhotoToStorageFileAsync($props,$f))).Wait(3000);" +
                    "  Start-Sleep -Milliseconds 120;" +
                    "}";

            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", script);
            pb.redirectErrorStream(true);
            captureProcess = pb.start();
            physicalCameraActive = true;
        } catch (Exception e) {
            physicalCameraActive = false;
        }
    }

    @Override
    public BufferedImage captureFrame() {
        if (physicalCameraActive && frameFile != null && frameFile.exists() && frameFile.length() > 500) {
            try {
                BufferedImage img = ImageIO.read(frameFile);
                if (img != null) {
                    lastGoodFrame = img;
                    return img;
                }
            } catch (Exception ignored) {}
        }

        if (lastGoodFrame != null) {
            return lastGoodFrame;
        }

        // Si la cámara física aún no produce imagen, utilizar cámara virtual
        return fallbackVirtual.captureFrame();
    }

    @Override
    public void setAudioLevel(float level) {
        fallbackVirtual.setAudioLevel(level);
    }

    @Override
    public void setUserName(String name) {
        fallbackVirtual.setUserName(name);
    }

    @Override
    public void close() {
        running = false;
        if (captureProcess != null) {
            try {
                captureProcess.destroyForcibly();
            } catch (Exception ignored) {}
            captureProcess = null;
        }
        if (frameFile != null && frameFile.exists()) {
            frameFile.delete();
        }
        fallbackVirtual.close();
    }
}

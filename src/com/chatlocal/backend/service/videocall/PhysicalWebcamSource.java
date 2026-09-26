package com.chatlocal.backend.service.videocall;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Fuente de video para captura de cámara web física en Windows.
 * Utiliza Windows.Media.Capture nativo con puente optimizado de PowerShell WinRT.
 * Lee cuadros en tiempo real directamente del hardware (Integrated Camera).
 * Si la cámara física tarda en inicializar o está ocupada, conmuta de forma
 * fluida a la cámara virtual hasta que los cuadros reales estén disponibles.
 */
public class PhysicalWebcamSource implements VideoSource {

    private final String userName;
    private final VirtualCameraVideoSource fallbackVirtual;
    private Process captureProcess;
    private File scriptFile;
    private File frameFile;
    private File tempFrameFile;
    private BufferedImage lastGoodFrame;
    private volatile boolean running = true;
    private volatile boolean physicalCameraActive = false;

    public PhysicalWebcamSource(String userName) {
        this.userName = (userName != null && !userName.isEmpty()) ? userName : "Usuario";
        this.fallbackVirtual = new VirtualCameraVideoSource(this.userName);
        this.fallbackVirtual.setCustomSubtitle("Iniciando cámara física...");
        initPhysicalCamera();
    }

    private void initPhysicalCamera() {
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "ChatLocalCamera");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            String sessionId = System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
            scriptFile = new File(tempDir, "webcam_cap_" + sessionId + ".ps1");
            frameFile = new File(tempDir, "live_frame_" + sessionId + ".jpg");
            tempFrameFile = new File(tempDir, "live_frame_" + sessionId + ".tmp.jpg");

            // Generar script PowerShell optimizado para captura continua con Windows.Media.Capture
            String psScript =
                    "param([string]$targetPath)\r\n" +
                    "$ErrorActionPreference = 'Stop'\r\n" +
                    "try {\r\n" +
                    "    Add-Type -AssemblyName System.Runtime.WindowsRuntime\r\n" +
                    "    [Windows.Media.Capture.MediaCapture, Windows.Media, ContentType = WindowsRuntime] | Out-Null\r\n" +
                    "    [Windows.Storage.StorageFile, Windows.Storage, ContentType = WindowsRuntime] | Out-Null\r\n" +
                    "    [Windows.Media.MediaProperties.ImageEncodingProperties, Windows.Media, ContentType = WindowsRuntime] | Out-Null\r\n" +
                    "    $settings = New-Object Windows.Media.Capture.MediaCaptureInitializationSettings\r\n" +
                    "    $settings.StreamingCaptureMode = [Windows.Media.Capture.StreamingCaptureMode]::Video\r\n" +
                    "    $capture = New-Object Windows.Media.Capture.MediaCapture\r\n" +
                    "    $asTaskNonGeneric = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and -not $_.IsGenericMethod } | Select-Object -First 1\r\n" +
                    "    $asTaskGeneric = [System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.IsGenericMethod } | Select-Object -First 1\r\n" +
                    "    $asTaskFile = $asTaskGeneric.MakeGenericMethod([Windows.Storage.StorageFile])\r\n" +
                    "    $initTask = $capture.InitializeAsync($settings)\r\n" +
                    "    $asTaskNonGeneric.Invoke($null, @($initTask)).Wait(10000)\r\n" +
                    "    $tempPath = $targetPath + '.tmp.jpg'\r\n" +
                    "    New-Item -Path $tempPath -ItemType File -Force | Out-Null\r\n" +
                    "    $fileTask = [Windows.Storage.StorageFile]::GetFileFromPathAsync($tempPath)\r\n" +
                    "    $file = $asTaskFile.Invoke($null, @($fileTask)).Result\r\n" +
                    "    $photoProps = [Windows.Media.MediaProperties.ImageEncodingProperties]::CreateJpeg()\r\n" +
                    "    $photoProps.Width = 1280\r\n" +
                    "    $photoProps.Height = 720\r\n" +
                    "    [Console]::WriteLine('READY')\r\n" +
                    "    [Console]::Out.Flush()\r\n" +
                    "    while ($true) {\r\n" +
                    "        $capTask = $capture.CapturePhotoToStorageFileAsync($photoProps, $file)\r\n" +
                    "        $asTaskNonGeneric.Invoke($null, @($capTask)).Wait(4000)\r\n" +
                    "        [System.IO.File]::Copy($tempPath, $targetPath, $true)\r\n" +
                    "        Start-Sleep -Milliseconds 10\r\n" +
                    "    }\r\n" +
                    "} catch {\r\n" +
                    "    [Console]::Error.WriteLine('ERROR: ' + $_.Exception.Message)\r\n" +
                    "    [Console]::Error.Flush()\r\n" +
                    "} finally {\r\n" +
                    "    if ($capture -ne $null) { $capture.Dispose() }\r\n" +
                    "}\r\n";

            Files.write(scriptFile.toPath(), psScript.getBytes(StandardCharsets.UTF_8));

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.getAbsolutePath(),
                    frameFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            captureProcess = pb.start();

            // Monitor de salida en segundo plano para logging y detección de READY
            Thread monitorThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(captureProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        if ("READY".equals(line.trim())) {
                            physicalCameraActive = true;
                            fallbackVirtual.setCustomSubtitle(null);
                            System.out.println("[Webcam] Cámara física inicializada y transmitiendo.");
                        } else if (line.startsWith("ERROR:")) {
                            System.err.println("[Webcam Native] " + line);
                            fallbackVirtual.setCustomSubtitle("Cámara física no disponible (en uso)");
                        }
                    }
                } catch (Exception ignored) {}
            }, "webcam-monitor-" + sessionId);
            monitorThread.setDaemon(true);
            monitorThread.start();

        } catch (Exception e) {
            System.err.println("[Webcam] No se pudo iniciar el proceso de cámara física: " + e.getMessage());
            physicalCameraActive = false;
        }
    }

    @Override
    public BufferedImage captureFrame() {
        if (frameFile != null && frameFile.exists() && frameFile.length() > 1000) {
            try {
                byte[] data = Files.readAllBytes(frameFile.toPath());
                if (data.length > 1000) {
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                    if (img != null) {
                        lastGoodFrame = img;
                        physicalCameraActive = true;
                        return img;
                    }
                }
            } catch (Exception ignored) {
                // Durante la copia atómica en disco puede haber un micro-bloqueo temporal
            }
        }

        // Si ya tenemos un cuadro previo de la cámara, lo reutilizamos para evitar parpadeos
        if (lastGoodFrame != null) {
            return lastGoodFrame;
        }

        // Si la cámara aún está calentando el sensor, mostrar vista virtual con avatar
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

    public boolean isPhysicalCameraActive() {
        return physicalCameraActive;
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

        // Limpiar archivos temporales de esta sesión
        if (frameFile != null && frameFile.exists()) {
            frameFile.delete();
        }
        if (tempFrameFile != null && tempFrameFile.exists()) {
            tempFrameFile.delete();
        }
        if (scriptFile != null && scriptFile.exists()) {
            scriptFile.delete();
        }

        fallbackVirtual.close();
    }
}

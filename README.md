# wasa 3.0

Proyecto de mensajería y videollamadas P2P en red local para el curso de Programación Orientada a Objetos (POO).

---

## 🚀 Características Principales

- **Arquitectura Modular POO**: Separación limpia entre Backend (`model`, `network`, `service`) y UI (`theme`, `components`, `dialogs`, `panels`).
- **Nombres y Avatares Dinámicos**: Identificación visual inmediata con iniciales y color distintivo de avatar en cada burbuja de chat.
- **Sidebar Estilo WhatsApp / Discord**:
  - Pestañas organizadas: Canales/Salas, Contactos LAN y Búsqueda en tiempo real.
  - Creación de salas y reuniones locales con código de acceso opcional.
- **Notas de Voz Nativas (Instant Audio Memos)**:
  - Grabación y reproducción de audio 100% nativa con `javax.sound.sampled` (16kHz mono WAV).
  - Barra de grabación con temporizador en vivo, botón de cancelar y enviar.
- **Drawer de Emojis y Stickers**:
  - Selector emergente categorizado (Emoticones, Expresiones, Reacciones rápidas).
- **Checks de Estado de Mensaje**:
  - `✓` Enviado al socket.
  - `✓✓` Entregado / Visto (verde esmeralda / cyan).
- **Motor de Temas en Vivo (5 Paletas)**:
  1. 🟣 **Neon Cyber**: Fondo oscuro con acentos violeta eléctrico y cyan.
  2. 🟢 **Emerald Matrix**: Estilo terminal futurista verde esmeralda.
  3. 🔵 **Ocean Discord**: Azul profundo y slate moderno.
  4. 🔴 **Crimson Velvet**: Tonos borgoña y rubí elegantes.
  5. ☀️ **Solar Light**: Modo claro de alto contraste.
- **Videollamadas en Tiempo Real (LAN & P2P)**:
  - Streaming de video de ultra baja latencia con fragmentación UDP MTU-safe y compresión JPEG (~18-20 FPS).
  - Audio dúplex en vivo sobre UDP con la API nativa `javax.sound.sampled` (16kHz PCM mono).
  - Fuentes de video dinámicas intercambiables en caliente:
    - 📹 **Cámara Virtual con Avatar Reactivo**: halo de energía pulsante que reacciona al micrófono, ecualizador gráfico de audio en tiempo real y OSD HUD (`● LIVE`, contador de tiempo, FPS).
    - 🖥️ **Compartir Pantalla Completa**: captura de escritorio en vivo con `java.awt.Robot` y escalado bilineal sin retardo.
    - 🚫 **Cámara Apagada**: modo solo audio.
  - Ventana de videollamada estilo Discord / FaceTime / Google Meet:
    - Video remoto en vista principal con relación de aspecto preservada.
    - Miniatura Picture-in-Picture (PiP) local en la esquina superior.
    - Dock inferior flotante con controles rápidos: Silenciar Micrófono, Apagar Cámara, Compartir Pantalla y Colgar.
    - Notificación modal de llamada entrante con halo animado y timbre melódico sintético procedural sin dependencias de audio externas.
- **Herramienta Ping/Pong de Latencia**:
  - Diagnóstico previo de conexión con medición en milisegundos (RTT) e indicador visual de estado.
- **Gestión de Contactos y Privacidad**:
  - Bloqueo y desbloqueo de usuarios con silenciado de paquetes a nivel de socket.
- **100% Pure Java**: Cero dependencias externas de librerías de terceros (funciona directamente con JDK 17+ / Temurin).

---

## 📂 Estructura del Proyecto

```text
version2/
├── .gitignore
├── build.bat              # Compila JAR y genera instalador .exe con jpackage
├── run.bat                # Compila y ejecuta la aplicación completa
├── run-preview.bat        # Vista previa instantánea del chat
├── login-preview.bat      # Vista previa de la pantalla de conexión y ping
├── test-duo.bat           # Lanza dos instancias para pruebas locales
├── PACKAGING.md           # Guía de empaquetado
├── PARTE1-INSTALADOR.md   # Guía del instalador
├── icon.ico / icon.png    # Íconos de la aplicación (gatito)
└── src/
    ├── ChatApp.java       # Lanzador rápido
    └── com/chatlocal/
        ├── Main.java      # Punto de entrada principal
        ├── backend/
        │   ├── event/     # ConexionDeEscucha, MensajeDeEscucha
        │   ├── model/     # MensajeChat, UsuarioPerfil, SalaChat, TipoMensaje, EstadoMensaje...
        │   ├── network/   # ConexionSocket, ConstantesProtocolo
        │   └── service/   # ServicioChat, GestorTransferenciaArchivos, UtilidadesRed, videocall...
        └── ui/
            ├── VentanaChat.java
            ├── VistaPreviaUi.java
            ├── theme/       # Tema, GestorTema, TemaApp, Iconos
            ├── components/  # BotonModerno, CampoTextoModerno, BarraGrabadorAudio...
            ├── dialogs/     # DialogoAjustes, DialogoCrearSala, videocall...
            └── panels/      # PanelLogin, PanelBarraLateral, PanelAreaChat, PanelConectando...
```

---

## 🛠️ Cómo Ejecutar

### 1. Ejecución Rápida de Desarrollo (sin empaquetar)
```cmd
run.bat
```

### 2. Previsualización Instantánea de UI/UX
```cmd
run-preview.bat
```

### 3. Prueba de Dos Clientes Simultáneos en la Misma Máquina
```cmd
test-duo.bat
```

### 4. Generar Instalador `.exe`
```cmd
build.bat
```
*(Requiere WiX Toolset 3.11+ para generar el instalador en `salida\wasa-3.0.exe`)*

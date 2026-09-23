# Wasa 2.0 (ChatLocal / Pulse LAN Messenger)

Segunda versión moderna, modular y escalable del sistema de mensajería P2P y cliente-servidor LAN para el curso de **Programación Orientada a Objetos (POO)**.

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
├── build.bat              # Script para compilar JAR y empaquetar .exe con jpackage
├── run.bat                # Compila y ejecuta la aplicación completa
├── run-preview.bat        # Vista previa instantánea del chat en ~1s
├── login-preview.bat      # Vista previa de la pantalla de login y ping
├── test-duo.bat           # Lanza dos instancias locales para pruebas en vivo
├── PACKAGING.md           # Guía detallada de empaquetado
├── PARTE1-INSTALADOR.md   # Guía del instalador
└── src/
    ├── ChatApp.java
    ├── ChatConnection.java
    └── com/chatlocal/
        ├── Main.java
        ├── backend/
        │   ├── model/       # ChatMessage, UserProfile, ChatRoom, MessageType, MessageStatus
        │   ├── network/     # SocketConnection, ProtocolConstants
        │   └── service/     # ChatService, ChatServiceImpl, AudioRecorderService, PingTester, FileTransferManager
        └── ui/
            ├── ChatWindow.java
            ├── theme/       # AppTheme, ThemeManager, Icons
            ├── components/  # AudioRecorderBar, EmojiPickerPopup, ChatBubblePanel, ContactListItem, ModernButton...
            ├── dialogs/     # SettingsDialog, CreateRoomDialog
            └── panels/      # LoginPanel, SidebarPanel, ChatAreaPanel, MainMessengerPanel, ConnectingPanel
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
*(Requiere WiX Toolset 3.11+ para generar el instalador en `salida\ChatLocal-2.0.exe`)*

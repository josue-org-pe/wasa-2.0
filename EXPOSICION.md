# Guía de Exposición: Arquitectura y Comunicación de wasa 3.0
**Curso:** Programación Orientada a Objetos (POO)  
**Tiempo estimado:** 12 a 15 minutos  
**Enfoque:** Arquitectura de red, comunicación de medios (TCP/UDP), concurrencia y patrones de diseño POO.  
*(Se omiten aspectos cosméticos como hojas de estilo o edición de avatar para centrarse en ingeniería de software y redes).*

---

## ⏱️ Estructura del Tiempo (15 Minutos)

| Minuto | Bloque | Objetivo clave |
|---|---|---|
| **00:00 - 02:00** | **1. Introducción y Arquitectura General** | Explicar el modelo cliente-servidor / P2P y la separación por capas. |
| **02:00 - 06:00** | **2. Canal de Control y Datos: Protocolo Binario TCP** | Cómo viajan los mensajes, archivos y notas de voz con `ConexionSocket`. |
| **06:00 - 10:00** | **3. Canal Multimedia en Tiempo Real: Streaming UDP** | Explicar por qué UDP, fragmentación MTU, captura de audio PCM y video JPEG. |
| **10:00 - 13:00** | **4. Principios POO y Patrones de Diseño** | Polimorfismo en fuentes de video, interfaces de servicio y patrón Observador. |
| **13:00 - 15:00** | **5. Demostración Rápida y Conclusiones** | Ejecutar con `test-duo.bat`, llamada de prueba y ronda de preguntas. |

---

## 🧩 1. Módulos Más Importantes del Sistema

Para la exposición, enfócate únicamente en estos 3 paquetes del Backend:

```text
com.chatlocal.backend/
├── network/
│   ├── ConstantesProtocolo.java   # Definición de cabeceras binarias (1 byte de tipo)
│   └── ConexionSocket.java        # Hilo de lectura/escritura sobre TCP
├── service/
│   ├── ServicioChat.java          # Interfaz desacoplada del negocio de mensajería
│   ├── ServicioChatImpl.java      # Servidor multicliente (ServerSocket) y ruteo
│   └── videocall/
│       ├── GestorTransmisionMedia.java   # Hilos emisores y receptores UDP
│       ├── FuenteVideo.java              # Interfaz polimórfica de captura
│       ├── FuenteCamaraWeb.java          # Captura física DirectShow / OpenCV
│       ├── FuenteCamaraVirtual.java      # Renderizado procedural reactivo
│       └── FuenteCompartirPantalla.java  # Captura con java.awt.Robot
```

---

## 📡 2. Canal TCP: Mensajería, Archivos y Señalización

### ¿Por qué TCP para este canal?
- **Garantía de entrega:** Si enviamos un archivo o mensaje, no puede haber pérdida de bytes ni desorden de paquetes.
- **Flujo bidireccional continuo:** Mantiene el socket abierto durante toda la sesión (`Socket` / `ServerSocket`).

### Protocolo Binario Personalizado (`ConstantesProtocolo` y `ConexionSocket`)
En lugar de serializar objetos pesados o usar JSON/XML que añaden sobrecarga innecesaria, se diseñó un protocolo binario ligero:

```
[1 Byte: Tipo] [Cabecera según tipo...] [Cuerpo / Payload]
```

1. **Mensajes de Texto (`TYPE_TEXT = 1`):**
   - Estructura: `Tipo (1B)` + `ID (UTF)` + `Remitente (UTF)` + `Color (int)` + `Sala (UTF)` + `Destinatario (UTF)` + `Texto (UTF)`.
   - **Confirmación de Entrega (ACK):** Al recibir el mensaje, el receptor responde inmediatamente con `TYPE_ACK (9)` enviando el `ID`. Al recibir el ACK, el emisor actualiza el estado de `✓` a `✓✓`.

2. **Transferencia de Archivos y Notas de Voz (`TYPE_FILE = 2`, `TYPE_AUDIO = 7`):**
   - Estructura: Se envían primero los metadatos (nombre y tamaño en bytes como `long`).
   - Luego se transmite el flujo continuo en bloques de `8192 bytes` (`BUFFER_SIZE`).
   - El receptor escribe directamente al disco con `BufferedOutputStream` en `~/wasa/archivos_recibidos/`, evitando desbordar la memoria RAM.

3. **Señalización de Videollamada (`TYPE_CALL_REQUEST`, `TYPE_CALL_ACCEPT`, `TYPE_CALL_END`):**
   - El canal TCP actúa como "canal de señalización" (similar a SIP en telefonía): intercambia la IP y el puerto UDP de los pares antes de iniciar la transmisión de audio/video.

---

## 🎥 3. Canal UDP: Streaming de Video y Audio en Tiempo Real

### ¿Por qué UDP para la videollamada?
- **Baja latencia:** TCP retransmite paquetes perdidos, lo cual generaría congelamiento (*lag* acumulativo). En una videollamada en vivo, un cuadro retrasado ya no sirve; se descarta y se procesa el siguiente.
- **Independencia de hilos:** Se ejecuta sobre `DatagramSocket`, completamente separado del socket TCP del chat.

### Funcionamiento de `GestorTransmisionMedia.java`

```mermaid
graph LR
    subgraph Emisor
        Cam[FuenteVideo] -->|JPEG| Comp[Compresión 1280x720]
        Comp -->|Fragmentos < 60KB| UDP_TX[DatagramSocket Emisor]
        Mic[Microfono javax.sound] -->|PCM 16kHz| UDP_TX
    end
    
    UDP_TX -.->|Red LAN (UDP)| UDP_RX[DatagramSocket Receptor]
    
    subgraph Receptor
        UDP_RX -->|Reensamblado| Frame[Render en Ventana]
        UDP_RX -->|SourceDataLine| Speaker[Parlante / Audio]
    end
```

1. **Video (60 FPS / HD):**
   - Captura cuadros a través de `FuenteVideo.captureFrame()`.
   - Se comprime cada cuadro a formato JPEG con `ImageWriter` nativo de Java.
   - **Fragmentación Segura MTU:** Si el JPEG supera los 60,000 bytes, se divide en fragmentos con un índice y total de partes para que el socket UDP no exceda el límite del paquete de red. Al llegar todos los fragmentos al destino, se reconstruye el `BufferedImage`.

2. **Audio Dúplex en Vivo:**
   - Captura micrófono con `TargetDataLine` a 16,000 Hz, 16 bits mono (PCM).
   - Envía pequeños paquetes de 640 bytes (~20 ms de audio) para latencia imperceptible.
   - Reproduce inmediatamente en el par remoto usando `SourceDataLine`.

---

## 🏛️ 4. Principios POO Demostrables en el Código

Los puntos clave que los docentes buscan en una sustentación de POO:

### A. Polimorfismo e Interfaces
- **Interfaz `FuenteVideo`:**
  Permite intercambiar la fuente de video en caliente durante la videollamada sin modificar una sola línea del motor UDP (`GestorTransmisionMedia` solo conoce a `FuenteVideo`):
  - `FuenteCamaraWeb`: Obtiene cuadros de la webcam del dispositivo.
  - `FuenteCamaraVirtual`: Genera un avatar matemático procedural que pulsa con la voz.
  - `FuenteCompartirPantalla`: Captura el escritorio usando `java.awt.Robot`.

### B. Patrón de Diseño Observador (Observer Pattern / Event-Driven)
- Desacoplamiento total entre la capa de red y la interfaz gráfica:
  - `ConexionDeEscucha`: Notifica a los controladores cuando se conecta o desconecta un cliente.
  - `MensajeDeEscucha`: Despacha mensajes entrantes sin que el socket conozca las clases de Swing.
  - `OyenteVideollamada`: Notifica cuando llega un frame nuevo o cambia el nivel de audio.

### C. Concurrencia y Seguridad en Hilos (Thread Safety)
- El hilo de escucha de sockets (`chat-socket-listener`) corre en segundo plano como *Daemon Thread*.
- Toda actualización visual hacia Swing se despacha mediante `SwingUtilities.invokeLater()` para no romper el hilo de eventos (EDT).
- Se usan colecciones concurrentes de `java.util.concurrent` (`ConcurrentHashMap`, `CopyOnWriteArrayList`) para evitar condiciones de carrera cuando múltiples clientes mandan datos al mismo tiempo.

---

## 🎙️ Guion Rápido para el Expositor (Resumen Paso a Paso)

> **Minuto 0 a 2:**  
> *"Buenas tardes profesor. Nuestro proyecto 'wasa 3.0' es un sistema de mensajería y videollamadas local desarrollado 100% en Java puro, sin librerías externas. La arquitectura se divide en dos grandes subsistemas: el canal de transporte seguro por TCP para mensajería y archivos, y el canal de baja latencia por UDP para streaming multimedia."*

> **Minuto 2 a 6:**  
> *"En el canal TCP, implementamos `ConexionSocket` con un protocolo binario propio definido en `ConstantesProtocolo`. Cada paquete inicia con un byte indicador: texto, archivos, notas de voz o señalización. Destacamos el sistema de doble check: cuando el receptor deserializa el mensaje, dispara un paquete de confirmación (ACK) para confirmar que llegó al destino."*

> **Minuto 6 a 10:**  
> *"Para la videollamada, implementamos `GestorTransmisionMedia` sobre UDP. Usar TCP en video provocaría retrasos por retransmisión. El video se captura a resolución HD, se comprime en JPEG y se fragmenta en paquetes UDP seguros. En audio capturamos PCM a 16kHz en bloques de 20 milisegundos con `javax.sound.sampled`."*

> **Minuto 10 a 13:**  
> *"A nivel de POO, aplicamos polimorfismo puro con la interfaz `FuenteVideo`. La llamada puede alternar entre la cámara web (`FuenteCamaraWeb`), la cámara virtual (`FuenteCamaraVirtual`) o compartir la pantalla (`FuenteCompartirPantalla`) sin tocar el motor de transmisión. Además, desacoplamos la red de la interfaz mediante el patrón Observador con las interfaces `MensajeDeEscucha` y `ConexionDeEscucha`."*

> **Minuto 13 a 15:**  
> *(Demostración en vivo ejecutando `test-duo.bat`: mostrar envío de texto con doble check, una nota de voz y una llamada en vivo).*

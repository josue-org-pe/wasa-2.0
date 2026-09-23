d# Chat Local por Sockets — App con interfaz gráfica y .exe instalable

## Qué hace esta versión

A diferencia de `Server.java`/`Client.java` (que solo mandaban un archivo y terminaban),
esta app se queda abierta como un programa de chat real:

1. Pantalla de configuración: eliges si tu PC va a ser el "anfitrión" (host/servidor) o si te
   vas a "conectar" a otra PC (cliente), y defines la IP y el puerto.
2. Pantalla de verificación: al conectar, automáticamente se manda un mensaje interno de
   "ping" y espera el "pong" de respuesta — así confirmas que el canal realmente funciona
   antes de mostrar el chat (esto reemplaza el "checking con archivo de prueba" que
   mencionaste; usa un mensaje de control en vez de un archivo real para ser instantáneo).
3. Pantalla de chat: mensajes de texto en ambas direcciones + botón para adjuntar y enviar
   archivos, todo sobre la misma conexión abierta.

## Paso 1: Compilar

Desde la carpeta `chat-app/src`:

```
javac src.ChatConnection.java ChatApp.java
```

## Paso 2: Probarlo antes de empaquetar

Abre DOS terminales (o dos PCs). En ambas, dentro de `chat-app/src`:

```
java ChatApp
```

En una ventana elige "Ser el anfitrión", en la otra "Conectarme a un anfitrión" con la IP
de la primera. Deberían poder chatear y mandarse archivos con el botón "Adjuntar archivo".

## Paso 3: Empaquetar como .exe (jpackage)

`jpackage` viene incluido con el JDK (11+) y crea un instalador nativo de Windows que
incluye su propio Java empaquetado — quien lo reciba no necesita tener Java instalado.

1. Compila hacia una carpeta de salida:
   ```
   javac -d out src.ChatConnection.java ChatApp.java
   ```
2. Empaqueta en un .jar ejecutable:
   ```
   jar --create --file ChatApp.jar --main-class ChatApp -C out .
   ```
3. Opción A — Carpeta con .exe listo para copiar (no requiere instalar nada extra):
   ```
   jpackage --input . --name ChatLocal --main-jar ChatApp.jar --main-class ChatApp --type app-image --dest salida
   ```
   Esto genera `salida\ChatLocal\ChatLocal.exe`. Puedes copiar toda esa carpeta a otra PC
   y funciona directamente con doble clic, sin instalar Java.

4. Opción B — Instalador real (.exe con asistente, ícono, acceso directo en el menú Inicio):
   Requiere instalar primero **WiX Toolset** (https://wixtoolset.org/, gratis). Luego:
   ```
   jpackage --input . --name ChatLocal --main-jar ChatApp.jar --main-class ChatApp --type exe --dest salida --win-shortcut --win-menu
   ```
   Esto genera un instalador `ChatLocal-1.0.exe` que se instala como cualquier programa
   de Windows (Panel de Control > Programas, con su propio desinstalador).

## Limitaciones honestas de este enfoque

- Sigue siendo **conexión directa entre dos PCs en la misma red** (o con reenvío de
  puertos si son redes distintas) — no hay servidor central en internet como WhatsApp.
- Es **chat 1 a 1**, no grupos, salvo que extiendas el servidor para manejar varias
  conexiones simultáneas (ver hoja de ruta anterior, punto 4).
- No incluye llamadas de voz/video — eso requiere una librería de WebRTC aparte, como se
  explicó antes.
- El instalador `.exe` no está firmado digitalmente, así que Windows SmartScreen puede
  mostrar una advertencia la primera vez que alguien lo ejecute en otra PC ("Windows
  protegió su PC" > "Más información" > "Ejecutar de todas formas"). Para evitar esa
  advertencia se necesita un certificado de firma de código, que tiene costo.

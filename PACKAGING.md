# wasa 3.0 — App con interfaz gráfica y .exe instalable

## Qué hace esta versión

Esta app permite mensajería en tiempo real y videollamadas en red local:

1. Pantalla de configuración: eliges si tu PC va a ser el "anfitrión" (host/servidor) o si te
   vas a "conectar" a otra PC (cliente), y defines la IP y el puerto.
2. Pantalla de verificación: al conectar, automáticamente se manda un mensaje interno de
   "ping" y espera el "pong" de respuesta.
3. Pantalla de chat: mensajes de texto en ambas direcciones, notas de voz, emojis, videollamadas
   y envío de archivos.

## Paso 1: Compilar

```cmd
run.bat
```

## Paso 2: Empaquetar como .exe (jpackage)

`build.bat` se encarga de todo de forma automática.
Genera el instalador `salida\wasa-3.0.exe`.
   Esto genera un instalador `wasa-3.0.exe` que se instala como cualquier programa
   de Windows (Panel de Control > Programas, con su propio desinstalador).

## Notas
- Conexión P2P / LAN mediante sockets TCP y streaming UDP para video/audio.
- Soporta videollamadas con cámara real o virtual, notas de voz, emojis y salas privadas.
- El instalador `.exe` no está firmado digitalmente, así que Windows SmartScreen puede
  mostrar una advertencia la primera vez que alguien lo ejecute en otra PC ("Windows
  protegió su PC" > "Más información" > "Ejecutar de todas formas"). Para evitar esa
  advertencia se necesita un certificado de firma de código, que tiene costo.

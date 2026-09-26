# Parte 1 — Instalador real (.exe con asistente)

Este es el primer bloque del desarrollo incremental: lograr que la app se instale
como un programa normal de Windows, con su propio asistente que pide la carpeta
de instalación, crea acceso directo, y aparece en "Agregar o quitar programas".

## Qué necesitas instalar UNA sola vez en tu máquina de desarrollo

1. **JDK 17 o superior** (ya lo tienes de los pasos anteriores). Verifica:
   ```
   javac -version
   ```
2. **WiX Toolset** — es lo que `jpackage` usa por debajo para construir instaladores
   `.exe`/`.msi` en Windows (sin esto, jpackage solo puede generar una carpeta suelta,
   no un instalador con asistente).
   - Descárgalo de: https://wixtoolset.org/releases/
   - Instala la versión estable más reciente (3.x es la más compatible con jpackage).
   - Cierra y abre una terminal nueva después de instalar.

> Importante: WiX solo hace falta en la máquina donde **construyes** el instalador.
> La persona que lo recibe e instala **no necesita nada de esto** — el `.exe` final
> ya trae todo empaquetado, incluido un Java propio.

## Cómo generar el instalador

Desde la carpeta del proyecto, ejecuta:

```cmd
build.bat
```

Esto hace automáticamente:
1. Compila el proyecto completo
2. Empaqueta el JAR
3. Corre `jpackage` y genera el instalador en `salida\wasa-3.0.exe`

## Qué va a ver la persona que lo instala

Al hacer doble clic en `wasa-3.0.exe`, aparece el instalador con el ícono y asistente:

1. Pantalla de bienvenida con logo
2. Pantalla para elegir la carpeta de instalación
3. Instalación (copia los archivos, crea acceso directo en el escritorio y en el menú Inicio)
4. Finalización

Después de instalado, la persona busca "wasa" en el menú Inicio o usa el acceso directo del escritorio.

## Nota sobre la advertencia de Windows SmartScreen

Como el instalador no está firmado digitalmente (eso cuesta un certificado de pago),
la primera vez que alguien lo ejecute en otra PC, Windows puede mostrar:

> "Windows protegió su PC" — Microsoft Defender SmartScreen impidió el inicio de una
> aplicación no reconocida.

Es normal en instaladores caseros. Se resuelve haciendo clic en **"Más información"**
y luego **"Ejecutar de todas formas"**. No es un error del programa, es solo porque
Windows no reconoce quién lo firmó.

---

**Siguiente paso** (cuando quieras seguir): reforzar la Interfaz 1 — definir quién es
cliente/servidor, hacer el ping-pong de verificación más robusto, y mostrarle al
usuario mensajes claros si algo falla (IP incorrecta, timeout, firewall, etc.) antes
de dejarlo entrar a la pantalla de chat.

@echo off
setlocal

echo ============================================
echo   Construyendo instalador de ChatLocal
echo ============================================

echo.
echo [1/3] Compilando codigo fuente modular...
if exist out rmdir /s /q out
mkdir out
javac -d out -encoding UTF-8 --source-path src src\com\chatlocal\Main.java src\ChatApp.java
if errorlevel 1 (
    echo.
    echo ERROR: fallo la compilacion. Revisa los mensajes de arriba.
    pause
    exit /b 1
)

echo.
echo [2/3] Empaquetando en JAR ejecutable...
if exist ChatApp.jar del ChatApp.jar
jar --create --file ChatApp.jar --main-class com.chatlocal.Main -C out .
if errorlevel 1 (
    echo.
    echo ERROR: fallo la creacion del JAR.
    pause
    exit /b 1
)

echo.
echo [3/3] Generando instalador .exe con jpackage...
if exist salida rmdir /s /q salida

jpackage ^
  --input . ^
  --name ChatLocal ^
  --main-jar ChatApp.jar ^
  --main-class com.chatlocal.Main ^
  --type exe ^
  --dest salida ^
  --app-version 2.0 ^
  --vendor "Mi Proyecto" ^
  --win-shortcut ^
  --win-menu ^
  --win-dir-chooser

if errorlevel 1 (
    echo.
    echo ERROR: fallo jpackage.
    echo Causa mas comun: no tienes instalado WiX Toolset, que jpackage
    echo necesita para construir instaladores .exe/.msi en Windows.
    echo Descargalo de https://wixtoolset.org/ e intenta de nuevo.
    pause
    exit /b 1
)

echo.
echo ============================================
echo   Listo. El instalador quedo en:
echo   salida\ChatLocal-2.0.exe
echo ============================================
pause

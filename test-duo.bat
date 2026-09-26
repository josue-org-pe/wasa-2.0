@echo off
echo ========================================================
echo   Abriendo 2 instancias de wasa 3.0 (Prueba Dual)
echo ========================================================
if not exist out mkdir out
javac -d out -encoding UTF-8 --source-path src src\com\chatlocal\Main.java src\ChatApp.java
if errorlevel 1 (
    echo Error durante la compilacion.
    pause
    exit /b 1
)
start "wasa - Ventana 1" java -cp out com.chatlocal.Main
timeout /t 1 /nobreak >nul
start "wasa - Ventana 2" java -cp out com.chatlocal.Main

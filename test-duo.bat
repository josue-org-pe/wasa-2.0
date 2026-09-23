@echo off
echo ========================================================
echo   Abriendo 2 instancias de ChatLocal (Prueba Dual Local)
echo ========================================================
if not exist out mkdir out
javac -d out -encoding UTF-8 --source-path src src\com\chatlocal\Main.java
if errorlevel 1 (
    echo Error durante la compilacion.
    pause
    exit /b 1
)
start "ChatLocal - Ventana 1" java -cp out com.chatlocal.Main
timeout /t 1 /nobreak >nul
start "ChatLocal - Ventana 2" java -cp out com.chatlocal.Main

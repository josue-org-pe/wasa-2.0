@echo off
echo ========================================================
echo   Ejecutando ChatLocal en modo desarrollo (Instantaneo)
echo ========================================================
if not exist out mkdir out
javac -d out -encoding UTF-8 --source-path src src\com\chatlocal\Main.java
if errorlevel 1 (
    echo Error durante la compilacion.
    pause
    exit /b 1
)
start "" java -cp out com.chatlocal.Main

@echo off
echo ========================================================
echo   Vista previa de Login - wasa 3.0
echo ========================================================
if not exist out mkdir out
javac -d out -encoding UTF-8 --source-path src src\com\chatlocal\Main.java
if errorlevel 1 (
    echo Error durante la compilacion.
    pause
    exit /b 1
)
start "" java -cp out com.chatlocal.Main

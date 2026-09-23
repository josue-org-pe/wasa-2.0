@echo off
echo ========================================================
echo   Vista previa de UI / UX de ChatLocal (Sin red)
echo ========================================================
if not exist out mkdir out
javac -d out -encoding UTF-8 --source-path src src\com\chatlocal\ui\UiPreview.java
if errorlevel 1 (
    echo Error durante la compilacion.
    pause
    exit /b 1
)
start "" java -cp out com.chatlocal.ui.UiPreview

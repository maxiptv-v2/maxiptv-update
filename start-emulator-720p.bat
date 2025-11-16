@echo off
echo ========================================
echo INICIANDO EMULADOR FIRE STICK HD 720p
echo ========================================
echo.

set SDK_PATH=%LOCALAPPDATA%\Android\Sdk
if "%ANDROID_HOME%" NEQ "" set SDK_PATH=%ANDROID_HOME%

set EMULATOR=%SDK_PATH%\emulator\emulator.exe
set AVD_NAME=FireStick_HD_Test

if not exist "%EMULATOR%" (
    echo ERRO: Emulador nao encontrado!
    echo Caminho esperado: %EMULATOR%
    echo.
    echo Verifique se o Android SDK esta instalado.
    pause
    exit /b 1
)

echo Iniciando emulador...
echo Resolucao: 1280x720 (HD)
echo DPI: 213 (similar ao Fire Stick)
echo.

"%EMULATOR%" -avd %AVD_NAME% -skin 1280x720 -dpi-device 213

pause


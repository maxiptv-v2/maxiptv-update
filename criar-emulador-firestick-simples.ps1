# Script Simplificado para Criar Emulador Android TV Similar ao Fire Stick
# Versão mais prática e direta

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CRIAR EMULADOR ANDROID TV" -ForegroundColor Cyan
Write-Host "Perfil: Fire Stick HD (720p/1080p)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar Android SDK
$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) {
    $sdkPath = "$env:LOCALAPPDATA\Android\Sdk"
}

if (-not (Test-Path $sdkPath)) {
    Write-Host "ERRO: Android SDK não encontrado!" -ForegroundColor Red
    Write-Host ""
    Write-Host "SOLUÇÃO:" -ForegroundColor Yellow
    Write-Host "1. Abra o Android Studio" -ForegroundColor White
    Write-Host "2. Vá em: Tools > Device Manager" -ForegroundColor White
    Write-Host "3. Clique em 'Create Device'" -ForegroundColor White
    Write-Host "4. Escolha 'TV' > 'TV (1080p)'" -ForegroundColor White
    Write-Host "5. Escolha 'API 33 (Android 13)' ou superior" -ForegroundColor White
    Write-Host "6. Nomeie como 'FireStick_HD_Test'" -ForegroundColor White
    Write-Host ""
    Write-Host "OU use este script após instalar o Android SDK." -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Android SDK: $sdkPath" -ForegroundColor Green
Write-Host ""

# Caminhos
$emulator = "$sdkPath\emulator\emulator.exe"
$avdManager = "$sdkPath\cmdline-tools\latest\bin\avdmanager.bat"

if (-not (Test-Path $avdManager)) {
    $avdManager = "$sdkPath\tools\bin\avdmanager.bat"
}

if (-not (Test-Path $emulator)) {
    Write-Host "ERRO: Emulador não encontrado!" -ForegroundColor Red
    Write-Host "Instale o Android Emulator pelo Android Studio." -ForegroundColor Yellow
    exit 1
}

$avdName = "FireStick_HD_Test"

Write-Host "Opções disponíveis:" -ForegroundColor Cyan
Write-Host "1. Listar AVDs existentes" -ForegroundColor White
Write-Host "2. Criar novo AVD (Fire Stick HD)" -ForegroundColor White
Write-Host "3. Iniciar emulador existente" -ForegroundColor White
Write-Host "4. Iniciar em 720p" -ForegroundColor White
Write-Host "5. Iniciar em 1080p" -ForegroundColor White
Write-Host ""
$opcao = Read-Host "Escolha uma opção (1-5)"

switch ($opcao) {
    "1" {
        Write-Host ""
        Write-Host "AVDs disponíveis:" -ForegroundColor Cyan
        & $avdManager list avd
    }
    "2" {
        Write-Host ""
        Write-Host "INSTRUÇÕES PARA CRIAR AVD:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "1. Abra o Android Studio" -ForegroundColor White
        Write-Host "2. Vá em: Tools > Device Manager" -ForegroundColor White
        Write-Host "3. Clique em 'Create Device' (ou '+' no canto superior)" -ForegroundColor White
        Write-Host "4. Categoria: TV" -ForegroundColor White
        Write-Host "5. Selecione: 'TV (1080p)' ou 'TV (720p)'" -ForegroundColor White
        Write-Host "6. Clique 'Next'" -ForegroundColor White
        Write-Host "7. System Image: Escolha 'API 33' ou superior (Google APIs)" -ForegroundColor White
        Write-Host "   Se não tiver, clique 'Download' e aguarde" -ForegroundColor White
        Write-Host "8. Clique 'Next'" -ForegroundColor White
        Write-Host "9. AVD Name: FireStick_HD_Test" -ForegroundColor White
        Write-Host "10. Clique 'Finish'" -ForegroundColor White
        Write-Host ""
        Write-Host "CONFIGURAÇÕES RECOMENDADAS:" -ForegroundColor Cyan
        Write-Host "  - RAM: 2048 MB" -ForegroundColor White
        Write-Host "  - VM heap: 256 MB" -ForegroundColor White
        Write-Host "  - Internal Storage: 2048 MB" -ForegroundColor White
        Write-Host ""
        Write-Host "Após criar, você pode iniciar com:" -ForegroundColor Green
        Write-Host "  .\start-emulator-720p.bat" -ForegroundColor White
        Write-Host "  .\start-emulator-1080p.bat" -ForegroundColor White
    }
    "3" {
        Write-Host ""
        Write-Host "Iniciando emulador $avdName..." -ForegroundColor Yellow
        Start-Process -FilePath $emulator -ArgumentList "-avd", $avdName -NoNewWindow
        Write-Host "[OK] Emulador iniciado!" -ForegroundColor Green
    }
    "4" {
        Write-Host ""
        Write-Host "Iniciando emulador em 720p..." -ForegroundColor Yellow
        Start-Process -FilePath $emulator -ArgumentList "-avd", $avdName, "-skin", "1280x720", "-dpi-device", "213" -NoNewWindow
        Write-Host "[OK] Emulador iniciado em 720p!" -ForegroundColor Green
    }
    "5" {
        Write-Host ""
        Write-Host "Iniciando emulador em 1080p..." -ForegroundColor Yellow
        Start-Process -FilePath $emulator -ArgumentList "-avd", $avdName, "-skin", "1920x1080", "-dpi-device", "320" -NoNewWindow
        Write-Host "[OK] Emulador iniciado em 1080p!" -ForegroundColor Green
    }
    default {
        Write-Host "Opção inválida!" -ForegroundColor Red
    }
}

Write-Host ""


# Script para Criar Emulador Android TV (NÃO Smartphone)
# Perfil: Fire Stick HD (720p/1080p)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CRIAR EMULADOR ANDROID TV" -ForegroundColor Cyan
Write-Host "Perfil: Fire Stick HD" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$sdkPath = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$avdManager = "$sdkPath\cmdline-tools\latest\bin\avdmanager.bat"
$sdkManager = "$sdkPath\cmdline-tools\latest\bin\sdkmanager.bat"
$emulator = "$sdkPath\emulator\emulator.exe"
$avdName = "FireStick_HD_Test"

# Verificar SDK
if (-not (Test-Path $sdkPath)) {
    Write-Host "[ERRO] Android SDK nao encontrado!" -ForegroundColor Red
    exit 1
}

# Verificar avdmanager
if (-not (Test-Path $avdManager)) {
    $avdManager = "$sdkPath\tools\bin\avdmanager.bat"
}

if (-not (Test-Path $avdManager)) {
    Write-Host "[AVISO] AVD Manager nao disponivel via linha de comando" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "INSTRUCOES PARA CRIAR EMULADOR TV" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Abra o Android Studio" -ForegroundColor White
    Write-Host ""
    Write-Host "2. Vá em: Tools > Device Manager" -ForegroundColor White
    Write-Host ""
    Write-Host "3. Clique em 'Create Device' (ou botao '+' no canto superior)" -ForegroundColor White
    Write-Host ""
    Write-Host "4. IMPORTANTE: Escolha a categoria 'TV' (NAO Phone ou Tablet!)" -ForegroundColor Yellow
    Write-Host "   - Categoria: TV" -ForegroundColor Green
    Write-Host "   - Selecione: 'TV (1080p)' ou 'TV (720p)'" -ForegroundColor Green
    Write-Host ""
    Write-Host "5. Clique 'Next'" -ForegroundColor White
    Write-Host ""
    Write-Host "6. System Image: Escolha 'API 33' ou superior" -ForegroundColor White
    Write-Host "   - IMPORTANTE: Escolha 'Google APIs' (nao AOSP)" -ForegroundColor Yellow
    Write-Host "   - Se nao tiver, clique 'Download' e aguarde" -ForegroundColor White
    Write-Host ""
    Write-Host "7. Clique 'Next'" -ForegroundColor White
    Write-Host ""
    Write-Host "8. AVD Name: FireStick_HD_Test" -ForegroundColor White
    Write-Host ""
    Write-Host "9. Clique 'Show Advanced Settings' (opcional)" -ForegroundColor White
    Write-Host "   - RAM: 2048 MB" -ForegroundColor Gray
    Write-Host "   - VM heap: 256 MB" -ForegroundColor Gray
    Write-Host ""
    Write-Host "10. Clique 'Finish'" -ForegroundColor White
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "APOS CRIAR:" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Para iniciar em 1080p:" -ForegroundColor Green
    Write-Host "  .\start-emulator-1080p.bat" -ForegroundColor White
    Write-Host ""
    Write-Host "Para iniciar em 720p:" -ForegroundColor Green
    Write-Host "  .\start-emulator-720p.bat" -ForegroundColor White
    Write-Host ""
    exit 0
}

Write-Host "[OK] AVD Manager encontrado" -ForegroundColor Green
Write-Host ""

# Verificar AVDs existentes
Write-Host "Verificando AVDs existentes..." -ForegroundColor Yellow
$avds = & $avdManager list avd 2>&1

# Verificar se já existe um AVD de TV
$hasTvAvd = $false
foreach ($line in $avds) {
    if ($line -match $avdName) {
        $hasTvAvd = $true
        Write-Host "[OK] AVD '$avdName' ja existe!" -ForegroundColor Green
        break
    }
}

if ($hasTvAvd) {
    Write-Host ""
    Write-Host "Para iniciar:" -ForegroundColor Cyan
    Write-Host "  .\start-emulator-1080p.bat" -ForegroundColor White
    Write-Host "  .\start-emulator-720p.bat" -ForegroundColor White
    exit 0
}

# Listar AVDs existentes para verificar se são de TV
Write-Host ""
Write-Host "AVDs encontrados:" -ForegroundColor Cyan
$avds | ForEach-Object { 
    if ($_ -match "Name:") {
        $name = $_ -replace ".*Name:\s*", ""
        Write-Host "  - $name" -ForegroundColor White
    }
}

Write-Host ""
Write-Host "[AVISO] Nenhum AVD de TV encontrado!" -ForegroundColor Yellow
Write-Host ""
Write-Host "Para criar um AVD de TV, siga as instrucoes acima" -ForegroundColor Yellow
Write-Host "ou execute este script novamente apos criar via Android Studio." -ForegroundColor Yellow
Write-Host ""


# Script para instalar APK quando emulador estiver aberto
# Execute este script DEPOIS de abrir o emulador manualmente

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " AGUARDANDO EMULADOR" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Por favor, abra o emulador manualmente:" -ForegroundColor Yellow
Write-Host "1. Android Studio > Tools > Device Manager" -ForegroundColor White
Write-Host "2. Clique no Play do emulador" -ForegroundColor White
Write-Host ""
Write-Host "Aguardando emulador conectar..." -ForegroundColor Cyan
Write-Host ""

# Aguardar emulador conectar
$timeout = 120
$elapsed = 0
$connected = $false

while ($elapsed -lt $timeout -and -not $connected) {
    $devices = & $adb devices | Select-String "emulator.*device"
    if ($devices) {
        $connected = $true
        Write-Host "Emulador conectado!" -ForegroundColor Green
    } else {
        Write-Host "Aguardando... ($elapsed seg)" -ForegroundColor Gray
        Start-Sleep -Seconds 5
        $elapsed += 5
    }
}

if ($connected) {
    Write-Host ""
    Write-Host "Instalando APK..." -ForegroundColor Yellow
    & $adb install -r app\build\outputs\apk\debug\app-debug.apk
    
    Write-Host ""
    Write-Host "Abrindo app..." -ForegroundColor Green
    & $adb logcat -c
    & $adb shell am start -n com.maxiptv/.MainActivity
    
    Start-Sleep -Seconds 3
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host " APP ABERTO - FUNCIONALIDADES" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "SERIES:" -ForegroundColor Cyan
    Write-Host "  ✅ Seletor de temporadas" -ForegroundColor White
    Write-Host "  ✅ Botao de idioma (mostra so opcoes disponiveis)" -ForegroundColor White
    Write-Host "  ✅ Banners sem duplicados" -ForegroundColor White
    Write-Host ""
    Write-Host "FILMES:" -ForegroundColor Cyan
    Write-Host "  ✅ Botao de idioma (mostra so opcoes disponiveis)" -ForegroundColor White
    Write-Host "  ✅ Banners sem duplicados" -ForegroundColor White
    Write-Host ""
    Write-Host "PLAYER:" -ForegroundColor Cyan
    Write-Host "  ✅ Duplo clique = Fullscreen" -ForegroundColor White
    Write-Host ""
    Write-Host "LIVE:" -ForegroundColor Cyan
    Write-Host "  ✅ Logos dos canais" -ForegroundColor White
    Write-Host ""
    Write-Host "Pressione ENTER para monitorar logs..." -ForegroundColor Yellow
    Read-Host
    
    Write-Host ""
    Write-Host "MONITORANDO URLs do player:" -ForegroundColor Green
    Write-Host ""
    & $adb logcat -v time | Select-String "PlayerActivity.*URL|REPRODUZINDO" -CaseSensitive:$false
    
} else {
    Write-Host ""
    Write-Host "TIMEOUT - Emulador nao conectou em $timeout segundos" -ForegroundColor Red
    Write-Host ""
}







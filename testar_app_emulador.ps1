# Script para testar MaxiPTV no emulador e capturar erros
# ========================================

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " TESTE MaxiPTV - CAPTURA DE ERROS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se emulador está conectado
Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
$devices = & $adb devices | Select-String "emulator|device" | Select-String -NotMatch "List of devices"

if ($devices) {
    Write-Host "Dispositivo encontrado!" -ForegroundColor Green
    Write-Host ""
    
    # Limpar logs antigos
    Write-Host "Limpando logs antigos..." -ForegroundColor Yellow
    & $adb logcat -c
    Write-Host "OK" -ForegroundColor Green
    Write-Host ""
    
    # Abrir o app
    Write-Host "Abrindo MaxiPTV..." -ForegroundColor Yellow
    & $adb shell am start -n com.maxiptv/.MainActivity
    Write-Host "App aberto!" -ForegroundColor Green
    Write-Host ""
    
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host " AGUARDE - Monitorando erros..." -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Faca o login e navegue pelas categorias" -ForegroundColor Cyan
    Write-Host "Usuario: max" -ForegroundColor White
    Write-Host "Senha: 1h2yd90" -ForegroundColor White
    Write-Host ""
    Write-Host "Pressione CTRL+C quando o app fechar/crashar" -ForegroundColor Yellow
    Write-Host ""
    
    # Monitorar logs em tempo real
    & $adb logcat | Select-String "maxiptv|FATAL|Exception|AndroidRuntime"
    
} else {
    Write-Host "NENHUM DISPOSITIVO CONECTADO!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Para abrir o emulador:" -ForegroundColor Yellow
    Write-Host "1. Abra Android Studio" -ForegroundColor White
    Write-Host "2. Va em Tools > Device Manager" -ForegroundColor White
    Write-Host "3. Clique no play do emulador" -ForegroundColor White
    Write-Host ""
    Write-Host "OU execute:" -ForegroundColor Yellow
    Write-Host "  emulator -avd <nome_do_emulador>" -ForegroundColor Cyan
    Write-Host ""
}










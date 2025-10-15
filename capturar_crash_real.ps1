# Script para capturar crash do MaxiPTV

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    Write-Host "❌ ADB não encontrado!" -ForegroundColor Red
    Write-Host "Instale Android SDK Platform-Tools" -ForegroundColor Yellow
    exit
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CAPTURA DE CRASH - MaxiPTV" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Dispositivos conectados:" -ForegroundColor Yellow
& $adb devices
Write-Host ""

Write-Host "INSTRUÇÕES:" -ForegroundColor Yellow
Write-Host "1. Mantenha o dispositivo conectado" -ForegroundColor Gray
Write-Host "2. Pressione ENTER para limpar logs" -ForegroundColor Gray
Write-Host "3. ABRA O APP no dispositivo" -ForegroundColor Gray
Write-Host "4. Aguarde o crash e pressione ENTER novamente" -ForegroundColor Gray
Write-Host ""

Read-Host "Pressione ENTER para começar"

Write-Host ""
Write-Host "Limpando logs antigos..." -ForegroundColor Yellow
& $adb logcat -c

Write-Host "✅ Logs limpos!" -ForegroundColor Green
Write-Host ""
Write-Host "⚠️  ABRA O APP MAXIPTV AGORA!" -ForegroundColor Yellow
Write-Host ""

Read-Host "Pressione ENTER após o crash"

Write-Host ""
Write-Host "Capturando logs do crash..." -ForegroundColor Cyan

$logFile = "crash_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
& $adb logcat -d > $logFile

Write-Host ""
Write-Host "========================================" -ForegroundColor Red
Write-Host "  ERROS CRÍTICOS ENCONTRADOS:" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red
Write-Host ""

# Filtrar erros mais importantes
$erros = Get-Content $logFile | Select-String "FATAL|AndroidRuntime|Exception|maxiptv" -Context 3

if ($erros) {
    $erros | ForEach-Object { Write-Host $_ -ForegroundColor Red }
} else {
    Write-Host "Nenhum erro fatal encontrado nos logs" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Log completo salvo em: $logFile" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Envie o arquivo $logFile se precisar de mais ajuda" -ForegroundColor Yellow










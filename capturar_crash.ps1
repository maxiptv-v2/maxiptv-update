# Script para capturar logs do crash do MaxiPTV

$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adbPath)) {
    Write-Host "ADB não encontrado!" -ForegroundColor Red
    Write-Host "Instale o Android SDK Platform-Tools" -ForegroundColor Yellow
    exit
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CAPTURA DE CRASH - MaxiPTV" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Verificando dispositivo conectado..." -ForegroundColor Yellow
& $adbPath devices
Write-Host ""

Write-Host "INSTRUÇÕES:" -ForegroundColor Yellow
Write-Host "1. Mantenha o dispositivo conectado via USB" -ForegroundColor Gray
Write-Host "2. Pressione ENTER para limpar logs antigos" -ForegroundColor Gray
Write-Host "3. Abra o app MaxiPTV no dispositivo" -ForegroundColor Gray
Write-Host "4. Aguarde o crash" -ForegroundColor Gray
Write-Host ""

Read-Host "Pressione ENTER para começar"

Write-Host ""
Write-Host "Limpando logs antigos..." -ForegroundColor Yellow
& $adbPath logcat -c

Write-Host "Aguardando crash (abra o app AGORA)..." -ForegroundColor Green
Write-Host "Pressione ENTER após o crash" -ForegroundColor Yellow
Read-Host

Write-Host ""
Write-Host "Capturando logs..." -ForegroundColor Cyan

$logFile = "crash_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
& $adbPath logcat -d > $logFile

Write-Host ""
Write-Host "Filtrando erros críticos..." -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Red
Write-Host "  ERROS ENCONTRADOS:" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red

Get-Content $logFile | Select-String "FATAL|AndroidRuntime|maxiptv|Exception|Error" -Context 2

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Log completo salvo em: $logFile" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan










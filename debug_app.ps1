# Script de Debug do MaxiPTV
# Verifica logs do Android e identifica crashes

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DEBUG MaxiPTV - Análise de Crash" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se ADB está disponível
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

if (Test-Path $adbPath) {
    Write-Host "✓ ADB encontrado" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "Verificando dispositivos conectados..." -ForegroundColor Yellow
    & $adbPath devices
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  CAPTURANDO LOGS DO CRASH" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Conecte o dispositivo via USB" -ForegroundColor Yellow
    Write-Host "2. Ative 'Depuração USB' nas Configurações do Desenvolvedor" -ForegroundColor Yellow
    Write-Host "3. Abra o app MaxiPTV no dispositivo" -ForegroundColor Yellow
    Write-Host "4. Aguarde o crash..." -ForegroundColor Yellow
    Write-Host ""
    
    $resposta = Read-Host "Pressione ENTER quando o app crashar para capturar os logs"
    
    Write-Host ""
    Write-Host "Capturando últimos logs..." -ForegroundColor Green
    
    # Limpar logs antigos
    & $adbPath logcat -c
    
    # Capturar logs do crash
    Write-Host ""
    Write-Host "Logs do MaxiPTV:" -ForegroundColor Cyan
    & $adbPath logcat -d | Select-String -Pattern "maxiptv|AndroidRuntime|FATAL" -Context 2,5
    
    # Salvar logs completos
    $logFile = "crash_log_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
    & $adbPath logcat -d > $logFile
    Write-Host ""
    Write-Host "✓ Log completo salvo em: $logFile" -ForegroundColor Green
    
} else {
    Write-Host "✗ ADB não encontrado em: $adbPath" -ForegroundColor Red
    Write-Host ""
    Write-Host "Para instalar ADB:" -ForegroundColor Yellow
    Write-Host "1. Abra Android Studio" -ForegroundColor Gray
    Write-Host "2. Tools > SDK Manager" -ForegroundColor Gray
    Write-Host "3. SDK Tools > Android SDK Platform-Tools" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ANÁLISE DO CÓDIGO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar se tem tela de login
Write-Host "Verificando tela de login no código..." -ForegroundColor Yellow

$temLogin = $false
$arquivosKotlin = Get-ChildItem -Path "app\src\main\java" -Filter "*.kt" -Recurse

foreach ($arquivo in $arquivosKotlin) {
    $conteudo = Get-Content $arquivo.FullName -Raw
    if ($conteudo -match "TextField.*password|OutlinedTextField.*password|login|Login|authentication") {
        Write-Host "  Possível tela de login em: $($arquivo.Name)" -ForegroundColor Cyan
        $temLogin = $true
    }
}

if (-not $temLogin) {
    Write-Host "  ✗ Nenhuma tela de login detectada no código" -ForegroundColor Red
    Write-Host ""
    Write-Host "  O app usa credenciais fixas no build.gradle.kts:" -ForegroundColor Yellow
    Write-Host "    - API: https://canais.is/player_api.php" -ForegroundColor Gray
    Write-Host "    - User: max" -ForegroundColor Gray
    Write-Host "    - Pass: 1h2yd90" -ForegroundColor Gray
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  POSSÍVEIS CAUSAS DO CRASH" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Permissões de Internet não concedidas" -ForegroundColor Yellow
Write-Host "2. Problema ao conectar com a API" -ForegroundColor Yellow
Write-Host "3. Falta de biblioteca nativa" -ForegroundColor Yellow
Write-Host "4. Erro no código Kotlin/Compose" -ForegroundColor Yellow
Write-Host "5. Incompatibilidade com versão do Android" -ForegroundColor Yellow

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan










# Script para verificar se a lógica de Wi-Fi lento está implementada corretamente

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  VERIFICAÇÃO: LÓGICA WI-FI LENTO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar variáveis de controle
Write-Host "1. Verificando variáveis de controle..." -ForegroundColor Yellow
$variables = Select-String -Path "app\src\main\java\com\maxiptv\ui\player\PlayerActivity.kt" -Pattern "bufferingCount|qualityReduced|currentMaxBitrate|lastBufferingTime" -CaseSensitive
if ($variables) {
    Write-Host "   ✅ Variáveis de controle encontradas:" -ForegroundColor Green
    $variables | ForEach-Object {
        Write-Host "      $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "   ❌ Variáveis de controle NÃO encontradas!" -ForegroundColor Red
}

# Verificar detecção de buffering
Write-Host ""
Write-Host "2. Verificando detecção de buffering..." -ForegroundColor Yellow
$bufferingDetection = Select-String -Path "app\src\main\java\com\maxiptv\ui\player\PlayerActivity.kt" -Pattern "STATE_BUFFERING|buffering frequente|Wi-Fi lento detectado" -CaseSensitive
if ($bufferingDetection) {
    Write-Host "   ✅ Detecção de buffering encontrada:" -ForegroundColor Green
    $bufferingDetection | ForEach-Object {
        Write-Host "      Linha $($_.LineNumber): $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "   ❌ Detecção de buffering NÃO encontrada!" -ForegroundColor Red
}

# Verificar aplicação de novo bitrate
Write-Host ""
Write-Host "3. Verificando aplicação de novo bitrate..." -ForegroundColor Yellow
$bitrateApplication = Select-String -Path "app\src\main\java\com\maxiptv\ui\player\PlayerActivity.kt" -Pattern "setMaxVideoBitrate|Reduzindo qualidade|Qualidade reduzida" -CaseSensitive
if ($bitrateApplication) {
    Write-Host "   ✅ Aplicação de novo bitrate encontrada:" -ForegroundColor Green
    $bitrateApplication | ForEach-Object {
        Write-Host "      Linha $($_.LineNumber): $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "   ❌ Aplicação de novo bitrate NÃO encontrada!" -ForegroundColor Red
}

# Verificar listener
Write-Host ""
Write-Host "4. Verificando listener do player..." -ForegroundColor Yellow
$listener = Select-String -Path "app\src\main\java\com\maxiptv\ui\player\PlayerActivity.kt" -Pattern "onPlaybackStateChanged|addListener" -CaseSensitive
if ($listener) {
    Write-Host "   ✅ Listener do player encontrado:" -ForegroundColor Green
    $listener | Select-Object -First 3 | ForEach-Object {
        Write-Host "      Linha $($_.LineNumber): $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "   ❌ Listener do player NÃO encontrado!" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ✅ VERIFICAÇÃO CONCLUÍDA" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan


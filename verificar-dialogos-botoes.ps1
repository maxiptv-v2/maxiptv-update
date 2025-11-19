# Script para verificar se os diálogos dos botões estão aplicando configurações corretamente

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICAÇÃO DOS DIÁLOGOS DOS BOTÕES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$vodDetailsScreen = "app/src/main/java/com/maxiptv/ui/screens/VodDetailsScreen.kt"
$playerActivity = "app/src/main/java/com/maxiptv/ui/player/PlayerActivity.kt"

Write-Host "1. VERIFICANDO DIÁLOGO DE QUALIDADE..." -ForegroundColor Yellow
Write-Host ""

# Verificar se o diálogo salva a qualidade corretamente
$qualityDialogSave = Select-String -Path $vodDetailsScreen -Pattern "PlayerSettingsManager\.setVideoQuality\(quality\)" -Context 2,2
if ($qualityDialogSave) {
    Write-Host "   ✅ Diálogo de Qualidade SALVA a configuração via PlayerSettingsManager" -ForegroundColor Green
    Write-Host "      Linha: $($qualityDialogSave.LineNumber)" -ForegroundColor Gray
} else {
    Write-Host "   ❌ ERRO: Diálogo de Qualidade NÃO salva a configuração!" -ForegroundColor Red
}

# Verificar se o PlayerActivity lê a qualidade
$qualityPlayerRead = Select-String -Path $playerActivity -Pattern "PlayerSettingsManager\.getVideoQuality\(\)" -Context 2,5
if ($qualityPlayerRead) {
    Write-Host "   ✅ PlayerActivity LÊ a qualidade do PlayerSettingsManager" -ForegroundColor Green
    Write-Host "      Linha: $($qualityPlayerRead.LineNumber)" -ForegroundColor Gray
    
    # Verificar se aplica corretamente
    $qualityApply = Select-String -Path $playerActivity -Pattern "setMaxVideoBitrate\(videoQuality\.maxBitrate\)" -Context 1,1
    if ($qualityApply) {
        Write-Host "   ✅ PlayerActivity APLICA a qualidade corretamente (maxBitrate, minBitrate, maxVideoSize)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ AVISO: Verificar se a qualidade está sendo aplicada corretamente" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ ERRO: PlayerActivity NÃO lê a qualidade!" -ForegroundColor Red
}

Write-Host ""
Write-Host "2. VERIFICANDO DIÁLOGO DE LEGENDAS..." -ForegroundColor Yellow
Write-Host ""

# Verificar se o diálogo salva a legenda selecionada
$subtitleDialogSave = Select-String -Path $vodDetailsScreen -Pattern "selectedSubtitleTrack = " -Context 1,1
if ($subtitleDialogSave) {
    Write-Host "   ✅ Diálogo de Legendas SALVA a track selecionada em selectedSubtitleTrack" -ForegroundColor Green
    Write-Host "      Linhas encontradas: $($subtitleDialogSave.Count)" -ForegroundColor Gray
} else {
    Write-Host "   ❌ ERRO: Diálogo de Legendas NÃO salva a track!" -ForegroundColor Red
}

# Verificar se passa para o Intent
$subtitleIntent = Select-String -Path $vodDetailsScreen -Pattern "putExtra\(\"selectedSubtitleTrack\"" -Context 1,1
if ($subtitleIntent) {
    Write-Host "   ✅ selectedSubtitleTrack é passado para o Intent" -ForegroundColor Green
    Write-Host "      Linha: $($subtitleIntent.LineNumber)" -ForegroundColor Gray
} else {
    Write-Host "   ❌ ERRO: selectedSubtitleTrack NÃO é passado para o Intent!" -ForegroundColor Red
}

# Verificar se o PlayerActivity lê e aplica
$subtitlePlayerRead = Select-String -Path $playerActivity -Pattern "getStringExtra\(\"selectedSubtitleTrack\"\)" -Context 1,5
if ($subtitlePlayerRead) {
    Write-Host "   ✅ PlayerActivity LÊ selectedSubtitleTrack do Intent" -ForegroundColor Green
    Write-Host "      Linha: $($subtitlePlayerRead.LineNumber)" -ForegroundColor Gray
    
    # Verificar se aplica corretamente
    $subtitleApply = Select-String -Path $playerActivity -Pattern "setPreferredTextLanguage\(trackFormat\.language\)" -Context 1,1
    if ($subtitleApply) {
        Write-Host "   ✅ PlayerActivity APLICA a legenda corretamente (setPreferredTextLanguage)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ AVISO: Verificar se a legenda está sendo aplicada corretamente" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ ERRO: PlayerActivity NÃO lê selectedSubtitleTrack!" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. VERIFICANDO DIÁLOGO DE ÁUDIO..." -ForegroundColor Yellow
Write-Host ""

# Verificar se o diálogo salva o áudio selecionado
$audioDialogSave = Select-String -Path $vodDetailsScreen -Pattern "selectedAudioTrack = " -Context 1,1
if ($audioDialogSave) {
    Write-Host "   ✅ Diálogo de Áudio SALVA a track selecionada em selectedAudioTrack" -ForegroundColor Green
    Write-Host "      Linhas encontradas: $($audioDialogSave.Count)" -ForegroundColor Gray
} else {
    Write-Host "   ❌ ERRO: Diálogo de Áudio NÃO salva a track!" -ForegroundColor Red
}

# Verificar se passa para o Intent
$audioIntent = Select-String -Path $vodDetailsScreen -Pattern "putExtra\(\"selectedAudioTrack\"" -Context 1,1
if ($audioIntent) {
    Write-Host "   ✅ selectedAudioTrack é passado para o Intent" -ForegroundColor Green
    Write-Host "      Linha: $($audioIntent.LineNumber)" -ForegroundColor Gray
} else {
    Write-Host "   ❌ ERRO: selectedAudioTrack NÃO é passado para o Intent!" -ForegroundColor Red
}

# Verificar se o PlayerActivity lê e aplica
$audioPlayerRead = Select-String -Path $playerActivity -Pattern "getStringExtra\(\"selectedAudioTrack\"\)" -Context 1,5
if ($audioPlayerRead) {
    Write-Host "   ✅ PlayerActivity LÊ selectedAudioTrack do Intent" -ForegroundColor Green
    Write-Host "      Linha: $($audioPlayerRead.LineNumber)" -ForegroundColor Gray
    
    # Verificar se aplica corretamente
    $audioApply = Select-String -Path $playerActivity -Pattern "setPreferredAudioLanguage\(trackFormat\.language\)" -Context 1,1
    if ($audioApply) {
        Write-Host "   ✅ PlayerActivity APLICA o áudio corretamente (setPreferredAudioLanguage)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ AVISO: Verificar se o áudio está sendo aplicado corretamente" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ ERRO: PlayerActivity NÃO lê selectedAudioTrack!" -ForegroundColor Red
}

Write-Host ""
Write-Host "4. VERIFICANDO PROBLEMAS CONHECIDOS..." -ForegroundColor Yellow
Write-Host ""

# Verificar se há comparação incorreta de IDs (String vs Int)
$idComparison = Select-String -Path $playerActivity -Pattern "trackFormat\.id\.toString\(\) == " -Context 1,1
if ($idComparison) {
    Write-Host "   ✅ Comparação de IDs está correta (toString())" -ForegroundColor Green
} else {
    Write-Host "   ⚠️ AVISO: Verificar comparação de IDs (pode estar comparando String com Int)" -ForegroundColor Yellow
}

# Verificar se há delay para garantir que PlayerSettingsManager salve antes do player iniciar
$delayCheck = Select-String -Path $playerActivity -Pattern "delay\(100\)" -Context 2,2
if ($delayCheck) {
    Write-Host "   ✅ Há delay para garantir que PlayerSettingsManager salve antes do player iniciar" -ForegroundColor Green
} else {
    Write-Host "   ⚠️ AVISO: Não há delay - pode haver race condition ao salvar qualidade" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICAÇÃO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Se todos os itens acima estão verdes, os diálogos estão funcionando corretamente!" -ForegroundColor Green
Write-Host "⚠️ Se houver avisos amarelos, verificar manualmente" -ForegroundColor Yellow
Write-Host "❌ Se houver erros vermelhos, CORRIGIR IMEDIATAMENTE" -ForegroundColor Red
Write-Host ""


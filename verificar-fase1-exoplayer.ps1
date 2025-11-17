# Script para verificar implementação da Fase 1 - ExoPlayer Melhorias Profissionais

Write-Host ""
Write-Host "🔍 VERIFICANDO FASE 1 - EXOPLAYER MELHORIAS PROFISSIONAIS" -ForegroundColor Cyan
Write-Host ""

$errors = @()
$warnings = @()
$success = @()

$playerActivityPath = "app/src/main/java/com/maxiptv/ui/player/PlayerActivity.kt"

if (-not (Test-Path $playerActivityPath)) {
    Write-Host "❌ ERRO: Arquivo PlayerActivity.kt não encontrado!" -ForegroundColor Red
    exit 1
}

$content = Get-Content $playerActivityPath -Raw

# 1. Verificar se aplicação de configurações está implementada
Write-Host "1️⃣ Verificando aplicação de configurações..." -ForegroundColor Yellow

if ($content -match "PlayerSettingsManager\.getPlaybackSpeed\(\)") {
    $success += "✅ Velocidade de reprodução configurada sendo aplicada"
} else {
    $errors += "❌ Velocidade de reprodução NÃO está sendo aplicada"
}

if ($content -match "PlayerSettingsManager\.getVideoQuality\(\)") {
    $success += "✅ Qualidade de vídeo configurada sendo aplicada"
} else {
    $errors += "❌ Qualidade de vídeo NÃO está sendo aplicada"
}

if ($content -match "PlaybackParameters") {
    $success += "✅ PlaybackParameters sendo usado para velocidade"
} else {
    $errors += "❌ PlaybackParameters NÃO está sendo usado"
}

if ($content -match "lifecycleScope\.launch") {
    $success += "✅ lifecycleScope.launch usado para operações assíncronas"
} else {
    $warnings += "⚠️ lifecycleScope.launch pode não estar sendo usado"
}

# 2. Verificar seleção manual de qualidade
Write-Host "2️⃣ Verificando seleção manual de qualidade..." -ForegroundColor Yellow

if ($content -match "showQualityDialog") {
    $success += "✅ Função showQualityDialog() implementada"
} else {
    $errors += "❌ Função showQualityDialog() NÃO encontrada"
}

if ($content -match "applyQuality") {
    $success += "✅ Função applyQuality() implementada"
} else {
    $errors += "❌ Função applyQuality() NÃO encontrada"
}

if ($content -match "applyFormatQuality") {
    $success += "✅ Função applyFormatQuality() implementada"
} else {
    $errors += "❌ Função applyFormatQuality() NÃO encontrada"
}

if ($content -match "AlertDialog\.Builder") {
    $success += "✅ AlertDialog sendo usado para seleção de qualidade"
} else {
    $errors += "❌ AlertDialog NÃO está sendo usado"
}

if ($content -match "currentTracks") {
    $success += "✅ currentTracks sendo usado para buscar tracks disponíveis"
} else {
    $warnings += "⚠️ currentTracks pode não estar sendo usado corretamente"
}

# 3. Verificar controles avançados (TV)
Write-Host "3️⃣ Verificando controles avançados (TV)..." -ForegroundColor Yellow

if ($content -match "onKeyDown") {
    $success += "✅ Função onKeyDown() implementada"
} else {
    $errors += "❌ Função onKeyDown() NÃO encontrada"
}

if ($content -match "KEYCODE_DPAD_LEFT") {
    $success += "✅ Controle SETA ESQUERDA implementado (retroceder 10s)"
} else {
    $errors += "❌ Controle SETA ESQUERDA NÃO implementado"
}

if ($content -match "KEYCODE_DPAD_RIGHT") {
    $success += "✅ Controle SETA DIREITA implementado (avançar 10s)"
} else {
    $errors += "❌ Controle SETA DIREITA NÃO implementado"
}

if ($content -match "KEYCODE_MENU") {
    $success += "✅ Controle MENU implementado (abrir qualidade)"
} else {
    $errors += "❌ Controle MENU NÃO implementado"
}

if ($content -match "seekTo") {
    $success += "✅ Função seekTo() sendo usada para avançar/retroceder"
} else {
    $errors += "❌ Função seekTo() NÃO está sendo usada"
}

if ($content -match "showSeekIndicator") {
    $success += "✅ Função showSeekIndicator() implementada"
} else {
    $warnings += "⚠️ Função showSeekIndicator() pode não estar implementada"
}

# 4. Verificar imports necessários
Write-Host "4️⃣ Verificando imports necessários..." -ForegroundColor Yellow

$requiredImports = @(
    "PlayerSettingsManager",
    "PlaybackParameters",
    "lifecycleScope",
    "KeyEvent",
    "AlertDialog",
    "Format",
    "Tracks"
)

foreach ($import in $requiredImports) {
    if ($content -match $import) {
        $success += "✅ Import relacionado a '$import' encontrado"
    } else {
        $warnings += "⚠️ Import relacionado a '$import' pode estar faltando"
    }
}

# 5. Verificar tratamento de erros
Write-Host "5️⃣ Verificando tratamento de erros..." -ForegroundColor Yellow

if ($content -match "try\s*\{") {
    $success += "✅ Blocos try-catch implementados"
} else {
    $warnings += "⚠️ Tratamento de erros pode estar incompleto"
}

# 6. Verificar logs de debug
Write-Host "6️⃣ Verificando logs de debug..." -ForegroundColor Yellow

if ($content -match "android\.util\.Log\.i.*Velocidade aplicada") {
    $success += "✅ Log de velocidade aplicada implementado"
} else {
    $warnings += "⚠️ Log de velocidade pode não estar implementado"
}

if ($content -match "android\.util\.Log\.i.*Qualidade aplicada") {
    $success += "✅ Log de qualidade aplicada implementado"
} else {
    $warnings += "⚠️ Log de qualidade pode não estar implementado"
}

# 7. Verificar compatibilidade com MaxiApp.isTv
Write-Host "7️⃣ Verificando compatibilidade com TV..." -ForegroundColor Yellow

if ($content -match "MaxiApp\.isTv") {
    $success += "✅ Verificação MaxiApp.isTv implementada"
} else {
    $warnings += "⚠️ Verificação MaxiApp.isTv pode não estar implementada"
}

# Resultados
Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "RESULTADOS DA VERIFICAÇÃO" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

if ($success.Count -gt 0) {
    Write-Host "✅ SUCESSOS ($($success.Count)):" -ForegroundColor Green
    foreach ($item in $success) {
        Write-Host "   $item" -ForegroundColor Green
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "⚠️ AVISOS ($($warnings.Count)):" -ForegroundColor Yellow
    foreach ($item in $warnings) {
        Write-Host "   $item" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($errors.Count -gt 0) {
    Write-Host "❌ ERROS ($($errors.Count)):" -ForegroundColor Red
    foreach ($item in $errors) {
        Write-Host "   $item" -ForegroundColor Red
    }
    Write-Host ""
}

# Resumo final
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "RESUMO" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

$totalChecks = $success.Count + $warnings.Count + $errors.Count
$successRate = if ($totalChecks -gt 0) { [math]::Round(($success.Count / $totalChecks) * 100, 1) } else { 0 }

Write-Host "Total de verificações: $totalChecks" -ForegroundColor White
Write-Host "Sucessos: $($success.Count)" -ForegroundColor Green
Write-Host "Avisos: $($warnings.Count)" -ForegroundColor Yellow
Write-Host "Erros: $($errors.Count)" -ForegroundColor Red
Write-Host "Taxa de sucesso: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 60) { "Yellow" } else { "Red" })
Write-Host ""

if ($errors.Count -eq 0) {
    Write-Host "✅ IMPLEMENTAÇÃO DA FASE 1 ESTÁ CORRETA!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Funcionalidades implementadas:" -ForegroundColor Cyan
    Write-Host "  • Aplicação automática de velocidade e qualidade" -ForegroundColor White
    Write-Host "  • Seleção manual de qualidade (botão MENU)" -ForegroundColor White
    Write-Host "  • Controles avançados TV (setas esquerda/direita)" -ForegroundColor White
    Write-Host "  • Indicadores visuais básicos" -ForegroundColor White
    Write-Host ""
    Write-Host "Pronto para testar no dispositivo!" -ForegroundColor Green
} else {
    Write-Host "❌ IMPLEMENTAÇÃO TEM ERROS QUE PRECISAM SER CORRIGIDOS" -ForegroundColor Red
    Write-Host ""
    Write-Host "Corrija os erros acima antes de testar." -ForegroundColor Yellow
}

Write-Host ""


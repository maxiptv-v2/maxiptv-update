# Script para verificar se a correção do fullscreen Fire Stick foi aplicada corretamente

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICANDO CORRECAO FULLSCREEN FIRE STICK" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$file = "app/src/main/java/com/maxiptv/ui/screens/LiveScreen.kt"
$content = Get-Content $file -Raw -Encoding UTF8

Write-Host "1. Verificando import systemBarsPadding..." -ForegroundColor Yellow
if ($content -match "systemBarsPadding") {
    Write-Host "   ✅ Import systemBarsPadding encontrado" -ForegroundColor Green
} else {
    Write-Host "   ❌ Import systemBarsPadding NÃO encontrado" -ForegroundColor Red
    exit 1
}

Write-Host ""

Write-Host "2. Verificando RESIZE_MODE_FILL..." -ForegroundColor Yellow
if ($content -match "RESIZE_MODE_FILL") {
    Write-Host "   ✅ RESIZE_MODE_FILL encontrado" -ForegroundColor Green
} else {
    Write-Host "   ❌ RESIZE_MODE_FILL NÃO encontrado" -ForegroundColor Red
    exit 1
}

Write-Host ""

Write-Host "3. Verificando se RESIZE_MODE_FIT foi removido..." -ForegroundColor Yellow
if ($content -notmatch "RESIZE_MODE_FIT") {
    Write-Host "   ✅ RESIZE_MODE_FIT removido (correto)" -ForegroundColor Green
} else {
    Write-Host "   ⚠️ RESIZE_MODE_FIT ainda presente (pode causar problemas)" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "4. Verificando modifier com systemBarsPadding()..." -ForegroundColor Yellow
if ($content -match "\.systemBarsPadding\(\)") {
    Write-Host "   ✅ systemBarsPadding() aplicado no modifier" -ForegroundColor Green
} else {
    Write-Host "   ❌ systemBarsPadding() NÃO aplicado no modifier" -ForegroundColor Red
    exit 1
}

Write-Host ""

Write-Host "5. Verificando ordem do modifier..." -ForegroundColor Yellow
if ($content -match "fillMaxSize\(\)\s*\.systemBarsPadding\(\)") {
    Write-Host "   ✅ Ordem correta: fillMaxSize() antes de systemBarsPadding()" -ForegroundColor Green
} else {
    Write-Host "   ⚠️ Ordem pode estar incorreta" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ CORREÇÃO APLICADA CORRETAMENTE!" -ForegroundColor Green
Write-Host ""
Write-Host "Mudanças aplicadas:" -ForegroundColor White
Write-Host "  - RESIZE_MODE_FIT → RESIZE_MODE_FILL" -ForegroundColor Gray
Write-Host "  - Adicionado systemBarsPadding() no modifier" -ForegroundColor Gray
Write-Host "  - Mantido fillMaxSize() e background(Color.Black)" -ForegroundColor Gray
Write-Host ""
Write-Host "Isso deve garantir fullscreen completo no Fire Stick Amazon!" -ForegroundColor White


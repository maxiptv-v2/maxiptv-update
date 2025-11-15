# Script para verificar compatibilidade de cores com Fire OS
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "VERIFICACAO: Compatibilidade de Cores com Fire OS" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$file = "app/src/main/java/com/maxiptv/ui/screens/HomeScreen.kt"

if (-not (Test-Path $file)) {
    Write-Host "[ERRO] Arquivo nao encontrado: $file" -ForegroundColor Red
    exit 1
}

Write-Host "[1] Verificando estrutura do CategoryButton..." -ForegroundColor Yellow
Write-Host ""

# Buscar CategoryButton completo
$categoryButton = Select-String -Path $file -Pattern "fun CategoryButton" -Context 0, 120
if ($categoryButton) {
    Write-Host "✅ Funcao CategoryButton encontrada" -ForegroundColor Green
    Write-Host ""
    
    # Verificar contentColor do Button
    Write-Host "[2] Verificando contentColor do Button:" -ForegroundColor Yellow
    $contentColor = Select-String -Path $file -Pattern "contentColor\s*=\s*Color\.Black" -Context 2, 5
    if ($contentColor) {
        Write-Host "  ✅ contentColor = Color.Black encontrado no Button" -ForegroundColor Green
        $contentColor | ForEach-Object {
            if ($_.Line -match "contentColor") {
                Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "  ❌ contentColor nao encontrado!" -ForegroundColor Red
    }
    
    Write-Host ""
    
    # Verificar se Text herda cor ou tem cor explícita
    Write-Host "[3] Verificando Text do emoji e nome:" -ForegroundColor Yellow
    
    $emojiText = Select-String -Path $file -Pattern "Text\s*\([^)]*text\s*=\s*emoji" -Context 0, 5
    if ($emojiText) {
        Write-Host "  ✅ Text do emoji encontrado" -ForegroundColor Green
        $hasColor = $emojiText | Where-Object { $_.Line -match "color\s*=" }
        if ($hasColor) {
            Write-Host "     ✅ Tem cor explícita definida" -ForegroundColor Green
            $hasColor | ForEach-Object { Write-Host "        $($_.Line.Trim())" -ForegroundColor Gray }
        } else {
            Write-Host "     ⚠️  Nao tem cor explícita - herdara do contentColor do Button" -ForegroundColor Yellow
            Write-Host "        Isso funciona no TV Box, mas pode nao funcionar no Fire OS" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    
    $nameText = Select-String -Path $file -Pattern "Text\s*\([^)]*text\s*=\s*text" -Context 0, 8
    if ($nameText) {
        Write-Host "  ✅ Text do nome encontrado" -ForegroundColor Green
        $hasColor = $nameText | Where-Object { $_.Line -match "color\s*=" }
        if ($hasColor) {
            Write-Host "     ✅ Tem cor explícita definida" -ForegroundColor Green
            $hasColor | ForEach-Object { Write-Host "        $($_.Line.Trim())" -ForegroundColor Gray }
        } else {
            Write-Host "     ⚠️  Nao tem cor explícita - herdara do contentColor do Button" -ForegroundColor Yellow
            Write-Host "        Isso funciona no TV Box, mas pode nao funcionar no Fire OS" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    Write-Host "[4] Verificando compatibilidade com Fire OS:" -ForegroundColor Yellow
    
    # Fire OS pode ter problemas com herança de cor
    Write-Host "  ⚠️  Fire OS (Fire Stick Amazon) pode ter problemas com heranca de cor" -ForegroundColor Yellow
    Write-Host "     Material3 Compose pode nao aplicar contentColor corretamente no Fire OS" -ForegroundColor Yellow
    Write-Host ""
    
    Write-Host "===============================================================" -ForegroundColor Cyan
    Write-Host "RESULTADO:" -ForegroundColor Cyan
    Write-Host "===============================================================" -ForegroundColor Cyan
    Write-Host ""
    
    $emojiHasColor = $emojiText | Where-Object { $_.Line -match "color\s*=" }
    $nameHasColor = $nameText | Where-Object { $_.Line -match "color\s*=" }
    
    if (-not $emojiHasColor -and -not $nameHasColor) {
        Write-Host "⚠️  STATUS: Cores nao explicitas - pode nao funcionar no Fire OS" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "RECOMENDACAO:" -ForegroundColor Cyan
        Write-Host "  Para garantir compatibilidade com Fire OS, adicione:" -ForegroundColor White
        Write-Host "  - color = Color.Black no Text do emoji" -ForegroundColor Green
        Write-Host "  - color = Color.Black no Text do nome" -ForegroundColor Green
        Write-Host ""
        Write-Host "  OU teste primeiro no Fire Stick para ver se funciona sem cores explicitas" -ForegroundColor Yellow
    } else {
        Write-Host "✅ STATUS: Cores explicitas definidas - deve funcionar em todos os dispositivos" -ForegroundColor Green
    }
    
    Write-Host ""
    Write-Host "NOTA: No TV Box funciona sem cores explicitas porque herda do contentColor" -ForegroundColor Gray
    Write-Host "      No Fire OS pode ser necessario definir cores explicitas" -ForegroundColor Gray
    Write-Host ""
    
} else {
    Write-Host "❌ Funcao CategoryButton nao encontrada!" -ForegroundColor Red
}


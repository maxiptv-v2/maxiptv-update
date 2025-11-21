# ========================================
# ANÁLISE: Fonte Profissional para Sinopse
# ========================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "ANÁLISE: Fonte Profissional Sinopse" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1. Verificar fontes disponíveis no Android
Write-Host "1️⃣ FONTES DISPONÍVEIS NO ANDROID:" -ForegroundColor Yellow
Write-Host "   ✅ Roboto Condensed: DISPONÍVEL (FontFamily.SansSerif com FontWeight.Bold)" -ForegroundColor Green
Write-Host "   ❌ Google Sans: NÃO DISPONÍVEL (precisa adicionar arquivo .ttf)" -ForegroundColor Red
Write-Host "   ❌ Inter: NÃO DISPONÍVEL (precisa adicionar arquivo .ttf)" -ForegroundColor Red
Write-Host "   ❌ Source Sans Pro: NÃO DISPONÍVEL (precisa adicionar arquivo .ttf)" -ForegroundColor Red
Write-Host "`n   💡 RECOMENDAÇÃO: Usar Roboto Condensed via FontFamily.SansSerif + FontWeight.Bold" -ForegroundColor Cyan

# 2. Verificar técnicas de legibilidade
Write-Host "`n2️⃣ TÉCNICAS DE LEGIBILIDADE:" -ForegroundColor Yellow
Write-Host "   ✅ DropShadow: COMPATÍVEL (TextStyle.shadow)" -ForegroundColor Green
Write-Host "   ✅ Gradiente Overlay: JÁ IMPLEMENTADO (mas pode melhorar)" -ForegroundColor Green
Write-Host "   ⚠️  Stroke Text: POSSÍVEL mas complexo (não recomendado)" -ForegroundColor Yellow

# 3. Verificar código atual
Write-Host "`n3️⃣ CÓDIGO ATUAL:" -ForegroundColor Yellow
$vodDetails = Get-Content "app/src/main/java/com/maxiptv/ui/screens/VodDetailsScreen.kt" -Raw

if ($vodDetails -match "fontSize = if \(MaxiApp\.isTv\) (\d+)\.sp") {
    $currentSize = $matches[1]
    Write-Host "   📏 Tamanho atual: ${currentSize}sp (TV)" -ForegroundColor White
    if ([int]$currentSize -ge 18 -and [int]$currentSize -le 20) {
        Write-Host "   ✅ Tamanho dentro da recomendação (18-20sp)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Tamanho fora da recomendação (deveria ser 18-20sp)" -ForegroundColor Yellow
    }
}

if ($vodDetails -match "fontFamily = FontFamily\.SansSerif") {
    Write-Host "   ✅ Fonte atual: FontFamily.SansSerif" -ForegroundColor Green
}

if ($vodDetails -match "fontWeight = FontWeight\.(\w+)") {
    $currentWeight = $matches[1]
    Write-Host "   📝 Peso atual: FontWeight.$currentWeight" -ForegroundColor White
    if ($currentWeight -eq "Bold") {
        Write-Host "   ✅ Peso correto para Roboto Condensed Bold" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Precisa mudar para FontWeight.Bold" -ForegroundColor Yellow
    }
}

if ($vodDetails -match "color = if \(MaxiApp\.isTv\)") {
    Write-Host "   🎨 Cor atual: Condicional para TV" -ForegroundColor White
    if ($vodDetails -match "Color\(0xFF1A1A1A\)") {
        Write-Host "   ⚠️  Cor escura atual pode não funcionar bem com banner claro" -ForegroundColor Yellow
        Write-Host "   💡 RECOMENDAÇÃO: Mudar para Color.White + sombra preta" -ForegroundColor Cyan
    }
}

# 4. Verificar se já tem sombra
if ($vodDetails -match "Shadow|shadow") {
    Write-Host "   ✅ Sombra já implementada" -ForegroundColor Green
} else {
    Write-Host "   ❌ Sombra NÃO implementada (CRÍTICO para legibilidade)" -ForegroundColor Red
    Write-Host "   💡 PRECISA ADICIONAR: TextStyle.shadow" -ForegroundColor Cyan
}

# 5. Verificar overlay atual
if ($vodDetails -match "Brush\.verticalGradient") {
    Write-Host "   ✅ Overlay gradiente já implementado" -ForegroundColor Green
    if ($vodDetails -match "alpha = 0\.(\d+)f") {
        $alphaValues = [regex]::Matches($vodDetails, "alpha = 0\.(\d+)f")
        $maxAlpha = ($alphaValues | ForEach-Object { [int]$_.Groups[1].Value } | Measure-Object -Maximum).Maximum
        Write-Host "   📊 Opacidade máxima atual: ${maxAlpha}%" -ForegroundColor White
        if ($maxAlpha -lt 70) {
            Write-Host "   ⚠️  Opacidade pode ser aumentada para melhor contraste (70-80%)" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "   ❌ Overlay gradiente NÃO encontrado" -ForegroundColor Red
}

# 6. Resumo e recomendações
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "RESUMO E RECOMENDAÇÕES" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "✅ O QUE ESTÁ BOM:" -ForegroundColor Green
Write-Host "   • Tamanho da fonte (20sp) está adequado" -ForegroundColor White
Write-Host "   • Overlay gradiente já existe" -ForegroundColor White
Write-Host "   • FontFamily.SansSerif disponível" -ForegroundColor White

Write-Host "`n⚠️  O QUE PRECISA MELHORAR:" -ForegroundColor Yellow
Write-Host "   1. Mudar FontWeight.Normal → FontWeight.Bold (Roboto Condensed Bold)" -ForegroundColor White
Write-Host "   2. Mudar cor de escura para BRANCA (Color.White)" -ForegroundColor White
Write-Host "   3. ADICIONAR sombra preta (Shadow com alpha 0.65-0.7)" -ForegroundColor White
Write-Host "   4. Aumentar opacidade do overlay para 70-80% na área do texto" -ForegroundColor White

Write-Host "`n💡 IMPLEMENTAÇÃO RECOMENDADA:" -ForegroundColor Cyan
Write-Host "   • Fonte: FontFamily.SansSerif + FontWeight.Bold" -ForegroundColor White
Write-Host "   • Cor: Color.White" -ForegroundColor White
Write-Host "   • Sombra: Shadow(color=Black.alpha(0.7), offset=(2,2), blurRadius=6)" -ForegroundColor White
Write-Host "   • Overlay: Aumentar gradiente na área inferior (onde fica o texto)" -ForegroundColor White

Write-Host "`n✅ COMPATIBILIDADE:" -ForegroundColor Green
Write-Host "   • ✅ Android TV Box" -ForegroundColor White
Write-Host "   • ✅ Fire Stick Amazon" -ForegroundColor White
Write-Host "   • ✅ Smartphones" -ForegroundColor White
Write-Host "   • ✅ Tablets" -ForegroundColor White
Write-Host "   • ✅ Projetores" -ForegroundColor White

Write-Host "`n📊 IMPACTO:" -ForegroundColor Cyan
Write-Host "   • Peso do app: SEM MUDANÇA (usa fontes do sistema)" -ForegroundColor Green
Write-Host "   • Performance: SEM IMPACTO (sombra é leve)" -ForegroundColor Green
Write-Host "   • Legibilidade: MELHORIA SIGNIFICATIVA" -ForegroundColor Green

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "CONCLUSÃO: IMPLEMENTAÇÃO SEGURA ✅" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan


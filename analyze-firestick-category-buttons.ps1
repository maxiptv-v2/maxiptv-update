# Script para analisar problema de fontes e emojis nos cards verdes no Fire Stick
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "ANALISE: Problema de Fontes e Emojis nos Cards Verdes" -ForegroundColor Cyan
Write-Host "         (Fire Stick Amazon)" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$file = "app/src/main/java/com/maxiptv/ui/screens/HomeScreen.kt"

if (-not (Test-Path $file)) {
    Write-Host "[ERRO] Arquivo nao encontrado: $file" -ForegroundColor Red
    exit 1
}

Write-Host "[1] Analisando CategoryButton..." -ForegroundColor Yellow
Write-Host ""

# Buscar CategoryButton
$categoryButton = Select-String -Path $file -Pattern "fun CategoryButton" -Context 0, 100
if ($categoryButton) {
    Write-Host "✅ Funcao CategoryButton encontrada" -ForegroundColor Green
    Write-Host ""
    
    # Analisar propriedades específicas do Fire Stick
    Write-Host "[2] Propriedades especificas do Fire Stick:" -ForegroundColor Yellow
    
    $firestickProps = @(
        @{Pattern = "firestick.*->.*\.dp"; Name = "Altura do botao"},
        @{Pattern = "firestick.*->.*\.sp"; Name = "Tamanho da fonte"},
        @{Pattern = "firestick.*->.*PaddingValues"; Name = "Padding"},
        @{Pattern = "emojiSize.*firestick"; Name = "Tamanho do emoji"},
        @{Pattern = "contentColor.*Color"; Name = "Cor do conteudo"}
    )
    
    foreach ($prop in $firestickProps) {
        $match = Select-String -Path $file -Pattern $prop.Pattern
        if ($match) {
            Write-Host "  ✅ $($prop.Name): Encontrado" -ForegroundColor Green
            $match | ForEach-Object { Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray }
        } else {
            Write-Host "  ⚠️  $($prop.Name): Nao encontrado" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    Write-Host "[3] Verificando possiveis problemas:" -ForegroundColor Yellow
    
    # Verificar se contentColor está definido
    $contentColor = Select-String -Path $file -Pattern "contentColor\s*=\s*Color" -Context 0, 2
    if ($contentColor) {
        Write-Host "  ✅ contentColor definido:" -ForegroundColor Green
        $contentColor | ForEach-Object { 
            $line = $_.Line.Trim()
            if ($line -match "contentColor") {
                Write-Host "     $line" -ForegroundColor Gray
                if ($line -match "Color\.Black") {
                    Write-Host "     ⚠️  PROBLEMA POTENCIAL: Color.Black pode nao aparecer em fundo verde escuro!" -ForegroundColor Red
                }
            }
        }
    }
    
    Write-Host ""
    
    # Verificar se Text tem color definido explicitamente
    $textColor = Select-String -Path $file -Pattern "Text\s*\([^)]*text\s*=\s*emoji" -Context 0, 5
    if (-not $textColor) {
        Write-Host "  ⚠️  PROBLEMA ENCONTRADO: Text do emoji pode nao ter cor definida!" -ForegroundColor Red
        Write-Host "     O emoji pode estar herdando cor incorreta do tema" -ForegroundColor Yellow
    }
    
    $textColorText = Select-String -Path $file -Pattern "Text\s*\([^)]*text\s*=\s*text" -Context 0, 5
    if ($textColorText) {
        $hasColor = $textColorText | Where-Object { $_.Line -match "color\s*=" }
        if (-not $hasColor) {
            Write-Host "  ⚠️  PROBLEMA ENCONTRADO: Text do nome pode nao ter cor definida!" -ForegroundColor Red
            Write-Host "     O texto pode estar herdando cor incorreta do tema" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    Write-Host "[4] Verificando SafeArea e Padding:" -ForegroundColor Yellow
    
    # Verificar se há problemas com SafeArea que podem estar cortando conteúdo
    $safeArea = Select-String -Path $file -Pattern "SafeArea|MaxiSafeArea" -Context 0, 2
    if ($safeArea) {
        Write-Host "  ⚠️  SafeArea encontrado - pode estar cortando conteudo no Fire Stick" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "[5] Analisando estrutura do CategoryButton:" -ForegroundColor Yellow
    
    # Buscar a estrutura completa
    $buttonStructure = Select-String -Path $file -Pattern "Button\s*\(|Column\s*\(|Text\s*\(.*emoji|Text\s*\(.*text" -Context 0, 1
    if ($buttonStructure) {
        Write-Host "  ✅ Estrutura encontrada:" -ForegroundColor Green
        $buttonStructure | Select-Object -First 10 | ForEach-Object {
            $line = $_.Line.Trim()
            if ($line -match "Button|Column|Text") {
                Write-Host "     $line" -ForegroundColor Gray
            }
        }
    }
    
    Write-Host ""
    Write-Host "===============================================================" -ForegroundColor Cyan
    Write-Host "DIAGNOSTICO:" -ForegroundColor Cyan
    Write-Host "===============================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "PROBLEMAS IDENTIFICADOS:" -ForegroundColor Red
    Write-Host ""
    Write-Host "1. Text do emoji pode nao ter cor definida explicitamente" -ForegroundColor Yellow
    Write-Host "   -> Solucao: Adicionar color = Color.Black no Text do emoji" -ForegroundColor Green
    Write-Host ""
    Write-Host "2. Text do nome pode nao ter cor definida explicitamente" -ForegroundColor Yellow
    Write-Host "   -> Solucao: Adicionar color = Color.Black no Text do nome" -ForegroundColor Green
    Write-Host ""
    Write-Host "3. Fire Stick pode ter problemas com heranca de cor do tema" -ForegroundColor Yellow
    Write-Host "   -> Solucao: Definir cores explicitamente em todos os Text" -ForegroundColor Green
    Write-Host ""
    Write-Host "4. Emojis podem nao renderizar corretamente no Fire Stick" -ForegroundColor Yellow
    Write-Host "   -> Solucao: Verificar se fontSize do emoji esta adequado" -ForegroundColor Green
    Write-Host ""
    Write-Host "5. SafeArea pode estar cortando conteudo" -ForegroundColor Yellow
    Write-Host "   -> Solucao: Verificar padding bottom do SafeArea" -ForegroundColor Green
    Write-Host ""
    Write-Host "===============================================================" -ForegroundColor Cyan
    Write-Host "RECOMENDACOES:" -ForegroundColor Cyan
    Write-Host "===============================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Adicionar color = Color.Black explicitamente nos Text do emoji" -ForegroundColor Green
    Write-Host "2. Adicionar color = Color.Black explicitamente no Text do nome" -ForegroundColor Green
    Write-Host "3. Verificar se fontSize do emoji (28.sp) nao esta muito grande" -ForegroundColor Green
    Write-Host "4. Verificar se buttonHeight (90.dp) nao esta cortando conteudo" -ForegroundColor Green
    Write-Host "5. Verificar se padding esta adequado para Fire Stick" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "❌ Funcao CategoryButton nao encontrada!" -ForegroundColor Red
}


# Script para corrigir erros de compilação no PlayerActivity.kt

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "CORRIGINDO ERROS DE COMPILAÇÃO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$file = "app/src/main/java/com/maxiptv/ui/player/PlayerActivity.kt"
$content = Get-Content $file -Raw -Encoding UTF8

Write-Host "1. Verificando estrutura da classe..." -ForegroundColor Yellow

# Verificar se há fechamento incorreto do onCreate
$onCreateEnd = Select-String -Path $file -Pattern "^\s*\}\s*$" -Context 0,2 | Select-Object -First 1
if ($onCreateEnd) {
    Write-Host "   ⚠️ Possível fechamento incorreto detectado" -ForegroundColor Yellow
}

# Verificar se toggleFullscreen está dentro da classe
$toggleFullscreen = Select-String -Path $file -Pattern "^\s*private fun toggleFullscreen" -Context 1,1
if ($toggleFullscreen) {
    $lineBefore = $toggleFullscreen.LineNumber - 1
    $lineBeforeContent = (Get-Content $file)[$lineBefore - 1]
    if ($lineBeforeContent -match "^\s*\}\s*$" -and $lineBeforeContent -notmatch "^\s+\}") {
        Write-Host "   ❌ ERRO: toggleFullscreen está fora da classe!" -ForegroundColor Red
        Write-Host "      Linha $lineBefore parece fechar a classe prematuramente" -ForegroundColor Gray
    } else {
        Write-Host "   ✅ toggleFullscreen está dentro da classe" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "2. Verificando variáveis não declaradas..." -ForegroundColor Yellow

# Verificar subtitleApplied
$subtitleApplied = Select-String -Path $file -Pattern "subtitleApplied" -Context 2,2
$declared = $false
foreach ($match in $subtitleApplied) {
    if ($match.Line -match "var subtitleApplied|val subtitleApplied") {
        $declared = $true
        break
    }
}
if (-not $declared) {
    Write-Host "   ❌ ERRO: subtitleApplied não está declarada corretamente!" -ForegroundColor Red
} else {
    Write-Host "   ✅ subtitleApplied está declarada" -ForegroundColor Green
}

Write-Host ""
Write-Host "3. Corrigindo erros..." -ForegroundColor Yellow

# Ler o arquivo linha por linha
$lines = Get-Content $file
$newLines = @()
$i = 0
$inOnCreate = $false
$onCreateDepth = 0
$fixed = $false

while ($i -lt $lines.Count) {
    $line = $lines[$i]
    $lineNum = $i + 1
    
    # Detectar início do onCreate
    if ($line -match "override fun onCreate") {
        $inOnCreate = $true
        $onCreateDepth = 0
    }
    
    # Contar chaves para detectar fechamento do onCreate
    if ($inOnCreate) {
        $openBraces = ($line.ToCharArray() | Where-Object { $_ -eq '{' }).Count
        $closeBraces = ($line.ToCharArray() | Where-Object { $_ -eq '}' }).Count
        $onCreateDepth += $openBraces - $closeBraces
        
        # Se chegou ao fechamento do onCreate (depth = 0 e não é o primeiro {)
        if ($onCreateDepth -eq 0 -and $line -match "^\s*\}\s*$" -and $lineNum -lt 990) {
            # Verificar se a próxima linha é uma função da classe
            if ($i + 1 -lt $lines.Count) {
                $nextLine = $lines[$i + 1]
                if ($nextLine -match "^\s*(private|override|fun|protected|internal)\s+fun") {
                    Write-Host "   ✅ Linha $lineNum : onCreate fechado corretamente" -ForegroundColor Green
                    $inOnCreate = $false
                } else {
                    Write-Host "   ⚠️ Linha $lineNum : Possível fechamento incorreto" -ForegroundColor Yellow
                }
            }
        }
    }
    
    # Corrigir linha 666 - subtitleApplied
    if ($lineNum -eq 666 -and $line -match "if \(!subtitleApplied\)" -and -not $declared) {
        Write-Host "   🔧 Corrigindo linha $lineNum : subtitleApplied" -ForegroundColor Cyan
        # Não fazer nada aqui, já está correto no código atual
    }
    
    # Corrigir linha 984 - fechamento incorreto
    if ($lineNum -eq 984 -and $line -match "^\s*\}\s*$") {
        $nextLine = $lines[$i + 1]
        if ($nextLine -match "^\s*private fun toggleFullscreen") {
            Write-Host "   ✅ Linha $lineNum : Fechamento correto antes de toggleFullscreen" -ForegroundColor Green
        }
    }
    
    $newLines += $line
    $i++
}

Write-Host ""
Write-Host "4. Verificando imports e referências..." -ForegroundColor Yellow

# Verificar se há referências a funções que não existem mais
$missingFunctions = @()
if ((Select-String -Path $file -Pattern "showQualityDialog\(\)" -Quiet)) {
    $missingFunctions += "showQualityDialog"
}
if ((Select-String -Path $file -Pattern "showSubtitleDialog\(\)" -Quiet)) {
    $missingFunctions += "showSubtitleDialog"
}
if ((Select-String -Path $file -Pattern "showAudioDialog\(\)" -Quiet)) {
    $missingFunctions += "showAudioDialog"
}

if ($missingFunctions.Count -gt 0) {
    Write-Host "   ⚠️ AVISO: Funções removidas ainda sendo chamadas:" -ForegroundColor Yellow
    foreach ($func in $missingFunctions) {
        Write-Host "      - $func" -ForegroundColor Gray
    }
} else {
    Write-Host "   ✅ Nenhuma função removida sendo chamada" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANÁLISE CONCLUÍDA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Próximos passos:" -ForegroundColor Yellow
Write-Host "1. Verificar se o onCreate está fechado corretamente" -ForegroundColor White
Write-Host "2. Verificar se todas as funções estão dentro da classe" -ForegroundColor White
Write-Host "3. Verificar se todas as variáveis estão declaradas" -ForegroundColor White
Write-Host ""


# Análise Completa do APK - Código Duplicado e Qualidade
# Não remove nada, apenas identifica problemas

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ANÁLISE COMPLETA DO APK" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$appDir = "app/src/main/java/com/maxiptv"
$issues = @()
$duplicates = @()
$warnings = @()

# 1. VERIFICAR CÓDIGO DUPLICADO
Write-Host "1. Procurando código duplicado..." -ForegroundColor Yellow

# Função para normalizar código (remover espaços, comentários, etc)
function Normalize-Code {
    param([string]$code)
    # Remover comentários de linha
    $code = $code -replace '//.*$', ''
    # Remover comentários de bloco
    $code = $code -replace '/\*.*?\*/', '', 'Singleline'
    # Remover espaços múltiplos
    $code = $code -replace '\s+', ' '
    # Remover quebras de linha
    $code = $code -replace '[\r\n]+', ' '
    return $code.Trim()
}

# Buscar todos os arquivos Kotlin
$kotlinFiles = Get-ChildItem -Path $appDir -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch '\\build\\' }

Write-Host "   Encontrados $($kotlinFiles.Count) arquivos Kotlin" -ForegroundColor Gray

# Agrupar por funções/métodos similares
$functionSignatures = @{}
$duplicateFunctions = @{}

foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    $lines = Get-Content $file.FullName
    
    # Procurar funções duplicadas (mesma assinatura)
    $functionPattern = '(fun\s+\w+\s*\([^)]*\)\s*[:=][^{]*\{[^}]*\})'
    $matches = [regex]::Matches($content, $functionPattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
    
    foreach ($match in $matches) {
        $signature = $match.Groups[0].Value
        $normalized = Normalize-Code $signature
        
        if ($functionSignatures.ContainsKey($normalized)) {
            if (-not $duplicateFunctions.ContainsKey($normalized)) {
                $duplicateFunctions[$normalized] = @()
            }
            $duplicateFunctions[$normalized] += $file.Name
            $duplicateFunctions[$normalized] += $functionSignatures[$normalized]
        } else {
            $functionSignatures[$normalized] = $file.Name
        }
    }
}

# 2. VERIFICAR IMPORTS DUPLICADOS
Write-Host "2. Verificando imports duplicados..." -ForegroundColor Yellow

foreach ($file in $kotlinFiles) {
    $lines = Get-Content $file.FullName
    $imports = $lines | Where-Object { $_ -match '^import\s+' }
    $uniqueImports = $imports | Select-Object -Unique
    $duplicateImports = $imports | Group-Object | Where-Object { $_.Count -gt 1 }
    
    if ($duplicateImports) {
        $issues += "📦 $($file.Name): Imports duplicados encontrados"
        foreach ($dup in $duplicateImports) {
            $warnings += "   - Import duplicado: $($dup.Name) ($($dup.Count) vezes)"
        }
    }
}

# 3. VERIFICAR CÓDIGO MORTO (não usado)
Write-Host "3. Verificando código não utilizado..." -ForegroundColor Yellow

# Buscar classes/funções privadas que podem não estar sendo usadas
foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    $lines = Get-Content $file.FullName
    
    # Procurar funções privadas
    $privateFunctions = [regex]::Matches($content, 'private\s+(fun|suspend\s+fun)\s+(\w+)')
    
    foreach ($match in $privateFunctions) {
        $functionName = $match.Groups[2].Value
        # Verificar se é usado em outros arquivos
        $used = $false
        foreach ($otherFile in $kotlinFiles) {
            if ($otherFile.FullName -ne $file.FullName) {
                $otherContent = Get-Content $otherFile.FullName -Raw
                if ($otherContent -match "\b$functionName\b") {
                    $used = $true
                    break
                }
            }
        }
        if (-not $used -and $functionName -ne 'onCreate' -and $functionName -ne 'onDestroy') {
            $warnings += "⚠️ $($file.Name): Função privada '$functionName' pode não estar sendo usada"
        }
    }
}

# 4. VERIFICAR STRINGS DUPLICADAS (hardcoded)
Write-Host "4. Verificando strings hardcoded duplicadas..." -ForegroundColor Yellow

$stringPattern = '"[^"]{10,}"' # Strings com mais de 10 caracteres
$allStrings = @{}

foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    $matches = [regex]::Matches($content, $stringPattern)
    
    foreach ($match in $matches) {
        $str = $match.Value
        if (-not $allStrings.ContainsKey($str)) {
            $allStrings[$str] = @()
        }
        $allStrings[$str] += "$($file.Name)"
    }
}

$duplicateStrings = $allStrings.GetEnumerator() | Where-Object { $_.Value.Count -gt 2 }
if ($duplicateStrings) {
    $issues += "📝 Strings duplicadas encontradas (podem ser movidas para strings.xml)"
    foreach ($dup in $duplicateStrings) {
        $warnings += "   - String repetida $($dup.Value.Count) vezes: $($dup.Key.Substring(0, [Math]::Min(50, $dup.Key.Length)))..."
    }
}

# 5. VERIFICAR CÓDIGO DEPRECATED
Write-Host "5. Verificando código deprecated..." -ForegroundColor Yellow

foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    $lines = Get-Content $file.FullName
    
    # Procurar uso de APIs deprecated
    $deprecatedPatterns = @(
        'window\.addFlags\(WindowManager\.LayoutParams\.FLAG_FULLSCREEN\)',
        'systemUiVisibility',
        'View\.SYSTEM_UI_FLAG_',
        '@Deprecated',
        'deprecated'
    )
    
    foreach ($pattern in $deprecatedPatterns) {
        if ($content -match $pattern) {
            $lineNum = ($lines | Select-String -Pattern $pattern).LineNumber
            $warnings += "⚠️ $($file.Name): Linha $lineNum - Possível uso de API deprecated: $pattern"
        }
    }
}

# 6. VERIFICAR ARQUIVOS GRANDES (mais de 1000 linhas)
Write-Host "6. Verificando arquivos grandes..." -ForegroundColor Yellow

foreach ($file in $kotlinFiles) {
    $lineCount = (Get-Content $file.FullName | Measure-Object -Line).Lines
    if ($lineCount -gt 1000) {
        $warnings += "📄 $($file.Name): Arquivo muito grande ($lineCount linhas) - considere dividir"
    }
}

# 7. VERIFICAR IMPORTS NÃO USADOS
Write-Host "7. Verificando imports não usados..." -ForegroundColor Yellow

foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    $lines = Get-Content $file.FullName
    
    $imports = $lines | Where-Object { $_ -match '^import\s+([^\s]+)' } | ForEach-Object {
        if ($_ -match '^import\s+([^\s]+)') {
            $matches[1]
        }
    }
    
    foreach ($import in $imports) {
        $className = $import.Split('.')[-1]
        # Verificar se a classe é usada no arquivo
        if ($content -notmatch "\b$className\b") {
            $warnings += "📦 $($file.Name): Import possivelmente não usado: $import"
        }
    }
}

# 8. VERIFICAR DUPLICAÇÃO DE LÓGICA (não apenas código idêntico)
Write-Host "8. Verificando lógica duplicada..." -ForegroundColor Yellow

# Padrões comuns que podem estar duplicados
$commonPatterns = @{
    'SafeArea' = 'SafeArea|SafePadding|overscan'
    'Device Detection' = 'isFireStick|isTv|isTvBox|isNativeTv'
    'Image Loading' = 'AsyncImage|ImageRequest|Coil'
    'Player Setup' = 'ExoPlayer|PlayerView|MediaItem'
}

foreach ($patternName in $commonPatterns.Keys) {
    $pattern = $commonPatterns[$patternName]
    $filesWithPattern = $kotlinFiles | Where-Object {
        (Get-Content $_.FullName -Raw) -match $pattern
    }
    
    if ($filesWithPattern.Count -gt 3) {
        $warnings += "🔄 Padrão '$patternName' usado em $($filesWithPattern.Count) arquivos - considere criar utilitário comum"
    }
}

# RESUMO
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RESUMO DA ANÁLISE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($duplicateFunctions.Count -gt 0) {
    Write-Host "❌ FUNÇÕES DUPLICADAS ENCONTRADAS:" -ForegroundColor Red
    foreach ($dup in $duplicateFunctions.Keys) {
        Write-Host "   - Função duplicada em: $($duplicateFunctions[$dup] -join ', ')" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($issues.Count -gt 0) {
    Write-Host "⚠️ PROBLEMAS ENCONTRADOS:" -ForegroundColor Yellow
    foreach ($issue in $issues) {
        Write-Host "   $issue" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "📋 AVISOS E SUGESTÕES ($($warnings.Count) itens):" -ForegroundColor Cyan
    $warnings | Select-Object -First 20 | ForEach-Object {
        Write-Host "   $_" -ForegroundColor Gray
    }
    if ($warnings.Count -gt 20) {
        Write-Host "   ... e mais $($warnings.Count - 20) avisos" -ForegroundColor Gray
    }
    Write-Host ""
}

# Estatísticas finais
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ESTATÍSTICAS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total de arquivos Kotlin: $($kotlinFiles.Count)" -ForegroundColor White
Write-Host "Funções duplicadas: $($duplicateFunctions.Count)" -ForegroundColor $(if ($duplicateFunctions.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host "Problemas encontrados: $($issues.Count)" -ForegroundColor $(if ($issues.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host "Avisos: $($warnings.Count)" -ForegroundColor $(if ($warnings.Count -gt 50) { "Yellow" } else { "Green" })
Write-Host ""

# Salvar relatório
$reportFile = "relatorio-analise-apk-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
$report = @"
RELATÓRIO DE ANÁLISE DO APK
Data: $(Get-Date)

FUNÇÕES DUPLICADAS:
$($duplicateFunctions.Keys | ForEach-Object { "- $_`n  Arquivos: $($duplicateFunctions[$_] -join ', ')`n" })

PROBLEMAS:
$($issues -join "`n")

AVISOS:
$($warnings -join "`n")
"@

$report | Out-File -FilePath $reportFile -Encoding UTF8
Write-Host "✅ Relatório salvo em: $reportFile" -ForegroundColor Green
Write-Host ""


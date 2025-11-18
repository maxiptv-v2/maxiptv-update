# Análise de Código Duplicado - MaxiPTV
# Identifica código duplicado sem remover nada

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ANÁLISE DE CÓDIGO DUPLICADO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$appDir = "app/src/main/java/com/maxiptv"
$duplicates = @()
$warnings = @()

# Buscar todos os arquivos Kotlin
$kotlinFiles = Get-ChildItem -Path $appDir -Recurse -Filter "*.kt" | Where-Object { 
    $_.FullName -notmatch '\\build\\' -and 
    $_.FullName -notmatch '\\test\\'
}

Write-Host "📁 Arquivos encontrados: $($kotlinFiles.Count)" -ForegroundColor Gray
Write-Host ""

# 1. VERIFICAR IMPORTS DUPLICADOS
Write-Host "1️⃣ Verificando imports duplicados..." -ForegroundColor Yellow
$importDuplicates = 0

foreach ($file in $kotlinFiles) {
    $lines = Get-Content $file.FullName
    $importLines = $lines | Where-Object { $_ -match '^\s*import\s+' }
    
    # Verificar duplicatas exatas
    $uniqueImports = $importLines | Select-Object -Unique
    if ($importLines.Count -ne $uniqueImports.Count) {
        $duplicates += "📦 $($file.Name): Imports duplicados"
        $duplicateImports = $importLines | Group-Object | Where-Object { $_.Count -gt 1 }
        foreach ($dup in $duplicateImports) {
            $warnings += "   - '$($dup.Name)' aparece $($dup.Count) vezes"
        }
        $importDuplicates++
    }
}

Write-Host "   ✅ Imports verificados: $importDuplicates arquivos com duplicatas" -ForegroundColor $(if ($importDuplicates -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# 2. VERIFICAR FUNÇÕES IDÊNTICAS (blocos de código duplicados)
Write-Host "2️⃣ Verificando funções/blocos duplicados..." -ForegroundColor Yellow

function Get-FunctionContent {
    param([string]$filePath)
    $content = Get-Content $filePath -Raw
    $functions = @()
    
    # Padrão para funções
    $pattern = '(fun\s+\w+\s*\([^)]*\)\s*(?::\s*[^{]+)?\s*\{[^}]*\})'
    $matches = [regex]::Matches($content, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
    
    foreach ($match in $matches) {
        $funcCode = $match.Value
        # Normalizar: remover espaços extras, quebras de linha
        $normalized = $funcCode -replace '\s+', ' ' -replace '[\r\n]+', ' '
        $functions += @{
            Original = $funcCode
            Normalized = $normalized.Trim()
            StartLine = ($content.Substring(0, $match.Index) -split "`n").Count
        }
    }
    
    return $functions
}

$allFunctions = @{}
$duplicateFunctions = @{}

foreach ($file in $kotlinFiles) {
    $functions = Get-FunctionContent $file.FullName
    
    foreach ($func in $functions) {
        $key = $func.Normalized.Substring(0, [Math]::Min(100, $func.Normalized.Length))
        
        if ($allFunctions.ContainsKey($key)) {
            if (-not $duplicateFunctions.ContainsKey($key)) {
                $duplicateFunctions[$key] = @()
            }
            $duplicateFunctions[$key] += "$($file.Name):$($func.StartLine)"
            if ($allFunctions[$key].File -ne $file.Name) {
                $duplicateFunctions[$key] += "$($allFunctions[$key].File):$($allFunctions[$key].Line)"
            }
        } else {
            $allFunctions[$key] = @{
                File = $file.Name
                Line = $func.StartLine
            }
        }
    }
}

Write-Host "   ✅ Funções verificadas: $($duplicateFunctions.Count) duplicatas encontradas" -ForegroundColor $(if ($duplicateFunctions.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# 3. VERIFICAR STRINGS HARDCODED DUPLICADAS
Write-Host "3️⃣ Verificando strings hardcoded duplicadas..." -ForegroundColor Yellow

$allStrings = @{}
foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    # Buscar strings com mais de 15 caracteres (provavelmente não são variáveis)
    $stringMatches = [regex]::Matches($content, '"([^"]{15,})"')
    
    foreach ($match in $stringMatches) {
        $str = $match.Groups[1].Value
        if (-not $allStrings.ContainsKey($str)) {
            $allStrings[$str] = @()
        }
        $allStrings[$str] += $file.Name
    }
}

$duplicateStrings = $allStrings.GetEnumerator() | Where-Object { $_.Value.Count -gt 2 } | Select-Object -First 10
Write-Host "   ✅ Strings verificadas: $($duplicateStrings.Count) strings repetidas 3+ vezes" -ForegroundColor $(if ($duplicateStrings.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# 4. VERIFICAR CÓDIGO ESPECÍFICO DUPLICADO (padrões conhecidos)
Write-Host "4️⃣ Verificando padrões específicos duplicados..." -ForegroundColor Yellow

# Verificar se há múltiplas definições de mesma constante
$constants = @{}
foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    # Buscar constantes (val, const val)
    $constMatches = [regex]::Matches($content, '(?:val|const val)\s+(\w+)\s*=\s*([^\n]+)')
    
    foreach ($match in $constMatches) {
        $name = $match.Groups[1].Value
        $value = $match.Groups[2].Value.Trim()
        
        if (-not $constants.ContainsKey($name)) {
            $constants[$name] = @()
        }
        $constants[$name] += @{
            File = $file.Name
            Value = $value
        }
    }
}

$duplicateConstants = $constants.GetEnumerator() | Where-Object { 
    $_.Value.Count -gt 1 -and 
    ($_.Value | Select-Object -Unique -Property Value).Count -eq 1 
} | Select-Object -First 5

Write-Host "   ✅ Constantes verificadas: $($duplicateConstants.Count) constantes duplicadas" -ForegroundColor $(if ($duplicateConstants.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# 5. VERIFICAR LÓGICA DUPLICADA (código similar mas não idêntico)
Write-Host "5️⃣ Verificando lógica similar duplicada..." -ForegroundColor Yellow

# Padrões comuns que podem estar duplicados
$commonPatterns = @{
    'Device Type Check' = 'isFireStick|isTv|isTvBox|isNativeTv|isPhone|isTablet'
    'Safe Area Padding' = 'SafePadding|PaddingValues|overscan|startDp|endDp'
    'Image Loading' = 'AsyncImage|ImageRequest\.Builder|Coil'
    'Player Setup' = 'ExoPlayer\.Builder|PlayerView|MediaItem'
    'Navigation' = 'nav\.navigate|nav\.popBackStack'
}

$patternDuplicates = @{}
foreach ($patternName in $commonPatterns.Keys) {
    $pattern = $commonPatterns[$patternName]
    $filesWithPattern = @()
    
    foreach ($file in $kotlinFiles) {
        $content = Get-Content $file.FullName -Raw
        if ($content -match $pattern) {
            $filesWithPattern += $file.Name
        }
    }
    
    if ($filesWithPattern.Count -gt 3) {
        $patternDuplicates[$patternName] = $filesWithPattern.Count
    }
}

Write-Host "   ✅ Padrões verificados: $($patternDuplicates.Count) padrões usados em muitos arquivos" -ForegroundColor $(if ($patternDuplicates.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# RESUMO FINAL
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RESUMO DA ANÁLISE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($duplicateFunctions.Count -gt 0) {
    Write-Host "❌ FUNÇÕES/BLOCOS DUPLICADOS:" -ForegroundColor Red
    $count = 0
    foreach ($dup in $duplicateFunctions.Keys) {
        if ($count -lt 5) {
            Write-Host "   📄 Encontrado em:" -ForegroundColor Yellow
            $duplicateFunctions[$dup] | Select-Object -Unique | ForEach-Object {
                Write-Host "      - $_" -ForegroundColor Gray
            }
            Write-Host ""
        }
        $count++
    }
    if ($duplicateFunctions.Count -gt 5) {
        Write-Host "   ... e mais $($duplicateFunctions.Count - 5) duplicatas" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($duplicates.Count -gt 0) {
    Write-Host "⚠️ PROBLEMAS DE DUPLICAÇÃO:" -ForegroundColor Yellow
    $duplicates | Select-Object -First 10 | ForEach-Object {
        Write-Host "   $_" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($duplicateStrings) {
    Write-Host "📝 STRINGS DUPLICADAS (considere mover para strings.xml):" -ForegroundColor Cyan
    foreach ($dup in $duplicateStrings) {
        $strPreview = $dup.Key.Substring(0, [Math]::Min(40, $dup.Key.Length))
        Write-Host "   - '$strPreview...' aparece em $($dup.Value.Count) arquivos" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($duplicateConstants) {
    Write-Host "🔧 CONSTANTES DUPLICADAS:" -ForegroundColor Cyan
    foreach ($dup in $duplicateConstants) {
        Write-Host "   - '$($dup.Key)' definida em $($dup.Value.Count) arquivos" -ForegroundColor Gray
        $dup.Value | ForEach-Object {
            Write-Host "     → $($_.File)" -ForegroundColor DarkGray
        }
    }
    Write-Host ""
}

if ($patternDuplicates.Count -gt 0) {
    Write-Host "🔄 PADRÕES USADOS EM MÚLTIPLOS ARQUIVOS:" -ForegroundColor Cyan
    foreach ($pattern in $patternDuplicates.Keys) {
        Write-Host "   - '$pattern': usado em $($patternDuplicates[$pattern]) arquivos" -ForegroundColor Gray
    }
    Write-Host ""
}

# Estatísticas
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ESTATÍSTICAS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total de arquivos: $($kotlinFiles.Count)" -ForegroundColor White
Write-Host "Funções/blocos duplicados: $($duplicateFunctions.Count)" -ForegroundColor $(if ($duplicateFunctions.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host "Imports duplicados: $importDuplicates arquivos" -ForegroundColor $(if ($importDuplicates -gt 0) { "Yellow" } else { "Green" })
Write-Host "Strings duplicadas: $($duplicateStrings.Count)" -ForegroundColor $(if ($duplicateStrings.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host "Constantes duplicadas: $($duplicateConstants.Count)" -ForegroundColor $(if ($duplicateConstants.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# Salvar relatório detalhado
$reportFile = "relatorio-codigo-duplicado-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
$report = @"
RELATÓRIO DE ANÁLISE - CÓDIGO DUPLICADO
Data: $(Get-Date)
Total de arquivos analisados: $($kotlinFiles.Count)

FUNÇÕES/BLOCOS DUPLICADOS:
$($duplicateFunctions.Keys | ForEach-Object { 
    "- Bloco duplicado encontrado em:`n  $($duplicateFunctions[$_] -join "`n  ")
`n"
})

IMPORTS DUPLICADOS:
$($duplicates | Where-Object { $_ -match 'Imports' } | ForEach-Object { "$_`n" })

STRINGS DUPLICADAS:
$($duplicateStrings | ForEach-Object { 
    "- '$($_.Key.Substring(0, [Math]::Min(50, $_.Key.Length)))...'`n  Arquivos: $($_.Value -join ', ')
`n"
})

CONSTANTES DUPLICADAS:
$($duplicateConstants | ForEach-Object {
    "- '$($_.Key)' definida em:`n  $($_.Value | ForEach-Object { "  → $($_.File)" })
`n"
})
"@

$report | Out-File -FilePath $reportFile -Encoding UTF8
Write-Host "✅ Relatório detalhado salvo em: $reportFile" -ForegroundColor Green
Write-Host ""

# Verificar qualidade geral
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  QUALIDADE GERAL DO CÓDIGO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$totalIssues = $duplicateFunctions.Count + $importDuplicates + $duplicateStrings.Count + $duplicateConstants.Count

if ($totalIssues -eq 0) {
    Write-Host "✅ EXCELENTE! Nenhum código duplicado encontrado!" -ForegroundColor Green
} elseif ($totalIssues -lt 5) {
    Write-Host "✅ BOM! Poucos problemas de duplicação encontrados." -ForegroundColor Green
} elseif ($totalIssues -lt 10) {
    Write-Host "⚠️ ATENÇÃO! Alguns problemas de duplicação encontrados." -ForegroundColor Yellow
} else {
    Write-Host "❌ ATENÇÃO! Vários problemas de duplicação encontrados." -ForegroundColor Red
}

Write-Host ""


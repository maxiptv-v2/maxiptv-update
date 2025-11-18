# Analise de Codigo Duplicado - MaxiPTV
# Identifica codigo duplicado sem remover nada

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ANALISE DE CODIGO DUPLICADO" -ForegroundColor Cyan
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

Write-Host "Arquivos encontrados: $($kotlinFiles.Count)" -ForegroundColor Gray
Write-Host ""

# 1. VERIFICAR IMPORTS DUPLICADOS
Write-Host "1. Verificando imports duplicados..." -ForegroundColor Yellow
$importDuplicates = 0

foreach ($file in $kotlinFiles) {
    $lines = Get-Content $file.FullName
    $importLines = $lines | Where-Object { $_ -match '^\s*import\s+' }
    
    # Verificar duplicatas exatas
    $uniqueImports = $importLines | Select-Object -Unique
    if ($importLines.Count -ne $uniqueImports.Count) {
        $duplicates += "[IMPORTS] $($file.Name): Imports duplicados"
        $duplicateImports = $importLines | Group-Object | Where-Object { $_.Count -gt 1 }
        foreach ($dup in $duplicateImports) {
            $warnings += "   - '$($dup.Name)' aparece $($dup.Count) vezes"
        }
        $importDuplicates++
    }
}

Write-Host "   Imports verificados: $importDuplicates arquivos com duplicatas" -ForegroundColor $(if ($importDuplicates -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# 2. VERIFICAR STRINGS HARDCODED DUPLICADAS
Write-Host "2. Verificando strings hardcoded duplicadas..." -ForegroundColor Yellow

$allStrings = @{}
foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    # Buscar strings com mais de 15 caracteres
    $stringPattern = '"[^"]{15,}"'
    $stringMatches = [regex]::Matches($content, $stringPattern)
    
    foreach ($match in $stringMatches) {
        $str = $match.Value
        if (-not $allStrings.ContainsKey($str)) {
            $allStrings[$str] = @()
        }
        $allStrings[$str] += $file.Name
    }
}

$duplicateStrings = $allStrings.GetEnumerator() | Where-Object { $_.Value.Count -gt 2 } | Select-Object -First 10
Write-Host "   Strings verificadas: $($duplicateStrings.Count) strings repetidas 3+ vezes" -ForegroundColor $(if ($duplicateStrings.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# 3. VERIFICAR CONSTANTES DUPLICADAS
Write-Host "3. Verificando constantes duplicadas..." -ForegroundColor Yellow

$constants = @{}
foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    # Buscar constantes (val, const val)
    $constPattern = '(?:val|const val)\s+(\w+)\s*=\s*([^\n\r]+)'
    $constMatches = [regex]::Matches($content, $constPattern)
    
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
} | Select-Object -First 10

Write-Host "   Constantes verificadas: $($duplicateConstants.Count) constantes duplicadas" -ForegroundColor $(if ($duplicateConstants.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# 4. VERIFICAR FUNCOES SIMILARES (mesma assinatura)
Write-Host "4. Verificando funcoes com assinaturas similares..." -ForegroundColor Yellow

$functionSignatures = @{}
$duplicateSignatures = @{}

foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    # Buscar assinaturas de funcoes
    $funcPattern = 'fun\s+(\w+)\s*\([^)]*\)'
    $funcMatches = [regex]::Matches($content, $funcPattern)
    
    foreach ($match in $funcMatches) {
        $signature = $match.Value
        $funcName = $match.Groups[1].Value
        
        if ($functionSignatures.ContainsKey($signature)) {
            if (-not $duplicateSignatures.ContainsKey($signature)) {
                $duplicateSignatures[$signature] = @()
            }
            $duplicateSignatures[$signature] += "$($file.Name)"
            if ($functionSignatures[$signature] -ne $file.Name) {
                $duplicateSignatures[$signature] += $functionSignatures[$signature]
            }
        } else {
            $functionSignatures[$signature] = $file.Name
        }
    }
}

Write-Host "   Funcoes verificadas: $($duplicateSignatures.Count) assinaturas duplicadas" -ForegroundColor $(if ($duplicateSignatures.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# 5. VERIFICAR PADROES COMUNS DUPLICADOS
Write-Host "5. Verificando padroes comuns duplicados..." -ForegroundColor Yellow

$commonPatterns = @{
    'Device Type Check' = 'isFireStick|isTv|isTvBox|isNativeTv|isPhone|isTablet'
    'Safe Area Padding' = 'SafePadding|PaddingValues|overscan|startDp|endDp'
    'Image Loading' = 'AsyncImage|ImageRequest\.Builder|Coil'
    'Player Setup' = 'ExoPlayer\.Builder|PlayerView|MediaItem'
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

Write-Host "   Padroes verificados: $($patternDuplicates.Count) padroes usados em muitos arquivos" -ForegroundColor $(if ($patternDuplicates.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# RESUMO FINAL
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RESUMO DA ANALISE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($duplicateSignatures.Count -gt 0) {
    Write-Host "FUNCOES COM ASSINATURAS DUPLICADAS:" -ForegroundColor Red
    $count = 0
    foreach ($dup in $duplicateSignatures.Keys) {
        if ($count -lt 5) {
            Write-Host "   [$dup]" -ForegroundColor Yellow
            $duplicateSignatures[$dup] | Select-Object -Unique | ForEach-Object {
                Write-Host "      - $_" -ForegroundColor Gray
            }
            Write-Host ""
        }
        $count++
    }
    if ($duplicateSignatures.Count -gt 5) {
        Write-Host "   ... e mais $($duplicateSignatures.Count - 5) duplicatas" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($duplicates.Count -gt 0) {
    Write-Host "PROBLEMAS DE DUPLICACAO:" -ForegroundColor Yellow
    $duplicates | Select-Object -First 10 | ForEach-Object {
        Write-Host "   $_" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($duplicateStrings) {
    Write-Host "STRINGS DUPLICADAS (considere mover para strings.xml):" -ForegroundColor Cyan
    foreach ($dup in $duplicateStrings) {
        $strPreview = $dup.Key.Substring(0, [Math]::Min(40, $dup.Key.Length))
        Write-Host "   - '$strPreview...' aparece em $($dup.Value.Count) arquivos" -ForegroundColor Gray
        $dup.Value | Select-Object -First 3 | ForEach-Object {
            Write-Host "     → $_" -ForegroundColor DarkGray
        }
        Write-Host ""
    }
    Write-Host ""
}

if ($duplicateConstants) {
    Write-Host "CONSTANTES DUPLICADAS:" -ForegroundColor Cyan
    foreach ($dup in $duplicateConstants) {
        Write-Host "   - '$($dup.Key)' definida em $($dup.Value.Count) arquivos" -ForegroundColor Gray
        $dup.Value | ForEach-Object {
            Write-Host "     → $($_.File)" -ForegroundColor DarkGray
        }
        Write-Host ""
    }
    Write-Host ""
}

if ($patternDuplicates.Count -gt 0) {
    Write-Host "PADROES USADOS EM MULTIPLOS ARQUIVOS:" -ForegroundColor Cyan
    foreach ($pattern in $patternDuplicates.Keys) {
        Write-Host "   - '$pattern': usado em $($patternDuplicates[$pattern]) arquivos" -ForegroundColor Gray
    }
    Write-Host ""
}

# Estatisticas
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  ESTATISTICAS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total de arquivos: $($kotlinFiles.Count)" -ForegroundColor White
Write-Host "Funcoes com assinaturas duplicadas: $($duplicateSignatures.Count)" -ForegroundColor $(if ($duplicateSignatures.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host "Imports duplicados: $importDuplicates arquivos" -ForegroundColor $(if ($importDuplicates -gt 0) { "Yellow" } else { "Green" })
Write-Host "Strings duplicadas: $($duplicateStrings.Count)" -ForegroundColor $(if ($duplicateStrings.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host "Constantes duplicadas: $($duplicateConstants.Count)" -ForegroundColor $(if ($duplicateConstants.Count -gt 0) { "Yellow" } else { "Green" })
Write-Host ""

# Salvar relatorio
$reportFile = "relatorio-duplicados-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
$report = "RELATORIO DE ANALISE - CODIGO DUPLICADO`n"
$report += "Data: $(Get-Date)`n"
$report += "Total de arquivos analisados: $($kotlinFiles.Count)`n`n"

$report += "FUNCOES COM ASSINATURAS DUPLICADAS:`n"
foreach ($dup in $duplicateSignatures.Keys) {
    $report += "- $dup`n"
    $report += "  Arquivos: $($duplicateSignatures[$dup] -join ', ')`n`n"
}

$report += "IMPORTS DUPLICADOS:`n"
$duplicates | Where-Object { $_ -match 'IMPORTS' } | ForEach-Object { $report += "$_`n" }
$report += "`n"

$report += "STRINGS DUPLICADAS:`n"
foreach ($dup in $duplicateStrings) {
    $strPreview = $dup.Key.Substring(0, [Math]::Min(50, $dup.Key.Length))
    $report += "- '$strPreview...'`n"
    $report += "  Arquivos: $($dup.Value -join ', ')`n`n"
}

$report += "CONSTANTES DUPLICADAS:`n"
foreach ($dup in $duplicateConstants) {
    $report += "- '$($dup.Key)' definida em:`n"
    $dup.Value | ForEach-Object { $report += "  → $($_.File)`n" }
    $report += "`n"
}

$report | Out-File -FilePath $reportFile -Encoding UTF8
Write-Host "Relatorio detalhado salvo em: $reportFile" -ForegroundColor Green
Write-Host ""

# Verificar qualidade geral
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  QUALIDADE GERAL DO CODIGO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$totalIssues = $duplicateSignatures.Count + $importDuplicates + $duplicateStrings.Count + $duplicateConstants.Count

if ($totalIssues -eq 0) {
    Write-Host "EXCELENTE! Nenhum codigo duplicado encontrado!" -ForegroundColor Green
} elseif ($totalIssues -lt 5) {
    Write-Host "BOM! Poucos problemas de duplicacao encontrados." -ForegroundColor Green
} elseif ($totalIssues -lt 10) {
    Write-Host "ATENCAO! Alguns problemas de duplicacao encontrados." -ForegroundColor Yellow
} else {
    Write-Host "ATENCAO! Varios problemas de duplicacao encontrados." -ForegroundColor Red
}

Write-Host ""


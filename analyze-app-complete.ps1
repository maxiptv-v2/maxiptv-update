# Script de analise completa do app MaxiPTV
# Verifica: codigo duplicado, deprecated, performance, estrutura

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "ANALISE COMPLETA DO APP MAXIPTV" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$issues = @()
$warnings = @()
$suggestions = @()

# Funcao para encontrar arquivos Kotlin
function Get-KotlinFiles {
    $files = Get-ChildItem -Path "app/src/main/java" -Filter "*.kt" -Recurse
    return $files
}

# Funcao para ler conteudo de arquivo
function Get-FileContent {
    param([string]$FilePath)
    try {
        return Get-Content -Path $FilePath -Raw -ErrorAction SilentlyContinue
    } catch {
        return $null
    }
}

Write-Host "[1/8] Analisando estrutura do projeto..." -ForegroundColor Yellow
$kotlinFiles = Get-KotlinFiles
Write-Host "  Encontrados $($kotlinFiles.Count) arquivos Kotlin" -ForegroundColor Green
Write-Host ""

# Verificar arquivos grandes (possivel problema de performance)
Write-Host "[2/8] Verificando arquivos grandes..." -ForegroundColor Yellow
foreach ($file in $kotlinFiles) {
    $lines = (Get-Content $file.FullName | Measure-Object -Line).Lines
    if ($lines -gt 1000) {
        $warnings += "Arquivo muito grande: $($file.Name) ($lines linhas) - Considere dividir em modulos"
        Write-Host "  [AVISO] $($file.Name): $lines linhas" -ForegroundColor Yellow
    }
}
Write-Host ""

# Verificar codigo duplicado (funcoes/metodos similares)
Write-Host "[3/8] Verificando codigo duplicado..." -ForegroundColor Yellow
$functionSignatures = @{}
$duplicates = @()

foreach ($file in $kotlinFiles) {
    $content = Get-FileContent -FilePath $file.FullName
    if ($null -eq $content) { continue }
    
    # Procurar por funcoes
    $functionPattern = 'fun\s+(\w+)\s*\([^)]*\)'
    $matches = [regex]::Matches($content, $functionPattern)
    
    foreach ($match in $matches) {
        $funcName = $match.Groups[1].Value
        $signature = $match.Value
        
        if ($functionSignatures.ContainsKey($signature)) {
            $duplicates += "Funcao duplicada encontrada: $funcName em $($file.Name) e $($functionSignatures[$signature])"
        } else {
            $functionSignatures[$signature] = $file.Name
        }
    }
}

if ($duplicates.Count -gt 0) {
    foreach ($dup in $duplicates) {
        $issues += $dup
        Write-Host "  [ERRO] $dup" -ForegroundColor Red
    }
} else {
    Write-Host "  [OK] Nenhuma funcao duplicada encontrada" -ForegroundColor Green
}
Write-Host ""

# Verificar imports nao utilizados
Write-Host "[4/8] Verificando imports nao utilizados..." -ForegroundColor Yellow
foreach ($file in $kotlinFiles) {
    $content = Get-FileContent -FilePath $file.FullName
    if ($null -eq $content) { continue }
    
    # Extrair imports
    $importPattern = 'import\s+([^\s;]+)'
    $imports = [regex]::Matches($content, $importPattern)
    
    foreach ($importMatch in $imports) {
        $importName = $importMatch.Groups[1].Value
        $className = $importName.Split('.')[-1]
        
        # Verificar se a classe e usada (exceto imports com *)
        if (-not $importName.Contains('*')) {
            $usagePattern = "\b$className\b"
            $usageCount = ([regex]::Matches($content, $usagePattern)).Count
            
            # Se aparece apenas no import, provavelmente nao e usado
            if ($usageCount -le 1) {
                $warnings += "Possivel import nao utilizado: $importName em $($file.Name)"
            }
        }
    }
}
Write-Host "  [OK] Verificacao de imports concluida" -ForegroundColor Green
Write-Host ""

# Verificar codigo deprecated
Write-Host "[5/8] Verificando codigo deprecated..." -ForegroundColor Yellow
$deprecatedPatterns = @(
    '@Deprecated',
    'systemUiVisibility',
    'getSystemService\(Context\.',
    'WindowManager\.LayoutParams\.FLAG_TRANSLUCENT',
    'View\.SYSTEM_UI_FLAG_',
    'Handler\(\)',
    'AsyncTask',
    'HttpURLConnection',
    'startActivityForResult'
)

foreach ($file in $kotlinFiles) {
    $content = Get-FileContent -FilePath $file.FullName
    if ($null -eq $content) { continue }
    
    foreach ($pattern in $deprecatedPatterns) {
        if ($content -match $pattern) {
            $lineNum = ($content.Substring(0, $content.IndexOf($pattern)).Split("`n")).Count
            $issues += "Codigo deprecated encontrado em $($file.Name) (linha ~$lineNum): $pattern"
            Write-Host "  [ERRO] $($file.Name): $pattern" -ForegroundColor Red
        }
    }
}

if ($issues.Count -eq 0) {
    Write-Host "  [OK] Nenhum codigo deprecated encontrado" -ForegroundColor Green
}
Write-Host ""

# Verificar problemas de performance
Write-Host "[6/8] Verificando problemas de performance..." -ForegroundColor Yellow
$performanceIssues = @(
    @{Pattern = 'Thread\.sleep'; Message = "Thread.sleep() pode causar travamentos - usar coroutines"},
    @{Pattern = 'while\s*\(true\)'; Message = "Loops infinitos podem travar - verificar condicoes de saida"},
    @{Pattern = '\.get\(\)'; Message = "Evitar .get() em coroutines - usar await()"},
    @{Pattern = 'runBlocking'; Message = "runBlocking pode travar thread principal - usar com cuidado"}
)

foreach ($file in $kotlinFiles) {
    $content = Get-FileContent -FilePath $file.FullName
    if ($null -eq $content) { continue }
    
    foreach ($issue in $performanceIssues) {
        if ($content -match $issue.Pattern) {
            $warnings += "$($file.Name): $($issue.Message)"
            Write-Host "  [AVISO] $($file.Name): $($issue.Message)" -ForegroundColor Yellow
        }
    }
}
Write-Host ""

# Verificar codigo comentado (possivel codigo morto)
Write-Host "[7/8] Verificando codigo comentado..." -ForegroundColor Yellow
$commentedCodeCount = 0
foreach ($file in $kotlinFiles) {
    $content = Get-FileContent -FilePath $file.FullName
    if ($null -eq $content) { continue }
    
    $lines = $content.Split("`n")
    $commentedLines = 0
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if ($trimmed.StartsWith("//") -and $trimmed.Length -gt 5) {
            $commentedLines++
        }
    }
    
    if ($commentedLines -gt 20) {
        $warnings += "Muito codigo comentado em $($file.Name) ($commentedLines linhas) - Considere remover"
        Write-Host "  [AVISO] $($file.Name): $commentedLines linhas comentadas" -ForegroundColor Yellow
    }
    $commentedCodeCount += $commentedLines
}
Write-Host "  Total de linhas comentadas: $commentedCodeCount" -ForegroundColor Cyan
Write-Host ""

# Verificar estrutura e organizacao
Write-Host "[8/8] Verificando estrutura e organizacao..." -ForegroundColor Yellow

# Verificar se todos os arquivos estao nos pacotes corretos
$packageIssues = 0
foreach ($file in $kotlinFiles) {
    $content = Get-FileContent -FilePath $file.FullName
    if ($null -eq $content) { continue }
    
    if (-not ($content -match '^package\s+com\.maxiptv')) {
        $issues += "Arquivo sem package correto: $($file.Name)"
        $packageIssues++
    }
}

if ($packageIssues -eq 0) {
    Write-Host "  [OK] Todos os arquivos tem package correto" -ForegroundColor Green
} else {
    Write-Host "  [ERRO] $packageIssues arquivos com problema de package" -ForegroundColor Red
}
Write-Host ""

# ============================================
# RESUMO FINAL
# ============================================

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "RESUMO DA ANALISE" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Arquivos Kotlin analisados: $($kotlinFiles.Count)" -ForegroundColor Cyan
Write-Host ""

if ($issues.Count -gt 0) {
    Write-Host "[ERROS ENCONTRADOS: $($issues.Count)]" -ForegroundColor Red
    foreach ($issue in $issues) {
        Write-Host "  - $issue" -ForegroundColor Red
    }
    Write-Host ""
} else {
    Write-Host "[OK] Nenhum erro critico encontrado" -ForegroundColor Green
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "[AVISOS: $($warnings.Count)]" -ForegroundColor Yellow
    foreach ($warning in $warnings) {
        Write-Host "  - $warning" -ForegroundColor Yellow
    }
    Write-Host ""
} else {
    Write-Host "[OK] Nenhum aviso encontrado" -ForegroundColor Green
    Write-Host ""
}

# Estatisticas
$totalLines = 0
foreach ($file in $kotlinFiles) {
    $lines = (Get-Content $file.FullName -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
    $totalLines += $lines
}

Write-Host "Estatisticas:" -ForegroundColor Cyan
Write-Host "  Total de linhas de codigo: $totalLines" -ForegroundColor White
Write-Host "  Media de linhas por arquivo: $([math]::Round($totalLines / $kotlinFiles.Count, 0))" -ForegroundColor White
Write-Host ""

if ($issues.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "===============================================================" -ForegroundColor Green
    Write-Host "[OK] APP ESTA EM BOA FORMA!" -ForegroundColor Green
    Write-Host "===============================================================" -ForegroundColor Green
    exit 0
} else {
    Write-Host "===============================================================" -ForegroundColor Yellow
    Write-Host "[ATENCAO] ALGUNS PROBLEMAS ENCONTRADOS" -ForegroundColor Yellow
    Write-Host "===============================================================" -ForegroundColor Yellow
    exit 1
}


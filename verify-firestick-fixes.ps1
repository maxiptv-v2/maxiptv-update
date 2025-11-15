# Script de Verificação: Correções Fire Stick
# Verifica se as correções foram aplicadas corretamente sem erros

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICAÇÃO: Correções Fire Stick" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$errors = @()
$warnings = @()

# 1. Verificar HomeScreen - Cor dos textos nos cards
Write-Host "[1] Verificando HomeScreen.kt - Cor dos textos nos cards..." -ForegroundColor Yellow
$homeScreen = Get-Content "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt" -Raw

# Verificar se Color.White está sendo usado para Fire Stick
if ($homeScreen -match 'color = if \(deviceType == "firestick"\) Color\.White') {
    Write-Host "  ✓ Cor White aplicada corretamente para Fire Stick" -ForegroundColor Green
} else {
    $errors += "HomeScreen.kt: Cor White não encontrada para Fire Stick"
    Write-Host "  ✗ Cor White não encontrada para Fire Stick" -ForegroundColor Red
}

# Verificar se Color.Unspecified está sendo usado para outros dispositivos
if ($homeScreen -match 'Color\.Unspecified') {
    Write-Host "  ✓ Color.Unspecified mantido para outros dispositivos" -ForegroundColor Green
} else {
    $warnings += "HomeScreen.kt: Color.Unspecified não encontrado"
    Write-Host "  ⚠ Color.Unspecified não encontrado" -ForegroundColor Yellow
}

# 2. Verificar LiveScreen - TopBar condicional
Write-Host ""
Write-Host "[2] Verificando LiveScreen.kt - TopBar condicional..." -ForegroundColor Yellow
$liveScreen = Get-Content "app\src\main\java\com\maxiptv\ui\screens\LiveScreen.kt" -Raw

# Verificar se TopBar esta condicional (if isFireStick)
if ($liveScreen -match 'if \(isFireStick\)') {
    Write-Host "  OK TopBar esta condicional (apenas Fire Stick)" -ForegroundColor Green
} else {
    $errors += "LiveScreen.kt: TopBar nao esta condicional (deve aparecer apenas no Fire Stick)"
    Write-Host "  X TopBar nao esta condicional" -ForegroundColor Red
}

# Verificar imports necessarios
if ($liveScreen -match 'import.*PlayArrow') {
    Write-Host "  OK Import PlayArrow encontrado" -ForegroundColor Green
} else {
    $errors += "LiveScreen.kt: Import PlayArrow nao encontrado"
    Write-Host "  X Import PlayArrow nao encontrado" -ForegroundColor Red
}

if ($liveScreen -match 'import.*ArrowBack') {
    Write-Host "  OK Import ArrowBack encontrado" -ForegroundColor Green
} else {
    $errors += "LiveScreen.kt: Import ArrowBack nao encontrado"
    Write-Host "  X Import ArrowBack nao encontrado" -ForegroundColor Red
}

# 3. Verificar VodScreen - TopBar condicional
Write-Host ""
Write-Host "[3] Verificando VodScreen.kt - TopBar condicional..." -ForegroundColor Yellow
$vodScreen = Get-Content "app\src\main\java\com\maxiptv\ui\screens\VodScreen.kt" -Raw

if ($vodScreen -match 'if \(isFireStick\)') {
    Write-Host "  OK TopBar esta condicional (apenas Fire Stick)" -ForegroundColor Green
} else {
    $errors += "VodScreen.kt: TopBar nao esta condicional"
    Write-Host "  X TopBar nao esta condicional" -ForegroundColor Red
}

if ($vodScreen -match 'import.*PlayArrow') {
    Write-Host "  OK Import PlayArrow encontrado" -ForegroundColor Green
} else {
    $errors += "VodScreen.kt: Import PlayArrow nao encontrado"
    Write-Host "  X Import PlayArrow nao encontrado" -ForegroundColor Red
}

# 4. Verificar SeriesScreen - TopBar condicional
Write-Host ""
Write-Host "[4] Verificando SeriesScreen.kt - TopBar condicional..." -ForegroundColor Yellow
$seriesScreen = Get-Content "app\src\main\java\com\maxiptv\ui\screens\SeriesScreen.kt" -Raw

if ($seriesScreen -match 'if \(isFireStick\)') {
    Write-Host "  OK TopBar esta condicional (apenas Fire Stick)" -ForegroundColor Green
} else {
    $errors += "SeriesScreen.kt: TopBar nao esta condicional"
    Write-Host "  X TopBar nao esta condicional" -ForegroundColor Red
}

if ($seriesScreen -match 'import.*PlayArrow') {
    Write-Host "  OK Import PlayArrow encontrado" -ForegroundColor Green
} else {
    $errors += "SeriesScreen.kt: Import PlayArrow nao encontrado"
    Write-Host "  X Import PlayArrow nao encontrado" -ForegroundColor Red
}

# 5. Verificar CategoryChips - Padding ajustado
Write-Host ""
Write-Host "[5] Verificando CategoryChips.kt - Padding ajustado..." -ForegroundColor Yellow
$categoryChips = Get-Content "app\src\main\java\com\maxiptv\ui\screens\CategoryChips.kt" -Raw

# Verificar se padding do topo esta diferente para Fire Stick
if ($categoryChips -match 'topPadding.*isFireStick.*8\.dp') {
    Write-Host "  OK Padding do topo ajustado para Fire Stick (8.dp)" -ForegroundColor Green
} else {
    $warnings += "CategoryChips.kt: Padding do topo pode nao estar correto"
    Write-Host "  ! Padding do topo pode nao estar correto" -ForegroundColor Yellow
}

if ($categoryChips -match 'topPadding.*isTv.*16\.dp') {
    Write-Host "  OK Padding do topo mantido para TV Box (16.dp)" -ForegroundColor Green
} else {
    $warnings += "CategoryChips.kt: Padding do topo para TV Box pode nao estar correto"
    Write-Host "  ! Padding do topo para TV Box pode nao estar correto" -ForegroundColor Yellow
}

# 6. Verificar se há erros de sintaxe básicos
Write-Host ""
Write-Host "[6] Verificando erros de sintaxe básicos..." -ForegroundColor Yellow

$files = @(
    "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt",
    "app\src\main\java\com\maxiptv\ui\screens\LiveScreen.kt",
    "app\src\main\java\com\maxiptv\ui\screens\VodScreen.kt",
    "app\src\main\java\com\maxiptv\ui\screens\SeriesScreen.kt",
    "app\src\main\java\com\maxiptv\ui\screens\CategoryChips.kt"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        
        # Verificar parenteses balanceados
        $openParens = ([regex]::Matches($content, '\(')).Count
        $closeParens = ([regex]::Matches($content, '\)')).Count
        if ($openParens -ne $closeParens) {
            $fileName = Split-Path $file -Leaf
            $errors += "${fileName}: Parenteses nao balanceados (abertos: $openParens, fechados: $closeParens)"
            Write-Host "  X ${fileName}: Parenteses nao balanceados" -ForegroundColor Red
        } else {
            $fileName = Split-Path $file -Leaf
            Write-Host "  OK ${fileName}: Parenteses balanceados" -ForegroundColor Green
        }
        
        # Verificar chaves balanceadas
        $openBraces = ([regex]::Matches($content, '\{')).Count
        $closeBraces = ([regex]::Matches($content, '\}')).Count
        if ($openBraces -ne $closeBraces) {
            $fileName = Split-Path $file -Leaf
            $errors += "${fileName}: Chaves nao balanceadas (abertas: $openBraces, fechadas: $closeBraces)"
            Write-Host "  X ${fileName}: Chaves nao balanceadas" -ForegroundColor Red
        } else {
            $fileName = Split-Path $file -Leaf
            Write-Host "  OK ${fileName}: Chaves balanceadas" -ForegroundColor Green
        }
    } else {
        $fileName = Split-Path $file -Leaf
        $errors += "${fileName}: Arquivo nao encontrado"
        Write-Host "  X ${fileName}: Arquivo nao encontrado" -ForegroundColor Red
    }
}

# 7. Verificar se MaxiApp.isFireStick está sendo usado
Write-Host ""
Write-Host "[7] Verificando uso de MaxiApp.isFireStick..." -ForegroundColor Yellow
$allFiles = Get-ChildItem -Path "app\src\main\java\com\maxiptv\ui\screens" -Filter "*.kt" -Recurse
$fireStickUsage = @()
foreach ($file in $allFiles) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match 'MaxiApp\.isFireStick|isFireStick') {
        $fireStickUsage += $file.Name
    }
}
if ($fireStickUsage.Count -gt 0) {
    Write-Host "  OK MaxiApp.isFireStick encontrado em:" -ForegroundColor Green
    foreach ($file in $fireStickUsage) {
        Write-Host "    - $file" -ForegroundColor Gray
    }
} else {
    $warnings += "Nenhum uso de MaxiApp.isFireStick encontrado"
    Write-Host "  ! Nenhum uso de MaxiApp.isFireStick encontrado" -ForegroundColor Yellow
}

# Resumo
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICAÇÃO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($errors.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "OK TODAS AS VERIFICACOES PASSARAM!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Correcoes aplicadas:" -ForegroundColor White
    Write-Host "  OK Cor dos textos nos cards (apenas Fire Stick)" -ForegroundColor Green
    Write-Host "  OK TopBar condicional nas telas Live/Vod/Series (apenas Fire Stick)" -ForegroundColor Green
    Write-Host "  OK Padding do CategoryChips ajustado" -ForegroundColor Green
    Write-Host ""
    Write-Host "Pronto para compilar!" -ForegroundColor Cyan
    exit 0
} else {
    if ($errors.Count -gt 0) {
        Write-Host "ERROS ENCONTRADOS ($($errors.Count)):" -ForegroundColor Red
        foreach ($error in $errors) {
            Write-Host "  - $error" -ForegroundColor Red
        }
        Write-Host ""
    }
    
    if ($warnings.Count -gt 0) {
        Write-Host "AVISOS ($($warnings.Count)):" -ForegroundColor Yellow
        foreach ($warning in $warnings) {
            Write-Host "  - $warning" -ForegroundColor Yellow
        }
        Write-Host ""
    }
    
    if ($errors.Count -gt 0) {
        Write-Host "Corrija os erros antes de compilar!" -ForegroundColor Red
        exit 1
    } else {
        Write-Host "Avisos encontrados, mas pode compilar." -ForegroundColor Yellow
        exit 0
    }
}


# Script para testar se o ajuste de largura (fillMaxWidthAdjusted) foi aplicado corretamente
# Verifica Fire Stick e Native TV (90%) vs TV Box (100%)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE: Ajuste de Largura (90% vs 100%)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$errors = @()
$warnings = @()
$success = @()

# 1. Verificar se o modifier fillMaxWidthAdjusted existe em SafeLayout.kt
Write-Host "[1/7] Verificando modifier fillMaxWidthAdjusted() em SafeLayout.kt..." -ForegroundColor Yellow

$safeLayoutPath = "app/src/main/java/com/maxiptv/ui/components/SafeLayout.kt"
if (Test-Path $safeLayoutPath) {
    $safeLayoutContent = Get-Content $safeLayoutPath -Raw
    
    if ($safeLayoutContent -match "fun Modifier\.fillMaxWidthAdjusted\(\)") {
        Write-Host "  ✅ Modifier fillMaxWidthAdjusted() encontrado" -ForegroundColor Green
        $success += "Modifier fillMaxWidthAdjusted() existe"
        
        # Verificar lógica Fire Stick (0.90f)
        if ($safeLayoutContent -match "MaxiApp\.isFireStick" -and $safeLayoutContent -match "fillMaxWidth\(0\.90f\)") {
            Write-Host "  ✅ Fire Stick: 0.90f configurado corretamente" -ForegroundColor Green
            $success += "Fire Stick: 0.90f"
        } else {
            Write-Host "  ❌ Fire Stick: 0.90f NÃO encontrado" -ForegroundColor Red
            $errors += "Fire Stick não está usando 0.90f em fillMaxWidthAdjusted()"
        }
        
        # Verificar lógica Native TV (0.90f)
        if ($safeLayoutContent -match "MaxiApp\.isNativeTv" -and $safeLayoutContent -match "fillMaxWidth\(0\.90f\)") {
            Write-Host "  ✅ Native TV: 0.90f configurado corretamente" -ForegroundColor Green
            $success += "Native TV: 0.90f"
        } else {
            Write-Host "  ❌ Native TV: 0.90f NÃO encontrado" -ForegroundColor Red
            $errors += "Native TV não está usando 0.90f em fillMaxWidthAdjusted()"
        }
        
        # Verificar lógica TV Box (100% - fillMaxWidth sem parâmetro)
        if ($safeLayoutContent -match "MaxiApp\.isTvBox" -and $safeLayoutContent -match "fillMaxWidth\(\)") {
            Write-Host "  ✅ TV Box: 100% (fillMaxWidth sem parâmetro) configurado corretamente" -ForegroundColor Green
            $success += "TV Box: 100%"
        } else {
            Write-Host "  ⚠️  TV Box: verificação manual necessária" -ForegroundColor Yellow
            $warnings += "TV Box pode não estar usando fillMaxWidth() sem parâmetro"
        }
    } else {
        Write-Host "  ❌ Modifier fillMaxWidthAdjusted() NÃO encontrado" -ForegroundColor Red
        $errors += "fillMaxWidthAdjusted() não existe em SafeLayout.kt"
    }
} else {
    Write-Host "  ❌ Arquivo SafeLayout.kt não encontrado" -ForegroundColor Red
    $errors += "SafeLayout.kt não existe"
}

Write-Host ""

# 2. Verificar HomeScreen.kt
Write-Host "[2/7] Verificando HomeScreen.kt..." -ForegroundColor Yellow

$homeScreenPath = "app/src/main/java/com/maxiptv/ui/screens/HomeScreen.kt"
if (Test-Path $homeScreenPath) {
    $homeScreenContent = Get-Content $homeScreenPath -Raw
    
    # Verificar import
    if ($homeScreenContent -match "import com\.maxiptv\.ui\.components\.fillMaxWidthAdjusted") {
        Write-Host "  ✅ Import de fillMaxWidthAdjusted encontrado" -ForegroundColor Green
        $success += "HomeScreen: import correto"
    } else {
        Write-Host "  ❌ Import de fillMaxWidthAdjusted NÃO encontrado" -ForegroundColor Red
        $errors += "HomeScreen.kt não tem import de fillMaxWidthAdjusted"
    }
    
    # Verificar TopBar (Box com fillMaxWidthAdjusted)
    if ($homeScreenContent -match "fillMaxWidthAdjusted\(\)" -and $homeScreenContent -match "Logo Max IPTV|TopBar") {
        Write-Host "  ✅ TopBar usando fillMaxWidthAdjusted()" -ForegroundColor Green
        $success += "HomeScreen: TopBar ajustado"
    } else {
        # Verificar se há Box com fillMaxWidthAdjusted antes do padding
        $lines = Get-Content $homeScreenPath
        $foundTopBar = $false
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i] -match "Box" -and $i+1 -lt $lines.Count -and $lines[$i+1] -match "fillMaxWidthAdjusted") {
                $foundTopBar = $true
                break
            }
        }
        if ($foundTopBar) {
            Write-Host "  ✅ TopBar usando fillMaxWidthAdjusted()" -ForegroundColor Green
            $success += "HomeScreen: TopBar ajustado"
        } else {
            Write-Host "  ⚠️  TopBar pode não estar usando fillMaxWidthAdjusted()" -ForegroundColor Yellow
            $warnings += "HomeScreen TopBar pode não estar ajustado"
        }
    }
    
    # Verificar Row de categorias
    if ($homeScreenContent -match "fillMaxWidthAdjusted\(\)" -and $homeScreenContent -match "Botões de Categoria|Row") {
        Write-Host "  ✅ Row de categorias usando fillMaxWidthAdjusted()" -ForegroundColor Green
        $success += "HomeScreen: Row de categorias ajustado"
    } else {
        Write-Host "  ❌ Row de categorias NÃO está usando fillMaxWidthAdjusted()" -ForegroundColor Red
        $errors += "HomeScreen Row de categorias não está ajustado"
    }
    
    # Verificar CategoryButton (cards individuais)
    if ($homeScreenContent -match "deviceType.*firestick.*fillMaxWidth\(0\.90f\)|fillMaxWidth\(0\.90f\).*firestick") {
        Write-Host "  ✅ CategoryButton (cards) usando fillMaxWidth(0.90f) para Fire Stick" -ForegroundColor Green
        $success += "HomeScreen: CategoryButton ajustado para Fire Stick"
    } else {
        Write-Host "  ⚠️  CategoryButton pode não estar usando fillMaxWidth(0.90f) para Fire Stick" -ForegroundColor Yellow
        $warnings += "HomeScreen CategoryButton pode não estar ajustado"
    }
    
    if ($homeScreenContent -match "isNativeTv.*fillMaxWidth\(0\.90f\)|fillMaxWidth\(0\.90f\).*isNativeTv") {
        Write-Host "  ✅ CategoryButton (cards) usando fillMaxWidth(0.90f) para Native TV" -ForegroundColor Green
        $success += "HomeScreen: CategoryButton ajustado para Native TV"
    } else {
        Write-Host "  ⚠️  CategoryButton pode não estar usando fillMaxWidth(0.90f) para Native TV" -ForegroundColor Yellow
        $warnings += "HomeScreen CategoryButton Native TV pode não estar ajustado"
    }
} else {
    Write-Host "  ❌ Arquivo HomeScreen.kt não encontrado" -ForegroundColor Red
    $errors += "HomeScreen.kt não existe"
}

Write-Host ""

# 3. Verificar LiveScreen.kt
Write-Host "[3/7] Verificando LiveScreen.kt..." -ForegroundColor Yellow

$liveScreenPath = "app/src/main/java/com/maxiptv/ui/screens/LiveScreen.kt"
if (Test-Path $liveScreenPath) {
    $liveScreenContent = Get-Content $liveScreenPath -Raw
    
    # Verificar import
    if ($liveScreenContent -match "import com\.maxiptv\.ui\.components\.fillMaxWidthAdjusted") {
        Write-Host "  ✅ Import de fillMaxWidthAdjusted encontrado" -ForegroundColor Green
        $success += "LiveScreen: import correto"
    } else {
        Write-Host "  ❌ Import de fillMaxWidthAdjusted NÃO encontrado" -ForegroundColor Red
        $errors += "LiveScreen.kt não tem import de fillMaxWidthAdjusted"
    }
    
    # Verificar TopBar
    if ($liveScreenContent -match "fillMaxWidthAdjusted\(\)" -and $liveScreenContent -match "TopBar|Logo.*Max IPTV") {
        Write-Host "  ✅ TopBar usando fillMaxWidthAdjusted()" -ForegroundColor Green
        $success += "LiveScreen: TopBar ajustado"
    } else {
        Write-Host "  ❌ TopBar NÃO está usando fillMaxWidthAdjusted()" -ForegroundColor Red
        $errors += "LiveScreen TopBar não está ajustado"
    }
} else {
    Write-Host "  ❌ Arquivo LiveScreen.kt não encontrado" -ForegroundColor Red
    $errors += "LiveScreen.kt não existe"
}

Write-Host ""

# 4. Verificar VodScreen.kt
Write-Host "[4/7] Verificando VodScreen.kt..." -ForegroundColor Yellow

$vodScreenPath = "app/src/main/java/com/maxiptv/ui/screens/VodScreen.kt"
if (Test-Path $vodScreenPath) {
    $vodScreenContent = Get-Content $vodScreenPath -Raw
    
    # Verificar import
    if ($vodScreenContent -match "import com\.maxiptv\.ui\.components\.fillMaxWidthAdjusted") {
        Write-Host "  ✅ Import de fillMaxWidthAdjusted encontrado" -ForegroundColor Green
        $success += "VodScreen: import correto"
    } else {
        Write-Host "  ❌ Import de fillMaxWidthAdjusted NÃO encontrado" -ForegroundColor Red
        $errors += "VodScreen.kt não tem import de fillMaxWidthAdjusted"
    }
    
    # Verificar TopBar
    if ($vodScreenContent -match "fillMaxWidthAdjusted\(\)" -and $vodScreenContent -match "TopBar|Logo.*Max IPTV") {
        Write-Host "  ✅ TopBar usando fillMaxWidthAdjusted()" -ForegroundColor Green
        $success += "VodScreen: TopBar ajustado"
    } else {
        Write-Host "  ❌ TopBar NÃO está usando fillMaxWidthAdjusted()" -ForegroundColor Red
        $errors += "VodScreen TopBar não está ajustado"
    }
} else {
    Write-Host "  ❌ Arquivo VodScreen.kt não encontrado" -ForegroundColor Red
    $errors += "VodScreen.kt não existe"
}

Write-Host ""

# 5. Verificar SeriesScreen.kt
Write-Host "[5/7] Verificando SeriesScreen.kt..." -ForegroundColor Yellow

$seriesScreenPath = "app/src/main/java/com/maxiptv/ui/screens/SeriesScreen.kt"
if (Test-Path $seriesScreenPath) {
    $seriesScreenContent = Get-Content $seriesScreenPath -Raw
    
    # Verificar import
    if ($seriesScreenContent -match "import com\.maxiptv\.ui\.components\.fillMaxWidthAdjusted") {
        Write-Host "  ✅ Import de fillMaxWidthAdjusted encontrado" -ForegroundColor Green
        $success += "SeriesScreen: import correto"
    } else {
        Write-Host "  ❌ Import de fillMaxWidthAdjusted NÃO encontrado" -ForegroundColor Red
        $errors += "SeriesScreen.kt não tem import de fillMaxWidthAdjusted"
    }
    
    # Verificar TopBar
    if ($seriesScreenContent -match "fillMaxWidthAdjusted\(\)" -and $seriesScreenContent -match "TopBar|Logo.*Max IPTV") {
        Write-Host "  ✅ TopBar usando fillMaxWidthAdjusted()" -ForegroundColor Green
        $success += "SeriesScreen: TopBar ajustado"
    } else {
        Write-Host "  ❌ TopBar NÃO está usando fillMaxWidthAdjusted()" -ForegroundColor Red
        $errors += "SeriesScreen TopBar não está ajustado"
    }
} else {
    Write-Host "  ❌ Arquivo SeriesScreen.kt não encontrado" -ForegroundColor Red
    $errors += "SeriesScreen.kt não existe"
}

Write-Host ""

# 6. Verificar CategoryChips.kt
Write-Host "[6/7] Verificando CategoryChips.kt..." -ForegroundColor Yellow

$categoryChipsPath = "app/src/main/java/com/maxiptv/ui/screens/CategoryChips.kt"
if (Test-Path $categoryChipsPath) {
    $categoryChipsContent = Get-Content $categoryChipsPath -Raw
    
    # Verificar import
    if ($categoryChipsContent -match "import com\.maxiptv\.ui\.components\.fillMaxWidthAdjusted") {
        Write-Host "  ✅ Import de fillMaxWidthAdjusted encontrado" -ForegroundColor Green
        $success += "CategoryChips: import correto"
    } else {
        Write-Host "  ❌ Import de fillMaxWidthAdjusted NÃO encontrado" -ForegroundColor Red
        $errors += "CategoryChips.kt não tem import de fillMaxWidthAdjusted"
    }
    
    # Verificar Row
    if ($categoryChipsContent -match "fillMaxWidthAdjusted\(\)" -and $categoryChipsContent -match "Row") {
        Write-Host "  ✅ Row usando fillMaxWidthAdjusted()" -ForegroundColor Green
        $success += "CategoryChips: Row ajustado"
    } else {
        Write-Host "  ❌ Row NÃO está usando fillMaxWidthAdjusted()" -ForegroundColor Red
        $errors += "CategoryChips Row não está ajustado"
    }
} else {
    Write-Host "  ❌ Arquivo CategoryChips.kt não encontrado" -ForegroundColor Red
    $errors += "CategoryChips.kt não existe"
}

Write-Host ""

# 7. Verificar se não há fillMaxWidth() sem ajuste em lugares críticos
Write-Host "[7/7] Verificando se não há fillMaxWidth() sem ajuste em lugares críticos..." -ForegroundColor Yellow

$criticalFiles = @(
    "app/src/main/java/com/maxiptv/ui/screens/HomeScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/LiveScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/VodScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/SeriesScreen.kt"
)

$foundIssues = $false
foreach ($file in $criticalFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        
        # Procurar por fillMaxWidth() em Box/Row que deveriam usar fillMaxWidthAdjusted()
        # Mas não contar se já está usando fillMaxWidthAdjusted() ou fillMaxWidth(0.90f)
        $lines = Get-Content $file
        $lineNumber = 0
        foreach ($line in $lines) {
            $lineNumber++
            if ($line -match "\.fillMaxWidth\(\)" -and 
                $line -notmatch "fillMaxWidthAdjusted" -and 
                $line -notmatch "fillMaxWidth\(0\.90f\)" -and
                ($line -match "Box|Row|Column") -and
                ($line -match "TopBar|Category|Button")) {
                Write-Host "  ⚠️  Linha $lineNumber em $(Split-Path $file -Leaf): pode precisar de ajuste" -ForegroundColor Yellow
                Write-Host "     $($line.Trim())" -ForegroundColor Gray
                $foundIssues = $true
            }
        }
    }
}

if (-not $foundIssues) {
    Write-Host "  ✅ Nenhum problema encontrado" -ForegroundColor Green
    $success += "Nenhum fillMaxWidth() sem ajuste encontrado"
} else {
    $warnings += "Alguns fillMaxWidth() podem precisar de ajuste"
}

Write-Host ""

# RESUMO FINAL
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DO TESTE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($success.Count -gt 0) {
    Write-Host "✅ SUCESSOS ($($success.Count)):" -ForegroundColor Green
    foreach ($s in $success) {
        Write-Host "   • $s" -ForegroundColor White
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "⚠️  AVISOS ($($warnings.Count)):" -ForegroundColor Yellow
    foreach ($w in $warnings) {
        Write-Host "   • $w" -ForegroundColor White
    }
    Write-Host ""
}

if ($errors.Count -gt 0) {
    Write-Host "❌ ERROS ($($errors.Count)):" -ForegroundColor Red
    foreach ($e in $errors) {
        Write-Host "   • $e" -ForegroundColor White
    }
    Write-Host ""
    
    Write-Host "RESULTADO: ❌ FALHOU - Corrija os erros acima" -ForegroundColor Red
    exit 1
} else {
    Write-Host "RESULTADO: ✅ PASSOU - Ajuste de largura aplicado corretamente!" -ForegroundColor Green
    Write-Host ""
    Write-Host "O ajuste está configurado para:" -ForegroundColor Cyan
    Write-Host "  • Fire Stick: 90% da largura real (0.90f)" -ForegroundColor Yellow
    Write-Host "  • Native TV: 90% da largura real (0.90f)" -ForegroundColor Yellow
    Write-Host "  • TV Box: 100% da largura (sem alteração)" -ForegroundColor Green
    Write-Host ""
    exit 0
}


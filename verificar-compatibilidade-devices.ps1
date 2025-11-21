# Script para verificar se o código está completamente atualizado para todos os dispositivos suportados

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACAO DE COMPATIBILIDADE DE DEVICES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$errors = @()
$warnings = @()
$success = @()

# Dispositivos suportados
$devices = @("Fire Stick", "TV Box", "Native TV", "Projector", "Phone", "Tablet")

Write-Host "1. Verificando detecção de dispositivos..." -ForegroundColor Yellow

$maxiAppFile = "app/src/main/java/com/maxiptv/MaxiApp.kt"
if (-not (Test-Path $maxiAppFile)) {
    Write-Host "   ❌ MaxiApp.kt não encontrado" -ForegroundColor Red
    exit 1
}

$maxiAppContent = Get-Content $maxiAppFile -Raw

# Verificar variáveis de detecção
$detectionVars = @(
    "isFireStick",
    "isTvBox", 
    "isNativeTv",
    "isProjector",
    "isPhone",
    "isTablet",
    "isTv",
    "deviceCategory"
)

foreach ($var in $detectionVars) {
    if ($maxiAppContent -match "\b$var\b") {
        Write-Host "   ✅ $var detectado" -ForegroundColor Green
    } else {
        Write-Host "   ❌ $var NÃO encontrado" -ForegroundColor Red
        $errors += "$var não encontrado"
    }
}

Write-Host ""

Write-Host "2. Verificando configurações específicas por dispositivo..." -ForegroundColor Yellow

# Fire Stick
if ($maxiAppContent -match "fireStickOverscanPadding|fireStickSafeAreaPadding") {
    Write-Host "   ✅ Configurações Fire Stick encontradas" -ForegroundColor Green
    $success += "Fire Stick: Configurações presentes"
} else {
    Write-Host "   ❌ Configurações Fire Stick NÃO encontradas" -ForegroundColor Red
    $errors += "Configurações Fire Stick faltando"
}

# Verificar ajustes automáticos por tamanho de TV
if ($maxiAppContent -match "diagonalInches|fireStickOverscanPadding.*when") {
    Write-Host "   ✅ Ajuste automático por tamanho de TV encontrado" -ForegroundColor Green
    $success += "Fire Stick: Ajuste automático por tamanho de TV"
} else {
    Write-Host "   ⚠️ Ajuste automático por tamanho de TV pode estar faltando" -ForegroundColor Yellow
    $warnings += "Ajuste automático por tamanho de TV pode estar faltando"
}

Write-Host ""

Write-Host "3. Verificando Safe Area adjustments..." -ForegroundColor Yellow

$safeAreaFile = "app/src/main/java/com/maxiptv/ui/theme/SafeArea.kt"
if (Test-Path $safeAreaFile) {
    $safeAreaContent = Get-Content $safeAreaFile -Raw
    
    # Verificar se há ajustes para cada tipo de dispositivo
    $deviceChecks = @{
        "Fire Stick" = "isFireStick|fireStick"
        "TV Box" = "isTvBox|tvBox"
        "Native TV" = "isNativeTv|nativeTv"
        "Projector" = "isProjector|projector"
        "Phone" = "isPhone|phone"
        "Tablet" = "isTablet|tablet"
    }
    
    foreach ($device in $deviceChecks.Keys) {
        $pattern = $deviceChecks[$device]
        if ($safeAreaContent -match $pattern) {
            Write-Host "   ✅ Safe Area para $device encontrado" -ForegroundColor Green
            $success += "Safe Area: $device configurado"
        } else {
            Write-Host "   ⚠️ Safe Area para $device pode estar faltando" -ForegroundColor Yellow
            $warnings += "Safe Area para $device pode estar faltando"
        }
    }
} else {
    Write-Host "   ❌ SafeArea.kt não encontrado" -ForegroundColor Red
    $errors += "SafeArea.kt não encontrado"
}

Write-Host ""

Write-Host "4. Verificando TopBar condicional (Fire Stick)..." -ForegroundColor Yellow

$screens = @(
    "LiveScreen.kt",
    "VodScreen.kt", 
    "SeriesScreen.kt"
)

foreach ($screen in $screens) {
    $screenPath = "app/src/main/java/com/maxiptv/ui/screens/$screen"
    if (Test-Path $screenPath) {
        $screenContent = Get-Content $screenPath -Raw
        if ($screenContent -match "isFireStick.*TopBar|if.*isFireStick.*TopBar") {
            Write-Host "   ✅ TopBar condicional em $screen encontrado" -ForegroundColor Green
            $success += "TopBar: $screen configurado"
        } else {
            Write-Host "   ⚠️ TopBar condicional em $screen pode estar faltando" -ForegroundColor Yellow
            $warnings += "TopBar condicional em $screen pode estar faltando"
        }
    }
}

Write-Host ""

Write-Host "5. Verificando fullscreen implementation..." -ForegroundColor Yellow

$liveScreenFile = "app/src/main/java/com/maxiptv/ui/screens/LiveScreen.kt"
if (Test-Path $liveScreenFile) {
    $liveScreenContent = Get-Content $liveScreenFile -Raw
    
    # Verificar fullscreen
    if ($liveScreenContent -match "isFullscreen|DisposableEffect\(isFullscreen\)") {
        Write-Host "   ✅ Fullscreen implementation encontrado" -ForegroundColor Green
        $success += "Fullscreen: Implementado"
    } else {
        Write-Host "   ❌ Fullscreen implementation NÃO encontrado" -ForegroundColor Red
        $errors += "Fullscreen implementation faltando"
    }
    
    # Verificar systemBarsPadding e RESIZE_MODE_FILL
    if ($liveScreenContent -match "systemBarsPadding|RESIZE_MODE_FILL") {
        Write-Host "   ✅ Correções fullscreen Fire Stick encontradas" -ForegroundColor Green
        $success += "Fullscreen: Correções Fire Stick aplicadas"
    } else {
        Write-Host "   ❌ Correções fullscreen Fire Stick NÃO encontradas" -ForegroundColor Red
        $errors += "Correções fullscreen Fire Stick faltando"
    }
}

Write-Host ""

Write-Host "6. Verificando ajustes de layout por dispositivo..." -ForegroundColor Yellow

# Verificar fillMaxWidthAdjusted
$componentsFile = "app/src/main/java/com/maxiptv/ui/components/SafeLayout.kt"
if (Test-Path $componentsFile) {
    $componentsContent = Get-Content $componentsFile -Raw
    if ($componentsContent -match "fillMaxWidthAdjusted") {
        Write-Host "   ✅ fillMaxWidthAdjusted encontrado" -ForegroundColor Green
        $success += "Layout: fillMaxWidthAdjusted implementado"
    }
}

# Verificar uso em screens
foreach ($screen in $screens) {
    $screenPath = "app/src/main/java/com/maxiptv/ui/screens/$screen"
    if (Test-Path $screenPath) {
        $screenContent = Get-Content $screenPath -Raw
        if ($screenContent -match "fillMaxWidthAdjusted") {
            Write-Host "   ✅ fillMaxWidthAdjusted usado em $screen" -ForegroundColor Green
        }
    }
}

Write-Host ""

Write-Host "7. Verificando PlayerActivity para diferentes dispositivos..." -ForegroundColor Yellow

$playerActivityFile = "app/src/main/java/com/maxiptv/ui/player/PlayerActivity.kt"
if (Test-Path $playerActivityFile) {
    $playerContent = Get-Content $playerActivityFile -Raw
    
    # Verificar ajustes de overscan por dispositivo
    if ($playerContent -match "isFireStick.*overscan|isNativeTv.*overscan|isTvBox.*overscan") {
        Write-Host "   ✅ Ajustes de overscan por dispositivo encontrados" -ForegroundColor Green
        $success += "PlayerActivity: Ajustes de overscan por dispositivo"
    } else {
        Write-Host "   ⚠️ Ajustes de overscan por dispositivo podem estar faltando" -ForegroundColor Yellow
        $warnings += "PlayerActivity: Ajustes de overscan podem estar faltando"
    }
}

Write-Host ""

Write-Host "8. Verificando MainActivity configurações..." -ForegroundColor Yellow

$mainActivityFile = "app/src/main/java/com/maxiptv/MainActivity.kt"
if (Test-Path $mainActivityFile) {
    $mainContent = Get-Content $mainActivityFile -Raw
    
    if ($mainContent -match "MaxiApp\.isTv|if.*isTv") {
        Write-Host "   ✅ Configurações TV em MainActivity encontradas" -ForegroundColor Green
        $success += "MainActivity: Configurações TV"
    }
}

Write-Host ""

Write-Host "9. Verificando AndroidManifest..." -ForegroundColor Yellow

$manifestFile = "app/src/main/AndroidManifest.xml"
if (Test-Path $manifestFile) {
    $manifestContent = Get-Content $manifestFile -Raw
    
    # Verificar feature LEANBACK (necessário para TV)
    if ($manifestContent -match "android\.hardware\.type\.television|LEANBACK") {
        Write-Host "   ✅ Feature TV (LEANBACK) encontrada" -ForegroundColor Green
        $success += "AndroidManifest: Feature TV configurada"
    } else {
        Write-Host "   ⚠️ Feature TV pode estar faltando" -ForegroundColor Yellow
        $warnings += "AndroidManifest: Feature TV pode estar faltando"
    }
}

Write-Host ""

Write-Host "10. Verificando keywords de detecção..." -ForegroundColor Yellow

# Verificar se há keywords suficientes para cada tipo
$keywordChecks = @{
    "Fire Stick" = @("aft", "sheldon", "mantis", "amazon", "fire")
    "TV Box" = @("tv box", "tvbox", "amlogic", "s905", "rk3328")
    "Native TV" = @("philco", "smarttv", "tcl", "hisense", "sony")
    "Projector" = @("projector", "xgimi", "nebula", "dangbei")
}

foreach ($device in $keywordChecks.Keys) {
    $keywords = $keywordChecks[$device]
    $found = 0
    foreach ($keyword in $keywords) {
        if ($maxiAppContent -match $keyword) {
            $found++
        }
    }
    if ($found -ge 2) {
        Write-Host "   ✅ Keywords para $device suficientes ($found encontradas)" -ForegroundColor Green
        $success += "Keywords: $device configurado"
    } else {
        Write-Host "   ⚠️ Keywords para $device podem estar insuficientes ($found encontradas)" -ForegroundColor Yellow
        $warnings += "Keywords para $device podem estar insuficientes"
    }
}

Write-Host ""

# Resumo
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($success.Count -gt 0) {
    Write-Host "✅ SUCESSOS ($($success.Count)):" -ForegroundColor Green
    foreach ($s in $success) {
        Write-Host "   - $s" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "⚠️ AVISOS ($($warnings.Count)):" -ForegroundColor Yellow
    foreach ($w in $warnings) {
        Write-Host "   - $w" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($errors.Count -gt 0) {
    Write-Host "❌ ERROS ($($errors.Count)):" -ForegroundColor Red
    foreach ($e in $errors) {
        Write-Host "   - $e" -ForegroundColor Gray
    }
    Write-Host ""
    Write-Host "⚠️ CODIGO PRECISA DE ATENCAO" -ForegroundColor Red
    exit 1
} else {
    if ($warnings.Count -eq 0) {
        Write-Host "🎉 CODIGO COMPLETAMENTE ATUALIZADO PARA TODOS OS DEVICES!" -ForegroundColor Green
    } else {
        Write-Host "✅ CODIGO ATUALIZADO (com alguns avisos menores)" -ForegroundColor Green
    }
    exit 0
}


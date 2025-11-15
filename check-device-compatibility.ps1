# Script Profissional: Verificacao de Compatibilidade com Dispositivos
# Verifica se o app tem tudo necessario para funcionar em todos os dispositivos suportados

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACAO: Compatibilidade Dispositivos" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$issues = @()
$warnings = @()
$recommendations = @()

# Dispositivos suportados
$supportedDevices = @(
    "Fire Stick Amazon",
    "TV Box Android (Genericos)",
    "TVs Android Nativas",
    "Projetores Android",
    "Smartphones Android",
    "Tablets Android",
    "Chromecast / Android TV"
)

Write-Host "Dispositivos suportados:" -ForegroundColor White
foreach ($device in $supportedDevices) {
    Write-Host "  - $device" -ForegroundColor Gray
}
Write-Host ""

# 1. Verificar MaxiApp.kt - Deteccao de dispositivos
Write-Host "[1] Verificando deteccao de dispositivos (MaxiApp.kt)..." -ForegroundColor Yellow
$maxiApp = Get-Content "app\src\main\java\com\maxiptv\MaxiApp.kt" -Raw -ErrorAction SilentlyContinue

if (-not $maxiApp) {
    $issues += "MaxiApp.kt nao encontrado!"
    Write-Host "  X Arquivo nao encontrado" -ForegroundColor Red
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar deteccao de Fire Stick
    if ($maxiApp -match "isFireStick|Fire Stick|firestick") {
        Write-Host "  OK Deteccao Fire Stick encontrada" -ForegroundColor Green
    } else {
        $issues += "MaxiApp.kt: Deteccao Fire Stick nao encontrada"
        Write-Host "  X Deteccao Fire Stick nao encontrada" -ForegroundColor Red
    }
    
    # Verificar deteccao de TV Box
    if ($maxiApp -match "isTvBox|TV Box|tvbox") {
        Write-Host "  OK Deteccao TV Box encontrada" -ForegroundColor Green
    } else {
        $issues += "MaxiApp.kt: Deteccao TV Box nao encontrada"
        Write-Host "  X Deteccao TV Box nao encontrada" -ForegroundColor Red
    }
    
    # Verificar deteccao de TV Nativa
    if ($maxiApp -match "isNativeTv|Native TV|nativetv") {
        Write-Host "  OK Deteccao TV Nativa encontrada" -ForegroundColor Green
    } else {
        $warnings += "MaxiApp.kt: Deteccao TV Nativa pode estar faltando"
        Write-Host "  ! Deteccao TV Nativa pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar deteccao de Projetor
    if ($maxiApp -match "isProjector|Projector|projector") {
        Write-Host "  OK Deteccao Projetor encontrada" -ForegroundColor Green
    } else {
        $warnings += "MaxiApp.kt: Deteccao Projetor pode estar faltando"
        Write-Host "  ! Deteccao Projetor pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar deteccao de Smartphone
    if ($maxiApp -match "isPhone|Phone|phone") {
        Write-Host "  OK Deteccao Smartphone encontrada" -ForegroundColor Green
    } else {
        $issues += "MaxiApp.kt: Deteccao Smartphone nao encontrada"
        Write-Host "  X Deteccao Smartphone nao encontrada" -ForegroundColor Red
    }
    
    # Verificar deteccao de Tablet
    if ($maxiApp -match "isTablet|Tablet|tablet") {
        Write-Host "  OK Deteccao Tablet encontrada" -ForegroundColor Green
    } else {
        $warnings += "MaxiApp.kt: Deteccao Tablet pode estar faltando"
        Write-Host "  ! Deteccao Tablet pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar configuracao de SafeArea para Fire Stick
    if ($maxiApp -match "fireStickOverscanPadding|fireStickSafeAreaPadding") {
        Write-Host "  OK Configuracao SafeArea Fire Stick encontrada" -ForegroundColor Green
    } else {
        $warnings += "MaxiApp.kt: Configuracao SafeArea Fire Stick pode estar faltando"
        Write-Host "  ! Configuracao SafeArea Fire Stick pode estar faltando" -ForegroundColor Yellow
    }
}

# 2. Verificar SafeArea.kt - Ajustes por dispositivo
Write-Host ""
Write-Host "[2] Verificando SafeArea.kt (ajustes por dispositivo)..." -ForegroundColor Yellow
$safeArea = Get-Content "app\src\main\java\com\maxiptv\ui\theme\SafeArea.kt" -Raw -ErrorAction SilentlyContinue

if (-not $safeArea) {
    $issues += "SafeArea.kt nao encontrado!"
    Write-Host "  X Arquivo nao encontrado" -ForegroundColor Red
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar tratamento para Fire Stick
    if ($safeArea -match "isFireStick|Fire Stick|fireStick") {
        Write-Host "  OK Tratamento Fire Stick encontrado" -ForegroundColor Green
    } else {
        $issues += "SafeArea.kt: Tratamento Fire Stick nao encontrado"
        Write-Host "  X Tratamento Fire Stick nao encontrado" -ForegroundColor Red
    }
    
    # Verificar tratamento para TV Box
    if ($safeArea -match "isTvBox|TvBox|tvBox") {
        Write-Host "  OK Tratamento TV Box encontrado" -ForegroundColor Green
    } else {
        $warnings += "SafeArea.kt: Tratamento TV Box pode estar faltando"
        Write-Host "  ! Tratamento TV Box pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar SafeAreaAutoDetector
    if ($safeArea -match "SafeAreaAutoDetector|detectAndApplySettings") {
        Write-Host "  OK SafeAreaAutoDetector encontrado" -ForegroundColor Green
    } else {
        $warnings += "SafeArea.kt: SafeAreaAutoDetector pode estar faltando"
        Write-Host "  ! SafeAreaAutoDetector pode estar faltando" -ForegroundColor Yellow
    }
}

# 3. Verificar AndroidManifest.xml - Features e permissoes
Write-Host ""
Write-Host "[3] Verificando AndroidManifest.xml..." -ForegroundColor Yellow
$manifest = Get-Content "app\src\main\AndroidManifest.xml" -Raw -ErrorAction SilentlyContinue

if (-not $manifest) {
    $issues += "AndroidManifest.xml nao encontrado!"
    Write-Host "  X Arquivo nao encontrado" -ForegroundColor Red
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar LEANBACK_LAUNCHER (necessario para TV)
    if ($manifest -match "LEANBACK_LAUNCHER") {
        Write-Host "  OK LEANBACK_LAUNCHER encontrado (necessario para TV)" -ForegroundColor Green
    } else {
        $issues += "AndroidManifest.xml: LEANBACK_LAUNCHER nao encontrado (necessario para TV)"
        Write-Host "  X LEANBACK_LAUNCHER nao encontrado" -ForegroundColor Red
    }
    
    # Verificar leanback feature
    if ($manifest -match "android.software.leanback") {
        Write-Host "  OK Feature leanback encontrada" -ForegroundColor Green
    } else {
        $warnings += "AndroidManifest.xml: Feature leanback pode estar faltando"
        Write-Host "  ! Feature leanback pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar touchscreen (deve ser opcional para TV)
    if ($manifest -match "android.hardware.touchscreen.*required.*false") {
        Write-Host "  OK Touchscreen opcional (correto para TV)" -ForegroundColor Green
    } else {
        $warnings += "AndroidManifest.xml: Touchscreen pode estar obrigatorio (deve ser opcional para TV)"
        Write-Host "  ! Touchscreen pode estar obrigatorio" -ForegroundColor Yellow
    }
    
    # Verificar gamepad (D-pad para Fire Stick)
    if ($manifest -match "android.hardware.gamepad") {
        Write-Host "  OK Gamepad/D-pad suportado" -ForegroundColor Green
    } else {
        $warnings += "AndroidManifest.xml: Gamepad/D-pad pode estar faltando"
        Write-Host "  ! Gamepad/D-pad pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar permissoes necessarias
    $requiredPermissions = @(
        "INTERNET",
        "ACCESS_NETWORK_STATE",
        "REQUEST_INSTALL_PACKAGES"
    )
    
    foreach ($perm in $requiredPermissions) {
        if ($manifest -match $perm) {
            Write-Host "  OK Permissao $perm encontrada" -ForegroundColor Green
        } else {
            $issues += "AndroidManifest.xml: Permissao $perm nao encontrada"
            Write-Host "  X Permissao $perm nao encontrada" -ForegroundColor Red
        }
    }
}

# 4. Verificar MainActivity.kt - Configuracoes de TV
Write-Host ""
Write-Host "[4] Verificando MainActivity.kt (configuracoes TV)..." -ForegroundColor Yellow
$mainActivity = Get-Content "app\src\main\java\com\maxiptv\MainActivity.kt" -Raw -ErrorAction SilentlyContinue

if (-not $mainActivity) {
    $warnings += "MainActivity.kt nao encontrado ou nao pode ser lido"
    Write-Host "  ! Arquivo nao encontrado" -ForegroundColor Yellow
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar configuracao de TV
    if ($mainActivity -match "isTv|MaxiApp\.isTv") {
        Write-Host "  OK Configuracao TV encontrada" -ForegroundColor Green
    } else {
        $warnings += "MainActivity.kt: Configuracao TV pode estar faltando"
        Write-Host "  ! Configuracao TV pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar WindowInsetsController (para ocultar status bar)
    if ($mainActivity -match "WindowInsetsController|setDecorFitsSystemWindows") {
        Write-Host "  OK WindowInsetsController encontrado" -ForegroundColor Green
    } else {
        $warnings += "MainActivity.kt: WindowInsetsController pode estar faltando"
        Write-Host "  ! WindowInsetsController pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar checkPendingDownload (para atualizacoes)
    if ($mainActivity -match "checkPendingDownload|ApkDownloader\.checkPendingDownload") {
        Write-Host "  OK checkPendingDownload encontrado" -ForegroundColor Green
    } else {
        $warnings += "MainActivity.kt: checkPendingDownload pode estar faltando"
        Write-Host "  ! checkPendingDownload pode estar faltando" -ForegroundColor Yellow
    }
}

# 5. Verificar HomeScreen.kt - UI adaptativa
Write-Host ""
Write-Host "[5] Verificando HomeScreen.kt (UI adaptativa)..." -ForegroundColor Yellow
$homeScreen = Get-Content "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt" -Raw -ErrorAction SilentlyContinue

if (-not $homeScreen) {
    $warnings += "HomeScreen.kt nao encontrado ou nao pode ser lido"
    Write-Host "  ! Arquivo nao encontrado" -ForegroundColor Yellow
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar adaptacao por dispositivo
    if ($homeScreen -match "isFireStick|isTv|isPhone|deviceType") {
        Write-Host "  OK Adaptacao por dispositivo encontrada" -ForegroundColor Green
    } else {
        $warnings += "HomeScreen.kt: Adaptacao por dispositivo pode estar faltando"
        Write-Host "  ! Adaptacao por dispositivo pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar CategoryButton com deviceType
    if ($homeScreen -match "fun CategoryButton.*deviceType|deviceType.*String|when.*deviceType") {
        Write-Host "  OK CategoryButton adaptativo encontrado" -ForegroundColor Green
    } else {
        $warnings += "HomeScreen.kt: CategoryButton pode nao estar adaptativo"
        Write-Host "  ! CategoryButton pode nao estar adaptativo" -ForegroundColor Yellow
    }
}

# 6. Verificar PlayerActivity.kt - Player para TV
Write-Host ""
Write-Host "[6] Verificando PlayerActivity.kt (player TV)..." -ForegroundColor Yellow
$playerActivity = Get-Content "app\src\main\java\com\maxiptv\ui\player\PlayerActivity.kt" -Raw -ErrorAction SilentlyContinue

if (-not $playerActivity) {
    $warnings += "PlayerActivity.kt nao encontrado ou nao pode ser lido"
    Write-Host "  ! Arquivo nao encontrado" -ForegroundColor Yellow
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar WindowInsetsController no player
    if ($playerActivity -match "WindowInsetsController|setDecorFitsSystemWindows") {
        Write-Host "  OK WindowInsetsController no player encontrado" -ForegroundColor Green
    } else {
        $warnings += "PlayerActivity.kt: WindowInsetsController pode estar faltando"
        Write-Host "  ! WindowInsetsController pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar suporte a Picture-in-Picture (PiP)
    if ($manifest -match "supportsPictureInPicture|PICTURE_IN_PICTURE") {
        Write-Host "  OK Picture-in-Picture suportado" -ForegroundColor Green
    } else {
        Write-Host "  - Picture-in-Picture nao encontrado (opcional)" -ForegroundColor Gray
    }
}

# 7. Verificar build.gradle.kts - Dependencias TV
Write-Host ""
Write-Host "[7] Verificando build.gradle.kts (dependencias TV)..." -ForegroundColor Yellow
$buildGradle = Get-Content "app\build.gradle.kts" -Raw -ErrorAction SilentlyContinue

if (-not $buildGradle) {
    $issues += "build.gradle.kts nao encontrado!"
    Write-Host "  X Arquivo nao encontrado" -ForegroundColor Red
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar androidx.tv (necessario para TV)
    if ($buildGradle -match "androidx\.tv") {
        Write-Host "  OK androidx.tv encontrado (necessario para TV)" -ForegroundColor Green
    } else {
        $issues += "build.gradle.kts: androidx.tv nao encontrado (necessario para TV)"
        Write-Host "  X androidx.tv nao encontrado" -ForegroundColor Red
    }
    
    # Verificar Compose (necessario)
    if ($buildGradle -match "androidx\.compose") {
        Write-Host "  OK Compose encontrado" -ForegroundColor Green
    } else {
        $issues += "build.gradle.kts: Compose nao encontrado"
        Write-Host "  X Compose nao encontrado" -ForegroundColor Red
    }
    
    # Verificar Navigation Compose
    if ($buildGradle -match "navigation-compose") {
        Write-Host "  OK Navigation Compose encontrado" -ForegroundColor Green
    } else {
        $issues += "build.gradle.kts: Navigation Compose nao encontrado"
        Write-Host "  X Navigation Compose nao encontrado" -ForegroundColor Red
    }
    
    # Verificar minSdk
    if ($buildGradle -match "minSdk\s*=\s*(\d+)") {
        $minSdk = [int]$matches[1]
        Write-Host "  OK minSdk: $minSdk" -ForegroundColor Green
        
        if ($minSdk -lt 21) {
            $warnings += "build.gradle.kts: minSdk $minSdk pode ser muito baixo (recomendado 21+)"
            Write-Host "  ! minSdk pode ser muito baixo" -ForegroundColor Yellow
        }
    }
}

# 8. Verificar SafeAreaAutoDetector.kt
Write-Host ""
Write-Host "[8] Verificando SafeAreaAutoDetector.kt..." -ForegroundColor Yellow
$autoDetector = Get-Content "app\src\main\java\com\maxiptv\ui\theme\SafeAreaAutoDetector.kt" -Raw -ErrorAction SilentlyContinue

if (-not $autoDetector) {
    $warnings += "SafeAreaAutoDetector.kt nao encontrado"
    Write-Host "  ! Arquivo nao encontrado" -ForegroundColor Yellow
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar suporte a diferentes tamanhos de TV
    if ($autoDetector -match "32|40|55|inches|polegadas") {
        Write-Host "  OK Suporte a diferentes tamanhos de TV encontrado" -ForegroundColor Green
    } else {
        $warnings += "SafeAreaAutoDetector.kt: Suporte a diferentes tamanhos pode estar faltando"
        Write-Host "  ! Suporte a diferentes tamanhos pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar deteccao automatica
    if ($autoDetector -match "detectAndApplySettings|calculateDiagonalInches") {
        Write-Host "  OK Deteccao automatica encontrada" -ForegroundColor Green
    } else {
        $warnings += "SafeAreaAutoDetector.kt: Deteccao automatica pode estar faltando"
        Write-Host "  ! Deteccao automatica pode estar faltando" -ForegroundColor Yellow
    }
}

# 9. Verificar ApkDownloader.kt - Atualizacoes Fire OS
Write-Host ""
Write-Host "[9] Verificando ApkDownloader.kt (atualizacoes Fire OS)..." -ForegroundColor Yellow
$apkDownloader = Get-Content "app\src\main\java\com\maxiptv\data\ApkDownloader.kt" -Raw -ErrorAction SilentlyContinue

if (-not $apkDownloader) {
    $warnings += "ApkDownloader.kt nao encontrado"
    Write-Host "  ! Arquivo nao encontrado" -ForegroundColor Yellow
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar tratamento Fire OS
    if ($apkDownloader -match "isFireOS|Fire OS|Fire Stick") {
        Write-Host "  OK Tratamento Fire OS encontrado" -ForegroundColor Green
    } else {
        $warnings += "ApkDownloader.kt: Tratamento Fire OS pode estar faltando"
        Write-Host "  ! Tratamento Fire OS pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar ApplicationContext (importante para Fire OS)
    if ($apkDownloader -match "applicationContext|ApplicationContext") {
        Write-Host "  OK ApplicationContext usado (correto para Fire OS)" -ForegroundColor Green
    } else {
        $warnings += "ApkDownloader.kt: ApplicationContext pode nao estar sendo usado"
        Write-Host "  ! ApplicationContext pode nao estar sendo usado" -ForegroundColor Yellow
    }
}

# Resumo
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($issues.Count -gt 0) {
    Write-Host "PROBLEMAS CRITICOS ENCONTRADOS ($($issues.Count)):" -ForegroundColor Red
    Write-Host ""
    foreach ($issue in $issues) {
        Write-Host "  X $issue" -ForegroundColor Red
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "AVISOS ($($warnings.Count)):" -ForegroundColor Yellow
    Write-Host ""
    foreach ($warning in $warnings) {
        Write-Host "  ! $warning" -ForegroundColor Yellow
    }
    Write-Host ""
}

# Checklist de compatibilidade
Write-Host "CHECKLIST DE COMPATIBILIDADE:" -ForegroundColor Magenta
Write-Host ""
Write-Host "Fire Stick Amazon:" -ForegroundColor White
Write-Host "  [$(if ($maxiApp -match 'isFireStick') { 'X' } else { ' ' })] Deteccao implementada" -ForegroundColor $(if ($maxiApp -match 'isFireStick') { 'Green' } else { 'Red' })
Write-Host "  [$(if ($safeArea -match 'isFireStick') { 'X' } else { ' ' })] SafeArea configurado" -ForegroundColor $(if ($safeArea -match 'isFireStick') { 'Green' } else { 'Red' })
Write-Host "  [$(if ($apkDownloader -match 'isFireOS') { 'X' } else { ' ' })] Atualizacoes funcionando" -ForegroundColor $(if ($apkDownloader -match 'isFireOS') { 'Green' } else { 'Red' })
Write-Host ""
Write-Host "TV Box Android:" -ForegroundColor White
Write-Host "  [$(if ($maxiApp -match 'isTvBox') { 'X' } else { ' ' })] Deteccao implementada" -ForegroundColor $(if ($maxiApp -match 'isTvBox') { 'Green' } else { 'Red' })
Write-Host "  [$(if ($manifest -match 'LEANBACK_LAUNCHER') { 'X' } else { ' ' })] Leanback launcher" -ForegroundColor $(if ($manifest -match 'LEANBACK_LAUNCHER') { 'Green' } else { 'Red' })
Write-Host ""
Write-Host "TVs Android Nativas:" -ForegroundColor White
Write-Host "  [$(if ($maxiApp -match 'isNativeTv') { 'X' } else { ' ' })] Deteccao implementada" -ForegroundColor $(if ($maxiApp -match 'isNativeTv') { 'Green' } else { 'Red' })
Write-Host ""
Write-Host "Smartphones:" -ForegroundColor White
Write-Host "  [$(if ($maxiApp -match 'isPhone') { 'X' } else { ' ' })] Deteccao implementada" -ForegroundColor $(if ($maxiApp -match 'isPhone') { 'Green' } else { 'Red' })
Write-Host "  [$(if ($homeScreen -match 'deviceType') { 'X' } else { ' ' })] UI adaptativa" -ForegroundColor $(if ($homeScreen -match 'deviceType') { 'Green' } else { 'Red' })
Write-Host ""
Write-Host "Tablets:" -ForegroundColor White
Write-Host "  [$(if ($maxiApp -match 'isTablet') { 'X' } else { ' ' })] Deteccao implementada" -ForegroundColor $(if ($maxiApp -match 'isTablet') { 'Green' } else { 'Red' })
Write-Host ""

if ($issues.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "PARABENS! O app esta completo para todos os dispositivos!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Todos os dispositivos suportados tem configuracao adequada." -ForegroundColor Green
    exit 0
} elseif ($issues.Count -eq 0) {
    Write-Host "App esta funcional, mas ha avisos menores." -ForegroundColor Yellow
    Write-Host "Recomendado corrigir avisos para melhor compatibilidade." -ForegroundColor Yellow
    exit 0
} else {
    Write-Host "ATENCAO: Corrija os problemas criticos antes de compilar!" -ForegroundColor Red
    exit 1
}


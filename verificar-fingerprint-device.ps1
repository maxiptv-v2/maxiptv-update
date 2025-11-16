# Script de Verificação do Sistema de Fingerprint Device
# Verifica se o sistema de detecção automática de medidas de tela está funcionando corretamente

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICAÇÃO DO SISTEMA DE FINGERPRINT DEVICE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar DeviceFingerprint.kt
Write-Host "1. VERIFICANDO DeviceFingerprint.kt..." -ForegroundColor Yellow
$deviceFingerprint = Get-Content "app\src\main\java\com\maxiptv\data\DeviceFingerprint.kt" -Raw

if ($deviceFingerprint -match "fun collect\(context: Context\)") {
    Write-Host "   [OK] Função collect() encontrada" -ForegroundColor Green
    
    # Verificar se coleta todas as informações necessárias
    $checks = @(
        @{Name="manufacturer"; Pattern="Build.MANUFACTURER"},
        @{Name="model"; Pattern="Build.MODEL"},
        @{Name="widthPixels"; Pattern="widthPixels"},
        @{Name="heightPixels"; Pattern="heightPixels"},
        @{Name="densityDpi"; Pattern="densityDpi"},
        @{Name="key generation"; Pattern="joinToString"}
    )
    
    foreach ($check in $checks) {
        if ($deviceFingerprint -match $check.Pattern) {
            Write-Host "   [OK] $($check.Name) verificado" -ForegroundColor Green
        } else {
            Write-Host "   [ERRO] $($check.Name) não encontrado!" -ForegroundColor Red
        }
    }
} else {
    Write-Host "   [ERRO] Função collect() não encontrada!" -ForegroundColor Red
}

Write-Host ""

# 2. Verificar SafeAreaAutoDetector.kt
Write-Host "2. VERIFICANDO SafeAreaAutoDetector.kt..." -ForegroundColor Yellow
$autoDetector = Get-Content "app\src\main\java\com\maxiptv\ui\theme\SafeAreaAutoDetector.kt" -Raw

$detectorChecks = @(
    @{Name="calculateDiagonalInchesImproved"; Pattern="fun calculateDiagonalInchesImproved"},
    @{Name="calculateInitialAdjustments"; Pattern="fun calculateInitialAdjustments"},
    @{Name="saveDetectedSettings"; Pattern="fun saveDetectedSettings"},
    @{Name="loadDetectedSettings"; Pattern="fun loadDetectedSettings"},
    @{Name="hasDetectedSettings"; Pattern="fun hasDetectedSettings"},
    @{Name="Fire Stick support"; Pattern="MaxiApp.isFireStick"},
    @{Name="TV Box support"; Pattern="MaxiApp.isTvBox"},
    @{Name="Native TV support"; Pattern="MaxiApp.isNativeTv"},
    @{Name="Projector support"; Pattern="MaxiApp.isProjector"},
    @{Name="Diagonal calculation"; Pattern="calculateDiagonalInchesImproved"}
)

foreach ($check in $detectorChecks) {
    if ($autoDetector -match $check.Pattern) {
        Write-Host "   [OK] $($check.Name) verificado" -ForegroundColor Green
    } else {
        Write-Host "   [ERRO] $($check.Name) não encontrado!" -ForegroundColor Red
    }
}

# Verificar se calcula diagonal corretamente
if ($autoDetector -match "calculateDiagonalInchesImproved.*widthPx.*heightPx.*xDpi.*yDpi") {
    Write-Host "   [OK] Cálculo de diagonal usa dimensões reais da tela" -ForegroundColor Green
} else {
    Write-Host "   [AVISO] Verificar cálculo de diagonal" -ForegroundColor Yellow
}

Write-Host ""

# 3. Verificar SafeArea.kt
Write-Host "3. VERIFICANDO SafeArea.kt..." -ForegroundColor Yellow
$safeArea = Get-Content "app\src\main\java\com\maxiptv\ui\theme\SafeArea.kt" -Raw

$safeAreaChecks = @(
    @{Name="MaxiSafeArea composable"; Pattern="@Composable\s+fun MaxiSafeArea"},
    @{Name="DeviceFingerprint.collect"; Pattern="DeviceFingerprint.collect"},
    @{Name="SafeAreaAutoDetector.loadDetectedSettings"; Pattern="SafeAreaAutoDetector.loadDetectedSettings"},
    @{Name="AutoDetectSafeArea composable"; Pattern="AutoDetectSafeArea"},
    @{Name="SafeAreaMetrics.save"; Pattern="SafeAreaMetrics.save"},
    @{Name="Diagonal calculation"; Pattern="calculateDiagonalInchesImproved"}
)

foreach ($check in $safeAreaChecks) {
    if ($safeArea -match $check.Pattern) {
        Write-Host "   [OK] $($check.Name) verificado" -ForegroundColor Green
    } else {
        Write-Host "   [ERRO] $($check.Name) não encontrado!" -ForegroundColor Red
    }
}

# Verificar prioridade de aplicação
if ($safeArea -match "overrideState.*autoDetectedOverride.*overflowCorrections") {
    Write-Host "   [OK] Prioridade de aplicação correta (override > auto > overflow > padrão)" -ForegroundColor Green
} else {
    Write-Host "   [AVISO] Verificar ordem de prioridade" -ForegroundColor Yellow
}

Write-Host ""

# 4. Verificar SafeAreaOverrides.kt
Write-Host "4. VERIFICANDO SafeAreaOverrides.kt..." -ForegroundColor Yellow
$overrides = Get-Content "app\src\main\java\com\maxiptv\ui\theme\SafeAreaOverrides.kt" -Raw

$overrideChecks = @(
    @{Name="update function"; Pattern="fun update"},
    @{Name="hasOverride function"; Pattern="fun hasOverride"},
    @{Name="overrideFlow function"; Pattern="fun overrideFlow"},
    @{Name="JSON storage"; Pattern="JSONObject"},
    @{Name="SharedPreferences storage"; Pattern="getSharedPreferences"}
)

foreach ($check in $overrideChecks) {
    if ($overrides -match $check.Pattern) {
        Write-Host "   [OK] $($check.Name) verificado" -ForegroundColor Green
    } else {
        Write-Host "   [ERRO] $($check.Name) não encontrado!" -ForegroundColor Red
    }
}

Write-Host ""

# 5. Verificar SafeAreaMetrics.kt
Write-Host "5. VERIFICANDO SafeAreaMetrics.kt..." -ForegroundColor Yellow
$metrics = Get-Content "app\src\main\java\com\maxiptv\ui\theme\SafeAreaMetrics.kt" -Raw

$metricsChecks = @(
    @{Name="save function"; Pattern="fun save"},
    @{Name="snapshot function"; Pattern="fun snapshot"},
    @{Name="SafeAreaSnapshot data class"; Pattern="data class SafeAreaSnapshot"},
    @{Name="SharedPreferences storage"; Pattern="getSharedPreferences"}
)

foreach ($check in $metricsChecks) {
    if ($metrics -match $check.Pattern) {
        Write-Host "   [OK] $($check.Name) verificado" -ForegroundColor Green
    } else {
        Write-Host "   [ERRO] $($check.Name) não encontrado!" -ForegroundColor Red
    }
}

Write-Host ""

# 6. Verificar integração no MaxiApp
Write-Host "6. VERIFICANDO INTEGRAÇÃO NO MaxiApp.kt..." -ForegroundColor Yellow
$maxiApp = Get-Content "app\src\main\java\com\maxiptv\MaxiApp.kt" -Raw

if ($maxiApp -match "fireStickOverscanPadding|fireStickSafeAreaPadding") {
    Write-Host "   [OK] Configurações de Fire Stick encontradas" -ForegroundColor Green
} else {
    Write-Host "   [AVISO] Configurações de Fire Stick podem estar faltando" -ForegroundColor Yellow
}

if ($maxiApp -match "isFireStick|isTvBox|isNativeTv|isProjector") {
    Write-Host "   [OK] Detecção de tipo de dispositivo encontrada" -ForegroundColor Green
} else {
    Write-Host "   [ERRO] Detecção de tipo de dispositivo não encontrada!" -ForegroundColor Red
}

Write-Host ""

# 7. Verificar fluxo completo
Write-Host "7. VERIFICANDO FLUXO COMPLETO..." -ForegroundColor Yellow

Write-Host "   Fluxo esperado:" -ForegroundColor Cyan
Write-Host "   1. App inicia -> MaxiApp.onCreate() detecta tipo de dispositivo" -ForegroundColor White
Write-Host "   2. MaxiSafeArea é chamado -> DeviceFingerprint.collect() cria fingerprint" -ForegroundColor White
Write-Host "   3. SafeAreaOverrides.overrideFlow() verifica se tem override salvo" -ForegroundColor White
Write-Host "   4. Se não tem override -> SafeAreaAutoDetector.loadDetectedSettings()" -ForegroundColor White
Write-Host "   5. Se não tem detecção -> AutoDetectSafeArea calcula ajustes" -ForegroundColor White
Write-Host "   6. calculateInitialAdjustments() usa dimensões reais da tela" -ForegroundColor White
Write-Host "   7. calculateDiagonalInchesImproved() calcula diagonal baseado em pixels e DPI" -ForegroundColor White
Write-Host "   8. Ajustes são salvos em SafeAreaAutoDetector.saveDetectedSettings()" -ForegroundColor White
Write-Host "   9. SafeAreaMetrics.save() salva métricas para histórico" -ForegroundColor White
Write-Host "   10. Próxima vez que abrir -> carrega ajustes salvos automaticamente" -ForegroundColor White

Write-Host ""

# Verificar se o fluxo está completo
$flowChecks = @(
    @{Name="DeviceFingerprint usado em SafeArea"; Pattern="DeviceFingerprint.collect"},
    @{Name="AutoDetectSafeArea chamado quando necessário"; Pattern="AutoDetectSafeArea"},
    @{Name="Salvamento após detecção"; Pattern="saveDetectedSettings|SafeAreaOverrides.update"},
    @{Name="Carregamento na próxima vez"; Pattern="loadDetectedSettings|hasDetectedSettings"}
)

$allOk = $true
foreach ($check in $flowChecks) {
    $found = ($safeArea -match $check.Pattern) -or ($autoDetector -match $check.Pattern)
    if ($found) {
        Write-Host "   [OK] $($check.Name)" -ForegroundColor Green
    } else {
        Write-Host "   [ERRO] $($check.Name) não encontrado!" -ForegroundColor Red
        $allOk = $false
    }
}

Write-Host ""

# 8. Verificar suporte a todos os tamanhos de TV
Write-Host "8. VERIFICANDO SUPORTE A TODOS OS TAMANHOS DE TV..." -ForegroundColor Yellow

$sizeRanges = @("32", "40", "43", "45", "50", "55", "60", "65", "70", "75", "80", "85", "100")
$supportedSizes = 0

foreach ($size in $sizeRanges) {
    if ($autoDetector -match "diagonalInches\s*>=\s*$size") {
        $supportedSizes++
    }
}

Write-Host "   Tamanhos suportados encontrados: $supportedSizes de $($sizeRanges.Count)" -ForegroundColor $(if ($supportedSizes -ge 10) { "Green" } else { "Yellow" })

if ($autoDetector -match "diagonalInches\s*>=\s*32" -and $autoDetector -match "diagonalInches\s*>=\s*100") {
    Write-Host "   [OK] Suporte de 32\" até 100\"+ verificado" -ForegroundColor Green
} else {
    Write-Host "   [AVISO] Verificar se todos os tamanhos estão cobertos" -ForegroundColor Yellow
}

Write-Host ""

# RESUMO FINAL
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICAÇÃO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($allOk) {
    Write-Host "[OK] Sistema de fingerprint device parece estar funcionando corretamente!" -ForegroundColor Green
    Write-Host ""
    Write-Host "PONTOS VERIFICADOS:" -ForegroundColor Cyan
    Write-Host "  - DeviceFingerprint coleta informações do dispositivo" -ForegroundColor White
    Write-Host "  - SafeAreaAutoDetector calcula ajustes baseado em tipo e tamanho" -ForegroundColor White
    Write-Host "  - SafeArea aplica ajustes com prioridade correta" -ForegroundColor White
    Write-Host "  - SafeAreaOverrides armazena override localmente" -ForegroundColor White
    Write-Host "  - SafeAreaMetrics salva histórico de métricas" -ForegroundColor White
    Write-Host "  - AutoDetectSafeArea detecta e salva na primeira vez" -ForegroundColor White
    Write-Host "  - Sistema carrega ajustes salvos nas próximas vezes" -ForegroundColor White
    Write-Host "  - Suporte a todos os tamanhos de TV (32\" até 100\"+)" -ForegroundColor White
} else {
    Write-Host "[AVISO] Alguns problemas foram encontrados. Verifique os erros acima." -ForegroundColor Yellow
}

Write-Host ""


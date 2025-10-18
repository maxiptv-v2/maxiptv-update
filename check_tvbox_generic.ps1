# Script de Verificacao para TV Box Genericas
# Verifica deteccao automatica e prevencao de overscan

Write-Host "VERIFICACAO TV BOX GENERICAS" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan
Write-Host "Detecao automatica + Prevencao Overscan" -ForegroundColor White

# Funcao para verificar arquivo
function Test-File {
    param([string]$Path, [string]$Description)
    
    if (Test-Path $Path) {
        Write-Host "OK $Description" -ForegroundColor Green
        return $true
    } else {
        Write-Host "ERRO $Description" -ForegroundColor Red
        return $false
    }
}

# Funcao para verificar conteudo de arquivo
function Test-FileContent {
    param([string]$Path, [string]$Pattern, [string]$Description)
    
    if (Test-Path $Path) {
        $content = Get-Content $Path -Raw
        if ($content -match $Pattern) {
            Write-Host "OK $Description" -ForegroundColor Green
            return $true
        } else {
            Write-Host "ERRO $Description" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "ERRO $Description (arquivo nao existe)" -ForegroundColor Red
        return $false
    }
}

# Funcao para verificar dimensao
function Test-Dimension {
    param([string]$Path, [string]$DimenName, [string]$Description)
    
    if (Test-Path $Path) {
        $content = Get-Content $Path -Raw
        if ($content -match "<dimen name=`"$DimenName`"") {
            Write-Host "OK $Description" -ForegroundColor Green
            return $true
        } else {
            Write-Host "ERRO $Description" -ForegroundColor Red
            return $false
        }
    } else {
        Write-Host "ERRO $Description (arquivo nao existe)" -ForegroundColor Red
        return $false
    }
}

Write-Host "`nDETECCAO AUTOMATICA DE TV BOX..." -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Yellow

$detectionOk = $true
$detectionOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "UI_MODE_TYPE_TELEVISION" "UI Mode Television") -and $detectionOk
$detectionOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "UiModeManager" "UiModeManager import") -and $detectionOk
$detectionOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "isTv.*=" "Variavel isTv") -and $detectionOk
$detectionOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "model.contains.*tv" "Detecao por modelo") -and $detectionOk
$detectionOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "product.contains.*atv" "Detecao por produto") -and $detectionOk

Write-Host "`nMANIFEST PARA TV BOX..." -ForegroundColor Yellow
Write-Host "=======================" -ForegroundColor Yellow

$manifestOk = $true
$manifestOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "LEANBACK_LAUNCHER" "Launcher TV") -and $manifestOk
$manifestOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android.software.leanback.*required.*false" "Leanback opcional") -and $manifestOk
$manifestOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android.hardware.touchscreen.*required.*false" "Touchscreen opcional") -and $manifestOk
$manifestOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android:banner" "Banner TV") -and $manifestOk

Write-Host "`nRECURSOS PARA PREVENCAO DE OVERSCAN..." -ForegroundColor Yellow
Write-Host "=======================================" -ForegroundColor Yellow

$overscanOk = $true
$overscanOk = (Test-Dimension "app\src\main\res\values-television\dimens.xml" "tv_safe_margin" "Margem segura TV") -and $overscanOk
$overscanOk = (Test-Dimension "app\src\main\res\values-television\dimens.xml" "padding_screen" "Padding tela") -and $overscanOk
$overscanOk = (Test-Dimension "app\src\main\res\values-television\dimens.xml" "tv_player_height" "Altura player") -and $overscanOk
$overscanOk = (Test-Dimension "app\src\main\res\values-television\dimens.xml" "tv_carousel_height" "Altura carrossel") -and $overscanOk

Write-Host "`nVERIFICACAO DE VALORES DE OVERSCAN..." -ForegroundColor Yellow
Write-Host "======================================" -ForegroundColor Yellow

# Verificar se os valores sao adequados para overscan
if (Test-Path "app\src\main\res\values-television\dimens.xml") {
    $content = Get-Content "app\src\main\res\values-television\dimens.xml" -Raw
    
    # Verificar margem segura (deve ser >= 24dp)
    if ($content -match 'tv_safe_margin.*?>(\d+)dp') {
        $margin = [int]$matches[1]
        if ($margin -ge 24) {
            Write-Host "OK Margem segura adequada: ${margin}dp" -ForegroundColor Green
        } else {
            Write-Host "ERRO Margem segura muito pequena: ${margin}dp (minimo 24dp)" -ForegroundColor Red
            $overscanOk = $false
        }
    } else {
        Write-Host "ERRO Nao encontrou tv_safe_margin" -ForegroundColor Red
        $overscanOk = $false
    }
    
    # Verificar padding tela (deve ser >= 24dp)
    if ($content -match 'padding_screen.*?>(\d+)dp') {
        $padding = [int]$matches[1]
        if ($padding -ge 24) {
            Write-Host "OK Padding tela adequado: ${padding}dp" -ForegroundColor Green
        } else {
            Write-Host "ERRO Padding tela muito pequeno: ${padding}dp (minimo 24dp)" -ForegroundColor Red
            $overscanOk = $false
        }
    } else {
        Write-Host "ERRO Nao encontrou padding_screen" -ForegroundColor Red
        $overscanOk = $false
    }
    
    # Verificar altura do player (deve ser >= 300dp para TV)
    if ($content -match 'tv_player_height.*?>(\d+)dp') {
        $playerHeight = [int]$matches[1]
        if ($playerHeight -ge 300) {
            Write-Host "OK Altura player adequada: ${playerHeight}dp" -ForegroundColor Green
        } else {
            Write-Host "ERRO Altura player muito pequena: ${playerHeight}dp (minimo 300dp)" -ForegroundColor Red
            $overscanOk = $false
        }
    } else {
        Write-Host "ERRO Nao encontrou tv_player_height" -ForegroundColor Red
        $overscanOk = $false
    }
}

Write-Host "`nVERIFICACAO DE CODIGO DE APLICACAO..." -ForegroundColor Yellow
Write-Host "=====================================" -ForegroundColor Yellow

$codeOk = $true
$codeOk = (Test-FileContent "app\src\main\java\com\maxiptv\MainActivity.kt" "MaxiApp.isTv" "Uso da variavel isTv") -and $codeOk
$codeOk = (Test-FileContent "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt" "MaxiApp.isTv" "Uso da variavel isTv na HomeScreen") -and $codeOk

Write-Host "`nRESUMO DA VERIFICACAO:" -ForegroundColor Magenta
Write-Host "======================" -ForegroundColor Magenta

if ($detectionOk) {
    Write-Host "OK Deteccao automatica: FUNCIONANDO" -ForegroundColor Green
} else {
    Write-Host "ERRO Deteccao automatica: PROBLEMAS" -ForegroundColor Red
}

if ($manifestOk) {
    Write-Host "OK Manifest TV: FUNCIONANDO" -ForegroundColor Green
} else {
    Write-Host "ERRO Manifest TV: PROBLEMAS" -ForegroundColor Red
}

if ($overscanOk) {
    Write-Host "OK Prevencao overscan: FUNCIONANDO" -ForegroundColor Green
} else {
    Write-Host "ERRO Prevencao overscan: PROBLEMAS" -ForegroundColor Red
}

if ($codeOk) {
    Write-Host "OK Codigo de aplicacao: FUNCIONANDO" -ForegroundColor Green
} else {
    Write-Host "ERRO Codigo de aplicacao: PROBLEMAS" -ForegroundColor Red
}

Write-Host "`nCHECKLIST PARA TV BOX GENERICAS:" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

Write-Host "1. DETECCAO AUTOMATICA:" -ForegroundColor White
Write-Host "   - UiModeManager.currentModeType == UI_MODE_TYPE_TELEVISION" -ForegroundColor Gray
Write-Host "   - Verificacao de manufacturer/model/product" -ForegroundColor Gray
Write-Host "   - Variavel isTv global" -ForegroundColor Gray
Write-Host "   - Logs de debug para identificar dispositivo" -ForegroundColor Gray

Write-Host "`n2. MANIFEST OBRIGATORIO:" -ForegroundColor White
Write-Host "   - android.intent.category.LEANBACK_LAUNCHER" -ForegroundColor Gray
Write-Host "   - android.software.leanback required=false" -ForegroundColor Gray
Write-Host "   - android.hardware.touchscreen required=false" -ForegroundColor Gray
Write-Host "   - android:banner para TV launcher" -ForegroundColor Gray

Write-Host "`n3. PREVENCAO DE OVERSCAN:" -ForegroundColor White
Write-Host "   - tv_safe_margin >= 24dp (margem segura)" -ForegroundColor Gray
Write-Host "   - padding_screen >= 24dp (padding geral)" -ForegroundColor Gray
Write-Host "   - tv_player_height >= 300dp (player visivel)" -ForegroundColor Gray
Write-Host "   - tv_carousel_height adequado" -ForegroundColor Gray

Write-Host "`n4. CODIGO DE APLICACAO:" -ForegroundColor White
Write-Host "   - Usar MaxiApp.isTv para condicionais" -ForegroundColor Gray
Write-Host "   - Aplicar dimensoes TV quando isTv = true" -ForegroundColor Gray
Write-Host "   - Usar tv_safe_margin para evitar overscan" -ForegroundColor Gray

Write-Host "`n5. RECURSOS NECESSARIOS:" -ForegroundColor White
Write-Host "   - values-television/dimens.xml (dimensoes TV)" -ForegroundColor Gray
Write-Host "   - values/dimens.xml (dimensoes padrao)" -ForegroundColor Gray
Write-Host "   - ic_launcher.png em todas as densidades" -ForegroundColor Gray

Write-Host "`nRECOMENDACOES ESPECIFICAS:" -ForegroundColor Yellow
Write-Host "===========================" -ForegroundColor Yellow

Write-Host "Para TV Box genericas:" -ForegroundColor White
Write-Host "- Margem segura: 32dp (recomendado)" -ForegroundColor Gray
Write-Host "- Padding tela: 32dp (recomendado)" -ForegroundColor Gray
Write-Host "- Altura player: 380dp (recomendado)" -ForegroundColor Gray
Write-Host "- Textos maiores: 28sp titulo, 20sp corpo" -ForegroundColor Gray
Write-Host "- Deteccao por UI_MODE_TYPE_TELEVISION" -ForegroundColor Gray

Write-Host "`nVerificacao concluida!" -ForegroundColor Green

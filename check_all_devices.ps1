# Script de Verificacao Completa para Todos os Dispositivos
# Verifica compatibilidade com Smartphone, Tablet, Fire Stick, TV Box Android

Write-Host "DISPOSITIVOS SUPORTADOS:" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan
Write-Host "Smartphone Android" -ForegroundColor White
Write-Host "Tablet Android" -ForegroundColor White
Write-Host "Fire Stick" -ForegroundColor White
Write-Host "TV Box Android" -ForegroundColor White
Write-Host "Chromecast" -ForegroundColor White
Write-Host "Android TV" -ForegroundColor White

Write-Host "`nVERIFICACAO GERAL DO PROJETO..." -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Yellow

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

Write-Host "`nVERIFICANDO SMARTPHONE ANDROID..." -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

$smartphoneOk = $true
$smartphoneOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android.intent.category.LAUNCHER" "Launcher para smartphone") -and $smartphoneOk
$smartphoneOk = (Test-Dimension "app\src\main\res\values\dimens.xml" "tv_player_height" "Dimensoes smartphone") -and $smartphoneOk
$smartphoneOk = (Test-File "app\src\main\res\mipmap-mdpi\ic_launcher.png" "Icone MDPI") -and $smartphoneOk
$smartphoneOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "isPhone.*=" "Detecao smartphone") -and $smartphoneOk

Write-Host "`nVERIFICANDO TABLET ANDROID..." -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

$tabletOk = $true
$tabletOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android.intent.category.LAUNCHER" "Launcher para tablet") -and $tabletOk
$tabletOk = (Test-Dimension "app\src\main\res\values\dimens.xml" "tv_player_height" "Dimensoes tablet") -and $tabletOk
$tabletOk = (Test-File "app\src\main\res\mipmap-xhdpi\ic_launcher.png" "Icone XHDPI") -and $tabletOk
$tabletOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "isTablet.*=" "Detecao tablet") -and $tabletOk

Write-Host "`nVERIFICANDO FIRE STICK..." -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan

$firestickOk = $true
$firestickOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "LEANBACK_LAUNCHER" "Launcher TV") -and $firestickOk
$firestickOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android.software.leanback" "Leanback feature") -and $firestickOk
$firestickOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android.hardware.touchscreen.*required.*false" "Touchscreen opcional") -and $firestickOk
$firestickOk = (Test-Dimension "app\src\main\res\values-television\dimens.xml" "tv_player_height" "Dimensoes TV") -and $firestickOk
$firestickOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "manufacturer.contains.*amazon" "Detecao Fire Stick") -and $firestickOk

Write-Host "`nVERIFICANDO TV BOX ANDROID..." -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

$tvboxOk = $true
$tvboxOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "LEANBACK_LAUNCHER" "Launcher TV") -and $tvboxOk
$tvboxOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android:banner" "Banner TV") -and $tvboxOk
$tvboxOk = (Test-Dimension "app\src\main\res\values-television\dimens.xml" "tv_player_height" "Dimensoes TV") -and $tvboxOk
$tvboxOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "UI_MODE_TYPE_TELEVISION" "Detecao TV") -and $tvboxOk

Write-Host "`nVERIFICANDO CHROMECAST..." -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan

$chromecastOk = $true
$chromecastOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "LEANBACK_LAUNCHER" "Launcher TV") -and $chromecastOk
$chromecastOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android:banner" "Banner TV") -and $chromecastOk
$chromecastOk = (Test-Dimension "app\src\main\res\values-television\dimens.xml" "tv_player_height" "Dimensoes TV") -and $chromecastOk
$chromecastOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "chromecast" "Detecao Chromecast") -and $chromecastOk

Write-Host "`nVERIFICANDO ANDROID TV..." -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan

$androidtvOk = $true
$androidtvOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "LEANBACK_LAUNCHER" "Launcher TV") -and $androidtvOk
$androidtvOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android:banner" "Banner TV") -and $androidtvOk
$androidtvOk = (Test-Dimension "app\src\main\res\values-television\dimens.xml" "tv_player_height" "Dimensoes TV") -and $androidtvOk
$androidtvOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "UI_MODE_TYPE_TELEVISION" "Detecao TV") -and $androidtvOk

Write-Host "`nRESUMO COMPLETO:" -ForegroundColor Magenta
Write-Host "===================" -ForegroundColor Magenta

if ($smartphoneOk) {
    Write-Host "OK Smartphone Android: COMPATIVEL" -ForegroundColor Green
} else {
    Write-Host "ERRO Smartphone Android: PROBLEMAS" -ForegroundColor Red
}

if ($tabletOk) {
    Write-Host "OK Tablet Android: COMPATIVEL" -ForegroundColor Green
} else {
    Write-Host "ERRO Tablet Android: PROBLEMAS" -ForegroundColor Red
}

if ($firestickOk) {
    Write-Host "OK Fire Stick: COMPATIVEL" -ForegroundColor Green
} else {
    Write-Host "ERRO Fire Stick: PROBLEMAS" -ForegroundColor Red
}

if ($tvboxOk) {
    Write-Host "OK TV Box Android: COMPATIVEL" -ForegroundColor Green
} else {
    Write-Host "ERRO TV Box Android: PROBLEMAS" -ForegroundColor Red
}

if ($chromecastOk) {
    Write-Host "OK Chromecast: COMPATIVEL" -ForegroundColor Green
} else {
    Write-Host "ERRO Chromecast: PROBLEMAS" -ForegroundColor Red
}

if ($androidtvOk) {
    Write-Host "OK Android TV: COMPATIVEL" -ForegroundColor Green
} else {
    Write-Host "ERRO Android TV: PROBLEMAS" -ForegroundColor Red
}

Write-Host "`nCHECKLIST DE COMPATIBILIDADE:" -ForegroundColor Yellow
Write-Host "=================================" -ForegroundColor Yellow

Write-Host "SMARTPHONE:" -ForegroundColor White
Write-Host "   - android.intent.category.LAUNCHER" -ForegroundColor Gray
Write-Host "   - values/dimens.xml" -ForegroundColor Gray
Write-Host "   - ic_launcher.png (MDPI, HDPI)" -ForegroundColor Gray
Write-Host "   - Detecao: screenWidthDp < 600" -ForegroundColor Gray

Write-Host "`nTABLET:" -ForegroundColor White
Write-Host "   - android.intent.category.LAUNCHER" -ForegroundColor Gray
Write-Host "   - values/dimens.xml" -ForegroundColor Gray
Write-Host "   - ic_launcher.png (XHDPI, XXHDPI)" -ForegroundColor Gray
Write-Host "   - Detecao: screenWidthDp >= 600" -ForegroundColor Gray

Write-Host "`nFIRE STICK:" -ForegroundColor White
Write-Host "   - android.intent.category.LEANBACK_LAUNCHER" -ForegroundColor Gray
Write-Host "   - android.software.leanback required=false" -ForegroundColor Gray
Write-Host "   - android.hardware.touchscreen required=false" -ForegroundColor Gray
Write-Host "   - values-television/dimens.xml" -ForegroundColor Gray
Write-Host "   - Detecao: manufacturer.contains('amazon')" -ForegroundColor Gray

Write-Host "`nTV BOX:" -ForegroundColor White
Write-Host "   - android.intent.category.LEANBACK_LAUNCHER" -ForegroundColor Gray
Write-Host "   - android:banner" -ForegroundColor Gray
Write-Host "   - values-television/dimens.xml" -ForegroundColor Gray
Write-Host "   - Detecao: UI_MODE_TYPE_TELEVISION" -ForegroundColor Gray

Write-Host "`nCHROMECAST:" -ForegroundColor White
Write-Host "   - android.intent.category.LEANBACK_LAUNCHER" -ForegroundColor Gray
Write-Host "   - android:banner" -ForegroundColor Gray
Write-Host "   - values-television/dimens.xml" -ForegroundColor Gray
Write-Host "   - Detecao: model.contains('chromecast')" -ForegroundColor Gray

Write-Host "`nANDROID TV:" -ForegroundColor White
Write-Host "   - android.intent.category.LEANBACK_LAUNCHER" -ForegroundColor Gray
Write-Host "   - android:banner" -ForegroundColor Gray
Write-Host "   - values-television/dimens.xml" -ForegroundColor Gray
Write-Host "   - Detecao: UI_MODE_TYPE_TELEVISION" -ForegroundColor Gray

Write-Host "`nVerificacao concluida!" -ForegroundColor Green
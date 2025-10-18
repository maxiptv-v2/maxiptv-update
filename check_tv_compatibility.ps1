# Script de Verificacao para TV Box Android
# Analisa o que o codigo precisa para reconhecer TV Box corretamente

Write-Host "ANALISADOR DE COMPATIBILIDADE TV BOX" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

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

Write-Host "`nVERIFICANDO MANIFEST..." -ForegroundColor Yellow

# Verificar AndroidManifest.xml
$manifestOk = $true
$manifestOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "LEANBACK_LAUNCHER" "LEANBACK_LAUNCHER no Manifest") -and $manifestOk
$manifestOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android.software.leanback" "leanback feature") -and $manifestOk
$manifestOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android.hardware.touchscreen.*required.*false" "touchscreen required=false") -and $manifestOk
$manifestOk = (Test-FileContent "app\src\main\AndroidManifest.xml" "android:banner" "banner para TV") -and $manifestOk

Write-Host "`nVERIFICANDO RECURSOS..." -ForegroundColor Yellow

# Verificar recursos
$resourcesOk = $true
$resourcesOk = (Test-File "app\src\main\res\values-television\dimens.xml" "Dimensoes para TV") -and $resourcesOk
$resourcesOk = (Test-File "app\src\main\res\values\dimens.xml" "Dimensoes padrao") -and $resourcesOk
$resourcesOk = (Test-File "app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml" "Icone adaptativo") -and $resourcesOk

Write-Host "`nVERIFICANDO ICONES..." -ForegroundColor Yellow

# Verificar icones
$iconsOk = $true
$iconsOk = (Test-File "app\src\main\res\mipmap-mdpi\ic_launcher.png" "Icone MDPI") -and $iconsOk
$iconsOk = (Test-File "app\src\main\res\mipmap-hdpi\ic_launcher.png" "Icone HDPI") -and $iconsOk
$iconsOk = (Test-File "app\src\main\res\mipmap-xhdpi\ic_launcher.png" "Icone XHDPI") -and $iconsOk
$iconsOk = (Test-File "app\src\main\res\mipmap-xxhdpi\ic_launcher.png" "Icone XXHDPI") -and $iconsOk
$iconsOk = (Test-File "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png" "Icone XXXHDPI") -and $iconsOk

Write-Host "`nVERIFICANDO CODIGO..." -ForegroundColor Yellow

# Verificar codigo
$codeOk = $true
$codeOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "UI_MODE_TYPE_TELEVISION" "Deteccao de TV no MaxiApp") -and $codeOk
$codeOk = (Test-FileContent "app\src\main\java\com\maxiptv\MaxiApp.kt" "isTv.*=" "Variavel isTv") -and $codeOk

Write-Host "`nRESUMO DA VERIFICACAO:" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan

if ($manifestOk) {
    Write-Host "OK Manifest: OK" -ForegroundColor Green
} else {
    Write-Host "ERRO Manifest: PROBLEMAS ENCONTRADOS" -ForegroundColor Red
}

if ($resourcesOk) {
    Write-Host "OK Recursos: OK" -ForegroundColor Green
} else {
    Write-Host "ERRO Recursos: PROBLEMAS ENCONTRADOS" -ForegroundColor Red
}

if ($iconsOk) {
    Write-Host "OK Icones: OK" -ForegroundColor Green
} else {
    Write-Host "ERRO Icones: PROBLEMAS ENCONTRADOS" -ForegroundColor Red
}

if ($codeOk) {
    Write-Host "OK Codigo: OK" -ForegroundColor Green
} else {
    Write-Host "ERRO Codigo: PROBLEMAS ENCONTRADOS" -ForegroundColor Red
}

Write-Host "`nRECOMENDACOES PARA TV BOX:" -ForegroundColor Magenta
Write-Host "===============================" -ForegroundColor Magenta

Write-Host "1. Manifest deve ter:" -ForegroundColor White
Write-Host "   - LEANBACK_LAUNCHER intent-filter" -ForegroundColor Gray
Write-Host "   - android.software.leanback required=false" -ForegroundColor Gray
Write-Host "   - android.hardware.touchscreen required=false" -ForegroundColor Gray
Write-Host "   - android:banner para TV launcher" -ForegroundColor Gray

Write-Host "`n2. Recursos necessarios:" -ForegroundColor White
Write-Host "   - values-television/dimens.xml (dimensoes maiores)" -ForegroundColor Gray
Write-Host "   - values/dimens.xml (dimensoes padrao)" -ForegroundColor Gray
Write-Host "   - mipmap-anydpi-v26/ic_launcher.xml (icone adaptativo)" -ForegroundColor Gray

Write-Host "`n3. Icones obrigatorios:" -ForegroundColor White
Write-Host "   - ic_launcher.png em todas as densidades" -ForegroundColor Gray
Write-Host "   - ic_launcher_banner.png para TV" -ForegroundColor Gray

Write-Host "`n4. Codigo de deteccao:" -ForegroundColor White
Write-Host "   - UiModeManager.currentModeType == UI_MODE_TYPE_TELEVISION" -ForegroundColor Gray
Write-Host "   - Verificacao de manufacturer/model" -ForegroundColor Gray
Write-Host "   - Variavel isTv para controle de UI" -ForegroundColor Gray

Write-Host "`nScript concluido!" -ForegroundColor Green
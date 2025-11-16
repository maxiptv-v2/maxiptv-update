# Script para verificar código deprecated no app MaxiPTV

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  VERIFICAÇÃO DE CÓDIGO DEPRECATED" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$deprecatedFound = $false

# Verificar FLAG_FULLSCREEN (deprecated em API 30+)
Write-Host "1. Verificando FLAG_FULLSCREEN..." -ForegroundColor Yellow
$flagFullscreen = Select-String -Path "app\src\main\java\com\maxiptv\**\*.kt" -Pattern "FLAG_FULLSCREEN" -CaseSensitive
if ($flagFullscreen) {
    Write-Host "   ⚠️  FLAG_FULLSCREEN encontrado (deprecated em API 30+)" -ForegroundColor Yellow
    $flagFullscreen | ForEach-Object {
        Write-Host "      Arquivo: $($_.Path):$($_.LineNumber)" -ForegroundColor Gray
        Write-Host "      Linha: $($_.Line)" -ForegroundColor Gray
    }
    $deprecatedFound = $true
} else {
    Write-Host "   ✅ Nenhum uso de FLAG_FULLSCREEN encontrado" -ForegroundColor Green
}

# Verificar systemUiVisibility (deprecated em API 30+)
Write-Host ""
Write-Host "2. Verificando systemUiVisibility..." -ForegroundColor Yellow
$systemUiVisibility = Select-String -Path "app\src\main\java\com\maxiptv\**\*.kt" -Pattern "systemUiVisibility" -CaseSensitive
if ($systemUiVisibility) {
    Write-Host "   ⚠️  systemUiVisibility encontrado (deprecated em API 30+)" -ForegroundColor Yellow
    $systemUiVisibility | ForEach-Object {
        Write-Host "      Arquivo: $($_.Path):$($_.LineNumber)" -ForegroundColor Gray
        Write-Host "      Linha: $($_.Line)" -ForegroundColor Gray
    }
    $deprecatedFound = $true
} else {
    Write-Host "   ✅ Nenhum uso de systemUiVisibility encontrado" -ForegroundColor Green
}

# Verificar GestureDetectorCompat (deprecated)
Write-Host ""
Write-Host "3. Verificando GestureDetectorCompat..." -ForegroundColor Yellow
$gestureDetectorCompat = Select-String -Path "app\src\main\java\com\maxiptv\**\*.kt" -Pattern "GestureDetectorCompat" -CaseSensitive
if ($gestureDetectorCompat) {
    Write-Host "   ⚠️  GestureDetectorCompat encontrado (deprecated)" -ForegroundColor Yellow
    $gestureDetectorCompat | ForEach-Object {
        Write-Host "      Arquivo: $($_.Path):$($_.LineNumber)" -ForegroundColor Gray
        Write-Host "      Linha: $($_.Line)" -ForegroundColor Gray
    }
    $deprecatedFound = $true
} else {
    Write-Host "   ✅ Nenhum uso de GestureDetectorCompat encontrado" -ForegroundColor Green
}

# Verificar métodos @Deprecated internos (apenas marcações, não são problemas)
Write-Host ""
Write-Host "4. Verificando métodos @Deprecated internos..." -ForegroundColor Yellow
$internalDeprecated = Select-String -Path "app\src\main\java\com\maxiptv\**\*.kt" -Pattern "@Deprecated" -CaseSensitive
if ($internalDeprecated) {
    Write-Host "   ℹ️  Métodos @Deprecated internos encontrados (apenas marcações, OK):" -ForegroundColor Cyan
    $internalDeprecated | ForEach-Object {
        Write-Host "      Arquivo: $($_.Path):$($_.LineNumber)" -ForegroundColor Gray
    }
} else {
    Write-Host "   ✅ Nenhum método @Deprecated interno encontrado" -ForegroundColor Green
}

# Verificar uso de APIs antigas do Compose
Write-Host ""
Write-Host "5. Verificando APIs antigas do Compose..." -ForegroundColor Yellow
$oldCompose = Select-String -Path "app\src\main\java\com\maxiptv\**\*.kt" -Pattern "androidx.compose.material\.|MaterialTheme\.colors" -CaseSensitive
if ($oldCompose) {
    Write-Host "   ⚠️  Possíveis APIs antigas do Compose encontradas:" -ForegroundColor Yellow
    $oldCompose | Select-Object -First 5 | ForEach-Object {
        Write-Host "      Arquivo: $($_.Path):$($_.LineNumber)" -ForegroundColor Gray
    }
} else {
    Write-Host "   ✅ Nenhuma API antiga do Compose encontrada" -ForegroundColor Green
}

# Verificar targetSdk e compileSdk
Write-Host ""
Write-Host "6. Verificando configurações do build.gradle.kts..." -ForegroundColor Yellow
$buildGradle = Get-Content "app\build.gradle.kts" -Raw
if ($buildGradle -match "compileSdk\s*=\s*(\d+)") {
    $compileSdk = $matches[1]
    Write-Host "   ✅ compileSdk: $compileSdk" -ForegroundColor Green
}
if ($buildGradle -match "targetSdk\s*=\s*(\d+)") {
    $targetSdk = $matches[1]
    Write-Host "   ✅ targetSdk: $targetSdk" -ForegroundColor Green
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($deprecatedFound) {
    Write-Host "  ⚠️  CÓDIGO DEPRECATED ENCONTRADO" -ForegroundColor Yellow
    Write-Host "  Verifique os itens acima e atualize se necessário" -ForegroundColor Yellow
} else {
    Write-Host "  ✅ NENHUM CÓDIGO DEPRECATED CRÍTICO ENCONTRADO" -ForegroundColor Green
    Write-Host "  O app está usando APIs modernas!" -ForegroundColor Green
}
Write-Host "========================================" -ForegroundColor Cyan

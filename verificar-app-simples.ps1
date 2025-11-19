Write-Host "VERIFICACAO COMPLETA DO APP" -ForegroundColor Cyan
Write-Host ""

# Verificar arquivos principais
Write-Host "1. Arquivos principais:" -ForegroundColor Yellow
$files = @(
    "app/src/main/java/com/maxiptv/MainActivity.kt",
    "app/src/main/java/com/maxiptv/MaxiApp.kt",
    "app/src/main/java/com/maxiptv/ui/screens/HomeScreen.kt",
    "app/src/main/java/com/maxiptv/ui/player/PlayerActivity.kt"
)

foreach ($file in $files) {
    if (Test-Path $file) {
        Write-Host "   OK: $file" -ForegroundColor Green
    } else {
        Write-Host "   FALTANDO: $file" -ForegroundColor Red
    }
}

Write-Host ""

# Verificar versao
Write-Host "2. Versao:" -ForegroundColor Yellow
if (Test-Path "version.json") {
    $version = Get-Content "version.json" | ConvertFrom-Json
    Write-Host "   Versao: $($version.version) (Build $($version.build))" -ForegroundColor Green
} else {
    Write-Host "   version.json nao encontrado" -ForegroundColor Red
}

Write-Host ""

# Verificar build.gradle.kts
Write-Host "3. Configuracoes do Gradle:" -ForegroundColor Yellow
if (Test-Path "app/build.gradle.kts") {
    $gradle = Get-Content "app/build.gradle.kts" -Raw
    
    if ($gradle -match "minSdk\s*=\s*(\d+)") {
        Write-Host "   minSdk: $($matches[1])" -ForegroundColor Green
    }
    
    if ($gradle -match "targetSdk\s*=\s*(\d+)") {
        Write-Host "   targetSdk: $($matches[1])" -ForegroundColor Green
    }
    
    if ($gradle -match "signingConfig") {
        Write-Host "   Signing config: OK" -ForegroundColor Green
    }
} else {
    Write-Host "   build.gradle.kts nao encontrado" -ForegroundColor Red
}

Write-Host ""

# Verificar dependencias
Write-Host "4. Dependencias principais:" -ForegroundColor Yellow
if (Test-Path "app/build.gradle.kts") {
    $gradle = Get-Content "app/build.gradle.kts" -Raw
    
    $deps = @("compose", "media3", "coil", "coroutines", "okhttp")
    foreach ($dep in $deps) {
        if ($gradle -match $dep) {
            Write-Host "   OK: $dep" -ForegroundColor Green
        } else {
            Write-Host "   FALTANDO: $dep" -ForegroundColor Red
        }
    }
}

Write-Host ""

# Verificar AndroidManifest
Write-Host "5. AndroidManifest:" -ForegroundColor Yellow
if (Test-Path "app/src/main/AndroidManifest.xml") {
    $manifest = Get-Content "app/src/main/AndroidManifest.xml" -Raw
    
    if ($manifest -match "INTERNET") {
        Write-Host "   Permissao INTERNET: OK" -ForegroundColor Green
    }
    
    if ($manifest -match "MainActivity") {
        Write-Host "   MainActivity: OK" -ForegroundColor Green
    }
    
    if ($manifest -match "PlayerActivity") {
        Write-Host "   PlayerActivity: OK" -ForegroundColor Green
    }
} else {
    Write-Host "   AndroidManifest.xml nao encontrado" -ForegroundColor Red
}

Write-Host ""
Write-Host "VERIFICACAO CONCLUIDA" -ForegroundColor Cyan


# Script de Diagnóstico Completo para Compilação do MaxiPTV
# Este script apenas VERIFICA os requisitos, sem modificar nada

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DIAGNÓSTICO COMPLETO - MaxiPTV v2" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$problemas = @()
$avisos = @()

# 1. Verificar Java/JDK
Write-Host "[1] Java/JDK..." -ForegroundColor Yellow
try {
    $javaOutput = java -version 2>&1 | Out-String
    Write-Host "   ✓ Java encontrado" -ForegroundColor Green
    Write-Host "     $javaOutput" -ForegroundColor Gray
} catch {
    Write-Host "   ✗ Java não encontrado" -ForegroundColor Red
    $problemas += "Java JDK não instalado"
}

# 2. Android SDK
Write-Host ""
Write-Host "[2] Android SDK..." -ForegroundColor Yellow
$androidSdkPath = "$env:LOCALAPPDATA\Android\Sdk"
if (Test-Path $androidSdkPath) {
    Write-Host "   ✓ Android SDK: $androidSdkPath" -ForegroundColor Green
    
    $buildToolsPath = Join-Path $androidSdkPath "build-tools"
    if (Test-Path $buildToolsPath) {
        $buildTools = Get-ChildItem $buildToolsPath -Directory | Sort-Object Name -Descending | Select-Object -First 1
        Write-Host "   ✓ Build Tools: $($buildTools.Name)" -ForegroundColor Green
    }
    
    $platformsPath = Join-Path $androidSdkPath "platforms"
    if (Test-Path $platformsPath) {
        $platforms = Get-ChildItem $platformsPath -Directory | Sort-Object Name -Descending
        Write-Host "   ✓ Platforms: $($platforms.Count) disponíveis" -ForegroundColor Green
        $platforms | Select-Object -First 5 | ForEach-Object { Write-Host "     - $($_.Name)" -ForegroundColor Gray }
    }
} else {
    Write-Host "   ✗ Android SDK não encontrado" -ForegroundColor Red
    $problemas += "Android SDK não instalado"
}

# 3. Gradle
Write-Host ""
Write-Host "[3] Gradle..." -ForegroundColor Yellow
$gradleWrapperLocal = ".\gradlew.bat"
if (Test-Path $gradleWrapperLocal) {
    Write-Host "   ✓ Gradle Wrapper encontrado no projeto" -ForegroundColor Green
} else {
    Write-Host "   ✗ Gradle Wrapper NÃO encontrado" -ForegroundColor Red
    $problemas += "Gradle Wrapper precisa ser criado"
}

$gradleUserHome = "$env:USERPROFILE\.gradle\wrapper\dists"
if (Test-Path $gradleUserHome) {
    $gradleVersions = Get-ChildItem $gradleUserHome -Directory | Sort-Object Name -Descending | Select-Object -First 5
    Write-Host "   ✓ Gradle no sistema ($($gradleVersions.Count) versões):" -ForegroundColor Green
    $gradleVersions | ForEach-Object { Write-Host "     - $($_.Name)" -ForegroundColor Gray }
}

# 4. Arquivos do projeto
Write-Host ""
Write-Host "[4] Arquivos do projeto..." -ForegroundColor Yellow
$arquivosNecessarios = @(
    "settings.gradle.kts",
    "build.gradle.kts",
    "app\build.gradle.kts",
    "app\src\main\AndroidManifest.xml"
)

foreach ($arquivo in $arquivosNecessarios) {
    if (Test-Path $arquivo) {
        Write-Host "   ✓ $arquivo" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $arquivo FALTANDO" -ForegroundColor Red
        $problemas += "Arquivo faltando: $arquivo"
    }
}

# 5. Código fonte Kotlin
Write-Host ""
Write-Host "[5] Código fonte..." -ForegroundColor Yellow
$kotlinFiles = Get-ChildItem -Path "app\src\main\java" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue
if ($kotlinFiles) {
    Write-Host "   ✓ $($kotlinFiles.Count) arquivos Kotlin encontrados" -ForegroundColor Green
} else {
    Write-Host "   ✗ Nenhum arquivo Kotlin" -ForegroundColor Red
    $problemas += "Código fonte não encontrado"
}

# 6. Verificar AndroidManifest
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  REQUISITOS PARA DISPOSITIVOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (Test-Path "app\src\main\AndroidManifest.xml") {
    $manifest = Get-Content "app\src\main\AndroidManifest.xml" -Raw
    
    Write-Host "📱 SMARTPHONES E TABLETS:" -ForegroundColor Cyan
    Write-Host ""
    
    # Permissões
    if ($manifest.Contains("android.permission.INTERNET")) {
        Write-Host "   ✓ INTERNET - Streaming" -ForegroundColor Green
    } else {
        Write-Host "   ✗ INTERNET - FALTA" -ForegroundColor Red
        $problemas += "Permissão INTERNET faltando"
    }
    
    if ($manifest.Contains("android.permission.ACCESS_NETWORK_STATE")) {
        Write-Host "   ✓ ACCESS_NETWORK_STATE - Status de rede" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ ACCESS_NETWORK_STATE - Recomendado" -ForegroundColor Yellow
        $avisos += "ACCESS_NETWORK_STATE recomendado"
    }
    
    Write-Host ""
    Write-Host "📺 ANDROID TV / FIRE TV:" -ForegroundColor Cyan
    Write-Host ""
    
    # Leanback
    if ($manifest.Contains("android.software.leanback")) {
        Write-Host "   ✓ Feature Leanback - Suporte TV" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Feature Leanback FALTANDO" -ForegroundColor Red
        $problemas += "Leanback feature faltando (Android TV)"
    }
    
    if ($manifest.Contains("LEANBACK_LAUNCHER")) {
        Write-Host "   ✓ Leanback Launcher - Aparece na TV" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Leanback Launcher FALTANDO" -ForegroundColor Red
        $problemas += "LEANBACK_LAUNCHER faltando"
    }
    
    # Picture in Picture
    if ($manifest.Contains("supportsPictureInPicture")) {
        Write-Host "   ✓ Picture-in-Picture" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ Picture-in-Picture - Recomendado" -ForegroundColor Yellow
        $avisos += "PiP recomendado"
    }
    
    # Cleartext Traffic
    if ($manifest.Contains("usesCleartextTraffic")) {
        Write-Host "   ✓ Cleartext Traffic - Para IPTV" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Cleartext Traffic FALTANDO" -ForegroundColor Red
        $problemas += "usesCleartextTraffic necessário"
    }
    
    # Network Security Config
    if ($manifest.Contains("networkSecurityConfig")) {
        Write-Host "   ✓ Network Security Config" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ Network Security Config" -ForegroundColor Yellow
        $avisos += "Network Security Config recomendado"
    }
}

# 7. Verificar dependências do ExoPlayer
Write-Host ""
Write-Host "🎬 STREAMING (ExoPlayer):" -ForegroundColor Cyan
Write-Host ""

if (Test-Path "app\build.gradle.kts") {
    $buildGradle = Get-Content "app\build.gradle.kts" -Raw
    
    if ($buildGradle.Contains("exoplayer:2")) {
        Write-Host "   ✓ ExoPlayer Core" -ForegroundColor Green
    } else {
        Write-Host "   ✗ ExoPlayer Core FALTANDO" -ForegroundColor Red
        $problemas += "ExoPlayer não encontrado"
    }
    
    if ($buildGradle.Contains("exoplayer-hls")) {
        Write-Host "   ✓ ExoPlayer HLS - Streaming" -ForegroundColor Green
    } else {
        Write-Host "   ✗ ExoPlayer HLS FALTANDO" -ForegroundColor Red
        $problemas += "ExoPlayer HLS necessário"
    }
    
    if ($buildGradle.Contains("exoplayer-ui")) {
        Write-Host "   ✓ ExoPlayer UI" -ForegroundColor Green
    } else {
        Write-Host "   ⚠ ExoPlayer UI" -ForegroundColor Yellow
        $avisos += "ExoPlayer UI recomendado"
    }
}

# RESUMO
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RESUMO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (($problemas.Count -eq 0) -and ($avisos.Count -eq 0)) {
    Write-Host "✅ TUDO PRONTO PARA COMPILAR!" -ForegroundColor Green
    Write-Host ""
    Write-Host "O app funciona em:" -ForegroundColor White
    Write-Host "  • Smartphones Android 5.0+" -ForegroundColor Gray
    Write-Host "  • Tablets Android" -ForegroundColor Gray
    Write-Host "  • Android TV" -ForegroundColor Gray
    Write-Host "  • Fire TV (Amazon)" -ForegroundColor Gray
} else {
    if ($problemas.Count -gt 0) {
        Write-Host "❌ PROBLEMAS CRÍTICOS ($($problemas.Count)):" -ForegroundColor Red
        Write-Host ""
        $count = 1
        foreach ($p in $problemas) {
            Write-Host "  $count. $p" -ForegroundColor Red
            $count++
        }
        Write-Host ""
    }
    
    if ($avisos.Count -gt 0) {
        Write-Host "⚠️  AVISOS ($($avisos.Count)):" -ForegroundColor Yellow
        Write-Host ""
        $count = 1
        foreach ($a in $avisos) {
            Write-Host "  $count. $a" -ForegroundColor Yellow
            $count++
        }
        Write-Host ""
    }
}

# PRÓXIMOS PASSOS
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  PRÓXIMOS PASSOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Test-Path $gradleWrapperLocal)) {
    Write-Host "1. CRIAR GRADLE WRAPPER:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host '   $gradle = Get-ChildItem "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.9-bin" -Recurse -Filter "gradle.bat" | Select-Object -First 1' -ForegroundColor Gray
    Write-Host '   & $gradle.FullName wrapper --gradle-version 8.2.2' -ForegroundColor Gray
    Write-Host ""
}

Write-Host "2. COMPILAR:" -ForegroundColor Yellow
Write-Host ""
Write-Host "   DEBUG:   .\gradlew.bat assembleDebug" -ForegroundColor Gray
Write-Host "   RELEASE: .\gradlew.bat assembleRelease" -ForegroundColor Gray
Write-Host ""

Write-Host "3. APK GERADO EM:" -ForegroundColor Yellow
Write-Host ""
Write-Host "   app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Gray
Write-Host "   app\build\outputs\apk\release\app-release.apk" -ForegroundColor Gray
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Diagnóstico completo!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

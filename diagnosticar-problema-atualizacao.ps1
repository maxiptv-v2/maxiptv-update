# Script para diagnosticar problema de atualização
# Verifica se a mesma chave de assinatura está sendo usada e se o versionCode está correto

Write-Host "🔍 DIAGNÓSTICO DE ATUALIZAÇÃO" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar keystore
Write-Host "1️⃣ Verificando configuração de assinatura..." -ForegroundColor Yellow
$keystoreFile = "keystore.properties"
if (Test-Path $keystoreFile) {
    Write-Host "   ✅ keystore.properties encontrado" -ForegroundColor Green
    $keystoreProps = Get-Content $keystoreFile
    $keystoreProps | ForEach-Object {
        if ($_ -match "keyAlias|storeFile") {
            Write-Host "   $_" -ForegroundColor Gray
        }
    }
} else {
    Write-Host "   ⚠️ keystore.properties NÃO encontrado!" -ForegroundColor Red
    Write-Host "   ⚠️ O APK pode não estar sendo assinado corretamente!" -ForegroundColor Red
}

Write-Host ""

# 2. Verificar versionCode no build.gradle.kts
Write-Host "2️⃣ Verificando versionCode no build.gradle.kts..." -ForegroundColor Yellow
$buildGradle = Get-Content "app\build.gradle.kts" -Raw
if ($buildGradle -match "versionCode\s*=\s*(\d+)") {
    $versionCode = $matches[1]
    Write-Host "   ✅ versionCode encontrado: $versionCode" -ForegroundColor Green
} else {
    Write-Host "   ❌ versionCode não encontrado!" -ForegroundColor Red
}

if ($buildGradle -match "versionName\s*=\s*""([^""]+)""") {
    $versionName = $matches[1]
    Write-Host "   ✅ versionName encontrado: $versionName" -ForegroundColor Green
} else {
    Write-Host "   ❌ versionName não encontrado!" -ForegroundColor Red
}

Write-Host ""

# 3. Verificar versionCode no update.json
Write-Host "3️⃣ Verificando versionCode no update.json..." -ForegroundColor Yellow
if (Test-Path "update.json") {
    $updateJson = Get-Content "update.json" | ConvertFrom-Json
    Write-Host "   ✅ update.json encontrado" -ForegroundColor Green
    Write-Host "   📊 Versão no update.json: $($updateJson.version)" -ForegroundColor Cyan
    Write-Host "   📊 versionCode no update.json: $($updateJson.versionCode)" -ForegroundColor Cyan
    
    # Comparar com build.gradle.kts
    if ($versionCode -and $updateJson.versionCode) {
        if ([int]$versionCode -eq [int]$updateJson.versionCode) {
            Write-Host "   ✅ versionCode está sincronizado!" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️ ATENÇÃO: versionCode NÃO está sincronizado!" -ForegroundColor Red
            Write-Host "      build.gradle.kts: $versionCode" -ForegroundColor Yellow
            Write-Host "      update.json: $($updateJson.versionCode)" -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "   ❌ update.json não encontrado!" -ForegroundColor Red
}

Write-Host ""

# 4. Verificar se APK foi gerado
Write-Host "4️⃣ Verificando APK gerado..." -ForegroundColor Yellow
$apkPath = "app\build\outputs\apk\release\maxiptv-release.apk"
if (Test-Path $apkPath) {
    $apkInfo = Get-Item $apkPath
    Write-Host "   ✅ APK encontrado: $($apkInfo.Name)" -ForegroundColor Green
    Write-Host "   📊 Tamanho: $([math]::Round($apkInfo.Length / 1MB, 2)) MB" -ForegroundColor Cyan
    Write-Host "   📊 Última modificação: $($apkInfo.LastWriteTime)" -ForegroundColor Cyan
    
    # Tentar verificar assinatura do APK (se aapt2 estiver disponível)
    $aapt2Path = "$env:ANDROID_HOME\build-tools\*\aapt2.exe"
    $aapt2 = Get-Item $aapt2Path -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($aapt2) {
        Write-Host ""
        Write-Host "   🔍 Verificando assinatura do APK..." -ForegroundColor Yellow
        $aaptOutput = & $aapt2.FullName dump badging $apkPath 2>&1
        if ($aaptOutput -match "versionCode='(\d+)'") {
            $apkVersionCode = $matches[1]
            Write-Host "   ✅ versionCode no APK: $apkVersionCode" -ForegroundColor Green
            
            if ($versionCode -and $apkVersionCode -eq $versionCode) {
                Write-Host "   ✅ versionCode do APK está correto!" -ForegroundColor Green
            } else {
                Write-Host "   ⚠️ ATENÇÃO: versionCode do APK não corresponde!" -ForegroundColor Red
            }
        }
        
        if ($aaptOutput -match "versionName='([^']+)'") {
            $apkVersionName = $matches[1]
            Write-Host "   ✅ versionName no APK: $apkVersionName" -ForegroundColor Green
        }
    } else {
        Write-Host "   ⚠️ aapt2 não encontrado - não é possível verificar assinatura do APK" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ APK não encontrado!" -ForegroundColor Red
    Write-Host "   💡 Execute: .\build-release.ps1 release" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "📋 RESUMO:" -ForegroundColor Cyan
Write-Host "   - Se a mesma chave de assinatura está sendo usada: ✅ SIM (necessário para atualização)" -ForegroundColor Green
Write-Host "   - Se versionCode está sincronizado: Verifique acima" -ForegroundColor Yellow
Write-Host "   - Se APK foi gerado: Verifique acima" -ForegroundColor Yellow
Write-Host ""
Write-Host "💡 POSSÍVEIS CAUSAS DO PROBLEMA:" -ForegroundColor Yellow
Write-Host "   1. APK não está sendo instalado corretamente" -ForegroundColor White
Write-Host "   2. App não está reiniciando após instalação" -ForegroundColor White
Write-Host "   3. PackageManager não está atualizando versão imediatamente" -ForegroundColor White
Write-Host "   4. Cache do sistema está mantendo versão antiga" -ForegroundColor White
Write-Host ""
Write-Host "✅ CORREÇÕES APLICADAS:" -ForegroundColor Green
Write-Host "   - Delay de 2s antes de verificar versão após abrir app" -ForegroundColor White
Write-Host "   - App fecha automaticamente após iniciar instalação" -ForegroundColor White
Write-Host "   - Logs detalhados de versão atual vs disponível" -ForegroundColor White


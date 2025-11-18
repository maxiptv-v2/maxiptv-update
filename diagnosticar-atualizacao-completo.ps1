# Script completo para diagnosticar problema de atualização
# Verifica todos os aspectos do processo de atualização

Write-Host "DIAGNOSTICO COMPLETO DE ATUALIZACAO" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor Cyan
Write-Host ""

# 1. Verificar configuracao de assinatura
Write-Host "1. VERIFICANDO ASSINATURA DO APK" -ForegroundColor Yellow
Write-Host ("-" * 60) -ForegroundColor Gray
$keystoreFile = "keystore.properties"
if (Test-Path $keystoreFile) {
    Write-Host "OK keystore.properties encontrado" -ForegroundColor Green
    $keystoreProps = Get-Content $keystoreFile
    $hasKeyAlias = $false
    $hasStoreFile = $false
    $keystoreProps | ForEach-Object {
        if ($_ -match "keyAlias\s*=\s*(.+)") {
            Write-Host "   keyAlias: $($matches[1])" -ForegroundColor Gray
            $hasKeyAlias = $true
        }
        if ($_ -match "storeFile\s*=\s*(.+)") {
            Write-Host "   storeFile: $($matches[1])" -ForegroundColor Gray
            if (Test-Path $matches[1]) {
                Write-Host "   OK Arquivo keystore existe" -ForegroundColor Green
            } else {
                Write-Host "   ERRO Arquivo keystore NÃO existe!" -ForegroundColor Red
            }
            $hasStoreFile = $true
        }
    }
    if (-not $hasKeyAlias -or -not $hasStoreFile) {
        Write-Host "   ATENCAO Configuração de keystore incompleta!" -ForegroundColor Red
    }
} else {
    Write-Host "   ERRO keystore.properties NÃO encontrado!" -ForegroundColor Red
    Write-Host "   ATENCAO O APK pode não estar sendo assinado corretamente!" -ForegroundColor Red
}
Write-Host ""

# 2. Verificar versionCode no código
Write-Host "2. VERIFICANDO VERSIONCODE NO CÓDIGO" -ForegroundColor Yellow
Write-Host ("-" * 60) -ForegroundColor Gray
$buildGradle = Get-Content "app\build.gradle.kts" -Raw
$versionCodeGradle = $null
$versionNameGradle = $null

if ($buildGradle -match "versionCode\s*=\s*(\d+)") {
    $versionCodeGradle = [int]$matches[1]
    Write-Host "   OK versionCode no build.gradle.kts: $versionCodeGradle" -ForegroundColor Green
} else {
    Write-Host "   ERRO versionCode não encontrado no build.gradle.kts!" -ForegroundColor Red
}

if ($buildGradle -match 'versionName\s*=\s*"([^"]+)"') {
    $versionNameGradle = $matches[1]
    Write-Host "   OK versionName no build.gradle.kts: $versionNameGradle" -ForegroundColor Green
} else {
    Write-Host "   ERRO versionName nao encontrado no build.gradle.kts!" -ForegroundColor Red
}

# Verificar signing config
if ($buildGradle -match "enableV1Signing\s*=\s*true") {
    Write-Host "   OK V1 signing habilitado" -ForegroundColor Green
} else {
    Write-Host "   ATENCAO V1 signing NÃO habilitado (necessário para Fire OS)" -ForegroundColor Yellow
}

if ($buildGradle -match "enableV2Signing\s*=\s*true") {
    Write-Host "   OK V2 signing habilitado" -ForegroundColor Green
} else {
    Write-Host "   ATENCAO V2 signing NÃO habilitado" -ForegroundColor Yellow
}
Write-Host ""

# 3. Verificar versionCode no update.json
Write-Host "3. VERIFICANDO VERSIONCODE NO UPDATE.JSON" -ForegroundColor Yellow
Write-Host ("-" * 60) -ForegroundColor Gray
if (Test-Path "update.json") {
    $updateJson = Get-Content "update.json" | ConvertFrom-Json
    Write-Host "   OK update.json encontrado" -ForegroundColor Green
    Write-Host "    Versão: $($updateJson.version)" -ForegroundColor Cyan
    Write-Host "    versionCode: $($updateJson.versionCode)" -ForegroundColor Cyan
    Write-Host "    downloadUrl: $($updateJson.downloadUrl)" -ForegroundColor Cyan
    
    # Comparar com build.gradle.kts
    if ($versionCodeGradle -and $updateJson.versionCode) {
        if ([int]$versionCodeGradle -eq [int]$updateJson.versionCode) {
            Write-Host "   OK versionCode está sincronizado!" -ForegroundColor Green
        } else {
            Write-Host "   ERRO ATENÇÃO: versionCode NÃO está sincronizado!" -ForegroundColor Red
            Write-Host "      build.gradle.kts: $versionCodeGradle" -ForegroundColor Yellow
            Write-Host "      update.json: $($updateJson.versionCode)" -ForegroundColor Yellow
        }
    }
    
    # Verificar URL do APK
    if ($updateJson.downloadUrl -match "maxiptv-release\.apk") {
        Write-Host "   OK URL do APK está correta (maxiptv-release.apk)" -ForegroundColor Green
    } else {
        Write-Host "   ATENCAO URL do APK pode estar incorreta: $($updateJson.downloadUrl)" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ERRO update.json não encontrado!" -ForegroundColor Red
}
Write-Host ""

# 4. Verificar APK local
Write-Host "4. VERIFICANDO APK LOCAL" -ForegroundColor Yellow
Write-Host ("-" * 60) -ForegroundColor Gray
$apkPath = "app\build\outputs\apk\release\maxiptv-release.apk"
$apkLocal = "maxiptv-release.apk"

if (Test-Path $apkPath) {
    $apkInfo = Get-Item $apkPath
    Write-Host "   OK APK encontrado em: $apkPath" -ForegroundColor Green
    Write-Host "    Tamanho: $([math]::Round($apkInfo.Length / 1MB, 2)) MB" -ForegroundColor Cyan
    Write-Host "    Última modificação: $($apkInfo.LastWriteTime)" -ForegroundColor Cyan
} else {
    Write-Host "   ERRO APK não encontrado em: $apkPath" -ForegroundColor Red
}

if (Test-Path $apkLocal) {
    $apkLocalInfo = Get-Item $apkLocal
    Write-Host "   OK APK encontrado em: $apkLocal" -ForegroundColor Green
    Write-Host "    Tamanho: $([math]::Round($apkLocalInfo.Length / 1MB, 2)) MB" -ForegroundColor Cyan
    Write-Host "    Última modificação: $($apkLocalInfo.LastWriteTime)" -ForegroundColor Cyan
} else {
    Write-Host "   ATENCAO APK não encontrado em: $apkLocal" -ForegroundColor Yellow
}
Write-Host ""

# 5. Verificar versionCode no APK (se aapt2 disponível)
Write-Host "5. VERIFICANDO VERSIONCODE NO APK" -ForegroundColor Yellow
Write-Host ("-" * 60) -ForegroundColor Gray
$apkToCheck = if (Test-Path $apkPath) { $apkPath } elseif (Test-Path $apkLocal) { $apkLocal } else { $null }

if ($apkToCheck) {
    # Tentar encontrar aapt2
    $aapt2Path = $null
    if ($env:ANDROID_HOME) {
        $aapt2Files = Get-ChildItem "$env:ANDROID_HOME\build-tools" -Recurse -Filter "aapt2.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($aapt2Files) {
            $aapt2Path = $aapt2Files.FullName
        }
    }
    
    if ($aapt2Path) {
        Write-Host "    Analisando APK com aapt2..." -ForegroundColor Cyan
        try {
            $aaptOutput = & $aapt2Path dump badging $apkToCheck 2>&1 | Out-String
            
            if ($aaptOutput -match "versionCode='(\d+)'") {
                $apkVersionCode = [int]$matches[1]
                Write-Host "   OK versionCode no APK: $apkVersionCode" -ForegroundColor Green
                
                if ($versionCodeGradle -and $apkVersionCode -eq $versionCodeGradle) {
                    Write-Host "   OK versionCode do APK está correto!" -ForegroundColor Green
                } else {
                    Write-Host "   ERRO ATENÇÃO: versionCode do APK não corresponde!" -ForegroundColor Red
                    Write-Host "      build.gradle.kts: $versionCodeGradle" -ForegroundColor Yellow
                    Write-Host "      APK: $apkVersionCode" -ForegroundColor Yellow
                }
            } else {
                Write-Host "   ATENCAO Não foi possível extrair versionCode do APK" -ForegroundColor Yellow
            }
            
            if ($aaptOutput -match "versionName='([^']+)'") {
                $apkVersionName = $matches[1]
                Write-Host "   OK versionName no APK: $apkVersionName" -ForegroundColor Green
            }
            
            # Verificar assinatura
            if ($aaptOutput -match "package: name='([^']+)'") {
                $packageName = $matches[1]
                Write-Host "   OK Package name: $packageName" -ForegroundColor Green
            }
        } catch {
            Write-Host "   ATENCAO Erro ao analisar APK: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ATENCAO aapt2 não encontrado - não é possível verificar versionCode no APK" -ForegroundColor Yellow
        Write-Host "    Instale Android SDK Build Tools para análise completa" -ForegroundColor Cyan
    }
} else {
    Write-Host "   ATENCAO Nenhum APK encontrado para análise" -ForegroundColor Yellow
}
Write-Host ""

# 6. Verificar código de atualização
Write-Host "6. VERIFICANDO CÓDIGO DE ATUALIZAÇÃO" -ForegroundColor Yellow
Write-Host ("-" * 60) -ForegroundColor Gray

# Verificar UpdateManager.kt
$updateManagerPath = "app\src\main\java\com\maxiptv\data\UpdateManager.kt"
if (Test-Path $updateManagerPath) {
    $updateManagerCode = Get-Content $updateManagerPath -Raw
    
    # Verificar se está usando getCurrentVersionCode corretamente
    if ($updateManagerCode -match "getCurrentVersionCode") {
        Write-Host "   OK UpdateManager usa getCurrentVersionCode" -ForegroundColor Green
    } else {
        Write-Host "   ATENCAO UpdateManager pode não estar verificando versionCode corretamente" -ForegroundColor Yellow
    }
    
    # Verificar URL do update.json
    if ($updateManagerCode -match 'UPDATE_JSON_URL\s*=\s*"([^"]+)"') {
        $updateUrl = $matches[1]
        Write-Host "   OK URL do update.json: $updateUrl" -ForegroundColor Green
    }
    
    # Verificar se está comparando versionCode
    if ($updateManagerCode -match "updateInfo\.versionCode\s*>\s*currentVersionCode") {
        Write-Host "   OK Comparação de versionCode está correta" -ForegroundColor Green
    } else {
        Write-Host "   ATENCAO Comparação de versionCode pode estar incorreta" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ERRO UpdateManager.kt não encontrado!" -ForegroundColor Red
}

# Verificar ApkDownloader.kt
$apkDownloaderPath = "app\src\main\java\com\maxiptv\data\ApkDownloader.kt"
if (Test-Path $apkDownloaderPath) {
    $apkDownloaderCode = Get-Content $apkDownloaderPath -Raw
    
    # Verificar se está fechando app após instalação
    if ($apkDownloaderCode -match "finishAffinity|Fechando app") {
        Write-Host "   OK ApkDownloader fecha app após instalação" -ForegroundColor Green
    } else {
        Write-Host "   ATENCAO ApkDownloader pode não estar fechando app após instalação" -ForegroundColor Yellow
    }
    
    # Verificar se está usando ApplicationContext
    if ($apkDownloaderCode -match "applicationContext") {
        Write-Host "   OK ApkDownloader usa ApplicationContext (correto)" -ForegroundColor Green
    } else {
        Write-Host "   ATENCAO ApkDownloader pode não estar usando ApplicationContext" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ERRO ApkDownloader.kt não encontrado!" -ForegroundColor Red
}
Write-Host ""

# 7. Verificar HomeScreen.kt
Write-Host "7️⃣ VERIFICANDO VERIFICAÇÃO DE VERSÃO NO HOMESCREEN" -ForegroundColor Yellow
Write-Host ("-" * 60) -ForegroundColor Gray
$homeScreenPath = "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt"
if (Test-Path $homeScreenPath) {
    $homeScreenCode = Get-Content $homeScreenPath -Raw
    
    # Verificar se tem delay antes de verificar versão
    if ($homeScreenCode -match "delay\(2000\)|delay\(1000\)") {
        Write-Host "   OK HomeScreen tem delay antes de verificar versão" -ForegroundColor Green
    } else {
        Write-Host "   ATENCAO HomeScreen pode não ter delay antes de verificar versão" -ForegroundColor Yellow
    }
    
    # Verificar se está logando versão atual
    if ($homeScreenCode -match "Versão atual|currentVersion|getCurrentVersion") {
        Write-Host "   OK HomeScreen verifica versão atual" -ForegroundColor Green
    } else {
        Write-Host "   ATENCAO HomeScreen pode não estar verificando versão atual" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ERRO HomeScreen.kt não encontrado!" -ForegroundColor Red
}
Write-Host ""

# 8. Resumo e recomendações
Write-Host " RESUMO E DIAGNÓSTICO" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor Cyan
Write-Host ""

$issues = @()

if (-not (Test-Path $keystoreFile)) {
    $issues += "ERRO keystore.properties não encontrado"
}

if ($versionCodeGradle -and $updateJson.versionCode -and [int]$versionCodeGradle -ne [int]$updateJson.versionCode) {
    $issues += "ERRO versionCode não sincronizado entre build.gradle.kts e update.json"
}

if (-not (Test-Path $apkPath) -and -not (Test-Path $apkLocal)) {
    $issues += "ERRO APK não encontrado"
}

if ($issues.Count -eq 0) {
    Write-Host "OK Nenhum problema crítico encontrado no código!" -ForegroundColor Green
    Write-Host ""
    Write-Host " POSSÍVEIS CAUSAS DO PROBLEMA:" -ForegroundColor Yellow
    Write-Host "   1. APK não está sendo instalado corretamente no dispositivo" -ForegroundColor White
    Write-Host "   2. App não está reiniciando após instalação" -ForegroundColor White
    Write-Host "   3. PackageManager não atualiza versão imediatamente após instalação" -ForegroundColor White
    Write-Host "   4. Cache do sistema mantém versão antiga" -ForegroundColor White
    Write-Host "   5. APK baixado está corrompido ou incompleto" -ForegroundColor White
    Write-Host ""
    Write-Host "CORRECOES JA IMPLEMENTADAS:" -ForegroundColor Green
    Write-Host "   OK Delay de 2s antes de verificar versao ao abrir app" -ForegroundColor White
    Write-Host "   OK App fecha automaticamente apos iniciar instalacao" -ForegroundColor White
    Write-Host "   OK Logs detalhados de versao atual vs disponivel" -ForegroundColor White
    Write-Host ""
    Write-Host " PRÓXIMOS PASSOS PARA TESTAR:" -ForegroundColor Cyan
    Write-Host "   1. Compilar novo APK com as correções" -ForegroundColor White
    Write-Host "   2. Instalar manualmente no dispositivo para testar" -ForegroundColor White
    Write-Host "   3. Verificar logs do Logcat durante atualização" -ForegroundColor White
    Write-Host "   4. Verificar se PackageManager retorna versão correta após instalação" -ForegroundColor White
} else {
    Write-Host "ATENCAO PROBLEMAS ENCONTRADOS:" -ForegroundColor Red
    $issues | ForEach-Object {
        Write-Host "   $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host ("=" * 60) -ForegroundColor Cyan


# Script profissional para diagnosticar problema de atualização no Fire OS
# Analisa a lógica de atualização e identifica o que falta para Fire OS aceitar

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTICO: Atualizacao Fire OS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$errors = @()
$warnings = @()
$success = @()

# 1. Verificar configuração de assinatura no build.gradle.kts
Write-Host "[1/8] Verificando configuracao de assinatura..." -ForegroundColor Yellow

$buildGradlePath = "app/build.gradle.kts"
if (Test-Path $buildGradlePath) {
    $buildGradleContent = Get-Content $buildGradlePath -Raw
    
    if ($buildGradleContent -match "enableV1Signing\s*=\s*true") {
        Write-Host "  ✅ v1 Signing habilitado" -ForegroundColor Green
        $success += "v1 Signing habilitado"
    } else {
        Write-Host "  ❌ v1 Signing NAO habilitado" -ForegroundColor Red
        $errors += "v1 Signing não está habilitado no build.gradle.kts"
    }
    
    if ($buildGradleContent -match "enableV2Signing\s*=\s*true") {
        Write-Host "  ✅ v2 Signing habilitado" -ForegroundColor Green
        $success += "v2 Signing habilitado"
    } else {
        Write-Host "  ❌ v2 Signing NAO habilitado" -ForegroundColor Red
        $errors += "v2 Signing não está habilitado no build.gradle.kts"
    }
} else {
    Write-Host "  ❌ Arquivo build.gradle.kts nao encontrado" -ForegroundColor Red
    $errors += "build.gradle.kts não existe"
}

Write-Host ""

# 2. Verificar ApkDownloader.kt - lógica de download e instalação
Write-Host "[2/8] Verificando ApkDownloader.kt..." -ForegroundColor Yellow

$apkDownloaderPath = "app/src/main/java/com/maxiptv/data/ApkDownloader.kt"
if (Test-Path $apkDownloaderPath) {
    $apkDownloaderContent = Get-Content $apkDownloaderPath -Raw
    
    # Verificar detecção de Fire OS
    if ($apkDownloaderContent -match "isFireOS|Fire.*OS|fire.*os") {
        Write-Host "  ✅ Deteccao de Fire OS encontrada" -ForegroundColor Green
        $success += "Detecção de Fire OS implementada"
    } else {
        Write-Host "  ⚠️  Deteccao de Fire OS pode nao estar implementada" -ForegroundColor Yellow
        $warnings += "ApkDownloader pode não ter detecção específica de Fire OS"
    }
    
    # Verificar FileProvider
    if ($apkDownloaderContent -match "FileProvider|fileprovider") {
        Write-Host "  ✅ FileProvider encontrado" -ForegroundColor Green
        $success += "FileProvider implementado"
    } else {
        Write-Host "  ❌ FileProvider NAO encontrado" -ForegroundColor Red
        $errors += "FileProvider não está sendo usado em ApkDownloader"
    }
    
    # Verificar permissão de instalação
    if ($apkDownloaderContent -match "REQUEST_INSTALL_PACKAGES|canInstallPackages|requestInstallPermission") {
        Write-Host "  ✅ Permissao de instalacao verificada" -ForegroundColor Green
        $success += "Permissão de instalação verificada"
    } else {
        Write-Host "  ❌ Permissao de instalacao NAO verificada" -ForegroundColor Red
        $errors += "Permissão de instalação não está sendo verificada"
    }
    
    # Verificar tratamento específico para Fire OS
    if ($apkDownloaderContent -match "isFireOS.*Thread\.sleep|Fire.*OS.*sleep|fire.*os.*2000") {
        Write-Host "  ✅ Tratamento especifico para Fire OS encontrado" -ForegroundColor Green
        $success += "Tratamento específico para Fire OS implementado"
    } else {
        Write-Host "  ⚠️  Tratamento especifico para Fire OS pode nao estar completo" -ForegroundColor Yellow
        $warnings += "Pode faltar tratamento específico para Fire OS"
    }
    
    # Verificar limpeza de cache/arquivos antigos
    if ($apkDownloaderContent -match "delete.*apk|limpar.*arquivo|clear.*old") {
        Write-Host "  ✅ Limpeza de arquivos antigos encontrada" -ForegroundColor Green
        $success += "Limpeza de arquivos antigos implementada"
    } else {
        Write-Host "  ⚠️  Limpeza de arquivos antigos pode nao estar implementada" -ForegroundColor Yellow
        $warnings += "Pode faltar limpeza de arquivos antigos"
    }
    
    # Verificar timestamp no nome do arquivo
    if ($apkDownloaderPath -match "timestamp|System\.currentTimeMillis") {
        Write-Host "  ✅ Timestamp no nome do arquivo encontrado" -ForegroundColor Green
        $success += "Timestamp no nome do arquivo implementado"
    } else {
        Write-Host "  ⚠️  Timestamp no nome do arquivo pode nao estar implementado" -ForegroundColor Yellow
        $warnings += "Pode faltar timestamp no nome do arquivo para evitar cache"
    }
} else {
    Write-Host "  ❌ Arquivo ApkDownloader.kt nao encontrado" -ForegroundColor Red
    $errors += "ApkDownloader.kt não existe"
}

Write-Host ""

# 3. Verificar AndroidManifest.xml - permissões e FileProvider
Write-Host "[3/8] Verificando AndroidManifest.xml..." -ForegroundColor Yellow

$manifestPath = "app/src/main/AndroidManifest.xml"
if (Test-Path $manifestPath) {
    $manifestContent = Get-Content $manifestPath -Raw
    
    # Verificar permissão de instalação
    if ($manifestContent -match "REQUEST_INSTALL_PACKAGES|android\.permission\.REQUEST_INSTALL_PACKAGES") {
        Write-Host "  ✅ Permissao REQUEST_INSTALL_PACKAGES encontrada" -ForegroundColor Green
        $success += "Permissão REQUEST_INSTALL_PACKAGES no manifest"
    } else {
        Write-Host "  ❌ Permissao REQUEST_INSTALL_PACKAGES NAO encontrada" -ForegroundColor Red
        $errors += "Permissão REQUEST_INSTALL_PACKAGES não está no AndroidManifest.xml"
    }
    
    # Verificar FileProvider
    if ($manifestContent -match "FileProvider|fileprovider") {
        Write-Host "  ✅ FileProvider declarado no manifest" -ForegroundColor Green
        $success += "FileProvider declarado no manifest"
    } else {
        Write-Host "  ❌ FileProvider NAO declarado no manifest" -ForegroundColor Red
        $errors += "FileProvider não está declarado no AndroidManifest.xml"
    }
    
    # Verificar provider com authorities correto
    if ($manifestContent -match 'android:authorities=".*fileprovider"') {
        Write-Host "  ✅ FileProvider authorities configurado" -ForegroundColor Green
        $success += "FileProvider authorities configurado"
    } else {
        Write-Host "  ⚠️  FileProvider authorities pode nao estar configurado corretamente" -ForegroundColor Yellow
        $warnings += "FileProvider authorities pode não estar configurado"
    }
} else {
    Write-Host "  ❌ Arquivo AndroidManifest.xml nao encontrado" -ForegroundColor Red
    $errors += "AndroidManifest.xml não existe"
}

Write-Host ""

# 4. Verificar file_paths.xml - configuração do FileProvider
Write-Host "[4/8] Verificando file_paths.xml..." -ForegroundColor Yellow

$filePathsPath = "app/src/main/res/xml/file_paths.xml"
if (Test-Path $filePathsPath) {
    $filePathsContent = Get-Content $filePathsPath -Raw
    
    if ($filePathsContent -match "external-path|downloads") {
        Write-Host "  ✅ file_paths.xml configurado" -ForegroundColor Green
        $success += "file_paths.xml configurado"
    } else {
        Write-Host "  ⚠️  file_paths.xml pode nao estar completo" -ForegroundColor Yellow
        $warnings += "file_paths.xml pode não estar completo"
    }
} else {
    Write-Host "  ❌ Arquivo file_paths.xml nao encontrado" -ForegroundColor Red
    $errors += "file_paths.xml não existe"
}

Write-Host ""

# 5. Verificar UpdateManager.kt - lógica de verificação de atualização
Write-Host "[5/8] Verificando UpdateManager.kt..." -ForegroundColor Yellow

$updateManagerPath = "app/src/main/java/com/maxiptv/data/UpdateManager.kt"
if (Test-Path $updateManagerPath) {
    $updateManagerContent = Get-Content $updateManagerPath -Raw
    
    # Verificar timestamp na URL para evitar cache
    if ($updateManagerContent -match "timestamp|System\.currentTimeMillis|no-cache") {
        Write-Host "  ✅ Cache prevention encontrado" -ForegroundColor Green
        $success += "Cache prevention implementado"
    } else {
        Write-Host "  ⚠️  Cache prevention pode nao estar implementado" -ForegroundColor Yellow
        $warnings += "Pode faltar cache prevention na verificação de atualização"
    }
} else {
    Write-Host "  ❌ Arquivo UpdateManager.kt nao encontrado" -ForegroundColor Red
    $errors += "UpdateManager.kt não existe"
}

Write-Host ""

# 6. Verificar se há BroadcastReceiver para download completo
Write-Host "[6/8] Verificando BroadcastReceiver..." -ForegroundColor Yellow

if (Test-Path $apkDownloaderPath) {
    $apkDownloaderContent = Get-Content $apkDownloaderPath -Raw
    
    if ($apkDownloaderContent -match "BroadcastReceiver|DOWNLOAD_COMPLETE|ACTION_DOWNLOAD_COMPLETE") {
        Write-Host "  ✅ BroadcastReceiver encontrado" -ForegroundColor Green
        $success += "BroadcastReceiver implementado"
    } else {
        Write-Host "  ⚠️  BroadcastReceiver pode nao estar implementado" -ForegroundColor Yellow
        $warnings += "Pode faltar BroadcastReceiver para download completo"
    }
    
    # Verificar se usa ApplicationContext
    if ($apkDownloaderContent -match "applicationContext|ApplicationContext") {
        Write-Host "  ✅ ApplicationContext usado (persistencia)" -ForegroundColor Green
        $success += "ApplicationContext usado"
    } else {
        Write-Host "  ⚠️  ApplicationContext pode nao estar sendo usado" -ForegroundColor Yellow
        $warnings += "Pode faltar ApplicationContext para persistência"
    }
}

Write-Host ""

# 7. Verificar MainActivity - checkPendingDownload
Write-Host "[7/8] Verificando MainActivity.kt..." -ForegroundColor Yellow

$mainActivityPath = "app/src/main/java/com/maxiptv/MainActivity.kt"
if (Test-Path $mainActivityPath) {
    $mainActivityContent = Get-Content $mainActivityPath -Raw
    
    if ($mainActivityContent -match "checkPendingDownload|ApkDownloader.*checkPending") {
        Write-Host "  ✅ checkPendingDownload chamado no MainActivity" -ForegroundColor Green
        $success += "checkPendingDownload implementado"
    } else {
        Write-Host "  ⚠️  checkPendingDownload pode nao estar sendo chamado" -ForegroundColor Yellow
        $warnings += "Pode faltar checkPendingDownload no MainActivity"
    }
} else {
    Write-Host "  ❌ Arquivo MainActivity.kt nao encontrado" -ForegroundColor Red
    $errors += "MainActivity.kt não existe"
}

Write-Host ""

# 8. Verificar SharedPreferences para persistência
Write-Host "[8/8] Verificando persistencia de download..." -ForegroundColor Yellow

if (Test-Path $apkDownloaderPath) {
    $apkDownloaderContent = Get-Content $apkDownloaderPath -Raw
    
    if ($apkDownloaderPath -match "SharedPreferences|getSharedPreferences|saveDownloadInfo") {
        Write-Host "  ✅ Persistencia de download encontrada" -ForegroundColor Green
        $success += "Persistência de download implementada"
    } else {
        Write-Host "  ⚠️  Persistencia de download pode nao estar implementada" -ForegroundColor Yellow
        $warnings += "Pode faltar persistência de informações de download"
    }
}

Write-Host ""

# RESUMO E ANÁLISE
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DO DIAGNOSTICO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($success.Count -gt 0) {
    Write-Host "✅ SUCESSOS ($($success.Count)):" -ForegroundColor Green
    foreach ($s in $success) {
        Write-Host "   • $s" -ForegroundColor White
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "⚠️  AVISOS ($($warnings.Count)):" -ForegroundColor Yellow
    foreach ($w in $warnings) {
        Write-Host "   • $w" -ForegroundColor White
    }
    Write-Host ""
}

if ($errors.Count -gt 0) {
    Write-Host "❌ ERROS CRITICOS ($($errors.Count)):" -ForegroundColor Red
    foreach ($e in $errors) {
        Write-Host "   • $e" -ForegroundColor White
    }
    Write-Host ""
}

# Criar prompt detalhado para IA
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "PROMPT PARA ANALISE DE IA" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$prompt = @"
PROBLEMA: Aplicativo Android não atualiza no Fire OS (Amazon Fire Stick)

CONTEXTO:
- App Android desenvolvido em Kotlin com Jetpack Compose
- Sistema de atualização implementado usando DownloadManager e FileProvider
- Fire OS requer assinatura v1 e v2 (já configurado)
- APK é baixado mas não instala quando usuário clica em "Atualizar"

ARQUITETURA ATUAL:
1. UpdateManager.kt - Verifica atualização via JSON remoto
2. ApkDownloader.kt - Baixa e instala APK usando DownloadManager
3. FileProvider configurado em AndroidManifest.xml
4. BroadcastReceiver para detectar download completo
5. SharedPreferences para persistir informações de download
6. MainActivity chama checkPendingDownload() no onCreate

PROBLEMAS IDENTIFICADOS:
$($errors -join "`n- ")
$($warnings -join "`n- ")

REQUISITOS FIRE OS:
- APK deve ser assinado com v1 e v2 (já implementado)
- FileProvider deve estar configurado corretamente
- Permissão REQUEST_INSTALL_PACKAGES necessária
- Fire OS tem cache agressivo - precisa limpar arquivos antigos
- Fire OS precisa de tempo adicional para processar arquivo
- Intent flags específicos podem ser necessários

PERGUNTA:
O que mais pode estar faltando para o Fire OS aceitar a atualização?
Quais são as melhores práticas específicas para Fire OS?
Como garantir que o APK seja instalado corretamente após download?
"@

Write-Host $prompt -ForegroundColor White
Write-Host ""

# Salvar prompt em arquivo
$prompt | Out-File -FilePath "prompt-analise-fire-os.txt" -Encoding UTF8
Write-Host "Prompt salvo em: prompt-analise-fire-os.txt" -ForegroundColor Green
Write-Host ""

# Verificar código específico do ApkDownloader
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE DETALHADA DO CODIGO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (Test-Path $apkDownloaderPath) {
    Write-Host "Trechos relevantes de ApkDownloader.kt:" -ForegroundColor Yellow
    Write-Host ""
    
    $lines = Get-Content $apkDownloaderPath
    $relevantLines = @()
    
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line -match "isFireOS|Fire.*OS|installApk|FileProvider|REQUEST_INSTALL|BroadcastReceiver|ApplicationContext|timestamp|delete.*apk") {
            $contextStart = [Math]::Max(0, $i - 2)
            $contextEnd = [Math]::Min($lines.Count - 1, $i + 2)
            $relevantLines += "Linhas $($contextStart+1)-$($contextEnd+1):"
            $relevantLines += $lines[$contextStart..$contextEnd] -join "`n"
            $relevantLines += ""
        }
    }
    
    if ($relevantLines.Count -gt 0) {
        Write-Host ($relevantLines -join "`n") -ForegroundColor Gray
    } else {
        Write-Host "Nenhum trecho relevante encontrado" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RECOMENDACOES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Verificar se APK esta sendo assinado corretamente:" -ForegroundColor Yellow
Write-Host "   apksigner verify --print-certs maxiptv-release.apk" -ForegroundColor White
Write-Host ""

Write-Host "2. Verificar logs do Fire OS durante tentativa de instalacao" -ForegroundColor Yellow
Write-Host "   adb logcat | Select-String -Pattern 'install|package|fire'" -ForegroundColor White
Write-Host ""

Write-Host "3. Verificar se FileProvider esta funcionando:" -ForegroundColor Yellow
Write-Host "   Testar URI gerado pelo FileProvider" -ForegroundColor White
Write-Host ""

Write-Host "4. Considerar usar PackageInstaller API diretamente:" -ForegroundColor Yellow
Write-Host "   Mais controle sobre o processo de instalacao" -ForegroundColor White
Write-Host ""

if ($errors.Count -gt 0) {
    Write-Host "RESULTADO: ❌ FALHOU - Corrija os erros criticos acima" -ForegroundColor Red
    exit 1
} else {
    Write-Host "RESULTADO: ⚠️  AVISOS ENCONTRADOS - Revise os avisos acima" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "O codigo parece estar bem estruturado, mas pode precisar de" -ForegroundColor Cyan
    Write-Host "ajustes especificos para Fire OS. Consulte o prompt salvo em" -ForegroundColor Cyan
    Write-Host "prompt-analise-fire-os.txt para analise detalhada." -ForegroundColor Cyan
    exit 0
}


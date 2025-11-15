# Script Profissional de Analise Profunda: Problemas Especificos do Fire Stick
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "ANALISE PROFUNDA: Problemas Especificos Fire Stick Amazon" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$apkDownloaderFile = "app/src/main/java/com/maxiptv/data/ApkDownloader.kt"
$filePathsFile = "app/src/main/res/xml/file_paths.xml"
$manifestFile = "app/src/main/AndroidManifest.xml"

Write-Host "[ANALISE 1] Verificando file_paths.xml para FileProvider..." -ForegroundColor Yellow
Write-Host ""

if (Test-Path $filePathsFile) {
    $filePathsContent = Get-Content $filePathsFile -Raw
    Write-Host "  ✅ Arquivo file_paths.xml encontrado" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Conteudo atual:" -ForegroundColor Cyan
    Write-Host $filePathsContent -ForegroundColor Gray
    Write-Host ""
    
    # Verificar se tem todos os caminhos necessarios
    $hasDownloads = $filePathsContent -match "Downloads"
    $hasDownload = $filePathsContent -match 'path="Download'
    $hasExternal = $filePathsContent -match 'external-path'
    
    if (-not $hasDownloads) {
        Write-Host "  ❌ PROBLEMA ENCONTRADO: Caminho 'Downloads/' nao esta no file_paths.xml!" -ForegroundColor Red
        Write-Host "     O codigo tenta acessar /Downloads/ mas FileProvider nao permite" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  SOLUCAO NECESSARIA:" -ForegroundColor Green
        Write-Host "     Adicionar: <external-path name=`"downloads_alt`" path=`"Downloads/`" />" -ForegroundColor White
    } else {
        Write-Host "  ✅ Caminho Downloads/ encontrado" -ForegroundColor Green
    }
    
    if (-not $hasDownload) {
        Write-Host "  ⚠️  Caminho Download/ pode estar faltando" -ForegroundColor Yellow
    } else {
        Write-Host "  ✅ Caminho Download/ encontrado" -ForegroundColor Green
    }
    
    if (-not $hasExternal) {
        Write-Host "  ❌ PROBLEMA CRITICO: Nenhum external-path encontrado!" -ForegroundColor Red
    } else {
        Write-Host "  ✅ external-path configurado" -ForegroundColor Green
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: file_paths.xml nao encontrado!" -ForegroundColor Red
    Write-Host "     FileProvider nao funcionara sem este arquivo" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[ANALISE 2] Verificando registro do BroadcastReceiver..." -ForegroundColor Yellow
Write-Host ""

$receiverCode = Select-String -Path $apkDownloaderFile -Pattern "registerReceiver|unregisterReceiver|registerDownloadReceiver" -Context 0, 10
if ($receiverCode) {
    Write-Host "  ✅ Codigo de registro encontrado" -ForegroundColor Green
    
    # Verificar se usa ApplicationContext
    $usesAppContext = $receiverCode | Where-Object { 
        $context = $_.Context
        ($_.Line -match "applicationContext") -or 
        ($context.PreContext -match "applicationContext") -or 
        ($context.PostContext -match "applicationContext")
    }
    
    if ($usesAppContext) {
        Write-Host "  ✅ Usa ApplicationContext para registrar receiver" -ForegroundColor Green
    } else {
        Write-Host "  ❌ PROBLEMA: Pode nao estar usando ApplicationContext!" -ForegroundColor Red
    }
    
    # Verificar se faz unregister antes de registrar novo
    $hasUnregister = $receiverCode | Where-Object { $_.Line -match "unregister" }
    if ($hasUnregister) {
        Write-Host "  ✅ Faz unregister antes de registrar (evita duplicacao)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Pode nao estar fazendo unregister antes de registrar" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: Codigo de registro nao encontrado!" -ForegroundColor Red
}

Write-Host ""
Write-Host "[ANALISE 3] Verificando tratamento quando app fecha durante download..." -ForegroundColor Yellow
Write-Host ""

# Verificar se checkPendingDownload verifica status corretamente
$checkPending = Select-String -Path $apkDownloaderFile -Pattern "checkPendingDownload|checkDownloadStatus" -Context 0, 15
if ($checkPending) {
    Write-Host "  ✅ Funcoes de verificacao encontradas" -ForegroundColor Green
    
    # Verificar se verifica STATUS_SUCCESSFUL
    $checksSuccess = $checkPending | Where-Object { 
        $context = $_.Context
        ($_.Line -match "STATUS_SUCCESSFUL|STATUS_COMPLETE") -or
        ($context.PreContext -match "STATUS_SUCCESSFUL|STATUS_COMPLETE") -or
        ($context.PostContext -match "STATUS_SUCCESSFUL|STATUS_COMPLETE")
    }
    
    if ($checksSuccess) {
        Write-Host "  ✅ Verifica STATUS_SUCCESSFUL corretamente" -ForegroundColor Green
    } else {
        Write-Host "  ❌ PROBLEMA: Pode nao estar verificando STATUS_SUCCESSFUL!" -ForegroundColor Red
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: Funcoes de verificacao nao encontradas!" -ForegroundColor Red
}

Write-Host ""
Write-Host "[ANALISE 4] Verificando Intent de instalacao especifico para Fire OS..." -ForegroundColor Yellow
Write-Host ""

$installIntent = Select-String -Path $apkDownloaderFile -Pattern "installIntent|ACTION_VIEW|FLAG_ACTIVITY" -Context 0, 8
if ($installIntent) {
    Write-Host "  ✅ Intent de instalacao encontrado" -ForegroundColor Green
    
    # Verificar flags especificas para Fire OS
    $fireFlags = $installIntent | Where-Object {
        $context = $_.Context
        ($_.Line -match "FLAG_ACTIVITY_CLEAR_TOP|isFire") -or
        ($context.PreContext -match "FLAG_ACTIVITY_CLEAR_TOP|isFire") -or
        ($context.PostContext -match "FLAG_ACTIVITY_CLEAR_TOP|isFire")
    }
    
    if ($fireFlags) {
        Write-Host "  ✅ Flags especificas para Fire OS encontradas" -ForegroundColor Green
        $fireFlags | Select-Object -First 3 | ForEach-Object {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ⚠️  Flags especificas para Fire OS podem estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar se usa ACTION_VIEW (nao ACTION_INSTALL_PACKAGE)
    $usesActionView = $installIntent | Where-Object { $_.Line -match "ACTION_VIEW" }
    if ($usesActionView) {
        Write-Host "  ✅ Usa ACTION_VIEW (correto para Fire OS)" -ForegroundColor Green
    } else {
        Write-Host "  ❌ PROBLEMA: Pode estar usando ACTION_INSTALL_PACKAGE (nao funciona no Fire OS)!" -ForegroundColor Red
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: Intent de instalacao nao encontrado!" -ForegroundColor Red
}

Write-Host ""
Write-Host "[ANALISE 5] Verificando permissoes e configuracoes no AndroidManifest..." -ForegroundColor Yellow
Write-Host ""

if (Test-Path $manifestFile) {
    $manifestContent = Get-Content $manifestFile -Raw
    
    # Verificar FileProvider
    $hasFileProvider = $manifestContent -match "FileProvider|fileprovider"
    if ($hasFileProvider) {
        Write-Host "  ✅ FileProvider configurado no manifest" -ForegroundColor Green
        
        # Verificar authorities
        $authorities = Select-String -Path $manifestFile -Pattern "authorities.*fileprovider" -Context 0, 1
        if ($authorities) {
            Write-Host "     Authorities: $($authorities.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ❌ PROBLEMA CRITICO: FileProvider nao configurado!" -ForegroundColor Red
    }
    
    # Verificar permissoes
    $hasInstallPermission = $manifestContent -match "REQUEST_INSTALL_PACKAGES"
    if ($hasInstallPermission) {
        Write-Host "  ✅ Permissao REQUEST_INSTALL_PACKAGES encontrada" -ForegroundColor Green
    } else {
        Write-Host "  ❌ PROBLEMA: Permissao REQUEST_INSTALL_PACKAGES faltando!" -ForegroundColor Red
    }
    
    $hasInternet = $manifestContent -match "INTERNET"
    if ($hasInternet) {
        Write-Host "  ✅ Permissao INTERNET encontrada" -ForegroundColor Green
    } else {
        Write-Host "  ❌ PROBLEMA: Permissao INTERNET faltando!" -ForegroundColor Red
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: AndroidManifest nao encontrado!" -ForegroundColor Red
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "PROBLEMAS IDENTIFICADOS:" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$criticalIssues = @()
$warnings = @()

# Verificar problemas criticos
if (Test-Path $filePathsFile) {
    $filePathsContent = Get-Content $filePathsFile -Raw
    if (-not ($filePathsContent -match "Downloads")) {
        $criticalIssues += "file_paths.xml nao tem caminho 'Downloads/' - FileProvider falhara ao acessar arquivos em /Downloads/"
    }
}

Write-Host "PROBLEMAS CRITICOS:" -ForegroundColor Red
if ($criticalIssues.Count -eq 0) {
    Write-Host "  ✅ Nenhum problema critico encontrado!" -ForegroundColor Green
} else {
    $criticalIssues | ForEach-Object {
        Write-Host "  ❌ $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "AVISOS:" -ForegroundColor Yellow
if ($warnings.Count -eq 0) {
    Write-Host "  ✅ Nenhum aviso encontrado!" -ForegroundColor Green
} else {
    $warnings | ForEach-Object {
        Write-Host "  ⚠️  $_" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "RECOMENDACOES ESPECIFICAS PARA FIRE STICK:" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Verificar se file_paths.xml tem TODOS os caminhos:" -ForegroundColor Green
Write-Host "   - external-path com path=`".`" (raiz)" -ForegroundColor White
Write-Host "   - external-path com path=`"Download/`" (com D maiusculo)" -ForegroundColor White
Write-Host "   - external-path com path=`"Downloads/`" (com D maiusculo e S)" -ForegroundColor White
Write-Host ""
Write-Host "2. Garantir que BroadcastReceiver usa ApplicationContext:" -ForegroundColor Green
Write-Host "   - context.applicationContext.registerReceiver(...)" -ForegroundColor White
Write-Host ""
Write-Host "3. Verificar se checkPendingDownload e chamado no MainActivity:" -ForegroundColor Green
Write-Host "   - ApkDownloader.checkPendingDownload(this) no onCreate" -ForegroundColor White
Write-Host ""
Write-Host "4. Garantir que Intent usa ACTION_VIEW (nao ACTION_INSTALL_PACKAGE):" -ForegroundColor Green
Write-Host "   - Intent.ACTION_VIEW funciona melhor no Fire OS" -ForegroundColor White
Write-Host ""
Write-Host "5. Verificar permissoes no AndroidManifest:" -ForegroundColor Green
Write-Host "   - REQUEST_INSTALL_PACKAGES (obrigatorio)" -ForegroundColor White
Write-Host "   - INTERNET (para download)" -ForegroundColor White
Write-Host ""
Write-Host "6. Verificar se FileProvider esta configurado corretamente:" -ForegroundColor Green
Write-Host "   - authorities=`"`${applicationId}.fileprovider`"" -ForegroundColor White
Write-Host "   - resource=`"@xml/file_paths`"" -ForegroundColor White
Write-Host ""


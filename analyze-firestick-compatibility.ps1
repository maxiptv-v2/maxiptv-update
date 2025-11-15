# Script Profissional: Analise de Compatibilidade Fire OS vs Android
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "ANALISE DE COMPATIBILIDADE: Fire OS vs Android Normal" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$apkDownloaderFile = "app/src/main/java/com/maxiptv/data/ApkDownloader.kt"

Write-Host "[COMPATIBILIDADE 1] Verificando APIs e metodos usados..." -ForegroundColor Yellow
Write-Host ""

# Verificar APIs que podem nao funcionar no Fire OS
Write-Host "[1.1] Verificando DownloadManager..." -ForegroundColor Cyan
$downloadManager = Select-String -Path $apkDownloaderFile -Pattern "DownloadManager|enqueue|query|COLUMN_STATUS" -Context 0, 2
if ($downloadManager) {
    Write-Host "  ✅ DownloadManager sendo usado" -ForegroundColor Green
    
    # Verificar se verifica versao do Android
    $checksVersion = Select-String -Path $apkDownloaderFile -Pattern "Build\.VERSION|SDK_INT|VERSION_CODES" -Context 0, 2
    if ($checksVersion) {
        Write-Host "  ✅ Verifica versao do Android antes de usar APIs" -ForegroundColor Green
        $checksVersion | Select-Object -First 3 | ForEach-Object {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ⚠️  Pode nao estar verificando versao do Android" -ForegroundColor Yellow
        Write-Host "     Fire OS pode ter APIs diferentes" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ DownloadManager nao encontrado!" -ForegroundColor Red
}

Write-Host ""

Write-Host "[1.2] Verificando FileProvider e compatibilidade..." -ForegroundColor Cyan
$fileProvider = Select-String -Path $apkDownloaderFile -Pattern "FileProvider|getUriForFile|VERSION_CODES\.N" -Context 0, 5
if ($fileProvider) {
    Write-Host "  ✅ FileProvider sendo usado" -ForegroundColor Green
    
    # Verificar se tem fallback para versoes antigas
    $hasFallback = $fileProvider | Where-Object {
        $context = $_.Context
        ($_.Line -match "fallback|Uri\.fromFile|VERSION_CODES\.N") -or
        ($context.PreContext -match "fallback|Uri\.fromFile") -or
        ($context.PostContext -match "fallback|Uri\.fromFile")
    }
    
    if ($hasFallback) {
        Write-Host "  ✅ Tem fallback para versoes antigas do Fire OS" -ForegroundColor Green
        $hasFallback | Select-Object -First 3 | ForEach-Object {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ⚠️  Pode nao ter fallback para Fire OS antigo" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ FileProvider nao encontrado!" -ForegroundColor Red
}

Write-Host ""

Write-Host "[COMPATIBILIDADE 2] Verificando diferencas especificas do Fire OS..." -ForegroundColor Yellow
Write-Host ""

Write-Host "[2.1] Verificando PackageInstaller vs PackageManager..." -ForegroundColor Cyan
$packageInstaller = Select-String -Path $apkDownloaderFile -Pattern "PackageInstaller|PackageManager|resolveActivity|canRequestPackageInstalls" -Context 0, 3
if ($packageInstaller) {
    Write-Host "  ✅ PackageManager sendo usado" -ForegroundColor Green
    
    # Verificar se usa resolveActivity antes de startActivity
    $checksResolve = $packageInstaller | Where-Object {
        $context = $_.Context
        ($_.Line -match "resolveActivity") -or
        ($context.PreContext -match "resolveActivity") -or
        ($context.PostContext -match "resolveActivity")
    }
    
    if ($checksResolve) {
        Write-Host "  ✅ Verifica resolveActivity antes de iniciar (importante para Fire OS)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Pode nao estar verificando resolveActivity" -ForegroundColor Yellow
        Write-Host "     Fire OS pode nao ter PackageInstaller padrao" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ PackageManager nao encontrado!" -ForegroundColor Red
}

Write-Host ""

Write-Host "[2.2] Verificando tratamento de permissoes no Fire OS..." -ForegroundColor Cyan
$permissions = Select-String -Path $apkDownloaderFile -Pattern "canInstallPackages|requestInstallPermission|ACTION_MANAGE_UNKNOWN_APP" -Context 0, 5
if ($permissions) {
    Write-Host "  ✅ Tratamento de permissoes encontrado" -ForegroundColor Green
    
    # Verificar se verifica permissao ANTES de tentar instalar
    $checksBefore = $permissions | Where-Object {
        $context = $_.Context
        ($_.Line -match "canInstallPackages|if.*canInstall") -or
        ($context.PreContext -match "canInstallPackages|if.*canInstall") -or
        ($context.PostContext -match "canInstallPackages|if.*canInstall")
    }
    
    if ($checksBefore) {
        Write-Host "  ✅ Verifica permissao ANTES de instalar (correto)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Pode nao estar verificando permissao antes de instalar" -ForegroundColor Yellow
    }
    
    # Verificar se tem tratamento especifico para Fire OS
    $firePermission = $permissions | Where-Object {
        $context = $_.Context
        ($_.Line -match "isFire|Fire") -or
        ($context.PreContext -match "isFire|Fire") -or
        ($context.PostContext -match "isFire|Fire")
    }
    
    if ($firePermission) {
        Write-Host "  ✅ Tem tratamento especifico para Fire OS" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Pode nao ter tratamento especifico para Fire OS" -ForegroundColor Yellow
        Write-Host "     Fire OS pode ter comportamento diferente nas permissoes" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ Tratamento de permissoes nao encontrado!" -ForegroundColor Red
}

Write-Host ""

Write-Host "[COMPATIBILIDADE 3] Verificando problemas conhecidos do Fire OS..." -ForegroundColor Yellow
Write-Host ""

Write-Host "[3.1] Verificando timing e sincronizacao..." -ForegroundColor Cyan
$timing = Select-String -Path $apkDownloaderFile -Pattern "Thread\.sleep|delay|waitTime|1500|1000" -Context 0, 2
if ($timing) {
    Write-Host "  ✅ Tempos de espera implementados" -ForegroundColor Green
    
    # Verificar se tem tempos diferentes para Fire OS
    $fireTiming = $timing | Where-Object {
        $context = $_.Context
        ($_.Line -match "isFire.*1500|1500.*isFire|Fire.*sleep") -or
        ($context.PreContext -match "isFire.*1500|1500.*isFire") -or
        ($context.PostContext -match "isFire.*1500|1500.*isFire")
    }
    
    if ($fireTiming) {
        Write-Host "  ✅ Tem tempos maiores para Fire OS (correto)" -ForegroundColor Green
        $fireTiming | Select-Object -First 2 | ForEach-Object {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ⚠️  Pode nao ter tempos especificos para Fire OS" -ForegroundColor Yellow
        Write-Host "     Fire OS precisa de mais tempo para escrever arquivo" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  Tempos de espera podem estar faltando" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[3.2] Verificando tratamento quando app fecha..." -ForegroundColor Cyan
$appCloses = Select-String -Path $apkDownloaderFile -Pattern "checkPendingDownload|SharedPreferences.*download|downloadId.*prefs" -Context 0, 3
if ($appCloses) {
    Write-Host "  ✅ Tem tratamento para quando app fecha" -ForegroundColor Green
    
    # Verificar se verifica STATUS_SUCCESSFUL corretamente
    $checksStatus = Select-String -Path $apkDownloaderFile -Pattern "STATUS_SUCCESSFUL|STATUS_COMPLETE|checkDownloadStatus" -Context 0, 5
    if ($checksStatus) {
        Write-Host "  ✅ Verifica status do download corretamente" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Pode nao estar verificando status corretamente" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: Nao tem tratamento para quando app fecha!" -ForegroundColor Red
    Write-Host "     Fire OS fecha app durante download - precisa resumir depois" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[COMPATIBILIDADE 4] Verificando problemas especificos conhecidos..." -ForegroundColor Yellow
Write-Host ""

Write-Host "[4.1] Problema conhecido: Fire OS pode nao ter PackageInstaller padrao" -ForegroundColor Cyan
$resolveCheck = Select-String -Path $apkDownloaderFile -Pattern "resolveActivity.*installIntent|resolveInfo.*null" -Context 0, 5
if ($resolveCheck) {
    Write-Host "  ✅ Verifica se resolveActivity retorna null" -ForegroundColor Green
    $resolveCheck | Select-Object -First 2 | ForEach-Object {
        Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️  Pode nao estar tratando caso resolveActivity retorne null" -ForegroundColor Yellow
    Write-Host "     Fire OS pode nao ter app para instalar APK" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[4.2] Problema conhecido: Fire OS pode bloquear instalacao de fontes desconhecidas" -ForegroundColor Cyan
$unknownSources = Select-String -Path $apkDownloaderFile -Pattern "ACTION_MANAGE_UNKNOWN_APP|unknown.*sources|Settings" -Context 0, 3
if ($unknownSources) {
    Write-Host "  ✅ Abre configuracoes para permitir fontes desconhecidas" -ForegroundColor Green
    $unknownSources | Select-Object -First 2 | ForEach-Object {
        Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️  Pode nao estar abrindo configuracoes para permitir fontes desconhecidas" -ForegroundColor Yellow
    Write-Host "     Fire OS bloqueia instalacao por padrao" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[4.3] Problema conhecido: Fire OS pode ter caminhos de arquivo diferentes" -ForegroundColor Cyan
$multiplePaths = Select-String -Path $apkDownloaderFile -Pattern "/Download|/Downloads|possiblePaths|altPath" -Context 0, 3
if ($multiplePaths) {
    Write-Host "  ✅ Verifica multiplos caminhos de arquivo" -ForegroundColor Green
    $pathCount = ($multiplePaths | Where-Object { $_.Line -match "/Download|altPath" }).Count
    Write-Host "     Encontradas $pathCount referencias a caminhos alternativos" -ForegroundColor Gray
} else {
    Write-Host "  ⚠️  Pode nao estar verificando caminhos alternativos" -ForegroundColor Yellow
    Write-Host "     Fire OS pode salvar em /Download ou /Downloads" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[4.4] Problema conhecido: Fire OS pode ter comportamento diferente no BroadcastReceiver" -ForegroundColor Cyan
$broadcastReceiver = Select-String -Path $apkDownloaderFile -Pattern "BroadcastReceiver|registerReceiver|unregisterReceiver" -Context 0, 5
if ($broadcastReceiver) {
    Write-Host "  ✅ BroadcastReceiver implementado" -ForegroundColor Green
    
    # Verificar se usa ApplicationContext
    $usesAppContext = $broadcastReceiver | Where-Object {
        $context = $_.Context
        ($_.Line -match "applicationContext") -or
        ($context.PreContext -match "applicationContext") -or
        ($context.PostContext -match "applicationContext")
    }
    
    if ($usesAppContext) {
        Write-Host "  ✅ Usa ApplicationContext (correto para Fire OS)" -ForegroundColor Green
    } else {
        Write-Host "  ❌ PROBLEMA: Pode nao estar usando ApplicationContext!" -ForegroundColor Red
        Write-Host "     Fire OS pode destruir Activity durante download" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: BroadcastReceiver nao encontrado!" -ForegroundColor Red
}

Write-Host ""

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "PROBLEMAS DE COMPATIBILIDADE IDENTIFICADOS:" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$compatibilityIssues = @()

# Verificar problemas de compatibilidade
$resolveCheck = Select-String -Path $apkDownloaderFile -Pattern "resolveActivity.*null|if.*resolveInfo" -Quiet
if (-not $resolveCheck) {
    $compatibilityIssues += "Pode nao estar tratando caso resolveActivity retorne null (Fire OS pode nao ter PackageInstaller)"
}

$hasFireTiming = Select-String -Path $apkDownloaderFile -Pattern "isFire.*1500|1500.*isFire" -Quiet
if (-not $hasFireTiming) {
    $compatibilityIssues += "Pode nao ter tempos de espera especificos para Fire OS (1500ms vs 500ms)"
}

$hasMultiplePaths = Select-String -Path $apkDownloaderFile -Pattern "possiblePaths|altPath.*Download" -Quiet
if (-not $hasMultiplePaths) {
    $compatibilityIssues += "Pode nao estar verificando multiplos caminhos de arquivo (/Download vs /Downloads)"
}

Write-Host "PROBLEMAS DE COMPATIBILIDADE:" -ForegroundColor Red
if ($compatibilityIssues.Count -eq 0) {
    Write-Host "  ✅ Nenhum problema de compatibilidade encontrado!" -ForegroundColor Green
} else {
    $compatibilityIssues | ForEach-Object {
        Write-Host "  ⚠️  $_" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "DIFERENCAS CONHECIDAS: Fire OS vs Android Normal" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Fire OS pode nao ter PackageInstaller padrao do Android" -ForegroundColor Yellow
Write-Host "   -> Solucao: Verificar resolveActivity antes de startActivity" -ForegroundColor Green
Write-Host ""
Write-Host "2. Fire OS fecha app quando inicia instalacao" -ForegroundColor Yellow
Write-Host "   -> Solucao: Usar ApplicationContext e SharedPreferences" -ForegroundColor Green
Write-Host ""
Write-Host "3. Fire OS pode salvar arquivos em caminhos diferentes" -ForegroundColor Yellow
Write-Host "   -> Solucao: Verificar /Download e /Downloads" -ForegroundColor Green
Write-Host ""
Write-Host "4. Fire OS precisa de mais tempo para escrever arquivo" -ForegroundColor Yellow
Write-Host "   -> Solucao: Thread.sleep(1500ms) para Fire OS vs 500ms Android" -ForegroundColor Green
Write-Host ""
Write-Host "5. Fire OS pode bloquear instalacao por padrao" -ForegroundColor Yellow
Write-Host "   -> Solucao: Abrir Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES" -ForegroundColor Green
Write-Host ""
Write-Host "6. Fire OS pode ter comportamento diferente no BroadcastReceiver" -ForegroundColor Yellow
Write-Host "   -> Solucao: Usar ApplicationContext para registrar receiver" -ForegroundColor Green
Write-Host ""
Write-Host "7. Fire OS pode nao suportar ACTION_INSTALL_PACKAGE" -ForegroundColor Yellow
Write-Host "   -> Solucao: Usar ACTION_VIEW ao inves de ACTION_INSTALL_PACKAGE" -ForegroundColor Green
Write-Host ""
Write-Host "8. Fire OS pode ter FileProvider com caminhos diferentes" -ForegroundColor Yellow
Write-Host "   -> Solucao: Configurar file_paths.xml com todos os caminhos" -ForegroundColor Green
Write-Host ""


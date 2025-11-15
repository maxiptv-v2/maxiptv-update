# Script Profissional de Analise: Requisitos para Atualizacao no Fire Stick Amazon
# Analisa o codigo do ApkDownloader para identificar problemas especificos do Fire OS

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "ANALISE PROFISSIONAL: Requisitos para Atualizacao Fire Stick" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$apkDownloaderFile = "app/src/main/java/com/maxiptv/data/ApkDownloader.kt"
$mainActivityFile = "app/src/main/java/com/maxiptv/MainActivity.kt"
$manifestFile = "app/src/main/AndroidManifest.xml"

if (-not (Test-Path $apkDownloaderFile)) {
    Write-Host "[ERRO] Arquivo nao encontrado: $apkDownloaderFile" -ForegroundColor Red
    exit 1
}

Write-Host "[1] ANALISANDO CODIGO DO ApkDownloader..." -ForegroundColor Yellow
Write-Host ""

# 1. Verificar deteccao do Fire OS
Write-Host "[1.1] Verificando deteccao do Fire OS:" -ForegroundColor Cyan
$fireOSDetection = Select-String -Path $apkDownloaderFile -Pattern "isFireOS|isFireStick|amazon" -Context 0, 3
if ($fireOSDetection) {
    Write-Host "  ✅ Funcao isFireOS encontrada" -ForegroundColor Green
    $fireOSDetection | ForEach-Object {
        if ($_.Line -match "isFireOS|amazon") {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
    }
} else {
    Write-Host "  ❌ PROBLEMA: Deteccao do Fire OS nao encontrada!" -ForegroundColor Red
}

Write-Host ""

# 2. Verificar ApplicationContext
Write-Host "[1.2] Verificando uso de ApplicationContext:" -ForegroundColor Cyan
$appContext = Select-String -Path $apkDownloaderFile -Pattern "applicationContext|ApplicationContext" -Context 0, 2
if ($appContext) {
    Write-Host "  ✅ ApplicationContext sendo usado" -ForegroundColor Green
    $appContext | Select-Object -First 5 | ForEach-Object {
        Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: ApplicationContext nao esta sendo usado!" -ForegroundColor Red
    Write-Host "     Isso causa fechamento do app quando Activity fecha durante download" -ForegroundColor Yellow
}

Write-Host ""

# 3. Verificar SharedPreferences para persistencia
Write-Host "[1.3] Verificando persistencia com SharedPreferences:" -ForegroundColor Cyan
$sharedPrefs = Select-String -Path $apkDownloaderFile -Pattern "SharedPreferences|saveDownloadInfo|checkPendingDownload" -Context 0, 3
if ($sharedPrefs) {
    Write-Host "  ✅ SharedPreferences implementado" -ForegroundColor Green
    $sharedPrefs | Select-Object -First 8 | ForEach-Object {
        if ($_.Line -match "SharedPreferences|saveDownloadInfo|checkPendingDownload|KEY_DOWNLOAD|KEY_FILE") {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: SharedPreferences nao encontrado!" -ForegroundColor Red
    Write-Host "     Download pendente nao sera resumido quando app reabre" -ForegroundColor Yellow
}

Write-Host ""

# 4. Verificar checkPendingDownload no MainActivity
Write-Host "[1.4] Verificando checkPendingDownload no MainActivity:" -ForegroundColor Cyan
if (Test-Path $mainActivityFile) {
    $checkPending = Select-String -Path $mainActivityFile -Pattern "checkPendingDownload" -Context 2, 2
    if ($checkPending) {
        Write-Host "  ✅ checkPendingDownload chamado no MainActivity.onCreate" -ForegroundColor Green
        $checkPending | ForEach-Object {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ❌ PROBLEMA CRITICO: checkPendingDownload nao chamado no MainActivity!" -ForegroundColor Red
        Write-Host "     Downloads pendentes nao serao verificados ao abrir o app" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  MainActivity nao encontrado para verificacao" -ForegroundColor Yellow
}

Write-Host ""

# 5. Verificar caminhos de arquivo multiplos para Fire OS
Write-Host "[1.5] Verificando multiplos caminhos de arquivo para Fire OS:" -ForegroundColor Cyan
$multiplePaths = Select-String -Path $apkDownloaderFile -Pattern "/Download|/Downloads|File\(|exists\(\)" -Context 0, 2
if ($multiplePaths) {
    Write-Host "  ✅ Multiplos caminhos de arquivo implementados" -ForegroundColor Green
    $pathCount = ($multiplePaths | Where-Object { $_.Line -match "/Download|/Downloads" }).Count
    Write-Host "     Encontrados $pathCount referencias a caminhos de download" -ForegroundColor Gray
} else {
    Write-Host "  ⚠️  Multiplos caminhos podem nao estar implementados" -ForegroundColor Yellow
}

Write-Host ""

# 6. Verificar tempos de espera (Thread.sleep) para Fire OS
Write-Host "[1.6] Verificando tempos de espera especificos para Fire OS:" -ForegroundColor Cyan
$waitTimes = Select-String -Path $apkDownloaderFile -Pattern "Thread\.sleep|delay|waitTime|isFire" -Context 0, 2
if ($waitTimes) {
    Write-Host "  ✅ Tempos de espera condicionais encontrados" -ForegroundColor Green
    $waitTimes | Where-Object { $_.Line -match "sleep|waitTime|isFire" } | Select-Object -First 5 | ForEach-Object {
        Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️  Tempos de espera podem nao estar adequados para Fire OS" -ForegroundColor Yellow
}

Write-Host ""

# 7. Verificar flags de Intent para Fire OS
Write-Host "[1.7] Verificando flags de Intent especificas para Fire OS:" -ForegroundColor Cyan
$intentFlags = Select-String -Path $apkDownloaderFile -Pattern "FLAG_|ACTION_VIEW|ACTION_INSTALL|Intent\(" -Context 0, 3
if ($intentFlags) {
    Write-Host "  ✅ Flags de Intent encontradas" -ForegroundColor Green
    $fireFlags = $intentFlags | Where-Object { $_.Line -match "FLAG_ACTIVITY_CLEAR_TOP|FLAG_ACTIVITY_NEW_TASK|isFire" }
    if ($fireFlags) {
        Write-Host "     Flags especificas para Fire OS encontradas:" -ForegroundColor Gray
        $fireFlags | Select-Object -First 5 | ForEach-Object {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ⚠️  Flags especificas para Fire OS podem estar faltando" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ PROBLEMA: Flags de Intent nao encontradas!" -ForegroundColor Red
}

Write-Host ""

# 8. Verificar BroadcastReceiver com ApplicationContext
Write-Host "[1.8] Verificando BroadcastReceiver com ApplicationContext:" -ForegroundColor Cyan
$broadcastReceiver = Select-String -Path $apkDownloaderFile -Pattern "BroadcastReceiver|registerDownloadReceiver|applicationContext" -Context 0, 5
if ($broadcastReceiver) {
    Write-Host "  ✅ BroadcastReceiver encontrado" -ForegroundColor Green
    $hasAppContext = $broadcastReceiver | Where-Object { $_.Line -match "applicationContext" }
    if ($hasAppContext) {
        Write-Host "     ✅ Usando ApplicationContext no BroadcastReceiver" -ForegroundColor Green
    } else {
        Write-Host "  ❌ PROBLEMA: BroadcastReceiver pode nao estar usando ApplicationContext!" -ForegroundColor Red
    }
} else {
    Write-Host "  ❌ PROBLEMA CRITICO: BroadcastReceiver nao encontrado!" -ForegroundColor Red
}

Write-Host ""

# 9. Verificar permissoes no AndroidManifest
Write-Host "[1.9] Verificando permissoes no AndroidManifest:" -ForegroundColor Cyan
if (Test-Path $manifestFile) {
    $permissions = Select-String -Path $manifestFile -Pattern "REQUEST_INSTALL|INTERNET|WRITE_EXTERNAL|READ_EXTERNAL" -Context 0, 1
    if ($permissions) {
        Write-Host "  ✅ Permissoes encontradas:" -ForegroundColor Green
        $permissions | ForEach-Object {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
        
        $hasInstallPermission = $permissions | Where-Object { $_.Line -match "REQUEST_INSTALL" }
        if (-not $hasInstallPermission) {
            Write-Host "  ⚠️  ATENCAO: Permissao REQUEST_INSTALL_PACKAGES pode estar faltando" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ❌ PROBLEMA: Permissoes nao encontradas!" -ForegroundColor Red
    }
} else {
    Write-Host "  ⚠️  AndroidManifest nao encontrado" -ForegroundColor Yellow
}

Write-Host ""

# 10. Verificar FileProvider configurado
Write-Host "[1.10] Verificando FileProvider no AndroidManifest:" -ForegroundColor Cyan
if (Test-Path $manifestFile) {
    $fileProvider = Select-String -Path $manifestFile -Pattern "FileProvider|fileprovider|file_paths" -Context 0, 3
    if ($fileProvider) {
        Write-Host "  ✅ FileProvider configurado" -ForegroundColor Green
        $fileProvider | ForEach-Object {
            Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ❌ PROBLEMA CRITICO: FileProvider nao configurado!" -ForegroundColor Red
        Write-Host "     Instalacao de APK falhara no Android 7.0+" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  AndroidManifest nao encontrado" -ForegroundColor Yellow
}

Write-Host ""

# 11. Verificar tratamento de erro especifico para Fire OS
Write-Host "[1.11] Verificando tratamento de erros especifico para Fire OS:" -ForegroundColor Cyan
$errorHandling = Select-String -Path $apkDownloaderFile -Pattern "catch|Exception|Error|isFire" -Context 0, 3
if ($errorHandling) {
    $fireErrorHandling = $errorHandling | Where-Object { 
        $context = $_.Context
        ($_.Line -match "isFire|Fire") -or 
        ($context.PreContext -match "isFire|Fire") -or 
        ($context.PostContext -match "isFire|Fire")
    }
    if ($fireErrorHandling) {
        Write-Host "  ✅ Tratamento de erro especifico para Fire OS encontrado" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Tratamento de erro pode nao ser especifico para Fire OS" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  Tratamento de erro nao encontrado" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTICO FINAL:" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Resumo dos problemas encontrados
$problems = @()
$warnings = @()

# Verificar cada requisito critico
if (-not (Select-String -Path $apkDownloaderFile -Pattern "applicationContext" -Quiet)) {
    $problems += "ApplicationContext nao usado - app fecha quando Activity fecha"
}

if (-not (Select-String -Path $apkDownloaderFile -Pattern "SharedPreferences.*download|saveDownloadInfo" -Quiet)) {
    $problems += "SharedPreferences nao usado - download pendente nao e resumido"
}

if (Test-Path $mainActivityFile) {
    if (-not (Select-String -Path $mainActivityFile -Pattern "checkPendingDownload" -Quiet)) {
        $problems += "checkPendingDownload nao chamado no MainActivity"
    }
}

if (-not (Select-String -Path $apkDownloaderFile -Pattern "FLAG_ACTIVITY_CLEAR_TOP|FLAG_ACTIVITY_NEW_TASK" -Quiet)) {
    $warnings += "Flags de Intent podem estar incompletas para Fire OS"
}

if (Test-Path $manifestFile) {
    if (-not (Select-String -Path $manifestFile -Pattern "FileProvider" -Quiet)) {
        $problems += "FileProvider nao configurado no AndroidManifest"
    }
}

Write-Host "PROBLEMAS CRITICOS ENCONTRADOS:" -ForegroundColor Red
if ($problems.Count -eq 0) {
    Write-Host "  ✅ Nenhum problema critico encontrado!" -ForegroundColor Green
} else {
    $problems | ForEach-Object {
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
Write-Host "REQUISITOS PARA FIRE STICK FUNCIONAR:" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. ✅ ApplicationContext deve ser usado (nao Activity context)" -ForegroundColor Green
Write-Host "2. ✅ SharedPreferences para salvar downloadId e fileName" -ForegroundColor Green
Write-Host "3. ✅ checkPendingDownload() chamado no MainActivity.onCreate" -ForegroundColor Green
Write-Host "4. ✅ BroadcastReceiver usando ApplicationContext" -ForegroundColor Green
Write-Host "5. ✅ Multiplos caminhos de arquivo (/Download, /Downloads)" -ForegroundColor Green
Write-Host "6. ✅ Tempos de espera maiores para Fire OS (1500ms vs 500ms)" -ForegroundColor Green
Write-Host "7. ✅ Flags de Intent corretas (FLAG_ACTIVITY_CLEAR_TOP para Fire OS)" -ForegroundColor Green
Write-Host "8. ✅ FileProvider configurado no AndroidManifest" -ForegroundColor Green
Write-Host "9. ✅ Permissao REQUEST_INSTALL_PACKAGES no AndroidManifest" -ForegroundColor Green
Write-Host "10. ✅ Intent.ACTION_VIEW ao inves de ACTION_INSTALL_PACKAGE" -ForegroundColor Green
Write-Host ""


# Script Profissional: Diagnostico Problema Atualizacao TV Box Android
# Identifica por que a atualizacao nao esta funcionando na TV Box

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTICO: Problema Atualizacao TV Box" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$issues = @()
$warnings = @()
$findings = @()

# 1. Verificar ApkDownloader.kt - Logica de instalacao
Write-Host "[1] Analisando ApkDownloader.kt..." -ForegroundColor Yellow
$apkDownloader = Get-Content "app\src\main\java\com\maxiptv\data\ApkDownloader.kt" -Raw -ErrorAction SilentlyContinue

if (-not $apkDownloader) {
    $issues += "ApkDownloader.kt nao encontrado!"
    Write-Host "  X Arquivo nao encontrado" -ForegroundColor Red
    exit 1
}

Write-Host "  OK Arquivo encontrado" -ForegroundColor Green

# Verificar se trata TV Box diferente de Fire OS
if ($apkDownloader -match "isFireOS|isFire") {
    Write-Host "  OK Tratamento Fire OS encontrado" -ForegroundColor Green
    
    # Verificar se TV Box tem tratamento especifico
    if ($apkDownloader -match "!isFire|else.*Fire|if.*!isFire") {
        Write-Host "  OK TV Box tem tratamento especifico (else do Fire OS)" -ForegroundColor Green
    } else {
        $warnings += "ApkDownloader.kt: TV Box pode estar usando mesmo tratamento do Fire OS"
        Write-Host "  ! TV Box pode estar usando tratamento do Fire OS" -ForegroundColor Yellow
    }
} else {
    $issues += "ApkDownloader.kt: Tratamento Fire OS nao encontrado"
    Write-Host "  X Tratamento Fire OS nao encontrado" -ForegroundColor Red
}

# Verificar BroadcastReceiver
if ($apkDownloader -match "BroadcastReceiver|registerReceiver|ACTION_DOWNLOAD_COMPLETE") {
    Write-Host "  OK BroadcastReceiver encontrado" -ForegroundColor Green
    
    # Verificar se usa ApplicationContext
    if ($apkDownloader -match "registerReceiver.*applicationContext|ApplicationContext.*registerReceiver") {
        Write-Host "  OK BroadcastReceiver usa ApplicationContext" -ForegroundColor Green
    } else {
        $warnings += "ApkDownloader.kt: BroadcastReceiver pode nao estar usando ApplicationContext"
        Write-Host "  ! BroadcastReceiver pode nao estar usando ApplicationContext" -ForegroundColor Yellow
    }
} else {
    $issues += "ApkDownloader.kt: BroadcastReceiver nao encontrado"
    Write-Host "  X BroadcastReceiver nao encontrado" -ForegroundColor Red
}

# Verificar se installApk e chamado apos download
if ($apkDownloader -match "installApk.*fileName|installApk\(.*fileName\)") {
    Write-Host "  OK installApk chamado apos download" -ForegroundColor Green
} else {
    $issues += "ApkDownloader.kt: installApk pode nao estar sendo chamado"
    Write-Host "  X installApk pode nao estar sendo chamado" -ForegroundColor Red
}

# Verificar se verifica permissao antes de instalar
if ($apkDownloader -match "canInstallPackages.*before|Verificar permissao.*antes") {
    Write-Host "  OK Verifica permissao antes de instalar" -ForegroundColor Green
} else {
    $warnings += "ApkDownloader.kt: Pode nao verificar permissao antes de instalar"
    Write-Host "  ! Pode nao verificar permissao antes de instalar" -ForegroundColor Yellow
}

# Verificar se procura arquivo em multiplos caminhos
if ($apkDownloader -match "possiblePaths|searchDirs|multiple.*path") {
    Write-Host "  OK Procura arquivo em multiplos caminhos" -ForegroundColor Green
} else {
    $warnings += "ApkDownloader.kt: Pode nao procurar em multiplos caminhos"
    Write-Host "  ! Pode nao procurar em multiplos caminhos" -ForegroundColor Yellow
}

# Verificar se tem checkPendingDownload
if ($apkDownloader -match "checkPendingDownload|fun checkPendingDownload") {
    Write-Host "  OK checkPendingDownload encontrado" -ForegroundColor Green
} else {
    $warnings += "ApkDownloader.kt: checkPendingDownload pode estar faltando"
    Write-Host "  ! checkPendingDownload pode estar faltando" -ForegroundColor Yellow
}

# 2. Verificar UpdateManager.kt - Verificacao de versao
Write-Host ""
Write-Host "[2] Analisando UpdateManager.kt..." -ForegroundColor Yellow
$updateManager = Get-Content "app\src\main\java\com\maxiptv\data\UpdateManager.kt" -Raw -ErrorAction SilentlyContinue

if (-not $updateManager) {
    $warnings += "UpdateManager.kt nao encontrado"
    Write-Host "  ! Arquivo nao encontrado" -ForegroundColor Yellow
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar como compara versoes
    if ($updateManager -match "compareVersion|versionCompare|compare.*version") {
        Write-Host "  OK Comparacao de versao encontrada" -ForegroundColor Green
    } else {
        $warnings += "UpdateManager.kt: Comparacao de versao pode estar faltando"
        Write-Host "  ! Comparacao de versao pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar getCurrentVersionName
    if ($updateManager -match "getCurrentVersionName|currentVersion|versionName") {
        Write-Host "  OK getCurrentVersionName encontrado" -ForegroundColor Green
    } else {
        $warnings += "UpdateManager.kt: getCurrentVersionName pode estar faltando"
        Write-Host "  ! getCurrentVersionName pode estar faltando" -ForegroundColor Yellow
    }
}

# 3. Verificar MainActivity.kt - checkPendingDownload
Write-Host ""
Write-Host "[3] Verificando MainActivity.kt..." -ForegroundColor Yellow
$mainActivity = Get-Content "app\src\main\java\com\maxiptv\MainActivity.kt" -Raw -ErrorAction SilentlyContinue

if ($mainActivity) {
    if ($mainActivity -match "checkPendingDownload|ApkDownloader\.checkPendingDownload") {
        Write-Host "  OK checkPendingDownload chamado no onCreate" -ForegroundColor Green
    } else {
        $issues += "MainActivity.kt: checkPendingDownload nao chamado no onCreate"
        Write-Host "  X checkPendingDownload nao chamado no onCreate" -ForegroundColor Red
    }
} else {
    $warnings += "MainActivity.kt nao encontrado ou nao pode ser lido"
    Write-Host "  ! Arquivo nao encontrado" -ForegroundColor Yellow
}

# 4. Verificar se ha tratamento especifico para TV Box (nao Fire OS)
Write-Host ""
Write-Host "[4] Verificando tratamento especifico TV Box..." -ForegroundColor Yellow

# Verificar se TV Box tem tratamento diferente
$tvBoxSpecific = $false
if ($apkDownloader -match "isTvBox|TvBox|tvBox|TV Box") {
    $tvBoxSpecific = $true
    Write-Host "  OK Tratamento especifico TV Box encontrado" -ForegroundColor Green
} else {
    Write-Host "  - Tratamento especifico TV Box nao encontrado (usa tratamento generico)" -ForegroundColor Gray
}

# Verificar se tem tratamento para quando nao e Fire OS
if ($apkDownloader -match "!isFire|else.*Fire|if.*!isFire") {
    Write-Host "  OK Tratamento para nao-Fire OS encontrado" -ForegroundColor Green
} else {
    $warnings += "ApkDownloader.kt: Pode nao ter tratamento especifico para TV Box"
    Write-Host "  ! Pode nao ter tratamento especifico para TV Box" -ForegroundColor Yellow
}

# 5. Verificar problemas conhecidos
Write-Host ""
Write-Host "[5] Verificando problemas conhecidos..." -ForegroundColor Yellow

# Problema 1: BroadcastReceiver pode nao estar recebendo evento
Write-Host "  Possivel problema: BroadcastReceiver pode nao estar recebendo evento" -ForegroundColor Gray
Write-Host "    Solucao: Verificar se esta registrado com ApplicationContext" -ForegroundColor Gray

# Problema 2: Arquivo pode nao estar sendo encontrado
Write-Host "  Possivel problema: Arquivo pode nao estar sendo encontrado apos download" -ForegroundColor Gray
Write-Host "    Solucao: Verificar caminhos de busca e permissoes" -ForegroundColor Gray

# Problema 3: Permissao pode nao estar sendo solicitada
Write-Host "  Possivel problema: Permissao de instalacao pode nao estar sendo solicitada" -ForegroundColor Gray
Write-Host "    Solucao: Verificar canInstallPackages() e requestInstallPermission()" -ForegroundColor Gray

# Problema 4: Intent pode estar falhando silenciosamente
Write-Host "  Possivel problema: Intent de instalacao pode estar falhando silenciosamente" -ForegroundColor Gray
Write-Host "    Solucao: Adicionar mais logs e tratamento de erros" -ForegroundColor Gray

# Problema 5: Versao pode nao estar sendo atualizada corretamente
Write-Host "  Possivel problema: Versao pode nao estar sendo atualizada no build.gradle.kts" -ForegroundColor Gray
Write-Host "    Solucao: Verificar se versionCode e versionName estao corretos" -ForegroundColor Gray

# Resumo e recomendacoes
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTICO COMPLETO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($issues.Count -gt 0) {
    Write-Host "PROBLEMAS CRITICOS ENCONTRADOS ($($issues.Count)):" -ForegroundColor Red
    Write-Host ""
    foreach ($issue in $issues) {
        Write-Host "  X $issue" -ForegroundColor Red
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "POSSIVEIS PROBLEMAS ($($warnings.Count)):" -ForegroundColor Yellow
    Write-Host ""
    foreach ($warning in $warnings) {
        Write-Host "  ! $warning" -ForegroundColor Yellow
    }
    Write-Host ""
}

Write-Host "POSSIVEIS CAUSAS DO PROBLEMA:" -ForegroundColor Magenta
Write-Host ""
Write-Host "1. BROADCASTRECEIVER NAO ESTA RECEBENDO EVENTO:" -ForegroundColor White
Write-Host "   - BroadcastReceiver pode nao estar registrado corretamente" -ForegroundColor Gray
Write-Host "   - App pode estar fechando antes do download completar" -ForegroundColor Gray
Write-Host "   - DownloadManager pode nao estar enviando broadcast" -ForegroundColor Gray
Write-Host ""
Write-Host "2. ARQUIVO NAO ESTA SENDO ENCONTRADO:" -ForegroundColor White
Write-Host "   - Caminho do arquivo pode estar incorreto" -ForegroundColor Gray
Write-Host "   - Permissoes de leitura podem estar faltando" -ForegroundColor Gray
Write-Host "   - Arquivo pode estar sendo deletado antes da instalacao" -ForegroundColor Gray
Write-Host ""
Write-Host "3. INSTALACAO ESTA FALHANDO SILENCIOSAMENTE:" -ForegroundColor White
Write-Host "   - Permissao de instalacao pode nao estar sendo solicitada" -ForegroundColor Gray
Write-Host "   - Intent pode estar falhando sem mostrar erro" -ForegroundColor Gray
Write-Host "   - PackageInstaller pode nao estar disponivel" -ForegroundColor Gray
Write-Host ""
Write-Host "4. VERSAO NAO ESTA SENDO ATUALIZADA:" -ForegroundColor White
Write-Host "   - versionCode pode nao estar sendo incrementado" -ForegroundColor Gray
Write-Host "   - Comparacao de versao pode estar incorreta" -ForegroundColor Gray
Write-Host "   - Cache pode estar mantendo versao antiga" -ForegroundColor Gray
Write-Host ""

Write-Host "RECOMENDACOES PARA CORRIGIR:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. ADICIONAR MAIS LOGS:" -ForegroundColor White
Write-Host "   - Log quando BroadcastReceiver recebe evento" -ForegroundColor Gray
Write-Host "   - Log quando arquivo e encontrado/nao encontrado" -ForegroundColor Gray
Write-Host "   - Log quando instalacao e iniciada/falha" -ForegroundColor Gray
Write-Host ""
Write-Host "2. VERIFICAR PERMISSOES:" -ForegroundColor White
Write-Host "   - Verificar se REQUEST_INSTALL_PACKAGES esta no manifest" -ForegroundColor Gray
Write-Host "   - Verificar se permissao esta sendo solicitada antes de instalar" -ForegroundColor Gray
Write-Host "   - Verificar se usuario concedeu permissao" -ForegroundColor Gray
Write-Host ""
Write-Host "3. MELHORAR TRATAMENTO DE ERROS:" -ForegroundColor White
Write-Host "   - Capturar todas as excecoes e logar" -ForegroundColor Gray
Write-Host "   - Mostrar mensagem de erro ao usuario se falhar" -ForegroundColor Gray
Write-Host "   - Tentar novamente automaticamente se falhar" -ForegroundColor Gray
Write-Host ""
Write-Host "4. VERIFICAR CAMINHOS:" -ForegroundColor White
Write-Host "   - Adicionar mais caminhos de busca para TV Box" -ForegroundColor Gray
Write-Host "   - Verificar permissoes de leitura nos caminhos" -ForegroundColor Gray
Write-Host "   - Logar todos os caminhos verificados" -ForegroundColor Gray
Write-Host ""

if ($issues.Count -eq 0) {
    Write-Host "Nenhum problema critico encontrado no codigo." -ForegroundColor Green
    Write-Host "O problema pode ser:" -ForegroundColor Yellow
    Write-Host "  - Permissao nao concedida pelo usuario" -ForegroundColor Yellow
    Write-Host "  - BroadcastReceiver nao esta recebendo evento" -ForegroundColor Yellow
    Write-Host "  - Arquivo nao esta sendo encontrado apos download" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Recomendado adicionar mais logs e tratamento de erros." -ForegroundColor Cyan
    exit 0
} else {
    Write-Host "Corrija os problemas criticos antes de compilar!" -ForegroundColor Red
    exit 1
}


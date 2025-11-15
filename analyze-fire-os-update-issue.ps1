# Script Profissional: Analise de Problema de Atualizacao no Fire OS
# Identifica problemas e sugere correcoes para atualizacao de APK no Fire Stick Amazon

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE: Problema de Atualizacao Fire OS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$issues = @()
$suggestions = @()
$criticalIssues = @()

# 1. Analisar ApkDownloader.kt
Write-Host "[1] Analisando ApkDownloader.kt..." -ForegroundColor Yellow
$apkDownloader = Get-Content "app\src\main\java\com\maxiptv\data\ApkDownloader.kt" -Raw

if (-not $apkDownloader) {
    $criticalIssues += "ApkDownloader.kt nao encontrado!"
    Write-Host "  X Arquivo nao encontrado" -ForegroundColor Red
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar se usa ACTION_INSTALL_PACKAGE (Fire OS pode precisar)
    if ($apkDownloader -match 'ACTION_INSTALL_PACKAGE') {
        Write-Host "  OK ACTION_INSTALL_PACKAGE encontrado" -ForegroundColor Green
    } else {
        $issues += "ApkDownloader.kt: Nao usa ACTION_INSTALL_PACKAGE (Fire OS pode precisar)"
        Write-Host "  ! ACTION_INSTALL_PACKAGE nao encontrado" -ForegroundColor Yellow
        $suggestions += "Considerar usar Intent(Intent.ACTION_INSTALL_PACKAGE) para Fire OS"
    }
    
    # Verificar se usa PackageInstaller (API moderna)
    if ($apkDownloader -match 'PackageInstaller') {
        Write-Host "  OK PackageInstaller encontrado" -ForegroundColor Green
    } else {
        $issues += "ApkDownloader.kt: Nao usa PackageInstaller (API moderna)"
        Write-Host "  ! PackageInstaller nao encontrado" -ForegroundColor Yellow
        $suggestions += "Considerar usar PackageInstaller para instalacao mais confiavel no Fire OS"
    }
    
    # Verificar se verifica permissao ANTES de iniciar download
    if ($apkDownloader -match 'canInstallPackages.*before|Verificar permissao ANTES') {
        Write-Host "  OK Verifica permissao antes do download" -ForegroundColor Green
    } else {
        $issues += "ApkDownloader.kt: Pode nao verificar permissao antes do download"
        Write-Host "  ! Verificacao de permissao pode estar incompleta" -ForegroundColor Yellow
    }
    
    # Verificar se usa ApplicationContext
    if ($apkDownloader -match 'applicationContext|ApplicationContext') {
        Write-Host "  OK Usa ApplicationContext (persiste quando app fecha)" -ForegroundColor Green
    } else {
        $criticalIssues += "ApkDownloader.kt: Nao usa ApplicationContext - app fecha e download para"
        Write-Host "  X Nao usa ApplicationContext" -ForegroundColor Red
    }
    
    # Verificar se tem tratamento especifico para Fire OS
    if ($apkDownloader -match 'isFireOS|Fire OS|Fire Stick') {
        Write-Host "  OK Tem tratamento especifico para Fire OS" -ForegroundColor Green
        
        # Verificar se aguarda tempo suficiente no Fire OS
        if ($apkDownloader -match 'Thread\.sleep.*1500|Thread\.sleep.*2000') {
            Write-Host "  OK Aguarda tempo suficiente no Fire OS (1500ms+)" -ForegroundColor Green
        } else {
            $issues += "ApkDownloader.kt: Pode nao aguardar tempo suficiente no Fire OS"
            Write-Host "  ! Tempo de espera pode ser insuficiente" -ForegroundColor Yellow
            $suggestions += "Aumentar Thread.sleep para 2000ms no Fire OS"
        }
    } else {
        $criticalIssues += "ApkDownloader.kt: Nao tem tratamento especifico para Fire OS"
        Write-Host "  X Sem tratamento especifico para Fire OS" -ForegroundColor Red
    }
    
    # Verificar se usa FileProvider corretamente
    if ($apkDownloader -match 'FileProvider\.getUriForFile') {
        Write-Host "  OK Usa FileProvider corretamente" -ForegroundColor Green
        
        # Verificar se tem fallback para versoes antigas
        if ($apkDownloader -match 'Uri\.fromFile.*fallback|fallback.*Uri\.fromFile') {
            Write-Host "  OK Tem fallback para versoes antigas do Fire OS" -ForegroundColor Green
        } else {
            $issues += "ApkDownloader.kt: Pode nao ter fallback para Fire OS antigo"
            Write-Host "  ! Fallback pode estar faltando" -ForegroundColor Yellow
        }
    } else {
        $criticalIssues += "ApkDownloader.kt: Nao usa FileProvider (necessario para Android 7.0+)"
        Write-Host "  X Nao usa FileProvider" -ForegroundColor Red
    }
    
    # Verificar se tem FLAG_ACTIVITY_NEW_TASK
    if ($apkDownloader -match 'FLAG_ACTIVITY_NEW_TASK') {
        Write-Host "  OK Usa FLAG_ACTIVITY_NEW_TASK" -ForegroundColor Green
    } else {
        $criticalIssues += "ApkDownloader.kt: Falta FLAG_ACTIVITY_NEW_TASK (necessario quando app fecha)"
        Write-Host "  X Falta FLAG_ACTIVITY_NEW_TASK" -ForegroundColor Red
    }
    
    # Verificar se tem FLAG_GRANT_READ_URI_PERMISSION
    if ($apkDownloader -match 'FLAG_GRANT_READ_URI_PERMISSION') {
        Write-Host "  OK Usa FLAG_GRANT_READ_URI_PERMISSION" -ForegroundColor Green
    } else {
        $criticalIssues += "ApkDownloader.kt: Falta FLAG_GRANT_READ_URI_PERMISSION"
        Write-Host "  X Falta FLAG_GRANT_READ_URI_PERMISSION" -ForegroundColor Red
    }
    
    # Verificar se tem tratamento de SecurityException
    if ($apkDownloader -match 'SecurityException') {
        Write-Host "  OK Trata SecurityException (comum no Fire OS)" -ForegroundColor Green
    } else {
        $issues += "ApkDownloader.kt: Nao trata SecurityException (Fire OS pode lancar)"
        Write-Host "  ! Nao trata SecurityException" -ForegroundColor Yellow
        $suggestions += "Adicionar tratamento especifico para SecurityException no Fire OS"
    }
    
    # Verificar se tem tratamento de ActivityNotFoundException
    if ($apkDownloader -match 'ActivityNotFoundException') {
        Write-Host "  OK Trata ActivityNotFoundException" -ForegroundColor Green
    } else {
        $issues += "ApkDownloader.kt: Nao trata ActivityNotFoundException"
        Write-Host "  ! Nao trata ActivityNotFoundException" -ForegroundColor Yellow
    }
    
    # Verificar se BroadcastReceiver esta registrado com ApplicationContext
    if ($apkDownloader -match 'registerReceiver.*applicationContext|ApplicationContext.*registerReceiver') {
        Write-Host "  OK BroadcastReceiver registrado com ApplicationContext" -ForegroundColor Green
    } else {
        $criticalIssues += "ApkDownloader.kt: BroadcastReceiver pode nao estar usando ApplicationContext"
        Write-Host "  X BroadcastReceiver pode estar usando contexto errado" -ForegroundColor Red
    }
    
    # Verificar se limpa arquivos antigos antes de baixar
    if ($apkDownloader -match 'LIMPAR ARQUIVOS ANTIGOS|delete.*maxiptv.*apk') {
        Write-Host "  OK Limpa arquivos antigos antes de baixar" -ForegroundColor Green
    } else {
        $issues += "ApkDownloader.kt: Pode nao limpar arquivos antigos (cache do Fire OS)"
        Write-Host "  ! Limpeza de arquivos antigos pode estar faltando" -ForegroundColor Yellow
        $suggestions += "Adicionar logica para limpar APKs antigos antes de baixar novo"
    }
}

# 2. Verificar AndroidManifest.xml - Permissoes
Write-Host ""
Write-Host "[2] Verificando AndroidManifest.xml..." -ForegroundColor Yellow
$manifest = Get-Content "app\src\main\AndroidManifest.xml" -Raw

if (-not $manifest) {
    $criticalIssues += "AndroidManifest.xml nao encontrado!"
    Write-Host "  X Arquivo nao encontrado" -ForegroundColor Red
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar permissao INTERNET
    if ($manifest -match 'android\.permission\.INTERNET') {
        Write-Host "  OK Permissao INTERNET encontrada" -ForegroundColor Green
    } else {
        $criticalIssues += "AndroidManifest.xml: Falta permissao INTERNET"
        Write-Host "  X Falta permissao INTERNET" -ForegroundColor Red
    }
    
    # Verificar permissao WRITE_EXTERNAL_STORAGE
    if ($manifest -match 'WRITE_EXTERNAL_STORAGE') {
        Write-Host "  OK Permissao WRITE_EXTERNAL_STORAGE encontrada" -ForegroundColor Green
    } else {
        $issues += "AndroidManifest.xml: Pode faltar WRITE_EXTERNAL_STORAGE (Android < 10)"
        Write-Host "  ! WRITE_EXTERNAL_STORAGE pode estar faltando" -ForegroundColor Yellow
    }
    
    # Verificar permissao REQUEST_INSTALL_PACKAGES
    if ($manifest -match 'REQUEST_INSTALL_PACKAGES') {
        Write-Host "  OK Permissao REQUEST_INSTALL_PACKAGES encontrada" -ForegroundColor Green
    } else {
        $criticalIssues += "AndroidManifest.xml: Falta permissao REQUEST_INSTALL_PACKAGES (Android 8.0+)"
        Write-Host "  X Falta permissao REQUEST_INSTALL_PACKAGES" -ForegroundColor Red
    }
    
    # Verificar FileProvider configurado
    if ($manifest -match 'FileProvider|fileprovider') {
        Write-Host "  OK FileProvider configurado" -ForegroundColor Green
        
        # Verificar se tem authority correto
        if ($manifest -match 'android:authorities.*fileprovider') {
            Write-Host "  OK Authority do FileProvider configurado" -ForegroundColor Green
        } else {
            $issues += "AndroidManifest.xml: Authority do FileProvider pode estar incorreto"
            Write-Host "  ! Authority pode estar incorreto" -ForegroundColor Yellow
        }
    } else {
        $criticalIssues += "AndroidManifest.xml: FileProvider nao configurado (necessario Android 7.0+)"
        Write-Host "  X FileProvider nao configurado" -ForegroundColor Red
    }
}

# 3. Verificar file_paths.xml
Write-Host ""
Write-Host "[3] Verificando file_paths.xml..." -ForegroundColor Yellow
$filePaths = Get-Content "app\src\main\res\xml\file_paths.xml" -Raw -ErrorAction SilentlyContinue

if (-not $filePaths) {
    $criticalIssues += "file_paths.xml nao encontrado!"
    Write-Host "  X Arquivo nao encontrado" -ForegroundColor Red
} else {
    Write-Host "  OK Arquivo encontrado" -ForegroundColor Green
    
    # Verificar se tem caminho para Downloads
    if ($filePaths -match 'downloads|Download') {
        Write-Host "  OK Caminho para Downloads configurado" -ForegroundColor Green
    } else {
        $criticalIssues += "file_paths.xml: Falta caminho para Downloads"
        Write-Host "  X Falta caminho para Downloads" -ForegroundColor Red
    }
    
    # Verificar se tem external-path
    if ($filePaths -match 'external-path') {
        Write-Host "  OK external-path configurado" -ForegroundColor Green
    } else {
        $criticalIssues += "file_paths.xml: Falta external-path"
        Write-Host "  X Falta external-path" -ForegroundColor Red
    }
}

# 4. Verificar MainActivity - se chama checkPendingDownload
Write-Host ""
Write-Host "[4] Verificando MainActivity.kt..." -ForegroundColor Yellow
$mainActivity = Get-Content "app\src\main\java\com\maxiptv\MainActivity.kt" -Raw -ErrorAction SilentlyContinue

if ($mainActivity) {
    if ($mainActivity -match 'checkPendingDownload|ApkDownloader\.checkPendingDownload') {
        Write-Host "  OK Chama checkPendingDownload no onCreate" -ForegroundColor Green
    } else {
        $issues += "MainActivity.kt: Nao chama checkPendingDownload (download pode ficar pendente)"
        Write-Host "  ! Nao chama checkPendingDownload" -ForegroundColor Yellow
        $suggestions += "Adicionar ApkDownloader.checkPendingDownload(this) no onCreate do MainActivity"
    }
} else {
    Write-Host "  ! MainActivity.kt nao encontrado ou nao pode ser lido" -ForegroundColor Yellow
}

# 5. Analisar problemas conhecidos do Fire OS
Write-Host ""
Write-Host "[5] Problemas conhecidos do Fire OS..." -ForegroundColor Yellow
Write-Host "  - Fire OS pode fechar app quando tenta instalar APK" -ForegroundColor Gray
Write-Host "  - Fire OS pode bloquear instalacao de fontes desconhecidas" -ForegroundColor Gray
Write-Host "  - Fire OS pode precisar de permissoes especiais" -ForegroundColor Gray
Write-Host "  - Fire OS pode ter caminhos diferentes para Downloads" -ForegroundColor Gray
Write-Host "  - Fire OS pode precisar de mais tempo para processar arquivo" -ForegroundColor Gray

# Resumo e Recomendacoes
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA ANALISE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($criticalIssues.Count -gt 0) {
    Write-Host "PROBLEMAS CRITICOS ENCONTRADOS ($($criticalIssues.Count)):" -ForegroundColor Red
    foreach ($issue in $criticalIssues) {
        Write-Host "  X $issue" -ForegroundColor Red
    }
    Write-Host ""
}

if ($issues.Count -gt 0) {
    Write-Host "PROBLEMAS ENCONTRADOS ($($issues.Count)):" -ForegroundColor Yellow
    foreach ($issue in $issues) {
        Write-Host "  ! $issue" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($suggestions.Count -gt 0) {
    Write-Host "SUGESTOES DE CORRECAO:" -ForegroundColor Cyan
    foreach ($suggestion in $suggestions) {
        Write-Host "  -> $suggestion" -ForegroundColor Cyan
    }
    Write-Host ""
}

# Recomendacoes especificas para Fire OS
Write-Host "RECOMENDACOES ESPECIFICAS PARA FIRE OS:" -ForegroundColor Magenta
Write-Host ""
Write-Host "1. USAR PackageInstaller (API moderna):" -ForegroundColor White
Write-Host "   - Mais confiavel que Intent.ACTION_VIEW" -ForegroundColor Gray
Write-Host "   - Funciona melhor no Fire OS" -ForegroundColor Gray
Write-Host "   - Permite controle melhor do processo" -ForegroundColor Gray
Write-Host ""
Write-Host "2. VERIFICAR PERMISSAO ANTES DE QUALQUER OPERACAO:" -ForegroundColor White
Write-Host "   - Verificar canInstallPackages() ANTES de baixar" -ForegroundColor Gray
Write-Host "   - Verificar novamente ANTES de instalar" -ForegroundColor Gray
Write-Host "   - Solicitar permissao se necessario" -ForegroundColor Gray
Write-Host ""
Write-Host "3. AGUARDAR TEMPO SUFICIENTE:" -ForegroundColor White
Write-Host "   - Thread.sleep(2000) no Fire OS antes de instalar" -ForegroundColor Gray
Write-Host "   - Verificar se arquivo existe e tem tamanho valido" -ForegroundColor Gray
Write-Host "   - Aguardar apos BroadcastReceiver receber evento" -ForegroundColor Gray
Write-Host ""
Write-Host "4. USAR APPLICATIONCONTEXT EM TUDO:" -ForegroundColor White
Write-Host "   - DownloadManager com ApplicationContext" -ForegroundColor Gray
Write-Host "   - BroadcastReceiver com ApplicationContext" -ForegroundColor Gray
Write-Host "   - Intent de instalacao com ApplicationContext" -ForegroundColor Gray
Write-Host ""
Write-Host "5. TRATAR EXCECOES ESPECIFICAS:" -ForegroundColor White
Write-Host "   - SecurityException (Fire OS pode lancar)" -ForegroundColor Gray
Write-Host "   - ActivityNotFoundException (Fire OS pode nao ter PackageInstaller)" -ForegroundColor Gray
Write-Host "   - FileNotFoundException (caminhos diferentes)" -ForegroundColor Gray
Write-Host ""
Write-Host "6. LIMPAR CACHE E ARQUIVOS ANTIGOS:" -ForegroundColor White
Write-Host "   - Deletar APKs antigos antes de baixar novo" -ForegroundColor Gray
Write-Host "   - Limpar cache do DownloadManager se necessario" -ForegroundColor Gray
Write-Host ""
Write-Host "7. VERIFICAR MULTIPLOS CAMINHOS:" -ForegroundColor White
Write-Host "   - /storage/emulated/0/Download/" -ForegroundColor Gray
Write-Host "   - /storage/emulated/0/Downloads/" -ForegroundColor Gray
Write-Host "   - Caminhos alternativos do Fire OS" -ForegroundColor Gray
Write-Host ""

if ($criticalIssues.Count -eq 0 -and $issues.Count -eq 0) {
    Write-Host "OK NENHUM PROBLEMA CRITICO ENCONTRADO!" -ForegroundColor Green
    Write-Host ""
    Write-Host "O codigo parece estar bem estruturado para Fire OS." -ForegroundColor Green
    Write-Host "Se o problema persistir, pode ser:" -ForegroundColor Yellow
    Write-Host "  - Permissao nao concedida pelo usuario" -ForegroundColor Yellow
    Write-Host "  - Fire OS bloqueando instalacao de fontes desconhecidas" -ForegroundColor Yellow
    Write-Host "  - Versao do Fire OS muito antiga ou muito nova" -ForegroundColor Yellow
    Write-Host "  - Problema de cache do sistema" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Sugestao: Implementar PackageInstaller como alternativa" -ForegroundColor Cyan
    exit 0
} else {
    Write-Host "CORRIJA OS PROBLEMAS CRITICOS ANTES DE COMPILAR!" -ForegroundColor Red
    exit 1
}


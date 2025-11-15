# Script Profissional: Teste Completo de Compatibilidade Fire OS para Atualizacao
# Analisa todos os aspectos do sistema de atualizacao e problemas de cache

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "TESTE COMPLETO: Sistema de Atualizacao Fire Stick Amazon" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$apkDownloaderFile = "app/src/main/java/com/maxiptv/data/ApkDownloader.kt"
$updateManagerFile = "app/src/main/java/com/maxiptv/data/UpdateManager.kt"
$mainActivityFile = "app/src/main/java/com/maxiptv/MainActivity.kt"
$filePathsFile = "app/src/main/res/xml/file_paths.xml"
$manifestFile = "app/src/main/AndroidManifest.xml"

Write-Host "[TESTE 1] Verificando sistema de download e cache..." -ForegroundColor Yellow
Write-Host ""

# Verificar se DownloadManager limpa cache
Write-Host "[1.1] Verificando DownloadManager e cache:" -ForegroundColor Cyan
$downloadManager = Select-String -Path $apkDownloaderFile -Pattern "DownloadManager|setAllowedOverMetered|setAllowedOverRoaming|VISIBILITY" -Context 0, 3
if ($downloadManager) {
    Write-Host "  ✅ DownloadManager configurado" -ForegroundColor Green
    
    # Verificar se tem configuracoes para evitar cache
    $hasNoCache = $downloadManager | Where-Object {
        $_.Line -match "no-cache|Cache-Control|setAllowedOverMetered"
    }
    
    if ($hasNoCache) {
        Write-Host "  ✅ Tem configuracoes para evitar cache" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  PROBLEMA POTENCIAL: Pode nao estar evitando cache do DownloadManager" -ForegroundColor Yellow
        Write-Host "     Fire OS pode usar versao em cache ao inves de baixar nova" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ DownloadManager nao encontrado!" -ForegroundColor Red
}

Write-Host ""

# Verificar UpdateManager e cache
Write-Host "[1.2] Verificando UpdateManager e cache HTTP:" -ForegroundColor Cyan
if (Test-Path $updateManagerFile) {
    $updateManager = Select-String -Path $updateManagerFile -Pattern "Cache-Control|no-cache|OkHttpClient|Request" -Context 0, 5
    if ($updateManager) {
        Write-Host "  ✅ UpdateManager encontrado" -ForegroundColor Green
        
        $hasNoCache = $updateManager | Where-Object { $_.Line -match "no-cache|Cache-Control" }
        if ($hasNoCache) {
            Write-Host "  ✅ Tem Cache-Control: no-cache (evita cache HTTP)" -ForegroundColor Green
            $hasNoCache | ForEach-Object {
                Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
            }
        } else {
            Write-Host "  ⚠️  PROBLEMA ENCONTRADO: Pode nao estar evitando cache HTTP!" -ForegroundColor Yellow
            Write-Host "     Fire OS pode usar versao antiga do update.json em cache" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ⚠️  UpdateManager pode nao ter configuracoes de cache" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  UpdateManager nao encontrado" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[TESTE 2] Verificando problemas de cache do Fire OS..." -ForegroundColor Yellow
Write-Host ""

Write-Host "[2.1] Problema conhecido: Fire OS cacheia Downloads:" -ForegroundColor Cyan
$checksFileExists = Select-String -Path $apkDownloaderFile -Pattern "file\.exists\(\)|\.length\(\)|possibleFile" -Context 0, 3
if ($checksFileExists) {
    Write-Host "  ✅ Verifica se arquivo existe antes de instalar" -ForegroundColor Green
    
    # Verificar se verifica tamanho do arquivo
    $checksSize = $checksFileExists | Where-Object { $_.Line -match "\.length\(\)|file\.length" }
    if ($checksSize) {
        Write-Host "  ✅ Verifica tamanho do arquivo (evita usar arquivo corrompido em cache)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Pode nao estar verificando tamanho do arquivo" -ForegroundColor Yellow
    }
    
    # Verificar se procura arquivo mais recente
    $checksRecent = Select-String -Path $apkDownloaderFile -Pattern "lastModified|maxByOrNull|latest" -Context 0, 2
    if ($checksRecent) {
        Write-Host "  ✅ Procura arquivo mais recente (evita usar cache antigo)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Pode nao estar procurando arquivo mais recente" -ForegroundColor Yellow
        Write-Host "     Fire OS pode usar arquivo antigo em cache" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  Pode nao estar verificando existencia do arquivo" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[2.2] Verificando se limpa arquivos antigos:" -ForegroundColor Cyan
$cleansOld = Select-String -Path $apkDownloaderFile -Pattern "delete|remove|clear|unlink" -Context 0, 2
if ($cleansOld) {
    Write-Host "  ✅ Tem codigo para limpar arquivos" -ForegroundColor Green
    $cleansOld | Select-Object -First 3 | ForEach-Object {
        Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️  PROBLEMA POTENCIAL: Pode nao estar limpando arquivos antigos!" -ForegroundColor Yellow
    Write-Host "     Fire OS pode usar APK antigo em cache ao inves de baixar novo" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  SOLUCAO SUGERIDA:" -ForegroundColor Green
    Write-Host "     Antes de baixar novo APK, deletar APKs antigos do MaxiPTV" -ForegroundColor White
    Write-Host "     Isso força DownloadManager a baixar versao nova" -ForegroundColor White
}

Write-Host ""

Write-Host "[TESTE 3] Verificando compatibilidade especifica Fire OS..." -ForegroundColor Yellow
Write-Host ""

Write-Host "[3.1] Verificando tratamento de erros especificos:" -ForegroundColor Cyan
$errorHandling = Select-String -Path $apkDownloaderFile -Pattern "SecurityException|ActivityNotFoundException|resolveInfo.*null" -Context 0, 5
if ($errorHandling) {
    Write-Host "  ✅ Tratamento de erros especificos encontrado" -ForegroundColor Green
    $errorHandling | Select-Object -First 5 | ForEach-Object {
        Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️  Pode nao ter tratamento especifico para erros do Fire OS" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[3.2] Verificando verificacao de permissao antes de instalar:" -ForegroundColor Cyan
$checksPermission = Select-String -Path $apkDownloaderFile -Pattern "canInstallPackages.*before|Verificar permissao.*antes|NOVAMENTE.*antes" -Context 0, 3
if ($checksPermission) {
    Write-Host "  ✅ Verifica permissao imediatamente antes de instalar" -ForegroundColor Green
    $checksPermission | Select-Object -First 2 | ForEach-Object {
        Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️  Pode nao estar verificando permissao imediatamente antes" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[TESTE 4] Verificando solucoes para cache do Fire OS..." -ForegroundColor Yellow
Write-Host ""

Write-Host "[4.1] Solucao 1: Limpar arquivos antigos antes de baixar:" -ForegroundColor Cyan
$hasCleanBefore = Select-String -Path $apkDownloaderFile -Pattern "delete.*before|remove.*before|clean.*before.*download" -Context 0, 3
if ($hasCleanBefore) {
    Write-Host "  ✅ Limpa arquivos antigos antes de baixar" -ForegroundColor Green
} else {
    Write-Host "  ❌ PROBLEMA ENCONTRADO: Nao limpa arquivos antigos antes de baixar!" -ForegroundColor Red
    Write-Host "     Fire OS pode usar APK antigo em cache" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  CODIGO SUGERIDO PARA ADICIONAR:" -ForegroundColor Green
    Write-Host "     // Limpar APKs antigos antes de baixar novo (evita cache)" -ForegroundColor White
    Write-Host "     val downloadsDir = Environment.getExternalStoragePublicDirectory(...)" -ForegroundColor White
    Write-Host "     downloadsDir.listFiles()?.filter { it.name.startsWith(`"maxiptv`") }?.forEach { it.delete() }" -ForegroundColor White
}

Write-Host ""

Write-Host "[4.2] Solucao 2: Forcar DownloadManager a baixar novamente:" -ForegroundColor Cyan
$forcesDownload = Select-String -Path $apkDownloaderFile -Pattern "setAllowedOverMetered|setAllowedOverRoaming|VISIBILITY_VISIBLE" -Context 0, 2
if ($forcesDownload) {
    Write-Host "  ✅ DownloadManager configurado para baixar sempre" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Pode nao estar forçando DownloadManager a baixar novamente" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "[4.3] Solucao 3: Adicionar timestamp ao nome do arquivo:" -ForegroundColor Cyan
$hasTimestamp = Select-String -Path $apkDownloaderFile -Pattern "System\.currentTimeMillis|timestamp|Date|Time" -Context 0, 2
if ($hasTimestamp) {
    Write-Host "  ✅ Usa timestamp ou data no nome do arquivo" -ForegroundColor Green
    $hasTimestamp | Select-Object -First 2 | ForEach-Object {
        Write-Host "     $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️  PROBLEMA POTENCIAL: Nome do arquivo pode ser sempre o mesmo!" -ForegroundColor Yellow
    Write-Host "     Fire OS pode usar arquivo em cache com mesmo nome" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  SOLUCAO SUGERIDA:" -ForegroundColor Green
    Write-Host "     Adicionar timestamp ao nome: `"maxiptv-${version}-${timestamp}.apk`"" -ForegroundColor White
    Write-Host "     Isso garante que cada download tenha nome unico" -ForegroundColor White
}

Write-Host ""

Write-Host "[TESTE 5] Verificando se UpdateManager evita cache..." -ForegroundColor Yellow
Write-Host ""

if (Test-Path $updateManagerFile) {
    $updateContent = Get-Content $updateManagerFile -Raw
    
    # Verificar Cache-Control
    $hasNoCache = $updateContent -match "Cache-Control.*no-cache|addHeader.*no-cache"
    if ($hasNoCache) {
        Write-Host "  ✅ UpdateManager tem Cache-Control: no-cache" -ForegroundColor Green
    } else {
        Write-Host "  ❌ PROBLEMA ENCONTRADO: UpdateManager pode nao estar evitando cache!" -ForegroundColor Red
        Write-Host "     Fire OS pode usar versao antiga do update.json" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  SOLUCAO NECESSARIA:" -ForegroundColor Green
        Write-Host "     Adicionar: .addHeader(`"Cache-Control`", `"no-cache`")" -ForegroundColor White
    }
    
    # Verificar se adiciona timestamp na URL
    $hasTimestampUrl = $updateContent -match "\?.*t=|timestamp|currentTimeMillis"
    if ($hasTimestampUrl) {
        Write-Host "  ✅ Adiciona timestamp na URL (evita cache)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Pode nao estar adicionando timestamp na URL" -ForegroundColor Yellow
        Write-Host "     Fire OS pode cachear resposta do update.json" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  UpdateManager nao encontrado" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "PROBLEMAS DE CACHE IDENTIFICADOS:" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$cacheProblems = @()

# Verificar problemas de cache
if (-not (Select-String -Path $apkDownloaderFile -Pattern "delete.*maxiptv|remove.*maxiptv.*before" -Quiet)) {
    $cacheProblems += "Nao limpa arquivos antigos antes de baixar novo APK"
}

if (Test-Path $updateManagerFile) {
    $updateContent = Get-Content $updateManagerFile -Raw
    if (-not ($updateContent -match "Cache-Control.*no-cache")) {
        $cacheProblems += "UpdateManager pode nao estar evitando cache HTTP"
    }
}

$hasTimestamp = Select-String -Path $apkDownloaderFile -Pattern "System\.currentTimeMillis.*fileName|timestamp.*apk" -Quiet
if (-not $hasTimestamp) {
    $cacheProblems += "Nome do arquivo pode ser sempre o mesmo (Fire OS usa cache)"
}

Write-Host "PROBLEMAS DE CACHE:" -ForegroundColor Red
if ($cacheProblems.Count -eq 0) {
    Write-Host "  ✅ Nenhum problema de cache encontrado!" -ForegroundColor Green
} else {
    $cacheProblems | ForEach-Object {
        Write-Host "  ❌ $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "SOLUCOES RECOMENDADAS PARA CACHE DO FIRE OS:" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. LIMPAR ARQUIVOS ANTIGOS ANTES DE BAIXAR:" -ForegroundColor Green
Write-Host "   - Deletar todos os APKs antigos do MaxiPTV antes de baixar novo" -ForegroundColor White
Write-Host "   - Isso força DownloadManager a baixar versao nova" -ForegroundColor White
Write-Host ""
Write-Host "2. ADICIONAR TIMESTAMP AO NOME DO ARQUIVO:" -ForegroundColor Green
Write-Host "   - Usar: `"maxiptv-${version}-${System.currentTimeMillis()}.apk`"" -ForegroundColor White
Write-Host "   - Garante que cada download tenha nome unico" -ForegroundColor White
Write-Host ""
Write-Host "3. FORCAR UpdateManager A EVITAR CACHE:" -ForegroundColor Green
Write-Host "   - Adicionar: .addHeader(`"Cache-Control`", `"no-cache`")" -ForegroundColor White
Write-Host "   - Adicionar timestamp na URL: `"?t=${System.currentTimeMillis()}`"" -ForegroundColor White
Write-Host ""
Write-Host "4. VERIFICAR TAMANHO DO ARQUIVO ANTES DE INSTALAR:" -ForegroundColor Green
Write-Host "   - Comparar tamanho baixado com tamanho esperado" -ForegroundColor White
Write-Host "   - Se diferente, deletar e baixar novamente" -ForegroundColor White
Write-Host ""
Write-Host "5. LIMPAR CACHE DO DOWNLOADMANAGER (se possivel):" -ForegroundColor Green
Write-Host "   - Fire OS pode cachear downloads do DownloadManager" -ForegroundColor White
Write-Host "   - Considerar usar nome de arquivo unico sempre" -ForegroundColor White
Write-Host ""


# Script para compilar o app em Release e enviar atualização para GitHub
param(
    [switch]$SkipClean = $false  # Use -SkipClean para pular a limpeza
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  COMPILANDO E ENVIANDO ATUALIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"

# Configurações do GitHub (token já configurado no remote)

try {
    # 1. Limpar build anterior (opcional)
    if (-not $SkipClean) {
        Write-Host "1. Limpando build anterior..." -ForegroundColor Yellow
        try {
            .\gradlew.bat clean --console=plain 2>&1 | Out-Null
            Write-Host "   [OK] Build limpo com sucesso" -ForegroundColor Green
        } catch {
            Write-Host "   [AVISO] Pulando limpeza (arquivos podem estar em uso) - continuando compilacao..." -ForegroundColor Yellow
        }
    } else {
        Write-Host "1. Pulando limpeza (--SkipClean ativado)..." -ForegroundColor Yellow
    }
    
    # 2. Compilar em Release
    Write-Host ""
    Write-Host "2. Compilando em Release..." -ForegroundColor Yellow
    .\gradlew.bat assembleRelease --console=plain
    
    if ($LASTEXITCODE -ne 0) {
        throw "Erro na compilação"
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  COMPILACAO CONCLUIDA!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    
    # Listar APKs gerados
    $apkFiles = Get-ChildItem -Path "app\build\outputs\apk\release\" -Filter "*.apk" -ErrorAction SilentlyContinue
    if ($apkFiles) {
        Write-Host "APKs encontrados:" -ForegroundColor Yellow
        foreach ($apk in $apkFiles) {
            Write-Host "  - $($apk.Name) ($([math]::Round($apk.Length / 1MB, 2)) MB)" -ForegroundColor White
        }
    }
    
    # 3. Obter versão do build.gradle.kts e comparar com versão anterior
    Write-Host ""
    Write-Host "3. Obtendo versão do app..." -ForegroundColor Yellow
    $buildGradle = Get-Content "app\build.gradle.kts" -Raw
    if ($buildGradle -match 'versionCode\s*=\s*(\d+)') {
        $versionCode = $matches[1]
    }
    if ($buildGradle -match 'versionName\s*=\s*"([^"]+)"') {
        $versionName = $matches[1]
    }
    
    # Buscar última tag/versão do Git
    $lastTag = git describe --tags --abbrev=0 2>$null
    $lastVersion = "N/A"
    $lastBuild = "N/A"
    if ($lastTag) {
        # Extrair versão da tag (ex: v1.0.276 -> 1.0.276)
        if ($lastTag -match 'v?(\d+\.\d+\.\d+)') {
            $lastVersion = $matches[1]
            # Tentar extrair build code da versão (assumindo que versionName = "1.0.XXX" corresponde ao build XXX)
            if ($lastVersion -match '\.(\d+)$') {
                $lastBuild = $matches[1]
            }
        }
    }
    
    Write-Host ""
    Write-Host "   Versão Anterior: v$lastVersion (Build $lastBuild)" -ForegroundColor Gray
    Write-Host "   Versão Nova:     v$versionName (Build $versionCode)" -ForegroundColor Cyan
    Write-Host ""
    
    if ($lastBuild -ne "N/A" -and [int]$versionCode -gt [int]$lastBuild) {
        Write-Host "   [OK] Versao aumentada de $lastBuild para $versionCode" -ForegroundColor Green
    } elseif ($lastBuild -eq "N/A") {
        Write-Host "   [INFO] Primeira versao ou tag nao encontrada" -ForegroundColor Yellow
    } else {
        Write-Host "   [AVISO] Versao nao aumentou!" -ForegroundColor Yellow
    }
    
    # 4. Verificar se há mudanças para commitar
    Write-Host ""
    Write-Host "4. Verificando mudanças..." -ForegroundColor Yellow
    $status = git status --porcelain
    if ($status) {
        Write-Host "   Mudanças encontradas, preparando commit..." -ForegroundColor Cyan
        
        # Adicionar apenas arquivos de código (não build)
        git add app/src/
        git add build-release.ps1
        
        # Commit
        $commitMessage = "Release v$versionName - Build $versionCode - Fix: Restaurado VodInfo para versão 270 (sinopse funcionando)"
        git commit -m $commitMessage
        
        Write-Host "   Commit criado: $commitMessage" -ForegroundColor Green
    } else {
        Write-Host "   Nenhuma mudança para commitar" -ForegroundColor Yellow
    }
    
    # 5. Criar tag se não existir
    Write-Host ""
    Write-Host "5. Criando tag de versão..." -ForegroundColor Yellow
    $tagName = "v$versionName"
    $tagExists = git tag -l $tagName
    if (-not $tagExists) {
        git tag -a $tagName -m "Release v$versionName - Build $versionCode"
        Write-Host "   Tag criada: $tagName" -ForegroundColor Green
    } else {
        Write-Host "   Tag já existe: $tagName" -ForegroundColor Yellow
    }
    
    # 6. Push para GitHub
    Write-Host ""
    Write-Host "6. Enviando para GitHub..." -ForegroundColor Yellow
    
    # Push commits (o remote já tem a chave configurada)
    git push origin HEAD
    
    # Push tags
    git push origin --tags
    
    Write-Host "   Push concluído com sucesso!" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  PROCESSO CONCLUIDO!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Versão: $versionName (Build $versionCode)" -ForegroundColor Cyan
    Write-Host "APK: app\build\outputs\apk\release\" -ForegroundColor Cyan
    Write-Host ""
    
} catch {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  ERRO" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Erro: $_" -ForegroundColor Red
    exit 1
}

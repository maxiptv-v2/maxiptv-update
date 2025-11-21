# Script para compilar o app em Release e enviar atualização para GitHub
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  COMPILANDO E ENVIANDO ATUALIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"

# Configurações do GitHub (token já configurado no remote)

try {
    # 1. Limpar build anterior (com tratamento de erro)
    Write-Host "1. Limpando build anterior..." -ForegroundColor Yellow
    try {
        .\gradlew.bat clean --console=plain 2>&1 | Out-Null
    } catch {
        Write-Host "   ⚠️ Não foi possível limpar completamente (arquivos podem estar em uso)" -ForegroundColor Yellow
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
    
    # 3. Obter versão do build.gradle.kts
    Write-Host ""
    Write-Host "3. Obtendo versão do app..." -ForegroundColor Yellow
    $buildGradle = Get-Content "app\build.gradle.kts" -Raw
    if ($buildGradle -match 'versionCode\s*=\s*(\d+)') {
        $versionCode = $matches[1]
    }
    if ($buildGradle -match 'versionName\s*=\s*"([^"]+)"') {
        $versionName = $matches[1]
    }
    
    Write-Host "   Versão: $versionName (Build $versionCode)" -ForegroundColor Cyan
    
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

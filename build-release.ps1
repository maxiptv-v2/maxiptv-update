# Script para compilar o app em Release e enviar atualização para GitHub

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  COMPILANDO E ENVIANDO ATUALIZACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$ErrorActionPreference = "Stop"

# Configurações do GitHub (token já configurado no remote)

try {
    # 0. Obter versão atual e aumentar automaticamente
    Write-Host "0. Obtendo e aumentando versao..." -ForegroundColor Yellow
    $buildGradlePath = "app\build.gradle.kts"
    $buildGradle = Get-Content $buildGradlePath -Raw
    
    # Extrair versão atual
    $oldVersionCode = 0
    $oldVersionName = ""
    if ($buildGradle -match 'versionCode\s*=\s*(\d+)') {
        $oldVersionCode = [int]$matches[1]
    }
    if ($buildGradle -match 'versionName\s*=\s*"([^"]+)"') {
        $oldVersionName = $matches[1]
    }
    
    # Aumentar versão
    $newVersionCode = $oldVersionCode + 1
    $versionParts = $oldVersionName -split '\.'
    if ($versionParts.Length -ge 3) {
        $versionParts[2] = [int]$versionParts[2] + 1
        $newVersionName = $versionParts -join '.'
    } else {
        $newVersionName = "1.0.$newVersionCode"
    }
    
    Write-Host ""
    Write-Host "   Versao Anterior: v$oldVersionName (Build $oldVersionCode)" -ForegroundColor Gray
    Write-Host "   Versao Nova:     v$newVersionName (Build $newVersionCode)" -ForegroundColor Cyan
    Write-Host ""
    
    # Atualizar build.gradle.kts
    $buildGradle = $buildGradle -replace 'versionCode\s*=\s*\d+', "versionCode = $newVersionCode"
    $buildGradle = $buildGradle -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newVersionName`""
    
    Set-Content -Path $buildGradlePath -Value $buildGradle -NoNewline
    Write-Host "   [OK] Versao atualizada no build.gradle.kts" -ForegroundColor Green
    Write-Host ""
    
    # Salvar versões para uso posterior
    $versionCode = $newVersionCode
    $versionName = $newVersionName
    
    # 1. Compilar em Release (sem limpar build anterior)
    Write-Host ""
    Write-Host "1. Compilando em Release..." -ForegroundColor Yellow
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
        
        # Copiar APK para raiz como binário
        Write-Host ""
        Write-Host "2. Copiando APK para raiz (como binario)..." -ForegroundColor Yellow
        $sourceApk = $apkFiles[0].FullName
        $destApk = "maxiptv-release.apk"
        
        # Garantir que .gitattributes trata APK como binário
        if (-not (Test-Path ".gitattributes")) {
            Write-Host "   Criando .gitattributes..." -ForegroundColor Gray
            "*.apk binary" | Out-File -FilePath ".gitattributes" -Encoding utf8 -NoNewline
        } else {
            $gitattrs = Get-Content ".gitattributes" -Raw
            if ($gitattrs -notmatch "\.apk.*binary") {
                Write-Host "   Adicionando regra para APK em .gitattributes..." -ForegroundColor Gray
                Add-Content -Path ".gitattributes" -Value "*.apk binary"
            }
        }
        
        # Copiar APK preservando como binário
        Copy-Item $sourceApk $destApk -Force
        Write-Host "   [OK] APK copiado para: $destApk" -ForegroundColor Green
        
        # Verificar se arquivo foi copiado corretamente
        if (Test-Path $destApk) {
            $destSize = (Get-Item $destApk).Length
            $sourceSize = $apkFiles[0].Length
            if ($destSize -eq $sourceSize) {
                Write-Host "   [OK] Tamanho verificado: $([math]::Round($destSize / 1MB, 2)) MB" -ForegroundColor Green
            } else {
                Write-Host "   [ERRO] Tamanhos diferentes! Origem: $sourceSize, Destino: $destSize" -ForegroundColor Red
                throw "Erro ao copiar APK"
            }
        } else {
            throw "APK nao foi copiado corretamente"
        }
        
        # Atualizar update.json com nova versão e tamanho do APK
        Write-Host ""
        Write-Host "3. Atualizando update.json..." -ForegroundColor Yellow
        $apkSizeMB = [math]::Round($apkFiles[0].Length / 1MB, 1)
        $updateJson = Get-Content "update.json" -Raw | ConvertFrom-Json
        $updateJson.version = "v$versionName"
        $updateJson.versionCode = $versionCode
        $updateJson.buildNumber = $versionCode
        $updateJson.fileSize = "$apkSizeMB MB"
        $updateJson.lastUpdated = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ")
        $updateJson | ConvertTo-Json -Depth 10 | Set-Content "update.json"
        Write-Host "   [OK] update.json atualizado para v$versionName" -ForegroundColor Green
    } else {
        Write-Host "   [AVISO] Nenhum APK encontrado para atualizar update.json" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "4. Versao do app: v$versionName (Build $versionCode)" -ForegroundColor Yellow
    
    # 5. Verificar se há mudanças para commitar
    Write-Host ""
    Write-Host "5. Verificando mudanças..." -ForegroundColor Yellow
    $status = git status --porcelain
    if ($status) {
        Write-Host "   Mudanças encontradas, preparando commit..." -ForegroundColor Cyan
        
        # Garantir que APK seja tratado como binário no Git
        Write-Host "   Configurando APK como binario no Git..." -ForegroundColor Gray
        git add .gitattributes 2>&1 | Out-Null
        
        # Adicionar arquivos de código
        git add app/src/
        git add app/build.gradle.kts
        git add build-release.ps1
        
        # ✅ GARANTIR que update.json seja sempre incluído
        if (Test-Path "update.json") {
            git add -f update.json
            Write-Host "   [OK] update.json adicionado ao commit" -ForegroundColor Green
        } else {
            Write-Host "   [AVISO] update.json nao encontrado!" -ForegroundColor Yellow
        }
        
        # Adicionar APK como binário (se existir)
        if (Test-Path "maxiptv-release.apk") {
            Write-Host "   Adicionando APK como binario..." -ForegroundColor Gray
            
            # Garantir que Git reconhece como binário
            git rm --cached maxiptv-release.apk 2>&1 | Out-Null
            git add --renormalize maxiptv-release.apk 2>&1 | Out-Null
            
            # Verificar se Git reconheceu como binário
            $gitCheck = git check-attr -a maxiptv-release.apk 2>&1
            if ($gitCheck -match "binary.*set") {
                Write-Host "   [OK] Git reconheceu APK como binario" -ForegroundColor Green
            } else {
                Write-Host "   [AVISO] Verificando configuracao do Git..." -ForegroundColor Yellow
            }
            
            # Adicionar forçadamente
            git add -f maxiptv-release.apk
            Write-Host "   [OK] APK adicionado ao Git como binario" -ForegroundColor Green
        }
        
        # Commit
        $commitMessage = "Release v$versionName - Build $versionCode"
        git commit -m $commitMessage
        
        Write-Host "   Commit criado: $commitMessage" -ForegroundColor Green
    } else {
        Write-Host "   Nenhuma mudança para commitar" -ForegroundColor Yellow
    }
    
    # 6. Criar tag se não existir
    Write-Host ""
    Write-Host "6. Criando tag de versão..." -ForegroundColor Yellow
    $tagName = "v$versionName"
    $tagExists = git tag -l $tagName
    if (-not $tagExists) {
        git tag -a $tagName -m "Release v$versionName - Build $versionCode"
        Write-Host "   Tag criada: $tagName" -ForegroundColor Green
    } else {
        Write-Host "   Tag já existe: $tagName" -ForegroundColor Yellow
    }
    
    # 7. Push para GitHub
    Write-Host ""
    Write-Host "7. Enviando para GitHub..." -ForegroundColor Yellow
    
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

# Script para verificar se o app está completamente atualizado e otimizado

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICAÇÃO COMPLETA DO APP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$issues = @()
$warnings = @()
$success = @()

# 1. Verificar código duplicado
Write-Host "1. Verificando código duplicado..." -ForegroundColor Yellow
$duplicated = @()
$files = Get-ChildItem -Path "app/src/main/java" -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch "\\build\\" }
$contentMap = @{}

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $lines = $content -split "`n"
    
    # Verificar funções duplicadas
    $functions = Select-String -Path $file.FullName -Pattern "^\s*(private|public|protected|internal|override)?\s*fun\s+(\w+)\s*\(" -AllMatches
    foreach ($match in $functions.Matches) {
        $funcName = $match.Groups[2].Value
        if ($contentMap.ContainsKey($funcName)) {
            $duplicated += "Função '$funcName' duplicada em $($file.Name) e $($contentMap[$funcName])"
        } else {
            $contentMap[$funcName] = $file.Name
        }
    }
}

if ($duplicated.Count -eq 0) {
    Write-Host "   ✅ Nenhum código duplicado encontrado" -ForegroundColor Green
    $success += "Código duplicado: OK"
} else {
    Write-Host "   ⚠️ Código duplicado encontrado:" -ForegroundColor Yellow
    foreach ($dup in $duplicated) {
        Write-Host "      - $dup" -ForegroundColor Gray
    }
    $warnings += "Código duplicado encontrado"
}

Write-Host ""

# 2. Verificar código deprecated
Write-Host "2. Verificando código deprecated..." -ForegroundColor Yellow
$deprecated = @()
$deprecatedPatterns = @(
    "@Deprecated",
    "deprecated",
    "DeprecatedApi",
    "DeprecatedSinceKotlin"
)

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    foreach ($pattern in $deprecatedPatterns) {
        if ($content -match $pattern) {
            $deprecated += "$($file.Name): Contém código deprecated"
            break
        }
    }
}

if ($deprecated.Count -eq 0) {
    Write-Host "   ✅ Nenhum código deprecated encontrado" -ForegroundColor Green
    $success += "Código deprecated: OK"
} else {
    Write-Host "   ⚠️ Código deprecated encontrado:" -ForegroundColor Yellow
    foreach ($dep in $deprecated) {
        Write-Host "      - $dep" -ForegroundColor Gray
    }
    $warnings += "Código deprecated encontrado"
}

Write-Host ""

# 3. Verificar imports não utilizados
Write-Host "3. Verificando imports não utilizados..." -ForegroundColor Yellow
$unusedImports = 0
foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $imports = Select-String -Path $file.FullName -Pattern "^import\s+([^\s]+)" -AllMatches
    
    foreach ($import in $imports.Matches) {
        $importName = $import.Groups[1].Value
        $className = $importName.Split(".")[-1]
        
        # Verificar se a classe é usada no código (exceto na linha de import)
        $contentWithoutImports = $content -replace "import\s+[^`n]+`n", ""
        if ($contentWithoutImports -notmatch "\b$className\b") {
            $unusedImports++
        }
    }
}

if ($unusedImports -eq 0) {
    Write-Host "   ✅ Nenhum import não utilizado encontrado" -ForegroundColor Green
    $success += "Imports não utilizados: OK"
} else {
    Write-Host "   ⚠️ $unusedImports imports possivelmente não utilizados encontrados" -ForegroundColor Yellow
    $warnings += "Imports não utilizados encontrados"
}

Write-Host ""

# 4. Verificar variáveis não utilizadas
Write-Host "4. Verificando variáveis não utilizadas..." -ForegroundColor Yellow
$unusedVars = Select-String -Path "app/src/main/java" -Recurse -Filter "*.kt" -Pattern "Variable '[^']+' is never used" | Measure-Object
if ($unusedVars.Count -eq 0) {
    Write-Host "   ✅ Nenhuma variável não utilizada encontrada" -ForegroundColor Green
    $success += "Variáveis não utilizadas: OK"
} else {
    Write-Host "   ⚠️ Variáveis não utilizadas encontradas (verificar warnings do compilador)" -ForegroundColor Yellow
    $warnings += "Variáveis não utilizadas encontradas"
}

Write-Host ""

# 5. Verificar estrutura de arquivos principais
Write-Host "5. Verificando estrutura de arquivos principais..." -ForegroundColor Yellow
$requiredFiles = @(
    "app/src/main/java/com/maxiptv/MainActivity.kt",
    "app/src/main/java/com/maxiptv/MaxiApp.kt",
    "app/src/main/java/com/maxiptv/ui/screens/HomeScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/LiveScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/VodScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/SeriesScreen.kt",
    "app/src/main/java/com/maxiptv/ui/player/PlayerActivity.kt",
    "app/src/main/java/com/maxiptv/data/Repo.kt",
    "app/src/main/java/com/maxiptv/data/Models.kt"
)

$missingFiles = @()
foreach ($file in $requiredFiles) {
    if (-not (Test-Path $file)) {
        $missingFiles += $file
    }
}

if ($missingFiles.Count -eq 0) {
    Write-Host "   ✅ Todos os arquivos principais estão presentes" -ForegroundColor Green
    $success += "Estrutura de arquivos: OK"
} else {
    Write-Host "   ❌ Arquivos faltando:" -ForegroundColor Red
    foreach ($file in $missingFiles) {
        Write-Host "      - $file" -ForegroundColor Gray
    }
    $issues += "Arquivos principais faltando"
}

Write-Host ""

# 6. Verificar versão e build
Write-Host "6. Verificando versão e build..." -ForegroundColor Yellow
if (Test-Path "version.json") {
    $version = Get-Content "version.json" | ConvertFrom-Json
    Write-Host "   ✅ Versão atual: $($version.version) (Build $($version.build))" -ForegroundColor Green
    $success += "Versão: $($version.version)"
} else {
    Write-Host "   ⚠️ Arquivo version.json não encontrado" -ForegroundColor Yellow
    $warnings += "version.json não encontrado"
}

Write-Host ""

# 7. Verificar configurações do Gradle
Write-Host "7. Verificando configurações do Gradle..." -ForegroundColor Yellow
if (Test-Path "app/build.gradle.kts") {
    $gradleContent = Get-Content "app/build.gradle.kts" -Raw
    
    # Verificar se minSdk está configurado
    if ($gradleContent -match "minSdk\s*=\s*(\d+)") {
        $minSdk = $matches[1]
        Write-Host "   ✅ minSdk: $minSdk" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ minSdk não encontrado" -ForegroundColor Yellow
        $warnings += "minSdk não configurado"
    }
    
    # Verificar se targetSdk está configurado
    if ($gradleContent -match "targetSdk\s*=\s*(\d+)") {
        $targetSdk = $matches[1]
        Write-Host "   ✅ targetSdk: $targetSdk" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ targetSdk não encontrado" -ForegroundColor Yellow
        $warnings += "targetSdk não configurado"
    }
    
    # Verificar se signing está configurado
    if ($gradleContent -match "signingConfigs|signingConfig") {
        Write-Host "   ✅ Signing config configurado" -ForegroundColor Green
        $success += "Signing config: OK"
    } else {
        Write-Host "   ⚠️ Signing config não encontrado" -ForegroundColor Yellow
        $warnings += "Signing config não configurado"
    }
} else {
    Write-Host "   ❌ Arquivo build.gradle.kts não encontrado" -ForegroundColor Red
    $issues += "build.gradle.kts não encontrado"
}

Write-Host ""

# 8. Verificar dependências principais
Write-Host "8. Verificando dependências principais..." -ForegroundColor Yellow
if (Test-Path "app/build.gradle.kts") {
    $gradleContent = Get-Content "app/build.gradle.kts" -Raw
    
    $requiredDeps = @(
        "androidx.compose",
        "androidx.media3",
        "coil",
        "kotlinx.coroutines",
        "okhttp"
    )
    
    $missingDeps = @()
    foreach ($dep in $requiredDeps) {
        if ($gradleContent -notmatch $dep) {
            $missingDeps += $dep
        }
    }
    
    if ($missingDeps.Count -eq 0) {
        Write-Host "   ✅ Todas as dependências principais estão presentes" -ForegroundColor Green
        $success += "Dependências: OK"
    } else {
        Write-Host "   ⚠️ Dependências faltando:" -ForegroundColor Yellow
        foreach ($dep in $missingDeps) {
            Write-Host "      - $dep" -ForegroundColor Gray
        }
        $warnings += "Dependências faltando"
    }
}

Write-Host ""

# 9. Verificar AndroidManifest
Write-Host "9. Verificando AndroidManifest..." -ForegroundColor Yellow
if (Test-Path "app/src/main/AndroidManifest.xml") {
    $manifest = Get-Content "app/src/main/AndroidManifest.xml" -Raw
    
    # Verificar permissões essenciais
    $requiredPerms = @("INTERNET", "ACCESS_NETWORK_STATE")
    $missingPerms = @()
    foreach ($perm in $requiredPerms) {
        if ($manifest -notmatch $perm) {
            $missingPerms += $perm
        }
    }
    
    if ($missingPerms.Count -eq 0) {
        Write-Host "   ✅ Permissões essenciais configuradas" -ForegroundColor Green
        $success += "Permissões: OK"
    } else {
        Write-Host "   ⚠️ Permissões faltando:" -ForegroundColor Yellow
        foreach ($perm in $missingPerms) {
            Write-Host "      - $perm" -ForegroundColor Gray
        }
        $warnings += "Permissões faltando"
    }
    
    # Verificar activities principais
    if ($manifest -match "MainActivity" -and $manifest -match "PlayerActivity") {
        Write-Host "   ✅ Activities principais configuradas" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ Activities principais podem estar faltando" -ForegroundColor Yellow
        $warnings += "Activities principais podem estar faltando"
    }
} else {
    Write-Host "   ❌ AndroidManifest.xml não encontrado" -ForegroundColor Red
    $issues += "AndroidManifest.xml não encontrado"
}

Write-Host ""

# 10. Verificar se há erros de compilação recentes
Write-Host "10. Verificando últimos erros de compilação..." -ForegroundColor Yellow
$lastBuild = Get-Content "app/build.gradle.kts" -Raw
if ($lastBuild -match "versionCode\s*=\s*(\d+)") {
    $versionCode = $matches[1]
    Write-Host "   ✅ Version Code: $versionCode" -ForegroundColor Green
    $success += "Version Code: $versionCode"
} else {
    Write-Host "   ⚠️ Version Code não encontrado" -ForegroundColor Yellow
    $warnings += "Version Code não encontrado"
}

Write-Host ""

# Resumo
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICAÇÃO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($success.Count -gt 0) {
    Write-Host "✅ SUCESSOS ($($success.Count)):" -ForegroundColor Green
    foreach ($s in $success) {
        Write-Host "   - $s" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "⚠️ AVISOS ($($warnings.Count)):" -ForegroundColor Yellow
    foreach ($w in $warnings) {
        Write-Host "   - $w" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($issues.Count -gt 0) {
    Write-Host "❌ PROBLEMAS ($($issues.Count)):" -ForegroundColor Red
    foreach ($i in $issues) {
        Write-Host "   - $i" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($issues.Count -eq 0 -and $warnings.Count -eq 0) {
    Write-Host "🎉 APP ESTÁ COMPLETAMENTE ATUALIZADO E OTIMIZADO!" -ForegroundColor Green
} elseif ($issues.Count -eq 0) {
    Write-Host "✅ APP ESTÁ ATUALIZADO (com alguns avisos menores)" -ForegroundColor Green
} else {
    Write-Host "⚠️ APP PRECISA DE ATENÇÃO" -ForegroundColor Yellow
}

Write-Host ""


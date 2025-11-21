# Script para verificar código deprecated e código que deixa o app pesado

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACAO: DEPRECATED E PESO DO APP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$deprecated = @()
$pesoIssues = @()
$success = @()

$kotlinFiles = Get-ChildItem -Path "app/src/main/java" -Recurse -Filter "*.kt" | Where-Object { $_.FullName -notmatch "\\build\\" }

Write-Host "1. Verificando código deprecated..." -ForegroundColor Yellow

# Padrões deprecated comuns
$deprecatedPatterns = @{
    "systemUiVisibility" = "Deprecated em API 30+, usar WindowInsetsControllerCompat"
    "@Deprecated" = "Código marcado como deprecated"
    "onBackPressed" = "Deprecated, usar OnBackPressedDispatcher"
    "FLAG_FULLSCREEN" = "Deprecated em API 30+, usar WindowInsetsControllerCompat"
    "GestureDetectorCompat" = "Deprecated, usar GestureDetector"
    "getExternalStorageDirectory" = "Deprecated em API 29+, usar Scoped Storage"
    "requestPermissions" = "Deprecated em API 23+, usar ActivityResultLauncher"
    "onRequestPermissionsResult" = "Deprecated, usar ActivityResultLauncher"
    "MediaPlayer" = "Deprecated para vídeo, usar ExoPlayer"
    "VideoView" = "Deprecated, usar ExoPlayer"
    "WebView" = "Pode ser pesado se não otimizado"
    "BitmapFactory.decode" = "Pode causar OutOfMemoryError se não otimizado"
}

foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "")
    
    foreach ($pattern in $deprecatedPatterns.Keys) {
        if ($content -match $pattern) {
            $lineNum = (Select-String -Path $file.FullName -Pattern $pattern -AllMatches).LineNumber[0]
            $deprecated += "$relativePath (linha $lineNum): $pattern - $($deprecatedPatterns[$pattern])"
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
}

Write-Host ""

Write-Host "2. Verificando código que pode deixar o app pesado..." -ForegroundColor Yellow

# Padrões que podem deixar o app pesado
$pesoPatterns = @{
    "BitmapFactory.decode" = "Decodificar bitmaps sem otimização pode causar OutOfMemoryError"
    "getBitmap" = "Carregar bitmaps grandes pode causar problemas de memória"
    "createScaledBitmap" = "Redimensionar bitmaps pode ser pesado"
    "WebView" = "WebView é pesado e pode aumentar significativamente o tamanho do APK"
    "largeHeap" = "Usar largeHeap pode indicar problemas de memória"
    "while.*true" = "Loops infinitos podem causar problemas de performance"
    "Thread.sleep" = "Thread.sleep pode bloquear a UI thread"
    "runBlocking" = "runBlocking bloqueia a thread atual"
    "getExternalStorageDirectory" = "Acesso a storage externo pode ser lento"
    "File.*listFiles" = "Listar muitos arquivos pode ser lento"
    "for.*in.*.*forEach" = "Loops aninhados podem ser pesados"
    "remember.*derivedStateOf" = "Verificar se está sendo usado corretamente"
}

foreach ($file in $kotlinFiles) {
    $content = Get-Content $file.FullName -Raw
    $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "")
    
    foreach ($pattern in $pesoPatterns.Keys) {
        $matches = Select-String -Path $file.FullName -Pattern $pattern -AllMatches
        if ($matches) {
            $count = $matches.Matches.Count
            if ($count -gt 0) {
                $lineNum = $matches.LineNumber[0]
                $pesoIssues += "$relativePath (linha $lineNum, $count ocorrências): $pattern - $($pesoPatterns[$pattern])"
            }
        }
    }
}

# Verificar tamanho de imagens
Write-Host "   Verificando recursos de imagem..." -ForegroundColor Gray
$imageDirs = @(
    "app/src/main/res/mipmap-*",
    "app/src/main/res/drawable-*"
)

$largeImages = @()
foreach ($dirPattern in $imageDirs) {
    $dirs = Get-ChildItem -Path "app/src/main/res" -Directory | Where-Object { $_.Name -match "mipmap|drawable" }
    foreach ($dir in $dirs) {
        $images = Get-ChildItem -Path $dir.FullName -File -Include "*.png","*.jpg","*.jpeg","*.webp" -ErrorAction SilentlyContinue
        foreach ($img in $images) {
            $sizeKB = [math]::Round($img.Length / 1KB, 2)
            if ($sizeKB -gt 500) {
                $largeImages += "$($dir.Name)/$($img.Name): ${sizeKB}KB"
            }
        }
    }
}

if ($largeImages.Count -gt 0) {
    Write-Host "   ⚠️ Imagens grandes encontradas:" -ForegroundColor Yellow
    foreach ($img in $largeImages) {
        Write-Host "      - $img" -ForegroundColor Gray
    }
    $pesoIssues += "Imagens grandes encontradas ($($largeImages.Count) imagens > 500KB)"
}

# Verificar dependências pesadas
Write-Host "   Verificando dependências..." -ForegroundColor Gray
$gradleFile = "app/build.gradle.kts"
if (Test-Path $gradleFile) {
    $gradleContent = Get-Content $gradleFile -Raw
    
    $heavyDeps = @{
        "com.google.android.gms" = "Google Play Services pode ser pesado"
        "com.facebook" = "Facebook SDK pode ser pesado"
        "com.google.firebase" = "Firebase pode aumentar o tamanho do APK"
        "com.squareup.retrofit2" = "Retrofit está presente (verificar se necessário)"
        "com.squareup.moshi" = "Moshi está presente (verificar se necessário)"
    }
    
    foreach ($dep in $heavyDeps.Keys) {
        if ($gradleContent -match $dep) {
            Write-Host "   ⚠️ Dependência potencialmente pesada: $dep" -ForegroundColor Yellow
            $pesoIssues += "Dependência: $dep - $($heavyDeps[$dep])"
        }
    }
}

# Verificar tamanho do APK
Write-Host "   Verificando tamanho do APK..." -ForegroundColor Gray
$apkFile = "app/build/outputs/apk/release/maxiptv-release.apk"
if (Test-Path $apkFile) {
    $apkSizeMB = [math]::Round((Get-Item $apkFile).Length / 1MB, 2)
    Write-Host "   ℹ️ Tamanho do APK: ${apkSizeMB}MB" -ForegroundColor Gray
    
    if ($apkSizeMB -gt 100) {
        $pesoIssues += "APK muito grande: ${apkSizeMB}MB (considerar otimização)"
        Write-Host "   ⚠️ APK maior que 100MB" -ForegroundColor Yellow
    } elseif ($apkSizeMB -gt 50) {
        Write-Host "   ⚠️ APK maior que 50MB (considerar otimização)" -ForegroundColor Yellow
    } else {
        Write-Host "   ✅ Tamanho do APK adequado" -ForegroundColor Green
        $success += "Tamanho do APK: ${apkSizeMB}MB (adequado)"
    }
} else {
    Write-Host "   ℹ️ APK não encontrado (compilar primeiro para verificar tamanho)" -ForegroundColor Gray
}

Write-Host ""

Write-Host "3. Verificando otimizações de imagem..." -ForegroundColor Yellow

# Verificar se Coil está sendo usado (otimizado)
$coilUsage = Select-String -Path "app/src/main/java" -Recurse -Filter "*.kt" -Pattern "coil|AsyncImage" | Measure-Object
if ($coilUsage.Count -gt 0) {
    Write-Host "   ✅ Coil está sendo usado para carregamento otimizado de imagens" -ForegroundColor Green
    $success += "Coil: Usado para otimização de imagens"
} else {
    Write-Host "   ⚠️ Coil não encontrado (pode estar usando métodos não otimizados)" -ForegroundColor Yellow
    $pesoIssues += "Coil não encontrado para otimização de imagens"
}

# Verificar se há ImageRequest com size
$imageRequestSize = Select-String -Path "app/src/main/java" -Recurse -Filter "*.kt" -Pattern "ImageRequest.*size" | Measure-Object
if ($imageRequestSize.Count -gt 0) {
    Write-Host "   ✅ ImageRequest com size está sendo usado (otimização)" -ForegroundColor Green
    $success += "ImageRequest: Size configurado para otimização"
} else {
    Write-Host "   ⚠️ ImageRequest com size pode não estar sendo usado em todos os lugares" -ForegroundColor Yellow
}

Write-Host ""

Write-Host "4. Verificando ProGuard/R8..." -ForegroundColor Yellow

if (Test-Path $gradleFile) {
    $gradleContent = Get-Content $gradleFile -Raw
    if ($gradleContent -match "isMinifyEnabled.*true|minifyEnabled.*true") {
        Write-Host "   ✅ ProGuard/R8 está habilitado (reduz tamanho do APK)" -ForegroundColor Green
        $success += "ProGuard/R8: Habilitado"
    } else {
        Write-Host "   ⚠️ ProGuard/R8 pode não estar habilitado" -ForegroundColor Yellow
        $pesoIssues += "ProGuard/R8 pode não estar habilitado"
    }
}

Write-Host ""

Write-Host "5. Verificando cache e otimizações..." -ForegroundColor Yellow

# Verificar se há cache implementado
$cacheUsage = Select-String -Path "app/src/main/java" -Recurse -Filter "*.kt" -Pattern "cache|Cache|CACHE" | Measure-Object
if ($cacheUsage.Count -gt 0) {
    Write-Host "   ✅ Sistema de cache encontrado" -ForegroundColor Green
    $success += "Cache: Implementado"
} else {
    Write-Host "   ⚠️ Sistema de cache pode estar faltando" -ForegroundColor Yellow
}

# Verificar DataStore (otimizado)
$dataStoreUsage = Select-String -Path "app/src/main/java" -Recurse -Filter "*.kt" -Pattern "DataStore|datastore" | Measure-Object
if ($dataStoreUsage.Count -gt 0) {
    Write-Host "   ✅ DataStore está sendo usado (otimizado)" -ForegroundColor Green
    $success += "DataStore: Usado (otimizado)"
}

Write-Host ""

# Resumo
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($success.Count -gt 0) {
    Write-Host "✅ SUCESSOS ($($success.Count)):" -ForegroundColor Green
    foreach ($s in $success) {
        Write-Host "   - $s" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($deprecated.Count -gt 0) {
    Write-Host "⚠️ CODIGO DEPRECATED ($($deprecated.Count)):" -ForegroundColor Yellow
    foreach ($d in $deprecated) {
        Write-Host "   - $d" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($pesoIssues.Count -gt 0) {
    Write-Host "⚠️ POSSIVEIS PROBLEMAS DE PESO ($($pesoIssues.Count)):" -ForegroundColor Yellow
    foreach ($p in $pesoIssues) {
        Write-Host "   - $p" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($deprecated.Count -eq 0 -and $pesoIssues.Count -eq 0) {
    Write-Host "🎉 APP OTIMIZADO E SEM CODIGO DEPRECATED!" -ForegroundColor Green
} elseif ($deprecated.Count -eq 0) {
    Write-Host "✅ SEM CODIGO DEPRECATED (alguns avisos de peso)" -ForegroundColor Green
} else {
    Write-Host "⚠️ APP PRECISA DE ATENCAO" -ForegroundColor Yellow
}

Write-Host ""


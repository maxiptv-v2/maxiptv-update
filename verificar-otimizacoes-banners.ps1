# Script para verificar otimizacoes de banners
Write-Host "Verificando otimizacoes de banners..." -ForegroundColor Cyan
Write-Host ""

$erros = @()
$avisos = @()
$sucessos = @()

$arquivos = @(
    "app/src/main/java/com/maxiptv/ui/screens/VodScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/SeriesScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/VodDetailsScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/SeriesDetailsScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/SearchScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/FavoritesScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/AdultContentScreen.kt",
    "app/src/main/java/com/maxiptv/ui/screens/BannerCarousel.kt"
)

foreach ($arquivo in $arquivos) {
    if (-not (Test-Path $arquivo)) {
        $erros += "ERRO: Arquivo nao encontrado: $arquivo"
        continue
    }
    
    $conteudo = Get-Content $arquivo -Raw
    $nomeArquivo = Split-Path $arquivo -Leaf
    
    Write-Host "Analisando: $nomeArquivo" -ForegroundColor Yellow
    
    # Verificar se tem AsyncImage
    if ($conteudo -match "AsyncImage") {
        # Verificar se tem ImageRequest.Builder
        if ($conteudo -notmatch "ImageRequest\.Builder") {
            $erros += "ERRO: ${nomeArquivo}: AsyncImage encontrado mas sem ImageRequest.Builder"
        } else {
            $sucessos += "OK: ${nomeArquivo}: Usa ImageRequest.Builder"
        }
        
        # Verificar se tem .size() definido
        $patternSize = '\.size\('
        if ($conteudo -notmatch $patternSize) {
            $erros += "ERRO: ${nomeArquivo}: AsyncImage sem .size() definido"
        } else {
            # Extrair o tamanho
            $matchesSize = [regex]::Matches($conteudo, '\.size\(([^)]+)\)')
            foreach ($match in $matchesSize) {
                $tamanho = $match.Groups[1].Value
                $sucessos += "OK: ${nomeArquivo}: Tamanho definido ($tamanho)"
            }
        }
        
        # Verificar imports necessarios
        if ($conteudo -match "ImageRequest\.Builder" -and $conteudo -notmatch "import.*ImageRequest") {
            $erros += "ERRO: ${nomeArquivo}: Usa ImageRequest mas nao tem import"
        }
        
        if ($conteudo -match "LocalContext\.current" -and $conteudo -notmatch "import.*LocalContext") {
            $erros += "ERRO: ${nomeArquivo}: Usa LocalContext mas nao tem import"
        }
        
        # Verificar cache policies
        if ($conteudo -match "ImageRequest\.Builder" -and $conteudo -notmatch "memoryCachePolicy") {
            $avisos += "AVISO: ${nomeArquivo}: ImageRequest sem memoryCachePolicy"
        }
        
        if ($conteudo -match "ImageRequest\.Builder" -and $conteudo -notmatch "diskCachePolicy") {
            $avisos += "AVISO: ${nomeArquivo}: ImageRequest sem diskCachePolicy"
        }
    }
    
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($sucessos.Count -gt 0) {
    Write-Host "SUCESSOS ($($sucessos.Count)):" -ForegroundColor Green
    foreach ($s in $sucessos) {
        Write-Host "  $s" -ForegroundColor Green
    }
    Write-Host ""
}

if ($avisos.Count -gt 0) {
    Write-Host "AVISOS ($($avisos.Count)):" -ForegroundColor Yellow
    foreach ($a in $avisos) {
        Write-Host "  $a" -ForegroundColor Yellow
    }
    Write-Host ""
}

if ($erros.Count -gt 0) {
    Write-Host "ERROS ($($erros.Count)):" -ForegroundColor Red
    foreach ($e in $erros) {
        Write-Host "  $e" -ForegroundColor Red
    }
    Write-Host ""
    exit 1
} else {
    Write-Host "Todas as verificacoes passaram!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar tamanhos especificos
    Write-Host "Verificando tamanhos de imagens..." -ForegroundColor Cyan
    Write-Host ""
    
    $tamanhosEncontrados = @()
    
    foreach ($arquivo in $arquivos) {
        if (Test-Path $arquivo) {
            $conteudo = Get-Content $arquivo -Raw
            $nomeArquivo = Split-Path $arquivo -Leaf
            
            $matchesSize = [regex]::Matches($conteudo, '\.size\(([^)]+)\)')
            foreach ($match in $matchesSize) {
                $tamanho = $match.Groups[1].Value
                $tamanhosEncontrados += "${nomeArquivo}: $tamanho"
            }
        }
    }
    
    if ($tamanhosEncontrados.Count -gt 0) {
        Write-Host "Tamanhos encontrados:" -ForegroundColor Cyan
        foreach ($t in $tamanhosEncontrados) {
            Write-Host "  - $t" -ForegroundColor White
        }
    }
    
    Write-Host ""
    Write-Host "Verificacao completa!" -ForegroundColor Green
    exit 0
}

# Script para testar sinopse e rating/avaliacao dos VODs
# Testa varios filmes para encontrar um com rating

param(
    [string]$BaseUrl = "https://aztv.cx",
    [string]$Username = "mae1",
    [string]$Password = "1234",
    [int]$TestCount = 5
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TESTE DE SINOPSE E RATING" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Limpar URL base
$BaseUrl = $BaseUrl -replace "/player_api\.php$", ""
$BaseUrl = $BaseUrl -replace "player_api\.php$", ""
if (-not $BaseUrl.EndsWith("/")) {
    $BaseUrl = "$BaseUrl/"
}

Write-Host "Testando conexao com a API..." -ForegroundColor Yellow

# 1. Testar autenticacao
try {
    $authUrl = $BaseUrl + "player_api.php?username=" + $Username + "&password=" + $Password
    $authResponse = Invoke-RestMethod -Uri $authUrl -Method Get -ErrorAction Stop
    Write-Host "   OK - Autenticacao OK" -ForegroundColor Green
} catch {
    Write-Host "   ERRO ao autenticar: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Buscando lista de VODs..." -ForegroundColor Yellow

# 2. Buscar lista de VODs
try {
    $vodListUrl = $BaseUrl + "player_api.php?username=" + $Username + "&password=" + $Password + "&action=get_vod_streams"
    $vodList = Invoke-RestMethod -Uri $vodListUrl -Method Get -ErrorAction Stop
    
    Write-Host "   OK - Encontrados $($vodList.Count) VODs" -ForegroundColor Green
    Write-Host ""
    
    # Testar varios filmes
    $tested = 0
    $foundWithRating = 0
    
    foreach ($vod in $vodList) {
        if ($tested -ge $TestCount) { break }
        
        $vodId = $vod.stream_id
        $vodName = $vod.name
        
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "  Filme $($tested + 1): $vodName" -ForegroundColor Cyan
        Write-Host "  ID: $vodId" -ForegroundColor Gray
        Write-Host "========================================" -ForegroundColor Cyan
        
        try {
            $vodInfoUrl = $BaseUrl + "player_api.php?username=" + $Username + "&password=" + $Password + "&action=get_vod_info&vod_id=" + $vodId
            $vodInfo = Invoke-RestMethod -Uri $vodInfoUrl -Method Get -ErrorAction Stop
            
            # Verificar sinopse
            $hasSynopsis = $false
            $synopsis = ""
            $synopsisLocation = ""
            
            if ($vodInfo.info.plot) {
                $hasSynopsis = $true
                $synopsis = $vodInfo.info.plot.ToString()
                $synopsisLocation = "info.plot"
            } elseif ($vodInfo.info.description) {
                $hasSynopsis = $true
                $synopsis = $vodInfo.info.description.ToString()
                $synopsisLocation = "info.description"
            } elseif ($vodInfo.movie_data.plot) {
                $hasSynopsis = $true
                $synopsis = $vodInfo.movie_data.plot.ToString()
                $synopsisLocation = "movie_data.plot"
            }
            
            # Verificar rating
            $hasRating = $false
            $rating = $null
            $ratingLocation = ""
            
            # Verificar rating em info
            if ($vodInfo.info.rating -and $vodInfo.info.rating -ne 0) {
                $hasRating = $true
                $rating = $vodInfo.info.rating
                $ratingLocation = "info.rating"
            } elseif ($vodInfo.info.rating_5based -and $vodInfo.info.rating_5based -ne 0) {
                $hasRating = $true
                $rating = $vodInfo.info.rating_5based
                $ratingLocation = "info.rating_5based"
            }
            
            # Verificar outros campos de rating
            if (-not $hasRating -and $vodInfo.movie_data) {
                $ratingFields = @("rating", "imdb_rating", "tmdb_rating", "vote_average")
                foreach ($field in $ratingFields) {
                    if ($vodInfo.movie_data.$field -and $vodInfo.movie_data.$field -ne 0) {
                        $hasRating = $true
                        $rating = $vodInfo.movie_data.$field
                        $ratingLocation = "movie_data.$field"
                        break
                    }
                }
            }
            
            # Exibir resultados
            Write-Host ""
            Write-Host "SINOPSE:" -ForegroundColor Yellow
            if ($hasSynopsis) {
                Write-Host "   OK - Encontrada em $synopsisLocation" -ForegroundColor Green
                Write-Host "   Tamanho: $($synopsis.Length) caracteres" -ForegroundColor Cyan
                $preview = if ($synopsis.Length -gt 150) { $synopsis.Substring(0, 150) + "..." } else { $synopsis }
                Write-Host "   Preview: $preview" -ForegroundColor White
            } else {
                Write-Host "   AVISO - Nao encontrada" -ForegroundColor Yellow
            }
            
            Write-Host ""
            Write-Host "RATING/AVALIACAO:" -ForegroundColor Yellow
            if ($hasRating) {
                Write-Host "   OK - Encontrado em $ratingLocation" -ForegroundColor Green
                Write-Host "   Valor: $rating" -ForegroundColor Cyan
                $foundWithRating++
            } else {
                Write-Host "   AVISO - Rating nao encontrado ou e zero" -ForegroundColor Yellow
                Write-Host "   info.rating: $($vodInfo.info.rating)" -ForegroundColor Gray
                Write-Host "   info.rating_5based: $($vodInfo.info.rating_5based)" -ForegroundColor Gray
            }
            
            # Verificar outros campos
            Write-Host ""
            Write-Host "OUTROS CAMPOS:" -ForegroundColor Yellow
            if ($vodInfo.info.genre) {
                Write-Host "   Genero: $($vodInfo.info.genre)" -ForegroundColor Cyan
            }
            if ($vodInfo.info.releasedate) {
                Write-Host "   Data Lancamento: $($vodInfo.info.releasedate)" -ForegroundColor Cyan
            }
            if ($vodInfo.info.cast) {
                Write-Host "   Elenco: $($vodInfo.info.cast.ToString().Substring(0, [Math]::Min(80, $vodInfo.info.cast.ToString().Length)))..." -ForegroundColor Cyan
            }
            
            $tested++
            Write-Host ""
            
        } catch {
            Write-Host "   ERRO ao buscar informacoes: $_" -ForegroundColor Red
            Write-Host ""
        }
    }
    
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  RESUMO FINAL" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Filmes testados: $tested" -ForegroundColor White
    Write-Host "Filmes com rating: $foundWithRating" -ForegroundColor $(if ($foundWithRating -gt 0) { "Green" } else { "Yellow" })
    Write-Host ""
    
    if ($foundWithRating -eq 0) {
        Write-Host "AVISO: Nenhum filme testado tinha rating diferente de zero." -ForegroundColor Yellow
        Write-Host "Isso pode significar que:" -ForegroundColor Yellow
        Write-Host "  1. A API nao retorna ratings para esses filmes especificos" -ForegroundColor Yellow
        Write-Host "  2. Os ratings estao em outro campo nao verificado" -ForegroundColor Yellow
        Write-Host "  3. Os filmes realmente nao tem avaliacao cadastrada" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "   ERRO ao buscar lista de VODs: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Teste concluido!" -ForegroundColor Green
Write-Host ""


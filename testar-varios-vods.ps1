# ========================================
# TESTE: Varios VOD IDs para encontrar ratings
# ========================================

param(
    [string]$BaseUrl = "https://aztv.cx",
    [string]$Username = "mae1",
    [string]$Password = "1234",
    [int]$StartId = 1,
    [int]$EndId = 100
)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE: Varios VOD IDs" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Limpar URL base
$BaseUrl = $BaseUrl.TrimEnd('/')
$BaseUrl = $BaseUrl -replace '/player_api\.php$', ''
$BaseUrl = if ($BaseUrl.EndsWith('/')) { $BaseUrl } else { "$BaseUrl/" }

Write-Host "Testando IDs de $StartId ate $EndId" -ForegroundColor Yellow
Write-Host "Base URL: $BaseUrl" -ForegroundColor Gray
Write-Host ""

$ratingFields = @("rating", "imdb_rating", "imdbRating", "tmdb_rating", "tmdbRating", 
                 "rate", "score", "vote_average", "voteAverage", "rotten_tomatoes", 
                 "metacritic_score", "rt_rating", "imdb_score", "tmdb_score",
                 "avaliacao", "avaliacao_imdb", "avaliacao_tmdb", "nota", "nota_imdb",
                 "classificacao", "classificacao_imdb", "pontuacao")

$foundWithRating = @()
$foundWithData = @()
$notFound = @()

for ($vodId = $StartId; $vodId -le $EndId; $vodId++) {
    $apiUrl = "${BaseUrl}player_api.php?username=$Username&password=$Password&action=get_vod_info&vod_id=$vodId"
    
    try {
        $response = Invoke-RestMethod -Uri $apiUrl -Method Get -ContentType "application/json" -ErrorAction Stop -TimeoutSec 5
        
        if ($null -ne $response.info -and $null -ne $response.movie_data) {
            $name = $response.info.name
            $hasRating = $false
            $ratingValue = $null
            $ratingField = $null
            
            # Verificar campos de rating
            foreach ($field in $ratingFields) {
                if ($response.movie_data.PSObject.Properties.Name -contains $field) {
                    $hasRating = $true
                    $ratingValue = $response.movie_data.$field
                    $ratingField = $field
                    break
                }
            }
            
            # Verificar valores numericos que podem ser rating
            if (-not $hasRating) {
                $response.movie_data.PSObject.Properties | ForEach-Object {
                    $key = $_.Name
                    $value = $_.Value
                    
                    if ($value -is [double] -or $value -is [int] -or $value -is [float] -or $value -is [decimal]) {
                        $numValue = [double]$value
                        if ($numValue -ge 0 -and $numValue -le 10 -and $key -ne "stream_id") {
                            $hasRating = $true
                            $ratingValue = $numValue
                            $ratingField = $key
                        }
                    } elseif ($value -is [string] -and $key -ne "stream_id") {
                        $parsed = [double]::TryParse($value, [ref]$null)
                        if ($parsed) {
                            $numValue = [double]$value
                            if ($numValue -ge 0 -and $numValue -le 10) {
                                $hasRating = $true
                                $ratingValue = $numValue
                                $ratingField = $key
                            }
                        }
                    }
                }
            }
            
            if ($hasRating) {
                $foundWithRating += [PSCustomObject]@{
                    Id = $vodId
                    Name = $name
                    RatingField = $ratingField
                    RatingValue = $ratingValue
                }
                Write-Host "✅ ID $vodId : $name - RATING: $ratingField = $ratingValue" -ForegroundColor Green
            } else {
                $foundWithData += [PSCustomObject]@{
                    Id = $vodId
                    Name = $name
                }
                Write-Host "  ID $vodId : $name (sem rating)" -ForegroundColor Gray
            }
        } else {
            $notFound += $vodId
        }
    } catch {
        # Silenciar erros de IDs inexistentes
    }
    
    # Mostrar progresso a cada 10 IDs
    if ($vodId % 10 -eq 0) {
        Write-Host "Progresso: $vodId/$EndId..." -ForegroundColor Cyan
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "VODs COM RATING ENCONTRADOS: $($foundWithRating.Count)" -ForegroundColor Green
if ($foundWithRating.Count -gt 0) {
    Write-Host ""
    $foundWithRating | ForEach-Object {
        Write-Host "  ID $($_.Id): $($_.Name)" -ForegroundColor White
        Write-Host "    Rating: $($_.RatingField) = $($_.RatingValue)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "VODs COM DADOS (sem rating): $($foundWithData.Count)" -ForegroundColor Yellow
if ($foundWithData.Count -gt 0 -and $foundWithData.Count -le 10) {
    $foundWithData | ForEach-Object {
        Write-Host "  ID $($_.Id): $($_.Name)" -ForegroundColor Gray
    }
} elseif ($foundWithData.Count -gt 10) {
    Write-Host "  (Mostrando apenas os primeiros 10)" -ForegroundColor Gray
    $foundWithData | Select-Object -First 10 | ForEach-Object {
        Write-Host "  ID $($_.Id): $($_.Name)" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "IDs NAO ENCONTRADOS: $($notFound.Count)" -ForegroundColor Red

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""


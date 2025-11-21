# ========================================
# TESTE: API - Avaliacoes dos Filmes
# Usa a mesma logica do codigo do app
# ========================================

param(
    [string]$BaseUrl = "https://aztv.cx",
    [string]$Username = "",
    [string]$Password = "",
    [int]$VodId = 1
)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE: API - Avaliacoes dos Filmes" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Se nao informou credenciais, pedir
if ([string]::IsNullOrEmpty($Username)) {
    Write-Host "Informe as credenciais:" -ForegroundColor Yellow
    $Username = Read-Host "Username"
    $SecurePassword = Read-Host "Password" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecurePassword))
}

if ([string]::IsNullOrEmpty($VodId) -or $VodId -eq 0) {
    $VodIdInput = Read-Host "VOD ID para testar (Enter para usar 1)"
    if (![string]::IsNullOrEmpty($VodIdInput)) {
        $VodId = [int]$VodIdInput
    } else {
        $VodId = 1
    }
}

# Limpar URL base (mesma logica do codigo)
$BaseUrl = $BaseUrl.TrimEnd('/')
$BaseUrl = $BaseUrl -replace '/player_api\.php$', ''
$BaseUrl = if ($BaseUrl.EndsWith('/')) { $BaseUrl } else { "$BaseUrl/" }

Write-Host "Testando VOD ID: $VodId" -ForegroundColor Yellow
Write-Host "Username: $Username" -ForegroundColor Gray
Write-Host "Base URL: $BaseUrl" -ForegroundColor Gray
Write-Host ""

# Variações de parâmetros para testar (baseado na API Xtream Code)
$ParamVariations = @(
    @{ Name = "Padrao (sem extras)"; Params = @{} },
    @{ Name = "Extended=1"; Params = @{ "extended" = "1" } },
    @{ Name = "Full=1"; Params = @{ "full" = "1" } },
    @{ Name = "TMDB=1"; Params = @{ "tmdb" = "1" } },
    @{ Name = "IMDB=1"; Params = @{ "imdb" = "1" } },
    @{ Name = "Extended+TMDB"; Params = @{ "extended" = "1"; "tmdb" = "1" } }
)

$bestResponse = $null
$bestParams = $null
$workingUrl = $null

foreach ($variation in $ParamVariations) {
    Write-Host "Testando: $($variation.Name)" -ForegroundColor Cyan
    
    # Construir URL exatamente como no codigo
    $apiUrl = "${BaseUrl}player_api.php?username=$Username&password=$Password&action=get_vod_info&vod_id=$VodId"
    
    # Adicionar parametros extras se houver
    foreach ($key in $variation.Params.Keys) {
        $apiUrl += "&$key=$($variation.Params[$key])"
    }
    
    try {
        $response = Invoke-RestMethod -Uri $apiUrl -Method Get -ContentType "application/json" -ErrorAction Stop -TimeoutSec 10
        
        if ($null -eq $workingUrl) {
            $workingUrl = $BaseUrl
            Write-Host "  URL funcionando!" -ForegroundColor Green
        }
        
        # Verificar se tem rating
        $hasRating = $false
        if ($null -ne $response.movie_data) {
            $ratingFields = @("rating", "imdb_rating", "imdbRating", "tmdb_rating", "tmdbRating", 
                             "rate", "score", "vote_average", "voteAverage", "rotten_tomatoes", 
                             "metacritic_score", "rt_rating", "imdb_score", "tmdb_score",
                             "avaliacao", "avaliacao_imdb", "avaliacao_tmdb", "nota", "nota_imdb",
                             "classificacao", "classificacao_imdb", "pontuacao")
            foreach ($field in $ratingFields) {
                if ($response.movie_data.PSObject.Properties.Name -contains $field) {
                    $hasRating = $true
                    Write-Host "  ENCONTROU RATING: $field = $($response.movie_data.$field)" -ForegroundColor Green
                    break
                }
            }
        }
        
        # Salvar melhor resposta (com mais campos ou com rating)
        if ($hasRating -or ($null -eq $bestResponse)) {
            $bestResponse = $response
            $bestParams = $variation.Name
        } elseif ($null -ne $response.movie_data -and $null -ne $bestResponse.movie_data) {
            $currentFields = $response.movie_data.PSObject.Properties.Count
            $bestFields = $bestResponse.movie_data.PSObject.Properties.Count
            if ($currentFields -gt $bestFields) {
                $bestResponse = $response
                $bestParams = $variation.Name
            }
        }
        
    } catch {
        Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
        continue
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESULTADO FINAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($null -eq $bestResponse) {
    Write-Host "ERRO: Nenhuma resposta valida obtida!" -ForegroundColor Red
    exit
}

if ($null -ne $bestParams) {
    Write-Host "Melhor combinacao: $bestParams" -ForegroundColor Green
    Write-Host ""
}

$response = $bestResponse

# Informacoes basicas
Write-Host "INFO DO FILME (objeto 'info'):" -ForegroundColor Yellow
if ($response.info) {
    Write-Host "  Nome: $($response.info.name)" -ForegroundColor White
    if ($response.info.plot) {
        $plot = $response.info.plot
        $plotPreview = if ($plot.Length -gt 150) { $plot.Substring(0, 150) + "..." } else { $plot }
        Write-Host "  Sinopse (plot): $plotPreview" -ForegroundColor Green
        Write-Host "  ✅ SINOPSE ESTA AQUI: response.info.plot" -ForegroundColor Green
    } else {
        Write-Host "  Sinopse: null" -ForegroundColor Gray
    }
    Write-Host "  Cover: $($response.info.cover)" -ForegroundColor Gray
    
    # Verificar se info tem campos de rating
    Write-Host ""
    Write-Host "  Campos do objeto 'info':" -ForegroundColor Cyan
    $response.info.PSObject.Properties | ForEach-Object {
        $key = $_.Name
        $value = $_.Value
        $typeName = if ($null -ne $value) { $value.GetType().Name } else { "null" }
        Write-Host "    - $key = $value (tipo: $typeName)" -ForegroundColor Gray
    }
} else {
    Write-Host "  AVISO: info e null" -ForegroundColor Red
}
Write-Host ""

# movie_data completo
Write-Host "MOVIE_DATA (Campos disponiveis):" -ForegroundColor Yellow
if ($null -eq $response.movie_data) {
    Write-Host "  ERRO: movie_data e NULL!" -ForegroundColor Red
    Write-Host "  Isso explica por que o rating nao aparece!" -ForegroundColor Yellow
} else {
    $fieldCount = $response.movie_data.PSObject.Properties.Count
    Write-Host "  SUCESSO: movie_data existe com $fieldCount campos" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Todos os campos:" -ForegroundColor White
    
    # Verificar campos que podem conter "avaliacao" ou "rating" no nome
    $possibleRatingFields = @()
    
    $response.movie_data.PSObject.Properties | ForEach-Object {
        $key = $_.Name
        $value = $_.Value
        $typeName = $value.GetType().Name
        
        # Verificar se o nome do campo contém palavras relacionadas a rating
        $keyLower = $key.ToLower()
        if ($keyLower -match "avaliacao|rating|nota|score|pontuacao|classificacao|rate|imdb|tmdb|metacritic|rotten") {
            $possibleRatingFields += $key
        }
        
        if ($value -is [string]) {
            if ($value.Length -gt 50) {
                $valueStr = "`"$($value.Substring(0, 50))...`""
            } else {
                $valueStr = "`"$value`""
            }
        } elseif ($value -is [System.Object[]]) {
            $valueStr = "[Array com $($value.Length) itens]"
        } else {
            $valueStr = $value.ToString()
        }
        
        $color = if ($possibleRatingFields -contains $key) { "Yellow" } else { "Gray" }
        Write-Host "    - $key = $valueStr (tipo: $typeName)" -ForegroundColor $color
    }
    
    if ($possibleRatingFields.Count -gt 0) {
        Write-Host ""
        Write-Host "  CAMPOS SUSPEITOS (podem ser rating):" -ForegroundColor Yellow
        foreach ($field in $possibleRatingFields) {
            $val = $response.movie_data.$field
            Write-Host "    ⚠️  $field = $val" -ForegroundColor Yellow
        }
    }
    Write-Host ""
    
    # Buscar especificamente campos de rating
    Write-Host "CAMPOS DE RATING ENCONTRADOS:" -ForegroundColor Yellow
    $ratingFields = @("rating", "imdb_rating", "imdbRating", "tmdb_rating", "tmdbRating", 
                     "rate", "score", "vote_average", "voteAverage", "rotten_tomatoes", 
                     "metacritic_score", "rt_rating", "imdb_score", "tmdb_score",
                     "avaliacao", "avaliacao_imdb", "avaliacao_tmdb", "nota", "nota_imdb",
                     "classificacao", "classificacao_imdb", "pontuacao")
    
    $foundAnyRating = $false
    foreach ($field in $ratingFields) {
        if ($response.movie_data.PSObject.Properties.Name -contains $field) {
            $value = $response.movie_data.$field
            $foundAnyRating = $true
            $typeName = $value.GetType().Name
            Write-Host "  SUCESSO: $field = $value (tipo: $typeName)" -ForegroundColor Green
        }
    }
    
    if (-not $foundAnyRating) {
        Write-Host "  AVISO: Nenhum campo de rating padrao encontrado" -ForegroundColor Yellow
    }
    Write-Host ""
    
    # Buscar valores numericos que podem ser rating (0-10)
    Write-Host "VALORES NUMERICOS QUE PODEM SER RATING (0-10):" -ForegroundColor Yellow
    $foundNumericRating = $false
    $response.movie_data.PSObject.Properties | ForEach-Object {
        $key = $_.Name
        $value = $_.Value
        
        if ($value -is [double] -or $value -is [int] -or $value -is [float] -or $value -is [decimal]) {
            $numValue = [double]$value
            if ($numValue -ge 0 -and $numValue -le 10) {
                $foundNumericRating = $true
                Write-Host "  SUCESSO: $key = $numValue (possivel rating)" -ForegroundColor Green
            }
        } elseif ($value -is [string]) {
            $parsed = [double]::TryParse($value, [ref]$null)
            if ($parsed) {
                $numValue = [double]$value
                if ($numValue -ge 0 -and $numValue -le 10) {
                    $foundNumericRating = $true
                    Write-Host "  SUCESSO: $key = `"$value`" (possivel rating como String)" -ForegroundColor Green
                }
            }
        }
    }
    
    if (-not $foundNumericRating) {
        Write-Host "  AVISO: Nenhum valor numerico entre 0-10 encontrado" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($null -eq $response.movie_data) {
    Write-Host "ERRO: movie_data e NULL" -ForegroundColor Red
    Write-Host "  A API nao retorna movie_data para este VOD" -ForegroundColor Yellow
    Write-Host "  Por isso o rating nao aparece no app!" -ForegroundColor Yellow
} elseif ($foundAnyRating) {
    Write-Host "SUCESSO: Rating encontrado nos campos padrao!" -ForegroundColor Green
} elseif ($foundNumericRating) {
    Write-Host "AVISO: Rating encontrado em campo nao padrao" -ForegroundColor Yellow
    Write-Host "  Verifique os campos acima para identificar o campo correto" -ForegroundColor White
} else {
    Write-Host "AVISO: Rating nao encontrado" -ForegroundColor Yellow
    Write-Host "  A API pode nao retornar rating para este filme" -ForegroundColor White
    Write-Host "  Ou o campo tem um nome diferente dos esperados" -ForegroundColor White
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

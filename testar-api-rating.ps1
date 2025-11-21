# ========================================
# TESTE: Mapear API Xtream Code para Rating
# ========================================

param(
    [string]$BaseUrl = "",
    [string]$Username = "",
    [string]$Password = "",
    [int]$VodId = 1
)

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TESTE: Mapeamento da API Xtream Code" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Se não informou parâmetros, pedir
if ([string]::IsNullOrEmpty($BaseUrl)) {
    Write-Host "Informe os dados da API:" -ForegroundColor Yellow
    $BaseUrl = Read-Host "URL Base (ex: https://seu-servidor.com:porta)"
    $Username = Read-Host "Username"
    $Password = Read-Host "Password" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($Password))
    $VodIdInput = Read-Host "VOD ID para testar (Enter para usar 1)"
    if (![string]::IsNullOrEmpty($VodIdInput)) {
        $VodId = [int]$VodIdInput
    }
}

# Limpar URL base
$BaseUrl = $BaseUrl.TrimEnd('/')
$BaseUrl = $BaseUrl -replace '/player_api\.php$', ''

# Construir URL da API
$ApiUrl = "$BaseUrl/player_api.php?username=$Username&password=$Password&action=get_vod_info&vod_id=$VodId"

Write-Host "📡 Fazendo requisição para a API..." -ForegroundColor Yellow
Write-Host "   URL: $ApiUrl" -ForegroundColor Gray
Write-Host ""

try {
    # Fazer requisição HTTP
    $response = Invoke-RestMethod -Uri $ApiUrl -Method Get -ContentType "application/json" -ErrorAction Stop
    
    Write-Host "✅ Resposta recebida da API`n" -ForegroundColor Green
    
    # Mostrar informações básicas
    Write-Host "📋 INFO:" -ForegroundColor Yellow
    if ($response.info) {
        Write-Host "   name: $($response.info.name)" -ForegroundColor White
        $plotPreview = if ($response.info.plot) { $response.info.plot.Substring(0, [Math]::Min(100, $response.info.plot.Length)) + "..." } else { "null" }
        Write-Host "   plot: $plotPreview" -ForegroundColor White
        Write-Host "   cover: $($response.info.cover)" -ForegroundColor White
    } else {
        Write-Host "   ⚠️ info é null" -ForegroundColor Red
    }
    Write-Host ""
    
    # Analisar movie_data
    Write-Host "📊 MOVIE_DATA:" -ForegroundColor Yellow
    if ($null -eq $response.movie_data) {
        Write-Host "   ⚠️ movie_data é NULL!" -ForegroundColor Red
        Write-Host "   Isso explica por que o rating não aparece!" -ForegroundColor Yellow
    } else {
        Write-Host "   Total de campos: $($response.movie_data.PSObject.Properties.Count)" -ForegroundColor White
        Write-Host ""
        Write-Host "   Campos disponíveis:" -ForegroundColor White
        
        # Listar todos os campos
        $response.movie_data.PSObject.Properties | ForEach-Object {
            $key = $_.Name
            $value = $_.Value
            $type = $value.GetType().Name
            $valueStr = if ($value -is [string]) { "`"$value`"" } else { $value.ToString() }
            Write-Host "     • $key = $valueStr (tipo: $type)" -ForegroundColor Gray
        }
        Write-Host ""
        
        # Buscar campos de rating
        Write-Host "🔍 CAMPOS DE RATING:" -ForegroundColor Yellow
        $ratingFields = @("rating", "imdb_rating", "imdbRating", "tmdb_rating", "tmdbRating", 
                         "rate", "score", "vote_average", "voteAverage", "rotten_tomatoes", 
                         "metacritic_score", "rt_rating")
        
        $foundAnyRating = $false
        foreach ($field in $ratingFields) {
            if ($response.movie_data.PSObject.Properties.Name -contains $field) {
                $value = $response.movie_data.$field
                $foundAnyRating = $true
                Write-Host "   ✅ $field = $value (tipo: $($value.GetType().Name))" -ForegroundColor Green
            }
        }
        
        if (-not $foundAnyRating) {
            Write-Host "   ⚠️ Nenhum campo de rating padrão encontrado" -ForegroundColor Yellow
        }
        Write-Host ""
        
        # Buscar valores numéricos que podem ser rating (0-10)
        Write-Host "🔍 POSSÍVEIS CAMPOS DE RATING (valores numéricos 0-10):" -ForegroundColor Yellow
        $foundNumericRating = $false
        $response.movie_data.PSObject.Properties | ForEach-Object {
            $key = $_.Name
            $value = $_.Value
            
            if ($value -is [double] -or $value -is [int] -or $value -is [float] -or $value -is [decimal]) {
                $numValue = [double]$value
                if ($numValue -ge 0 -and $numValue -le 10) {
                    $foundNumericRating = $true
                    Write-Host "   ✅ $key = $numValue (possível rating)" -ForegroundColor Green
                }
            } elseif ($value -is [string]) {
                $numValue = [double]::TryParse($value, [ref]$null)
                if ($numValue -and $numValue -ge 0 -and $numValue -le 10) {
                    $foundNumericRating = $true
                    Write-Host "   ✅ $key = `"$value`" (possível rating como String)" -ForegroundColor Green
                }
            }
        }
        
        if (-not $foundNumericRating) {
            Write-Host "   ⚠️ Nenhum valor numérico entre 0-10 encontrado" -ForegroundColor Yellow
        }
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "RESUMO:" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    if ($null -eq $response.movie_data) {
        Write-Host "❌ PROBLEMA ENCONTRADO: movie_data é NULL" -ForegroundColor Red
        Write-Host "   A API não está retornando movie_data" -ForegroundColor Yellow
        Write-Host "   Isso explica por que o rating não aparece!" -ForegroundColor Yellow
    } elseif ($response.movie_data.PSObject.Properties.Count -eq 0) {
        Write-Host "⚠️ movie_data está vazio" -ForegroundColor Yellow
    } else {
        Write-Host "✅ movie_data contém $($response.movie_data.PSObject.Properties.Count) campos" -ForegroundColor Green
        Write-Host "   Verifique os campos acima para encontrar o rating" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "💡 DICA: Copie o JSON completo para análise:" -ForegroundColor Cyan
    Write-Host "   `$response | ConvertTo-Json -Depth 10" -ForegroundColor Gray
    
} catch {
    Write-Host "❌ Erro ao fazer requisição: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Verifique:" -ForegroundColor Yellow
    Write-Host "  - URL base está correta?" -ForegroundColor White
    Write-Host "  - Username e Password estão corretos?" -ForegroundColor White
    Write-Host "  - VOD ID existe?" -ForegroundColor White
    Write-Host "  - Servidor está acessível?" -ForegroundColor White
}

Write-Host "`n========================================`n" -ForegroundColor Cyan


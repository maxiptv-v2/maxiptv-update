# ========================================
# TESTE RÁPIDO: VOD ID 1 em aztv.cx
# ========================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TESTE: VOD ID 1 - aztv.cx" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Pedir credenciais
Write-Host "Informe as credenciais:" -ForegroundColor Yellow
$Username = Read-Host "Username"
$Password = Read-Host "Password" -AsSecureString
$Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($Password))

$VodId = 1
Write-Host "`nTestando VOD ID: $VodId" -ForegroundColor Yellow

# URLs possíveis
$BaseUrls = @(
    "https://aztv.cx",
    "http://aztv.cx",
    "https://aztv.cx:8080",
    "http://aztv.cx:8080",
    "https://aztv.cx:25463",
    "http://aztv.cx:25463"
)

foreach ($BaseUrl in $BaseUrls) {
    Write-Host "`nTentando: $BaseUrl" -ForegroundColor Cyan
    
    $ApiUrl = "$BaseUrl/player_api.php?username=$Username&password=$Password&action=get_vod_info&vod_id=$VodId"
    
    try {
        $response = Invoke-RestMethod -Uri $ApiUrl -Method Get -ContentType "application/json" -ErrorAction Stop -TimeoutSec 10
        
        Write-Host "✅ SUCESSO! URL funcionando: $BaseUrl" -ForegroundColor Green
        Write-Host ""
        
        # Informações básicas
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
        
        # movie_data
        Write-Host "📊 MOVIE_DATA:" -ForegroundColor Yellow
        if ($null -eq $response.movie_data) {
            Write-Host "   ❌ movie_data é NULL!" -ForegroundColor Red
            Write-Host "   Isso explica por que o rating não aparece!" -ForegroundColor Yellow
        } else {
            Write-Host "   ✅ movie_data existe com $($response.movie_data.PSObject.Properties.Count) campos" -ForegroundColor Green
            Write-Host ""
            Write-Host "   Todos os campos:" -ForegroundColor White
            
            $response.movie_data.PSObject.Properties | ForEach-Object {
                $key = $_.Name
                $value = $_.Value
                $type = $value.GetType().Name
                $valueStr = if ($value -is [string]) { "`"$value`"" } else { $value.ToString() }
                Write-Host "     • $key = $valueStr (tipo: $type)" -ForegroundColor Gray
            }
            Write-Host ""
            
            # Buscar rating
            Write-Host "🔍 BUSCANDO RATING:" -ForegroundColor Yellow
            $ratingFields = @("rating", "imdb_rating", "imdbRating", "tmdb_rating", "tmdbRating", 
                             "rate", "score", "vote_average", "voteAverage", "rotten_tomatoes", 
                             "metacritic_score", "rt_rating")
            
            $foundRating = $false
            foreach ($field in $ratingFields) {
                if ($response.movie_data.PSObject.Properties.Name -contains $field) {
                    $value = $response.movie_data.$field
                    $foundRating = $true
                    Write-Host "   ✅ ENCONTRADO: $field = $value (tipo: $($value.GetType().Name))" -ForegroundColor Green
                }
            }
            
            if (-not $foundRating) {
                Write-Host "   ⚠️ Nenhum campo de rating padrão encontrado" -ForegroundColor Yellow
                Write-Host ""
                Write-Host "   Valores numéricos que podem ser rating (0-10):" -ForegroundColor Yellow
                $response.movie_data.PSObject.Properties | ForEach-Object {
                    $key = $_.Name
                    $value = $_.Value
                    
                    if ($value -is [double] -or $value -is [int] -or $value -is [float] -or $value -is [decimal]) {
                        $numValue = [double]$value
                        if ($numValue -ge 0 -and $numValue -le 10) {
                            Write-Host "     ✅ $key = $numValue (possível rating)" -ForegroundColor Green
                        }
                    } elseif ($value -is [string]) {
                        $parsed = [double]::TryParse($value, [ref]$null)
                        if ($parsed -and [double]$value -ge 0 -and [double]$value -le 10) {
                            Write-Host "     ✅ $key = `"$value`" (possível rating como String)" -ForegroundColor Green
                        }
                    }
                }
            }
        }
        
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "RESUMO:" -ForegroundColor Cyan
        Write-Host "========================================" -ForegroundColor Cyan
        
        if ($null -eq $response.movie_data) {
            Write-Host "❌ PROBLEMA: movie_data é NULL" -ForegroundColor Red
            Write-Host "   A API não retorna movie_data para este VOD" -ForegroundColor Yellow
        } elseif ($foundRating) {
            Write-Host "✅ Rating encontrado!" -ForegroundColor Green
        } else {
            Write-Host "⚠️ Rating não encontrado nos campos padrão" -ForegroundColor Yellow
            Write-Host "   Verifique os campos acima para encontrar o campo correto" -ForegroundColor White
        }
        
        Write-Host ""
        Write-Host "💡 Para ver JSON completo:" -ForegroundColor Cyan
        Write-Host "   `$response | ConvertTo-Json -Depth 10" -ForegroundColor Gray
        
        break
        
    } catch {
        Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
        continue
    }
}

Write-Host "`n========================================`n" -ForegroundColor Cyan


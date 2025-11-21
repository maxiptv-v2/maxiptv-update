# ========================================
# TESTE: API aztv.cx
# ========================================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TESTE: API aztv.cx" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Pedir credenciais
Write-Host "Informe as credenciais:" -ForegroundColor Yellow
$Username = Read-Host "Username"
$Password = Read-Host "Password" -AsSecureString
$Password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($Password))
$VodIdInput = Read-Host "VOD ID para testar (Enter para usar 1)"
$VodId = if ([string]::IsNullOrEmpty($VodIdInput)) { 1 } else { [int]$VodIdInput }

# URLs possíveis (com e sem porta)
$BaseUrls = @(
    "https://aztv.cx",
    "http://aztv.cx",
    "https://aztv.cx:8080",
    "http://aztv.cx:8080",
    "https://aztv.cx:25463",
    "http://aztv.cx:25463"
)

Write-Host "`nTestando diferentes URLs..." -ForegroundColor Yellow

foreach ($BaseUrl in $BaseUrls) {
    Write-Host "`nTentando: $BaseUrl" -ForegroundColor Cyan
    
    $ApiUrl = "$BaseUrl/player_api.php?username=$Username&password=$Password&action=get_vod_info&vod_id=$VodId"
    
    try {
        $response = Invoke-RestMethod -Uri $ApiUrl -Method Get -ContentType "application/json" -ErrorAction Stop -TimeoutSec 10
        
        Write-Host "✅ SUCESSO! URL funcionando: $BaseUrl" -ForegroundColor Green
        Write-Host ""
        
        # Mostrar informações básicas
        Write-Host "📋 INFO:" -ForegroundColor Yellow
        if ($response.info) {
            Write-Host "   name: $($response.info.name)" -ForegroundColor White
            $plotPreview = if ($response.info.plot) { $response.info.plot.Substring(0, [Math]::Min(100, $response.info.plot.Length)) + "..." } else { "null" }
            Write-Host "   plot: $plotPreview" -ForegroundColor White
            Write-Host "   cover: $($response.info.cover)" -ForegroundColor White
        }
        Write-Host ""
        
        # Analisar movie_data
        Write-Host "📊 MOVIE_DATA:" -ForegroundColor Yellow
        if ($null -eq $response.movie_data) {
            Write-Host "   ⚠️ movie_data é NULL!" -ForegroundColor Red
        } else {
            Write-Host "   Total de campos: $($response.movie_data.PSObject.Properties.Count)" -ForegroundColor White
            Write-Host ""
            Write-Host "   Campos disponíveis:" -ForegroundColor White
            
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
            
            # Buscar valores numéricos 0-10
            Write-Host ""
            Write-Host "🔍 POSSÍVEIS CAMPOS DE RATING (valores 0-10):" -ForegroundColor Yellow
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
                    $parsed = [double]::TryParse($value, [ref]$null)
                    if ($parsed -and [double]$value -ge 0 -and [double]$value -le 10) {
                        $foundNumericRating = $true
                        Write-Host "   ✅ $key = `"$value`" (possível rating como String)" -ForegroundColor Green
                    }
                }
            }
        }
        
        Write-Host ""
        Write-Host "💡 Para ver JSON completo:" -ForegroundColor Cyan
        Write-Host "   `$response | ConvertTo-Json -Depth 10" -ForegroundColor Gray
        
        break  # Se funcionou, para de testar outras URLs
        
    } catch {
        Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
        continue
    }
}

Write-Host "`n========================================`n" -ForegroundColor Cyan


# Teste rápido da URL dl.php
$url = "https://maxiptv-update-1.onrender.com/dl/5736"
Write-Host "🔍 Testando URL: $url" -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri $url -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    Write-Host "❌ Não deveria seguir redirect!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "📡 Status Code: $statusCode" -ForegroundColor Yellow
    
    if ($statusCode -eq 302 -or $statusCode -eq 301) {
        $location = $_.Exception.Response.Headers.Location
        Write-Host "✅ Redirect para: $location" -ForegroundColor Green
    } elseif ($statusCode -eq 404) {
        Write-Host "❌ Código não encontrado (404)" -ForegroundColor Red
        
        # Ler corpo da resposta
        try {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorBody = $reader.ReadToEnd()
            Write-Host ""
            Write-Host "Corpo da resposta:" -ForegroundColor Yellow
            Write-Host $errorBody -ForegroundColor White
        } catch {
            Write-Host "Não foi possível ler o corpo da resposta" -ForegroundColor Gray
        }
    } elseif ($statusCode -eq 403) {
        Write-Host "❌ Acesso negado (403)" -ForegroundColor Red
    } else {
        Write-Host "📄 Resposta: $statusCode" -ForegroundColor Yellow
        
        # Ler corpo da resposta
        try {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorBody = $reader.ReadToEnd()
            Write-Host ""
            Write-Host "Corpo da resposta:" -ForegroundColor Yellow
            Write-Host $errorBody -ForegroundColor White
        } catch {
            Write-Host "Não foi possível ler o corpo da resposta" -ForegroundColor Gray
        }
    }
}

Write-Host ""
Write-Host "✅ Teste concluído!" -ForegroundColor Green


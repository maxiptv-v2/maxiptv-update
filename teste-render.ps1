# Teste do servidor Render

Write-Host "🧪 Testando servidor Render..." -ForegroundColor Cyan
Write-Host ""

$url = "https://maxiptv-update.onrender.com/download.php?code=4064"

Write-Host "URL: $url" -ForegroundColor Yellow
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri $url -MaximumRedirection 0 -ErrorAction Stop
    
    Write-Host "✅ Status: $($response.StatusCode)" -ForegroundColor Green
    
    if ($response.Headers.Location) {
        $redirectUrl = $response.Headers.Location
        Write-Host "Redirecionando para: $redirectUrl" -ForegroundColor Cyan
        
        if ($redirectUrl -like "*github.com*") {
            Write-Host ""
            Write-Host "✅ SERVIDOR FUNCIONANDO PERFEITAMENTE!" -ForegroundColor Green
            Write-Host "Cliente será redirecionado para GitHub" -ForegroundColor Yellow
        }
    }
} catch {
    if ($_.Exception.Response.StatusCode) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "❌ HTTP $statusCode" -ForegroundColor Red
        
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorContent = $reader.ReadToEnd()
            Write-Host "Mensagem: $errorContent" -ForegroundColor Yellow
        } catch {
            Write-Host "Não foi possível ler o erro" -ForegroundColor Yellow
        }
    } else {
        Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
    }
}


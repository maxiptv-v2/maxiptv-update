# Script para testar o download com codigo

Write-Host "Testando download com codigo..." -ForegroundColor Cyan
Write-Host ""

# Teste com codigo 4064 (casa1)
$code = "4064"
Write-Host "Testando codigo: $code" -ForegroundColor Yellow
Write-Host ""

# URL do PHP server (substitua pela URL real do seu servidor PHP)
$phpUrl = "https://maxiptvdowloader.com.000webhostapp.com/download.php?code=$code"

Write-Host "URL do PHP: $phpUrl" -ForegroundColor Cyan
Write-Host ""

try {
    Write-Host "Fazendo requisicao..." -ForegroundColor Yellow
    $response = Invoke-WebRequest -Uri $phpUrl -MaximumRedirection 0 -ErrorAction SilentlyContinue
    
    if ($response.StatusCode -eq 302) {
        $redirectUrl = $response.Headers.Location
        Write-Host "Redirecionamento detectado!" -ForegroundColor Green
        Write-Host "URL de redirecionamento: $redirectUrl" -ForegroundColor Cyan
    } else {
        Write-Host "Status Code: $($response.StatusCode)" -ForegroundColor Yellow
        Write-Host "Conteudo: $($response.Content)" -ForegroundColor Gray
    }
} catch {
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $content = (New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd()
        Write-Host "ERRO: Status $statusCode" -ForegroundColor Red
        Write-Host "Mensagem: $content" -ForegroundColor Red
    } else {
        Write-Host "ERRO: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Testando outro codigo: 6444 (leo1)" -ForegroundColor Yellow
$code2 = "6444"
$phpUrl2 = "https://maxiptvdowloader.com.000webhostapp.com/download.php?code=$code2"

try {
    $response2 = Invoke-WebRequest -Uri $phpUrl2 -MaximumRedirection 0 -ErrorAction SilentlyContinue
    
    if ($response2.StatusCode -eq 302) {
        $redirectUrl2 = $response2.Headers.Location
        Write-Host "Redirecionamento detectado!" -ForegroundColor Green
        Write-Host "URL de redirecionamento: $redirectUrl2" -ForegroundColor Cyan
    }
} catch {
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $content = (New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd()
        Write-Host "ERRO: Status $statusCode" -ForegroundColor Red
        Write-Host "Mensagem: $content" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== DIAGNOSTICO ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Possiveis problemas:" -ForegroundColor Yellow
Write-Host "1. URL do PHP server pode estar errada" -ForegroundColor White
Write-Host "2. PHP server pode nao estar online" -ForegroundColor White
Write-Host "3. Codigo pode nao existir no JSONBin" -ForegroundColor White
Write-Host "4. Codigo pode estar inativo ou ja usado" -ForegroundColor White
Write-Host ""
Write-Host "Informe a URL correta do seu servidor PHP!" -ForegroundColor Cyan


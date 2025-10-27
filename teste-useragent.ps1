# Teste com diferentes User-Agents para bypass JavaScript

Write-Host "🧪 Testando diferentes User-Agents..." -ForegroundColor Cyan
Write-Host ""

$url = "http://maipt12.unaux.com/api.php?code=4064"
$userAgents = @(
    "Mozilla/5.0 (Android 11; Mobile) AppleWebKit/537.36",
    "curl/7.68.0",
    "MaxiPTV-Downloader/1.0"
)

foreach ($ua in $userAgents) {
    Write-Host "Testing: $ua" -ForegroundColor Yellow
    
    try {
        $response = Invoke-WebRequest -Uri $url -Headers @{ "User-Agent" = $ua } -UseBasicParsing -ErrorAction Stop
        
        if ($response.Content -like "*apk*" -or $response.Content -like "*github*") {
            Write-Host "✅ SUCESSO com User-Agent!" -ForegroundColor Green
            Write-Host "Conteúdo:" $response.Content.Substring(0, [Math]::Min(200, $response.Content.Length))
            break
        } elseif ($response.Content -like "*aes.js*") {
            Write-Host "❌ Ainda bloqueado por JavaScript" -ForegroundColor Red
        } else {
            Write-Host "❓ Resposta: $($response.Content.Substring(0, [Math]::Min(100, $response.Content.Length)))..." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Write-Host ""
}


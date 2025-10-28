# Script para aguardar e verificar quando o deploy do Render.com terminar

$maxTentativas = 30  # 30 tentativas = 5 minutos
$tentativa = 0

Write-Host "Aguardando deploy do Render.com..." -ForegroundColor Yellow
Write-Host "Verificando a cada 10 segundos..." -ForegroundColor Gray
Write-Host ""

$url = "https://maxiptv-update.onrender.com/valida.php"

while ($tentativa -lt $maxTentativas) {
    $tentativa++
    Write-Host "[$tentativa/$maxTentativas] Testando..." -ForegroundColor Cyan -NoNewline
    
    try {
        $response = Invoke-WebRequest -Uri "$url?code=1234" -Method GET -TimeoutSec 5 -ErrorAction Stop
        
        Write-Host " SUCESSO! Deploy concluido!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Valida.php esta funcionando!" -ForegroundColor Green
        Write-Host "Status: $($response.StatusCode)"
        
        # Tentar parsear resposta
        try {
            $json = $response.Content | ConvertFrom-Json
            Write-Host "Resposta: $($response.Content)"
        } catch {
            Write-Host "Resposta: $($response.Content)"
        }
        
        exit
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($statusCode -eq 404) {
            Write-Host " Ainda nao disponivel (404)" -ForegroundColor Yellow
        } elseif ($statusCode -eq 503 -or $statusCode -eq 502) {
            Write-Host " Servidor em deploy ($statusCode)" -ForegroundColor Yellow
        } else {
            Write-Host " Erro $statusCode" -ForegroundColor Red
        }
        
        if ($tentativa -lt $maxTentativas) {
            Start-Sleep -Seconds 10
        }
    }
}

Write-Host ""
Write-Host "Tempo de espera esgotado. Deploy pode ainda estar em andamento." -ForegroundColor Red
Write-Host "Verifique manualmente no dashboard do Render.com" -ForegroundColor Yellow


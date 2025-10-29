# Testar serviço Render que está em Live

Write-Host "=== TESTANDO SERVIÇO RENDER (LIVE) ===" -ForegroundColor Cyan
Write-Host ""

$url = "https://maxiptv-update.onrender.com/?code=2011"
Write-Host "Testando código 2011..." -ForegroundColor Yellow
Write-Host "URL: $url" -ForegroundColor Gray
Write-Host ""

try {
    $r = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 15
    Write-Host "✅ SUCESSO!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Status: $($r.status)" -ForegroundColor Cyan
    
    if ($r.status -eq 'ok') {
        Write-Host ""
        Write-Host "Dados retornados:" -ForegroundColor Yellow
        Write-Host "  Usuario: $($r.usuario)" -ForegroundColor White
        Write-Host "  Senha: $($r.senha)" -ForegroundColor White
        Write-Host "  API: $($r.api)" -ForegroundColor White
        Write-Host "  Expira: $($r.expira_em)" -ForegroundColor White
        Write-Host "  APK: $($r.apk)" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "✅✅✅ PHP FUNCIONANDO PERFEITAMENTE! ✅✅✅" -ForegroundColor Green
    } else {
        Write-Host "Mensagem: $($r.mensagem)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ ERRO:" -ForegroundColor Red
    $statusCode = if ($_.Exception.Response) { 
        $_.Exception.Response.StatusCode.value__ 
    } else { 
        "N/A" 
    }
    Write-Host "HTTP Status: $statusCode" -ForegroundColor Yellow
    Write-Host "Mensagem: $($_.Exception.Message)" -ForegroundColor Yellow
    
    if ($_.ErrorDetails.Message) {
        Write-Host ""
        Write-Host "Resposta do servidor:" -ForegroundColor Yellow
        try {
            $err = $_.ErrorDetails.Message | ConvertFrom-Json
            Write-Host "Status: $($err.status)" -ForegroundColor White
            Write-Host "Mensagem: $($err.mensagem)" -ForegroundColor White
        } catch {
            Write-Host $_.ErrorDetails.Message.Substring(0, [Math]::Min(500, $_.ErrorDetails.Message.Length)) -ForegroundColor Gray
        }
    }
}

Write-Host ""
Write-Host "=== TESTE CONCLUÍDO ===" -ForegroundColor Cyan


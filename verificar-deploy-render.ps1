# Verificar se o deploy do Render foi concluído

$url = "https://maxiptv-update.onrender.com/?code=2208"

Write-Host "=== VERIFICANDO DEPLOY NO RENDER ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Aguardando 10 segundos..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host "Testando URL: $url" -ForegroundColor Yellow
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri $url -Method Get -TimeoutSec 15
    $content = $response.Content
    
    Write-Host "Status HTTP: $($response.StatusCode)" -ForegroundColor Cyan
    Write-Host ""
    
    try {
        $json = $content | ConvertFrom-Json
        Write-Host "✅ RESPOSTA JSON VALIDA!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Status: $($json.status)" -ForegroundColor $(if ($json.status -eq 'ok') { 'Green' } else { 'Yellow' })
        
        if ($json.status -eq 'ok') {
            Write-Host "Usuario: $($json.usuario)" -ForegroundColor White
            Write-Host "Senha: $($json.senha)" -ForegroundColor White
            Write-Host "API: $($json.api)" -ForegroundColor White
            Write-Host "Expira: $($json.expira_em)" -ForegroundColor White
            Write-Host "APK: $($json.apk)" -ForegroundColor Cyan
            
            Write-Host ""
            Write-Host "✅ PHP FUNCIONANDO CORRETAMENTE!" -ForegroundColor Green
        } else {
            Write-Host "Mensagem: $($json.mensagem)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "⚠️  Resposta não é JSON válido:" -ForegroundColor Yellow
        Write-Host $content.Substring(0, [Math]::Min(500, $content.Length)) -ForegroundColor Gray
        
        # Verificar se há referência a simpleCode na resposta
        if ($content -match 'simpleCode' -or $content -match 'simpleCodes') {
            Write-Host ""
            Write-Host "❌ AINDA TEM REFERENCIA A SIMPLECODE!" -ForegroundColor Red
            Write-Host "O Render ainda está servindo código antigo." -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "❌ ERRO AO CONECTAR:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Yellow
    
    if ($_.ErrorDetails.Message) {
        Write-Host ""
        Write-Host "Detalhes:" -ForegroundColor Yellow
        Write-Host $_.ErrorDetails.Message.Substring(0, [Math]::Min(300, $_.ErrorDetails.Message.Length)) -ForegroundColor Gray
        
        if ($_.ErrorDetails.Message -match 'simpleCode' -or $_.ErrorDetails.Message -match 'simpleCodes') {
            Write-Host ""
            Write-Host "❌ AINDA TEM REFERENCIA A SIMPLECODE!" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "=== TESTE CONCLUIDO ===" -ForegroundColor Cyan


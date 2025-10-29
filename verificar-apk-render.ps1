# Verificar se o Render retorna a URL correta do APK

Write-Host "=== VERIFICANDO URL DO APK NO RENDER ===" -ForegroundColor Cyan
Write-Host ""

$url = "https://maxiptv-update-1.onrender.com/?code=2011"

try {
    Write-Host "Testando Render (pode demorar ate 60s se estiver dormindo)..." -ForegroundColor Yellow
    $r = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 60
    
    Write-Host "✅ Render respondeu!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Status: $($r.status)" -ForegroundColor Cyan
    
    if ($r.status -eq 'ok') {
        Write-Host ""
        Write-Host "🔍 URL DO APK (campo 'apk'):" -ForegroundColor Cyan
        $apkUrl = $r.apk
        Write-Host "  $apkUrl" -ForegroundColor White
        Write-Host ""
        
        if ($apkUrl -match 'raw\.githubusercontent') {
            Write-Host "  ✅ URL CORRETA! (raw.githubusercontent)" -ForegroundColor Green
        } elseif ($apkUrl -match 'releases/latest/download') {
            Write-Host "  ❌ URL ANTIGA! (ainda usando releases/latest/download)" -ForegroundColor Red
        } else {
            Write-Host "  ⚠️  URL diferente do esperado" -ForegroundColor Yellow
        }
        
        Write-Host ""
        Write-Host "Testando se a URL do APK funciona..." -ForegroundColor Yellow
        try {
            $apkTest = Invoke-WebRequest -Uri $apkUrl -Method Head -TimeoutSec 10 -ErrorAction Stop
            Write-Host "  ✅ URL do APK funciona! Status: $($apkTest.StatusCode)" -ForegroundColor Green
            Write-Host "  Tamanho: $([Math]::Round($apkTest.Headers.'Content-Length' / 1MB, 2)) MB" -ForegroundColor White
        } catch {
            Write-Host "  ❌ URL do APK NAO funciona: $($_.Exception.Message)" -ForegroundColor Red
        }
    } else {
        Write-Host "Mensagem: $($r.mensagem)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Timeout ou erro:" -ForegroundColor Red
    Write-Host "  $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "O Render pode estar dormindo. Teste no navegador:" -ForegroundColor Yellow
    Write-Host "  https://maxiptv-update-1.onrender.com/?code=2011" -ForegroundColor Cyan
}


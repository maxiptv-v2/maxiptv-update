# Verificar se URL do APK foi atualizada no Render

Write-Host "=== VERIFICANDO URL DO APK NO RENDER ===" -ForegroundColor Cyan
Write-Host ""

$url = "https://maxiptv-update-1.onrender.com/?code=9999"

try {
    Write-Host "Testando Render (pode demorar ate 60s)..." -ForegroundColor Yellow
    $r = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 60
    
    Write-Host "✅ Render respondeu!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Status: $($r.status)" -ForegroundColor Cyan
    
    if ($r.PSObject.Properties.Name -contains 'apk') {
        $apkUrl = $r.apk
        Write-Host ""
        Write-Host "🔍 URL do APK (campo 'apk'):" -ForegroundColor Cyan
        Write-Host "  $apkUrl" -ForegroundColor White
        Write-Host ""
        
        if ($apkUrl -match 'raw\.githubusercontent\.com') {
            Write-Host "  ✅✅✅ URL CORRETA! (raw.githubusercontent)" -ForegroundColor Green
            Write-Host "  O Render foi atualizado com sucesso!" -ForegroundColor Green
        } elseif ($apkUrl -match 'releases/latest/download') {
            Write-Host "  ❌ URL ANTIGA! (ainda usando releases/latest/download)" -ForegroundColor Red
            Write-Host ""
            Write-Host "  Faca deploy manual no Render!" -ForegroundColor Yellow
        } else {
            Write-Host "  ⚠️  URL diferente do esperado" -ForegroundColor Yellow
        }
    } else {
        Write-Host ""
        Write-Host "⚠️  Campo 'apk' nao encontrado (normal para codigo invalido)" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Mas isso significa que precisamos testar com codigo valido." -ForegroundColor Gray
        Write-Host "Ou verificar o codigo fonte do valida.php no Render." -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "Para confirmar 100%, veja nos logs do Render" -ForegroundColor Cyan
    Write-Host "o commit que foi feito deploy." -ForegroundColor Cyan
    
} catch {
    Write-Host "❌ Timeout ou erro:" -ForegroundColor Red
    Write-Host "  $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Teste manualmente no navegador:" -ForegroundColor Yellow
    Write-Host "  $url" -ForegroundColor Cyan
}


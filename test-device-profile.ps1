$baseUrl = "https://maxiptv-update-1.onrender.com"
$fingerprint = "philco|philco|android tv|philco_tv|sdk30|1920x1080|dpi320"

Write-Host "=== TESTE device-profile.php ===" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. POST (salvar perfil)..." -ForegroundColor Yellow
$body = '{"fingerprint":"philco|philco|android tv|philco_tv|sdk30|1920x1080|dpi320","device":{"manufacturer":"philco","brand":"philco","model":"android tv","product":"philco_tv","sdkInt":30},"screen":{"widthPx":1920,"heightPx":1080,"densityDpi":320,"density":2.0},"scaleFactor":0.95,"overscanAdjusted":true,"source":"android_app_1.0.187","profile":{"profile":"remote","topDp":24.0,"bottomDp":28.0,"startDp":32.0,"endDp":32.0,"scaleFactor":0.95}}'

try {
    $r = Invoke-WebRequest -Uri "$baseUrl/device-profile.php" -Method Post -Body $body -ContentType "application/json" -Headers @{"User-Agent"="MaxiPTV/1.0.187"}
    Write-Host "Status: $($r.StatusCode)" -ForegroundColor Green
    Write-Host "Resposta: $($r.Content)" -ForegroundColor Gray
} catch {
    Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Resposta: $($reader.ReadToEnd())" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "2. GET (buscar perfil)..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

try {
    $r = Invoke-WebRequest -Uri "$baseUrl/device-profile.php?fingerprint=$fingerprint" -Headers @{"User-Agent"="MaxiPTV/1.0.187"}
    Write-Host "Status: $($r.StatusCode)" -ForegroundColor Green
    Write-Host "Resposta: $($r.Content)" -ForegroundColor Gray
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Host "Status: $code" -ForegroundColor $(if($code -eq 404){'Yellow'}else{'Red'})
    if ($code -eq 404) {
        Write-Host "Perfil nao encontrado (normal se acabou de criar)" -ForegroundColor Gray
    } else {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Resposta: $($reader.ReadToEnd())" -ForegroundColor Red
    }
}


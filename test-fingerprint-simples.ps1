# Teste simples do device-profile.php

$baseUrl = "https://maxiptv-update-1.onrender.com"

# Fingerprint de exemplo
$fingerprint = "philco|philco|android tv|philco_tv|sdk30|1920x1080|dpi320"

Write-Host "Testando POST..." -ForegroundColor Yellow

$payload = @{
    fingerprint = $fingerprint
    device = @{
        manufacturer = "philco"
        brand = "philco"
        model = "android tv"
        product = "philco_tv"
        sdkInt = 30
    }
    screen = @{
        widthPx = 1920
        heightPx = 1080
        densityDpi = 320
        density = 2.0
    }
    scaleFactor = 0.95
    overscanAdjusted = $true
    source = "android_app_1.0.187"
    profile = @{
        profile = "remote"
        topDp = 24.0
        bottomDp = 28.0
        startDp = 32.0
        endDp = 32.0
        scaleFactor = 0.95
    }
} | ConvertTo-Json -Depth 5

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/device-profile.php" `
        -Method Post `
        -Body $payload `
        -ContentType "application/json" `
        -Headers @{ "User-Agent" = "MaxiPTV/1.0.187 (Android)" }
    
    Write-Host "POST OK:" -ForegroundColor Green
    $response | ConvertTo-Json
} catch {
    Write-Host "POST ERRO:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        Write-Host $reader.ReadToEnd()
    }
}

Write-Host "`nTestando GET..." -ForegroundColor Yellow
Start-Sleep -Seconds 1

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/device-profile.php?fingerprint=$fingerprint" `
        -Method Get `
        -Headers @{ "User-Agent" = "MaxiPTV/1.0.187 (Android)" }
    
    Write-Host "GET OK:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 5
} catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Host "GET retornou HTTP $code" -ForegroundColor Yellow
    if ($code -eq 404) {
        Write-Host "Perfil nao encontrado (normal se acabou de criar)" -ForegroundColor Gray
    } else {
        Write-Host $_.Exception.Message
    }
}


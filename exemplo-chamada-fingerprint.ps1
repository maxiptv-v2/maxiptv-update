# EXEMPLO COMPLETO - Como chamar device-profile.php corretamente
# Simula exatamente o que o app Android faz

$baseUrl = "https://maxiptv-update-1.onrender.com"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "EXEMPLO: Como chamar device-profile.php" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# PASSO 1: Gerar fingerprint (igual ao app Android)
# Formato: manufacturer|brand|model|product|sdk{numero}|{width}x{height}|dpi{densityDpi}
# Tudo em lowercase, separado por |

$manufacturer = "philco"
$brand = "philco"
$model = "android tv"
$product = "philco_tv"
$sdkInt = 30
$widthPx = 1920
$heightPx = 1080
$densityDpi = 320

$fingerprint = "$manufacturer|$brand|$model|$product|sdk$sdkInt|${widthPx}x${heightPx}|dpi$densityDpi"

Write-Host "[PASSO 1] Fingerprint gerado:" -ForegroundColor Yellow
Write-Host "  $fingerprint" -ForegroundColor Gray
Write-Host ""

# PASSO 2: Fazer POST para salvar o perfil
Write-Host "[PASSO 2] Enviando POST para salvar perfil..." -ForegroundColor Yellow

$payload = @{
    fingerprint = $fingerprint
    device = @{
        manufacturer = $manufacturer
        brand = $brand
        model = $model
        product = $product
        sdkInt = $sdkInt
    }
    screen = @{
        widthPx = $widthPx
        heightPx = $heightPx
        densityDpi = $densityDpi
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

Write-Host "Payload JSON:" -ForegroundColor Gray
Write-Host $payload -ForegroundColor DarkGray
Write-Host ""

try {
    $responsePost = Invoke-RestMethod -Uri "$baseUrl/device-profile.php" `
        -Method Post `
        -Body $payload `
        -ContentType "application/json" `
        -Headers @{ "User-Agent" = "MaxiPTV/1.0.187 (Android)" }
    
    Write-Host "✓ POST sucesso:" -ForegroundColor Green
    $responsePost | ConvertTo-Json
} catch {
    Write-Host "✗ POST falhou:" -ForegroundColor Red
    Write-Host "  HTTP: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "  Resposta: $errorBody" -ForegroundColor Red
    }
}
Write-Host ""

# PASSO 3: Fazer GET para buscar o perfil salvo
Write-Host "[PASSO 3] Buscando perfil com GET..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

try {
    $responseGet = Invoke-RestMethod -Uri "$baseUrl/device-profile.php?fingerprint=$fingerprint" `
        -Method Get `
        -Headers @{ "User-Agent" = "MaxiPTV/1.0.187 (Android)" }
    
    Write-Host "✓ GET sucesso:" -ForegroundColor Green
    $responseGet | ConvertTo-Json -Depth 5
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "  ⚠ GET retornou 404 (perfil não encontrado)" -ForegroundColor Yellow
        Write-Host "  Isso é normal se o POST não salvou ainda" -ForegroundColor Gray
    } else {
        Write-Host "✗ GET falhou:" -ForegroundColor Red
        Write-Host "  HTTP: $statusCode" -ForegroundColor Red
        Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
    }
}
Write-Host ""

# RESUMO
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA LOGICA:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Fingerprint = manufacturer|brand|model|product|sdk{numero}|{width}x{height}|dpi{dpi}" -ForegroundColor White
Write-Host "   Exemplo: philco|philco|android tv|philco_tv|sdk30|1920x1080|dpi320" -ForegroundColor Gray
Write-Host ""
Write-Host "2. POST /device-profile.php" -ForegroundColor White
Write-Host "   Body: JSON com fingerprint, device, screen, scaleFactor, profile" -ForegroundColor Gray
Write-Host ""
Write-Host "3. GET /device-profile.php?fingerprint={fingerprint}" -ForegroundColor White
Write-Host "   Retorna: JSON com status ok e profile ou 404" -ForegroundColor Gray
Write-Host ""



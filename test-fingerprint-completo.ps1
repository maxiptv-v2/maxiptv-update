# Script de teste completo para device-profile.php
# Simula exatamente como o app Android chama o endpoint

$baseUrl = "https://maxiptv-update-1.onrender.com"
$headers = @{ 
    "Content-Type" = "application/json"
    "User-Agent" = "MaxiPTV/1.0.187 (Android)"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE COMPLETO - device-profile.php" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. TESTE GET - Buscar perfil inexistente (deve retornar not_found)
Write-Host "[1/4] Testando GET (fingerprint inexistente)..." -ForegroundColor Yellow
$testFingerprint = "test_android_device_$(Get-Date -Format 'yyyyMMddHHmmss')"
try {
    $responseGet = Invoke-RestMethod -Uri "$baseUrl/device-profile.php?fingerprint=$testFingerprint" `
        -Method Get `
        -Headers @{ "User-Agent" = "MaxiPTV/1.0.187 (Android)" }
    
    Write-Host "  ✓ GET retornou: " -NoNewline -ForegroundColor Green
    $responseGet | ConvertTo-Json -Depth 3
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "  ✓ GET retornou 404 (not_found) - CORRETO!" -ForegroundColor Green
    } else {
        Write-Host "  ✗ GET falhou: HTTP $statusCode" -ForegroundColor Red
        Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
    }
}
Write-Host ""

# 2. TESTE POST - Enviar perfil (simula o app Android)
Write-Host "[2/4] Testando POST (enviar perfil)..." -ForegroundColor Yellow

$payload = @{
    fingerprint = $testFingerprint
    device = @{
        manufacturer = "test"
        brand = "test"
        model = "Android TV Test"
        product = "test_product"
        sdkInt = 33
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
    $responsePost = Invoke-RestMethod -Uri "$baseUrl/device-profile.php" `
        -Method Post `
        -Body $payload `
        -Headers $headers
    
    Write-Host "  ✓ POST retornou: " -NoNewline -ForegroundColor Green
    $responsePost | ConvertTo-Json -Depth 3
    
    if ($responsePost.status -eq "ok") {
        Write-Host "  ✓ Perfil salvo com sucesso!" -ForegroundColor Green
    } else {
        Write-Host "  ✗ POST retornou status diferente de 'ok'" -ForegroundColor Red
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "  ✗ POST falhou: HTTP $statusCode" -ForegroundColor Red
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
    
    # Tentar ler o body do erro
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        Write-Host "  Resposta: $errorBody" -ForegroundColor Red
    }
}
Write-Host ""

# 3. TESTE GET - Buscar perfil recém-salvo (deve retornar o perfil)
Write-Host "[3/4] Testando GET (buscar perfil recém-salvo)..." -ForegroundColor Yellow
Start-Sleep -Seconds 2  # Aguardar um pouco para garantir que foi salvo

try {
    $responseGet2 = Invoke-RestMethod -Uri "$baseUrl/device-profile.php?fingerprint=$testFingerprint" `
        -Method Get `
        -Headers @{ "User-Agent" = "MaxiPTV/1.0.187 (Android)" }
    
    Write-Host "  ✓ GET retornou o perfil:" -ForegroundColor Green
    $responseGet2 | ConvertTo-Json -Depth 5
    
    if ($responseGet2.status -eq "ok" -and $responseGet2.profile) {
        Write-Host "  ✓ Perfil encontrado e válido!" -ForegroundColor Green
    } else {
        Write-Host "  ✗ GET não retornou perfil válido" -ForegroundColor Red
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "  ✗ GET falhou: HTTP $statusCode" -ForegroundColor Red
    Write-Host "  Erro: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# 4. VERIFICAR NA JSONBIN DIRETAMENTE
Write-Host "[4/4] Verificando diretamente na JSONBin..." -ForegroundColor Yellow
try {
    $jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
    $jsonbinKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
    
    $jsonbinResponse = Invoke-RestMethod -Uri $jsonbinUrl `
        -Headers @{ "X-Master-Key" = $jsonbinKey }
    
    $profiles = $jsonbinResponse.record._device_profiles
    
    if ($profiles -and $profiles.PSObject.Properties.Name -contains $testFingerprint) {
        Write-Host "  ✓ Fingerprint encontrado na JSONBin!" -ForegroundColor Green
        Write-Host "  Total de perfis salvos: $($profiles.PSObject.Properties.Count)" -ForegroundColor Cyan
    } else {
        Write-Host "  ✗ Fingerprint NÃO encontrado na JSONBin" -ForegroundColor Red
        Write-Host "  Total de perfis na bin: $($profiles.PSObject.Properties.Count)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ Erro ao consultar JSONBin: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE CONCLUÍDO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Fingerprint usado no teste: $testFingerprint" -ForegroundColor Gray



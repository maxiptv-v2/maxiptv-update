# Testar valida.php completo

$code = Read-Host "Digite o codigo para testar (ou Enter para 6986)"
if ([string]::IsNullOrWhiteSpace($code)) {
    $code = "6986"
}

Write-Host ""
Write-Host "=== TESTANDO VALIDA.PHP ===" -ForegroundColor Cyan
Write-Host "Codigo: $code" -ForegroundColor Yellow
Write-Host ""

# Testar valida.php
Write-Host "1. Testando /valida.php?code=$code" -ForegroundColor Yellow
try {
    $url = "https://maxiptv-update.onrender.com/valida.php?code=$code"
    $response = Invoke-RestMethod -Uri $url -Method Get
    Write-Host "   Status: $($response.status)" -ForegroundColor $(if ($response.status -eq "ok") { "Green" } else { "Red" })
    if ($response.status -eq "ok") {
        Write-Host "   Usuario: $($response.usuario)" -ForegroundColor Green
        Write-Host "   Senha: $($response.senha)" -ForegroundColor Green
        Write-Host "   API: $($response.api)" -ForegroundColor Green
        Write-Host "   Expira: $($response.expira_em)" -ForegroundColor Green
        Write-Host "   APK: $($response.apk)" -ForegroundColor Cyan
    } else {
        Write-Host "   Mensagem: $($response.mensagem)" -ForegroundColor Red
    }
} catch {
    Write-Host "   Erro: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "   Detalhes: $($_.ErrorDetails.Message)" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "2. Testando /?code=$code (root)" -ForegroundColor Yellow
try {
    $url = "https://maxiptv-update.onrender.com/?code=$code"
    $response = Invoke-RestMethod -Uri $url -Method Get
    Write-Host "   Status: $($response.status)" -ForegroundColor $(if ($response.status -eq "ok") { "Green" } else { "Red" })
    if ($response.status -eq "ok") {
        Write-Host "   Usuario: $($response.usuario)" -ForegroundColor Green
    } else {
        Write-Host "   Mensagem: $($response.mensagem)" -ForegroundColor Red
    }
} catch {
    Write-Host "   Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. Verificando codigo no JSONBin..." -ForegroundColor Yellow
try {
    $headers = @{
        "X-Master-Key" = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
    }
    $r = Invoke-RestMethod -Uri "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest" -Headers $headers
    if ($r.record.$code) {
        Write-Host "   Codigo $code encontrado no JSONBin!" -ForegroundColor Green
        Write-Host "   Username: $($r.record.$code.username)" -ForegroundColor White
        Write-Host "   Password: $($r.record.$code.password)" -ForegroundColor White
    } else {
        Write-Host "   Codigo $code NAO encontrado no JSONBin" -ForegroundColor Red
        Write-Host "   Chaves disponiveis: $($r.record.PSObject.Properties.Name -join ', ')" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   Erro ao verificar JSONBin: $($_.Exception.Message)" -ForegroundColor Red
}


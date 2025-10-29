# Testar se a preservacao funciona simulando o que o app faz

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

Write-Host "=== TESTE DE PRESERVACAO DE CODIGOS ===" -ForegroundColor Cyan
Write-Host ""

# 1. Adicionar codigos
Write-Host "1. Adicionando codigos 4709 e 1234..." -ForegroundColor Yellow
.\limpar-e-adicionar-codigos.ps1 | Select-Object -Last 8

Start-Sleep -Seconds 2

# 2. Verificar se estao la
Write-Host ""
Write-Host "2. Verificando se codigos foram adicionados..." -ForegroundColor Yellow
$r = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers
$codigosAntes = $r.record.PSObject.Properties | Where-Object { $_.Name -match '^\d{4}$' }
Write-Host "   Codigos antes: $($codigosAntes.Name -join ', ')" -ForegroundColor $(if ($codigosAntes) { "Green" } else { "Red" })

# 3. Simular salvamento de sessions/users (como o app faz)
Write-Host ""
Write-Host "3. Simulando salvamento de sessions/users (como app antigo faria)..." -ForegroundColor Yellow

$record = @{}

# Buscar sessions e users atuais
$record['sessions'] = $r.record.sessions
$record['users'] = $r.record.users

# Preservar codigos (como o app NOVO faz)
$codigosAntes | ForEach-Object {
    $record[$_.Name] = $_.Value
    Write-Host "   ✅ Preservando codigo: $($_.Name)" -ForegroundColor Green
}

# Atualizar sessions (simular heartbeat)
$record['sessions'].casa1.lastHeartbeat = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
Write-Host "   ✅ Atualizando heartbeat de casa1" -ForegroundColor Cyan

# Salvar de volta
$jsonBody = $record | ConvertTo-Json -Depth 10
$putHeaders = @{
    "X-Master-Key" = $apiKey
    "Content-Type" = "application/json"
}
$putUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"

Invoke-RestMethod -Uri $putUrl -Method Put -Headers $putHeaders -Body $jsonBody | Out-Null
Write-Host "   ✅ Salvo no JSONBin" -ForegroundColor Green

# 4. Verificar se codigos ainda estao la
Write-Host ""
Write-Host "4. Verificando se codigos foram preservados..." -ForegroundColor Yellow
Start-Sleep -Seconds 1
$r2 = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers
$codigosDepois = $r2.record.PSObject.Properties | Where-Object { $_.Name -match '^\d{4}$' }
Write-Host "   Codigos depois: $($codigosDepois.Name -join ', ')" -ForegroundColor $(if ($codigosDepois) { "Green" } else { "Red" })

if ($codigosDepois -and $codigosAntes) {
    $preservados = ($codigosAntes.Name | Where-Object { $codigosDepois.Name -contains $_ })
    if ($preservados.Count -eq $codigosAntes.Count) {
        Write-Host ""
        Write-Host "✅ TESTE PASSOU! Codigos foram preservados!" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "❌ TESTE FALHOU! Alguns codigos foram perdidos" -ForegroundColor Red
    }
} elseif ($codigosDepois) {
    Write-Host ""
    Write-Host "⚠️  Codigos presentes mas diferentes dos originais" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "❌ TESTE FALHOU! Nenhum codigo encontrado apos salvamento" -ForegroundColor Red
}


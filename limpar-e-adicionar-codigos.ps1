# Limpar codigos antigos e adicionar apenas os codigos especificados

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

Write-Host "=== LIMPANDO E ADICIONANDO CODIGOS ===" -ForegroundColor Cyan
Write-Host ""

# 1. Buscar estrutura atual
Write-Host "1. Buscando estrutura atual..." -ForegroundColor Yellow
$r = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers
$record = $r.record

Write-Host "   Chaves atuais: $($record.PSObject.Properties.Name -join ', ')" -ForegroundColor White
Write-Host ""

# 2. Criar novo record preservando APENAS sessions e users
$novoRecord = @{}

# Preservar sessions se existir
if ($record.sessions) {
    $novoRecord['sessions'] = $record.sessions
    Write-Host "   ✅ Sessions preservado" -ForegroundColor Green
}

# Preservar users se existir
if ($record.users) {
    $novoRecord['users'] = $record.users
    $usersCount = if ($record.users -is [Array]) { $record.users.Count } else { $record.users.PSObject.Properties.Count }
    Write-Host "   ✅ Users preservado ($usersCount usuarios)" -ForegroundColor Green
}

# 3. Adicionar APENAS os codigos especificados
Write-Host ""
Write-Host "2. Adicionando codigos conforme modelo..." -ForegroundColor Yellow

# URL correta do APK
$apkUrlCorreto = "https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/maxiptv-release.apk"

# Codigo 4709 (casa1)
$novoRecord['4709'] = @{
    username = "casa1"
    password = "1234"
    apiUrl = "https://canais.is/player_api.php"
    expiryDate = "01/11/2025"
    apkUrl = $apkUrlCorreto
}
Write-Host "   ✅ Codigo 4709 adicionado" -ForegroundColor Green

# Codigo 1234 (casa2)
$novoRecord['1234'] = @{
    username = "casa2"
    password = "1234"
    apiUrl = "https://canais.is/player_api.php"
    expiryDate = "26/11/2025"
    apkUrl = $apkUrlCorreto
}
Write-Host "   ✅ Codigo 1234 adicionado" -ForegroundColor Green

# 4. Converter para JSON
$jsonBody = $novoRecord | ConvertTo-Json -Depth 10

Write-Host ""
Write-Host "3. Enviando para JSONBin (limpando codigos antigos e adicionando novos)..." -ForegroundColor Yellow

# 5. Enviar para JSONBin
$putHeaders = @{
    "X-Master-Key" = $apiKey
    "Content-Type" = "application/json"
}

$putUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"

try {
    $response = Invoke-RestMethod -Uri $putUrl -Method Put -Headers $putHeaders -Body $jsonBody
    Write-Host "   ✅ JSONBin atualizado com sucesso!" -ForegroundColor Green
    Write-Host ""
    
    # 6. Verificar resultado
    Write-Host "4. Verificando resultado..." -ForegroundColor Yellow
    $r2 = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers
    Write-Host "   Chaves finais: $($r2.record.PSObject.Properties.Name -join ', ')" -ForegroundColor White
    Write-Host ""
    
    # Mostrar códigos
    $codigos = $r2.record.PSObject.Properties | Where-Object { $_.Name -match '^\d{4}$' }
    Write-Host "   Codigos encontrados:" -ForegroundColor Cyan
    foreach ($codigo in $codigos) {
        $c = $codigo.Value
        Write-Host "     • $($codigo.Name): $($c.username) - Expira: $($c.expiryDate)" -ForegroundColor White
        Write-Host "       API: $($c.apiUrl)" -ForegroundColor Gray
        Write-Host "       APK: $($c.apkUrl)" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "✅ JSONBin atualizado conforme modelo!" -ForegroundColor Green
    
} catch {
    Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "   Detalhes: $($_.ErrorDetails.Message)" -ForegroundColor Gray
    }
}


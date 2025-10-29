# Atualizar JSONBin com modelo correto (preservando sessions e users)

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

Write-Host "=== ATUALIZANDO JSONBIN COM MODELO CORRETO ===" -ForegroundColor Cyan
Write-Host ""

# 1. Buscar estrutura atual
Write-Host "1. Buscando estrutura atual..." -ForegroundColor Yellow
$r = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers
$record = $r.record

Write-Host "   Chaves atuais: $($record.PSObject.Properties.Name -join ', ')" -ForegroundColor White
Write-Host ""

# 2. Criar novo record preservando sessions e users
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

# 3. Adicionar códigos no formato correto usando dados reais dos usuários
Write-Host ""
Write-Host "2. Adicionando codigos no formato correto..." -ForegroundColor Yellow

# URL correta do APK
$apkUrlCorreto = "https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/maxiptv-release.apk"

# Buscar dados reais dos usuários para usar as URLs corretas
$userCasa1 = $record.users | Where-Object { $_.username -eq "casa1" } | Select-Object -First 1
$userCasa2 = $record.users | Where-Object { $_.username -eq "casa2" } | Select-Object -First 1

# Código 1: casa1 (com URLs corretas do usuário)
if ($userCasa1) {
    $novoRecord['4709'] = @{
        username = $userCasa1.username
        password = $userCasa1.password
        apiUrl = $userCasa1.apiUrl
        expiryDate = $userCasa1.expiryDate
        apkUrl = $apkUrlCorreto
    }
    Write-Host "   ✅ Codigo 4709 ($($userCasa1.username)) - API: $($userCasa1.apiUrl)" -ForegroundColor Green
} else {
    Write-Host "   ⚠️ Usuario casa1 nao encontrado" -ForegroundColor Yellow
}

# Código 2: casa2 (com URLs corretas do usuário)
if ($userCasa2) {
    $novoRecord['1234'] = @{
        username = $userCasa2.username
        password = $userCasa2.password
        apiUrl = $userCasa2.apiUrl
        expiryDate = $userCasa2.expiryDate
        apkUrl = $apkUrlCorreto
    }
    Write-Host "   ✅ Codigo 1234 ($($userCasa2.username)) - API: $($userCasa2.apiUrl)" -ForegroundColor Green
} else {
    Write-Host "   ⚠️ Usuario casa2 nao encontrado" -ForegroundColor Yellow
}

# 4. Converter para JSON
$jsonBody = $novoRecord | ConvertTo-Json -Depth 10

Write-Host ""
Write-Host "3. Enviando para JSONBin..." -ForegroundColor Yellow

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
    }
    
    Write-Host ""
    Write-Host "✅ JSONBin atualizado conforme modelo!" -ForegroundColor Green
    
} catch {
    Write-Host "   ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "   Detalhes: $($_.ErrorDetails.Message)" -ForegroundColor Gray
    }
}


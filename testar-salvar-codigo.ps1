# Testar se o codigo esta salvando corretamente

Write-Host "Verificando estrutura ANTES de salvar..." -ForegroundColor Cyan
Write-Host ""

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

# Ver estrutura atual
$r = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers
Write-Host "Chaves atuais: $($r.record.PSObject.Properties.Name -join ', ')" -ForegroundColor Yellow
Write-Host ""

# Criar codigo de teste no formato correto
$codigoTeste = "9999"
$codigoData = @{
    username = "casa1"
    password = "1234"
    apiUrl = "https://canais.is/player_api.php"
    expiryDate = "01/11/2025"
    apkUrl = "https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/maxiptv-release.apk"
}

Write-Host "Criando codigo de teste: $codigoTeste" -ForegroundColor Cyan
Write-Host ""

# Buscar record atual e adicionar o codigo
$recordAtual = $r.record

# Criar novo objeto preservando tudo
$novoRecord = @{}
$recordAtual.PSObject.Properties | ForEach-Object {
    $novoRecord[$_.Name] = $_.Value
}

# Adicionar codigo
$novoRecord[$codigoTeste] = $codigoData

# Converter para JSON
$jsonBody = $novoRecord | ConvertTo-Json -Depth 10

Write-Host "JSON que sera enviado (primeiros 500 chars):" -ForegroundColor Yellow
Write-Host $jsonBody.Substring(0, [Math]::Min(500, $jsonBody.Length))
Write-Host ""

# Enviar
Write-Host "Enviando para JSONBin..." -ForegroundColor Cyan
$putHeaders = @{
    "X-Master-Key" = $apiKey
    "Content-Type" = "application/json"
}

$putUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"

try {
    $response = Invoke-RestMethod -Uri $putUrl -Method Put -Headers $putHeaders -Body $jsonBody
    Write-Host "Codigo salvo com sucesso!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar resultado
    Write-Host "Verificando estrutura DEPOIS de salvar..." -ForegroundColor Cyan
    $r2 = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers
    Write-Host "Chaves agora: $($r2.record.PSObject.Properties.Name -join ', ')" -ForegroundColor Yellow
    Write-Host ""
    
    if ($r2.record.$codigoTeste) {
        Write-Host "Codigo $codigoTeste encontrado!" -ForegroundColor Green
        Write-Host "   Username: $($r2.record.$codigoTeste.username)"
        Write-Host "   ApkUrl: $($r2.record.$codigoTeste.apkUrl)"
    }
    
    if ($r2.record.sessions) {
        Write-Host "Sessions preservado: $($r2.record.sessions.PSObject.Properties.Count)" -ForegroundColor Green
    }
    
    if ($r2.record.users) {
        $usersCount = if ($r2.record.users -is [Array]) { $r2.record.users.Count } else { $r2.record.users.PSObject.Properties.Count }
        Write-Host "Users preservado: $usersCount" -ForegroundColor Green
    }
    
} catch {
    Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
}

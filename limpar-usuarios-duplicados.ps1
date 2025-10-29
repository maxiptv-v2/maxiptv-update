# Limpar usuários duplicados no JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

Write-Host "=== LIMPANDO USUARIOS DUPLICADOS ===" -ForegroundColor Cyan
Write-Host ""

# Buscar estrutura atual
$r = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers
$record = $r.record

Write-Host "Usuarios antes: $($record.users.Count)" -ForegroundColor Yellow
$record.users | ForEach-Object { Write-Host "  • $($_.username) (ID: $($_.id))" }

# Remover duplicados mantendo apenas o primeiro de cada username
$usuariosUnicos = @()
$usernamesVistos = @{}

foreach ($user in $record.users) {
    if (-not $usernamesVistos.ContainsKey($user.username)) {
        $usuariosUnicos += $user
        $usernamesVistos[$user.username] = $true
        Write-Host "  ✅ Mantendo: $($user.username) (ID: $($user.id))" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Removendo duplicado: $($user.username) (ID: $($user.id))" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Usuarios unicos: $($usuariosUnicos.Count)" -ForegroundColor Yellow

# Criar novo record preservando tudo exceto usuários duplicados
$novoRecord = @{}

# Preservar sessions
if ($record.sessions) {
    $novoRecord['sessions'] = $record.sessions
}

# Preservar códigos
$record.PSObject.Properties | Where-Object { $_.Name -match '^\d{4}$' } | ForEach-Object {
    $novoRecord[$_.Name] = $_.Value
    Write-Host "  ✅ Preservando codigo: $($_.Name)" -ForegroundColor Cyan
}

# Adicionar apenas usuários únicos
$novoRecord['users'] = $usuariosUnicos

# Enviar para JSONBin
$jsonBody = $novoRecord | ConvertTo-Json -Depth 10
$putHeaders = @{
    "X-Master-Key" = $apiKey
    "Content-Type" = "application/json"
}
$putUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"

try {
    Invoke-RestMethod -Uri $putUrl -Method Put -Headers $putHeaders -Body $jsonBody | Out-Null
    Write-Host ""
    Write-Host "✅ JSONBin atualizado! Duplicados removidos." -ForegroundColor Green
    
    # Verificar resultado
    $r2 = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers
    Write-Host ""
    Write-Host "Usuarios finais: $($r2.record.users.Count)" -ForegroundColor Yellow
    $r2.record.users | ForEach-Object { Write-Host "  • $($_.username)" -ForegroundColor White }
    
} catch {
    Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}


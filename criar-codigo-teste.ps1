# Criar código de teste manualmente no JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

Write-Host "Criando codigo de teste..." -ForegroundColor Cyan

try {
    # Buscar dados atuais
    $response = Invoke-RestMethod -Uri "$jsonbin_url/latest" -Headers @{"X-Master-Key" = $apiKey} -Method Get
    
    # Criar código de teste
    $testCode = @{
        usuario = "casa2"
        senha = "1234"
        api = "https://canais.is/player_api.php"
        apk = "https://maxiptv-update.onrender.com/download.php?code=2273"
        expira_em = "31/12/2030"
        ativo = $true
        usado = $false
        usado_em = $null
        usado_device = $null
    }
    
    # Adicionar ao simpleCodes
    if (-not $response.record.simpleCodes) {
        $response.record.simpleCodes = @{}
    }
    
    $response.record.simpleCodes["2273"] = $testCode
    
    # Salvar
    $body = @{
        simpleCodes = $response.record.simpleCodes
        users = $response.record.users
        sessions = $response.record.sessions
    } | ConvertTo-Json -Depth 10 -Compress
    
    $updateHeaders = @{
        "X-Master-Key" = $apiKey
        "Content-Type" = "application/json"
    }
    
    Invoke-RestMethod -Uri $jsonbin_url -Headers $updateHeaders -Method Put -Body $body | Out-Null
    
    Write-Host "Codigo 2273 criado com sucesso!" -ForegroundColor Green
    
} catch {
    Write-Host "Erro:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


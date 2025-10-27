# Corrigir estrutura do JSONBin adicionando simpleCodes

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

Write-Host "Buscando dados atuais..." -ForegroundColor Cyan

try {
    # Buscar dados atuais
    $response = Invoke-RestMethod -Uri "$jsonbin_url/latest" -Headers @{"X-Master-Key" = $apiKey} -Method Get
    
    Write-Host "Dados encontrados:" -ForegroundColor Green
    Write-Host "  Sessoes: $($response.record.sessions.Count)"
    Write-Host "  Usuarios: $($response.record.users.Count)"
    
    # Verificar se simpleCodes existe
    if (-not ($response.record.PSObject.Properties.Name -contains "simpleCodes")) {
        Write-Host ""
        Write-Host "Adicionando simpleCodes vazio..." -ForegroundColor Yellow
        
        # Criar objeto customizado
        $newRecord = @{
            sessions = $response.record.sessions
            users = $response.record.users
            simpleCodes = @{}
        }
        
        # Converter para JSON
        $body = $newRecord | ConvertTo-Json -Depth 100 -Compress
        
        Write-Host ""
        Write-Host "JSON a enviar:" -ForegroundColor Cyan
        Write-Host $body
        Write-Host ""
        
        # Enviar de volta
        $updateHeaders = @{
            "X-Master-Key" = $apiKey
            "Content-Type" = "application/json"
        }
        
        $updateResponse = Invoke-RestMethod -Uri $jsonbin_url -Headers $updateHeaders -Method Put -Body $body
        
        Write-Host "SUCESSO! simpleCodes criado no JSONBin." -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "simpleCodes ja existe" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "Erro:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


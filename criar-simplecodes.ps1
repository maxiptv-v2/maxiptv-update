# Criar estrutura simpleCodes vazia no JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

Write-Host "Buscando dados atuais..." -ForegroundColor Cyan

try {
    # Buscar dados atuais
    $response = Invoke-RestMethod -Uri "$jsonbin_url/latest" -Headers @{"X-Master-Key" = $apiKey} -Method Get
    
    Write-Host "Dados atuais encontrados:" -ForegroundColor Green
    Write-Host "  Sessoes: $($response.record.sessions.Count)"
    Write-Host "  Usuarios: $($response.record.users.Count)"
    
    # Adicionar simpleCodes vazio se não existir
    if (-not $response.record.simpleCodes) {
        Write-Host ""
        Write-Host "Adicionando simpleCodes vazio..." -ForegroundColor Yellow
        
        $response.record | Add-Member -MemberType NoteProperty -Name "simpleCodes" -Value @{}
        
        # Enviar de volta
        $body = $response.record | ConvertTo-Json -Depth 100
        
        $updateHeaders = @{
            "X-Master-Key" = $apiKey
            "Content-Type" = "application/json"
        }
        
        $updateResponse = Invoke-RestMethod -Uri $jsonbin_url -Headers $updateHeaders -Method Put -Body $body
        
        Write-Host "SUCESSO! simpleCodes criado." -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "simpleCodes ja existe com $($response.record.simpleCodes.Count) codigos" -ForegroundColor Green
    }
    
} catch {
    Write-Host "Erro:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


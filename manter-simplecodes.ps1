# Manter simpleCodes sempre presente no JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$latest_url = "$jsonbin_url/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

while ($true) {
    try {
        # Buscar dados atuais
        $response = Invoke-RestMethod -Uri $latest_url -Headers @{"X-Master-Key" = $apiKey} -Method Get
        
        $needsUpdate = $false
        
        # Verificar se simpleCodes existe
        if (-not $response.record.simpleCodes) {
            Write-Host "simpleCodes NAO existe! Adicionando..." -ForegroundColor Yellow
            $response.record | Add-Member -MemberType NoteProperty -Name "simpleCodes" -Value @{} -Force
            $needsUpdate = $true
        }
        
        # Se precisa atualizar
        if ($needsUpdate) {
            $body = $response.record | ConvertTo-Json -Depth 10 -Compress
            
            $updateHeaders = @{
                "X-Master-Key" = $apiKey
                "Content-Type" = "application/json"
            }
            
            Invoke-RestMethod -Uri $jsonbin_url -Headers $updateHeaders -Method Put -Body $body | Out-Null
            Write-Host "simpleCodes restaurado!" -ForegroundColor Green
        } else {
            Write-Host "simpleCodes OK (Total: $($response.record.simpleCodes.Count))" -ForegroundColor White
        }
        
        Start-Sleep -Seconds 10
        
    } catch {
        Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
        Start-Sleep -Seconds 5
    }
}


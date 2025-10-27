# Verificar se simpleCodes existe no JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

Write-Host "Verificando estrutura do JSONBin..." -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers -Method Get
    $record = $response.record
    
    Write-Host "Estrutura atual:" -ForegroundColor Yellow
    Write-Host "  Keys: $($record.PSObject.Properties.Name -join ', ')" -ForegroundColor White
    Write-Host ""
    
    if ($record.sessions) {
        Write-Host "Sessoes: $($record.sessions.Count)" -ForegroundColor Green
    }
    
    if ($record.users) {
        Write-Host "Usuarios: $($record.users.Count)" -ForegroundColor Green
        $record.users | Select-Object -First 3 | ForEach-Object {
            Write-Host "  - $($_.username)" -ForegroundColor White
        }
    }
    
    if ($record.simpleCodes) {
        Write-Host "Codigos: $($record.simpleCodes.Count)" -ForegroundColor Green
    } else {
        Write-Host "Codigos: 0 (nao encontrado)" -ForegroundColor Red
        Write-Host ""
        Write-Host "Acao: Preciso gerar codigos pelo painel admin!" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "Erro:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


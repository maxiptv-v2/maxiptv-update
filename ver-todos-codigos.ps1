# Ver todos os codigos no JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

Write-Host "Buscando dados do JSONBin..." -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers -Method Get
    $record = $response.record
    
    Write-Host "Estrutura encontrada:" -ForegroundColor Green
    Write-Host "  Sessoes: $($record.sessions.Count)"
    Write-Host "  Usuarios: $($record.users.Count)"
    Write-Host ""
    
    if ($record.simpleCodes) {
        Write-Host "Total de codigos: $($record.simpleCodes.Count)" -ForegroundColor Yellow
        Write-Host ""
        
        foreach ($code in $record.simpleCodes.PSObject.Properties.Name) {
            $data = $record.simpleCodes.$code
            $ativo = if ($data.ativo) { "SIM" } else { "NAO" }
            $usado = if ($data.usado) { "SIM" } else { "NAO" }
            
            Write-Host "Codigo: $code" -ForegroundColor Cyan
            Write-Host "  Usuario: $($data.usuario)"
            Write-Host "  Ativo: $ativo"
            Write-Host "  Usado: $usado"
            Write-Host "  Expira: $($data.expira_em)"
            Write-Host ""
        }
    } else {
        Write-Host "AVISO: simpleCodes nao existe no JSONBin!" -ForegroundColor Red
        Write-Host ""
        Write-Host "O app precisa salvar os codigos primeiro." -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "Erro ao buscar:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


# Script para verificar estrutura do JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

Write-Host "Buscando dados do JSONBin..." -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers -Method Get
    
    Write-Host "Sucesso! Estrutura recebida:" -ForegroundColor Green
    Write-Host ""
    
    # Mostrar todas as keys do record
    $record = $response.record
    Write-Host "Keys no record:" -ForegroundColor Yellow
    $record.PSObject.Properties.Name | ForEach-Object {
        Write-Host "  - $_" -ForegroundColor White
    }
    Write-Host ""
    
    # Verificar se simpleCodes existe
    if ($record.simpleCodes) {
        Write-Host "simpleCodes encontrado! Total de codigos:" -ForegroundColor Green
        $codes = $record.simpleCodes
        Write-Host "  $($codes.Count)" -ForegroundColor White
        Write-Host ""
        
        # Mostrar alguns codigos
        $count = 0
        Write-Host "Primeiros codigos:" -ForegroundColor Cyan
        $codes.PSObject.Properties.Name | Select-Object -First 5 | ForEach-Object {
            $count++
            $code = $_
            $data = $codes.$code
            Write-Host ""
            Write-Host "Codigo: $code" -ForegroundColor Yellow
            Write-Host "  Usuario: $($data.usuario)"
            Write-Host "  Ativo: $($data.ativo)"
            Write-Host "  Usado: $($data.usado)"
        }
    } else {
        Write-Host "simpleCodes NAO encontrado!" -ForegroundColor Red
    }
    
} catch {
    Write-Host "Erro ao buscar dados:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

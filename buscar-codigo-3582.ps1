# Buscar código 3582 no JSONBin completo

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{ "X-Master-Key" = $apiKey }

try {
    $response = Invoke-RestMethod -Uri "$jsonbin_url/latest" -Headers $headers -Method Get
    
    Write-Host "Estrutura completa do record:" -ForegroundColor Cyan
    $response.record | ConvertTo-Json -Depth 10 | Out-File -FilePath "jsonbin-completo.json"
    
    Write-Host "Arquivo salvo: jsonbin-completo.json" -ForegroundColor Green
    Write-Host ""
    Write-Host "Buscando código 3582..." -ForegroundColor Yellow
    
    # Buscar em todas as propriedades
    $response.record.PSObject.Properties | ForEach-Object {
        $key = $_.Name
        $value = $_.Value
        
        if ($key -match "3582" -or ($value -is [Hashtable] -and $value.ContainsKey("3582"))) {
            Write-Host "ENCONTRADO em: $key" -ForegroundColor Green
        }
        
        if ($value -is [Hashtable]) {
            Write-Host "Campo: $key, Count: $($value.Count)"
            $value.Keys | Where-Object { $_ -match "3582" } | ForEach-Object {
                Write-Host "  -> Codigo encontrado: $_" -ForegroundColor Green
            }
        }
    }
    
} catch {
    Write-Host "Erro:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


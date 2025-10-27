# Ver estrutura completa do JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{ "X-Master-Key" = $apiKey }

Write-Host "Buscando dados completos..." -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri "$jsonbin_url/latest" -Headers $headers -Method Get
    
    Write-Host "Keys na raiz do record:" -ForegroundColor Yellow
    $response.record.PSObject.Properties | ForEach-Object {
        $key = $_.Name
        $value = $_.Value
        $type = $value.GetType().Name
        
        Write-Host ""
        Write-Host "Key: $key" -ForegroundColor Cyan
        Write-Host "  Type: $type"
        
        if ($type -eq "Hashtable" -or $type -eq "PSCustomObject") {
            Write-Host "  Count: $($value.Count)"
            
            if ($key -eq "simpleCodes") {
                Write-Host ""
                Write-Host "  CONTEUDO SIMPLECODES:" -ForegroundColor Green
                $value.PSObject.Properties | ForEach-Object {
                    Write-Host "    Codigo: $($_.Name)" -ForegroundColor Yellow
                    Write-Host "      Usuario: $($_.Value.usuario)"
                    Write-Host "      Ativo: $($_.Value.ativo)"
                    Write-Host "      Usado: $($_.Value.usado)"
                }
            }
        }
    }
    
    Write-Host ""
    Write-Host "JSON COMPLETO (primeiros 2000 chars):" -ForegroundColor Magenta
    Write-Host ($response.record | ConvertTo-Json -Depth 5).Substring(0, 2000)
    
} catch {
    Write-Host "Erro:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


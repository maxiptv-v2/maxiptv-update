# Testar a lógica do PHP localmente

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$code = "2273"

Write-Host "Testando logica do PHP..." -ForegroundColor Cyan
Write-Host "URL: $jsonbin_url" -ForegroundColor Gray
Write-Host "Codigo: $code"
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Headers @{"X-Master-Key" = $apiKey} -Method Get
    
    Write-Host "Resposta recebida:" -ForegroundColor Green
    Write-Host "  Record keys: $($response.record.PSObject.Properties.Name -join ', ')"
    Write-Host ""
    
    if ($response.record.simpleCodes) {
        Write-Host "simpleCodes EXISTE!" -ForegroundColor Green
        Write-Host "  Total: $($response.record.simpleCodes.Count)"
        
        if ($response.record.simpleCodes.$code) {
            $data = $response.record.simpleCodes.$code
            Write-Host ""
            Write-Host "Codigo $code encontrado:" -ForegroundColor Yellow
            Write-Host "  Usuario: $($data.usuario)"
            Write-Host "  Senha: $($data.senha)"
            Write-Host "  Ativo: $($data.ativo)"
            Write-Host "  Usado: $($data.usado)"
            Write-Host "  Expira: $($data.expira_em)"
            
            Write-Host ""
            Write-Host "RESULTADO ESPERADO:" -ForegroundColor Cyan
            $result = @{
                success = $true
                usuario = $data.usuario
                senha = $data.senha
                api = $data.api
                expira_em = $data.expira_em
            } | ConvertTo-Json
            
            Write-Host $result -ForegroundColor White
        } else {
            Write-Host "Codigo $code NAO encontrado!" -ForegroundColor Red
        }
    } else {
        Write-Host "simpleCodes NAO EXISTE!" -ForegroundColor Red
    }
    
} catch {
    Write-Host "Erro:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


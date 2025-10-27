# Script de Diagnostico JSONBin
# Verifica conexao e estrutura completa

Write-Host "Diagnostico JSONBin MaxiPTV" -ForegroundColor Cyan
Write-Host ""

# Credenciais
$BIN_ID = "68ec647643b1c97be964e96b"
$MASTER_KEY = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$URL = "https://api.jsonbin.io/v3/b/$BIN_ID"

Write-Host "Conectando ao JSONBin..." -ForegroundColor Yellow
Write-Host "URL: $URL" -ForegroundColor Gray
Write-Host ""

try {
    # Buscar dados
    $headers = @{
        "X-Master-Key" = $MASTER_KEY
    }
    
    $response = Invoke-RestMethod -Uri $URL -Headers $headers -Method Get
    
    Write-Host "Conexao bem-sucedida!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar estrutura
    Write-Host "Analisando estrutura..." -ForegroundColor Yellow
    
    $record = $response.record
    
    if ($record.users) {
        $users = $record.users
        Write-Host "Campo 'users' encontrado" -ForegroundColor Green
        Write-Host "Total de usuarios: $($users.Count)" -ForegroundColor Cyan
        
        if ($users.Count -gt 0) {
            Write-Host ""
            Write-Host "Usuarios cadastrados:" -ForegroundColor Yellow
            foreach ($user in $users) {
                Write-Host "  - $($user.username) (ID: $($user.id))" -ForegroundColor White
                Write-Host "    API: $($user.apiUrl)" -ForegroundColor Gray
                Write-Host "    Expira: $($user.expiryDate)" -ForegroundColor Gray
                Write-Host ""
            }
        }
    } else {
        Write-Host "Campo 'users' NAO encontrado!" -ForegroundColor Red
    }
    
    Write-Host ""
    
    if ($record.simpleCodes) {
        $codes = $record.simpleCodes
        Write-Host "Campo 'simpleCodes' encontrado" -ForegroundColor Green
        Write-Host "Total de codigos: $($codes.Count)" -ForegroundColor Cyan
        
        if ($codes.Count -gt 0) {
            Write-Host ""
            Write-Host "Codigos cadastrados:" -ForegroundColor Yellow
            foreach ($codeEntry in $codes.PSObject.Properties) {
                $code = $codeEntry.Name
                $codeData = $codeEntry.Value
                $ativo = if ($codeData.ativo) { "Ativo" } else { "Inativo" }
                $usado = if ($codeData.usado) { "Usado" } else { "Nao usado" }
                Write-Host "  Codigo: $code" -ForegroundColor White
                Write-Host "    Usuario: $($codeData.usuario)" -ForegroundColor Gray
                Write-Host "    Status: $ativo / $usado" -ForegroundColor Gray
                Write-Host ""
            }
        }
    } else {
        Write-Host "Campo 'simpleCodes' NAO encontrado!" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "Chaves disponiveis no record:" -ForegroundColor Yellow
    foreach ($key in $record.PSObject.Properties.Name) {
        Write-Host "  - $key" -ForegroundColor White
    }
    
} catch {
    Write-Host "Erro ao conectar com JSONBin!" -ForegroundColor Red
    Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Concluido" -ForegroundColor Cyan
Write-Host ""

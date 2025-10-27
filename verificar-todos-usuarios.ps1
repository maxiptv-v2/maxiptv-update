# Script para verificar TODOS os usuarios no JSONBin

Write-Host "Verificando TODOS os usuarios no JSONBin..." -ForegroundColor Cyan
Write-Host ""

# Credenciais
$BIN_ID = "68ec647643b1c97be964e96b"
$MASTER_KEY = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$URL = "https://api.jsonbin.io/v3/b/$BIN_ID"

try {
    $headers = @{
        "X-Master-Key" = $MASTER_KEY
    }
    
    $response = Invoke-RestMethod -Uri $URL -Headers $headers -Method Get
    
    Write-Host "Conexao bem-sucedida!" -ForegroundColor Green
    Write-Host ""
    
    $record = $response.record
    
    # Verificar campo 'users'
    if ($record.users -and $record.users.Count -gt 0) {
        Write-Host "Total de usuarios no JSONBin: $($record.users.Count)" -ForegroundColor Yellow
        Write-Host ""
        
        foreach ($user in $record.users) {
            Write-Host "ID: $($user.id)" -ForegroundColor Cyan
            Write-Host "  Username: $($user.username)" -ForegroundColor White
            Write-Host "  API: $($user.apiUrl)" -ForegroundColor Gray
            Write-Host "  Expira: $($user.expiryDate)" -ForegroundColor Gray
            Write-Host ""
        }
    } else {
        Write-Host "ERRO: Nenhum usuario encontrado ou campo 'users' esta vazio!" -ForegroundColor Red
        Write-Host "Campo 'users' existe? $($record.users -ne $null)" -ForegroundColor Yellow
        Write-Host "Tipo do campo: $($record.users.GetType().FullName)" -ForegroundColor Yellow
    }
    
    Write-Host "Chaves disponiveis no JSON:" -ForegroundColor Yellow
    foreach ($key in $record.PSObject.Properties.Name) {
        Write-Host "  - $key" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "Estrutura completa em JSON:" -ForegroundColor Yellow
    $record | ConvertTo-Json -Depth 10
    
} catch {
    Write-Host "Erro ao conectar com JSONBin!" -ForegroundColor Red
    Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
}


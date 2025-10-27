# Buscar usuarios e codigos relacionados a "marcelo"

Write-Host "Buscando usuarios 'marcelo' no JSONBin..." -ForegroundColor Cyan
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
    
    Write-Host "JSONBin conectado com sucesso!" -ForegroundColor Green
    Write-Host ""
    
    $record = $response.record
    
    # Buscar em 'users'
    Write-Host "=== USUARIOS NO ARRAY 'users' ===" -ForegroundColor Yellow
    if ($record.users) {
        $foundUsers = $record.users | Where-Object { $_.username -like "*marcelo*" }
        if ($foundUsers) {
            Write-Host "Usuarios encontrados com 'marcelo':" -ForegroundColor Green
            foreach ($user in $foundUsers) {
                Write-Host "  - Username: $($user.username)" -ForegroundColor White
                Write-Host "    ID: $($user.id)" -ForegroundColor Gray
                Write-Host "    API: $($user.apiUrl)" -ForegroundColor Gray
                Write-Host "    Expira: $($user.expiryDate)" -ForegroundColor Gray
                Write-Host ""
            }
        } else {
            Write-Host "Nenhum usuario com 'marcelo' encontrado em 'users'" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "Todos os usuarios em 'users':" -ForegroundColor Yellow
            foreach ($user in $record.users) {
                Write-Host "  - $($user.username)" -ForegroundColor White
            }
        }
    } else {
        Write-Host "Campo 'users' nao encontrado!" -ForegroundColor Red
    }
    
    Write-Host ""
    
    # Buscar em 'simpleCodes'
    Write-Host "=== CODIGOS NO CAMPO 'simpleCodes' ===" -ForegroundColor Yellow
    if ($record.simpleCodes) {
        $foundCodes = $record.simpleCodes.PSObject.Properties | Where-Object { $_.Value.usuario -like "*marcelo*" }
        if ($foundCodes) {
            Write-Host "Codigos encontrados com usuario 'marcelo':" -ForegroundColor Green
            foreach ($codeEntry in $foundCodes) {
                $code = $codeEntry.Name
                $codeData = $codeEntry.Value
                Write-Host "  Codigo: $code" -ForegroundColor White
                Write-Host "    Usuario: $($codeData.usuario)" -ForegroundColor Cyan
                Write-Host "    Ativo: $($codeData.ativo)" -ForegroundColor Gray
                Write-Host "    Usado: $($codeData.usado)" -ForegroundColor Gray
                Write-Host ""
            }
        } else {
            Write-Host "Nenhum codigo com usuario 'marcelo' encontrado em 'simpleCodes'" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "Todos os usuarios em 'simpleCodes':" -ForegroundColor Yellow
            $usersInCodes = @{}
            foreach ($codeEntry in $record.simpleCodes.PSObject.Properties) {
                $usuario = $codeEntry.Value.usuario
                if (-not $usersInCodes.ContainsKey($usuario)) {
                    $usersInCodes[$usuario] = @()
                }
                $usersInCodes[$usuario] += $codeEntry.Name
            }
            foreach ($usuario in $usersInCodes.Keys) {
                $codigos = $usersInCodes[$usuario] -join ", "
                Write-Host "  $usuario : $codigos" -ForegroundColor White
            }
        }
    } else {
        Write-Host "Campo 'simpleCodes' nao encontrado!" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "=== RESUMO ===" -ForegroundColor Cyan
    Write-Host "Total de usuarios no array 'users': $($record.users.Count)" -ForegroundColor White
    Write-Host "Total de codigos em 'simpleCodes': $($record.simpleCodes.PSObject.Properties.Count)" -ForegroundColor White
    
    # Usuarios unicos em codigos
    $uniqueUsersInCodes = @{}
    foreach ($codeEntry in $record.simpleCodes.PSObject.Properties) {
        $usuario = $codeEntry.Value.usuario
        if (-not $uniqueUsersInCodes.ContainsKey($usuario)) {
            $uniqueUsersInCodes[$usuario] = 0
        }
        $uniqueUsersInCodes[$usuario]++
    }
    Write-Host "Usuarios unicos em 'simpleCodes': $($uniqueUsersInCodes.Keys.Count)" -ForegroundColor White
    foreach ($usuario in $uniqueUsersInCodes.Keys) {
        Write-Host "  - $usuario : $($uniqueUsersInCodes[$usuario]) codigo(s)" -ForegroundColor Gray
    }
    
} catch {
    Write-Host "Erro ao conectar com JSONBin!" -ForegroundColor Red
    Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
}


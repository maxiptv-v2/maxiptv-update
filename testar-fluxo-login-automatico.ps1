# Script para testar fluxo completo de login automatico
# Mostra EXATAMENTE o que esta retornando e ensina como corrigir

param(
    [Parameter(Mandatory=$true)]
    [string]$Code
)

Write-Host "=== TESTE COMPLETO DO FLUXO DE LOGIN AUTOMATICO ===" -ForegroundColor Cyan
Write-Host "Codigo: $Code" -ForegroundColor Yellow
Write-Host ""

$serverUrl = "https://maxiptv-update-1.onrender.com"
$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$jsonbinKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{"X-Master-Key" = $jsonbinKey}

# PASSO 1: Simular acesso do downloader (dl.php)
Write-Host "PASSO 1: Simulando downloader acessando dl.php..." -ForegroundColor Cyan
try {
    $dlUrl = "$serverUrl/dl/$Code"
    Write-Host "   Acessando: $dlUrl" -ForegroundColor Gray
    
    $dlResponse = Invoke-WebRequest -Uri $dlUrl -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    
    if ($dlResponse.StatusCode -eq 302) {
        $redirectUrl = $dlResponse.Headers.Location
        Write-Host "   OK Redirect para APK: $redirectUrl" -ForegroundColor Green
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 302) {
        Write-Host "   OK Redirect detectado (302)" -ForegroundColor Green
    } else {
        Write-Host "   ERRO: Status $statusCode" -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorContent = $reader.ReadToEnd()
            Write-Host "   Resposta: $errorContent" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "PASSO 2: Verificando se codigo foi salvo em _pending_logins..." -ForegroundColor Cyan
Start-Sleep -Seconds 2
try {
    $jsonbin = Invoke-RestMethod -Uri "$jsonbinUrl/latest" -Headers $headers -Method Get
    if ($jsonbin.record._pending_logins) {
        $pending = $jsonbin.record._pending_logins
        Write-Host "   OK _pending_logins encontrado!" -ForegroundColor Green
        Write-Host "   Total de codigos pendentes: $($pending.PSObject.Properties.Count)" -ForegroundColor White
        
        $found = $false
        $pending.PSObject.Properties | ForEach-Object {
            $pendingData = $_.Value
            if ($pendingData.code -eq $Code) {
                $found = $true
                Write-Host "   OK Codigo $Code encontrado!" -ForegroundColor Green
                Write-Host "      Username: $($pendingData.username)" -ForegroundColor White
                Write-Host "      Timestamp: $($pendingData.timestamp)" -ForegroundColor White
            }
        }
        
        if (-not $found) {
            Write-Host "   AVISO: Codigo $Code NAO encontrado em _pending_logins" -ForegroundColor Yellow
            Write-Host "   Isso significa que dl.php NAO salvou o codigo pendente!" -ForegroundColor Red
            Write-Host ""
            Write-Host "   CORRECAO: Verifique se dl.php esta salvando em _pending_logins" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ERRO: _pending_logins nao existe ou esta vazio" -ForegroundColor Red
        Write-Host "   Isso significa que dl.php NAO esta salvando o codigo!" -ForegroundColor Red
    }
} catch {
    Write-Host "   ERRO ao verificar: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "PASSO 3: Testando get-pending-code.php (o que o app chama)..." -ForegroundColor Cyan
try {
    $pendingUrl = "$serverUrl/get-pending-code.php"
    Write-Host "   URL: $pendingUrl" -ForegroundColor Gray
    
    $pendingResponse = Invoke-RestMethod -Uri $pendingUrl -Method Get -TimeoutSec 10
    Write-Host "   RESPOSTA REAL:" -ForegroundColor Yellow
    $pendingResponse | ConvertTo-Json -Depth 5 | Write-Host
    
    if ($pendingResponse.status -eq "ok") {
        Write-Host "   OK Status: ok" -ForegroundColor Green
        Write-Host "   Codigo retornado: $($pendingResponse.code)" -ForegroundColor Green
    } else {
        Write-Host "   AVISO: Status diferente de 'ok'" -ForegroundColor Yellow
        Write-Host "   Status retornado: $($pendingResponse.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ERRO: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorContent = $reader.ReadToEnd()
        Write-Host "   Resposta: $errorContent" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "PASSO 4: Testando auto_login.php (o que o app chama com o codigo)..." -ForegroundColor Cyan
try {
    $autoLoginUrl = "$serverUrl/auto_login.php?code=$Code"
    Write-Host "   URL: $autoLoginUrl" -ForegroundColor Gray
    
    $autoLoginResponse = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -TimeoutSec 10
    Write-Host "   RESPOSTA REAL (JSON completo):" -ForegroundColor Yellow
    $jsonComplete = $autoLoginResponse | ConvertTo-Json -Depth 5
    Write-Host $jsonComplete -ForegroundColor White
    
    Write-Host ""
    Write-Host "   ANALISANDO FORMATO NOVO..." -ForegroundColor Cyan
    
    # Verificar novo formato: { "status": "success", "autologin": { ... } }
    $temStatus = $autoLoginResponse.PSObject.Properties.Name -contains "status"
    $temAutologin = $autoLoginResponse.PSObject.Properties.Name -contains "autologin"
    
    if ($temStatus) {
        $status = $autoLoginResponse.status
        Write-Host "   Campo 'status': $status" -ForegroundColor $(if ($status -eq "success") { "Green" } else { "Yellow" })
        
        if ($status -eq "success") {
            Write-Host "   OK Status: success" -ForegroundColor Green
            
            if ($temAutologin) {
                $autologin = $autoLoginResponse.autologin
                Write-Host "   OK Objeto 'autologin' presente" -ForegroundColor Green
                
                # Verificar campos dentro de autologin
                $temUsername = $autologin.PSObject.Properties.Name -contains "username"
                $temPassword = $autologin.PSObject.Properties.Name -contains "password"
                $temApiUrl = $autologin.PSObject.Properties.Name -contains "api_url"
                $temExpiresIn = $autologin.PSObject.Properties.Name -contains "expires_in"
                $temExpiryDate = $autologin.PSObject.Properties.Name -contains "expiryDate"
                
                Write-Host "   Campo 'autologin.username': $(if ($temUsername) { "OK - valor: $($autologin.username)" } else { "FALTANDO!" })" -ForegroundColor $(if ($temUsername) { "Green" } else { "Red" })
                Write-Host "   Campo 'autologin.password': $(if ($temPassword) { "OK - valor: $($autologin.password)" } else { "FALTANDO!" })" -ForegroundColor $(if ($temPassword) { "Green" } else { "Red" })
                Write-Host "   Campo 'autologin.api_url': $(if ($temApiUrl) { "OK - valor: $($autologin.api_url)" } else { "FALTANDO!" })" -ForegroundColor $(if ($temApiUrl) { "Green" } else { "Red" })
                Write-Host "   Campo 'autologin.expires_in': $(if ($temExpiresIn) { "OK - valor: $($autologin.expires_in) segundos" } else { "FALTANDO!" })" -ForegroundColor $(if ($temExpiresIn) { "Green" } else { "Red" })
                Write-Host "   Campo 'autologin.expiryDate': $(if ($temExpiryDate) { "OK - valor: $($autologin.expiryDate)" } else { "FALTANDO!" })" -ForegroundColor $(if ($temExpiryDate) { "Green" } else { "Red" })
                
                # Verificar se todos os campos obrigatorios estao presentes
                if ($temUsername -and $temPassword -and $temApiUrl -and $temExpiresIn -and $temExpiryDate) {
                    Write-Host ""
                    Write-Host "   OK FORMATO CORRETO! Todos os campos obrigatorios presentes." -ForegroundColor Green
                    Write-Host "   O app deve conseguir fazer login automatico com esse formato!" -ForegroundColor Green
                } else {
                    Write-Host ""
                    Write-Host "   ERRO: FORMATO INCORRETO! Faltam campos obrigatorios." -ForegroundColor Red
                }
            } else {
                Write-Host "   ERRO: Campo 'autologin' FALTANDO!" -ForegroundColor Red
            }
        } elseif ($status -eq "expired") {
            Write-Host "   AVISO: Status 'expired' - Usuario expirado" -ForegroundColor Yellow
            Write-Host "   Mensagem: $($autoLoginResponse.message)" -ForegroundColor Yellow
        } else {
            Write-Host "   AVISO: Status diferente de 'success': $status" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ERRO: Campo 'status' FALTANDO!" -ForegroundColor Red
        Write-Host ""
        Write-Host "   FORMATO ESPERADO:" -ForegroundColor Cyan
        Write-Host "   {" -ForegroundColor White
        Write-Host '     "status": "success",' -ForegroundColor White
        Write-Host '     "autologin": {' -ForegroundColor White
        Write-Host '       "username": "...",' -ForegroundColor White
        Write-Host '       "password": "...",' -ForegroundColor White
        Write-Host '       "api_url": "...",' -ForegroundColor White
        Write-Host '       "expires_in": 21600,' -ForegroundColor White
        Write-Host '       "expiryDate": "DD/MM/YYYY"' -ForegroundColor White
        Write-Host "     }" -ForegroundColor White
        Write-Host "   }" -ForegroundColor White
    }
    
} catch {
    Write-Host "   ERRO ao testar auto_login.php: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status HTTP: $statusCode" -ForegroundColor Red
        
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorContent = $reader.ReadToEnd()
            Write-Host "   Resposta recebida:" -ForegroundColor Red
            Write-Host $errorContent -ForegroundColor Red
        } catch {
            Write-Host "   Nao foi possivel ler a resposta" -ForegroundColor Red
        }
    }
}

Write-Host ""
Write-Host "=== RESUMO ===" -ForegroundColor Cyan
Write-Host "Se o login automatico nao funciona, verifique:" -ForegroundColor Yellow
Write-Host "1. dl.php esta salvando o codigo em _pending_logins?" -ForegroundColor White
Write-Host "2. get-pending-code.php esta retornando o codigo correto?" -ForegroundColor White
Write-Host "3. auto_login.php esta retornando {user, password, api, expiryDate}?" -ForegroundColor White
Write-Host "4. O app esta chamando get-pending-code.php quando abre?" -ForegroundColor White
Write-Host "5. O app esta chamando auto_login.php com o codigo recebido?" -ForegroundColor White
Write-Host ""


# Script completo para diagnosticar login automático
# Explica a lógica e mostra EXATAMENTE onde está o erro

param(
    [Parameter(Mandatory=$true)]
    [string]$Code
)

Write-Host "=== DIAGNOSTICO COMPLETO DO LOGIN AUTOMATICO ===" -ForegroundColor Cyan
Write-Host "Codigo: $Code" -ForegroundColor Yellow
Write-Host ""

$serverUrl = "https://maxiptv-update-1.onrender.com"
$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$jsonbinKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{"X-Master-Key" = $jsonbinKey}

Write-Host "=== LOGICA DO FLUXO DE LOGIN AUTOMATICO ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "FLUXO ESPERADO:" -ForegroundColor Yellow
Write-Host "1. Cliente acessa: https://maxiptv-update-1.onrender.com/dl/$Code" -ForegroundColor White
Write-Host "2. dl.php valida o codigo e salva em _pending_logins" -ForegroundColor White
Write-Host "3. dl.php redireciona para o APK no GitHub" -ForegroundColor White
Write-Host "4. Cliente baixa e instala o APK" -ForegroundColor White
Write-Host "5. App abre e chama get-pending-code.php" -ForegroundColor White
Write-Host "6. get-pending-code.php retorna o codigo pendente" -ForegroundColor White
Write-Host "7. App chama auto_login.php?code=$Code" -ForegroundColor White
Write-Host "8. auto_login.php retorna credenciais no formato JSON" -ForegroundColor White
Write-Host "9. App processa JSON e faz login automatico" -ForegroundColor White
Write-Host "10. App navega para home" -ForegroundColor White
Write-Host ""

Write-Host "=== PASSO 1: Verificando se codigo existe no JSONBin ===" -ForegroundColor Cyan
try {
    $jsonbin = Invoke-RestMethod -Uri "$jsonbinUrl/latest" -Headers $headers -Method Get
    
    if ($jsonbin.record.$Code) {
        $codeData = $jsonbin.record.$Code
        Write-Host "   OK Codigo encontrado" -ForegroundColor Green
        Write-Host "      Username: $($codeData.username)" -ForegroundColor White
        Write-Host "      Expiry Date: $($codeData.expiryDate)" -ForegroundColor White
    } else {
        Write-Host "   ERRO: Codigo $Code NAO encontrado no JSONBin!" -ForegroundColor Red
        Write-Host ""
        Write-Host "   CORRECAO:" -ForegroundColor Yellow
        Write-Host "   1. Abra o app no painel admin (5 toques no logo)" -ForegroundColor White
        Write-Host "   2. Encontre o usuario e clique em 'Gerar Codigo'" -ForegroundColor White
        Write-Host "   3. O codigo sera salvo automaticamente no JSONBin" -ForegroundColor White
        exit 1
    }
} catch {
    Write-Host "   ERRO: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== PASSO 2: Testando dl.php (simula downloader) ===" -ForegroundColor Cyan
Write-Host "   LOGICA: dl.php deve validar codigo, salvar em _pending_logins e redirecionar para APK" -ForegroundColor Gray
try {
    $dlUrl = "$serverUrl/dl/$Code"
    Write-Host "   URL: $dlUrl" -ForegroundColor Gray
    
    try {
        $dlResponse = Invoke-WebRequest -Uri $dlUrl -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 302) {
            $redirectUrl = $_.Exception.Response.Headers.Location
            Write-Host "   OK dl.php redirecionou (302)" -ForegroundColor Green
            Write-Host "      Para: $redirectUrl" -ForegroundColor White
            
            if ($redirectUrl -match "maxiptv-release\.apk") {
                Write-Host "   OK URL do APK esta correta" -ForegroundColor Green
            } else {
                Write-Host "   AVISO: URL do APK pode estar incorreta" -ForegroundColor Yellow
            }
        } elseif ($statusCode -eq 404 -or $statusCode -eq 403) {
            Write-Host "   ERRO: dl.php retornou $statusCode" -ForegroundColor Red
            Write-Host "      Possiveis causas:" -ForegroundColor Yellow
            Write-Host "      - Codigo expirado (mais de 6 horas)" -ForegroundColor White
            Write-Host "      - Usuario expirado (expiryDate passou)" -ForegroundColor White
            Write-Host "      - Codigo nao existe" -ForegroundColor White
        }
    }
} catch {
    Write-Host "   ERRO: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== PASSO 3: Verificando se codigo foi salvo em _pending_logins ===" -ForegroundColor Cyan
Write-Host "   LOGICA: dl.php DEVE salvar o codigo em _pending_logins para o app buscar depois" -ForegroundColor Gray
Start-Sleep -Seconds 2
try {
    $jsonbin = Invoke-RestMethod -Uri "$jsonbinUrl/latest" -Headers $headers -Method Get
    
    if ($jsonbin.record._pending_logins) {
        $pending = $jsonbin.record._pending_logins
        Write-Host "   OK _pending_logins encontrado" -ForegroundColor Green
        
        $found = $false
        $pending.PSObject.Properties | ForEach-Object {
            $pendingData = $_.Value
            if ($pendingData.code -eq $Code) {
                $found = $true
                Write-Host "   OK Codigo $Code encontrado em _pending_logins!" -ForegroundColor Green
                Write-Host "      Username: $($pendingData.username)" -ForegroundColor White
                Write-Host "      Timestamp: $($pendingData.timestamp)" -ForegroundColor White
            }
        }
        
        if (-not $found) {
            Write-Host "   ERRO: Codigo $Code NAO encontrado em _pending_logins!" -ForegroundColor Red
            Write-Host ""
            Write-Host "   PROBLEMA: dl.php NAO salvou o codigo pendente" -ForegroundColor Red
            Write-Host ""
            Write-Host "   VERIFICAR:" -ForegroundColor Yellow
            Write-Host "   1. dl.php esta salvando corretamente em _pending_logins?" -ForegroundColor White
            Write-Host "   2. Verificar logs do Render para ver erros do PHP" -ForegroundColor White
            Write-Host "   3. Verificar se usuario nao esta expirado (dl.php bloqueia se expirado)" -ForegroundColor White
        }
    } else {
        Write-Host "   ERRO: _pending_logins NAO existe ou esta vazio!" -ForegroundColor Red
        Write-Host ""
        Write-Host "   PROBLEMA: dl.php NAO esta salvando codigos pendentes" -ForegroundColor Red
        Write-Host ""
        Write-Host "   VERIFICAR:" -ForegroundColor Yellow
        Write-Host "   1. dl.php esta executando a parte que salva em _pending_logins?" -ForegroundColor White
        Write-Host "   2. Verificar se ha erros no codigo PHP do dl.php" -ForegroundColor White
    }
} catch {
    Write-Host "   ERRO: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== PASSO 4: Testando get-pending-code.php ===" -ForegroundColor Cyan
Write-Host "   LOGICA: App chama isso quando abre. Deve retornar codigo pendente se existir" -ForegroundColor Gray
try {
    $pendingUrl = "$serverUrl/get-pending-code.php"
    Write-Host "   URL que app chama: $pendingUrl" -ForegroundColor Gray
    
    $pendingResponse = Invoke-RestMethod -Uri $pendingUrl -Method Get -TimeoutSec 10
    
    Write-Host "   Resposta:" -ForegroundColor Yellow
    $pendingResponse | ConvertTo-Json -Depth 5 | Write-Host
    
    if ($pendingResponse.status -eq "ok") {
        Write-Host "   OK Status: ok" -ForegroundColor Green
        Write-Host "   Codigo retornado: $($pendingResponse.code)" -ForegroundColor Green
        
        if ($pendingResponse.code -eq $Code) {
            Write-Host "   OK Codigo correto!" -ForegroundColor Green
        } else {
            Write-Host "   AVISO: Codigo retornado diferente do esperado" -ForegroundColor Yellow
            Write-Host "      Esperado: $Code" -ForegroundColor White
            Write-Host "      Recebido: $($pendingResponse.code)" -ForegroundColor White
        }
    } else {
        Write-Host "   ERRO: Status diferente de 'ok'" -ForegroundColor Red
        Write-Host "      Status: $($pendingResponse.status)" -ForegroundColor Red
        Write-Host ""
        Write-Host "   PROBLEMA: get-pending-code.php nao encontrou codigo pendente" -ForegroundColor Red
        Write-Host ""
        Write-Host "   POSSIVEIS CAUSAS:" -ForegroundColor Yellow
        Write-Host "   1. Codigo nao foi salvo em _pending_logins (ver PASSO 3)" -ForegroundColor White
        Write-Host "   2. Codigo expirou (mais de 15 minutos)" -ForegroundColor White
        Write-Host "   3. IP diferente (se get-pending-code.php busca por IP)" -ForegroundColor White
    }
} catch {
    Write-Host "   ERRO: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "   PROBLEMA: Nao foi possivel chamar get-pending-code.php" -ForegroundColor Red
    Write-Host "   VERIFICAR: Servidor Render esta online?" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== PASSO 5: Testando auto_login.php ===" -ForegroundColor Cyan
Write-Host "   LOGICA: App chama isso com o codigo recebido. Deve retornar credenciais no formato JSON" -ForegroundColor Gray
try {
    $autoLoginUrl = "$serverUrl/auto_login.php?code=$Code"
    Write-Host "   URL que app chama: $autoLoginUrl" -ForegroundColor Gray
    
    $autoLoginResponse = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -TimeoutSec 10
    
    Write-Host "   Resposta JSON:" -ForegroundColor Yellow
    $autoLoginResponse | ConvertTo-Json -Depth 5 | Write-Host
    Write-Host ""
    
    # Verificar novo formato
    if ($autoLoginResponse.status -eq "success") {
        Write-Host "   OK Status: success" -ForegroundColor Green
        
        if ($autoLoginResponse.autologin) {
            $autologin = $autoLoginResponse.autologin
            Write-Host "   OK Objeto autologin presente" -ForegroundColor Green
            
            $temUsername = $autologin.PSObject.Properties.Name -contains "username"
            $temPassword = $autologin.PSObject.Properties.Name -contains "password"
            $temApiUrl = $autologin.PSObject.Properties.Name -contains "api_url"
            $temExpiryDate = $autologin.PSObject.Properties.Name -contains "expiryDate"
            
            Write-Host "   Campo 'autologin.username': $(if ($temUsername) { "OK - $($autologin.username)" } else { "FALTANDO!" })" -ForegroundColor $(if ($temUsername) { "Green" } else { "Red" })
            Write-Host "   Campo 'autologin.password': $(if ($temPassword) { "OK - $($autologin.password)" } else { "FALTANDO!" })" -ForegroundColor $(if ($temPassword) { "Green" } else { "Red" })
            Write-Host "   Campo 'autologin.api_url': $(if ($temApiUrl) { "OK - $($autologin.api_url)" } else { "FALTANDO!" })" -ForegroundColor $(if ($temApiUrl) { "Green" } else { "Red" })
            Write-Host "   Campo 'autologin.expiryDate': $(if ($temExpiryDate) { "OK - $($autologin.expiryDate)" } else { "FALTANDO!" })" -ForegroundColor $(if ($temExpiryDate) { "Green" } else { "Red" })
            
            if ($temUsername -and $temPassword -and $temApiUrl -and $temExpiryDate) {
                Write-Host ""
                Write-Host "   OK FORMATO CORRETO! Todos os campos presentes." -ForegroundColor Green
            } else {
                Write-Host ""
                Write-Host "   ERRO: FALTAM CAMPOS OBRIGATORIOS!" -ForegroundColor Red
            }
        } else {
            Write-Host "   ERRO: Objeto 'autologin' FALTANDO!" -ForegroundColor Red
            Write-Host ""
            Write-Host "   PROBLEMA: auto_login.php nao esta retornando objeto autologin" -ForegroundColor Red
            Write-Host ""
            Write-Host "   CORRECAO NO auto_login.php:" -ForegroundColor Yellow
            Write-Host "   echo json_encode([" -ForegroundColor White
            Write-Host "       'status' => 'success'," -ForegroundColor White
            Write-Host "       'autologin' => [" -ForegroundColor White
            Write-Host "           'username' => `$user['username']," -ForegroundColor White
            Write-Host "           'password' => `$user['password']," -ForegroundColor White
            Write-Host "           'api_url' => `$user['apiUrl']," -ForegroundColor White
            Write-Host "           'expires_in' => 21600," -ForegroundColor White
            Write-Host "           'expiryDate' => `$expiryDate" -ForegroundColor White
            Write-Host "       ]" -ForegroundColor White
            Write-Host "   ]);" -ForegroundColor White
        }
    } elseif ($autoLoginResponse.status -eq "expired") {
        Write-Host "   ERRO: Usuario EXPIRADO!" -ForegroundColor Red
        Write-Host "      Mensagem: $($autoLoginResponse.message)" -ForegroundColor Red
        Write-Host ""
        Write-Host "   PROBLEMA: Usuario expirado - nao pode fazer login" -ForegroundColor Red
    } else {
        Write-Host "   ERRO: Status diferente de 'success'" -ForegroundColor Red
        Write-Host "      Status: $($autoLoginResponse.status)" -ForegroundColor Red
        Write-Host "      Mensagem: $($autoLoginResponse.mensagem)" -ForegroundColor Red
    }
} catch {
    Write-Host "   ERRO: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "   PROBLEMA: Nao foi possivel chamar auto_login.php" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== VERIFICACAO DO APP ANDROID ===" -ForegroundColor Cyan
Write-Host "   LOGICA: App deve processar o JSON e fazer login automatico" -ForegroundColor Gray
Write-Host ""
Write-Host "   O app DEVE fazer o seguinte:" -ForegroundColor Yellow
Write-Host "   1. Chamar get-pending-code.php quando abre (sem usuario logado)" -ForegroundColor White
Write-Host "   2. Se receber codigo, chamar auto_login.php?code=CODIGO" -ForegroundColor White
Write-Host "   3. Verificar se status == 'success'" -ForegroundColor White
Write-Host "   4. Extrair autologin.username, autologin.password, autologin.api_url" -ForegroundColor White
Write-Host "   5. Chamar UserManager.login(username, password)" -ForegroundColor White
Write-Host "   6. Salvar credenciais no SettingsRepo" -ForegroundColor White
Write-Host "   7. Criar sessao no JSONBin" -ForegroundColor White
Write-Host "   8. Navegar para 'home'" -ForegroundColor White
Write-Host ""
Write-Host "   VERIFICAR NO CODIGO DO APP (HomeNav.kt):" -ForegroundColor Yellow
Write-Host "   - Linha ~97: Verifica status == 'success'?" -ForegroundColor White
Write-Host "   - Linha ~100: Extrai autologin com optJSONObject('autologin')?" -ForegroundColor White
Write-Host "   - Linha ~102-104: Extrai username, password, api_url do autologin?" -ForegroundColor White
Write-Host "   - Linha ~110: Extrai expiryDate do autologin?" -ForegroundColor White
Write-Host "   - Linha ~138: Chama UserManager.login(user, pass)?" -ForegroundColor White
Write-Host "   - Linha ~176: Define shouldNavigateToHome = true?" -ForegroundColor White
Write-Host ""
Write-Host "   PARA VER LOGS DO APP:" -ForegroundColor Cyan
Write-Host "   adb logcat | grep -E 'HomeNav|auto_login|get-pending'" -ForegroundColor Gray
Write-Host ""

Write-Host "=== RESUMO FINAL ===" -ForegroundColor Cyan
Write-Host ""
$erros = @()

if (-not $found) {
    $erros += "Codigo nao salvo em _pending_logins"
}

if ($pendingResponse.status -ne "ok") {
    $erros += "get-pending-code.php nao retornou codigo"
}

if ($autoLoginResponse.status -ne "success") {
    $erros += "auto_login.php retornou status != 'success'"
}

if ($erros.Count -eq 0) {
    Write-Host "OK SERVIDOR FUNCIONANDO CORRETAMENTE!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Se o login automatico NAO funciona, o problema esta no APP:" -ForegroundColor Yellow
    Write-Host "1. App nao esta chamando get-pending-code.php?" -ForegroundColor White
    Write-Host "2. App nao esta processando o novo formato JSON?" -ForegroundColor White
    Write-Host "3. App nao esta fazendo UserManager.login()?" -ForegroundColor White
    Write-Host "4. App nao esta navegando para home?" -ForegroundColor White
    Write-Host ""
    Write-Host "SOLUCAO: Verificar logs do app com adb logcat" -ForegroundColor Cyan
} else {
    Write-Host "ERROS ENCONTRADOS NO SERVIDOR:" -ForegroundColor Red
    foreach ($erro in $erros) {
        Write-Host "   - $erro" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "CORRIGIR ERROS NO SERVIDOR ANTES DE TESTAR NO APP" -ForegroundColor Yellow
}

Write-Host ""


# ============================================================================
# Script Profissional de Diagnostico - Login Automatico MaxiPTV
# ============================================================================
# Este script identifica EXATAMENTE onde esta o erro no fluxo de login automatico
# Autor: Sistema de Diagnostico MaxiPTV
# ============================================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$Code
)

$serverUrl = "https://maxiptv-update-1.onrender.com"
$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$jsonbinKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{"X-Master-Key" = $jsonbinKey}

$erros = @()
$sucessos = @()

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "     DIAGNOSTICO PROFISSIONAL - LOGIN AUTOMATICO MAXIPTV" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Codigo: $Code" -ForegroundColor Yellow
Write-Host ""

# ============================================================================
# EXPLICACAO DA LOGICA DO FLUXO
# ============================================================================

Write-Host "=== COMO FUNCIONA O LOGIN AUTOMATICO ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "FLUXO ESPERADO:" -ForegroundColor Yellow
Write-Host "1. Cliente acessa: $serverUrl/dl/$Code" -ForegroundColor White
Write-Host "2. dl.php valida codigo e salva em _pending_logins" -ForegroundColor White
Write-Host "3. dl.php redireciona para APK no GitHub" -ForegroundColor White
Write-Host "4. Cliente baixa e instala o APK" -ForegroundColor White
Write-Host "5. App abre e chama get-pending-code.php" -ForegroundColor White
Write-Host "6. get-pending-code.php retorna codigo pendente" -ForegroundColor White
Write-Host "7. App chama auto_login.php?code=$Code" -ForegroundColor White
Write-Host "8. auto_login.php retorna JSON com credenciais" -ForegroundColor White
Write-Host "9. App processa JSON e faz login automatico" -ForegroundColor White
Write-Host "10. App navega para home" -ForegroundColor White
Write-Host ""

# ============================================================================
# ETAPA 1: VERIFICAR CODIGO NO JSONBIN
# ============================================================================

Write-Host "=== ETAPA 1: Verificando Codigo no JSONBin ===" -ForegroundColor Cyan
Write-Host "LOGICA: Codigo deve existir e estar valido (menos de 6 horas)" -ForegroundColor Gray
Write-Host ""

try {
    $jsonbin = Invoke-RestMethod -Uri "$jsonbinUrl/latest" -Headers $headers -Method Get
    
    if ($jsonbin.record.$Code) {
        $codeData = $jsonbin.record.$Code
        Write-Host "[OK] Codigo encontrado no JSONBin" -ForegroundColor Green
        Write-Host "     Username: $($codeData.username)" -ForegroundColor White
        Write-Host "     Expiry Date: $($codeData.expiryDate)" -ForegroundColor White
        
        # Verificar validade
        if ($codeData.createdAt) {
            $createdAt = [DateTimeOffset]::FromUnixTimeMilliseconds($codeData.createdAt).DateTime
            $sixHoursLater = $createdAt.AddHours(6)
            $now = Get-Date
            
            if ($now -gt $sixHoursLater) {
                Write-Host "[ERRO] Codigo EXPIRADO (mais de 6 horas)" -ForegroundColor Red
                Write-Host "       Criado em: $createdAt" -ForegroundColor Red
                Write-Host "       Expira em: $sixHoursLater" -ForegroundColor Red
                Write-Host ""
                Write-Host "SOLUCAO: Gerar novo codigo no painel admin" -ForegroundColor Yellow
                $erros += "Codigo expirado"
            } else {
                $hoursLeft = ($sixHoursLater - $now).TotalHours
                Write-Host "[OK] Codigo valido" -ForegroundColor Green
                Write-Host "     Horas restantes: $([math]::Round($hoursLeft, 2))" -ForegroundColor White
                $sucessos += "Codigo valido no JSONBin"
            }
        }
    } else {
        Write-Host "[ERRO] Codigo NAO encontrado no JSONBin" -ForegroundColor Red
        Write-Host ""
        Write-Host "SOLUCAO: Gerar codigo no painel admin" -ForegroundColor Yellow
        $erros += "Codigo nao existe"
        exit 1
    }
} catch {
    Write-Host "[ERRO] Erro ao acessar JSONBin: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ============================================================================
# ETAPA 2: TESTAR dl.php
# ============================================================================

Write-Host "=== ETAPA 2: Testando dl.php ===" -ForegroundColor Cyan
Write-Host "LOGICA: dl.php deve validar codigo, salvar em _pending_logins e redirecionar" -ForegroundColor Gray
Write-Host ""

try {
    $dlUrl = "$serverUrl/dl/$Code"
    Write-Host "URL: $dlUrl" -ForegroundColor Gray
    
    try {
        $dlResponse = Invoke-WebRequest -Uri $dlUrl -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($statusCode -eq 302) {
            $redirectUrl = $_.Exception.Response.Headers.Location
            Write-Host "[OK] dl.php redirecionou (302)" -ForegroundColor Green
            Write-Host "     Para: $redirectUrl" -ForegroundColor White
            $sucessos += "dl.php redirecionou corretamente"
            
            if ($redirectUrl -match "maxiptv-release\.apk") {
                Write-Host "[OK] URL do APK correta" -ForegroundColor Green
            } else {
                Write-Host "[AVISO] URL do APK pode estar incorreta" -ForegroundColor Yellow
            }
        } elseif ($statusCode -eq 404) {
            Write-Host "[ERRO] dl.php retornou 404" -ForegroundColor Red
            Write-Host ""
            Write-Host "PROBLEMA: Codigo invalido ou expirado no servidor" -ForegroundColor Red
            Write-Host ""
            Write-Host "VERIFICAR:" -ForegroundColor Yellow
            Write-Host "  1. Codigo existe no JSONBin? (ver ETAPA 1)" -ForegroundColor White
            Write-Host "  2. Codigo nao expirou? (menos de 6 horas)" -ForegroundColor White
            Write-Host "  3. Usuario nao expirou? (expiryDate valida)" -ForegroundColor White
            $erros += "dl.php retornou 404"
        } else {
            Write-Host "[ERRO] dl.php retornou status: $statusCode" -ForegroundColor Red
            $erros += "dl.php retornou $statusCode"
        }
    }
} catch {
    Write-Host "[ERRO] Erro ao testar dl.php: $($_.Exception.Message)" -ForegroundColor Red
    $erros += "Erro ao testar dl.php"
}

Write-Host ""

# ============================================================================
# ETAPA 3: VERIFICAR _pending_logins
# ============================================================================

Write-Host "=== ETAPA 3: Verificando _pending_logins ===" -ForegroundColor Cyan
Write-Host "LOGICA: dl.php DEVE salvar codigo aqui para app buscar depois" -ForegroundColor Gray
Write-Host ""

Start-Sleep -Seconds 2

try {
    $jsonbin = Invoke-RestMethod -Uri "$jsonbinUrl/latest" -Headers $headers -Method Get
    
    if ($jsonbin.record._pending_logins) {
        $pending = $jsonbin.record._pending_logins
        $found = $false
        
        $pending.PSObject.Properties | ForEach-Object {
            $pendingData = $_.Value
            if ($pendingData.code -eq $Code) {
                $found = $true
                Write-Host "[OK] Codigo $Code encontrado em _pending_logins" -ForegroundColor Green
                Write-Host "     Username: $($pendingData.username)" -ForegroundColor White
                Write-Host "     Timestamp: $($pendingData.timestamp)" -ForegroundColor White
                $sucessos += "Codigo salvo em _pending_logins"
            }
        }
        
        if (-not $found) {
            Write-Host "[ERRO] Codigo $Code NAO encontrado em _pending_logins" -ForegroundColor Red
            Write-Host ""
            Write-Host "PROBLEMA: dl.php NAO salvou o codigo pendente" -ForegroundColor Red
            Write-Host ""
            Write-Host "CAUSAS POSSIVEIS:" -ForegroundColor Yellow
            Write-Host "  1. dl.php nao esta executando o codigo que salva em _pending_logins" -ForegroundColor White
            Write-Host "  2. Erro ao salvar no JSONBin (timeout, etc)" -ForegroundColor White
            Write-Host "  3. Usuario expirado (dl.php bloqueia se expirado)" -ForegroundColor White
            Write-Host ""
            Write-Host "CORRECAO:" -ForegroundColor Yellow
            Write-Host "  Verificar logs do Render para ver erros do PHP" -ForegroundColor White
            Write-Host "  Verificar se dl.php esta salvando corretamente" -ForegroundColor White
            $erros += "Codigo nao salvo em _pending_logins"
        }
    } else {
        Write-Host "[ERRO] _pending_logins NAO existe" -ForegroundColor Red
        Write-Host ""
        Write-Host "PROBLEMA: dl.php NAO esta salvando codigos pendentes" -ForegroundColor Red
        $erros += "_pending_logins nao existe"
    }
} catch {
    Write-Host "[ERRO] Erro ao verificar: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# ============================================================================
# ETAPA 4: TESTAR get-pending-code.php
# ============================================================================

Write-Host "=== ETAPA 4: Testando get-pending-code.php ===" -ForegroundColor Cyan
Write-Host "LOGICA: App chama isso quando abre. Deve retornar codigo pendente" -ForegroundColor Gray
Write-Host ""

try {
    $pendingUrl = "$serverUrl/get-pending-code.php"
    Write-Host "URL: $pendingUrl" -ForegroundColor Gray
    
    $pendingResponse = Invoke-RestMethod -Uri $pendingUrl -Method Get -TimeoutSec 10
    
    Write-Host "Resposta:" -ForegroundColor Yellow
    $pendingResponse | ConvertTo-Json -Depth 5 | Write-Host
    Write-Host ""
    
    if ($pendingResponse.status -eq "ok") {
        Write-Host "[OK] Status: ok" -ForegroundColor Green
        Write-Host "[OK] Codigo retornado: $($pendingResponse.code)" -ForegroundColor Green
        
        if ($pendingResponse.code -eq $Code) {
            Write-Host "[OK] Codigo correto" -ForegroundColor Green
            $sucessos += "get-pending-code.php retornou codigo correto"
        } else {
            Write-Host "[AVISO] Codigo retornado diferente" -ForegroundColor Yellow
        }
    } else {
        Write-Host "[ERRO] Status diferente de 'ok': $($pendingResponse.status)" -ForegroundColor Red
        Write-Host ""
        Write-Host "PROBLEMA: get-pending-code.php nao encontrou codigo pendente" -ForegroundColor Red
        Write-Host ""
        Write-Host "CAUSAS POSSIVEIS:" -ForegroundColor Yellow
        Write-Host "  1. Codigo nao foi salvo em _pending_logins (ver ETAPA 3)" -ForegroundColor White
        Write-Host "  2. Codigo expirou (mais de 15 minutos)" -ForegroundColor White
        Write-Host "  3. IP diferente (se busca por IP)" -ForegroundColor White
        $erros += "get-pending-code.php nao retornou codigo"
    }
} catch {
    Write-Host "[ERRO] Erro ao chamar: $($_.Exception.Message)" -ForegroundColor Red
    $erros += "Erro ao testar get-pending-code.php"
}

Write-Host ""

# ============================================================================
# ETAPA 5: TESTAR auto_login.php
# ============================================================================

Write-Host "=== ETAPA 5: Testando auto_login.php ===" -ForegroundColor Cyan
Write-Host "LOGICA: App chama isso com codigo. Deve retornar JSON correto" -ForegroundColor Gray
Write-Host ""

try {
    $autoLoginUrl = "$serverUrl/auto_login.php?code=$Code"
    Write-Host "URL: $autoLoginUrl" -ForegroundColor Gray
    
    $autoLoginResponse = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -TimeoutSec 10
    
    Write-Host "Resposta JSON:" -ForegroundColor Yellow
    $autoLoginResponse | ConvertTo-Json -Depth 5 | Write-Host
    Write-Host ""
    
    # Verificar formato novo
    if ($autoLoginResponse.status -eq "success") {
        Write-Host "[OK] Status: success" -ForegroundColor Green
        
        if ($autoLoginResponse.autologin) {
            $autologin = $autoLoginResponse.autologin
            Write-Host "[OK] Objeto 'autologin' presente" -ForegroundColor Green
            
            $camposOk = 0
            $camposFaltando = @()
            
            # Verificar cada campo
            $campos = @("username", "password", "api_url", "expires_in", "expiryDate")
            foreach ($campo in $campos) {
                if ($autologin.PSObject.Properties.Name -contains $campo -and $autologin.$campo) {
                    Write-Host "[OK] Campo 'autologin.$campo': $($autologin.$campo)" -ForegroundColor Green
                    $camposOk++
                } else {
                    Write-Host "[ERRO] Campo 'autologin.$campo' FALTANDO ou VAZIO" -ForegroundColor Red
                    $camposFaltando += $campo
                }
            }
            
            if ($camposOk -eq 5) {
                Write-Host ""
                Write-Host "[OK] FORMATO CORRETO! Todos os campos presentes." -ForegroundColor Green
                Write-Host "     O app DEVE conseguir fazer login automatico!" -ForegroundColor Green
                $sucessos += "auto_login.php formato correto"
            } else {
                Write-Host ""
                Write-Host "[ERRO] FORMATO INCORRETO! Faltam campos." -ForegroundColor Red
                Write-Host "       Campos faltando: $($camposFaltando -join ', ')" -ForegroundColor Red
                Write-Host ""
                Write-Host "FORMATO ESPERADO:" -ForegroundColor Cyan
                Write-Host '{' -ForegroundColor White
                Write-Host '  "status": "success",' -ForegroundColor White
                Write-Host '  "autologin": {' -ForegroundColor White
                Write-Host '    "username": "...",' -ForegroundColor White
                Write-Host '    "password": "...",' -ForegroundColor White
                Write-Host '    "api_url": "...",' -ForegroundColor White
                Write-Host '    "expires_in": 21600,' -ForegroundColor White
                Write-Host '    "expiryDate": "DD/MM/YYYY"' -ForegroundColor White
                Write-Host '  }' -ForegroundColor White
                Write-Host '}' -ForegroundColor White
                Write-Host ""
                Write-Host "CORRECAO:" -ForegroundColor Yellow
                Write-Host "  Verificar auto_login.php no servidor Render" -ForegroundColor White
                Write-Host "  Verificar se foi deployado corretamente" -ForegroundColor White
                $erros += "auto_login.php formato incorreto"
            }
        } else {
            Write-Host "[ERRO] Objeto 'autologin' FALTANDO" -ForegroundColor Red
            $erros += "autologin faltando"
        }
    } elseif ($autoLoginResponse.status -eq "expired") {
        Write-Host "[ERRO] Status: expired - Usuario expirado" -ForegroundColor Red
        $erros += "Usuario expirado"
    } else {
        Write-Host "[ERRO] Status diferente de 'success': $($autoLoginResponse.status)" -ForegroundColor Red
        $erros += "auto_login.php status incorreto"
    }
} catch {
    Write-Host "[ERRO] Erro ao chamar: $($_.Exception.Message)" -ForegroundColor Red
    $erros += "Erro ao testar auto_login.php"
}

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "=== RESUMO FINAL ===" -ForegroundColor Cyan
Write-Host ""

Write-Host "Sucessos: $($sucessos.Count)" -ForegroundColor Green
foreach ($s in $sucessos) {
    Write-Host "  [OK] $s" -ForegroundColor Green
}

Write-Host ""
Write-Host "Erros: $($erros.Count)" -ForegroundColor Red
foreach ($e in $erros) {
    Write-Host "  [ERRO] $e" -ForegroundColor Red
}

Write-Host ""

if ($erros.Count -eq 0) {
    Write-Host "[OK] SERVIDOR FUNCIONANDO CORRETAMENTE!" -ForegroundColor Green
    Write-Host ""
    Write-Host "SE O LOGIN AUTOMATICO NAO FUNCIONA, O PROBLEMA ESTA NO APP:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  1. App nao esta chamando get-pending-code.php quando abre?" -ForegroundColor White
    Write-Host "  2. App nao esta processando o novo formato JSON?" -ForegroundColor White
    Write-Host "     (deve ler status == 'success' e autologin.{username,password,api_url})" -ForegroundColor Gray
    Write-Host "  3. App nao esta fazendo UserManager.login()?" -ForegroundColor White
    Write-Host "  4. App nao esta navegando para home?" -ForegroundColor White
    Write-Host ""
    Write-Host "SOLUCAO:" -ForegroundColor Cyan
    Write-Host "  Verificar logs do app: adb logcat | grep -E 'HomeNav|auto_login|get-pending'" -ForegroundColor White
    Write-Host "  Verificar se app foi compilado com v1.0.130 ou superior" -ForegroundColor White
} else {
    Write-Host "[ERRO] PROBLEMAS ENCONTRADOS NO SERVIDOR" -ForegroundColor Red
    Write-Host ""
    Write-Host "ACAO NECESSARIA:" -ForegroundColor Yellow
    Write-Host "  Corrigir erros no servidor antes de testar no app" -ForegroundColor White
    Write-Host "  Verificar logs do Render" -ForegroundColor White
}

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan


#!/usr/bin/env pwsh
# Script para diagnosticar login automático

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTICO LOGIN AUTOMATICO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configurações
$baseUrl = "https://maxiptv-update-1.onrender.com"
$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$jsonbinKey = "`$2a`$10`$wgdgGkG6KKqtQ8DfXJKKGO3LQFnHGcGGQNGQNGQNGQNGQNGQN"

Write-Host "[1/7] Verificando se o servidor Render está online..." -ForegroundColor Yellow
try {
    $test = Invoke-WebRequest -Uri "$baseUrl/dl.php" -Method GET -TimeoutSec 10 -UseBasicParsing
    Write-Host "✅ Servidor Render está online (HTTP $($test.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "❌ Servidor Render não está respondendo: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Verifique se o serviço está ativo no Render.com" -ForegroundColor Yellow
    Write-Host "   Continuando com outras verificações..." -ForegroundColor Cyan
}
Write-Host ""

Write-Host "[2/7] Verificando logs de debug no JSONBin..." -ForegroundColor Yellow
try {
    $logHeaders = @{
        "X-Master-Key" = $jsonbinKey
    }
    $logResponse = Invoke-RestMethod -Uri "$baseUrl/debug-login.php" -Method GET -Headers $logHeaders -TimeoutSec 10
    
    if ($logResponse -is [string]) {
        $logHtml = $logResponse
        Write-Host "📄 Logs HTML recebidos (${logHtml.Length} caracteres)" -ForegroundColor Cyan
        
        # Tentar extrair informações dos logs
        if ($logHtml -match "Total de logs: (\d+)") {
            $totalLogs = $matches[1]
            Write-Host "   Total de logs encontrados: $totalLogs" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️ Não foi possível extrair total de logs" -ForegroundColor Yellow
        }
        
        # Verificar se tem logs recentes
        if ($logHtml -match "nenhum log encontrado" -or $logHtml -match "Nenhum log" -or $logHtml -match "0 logs") {
            Write-Host "   ⚠️ NENHUM LOG ENCONTRADO - O app pode não estar chamando os endpoints!" -ForegroundColor Red
        } else {
            Write-Host "   ✅ Logs encontrados no JSONBin" -ForegroundColor Green
        }
        
        # Verificar últimos logs
        Write-Host "   Últimos 5 logs:" -ForegroundColor Cyan
        $lines = $logHtml -split "`n"
        $logCount = 0
        foreach ($line in $lines) {
            if ($line -match "timestamp|hora|log|erro|success|auto_login|get-pending-code" -and $logCount -lt 5) {
                $cleanLine = $line -replace '<[^>]+>', '' -replace '\s+', ' '
                if ($cleanLine.Trim().Length -gt 0) {
                    Write-Host "      - $($cleanLine.Trim().Substring(0, [Math]::Min(100, $cleanLine.Trim().Length)))" -ForegroundColor Gray
                    $logCount++
                }
            }
        }
    } else {
        Write-Host "   ℹ️ Resposta não é HTML: $($logResponse.GetType().Name)" -ForegroundColor Yellow
        $logResponse | ConvertTo-Json -Depth 3 | Write-Host
    }
} catch {
    Write-Host "❌ Erro ao buscar logs: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "[3/7] Verificando estrutura do JSONBin diretamente..." -ForegroundColor Yellow
$headers = @{
    "X-Master-Key" = $jsonbinKey
}
try {
    $response = Invoke-RestMethod -Uri $jsonbinUrl -Method GET -Headers $headers -TimeoutSec 10
} catch {
    Write-Host "❌ Erro ao acessar JSONBin: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Stack trace: $($_.ScriptStackTrace)" -ForegroundColor Gray
    $response = $null
}

if ($response -and $response.record) {
    $record = $response.record
    Write-Host "✅ JSONBin acessível" -ForegroundColor Green
    
    # Verificar estrutura
    $keys = $record.PSObject.Properties.Name
    Write-Host "   Chaves encontradas: $($keys -join ', ')" -ForegroundColor Cyan
    
    # Verificar _login_logs
    if ($record._login_logs) {
        $logs = $record._login_logs
        if ($logs -is [Array]) {
            Write-Host "   ✅ _login_logs encontrado: $($logs.Count) logs" -ForegroundColor Green
            
            # Mostrar últimos 3 logs
            Write-Host "   Últimos 3 logs:" -ForegroundColor Cyan
            $lastLogs = $logs | Select-Object -Last 3
            foreach ($log in $lastLogs) {
                $timestamp = if ($log.timestamp) { $log.timestamp } else { "sem timestamp" }
                $message = if ($log.message) { $log.message } else { "sem mensagem" }
                Write-Host "      [$timestamp] $message" -ForegroundColor Gray
            }
        } else {
            Write-Host "   ⚠️ _login_logs não é um array" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ❌ _login_logs NÃO ENCONTRADO - Logs podem estar sendo apagados!" -ForegroundColor Red
    }
    
    # Verificar _pending_logins
    if ($record._pending_logins) {
        $pending = $record._pending_logins
        Write-Host "   ✅ _pending_logins encontrado" -ForegroundColor Green
        
        if ($pending -is [PSCustomObject] -or $pending -is [Hashtable]) {
            $pendingKeys = $pending.PSObject.Properties.Name
            Write-Host "   Códigos pendentes ativos: $($pendingKeys.Count)" -ForegroundColor Cyan
            foreach ($key in $pendingKeys) {
                $codeData = $pending.$key
                $code = if ($codeData.code) { $codeData.code } else { $key }
                $ip = if ($codeData.ip) { $codeData.ip } else { "sem IP" }
                $timestamp = if ($codeData.timestamp) { $codeData.timestamp } else { "sem timestamp" }
                $used = if ($codeData.used) { "USADO" } else { "ATIVO" }
                Write-Host "      - Código: $code | IP: $ip | Status: $used | Timestamp: $timestamp" -ForegroundColor Gray
            }
        } else {
            Write-Host "   ⚠️ _pending_logins não é um objeto" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ℹ️ _pending_logins não encontrado (normal se não houver código pendente)" -ForegroundColor Yellow
    }
    
    # Verificar códigos de cliente
    $clientCodes = $keys | Where-Object { $_ -match '^[A-Za-z0-9]{3,10}$' }
    if ($clientCodes) {
        Write-Host "   ✅ Códigos de cliente encontrados: $($clientCodes.Count)" -ForegroundColor Green
        Write-Host "   Códigos: $($clientCodes -join ', ')" -ForegroundColor Cyan
    } else {
        Write-Host "   ⚠️ Nenhum código de cliente encontrado" -ForegroundColor Yellow
    }
    
    # Verificar sessions
    if ($record.sessions) {
        Write-Host "   ✅ sessions encontrado" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️ sessions não encontrado" -ForegroundColor Yellow
    }
    
    # Verificar users
    if ($record.users) {
        $users = $record.users
        if ($users -is [Array]) {
            Write-Host "   ✅ users encontrado: $($users.Count) usuários" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️ users não é um array" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ⚠️ users não encontrado" -ForegroundColor Yellow
    }
}
if (-not $response -or -not $response.record) {
    Write-Host "❌ Record não encontrado na resposta" -ForegroundColor Red
}
Write-Host ""

Write-Host "[4/7] Testando get-pending-code.php..." -ForegroundColor Yellow
try {
    $pendingResponse = Invoke-RestMethod -Uri "$baseUrl/get-pending-code.php" -Method GET -TimeoutSec 10
    
    if ($pendingResponse.status -eq "ok") {
        Write-Host "✅ get-pending-code.php respondeu com sucesso" -ForegroundColor Green
        $code = $pendingResponse.code
        if ($code -and $code -ne "") {
            Write-Host "   Código pendente encontrado: $code" -ForegroundColor Cyan
            Write-Host "   ✅ Há um código pendente para login automático!" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️ Nenhum código pendente encontrado" -ForegroundColor Yellow
            Write-Host "   Isso é normal se o app já foi aberto ou se não houve download recente" -ForegroundColor Gray
        }
    } else {
        Write-Host "   Status: $($pendingResponse.status)" -ForegroundColor Yellow
        if ($pendingResponse.mensagem) {
            Write-Host "   Mensagem: $($pendingResponse.mensagem)" -ForegroundColor Gray
        }
    }
} catch {
    Write-Host "❌ Erro ao testar get-pending-code.php: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "   Resposta: $responseBody" -ForegroundColor Gray
    }
}
Write-Host ""

Write-Host "[5/7] Testando auto_login.php com código de exemplo..." -ForegroundColor Yellow
try {
    # Primeiro, tentar buscar um código pendente
    $pendingCheck = Invoke-RestMethod -Uri "$baseUrl/get-pending-code.php" -Method GET -TimeoutSec 10 -ErrorAction SilentlyContinue
    $testCode = if ($pendingCheck.status -eq "ok" -and $pendingCheck.code) { $pendingCheck.code } else { "1078" }
    
    Write-Host "   Testando com código: $testCode" -ForegroundColor Cyan
    
    $autoLoginResponse = Invoke-RestMethod -Uri "$baseUrl/auto_login.php?code=$testCode" -Method GET -TimeoutSec 10
    
    if ($autoLoginResponse.status -eq "success") {
        Write-Host "✅ auto_login.php retornou sucesso!" -ForegroundColor Green
        
        if ($autoLoginResponse.autologin) {
            $autologin = $autoLoginResponse.autologin
            Write-Host "   Dados de autologin:" -ForegroundColor Cyan
            Write-Host "      - Username: $($autologin.username)" -ForegroundColor Gray
            $passwordDisplay = if ($autologin.password) { '***' } else { 'VAZIO' }
            Write-Host "      - Password: $passwordDisplay" -ForegroundColor Gray
            Write-Host "      - API URL: $($autologin.api_url)" -ForegroundColor Gray
            Write-Host "      - Expiry Date: $($autologin.expiryDate)" -ForegroundColor Gray
            Write-Host "      - Expires In: $($autologin.expires_in) segundos" -ForegroundColor Gray
            
            # Validar campos obrigatórios
            $missing = @()
            if (-not $autologin.username -or $autologin.username -eq "") { $missing += "username" }
            if (-not $autologin.password -or $autologin.password -eq "") { $missing += "password" }
            if (-not $autologin.api_url -or $autologin.api_url -eq "") { $missing += "api_url" }
            
            if ($missing.Count -gt 0) {
                Write-Host "   ❌ CAMPOS FALTANDO: $($missing -join ', ')" -ForegroundColor Red
                Write-Host "   Isso pode causar falha no login automático!" -ForegroundColor Yellow
            } else {
                Write-Host "   ✅ Todos os campos obrigatórios estão presentes" -ForegroundColor Green
            }
        } else {
            Write-Host "   ❌ Objeto 'autologin' não encontrado na resposta!" -ForegroundColor Red
            Write-Host "   Resposta completa:" -ForegroundColor Yellow
            $autoLoginResponse | ConvertTo-Json -Depth 3 | Write-Host
        }
    } else {
        Write-Host "   Status: $($autoLoginResponse.status)" -ForegroundColor Yellow
        if ($autoLoginResponse.mensagem) {
            Write-Host "   Mensagem: $($autoLoginResponse.mensagem)" -ForegroundColor Gray
        }
        Write-Host "   Resposta completa:" -ForegroundColor Yellow
        $autoLoginResponse | ConvertTo-Json -Depth 3 | Write-Host
    }
} catch {
    Write-Host "❌ Erro ao testar auto_login.php: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "   Resposta de erro: $responseBody" -ForegroundColor Gray
        } catch {
            Write-Host "   Não foi possível ler resposta de erro" -ForegroundColor Gray
        }
    }
}
Write-Host ""

Write-Host "[6/7] Verificando se o app está chamando os endpoints..." -ForegroundColor Yellow
Write-Host "   Analisando logs do JSONBin para verificar chamadas do app..." -ForegroundColor Cyan

try {
    $headers = @{
        "X-Master-Key" = $jsonbinKey
    }
    $response = Invoke-RestMethod -Uri $jsonbinUrl -Method GET -Headers $headers -TimeoutSec 10
    
    if ($response.record._login_logs) {
        $logs = $response.record._login_logs
        
        # Buscar logs relacionados ao app
        $appLogs = $logs | Where-Object { 
            $_.message -match "HomeNav|auto_login|get-pending-code|Login automatico|Tentando login" 
        } | Select-Object -Last 10
        
        if ($appLogs) {
            Write-Host "   ✅ Encontrados $($appLogs.Count) logs relacionados ao app" -ForegroundColor Green
            Write-Host "   Últimos logs do app:" -ForegroundColor Cyan
            foreach ($log in $appLogs) {
                $timestamp = if ($log.timestamp) { $log.timestamp } else { "sem timestamp" }
                $message = $log.message
                Write-Host "      [$timestamp] $message" -ForegroundColor Gray
            }
        } else {
            Write-Host "   ❌ NENHUM LOG DO APP ENCONTRADO!" -ForegroundColor Red
            Write-Host "   Isso indica que o app NÃO está chamando os endpoints!" -ForegroundColor Yellow
            Write-Host "   Possíveis causas:" -ForegroundColor Yellow
            Write-Host "      1. O app não está verificando código pendente no HomeNav" -ForegroundColor Gray
            Write-Host "      2. Erro de rede bloqueando as chamadas" -ForegroundColor Gray
            Write-Host "      3. O app está travando antes de fazer as chamadas" -ForegroundColor Gray
            Write-Host "      4. Os logs estão sendo apagados antes de serem visualizados" -ForegroundColor Gray
        }
        
        # Verificar se há erros recentes
        $errorLogs = $logs | Where-Object { 
            $_.level -eq "erro" -or $_.message -match "erro|error|failed|falhou|❌" 
        } | Select-Object -Last 5
        
        if ($errorLogs) {
            Write-Host "   ⚠️ Erros encontrados nos logs:" -ForegroundColor Yellow
            foreach ($log in $errorLogs) {
                $timestamp = if ($log.timestamp) { $log.timestamp } else { "sem timestamp" }
                Write-Host "      [$timestamp] $($log.message)" -ForegroundColor Red
            }
        }
    } else {
        Write-Host "   ❌ _login_logs não encontrado no JSONBin" -ForegroundColor Red
        Write-Host "   Os logs podem estar sendo apagados pelo app!" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Erro ao verificar logs: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "[7/7] Resumo e recomendações..." -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DIAGNOSTICO COMPLETO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "🔍 VERIFIQUE:" -ForegroundColor Yellow
Write-Host "   1. Se os logs aparecem e desaparecem, pode ser que o app esteja apagando os logs" -ForegroundColor White
Write-Host "   2. Verifique HomeNav.kt se está chamando get-pending-code.php no LaunchedEffect" -ForegroundColor White
Write-Host "   3. Verifique SessionManager.kt se está preservando _login_logs ao salvar" -ForegroundColor White
Write-Host "   4. Teste o app novamente e monitore os logs em tempo real em:" -ForegroundColor White
Write-Host "      $baseUrl/debug-login.php" -ForegroundColor Cyan
Write-Host ""
Write-Host "📱 PARA TESTAR:" -ForegroundColor Yellow
Write-Host "   1. Acesse $baseUrl/dl.php?code=SEU_CODIGO" -ForegroundColor White
Write-Host "   2. Isso criará um código pendente" -ForegroundColor White
Write-Host "   3. Abra o app MaxiPTV" -ForegroundColor White
Write-Host "   4. O app deve chamar get-pending-code.php e depois auto_login.php" -ForegroundColor White
Write-Host "   5. Monitore os logs em $baseUrl/debug-login.php" -ForegroundColor White
Write-Host ""


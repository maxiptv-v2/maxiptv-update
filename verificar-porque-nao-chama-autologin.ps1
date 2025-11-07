# Script para Verificar Por Que auto_login.php Não Está Sendo Chamado
Write-Host "🔍 VERIFICANDO POR QUE auto_login.php NÃO ESTÁ SENDO CHAMADO" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{
    "X-Master-Key" = $apiKey
}
$renderBase = "https://maxiptv-update-1.onrender.com"

# 1. Verificar código pendente
Write-Host "1️⃣ VERIFICANDO get-pending-code.php" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

$pendingUrl = "$renderBase/get-pending-code.php"
Write-Host "🔗 URL: $pendingUrl" -ForegroundColor White

try {
    $response = Invoke-RestMethod -Uri $pendingUrl -Method Get -ErrorAction Stop
    
    Write-Host "✅ Resposta recebida!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Resposta completa:" -ForegroundColor Yellow
    $response | ConvertTo-Json -Depth 10 | Write-Host
    Write-Host ""
    
    if ($response.status -eq 'ok') {
        Write-Host "✅ Status: OK" -ForegroundColor Green
        if ($response.code) {
            Write-Host "✅ Código pendente: $($response.code)" -ForegroundColor Green
            $testCode = $response.code
            
            Write-Host ""
            Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
            Write-Host ""
            
            # 2. Testar auto_login.php com o código encontrado
            Write-Host "2️⃣ TESTANDO auto_login.php COM CÓDIGO $testCode" -ForegroundColor Cyan
            Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray
            
            $autoLoginUrl = "$renderBase/auto_login.php?code=$testCode"
            Write-Host "🔗 URL: $autoLoginUrl" -ForegroundColor White
            
            try {
                $autoLoginResponse = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -ErrorAction Stop
                
                Write-Host "✅ Resposta recebida!" -ForegroundColor Green
                Write-Host ""
                Write-Host "Resposta completa:" -ForegroundColor Yellow
                $autoLoginResponse | ConvertTo-Json -Depth 10 | Write-Host
                Write-Host ""
                
                if ($autoLoginResponse.status -eq 'success') {
                    Write-Host "✅ Status: SUCCESS" -ForegroundColor Green
                    if ($autoLoginResponse.autologin) {
                        $autologin = $autoLoginResponse.autologin
                        Write-Host ""
                        Write-Host "Dados de autologin:" -ForegroundColor Yellow
                        Write-Host "  Username: $($autologin.username)" -ForegroundColor White
                        Write-Host "  Password: ***" -ForegroundColor White
                        Write-Host "  API URL: $($autologin.api_url)" -ForegroundColor White
                        Write-Host "  Expiry Date: $($autologin.expiryDate)" -ForegroundColor White
                        Write-Host ""
                        Write-Host "✅ TUDO ESTÁ FUNCIONANDO CORRETAMENTE NO PHP!" -ForegroundColor Green
                        Write-Host ""
                        Write-Host "⚠️  PROBLEMA: O app Android não está chamando auto_login.php" -ForegroundColor Yellow
                        Write-Host ""
                        Write-Host "🔍 POSSÍVEIS CAUSAS:" -ForegroundColor Cyan
                        Write-Host "   1. get-pending-code.php retorna status != 'ok'" -ForegroundColor White
                        Write-Host "   2. Código pendente está vazio" -ForegroundColor White
                        Write-Host "   3. Erro de rede no app Android" -ForegroundColor White
                        Write-Host "   4. LaunchedEffect não está executando" -ForegroundColor White
                        Write-Host "   5. Exceção sendo capturada silenciosamente" -ForegroundColor White
                    } else {
                        Write-Host "❌ Objeto 'autologin' não encontrado na resposta" -ForegroundColor Red
                    }
                } else {
                    Write-Host "❌ Status: $($autoLoginResponse.status)" -ForegroundColor Red
                    if ($autoLoginResponse.mensagem) {
                        Write-Host "   Mensagem: $($autoLoginResponse.mensagem)" -ForegroundColor Red
                    }
                }
                
            } catch {
                Write-Host "❌ Erro ao chamar auto_login.php: $($_.Exception.Message)" -ForegroundColor Red
                $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "N/A" }
                Write-Host "   Status Code: $statusCode" -ForegroundColor Red
            }
            
        } else {
            Write-Host "❌ Código não encontrado na resposta de get-pending-code.php" -ForegroundColor Red
            Write-Host ""
            Write-Host "⚠️  PROBLEMA: get-pending-code.php não retornou código pendente" -ForegroundColor Yellow
            Write-Host "   Isso significa que o app não consegue buscar o código para chamar auto_login.php" -ForegroundColor Yellow
        }
    } else {
        Write-Host "❌ Status: $($response.status)" -ForegroundColor Red
        if ($response.mensagem) {
            Write-Host "   Mensagem: $($response.mensagem)" -ForegroundColor Red
        }
        Write-Host ""
        Write-Host "⚠️  PROBLEMA: get-pending-code.php retornou status != 'ok'" -ForegroundColor Yellow
        Write-Host "   O app só chama auto_login.php se status == 'ok'" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "❌ Erro ao chamar get-pending-code.php: $($_.Exception.Message)" -ForegroundColor Red
    $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "N/A" }
    Write-Host "   Status Code: $statusCode" -ForegroundColor Red
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host ""

# 3. Verificar logs do JSONBin para ver o que está acontecendo
Write-Host "3️⃣ ANALISANDO LOGS DO JSONBIN" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Method Get -Headers $headers -ErrorAction Stop
    $record = $response.record
    
    if ($record._login_logs) {
        $logs = $record._login_logs
        $logCount = if ($logs -is [System.Array]) { $logs.Count } else { 0 }
        
        Write-Host "Total de logs: $logCount" -ForegroundColor White
        Write-Host ""
        
        # Filtrar logs relacionados a get-pending-code e auto_login
        $getPendingLogs = $logs | Where-Object { $_.data.endpoint -eq 'get-pending-code.php' }
        $autoLoginLogs = $logs | Where-Object { $_.data.endpoint -eq 'auto_login.php' }
        $dlLogs = $logs | Where-Object { $_.data.endpoint -eq 'dl.php' }
        
        Write-Host "Logs de dl.php: $($dlLogs.Count)" -ForegroundColor $(if ($dlLogs.Count -gt 0) { 'Green' } else { 'Yellow' })
        Write-Host "Logs de get-pending-code.php: $($getPendingLogs.Count)" -ForegroundColor $(if ($getPendingLogs.Count -gt 0) { 'Green' } else { 'Red' })
        Write-Host "Logs de auto_login.php: $($autoLoginLogs.Count)" -ForegroundColor $(if ($autoLoginLogs.Count -gt 0) { 'Green' } else { 'Red' })
        Write-Host ""
        
        if ($getPendingLogs.Count -eq 0) {
            Write-Host "❌ PROBLEMA IDENTIFICADO: get-pending-code.php NUNCA foi chamado!" -ForegroundColor Red
            Write-Host "   Isso significa que o app não está executando o LaunchedEffect" -ForegroundColor Yellow
            Write-Host "   ou está falhando antes de chamar get-pending-code.php" -ForegroundColor Yellow
        }
        
        if ($autoLoginLogs.Count -eq 0 -and $getPendingLogs.Count -gt 0) {
            Write-Host "❌ PROBLEMA IDENTIFICADO: auto_login.php NÃO foi chamado após get-pending-code.php" -ForegroundColor Red
            Write-Host "   Causa provável: get-pending-code.php não retornou status 'ok' ou código está vazio" -ForegroundColor Yellow
        }
        
        # Mostrar últimos logs relevantes
        if ($logCount -gt 0) {
            Write-Host ""
            Write-Host "Últimos 5 logs (ordem cronológica):" -ForegroundColor Yellow
            $sortedLogs = $logs | Sort-Object { [DateTime]::Parse($_.datetime) } | Select-Object -Last 5
            foreach ($log in $sortedLogs) {
                $color = switch ($log.type) {
                    'success' { 'Green' }
                    'error' { 'Red' }
                    'warning' { 'Yellow' }
                    'info' { 'Cyan' }
                    default { 'White' }
                }
                Write-Host "  [$($log.datetime)] $($log.type): $($log.message)" -ForegroundColor $color
                if ($log.data.endpoint) {
                    Write-Host "    Endpoint: $($log.data.endpoint)" -ForegroundColor Gray
                }
                if ($log.data.code) {
                    Write-Host "    Code: $($log.data.code)" -ForegroundColor Gray
                }
            }
        }
        
    } else {
        Write-Host "⚠️  Nenhuma log encontrada ainda" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "❌ Erro ao analisar logs: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "📊 CONCLUSÃO" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "💡 Para verificar logs em tempo real no Render:" -ForegroundColor Yellow
Write-Host "   https://maxiptv-update-1.onrender.com/debug-login.php" -ForegroundColor White
Write-Host ""
Write-Host "✅ Script concluído!" -ForegroundColor Green





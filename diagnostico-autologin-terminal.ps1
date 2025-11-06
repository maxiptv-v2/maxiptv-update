# Script de Diagnóstico - Autologin e Download
# Executa no terminal local para verificar todo o fluxo

param(
    [string]$Code = "1078"
)

Write-Host "🔍 DIAGNÓSTICO COMPLETO - AUTOLOGIN E DOWNLOAD" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

# Configurações
$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest"
$jsonbin_update = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$renderBase = "https://maxiptv-update-1.onrender.com"

Write-Host "📝 Código de teste: $Code" -ForegroundColor Yellow
Write-Host ""

# ==================== ETAPA 1: Verificar se código existe no JSONBin ====================
Write-Host "1️⃣ VERIFICANDO CÓDIGO NO JSONBIN" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

try {
    $headers = @{
        "X-Master-Key" = $apiKey
    }
    
    $response = Invoke-RestMethod -Uri $jsonbin_url -Method Get -Headers $headers -ErrorAction Stop
    
    $record = $response.record
    
    Write-Host "✅ JSONBin acessado com sucesso!" -ForegroundColor Green
    Write-Host "📦 Total de chaves no record: $($record.PSObject.Properties.Count)" -ForegroundColor White
    
    # Verificar se código existe
    if ($record.$Code) {
        $codeData = $record.$Code
        Write-Host "✅ Código '$Code' encontrado no JSONBin!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Dados do código:" -ForegroundColor Yellow
        Write-Host "  Username: $($codeData.username)" -ForegroundColor White
        Write-Host "  Password: ***" -ForegroundColor White
        Write-Host "  API URL: $($codeData.apiUrl)" -ForegroundColor White
        Write-Host "  Expiry Date: $($codeData.expiryDate)" -ForegroundColor White
        Write-Host "  Created At: $($codeData.createdAt)" -ForegroundColor White
    } else {
        Write-Host "❌ Código '$Code' NÃO encontrado no JSONBin!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Códigos disponíveis (primeiros 10):" -ForegroundColor Yellow
        $codes = $record.PSObject.Properties.Name | Where-Object { $_ -match '^[A-Za-z0-9]{3,10}$' }
        if ($codes) {
            Write-Host "  $($codes[0..9] -join ', ')" -ForegroundColor White
        } else {
            Write-Host "  Nenhum código encontrado!" -ForegroundColor Red
        }
    }
    
    # Verificar _pending_logins
    Write-Host ""
    if ($record._pending_logins) {
        $pendingLogins = $record._pending_logins
        $pendingCount = if ($pendingLogins -is [hashtable] -or $pendingLogins -is [PSCustomObject]) { 
            ($pendingLogins | Get-Member -MemberType NoteProperty).Count 
        } else { 
            $pendingLogins.Count 
        }
        Write-Host "📋 Códigos pendentes (_pending_logins): $pendingCount" -ForegroundColor Yellow
        
        if ($pendingCount -gt 0) {
            Write-Host "Últimos 3 códigos pendentes:" -ForegroundColor Yellow
            $pendingKeys = if ($pendingLogins -is [hashtable] -or $pendingLogins -is [PSCustomObject]) {
                ($pendingLogins | Get-Member -MemberType NoteProperty).Name
            } else {
                $pendingLogins.Keys
            }
            foreach ($key in ($pendingKeys | Select-Object -Last 3)) {
                $pending = $pendingLogins.$key
                Write-Host "  Chave: $key" -ForegroundColor White
                Write-Host "    Code: $($pending.code)" -ForegroundColor Gray
                Write-Host "    Username: $($pending.username)" -ForegroundColor Gray
                Write-Host "    Timestamp: $($pending.timestamp) ($(Get-Date -UnixTimeSeconds $pending.timestamp -Format 'yyyy-MM-dd HH:mm:ss'))" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "⚠️  _pending_logins não encontrado ou vazio" -ForegroundColor Yellow
    }
    
    # Verificar _login_logs
    Write-Host ""
    if ($record._login_logs) {
        $logs = $record._login_logs
        $logCount = $logs.Count
        Write-Host "📋 Logs de login (_login_logs): $logCount" -ForegroundColor Yellow
        
        if ($logCount -gt 0) {
            Write-Host "Últimos 5 logs:" -ForegroundColor Yellow
            foreach ($log in ($logs | Select-Object -Last 5)) {
                Write-Host "  [$($log.datetime)] $($log.type): $($log.message)" -ForegroundColor White
                if ($log.data.code) {
                    Write-Host "    Code: $($log.data.code)" -ForegroundColor Gray
                }
            }
        }
    } else {
        Write-Host "⚠️  _login_logs não encontrado ou vazio" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "❌ Erro ao acessar JSONBin: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
}

Write-Host ""
Write-Host ""

# ==================== ETAPA 2: Simular dl.php ====================
Write-Host "2️⃣ SIMULANDO dl.php (Download)" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

$dlUrl = "$renderBase/dl.php?code=$Code"
Write-Host "🔗 URL: $dlUrl" -ForegroundColor White

try {
    $response = Invoke-WebRequest -Uri $dlUrl -Method Get -MaximumRedirection 0 -ErrorAction SilentlyContinue
    
    Write-Host "❌ Erro: Não deveria seguir redirect!" -ForegroundColor Red
    
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 302 -or $_.Exception.Response.StatusCode.value__ -eq 301) {
        $location = $_.Exception.Response.Headers.Location
        Write-Host "✅ Redirect para: $location" -ForegroundColor Green
    } elseif ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "❌ Código inválido ou expirado (404)" -ForegroundColor Red
    } elseif ($_.Exception.Response.StatusCode.value__ -eq 403) {
        Write-Host "❌ Acesso negado (403)" -ForegroundColor Red
    } else {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "📡 Resposta HTTP: $statusCode" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "⏳ Aguardando 2 segundos para código pendente ser salvo..." -ForegroundColor Yellow
Start-Sleep -Seconds 2
Write-Host ""

# ==================== ETAPA 3: Verificar se código pendente foi salvo ====================
Write-Host "3️⃣ VERIFICANDO CÓDIGO PENDENTE APÓS dl.php" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Method Get -Headers $headers -ErrorAction Stop
    $record = $response.record
    
    if ($record._pending_logins) {
        $pendingLogins = $record._pending_logins
        $pendingCount = if ($pendingLogins -is [hashtable] -or $pendingLogins -is [PSCustomObject]) { 
            ($pendingLogins | Get-Member -MemberType NoteProperty).Count 
        } else { 
            $pendingLogins.Count 
        }
        Write-Host "📋 Total de códigos pendentes: $pendingCount" -ForegroundColor White
        
        # Buscar código pendente mais recente
        $mostRecent = $null
        $mostRecentKey = $null
        $mostRecentTime = 0
        
        $pendingKeys = if ($pendingLogins -is [hashtable] -or $pendingLogins -is [PSCustomObject]) {
            ($pendingLogins | Get-Member -MemberType NoteProperty).Name
        } else {
            $pendingLogins.Keys
        }
        
        foreach ($key in $pendingKeys) {
            $pending = $pendingLogins.$key
            $timestamp = $pending.timestamp
            if ($timestamp -gt $mostRecentTime) {
                $mostRecentTime = $timestamp
                $mostRecent = $pending
                $mostRecentKey = $key
            }
        }
        
        if ($mostRecent) {
            Write-Host "✅ Código pendente encontrado!" -ForegroundColor Green
            Write-Host ""
            Write-Host "  Chave: $mostRecentKey" -ForegroundColor White
            Write-Host "  Code: $($mostRecent.code)" -ForegroundColor White
            Write-Host "  Username: $($mostRecent.username)" -ForegroundColor White
            Write-Host "  Timestamp: $($mostRecent.timestamp) ($(Get-Date -UnixTimeSeconds $mostRecent.timestamp -Format 'yyyy-MM-dd HH:mm:ss'))" -ForegroundColor White
            Write-Host "  Expires At: $($mostRecent.expiresAt) ($(Get-Date -UnixTimeSeconds $mostRecent.expiresAt -Format 'yyyy-MM-dd HH:mm:ss'))" -ForegroundColor White
            Write-Host "  IP: $($mostRecent.ip)" -ForegroundColor White
            
            if ($mostRecent.code -eq $Code) {
                Write-Host ""
                Write-Host "✅ Código pendente corresponde ao código de teste!" -ForegroundColor Green
            } else {
                Write-Host ""
                Write-Host "⚠️  Código pendente ($($mostRecent.code)) não corresponde ao código de teste ($Code)" -ForegroundColor Yellow
            }
        } else {
            Write-Host "❌ Nenhum código pendente encontrado!" -ForegroundColor Red
        }
        
        # Verificar logs mais recentes
        Write-Host ""
        if ($record._login_logs) {
            $logs = $record._login_logs
            Write-Host "📋 Logs mais recentes (últimos 5):" -ForegroundColor Yellow
            foreach ($log in ($logs | Select-Object -Last 5)) {
                Write-Host "  [$($log.datetime)] $($log.type): $($log.message)" -ForegroundColor White
                if ($log.data.code) {
                    Write-Host "    Code: $($log.data.code)" -ForegroundColor Gray
                }
            }
        }
        
    } else {
        Write-Host "❌ _pending_logins não existe ou está vazio!" -ForegroundColor Red
    }
    
} catch {
    Write-Host "❌ Erro ao verificar código pendente: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host ""

# ==================== ETAPA 4: Testar get-pending-code.php ====================
Write-Host "4️⃣ TESTANDO get-pending-code.php" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

$pendingUrl = "$renderBase/get-pending-code.php"
Write-Host "🔗 URL: $pendingUrl" -ForegroundColor White

try {
    $response = Invoke-RestMethod -Uri $pendingUrl -Method Get -ErrorAction Stop
    
    Write-Host "✅ Resposta recebida!" -ForegroundColor Green
    Write-Host ""
    
    if ($response.status -eq 'ok') {
        Write-Host "✅ Status: OK" -ForegroundColor Green
        if ($response.code) {
            Write-Host "✅ Código pendente: $($response.code)" -ForegroundColor Green
            if ($response.code -eq $Code) {
                Write-Host "✅ Código corresponde ao código de teste!" -ForegroundColor Green
            } else {
                Write-Host "⚠️  Código ($($response.code)) não corresponde ao código de teste ($Code)" -ForegroundColor Yellow
            }
        } else {
            Write-Host "❌ Código não encontrado na resposta" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ Status: $($response.status)" -ForegroundColor Red
        if ($response.mensagem) {
            Write-Host "   Mensagem: $($response.mensagem)" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "Resposta completa:" -ForegroundColor Yellow
    $response | ConvertTo-Json -Depth 10 | Write-Host
    
} catch {
    Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status Code: $statusCode" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host ""

# ==================== ETAPA 5: Testar auto_login.php ====================
Write-Host "5️⃣ TESTANDO auto_login.php" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

$autoLoginUrl = "$renderBase/auto_login.php?code=$Code"
Write-Host "🔗 URL: $autoLoginUrl" -ForegroundColor White

try {
    $response = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -ErrorAction Stop
    
    Write-Host "✅ Resposta recebida!" -ForegroundColor Green
    Write-Host ""
    
    if ($response.status -eq 'success') {
        Write-Host "✅ Status: SUCCESS" -ForegroundColor Green
        if ($response.autologin) {
            $autologin = $response.autologin
            Write-Host ""
            Write-Host "Dados de autologin:" -ForegroundColor Yellow
            Write-Host "  Username: $($autologin.username)" -ForegroundColor White
            Write-Host "  Password: ***" -ForegroundColor White
            Write-Host "  API URL: $($autologin.api_url)" -ForegroundColor White
            Write-Host "  Expiry Date: $($autologin.expiryDate)" -ForegroundColor White
            Write-Host "  Expires In: $($autologin.expires_in) segundos" -ForegroundColor White
        } else {
            Write-Host "❌ Objeto 'autologin' não encontrado na resposta" -ForegroundColor Red
        }
    } else {
        Write-Host "❌ Status: $($response.status)" -ForegroundColor Red
        if ($response.mensagem) {
            Write-Host "   Mensagem: $($response.mensagem)" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "Resposta completa:" -ForegroundColor Yellow
    $response | ConvertTo-Json -Depth 10 | Write-Host
    
} catch {
    Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status Code: $statusCode" -ForegroundColor Red
        
        try {
            $errorStream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($errorStream)
            $errorBody = $reader.ReadToEnd()
            Write-Host "   Corpo do erro: $errorBody" -ForegroundColor Red
        } catch {
            # Ignorar erro ao ler corpo
        }
    }
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "✅ DIAGNÓSTICO CONCLUÍDO!" -ForegroundColor Green
Write-Host ""
Write-Host "💡 Para testar com outro código, execute:" -ForegroundColor Yellow
Write-Host "   .\diagnostico-autologin-terminal.ps1 -Code SEU_CODIGO" -ForegroundColor White
Write-Host ""


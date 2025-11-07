# Script para Verificar se Logs estão Interferindo com Usuários no JSONBin
Write-Host "🔍 VERIFICANDO INTERFERÊNCIA DE LOGS COM USUÁRIOS" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{
    "X-Master-Key" = $apiKey
}

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Method Get -Headers $headers -ErrorAction Stop
    $record = $response.record
    
    Write-Host "✅ Bin acessado!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar estrutura completa
    $keys = $record.PSObject.Properties.Name
    Write-Host "📦 Total de campos no bin: $($keys.Count)" -ForegroundColor White
    Write-Host "   Campos: $($keys -join ', ')" -ForegroundColor Gray
    Write-Host ""
    
    # Verificar usuários
    Write-Host "👥 VERIFICANDO USUÁRIOS:" -ForegroundColor Cyan
    if ($record.users) {
        $users = $record.users
        $userCount = if ($users -is [System.Array]) { $users.Count } else { 0 }
        
        Write-Host "   ✅ Campo 'users' existe" -ForegroundColor Green
        Write-Host "   📊 Total de usuários: $userCount" -ForegroundColor White
        
        if ($userCount -gt 0) {
            Write-Host ""
            Write-Host "   Lista de usuários:" -ForegroundColor Yellow
            foreach ($user in $users) {
                Write-Host "     • $($user.username)" -ForegroundColor White
                Write-Host "       ID: $($user.id)" -ForegroundColor Gray
                Write-Host "       API: $($user.apiUrl)" -ForegroundColor Gray
                Write-Host "       Expiry: $($user.expiryDate)" -ForegroundColor Gray
                Write-Host ""
            }
        } else {
            Write-Host "   ⚠️  Array de usuários está VAZIO!" -ForegroundColor Yellow
        }
        
        # Verificar estrutura dos usuários
        if ($userCount -gt 0) {
            $firstUser = $users[0]
            $requiredFields = @('id', 'username', 'password', 'apiUrl', 'expiryDate')
            $missingFields = @()
            
            foreach ($field in $requiredFields) {
                if (-not $firstUser.$field) {
                    $missingFields += $field
                }
            }
            
            if ($missingFields.Count -gt 0) {
                Write-Host "   ❌ PROBLEMA: Usuários não têm todos os campos necessários!" -ForegroundColor Red
                Write-Host "      Campos faltando: $($missingFields -join ', ')" -ForegroundColor Red
            } else {
                Write-Host "   ✅ Estrutura dos usuários está correta" -ForegroundColor Green
            }
        }
        
    } else {
        Write-Host "   ❌ Campo 'users' NÃO existe!" -ForegroundColor Red
        Write-Host "   💡 Isso é um problema - os usuários não estão sendo salvos!" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host ""
    
    # Verificar logs
    Write-Host "📋 VERIFICANDO LOGS:" -ForegroundColor Cyan
    if ($record._login_logs) {
        $logs = $record._login_logs
        $logCount = if ($logs -is [System.Array]) { $logs.Count } else { 0 }
        
        Write-Host "   ✅ Campo '_login_logs' existe" -ForegroundColor Green
        Write-Host "   📊 Total de logs: $logCount" -ForegroundColor White
        
        if ($logCount -gt 0) {
            Write-Host ""
            Write-Host "   Últimos 3 logs:" -ForegroundColor Yellow
            foreach ($log in ($logs | Select-Object -Last 3)) {
                Write-Host "     [$($log.datetime)] $($log.type): $($log.message)" -ForegroundColor White
            }
        }
        
        Write-Host ""
        Write-Host "   💡 Os logs são mostrados em: https://maxiptv-update-1.onrender.com/debug-login.php" -ForegroundColor Cyan
        
    } else {
        Write-Host "   ⚠️  Campo '_login_logs' não existe (normal se não houver logs ainda)" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host ""
    
    # Verificar sessions
    Write-Host "🔐 VERIFICANDO SESSÕES:" -ForegroundColor Cyan
    if ($record.sessions) {
        $sessions = $record.sessions
        $sessionCount = if ($sessions -is [hashtable] -or $sessions -is [PSCustomObject]) { 
            ($sessions | Get-Member -MemberType NoteProperty).Count 
        } else { 
            $sessions.Count 
        }
        
        Write-Host "   ✅ Campo 'sessions' existe" -ForegroundColor Green
        Write-Host "   📊 Total de sessões: $sessionCount" -ForegroundColor White
    } else {
        Write-Host "   ⚠️  Campo 'sessions' não existe" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host ""
    
    # Verificar códigos
    Write-Host "🔑 VERIFICANDO CÓDIGOS:" -ForegroundColor Cyan
    $codes = $keys | Where-Object { $_ -match '^[A-Za-z0-9]{3,10}$' -and $_ -ne 'sessions' -and $_ -ne 'users' }
    $codeCount = ($codes | Measure-Object).Count
    
    Write-Host "   📊 Total de códigos: $codeCount" -ForegroundColor White
    
    if ($codeCount -gt 0) {
        Write-Host "   Primeiros 5 códigos: $($codes[0..4] -join ', ')" -ForegroundColor Gray
    } else {
        Write-Host "   ⚠️  Nenhum código cadastrado ainda" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host ""
    
    # CONCLUSÃO
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "📊 RESUMO E DIAGNÓSTICO" -ForegroundColor Cyan
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host ""
    
    if ($record.users -and ($record.users | Measure-Object).Count -gt 0) {
        Write-Host "✅ Usuários: OK" -ForegroundColor Green
    } else {
        Write-Host "❌ Usuários: PROBLEMA - Campo não existe ou está vazio" -ForegroundColor Red
        Write-Host ""
        Write-Host "💡 SOLUÇÃO:" -ForegroundColor Yellow
        Write-Host "   1. O problema pode ser que quando salvamos logs, estamos fazendo PUT que pode interferir" -ForegroundColor White
        Write-Host "   2. Verifique se há código que salva dados sem preservar 'users'" -ForegroundColor White
        Write-Host "   3. Os usuários devem ser preservados quando salvamos logs" -ForegroundColor White
    }
    
    if ($record._login_logs) {
        Write-Host "✅ Logs: OK (serão mostrados em debug-login.php)" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Logs: Nenhum log ainda" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "🔗 Para ver logs no Render:" -ForegroundColor Cyan
    Write-Host "   https://maxiptv-update-1.onrender.com/debug-login.php" -ForegroundColor White
    
} catch {
    Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "✅ Script concluído!" -ForegroundColor Green





# ============================================================================
# Script de Diagnostico Completo - Login Automatico Nao Funciona
# ============================================================================

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "     DIAGNOSTICO COMPLETO - LOGIN AUTOMATICO NAO FUNCIONA" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

# Verificar logs no JSONBin
Write-Host "=== ETAPA 1: Verificando Logs no Servidor ===" -ForegroundColor Yellow
Write-Host ""

$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$headers = @{"X-Master-Key" = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'}

try {
    $response = Invoke-RestMethod -Uri $jsonbinUrl -Headers $headers -Method Get
    $logs = $response.record._login_logs
    
    if ($logs -and $logs.Count -gt 0) {
        Write-Host "Total de logs: $($logs.Count)" -ForegroundColor Green
        
        $appLogs = $logs | Where-Object { $_.message -match "get-pending|auto_login|HomeNav|LoginScreen" } | Sort-Object -Property timestamp -Descending | Select-Object -First 5
        
        if ($appLogs.Count -gt 0) {
            Write-Host ""
            Write-Host "Logs relacionados ao app:" -ForegroundColor Cyan
            $appLogs | ForEach-Object {
                $color = switch ($_.type) {
                    "success" { "Green" }
                    "error" { "Red" }
                    "warning" { "Yellow" }
                    default { "Cyan" }
                }
                Write-Host "  [$($_.datetime)] [$($_.type.ToUpper())] $($_.message)" -ForegroundColor $color
            }
        } else {
            Write-Host ""
            Write-Host "[PROBLEMA] Nenhum log do app encontrado!" -ForegroundColor Red
            Write-Host "Isso significa que o app NAO esta chamando os endpoints PHP" -ForegroundColor Red
        }
    } else {
        Write-Host "[PROBLEMA] Nenhum log encontrado no sistema" -ForegroundColor Red
    }
} catch {
    Write-Host "[ERRO] Erro ao buscar logs: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== ETAPA 2: Verificando Codigo Pendente ===" -ForegroundColor Yellow
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri $jsonbinUrl -Headers $headers -Method Get
    $pending = $response.record._pending_logins
    
    if ($pending) {
        $pendingCount = ($pending | Measure-Object).Count
        Write-Host "Total de codigos pendentes: $pendingCount" -ForegroundColor Green
        
        if ($pendingCount -gt 0) {
            Write-Host ""
            Write-Host "Codigos pendentes:" -ForegroundColor Cyan
            $pending.PSObject.Properties | ForEach-Object {
                $p = $_.Value
                Write-Host "  Codigo: $($p.code) | Username: $($p.username) | IP: $($_.Name)" -ForegroundColor White
            }
        } else {
            Write-Host "[AVISO] Nenhum codigo pendente encontrado" -ForegroundColor Yellow
            Write-Host "Isso pode significar que:" -ForegroundColor Yellow
            Write-Host "  1. O codigo pendente ja foi usado (get-pending-code.php remove apos usar)" -ForegroundColor White
            Write-Host "  2. O codigo pendente expirou (valido por 15 minutos)" -ForegroundColor White
            Write-Host "  3. dl.php nao salvou o codigo pendente" -ForegroundColor White
        }
    } else {
        Write-Host "[PROBLEMA] _pending_logins nao existe" -ForegroundColor Red
    }
} catch {
    Write-Host "[ERRO] Erro ao verificar codigos pendentes: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== ETAPA 3: Testando Endpoints Diretamente ===" -ForegroundColor Yellow
Write-Host ""

Write-Host "1. Testando get-pending-code.php..." -ForegroundColor Cyan
try {
    $pendingResponse = Invoke-RestMethod -Uri "https://maxiptv-update-1.onrender.com/get-pending-code.php" -TimeoutSec 10
    Write-Host "   [OK] Resposta: $($pendingResponse | ConvertTo-Json -Compress)" -ForegroundColor Green
} catch {
    Write-Host "   [ERRO] $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "2. Testando auto_login.php com codigo 4633..." -ForegroundColor Cyan
try {
    $autoLoginResponse = Invoke-RestMethod -Uri "https://maxiptv-update-1.onrender.com/auto_login.php?code=4633" -TimeoutSec 10
    Write-Host "   [OK] Resposta: $($autoLoginResponse | ConvertTo-Json -Compress)" -ForegroundColor Green
} catch {
    Write-Host "   [ERRO] $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "=== POSSIVEIS CAUSAS DO PROBLEMA ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. APP JA TEM USUARIO LOGADO LOCALMENTE" -ForegroundColor Yellow
Write-Host "   Se o app ja tem um usuario salvo, ele vai direto para home" -ForegroundColor White
Write-Host "   SOLUCAO: Desinstalar app completamente e instalar novamente" -ForegroundColor Green
Write-Host ""
Write-Host "2. APP NAO ESTA ABRINDO APOS INSTALACAO" -ForegroundColor Yellow
Write-Host "   O app pode nao estar abrindo automaticamente apos instalacao" -ForegroundColor White
Write-Host "   SOLUCAO: Abrir app manualmente apos instalar" -ForegroundColor Green
Write-Host ""
Write-Host "3. L launchedEffect NAO ESTA SENDO EXECUTADO" -ForegroundColor Yellow
Write-Host "   Pode haver um erro silencioso impedindo a execucao" -ForegroundColor White
Write-Host "   SOLUCAO: Verificar logs do app com: adb logcat | grep HomeNav" -ForegroundColor Green
Write-Host ""
Write-Host "4. ERRO DE CONEXAO" -ForegroundColor Yellow
Write-Host "   O app pode estar falhando ao conectar com o servidor" -ForegroundColor White
Write-Host "   SOLUCAO: Verificar conexao de internet e firewall" -ForegroundColor Green
Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan


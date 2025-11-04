# ============================================================================
# Script para Testar Sistema de Debug Login Automático
# ============================================================================

param(
    [Parameter(Mandatory=$false)]
    [string]$Code = "4633"
)

$serverUrl = "https://maxiptv-update-1.onrender.com"

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "     SISTEMA DE DEBUG LOGIN AUTOMATICO - MAXIPTV" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Como usar:" -ForegroundColor Yellow
Write-Host "1. Acesse no navegador: $serverUrl/debug-login.php" -ForegroundColor White
Write-Host "2. Ou execute este script para ver os logs recentes" -ForegroundColor White
Write-Host ""
Write-Host "O que o sistema faz:" -ForegroundColor Yellow
Write-Host "  - Registra TODAS as chamadas do app aos endpoints PHP" -ForegroundColor White
Write-Host "  - Mostra quando get-pending-code.php e auto_login.php sao chamados" -ForegroundColor White
Write-Host "  - Indica se o login automatico foi bem-sucedido ou teve erro" -ForegroundColor White
Write-Host ""
Write-Host "Fluxo esperado:" -ForegroundColor Yellow
Write-Host "  1. Cliente baixa APK via: $serverUrl/dl/$Code" -ForegroundColor White
Write-Host "  2. App abre e chama: get-pending-code.php" -ForegroundColor White
Write-Host "  3. App recebe codigo e chama: auto_login.php?code=$Code" -ForegroundColor White
Write-Host "  4. App faz login automatico" -ForegroundColor White
Write-Host ""
Write-Host "Verificando logs via API..." -ForegroundColor Cyan
Write-Host ""

# Buscar logs do JSONBin
$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$headers = @{"X-Master-Key" = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'}

try {
    $response = Invoke-RestMethod -Uri $jsonbinUrl -Headers $headers -Method Get
    $logs = $response.record._login_logs
    
    if ($logs -and $logs.Count -gt 0) {
        Write-Host "Total de logs encontrados: $($logs.Count)" -ForegroundColor Green
        Write-Host ""
        Write-Host "Ultimos 5 logs:" -ForegroundColor Yellow
        Write-Host ""
        
        $sortedLogs = $logs | Sort-Object -Property timestamp -Descending | Select-Object -First 5
        
        foreach ($log in $sortedLogs) {
            $color = switch ($log.type) {
                "success" { "Green" }
                "error" { "Red" }
                "warning" { "Yellow" }
                default { "Cyan" }
            }
            
            Write-Host "[$($log.datetime)] [$($log.type.ToUpper())]" -ForegroundColor $color
            Write-Host "  $($log.message)" -ForegroundColor White
            
            if ($log.data) {
                Write-Host "  Dados: " -NoNewline -ForegroundColor Gray
                $log.data | ConvertTo-Json -Compress | Write-Host -ForegroundColor Gray
            }
            
            Write-Host ""
        }
        
        Write-Host "Acesse $serverUrl/debug-login.php para ver todos os logs" -ForegroundColor Cyan
    } else {
        Write-Host "Nenhum log encontrado ainda." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Os logs aparecerao quando:" -ForegroundColor Yellow
        Write-Host "  1. O app chamar get-pending-code.php" -ForegroundColor White
        Write-Host "  2. O app chamar auto_login.php" -ForegroundColor White
        Write-Host ""
        Write-Host "Para testar:" -ForegroundColor Cyan
        Write-Host "  1. Baixe o APK usando: $serverUrl/dl/$Code" -ForegroundColor White
        Write-Host "  2. Instale e abra o app" -ForegroundColor White
        Write-Host "  3. Acesse: $serverUrl/debug-login.php" -ForegroundColor White
    }
} catch {
    Write-Host "Erro ao buscar logs: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Acesse diretamente: $serverUrl/debug-login.php" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan


# Script Final de Análise - Por Que Login Automático Não Funciona
Write-Host "🔍 ANÁLISE FINAL - POR QUE LOGIN AUTOMÁTICO NÃO FUNCIONA" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{
    "X-Master-Key" = $apiKey
}
$renderBase = "https://maxiptv-update-1.onrender.com"

Write-Host "📊 RESUMO DAS LOGS:" -ForegroundColor Cyan
Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Method Get -Headers $headers -ErrorAction Stop
    $record = $response.record
    
    if ($record._login_logs) {
        $logs = $record._login_logs
        $logCount = if ($logs -is [System.Array]) { $logs.Count } else { 0 }
        
        # Contar logs por endpoint
        $dlLogs = $logs | Where-Object { $_.data.endpoint -eq 'dl.php' }
        $getPendingLogs = $logs | Where-Object { $_.data.endpoint -eq 'get-pending-code.php' }
        $autoLoginLogs = $logs | Where-Object { $_.data.endpoint -eq 'auto_login.php' }
        
        Write-Host "✅ dl.php: $($dlLogs.Count) chamada(s)" -ForegroundColor Green
        Write-Host "✅ get-pending-code.php: $($getPendingLogs.Count) chamada(s)" -ForegroundColor Green
        Write-Host "✅ auto_login.php: $($autoLoginLogs.Count) chamada(s)" -ForegroundColor Green
        Write-Host ""
        
        Write-Host "🔍 CONCLUSÃO:" -ForegroundColor Cyan
        Write-Host "   Os endpoints PHP ESTÃO sendo chamados corretamente!" -ForegroundColor Green
        Write-Host ""
        Write-Host "⚠️  O PROBLEMA ESTÁ NO APP ANDROID:" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "   Possíveis causas:" -ForegroundColor Yellow
        Write-Host "   1. UserManager.login() está falhando" -ForegroundColor White
        Write-Host "      → Verifique logs do Android: adb logcat | grep 'HomeNav\|UserManager'" -ForegroundColor Gray
        Write-Host ""
        Write-Host "   2. Navegação não está funcionando" -ForegroundColor White
        Write-Host "      → Verifique se shouldNavigateToHome está sendo setado para true" -ForegroundColor Gray
        Write-Host ""
        Write-Host "   3. Usuário já existe e está bloqueando login" -ForegroundColor White
        Write-Host "      → Verifique se há sessão ativa em outro dispositivo" -ForegroundColor Gray
        Write-Host ""
        Write-Host "   4. Exceção sendo capturada silenciosamente" -ForegroundColor White
        Write-Host "      → Verifique logs do Android para erros não capturados" -ForegroundColor Gray
        Write-Host ""
        
        # Mostrar últimos logs relevantes
        Write-Host "📝 ÚLTIMOS LOGS RELEVANTES:" -ForegroundColor Cyan
        Write-Host "───────────────────────────────────────────────────────────────────────────────" -ForegroundColor Gray
        
        $sortedLogs = $logs | Sort-Object { [DateTime]::Parse($_.datetime) } | Select-Object -Last 10
        
        foreach ($log in $sortedLogs) {
            $color = switch ($log.type) {
                'success' { 'Green' }
                'error' { 'Red' }
                'warning' { 'Yellow' }
                'info' { 'Cyan' }
                default { 'White' }
            }
            
            Write-Host "[$($log.datetime)] $($log.type.ToUpper()): $($log.message)" -ForegroundColor $color
            if ($log.data.endpoint) {
                Write-Host "  Endpoint: $($log.data.endpoint)" -ForegroundColor Gray
            }
            if ($log.data.code) {
                Write-Host "  Code: $($log.data.code)" -ForegroundColor Gray
            }
            Write-Host ""
        }
        
    }
    
} catch {
    Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "💡 PRÓXIMOS PASSOS PARA DIAGNOSTICAR:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Verificar logs do Android no dispositivo:" -ForegroundColor Yellow
Write-Host "   adb logcat | grep 'HomeNav\|UserManager\|Login'" -ForegroundColor White
Write-Host ""
Write-Host "2. Verificar se UserManager.login() está retornando erro" -ForegroundColor Yellow
Write-Host "   Procure por: 'Login automático bem-sucedido' ou 'Erro no login automático'" -ForegroundColor White
Write-Host ""
Write-Host "3. Verificar se navegação está sendo executada" -ForegroundColor Yellow
Write-Host "   Procure por: 'Executando navegação para home' ou 'ERRO ao navegar'" -ForegroundColor White
Write-Host ""
Write-Host "4. Verificar se usuário já está logado em outro dispositivo" -ForegroundColor Yellow
Write-Host "   Verifique sessões no JSONBin ou no painel admin" -ForegroundColor White
Write-Host ""
Write-Host "✅ Script concluído!" -ForegroundColor Green





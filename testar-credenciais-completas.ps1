# Script para Testar se get-pending-code.php Retorna Credenciais Completas
Write-Host "🧪 TESTANDO get-pending-code.php COM CREDENCIAIS COMPLETAS" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

$renderBase = "https://maxiptv-update-1.onrender.com"

Write-Host "1️⃣ Testando get-pending-code.php" -ForegroundColor Cyan
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
        
        # Verificar se tem todos os campos necessários
        $hasCode = $response.code -ne $null -and $response.code -ne ''
        $hasUsername = $response.username -ne $null -and $response.username -ne ''
        $hasPassword = $response.password -ne $null -and $response.password -ne ''
        $hasApiUrl = $response.api_url -ne $null -and $response.api_url -ne ''
        $hasExpiryDate = $response.expiryDate -ne $null -and $response.expiryDate -ne ''
        
        Write-Host ""
        Write-Host "📊 Verificação de campos:" -ForegroundColor Cyan
        Write-Host "   Code: $(if ($hasCode) { '✅' } else { '❌' })" -ForegroundColor $(if ($hasCode) { 'Green' } else { 'Red' })
        Write-Host "   Username: $(if ($hasUsername) { '✅' } else { '❌' })" -ForegroundColor $(if ($hasUsername) { 'Green' } else { 'Red' })
        Write-Host "   Password: $(if ($hasPassword) { '✅' } else { '❌' })" -ForegroundColor $(if ($hasPassword) { 'Green' } else { 'Red' })
        Write-Host "   API URL: $(if ($hasApiUrl) { '✅' } else { '❌' })" -ForegroundColor $(if ($hasApiUrl) { 'Green' } else { 'Red' })
        Write-Host "   Expiry Date: $(if ($hasExpiryDate) { '✅' } else { '❌' })" -ForegroundColor $(if ($hasExpiryDate) { 'Green' } else { 'Red' })
        Write-Host ""
        
        if ($hasCode -and $hasUsername -and $hasPassword -and $hasApiUrl) {
            Write-Host "✅ TODAS AS CREDENCIAIS ESTÃO PRESENTES!" -ForegroundColor Green
            Write-Host ""
            Write-Host "   Código: $($response.code)" -ForegroundColor White
            Write-Host "   Username: $($response.username)" -ForegroundColor White
            Write-Host "   Password: ***" -ForegroundColor White
            Write-Host "   API URL: $($response.api_url)" -ForegroundColor White
            Write-Host "   Expiry Date: $($response.expiryDate)" -ForegroundColor White
            Write-Host ""
            Write-Host "💡 Agora o app pode fazer login automático diretamente sem chamar auto_login.php!" -ForegroundColor Yellow
        } else {
            Write-Host "⚠️  ALGUNS CAMPOS ESTÃO FALTANDO!" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "   Isso significa que:" -ForegroundColor Yellow
            Write-Host "   1. O código pendente não foi salvo com senha/API na primeira vez" -ForegroundColor White
            Write-Host "   2. Ou precisa baixar o APK novamente para gerar novo código pendente" -ForegroundColor White
            Write-Host ""
            Write-Host "   Solução: Baixar APK novamente usando dl.php para gerar novo código pendente" -ForegroundColor Yellow
        }
    } else {
        Write-Host "❌ Status: $($response.status)" -ForegroundColor Red
        if ($response.mensagem) {
            Write-Host "   Mensagem: $($response.mensagem)" -ForegroundColor Red
        }
    }
    
} catch {
    Write-Host "❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
    $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { "N/A" }
    Write-Host "   Status Code: $statusCode" -ForegroundColor Red
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "✅ Teste concluído!" -ForegroundColor Green
Write-Host ""
Write-Host "💡 Próximos passos:" -ForegroundColor Cyan
Write-Host "   1. Se campos estão faltando: baixar APK novamente para gerar novo código pendente" -ForegroundColor White
Write-Host "   2. Se todos os campos estão presentes: compilar app e testar autologin" -ForegroundColor White





#!/usr/bin/env pwsh
# Script para verificar formato de retorno do get-pending-code.php e comparar com formato esperado pelo app

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  VERIFICAÇÃO DE FORMATO AUTOLOGIN" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$url = "https://maxiptv-update-1.onrender.com/get-pending-code.php"

Write-Host "🔍 Testando get-pending-code.php..." -ForegroundColor Yellow
Write-Host "   URL: $url" -ForegroundColor Gray
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 15 -ErrorAction Stop
    
    Write-Host "✅ Resposta recebida!" -ForegroundColor Green
    Write-Host ""
    
    # Mostrar resposta completa formatada
    Write-Host "📥 RESPOSTA COMPLETA (JSON):" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Gray
    $response | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor White
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    
    # Verificar campos obrigatórios
    Write-Host "🔍 VERIFICAÇÃO DE CAMPOS OBRIGATÓRIOS:" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Gray
    
    $camposObrigatorios = @{
        "status" = "ok"
        "code" = "string não vazio"
        "username" = "string não vazio"
        "password" = "string não vazio"
        "api_url" = "string não vazio"
        "expiryDate" = "string não vazio"
    }
    
    $erros = @()
    $sucessos = @()
    
    foreach ($campo in $camposObrigatorios.Keys) {
        $valor = $response.$campo
        
        if ($null -eq $valor) {
            $erros += "❌ Campo '$campo' está AUSENTE"
        } elseif ($valor -is [string] -and [string]::IsNullOrWhiteSpace($valor)) {
            $erros += "❌ Campo '$campo' está VAZIO"
        } else {
            $displayValue = if ($campo -eq "password") { "***" } else { $valor }
            $sucessos += "✅ Campo '$campo' = $displayValue"
        }
    }
    
    # Mostrar sucessos
    foreach ($sucesso in $sucessos) {
        Write-Host $sucesso -ForegroundColor Green
    }
    
    # Mostrar erros
    if ($erros.Count -gt 0) {
        Write-Host ""
        Write-Host "⚠️ ERROS ENCONTRADOS:" -ForegroundColor Red
        foreach ($erro in $erros) {
            Write-Host "   $erro" -ForegroundColor Red
        }
    }
    
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    
    # Verificar formato esperado pelo app (baseado em HomeNav.kt)
    Write-Host "📋 FORMATO ESPERADO PELO APP (HomeNav.kt):" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host '{' -ForegroundColor White
    Write-Host '  "status": "ok",' -ForegroundColor White
    Write-Host '  "code": "XXXX",' -ForegroundColor White
    Write-Host '  "username": "usuario",' -ForegroundColor White
    Write-Host '  "password": "senha",' -ForegroundColor White
    Write-Host '  "api_url": "https://...",' -ForegroundColor White
    Write-Host '  "expiryDate": "DD/MM/YYYY"' -ForegroundColor White
    Write-Host '}' -ForegroundColor White
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    
    # Comparar formato
    Write-Host "🔍 COMPARAÇÃO COM FORMATO ESPERADO:" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Gray
    
    $statusOk = ($response.status -eq "ok")
    $temCode = ($null -ne $response.code -and $response.code -ne "")
    $temUsername = ($null -ne $response.username -and $response.username -ne "")
    $temPassword = ($null -ne $response.password -and $response.password -ne "")
    $temApiUrl = ($null -ne $response.api_url -and $response.api_url -ne "")
    $temExpiryDate = ($null -ne $response.expiryDate -and $response.expiryDate -ne "")
    
    Write-Host "   Status = 'ok': $(if ($statusOk) { '✅ SIM' } else { '❌ NÃO (recebido: ' + $response.status + ')' })" -ForegroundColor $(if ($statusOk) { 'Green' } else { 'Red' })
    Write-Host "   Tem 'code': $(if ($temCode) { '✅ SIM (' + $response.code + ')' } else { '❌ NÃO' })" -ForegroundColor $(if ($temCode) { 'Green' } else { 'Red' })
    Write-Host "   Tem 'username': $(if ($temUsername) { '✅ SIM (' + $response.username + ')' } else { '❌ NÃO' })" -ForegroundColor $(if ($temUsername) { 'Green' } else { 'Red' })
    Write-Host "   Tem 'password': $(if ($temPassword) { '✅ SIM (***)' } else { '❌ NÃO' })" -ForegroundColor $(if ($temPassword) { 'Green' } else { 'Red' })
    Write-Host "   Tem 'api_url': $(if ($temApiUrl) { '✅ SIM (' + $response.api_url + ')' } else { '❌ NÃO' })" -ForegroundColor $(if ($temApiUrl) { 'Green' } else { 'Red' })
    Write-Host "   Tem 'expiryDate': $(if ($temExpiryDate) { '✅ SIM (' + $response.expiryDate + ')' } else { '❌ NÃO' })" -ForegroundColor $(if ($temExpiryDate) { 'Green' } else { 'Red' })
    
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    
    # Verificar se formato está completo
    $formatoCompleto = $statusOk -and $temCode -and $temUsername -and $temPassword -and $temApiUrl -and $temExpiryDate
    
    if ($formatoCompleto) {
        Write-Host "✅ FORMATO CORRETO! Todas as credenciais estão presentes." -ForegroundColor Green
        Write-Host ""
        Write-Host "📱 O app deve conseguir fazer autologin com essas credenciais." -ForegroundColor Green
    } else {
        Write-Host "❌ FORMATO INCOMPLETO! Faltam campos obrigatórios." -ForegroundColor Red
        Write-Host ""
        Write-Host "⚠️ O app NÃO conseguirá fazer autologin sem todos os campos." -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # Verificar também o que o app espera fazer com essas credenciais
    Write-Host "📱 O QUE O APP FAZ COM ESSAS CREDENCIAIS:" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host "1. Recebe JSON do get-pending-code.php" -ForegroundColor White
    Write-Host "2. Verifica se status == 'ok'" -ForegroundColor White
    Write-Host "3. Extrai: username, password, api_url, expiryDate" -ForegroundColor White
    Write-Host "4. Chama UserManager.login(username, password)" -ForegroundColor White
    Write-Host "5. Configura XRepo.configure(api_url, username, password)" -ForegroundColor White
    Write-Host "6. Salva credenciais no SettingsRepo" -ForegroundColor White
    Write-Host "7. Cria sessão no JSONBin" -ForegroundColor White
    Write-Host "8. Navega para tela 'home'" -ForegroundColor White
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    
    # Sugestões de correção se houver problemas
    if (-not $formatoCompleto) {
        Write-Host "🔧 SUGESTÕES DE CORREÇÃO:" -ForegroundColor Yellow
        Write-Host "----------------------------------------" -ForegroundColor Gray
        
        if (-not $statusOk) {
            Write-Host "   - Verificar se get-pending-code.php retorna status='ok' quando encontra código" -ForegroundColor White
        }
        if (-not $temPassword) {
            Write-Host "   - Verificar se dl.php está salvando 'password' no código pendente" -ForegroundColor White
            Write-Host "   - Verificar se get-pending-code.php está extraindo 'password' do código pendente" -ForegroundColor White
        }
        if (-not $temApiUrl) {
            Write-Host "   - Verificar se dl.php está salvando 'apiUrl' no código pendente" -ForegroundColor White
            Write-Host "   - Verificar se get-pending-code.php está extraindo 'apiUrl' do código pendente" -ForegroundColor White
        }
        if (-not $temExpiryDate) {
            Write-Host "   - Verificar se dl.php está salvando 'expiryDate' no código pendente" -ForegroundColor White
            Write-Host "   - Verificar se get-pending-code.php está extraindo 'expiryDate' do código pendente" -ForegroundColor White
        }
        
        Write-Host "----------------------------------------" -ForegroundColor Gray
        Write-Host ""
    }
    
} catch {
    Write-Host "❌ ERRO ao testar get-pending-code.php:" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Possíveis causas:" -ForegroundColor Yellow
    Write-Host "   - Render ainda não atualizou o código" -ForegroundColor White
    Write-Host "   - Servidor offline ou em sleep mode" -ForegroundColor White
    Write-Host "   - Nenhum código pendente disponível" -ForegroundColor White
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""





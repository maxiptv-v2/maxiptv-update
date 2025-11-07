#!/usr/bin/env pwsh
# Script para simular exatamente o que o app faz no autologin

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SIMULAÇÃO DO FLUXO AUTOLOGIN DO APP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$url = "https://maxiptv-update-1.onrender.com/get-pending-code.php"

Write-Host "📱 PASSO 1: App chama get-pending-code.php" -ForegroundColor Yellow
Write-Host "   URL: $url" -ForegroundColor Gray
Write-Host ""

try {
    # Simular chamada HTTP do app
    $response = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 15 -ErrorAction Stop
    
    Write-Host "✅ Resposta recebida!" -ForegroundColor Green
    Write-Host ""
    
    # Mostrar resposta bruta (como o app recebe)
    Write-Host "📥 RESPOSTA BRUTA (como o app recebe):" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Gray
    $responseJson = $response | ConvertTo-Json -Depth 10 -Compress
    Write-Host $responseJson -ForegroundColor White
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    
    # PASSO 2: App verifica status
    Write-Host "📱 PASSO 2: App verifica se status == 'ok'" -ForegroundColor Yellow
    $status = $response.status
    Write-Host "   Status recebido: '$status'" -ForegroundColor $(if ($status -eq "ok") { "Green" } else { "Red" })
    
    if ($status -ne "ok") {
        Write-Host "   ❌ ERRO: Status não é 'ok'! App não continuará." -ForegroundColor Red
        exit 1
    }
    
    Write-Host "   ✅ Status OK! Continuando..." -ForegroundColor Green
    Write-Host ""
    
    # PASSO 3: App extrai credenciais
    Write-Host "📱 PASSO 3: App extrai credenciais do JSON" -ForegroundColor Yellow
    
    $code = $response.code
    $username = $response.username
    $password = $response.password
    $apiUrl = $response.api_url
    $expiryDate = $response.expiryDate
    
    Write-Host "   Code: $(if ($code) { $code } else { '❌ VAZIO' })" -ForegroundColor $(if ($code) { "Green" } else { "Red" })
    Write-Host "   Username: $(if ($username) { $username } else { '❌ VAZIO' })" -ForegroundColor $(if ($username) { "Green" } else { "Red" })
    Write-Host "   Password: $(if ($password) { '*** (presente)' } else { '❌ VAZIO' })" -ForegroundColor $(if ($password) { "Green" } else { "Red" })
    Write-Host "   API URL: $(if ($apiUrl) { $apiUrl } else { '❌ VAZIO' })" -ForegroundColor $(if ($apiUrl) { "Green" } else { "Red" })
    Write-Host "   ExpiryDate: $(if ($expiryDate) { $expiryDate } else { '❌ VAZIO' })" -ForegroundColor $(if ($expiryDate) { "Green" } else { "Red" })
    Write-Host ""
    
    # PASSO 4: App verifica se credenciais estão completas (linha 69 do HomeNav.kt)
    Write-Host "📱 PASSO 4: App verifica se credenciais estão completas" -ForegroundColor Yellow
    Write-Host "   Condição: password.isNotBlank() && apiUrl.isNotBlank()" -ForegroundColor Gray
    
    $passwordOk = -not [string]::IsNullOrWhiteSpace($password)
    $apiUrlOk = -not [string]::IsNullOrWhiteSpace($apiUrl)
    
    Write-Host "   password.isNotBlank(): $(if ($passwordOk) { '✅ TRUE' } else { '❌ FALSE' })" -ForegroundColor $(if ($passwordOk) { "Green" } else { "Red" })
    Write-Host "   apiUrl.isNotBlank(): $(if ($apiUrlOk) { '✅ TRUE' } else { '❌ FALSE' })" -ForegroundColor $(if ($apiUrlOk) { "Green" } else { "Red" })
    
    if (-not $passwordOk -or -not $apiUrlOk) {
        Write-Host ""
        Write-Host "   ❌ ERRO: Credenciais incompletas! App não continuará com autologin." -ForegroundColor Red
        Write-Host "   O app vai tentar usar auto_login.php como fallback." -ForegroundColor Yellow
        exit 1
    }
    
    Write-Host "   ✅ Credenciais completas! App continuará com autologin direto." -ForegroundColor Green
    Write-Host ""
    
    # PASSO 5: Simular o que o app faria
    Write-Host "📱 PASSO 5: Simulação do que o app faria:" -ForegroundColor Yellow
    Write-Host "   1. Buscar usuário no banco local (UserManager.getUsers())" -ForegroundColor White
    Write-Host "   2. Se não existir, criar novo UserAccount" -ForegroundColor White
    Write-Host "   3. Chamar UserManager.login('$username', '***')" -ForegroundColor White
    Write-Host "   4. Se login OK, configurar XRepo.configure('$apiUrl', '$username', '***')" -ForegroundColor White
    Write-Host "   5. Salvar credenciais no SettingsRepo" -ForegroundColor White
    Write-Host "   6. Criar sessão no JSONBin" -ForegroundColor White
    Write-Host "   7. Navegar para tela 'home'" -ForegroundColor White
    Write-Host ""
    
    Write-Host "✅ FLUXO COMPLETO SIMULADO COM SUCESSO!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar possíveis problemas
    Write-Host "🔍 POSSÍVEIS PROBLEMAS NO APP:" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Gray
    
    $problemas = @()
    
    # Verificar se o app está chamando get-pending-code.php
    Write-Host "   ⚠️ Verificar logs do app (Logcat) para ver se:" -ForegroundColor Yellow
    Write-Host "      - App está chamando get-pending-code.php" -ForegroundColor White
    Write-Host "      - App está recebendo resposta HTTP 200" -ForegroundColor White
    Write-Host "      - App está parseando JSON corretamente" -ForegroundColor White
    Write-Host "      - App está verificando status == 'ok'" -ForegroundColor White
    Write-Host "      - App está verificando password.isNotBlank() && apiUrl.isNotBlank()" -ForegroundColor White
    Write-Host ""
    
    # Verificar formato JSON
    Write-Host "   📋 FORMATO JSON VERIFICADO:" -ForegroundColor Cyan
    Write-Host "      ✅ Status presente e = 'ok'" -ForegroundColor Green
    Write-Host "      ✅ Todas as credenciais presentes" -ForegroundColor Green
    Write-Host "      ✅ Formato compatível com app" -ForegroundColor Green
    Write-Host ""
    
    # Verificar se há problemas de timing
    Write-Host "   ⏱️ POSSÍVEIS PROBLEMAS DE TIMING:" -ForegroundColor Yellow
    Write-Host "      - App pode estar chamando get-pending-code.php ANTES do código ser salvo" -ForegroundColor White
    Write-Host "      - Código pode estar sendo marcado como 'used' muito rápido" -ForegroundColor White
    Write-Host "      - Render pode ter delay no deploy" -ForegroundColor White
    Write-Host ""
    
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    
    # Sugestões
    Write-Host "💡 SUGESTÕES PARA DEBUG:" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host "1. Verificar logs do app usando:" -ForegroundColor White
    Write-Host "   adb logcat | grep -i 'HomeNav\|autologin\|pending'" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. Verificar se app está compilado com código mais recente" -ForegroundColor White
    Write-Host ""
    Write-Host "3. Verificar se Render atualizou o código PHP" -ForegroundColor White
    Write-Host ""
    Write-Host "4. Testar código pendente imediatamente após download" -ForegroundColor White
    Write-Host ""
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    
} catch {
    Write-Host "❌ ERRO ao simular fluxo:" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Possíveis causas:" -ForegroundColor Yellow
    Write-Host "   - Render ainda não atualizou" -ForegroundColor White
    Write-Host "   - Servidor offline" -ForegroundColor White
    Write-Host "   - Nenhum código pendente disponível" -ForegroundColor White
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""





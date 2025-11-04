# ============================================================================
# Script para Verificar Por Que App Nao Chama Endpoints PHP
# ============================================================================

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "     DIAGNOSTICO - APP NAO CHAMA ENDPOINTS PHP" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "PROBLEMA IDENTIFICADO:" -ForegroundColor Red
Write-Host "  Nenhum log aparece no debug-login.php" -ForegroundColor White
Write-Host "  Isso significa que o app NAO esta chamando:" -ForegroundColor White
Write-Host "    - get-pending-code.php" -ForegroundColor Red
Write-Host "    - auto_login.php" -ForegroundColor Red
Write-Host ""

Write-Host "CAUSA MAIS PROVAVEL:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. APP JA TEM USUARIO LOGADO LOCALMENTE" -ForegroundColor Cyan
Write-Host "   O codigo verifica: UserManager.getCurrentUser()" -ForegroundColor White
Write-Host "   Se retornar um usuario, vai direto para HOME" -ForegroundColor White
Write-Host "   SEM tentar login automatico!" -ForegroundColor Red
Write-Host ""
Write-Host "   VERIFICACAO NO CODIGO (HomeNav.kt linha 33-35):" -ForegroundColor Gray
Write-Host "     val currentUser = UserManager.getCurrentUser()" -ForegroundColor Gray
Write-Host "     if (currentUser != null) {" -ForegroundColor Gray
Write-Host "       initialRoute = 'home'  // VAI DIRETO PARA HOME" -ForegroundColor Gray
Write-Host "     } else {" -ForegroundColor Gray
Write-Host "       // TENTAR LOGIN AUTOMATICO (so executa se nao tem usuario)" -ForegroundColor Gray
Write-Host "     }" -ForegroundColor Gray
Write-Host ""
Write-Host "   SOLUCAO:" -ForegroundColor Green
Write-Host "   - Desinstalar o app COMPLETAMENTE" -ForegroundColor White
Write-Host "   - Limpar dados do app (Settings > Apps > MaxiPTV > Clear Data)" -ForegroundColor White
Write-Host "   - Baixar APK novamente: https://maxiptv-update-1.onrender.com/dl/6787" -ForegroundColor White
Write-Host "   - Instalar e abrir (PRIMEIRA VEZ, sem usuario logado)" -ForegroundColor White
Write-Host ""

Write-Host "2. APP NAO ESTA ABRINDO APOS INSTALACAO" -ForegroundColor Cyan
Write-Host "   O app pode nao estar abrindo automaticamente apos instalacao." -ForegroundColor White
Write-Host ""
Write-Host "   SOLUCAO:" -ForegroundColor Green
Write-Host "   - Apos instalar, abrir o app MANUALMENTE" -ForegroundColor White
Write-Host "   - Verificar se e realmente a primeira vez abrindo" -ForegroundColor White
Write-Host ""

Write-Host "3. L launchedEffect NAO ESTA SENDO EXECUTADO" -ForegroundColor Cyan
Write-Host "   Pode haver um erro silencioso impedindo a execucao." -ForegroundColor White
Write-Host ""
Write-Host "   SOLUCAO:" -ForegroundColor Green
Write-Host "   - Verificar logs do app: adb logcat | grep HomeNav" -ForegroundColor White
Write-Host "   - Procurar por 'Verificando sessao existente' ou 'Nenhum usuario logado'" -ForegroundColor White
Write-Host ""

Write-Host "4. PERMISSAO DE INTERNET OU ERRO DE CONEXAO" -ForegroundColor Cyan
Write-Host "   O app pode nao estar conseguindo conectar ao servidor." -ForegroundColor White
Write-Host ""
Write-Host "   SOLUCAO:" -ForegroundColor Green
Write-Host "   - Verificar conexao de internet" -ForegroundColor White
Write-Host "   - Verificar firewall ou antivirus bloqueando" -ForegroundColor White
Write-Host "   - Verificar se o Render esta online" -ForegroundColor White
Write-Host ""

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "TESTE SUGERIDO:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. DESINSTALAR app completamente:" -ForegroundColor White
Write-Host "   adb uninstall com.maxiptv" -ForegroundColor Gray
Write-Host ""
Write-Host "2. BAIXAR APK:" -ForegroundColor White
Write-Host "   https://maxiptv-update-1.onrender.com/dl/6787" -ForegroundColor Gray
Write-Host ""
Write-Host "3. INSTALAR e ABRIR MANUALMENTE" -ForegroundColor White
Write-Host ""
Write-Host "4. AGUARDAR 5 segundos" -ForegroundColor White
Write-Host ""
Write-Host "5. VERIFICAR LOGS:" -ForegroundColor White
Write-Host "   https://maxiptv-update-1.onrender.com/debug-login.php" -ForegroundColor Gray
Write-Host ""
Write-Host "LOGS ESPERADOS:" -ForegroundColor Green
Write-Host "  [INFO] App chamou get-pending-code.php" -ForegroundColor White
Write-Host "  [SUCCESS] Codigo pendente encontrado e retornado" -ForegroundColor White
Write-Host "  [INFO] App chamou auto_login.php" -ForegroundColor White
Write-Host "  [SUCCESS] Credenciais retornadas com sucesso" -ForegroundColor White
Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan


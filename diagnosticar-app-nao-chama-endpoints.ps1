# ============================================================================
# Script para Diagnosticar Por Que App Nao Chama Endpoints
# ============================================================================

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "     DIAGNOSTICO - APP NAO CHAMA GET-PENDING-CODE.PHP" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "PROBLEMA IDENTIFICADO:" -ForegroundColor Yellow
Write-Host "  Os logs mostram apenas dl.php, mas NAO aparecem:" -ForegroundColor White
Write-Host "    - get-pending-code.php" -ForegroundColor Red
Write-Host "    - auto_login.php" -ForegroundColor Red
Write-Host ""

Write-Host "CAUSAS POSSIVEIS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. APP JA TEM USUARIO LOGADO LOCALMENTE" -ForegroundColor Cyan
Write-Host "   Se o app ja tem um usuario salvo, ele vai direto para home" -ForegroundColor White
Write-Host "   sem tentar login automatico." -ForegroundColor White
Write-Host ""
Write-Host "   SOLUCAO:" -ForegroundColor Green
Write-Host "   - Desinstalar o app completamente" -ForegroundColor White
Write-Host "   - Baixar APK novamente: https://maxiptv-update-1.onrender.com/dl/4633" -ForegroundColor White
Write-Host "   - Instalar e abrir (primeira vez, sem usuario logado)" -ForegroundColor White
Write-Host ""

Write-Host "2. APP NAO ESTA ABRINDO APOS INSTALACAO" -ForegroundColor Cyan
Write-Host "   O app pode nao estar abrindo automaticamente apos instalacao." -ForegroundColor White
Write-Host ""
Write-Host "   SOLUCAO:" -ForegroundColor Green
Write-Host "   - Apos instalar, abrir o app MANUALMENTE" -ForegroundColor White
Write-Host "   - Verificar se o app esta realmente na primeira abertura" -ForegroundColor White
Write-Host ""

Write-Host "3. ERRO SILENCIOSO NO APP" -ForegroundColor Cyan
Write-Host "   Pode haver um erro que esta impedindo as chamadas." -ForegroundColor White
Write-Host ""
Write-Host "   SOLUCAO:" -ForegroundColor Green
Write-Host "   - Verificar logs do app com: adb logcat | grep HomeNav" -ForegroundColor White
Write-Host "   - Procurar por erros de conexao ou excecoes" -ForegroundColor White
Write-Host ""

Write-Host "4. PERMISSAO DE INTERNET" -ForegroundColor Cyan
Write-Host "   Verificar se o AndroidManifest tem permissao de internet." -ForegroundColor White
Write-Host ""
Write-Host "   SOLUCAO:" -ForegroundColor Green
Write-Host "   - Verificar AndroidManifest.xml" -ForegroundColor White
Write-Host "   - Deve ter: <uses-permission android:name=\"android.permission.INTERNET\" />" -ForegroundColor White
Write-Host ""

Write-Host "TESTE SUGERIDO:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Desinstalar o app completamente (limpar dados)" -ForegroundColor White
Write-Host "2. Baixar APK: https://maxiptv-update-1.onrender.com/dl/4633" -ForegroundColor White
Write-Host "3. Instalar o APK" -ForegroundColor White
Write-Host "4. Abrir o app MANUALMENTE (primeira vez)" -ForegroundColor White
Write-Host "5. Aguardar 5 segundos" -ForegroundColor White
Write-Host "6. Atualizar pagina de debug: https://maxiptv-update-1.onrender.com/debug-login.php" -ForegroundColor White
Write-Host ""

Write-Host "LOGS ESPERADOS:" -ForegroundColor Green
Write-Host "  [INFO] Downloader chamou dl.php" -ForegroundColor Gray
Write-Host "  [INFO] Codigo pendente salvo para login automatico" -ForegroundColor Gray
Write-Host "  [SUCCESS] Download iniciado - redirecionando para APK" -ForegroundColor Gray
Write-Host "  [INFO] App chamou get-pending-code.php  <-- DEVE APARECER" -ForegroundColor Yellow
Write-Host "  [SUCCESS] Codigo pendente encontrado e retornado" -ForegroundColor Yellow
Write-Host "  [INFO] App chamou auto_login.php  <-- DEVE APARECER" -ForegroundColor Yellow
Write-Host "  [SUCCESS] Credenciais retornadas com sucesso" -ForegroundColor Yellow
Write-Host ""

Write-Host "================================================================================" -ForegroundColor Cyan


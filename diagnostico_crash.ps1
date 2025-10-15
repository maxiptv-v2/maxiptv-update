# Diagnostico do Crash - MaxiPTV
# ==========================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Red
Write-Host " ERRO ENCONTRADO NO APP" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red
Write-Host ""

Write-Host "TIPO DE ERRO:" -ForegroundColor Yellow
Write-Host "  java.lang.IllegalArgumentException" -ForegroundColor Red
Write-Host ""

Write-Host "MENSAGEM DE ERRO:" -ForegroundColor Yellow
Write-Host "  Only VectorDrawables and rasterized asset types are supported" -ForegroundColor Red
Write-Host "  Formatos aceitos: PNG, JPG, WEBP" -ForegroundColor Red
Write-Host ""

Write-Host "LOCALIZACAO DO ERRO:" -ForegroundColor Yellow
Write-Host "  Arquivo: MainActivity.kt" -ForegroundColor Cyan
Write-Host "  Linha: 32" -ForegroundColor Cyan
Write-Host "  Metodo: onCreate" -ForegroundColor Cyan
Write-Host ""

Write-Host "CAUSA DO PROBLEMA:" -ForegroundColor Yellow
Write-Host "  - O app tentava carregar R.mipmap.ic_launcher" -ForegroundColor White
Write-Host "  - Este recurso e um Adaptive Icon no formato XML" -ForegroundColor White
Write-Host "  - Jetpack Compose nao suporta Adaptive Icons diretamente" -ForegroundColor White
Write-Host "  - Compose aceita: VectorDrawables, PNG, JPG, WEBP" -ForegroundColor White
Write-Host ""

Write-Host "CODIGO PROBLEMATICO:" -ForegroundColor Yellow
Write-Host "  Image(painter = painterResource(R.mipmap.ic_launcher))" -ForegroundColor Red
Write-Host ""

Write-Host "SOLUCAO APLICADA:" -ForegroundColor Green
Write-Host "  - Substituido Image por Icon" -ForegroundColor White
Write-Host "  - Usado Icons.Default.PlayArrow do Material Icons" -ForegroundColor White
Write-Host "  - Adicionado import necessario" -ForegroundColor White
Write-Host ""

Write-Host "CODIGO CORRIGIDO:" -ForegroundColor Green
Write-Host "  Icon(imageVector = Icons.Default.PlayArrow)" -ForegroundColor Green
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " PROXIMOS PASSOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Recompilar o app:" -ForegroundColor White
Write-Host "   .\gradlew.bat assembleDebug" -ForegroundColor Yellow
Write-Host ""
Write-Host "2. Reinstalar no emulador:" -ForegroundColor White
Write-Host "   adb install -r app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Yellow
Write-Host ""
Write-Host "3. Testar o login com credenciais:" -ForegroundColor White
Write-Host "   Usuario: max" -ForegroundColor Yellow
Write-Host "   Senha: 1h2yd90" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""










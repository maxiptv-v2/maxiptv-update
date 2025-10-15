# Script simples para abrir emulador e testar
$emulator = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " INICIANDO TESTE MaxiPTV" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Abrir emulador
Write-Host "1. Abrindo emulador..." -ForegroundColor Yellow
Start-Process -FilePath $emulator -ArgumentList "-avd", "Medium_Phone_API_36.0" -WindowStyle Normal
Write-Host "   Emulador abrindo (aguarde carregar)..." -ForegroundColor Green
Write-Host ""

Write-Host "2. Aguardando 30 segundos para o emulador iniciar..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host ""
Write-Host "3. Verificando conexao..." -ForegroundColor Yellow
& $adb devices

Write-Host ""
Write-Host "4. Instalando APK..." -ForegroundColor Yellow
& $adb uninstall com.maxiptv 2>$null
& $adb install app\build\outputs\apk\debug\app-debug.apk

Write-Host ""
Write-Host "5. Abrindo app..." -ForegroundColor Yellow
& $adb shell am start -n com.maxiptv/.MainActivity

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host " TESTE NO EMULADOR:" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "1. Faca login (max / 1h2yd90)" -ForegroundColor White
Write-Host "2. Va em FILMES (VOD)" -ForegroundColor White
Write-Host "3. Clique em um filme" -ForegroundColor White
Write-Host "4. Clique em 'Assistir'" -ForegroundColor White
Write-Host "5. Veja se o player abre e reproduz!" -ForegroundColor White
Write-Host ""
Write-Host "Pressione ENTER para monitorar logs..." -ForegroundColor Yellow
Read-Host

Write-Host ""
Write-Host "LOGS EM TEMPO REAL:" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
& $adb logcat -v time | Select-String "maxiptv|ExoPlayer|Player|FATAL|Exception" -CaseSensitive:$false









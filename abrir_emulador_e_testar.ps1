# Script para abrir emulador e testar MaxiPTV
# ========================================

$emulatorPath = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk = "app\build\outputs\apk\debug\app-debug.apk"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " ABRIR EMULADOR E TESTAR MaxiPTV" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Listar emuladores disponiveis
Write-Host "1. Listando emuladores disponiveis..." -ForegroundColor Yellow
$avds = & $emulatorPath -list-avds

if ($avds) {
    Write-Host "   Emuladores encontrados:" -ForegroundColor Green
    $avds | ForEach-Object { Write-Host "   - $_" -ForegroundColor White }
    Write-Host ""
    
    # Pegar o primeiro emulador
    $emulatorName = $avds[0]
    Write-Host "2. Abrindo emulador: $emulatorName" -ForegroundColor Yellow
    Write-Host "   Aguarde... (pode levar 1-2 minutos)" -ForegroundColor Gray
    
    # Abrir emulador em background
    Start-Process -FilePath $emulatorPath -ArgumentList "-avd", $emulatorName -WindowStyle Normal
    
    Write-Host "   Emulador iniciando..." -ForegroundColor Green
    Write-Host ""
    
    # Aguardar emulador estar pronto
    Write-Host "3. Aguardando emulador ficar pronto..." -ForegroundColor Yellow
    $timeout = 120  # 2 minutos
    $elapsed = 0
    $ready = $false
    
    while ($elapsed -lt $timeout -and -not $ready) {
        Start-Sleep -Seconds 5
        $elapsed += 5
        
        $bootComplete = & $adb shell getprop sys.boot_completed 2>$null
        if ($bootComplete -match "1") {
            $ready = $true
            Write-Host "   Emulador pronto!" -ForegroundColor Green
        } else {
            Write-Host "   Aguardando... ($elapsed segundos)" -ForegroundColor Gray
        }
    }
    
    if ($ready) {
        Write-Host ""
        
        # Desinstalar versao anterior
        Write-Host "4. Desinstalando versao anterior..." -ForegroundColor Yellow
        & $adb uninstall com.maxiptv 2>$null
        Write-Host "   OK" -ForegroundColor Green
        Write-Host ""
        
        # Instalar novo APK
        Write-Host "5. Instalando APK corrigido..." -ForegroundColor Yellow
        if (Test-Path $apk) {
            $installResult = & $adb install $apk 2>&1
            
            if ($installResult -match "Success") {
                Write-Host "   APK instalado com sucesso!" -ForegroundColor Green
                Write-Host ""
                
                # Limpar logs
                Write-Host "6. Limpando logs..." -ForegroundColor Yellow
                & $adb logcat -c
                Write-Host "   OK" -ForegroundColor Green
                Write-Host ""
                
                # Abrir app
                Write-Host "7. Abrindo MaxiPTV..." -ForegroundColor Yellow
                & $adb shell am start -n com.maxiptv/.MainActivity
                Start-Sleep -Seconds 2
                Write-Host "   App aberto!" -ForegroundColor Green
                Write-Host ""
                
                Write-Host "========================================" -ForegroundColor Yellow
                Write-Host " PRONTO PARA TESTAR!" -ForegroundColor Yellow
                Write-Host "========================================" -ForegroundColor Yellow
                Write-Host ""
                Write-Host "INSTRUCOES DE TESTE:" -ForegroundColor Cyan
                Write-Host "1. Faca login no app" -ForegroundColor White
                Write-Host "   - Usuario: max" -ForegroundColor Gray
                Write-Host "   - Senha: 1h2yd90" -ForegroundColor Gray
                Write-Host ""
                Write-Host "2. Va ate a secao de FILMES (VOD)" -ForegroundColor White
                Write-Host ""
                Write-Host "3. Escolha um filme e clique nele" -ForegroundColor White
                Write-Host ""
                Write-Host "4. Clique no botao 'Assistir'" -ForegroundColor White
                Write-Host ""
                Write-Host "5. O player deve abrir e reproduzir o filme!" -ForegroundColor White
                Write-Host ""
                Write-Host "========================================" -ForegroundColor Green
                Write-Host "Pressione ENTER para ver os logs em tempo real" -ForegroundColor Yellow
                Write-Host "ou CTRL+C para sair" -ForegroundColor Yellow
                Write-Host "========================================" -ForegroundColor Green
                Read-Host
                
                Write-Host ""
                Write-Host "MONITORANDO LOGS..." -ForegroundColor Green
                Write-Host ""
                
                # Monitorar logs
                & $adb logcat -v time | Select-String "maxiptv|ExoPlayer|MediaPlayer|VideoView|PlayerActivity|FATAL|AndroidRuntime" -CaseSensitive:$false
                
            } else {
                Write-Host "   ERRO ao instalar APK!" -ForegroundColor Red
                Write-Host $installResult
            }
        } else {
            Write-Host "   ERRO: APK nao encontrado!" -ForegroundColor Red
        }
    } else {
        Write-Host ""
        Write-Host "TIMEOUT: Emulador nao ficou pronto em $timeout segundos" -ForegroundColor Red
        Write-Host "Tente novamente ou abra o emulador manualmente" -ForegroundColor Yellow
    }
    
} else {
    Write-Host "   NENHUM EMULADOR ENCONTRADO!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Para criar um emulador:" -ForegroundColor Yellow
    Write-Host "1. Abra Android Studio" -ForegroundColor White
    Write-Host "2. Va em Tools > Device Manager" -ForegroundColor White
    Write-Host "3. Clique em 'Create Device'" -ForegroundColor White
    Write-Host "4. Escolha um dispositivo (ex: Pixel 5)" -ForegroundColor White
    Write-Host "5. Escolha uma API (ex: API 30)" -ForegroundColor White
    Write-Host "6. Clique em Finish" -ForegroundColor White
    Write-Host ""
}









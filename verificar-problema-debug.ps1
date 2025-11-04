# ============================================================================
# Script para Verificar Problema no Debug Login
# ============================================================================

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "     VERIFICANDO PROBLEMA NO DEBUG LOGIN" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host ""

# Verificar logs no JSONBin
$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$headers = @{"X-Master-Key" = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'}

Write-Host "1. Verificando logs no JSONBin..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri $jsonbinUrl -Headers $headers -Method Get
    $logs = $response.record._login_logs
    
    if ($logs -and $logs.Count -gt 0) {
        Write-Host "   [OK] Total de logs: $($logs.Count)" -ForegroundColor Green
        Write-Host ""
        Write-Host "   Ultimos 5 logs:" -ForegroundColor Cyan
        $logs | Sort-Object -Property timestamp -Descending | Select-Object -First 5 | ForEach-Object {
            $color = switch ($_.type) {
                "success" { "Green" }
                "error" { "Red" }
                "warning" { "Yellow" }
                default { "Cyan" }
            }
            Write-Host "   [$($_.datetime)] [$($_.type.ToUpper())] $($_.message)" -ForegroundColor $color
        }
    } else {
        Write-Host "   [ERRO] Nenhum log encontrado no JSONBin" -ForegroundColor Red
        Write-Host ""
        Write-Host "   Isso significa que:" -ForegroundColor Yellow
        Write-Host "   - O app ainda nao chamou os endpoints" -ForegroundColor White
        Write-Host "   - OU o sistema de logging nao esta funcionando" -ForegroundColor White
    }
} catch {
    Write-Host "   [ERRO] Erro ao buscar logs: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "2. Testando debug-login.php diretamente..." -ForegroundColor Yellow
try {
    $debugResponse = Invoke-WebRequest -Uri "https://maxiptv-update-1.onrender.com/debug-login.php" -TimeoutSec 10
    Write-Host "   [OK] Pagina carregou (Status: $($debugResponse.StatusCode))" -ForegroundColor Green
    
    # Verificar se a página contém "Nenhum log encontrado"
    if ($debugResponse.Content -match "Nenhum log encontrado") {
        Write-Host "   [AVISO] Pagina mostra 'Nenhum log encontrado'" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "   POSSIVEIS PROBLEMAS:" -ForegroundColor Red
        Write-Host "   1. debug-login.php nao esta buscando logs corretamente" -ForegroundColor White
        Write-Host "   2. Cache do navegador" -ForegroundColor White
        Write-Host "   3. Problema na estrutura JSON" -ForegroundColor White
    } elseif ($debugResponse.Content -match "Total de logs") {
        Write-Host "   [OK] Pagina mostra logs" -ForegroundColor Green
    }
} catch {
    Write-Host "   [ERRO] Erro ao acessar pagina: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. Criando log de teste..." -ForegroundColor Yellow
try {
    # Simular chamada ao auto_login.php para criar um log
    $testResponse = Invoke-RestMethod -Uri "https://maxiptv-update-1.onrender.com/auto_login.php?code=4633" -TimeoutSec 10
    Write-Host "   [OK] Log de teste criado" -ForegroundColor Green
    Write-Host "   Aguarde 2 segundos..." -ForegroundColor Gray
    Start-Sleep -Seconds 2
} catch {
    Write-Host "   [ERRO] Erro ao criar log de teste: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host "SOLUCAO:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Abra o navegador em modo privado/incognito" -ForegroundColor White
Write-Host "2. Acesse: https://maxiptv-update-1.onrender.com/debug-login.php" -ForegroundColor White
Write-Host "3. Ou pressione Ctrl+F5 para atualizar sem cache" -ForegroundColor White
Write-Host ""
Write-Host "Se ainda nao aparecer nada:" -ForegroundColor Yellow
Write-Host "4. Baixe o APK: https://maxiptv-update-1.onrender.com/dl/4633" -ForegroundColor White
Write-Host "5. Instale e abra o app" -ForegroundColor White
Write-Host "6. Volte na pagina de debug e atualize" -ForegroundColor White
Write-Host ""
Write-Host "================================================================================" -ForegroundColor Cyan


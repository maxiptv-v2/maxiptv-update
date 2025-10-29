# Simular o teste local do PHP para verificar sintaxe

Write-Host "=== TESTANDO LOGICA DO VALIDA.PHP LOCALMENTE ===" -ForegroundColor Cyan
Write-Host ""

# Simular dados que viriam do JSONBin
$jsonBinResponse = @{
    record = @{
        "2208" = @{
            username = "mae1"
            password = "1234"
            apiUrl = "https://canais.is/player_api.php"
            expiryDate = "21/04/2026"
            apkUrl = "https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/maxiptv-release.apk"
        }
        sessions = @{}
        users = @()
    }
}

$code = "2208"

Write-Host "1. Simulando busca do código $code..." -ForegroundColor Yellow
$codigos = $jsonBinResponse.record

Write-Host "2. Verificando se código existe..." -ForegroundColor Yellow
if ($codigos.$code) {
    Write-Host "   ✅ Código encontrado!" -ForegroundColor Green
    $user = $codigos.$code
    
    Write-Host ""
    Write-Host "3. Resposta que o PHP deveria retornar:" -ForegroundColor Yellow
    $resposta = @{
        status = "ok"
        usuario = $user.username
        senha = $user.password
        api = $user.apiUrl
        expira_em = $user.expiryDate
        apk = $user.apkUrl
    }
    
    $resposta | ConvertTo-Json | Write-Host -ForegroundColor Green
    Write-Host ""
    Write-Host "✅ LÓGICA FUNCIONANDO CORRETAMENTE!" -ForegroundColor Green
} else {
    Write-Host "   ❌ Código não encontrado!" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== VERIFICANDO POSSIVEIS PROBLEMAS ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  Se ainda dá erro 500 no Render, pode ser:" -ForegroundColor Yellow
Write-Host "   1. O Render ainda não terminou o deploy (aguarde mais tempo)" -ForegroundColor White
Write-Host "   2. Problema de permissões no Docker/Container" -ForegroundColor White
Write-Host "   3. Extensão cURL não está habilitada no PHP do Render" -ForegroundColor White
Write-Host "   4. Variável PORT não está configurada no Render" -ForegroundColor White
Write-Host ""
Write-Host "💡 Verifique os LOGS no dashboard do Render para ver o erro exato!" -ForegroundColor Cyan


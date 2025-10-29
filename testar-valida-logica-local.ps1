# Testar a logica do valida.php localmente simulando

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

Write-Host "=== SIMULANDO LOGICA DO VALIDA.PHP ===" -ForegroundColor Cyan
Write-Host ""

$code = "2208"

Write-Host "1. Buscando JSONBin..." -ForegroundColor Yellow
$r = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers

Write-Host "2. Verificando estrutura record..." -ForegroundColor Yellow
$codigos = $r.record
$keys = $codigos.PSObject.Properties.Name
Write-Host "   Chaves encontradas: $($keys -join ', ')" -ForegroundColor White

Write-Host ""
Write-Host "3. Buscando codigo $code..." -ForegroundColor Yellow
if ($codigos.$code) {
    Write-Host "   ✅ Codigo encontrado!" -ForegroundColor Green
    $user = $codigos.$code
    Write-Host ""
    Write-Host "   Dados do codigo:" -ForegroundColor Cyan
    Write-Host "     username: $($user.username)" -ForegroundColor White
    Write-Host "     password: $($user.password)" -ForegroundColor White
    Write-Host "     apiUrl: $($user.apiUrl)" -ForegroundColor White
    Write-Host "     expiryDate: $($user.expiryDate)" -ForegroundColor White
    Write-Host "     apkUrl: $($user.apkUrl)" -ForegroundColor White
    
    Write-Host ""
    Write-Host "4. Simulando resposta que o PHP deveria retornar:" -ForegroundColor Yellow
    $respostaEsperada = @{
        status = "ok"
        usuario = $user.username
        senha = $user.password
        api = $user.apiUrl
        expira_em = $user.expiryDate
        apk = $user.apkUrl
    }
    $respostaEsperada | ConvertTo-Json
} else {
    Write-Host "   ❌ Codigo nao encontrado!" -ForegroundColor Red
    $codigos4digitos = $keys | Where-Object { $_ -match '^\d{4}$' }
    Write-Host "   Codigos disponiveis: $($codigos4digitos -join ', ')" -ForegroundColor Yellow
}


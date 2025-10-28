# Criar código de teste manualmente no JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

Write-Host "Criando codigo de teste..." -ForegroundColor Cyan

# JSON completo
$jsonBody = @"
{"simpleCodes":{"2273":{"usuario":"casa2","senha":"1234","api":"https://canais.is/player_api.php","apk":"https://maxiptv-update.onrender.com/download.php?code=2273","expira_em":"31/12/2030","ativo":true,"usado":false,"usado_em":null,"usado_device":null}},"users":[{"id":"f63f6c32-c720-48a5-8f79-d25aa79fb4b1","username":"casa1","password":"1234","apiUrl":"https://canais.is/player_api.php","expiryDate":"01/11/2025"},{"id":"f7c434c7-b2fb-4a8f-bc18-87b477f48e16","username":"casa2","password":"1234","apiUrl":"https://canais.is/player_api.php","expiryDate":"26/11/2025"}],"sessions":{"casa1":{"username":"casa1","deviceId":"7de17d59-bcc6-483f-a6ed-4530be439787","deviceName":"Allwinner 8K618-T","loginTime":1761597004894,"lastHeartbeat":1761602621742}}}
"@

try {
    $updateHeaders = @{
        "X-Master-Key" = $apiKey
        "Content-Type" = "application/json"
    }
    
    Invoke-RestMethod -Uri $jsonbin_url -Headers $updateHeaders -Method Put -Body $jsonBody | Out-Null
    
    Write-Host "SUCESSO! Codigo 2273 criado!" -ForegroundColor Green
    
} catch {
    Write-Host "Erro:" -ForegroundColor Red
    Write-Host $_.Exception.Message
}


# Verificar estrutura atual do JSONBin

$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

$headers = @{
    "X-Master-Key" = $apiKey
}

Write-Host "🔍 Consultando JSONBin..." -ForegroundColor Cyan
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers -Method Get
    $record = $response.record
    
    Write-Host "✅ Estrutura atual do JSONBin:" -ForegroundColor Green
    Write-Host ""
    
    # Mostrar todas as chaves
    $keys = $record.PSObject.Properties.Name
    Write-Host "📋 Chaves encontradas:" -ForegroundColor Yellow
    foreach ($key in $keys) {
        Write-Host "  - $key" -ForegroundColor White
    }
    Write-Host ""
    
    # Detalhes de sessions
    if ($record.sessions) {
        Write-Host "📱 Sessions: $($record.sessions.Count)" -ForegroundColor Cyan
        if ($record.sessions.PSObject.Properties.Count -gt 0) {
            $record.sessions.PSObject.Properties | ForEach-Object {
                $session = $_.Value
                Write-Host "  • $($_.Name): $($session.deviceName)" -ForegroundColor White
            }
        }
        Write-Host ""
    }
    
    # Detalhes de users
    if ($record.users) {
        $usersCount = if ($record.users -is [Array]) { $record.users.Count } else { $record.users.PSObject.Properties.Count }
        Write-Host "👥 Users: $usersCount" -ForegroundColor Cyan
        Write-Host ""
    }
    
    # Detalhes de códigos (chaves de 4 dígitos)
    $codigos = $keys | Where-Object { $_ -match '^\d{4}$' }
    if ($codigos) {
        Write-Host "🔑 Códigos encontrados: $($codigos.Count)" -ForegroundColor Cyan
        foreach ($codigo in $codigos) {
            $codeData = $record.$codigo
            Write-Host ""
            Write-Host "  Código: $codigo" -ForegroundColor Yellow
            Write-Host "    - Username: $($codeData.username)" -ForegroundColor White
            Write-Host "    - Password: $($codeData.password)" -ForegroundColor White
            Write-Host "    - API: $($codeData.apiUrl)" -ForegroundColor White
            Write-Host "    - ExpiryDate: $($codeData.expiryDate)" -ForegroundColor White
            Write-Host "    - ApkUrl: $($codeData.apkUrl)" -ForegroundColor White
        }
        Write-Host ""
    } else {
        Write-Host "🔑 Códigos: 0 (nenhum código de 4 dígitos encontrado)" -ForegroundColor Red
        Write-Host ""
    }
    
    # Mostrar JSON completo formatado
    Write-Host "📄 JSON Completo:" -ForegroundColor Cyan
    Write-Host ""
    $jsonFormatted = $record | ConvertTo-Json -Depth 10
    Write-Host $jsonFormatted -ForegroundColor Gray
}
catch {
    Write-Host "❌ Erro ao consultar JSONBin:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

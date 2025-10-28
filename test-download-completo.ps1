# Teste completo do download.php

$code = "2273"
$url = "https://maxiptv-update.onrender.com/download.php?code=$code"

Write-Host "=== TESTE COMPLETO ===" -ForegroundColor Cyan
Write-Host "URL: $url" -ForegroundColor Gray
Write-Host "Codigo: $code"
Write-Host ""

# 1. Verificar se codigo existe no JSONBin
Write-Host "1. Verificando codigo no JSONBin..." -ForegroundColor Yellow
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Headers @{"X-Master-Key" = $apiKey} -Method Get
    
    if ($response.record.simpleCodes -and $response.record.simpleCodes.$code) {
        Write-Host "  [OK] Codigo encontrado no JSONBin" -ForegroundColor Green
    } else {
        Write-Host "  [ERRO] Codigo NAO encontrado no JSONBin" -ForegroundColor Red
        Write-Host "  Criando codigo..." -ForegroundColor Yellow
        .\criar-codigo-manual.ps1
    }
} catch {
    Write-Host "  [ERRO] Nao conseguiu acessar JSONBin" -ForegroundColor Red
}

Write-Host ""

# 2. Testar servidor Render.com
Write-Host "2. Testando servidor Render.com..." -ForegroundColor Yellow

try {
    $r = Invoke-WebRequest -Uri $url -Method GET -ErrorAction Stop
    Write-Host "  Status: $($r.StatusCode)" -ForegroundColor Green
    Write-Host "  Resposta:" -ForegroundColor Cyan
    
    try {
        $json = $r.Content | ConvertFrom-Json
        Write-Host "    Usuario: $($json.usuario)" -ForegroundColor White
        Write-Host "    Senha: $($json.senha)" -ForegroundColor White
        Write-Host "    API: $($json.api)" -ForegroundColor White
        Write-Host "    Expira: $($json.expira_em)" -ForegroundColor White
        Write-Host ""
        Write-Host "  [SUCESSO] Login automatico funcionando!" -ForegroundColor Green
    } catch {
        Write-Host "    $($r.Content)" -ForegroundColor White
    }
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "  Status: $statusCode" -ForegroundColor Red
    
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    $content = $reader.ReadToEnd()
    $reader.Close()
    $stream.Close()
    
    Write-Host "  Erro: $content" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== FIM DO TESTE ===" -ForegroundColor Cyan


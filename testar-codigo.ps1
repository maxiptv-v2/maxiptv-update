# Script para testar um codigo gerado no app

param(
    [Parameter(Mandatory=$true)]
    [string]$code
)

Write-Host ""
Write-Host "=== Teste de Codigo ===" -ForegroundColor Cyan
Write-Host "Codigo: $code" -ForegroundColor Yellow
Write-Host ""

# Verificar no JSONBin
Write-Host "1. Verificando no JSONBin..." -ForegroundColor Cyan
$jsonbin_url = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{ "X-Master-Key" = $apiKey }

try {
    $response = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers -Method Get
    $record = $response.record
    
    if ($record.simpleCodes -and $record.simpleCodes.$code) {
        $codigo = $record.simpleCodes.$code
        Write-Host "  [OK] Codigo encontrado!" -ForegroundColor Green
        Write-Host "     Usuario: $($codigo.usuario)"
        Write-Host "     Ativo: $($codigo.ativo)"
        Write-Host "     Usado: $($codigo.usado)"
        Write-Host ""
    } else {
        Write-Host "  [ERRO] Codigo nao encontrado no JSONBin!" -ForegroundColor Red
        Write-Host ""
        exit
    }
} catch {
    Write-Host "  [ERRO] Nao conseguiu acessar JSONBin" -ForegroundColor Red
    Write-Host $_.Exception.Message
    Write-Host ""
    exit
}

# Testar no servidor Render.com
Write-Host "2. Testando no servidor Render.com..." -ForegroundColor Cyan
$url = "https://maxiptv-update.onrender.com/download.php?code=$code"

try {
    $response = Invoke-WebRequest -Uri $url -Method GET -ErrorAction Stop
    Write-Host "  Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Resposta:" -ForegroundColor Cyan
    $content = $response.Content
    Write-Host $content -ForegroundColor White
    
    # Tentar parsear JSON
    try {
        $json = $content | ConvertFrom-Json
        if ($json.success) {
            Write-Host ""
            Write-Host "  [SUCESSO] Login automatico configurado!" -ForegroundColor Green
            Write-Host "     Usuario: $($json.usuario)"
            Write-Host "     API: $($json.api)"
            Write-Host "     Expira: $($json.expira_em)"
        }
    } catch {
        # Nao e JSON
    }
} catch {
    Write-Host "  [ERRO] Servidor retornou erro" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host ""
Write-Host "=== Fim do teste ===" -ForegroundColor Cyan


# Script para diagnosticar login automatico apos download do APK
param(
    [Parameter(Mandatory=$true)]
    [string]$Code
)

Write-Host "=== DIAGNOSTICO DE LOGIN AUTOMATICO ===" -ForegroundColor Cyan
Write-Host "Codigo: $Code" -ForegroundColor Yellow
Write-Host ""

$serverUrl = "https://maxiptv-update-1.onrender.com"
$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$jsonbinKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$headers = @{"X-Master-Key" = $jsonbinKey}

# 1. Verificar codigo no JSONBin
Write-Host "1. VERIFICANDO CODIGO NO JSONBIN..." -ForegroundColor Cyan
try {
    $jsonbin = Invoke-RestMethod -Uri $jsonbinUrl -Headers $headers -Method Get
    if ($jsonbin.record.$Code) {
        $data = $jsonbin.record.$Code
        Write-Host "OK Codigo encontrado!" -ForegroundColor Green
        Write-Host "   Username: $($data.username)" -ForegroundColor White
        Write-Host "   API: $($data.apiUrl)" -ForegroundColor White
        Write-Host "   ExpiryDate: $($data.expiryDate)" -ForegroundColor White
    } else {
        Write-Host "ERRO: Codigo nao encontrado!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERRO: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "2. TESTANDO auto_login.php..." -ForegroundColor Cyan
try {
    $url = "$serverUrl/auto_login.php?code=$Code"
    Write-Host "   URL: $url" -ForegroundColor Gray
    
    $response = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 10
    Write-Host "OK Resposta recebida:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 3 | Write-Host
    
    Write-Host ""
    Write-Host "VERIFICANDO FORMATO..." -ForegroundColor Cyan
    
    $expected = @("user", "password", "api", "expiryDate")
    $missing = @()
    $extra = @()
    
    foreach ($field in $expected) {
        if (-not $response.PSObject.Properties.Name -contains $field) {
            $missing += $field
        }
    }
    
    foreach ($prop in $response.PSObject.Properties.Name) {
        if ($prop -notin $expected -and $prop -ne "status" -and $prop -ne "mensagem") {
            $extra += $prop
        }
    }
    
    if ($missing.Count -eq 0 -and $extra.Count -eq 0) {
        Write-Host "OK FORMATO CORRETO!" -ForegroundColor Green
        Write-Host "   - user: $($response.user)" -ForegroundColor Green
        Write-Host "   - password: $($response.password)" -ForegroundColor Green
        Write-Host "   - api: $($response.api)" -ForegroundColor Green
        Write-Host "   - expiryDate: $($response.expiryDate)" -ForegroundColor Green
    } else {
        Write-Host "ERRO: FORMATO INCORRETO!" -ForegroundColor Red
        if ($missing.Count -gt 0) {
            Write-Host "   Campos FALTANDO:" -ForegroundColor Red
            $missing | ForEach-Object { Write-Host "     - $_" -ForegroundColor Red }
        }
        if ($extra.Count -gt 0) {
            Write-Host "   Campos EXTRAS:" -ForegroundColor Yellow
            $extra | ForEach-Object { Write-Host "     - $_" -ForegroundColor Yellow }
        }
        Write-Host ""
        Write-Host "FORMATO CORRETO ESPERADO:" -ForegroundColor Cyan
        Write-Host '{' -ForegroundColor White
        Write-Host '  "user": "max",' -ForegroundColor White
        Write-Host '  "password": "1234",' -ForegroundColor White
        Write-Host '  "api": "https://aztv.cx/player_api.php",' -ForegroundColor White
        Write-Host '  "expiryDate": "12/05/2026"' -ForegroundColor White
        Write-Host '}' -ForegroundColor White
    }
    
    if ($response.status) {
        Write-Host "AVISO: Campo 'status' presente (nao deveria estar)" -ForegroundColor Yellow
    }
    if ($response.apiUrl) {
        Write-Host "AVISO: Campo 'apiUrl' presente (deveria ser 'api')" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "ERRO ao testar auto_login.php: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status: $statusCode" -ForegroundColor Red
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $errorContent = $reader.ReadToEnd()
            Write-Host "   Resposta: $errorContent" -ForegroundColor Red
        } catch {}
    }
}

Write-Host ""
Write-Host "=== FIM DO DIAGNOSTICO ===" -ForegroundColor Cyan

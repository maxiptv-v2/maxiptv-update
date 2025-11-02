# Script para diagnosticar login automatico
# Testa cada etapa do processo: dl.php -> get-pending-code.php -> auto_login.php

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  DIAGNOSTICO: LOGIN AUTOMATICO APOS DOWNLOAD APK" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Solicitar codigo para testar
$codigoTeste = Read-Host "Digite o codigo para testar (ou Enter para usar codigo exemplo 6789)"
if ([string]::IsNullOrWhiteSpace($codigoTeste)) {
    $codigoTeste = "6789"
}

Write-Host ""
Write-Host "Testando codigo: $codigoTeste" -ForegroundColor Yellow
Write-Host ""

$jsonbinUrl = "https://api.jsonbin.io/v3/b/68ec647643b1c97be964e96b/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'
$renderUrl = "https://maxiptv-update-1.onrender.com"

# ============================================
# ETAPA 1: Verificar se codigo existe no JSONBin
# ============================================
Write-Host "============================================================" -ForegroundColor Green
Write-Host "ETAPA 1: Verificar codigo no JSONBin" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

try {
    $headers = @{
        "X-Master-Key" = $apiKey
    }
    
    $response = Invoke-RestMethod -Uri $jsonbinUrl -Method Get -Headers $headers -ErrorAction Stop
    $record = $response.record
    
    Write-Host "JSONBin conectado com sucesso" -ForegroundColor Green
    
    # Verificar se codigo existe
    if ($record.PSObject.Properties.Name -contains $codigoTeste) {
        Write-Host "Codigo '$codigoTeste' ENCONTRADO no JSONBin" -ForegroundColor Green
        $codeData = $record.$codigoTeste
        Write-Host "   Username: $($codeData.username)" -ForegroundColor White
        Write-Host "   API URL: $($codeData.apiUrl)" -ForegroundColor White
        Write-Host "   Expiry Date: $($codeData.expiryDate)" -ForegroundColor White
    } else {
        Write-Host "Codigo '$codigoTeste' NAO encontrado no JSONBin" -ForegroundColor Red
        Write-Host "   Codigos disponiveis:" -ForegroundColor Yellow
        foreach ($key in $record.PSObject.Properties.Name) {
            if ($key -notin @("sessions", "users", "_pending_logins")) {
                Write-Host "     - $key" -ForegroundColor Gray
            }
        }
        Write-Host ""
        Write-Host "TESTE INTERROMPIDO: Codigo nao existe no JSONBin" -ForegroundColor Red
        exit 1
    }
    
    # Verificar _pending_logins
    if ($record._pending_logins) {
        Write-Host ""
        Write-Host "Codigos pendentes encontrados:" -ForegroundColor Yellow
        foreach ($key in $record._pending_logins.PSObject.Properties.Name) {
            $pending = $record._pending_logins.$key
            Write-Host "   IP: $key" -ForegroundColor Gray
            Write-Host "      Codigo: $($pending.code)" -ForegroundColor Gray
            Write-Host "      Username: $($pending.username)" -ForegroundColor Gray
        }
    } else {
        Write-Host "Nenhum codigo pendente no momento" -ForegroundColor Gray
    }
    
} catch {
    Write-Host "ERRO ao conectar no JSONBin: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Start-Sleep -Seconds 1

# ============================================
# ETAPA 2: Testar dl.php (simula download)
# ============================================
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "ETAPA 2: Testar dl.php (simular download)" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

try {
    $dlUrl = "$renderUrl/dl/$codigoTeste"
    Write-Host "Acessando: $dlUrl" -ForegroundColor Cyan
    
    try {
        $request = Invoke-WebRequest -Uri $dlUrl -Method Get -MaximumRedirection 0 -ErrorAction Stop -TimeoutSec 10
        Write-Host "dl.php retornou HTTP $($request.StatusCode)" -ForegroundColor Yellow
    } catch {
        if ($_.Exception.Response.StatusCode -eq 302) {
            Write-Host "dl.php redirecionou corretamente (HTTP 302)" -ForegroundColor Green
        } elseif ($_.Exception.Response.StatusCode -eq 404) {
            Write-Host "Codigo invalido ou expirado (HTTP 404)" -ForegroundColor Red
        } else {
            Write-Host "Status: $($_.Exception.Response.StatusCode)" -ForegroundColor Yellow
        }
    }
    
} catch {
    Write-Host "ERRO ao acessar dl.php: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Start-Sleep -Seconds 3

# ============================================
# ETAPA 3: Verificar se codigo foi salvo em _pending_logins
# ============================================
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "ETAPA 3: Verificar _pending_logins apos dl.php" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

try {
    $response = Invoke-RestMethod -Uri $jsonbinUrl -Method Get -Headers $headers -ErrorAction Stop
    $record = $response.record
    
    if ($record._pending_logins) {
        $found = $false
        foreach ($key in $record._pending_logins.PSObject.Properties.Name) {
            $pending = $record._pending_logins.$key
            if ($pending.code -eq $codigoTeste) {
                Write-Host "Codigo '$codigoTeste' encontrado em _pending_logins!" -ForegroundColor Green
                Write-Host "   IP: $key" -ForegroundColor White
                Write-Host "   Username: $($pending.username)" -ForegroundColor White
                $found = $true
                break
            }
        }
        
        if (-not $found) {
            Write-Host "Codigo '$codigoTeste' NAO encontrado em _pending_logins" -ForegroundColor Red
        }
    } else {
        Write-Host "_pending_logins esta vazio - dl.php pode nao estar salvando" -ForegroundColor Red
    }
    
} catch {
    Write-Host "ERRO ao verificar _pending_logins: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Start-Sleep -Seconds 1

# ============================================
# ETAPA 4: Testar get-pending-code.php
# ============================================
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "ETAPA 4: Testar get-pending-code.php" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

try {
    $pendingUrl = "$renderUrl/get-pending-code.php"
    Write-Host "Acessando: $pendingUrl" -ForegroundColor Cyan
    
    $response = Invoke-RestMethod -Uri $pendingUrl -Method Get -TimeoutSec 10 -ErrorAction Stop
    
    Write-Host "Resposta recebida:" -ForegroundColor White
    $response | ConvertTo-Json -Depth 5 | Write-Host
    
    if ($response.status -eq "ok") {
        Write-Host "get-pending-code.php retornou codigo valido!" -ForegroundColor Green
        Write-Host "   Codigo: $($response.code)" -ForegroundColor White
        Write-Host "   Username: $($response.username)" -ForegroundColor White
    } elseif ($response.status -eq "nao_encontrado") {
        Write-Host "Nenhum codigo pendente encontrado" -ForegroundColor Yellow
    } else {
        Write-Host "Status inesperado: $($response.status)" -ForegroundColor Red
    }
    
} catch {
    Write-Host "ERRO ao acessar get-pending-code.php: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Start-Sleep -Seconds 1

# ============================================
# ETAPA 5: Testar auto_login.php
# ============================================
Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "ETAPA 5: Testar auto_login.php" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green

try {
    $autoLoginUrl = "$renderUrl/auto_login.php?code=$codigoTeste"
    Write-Host "Acessando: $autoLoginUrl" -ForegroundColor Cyan
    
    $response = Invoke-RestMethod -Uri $autoLoginUrl -Method Get -TimeoutSec 10 -ErrorAction Stop
    
    Write-Host "Resposta recebida:" -ForegroundColor White
    $response | ConvertTo-Json -Depth 5 | Write-Host
    
    if ($response.status -eq "ok") {
        Write-Host "auto_login.php retornou credenciais validas!" -ForegroundColor Green
        Write-Host "   User: $($response.user)" -ForegroundColor White
        Write-Host "   API URL: $($response.apiUrl)" -ForegroundColor White
    } else {
        Write-Host "auto_login.php retornou erro" -ForegroundColor Red
        Write-Host "   Mensagem: $($response.mensagem)" -ForegroundColor Red
    }
    
} catch {
    Write-Host "ERRO ao acessar auto_login.php: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  DIAGNOSTICO CONCLUIDO" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

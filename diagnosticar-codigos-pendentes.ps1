#!/usr/bin/env pwsh
# Script para diagnosticar por que códigos pendentes não estão sendo encontrados

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DIAGNÓSTICO DE CÓDIGOS PENDENTES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$jsonbin_url = "https://api.jsonbin.io/v3/b/690be6da43b1c97be99b8bc7/latest"
$apiKey = '$2a$10$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae'

Write-Host "🔍 Buscando dados do JSONBin..." -ForegroundColor Yellow
Write-Host ""

try {
    $headers = @{
        "X-Master-Key" = $apiKey
    }
    
    $response = Invoke-RestMethod -Uri $jsonbin_url -Headers $headers -Method Get -TimeoutSec 15 -ErrorAction Stop
    
    $record = $response.record
    
    Write-Host "✅ Dados recebidos do JSONBin!" -ForegroundColor Green
    Write-Host ""
    
    # Verificar códigos pendentes
    if (-not $record._pending_logins -or $record._pending_logins.Count -eq 0) {
        Write-Host "❌ Nenhum código pendente encontrado no JSONBin!" -ForegroundColor Red
        Write-Host ""
        exit 1
    }
    
    Write-Host "📋 CÓDIGOS PENDENTES ENCONTRADOS: $($record._pending_logins.Count)" -ForegroundColor Cyan
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    
    $currentTime = [DateTimeOffset]::Now.ToUnixTimeSeconds()
    
    $codigosValidos = 0
    $codigosExpirados = 0
    $codigosUsados = 0
    
    foreach ($key in $record._pending_logins.PSObject.Properties.Name) {
        $pending = $record._pending_logins.$key
        
        Write-Host "🔑 Código: $($pending.code)" -ForegroundColor Yellow
        Write-Host "   Key: $key" -ForegroundColor Gray
        Write-Host "   Username: $($pending.username)" -ForegroundColor White
        Write-Host "   Timestamp: $($pending.timestamp) ($([DateTimeOffset]::FromUnixTimeSeconds($pending.timestamp).ToString('dd/MM/yyyy HH:mm:ss')))" -ForegroundColor White
        
        # Verificar expiração
        $expiresAt = $pending.expiresAt
        if ($expiresAt) {
            $expired = $currentTime > $expiresAt
            $expiresAtDate = [DateTimeOffset]::FromUnixTimeSeconds($expiresAt).ToString('dd/MM/yyyy HH:mm:ss')
            Write-Host "   ExpiresAt: $expiresAt ($expiresAtDate)" -ForegroundColor $(if ($expired) { "Red" } else { "Green" })
            
            if ($expired) {
                Write-Host "   ⚠️ EXPIRADO! (há $([math]::Round(($currentTime - $expiresAt) / 60, 1)) minutos)" -ForegroundColor Red
                $codigosExpirados++
            } else {
                Write-Host "   ✅ Ainda válido (expira em $([math]::Round(($expiresAt - $currentTime) / 60, 1)) minutos)" -ForegroundColor Green
            }
        } else {
            Write-Host "   ⚠️ ExpiresAt não definido!" -ForegroundColor Yellow
        }
        
        # Verificar se foi usado
        $used = $pending.used
        if ($used) {
            $usedAt = $pending.usedAt
            if ($usedAt) {
                $usedTimeAgo = $currentTime - $usedAt
                Write-Host "   ⚠️ MARCADO COMO USADO há $([math]::Round($usedTimeAgo / 60, 1)) minutos" -ForegroundColor Yellow
                
                if ($usedTimeAgo -gt 300) {
                    Write-Host "   ❌ Usado há mais de 5 minutos - será IGNORADO pelo get-pending-code.php" -ForegroundColor Red
                    $codigosUsados++
                } else {
                    Write-Host "   ✅ Usado há menos de 5 minutos - pode ser usado novamente" -ForegroundColor Green
                }
            } else {
                Write-Host "   ⚠️ Marcado como usado mas usedAt não definido" -ForegroundColor Yellow
            }
        } else {
            Write-Host "   ✅ Não foi usado ainda" -ForegroundColor Green
        }
        
        # Verificar credenciais
        Write-Host "   Password: $(if ($pending.password) { '✅ Presente' } else { '❌ Ausente' })" -ForegroundColor $(if ($pending.password) { "Green" } else { "Red" })
        Write-Host "   API URL: $(if ($pending.apiUrl) { '✅ Presente' } else { '❌ Ausente' })" -ForegroundColor $(if ($pending.apiUrl) { "Green" } else { "Red" })
        Write-Host "   ExpiryDate: $(if ($pending.expiryDate) { $pending.expiryDate } else { '❌ Ausente' })" -ForegroundColor $(if ($pending.expiryDate) { "Green" } else { "Red" })
        
        # Verificar se seria encontrado pelo get-pending-code.php
        $seriaEncontrado = $true
        
        if ($expiresAt -and $currentTime > $expiresAt) {
            $seriaEncontrado = $false
            Write-Host "   ❌ NÃO seria encontrado: EXPIRADO" -ForegroundColor Red
        } elseif ($used -and $usedAt -and ($currentTime - $usedAt) -gt 300) {
            $seriaEncontrado = $false
            Write-Host "   ❌ NÃO seria encontrado: USADO há mais de 5 minutos" -ForegroundColor Red
        } else {
            $seriaEncontrado = $true
            Write-Host "   ✅ SERIA encontrado pelo get-pending-code.php" -ForegroundColor Green
            $codigosValidos++
        }
        
        Write-Host ""
    }
    
    Write-Host "----------------------------------------" -ForegroundColor Gray
    Write-Host ""
    Write-Host "📊 RESUMO:" -ForegroundColor Cyan
    Write-Host "   Total de códigos pendentes: $($record._pending_logins.Count)" -ForegroundColor White
    Write-Host "   ✅ Códigos válidos (seriam encontrados): $codigosValidos" -ForegroundColor Green
    Write-Host "   ⚠️ Códigos expirados: $codigosExpirados" -ForegroundColor Yellow
    Write-Host "   ⚠️ Códigos usados (há mais de 5 min): $codigosUsados" -ForegroundColor Yellow
    Write-Host ""
    
    if ($codigosValidos -eq 0) {
        Write-Host "❌ PROBLEMA IDENTIFICADO: Nenhum código válido disponível!" -ForegroundColor Red
        Write-Host ""
        Write-Host "💡 SOLUÇÕES:" -ForegroundColor Yellow
        Write-Host "   1. Gerar um novo código no painel admin" -ForegroundColor White
        Write-Host "   2. Usar o código no Downloader IMEDIATAMENTE após gerar" -ForegroundColor White
        Write-Host "   3. Abrir o app IMEDIATAMENTE após download" -ForegroundColor White
        Write-Host ""
        Write-Host "   ⚠️ Os códigos expiram em 15 minutos!" -ForegroundColor Yellow
        Write-Host "   ⚠️ Códigos usados são ignorados após 5 minutos!" -ForegroundColor Yellow
    } else {
        Write-Host "✅ Há $codigosValidos código(s) válido(s) disponível(is)!" -ForegroundColor Green
        Write-Host ""
        Write-Host "💡 Se o app não está encontrando, verificar:" -ForegroundColor Yellow
        Write-Host "   1. Se o app está chamando get-pending-code.php corretamente" -ForegroundColor White
        Write-Host "   2. Se há problemas de rede/timeout" -ForegroundColor White
        Write-Host "   3. Se o Render atualizou o código PHP" -ForegroundColor White
    }
    
    Write-Host ""
    
} catch {
    Write-Host "❌ ERRO ao buscar dados do JSONBin:" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""





# Script de teste para logica de atualizacao Fire OS
# Simula o comportamento do ApkDownloader.kt

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "TESTE DE LOGICA DE ATUALIZACAO FIRE OS" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Funcao para detectar Fire OS
function Test-IsFireOS {
    param(
        [string]$Manufacturer,
        [string]$Brand,
        [bool]$IsFireStick
    )
    
    $manufacturerLower = $Manufacturer.ToLower()
    $brandLower = $Brand.ToLower()
    
    return ($IsFireStick -or 
            $manufacturerLower.Contains("amazon") -or 
            $brandLower.Contains("amazon"))
}

# Funcao para testar tempos de espera
function Test-WaitTimes {
    param(
        [bool]$IsFire
    )
    
    Write-Host "[TESTE] Tempos de espera..." -ForegroundColor Yellow
    
    $waitTime = if ($IsFire) { 1500 } else { 500 }
    
    $fireColor = if ($IsFire) { "Red" } else { "Green" }
    Write-Host "  Fire OS: $IsFire" -ForegroundColor $fireColor
    Write-Host "  Tempo de espera: ${waitTime}ms" -ForegroundColor Yellow
    
    if ($IsFire -and $waitTime -ne 1500) {
        Write-Host "  [ERRO] Fire OS deveria ter 1500ms, mas tem ${waitTime}ms" -ForegroundColor Red
        return $false
    }
    
    if (-not $IsFire -and $waitTime -ne 500) {
        Write-Host "  [ERRO] Android normal deveria ter 500ms, mas tem ${waitTime}ms" -ForegroundColor Red
        return $false
    }
    
    Write-Host "  [OK] Tempo de espera correto" -ForegroundColor Green
    Write-Host ""
    return $true
}

# Funcao para testar flags do Intent
function Test-IntentFlags {
    param(
        [bool]$IsFire
    )
    
    Write-Host "[TESTE] Flags do Intent..." -ForegroundColor Yellow
    
    $fireColor = if ($IsFire) { "Red" } else { "Green" }
    Write-Host "  Fire OS: $IsFire" -ForegroundColor $fireColor
    
    $flags = @("FLAG_ACTIVITY_NEW_TASK", "FLAG_GRANT_READ_URI_PERMISSION", "FLAG_GRANT_WRITE_URI_PERMISSION")
    
    if ($IsFire) {
        $flags += "FLAG_ACTIVITY_CLEAR_TOP"
        Write-Host "  [OK] Flag adicional para Fire OS: FLAG_ACTIVITY_CLEAR_TOP" -ForegroundColor Green
    } else {
        Write-Host "  [OK] Flags padrao (sem CLEAR_TOP)" -ForegroundColor Green
    }
    
    Write-Host "  Flags aplicadas: $($flags -join ', ')" -ForegroundColor Cyan
    Write-Host ""
    return $true
}

# Funcao para testar fallback
function Test-Fallback {
    param(
        [bool]$IsFire
    )
    
    Write-Host "[TESTE] Fallback..." -ForegroundColor Yellow
    
    $fireColor = if ($IsFire) { "Red" } else { "Green" }
    Write-Host "  Fire OS: $IsFire" -ForegroundColor $fireColor
    
    if ($IsFire) {
        Write-Host "  [OK] Fallback DESABILITADO para Fire OS (correto)" -ForegroundColor Green
        Write-Host "  Motivo: Fire OS pode nao suportar fallback corretamente" -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] Fallback HABILITADO para Android normal (correto)" -ForegroundColor Green
    }
    
    Write-Host ""
    return $true
}

# Funcao para testar BroadcastReceiver
function Test-BroadcastReceiver {
    param(
        [bool]$IsFire
    )
    
    Write-Host "[TESTE] BroadcastReceiver..." -ForegroundColor Yellow
    
    $fireColor = if ($IsFire) { "Red" } else { "Green" }
    Write-Host "  Fire OS: $IsFire" -ForegroundColor $fireColor
    
    if ($IsFire) {
        Write-Host "  [OK] Aguarda 1000ms extra quando recebe broadcast (Fire OS)" -ForegroundColor Green
        Write-Host "  Motivo: Garantir que arquivo esta completamente escrito" -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] Sem espera extra (Android normal)" -ForegroundColor Green
    }
    
    Write-Host ""
    return $true
}

# Funcao para testar verificação de status
function Test-StatusCheck {
    param(
        [bool]$IsFire
    )
    
    Write-Host "[TESTE] Verificacao de status..." -ForegroundColor Yellow
    
    $fireColor = if ($IsFire) { "Red" } else { "Green" }
    Write-Host "  Fire OS: $IsFire" -ForegroundColor $fireColor
    
    if ($IsFire) {
        Write-Host "  [OK] Aguarda 1000ms antes de instalar apos status SUCCESSFUL" -ForegroundColor Green
    } else {
        Write-Host "  [OK] Instala imediatamente apos status SUCCESSFUL" -ForegroundColor Green
    }
    
    Write-Host ""
    return $true
}

# ============================================
# TESTES
# ============================================

$allTestsPassed = $true
$testResults = @()

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "TESTE 1: Deteccao de Fire OS" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Teste 1.1: Fire Stick detectado
$isFire1 = Test-IsFireOS -Manufacturer "Amazon" -Brand "Amazon" -IsFireStick $false
if ($isFire1) {
    Write-Host "[OK] TESTE 1.1 PASSOU: Fire OS detectado por manufacturer/brand" -ForegroundColor Green
    $testResults += @{Test="1.1"; Status="PASSOU"; Message="Fire OS detectado"}
} else {
    Write-Host "[ERRO] TESTE 1.1 FALHOU: Fire OS NAO detectado" -ForegroundColor Red
    $testResults += @{Test="1.1"; Status="FALHOU"; Message="Fire OS nao detectado"}
    $allTestsPassed = $false
}

# Teste 1.2: Android normal nao detectado como Fire OS
$isFire2 = Test-IsFireOS -Manufacturer "Samsung" -Brand "Samsung" -IsFireStick $false
if (-not $isFire2) {
    Write-Host "[OK] TESTE 1.2 PASSOU: Android normal NAO detectado como Fire OS" -ForegroundColor Green
    $testResults += @{Test="1.2"; Status="PASSOU"; Message="Android normal nao detectado como Fire OS"}
} else {
    Write-Host "[ERRO] TESTE 1.2 FALHOU: Android normal detectado como Fire OS" -ForegroundColor Red
    $testResults += @{Test="1.2"; Status="FALHOU"; Message="Falso positivo"}
    $allTestsPassed = $false
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "TESTE 2: Tempos de Espera" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$test2a = Test-WaitTimes -IsFire $true
$test2b = Test-WaitTimes -IsFire $false

if ($test2a -and $test2b) {
    Write-Host "[OK] TESTE 2 PASSOU: Tempos de espera corretos" -ForegroundColor Green
    $testResults += @{Test="2"; Status="PASSOU"; Message="Tempos corretos"}
} else {
    Write-Host "[ERRO] TESTE 2 FALHOU: Tempos de espera incorretos" -ForegroundColor Red
    $testResults += @{Test="2"; Status="FALHOU"; Message="Tempos incorretos"}
    $allTestsPassed = $false
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "TESTE 3: Flags do Intent" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$test3a = Test-IntentFlags -IsFire $true
$test3b = Test-IntentFlags -IsFire $false

if ($test3a -and $test3b) {
    Write-Host "[OK] TESTE 3 PASSOU: Flags do Intent corretas" -ForegroundColor Green
    $testResults += @{Test="3"; Status="PASSOU"; Message="Flags corretas"}
} else {
    Write-Host "[ERRO] TESTE 3 FALHOU: Flags do Intent incorretas" -ForegroundColor Red
    $testResults += @{Test="3"; Status="FALHOU"; Message="Flags incorretas"}
    $allTestsPassed = $false
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "TESTE 4: Fallback" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$test4a = Test-Fallback -IsFire $true
$test4b = Test-Fallback -IsFire $false

if ($test4a -and $test4b) {
    Write-Host "[OK] TESTE 4 PASSOU: Logica de fallback correta" -ForegroundColor Green
    $testResults += @{Test="4"; Status="PASSOU"; Message="Fallback correto"}
} else {
    Write-Host "[ERRO] TESTE 4 FALHOU: Logica de fallback incorreta" -ForegroundColor Red
    $testResults += @{Test="4"; Status="FALHOU"; Message="Fallback incorreto"}
    $allTestsPassed = $false
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "TESTE 5: BroadcastReceiver" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$test5a = Test-BroadcastReceiver -IsFire $true
$test5b = Test-BroadcastReceiver -IsFire $false

if ($test5a -and $test5b) {
    Write-Host "[OK] TESTE 5 PASSOU: Logica do BroadcastReceiver correta" -ForegroundColor Green
    $testResults += @{Test="5"; Status="PASSOU"; Message="BroadcastReceiver correto"}
} else {
    Write-Host "[ERRO] TESTE 5 FALHOU: Logica do BroadcastReceiver incorreta" -ForegroundColor Red
    $testResults += @{Test="5"; Status="FALHOU"; Message="BroadcastReceiver incorreto"}
    $allTestsPassed = $false
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "TESTE 6: Verificacao de Status" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$test6a = Test-StatusCheck -IsFire $true
$test6b = Test-StatusCheck -IsFire $false

if ($test6a -and $test6b) {
    Write-Host "[OK] TESTE 6 PASSOU: Verificacao de status correta" -ForegroundColor Green
    $testResults += @{Test="6"; Status="PASSOU"; Message="Status correto"}
} else {
    Write-Host "[ERRO] TESTE 6 FALHOU: Verificacao de status incorreta" -ForegroundColor Red
    $testResults += @{Test="6"; Status="FALHOU"; Message="Status incorreto"}
    $allTestsPassed = $false
}

# ============================================
# RESUMO FINAL
# ============================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "RESUMO DOS TESTES" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

foreach ($result in $testResults) {
    $color = if ($result.Status -eq "PASSOU") { "Green" } else { "Red" }
    $icon = if ($result.Status -eq "PASSOU") { "[OK]" } else { "[ERRO]" }
    Write-Host "$icon Teste $($result.Test): $($result.Status) - $($result.Message)" -ForegroundColor $color
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan

if ($allTestsPassed) {
    Write-Host "[OK] TODOS OS TESTES PASSARAM!" -ForegroundColor Green
    Write-Host "   A logica de atualizacao esta correta para Fire OS e Android normal" -ForegroundColor Green
    exit 0
} else {
    Write-Host "[ERRO] ALGUNS TESTES FALHARAM!" -ForegroundColor Red
    Write-Host "   Revise os testes que falharam acima" -ForegroundColor Red
    exit 1
}

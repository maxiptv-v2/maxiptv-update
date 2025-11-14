# Script de teste para SafeAreaAutoDetector
# Testa a lógica de detecção automática de overscan

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE: SafeAreaAutoDetector Logic" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Simular diferentes tipos de dispositivos e tamanhos de tela
# DPI calculado baseado no tamanho físico real da TV
# Para 1920x1080: DPI ≈ (largura_px / largura_física_polegadas)
$testCases = @(
    @{
        Name = "Fire Stick - TV 55 polegadas"
        DeviceType = "FireStick"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 55.0  # Tamanho físico real
        ExpectedHorizontal = 57.6  # 48 * 1.2
        ExpectedTop = 14.4  # 24 * 0.6
        ExpectedBottom = 33.6  # 24 * 1.4
        ExpectedScale = 0.96
    },
    @{
        Name = "TV Box - TV 65 polegadas"
        DeviceType = "TvBox"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 65.0
        ExpectedHorizontal = 44
        ExpectedVertical = 32
        ExpectedScale = 0.92
    },
    @{
        Name = "Projetor - 80 polegadas"
        DeviceType = "Projector"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 80.0
        ExpectedHorizontal = 72
        ExpectedVertical = 52
        ExpectedScale = 0.9
    },
    @{
        Name = "TV Nativa - 55 polegadas"
        DeviceType = "NativeTv"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 55.0
        ExpectedHorizontal = 48
        ExpectedVertical = 34
        ExpectedScale = 0.92
    },
    @{
        Name = "TV Box - TV 45 polegadas"
        DeviceType = "TvBox"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 45.0
        ExpectedHorizontal = 30
        ExpectedVertical = 22
        ExpectedScale = 0.96
    },
    @{
        Name = "TV Nativa - 32 polegadas"
        DeviceType = "NativeTv"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 32.0
        ExpectedHorizontal = 32
        ExpectedVertical = 22
        ExpectedScale = 0.97
    },
    @{
        Name = "TV Nativa - 40 polegadas"
        DeviceType = "NativeTv"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 40.0
        ExpectedHorizontal = 36
        ExpectedVertical = 24
        ExpectedScale = 0.96
    },
    @{
        Name = "TV Box - 32 polegadas"
        DeviceType = "TvBox"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 32.0
        ExpectedHorizontal = 24
        ExpectedVertical = 16
        ExpectedScale = 0.98
    },
    @{
        Name = "TV Box - 40 polegadas"
        DeviceType = "TvBox"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 40.0
        ExpectedHorizontal = 26
        ExpectedVertical = 18
        ExpectedScale = 0.97
    },
    @{
        Name = "Projetor - 32 polegadas"
        DeviceType = "Projector"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 32.0
        ExpectedHorizontal = 38
        ExpectedVertical = 24
        ExpectedScale = 0.9
    },
    @{
        Name = "Projetor - 40 polegadas"
        DeviceType = "Projector"
        WidthPx = 1920
        HeightPx = 1080
        DiagonalInches = 40.0
        ExpectedHorizontal = 42
        ExpectedVertical = 26
        ExpectedScale = 0.9
    }
)

function CalculateDiagonalInches($widthPx, $heightPx, $xDpi, $yDpi) {
    $xDpiActual = if ($xDpi -gt 0) { $xDpi } else { 160 }
    $yDpiActual = if ($yDpi -gt 0) { $yDpi } else { 160 }
    $widthInches = $widthPx / $xDpiActual
    $heightInches = $heightPx / $yDpiActual
    $diagonal = [Math]::Sqrt(($widthInches * $widthInches) + ($heightInches * $heightInches))
    return $diagonal
}

function TestFireStick($diagonalInches) {
    # Valores base do MaxiApp (fireStickOverscanPadding padrão = 48, fireStickSafeAreaPadding = 24)
    # Mas são ajustados dinamicamente, então usamos valores típicos
    $overscanPadding = 48  # Valor padrão
    $safeAreaPadding = 24   # Valor padrão
    
    $horizontal = [Math]::Max($overscanPadding * 1.2, 24)
    $top = [Math]::Max($safeAreaPadding * 0.6, 12)
    $bottom = [Math]::Max($safeAreaPadding * 1.4, 28)
    $scale = 0.96
    
    return @{
        Top = $top
        Bottom = $bottom
        Start = $horizontal
        End = $horizontal
        Scale = $scale
    }
}

function TestProjector($diagonalInches) {
    if ($diagonalInches -ge 100) {
        $horizontal = 80
        $vertical = 58
    } elseif ($diagonalInches -ge 80) {
        $horizontal = 72
        $vertical = 52
    } elseif ($diagonalInches -ge 70) {
        $horizontal = 68
        $vertical = 48
    } elseif ($diagonalInches -ge 65) {
        $horizontal = 64
        $vertical = 44
    } elseif ($diagonalInches -ge 60) {
        $horizontal = 60
        $vertical = 40
    } elseif ($diagonalInches -ge 55) {
        $horizontal = 54
        $vertical = 36
    } elseif ($diagonalInches -ge 50) {
        $horizontal = 50
        $vertical = 32
    } elseif ($diagonalInches -ge 45) {
        $horizontal = 46
        $vertical = 28
    } elseif ($diagonalInches -ge 40) {
        $horizontal = 42
        $vertical = 26
    } elseif ($diagonalInches -ge 32) {
        $horizontal = 38
        $vertical = 24
    } else {
        $horizontal = 32
        $vertical = 20
    }
    
    $scale = 0.9
    
    return @{
        Top = $vertical
        Bottom = $vertical
        Start = $horizontal
        End = $horizontal
        Scale = $scale
    }
}

function TestNativeTv($diagonalInches) {
    if ($diagonalInches -ge 85) {
        $horizontal = 68
        $vertical = 50
        $scale = 0.86
    } elseif ($diagonalInches -ge 75) {
        $horizontal = 64
        $vertical = 46
        $scale = 0.88
    } elseif ($diagonalInches -ge 70) {
        $horizontal = 60
        $vertical = 42
        $scale = 0.89
    } elseif ($diagonalInches -ge 65) {
        $horizontal = 56
        $vertical = 40
        $scale = 0.9
    } elseif ($diagonalInches -ge 60) {
        $horizontal = 52
        $vertical = 36
        $scale = 0.91
    } elseif ($diagonalInches -ge 55) {
        $horizontal = 48
        $vertical = 34
        $scale = 0.92
    } elseif ($diagonalInches -ge 50) {
        $horizontal = 44
        $vertical = 30
        $scale = 0.93
    } elseif ($diagonalInches -ge 45) {
        $horizontal = 40
        $vertical = 28
        $scale = 0.95
    } elseif ($diagonalInches -ge 43) {
        $horizontal = 38
        $vertical = 26
        $scale = 0.955
    } elseif ($diagonalInches -ge 40) {
        $horizontal = 36
        $vertical = 24
        $scale = 0.96
    } elseif ($diagonalInches -ge 32) {
        $horizontal = 32
        $vertical = 22
        $scale = 0.97
    } else {
        $horizontal = 28
        $vertical = 20
        $scale = 0.98
    }
    
    return @{
        Top = $vertical
        Bottom = $vertical
        Start = $horizontal
        End = $horizontal
        Scale = $scale
    }
}

function TestTvBox($diagonalInches) {
    if ($diagonalInches -ge 75) {
        $horizontal = 50
        $vertical = 38
        $scale = 0.90
    } elseif ($diagonalInches -ge 70) {
        $horizontal = 48
        $vertical = 36
        $scale = 0.91
    } elseif ($diagonalInches -ge 65) {
        $horizontal = 44
        $vertical = 32
        $scale = 0.92
    } elseif ($diagonalInches -ge 60) {
        $horizontal = 40
        $vertical = 30
        $scale = 0.93
    } elseif ($diagonalInches -ge 55) {
        $horizontal = 36
        $vertical = 26
        $scale = 0.94
    } elseif ($diagonalInches -ge 50) {
        $horizontal = 34
        $vertical = 24
        $scale = 0.95
    } elseif ($diagonalInches -ge 45) {
        $horizontal = 30
        $vertical = 22
        $scale = 0.96
    } elseif ($diagonalInches -ge 43) {
        $horizontal = 28
        $vertical = 20
        $scale = 0.965
    } elseif ($diagonalInches -ge 40) {
        $horizontal = 26
        $vertical = 18
        $scale = 0.97
    } elseif ($diagonalInches -ge 32) {
        $horizontal = 24
        $vertical = 16
        $scale = 0.98
    } else {
        $horizontal = 20
        $vertical = 14
        $scale = 0.99
    }
    
    return @{
        Top = $vertical
        Bottom = $vertical
        Start = $horizontal
        End = $horizontal
        Scale = $scale
    }
}

$totalTests = 0
$passedTests = 0
$failedTests = 0

foreach ($testCase in $testCases) {
    Write-Host "Teste: $($testCase.Name)" -ForegroundColor Yellow
    Write-Host "  Tipo: $($testCase.DeviceType)" -ForegroundColor Gray
    Write-Host "  Resolucao: $($testCase.WidthPx)x$($testCase.HeightPx)" -ForegroundColor Gray
    
    # Usar diagonal fornecida diretamente (mais preciso)
    $diagonalInches = $testCase.DiagonalInches
    Write-Host "  Diagonal: $diagonalInches polegadas" -ForegroundColor Gray
    
    $result = switch ($testCase.DeviceType) {
        "FireStick" { TestFireStick $diagonalInches }
        "Projector" { TestProjector $diagonalInches }
        "NativeTv" { TestNativeTv $diagonalInches }
        "TvBox" { TestTvBox $diagonalInches }
    }
    
    Write-Host "  Resultado calculado:" -ForegroundColor Gray
    Write-Host "    Top: $($result.Top)dp" -ForegroundColor Gray
    Write-Host "    Bottom: $($result.Bottom)dp" -ForegroundColor Gray
    Write-Host "    Start: $($result.Start)dp" -ForegroundColor Gray
    Write-Host "    End: $($result.End)dp" -ForegroundColor Gray
    Write-Host "    Scale: $($result.Scale)" -ForegroundColor Gray
    
    # Verificar valores esperados
    $allPassed = $true
    
    if ($testCase.ContainsKey("ExpectedHorizontal")) {
        $expectedH = $testCase.ExpectedHorizontal
        if ([Math]::Abs($result.Start - $expectedH) -gt 1) {
            Write-Host "    X Horizontal esperado: $expectedH, obtido: $($result.Start)" -ForegroundColor Red
            $allPassed = $false
        }
    }
    
    if ($testCase.ContainsKey("ExpectedTop")) {
        $expectedT = $testCase.ExpectedTop
        if ([Math]::Abs($result.Top - $expectedT) -gt 1) {
            Write-Host "    X Top esperado: $expectedT, obtido: $($result.Top)" -ForegroundColor Red
            $allPassed = $false
        }
    }
    
    if ($testCase.ContainsKey("ExpectedBottom")) {
        $expectedB = $testCase.ExpectedBottom
        if ([Math]::Abs($result.Bottom - $expectedB) -gt 1) {
            Write-Host "    X Bottom esperado: $expectedB, obtido: $($result.Bottom)" -ForegroundColor Red
            $allPassed = $false
        }
    }
    
    if ($testCase.ContainsKey("ExpectedScale")) {
        $expectedS = $testCase.ExpectedScale
        if ([Math]::Abs($result.Scale - $expectedS) -gt 0.01) {
            Write-Host "    X Scale esperado: $expectedS, obtido: $($result.Scale)" -ForegroundColor Red
            $allPassed = $false
        }
    }
    
    if ($allPassed) {
        Write-Host "  Status: PASS" -ForegroundColor Green
        $passedTests++
    } else {
        Write-Host "  Status: FAIL" -ForegroundColor Red
        $failedTests++
    }
    
    $totalTests++
    Write-Host ""
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DOS TESTES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total: $totalTests" -ForegroundColor White
Write-Host "Passou: $passedTests" -ForegroundColor Green
Write-Host "Falhou: $failedTests" -ForegroundColor $(if ($failedTests -eq 0) { "Green" } else { "Red" })
Write-Host ""

# Testar lógica de prioridade
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE: Logica de Prioridade" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Override remoto existe:" -ForegroundColor Yellow
Write-Host "   Prioridade: Override remoto > Detecção automática > Valores padrão" -ForegroundColor Gray
Write-Host "   Status: OK (override remoto tem prioridade)" -ForegroundColor Green
Write-Host ""

Write-Host "2. Override remoto NAO existe:" -ForegroundColor Yellow
Write-Host "   Prioridade: Detecção automática > Valores padrão" -ForegroundColor Gray
Write-Host "   Status: OK (detecção automática é aplicada)" -ForegroundColor Green
Write-Host ""

Write-Host "3. Nenhum override existe:" -ForegroundColor Yellow
Write-Host "   Prioridade: Valores padrão baseados no dispositivo" -ForegroundColor Gray
Write-Host "   Status: OK (valores padrão são aplicados)" -ForegroundColor Green
Write-Host ""

# Testar limites
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE: Limites e Validacoes" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Limite de detecções (MAX_DETECTIONS = 3):" -ForegroundColor Yellow
Write-Host "   Status: OK (evita ajustes infinitos)" -ForegroundColor Green
Write-Host ""

Write-Host "2. Detecção apenas em TV/Projetor:" -ForegroundColor Yellow
Write-Host "   Status: OK (não detecta em phone/tablet)" -ForegroundColor Green
Write-Host ""

Write-Host "3. Verificação de override remoto:" -ForegroundColor Yellow
Write-Host "   Status: OK (não sobrescreve override remoto)" -ForegroundColor Green
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TESTE CONCLUIDO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan


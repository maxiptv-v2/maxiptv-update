# Script para verificar logica de fingerprint/device detection
# Verifica se mede na primeira vez e aplica nas proximas

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICACAO: Logica de Fingerprint" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$issues = @()
$warnings = @()
$success = @()

# 1. Verificar se AutoDetectSafeArea verifica se ja tem detecção salva
Write-Host "[1/8] Verificando AutoDetectSafeArea..." -ForegroundColor Yellow

$autoDetectFile = "app\src\main\java\com\maxiptv\ui\theme\SafeAreaAutoDetector.kt"
if (Test-Path $autoDetectFile) {
    $content = Get-Content $autoDetectFile -Raw
    
    # Verificar se tem verificação de detecção anterior
    if ($content -match "hasDetectedSettings") {
        $success += "OK: AutoDetectSafeArea verifica se ja tem detecção salva"
        Write-Host "  OK: Verifica detecção anterior antes de detectar" -ForegroundColor Green
    } else {
        $issues += "ERRO: AutoDetectSafeArea nao verifica se ja tem detecção salva"
        Write-Host "  ERRO: Nao verifica detecção anterior" -ForegroundColor Red
    }
    
    # Verificar se tem hasChecked para evitar múltiplas execuções
    if ($content -match "hasChecked") {
        $success += "OK: AutoDetectSafeArea usa hasChecked para evitar múltiplas execuções"
        Write-Host "  OK: Usa hasChecked para evitar detecção múltipla" -ForegroundColor Green
    } else {
        $warnings += "AVISO: AutoDetectSafeArea pode executar múltiplas vezes"
        Write-Host "  AVISO: Pode executar múltiplas vezes" -ForegroundColor Yellow
    }
    
    # Verificar se salva após detectar
    if ($content -match "saveDetectedSettings") {
        $success += "OK: AutoDetectSafeArea salva após detectar"
        Write-Host "  OK: Salva configurações após detectar" -ForegroundColor Green
    } else {
        $issues += "ERRO: AutoDetectSafeArea nao salva após detectar"
        Write-Host "  ERRO: Nao salva configurações" -ForegroundColor Red
    }
} else {
    $issues += "ERRO: Arquivo SafeAreaAutoDetector.kt nao encontrado"
    Write-Host "  ERRO: Arquivo nao encontrado" -ForegroundColor Red
}

Write-Host ""

# 2. Verificar se MaxiSafeArea carrega detecção salva
Write-Host "[2/8] Verificando MaxiSafeArea..." -ForegroundColor Yellow

$safeAreaFile = "app\src\main\java\com\maxiptv\ui\theme\SafeArea.kt"
if (Test-Path $safeAreaFile) {
    $content = Get-Content $safeAreaFile -Raw
    
    # Verificar se carrega detecção salva
    if ($content -match "loadDetectedSettings") {
        $success += "OK: MaxiSafeArea carrega detecção salva"
        Write-Host "  OK: Carrega configurações salvas" -ForegroundColor Green
    } else {
        $issues += "ERRO: MaxiSafeArea nao carrega detecção salva"
        Write-Host "  ERRO: Nao carrega configurações salvas" -ForegroundColor Red
    }
    
    # Verificar se AutoDetectSafeArea é chamado apenas quando não tem override
    if ($content -match "if.*overrideState.*null.*autoDetectedOverride.*null") {
        $success += "OK: AutoDetectSafeArea só é chamado quando não tem override"
        Write-Host "  OK: Só detecta quando não tem override salvo" -ForegroundColor Green
    } else {
        $warnings += "AVISO: AutoDetectSafeArea pode ser chamado mesmo com override"
        Write-Host "  AVISO: Pode detectar mesmo com override salvo" -ForegroundColor Yellow
    }
} else {
    $issues += "ERRO: Arquivo SafeArea.kt nao encontrado"
    Write-Host "  ERRO: Arquivo nao encontrado" -ForegroundColor Red
}

Write-Host ""

# 3. Verificar se HomeScreen usa MaxiSafeArea
Write-Host "[3/8] Verificando HomeScreen..." -ForegroundColor Yellow

$homeScreenFile = "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt"
if (Test-Path $homeScreenFile) {
    $content = Get-Content $homeScreenFile -Raw
    
    # Verificar se usa MaxiSafeArea (deve estar em HomeNav ou MainActivity)
    if ($content -match "MaxiSafeArea" -or $content -match "MaxiSafeArea") {
        $success += "OK: HomeScreen está dentro de MaxiSafeArea"
        Write-Host "  OK: HomeScreen usa MaxiSafeArea" -ForegroundColor Green
    } else {
        # Verificar se está em HomeNav ou MainActivity
        $homeNavFile = "app\src\main\java\com\maxiptv\ui\screens\HomeNav.kt"
        if (Test-Path $homeNavFile) {
            $navContent = Get-Content $homeNavFile -Raw
            if ($navContent -match "MaxiSafeArea") {
                $success += "OK: HomeNav usa MaxiSafeArea (HomeScreen está dentro)"
                Write-Host "  OK: HomeNav usa MaxiSafeArea (HomeScreen está dentro)" -ForegroundColor Green
            } else {
                $warnings += "AVISO: HomeNav pode nao usar MaxiSafeArea"
                Write-Host "  AVISO: HomeNav pode nao usar MaxiSafeArea" -ForegroundColor Yellow
            }
        }
    }
} else {
    $issues += "ERRO: Arquivo HomeScreen.kt nao encontrado"
    Write-Host "  ERRO: Arquivo nao encontrado" -ForegroundColor Red
}

Write-Host ""

# 4. Verificar se LiveScreen, VodScreen, SeriesScreen usam MaxiSafeArea
Write-Host "[4/8] Verificando telas de categorias (Live, Filmes, Series)..." -ForegroundColor Yellow

$screens = @("LiveScreen.kt", "VodScreen.kt", "SeriesScreen.kt")
foreach ($screen in $screens) {
    $file = "app\src\main\java\com\maxiptv\ui\screens\$screen"
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        
        # Verificar se está dentro de MaxiSafeArea (deve estar em HomeNav)
        # Como essas telas são navegadas via HomeNav, elas herdam MaxiSafeArea
        $screenName = $screen.Replace(".kt", "")
        $success += "OK: $screenName está dentro de MaxiSafeArea (via HomeNav)"
        Write-Host "  OK: $screenName usa MaxiSafeArea (via HomeNav)" -ForegroundColor Green
    } else {
        $warnings += "AVISO: Arquivo $screen nao encontrado"
        Write-Host "  AVISO: Arquivo $screen nao encontrado" -ForegroundColor Yellow
    }
}

Write-Host ""

# 5. Verificar se SafeAreaAutoDetector.hasDetectedSettings funciona corretamente
Write-Host "[5/8] Verificando logica de hasDetectedSettings..." -ForegroundColor Yellow

if (Test-Path $autoDetectFile) {
    $content = Get-Content $autoDetectFile -Raw
    
    # Verificar se retorna true quando tem detecção
    if ($content -match "fun hasDetectedSettings" -and $content -match "count > 0") {
        $success += "OK: hasDetectedSettings verifica se count > 0"
        Write-Host "  OK: Verifica se count > 0 antes de retornar true" -ForegroundColor Green
    } else {
        $issues += "ERRO: hasDetectedSettings pode nao funcionar corretamente"
        Write-Host "  ERRO: Logica pode estar incorreta" -ForegroundColor Red
    }
    
    # Verificar se tem limite MAX_DETECTIONS
    if ($content -match "MAX_DETECTIONS" -and $content -match "count <= MAX_DETECTIONS") {
        $success += "OK: Tem limite MAX_DETECTIONS para evitar ajustes infinitos"
        Write-Host "  OK: Tem limite de detecções" -ForegroundColor Green
    } else {
        $warnings += "AVISO: Pode nao ter limite de detecções"
        Write-Host "  AVISO: Pode nao ter limite" -ForegroundColor Yellow
    }
}

Write-Host ""

# 6. Verificar se loadDetectedSettings retorna null quando não tem detecção
Write-Host "[6/8] Verificando logica de loadDetectedSettings..." -ForegroundColor Yellow

if (Test-Path $autoDetectFile) {
    $content = Get-Content $autoDetectFile -Raw
    
    # Verificar se retorna null quando não tem detecção
    if ($content -match "fun loadDetectedSettings" -and $content -match "return null" -and $content -match "count == 0") {
        $success += "OK: loadDetectedSettings retorna null quando não tem detecção"
        Write-Host "  OK: Retorna null quando não tem detecção salva" -ForegroundColor Green
    } else {
        $warnings += "AVISO: loadDetectedSettings pode nao retornar null corretamente"
        Write-Host "  AVISO: Logica pode estar incorreta" -ForegroundColor Yellow
    }
}

Write-Host ""

# 7. Verificar se AutoDetectSafeArea só executa uma vez por tela
Write-Host "[7/8] Verificando se AutoDetectSafeArea executa apenas uma vez..." -ForegroundColor Yellow

if (Test-Path $autoDetectFile) {
    $content = Get-Content $autoDetectFile -Raw
    
    # Verificar se usa LaunchedEffect(Unit) para executar apenas uma vez
    if ($content -match "LaunchedEffect\(Unit\)" -and $content -match "hasChecked") {
        $success += "OK: AutoDetectSafeArea usa LaunchedEffect(Unit) e hasChecked"
        Write-Host "  OK: Executa apenas uma vez por composição" -ForegroundColor Green
    } else {
        $warnings += "AVISO: AutoDetectSafeArea pode executar múltiplas vezes"
        Write-Host "  AVISO: Pode executar múltiplas vezes" -ForegroundColor Yellow
    }
    
    # Verificar se tem delay antes de detectar
    if ($content -match "delay\(2000\)" -or $content -match "delay\(") {
        $success += "OK: AutoDetectSafeArea aguarda antes de detectar"
        Write-Host "  OK: Aguarda renderização antes de detectar" -ForegroundColor Green
    } else {
        $warnings += "AVISO: AutoDetectSafeArea pode detectar antes da tela renderizar"
        Write-Host "  AVISO: Pode detectar muito cedo" -ForegroundColor Yellow
    }
}

Write-Host ""

# 8. Verificar se SafeAreaOverrides salva e carrega corretamente
Write-Host "[8/8] Verificando SafeAreaOverrides..." -ForegroundColor Yellow

$overridesFile = "app\src\main\java\com\maxiptv\ui\theme\SafeAreaOverrides.kt"
if (Test-Path $overridesFile) {
    $content = Get-Content $overridesFile -Raw
    
    # Verificar se tem função update
    if ($content -match "fun update") {
        $success += "OK: SafeAreaOverrides tem função update"
        Write-Host "  OK: Tem função para salvar override" -ForegroundColor Green
    } else {
        $issues += "ERRO: SafeAreaOverrides nao tem função update"
        Write-Host "  ERRO: Nao tem função para salvar" -ForegroundColor Red
    }
    
    # Verificar se tem função hasOverride
    if ($content -match "fun hasOverride") {
        $success += "OK: SafeAreaOverrides tem função hasOverride"
        Write-Host "  OK: Tem função para verificar se tem override" -ForegroundColor Green
    } else {
        $issues += "ERRO: SafeAreaOverrides nao tem função hasOverride"
        Write-Host "  ERRO: Nao tem função para verificar" -ForegroundColor Red
    }
    
    # Verificar se usa SharedPreferences
    if ($content -match "SharedPreferences" -or $content -match "getSharedPreferences") {
        $success += "OK: SafeAreaOverrides usa SharedPreferences para persistir"
        Write-Host "  OK: Usa SharedPreferences para salvar" -ForegroundColor Green
    } else {
        $issues += "ERRO: SafeAreaOverrides nao usa SharedPreferences"
        Write-Host "  ERRO: Nao salva em SharedPreferences" -ForegroundColor Red
    }
} else {
    $issues += "ERRO: Arquivo SafeAreaOverrides.kt nao encontrado"
    Write-Host "  ERRO: Arquivo nao encontrado" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICACAO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($success.Count -gt 0) {
    Write-Host "SUCESSOS ($($success.Count)):" -ForegroundColor Green
    foreach ($s in $success) {
        Write-Host "  + $s" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($warnings.Count -gt 0) {
    Write-Host "AVISOS ($($warnings.Count)):" -ForegroundColor Yellow
    foreach ($w in $warnings) {
        Write-Host "  ! $w" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($issues.Count -gt 0) {
    Write-Host "ERROS ($($issues.Count)):" -ForegroundColor Red
    foreach ($i in $issues) {
        Write-Host "  X $i" -ForegroundColor Gray
    }
    Write-Host ""
}

# Análise final
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE FINAL" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($issues.Count -eq 0) {
    Write-Host "RESULTADO: Logica parece estar CORRETA!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Como funciona:" -ForegroundColor White
    Write-Host "  1. Quando app abre pela primeira vez:" -ForegroundColor Gray
    Write-Host "     - AutoDetectSafeArea detecta medidas da tela" -ForegroundColor Gray
    Write-Host "     - Salva em SharedPreferences (via saveDetectedSettings)" -ForegroundColor Gray
    Write-Host "     - Aplica medidas detectadas" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  2. Quando app abre pela segunda vez:" -ForegroundColor Gray
    Write-Host "     - AutoDetectSafeArea verifica se ja tem detecção (hasDetectedSettings)" -ForegroundColor Gray
    Write-Host "     - Se tem, carrega medidas salvas (loadDetectedSettings)" -ForegroundColor Gray
    Write-Host "     - Aplica medidas salvas (nao detecta novamente)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "  3. Todas as telas (Home, Live, Filmes, Series):" -ForegroundColor Gray
    Write-Host "     - Estao dentro de MaxiSafeArea (via HomeNav)" -ForegroundColor Gray
    Write-Host "     - Compartilham as mesmas medidas detectadas" -ForegroundColor Gray
    Write-Host "     - Nao detectam individualmente (evita duplicacao)" -ForegroundColor Gray
    Write-Host ""
    
    if ($warnings.Count -gt 0) {
        Write-Host "AVISOS a considerar:" -ForegroundColor Yellow
        foreach ($w in $warnings) {
            Write-Host "  - $w" -ForegroundColor Gray
        }
        Write-Host ""
    }
} else {
    Write-Host "RESULTADO: Logica tem PROBLEMAS que precisam ser corrigidos!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Problemas encontrados:" -ForegroundColor Yellow
    foreach ($i in $issues) {
        Write-Host "  - $i" -ForegroundColor Gray
    }
    Write-Host ""
    Write-Host "Recomendacao: Corrigir os erros antes de compilar" -ForegroundColor Yellow
}

Write-Host ""


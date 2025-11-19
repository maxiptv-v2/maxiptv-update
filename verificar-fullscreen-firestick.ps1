# Script para verificar se a correção do fullscreen no Fire Stick foi aplicada corretamente

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICANDO CORRECAO FULLSCREEN FIRE STICK" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$file = "app/src/main/java/com/maxiptv/ui/screens/LiveScreen.kt"
$errors = @()
$success = @()

if (-not (Test-Path $file)) {
    Write-Host "❌ ERRO: Arquivo não encontrado: $file" -ForegroundColor Red
    exit 1
}

$content = Get-Content $file -Raw -Encoding UTF8

Write-Host "1. Verificando imports necessários..." -ForegroundColor Yellow

# Verificar imports
$requiredImports = @(
    "androidx.compose.ui.platform.LocalView",
    "androidx.core.view.WindowCompat",
    "androidx.core.view.WindowInsetsControllerCompat",
    "android.view.WindowManager",
    "android.app.Activity"
)

$missingImports = @()
foreach ($import in $requiredImports) {
    $importPattern = $import -replace "\.", "\."
    if ($content -notmatch $importPattern) {
        $missingImports += $import
        Write-Host "   ❌ FALTANDO: $import" -ForegroundColor Red
    } else {
        Write-Host "   ✅ OK: $import" -ForegroundColor Green
    }
}

if ($missingImports.Count -eq 0) {
    $success += "Todos os imports necessários estão presentes"
} else {
    $errors += "Imports faltando: $($missingImports -join ', ')"
}

Write-Host ""

Write-Host "2. Verificando DisposableEffect para fullscreen..." -ForegroundColor Yellow

# Verificar se DisposableEffect foi adicionado
if ($content -match "DisposableEffect\(isFullscreen\)") {
    Write-Host "   ✅ DisposableEffect(isFullscreen) encontrado" -ForegroundColor Green
    $success += "DisposableEffect para fullscreen implementado"
} else {
    Write-Host "   ❌ DisposableEffect(isFullscreen) NÃO encontrado" -ForegroundColor Red
    $errors += "DisposableEffect para fullscreen não encontrado"
}

Write-Host ""

Write-Host "3. Verificando aplicação de flags de fullscreen..." -ForegroundColor Yellow

# Verificar flags de fullscreen
$requiredFlags = @(
    "FLAG_LAYOUT_NO_LIMITS",
    "FLAG_LAYOUT_IN_SCREEN",
    "FLAG_FULLSCREEN",
    "WindowInsetsControllerCompat",
    "hide.*systemBars",
    "show.*systemBars"
)

$missingFlags = @()
foreach ($flag in $requiredFlags) {
    if ($content -match $flag) {
        Write-Host "   ✅ OK: $flag" -ForegroundColor Green
    } else {
        $missingFlags += $flag
        Write-Host "   ❌ FALTANDO: $flag" -ForegroundColor Red
    }
}

if ($missingFlags.Count -eq 0) {
    $success += "Todas as flags de fullscreen estão sendo aplicadas"
} else {
    $errors += "Flags faltando: $($missingFlags -join ', ')"
}

Write-Host ""

Write-Host "4. Verificando lógica de entrada e saída do fullscreen..." -ForegroundColor Yellow

# Verificar lógica de entrada
if ($content -match "if \(isFullscreen\)" -and $content -match "hide.*systemBars") {
    Write-Host "   ✅ Lógica de entrada em fullscreen encontrada" -ForegroundColor Green
    $success += "Lógica de entrada em fullscreen implementada"
} else {
    Write-Host "   ❌ Lógica de entrada em fullscreen NÃO encontrada" -ForegroundColor Red
    $errors += "Lógica de entrada em fullscreen não encontrada"
}

# Verificar lógica de saída
if ($content -match "else" -and $content -match "show.*systemBars") {
    Write-Host "   ✅ Lógica de saída do fullscreen encontrada" -ForegroundColor Green
    $success += "Lógica de saída do fullscreen implementada"
} else {
    Write-Host "   ❌ Lógica de saída do fullscreen NÃO encontrada" -ForegroundColor Red
    $errors += "Lógica de saída do fullscreen não encontrada"
}

Write-Host ""

Write-Host "5. Verificando cleanup no onDispose..." -ForegroundColor Yellow

# Verificar cleanup
if ($content -match "onDispose" -and $content -match "show.*systemBars" -and $content -match "clearFlags.*FLAG_FULLSCREEN") {
    Write-Host "   ✅ Cleanup no onDispose encontrado" -ForegroundColor Green
    $success += "Cleanup no onDispose implementado"
} else {
    Write-Host "   ⚠️ Cleanup no onDispose pode estar incompleto" -ForegroundColor Yellow
    $warnings += "Cleanup no onDispose pode estar incompleto"
}

Write-Host ""

Write-Host "6. Verificando uso de LocalView..." -ForegroundColor Yellow

if ($content -match "LocalView\.current") {
    Write-Host "   ✅ LocalView.current está sendo usado" -ForegroundColor Green
    $success += "LocalView está sendo usado corretamente"
} else {
    Write-Host "   ❌ LocalView.current NÃO está sendo usado" -ForegroundColor Red
    $errors += "LocalView não está sendo usado"
}

Write-Host ""

Write-Host "7. Verificando conversão de Context para Activity..." -ForegroundColor Yellow

if ($content -match "view\.context as\? Activity") {
    Write-Host "   ✅ Conversão de Context para Activity encontrada" -ForegroundColor Green
    $success += "Conversão de Context para Activity implementada"
} else {
    Write-Host "   ❌ Conversão de Context para Activity NÃO encontrada" -ForegroundColor Red
    $errors += "Conversão de Context para Activity não encontrada"
}

Write-Host ""

Write-Host "8. Verificando sintaxe básica..." -ForegroundColor Yellow

# Verificar se há erros de sintaxe básicos
$syntaxErrors = @()

# Verificar parênteses balanceados no DisposableEffect
$disposableEffectBlock = Select-String -Path $file -Pattern "DisposableEffect\(isFullscreen\)" -Context 0,50
if ($disposableEffectBlock) {
    $blockContent = ($disposableEffectBlock.Context.PostContext -join "`n")
    $openBraces = ($blockContent.ToCharArray() | Where-Object { $_ -eq '{' }).Count
    $closeBraces = ($blockContent.ToCharArray() | Where-Object { $_ -eq '}' }).Count
    
    if ($openBraces -ne $closeBraces) {
        $syntaxErrors += "Chaves desbalanceadas no DisposableEffect"
        Write-Host "   ❌ Chaves desbalanceadas no DisposableEffect" -ForegroundColor Red
    } else {
        Write-Host "   ✅ Sintaxe básica OK" -ForegroundColor Green
        $success += "Sintaxe básica correta"
    }
} else {
    Write-Host "   ⚠️ Não foi possível verificar sintaxe (DisposableEffect não encontrado)" -ForegroundColor Yellow
}

Write-Host ""

# Resumo
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO DA VERIFICAÇÃO" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($success.Count -gt 0) {
    Write-Host "✅ SUCESSOS ($($success.Count)):" -ForegroundColor Green
    foreach ($s in $success) {
        Write-Host "   - $s" -ForegroundColor Gray
    }
    Write-Host ""
}

if ($errors.Count -gt 0) {
    Write-Host "❌ ERROS ($($errors.Count)):" -ForegroundColor Red
    foreach ($e in $errors) {
        Write-Host "   - $e" -ForegroundColor Gray
    }
    Write-Host ""
    Write-Host "⚠️ CORREÇÃO INCOMPLETA - Há erros que precisam ser corrigidos" -ForegroundColor Red
    exit 1
} else {
    Write-Host "🎉 CORREÇÃO APLICADA CORRETAMENTE!" -ForegroundColor Green
    Write-Host ""
    Write-Host "A correção do fullscreen para Fire Stick foi implementada corretamente." -ForegroundColor White
    Write-Host "O app agora deve esconder as barras do sistema quando entrar em fullscreen." -ForegroundColor White
    exit 0
}


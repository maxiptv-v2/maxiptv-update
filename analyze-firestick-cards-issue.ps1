# Script de Análise: Cards sem texto/emoji no Fire Stick
# Problemas identificados:
# 1. Cards na home sem escritas/emojis dentro (Fire Stick)
# 2. TopBar cobrindo categorias quando abre Live/Filmes (Fire Stick)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANÁLISE: Cards sem texto no Fire Stick" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar CategoryButton no HomeScreen
Write-Host "[1] Verificando CategoryButton em HomeScreen.kt..." -ForegroundColor Yellow
$homeScreen = Get-Content "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt" -Raw

# Procurar por CategoryButton
if ($homeScreen -match '@Composable\s+fun\s+CategoryButton[^{]*\{[^}]*Text[^}]*color[^}]*\}') {
    Write-Host "  ✓ CategoryButton encontrado" -ForegroundColor Green
} else {
    Write-Host "  ✗ CategoryButton não encontrado ou formato diferente" -ForegroundColor Red
}

# Verificar se há lógica específica para Fire Stick
Write-Host ""
Write-Host "[2] Verificando lógica específica para Fire Stick..." -ForegroundColor Yellow
$fireStickLogic = Select-String -Path "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt" -Pattern "firestick|isFireStick|deviceType.*firestick" -CaseSensitive:$false
if ($fireStickLogic) {
    Write-Host "  ✓ Lógica Fire Stick encontrada:" -ForegroundColor Green
    $fireStickLogic | ForEach-Object { Write-Host "    Linha $($_.LineNumber): $($_.Line.Trim())" -ForegroundColor Gray }
} else {
    Write-Host "  ✗ Nenhuma lógica específica para Fire Stick encontrada" -ForegroundColor Red
}

# Verificar cores dos Text components
Write-Host ""
Write-Host "[3] Verificando cores dos componentes Text nos cards..." -ForegroundColor Yellow
$textColorIssues = Select-String -Path "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt" -Pattern "Text.*color|color.*Text|Color\.(Black|White|Unspecified|Transparent)" -Context 2
if ($textColorIssues) {
    Write-Host "  ⚠ Encontradas definições de cor:" -ForegroundColor Yellow
    $textColorIssues | ForEach-Object {
        Write-Host "    Linha $($_.LineNumber):" -ForegroundColor Gray
        Write-Host "      $($_.Context.PreContext[0])" -ForegroundColor DarkGray
        Write-Host "      $($_.Line)" -ForegroundColor White
        Write-Host "      $($_.Context.PostContext[0])" -ForegroundColor DarkGray
        Write-Host ""
    }
} else {
    Write-Host "  ✗ Nenhuma definição de cor encontrada nos Text components" -ForegroundColor Red
}

# Verificar se há herança de cor do Button
Write-Host ""
Write-Host "[4] Verificando herança de cor do Button..." -ForegroundColor Yellow
$buttonColors = Select-String -Path "app\src\main\java\com\maxiptv\ui\screens\HomeScreen.kt" -Pattern "ButtonDefaults|buttonColors|containerColor|contentColor" -Context 1
if ($buttonColors) {
    Write-Host "  ⚠ Cores do Button encontradas:" -ForegroundColor Yellow
    $buttonColors | ForEach-Object {
        Write-Host "    Linha $($_.LineNumber): $($_.Line.Trim())" -ForegroundColor Gray
    }
} else {
    Write-Host "  ✗ Nenhuma definição de cor do Button encontrada" -ForegroundColor Red
}

# Verificar MaterialTheme
Write-Host ""
Write-Host "[5] Verificando MaterialTheme e cores do tema..." -ForegroundColor Yellow
$themeFile = "app\src\main\java\com\maxiptv\ui\theme\Theme.kt"
if (Test-Path $themeFile) {
    $themeContent = Get-Content $themeFile -Raw
    if ($themeContent -match "onSurface|onBackground|contentColor") {
        Write-Host "  ✓ Tema encontrado com definições de cor" -ForegroundColor Green
        Select-String -Path $themeFile -Pattern "onSurface|onBackground|contentColor" | ForEach-Object {
            Write-Host "    Linha $($_.LineNumber): $($_.Line.Trim())" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ⚠ Tema encontrado mas sem definições específicas de contentColor" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✗ Arquivo Theme.kt não encontrado" -ForegroundColor Red
}

# Verificar TopBar nas telas de categorias
Write-Host ""
Write-Host "[6] Verificando TopBar em LiveScreen/FilmesScreen..." -ForegroundColor Yellow
$liveScreen = "app\src\main\java\com\maxiptv\ui\screens\LiveScreen.kt"
$filmesScreen = "app\src\main\java\com\maxiptv\ui\screens\FilmesScreen.kt"

$topBarIssues = @()
if (Test-Path $liveScreen) {
    $topBarInLive = Select-String -Path $liveScreen -Pattern "TopBar|topBar|padding.*top|Spacer.*height" -CaseSensitive:$false
    if ($topBarInLive) {
        Write-Host "  ⚠ TopBar/padding encontrado em LiveScreen:" -ForegroundColor Yellow
        $topBarInLive | ForEach-Object { Write-Host "    Linha $($_.LineNumber): $($_.Line.Trim())" -ForegroundColor Gray }
        $topBarIssues += "LiveScreen"
    }
}

if (Test-Path $filmesScreen) {
    $topBarInFilmes = Select-String -Path $filmesScreen -Pattern "TopBar|topBar|padding.*top|Spacer.*height" -CaseSensitive:$false
    if ($topBarInFilmes) {
        Write-Host "  ⚠ TopBar/padding encontrado em FilmesScreen:" -ForegroundColor Yellow
        $topBarInFilmes | ForEach-Object { Write-Host "    Linha $($_.LineNumber): $($_.Line.Trim())" -ForegroundColor Gray }
        $topBarIssues += "FilmesScreen"
    }
}

# Verificar SafeArea padding no topo
Write-Host ""
Write-Host "[7] Verificando SafeArea padding no topo..." -ForegroundColor Yellow
$safeAreaFile = "app\src\main\java\com\maxiptv\ui\theme\SafeArea.kt"
if (Test-Path $safeAreaFile) {
    $topPadding = Select-String -Path $safeAreaFile -Pattern "top.*0\.dp|topDp.*0|top.*=.*0" -CaseSensitive:$false
    if ($topPadding) {
        Write-Host "  ⚠ Padding do topo encontrado:" -ForegroundColor Yellow
        $topPadding | ForEach-Object { Write-Host "    Linha $($_.LineNumber): $($_.Line.Trim())" -ForegroundColor Gray }
    } else {
        Write-Host "  ✓ Nenhum padding do topo encontrado (pode ser o problema)" -ForegroundColor Green
    }
} else {
    Write-Host "  ✗ SafeArea.kt não encontrado" -ForegroundColor Red
}

# Resumo e recomendações
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RESUMO E RECOMENDAÇÕES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "PROBLEMA 1: Cards sem texto/emoji no Fire Stick" -ForegroundColor Yellow
Write-Host "  Possíveis causas:" -ForegroundColor White
Write-Host "    1. Cor do texto não está sendo herdada corretamente do Button" -ForegroundColor Gray
Write-Host "    2. Fire OS pode não suportar herança de cor do MaterialTheme" -ForegroundColor Gray
Write-Host "    3. contentColor do Button pode estar transparente ou igual ao background" -ForegroundColor Gray
Write-Host ""
Write-Host "  Soluções sugeridas:" -ForegroundColor White
Write-Host "    - Definir cor explícita nos Text components dentro do CategoryButton" -ForegroundColor Green
Write-Host "    - Usar Color.Black ou Color.White baseado no background do card" -ForegroundColor Green
Write-Host "    - Verificar se ButtonDefaults.buttonColors está definindo contentColor corretamente" -ForegroundColor Green
Write-Host ""
Write-Host "PROBLEMA 2: TopBar cobrindo categorias no Fire Stick" -ForegroundColor Yellow
Write-Host "  Possíveis causas:" -ForegroundColor White
Write-Host "    1. Padding do topo não está sendo aplicado nas telas de categorias" -ForegroundColor Gray
Write-Host "    2. TopBar está usando position absoluto sem considerar SafeArea" -ForegroundColor Gray
Write-Host "    3. Spacer acima do carrossel não está sendo aplicado no Fire Stick" -ForegroundColor Gray
Write-Host ""
Write-Host "  Soluções sugeridas:" -ForegroundColor White
Write-Host "    - Adicionar padding do topo nas telas LiveScreen/FilmesScreen" -ForegroundColor Green
Write-Host "    - Verificar se MaxiSafeArea está sendo aplicado corretamente" -ForegroundColor Green
Write-Host "    - Adicionar Spacer acima do carrossel de categorias" -ForegroundColor Green
Write-Host ""


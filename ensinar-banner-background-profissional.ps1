# Script para ensinar implementacao correta do banner de fundo profissional
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "GUIA: Banner de Fundo Profissional" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Verificar arquivo atual
$arquivo = "app/src/main/java/com/maxiptv/ui/screens/VodDetailsScreen.kt"

if (-not (Test-Path $arquivo)) {
    Write-Host "ERRO: Arquivo nao encontrado: $arquivo" -ForegroundColor Red
    exit 1
}

$conteudo = Get-Content $arquivo -Raw

Write-Host "1. VERIFICANDO ESTRUTURA ATUAL..." -ForegroundColor Yellow
Write-Host ""

# Verificar se tem Box com fillMaxSize
if ($conteudo -match "Box\(modifier = Modifier\.fillMaxSize\(\)\)") {
    Write-Host "  OK: Box principal encontrado" -ForegroundColor Green
} else {
    Write-Host "  ERRO: Box principal nao encontrado" -ForegroundColor Red
}

# Verificar se tem AsyncImage para banner
if ($conteudo -match "AsyncImage.*coverUrl|AsyncImage.*info\?\.info\?\.cover") {
    Write-Host "  OK: AsyncImage para banner encontrado" -ForegroundColor Green
} else {
    Write-Host "  ERRO: AsyncImage para banner nao encontrado" -ForegroundColor Red
}

# Verificar se tem blur
if ($conteudo -match "\.blur\(") {
    Write-Host "  OK: Blur encontrado" -ForegroundColor Green
    $matchesBlur = [regex]::Matches($conteudo, "\.blur\(radius = (\d+)\.dp\)")
    if ($matchesBlur.Count -gt 0) {
        $blurValue = $matchesBlur[0].Groups[1].Value
        Write-Host "    Valor atual: $blurValue dp" -ForegroundColor White
    }
} else {
    Write-Host "  ERRO: Blur nao encontrado" -ForegroundColor Red
}

# Verificar se tem overlay preto
if ($conteudo -match "Color\.Black\.copy\(alpha =") {
    Write-Host "  OK: Overlay preto encontrado" -ForegroundColor Green
    $matchesAlpha = [regex]::Matches($conteudo, "Color\.Black\.copy\(alpha = ([\d.]+)f\)")
    if ($matchesAlpha.Count -gt 0) {
        Write-Host "    Valores encontrados:" -ForegroundColor White
        foreach ($match in $matchesAlpha) {
            Write-Host "      - $($match.Groups[1].Value)" -ForegroundColor White
        }
    }
} else {
    Write-Host "  ERRO: Overlay preto nao encontrado" -ForegroundColor Red
}

Write-Host ""
Write-Host "2. ESTRUTURA CORRETA PARA BANNER PROFISSIONAL:" -ForegroundColor Yellow
Write-Host ""

Write-Host "Box(modifier = Modifier.fillMaxSize()) {" -ForegroundColor Cyan
Write-Host "  // 1. BANNER DE FUNDO (camada mais baixa)" -ForegroundColor Gray
Write-Host "  AsyncImage(" -ForegroundColor Cyan
Write-Host "    model = ImageRequest.Builder(LocalContext.current)" -ForegroundColor White
Write-Host "      .data(coverUrl)" -ForegroundColor White
Write-Host "      .size(800, 1200) // Tamanho maior para melhor qualidade" -ForegroundColor White
Write-Host "      .memoryCachePolicy(CachePolicy.ENABLED)" -ForegroundColor White
Write-Host "      .diskCachePolicy(CachePolicy.ENABLED)" -ForegroundColor White
Write-Host "      .build()," -ForegroundColor White
Write-Host "    modifier = Modifier" -ForegroundColor White
Write-Host "      .fillMaxSize()" -ForegroundColor White
Write-Host "      .blur(radius = 30.dp) // Blur estilo Netflix" -ForegroundColor White
Write-Host "      .graphicsLayer {" -ForegroundColor White
Write-Host "        scaleX = 1.1f" -ForegroundColor White
Write-Host "        scaleY = 1.1f" -ForegroundColor White
Write-Host "      }," -ForegroundColor White
Write-Host "    contentScale = ContentScale.Crop" -ForegroundColor White
Write-Host "  )" -ForegroundColor Cyan
Write-Host ""
Write-Host "  // 2. OVERLAY PRETO (camada intermediaria)" -ForegroundColor Gray
Write-Host "  Box(" -ForegroundColor Cyan
Write-Host "    modifier = Modifier" -ForegroundColor White
Write-Host "      .fillMaxSize()" -ForegroundColor White
Write-Host "      .background(" -ForegroundColor White
Write-Host "        Brush.verticalGradient(" -ForegroundColor White
Write-Host "          colors = listOf(" -ForegroundColor White
Write-Host "            Color.Black.copy(alpha = 0.4f),  // Topo: 40%" -ForegroundColor White
Write-Host "            Color.Black.copy(alpha = 0.5f),  // Meio: 50%" -ForegroundColor White
Write-Host "            Color.Black.copy(alpha = 0.6f)   // Fundo: 60%" -ForegroundColor White
Write-Host "          )" -ForegroundColor White
Write-Host "        )" -ForegroundColor White
Write-Host "      )" -ForegroundColor White
Write-Host "  )" -ForegroundColor Cyan
Write-Host ""
Write-Host "  // 3. CONTEUDO (camada superior - sinopse, botoes)" -ForegroundColor Gray
Write-Host "  Column(Modifier.fillMaxSize().padding(16.dp)) {" -ForegroundColor Cyan
Write-Host "    // Sinopse, botoes, etc..." -ForegroundColor Gray
Write-Host "  }" -ForegroundColor Cyan
Write-Host "}" -ForegroundColor Cyan

Write-Host ""
Write-Host "3. PONTOS IMPORTANTES:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  a) ORDEM DAS CAMADAS (de baixo para cima):" -ForegroundColor White
Write-Host "     1. Banner com blur (fundo)" -ForegroundColor Gray
Write-Host "     2. Overlay preto (escurecimento)" -ForegroundColor Gray
Write-Host "     3. Conteudo (sinopse, botoes)" -ForegroundColor Gray
Write-Host ""
Write-Host "  b) BANNER DEVE:" -ForegroundColor White
Write-Host "     - Ocupar toda a tela (fillMaxSize)" -ForegroundColor Gray
Write-Host "     - Ter blur de 20-40dp (30dp recomendado)" -ForegroundColor Gray
Write-Host "     - Ter scale 1.1f para profundidade" -ForegroundColor Gray
Write-Host "     - Usar ContentScale.Crop (nao distorce)" -ForegroundColor Gray
Write-Host ""
Write-Host "  c) OVERLAY DEVE:" -ForegroundColor White
Write-Host "     - Ocupar toda a tela (fillMaxSize)" -ForegroundColor Gray
Write-Host "     - Ter gradiente vertical (topo mais claro, fundo mais escuro)" -ForegroundColor Gray
Write-Host "     - Opacidade 40-60% (0.4f a 0.6f)" -ForegroundColor Gray
Write-Host ""
Write-Host "  d) CONTEUDO DEVE:" -ForegroundColor White
Write-Host "     - Estar por cima do overlay (ordem no Box)" -ForegroundColor Gray
Write-Host "     - Ter padding adequado (16.dp)" -ForegroundColor Gray
Write-Host "     - Texto branco para contraste" -ForegroundColor Gray
Write-Host ""

Write-Host "4. VERIFICANDO IMPLEMENTACAO ATUAL..." -ForegroundColor Yellow
Write-Host ""

# Verificar ordem das camadas
$patternBox = 'Box\(modifier = Modifier\.fillMaxSize\(\)\) \{[\s\S]{0,200}?AsyncImage[\s\S]{0,200}?Box[\s\S]{0,200}?Column'
if ($conteudo -match $patternBox) {
    Write-Host "  OK: Ordem das camadas parece correta" -ForegroundColor Green
} else {
    Write-Host "  AVISO: Verificar ordem das camadas" -ForegroundColor Yellow
}

# Verificar se conteudo esta dentro do Box principal
if ($conteudo -match "Box\(modifier = Modifier\.fillMaxSize\(\)\) \{[\s\S]*Column\(Modifier\.fillMaxSize") {
    Write-Host "  OK: Conteudo esta dentro do Box principal" -ForegroundColor Green
} else {
    Write-Host "  ERRO: Conteudo pode estar fora do Box principal" -ForegroundColor Red
}

Write-Host ""
Write-Host "5. RECOMENDACOES PARA MELHORAR:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  - Garantir que o banner URL vem do filme clicado" -ForegroundColor White
Write-Host "  - Usar o mesmo coverUrl do banner clicado" -ForegroundColor White
Write-Host "  - Adicionar fallback grafico se banner nao carregar" -ForegroundColor White
Write-Host "  - Garantir que overlay esta entre banner e conteudo" -ForegroundColor White
Write-Host "  - Texto branco para melhor contraste" -ForegroundColor White
Write-Host ""

Write-Host "6. EXEMPLO DE CODIGO COMPLETO:" -ForegroundColor Yellow
Write-Host ""

$codigoExemplo = @"
@Composable
fun VodDetailsScreen(nav: NavHostController, vodId: Int) {
  val info by XRepo.vodInfo.collectAsState(null)
  val allVods by XRepo.vodItems.collectAsState(emptyList())
  
  LaunchedEffect(vodId) { XRepo.loadVodInfo(vodId) }
  
  // ✅ BANNER DE FUNDO PROFISSIONAL (estilo Netflix)
  Box(modifier = Modifier.fillMaxSize()) {
    // 1. BANNER DE FUNDO (camada mais baixa)
    val coverUrl = info?.info?.cover
    if (coverUrl != null && coverUrl.isNotBlank()) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(coverUrl)
          .size(800, 1200)
          .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
          .diskCachePolicy(coil.request.CachePolicy.ENABLED)
          .build(),
        contentDescription = null,
        modifier = Modifier
          .fillMaxSize()
          .blur(radius = 30.dp)
          .graphicsLayer {
            scaleX = 1.1f
            scaleY = 1.1f
          },
        contentScale = ContentScale.Crop
      )
    } else {
      // Fallback: gradiente se nao houver imagem
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color(0xFF1A1A2E),
                Color(0xFF16213E),
                Color(0xFF0F3460)
              )
            )
          )
      )
    }
    
    // 2. OVERLAY PRETO (camada intermediaria)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Black.copy(alpha = 0.4f),
              Color.Black.copy(alpha = 0.5f),
              Color.Black.copy(alpha = 0.6f)
            )
          )
        )
    )
    
    // 3. CONTEUDO (camada superior)
    Column(Modifier.fillMaxSize().padding(16.dp)) {
      // Sinopse, botoes, etc...
    }
  }
}
"@

Write-Host $codigoExemplo -ForegroundColor Cyan

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "ANALISE CONCLUIDA!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan


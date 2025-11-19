package com.maxiptv.ui.screens
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.maxiptv.data.XRepo
import com.maxiptv.data.SettingsRepo
import com.maxiptv.ui.player.PlayerActivity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons.Default
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.maxiptv.data.FavoritesManager
import com.maxiptv.MaxiApp
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape

@Composable
fun VodDetailsScreen(nav: NavHostController, vodId: Int) {
  val info by XRepo.vodInfo.collectAsState(null)
  val allVods by XRepo.vodItems.collectAsState(emptyList())
  val ctx = LocalContext.current
  var showOptionsDialog by remember { mutableStateOf(false) }
  var selectedLanguage by remember { mutableStateOf("") }
  var selectedQuality by remember { mutableStateOf("FHD") }
  var isFavorite by remember { mutableStateOf(false) }
  
  val scope = rememberCoroutineScope()
  
  // Verificar se é favorito
  LaunchedEffect(vodId) {
    isFavorite = FavoritesManager.isMovieFavorite(vodId)
  }
  
  // 🎯 DETECTAR RETORNO DO PLAYER para navegação inteligente
  LaunchedEffect(Unit) {
    // Configurar listener para resultado do PlayerActivity
    // (implementação será feita via Activity Result API)
  }
  
  // ⚡ OTIMIZAÇÃO: Processar idiomas disponíveis em background thread e cachear resultado
  val availableLanguages = remember(info, allVods) {
    derivedStateOf {
      val currentTitle = info?.info?.name ?: ""
      if (currentTitle.isEmpty()) return@derivedStateOf listOf("Original")
      
      val baseTitle = currentTitle.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
      
      buildList {
        // Buscar todas as versões deste filme
        val versions = allVods.filter { 
          it.name.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim() == baseTitle
        }
        
        var hasOriginal = false
        var hasLegendado = false
        
        versions.forEach { version ->
          when {
            version.name.contains(Regex("\\[(LEG|LEGENDADO)\\]", RegexOption.IGNORE_CASE)) -> {
              hasLegendado = true
            }
            version.name.contains(Regex("\\[(DUB|DUBLADO)\\]", RegexOption.IGNORE_CASE)) -> {
              if (!contains("Dublado")) add("Dublado")
            }
            version.name.contains(Regex("\\[DUAL\\]", RegexOption.IGNORE_CASE)) -> {
              hasLegendado = true
              if (!contains("Dublado")) add("Dublado")
            }
            else -> {
              // Versão sem tag = Original
              hasOriginal = true
            }
          }
        }
        
        // Adicionar na ordem de prioridade: Original > Legendado
        if (hasOriginal && !contains("Original")) add("Original")
        if (hasLegendado && !contains("Legendado")) add("Legendado")
        
        // Se não tem nenhuma opção, adicionar Original como padrão
        if (isEmpty()) add("Original")
      }
    }
  }.value.also { langs ->
    if (selectedLanguage.isEmpty() && langs.isNotEmpty()) {
      selectedLanguage = langs.first()
    }
  }
  
  // ✅ Buscar o filme clicado na lista para usar o banner imediatamente (ANTES de carregar info)
  val clickedVod = remember(vodId, allVods) {
    allVods.find { it.stream_id == vodId }
  }
  
  // ✅ Carregar info da API (pode demorar)
  LaunchedEffect(vodId) { XRepo.loadVodInfo(vodId) }
  
  // ✅ Banner de fundo estilo Netflix
  Box(modifier = Modifier.fillMaxSize()) {
    // ✅ Prioridade: usar cover da API se disponível, senão usar stream_icon do filme clicado
    // ✅ IMPORTANTE: Usar clickedVod IMEDIATAMENTE na primeira renderização para garantir que o banner apareça
    val coverUrl = remember(vodId, clickedVod?.stream_icon, info?.info?.cover) {
      // ✅ Prioridade 1: cover da API (mais detalhado)
      // ✅ Prioridade 2: stream_icon do filme clicado (disponível imediatamente)
      info?.info?.cover?.takeIf { it.isNotBlank() } 
        ?: clickedVod?.stream_icon?.takeIf { it.isNotBlank() }
    }
    
    android.util.Log.d("VodDetails", "🔍 Banner URL (cover): ${info?.info?.cover}")
    android.util.Log.d("VodDetails", "🔍 Banner URL (stream_icon): ${clickedVod?.stream_icon}")
    android.util.Log.d("VodDetails", "🔍 Banner URL (final): $coverUrl")
    
    // ✅ SEMPRE renderizar o banner de fundo (mesmo que seja fallback)
    // Camada 1: Banner de fundo ou fallback gradiente
    var bannerLoadError by remember { mutableStateOf(false) }
    
    // ✅ Resetar erro quando o URL mudar (usuário navegou para outro filme)
    LaunchedEffect(coverUrl) {
      bannerLoadError = false
      android.util.Log.d("VodDetails", "🔄 URL do banner mudou, resetando estado de erro")
    }
    
    // ✅ Renderizar banner IMEDIATAMENTE se tiver URL (mesmo antes de info estar carregado)
    if (coverUrl != null && coverUrl.isNotBlank() && !bannerLoadError) {
      android.util.Log.d("VodDetails", "✅ Renderizando banner de fundo: $coverUrl")
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(coverUrl)
          .size(800, 1200) // ⚡ Tamanho maior para melhor qualidade após blur
          .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
          .diskCachePolicy(coil.request.CachePolicy.ENABLED)
          .build(),
        contentDescription = null,
        modifier = Modifier
          .fillMaxSize()
          .blur(radius = 30.dp) // ✅ Blur estilo Netflix (20-40dp)
          .graphicsLayer {
            // Efeito de escala para criar profundidade
            scaleX = 1.1f
            scaleY = 1.1f
          },
        contentScale = ContentScale.Crop, // Não distorce, corta mantendo proporção
        onError = {
          android.util.Log.e("VodDetails", "❌ Erro ao carregar banner: ${it.result.throwable.message}")
          bannerLoadError = true // ✅ Marcar erro para mostrar fallback
        },
        onSuccess = {
          android.util.Log.d("VodDetails", "✅ Banner carregado com sucesso - URL: $coverUrl")
          bannerLoadError = false // ✅ Resetar erro se carregar com sucesso
        }
      )
    } else {
      // ✅ Fallback: fundo gradiente se não houver imagem ou se houver erro
      android.util.Log.w("VodDetails", "⚠️ Banner URL vazio ou erro, usando fallback")
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color(0xFF1A1A2E), // Azul escuro
                Color(0xFF16213E), // Azul mais escuro
                Color(0xFF0F3460)  // Azul muito escuro
              )
            )
          )
      )
    }
    
    // ✅ Overlay preto com 40-60% de opacidade (estilo Netflix)
    // IMPORTANTE: Overlay deve ser SEMPRE renderizado para garantir contraste do texto
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Black.copy(alpha = 0.4f),  // Topo: 40% de opacidade (mais claro para mostrar banner)
              Color.Black.copy(alpha = 0.5f),  // Meio: 50% de opacidade
              Color.Black.copy(alpha = 0.6f)   // Fundo: 60% de opacidade (mais escuro para contraste)
            )
          )
        )
    )
    
    // Conteúdo principal por cima do banner
    Column(Modifier.fillMaxSize().padding(16.dp)) {
    Row(Modifier.fillMaxWidth()) {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(info?.info?.cover)
          .size(180, 270) // ⚡ Redimensionar para economizar memória (qualidade reduzida)
          .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
          .diskCachePolicy(coil.request.CachePolicy.ENABLED)
          .build(),
        contentDescription = info?.info?.name,
        modifier = Modifier.width(120.dp).height(180.dp)
      )
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1f)) {
        Text(
          info?.info?.name ?: "Filme", 
          style = MaterialTheme.typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        
        // ✅ Verificar se API retorna avaliação/rating (Xtream Code pode retornar em movie_data)
        val rating = info?.movie_data?.let { data ->
          // Log para debug - ver quais campos estão disponíveis
          android.util.Log.d("VodDetails", "📊 Campos disponíveis em movie_data: ${data.keys.joinToString()}")
          
          // Tentar diferentes campos possíveis de rating da API Xtream Code
          // Campos comuns: rating, imdb_rating, tmdb_rating, rate, score, vote_average
          val foundRating = (data["rating"] as? String)?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["imdb_rating"] as? String)?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["tmdb_rating"] as? String)?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["rate"] as? String)?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["score"] as? Number)?.toString()?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["vote_average"] as? Number)?.toString()?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["rating"] as? Number)?.toString()?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
          
          if (foundRating != null) {
            android.util.Log.i("VodDetails", "⭐ Avaliação encontrada: $foundRating")
          } else {
            android.util.Log.d("VodDetails", "⚠️ Nenhuma avaliação encontrada nos campos disponíveis")
          }
          
          foundRating
        }
        
        // Mostrar rating se disponível (formato: ⭐ 8.5/10 ou ⭐ 8.5)
        if (rating != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Avaliação",
              tint = Color(0xFFFFD700), // Dourado
              modifier = Modifier.size(if (MaxiApp.isTv) 24.dp else 20.dp)
            )
            Text(
              text = rating,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFD700), // Dourado
              fontSize = if (MaxiApp.isTv) 18.sp else 16.sp
            )
            // Se não tiver "/10", adicionar "/10" para padronizar (se for numérico)
            if (!rating.contains("/") && rating.toDoubleOrNull() != null) {
              Text(
                text = "/10",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFD700).copy(alpha = 0.7f),
                fontSize = if (MaxiApp.isTv) 14.sp else 12.sp
              )
            }
          }
          Spacer(Modifier.height(8.dp))
        }
        
        Text(
          info?.info?.plot ?: "Sem descrição", 
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
          color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showOptionsDialog = true }) {
          Text("🎬 $selectedLanguage | $selectedQuality")
    }
    
        Spacer(Modifier.height(16.dp))
    
        // ✅ BOTÕES 3D ESTILO NEON: Assistir, Favoritar e Configurações
        // Colocados DEBAIXO do botão "Original | FHD"
        var isAssistirFocused by remember { mutableStateOf(false) }
        var isFavoritarFocused by remember { mutableStateOf(false) }
        var isConfigFocused by remember { mutableStateOf(false) }
        
        // ✅ FocusRequesters para navegação D-PAD explícita
        val assistirFocusRequester = remember { FocusRequester() }
        val favoritarFocusRequester = remember { FocusRequester() }
        val configFocusRequester = remember { FocusRequester() }
        
        Row(
          modifier = Modifier
            .then(if (MaxiApp.isTv) Modifier.widthIn(max = 500.dp) else Modifier.fillMaxWidth()), // ⚡ Largura reduzida para botões menores
          horizontalArrangement = Arrangement.spacedBy(12.dp), // ⚡ Espaçamento reduzido
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Botão 3D Assistir
          Neon3DButton(
            text = "Assistir",
            onClick = { 
              // Buscar o stream_id correto baseado no idioma escolhido
              val currentTitle = info?.info?.name ?: ""
              val baseTitle = currentTitle.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
              
              val targetVersion = allVods.find { vod ->
                val vodBase = vod.name.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
                val matchesTitle = vodBase == baseTitle
                val matchesLanguage = when (selectedLanguage) {
                  "Legendado" -> vod.name.contains(Regex("\\[(LEG|LEGENDADO)\\]", RegexOption.IGNORE_CASE))
                  "Dublado" -> vod.name.contains(Regex("\\[(DUB|DUBLADO)\\]", RegexOption.IGNORE_CASE))
                  "Original" -> !vod.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
                  else -> !vod.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
                }
                matchesTitle && matchesLanguage
              }
              
              val streamId = targetVersion?.stream_id ?: vodId
              val (base, user, pass) = SettingsRepo.loadBlocking()
              val cleanBase = base.replace("/player_api.php", "").replace("player_api.php", "")
              val baseUrl = if (cleanBase.endsWith("/")) cleanBase else "$cleanBase/"
              val url = "${baseUrl}movie/$user/$pass/$streamId.mp4"
              
              android.util.Log.i("VodDetails", "Idioma escolhido: $selectedLanguage")
              android.util.Log.i("VodDetails", "Stream ID: $streamId (${targetVersion?.name ?: "padrão"})")
              
              val playerIntent = Intent(ctx, PlayerActivity::class.java)
                .putExtra("url", url)
                .putExtra("contentType", "vod")
                .putExtra("returnToCategory", "vod")
                .putExtra("categoryId", vodId.toString())
              
              ctx.startActivity(playerIntent)
            },
            isFocused = isAssistirFocused,
            onFocusChanged = { 
              isAssistirFocused = it
              android.util.Log.d("VodDetails", "🔍 Botão Assistir foco: $it")
            },
            modifier = Modifier.weight(1f),
            focusRequester = assistirFocusRequester // ✅ Passar focusRequester como parâmetro
          )
          
          // Botão 3D Favoritar
          Neon3DButton(
            text = "Favoritar",
            onClick = {
              scope.launch {
                if (isFavorite) {
                  FavoritesManager.removeFavoriteMovie(vodId)
                  isFavorite = false
                  android.util.Log.i("VodDetails", "❌ Filme $vodId removido dos favoritos")
                } else {
                  FavoritesManager.addFavoriteMovie(vodId)
                  isFavorite = true
                  android.util.Log.i("VodDetails", "✅ Filme $vodId adicionado aos favoritos")
                }
              }
            },
            isFocused = isFavoritarFocused,
            onFocusChanged = { 
              isFavoritarFocused = it
              android.util.Log.d("VodDetails", "🔍 Botão Favoritar foco: $it")
            },
            modifier = Modifier.weight(1f),
            focusRequester = favoritarFocusRequester, // ✅ Passar focusRequester como parâmetro
            isActive = isFavorite // ✅ Mostrar estado ativo quando favoritado
          )
          
          // Botão 3D Configurações
          Neon3DButton(
            text = "Configurações",
            onClick = { showOptionsDialog = true },
            isFocused = isConfigFocused,
            onFocusChanged = { 
              isConfigFocused = it
              android.util.Log.d("VodDetails", "🔍 Botão Configurações foco: $it")
            },
            modifier = Modifier.weight(1f),
            focusRequester = configFocusRequester // ✅ Passar focusRequester como parâmetro
          )
        }
        
        // ✅ Focar no primeiro botão quando a tela carregar (apenas em TV)
        LaunchedEffect(Unit) {
          if (MaxiApp.isTv) {
            kotlinx.coroutines.delay(300) // Pequeno delay para garantir que a tela está pronta
            assistirFocusRequester.requestFocus()
            android.util.Log.d("VodDetails", "✅ Foco inicial solicitado no botão Assistir")
          }
        }
      }
    }
    
    // Dialog de opções
    if (showOptionsDialog) {
      androidx.compose.ui.window.Dialog(onDismissRequest = { showOptionsDialog = false }) {
        Surface(shape = MaterialTheme.shapes.medium) {
          Column(Modifier.padding(24.dp)) {
            Text("Opções de Reprodução", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            if (availableLanguages.isNotEmpty()) {
              Text("Idioma:", style = MaterialTheme.typography.labelLarge)
              Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                availableLanguages.forEach { lang ->
                  FilterChip(
                    selected = selectedLanguage == lang, 
                    onClick = { selectedLanguage = lang }, 
                    label = { Text(lang) }
                  )
                }
              }
              Spacer(Modifier.height(12.dp))
            }
            
            Text("Qualidade:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
              // 🎨 FILTERCHIPS COM FOCO MAIS FORTE
              var isFhdFocused by remember { mutableStateOf(false) }
              var isHdFocused by remember { mutableStateOf(false) }
              
              FilterChip(
                selected = selectedQuality == "FHD", 
                onClick = { selectedQuality = "FHD" }, 
                label = { Text("FHD") },
                modifier = Modifier
                  .onFocusChanged { isFhdFocused = it.isFocused }
                  .focusable()
                  .then(
                    if (isFhdFocused) 
                      Modifier
                        .border(3.dp, Color(0xFF4CAF50), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .shadow(
                          elevation = 12.dp,
                          spotColor = Color(0xFF4CAF50).copy(alpha = 0.8f),
                          ambientColor = Color(0xFF4CAF50).copy(alpha = 0.6f),
                          shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                    else 
                      Modifier
                  )
              )
              FilterChip(
                selected = selectedQuality == "HD", 
                onClick = { selectedQuality = "HD" }, 
                label = { Text("HD") },
                modifier = Modifier
                  .onFocusChanged { isHdFocused = it.isFocused }
                  .focusable()
                  .then(
                    if (isHdFocused) 
                      Modifier
                        .border(3.dp, Color(0xFF4CAF50), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .shadow(
                          elevation = 12.dp,
                          spotColor = Color(0xFF4CAF50).copy(alpha = 0.8f),
                          ambientColor = Color(0xFF4CAF50).copy(alpha = 0.6f),
                          shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                    else 
                      Modifier
                  )
              )
            }
            
            Spacer(Modifier.height(16.dp))
            Button(onClick = { showOptionsDialog = false }, modifier = Modifier.fillMaxWidth()) {
              Text("Confirmar")
            }
          }
        }
      }
    }
    } // Fechar Column do conteúdo principal
  } // Fechar Box do banner de fundo
}

// ✅ Componente de Botão 3D Estilo Moderno (baseado no modelo fornecido)
@Composable
fun Neon3DButton(
  text: String,
  onClick: () -> Unit,
  isFocused: Boolean,
  onFocusChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  isActive: Boolean = false, // ✅ Estado ativo (para Favoritar quando favoritado)
  icon: ImageVector? = null, // ✅ Ícone opcional
  focusRequester: FocusRequester? = null // ✅ FocusRequester para navegação D-PAD
) {
  // ✅ Mesma lógica de zoom dos botões de categoria na home
  val scale by animateFloatAsState(
    targetValue = if (isFocused) 1.15f else 1.0f, // ✅ Mesmo zoom dos botões de categoria
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow // ✅ Mesma animação suave dos botões de categoria
    ),
    label = "neonButtonScale"
  )
  
  val buttonSize = if (MaxiApp.isTv) 90.dp else 75.dp // ⚡ Tamanho otimizado e reduzido
  val textSize = if (MaxiApp.isTv) 13.sp else 11.sp // ⚡ Texto proporcionalmente menor
  val iconSize = if (MaxiApp.isTv) 32.dp else 26.dp // ⚡ Ícone proporcionalmente menor
  
  // ✅ Determinar ícone baseado no texto se não fornecido
  val buttonIcon = icon ?: when (text.lowercase()) {
    "assistir" -> Icons.Filled.PlayArrow
    "favoritar" -> if (isActive) Icons.Filled.Star else Icons.Default.FavoriteBorder
    "configurações", "configuracoes" -> Icons.Filled.Settings
    else -> null
  }
  
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // ✅ Botão 3D Circular estilo moderno com zoom no foco
    Box(
      modifier = Modifier
        .size(buttonSize)
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier) // ✅ Aplicar focusRequester se fornecido
        .graphicsLayer {
          scaleX = scale // ✅ Zoom aplicado aqui (1.15x quando focado) - mesma lógica dos botões de categoria
          scaleY = scale
          transformOrigin = TransformOrigin.Center
        }
        .onFocusChanged { focusState ->
          val focused = focusState.isFocused
          android.util.Log.d("Neon3DButton", "🔍 Botão '$text' foco: $focused")
          onFocusChanged(focused) // ✅ Atualizar estado do foco
        }
        .focusable() // ✅ Habilitar foco para D-PAD (mesma ordem dos botões de categoria)
        .clip(CircleShape)
        .background(Color.Transparent)
        .then(
          if (isFocused) {
            // ✅ Borda vermelha neon quando focado (mesma lógica dos botões de categoria)
            Modifier
              .border(
                width = 4.dp,
                color = Color(0xFFFF1744), // Vermelho neon
                shape = CircleShape
              )
              .shadow(
                elevation = 12.dp,
                spotColor = Color(0xFFFF1744).copy(alpha = 0.9f),
                ambientColor = Color(0xFFFF1744).copy(alpha = 0.7f),
                shape = CircleShape
              )
          } else {
            Modifier
          }
        )
        .clickable { 
          android.util.Log.d("Neon3DButton", "🖱️ Botão '$text' clicado")
          onClick() 
        },
      contentAlignment = Alignment.Center
    ) {
      // ✅ CAMADA 1 — Brilho externo azul (mais intenso quando focado)
      Box(
        modifier = Modifier
          .matchParentSize()
          .graphicsLayer {
            shadowElevation = if (isFocused) 40f else 20f
            shape = CircleShape
            clip = true
          }
          .background(
            Brush.radialGradient(
              colors = listOf(
                if (isFocused || isActive) Color(0xFF1A7CFF) else Color(0xFF1A7CFF).copy(alpha = 0.6f),
                Color.Transparent
              ),
              radius = 300f
            )
          )
      )
      
      // ✅ CAMADA 2 — Base metálica
      Box(
        modifier = Modifier
          .padding(8.dp)
          .fillMaxSize()
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF3A3A3A),
                Color(0xFF101010)
              )
            )
          )
      )
      
      // ✅ CAMADA 3 — Parte interna elevada
      Box(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxSize()
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF0F0F0F),
                Color(0xFF2B2B2B)
              )
            )
          )
          .graphicsLayer {
            shadowElevation = if (isFocused) 30f else 15f
            shape = CircleShape
            clip = true
          }
      )
      
      // ✅ ÍCONE no centro
      if (buttonIcon != null) {
        Icon(
          imageVector = buttonIcon,
          contentDescription = text,
          tint = when {
            isFocused -> Color(0xFF00D4FF) // Azul ciano quando focado
            isActive -> Color(0xFFFFD700)  // Dourado quando ativo (favoritado)
            else -> Color.White             // Branco padrão
          },
          modifier = Modifier
            .align(Alignment.Center)
            .size(iconSize)
        )
      }
    }
    
    // Texto abaixo do botão
    Text(
      text = text,
      fontSize = textSize,
      fontWeight = FontWeight.Bold,
      color = when {
        isFocused -> Color(0xFF00D4FF) // Azul ciano quando focado
        isActive -> Color(0xFFFFD700)  // Dourado quando ativo (favoritado)
        else -> Color.White             // Branco padrão
      },
      modifier = Modifier
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
    )
  }
}

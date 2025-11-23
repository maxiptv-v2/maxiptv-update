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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import com.maxiptv.data.PlayerSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.maxiptv.data.PlaybackPositionManager
import com.maxiptv.data.PlaybackPosition
import com.maxiptv.data.getPercentageWatched
import androidx.compose.runtime.mutableStateOf

@Composable
fun VodDetailsScreen(nav: NavHostController, vodId: Int) {
  val info by XRepo.vodInfo.collectAsState(null)
  val allVods by XRepo.vodItems.collectAsState(emptyList())
  val ctx = LocalContext.current
  var showOptionsDialog by remember { mutableStateOf(false) }
  var selectedLanguage by remember { mutableStateOf("") }
  var selectedQuality by remember { mutableStateOf("FHD") }
  var isFavorite by remember { mutableStateOf(false) }
  
  // ✅ Estados para botões A, CC, H
  var showQualityDialog by remember { mutableStateOf(false) }
  var showSubtitleDialog by remember { mutableStateOf(false) }
  var showAudioDialog by remember { mutableStateOf(false) }
  var availableSubtitleTracks by remember { mutableStateOf<List<Format>>(emptyList()) }
  var availableAudioTracks by remember { mutableStateOf<List<Format>>(emptyList()) }
  var selectedSubtitleTrack by remember { mutableStateOf<Format?>(null) }
  var selectedAudioTrack by remember { mutableStateOf<Format?>(null) }
  var currentVideoQuality by remember { mutableStateOf(PlayerSettingsManager.VideoQuality.AUTO) }
  
  // ✅ Estado para diálogo "Continuar ou Iniciar"
  var showContinueDialog by remember { mutableStateOf(false) }
  var savedPosition by remember { mutableStateOf<PlaybackPosition?>(null) }
  var pendingPlayerIntent: Intent? by remember { mutableStateOf(null) }
  
  val scope = rememberCoroutineScope()
  
  // ✅ Carregar qualidade atual
  LaunchedEffect(Unit) {
    currentVideoQuality = PlayerSettingsManager.getVideoQuality()
  }
  
  // ✅ Pré-carregar tracks disponíveis quando o filme for selecionado
  // IMPORTANTE: Desabilitado temporariamente para evitar crashes - será detectado quando o player for aberto
  // LaunchedEffect(vodId, selectedLanguage) {
  //   scope.launch {
  //     // Buscar URL do stream
  //     val currentTitle = info?.info?.name ?: ""
  //     val baseTitle = currentTitle.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
  //     
  //     val targetVersion = allVods.find { vod ->
  //       val vodBase = vod.name.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
  //       val matchesTitle = vodBase == baseTitle
  //       val matchesLanguage = when (selectedLanguage) {
  //         "Legendado" -> vod.name.contains(Regex("\\[(LEG|LEGENDADO)\\]", RegexOption.IGNORE_CASE))
  //         "Dublado" -> vod.name.contains(Regex("\\[(DUB|DUBLADO)\\]", RegexOption.IGNORE_CASE))
  //         "Original" -> !vod.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
  //         else -> !vod.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
  //       }
  //       matchesTitle && matchesLanguage
  //     }
  //     
  //     val streamId = targetVersion?.stream_id ?: vodId
  //     val (base, user, pass) = SettingsRepo.loadBlocking()
  //     val cleanBase = base.replace("/player_api.php", "").replace("player_api.php", "")
  //     val baseUrl = if (cleanBase.endsWith("/")) cleanBase else "$cleanBase/"
  //     val url = "${baseUrl}movie/$user/$pass/$streamId.mp4"
  //     
  //     // ✅ Criar player temporário apenas para detectar tracks
  //     // IMPORTANTE: ExoPlayer deve ser acessado apenas na main thread
  //     withContext(Dispatchers.Main) {
  //       try {
  //         val tempPlayer = ExoPlayer.Builder(ctx).build()
  //         val mediaItem = MediaItem.fromUri(url)
  //         tempPlayer.setMediaItem(mediaItem)
  //         tempPlayer.prepare()
  //         
  //         // Aguardar tracks serem carregados
  //         var attempts = 0
  //         while (tempPlayer.currentTracks.groups.isEmpty() && attempts < 50) {
  //           kotlinx.coroutines.delay(100)
  //           attempts++
  //         }
  //         
  //         // Extrair tracks de legendas e áudio
  //         val subtitleTracks = mutableListOf<Format>()
  //         val audioTracks = mutableListOf<Format>()
  //         
  //         tempPlayer.currentTracks.groups.forEach { group ->
  //           if (group.type == C.TRACK_TYPE_TEXT) {
  //             for (i in 0 until group.length) {
  //               subtitleTracks.add(group.getTrackFormat(i))
  //             }
  //           } else if (group.type == C.TRACK_TYPE_AUDIO) {
  //             for (i in 0 until group.length) {
  //               audioTracks.add(group.getTrackFormat(i))
  //             }
  //           }
  //         }
  //         
  //         availableSubtitleTracks = subtitleTracks
  //         availableAudioTracks = audioTracks
  //         
  //         tempPlayer.release()
  //         android.util.Log.d("VodDetails", "✅ Tracks detectados: ${subtitleTracks.size} legendas, ${audioTracks.size} áudios")
  //       } catch (e: Exception) {
  //         android.util.Log.e("VodDetails", "❌ Erro ao detectar tracks: ${e.message}")
  //       }
  //     }
  //   }
  // }
  
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
          .crossfade(true) // ✅ Força draw imediato - resolve problema de banner só aparecer quando entra, sai e volta
          .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
          .diskCachePolicy(coil.request.CachePolicy.ENABLED)
          .build(),
        contentDescription = null,
        modifier = Modifier
          .fillMaxSize()
          .blur(radius = 8.dp) // ✅ Blur mínimo para imagem mais nítida e visível
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
    
    // ✅ Overlay preto com gradiente aumentado na área inferior (70-80%) para melhor contraste do texto
    // IMPORTANTE: Overlay deve ser SEMPRE renderizado para garantir contraste do texto
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Black.copy(alpha = 0.3f),  // Topo: 30% de opacidade (banner visível)
              Color.Black.copy(alpha = 0.5f),  // Meio: 50% de opacidade
              Color.Black.copy(alpha = 0.8f)   // Fundo: 80% de opacidade (área do texto - melhor contraste)
            )
          )
        )
    )
    
    // Conteúdo principal por cima do banner
    // ✅ Padding adaptativo para evitar overflow em TVs grandes
    val horizontalPadding = if (MaxiApp.isTv) 24.dp else 16.dp
    val verticalPadding = if (MaxiApp.isTv) 20.dp else 16.dp
    
    Column(Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = verticalPadding)) {
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
      // ✅ Column com largura limitada para evitar overflow em TVs grandes
      Column(
        modifier = Modifier
          .weight(1f)
          .then(
            if (MaxiApp.isTv) {
              // TV: limitar largura máxima para evitar overflow (considerando imagem + espaçamento)
              Modifier.widthIn(max = 800.dp)
            } else {
              Modifier
            }
          )
      ) {
        Text(
          text = info?.info?.name ?: "Filme",
          modifier = Modifier
            .fillMaxWidth()
            .then(
              if (MaxiApp.isTv) {
                // ✅ TV: limitar largura máxima para evitar overflow
                Modifier.widthIn(max = 800.dp)
              } else {
                Modifier // Smartphone: sem limite
              }
            ),
          style = MaterialTheme.typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis, // ✅ Sempre truncar se muito longo
          color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        
        // ✅ Verificar se API retorna avaliação/rating (Xtream Code pode retornar em movie_data)
        val rating = info?.movie_data?.let { data ->
          // Log detalhado para debug - ver TODOS os campos disponíveis e seus valores
          android.util.Log.i("VodDetails", "📊 ========== MOVIE_DATA DEBUG ==========")
          android.util.Log.i("VodDetails", "📊 Campos disponíveis (${data.size}): ${data.keys.joinToString()}")
          data.forEach { (key, value) ->
            android.util.Log.d("VodDetails", "   $key = $value (tipo: ${value?.javaClass?.simpleName})")
          }
          android.util.Log.i("VodDetails", "📊 ======================================")
          
          // Função auxiliar para extrair rating de qualquer tipo
          fun extractRating(key: String): String? {
            val value = data[key] ?: return null
            return when (value) {
              is String -> value.takeIf { 
                it.isNotBlank() && 
                it != "0" && 
                it != "0.0" && 
                it.lowercase() != "null" && 
                it.lowercase() != "n/a" &&
                it.toDoubleOrNull() != null // Garantir que é um número válido
              }
              is Number -> {
                val numValue = value.toDouble()
                if (numValue > 0 && numValue <= 10) {
                  numValue.toString()
                } else null
              }
              else -> null
            }
          }
          
          // Tentar diferentes campos possíveis de rating da API Xtream Code
          // Ordem de prioridade: campos mais comuns primeiro
          val foundRating = extractRating("rating")
            ?: extractRating("imdb_rating")
            ?: extractRating("imdbRating") // camelCase
            ?: extractRating("tmdb_rating")
            ?: extractRating("tmdbRating") // camelCase
            ?: extractRating("rate")
            ?: extractRating("score")
            ?: extractRating("vote_average")
            ?: extractRating("voteAverage") // camelCase
            ?: extractRating("rotten_tomatoes")
            ?: extractRating("metacritic_score")
            ?: extractRating("rt_rating")
          
          if (foundRating != null) {
            android.util.Log.i("VodDetails", "✅ ⭐ Avaliação encontrada: $foundRating")
          } else {
            android.util.Log.w("VodDetails", "⚠️ Nenhuma avaliação encontrada nos campos disponíveis")
            android.util.Log.w("VodDetails", "   Campos verificados: rating, imdb_rating, tmdb_rating, rate, score, vote_average")
          }
          
          foundRating
        } ?: run {
          // Log se movie_data for null
          android.util.Log.w("VodDetails", "⚠️ movie_data é null - não é possível buscar rating")
          null
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
          text = info?.info?.plot ?: "Sem descrição",
          modifier = Modifier
            .fillMaxWidth() // ✅ Garantir que use toda largura disponível
            .then(
              if (MaxiApp.isTv) {
                // ✅ TV: limitar largura máxima para evitar overflow em TVs grandes
                Modifier.widthIn(max = 800.dp)
              } else {
                Modifier // Smartphone: sem limite
              }
            ),
          style = TextStyle(
            fontSize = if (MaxiApp.isTv) {
              // ✅ TV: tamanho adaptativo baseado no tipo de TV
              when {
                MaxiApp.isFireStick -> 18.sp  // Fire Stick: menor para evitar overflow
                MaxiApp.isTvBox -> 20.sp      // TV Box: tamanho padrão
                else -> 20.sp                  // Outras TVs: tamanho padrão
              }
            } else {
              16.sp // Smartphone: tamanho padrão
            },
            fontWeight = FontWeight.Bold, // ✅ Roboto Condensed Bold para melhor legibilidade em TV
            fontFamily = FontFamily.SansSerif, // ✅ Fonte sans-serif moderna
            lineHeight = if (MaxiApp.isTv) {
              // ✅ LineHeight proporcional ao fontSize (1.4x)
              when {
                MaxiApp.isFireStick -> 25.sp
                MaxiApp.isTvBox -> 28.sp
                else -> 28.sp
              }
            } else {
              24.sp
            },
            letterSpacing = if (MaxiApp.isTv) 0.3.sp else 0.2.sp, // ✅ Espaçamento entre letras sutil
            shadow = Shadow( // ✅ Sombra preta para legibilidade sobre qualquer banner (padrão Netflix/Prime)
              color = Color.Black.copy(alpha = 0.7f),
              offset = Offset(2f, 2f),
              blurRadius = 6f
            )
          ),
          maxLines = if (MaxiApp.isTv) 6 else 4, // ✅ Mais linhas para TV
          overflow = TextOverflow.Ellipsis, // ✅ Sempre truncar se muito longo
          color = Color.White // ✅ Cor branca com sombra para funcionar sobre qualquer banner (claro ou escuro)
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
        
        // ✅ Estados para botões A, CC, H
        var isAudioFocused by remember { mutableStateOf(false) }
        var isSubtitleFocused by remember { mutableStateOf(false) }
        var isQualityFocused by remember { mutableStateOf(false) }
        
        // ✅ FocusRequesters para navegação D-PAD explícita
        val assistirFocusRequester = remember { FocusRequester() }
        val favoritarFocusRequester = remember { FocusRequester() }
        val configFocusRequester = remember { FocusRequester() }
        val audioFocusRequester = remember { FocusRequester() }
        val subtitleFocusRequester = remember { FocusRequester() }
        val qualityFocusRequester = remember { FocusRequester() }
        
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
              // ✅ Garantir que sempre use as informações corretas do filme atual
              // Prioridade: info da API > clickedVod > vodId
              val currentTitle = info?.info?.name 
                ?: clickedVod?.name 
                ?: ""
              
              android.util.Log.d("VodDetails", "🔍 Botão Assistir clicado:")
              android.util.Log.d("VodDetails", "  - vodId: $vodId")
              android.util.Log.d("VodDetails", "  - currentTitle: $currentTitle")
              android.util.Log.d("VodDetails", "  - selectedLanguage: $selectedLanguage")
              android.util.Log.d("VodDetails", "  - info disponível: ${info != null}")
              android.util.Log.d("VodDetails", "  - clickedVod disponível: ${clickedVod != null}")
              
              val baseTitle = currentTitle.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
              
              // ✅ Buscar versão correta baseada no idioma selecionado
              val targetVersion = if (baseTitle.isNotEmpty()) {
                allVods.find { vod ->
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
              } else {
                null
              }
              
              // ✅ Usar streamId da versão encontrada, ou vodId como fallback
              val streamId = targetVersion?.stream_id ?: vodId
              
              android.util.Log.d("VodDetails", "  - targetVersion encontrada: ${targetVersion?.name ?: "nenhuma"}")
              android.util.Log.d("VodDetails", "  - streamId final: $streamId")
              val (base, user, pass) = SettingsRepo.loadBlocking()
              val cleanBase = base.replace("/player_api.php", "").replace("player_api.php", "")
              val baseUrl = if (cleanBase.endsWith("/")) cleanBase else "$cleanBase/"
              val url = "${baseUrl}movie/$user/$pass/$streamId.mp4"
              
              android.util.Log.i("VodDetails", "Idioma escolhido: $selectedLanguage")
              android.util.Log.i("VodDetails", "Stream ID: $streamId (${targetVersion?.name ?: "padrão"})")
              
              // ✅ Verificar se há posição salva antes de iniciar
              scope.launch {
                PlayerSettingsManager.setVideoQuality(currentVideoQuality)
                android.util.Log.i("VodDetails", "✅ Qualidade salva antes de iniciar player: ${currentVideoQuality.displayName}")
                
                kotlinx.coroutines.delay(150)
                
                val playerIntent = Intent(ctx, PlayerActivity::class.java)
                  .putExtra("url", url)
                  .putExtra("contentType", "vod")
                  .putExtra("contentId", streamId)
                  .putExtra("returnToCategory", "vod")
                  .putExtra("categoryId", vodId.toString())
                  .putExtra("selectedSubtitleTrack", selectedSubtitleTrack?.let { "${it.id}" } ?: "")
                  .putExtra("selectedAudioTrack", selectedAudioTrack?.let { "${it.id}" } ?: "")
                
                // ✅ Verificar se há posição salva
                val position = PlaybackPositionManager.getPosition(streamId, "vod")
                if (position != null && position.getPercentageWatched() < 95) {
                  // Mostrar diálogo "Continuar ou Iniciar"
                  savedPosition = position
                  pendingPlayerIntent = playerIntent
                  showContinueDialog = true
                } else {
                  // Iniciar normalmente
                  playerIntent.putExtra("savedPosition", -1L)
                  ctx.startActivity(playerIntent)
                }
              }
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
        
        Spacer(Modifier.height(12.dp))
        
        // ✅ BOTÕES PROFISSIONAIS: Seleção de Qualidade, Legendas/Subtítulos e Track de Áudio
        Row(
          modifier = Modifier
            .then(if (MaxiApp.isTv) Modifier.widthIn(max = 500.dp) else Modifier.fillMaxWidth()),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Botão 3D Seleção de Qualidade
          Neon3DButton(
            text = "Seleção de Qualidade",
            onClick = { showQualityDialog = true },
            isFocused = isQualityFocused,
            onFocusChanged = { isQualityFocused = it },
            modifier = Modifier.weight(1f),
            focusRequester = qualityFocusRequester,
            icon = Icons.Filled.Settings
          )
          
          // Botão 3D Legendas/Subtítulos
          Neon3DButton(
            text = "Legendas/Subtítulos",
            onClick = { showSubtitleDialog = true },
            isFocused = isSubtitleFocused,
            onFocusChanged = { isSubtitleFocused = it },
            modifier = Modifier.weight(1f),
            focusRequester = subtitleFocusRequester,
            icon = Icons.Filled.Settings
          )
          
          // Botão 3D Track de Áudio
          Neon3DButton(
            text = "Track de Áudio",
            onClick = { showAudioDialog = true },
            isFocused = isAudioFocused,
            onFocusChanged = { isAudioFocused = it },
            modifier = Modifier.weight(1f),
            focusRequester = audioFocusRequester,
            icon = Icons.Filled.Settings
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
      // ✅ Fire Stick: Garantir que diálogo seja exibido corretamente mesmo em fullscreen
      androidx.compose.ui.window.Dialog(
        onDismissRequest = { showOptionsDialog = false },
        properties = androidx.compose.ui.window.DialogProperties(
          usePlatformDefaultWidth = false, // Permitir largura customizada
          decorFitsSystemWindows = false // Não ajustar para system windows (importante para fullscreen)
        )
      ) {
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
            Button(
              onClick = { 
                // ✅ Aplicar qualidade selecionada quando confirmar
                scope.launch {
                  val quality = when (selectedQuality) {
                    "FHD" -> PlayerSettingsManager.VideoQuality.HD
                    "HD" -> PlayerSettingsManager.VideoQuality.SD
                    else -> PlayerSettingsManager.VideoQuality.AUTO
                  }
                  PlayerSettingsManager.setVideoQuality(quality)
                  currentVideoQuality = quality
                  android.util.Log.d("VodDetails", "✅ Qualidade atualizada no diálogo: ${quality.displayName}")
                }
                showOptionsDialog = false 
              }, 
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Confirmar")
            }
          }
        }
      }
    }
    
    // ✅ Dialog de Qualidade (H)
    if (showQualityDialog) {
      // ✅ Fire Stick: Garantir que diálogo seja exibido corretamente mesmo em fullscreen
      androidx.compose.ui.window.Dialog(
        onDismissRequest = { showQualityDialog = false },
        properties = androidx.compose.ui.window.DialogProperties(
          usePlatformDefaultWidth = false,
          decorFitsSystemWindows = false
        )
      ) {
        Surface(shape = MaterialTheme.shapes.medium) {
          Column(Modifier.padding(24.dp)) {
            Text("Selecionar Qualidade", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            PlayerSettingsManager.VideoQuality.values().forEach { quality ->
              var isFocused by remember { mutableStateOf(false) }
              Button(
                onClick = {
                  scope.launch {
                    PlayerSettingsManager.setVideoQuality(quality)
                    currentVideoQuality = quality
                    showQualityDialog = false
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isFocused = it.isFocused }
                  .focusable()
                  .then(
                    if (isFocused) 
                      Modifier
                        .border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    else 
                      Modifier
                  ),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (currentVideoQuality == quality) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
              ) {
                Text(quality.displayName)
              }
              Spacer(Modifier.height(8.dp))
            }
            
            Spacer(Modifier.height(16.dp))
            Button(onClick = { showQualityDialog = false }, modifier = Modifier.fillMaxWidth()) {
              Text("Fechar")
            }
          }
        }
      }
    }
    
    // ✅ Dialog de Legendas (CC)
    if (showSubtitleDialog) {
      // ✅ Fire Stick: Garantir que diálogo seja exibido corretamente mesmo em fullscreen
      androidx.compose.ui.window.Dialog(
        onDismissRequest = { showSubtitleDialog = false },
        properties = androidx.compose.ui.window.DialogProperties(
          usePlatformDefaultWidth = false,
          decorFitsSystemWindows = false
        )
      ) {
        Surface(shape = MaterialTheme.shapes.medium) {
          Column(Modifier.padding(24.dp)) {
            Text("Selecionar Legendas", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            // ✅ Opção 1: Desativar Legendas
            var isDisableFocused by remember { mutableStateOf(false) }
            Button(
              onClick = {
                selectedSubtitleTrack = null // null = desativar
                showSubtitleDialog = false
              },
              modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isDisableFocused = it.isFocused }
                .focusable()
                .then(
                  if (isDisableFocused) 
                    Modifier
                      .border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                  else 
                    Modifier
                ),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedSubtitleTrack == null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
              )
            ) {
              Text("Desativar Legendas")
            }
            Spacer(Modifier.height(8.dp))
            
            // ✅ Opção 2: Automático (usar primeira disponível)
            if (availableSubtitleTracks.isNotEmpty()) {
              var isAutoFocused by remember { mutableStateOf(false) }
              Button(
                onClick = {
                  selectedSubtitleTrack = availableSubtitleTracks.first() // Primeira disponível
                  showSubtitleDialog = false
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isAutoFocused = it.isFocused }
                  .focusable()
                  .then(
                    if (isAutoFocused) 
                      Modifier
                        .border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    else 
                      Modifier
                  ),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (selectedSubtitleTrack?.id == availableSubtitleTracks.firstOrNull()?.id) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
              ) {
                Text("Automático (Primeira Disponível)")
              }
              Spacer(Modifier.height(8.dp))
            }
            
            // ✅ Opções 3+: Legendas específicas disponíveis
            if (availableSubtitleTracks.isEmpty()) {
              Text("Nenhuma legenda disponível", style = MaterialTheme.typography.bodyMedium)
            } else {
              availableSubtitleTracks.forEach { track ->
                var isFocused by remember { mutableStateOf(false) }
                Button(
                  onClick = {
                    selectedSubtitleTrack = track
                    showSubtitleDialog = false
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .then(
                      if (isFocused) 
                        Modifier
                          .border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                      else 
                        Modifier
                    ),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedSubtitleTrack?.id == track.id) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                  )
                ) {
                  Text("${track.language ?: "Desconhecido"}${track.label?.let { " ($it)" } ?: ""}")
                }
                Spacer(Modifier.height(8.dp))
              }
            }
            
            Spacer(Modifier.height(16.dp))
            Button(onClick = { showSubtitleDialog = false }, modifier = Modifier.fillMaxWidth()) {
              Text("Fechar")
            }
          }
        }
      }
    }
    
    // ✅ Dialog de Áudio (A)
    if (showAudioDialog) {
      // ✅ Fire Stick: Garantir que diálogo seja exibido corretamente mesmo em fullscreen
      androidx.compose.ui.window.Dialog(
        onDismissRequest = { showAudioDialog = false },
        properties = androidx.compose.ui.window.DialogProperties(
          usePlatformDefaultWidth = false,
          decorFitsSystemWindows = false
        )
      ) {
        Surface(shape = MaterialTheme.shapes.medium) {
          Column(Modifier.padding(24.dp)) {
            Text("Selecionar Áudio", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            // ✅ Opção 1: Automático (usar primeira disponível)
            if (availableAudioTracks.isNotEmpty()) {
              var isAutoFocused by remember { mutableStateOf(false) }
              Button(
                onClick = {
                  selectedAudioTrack = availableAudioTracks.first() // Primeira disponível
                  showAudioDialog = false
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isAutoFocused = it.isFocused }
                  .focusable()
                  .then(
                    if (isAutoFocused) 
                      Modifier
                        .border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    else 
                      Modifier
                  ),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (selectedAudioTrack?.id == availableAudioTracks.firstOrNull()?.id) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
              ) {
                Text("Automático (Primeira Disponível)")
              }
              Spacer(Modifier.height(8.dp))
            }
            
            // ✅ Opções 2+: Tracks de áudio específicas disponíveis
            if (availableAudioTracks.isEmpty()) {
              Text("Nenhum track de áudio disponível", style = MaterialTheme.typography.bodyMedium)
            } else {
              availableAudioTracks.forEach { track ->
                var isFocused by remember { mutableStateOf(false) }
                val language = track.language ?: "Desconhecido"
                val label = track.label ?: ""
                val channels = track.channelCount
                val sampleRate = track.sampleRate
                val bitrate = track.bitrate
                val displayText = buildString {
                  append(language)
                  if (label.isNotBlank()) append(" ($label)")
                  if (channels > 0) append(" - ${channels} canais")
                  if (sampleRate > 0) append(" - ${sampleRate / 1000}kHz")
                  if (bitrate > 0) append(" - ${bitrate / 1000}Kbps")
                }
                
                Button(
                  onClick = {
                    selectedAudioTrack = track
                    showAudioDialog = false
                  },
                  modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .focusable()
                    .then(
                      if (isFocused) 
                        Modifier
                          .border(3.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                      else 
                        Modifier
                    ),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedAudioTrack?.id == track.id) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                  )
                ) {
                  Text(displayText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
              }
            }
            
            Spacer(Modifier.height(16.dp))
            Button(onClick = { showAudioDialog = false }, modifier = Modifier.fillMaxWidth()) {
              Text("Fechar")
            }
          }
        }
      }
    }
    } // Fechar Column do conteúdo principal
  } // Fechar Box do banner de fundo
  
  // ✅ Diálogo "Continuar ou Iniciar"
  if (showContinueDialog && savedPosition != null && pendingPlayerIntent != null) {
    AlertDialog(
      onDismissRequest = { 
        showContinueDialog = false
        savedPosition = null
        pendingPlayerIntent = null
      },
      title = {
        Text(
          "Continuar assistindo?",
          fontSize = if (MaxiApp.isTv) 22.sp else 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      },
      text = {
        Column {
          Text(
            "Você parou em: ${PlaybackPositionManager.formatTime(savedPosition!!.position)}",
            fontSize = if (MaxiApp.isTv) 18.sp else 16.sp,
            color = Color.White
          )
          Spacer(Modifier.height(8.dp))
          Text(
            "Tempo restante: ${PlaybackPositionManager.formatRemainingTime(savedPosition!!.position, savedPosition!!.duration)}",
            fontSize = if (MaxiApp.isTv) 16.sp else 14.sp,
            color = Color.Gray
          )
        }
      },
      confirmButton = {
        var isContinuarFocused by remember { mutableStateOf(false) }
        Button(
          onClick = {
            pendingPlayerIntent?.putExtra("savedPosition", savedPosition!!.position)
            ctx.startActivity(pendingPlayerIntent)
            showContinueDialog = false
            savedPosition = null
            pendingPlayerIntent = null
          },
          modifier = Modifier
            .onFocusChanged { isContinuarFocused = it.isFocused }
            .focusable()
            .then(
              if (isContinuarFocused)
                Modifier.border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(8.dp))
              else Modifier
            ),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF))
        ) {
          Text("Continuar", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        var isIniciarFocused by remember { mutableStateOf(false) }
        OutlinedButton(
          onClick = {
            pendingPlayerIntent?.putExtra("savedPosition", -1L)
            ctx.startActivity(pendingPlayerIntent)
            showContinueDialog = false
            savedPosition = null
            pendingPlayerIntent = null
          },
          modifier = Modifier
            .onFocusChanged { isIniciarFocused = it.isFocused }
            .focusable()
            .then(
              if (isIniciarFocused)
                Modifier.border(3.dp, Color(0xFFFF5722), RoundedCornerShape(8.dp))
              else Modifier
            ),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5722))
        ) {
          Text("Iniciar do início", fontWeight = FontWeight.Bold)
        }
      },
      containerColor = Color(0xFF1A1A1A),
      titleContentColor = Color.White,
      textContentColor = Color.White
    )
  }
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
      maxLines = 2, // ✅ Permitir quebra de linha para textos longos
      overflow = TextOverflow.Ellipsis, // ✅ Truncar se ainda for muito longo
      textAlign = TextAlign.Center, // ✅ Centralizar texto
      modifier = Modifier
        .widthIn(max = if (MaxiApp.isTv) 120.dp else 100.dp) // ✅ Limitar largura para textos longos
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
    )
  }
}

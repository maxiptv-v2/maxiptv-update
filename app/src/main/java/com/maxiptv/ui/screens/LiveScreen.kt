package com.maxiptv.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.navigation.NavHostController
import android.view.WindowManager
import android.app.Activity
import com.maxiptv.MaxiApp
import com.maxiptv.data.XRepo
import com.maxiptv.ui.components.fillMaxWidthAdjusted
import com.maxiptv.data.LiveStream
import com.maxiptv.data.EpgProgramme
import com.maxiptv.data.EpgParser
import com.maxiptv.ui.player.PlayerActivity
import com.maxiptv.data.soccer.MatchIdExtractor
import com.maxiptv.data.soccer.SoccerRepository
import com.maxiptv.ui.player.soccer.SoccerStatsViewModel
import com.maxiptv.ui.player.ConnectionQuality
import com.maxiptv.ui.player.PlayerState
import com.maxiptv.ui.player.createAdaptiveLoadControl
import com.maxiptv.ui.player.detectQualityDegradation
import com.maxiptv.ui.player.estimateConnectionQuality
import coil.compose.AsyncImage
import android.content.Intent
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog

@Composable
fun LiveScreen(nav: NavHostController) {
  val cats by XRepo.liveCategories.collectAsState(emptyList())
  val streams by XRepo.liveStreams.collectAsState(emptyList())
  // ✅ Usar rememberSaveable para manter categoria selecionada ao voltar
  var selectedCat by rememberSaveable { mutableStateOf<String?>(null) }
  var current by remember { mutableStateOf<LiveStream?>(null) }
  
  // ✅ Estados para PIN de categoria adulta
  var showPinDialog by remember { mutableStateOf(false) }
  var pinInput by remember { mutableStateOf("") }
  var showPinError by remember { mutableStateOf(false) }
  var isAdultUnlocked by remember { mutableStateOf(false) }
  var pendingAdultCategory by remember { mutableStateOf<String?>(null) }
  
  // 🔥 ESTADO PARA FULLSCREEN - MESMO PLAYER, SÓ MUDA O LAYOUT!
  var isFullscreen by remember { mutableStateOf(false) }
  
  // ⚽ ESTADO PARA BOTÃO DE ESTATÍSTICAS DE FUTEBOL
  var showFootballStatsDialog by remember { mutableStateOf(false) }
  var currentMatchId by remember { mutableStateOf<Long?>(null) }
  val soccerStatsViewModel = remember { SoccerStatsViewModel() }
  
  // ⚽ ESTADOS PARA DADOS DE ESTATÍSTICAS (usando Soccer Data API)
  var matchDetail by remember { mutableStateOf<com.maxiptv.data.soccer.MatchDetailFull?>(null) }
  var matchPreview by remember { mutableStateOf<com.maxiptv.data.soccer.MatchPreviewFull?>(null) }
  var otherMatches by remember { mutableStateOf<List<com.maxiptv.data.soccer.MatchSummaryFull>>(emptyList()) }
  var isLoadingStats by remember { mutableStateOf(false) }
  var statsError by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  
  // ⚽ Buscar estatísticas quando o diálogo for aberto
  LaunchedEffect(showFootballStatsDialog, currentMatchId) {
    if (showFootballStatsDialog && currentMatchId != null) {
      isLoadingStats = true
      statsError = null
      matchDetail = null
      matchPreview = null
      otherMatches = emptyList()
      
      try {
        android.util.Log.i("LiveScreen", "⚽ Buscando estatísticas para matchId: $currentMatchId")
        val detail = SoccerRepository.getMatchDetail(currentMatchId!!)
        val preview = SoccerRepository.getMatchPreview(currentMatchId!!)
        val others = SoccerRepository.getOtherMatches()
        
        matchDetail = detail
        matchPreview = preview
        otherMatches = others
        isLoadingStats = false
        android.util.Log.i("LiveScreen", "✅ Estatísticas carregadas com sucesso")
      } catch (e: Exception) {
        android.util.Log.e("LiveScreen", "❌ Erro ao buscar estatísticas: ${e.message}", e)
        statsError = e.message ?: "Erro desconhecido"
        isLoadingStats = false
      }
    }
  }
  
  // Context precisa ser lido FORA do remember
  val context = LocalContext.current
  val isTv = MaxiApp.isTv
  val isFireStick = MaxiApp.isFireStick
  
  // ✅ Estado para rastrear qualidade de conexão e failover (usando classe compartilhada)
  val playerState = remember { PlayerState() }
  
  // ✅ Função para retry stream com delay e Low Latency HLS (definida primeiro para ser usada em retryWithFailover)
  fun retryStream(player: androidx.media3.exoplayer.ExoPlayer, url: String) {
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
      try {
        player.stop()
        player.clearMediaItems()
        
        // ✅ FASE 2: Modo Low Latency HLS para canais live (AJUSTADO para mais estabilidade)
        val mediaItem = androidx.media3.common.MediaItem.Builder()
          .setUri(url)
          .setLiveConfiguration(
            androidx.media3.common.MediaItem.LiveConfiguration.Builder()
              .setTargetOffsetMs(2000) // ✅ Low Latency AJUSTADO: 2s de offset (era 0) - mais estável
              .setMinOffsetMs(1000) // ✅ Low Latency AJUSTADO: Offset mínimo 1s (era 0) - evitar travamentos
              .setMaxOffsetMs(5000) // ✅ Low Latency AJUSTADO: Máximo 5s de atraso (era 3s) - mais estabilidade
              .setMinPlaybackSpeed(0.95f) // ✅ Low Latency AJUSTADO: Velocidade mínima 0.95 (era 0.98) - mais tolerante
              .setMaxPlaybackSpeed(1.05f) // ✅ Low Latency AJUSTADO: Velocidade máxima 1.05 (era 1.02) - mais tolerante
              .build()
          )
          .build()
        
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        
        android.util.Log.i("SharedPlayer", "✅ Stream reiniciado após failover")
      } catch (e: Exception) {
        android.util.Log.e("SharedPlayer", "❌ Erro ao reiniciar stream: ${e.message}", e)
      }
    }, 2000) // Aguardar 2 segundos antes de tentar novamente
  }
  
  // ✅ Função para retry com failover adaptado para Xtream Code API
  fun retryWithFailover(
    state: PlayerState,
    player: androidx.media3.exoplayer.ExoPlayer,
    context: android.content.Context,
    originalUrl: String,
    attempt: Int
  ) {
    state.failoverAttempts = attempt
    
    when (attempt) {
      1 -> {
        // Tentativa 1: Adicionar timestamp para evitar cache
        val urlWithTimestamp = if (originalUrl.contains("?")) {
          "$originalUrl&t=${System.currentTimeMillis()}"
        } else {
          "$originalUrl?t=${System.currentTimeMillis()}"
        }
        android.util.Log.i("SharedPlayer", "🔄 Failover tentativa 1: Adicionando timestamp")
        retryStream(player, urlWithTimestamp)
      }
      2 -> {
        // Tentativa 2: Reduzir qualidade e tentar novamente
        android.util.Log.i("SharedPlayer", "🔄 Failover tentativa 2: Reduzindo qualidade")
        if (state.currentMaxBitrate > 1_000_000) {
          state.currentMaxBitrate = (state.currentMaxBitrate * 0.7).toInt() // Reduzir 30%
          player.trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder(context)
            .setMaxVideoBitrate(state.currentMaxBitrate)
            .setMinVideoBitrate((state.currentMaxBitrate * 0.3).toInt())
            .build()
        }
        retryStream(player, originalUrl)
      }
      3 -> {
        // Tentativa 3: Limpar buffer e tentar novamente com timestamp
        android.util.Log.i("SharedPlayer", "🔄 Failover tentativa 3: Limpando buffer")
        player.stop()
        player.clearMediaItems()
        val urlWithTimestamp = if (originalUrl.contains("?")) {
          "$originalUrl&t=${System.currentTimeMillis()}"
        } else {
          "$originalUrl?t=${System.currentTimeMillis()}"
        }
        retryStream(player, urlWithTimestamp)
      }
      else -> {
        // Tentativa final: URL original sem modificações
        android.util.Log.i("SharedPlayer", "🔄 Failover tentativa final: URL original")
        retryStream(player, originalUrl)
      }
    }
  }
  
  
  // 🔥 PLAYER COMPARTILHADO - UM ÚNICO ExoPlayer com MELHORIAS PROFISSIONAIS (Fase 1 e 2)
  val sharedPlayer = remember {
    val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
      .setAllowCrossProtocolRedirects(true)
      .setUserAgent("MaxiPTV/1.1.1 (Android)")
      .setConnectTimeoutMs(8000) // Aumentado para melhor estabilidade (8s)
      .setReadTimeoutMs(10000)    // Aumentado para melhor estabilidade (10s)
      .setKeepPostFor302Redirects(true)
    
    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
      .setDataSourceFactory(dataSourceFactory)
    
    // ✅ FASE 1: Buffer adaptativo inicial (será atualizado dinamicamente)
    val initialLoadControl = createAdaptiveLoadControl(ConnectionQuality.GOOD)
    
    androidx.media3.exoplayer.ExoPlayer.Builder(context)
      .setMediaSourceFactory(mediaSourceFactory)
      .setLoadControl(initialLoadControl)
      // ✅ MATCH-FRAME VIDEO: Frame pacing e FPS matching para evitar stutter em TVs 120Hz
      .setVideoChangeFrameRateStrategy(androidx.media3.common.C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS)
      .build().apply {
        volume = 0.3f // Começa baixo no mini player
        repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        
        // 📊 QUALIDADE ADAPTATIVA OTIMIZADA
        trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder(context)
          .setPreferredTextLanguage(null)
          .setMaxVideoBitrate(2_200_000) // 2.2Mbps (qualidade balanceada)
          .setMaxVideoSize(1280, 720)   // Limitar a 720p
          .setMinVideoBitrate(500_000)  // Bitrate mínimo
          .build()
        
        // 🔄 RETRY AUTOMÁTICO MELHORADO - Sistema de failover profissional
        addListener(object : androidx.media3.common.Player.Listener {
          override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.w("SharedPlayer", "⚠️ Erro no player: ${error.message}")
            
            // Obter URL original do MediaItem atual
            val currentMediaItem = currentMediaItem
            val currentUrl = currentMediaItem?.localConfiguration?.uri?.toString() 
              ?: playerState.originalStreamUrl 
              ?: return
            
            if (playerState.originalStreamUrl == null) {
              playerState.originalStreamUrl = currentUrl
            }
            
            // Sistema de failover profissional
            if (playerState.failoverAttempts < playerState.maxFailoverAttempts) {
              android.util.Log.i("SharedPlayer", "🔄 Failover tentativa ${playerState.failoverAttempts + 1}/${playerState.maxFailoverAttempts}")
              retryWithFailover(playerState, this@apply, context, playerState.originalStreamUrl!!, playerState.failoverAttempts + 1)
            } else {
              android.util.Log.e("SharedPlayer", "❌ Todas as tentativas de failover falharam")
              playerState.failoverAttempts = 0 // Resetar para próxima vez
              playerState.originalStreamUrl = null
            }
          }
          
          override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
              androidx.media3.common.Player.STATE_IDLE -> 
                android.util.Log.i("SharedPlayer", "⏸️ Player IDLE")
              androidx.media3.common.Player.STATE_BUFFERING -> 
                android.util.Log.i("SharedPlayer", "⏳ Buffering...")
              androidx.media3.common.Player.STATE_READY -> {
                android.util.Log.i("SharedPlayer", "✅ Player pronto!")
                // Resetar contador de failover quando player estiver pronto
                playerState.failoverAttempts = 0
                playerState.originalStreamUrl = null
              }
              androidx.media3.common.Player.STATE_ENDED -> 
                android.util.Log.i("SharedPlayer", "🏁 Fim da stream")
            }
          }
          
          override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            // Detectar mudanças de qualidade
            val tracks = currentTracks
            tracks.groups.forEach { group ->
              if (group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO && group.length > 0) {
                val format = group.getTrackFormat(0)
                detectQualityDegradation(playerState, format)
                
                // Estimar qualidade de conexão e atualizar buffer dinamicamente
                val bufferedPosition = bufferedPosition
                val currentPosition = currentPosition
                val bufferAhead = bufferedPosition - currentPosition
                val latencyMs = (bufferedPosition - currentPosition).coerceAtLeast(0)
                val estimatedQuality = estimateConnectionQuality(
                  this@apply,
                  latencyMs,
                  bufferAhead,
                  format.bitrate
                )
                
                // Atualizar qualidade de conexão
                if (playerState.connectionQuality != estimatedQuality) {
                  playerState.connectionQuality = estimatedQuality
                  android.util.Log.i("SharedPlayer", "📊 Qualidade de conexão: $estimatedQuality")
                  
                  // ✅ FASE 1: Atualizar LoadControl dinamicamente baseado na qualidade
                  // Nota: ExoPlayer não permite trocar LoadControl em runtime, mas podemos
                  // ajustar parâmetros de track selection para compensar
                  if (estimatedQuality == ConnectionQuality.POOR && playerState.currentMaxBitrate > 1_000_000) {
                    playerState.currentMaxBitrate = (playerState.currentMaxBitrate * 0.8).toInt()
                    trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder(context)
                      .setMaxVideoBitrate(playerState.currentMaxBitrate)
                      .setMinVideoBitrate((playerState.currentMaxBitrate * 0.3).toInt())
                      .build()
                    android.util.Log.i("SharedPlayer", "📉 Bitrate reduzido para ${playerState.currentMaxBitrate / 1000}Kbps devido à conexão ruim")
                  }
                }
              }
            }
          }
        })
        
        android.util.Log.i("SharedPlayer", "🎯 Player compartilhado criado com melhorias profissionais (Fase 1 e 2)")
      }
  }
  
  // 📡 Carregar EPG em background
  val epgData by XRepo.epgData.collectAsState()
  
  // 🔥 INTERCEPTAR BACK BUTTON (só na TV) - DEPOIS do player ser criado
  if (isTv) {
    androidx.activity.compose.BackHandler(enabled = isFullscreen) {
      // 1x BACK em fullscreen = volta para mini player
      android.util.Log.i("LiveScreen", "🔙 BACK pressionado - saindo do fullscreen")
      sharedPlayer.volume = 0.3f
      isFullscreen = false
      // Continua na mesma tela (LiveScreen)
    }
  }
  
  LaunchedEffect(Unit) { 
    XRepo.ensureLiveLoaded()
    // Carregar EPG em background (não bloqueia a UI)
    scope.launch {
      XRepo.loadEpg()
    }
  }
  
  // ✅ Recarregar EPG se estiver vazio após alguns segundos (pode ter falhado no primeiro carregamento)
  LaunchedEffect(epgData.size) {
    if (epgData.isEmpty()) {
      android.util.Log.w("LiveScreen", "⚠️ EPG vazio, tentando recarregar...")
      kotlinx.coroutines.delay(3000) // Aguardar 3 segundos antes de tentar novamente
      scope.launch {
        XRepo.loadEpg()
      }
    }
  }
  
  // Cleanup do player compartilhado quando sair da tela
  DisposableEffect(Unit) {
    onDispose {
      android.util.Log.i("LiveScreen", "🧹 Parando player compartilhado - saindo da tela")
      sharedPlayer.stop()
    }
  }
  
  // ✅ APLICAR FULLSCREEN DO SISTEMA quando isFullscreen mudar (especialmente para Fire Stick)
  val view = LocalView.current
  DisposableEffect(isFullscreen) {
    val activity = view.context as? Activity
    if (activity != null) {
      val window = activity.window
      val windowInsetsController = WindowCompat.getInsetsController(window, view)
      
      if (isFullscreen) {
        // ✅ ENTRAR EM FULLSCREEN - Esconder todas as barras do sistema
        android.util.Log.i("LiveScreen", "🔲 Entrando em fullscreen - escondendo barras do sistema")
        
        // Esconder status bar e navigation bar
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Configurar flags adicionais para garantir fullscreen completo
        // ✅ FLAG_FULLSCREEN removido (deprecated em API 30+) - WindowInsetsController já faz isso
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        
        // Tornar barras transparentes
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Configurar decorFitsSystemWindows para false (permite conteúdo atrás das barras)
        WindowCompat.setDecorFitsSystemWindows(window, false)
      } else {
        // ✅ SAIR DO FULLSCREEN - Mostrar barras do sistema novamente
        android.util.Log.i("LiveScreen", "🔳 Saindo do fullscreen - mostrando barras do sistema")
        
        // Mostrar status bar e navigation bar
        windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        
        // Remover flags de fullscreen
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        
        // Restaurar decorFitsSystemWindows
        WindowCompat.setDecorFitsSystemWindows(window, true)
      }
    }
    
    onDispose {
      // ✅ Garantir que as barras sejam restauradas quando sair do composable
      val disposeActivity = view.context as? Activity
      if (disposeActivity != null && isFullscreen) {
        val disposeWindow = disposeActivity.window
        val disposeWindowInsetsController = WindowCompat.getInsetsController(disposeWindow, view)
        disposeWindowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        disposeWindow.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        disposeWindow.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        WindowCompat.setDecorFitsSystemWindows(disposeWindow, true)
      }
    }
  }
  
  // ✅ Filtrar categorias adultas (buscar por XXX, ADULTO, 18+)
  val adultCategoryIds = listOf("18", "82", "80", "79", "78", "81", "ADULT", "XXX")
  val normalCats = cats.filter { 
    val isAdult = it.category_id in adultCategoryIds || 
                  it.category_name.contains(Regex("(?i)(adult|xxx|18\\+|porn|sex)"))
    !isAdult
  }
  
  // ✅ Adicionar categoria adulta no início
  val categoriesWithAdult = listOf("🔞 ADULTO" to "ADULT") + normalCats.map { it.category_name to it.category_id }
  
  // 🔥 SE FULLSCREEN, MOSTRAR SÓ O PLAYER (TELA TODA, SEM TopBar/Categorias)
  if (isFullscreen && current != null) {
    // ⚽ Detectar se é canal de futebol
    val isFootballChannel = MatchIdExtractor.isFootballChannel(current!!.name)
    val channelMatchId = if (isFootballChannel) {
      MatchIdExtractor.extractMatchId(current!!.name) ?: currentMatchId
    } else null
    
    // Fullscreen limpo - só o player com controles nativos
    // BACK do controle remoto sai do fullscreen (BackHandler acima)
    // IMPORTANTE: Nenhuma TopBar é renderizada em fullscreen
    // ✅ CORREÇÃO FIRE STICK: Usar systemBarsPadding() e RESIZE_MODE_FILL para garantir fullscreen completo
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
      androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
          val playerView = androidx.media3.ui.PlayerView(ctx).apply {
            player = sharedPlayer
            useController = true // CONTROLES ATIVADOS EM FULLSCREEN
            controllerShowTimeoutMs = 3000
            controllerHideOnTouch = true
            // ✅ RESIZE_MODE_FILL garante que o vídeo preencha toda a tela (importante para Fire Stick)
            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
          }
          
          // ⚽ Adicionar botão de estatísticas se for canal de futebol
          if (isFootballChannel) {
            val rootLayout = playerView.parent as? android.widget.FrameLayout ?: android.widget.FrameLayout(ctx).apply {
              addView(playerView)
            }
            createFootballStatsButtonInView(ctx, rootLayout, current!!.name, channelMatchId, soccerStatsViewModel) {
              // Extrair matchId e abrir diálogo
              val matchId = MatchIdExtractor.extractMatchId(current!!.name)
              currentMatchId = matchId
              showFootballStatsDialog = true
            }
            rootLayout
          } else {
            playerView
          }
        },
        modifier = Modifier
          .fillMaxSize()           // Garante que o Compose ocupe 100% da tela
          .systemBarsPadding()      // Ajusta status/nav quando necessário (Android TV ignora, mas Fire Stick precisa)
      )
    }
    return // IMPORTANTE: Sair da função ANTES de renderizar TopBar ou qualquer outro elemento
  }
  
  // LAYOUT NORMAL (com TopBar, Categorias, Mini Player)
  // IMPORTANTE: TopBar só é renderizada quando NÃO está em fullscreen
  Column(Modifier.fillMaxSize()) {
    // TopBar com Logo e Botão Voltar (APENAS no Fire Stick e APENAS quando NÃO está em fullscreen)
    if (isFireStick && !isFullscreen) {
      val horizontalPadding = 12.dp
      
      Box(
        modifier = Modifier
          .fillMaxWidthAdjusted() // ✅ Fire Stick/Native TV: 90% da largura real
          .padding(
            vertical = 12.dp,
            horizontal = horizontalPadding
          )
      ) {
        // Logo à esquerda
        Row(
          modifier = Modifier.align(Alignment.CenterStart),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Start
        ) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Logo",
            modifier = Modifier.size(40.dp),
            tint = Color(0xFF00D4FF)
          )
          
          Spacer(Modifier.width(12.dp))
          
          Text(
            text = "Max IPTV",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D4FF)
          )
        }
        
        // Botão Voltar à direita
        IconButton(
          onClick = { nav.popBackStack() },
          modifier = Modifier.align(Alignment.CenterEnd)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Voltar",
            tint = Color(0xFF00D4FF),
            modifier = Modifier.size(40.dp)
          )
        }
      }
    }
    
    CategoryChips(
      categories = categoriesWithAdult, 
      selectedId = selectedCat, 
      onSelect = { catId ->
        if (catId == "ADULT") {
          // ✅ Verificar PIN para categoria adulta
          if (isAdultUnlocked) {
            // Já desbloqueado, mostrar canais adultos
            selectedCat = "ADULT"
          } else {
            // Mostrar dialog de PIN
            pendingAdultCategory = catId
            showPinDialog = true
            pinInput = ""
            showPinError = false
          }
        } else {
          selectedCat = catId
        }
      }
    )
    
    if (isTv) {
      // 📺 Layout TV com Mini Player
      Row(Modifier.weight(1f)) {
        // Lista de canais (lado esquerdo - reduzida)
        Surface(tonalElevation = 2.dp, modifier = Modifier.width(320.dp).fillMaxHeight()) {
          val filtered = when {
            selectedCat == "ADULT" && isAdultUnlocked -> {
              streams.filter { 
                it.category_id in adultCategoryIds || 
                it.name.contains(Regex("(?i)(adult|xxx|18\\+|porn|sex)"))
              }
            }
            selectedCat == null -> streams
            else -> streams.filter { it.category_id == selectedCat }
          }
          
          val headlineSize = 18.sp
          val supportingSize = 14.sp
          val iconSize = 48.dp
          
          LazyColumn { 
            items(filtered, key = { it.stream_id }) { s ->
              var isFocused by remember { mutableStateOf(false) }
              val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.1f else 1.0f,
                animationSpec = spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy,
                  stiffness = Spring.StiffnessLow
                ),
                label = "liveChannelZoom"
              )
              
              Box(
                modifier = Modifier
                  .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                  }
              ) {
                ListItem(
                  headlineContent = { 
                    Text(
                      text = s.name,
                      fontSize = headlineSize,
                      fontWeight = FontWeight.SemiBold,
                      fontFamily = FontFamily.SansSerif,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    ) 
                  }, 
                  supportingContent = { 
                    Text(
                      text = s.categoryName ?: "-",
                      fontSize = supportingSize,
                      fontFamily = FontFamily.SansSerif,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    ) 
                  },
                  leadingContent = {
                    Box(
                      modifier = Modifier
                        .size(iconSize + 8.dp)
                        .padding(4.dp),
                      contentAlignment = Alignment.Center
                    ) {
                      AsyncImage(
                        model = s.stream_icon,
                        contentDescription = s.name,
                        modifier = Modifier
                          .size(iconSize - 4.dp),
                        contentScale = ContentScale.Inside
                      )
                    }
                  },
                  modifier = Modifier
                    .clickable { 
                      // 1x OK = tocar canal onde está o foco
                      android.util.Log.i("LiveScreen", "🎯 Canal clicado: ${s.name}")
                      current = s
                      android.util.Log.i("LiveScreen", "🎯 Canal atual mudou para: ${current?.name}")
                    }
                    .focusable()
                    .onFocusChanged { focusState ->
                      isFocused = focusState.isFocused
                      if (focusState.isFocused) {
                        // Quando ganha foco, tocar o canal
                        android.util.Log.i("LiveScreen", "🎯 Canal com foco: ${s.name}")
                        current = s
                      }
                    }
                )
                // Overlay branco transparente quando focado (clareado e afinado)
                if (isFocused) {
                  Box(
                    modifier = Modifier
                      .matchParentSize()
                      .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                      )
                  )
                }
              }
              HorizontalDivider()
            } 
          }
        }
        
        // Mini Player (lado direito - espaço azul vazio)
        Box(
          modifier = Modifier
          .weight(1f) // Ocupar todo o espaço restante
          .fillMaxHeight()
          .padding(top = 0.dp, start = 8.dp, end = 8.dp, bottom = 8.dp), // SEM padding no topo - mini player no topo máximo
          contentAlignment = Alignment.TopCenter // Alinhado ao topo centralizado
        ) {
          if (current != null) {
            MiniPlayer(
              player = sharedPlayer,
              channel = current!!,
              epgData = epgData,
              onFullscreen = { 
                // 📺 Canais normais: apenas mudar layout (MESMO PLAYER, SÓ MUDA LAYOUT)
                android.util.Log.i("MiniPlayer", "🎯 Ativando fullscreen - volume 100%")
                sharedPlayer.volume = 1.0f // Volume máximo em fullscreen
                isFullscreen = true // Trocar para layout fullscreen
              },
              onStatsClick = {
                val matchId = MatchIdExtractor.extractMatchId(current!!.name)
                currentMatchId = matchId
                showFootballStatsDialog = true
              }
            )
          } else {
            // Espaço vazio quando nenhum canal selecionado
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Selecione um canal para visualizar",
                fontSize = 20.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    } else {
      // 📱 Layout original para smartphone/tablet
      Row(Modifier.weight(1f)) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.width(380.dp).fillMaxHeight()) {
          val filtered = when {
            selectedCat == "ADULT" && isAdultUnlocked -> {
              streams.filter { 
                it.category_id in adultCategoryIds || 
                it.name.contains(Regex("(?i)(adult|xxx|18\\+|porn|sex)"))
              }
            }
            selectedCat == null -> streams
            else -> streams.filter { it.category_id == selectedCat }
          }
          val headlineSize = 16.sp
          val supportingSize = 12.sp
          val iconSize = 40.dp
          
          LazyColumn { 
            items(filtered, key = { it.stream_id }) { s ->
              // Smartphone usa touch - sem zoom e overlay (mantém como estava)
              ListItem(
                headlineContent = { 
                  Text(
                    text = s.name,
                    fontSize = headlineSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  ) 
                }, 
                supportingContent = { 
                  Text(
                    text = s.categoryName ?: "-",
                    fontSize = supportingSize,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  ) 
                },
                leadingContent = {
                  Box(
                    modifier = Modifier
                      .size(iconSize + 8.dp)
                      .padding(4.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    AsyncImage(
                      model = s.stream_icon,
                      contentDescription = s.name,
                      modifier = Modifier
                        .size(iconSize - 4.dp),
                      contentScale = ContentScale.Inside
                    )
                  }
                },
                modifier = Modifier
                  .clickable { current = s }
              )
              HorizontalDivider()
            } 
          }
        }
        Box(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
          PlayerSurface(currentUrl = current?.toLiveUrl(), channelName = current?.name)
        }
      }
    }
  }
  
  // ✅ Modal de PIN para categoria adulta
  if (showPinDialog) {
    Dialog(onDismissRequest = { 
      showPinDialog = false 
      pendingAdultCategory = null
    }) {
      Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp,
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Cadeado",
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFFF6B6B)
          )
          
          Text(
            text = "🔞 Conteúdo Adulto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6B6B)
          )
          
          Text(
            text = "Digite o PIN para acessar canais adultos:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          OutlinedTextField(
            value = pinInput,
            onValueChange = { pinInput = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = showPinError,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          
          if (showPinError) {
            Text(
              text = "PIN incorreto! Tente novamente.",
              color = Color(0xFFFF5252),
              style = MaterialTheme.typography.bodySmall
            )
          }
          
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { 
                showPinDialog = false
                pendingAdultCategory = null
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Cancelar")
            }
            
            Button(
              onClick = {
                if (pinInput == "0000") {
                  // ✅ PIN correto - desbloquear categoria adulta
                  isAdultUnlocked = true
                  selectedCat = pendingAdultCategory
                  showPinDialog = false
                  pendingAdultCategory = null
                  showPinError = false
                } else {
                  // ❌ PIN incorreto
                  showPinError = true
                  pinInput = ""
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Confirmar")
            }
          }
        }
      }
    }
  }
  
  // ⚽ DIÁLOGO DE ESTATÍSTICAS DE FUTEBOL (usando API Soccer)
  if (showFootballStatsDialog && current != null) {
    val isFootballChannel = MatchIdExtractor.isFootballChannel(current!!.name)
    if (isFootballChannel) {
      FootballStatsDialog(
        channelName = current!!.name,
        matchId = currentMatchId,
        matchDetail = matchDetail,
        matchPreview = matchPreview,
        otherMatches = otherMatches,
        isLoading = isLoadingStats,
        error = statsError,
        onDismiss = { 
          showFootballStatsDialog = false
          matchDetail = null
          matchPreview = null
          otherMatches = emptyList()
          statsError = null
        },
        deviceType = when {
          MaxiApp.isTv -> "tv"
          MaxiApp.isFireStick -> "tv"
          else -> "phone"
        }
      )
    }
  }
}

@Composable
fun MiniPlayer(
  player: androidx.media3.exoplayer.ExoPlayer,
  channel: LiveStream,
  epgData: Map<String, List<EpgProgramme>>,
  onFullscreen: () -> Unit,
  onStatsClick: (() -> Unit)? = null
) {
  // Atualizar canal quando mudar - MUDAR MÍDIA NO MESMO PLAYER com Low Latency HLS
  LaunchedEffect(channel.stream_id) {
    android.util.Log.i("MiniPlayer", "🔄 Canal alterado no mini player: ${channel.name}")
    player.stop() // Parar player atual
    
    // ✅ FASE 2: Modo Low Latency HLS para canais live (AJUSTADO para mais estabilidade)
    val mediaItem = androidx.media3.common.MediaItem.Builder()
      .setUri(channel.toLiveUrl())
      .setLiveConfiguration(
        androidx.media3.common.MediaItem.LiveConfiguration.Builder()
          .setTargetOffsetMs(2000) // ✅ Low Latency AJUSTADO: 2s de offset (era 0) - mais estável
          .setMinOffsetMs(1000) // ✅ Low Latency AJUSTADO: Offset mínimo 1s (era 0) - evitar travamentos
          .setMaxOffsetMs(5000) // ✅ Low Latency AJUSTADO: Máximo 5s de atraso (mantido)
          .setMinPlaybackSpeed(0.95f) // ✅ Low Latency AJUSTADO: Velocidade mínima 0.95 (era 0.98) - mais tolerante
          .setMaxPlaybackSpeed(1.05f) // ✅ Low Latency AJUSTADO: Velocidade máxima 1.05 (era 1.02) - mais tolerante
          .build()
      )
      .build()
    
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true
  }
  
  val exoPlayer = player // Renomear localmente para evitar conflito
  
  Box(
    modifier = Modifier
      .fillMaxSize() // Ocupar todo o espaço azul
      .padding(8.dp)
      .clickable { 
        // 2x OK = fullscreen
        android.util.Log.i("MiniPlayer", "🎯 2x OK no mini player - abrindo fullscreen")
        onFullscreen()
      }
      .focusable()
      .onFocusChanged { focusState ->
        if (focusState.isFocused) {
          android.util.Log.i("MiniPlayer", "🎯 Mini player com foco - pronto para 2x OK")
        }
      },
    contentAlignment = Alignment.Center
  ) {
    // Player View - USANDO PLAYER COMPARTILHADO
    androidx.compose.ui.viewinterop.AndroidView(
      factory = { ctx ->
        androidx.media3.ui.PlayerView(ctx).apply {
          this.player = exoPlayer // Usar o player compartilhado renomeado
          useController = false // SEM CONTROLES
          resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
          // Desabilitar overlay nativo do ExoPlayer para mostrar nosso EPG
          setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
          layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
          )
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(16.dp)) // Bordas arredondadas para visual moderno
    )
    
    // 🎨 Overlay elegante com EPG (APENAS no mini player)
    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .background(
          androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              Color.Black.copy(alpha = 0.85f)
            )
          )
        )
        .padding(16.dp, 8.dp, 16.dp, 16.dp) // Padding reduzido no topo
    ) {
      // ✅ Recarregar EPG se estiver vazio quando um canal é selecionado
      val scope = rememberCoroutineScope()
      LaunchedEffect(channel.stream_id) {
        if (epgData.isEmpty()) {
          android.util.Log.w("MiniPlayer", "⚠️ EPG vazio ao selecionar canal, recarregando...")
          scope.launch {
            XRepo.loadEpg()
          }
        }
      }
      
      // Buscar programa atual e próximo do EPG
      val currentProgramme = EpgParser.getCurrentProgramme(channel.name, epgData)
      val nextProgramme = EpgParser.getNextProgramme(channel.name, epgData)
      
      // Log para debug do EPG
      android.util.Log.i("MiniPlayer", "📺 Canal: ${channel.name}")
      android.util.Log.i("MiniPlayer", "📡 EPG carregado: ${epgData.size} canais")
      if (epgData.isNotEmpty()) {
        android.util.Log.i("MiniPlayer", "📋 Primeiros canais EPG: ${epgData.keys.take(10).joinToString(", ")}")
      }
      android.util.Log.i("MiniPlayer", "🎬 Programa atual: ${currentProgramme?.title ?: "NÃO ENCONTRADO"}")
      android.util.Log.i("MiniPlayer", "🎬 Próximo programa: ${nextProgramme?.title ?: "NÃO ENCONTRADO"}")
      
      Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        // 📺 Nome do canal com visual profissional
        Text(
          text = channel.name,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          letterSpacing = 0.5.sp,
          style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
              color = Color.Black.copy(alpha = 0.9f),
              offset = androidx.compose.ui.geometry.Offset(2f, 2f),
              blurRadius = 6f
            )
          )
        )
        
        // 🎬 Programa atual (se disponível no EPG)
        if (currentProgramme != null) {
          Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
          ) {
            // Badge "AO VIVO" estilo moderno
            Box(
              modifier = Modifier
                .background(
                  androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFF1744), Color(0xFFE91E63))
                  ),
                  RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "● AO VIVO",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.8.sp
              )
            }
            
            // Horário do programa
            Text(
              text = "${currentProgramme.startTime()} - ${currentProgramme.stopTime()}",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF90CAF9), // Azul claro
              letterSpacing = 0.3.sp
            )
          }
          
          // Título do programa atual
          Text(
            text = currentProgramme.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFF59D), // Amarelo suave
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
            style = androidx.compose.ui.text.TextStyle(
              shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.7f),
                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                blurRadius = 3f
              )
            )
          )
          
          // 📺 Próxima atração com visual melhorado
          if (nextProgramme != null) {
            Row(
              horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
              // Label "Em seguida"
              Text(
                text = "EM SEGUIDA",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78909C), // Cinza azulado
                letterSpacing = 0.8.sp
              )
              
              Text(
                text = "•",
                fontSize = 10.sp,
                color = Color(0xFF78909C)
              )
              
              Text(
                text = "${nextProgramme.startTime()}h",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFB0BEC5)
              )
              
              Text(
                text = nextProgramme.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCFD8DC), // Cinza claro
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
        // ❌ OVERLAY ANTIGO REMOVIDO - SÓ EPG AGORA!
      }
    }
    
    // ⚽ BOTÃO DE ESTATÍSTICAS DE FUTEBOL (se for canal de futebol)
    val isFootballChannel = MatchIdExtractor.isFootballChannel(channel.name)
    val channelMatchId = if (isFootballChannel) {
      MatchIdExtractor.extractMatchId(channel.name)
    } else null
    
    if (isFootballChannel && onStatsClick != null) {
      // ⚽ Adicionar botão de estatísticas no canto superior direito
      androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
          val buttonSize = if (MaxiApp.isTv) 56 else 48 // dp
          val density = ctx.resources.displayMetrics.density
          val sizePx = (buttonSize * density).toInt()
          val margin = (16f * density).toInt()
          
          android.widget.ImageButton(ctx).apply {
            // Criar drawable de bola de futebol
            val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            
            val paint = android.graphics.Paint().apply {
              isAntiAlias = true
              style = android.graphics.Paint.Style.FILL
              color = android.graphics.Color.WHITE
            }
            
            val strokePaint = android.graphics.Paint().apply {
              isAntiAlias = true
              style = android.graphics.Paint.Style.STROKE
              strokeWidth = 4f
              color = android.graphics.Color.BLACK
            }
            
            val centerX = sizePx / 2f
            val centerY = sizePx / 2f
            val radius = (sizePx * 0.4f)
            
            canvas.drawCircle(centerX, centerY, radius, paint)
            canvas.drawCircle(centerX, centerY, radius, strokePaint)
            
            val linePaint = android.graphics.Paint().apply {
              isAntiAlias = true
              style = android.graphics.Paint.Style.STROKE
              strokeWidth = 3f
              color = android.graphics.Color.BLACK
            }
            
            canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, linePaint)
            canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, linePaint)
            
            setImageBitmap(bitmap)
            background = null
            
            setOnClickListener {
              android.util.Log.i("MiniPlayer", "⚽ Botão de estatísticas clicado")
              onStatsClick() // Usar o callback passado como parâmetro
            }
            
            // Animação de rotação
            val rotationAnimator = android.animation.ObjectAnimator.ofFloat(this, "rotation", 0f, 360f).apply {
              duration = 3000
              repeatCount = android.animation.ObjectAnimator.INFINITE
              interpolator = android.view.animation.LinearInterpolator()
              start()
            }
            
            layoutParams = android.widget.FrameLayout.LayoutParams(sizePx, sizePx).apply {
              gravity = android.view.Gravity.TOP or android.view.Gravity.END
              setMargins(0, margin, margin, 0)
            }
          }
        },
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(if (MaxiApp.isTv) 16.dp else 12.dp)
      )
    }
  }
}

// ⚽ FUNÇÃO AUXILIAR: Criar botão de estatísticas em AndroidView
private fun createFootballStatsButtonInView(
  ctx: android.content.Context,
  rootLayout: android.widget.FrameLayout,
  channelName: String,
  matchId: Long?,
  viewModel: SoccerStatsViewModel,
  onClick: () -> Unit
): android.widget.ImageButton {
  val buttonSize = if (MaxiApp.isTv) 56 else 48 // dp
  val density = ctx.resources.displayMetrics.density
  val sizePx = (buttonSize * density).toInt()
  val margin = (16f * density).toInt()
  val bufferingOffset = if (MaxiApp.isTv) (72f * density).toInt() else (64f * density).toInt()
  val topMargin = bufferingOffset + margin
  val rightMarginDp = if (MaxiApp.isTv) 48 else 16
  val rightMargin = (rightMarginDp * density).toInt()
  
  return android.widget.ImageButton(ctx).apply {
    // Criar drawable de bola de futebol
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    val paint = android.graphics.Paint().apply {
      isAntiAlias = true
      style = android.graphics.Paint.Style.FILL
      color = android.graphics.Color.WHITE
    }
    
    val strokePaint = android.graphics.Paint().apply {
      isAntiAlias = true
      style = android.graphics.Paint.Style.STROKE
      strokeWidth = 4f
      color = android.graphics.Color.BLACK
    }
    
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    val radius = (sizePx * 0.4f)
    
    canvas.drawCircle(centerX, centerY, radius, paint)
    canvas.drawCircle(centerX, centerY, radius, strokePaint)
    
    val linePaint = android.graphics.Paint().apply {
      isAntiAlias = true
      style = android.graphics.Paint.Style.STROKE
      strokeWidth = 3f
      color = android.graphics.Color.BLACK
    }
    
    canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, linePaint)
    canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, linePaint)
    
    setImageBitmap(bitmap)
    background = null
    
    setOnClickListener {
      android.util.Log.i("LiveScreen", "⚽ Botão de estatísticas clicado")
      onClick()
    }
    
    // Animação de rotação
    val rotationAnimator = android.animation.ObjectAnimator.ofFloat(this, "rotation", 0f, 360f).apply {
      duration = 3000
      repeatCount = android.animation.ObjectAnimator.INFINITE
      interpolator = android.view.animation.LinearInterpolator()
      start()
    }
    
    layoutParams = android.widget.FrameLayout.LayoutParams(sizePx, sizePx).apply {
      gravity = android.view.Gravity.TOP or android.view.Gravity.END
      setMargins(0, topMargin, rightMargin, 0)
    }
    
    elevation = 16f
    rootLayout.addView(this)
    android.util.Log.i("LiveScreen", "⚽ Botão de estatísticas criado no fullscreen")
  }
}

package com.maxiptv.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.TransformOrigin
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
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
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
  
  // EPG Data
  val epgData by XRepo.epgData.collectAsState()
  
  // ⚽ NOVO: Buscar Match ID automaticamente quando detectar canal de futebol
  // Buscar programa atual do EPG para verificar se é jogo de futebol
  val currentProgramme = remember(current?.name, epgData) {
    if (current != null) {
      EpgParser.getCurrentProgramme(current!!.name, epgData)
    } else {
      null
    }
  }
  
  // Verificar se é canal de futebol
  val isFootballChannel = remember(current?.name, currentProgramme?.title) {
    if (current != null) {
      MatchIdExtractor.isFootballChannel(current!!.name, currentProgramme?.title)
    } else {
      false
    }
  }
  
  // ⚽ CORREÇÃO: Se EPG não mostra jogo mas é canal de futebol, buscar jogo ao vivo na API
  var correctedEpgTitle by remember { mutableStateOf<String?>(null) }
  val scopeForEpg = rememberCoroutineScope()
  
  LaunchedEffect(current?.name, currentProgramme?.title, isFootballChannel) {
    if (current != null && isFootballChannel) {
      val epgTitle = currentProgramme?.title ?: ""
      // Verificar se o EPG mostra um jogo de futebol (contém " x " ou times conhecidos)
      val epgShowsFootball = epgTitle.contains(" x ", ignoreCase = true) ||
                            epgTitle.contains(" vs ", ignoreCase = true) ||
                            epgTitle.lowercase().contains("futebol") ||
                            epgTitle.lowercase().contains("jogo") ||
                            epgTitle.lowercase().contains("partida")
      
      // Se EPG não mostra jogo, buscar na API
      if (!epgShowsFootball && epgTitle.isNotEmpty()) {
        android.util.Log.i("LiveScreen", "⚠️ EPG mostra '$epgTitle' mas é canal de futebol - buscando jogo ao vivo na API...")
        scopeForEpg.launch {
          try {
            // Buscar jogo ao vivo para este canal
            val matchId = SoccerRepository.findMatchForChannel(current!!.name, null)
            if (matchId != null) {
              // Buscar detalhes do jogo para pegar o título
              val matchDetail = SoccerRepository.getMatchDetail(matchId)
              if (matchDetail != null) {
                val apiTitle = "${matchDetail.homeTeamName} x ${matchDetail.awayTeamName}"
                correctedEpgTitle = apiTitle
                android.util.Log.i("LiveScreen", "✅ Título corrigido da API: '$apiTitle'")
              }
            }
          } catch (e: Exception) {
            android.util.Log.w("LiveScreen", "⚠️ Erro ao buscar título da API: ${e.message}")
          }
        }
      } else {
        correctedEpgTitle = null // Usar EPG normal
      }
    } else {
      correctedEpgTitle = null
    }
  }
  
  // Usar título corrigido se disponível, senão usar EPG
  val displayEpgTitle = correctedEpgTitle ?: currentProgramme?.title
  
  // ⚽ ESTADOS PARA DADOS DE ESTATÍSTICAS (usando API Sports)
  var matchDetail by remember { mutableStateOf<com.maxiptv.data.soccer.MatchDetailFull?>(null) }
  var matchPreview by remember { mutableStateOf<com.maxiptv.data.soccer.MatchPreviewFull?>(null) }
  var otherMatches by remember { mutableStateOf<List<com.maxiptv.data.soccer.MatchSummaryFull>>(emptyList()) }
  var matchOdds by remember { mutableStateOf<com.maxiptv.data.soccer.ApiSportsOdds?>(null) }
  var isLoadingStats by remember { mutableStateOf(false) }
  var statsError by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()
  
  // ⚽ REMOVIDO: Busca automática de Match ID - agora só busca quando o botão for clicado
  // Apenas detectar se é canal de futebol para mostrar o botão
  // (isFootballChannel já está declarado acima na linha 107)
  
  // ⚽ Buscar estatísticas quando o diálogo for aberto
  LaunchedEffect(showFootballStatsDialog, currentMatchId) {
    if (showFootballStatsDialog) {
      // ✅ CORREÇÃO: Inicializar estados imediatamente para mostrar diálogo
      // O diálogo já deve estar visível (showFootballStatsDialog = true foi definido antes)
      isLoadingStats = true
      statsError = null
      
      // ✅ CORREÇÃO: Limpar dados apenas se necessário (permitir reutilizar dados anteriores)
      // Isso permite que o diálogo apareça imediatamente com dados anteriores se disponíveis
      if (matchDetail == null) {
        matchPreview = null
        otherMatches = emptyList()
        matchOdds = null
      }
      
      try {
        android.util.Log.i("LiveScreen", "═══════════════════════════════════════")
        android.util.Log.i("LiveScreen", "⚽ INICIANDO BUSCA NA API SPORTS")
        android.util.Log.i("LiveScreen", "   MatchId inicial: $currentMatchId")
        android.util.Log.i("LiveScreen", "   Canal: ${current?.name}")
        android.util.Log.i("LiveScreen", "   URL Base: https://v3.football.api-sports.io/")
        android.util.Log.i("LiveScreen", "═══════════════════════════════════════")
        
        // ✅ NOVO: Se não houver Match ID, usar busca inteligente para identificar a partida
        var finalMatchId = currentMatchId
        if (finalMatchId == null && current != null) {
          android.util.Log.i("LiveScreen", "🔍 Match ID não encontrado, buscando automaticamente para o canal...")
          
          // Tentar buscar Match ID usando busca inteligente por canal
          finalMatchId = SoccerRepository.findMatchForChannel(current!!.name)
          
          if (finalMatchId != null) {
            android.util.Log.i("LiveScreen", "   ✅ Match ID identificado automaticamente: $finalMatchId")
            // Atualizar currentMatchId para uso futuro
            currentMatchId = finalMatchId
          } else {
            android.util.Log.w("LiveScreen", "   ⚠️ Não foi possível identificar a partida para este canal")
          }
        }
        
        if (finalMatchId == null) {
          android.util.Log.e("LiveScreen", "❌ Match ID não disponível - não é possível buscar estatísticas")
          statsError = "Partida não encontrada. Verifique se o jogo está ao vivo."
          isLoadingStats = false
          return@LaunchedEffect
        }
        
        // Buscar detalhes da partida
        android.util.Log.i("LiveScreen", "📡 1/3 - Buscando getMatchDetail($finalMatchId)...")
        val detail = SoccerRepository.getMatchDetail(finalMatchId)
        android.util.Log.i("LiveScreen", "   ✅ getMatchDetail retornou: ${detail?.homeTeamName} x ${detail?.awayTeamName}")
        matchDetail = detail
        
        // Buscar preview da partida
        android.util.Log.i("LiveScreen", "📡 2/3 - Buscando getMatchPreview($finalMatchId)...")
        val preview = SoccerRepository.getMatchPreview(finalMatchId)
        android.util.Log.i("LiveScreen", "   ✅ getMatchPreview retornou (word_count: ${preview?.word_count})")
        matchPreview = preview
        
        // Buscar outros jogos
        android.util.Log.i("LiveScreen", "📡 3/4 - Buscando getOtherMatches()...")
        val others = SoccerRepository.getOtherMatches()
        android.util.Log.i("LiveScreen", "   ✅ getOtherMatches retornou ${others.size} partidas")
        otherMatches = others
        
        // Buscar odds (probabilidades de apostas)
        android.util.Log.i("LiveScreen", "📡 4/4 - Buscando odds (probabilidades de apostas)...")
        val odds = SoccerRepository.getLiveOdds(finalMatchId) ?: SoccerRepository.getOdds(finalMatchId)
        if (odds != null) {
          android.util.Log.i("LiveScreen", "   ✅ Odds encontradas: ${odds.bookmakers?.size ?: 0} casas de aposta")
        } else {
          android.util.Log.w("LiveScreen", "   ⚠️ Nenhuma odd encontrada")
        }
        matchOdds = odds
        
        isLoadingStats = false
        android.util.Log.i("LiveScreen", "═══════════════════════════════════════")
        android.util.Log.i("LiveScreen", "✅ TODAS AS ESTATÍSTICAS CARREGADAS COM SUCESSO!")
        android.util.Log.i("LiveScreen", "═══════════════════════════════════════")
      } catch (e: Exception) {
        android.util.Log.e("LiveScreen", "═══════════════════════════════════════")
        android.util.Log.e("LiveScreen", "❌ ERRO AO BUSCAR ESTATÍSTICAS")
        android.util.Log.e("LiveScreen", "   Erro: ${e.message}")
        android.util.Log.e("LiveScreen", "   Tipo: ${e.javaClass.simpleName}")
        android.util.Log.e("LiveScreen", "   StackTrace:", e)
        android.util.Log.e("LiveScreen", "═══════════════════════════════════════")
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
  val playerState = remember { 
    PlayerState().apply {
      currentMaxBitrate = 1_500_000 // Inicializar bitrate para Live TV (REDUZIDO de 2.2Mbps)
    }
  }
  
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
    try {
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
        
        // 📊 QUALIDADE ADAPTATIVA OTIMIZADA (REDUZIDA para evitar travamentos)
        trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder(context)
          .setPreferredTextLanguage(null)
          .setMaxVideoBitrate(1_500_000) // 1.5Mbps (REDUZIDO de 2.2Mbps - mais estável)
          .setMaxVideoSize(1280, 720)   // Limitar a 720p
          .setMinVideoBitrate(400_000)  // Bitrate mínimo (REDUZIDO de 500k)
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
              androidx.media3.common.Player.STATE_BUFFERING -> {
                val now = System.currentTimeMillis()
                
                // ✅ DETECÇÃO MELHORADA DE WI-FI LENTO: Múltiplos fatores de detecção
                val bufferAhead = bufferedPosition - currentPosition
                val timeSinceLastBuffering = if (playerState.lastBufferingTime > 0) now - playerState.lastBufferingTime else Long.MAX_VALUE
                
                // Fator 1: Buffering frequente (mais sensível - 2 eventos em 5s)
                if (playerState.lastBufferingTime > 0 && timeSinceLastBuffering < 5000) {
                  playerState.bufferingCount++
                  android.util.Log.w("SharedPlayer", "⚠️ Buffering frequente detectado (${playerState.bufferingCount} eventos em ${timeSinceLastBuffering / 1000}s)")
                } else if (timeSinceLastBuffering > 10000) {
                  // Reset contador se buffering espaçado (rede normal)
                  playerState.bufferingCount = 0
                  android.util.Log.d("SharedPlayer", "✅ Rede estável, resetando contador de buffering")
                }
                
                // Fator 2: Buffer muito baixo (< 2 segundos)
                val bufferLow = bufferAhead < 2000
                if (bufferLow) {
                  android.util.Log.w("SharedPlayer", "⚠️ Buffer muito baixo: ${bufferAhead}ms")
                }
                
                // Fator 3: Detecção usando ConnectionQuality
                val latencyMs = bufferAhead.coerceAtLeast(0)
                val estimatedQuality = estimateConnectionQuality(
                  this@apply,
                  latencyMs = latencyMs,
                  bufferAhead = bufferAhead,
                  bitrate = videoFormat?.bitrate ?: 0
                )
                
                // ✅ Atualizar qualidade de conexão
                val previousQuality = playerState.connectionQuality
                playerState.connectionQuality = estimatedQuality
                
                // Log quando qualidade muda
                if (previousQuality != estimatedQuality) {
                  android.util.Log.w("SharedPlayer", "📊 Qualidade de conexão mudou: $previousQuality → $estimatedQuality")
                  android.util.Log.w("SharedPlayer", "   - Latência: ${latencyMs}ms")
                  android.util.Log.w("SharedPlayer", "   - Buffer: ${bufferAhead}ms")
                  android.util.Log.w("SharedPlayer", "   - Bitrate: ${(videoFormat?.bitrate ?: 0) / 1000}kbps")
                }
                
                // ✅ REDUÇÃO GRADUAL DE QUALIDADE baseada em múltiplos fatores
                val shouldReduceQuality = when {
                  // Redução imediata: buffering muito frequente OU buffer muito baixo + qualidade ruim
                  playerState.bufferingCount >= 2 && (estimatedQuality == ConnectionQuality.POOR || bufferLow) -> {
                    android.util.Log.w("SharedPlayer", "🚨 Redução IMEDIATA: buffering frequente + conexão ruim")
                    true
                  }
                  // Redução leve: buffering frequente OU buffer baixo
                  playerState.bufferingCount >= 2 || (bufferLow && estimatedQuality == ConnectionQuality.POOR) -> {
                    android.util.Log.w("SharedPlayer", "⚠️ Redução LEVE: buffering ou buffer baixo detectado")
                    true
                  }
                  // Redução preventiva: qualidade ruim detectada
                  estimatedQuality == ConnectionQuality.POOR && playerState.qualityReductionLevel == 0 -> {
                    android.util.Log.w("SharedPlayer", "📉 Redução PREVENTIVA: qualidade de conexão ruim")
                    true
                  }
                  else -> false
                }
                
                if (shouldReduceQuality && playerState.currentMaxBitrate > 600_000) {
                  // ✅ Redução gradual baseada no nível atual (LIVE TV) - OTIMIZADA
                  val newBitrate = when (playerState.qualityReductionLevel) {
                    0 -> 1_000_000  // Nível 1: Redução leve (1.5Mbps → 1.0Mbps live)
                    1 -> 700_000    // Nível 2: Redução média (1.0Mbps → 700kbps live)
                    2 -> 500_000    // Nível 3: Redução alta (700kbps → 500kbps live)
                    else -> playerState.currentMaxBitrate // Não reduzir mais
                  }
                  
                  if (newBitrate < playerState.currentMaxBitrate) {
                    playerState.qualityReductionLevel++
                    playerState.currentMaxBitrate = newBitrate
                    
                    val newResolution = when (playerState.qualityReductionLevel) {
                      1 -> Pair(1280, 720)  // 720p
                      2 -> Pair(854, 480)   // 480p
                      else -> Pair(640, 360) // 360p
                    }
                    
                    android.util.Log.i("SharedPlayer", "📉 Wi-Fi lento detectado! Reduzindo qualidade (nível ${playerState.qualityReductionLevel})")
                    android.util.Log.i("SharedPlayer", "   Bitrate: ${playerState.currentMaxBitrate / 1000}kbps")
                    android.util.Log.i("SharedPlayer", "   Resolução: ${newResolution.first}x${newResolution.second}")
                    android.util.Log.i("SharedPlayer", "   Qualidade conexão: $estimatedQuality")
                    
                    // ✅ Aplicar novo bitrate e forçar re-seleção de tracks
                    val newParams = androidx.media3.common.TrackSelectionParameters.Builder(context)
                      .setPreferredTextLanguage(null)
                      .setMaxVideoBitrate(playerState.currentMaxBitrate)
                      .setMaxVideoSize(newResolution.first, newResolution.second)
                      .setMinVideoBitrate((playerState.currentMaxBitrate * 0.3).toInt())
                      .build()
                    
                    trackSelectionParameters = newParams
                    android.util.Log.i("SharedPlayer", "✅ Qualidade reduzida automaticamente para evitar travamentos")
                  }
                }
                
                playerState.lastBufferingTime = now
                android.util.Log.i("SharedPlayer", "⏳ Bufferizando... (contador: ${playerState.bufferingCount}, buffer: ${bufferAhead}ms, qualidade: $estimatedQuality)")
              }
              androidx.media3.common.Player.STATE_READY -> {
                android.util.Log.i("SharedPlayer", "✅ Player pronto!")
                // Resetar contador de failover quando player estiver pronto
                playerState.failoverAttempts = 0
                playerState.originalStreamUrl = null
                
                // ✅ RESTAURAR qualidade quando rede melhorar
                // Se está tocando bem por mais de 30 segundos, resetar contador de buffering e qualidade
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                  if (isPlaying && playerState.bufferingCount == 0 && playerState.connectionQuality != ConnectionQuality.POOR) {
                    if (playerState.qualityReductionLevel > 0) {
                      playerState.qualityReductionLevel = 0
                      playerState.currentMaxBitrate = 1_500_000 // Restaurar para 1.5Mbps (não 2.2Mbps)
                      trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder(context)
                        .setPreferredTextLanguage(null)
                        .setMaxVideoBitrate(playerState.currentMaxBitrate)
                        .setMaxVideoSize(1280, 720)
                        .setMinVideoBitrate(400_000)
                        .build()
                      android.util.Log.i("SharedPlayer", "✅ Rede melhorou! Qualidade restaurada (${playerState.currentMaxBitrate / 1000}kbps)")
                    }
                  }
                }, 30000) // 30 segundos de reprodução estável
              }
              androidx.media3.common.Player.STATE_ENDED -> 
                android.util.Log.i("SharedPlayer", "🏁 Fim da stream")
            }
          }
          
          override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            // ✅ Detectar mudanças de qualidade (detecção de degradação)
            val tracks = currentTracks
            tracks.groups.forEach { group ->
              if (group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO && group.length > 0) {
                val format = group.getTrackFormat(0)
                detectQualityDegradation(playerState, format)
                // Nota: A adaptação automática completa está no onPlaybackStateChanged
              }
            }
          }
        })
        
        android.util.Log.i("SharedPlayer", "🎯 Player compartilhado criado com melhorias profissionais (Fase 1 e 2)")
      }
    } catch (e: Exception) {
      android.util.Log.e("LiveScreen", "❌ ERRO CRÍTICO ao criar sharedPlayer: ${e.message}", e)
      e.printStackTrace()
      // Retornar um player vazio para evitar crash total
      androidx.media3.exoplayer.ExoPlayer.Builder(context).build()
    }
  }
  
  // 🔥 INTERCEPTAR BACK BUTTON (só na TV) - DEPOIS do player ser criado
  if (isTv) {
    androidx.activity.compose.BackHandler(enabled = isFullscreen) {
      // 1x BACK em fullscreen = volta para mini player
      android.util.Log.i("LiveScreen", "🔙 BACK pressionado - saindo do fullscreen")
      try {
        // ✅ IMPORTANTE: Ajustar volume de volta para o mini player (30%)
        sharedPlayer.volume = 0.3f
        // ✅ Sair do fullscreen - MiniPlayer será renderizado novamente automaticamente
        isFullscreen = false
        android.util.Log.i("LiveScreen", "✅ Fullscreen fechado - MiniPlayer será renderizado novamente")
        android.util.Log.i("LiveScreen", "   - Player continua tocando: ${sharedPlayer.isPlaying}")
        android.util.Log.i("LiveScreen", "   - Estado do player: ${sharedPlayer.playbackState}")
      } catch (e: Exception) {
        android.util.Log.e("LiveScreen", "❌ Erro ao sair do fullscreen: ${e.message}", e)
        // Garantir que sai do fullscreen mesmo em caso de erro
        isFullscreen = false
      }
      // Continua na mesma tela (LiveScreen) - MiniPlayer será renderizado automaticamente
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
  
  // 🔥 SE FULLSCREEN, MOSTRAR PLAYER COM EPG (TELA TODA, SEM TopBar/Categorias)
  if (isFullscreen && current != null) {
    // ✅ BRASIL: Estado para atualizar programas automaticamente (atualiza a cada 30 minutos)
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // ✅ Buscar programa atual do EPG para verificar se é jogo de futebol
    val currentProgrammeFullscreen = remember(currentTime, current?.name, epgData) {
        EpgParser.getCurrentProgramme(current!!.name, epgData)
    }
    // ⚽ Detectar se é canal de futebol (com verificação de EPG) - usar variável já declarada acima
    val isFootballChannelFullscreen = MatchIdExtractor.isFootballChannel(current!!.name, currentProgrammeFullscreen?.title)
    // ⚽ Usar currentMatchId já encontrado automaticamente (busca em partidas recentes/finalizadas)
    val channelMatchId = if (isFootballChannelFullscreen) {
      currentMatchId ?: MatchIdExtractor.extractMatchId(current!!.name)
    } else null
    
    // ✅ BRASIL: Atualizar tempo a cada 30 minutos para recalcular programas atual/próximo
    // NOTA: Isso NÃO chama a API, apenas recalcula qual programa está no ar usando dados já carregados
    LaunchedEffect(isFullscreen, current?.name) {
        if (isFullscreen && current != null) {
            while (true) {
                kotlinx.coroutines.delay(1800000) // Atualizar a cada 30 minutos (1800000 ms = 30 min)
                currentTime = System.currentTimeMillis()
                android.util.Log.d("LiveScreen", "🔄 Atualizando programas EPG (fullscreen) - recalculando programa atual")
            }
        }
    }
    
    // ✅ Buscar programa atual e próximo do EPG (atualizado automaticamente quando currentTime muda)
    val currentProgramme = remember(currentTime, current?.name, epgData) {
        EpgParser.getCurrentProgramme(current!!.name, epgData)
    }
    val nextProgramme = remember(currentTime, current?.name, epgData) {
        EpgParser.getNextProgramme(current!!.name, epgData)
    }
    
    // Fullscreen com EPG - player com controles nativos + overlay de EPG
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
            // ✅ CONFIGURAR FOCO: Tornar focusable para D-pad (seta para cima focará no botão de estatísticas)
            isFocusable = true
            isFocusableInTouchMode = false
            // ID único para navegação de foco
            id = android.view.View.generateViewId()
          }
          
          // ⚽ Adicionar botão de estatísticas se for canal de futebol
          if (isFootballChannelFullscreen) {
            val rootLayout = playerView.parent as? android.widget.FrameLayout ?: android.widget.FrameLayout(ctx).apply {
              addView(playerView)
            }
            val finalMatchId = currentMatchId ?: channelMatchId
            android.util.Log.i("LiveScreen", "⚽ Criando botão de estatísticas no FULLSCREEN:")
            android.util.Log.i("LiveScreen", "   - Canal: ${current!!.name}")
            android.util.Log.i("LiveScreen", "   - MatchId preservado do mini player: $currentMatchId")
            android.util.Log.i("LiveScreen", "   - MatchId final usado: $finalMatchId")
            val statsButton = createFootballStatsButtonInView(ctx, rootLayout, current!!.name, finalMatchId, soccerStatsViewModel) {
              // Usar matchId já encontrado ou tentar buscar novamente
              if (currentMatchId == null && current != null) {
                android.util.Log.i("LiveScreen", "⚽ Match ID não disponível, buscando automaticamente ao clicar no botão...")
                scope.launch {
                  try {
                    val matchId = SoccerRepository.findMatchForChannel(current!!.name)
                    if (matchId != null) {
                      currentMatchId = matchId
                      android.util.Log.i("LiveScreen", "✅ Match ID encontrado: $matchId")
                    }
                  } catch (e: Exception) {
                    android.util.Log.e("LiveScreen", "❌ Erro ao buscar Match ID", e)
                  }
                }
              }
              showFootballStatsDialog = true
            }
            // ✅ CONFIGURAR FOCO: Quando apertar seta para cima no PlayerView, focar no botão de estatísticas
            playerView.nextFocusUpId = statsButton.id
            statsButton.nextFocusDownId = playerView.id
            android.util.Log.i("LiveScreen", "✅ Navegação de foco configurada: PlayerView (ID: ${playerView.id}) → StatsButton (ID: ${statsButton.id})")
            rootLayout
          } else {
            playerView
          }
        },
        modifier = Modifier
          .fillMaxSize()           // Garante que o Compose ocupe 100% da tela
          .systemBarsPadding()      // Ajusta status/nav quando necessário (Android TV ignora, mas Fire Stick precisa)
      )
      
      // 🎨 Overlay moderno com EPG (MESMO ESTILO DO MINI PLAYER) - COM SAFE AREA/OVERSCAN
      // ✅ Aplicar padding de overscan para não cortar na TV (aumentado significativamente)
      val overscanPaddingFullscreen = when {
        MaxiApp.isFireStick -> (MaxiApp.fireStickOverscanPadding.coerceAtLeast(20) + 20).dp // Adicionar 20dp extra
        MaxiApp.isNativeTv -> 52.dp // Aumentado de 32dp para 52dp
        MaxiApp.isTvBox -> 48.dp // Aumentado de 28dp para 48dp
        else -> 0.dp
      }
      
      Box(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth()
          .background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
              colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.3f),
                Color.Black.copy(alpha = 0.75f),
                Color.Black.copy(alpha = 0.9f)
              ),
              startY = 0f,
              endY = Float.POSITIVE_INFINITY
            ),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
          )
          .padding(
            start = (24.dp + overscanPaddingFullscreen), // Padding mínimo 24dp + overscan (sem limite máximo)
            top = 12.dp,
            end = (24.dp + overscanPaddingFullscreen), // Padding mínimo 24dp + overscan (sem limite máximo)
            bottom = (24.dp + overscanPaddingFullscreen / 2) // Bottom com menos padding mas ainda seguro
          )
      ) {
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
          // 📺 Nome do canal com visual moderno
          Text(
            text = current!!.name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.3.sp,
            style = androidx.compose.ui.text.TextStyle(
              shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.95f),
                offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                blurRadius = 8f
              )
            )
          )
          
          // 🎬 Programa atual (se disponível no EPG)
          if (currentProgramme != null) {
            Row(
              horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
              // Badge "AO VIVO" - SEM FUNDO, APENAS TEXTO VERMELHO MODERNO
              Text(
                text = "● AO VIVO",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF1744), // Vermelho vibrante moderno
                letterSpacing = 1.sp,
                style = androidx.compose.ui.text.TextStyle(
                  shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.9f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 4f
                  )
                )
              )
              
              // Horário do programa com visual moderno
              Text(
                text = "${currentProgramme.startTime()} - ${currentProgramme.stopTime()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64B5F6), // Azul moderno mais vibrante
                letterSpacing = 0.4.sp,
                style = androidx.compose.ui.text.TextStyle(
                  shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 3f
                  )
                )
              )
            }
            
            // Título do programa atual com visual moderno
            Text(
              text = currentProgramme.title,
              fontSize = 17.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFFFFEB3B), // Amarelo mais vibrante
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              lineHeight = 22.sp,
              letterSpacing = 0.1.sp,
              style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                  color = Color.Black.copy(alpha = 0.8f),
                  offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                  blurRadius = 5f
                )
              )
            )
            
            // 📺 Próxima atração com visual moderno
            if (nextProgramme != null) {
              Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
              ) {
                // Label "Em seguida"
                Text(
                  text = "EM SEGUIDA",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF78909C), // Cinza azulado
                  letterSpacing = 0.8.sp,
                  style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                      color = Color.Black.copy(alpha = 0.7f),
                      offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                      blurRadius = 3f
                    )
                  )
                )
                
                Text(
                  text = "•",
                  fontSize = 11.sp,
                  color = Color(0xFF78909C),
                  style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                      color = Color.Black.copy(alpha = 0.7f),
                      offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                      blurRadius = 3f
                    )
                  )
                )
                
                Text(
                  text = "${nextProgramme.startTime()}h",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFFB0BEC5),
                  style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                      color = Color.Black.copy(alpha = 0.7f),
                      offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                      blurRadius = 3f
                    )
                  )
                )
                
                Text(
                  text = nextProgramme.title,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFFCFD8DC), // Cinza claro
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                  style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                      color = Color.Black.copy(alpha = 0.7f),
                      offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                      blurRadius = 3f
                    )
                  )
                )
              }
            }
          }
        }
      }
      
      // ⚽ DIÁLOGO DE ESTATÍSTICAS DE FUTEBOL (FULLSCREEN) - renderizar sobre o player
      if (showFootballStatsDialog && isFootballChannel) {
        FootballStatsDialog(
          channelName = current!!.name,
          matchId = currentMatchId,
          matchDetail = matchDetail,
          matchPreview = matchPreview,
          otherMatches = otherMatches,
          matchOdds = matchOdds,
          isLoading = isLoadingStats,
          error = statsError,
          onDismiss = { 
            showFootballStatsDialog = false
            matchDetail = null
            matchPreview = null
            otherMatches = emptyList()
            matchOdds = null
            statsError = null
          },
          deviceType = when {
            MaxiApp.isTv -> "tv"
            MaxiApp.isFireStick -> "tv"
            else -> "phone"
          },
          isVisible = showFootballStatsDialog
        )
      }
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
                      try {
                        // 1x OK = tocar canal onde está o foco
                        android.util.Log.i("LiveScreen", "🎯 Canal clicado: ${s.name}")
                        current = s
                        android.util.Log.i("LiveScreen", "🎯 Canal atual mudou para: ${current?.name}")
                      } catch (e: Exception) {
                        android.util.Log.e("LiveScreen", "❌ Erro ao clicar no canal: ${e.message}", e)
                        e.printStackTrace()
                      }
                    }
                    .focusable()
                    .onFocusChanged { focusState ->
                      try {
                        isFocused = focusState.isFocused
                        if (focusState.isFocused) {
                          // Quando ganha foco, tocar o canal
                          android.util.Log.i("LiveScreen", "🎯 Canal com foco: ${s.name}")
                          current = s
                        }
                      } catch (e: Exception) {
                        android.util.Log.e("LiveScreen", "❌ Erro ao mudar foco do canal: ${e.message}", e)
                        e.printStackTrace()
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
                try {
                  // 📺 Canais normais: apenas mudar layout (MESMO PLAYER, SÓ MUDA LAYOUT)
                  android.util.Log.i("MiniPlayer", "🎯 Ativando fullscreen - volume 100%")
                  
                  // ✅ CRÍTICO: Garantir que apenas UMA PlayerView use o player por vez
                  // O MiniPlayer será desmontado quando isFullscreen = true (return na função),
                  // mas para evitar conflitos durante a transição, garantimos que o player continue tocando
                  
                  // ✅ OTIMIZAÇÃO: Garantir que o player está pronto antes de mudar para fullscreen
                  if (sharedPlayer.playbackState == androidx.media3.common.Player.STATE_READY || 
                      sharedPlayer.playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
                    // ✅ IMPORTANTE: Não parar o player, apenas ajustar volume e mudar layout
                    // O player continua tocando, apenas a PlayerView muda (mini -> fullscreen)
                    sharedPlayer.volume = 1.0f // Volume máximo em fullscreen
                    isFullscreen = true // Trocar para layout fullscreen
                    // ✅ MiniPlayer será desmontado automaticamente pelo return na função
                    android.util.Log.i("MiniPlayer", "✅ Fullscreen ativado - mesmo player, apenas mudou PlayerView")
                  } else {
                    // Se player não está pronto, aguardar um pouco
                    android.util.Log.w("MiniPlayer", "⚠️ Player não está pronto, aguardando...")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                      if (sharedPlayer.playbackState == androidx.media3.common.Player.STATE_READY) {
                        sharedPlayer.volume = 1.0f
                        isFullscreen = true
                        android.util.Log.i("MiniPlayer", "✅ Fullscreen ativado (após delay) - mesmo player")
                      }
                    }, 500)
                  }
                } catch (e: Exception) {
                  android.util.Log.e("LiveScreen", "❌ Erro ao ativar fullscreen: ${e.message}", e)
                  e.printStackTrace()
                }
              },
              onStatsClick = {
                try {
                  android.util.Log.i("LiveScreen", "⚽ Botão de estatísticas clicado - iniciando busca de Match ID e dados...")
                  
                  // Abrir diálogo imediatamente
                  showFootballStatsDialog = true
                  isLoadingStats = true
                  statsError = null
                  
                  // Limpar dados anteriores
                  matchDetail = null
                  matchPreview = null
                  otherMatches = emptyList()
                  matchOdds = null
                  currentMatchId = null
                  
                  // Buscar Match ID e dados da API apenas quando o botão for clicado
                  scope.launch {
                    try {
                      var matchId: Long? = null
                      
                      // 1. Tentar extrair do nome do canal
                      if (current != null) {
                        matchId = MatchIdExtractor.extractMatchId(current!!.name)
                        android.util.Log.i("LiveScreen", "   Tentativa 1 - Extrair do nome: ${matchId ?: "não encontrado"}")
                      }
                      
                      // 2. Se não encontrou, buscar automaticamente na API (usando EPG se disponível)
                      if (matchId == null && current != null) {
                        val epgTitle = currentProgramme?.title
                        android.util.Log.i("LiveScreen", "   Tentativa 2 - Buscar na API para: ${current!!.name}")
                        if (epgTitle != null) {
                          android.util.Log.i("LiveScreen", "   Usando EPG: '$epgTitle'")
                        }
                        matchId = SoccerRepository.findMatchForChannel(current!!.name, epgTitle)
                        android.util.Log.i("LiveScreen", "   Resultado da busca: ${matchId ?: "não encontrado"}")
                      }
                      
                      currentMatchId = matchId
                      
                      // 3. Se encontrou Match ID, buscar dados da API
                      if (matchId != null) {
                        android.util.Log.i("LiveScreen", "   ✅ Match ID encontrado: $matchId - buscando dados da API...")
                        
                        // Buscar detalhes, preview, outros jogos e odds em paralelo
                        coroutineScope {
                          val detailDeferred = async { SoccerRepository.getMatchDetail(matchId) }
                          val previewDeferred = async { SoccerRepository.getMatchPreview(matchId) }
                          val otherMatchesDeferred = async { SoccerRepository.getOtherMatches() }
                          val oddsDeferred = async { SoccerRepository.getMatchOdds(matchId) } // Retorna null (API não tem odds)
                          
                          matchDetail = detailDeferred.await()
                          matchPreview = previewDeferred.await()
                          otherMatches = otherMatchesDeferred.await()
                          matchOdds = oddsDeferred.await() // Será null (API não tem odds)
                        }
                        
                        android.util.Log.i("LiveScreen", "   ✅ Dados carregados com sucesso!")
                      } else {
                        android.util.Log.w("LiveScreen", "   ⚠️ Match ID não encontrado - mostrando mensagem de erro")
                        statsError = "Partida não encontrada ou Match ID não disponível"
                      }
                      
                      isLoadingStats = false
                    } catch (e: Exception) {
                      android.util.Log.e("LiveScreen", "   ❌ Erro ao buscar dados: ${e.message}", e)
                      statsError = "Erro ao buscar dados: ${e.message}"
                      isLoadingStats = false
                    }
                  }
                } catch (e: Exception) {
                  android.util.Log.e("LiveScreen", "❌ Erro ao processar clique no botão: ${e.message}", e)
                  statsError = "Erro: ${e.message}"
                  isLoadingStats = false
                }
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
    val epgTitle = currentProgramme?.title
    val isFootballChannel = MatchIdExtractor.isFootballChannel(current!!.name, epgTitle)
    if (isFootballChannel) {
      FootballStatsDialog(
        channelName = current!!.name,
        matchId = currentMatchId,
        matchDetail = matchDetail,
        matchPreview = matchPreview,
        otherMatches = otherMatches,
        matchOdds = matchOdds,
        isLoading = isLoadingStats,
        error = statsError,
        onDismiss = { 
          showFootballStatsDialog = false
          matchDetail = null
          matchPreview = null
          otherMatches = emptyList()
          matchOdds = null
          statsError = null
        },
        deviceType = when {
          MaxiApp.isTv -> "tv"
          MaxiApp.isFireStick -> "tv"
          else -> "phone"
        },
        isVisible = showFootballStatsDialog // ✅ Passar estado de visibilidade para controlar foco
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
    try {
      android.util.Log.i("MiniPlayer", "🔄 Canal alterado no mini player: ${channel.name}")
      
      // ✅ PROTEÇÃO: Parar player apenas se não estiver em IDLE (estado inicial é normal)
      if (player.playbackState != androidx.media3.common.Player.STATE_IDLE) {
        player.stop() // Parar player atual apenas se estiver tocando
      }
      
      // ✅ PROTEÇÃO: Tentar gerar URL e verificar se é válida
      val url = try {
        channel.toLiveUrl()
      } catch (e: Exception) {
        android.util.Log.e("MiniPlayer", "❌ Erro ao gerar URL do canal: ${e.message}", e)
        return@LaunchedEffect
      }
      
      if (url.isBlank()) {
        android.util.Log.e("MiniPlayer", "❌ URL do canal está vazia")
        return@LaunchedEffect
      }
      
      android.util.Log.i("MiniPlayer", "   URL: $url")
      
      // ✅ FASE 2: Modo Low Latency HLS para canais live (AJUSTADO para mais estabilidade)
      val mediaItem = androidx.media3.common.MediaItem.Builder()
        .setUri(url)
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
      
      android.util.Log.i("MiniPlayer", "✅ Player atualizado com sucesso")
    } catch (e: Exception) {
      android.util.Log.e("MiniPlayer", "❌ Erro ao atualizar canal no mini player: ${e.message}", e)
      e.printStackTrace()
      // Não propagar exceção para evitar crash
    }
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
      // ✅ Removido .focusable() para não interferir com o foco do botão de futebol
      .onFocusChanged { focusState ->
        if (focusState.isFocused) {
          android.util.Log.i("MiniPlayer", "🎯 Mini player com foco - pronto para 2x OK")
        }
      },
    contentAlignment = Alignment.Center
  ) {
    // Player View - USANDO PLAYER COMPARTILHADO
    // ✅ IMPORTANTE: Desconectar player quando componente for desmontado (ex: ao entrar em fullscreen)
    androidx.compose.runtime.DisposableEffect(Unit) {
      onDispose {
        // Quando MiniPlayer é desmontado (ex: ao entrar em fullscreen), 
        // o PlayerView será removido automaticamente pelo Compose
        // O player continua tocando e será usado pelo fullscreen
        android.util.Log.i("MiniPlayer", "🧹 MiniPlayer desmontado - PlayerView será removido")
      }
    }
    
    androidx.compose.ui.viewinterop.AndroidView(
      factory = { ctx ->
        androidx.media3.ui.PlayerView(ctx).apply {
          this.player = exoPlayer // Usar o player compartilhado renomeado
          useController = false // SEM CONTROLES
          resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
          // Desabilitar overlay nativo do ExoPlayer para mostrar nosso EPG
          setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
          // ✅ IMPORTANTE: Desabilitar foco no PlayerView para não interferir com botão de futebol
          isFocusable = false
          isFocusableInTouchMode = false
          descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
          layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
          )
        }
      },
      update = { playerView ->
        // ✅ Atualizar player se mudar (garantir que está sincronizado)
        if (playerView.player != exoPlayer) {
          playerView.player = exoPlayer
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(16.dp)) // Bordas arredondadas para visual moderno
    )
    
    // 🎨 Overlay moderno com EPG (APENAS no mini player) - COM SAFE AREA/OVERSCAN
    // ✅ Aplicar padding de overscan para não cortar na TV (aumentado significativamente)
    val overscanPadding = when {
      MaxiApp.isFireStick -> (MaxiApp.fireStickOverscanPadding.coerceAtLeast(20) + 20).dp // Adicionar 20dp extra
      MaxiApp.isNativeTv -> 52.dp // Aumentado de 32dp para 52dp
      MaxiApp.isTvBox -> 48.dp // Aumentado de 28dp para 48dp
      else -> 0.dp
    }
    
    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .background(
          brush = androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              Color.Black.copy(alpha = 0.3f),
              Color.Black.copy(alpha = 0.75f),
              Color.Black.copy(alpha = 0.9f)
            ),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
          ),
          shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        )
        .padding(
          start = (24.dp + overscanPadding), // Padding mínimo 24dp + overscan (sem limite máximo)
          top = 12.dp,
          end = (24.dp + overscanPadding), // Padding mínimo 24dp + overscan (sem limite máximo)
          bottom = (24.dp + overscanPadding / 2) // Bottom com menos padding mas ainda seguro
        )
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
      
      // ✅ BRASIL: Estado para atualizar programas automaticamente no mini player
      var currentTimeMini by remember { mutableStateOf(System.currentTimeMillis()) }
      
      // ✅ BRASIL: Atualizar tempo a cada 30 minutos para recalcular programas atual/próximo
      // NOTA: Isso NÃO chama a API, apenas recalcula qual programa está no ar usando dados já carregados
      LaunchedEffect(channel.stream_id) {
        while (true) {
          kotlinx.coroutines.delay(1800000) // Atualizar a cada 30 minutos (1800000 ms = 30 min)
          currentTimeMini = System.currentTimeMillis()
          android.util.Log.d("MiniPlayer", "🔄 Atualizando programas EPG (mini player) - recalculando programa atual")
        }
      }
      
      // ✅ Buscar programa atual e próximo do EPG (atualizado automaticamente quando currentTimeMini muda)
      val currentProgramme = remember(currentTimeMini, channel.name, epgData) {
        EpgParser.getCurrentProgramme(channel.name, epgData)
      }
      val nextProgramme = remember(currentTimeMini, channel.name, epgData) {
        EpgParser.getNextProgramme(channel.name, epgData)
      }
      
      // Log para debug do EPG
      android.util.Log.i("MiniPlayer", "📺 Canal: ${channel.name}")
      android.util.Log.i("MiniPlayer", "📡 EPG carregado: ${epgData.size} canais")
      if (epgData.isNotEmpty()) {
        android.util.Log.i("MiniPlayer", "📋 Primeiros canais EPG: ${epgData.keys.take(10).joinToString(", ")}")
      }
      android.util.Log.i("MiniPlayer", "🎬 Programa atual: ${currentProgramme?.title ?: "NÃO ENCONTRADO"}")
      android.util.Log.i("MiniPlayer", "🎬 Próximo programa: ${nextProgramme?.title ?: "NÃO ENCONTRADO"}")
      
      Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        // 📺 Nome do canal com visual moderno
        Text(
          text = channel.name,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          letterSpacing = 0.3.sp,
          style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
              color = Color.Black.copy(alpha = 0.95f),
              offset = androidx.compose.ui.geometry.Offset(0f, 1f),
              blurRadius = 8f
            )
          )
        )
        
        // 🎬 Programa atual (se disponível no EPG)
        if (currentProgramme != null) {
          Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
          ) {
            // Badge "AO VIVO" - SEM FUNDO, APENAS TEXTO VERMELHO MODERNO
            Text(
              text = "● AO VIVO",
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color(0xFFFF1744), // Vermelho vibrante moderno
              letterSpacing = 1.sp,
              style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                  color = Color.Black.copy(alpha = 0.9f),
                  offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                  blurRadius = 4f
                )
              )
            )
            
            // Horário do programa com visual moderno
            Text(
              text = "${currentProgramme.startTime()} - ${currentProgramme.stopTime()}",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF64B5F6), // Azul moderno mais vibrante
              letterSpacing = 0.4.sp,
              style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                  color = Color.Black.copy(alpha = 0.7f),
                  offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                  blurRadius = 3f
                )
              )
            )
          }
          
          // Título do programa atual com visual moderno
          Text(
            text = currentProgramme.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFEB3B), // Amarelo mais vibrante
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp,
            style = androidx.compose.ui.text.TextStyle(
              shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.8f),
                offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                blurRadius = 5f
              )
            )
          )
          
          // 📺 Próxima atração com visual moderno
          if (nextProgramme != null) {
            Row(
              horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
              // Label "Em seguida"
              Text(
                text = "EM SEGUIDA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78909C), // Cinza azulado
                letterSpacing = 0.8.sp,
                style = androidx.compose.ui.text.TextStyle(
                  shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 3f
                  )
                )
              )
              
              Text(
                text = "•",
                fontSize = 11.sp,
                color = Color(0xFF78909C),
                style = androidx.compose.ui.text.TextStyle(
                  shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 3f
                  )
                )
              )
              
              Text(
                text = "${nextProgramme.startTime()}h",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFB0BEC5),
                style = androidx.compose.ui.text.TextStyle(
                  shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 3f
                  )
                )
              )
              
              Text(
                text = nextProgramme.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCFD8DC), // Cinza claro
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = androidx.compose.ui.text.TextStyle(
                  shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 3f
                  )
                )
              )
            }
          }
        }
      }
    }
    
    // ⚽ BOTÃO DE ESTATÍSTICAS DE FUTEBOL (se for canal de futebol)
    val isFootballChannel = MatchIdExtractor.isFootballChannel(channel.name)
    val channelMatchId = if (isFootballChannel) {
      MatchIdExtractor.extractMatchId(channel.name)
    } else null
    
    if (isFootballChannel && onStatsClick != null) {
      // ⚽ Estado para controlar foco e zoom animado
      var isFocused by remember { mutableStateOf(false) }
      val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.3f else 1.0f, // Aumentado para 1.3f para zoom mais visível
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium // Aumentado para animação mais rápida
        ),
        label = "statsButtonZoom"
      )
      
      // ⚽ Adicionar botão de estatísticas no canto superior direito com foco D-pad
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(if (MaxiApp.isTv) 16.dp else 12.dp)
          .graphicsLayer {
            // ✅ Zoom animado quando focado (1.0f → 1.3f)
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
          }
          .then(
            if (isFocused) {
              // ✅ Borda vermelha quando focado (mesma cor do "AO VIVO")
              Modifier.border(
                width = 4.dp,
                color = Color(0xFFFF1744), // Vermelho neon - mesma cor do "AO VIVO"
                shape = RoundedCornerShape(50) // Círculo
              )
            } else {
              Modifier
            }
          )
      ) {
        androidx.compose.ui.viewinterop.AndroidView(
          factory = { ctx ->
            val buttonSize = if (MaxiApp.isTv) 56 else 48 // dp
            val density = ctx.resources.displayMetrics.density
            val sizePx = (buttonSize * density).toInt()
            
            android.widget.ImageButton(ctx).apply {
              // ✅ Tornar focusable para D-pad - SEM interferir com ExoPlayer
              // O ExoPlayer está com isFocusable = false, então não há conflito
              isFocusable = true
              isFocusableInTouchMode = true
              focusable = android.view.View.FOCUSABLE
              // ✅ Prioridade de foco: botão pode receber foco antes do PlayerView
              importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
              
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
                android.util.Log.i("MiniPlayer", "⚽ Botão de estatísticas clicado - Iniciando busca na API Soccer...")
                android.util.Log.i("MiniPlayer", "   MatchId: ${MatchIdExtractor.extractMatchId(channel.name)}")
                android.util.Log.i("MiniPlayer", "   Canal: ${channel.name}")
                onStatsClick() // Usar o callback passado como parâmetro
              }
              
              // Animação de rotação
              val rotationAnimator = android.animation.ObjectAnimator.ofFloat(this, "rotation", 0f, 360f).apply {
                duration = 3000
                repeatCount = android.animation.ObjectAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
                start()
              }
              
              layoutParams = android.widget.FrameLayout.LayoutParams(sizePx, sizePx)
            }
          },
          update = { view ->
            // ✅ Atualizar listener de foco sempre que o Composable recompor
            view.setOnFocusChangeListener { _, hasFocus ->
              // Usar Handler para atualizar estado do Composable na UI thread
              android.os.Handler(android.os.Looper.getMainLooper()).post {
                isFocused = hasFocus
              }
              android.util.Log.d("MiniPlayer", "⚽ Botão de estatísticas - Foco D-pad: $hasFocus")
            }
          }
        )
      }
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
    // ✅ CONFIGURAR FOCO PARA D-PAD
    isFocusable = true
    isFocusableInTouchMode = true
    focusable = android.view.View.FOCUSABLE
    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
    // ✅ ID único para navegação de foco
    id = android.view.View.generateViewId()
    
    // ✅ Configurar navegação de foco (seta para baixo volta para o player)
    nextFocusUpId = android.view.View.NO_ID
    nextFocusLeftId = android.view.View.NO_ID
    nextFocusRightId = android.view.View.NO_ID
    
    // ✅ Listener para visual de foco
    setOnFocusChangeListener { _, hasFocus ->
      if (hasFocus) {
        android.util.Log.i("LiveScreen", "⚽ Botão de estatísticas FOCO no fullscreen")
        // Adicionar borda quando focado
        background = android.graphics.drawable.GradientDrawable().apply {
          shape = android.graphics.drawable.GradientDrawable.OVAL
          setStroke((4 * density).toInt(), android.graphics.Color.parseColor("#FF1744")) // Vermelho neon
          setColor(android.graphics.Color.TRANSPARENT)
        }
        // Zoom quando focado
        scaleX = 1.2f
        scaleY = 1.2f
      } else {
        background = null
        scaleX = 1.0f
        scaleY = 1.0f
      }
    }
    
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
    android.util.Log.i("LiveScreen", "⚽ Botão de estatísticas criado no fullscreen (ID: $id)")
  }
}

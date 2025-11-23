package com.maxiptv.ui.player
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.common.C
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.OnBackPressedCallback
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.VideoSize
import androidx.media3.common.Format
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Tracks
import androidx.media3.ui.SubtitleView
import android.view.accessibility.CaptioningManager
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.maxiptv.MaxiApp
import com.maxiptv.data.PlayerSettingsManager
import android.util.TypedValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Dns
import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.net.InetAddress
import java.net.Inet4Address
import android.app.AlertDialog
import android.widget.Button
import android.view.Gravity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.Typeface
import com.maxiptv.ui.player.ConnectionQuality
import com.maxiptv.ui.player.PlayerState
import com.maxiptv.ui.player.createAdaptiveLoadControl
import com.maxiptv.ui.player.detectQualityDegradation
import com.maxiptv.ui.player.estimateConnectionQuality
import com.maxiptv.data.PlaybackPositionManager

class PlayerActivity : ComponentActivity() {
  private var player: ExoPlayer? = null
  private var isFullscreen = true // Inicia em fullscreen
  private lateinit var gestureDetector: GestureDetector
  private lateinit var windowInsetsController: WindowInsetsControllerCompat
  private var reconnectAttempts = 0 // Contador de tentativas de reconexão
  private val maxReconnectAttempts: Int get() = if (isFootballMode) 8 else 5 // ⚽ FUTEBOL: mais tentativas (8 vs 5)
  private var bufferingCount = 0 // Contador de eventos de buffering
  private var lastBufferingTime = 0L // Último tempo de buffering
  private var currentMaxBitrate = 2_200_000 // Bitrate máximo atual (começa em 2.2Mbps)
  private var qualityReduced = false // Flag para saber se qualidade já foi reduzida
  private var qualityReductionLevel = 0 // Nível de redução de qualidade (0 = nenhuma, 1 = leve, 2 = média, 3 = alta)
  private var lastBufferSize = 0L // Último tamanho de buffer para detectar queda rápida
  private var lastPosition = 0L // Última posição do player (para detectar travamento)
  private var lastPositionTime = 0L // Último tempo que a posição mudou
  // ✅ FASE 2: Variáveis para failover e detecção de qualidade
  private var originalStreamUrl: String = "" // URL original do stream para failover
  private var failoverAttempts = 0 // Contador de tentativas de failover
  private val maxFailoverAttempts: Int get() = if (isFootballMode) 6 else 4 // ⚽ FUTEBOL: mais tentativas de failover (6 vs 4)
  private var lastVideoFormat: Format? = null // Último formato de vídeo para detectar degradação
  private var qualityDegradedWarningShown = false // Flag para não mostrar aviso repetidamente
  private var qualityDegradedToast: android.widget.Toast? = null // Toast para aviso de qualidade degradada
  private lateinit var pv: PlayerView // PlayerView para acesso em outros métodos
  private var contentType: String = "live" // Tipo de conteúdo (live, vod, series)
  private var subtitlesEnabled = true // Legendas habilitadas por padrão
  private var qualityOverlay: android.widget.TextView? = null // Overlay de qualidade atual
  private var remainingTimeOverlay: android.widget.TextView? = null // Overlay de tempo restante (VOD/Series)
  private var remainingTimeHandler: android.os.Handler? = null // Handler para atualizar tempo restante
  private var bufferIndicatorOverlay: android.widget.TextView? = null // Overlay de indicador de buffer
  private var bufferIndicatorHandler: android.os.Handler? = null // Handler para atualizar indicador de buffer
  // ✅ FASE 1: Overlays para melhorias profissionais
  private var latencyOverlay: android.widget.TextView? = null // Overlay de latência (Live)
  private var statsOverlay: android.widget.TextView? = null // Overlay de estatísticas detalhadas
  private var latencyHandler: android.os.Handler? = null // Handler para atualizar latência
  private var statsHandler: android.os.Handler? = null // Handler para atualizar estatísticas
  private var connectionQuality: ConnectionQuality = ConnectionQuality.GOOD // Qualidade de conexão estimada
  private var showLatencyStats: Boolean = false // Controla se deve mostrar latência/stats (só quando usuário interagir)
  private var lastUserInteraction: Long = 0L // Última interação do usuário
  // ✅ LIVE PROFESSIONAL: Overlay profissional para canais live
  private var liveChannelInfoOverlay: android.widget.TextView? = null // Overlay com informações do canal (Live)
  private var liveChannelInfoHandler: android.os.Handler? = null // Handler para atualizar informações do canal
  private var currentChannelName: String? = null // Nome do canal atual (Live)
  private var currentChannelLogo: String? = null // Logo do canal atual (Live)
  private var lastBufferTime = 0L // Último tempo de buffer
  private var contentId: Int? = null // ID do conteúdo (vodId ou seriesId) para salvar posição
  private var positionSaveHandler: android.os.Handler? = null // Handler para salvar posição periodicamente
  private var isFootballMode: Boolean = false // Modo futebol ativado
  private var footballOverlay: android.widget.ImageView? = null // Overlay de gramado para modo futebol
  private var footballStatsButton: android.widget.ImageButton? = null // Botão de estatísticas de futebol
  private var footballStatsOverlay: android.view.ViewGroup? = null // Overlay de estatísticas
  private var isStatsOverlayVisible: Boolean = false // Se overlay está visível
  private var footballStatsButtonEnabled: Boolean = true // Se botão está habilitado
  private var footballAutoZoomEnabled: Boolean = true // Se zoom automático está habilitado
  private var rotationAnimator: android.animation.ObjectAnimator? = null // Animação de rotação do botão
  private var currentZoomLevel: Float = 1.0f // Nível de zoom atual (1.0 = normal)
  // ⚽ NOVO: Sistema de estatísticas via API
  private var currentMatchId: Long? = null // ID da partida atual
  private var soccerStatsViewModel: com.maxiptv.ui.player.soccer.SoccerStatsViewModel? = null // ViewModel para estatísticas
  private var pauseControlsOverlay: android.view.ViewGroup? = null // Overlay com botões modernos quando pausa
  private var isPausedControlsVisible: Boolean = false // Se controles de pausa estão visíveis
  private val bufferIndicatorRunnable = object : Runnable {
    override fun run() {
      updateBufferIndicator()
      bufferIndicatorHandler?.postDelayed(this, 500) // Atualizar a cada 500ms (mais frequente que tempo restante)
    }
  }
  private val latencyRunnable = object : Runnable {
    override fun run() {
      updateLatency()
      latencyHandler?.postDelayed(this, 1000) // Atualizar a cada 1 segundo
    }
  }
  private val statsRunnable = object : Runnable {
    override fun run() {
      updateStreamStats()
      statsHandler?.postDelayed(this, 2000) // Atualizar a cada 2 segundos
    }
  }
  
  private val remainingTimeRunnable = object : Runnable {
    override fun run() {
      updateRemainingTime()
      remainingTimeHandler?.postDelayed(this, 1000) // Atualizar a cada 1 segundo
    }
  }
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // ✅ Se já existe um player, liberar antes de criar novo (singleTop pode reutilizar Activity)
    if (player != null) {
      android.util.Log.w("PlayerActivity", "⚠️ Player existente detectado - liberando antes de criar novo")
      player?.let { exo ->
        exo.stop()
        exo.clearMediaItems()
        exo.release()
      }
      player = null
    }
    
    // ⚽ Limpar recursos de futebol ao iniciar novo jogo
    rotationAnimator?.cancel()
    rotationAnimator = null
    footballStatsButton = null
    footballStatsOverlay = null
    isStatsOverlayVisible = false
    currentZoomLevel = 1.0f
    // ⚠️ pv será inicializado mais tarde, então resetar zoom será feito depois
    
    // ✅ API MODERNA - WindowInsetsController (substitui systemUiVisibility depreciado)
    windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
    
    // Configurar fullscreen completo - sem nenhuma barra (TopBar, Status Bar, Navigation Bar)
    windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    // ✅ BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE permite que barras apareçam temporariamente
    // quando necessário (ex: para exibir diálogos do sistema)
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    
    // Manter tela ligada durante reprodução
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
    // ✅ FLAG_FULLSCREEN removido (deprecated em API 30+) - WindowInsetsController já faz isso
    
    // ✅ Fire Stick: Garantir que diálogos possam ser exibidos mesmo em fullscreen
    if (MaxiApp.isFireStick) {
      // FLAG_LAYOUT_IN_SCREEN e FLAG_LAYOUT_NO_LIMITS já permitem diálogos
      // Mas garantir que não há flags que bloqueiem diálogos
      android.util.Log.d("PlayerActivity", "📺 Fire Stick: Configuração inicial de fullscreen permite diálogos")
    }
    
    // ✅ API MODERNA - OnBackPressedCallback (substitui onBackPressed depreciado)
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (isFullscreen) {
          // Se está em fullscreen, volta para modo normal
          toggleFullscreen()
        } else {
          // 🎯 NAVEGAÇÃO INTELIGENTE - Voltar para categoria específica
          val returnToCategory = intent.getStringExtra("returnToCategory")
          val categoryId = intent.getStringExtra("categoryId")
          val contentType = intent.getStringExtra("contentType")
          
          if (returnToCategory != null && categoryId != null) {
            // Voltar para a categoria específica (VOD ou Series)
            val destination = if (contentType == "vod") "vod/$categoryId" else "series/$categoryId"
            android.util.Log.i("PlayerActivity", "🎯 Navegação inteligente: voltando para $destination")
            
            // Usar Intent para navegar de volta para MainActivity com dados extras
            val returnIntent = android.content.Intent().apply {
              setClassName(this@PlayerActivity, "com.maxiptv.MainActivity")
              putExtra("navigateTo", destination)
              putExtra("returnFromPlayer", true)
              putExtra("categoryId", categoryId)
              putExtra("contentType", contentType)
              flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(returnIntent)
          }
          
          // ✅ Salvar posição antes de fechar (apenas VOD/Series)
          if ((contentType == "vod" || contentType == "series") && contentId != null) {
            player?.let { exo ->
              if (exo.duration > 0) {
                val position = exo.currentPosition
                val duration = exo.duration
                lifecycleScope.launch {
                  PlaybackPositionManager.savePosition(contentId!!, contentType, position, duration)
                  android.util.Log.i("PlayerActivity", "💾 Posição salva ao sair: ${PlaybackPositionManager.formatTime(position)}")
                }
              }
            }
          }
          
          // ✅ Parar handler de salvar posição
          positionSaveHandler?.removeCallbacksAndMessages(null)
          
          // ✅ Liberar player ANTES de fechar
          android.util.Log.i("PlayerActivity", "🛑 Fechando player e liberando recursos...")
          player?.let { exo ->
            exo.stop()
            exo.clearMediaItems()
            exo.release()
            android.util.Log.i("PlayerActivity", "✅ Player liberado completamente")
          }
          player = null
          
          // Fechar o player
          finish()
        }
      }
    })
    
    pv = PlayerView(this)
    // ⚽ Resetar zoom do modo futebol (agora que pv está inicializado)
    pv.scaleX = 1.0f
    pv.scaleY = 1.0f
    // Forçar PlayerView a ocupar toda a tela, incluindo áreas do sistema
    pv.layoutParams = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, 
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    pv.fitsSystemWindows = false
    
    // ✅ Configurar padding para mover controles (incluindo engrenagem nativa) para esquerda na TV
    // ✅ Considerar overscan: adicionar padding extra para evitar que controles fiquem para fora
    val overscanPaddingDp = when {
      MaxiApp.isFireStick -> MaxiApp.fireStickOverscanPadding.coerceAtLeast(20) // Fire Stick: mínimo 20dp
      MaxiApp.isNativeTv -> 24 // Native TV: 24dp
      MaxiApp.isTvBox -> 16 // TV Box: 16dp
      else -> 0 // Smartphone: sem overscan
    }
    // Converter dp para pixels
    val density = resources.displayMetrics.density
    val rightPaddingDp = if (MaxiApp.isTv) 32 + overscanPaddingDp else 0 // TV: padding direito + overscan
    val rightPadding = (rightPaddingDp * density).toInt() // Converter para pixels
    // Configurar margens negativas para ocupar área da status bar + padding direito para controles
    pv.setPadding(0, -getStatusBarHeight(), rightPadding, 0)
    
    // ✅ PREENCHER TELA TODA (sem barras pretas)
    pv.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    
    // ✅ HABILITAR CONTROLES (pause, play, seek, avançar/retroceder)
    pv.useController = true
    pv.controllerShowTimeoutMs = 5000 // Controles somem após 5 segundos de inatividade (apenas quando tocando)
    pv.controllerHideOnTouch = false // Não esconder no toque
    
    // ✅ CONTROLES AVANÇADOS: Configurar botões de avançar/retroceder 10s
    pv.setCustomErrorMessage("")
    pv.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
    
    // ✅ COR MODERNA DO BUFFERING: Customizar cor do buffering nativo do ExoPlayer
    // O ExoPlayer usa um ProgressBar interno, vamos criar um overlay customizado com cor azul/ciano
    // Nota: O buffering nativo do ExoPlayer não pode ser facilmente customizado, então vamos
    // criar um indicador visual customizado que aparece quando está buffering
    
    // ✅ FASE 1: BOTÃO VISUAL DE QUALIDADE
    // Criar FrameLayout para adicionar botão sobre o PlayerView
    val rootLayout = FrameLayout(this).apply {
      layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      addView(pv)
    }
    
    // ✅ MELHORIA 1: Criar overlay de qualidade atual (DEPOIS de criar rootLayout)
    qualityOverlay = android.widget.TextView(this).apply {
      text = ""
      textSize = if (MaxiApp.isTv) 18f else 14f
      setTextColor(android.graphics.Color.WHITE)
      setTypeface(null, android.graphics.Typeface.BOLD)
      setPadding(16, 12, 16, 12)
      background = GradientDrawable().apply {
        setColor(android.graphics.Color.argb(200, 0, 0, 0)) // Fundo preto semi-transparente
        cornerRadius = 8f
        setStroke(2, android.graphics.Color.argb(255, 0, 212, 255)) // Borda azul ciano
      }
      gravity = android.view.Gravity.CENTER
      visibility = android.view.View.GONE
      alpha = 0f
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        android.view.Gravity.TOP or android.view.Gravity.END
      ).apply {
        setMargins(0, if (MaxiApp.isTv) 80 else 60, if (MaxiApp.isTv) 40 else 20, 0)
      }
    }
    
    // Adicionar overlay ao rootLayout (canto superior direito)
    rootLayout.addView(qualityOverlay)
    
    // ✅ BOTÕES A, CC, H REMOVIDOS - Agora estão na tela de detalhes (VodDetailsScreen)
    
    // ✅ MELHORIA 2: Criar overlay de tempo restante (VOD/Series apenas)
    remainingTimeOverlay = android.widget.TextView(this).apply {
      text = ""
      textSize = if (MaxiApp.isTv) 16f else 12f
      setTextColor(android.graphics.Color.WHITE)
      setTypeface(null, android.graphics.Typeface.BOLD)
      setPadding(12, 8, 12, 8)
      background = GradientDrawable().apply {
        setColor(android.graphics.Color.argb(180, 0, 0, 0)) // Fundo preto semi-transparente
        cornerRadius = 6f
      }
      gravity = android.view.Gravity.CENTER
      visibility = android.view.View.GONE
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        android.view.Gravity.BOTTOM or android.view.Gravity.START
      ).apply {
        setMargins(if (MaxiApp.isTv) 40 else 20, 0, 0, if (MaxiApp.isTv) 120 else 80)
      }
    }
    
    // Adicionar overlay de tempo restante ao rootLayout (canto inferior esquerdo)
    rootLayout.addView(remainingTimeOverlay)
    
    // ✅ MELHORIA 3: Criar indicador visual de buffer melhorado
    bufferIndicatorOverlay = android.widget.TextView(this).apply {
      text = ""
      textSize = if (MaxiApp.isTv) 14f else 11f
      setTextColor(android.graphics.Color.WHITE)
      setTypeface(null, android.graphics.Typeface.BOLD)
      setPadding(10, 6, 10, 6)
      gravity = android.view.Gravity.CENTER
      visibility = android.view.View.GONE
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        android.view.Gravity.TOP or android.view.Gravity.START
      ).apply {
        setMargins(if (MaxiApp.isTv) 40 else 20, if (MaxiApp.isTv) 80 else 60, 0, 0)
      }
    }
    
    // Adicionar indicador de buffer ao rootLayout (canto superior esquerdo)
    rootLayout.addView(bufferIndicatorOverlay)
    
    // ⚽ OVERLAY DE GRAMADO PARA MODO FUTEBOL será adicionado depois, se necessário
    // (será criado dinamicamente quando modo futebol for ativado)
    
    // ✅ FASE 1: Criar overlay de latência (Live apenas)
    latencyOverlay = android.widget.TextView(this).apply {
      text = ""
      textSize = if (MaxiApp.isTv) 13f else 10f
      setTextColor(android.graphics.Color.WHITE)
      setTypeface(null, android.graphics.Typeface.BOLD)
      setPadding(8, 5, 8, 5)
      gravity = android.view.Gravity.CENTER
      visibility = android.view.View.GONE
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        android.view.Gravity.TOP or android.view.Gravity.START
      ).apply {
        setMargins(if (MaxiApp.isTv) 40 else 20, if (MaxiApp.isTv) 120 else 90, 0, 0) // Abaixo do buffer indicator
      }
    }
    rootLayout.addView(latencyOverlay)
    
    // ✅ FASE 1: Criar overlay de estatísticas detalhadas (acessível via long press no buffer indicator)
    statsOverlay = android.widget.TextView(this).apply {
      text = ""
      textSize = if (MaxiApp.isTv) 12f else 9f
      setTextColor(android.graphics.Color.WHITE)
      setTypeface(null, android.graphics.Typeface.NORMAL)
      setPadding(12, 8, 12, 8)
      gravity = android.view.Gravity.START
      visibility = android.view.View.GONE
      maxLines = 8
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        android.view.Gravity.TOP or android.view.Gravity.START
      ).apply {
        setMargins(if (MaxiApp.isTv) 40 else 20, if (MaxiApp.isTv) 160 else 130, 0, 0) // Abaixo do latency overlay
      }
    }
    rootLayout.addView(statsOverlay)
    
    // ✅ LIVE PROFESSIONAL: Criar overlay profissional com informações do canal (apenas para Live)
    liveChannelInfoOverlay = android.widget.TextView(this).apply {
      text = ""
      textSize = if (MaxiApp.isTv) 14f else 11f
      setTextColor(android.graphics.Color.WHITE)
      setTypeface(null, android.graphics.Typeface.BOLD)
      setPadding(16, 12, 16, 12)
      maxLines = 4
      gravity = android.view.Gravity.START
      visibility = android.view.View.GONE
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        android.view.Gravity.TOP or android.view.Gravity.END
      ).apply {
        setMargins(0, if (MaxiApp.isTv) 40 else 20, if (MaxiApp.isTv) 40 else 20, 0)
      }
      background = GradientDrawable().apply {
        setColor(android.graphics.Color.argb(220, 0, 0, 0)) // Fundo preto semi-transparente
        cornerRadius = 8f
        setStroke(2, android.graphics.Color.argb(255, 0, 212, 255)) // Borda azul ciano
      }
    }
    rootLayout.addView(liveChannelInfoOverlay)
    
    // ✅ FASE 1: Adicionar long press listener ao buffer indicator para mostrar/esconder stats
    bufferIndicatorOverlay?.setOnLongClickListener {
      if (statsOverlay?.visibility == android.view.View.VISIBLE) {
        statsOverlay?.visibility = android.view.View.GONE
      } else {
        statsOverlay?.visibility = android.view.View.VISIBLE
        updateStreamStats() // Atualizar imediatamente
      }
      true
    }
    
    // ✅ BOTÕES A, CC, H REMOVIDOS - Agora estão na tela de detalhes (VodDetailsScreen)
    // Código removido: criação dos botões qualityButton, subtitleButton e audioButton
    
    // ✅ MELHORIA 2: Criar overlay de tempo restante (VOD/Series apenas)
    remainingTimeOverlay = android.widget.TextView(this).apply {
      text = ""
      textSize = if (MaxiApp.isTv) 16f else 12f
      setTextColor(android.graphics.Color.WHITE)
      setTypeface(null, android.graphics.Typeface.BOLD)
      setPadding(12, 8, 12, 8)
      background = GradientDrawable().apply {
        setColor(android.graphics.Color.argb(180, 0, 0, 0)) // Fundo preto semi-transparente
        cornerRadius = 6f
      }
      gravity = android.view.Gravity.CENTER
      visibility = android.view.View.GONE
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        android.view.Gravity.BOTTOM or android.view.Gravity.START
      ).apply {
        setMargins(if (MaxiApp.isTv) 40 else 20, 0, 0, if (MaxiApp.isTv) 120 else 80)
      }
    }
    
    rootLayout.addView(remainingTimeOverlay)
    
    // ✅ REMOVIDO: Todo o código de criação dos botões A, CC, H foi removido
    // Os botões agora estão na tela de detalhes (VodDetailsScreen)
    
    // ✅ Configurar estilização de legendas do PlayerView
    setupSubtitleStyle()
    
    // ✅ Listener para mostrar controles quando necessário
    pv.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
      android.util.Log.d("PlayerActivity", "Controles visíveis: $visibility")
      // ✅ BOTÕES A, CC, H REMOVIDOS - Não há mais botões para mostrar/esconder
    })
    
    // ✅ API MODERNA - GestureDetector (substitui GestureDetectorCompat depreciado)
    gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
      override fun onSingleTapUp(e: MotionEvent): Boolean {
        // ✅ Detectar interação do usuário para mostrar latência/stats
        lastUserInteraction = System.currentTimeMillis()
        showLatencyStats = true
        updateLatency()
        updateStreamStats()
        return false
      }
      
      override fun onDoubleTap(e: MotionEvent): Boolean {
        // ✅ Detectar interação do usuário
        lastUserInteraction = System.currentTimeMillis()
        showLatencyStats = true
        
        // Duplo clique: play/pause
        player?.let {
          if (it.isPlaying) {
            it.pause()
          } else {
            it.play()
          }
        }
        return true
      }
      
      override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        // Clique simples: mostrar/esconder controles
        if (pv.isControllerFullyVisible) {
          pv.hideController()
        } else {
          pv.showController()
        }
        return true
      }
    })
    
    pv.setOnTouchListener { _, event ->
      gestureDetector.onTouchEvent(event)
      false
    }
    
    setContentView(rootLayout) // Usar rootLayout em vez de pv diretamente
    val url = intent.getStringExtra("url") ?: return
    contentType = intent.getStringExtra("contentType") ?: "live" // live, vod ou series
    val channelName = intent.getStringExtra("channelName") ?: ""
    
    // ✅ Detectar se é canal de futebol pelo nome
    val channelNameLower = channelName.lowercase().trim()
    android.util.Log.d("PlayerActivity", "🔍 Verificando canal: '$channelName' (lowercase: '$channelNameLower')")
    
    // ✅ CANAIS ESPECÍFICOS DE FUTEBOL
    val footballChannels = listOf(
      "band sport", "sportv", "cazetv", "caze tv"
    )
    
    // ✅ DETECÇÃO POR INÍCIO DO NOME (mais preciso)
    val startsWithFootball = channelNameLower.startsWith("premiere") ||
                            channelNameLower.startsWith("espn") ||
                            channelNameLower.startsWith("sportynet") ||
                            channelNameLower.startsWith("brasileirao") ||
                            channelNameLower.startsWith("copa")
    
    // ✅ TERMOS ESPECÍFICOS NO NOME
    val hasSpecificTerm = channelNameLower.contains("campeonato de futebol") ||
                          channelNameLower.contains("campeonato futebol")
    
    // Termos mais específicos primeiro (maior prioridade)
    val isSpecificFootballChannel = footballChannels.any { 
      channelNameLower.contains(it.lowercase()) 
    } || startsWithFootball || hasSpecificTerm
    
    // Termos genéricos (menor prioridade, apenas se não for muito genérico)
    val genericTerms = listOf("sport", "futebol", "futbol")
    val hasGenericTerm = genericTerms.any { 
      channelNameLower.contains(it.lowercase()) && 
      !channelNameLower.contains("news") && // Excluir "sport news" etc
      !channelNameLower.contains("noticias")
    }
    
    isFootballMode = contentType == "live" && (isSpecificFootballChannel || hasGenericTerm)
    
    // ⚽ NOVO: Tentar extrair matchId do nome do canal
    if (isFootballMode) {
      currentMatchId = com.maxiptv.data.soccer.MatchIdExtractor.extractMatchId(channelName)
      if (currentMatchId != null) {
        android.util.Log.i("PlayerActivity", "⚽ MatchId extraído: $currentMatchId")
      } else {
        android.util.Log.i("PlayerActivity", "⚽ MatchId não encontrado no nome do canal")
      }
    }
    
    if (isFootballMode) {
      android.util.Log.i("PlayerActivity", "⚽ MODO FUTEBOL ATIVADO para: '$channelName'")
      android.util.Log.i("PlayerActivity", "   - Canal específico: $isSpecificFootballChannel")
      android.util.Log.i("PlayerActivity", "   - Termo genérico: $hasGenericTerm")
      android.util.Log.i("PlayerActivity", "   - MatchId: ${currentMatchId ?: "não disponível"}")
    } else {
      android.util.Log.d("PlayerActivity", "📺 Modo normal (não é futebol): '$channelName'")
    }
    
    // ✅ Obter ID do conteúdo para salvar posição (apenas VOD/Series)
    if (contentType == "vod" || contentType == "series") {
      contentId = intent.getIntExtra("contentId", -1).takeIf { it > 0 }
      android.util.Log.d("PlayerActivity", "📌 ContentId para salvar posição: $contentId")
    }
    
    // ✅ LER CONFIGURAÇÕES DO INTENT (legendas e áudio selecionados na tela de detalhes)
    // ✅ Configurações serão lidas diretamente em onTracksChanged quando os tracks estiverem disponíveis
    android.util.Log.d("PlayerActivity", "🔍 PlayerActivity iniciado - configurações serão aplicadas quando tracks estiverem disponíveis")
    
    // ✅ MELHORIA 2: Mostrar/ocultar overlay de tempo restante baseado no tipo de conteúdo
    val isVodOrSeries = contentType == "vod" || contentType == "series"
    remainingTimeOverlay?.visibility = if (isVodOrSeries) android.view.View.VISIBLE else android.view.View.GONE
    
    // ⚽ CRIAR OVERLAY DE GRAMADO PARA MODO FUTEBOL (apenas se necessário)
    if (isFootballMode) {
      createFootballOverlay(rootLayout)
      android.util.Log.i("PlayerActivity", "⚽ Overlay de gramado criado para modo futebol")
      
      // ✅ Carregar preferências de futebol e criar botão
      // ⚽ Novo jogo: sempre reativar botão (mesmo que tenha sido desativado antes)
      // O usuário pode desativar novamente se não quiser neste jogo específico
      footballStatsButtonEnabled = true // Sempre ativado em novo jogo
      
      lifecycleScope.launch {
        footballAutoZoomEnabled = PlayerSettingsManager.isFootballAutoZoomEnabled()
      }
      
      // Criar botão de estatísticas imediatamente (não dentro de corrotina)
      createFootballStatsButton(rootLayout)
      android.util.Log.i("PlayerActivity", "⚽ Botão de estatísticas criado para modo futebol")
      
      // ⚽ NOVO: Inicializar ViewModel de estatísticas se houver matchId
      if (currentMatchId != null) {
        soccerStatsViewModel = com.maxiptv.ui.player.soccer.SoccerStatsViewModel()
        android.util.Log.i("PlayerActivity", "⚽ ViewModel de estatísticas inicializado para matchId: $currentMatchId")
      }
    }
    
    // Log da URL para debug
    android.util.Log.i("PlayerActivity", "=== REPRODUZINDO URL ===")
    android.util.Log.i("PlayerActivity", "URL: $url")
    android.util.Log.i("PlayerActivity", "TIPO: $contentType")
    android.util.Log.i("PlayerActivity", "=======================")
    
    // ⚡ Configurar DataSource com timeouts diferentes para LIVE vs VOD/SERIES vs FUTEBOL
    val isLive = contentType == "live"
    val connectTimeout = when {
      isFootballMode -> 15000  // ⚽ FUTEBOL: 15s (mais tempo para conexão estável)
      isLive -> 12000          // ✅ LIVE: 12s (AUMENTADO de 8s - mais tempo para conexão)
      else -> 8000             // VOD/SERIES: 8s
    }
    val readTimeout = when {
      isFootballMode -> 20000  // ⚽ FUTEBOL: 20s (mais tempo para leitura estável)
      isLive -> 15000          // ✅ LIVE: 15s (AUMENTADO de 10s - mais tempo para leitura)
      else -> 10000            // VOD/SERIES: 10s
    }
    
    // 🌐 DNS OTIMIZADO: Priorizar IPv4 para melhor compatibilidade
    val customDns = object : Dns {
      override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        // Priorizar endereços IPv4
        return addresses.sortedBy { if (it is Inet4Address) 0 else 1 }
      }
    }
    
    // 🚀 OkHttp otimizado para IPTV
    val okHttpClient = OkHttpClient.Builder()
      .dns(customDns)
      .connectTimeout(connectTimeout.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
      .readTimeout(readTimeout.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
      .retryOnConnectionFailure(true)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()
    
    // 🌐 XTREAM CODE API: Configurar DataSource com headers adequados
    // ✅ OkHttpClient já configurado com followRedirects e followSslRedirects
    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
      .setUserAgent("MaxiPTV/1.1.1 (Android)")
    
    // ✅ DefaultMediaSourceFactory detecta automaticamente formato baseado na URL/extensão
    // Suporta: .m3u8 (HLS), .mp4/.mpd (DASH), .mp4/.ts (Progressive)
    // Xtream Code usa: live/.../stream_id.m3u8 (HLS), movie/.../id.mp4 (Progressive), series/.../id.mp4 (Progressive)
    val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
    
    // ⚡ CACHE OTIMIZADO: Configurações diferentes para LIVE vs VOD/SERIES vs FUTEBOL
    // ✅ FASE 1: Buffer dinâmico baseado em qualidade de conexão estimada (inicia com GOOD)
    val loadControl: LoadControl = if (isFootballMode) {
      // ⚽ FUTEBOL: Buffer otimizado para evitar travamento em transmissões esportivas
      // Buffer maior para estabilidade, mas ainda com baixa latência
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          5000,   // minBufferMs: 5 segundos (maior que live normal para estabilidade)
          15000,  // maxBufferMs: 15 segundos (buffer maior para evitar travamentos)
          2000,   // bufferForPlaybackMs: 2 segundos (start rápido mas estável)
          4000    // bufferForPlaybackAfterRebufferMs: 4 segundos (reconexão mais estável)
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(5000, true) // 5s de back buffer para futebol
        .build()
    } else if (isLive) {
      // 📺 LIVE: Buffer dinâmico baseado em qualidade de conexão
      // Inicia com qualidade GOOD, será ajustado dinamicamente conforme estatísticas
      createAdaptiveLoadControl(isLive = true)
    } else {
      // 🎬 VOD/SERIES: Buffers ULTRA REDUZIDOS para Wi-Fi lento (evita travamentos)
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          3000,   // minBufferMs: 3 segundos (ULTRA REDUZIDO - start rápido)
          8000,   // maxBufferMs: 8 segundos (ULTRA REDUZIDO - evita acúmulo)
          1000,   // bufferForPlaybackMs: 1 segundo (start instantâneo)
          2000    // bufferForPlaybackAfterRebufferMs: 2 segundos (reconexão rápida)
        )
        .setPrioritizeTimeOverSizeThresholds(true) // Prioriza tempo real (como LIVE)
        .setBackBuffer(2000, true) // 2s de back buffer (ULTRA REDUZIDO)
        .build()
    }
    
    player = ExoPlayer.Builder(this)
      .setMediaSourceFactory(mediaSourceFactory)
      .setLoadControl(loadControl) // ✅ Aplicar cache otimizado
      .build().also { exo ->
        pv.player = exo
        
        // ✅ FASE 2: Salvar URL original para failover
        originalStreamUrl = url
        
        // 🎬 CONFIGURAR MEDIAITEM COM LIVE CONFIGURATION
        // ✅ FASE 2: Modo Low Latency HLS para reduzir latência
        val mediaItem = if (isFootballMode) {
          // ⚽ FUTEBOL: Configuração otimizada para evitar travamento
          MediaItem.Builder()
            .setUri(url)
            .setLiveConfiguration(
              MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(3000) // ⚽ FUTEBOL: 3s de offset (mais estável que live normal)
                .setMinOffsetMs(2000) // ⚽ FUTEBOL: Offset mínimo 2s (mais tolerante)
                .setMaxOffsetMs(8000) // ⚽ FUTEBOL: Máximo 8s (mais margem para estabilidade)
                .setMinPlaybackSpeed(0.92f) // ⚽ FUTEBOL: Velocidade mínima 0.92 (mais tolerante)
                .setMaxPlaybackSpeed(1.08f) // ⚽ FUTEBOL: Velocidade máxima 1.08 (mais tolerante)
                .build()
            )
            .build()
        } else if (isLive) {
          MediaItem.Builder()
            .setUri(url)
            .setLiveConfiguration(
              MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(2000) // ✅ Low Latency AJUSTADO: 2s de offset (era 0) - mais estável
                .setMinOffsetMs(1000) // ✅ Low Latency AJUSTADO: Offset mínimo 1s (era 0) - evitar travamentos
                .setMaxOffsetMs(5000) // ✅ Low Latency AJUSTADO: Máximo 5s de atraso (era 3s) - mais estabilidade
                .setMinPlaybackSpeed(0.95f) // ✅ Low Latency AJUSTADO: Velocidade mínima 0.95 (era 0.98) - mais tolerante
                .setMaxPlaybackSpeed(1.05f) // ✅ Low Latency AJUSTADO: Velocidade máxima 1.05 (era 1.02) - mais tolerante
                .build()
            )
            .build()
        } else {
          MediaItem.fromUri(url)
        }
        
        exo.setMediaItem(mediaItem)
        
        // ✅ FASE 1: APLICAR CONFIGURAÇÕES DO PlayerSettingsManager
        lifecycleScope.launch {
          try {
            // Aplicar velocidade de reprodução configurada
            val playbackSpeed = PlayerSettingsManager.getPlaybackSpeed()
            exo.playbackParameters = PlaybackParameters(playbackSpeed.multiplier)
            android.util.Log.i("PlayerActivity", "✅ Velocidade aplicada: ${playbackSpeed.displayName} (${playbackSpeed.multiplier}x)")
            
            // ✅ IMPORTANTE: Aguardar um pouco para garantir que PlayerSettingsManager tenha salvo a qualidade
            kotlinx.coroutines.delay(100)
            
            // Aplicar qualidade de vídeo configurada
            val videoQuality = PlayerSettingsManager.getVideoQuality()
            android.util.Log.i("PlayerActivity", "🔍 Qualidade lida do PlayerSettingsManager: ${videoQuality.displayName}")
            
            if (videoQuality != PlayerSettingsManager.VideoQuality.AUTO) {
              currentMaxBitrate = videoQuality.maxBitrate
              val (width, height) = when (videoQuality) {
                PlayerSettingsManager.VideoQuality.HD -> 1280 to 720
                PlayerSettingsManager.VideoQuality.SD -> 854 to 480
                PlayerSettingsManager.VideoQuality.ULTRA_LOW -> 640 to 360
                else -> 1280 to 720
              }
              exo.trackSelectionParameters = TrackSelectionParameters.Builder(this@PlayerActivity)
                .setPreferredTextLanguage(null)
                .setMaxVideoBitrate(videoQuality.maxBitrate)
                .setMinVideoBitrate(videoQuality.minBitrate)
                .setMaxVideoSize(width, height)
                .build()
              android.util.Log.i("PlayerActivity", "✅ Qualidade aplicada: ${videoQuality.displayName} (${videoQuality.maxBitrate / 1000}Kbps, ${width}x${height})")
            } else {
              // Qualidade automática: usar valores padrão (otimizados para futebol)
              currentMaxBitrate = when {
                isFootballMode -> 3_000_000  // ⚽ FUTEBOL: 3Mbps (maior qualidade para ver detalhes)
                isLive -> 2_200_000         // ✅ LIVE: 2.2Mbps
                else -> 2_500_000           // VOD/SERIES: 2.5Mbps
              }
              exo.trackSelectionParameters = TrackSelectionParameters.Builder(this@PlayerActivity)
                .setPreferredTextLanguage(null)
                .setMaxVideoBitrate(currentMaxBitrate)
                .setMaxVideoSize(if (isFootballMode) 1920 else 1280, if (isFootballMode) 1080 else 720) // ⚽ FUTEBOL: até 1080p
                .setMinVideoBitrate(when {
                  isFootballMode -> 800_000  // ⚽ FUTEBOL: mínimo 800Kbps (mais estável)
                  isLive -> 500_000
                  else -> 400_000
                })
                .build()
              android.util.Log.i("PlayerActivity", "✅ Qualidade automática aplicada${if (isFootballMode) " (MODO FUTEBOL)" else ""}")
            }
          } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "❌ Erro ao aplicar configurações: ${e.message}", e)
            // Usar valores padrão em caso de erro
            currentMaxBitrate = if (isLive) 2_200_000 else 2_500_000
            exo.trackSelectionParameters = TrackSelectionParameters.Builder(this@PlayerActivity)
              .setPreferredTextLanguage(null)
              .setMaxVideoBitrate(currentMaxBitrate)
              .setMaxVideoSize(1280, 720)
              .setMinVideoBitrate(if (isLive) 500_000 else 400_000)
              .build()
          }
        }
        
        // ✅ MATCH-FRAME VIDEO: Frame pacing e FPS matching para evitar stutter em TVs 120Hz
        // Sincronizar FPS do vídeo com refresh rate da TV (Samsung, Philco, etc.)
        try {
          // Usar API do ExoPlayer para sincronizar frame rate
          exo.videoChangeFrameRateStrategy = C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS
          android.util.Log.i("PlayerActivity", "✅ Match-Frame Video habilitado: FPS sincronizado com refresh rate da TV")
        } catch (e: Exception) {
          android.util.Log.w("PlayerActivity", "⚠️ Match-Frame Video não disponível nesta versão do ExoPlayer: ${e.message}")
        }
        
        exo.prepare()
        
        // ✅ Restaurar posição salva se fornecida (apenas VOD/Series)
        if ((contentType == "vod" || contentType == "series") && contentId != null) {
          val savedPosition = intent.getLongExtra("savedPosition", -1L)
          if (savedPosition > 0) {
            android.util.Log.i("PlayerActivity", "⏩ Restaurando posição salva: ${PlaybackPositionManager.formatTime(savedPosition)}")
            exo.seekTo(savedPosition)
          }
        }
        
        exo.playWhenReady = true
        
        // ⚽ Iniciar simulação de eventos de futebol (apenas para teste)
        if (isFootballMode && footballAutoZoomEnabled) {
          simulateFootballEvents()
        }
        
        // ✅ SALVAR POSIÇÃO PERIODICAMENTE (apenas VOD/Series)
        if ((contentType == "vod" || contentType == "series") && contentId != null) {
          positionSaveHandler = android.os.Handler(android.os.Looper.getMainLooper())
          val savePositionRunnable = object : Runnable {
            override fun run() {
              if (exo.isPlaying && exo.duration > 0) {
                val position = exo.currentPosition
                val duration = exo.duration
                lifecycleScope.launch {
                  PlaybackPositionManager.savePosition(contentId!!, contentType, position, duration)
                }
              }
              positionSaveHandler?.postDelayed(this, 10000) // Salvar a cada 10 segundos
            }
          }
          positionSaveHandler?.postDelayed(savePositionRunnable, 10000)
        }
        
        // ✅ APLICAR CONFIGURAÇÕES DE LEGENDAS E ÁUDIO DO INTENT
        // Aguardar tracks serem carregados antes de aplicar configurações
        exo.addListener(object : Player.Listener {
          override fun onTracksChanged(tracks: Tracks) {
            // ✅ Aplicar configurações quando tracks estiverem disponíveis
            if (tracks.groups.isNotEmpty()) {
              android.util.Log.d("PlayerActivity", "✅ Tracks carregados, aplicando configurações do Intent...")
              
              // ✅ Aplicar legenda selecionada
              val subtitleTrackIdStr = intent.getStringExtra("selectedSubtitleTrack")
              android.util.Log.d("PlayerActivity", "🔍 Legenda recebida do Intent: '$subtitleTrackIdStr'")
              if (subtitleTrackIdStr != null && subtitleTrackIdStr.isNotBlank()) {
                var subtitleApplied = false
                tracks.groups.forEach { group ->
                  if (group.type == C.TRACK_TYPE_TEXT) {
                    for (i in 0 until group.length) {
                      val trackFormat = group.getTrackFormat(i)
                      // ✅ Comparar IDs como String para garantir compatibilidade
                      if (trackFormat.id.toString() == subtitleTrackIdStr) {
                        val currentParams = exo.trackSelectionParameters
                        val params = TrackSelectionParameters.Builder(this@PlayerActivity)
                          .setMaxVideoBitrate(currentParams.maxVideoBitrate)
                          .setMinVideoBitrate(currentParams.minVideoBitrate)
                          .setMaxVideoSize(1280, 720) // Valor padrão SD
                          .setPreferredTextLanguage(trackFormat.language)
                          .setPreferredAudioLanguage(currentParams.preferredAudioLanguages.firstOrNull())
                          .build()
                        exo.trackSelectionParameters = params
                        subtitlesEnabled = true
                        pv.subtitleView?.visibility = android.view.View.VISIBLE
                        android.util.Log.i("PlayerActivity", "✅ Legenda aplicada do Intent: ${trackFormat.language} (ID: $subtitleTrackIdStr)")
                        subtitleApplied = true
                        return@forEach
                      }
                    }
                  }
                }
                if (!subtitleApplied) {
                  android.util.Log.w("PlayerActivity", "⚠️ Legenda selecionada (ID: $subtitleTrackIdStr) não encontrada nos tracks disponíveis")
                }
              } else {
                // ✅ Desativar legendas (quando selectedSubtitleTrack é vazio/null)
                val currentParams = exo.trackSelectionParameters
                val params = TrackSelectionParameters.Builder(this@PlayerActivity)
                  .setMaxVideoBitrate(currentParams.maxVideoBitrate)
                  .setMinVideoBitrate(currentParams.minVideoBitrate)
                  .setMaxVideoSize(1280, 720) // Valor padrão SD
                  .setPreferredTextLanguage(null) // null = desativar legendas
                  .setPreferredAudioLanguage(currentParams.preferredAudioLanguages.firstOrNull())
                  .build()
                exo.trackSelectionParameters = params
                subtitlesEnabled = false
                pv.subtitleView?.visibility = android.view.View.GONE
                android.util.Log.i("PlayerActivity", "✅ Legendas desativadas do Intent")
              }
              
              // ✅ Aplicar áudio selecionado
              val audioTrackIdStr = intent.getStringExtra("selectedAudioTrack")
              android.util.Log.d("PlayerActivity", "🔍 Áudio recebido do Intent: '$audioTrackIdStr'")
              if (audioTrackIdStr != null && audioTrackIdStr.isNotBlank()) {
                var audioApplied = false
                tracks.groups.forEach { group ->
                  if (group.type == C.TRACK_TYPE_AUDIO) {
                    for (i in 0 until group.length) {
                      val trackFormat = group.getTrackFormat(i)
                      // ✅ Comparar IDs como String para garantir compatibilidade
                      if (trackFormat.id.toString() == audioTrackIdStr) {
                        val currentParams = exo.trackSelectionParameters
                        val params = TrackSelectionParameters.Builder(this@PlayerActivity)
                          .setMaxVideoBitrate(currentParams.maxVideoBitrate)
                          .setMinVideoBitrate(currentParams.minVideoBitrate)
                          .setMaxVideoSize(1280, 720) // Valor padrão SD
                          .setPreferredTextLanguage(currentParams.preferredTextLanguages.firstOrNull())
                          .setPreferredAudioLanguage(trackFormat.language)
                          .build()
                        exo.trackSelectionParameters = params
                        android.util.Log.i("PlayerActivity", "✅ Áudio aplicado do Intent: ${trackFormat.language} (ID: $audioTrackIdStr)")
                        audioApplied = true
                        return@forEach
                      }
                    }
                  }
                }
                if (!audioApplied) {
                  android.util.Log.w("PlayerActivity", "⚠️ Áudio selecionado (ID: $audioTrackIdStr) não encontrado nos tracks disponíveis")
                }
              }
            }
          }
          
          override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
              Player.STATE_IDLE -> {
                android.util.Log.w("PlayerActivity", "⚠️ Player em IDLE")
              }
              Player.STATE_BUFFERING -> {
                val now = System.currentTimeMillis()
                
                // ✅ DETECÇÃO MELHORADA DE WI-FI LENTO: Múltiplos fatores de detecção
                val bufferAhead = exo.bufferedPosition - exo.currentPosition
                val timeSinceLastBuffering = if (lastBufferingTime > 0) now - lastBufferingTime else Long.MAX_VALUE
                
                // Fator 1: Buffering frequente (mais sensível - 2 eventos em 5s)
                if (lastBufferingTime > 0 && timeSinceLastBuffering < 5000) {
                  bufferingCount++
                  android.util.Log.w("PlayerActivity", "⚠️ Buffering frequente detectado ($bufferingCount eventos em ${timeSinceLastBuffering / 1000}s)")
                } else if (timeSinceLastBuffering > 10000) {
                  // Reset contador se buffering espaçado (rede normal)
                  bufferingCount = 0
                  android.util.Log.d("PlayerActivity", "✅ Rede estável, resetando contador de buffering")
                }
                
                // Fator 2: Buffer muito baixo (< 2 segundos)
                val bufferLow = bufferAhead < 2000
                if (bufferLow) {
                  android.util.Log.w("PlayerActivity", "⚠️ Buffer muito baixo: ${bufferAhead}ms")
                }
                
                // Fator 3: Detecção usando ConnectionQuality
                // ✅ Calcular latência antes de estimar qualidade
                val latencyMs = calculateLatency()
                val estimatedQuality = estimateConnectionQuality(
                  exo,
                  latencyMs = latencyMs,
                  bufferAhead = bufferAhead,
                  bitrate = exo.videoFormat?.bitrate ?: 0
                )
                
                // ✅ Atualizar qualidade de conexão
                val previousQuality = connectionQuality
                connectionQuality = estimatedQuality
                
                // Log quando qualidade muda
                if (previousQuality != estimatedQuality) {
                  android.util.Log.w("PlayerActivity", "📊 Qualidade de conexão mudou: $previousQuality → $estimatedQuality")
                  android.util.Log.w("PlayerActivity", "   - Latência: ${latencyMs}ms")
                  android.util.Log.w("PlayerActivity", "   - Buffer: ${bufferAhead}ms")
                  android.util.Log.w("PlayerActivity", "   - Bitrate: ${exo.videoFormat?.bitrate ?: 0 / 1000}kbps")
                }
                
                // ✅ REDUÇÃO GRADUAL DE QUALIDADE baseada em múltiplos fatores
                val shouldReduceQuality = when {
                  // Redução imediata: buffering muito frequente OU buffer muito baixo + qualidade ruim
                  bufferingCount >= 2 && (estimatedQuality == ConnectionQuality.POOR || bufferLow) -> {
                    android.util.Log.w("PlayerActivity", "🚨 Redução IMEDIATA: buffering frequente + conexão ruim")
                    true
                  }
                  // Redução leve: buffering frequente OU buffer baixo
                  bufferingCount >= 2 || (bufferLow && estimatedQuality == ConnectionQuality.POOR) -> {
                    android.util.Log.w("PlayerActivity", "⚠️ Redução LEVE: buffering ou buffer baixo detectado")
                    true
                  }
                  // Redução preventiva: qualidade ruim detectada
                  estimatedQuality == ConnectionQuality.POOR && qualityReductionLevel == 0 -> {
                    android.util.Log.w("PlayerActivity", "📉 Redução PREVENTIVA: qualidade de conexão ruim")
                    true
                  }
                  else -> false
                }
                
                if (shouldReduceQuality && currentMaxBitrate > 800_000) {
                  // ✅ Redução gradual baseada no nível atual
                  // ✅ Redução mais agressiva para conexões ruins
                  val newBitrate = when (qualityReductionLevel) {
                    0 -> if (isLive) 1_500_000 else 1_800_000  // Nível 1: Redução leve (2.2Mbps → 1.5Mbps live / 1.8Mbps vod)
                    1 -> if (isLive) 1_000_000 else 1_200_000  // Nível 2: Redução média (1.5Mbps → 1.0Mbps live / 1.2Mbps vod)
                    2 -> if (isLive) 600_000 else 800_000      // Nível 3: Redução alta (1.0Mbps → 600kbps live / 800kbps vod)
                    else -> currentMaxBitrate // Não reduzir mais
                  }
                  
                  if (newBitrate < currentMaxBitrate) {
                    qualityReductionLevel++
                    qualityReduced = true
                    currentMaxBitrate = newBitrate
                    
                    val newResolution = when (qualityReductionLevel) {
                      1 -> Pair(1280, 720)  // 720p
                      2 -> Pair(854, 480)   // 480p
                      else -> Pair(640, 360) // 360p
                    }
                    
                    android.util.Log.i("PlayerActivity", "📉 Wi-Fi lento detectado! Reduzindo qualidade (nível $qualityReductionLevel)")
                    android.util.Log.i("PlayerActivity", "   Bitrate: ${currentMaxBitrate / 1000}kbps")
                    android.util.Log.i("PlayerActivity", "   Resolução: ${newResolution.first}x${newResolution.second}")
                    android.util.Log.i("PlayerActivity", "   Qualidade conexão: $estimatedQuality")
                    
                    // ✅ Aplicar novo bitrate e forçar re-seleção de tracks
                    val newParams = TrackSelectionParameters.Builder(this@PlayerActivity)
                      .setPreferredTextLanguage(null)
                      .setMaxVideoBitrate(currentMaxBitrate)
                      .setMaxVideoSize(newResolution.first, newResolution.second)
                      .setMinVideoBitrate(if (isLive) (currentMaxBitrate * 0.3).toInt() else (currentMaxBitrate * 0.25).toInt())
                      .build()
                    
                    exo.trackSelectionParameters = newParams
                    android.util.Log.i("PlayerActivity", "✅ Qualidade reduzida automaticamente para evitar travamentos")
                  }
                }
                
                lastBufferingTime = now
                lastBufferSize = bufferAhead
                android.util.Log.i("PlayerActivity", "⏳ Bufferizando... (contador: $bufferingCount, buffer: ${bufferAhead}ms, qualidade: $estimatedQuality)")
              }
              Player.STATE_READY -> {
                android.util.Log.i("PlayerActivity", "✅ Player pronto")
                // Log de qualidade e performance
                val format = exo.videoFormat
                if (format != null) {
                  val bitrate = format.bitrate / 1000 // Kbps
                  val resolution = "${format.width}x${format.height}"
                  val speed = exo.playbackParameters.speed
                  android.util.Log.i("PlayerActivity", "📊 Qualidade: $resolution @ ${bitrate}kbps | Velocidade: ${speed}x")
                  
                  // ✅ FASE 1: Indicador visual de qualidade atual (pode ser expandido depois)
                  // Por enquanto apenas log, mas pode adicionar overlay visual
                }
                
                // ✅ MELHORIA 2: Iniciar atualização de tempo restante para VOD/Series
                if (contentType == "vod" || contentType == "series") {
                  startRemainingTimeUpdates()
                }
                
                // ✅ MELHORIA 3: Iniciar atualização de indicador de buffer
                startBufferIndicatorUpdates()
                // ✅ FASE 1: Iniciar atualização de latência e estatísticas (apenas para Live)
                if (contentType == "live") {
                  startLatencyUpdates()
                  startStatsUpdates()
                  startLiveChannelInfoUpdates() // ✅ LIVE PROFESSIONAL: Iniciar atualização de informações do canal
                }
              }
              Player.STATE_ENDED -> {
                android.util.Log.i("PlayerActivity", "🏁 Reprodução finalizada")
              }
            }
          }
          
          override fun onVideoSizeChanged(videoSize: VideoSize) {
            android.util.Log.i("PlayerActivity", "📺 Tamanho do vídeo: ${videoSize.width}x${videoSize.height}")
            
            // ✅ MELHORIA 1: Mostrar indicador de qualidade quando tamanho mudar
            val format = exo.videoFormat
            if (format != null) {
              val resolution = "${format.width}x${format.height}"
              val bitrate = format.bitrate
              showQualityIndicator(resolution, bitrate)
              android.util.Log.i("PlayerActivity", "📊 Qualidade: $resolution @ ${bitrate / 1000}Kbps")
              
              // ✅ FASE 2: Detectar degradação de qualidade
              detectQualityDegradation(format)
              lastVideoFormat = format // Salvar formato atual para comparação futura
            }
          }
          
          override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.e("PlayerActivity", "❌ Erro no player: ${error.message}", error)
            android.util.Log.e("PlayerActivity", "   Tipo: ${error.errorCode}")
            android.util.Log.e("PlayerActivity", "   Causa: ${error.cause}")
            
            // ✅ FASE 2: Tentar failover automático em caso de erro (prioridade)
            if (contentType == "live" && failoverAttempts < maxFailoverAttempts) {
              android.util.Log.i("PlayerActivity", "🔄 Tentando failover automático (tentativa ${failoverAttempts + 1}/$maxFailoverAttempts)...")
              retryWithFailover(originalStreamUrl, failoverAttempts + 1)
            } else {
              // ⚡ RECONEXÃO AUTOMÁTICA MELHORADA (com limite de tentativas) - fallback
              if (reconnectAttempts < maxReconnectAttempts) {
                reconnectAttempts++
                val retryDelay = if (isFootballMode) 1000L else 2000L // ⚽ FUTEBOL: reconexão mais rápida (1s vs 2s)
                android.util.Log.i("PlayerActivity", "🔄 Tentativa de reconexão $reconnectAttempts/$maxReconnectAttempts em ${retryDelay}ms...")
                
                pv.postDelayed({
                  try {
                    android.util.Log.i("PlayerActivity", "🔄 Reconectando...")
                    // Limpar buffer antes de reconectar
                    exo.stop()
                    exo.clearMediaItems()
                    exo.setMediaItem(mediaItem) // ✅ Usar mediaItem configurado
                    exo.prepare()
                    exo.playWhenReady = true
                    android.util.Log.i("PlayerActivity", "✅ Reconexão iniciada")
                  } catch (e: Exception) {
                    android.util.Log.e("PlayerActivity", "❌ Falha na reconexão: ${e.message}")
                  }
                }, retryDelay)
              } else {
                android.util.Log.e("PlayerActivity", "❌ Máximo de tentativas atingido. Verifique sua conexão.")
              }
            }
          }
          
          override fun onIsPlayingChanged(isPlaying: Boolean) {
            // ✅ Mostrar/esconder botões modernos quando pausa
            if (isPlaying) {
              hidePauseControls()
              // Reset contador quando voltar a tocar normalmente
              reconnectAttempts = 0
              
              // Player tocando: timeout normal para controles
              pv.controllerShowTimeoutMs = 5000
            } else {
              // Player pausado: mostrar botões modernos
              showPauseControls()
            }
              
              // ✅ DETECÇÃO DE TRAVAMENTO: Verificar se player não está progredindo
              if (isLive) {
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val checkProgress = object : Runnable {
                  override fun run() {
                    if (exo.isPlaying && exo.playbackState == Player.STATE_READY) {
                      val currentPos = exo.currentPosition
                      val now = System.currentTimeMillis()
                      
                      if (lastPosition == currentPos && lastPositionTime > 0) {
                        // Posição não mudou - verificar há quanto tempo
                        val timeStuck = now - lastPositionTime
                        if (timeStuck > 8000) { // Travado por mais de 8 segundos
                          android.util.Log.w("PlayerActivity", "⚠️ Travamento detectado! Posição não mudou há ${timeStuck / 1000}s")
                          if (reconnectAttempts < maxReconnectAttempts) {
                            reconnectAttempts++
                            android.util.Log.i("PlayerActivity", "🔄 Reconectando devido a travamento...")
                            exo.stop()
                            exo.clearMediaItems()
                            exo.setMediaItem(mediaItem)
                            exo.prepare()
                            exo.playWhenReady = true
                            lastPosition = 0L
                            lastPositionTime = 0L
                          }
                        }
                      } else {
                        // Posição mudou - atualizar
                        lastPosition = currentPos
                        lastPositionTime = now
                      }
                      
                      // Verificar novamente em 2 segundos
                      handler.postDelayed(this, 2000)
                    }
                  }
                }
                handler.postDelayed(checkProgress, 2000) // Começar verificação após 2 segundos
              }
              
              // ✅ Se está tocando bem por mais de 30 segundos, resetar contador de buffering e qualidade
              android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (exo.isPlaying && bufferingCount == 0 && connectionQuality != ConnectionQuality.POOR) {
                  // Rede melhorou - resetar redução de qualidade gradualmente
                  if (qualityReductionLevel > 0) {
                    qualityReductionLevel = 0
                    qualityReduced = false
                    // Restaurar bitrate original
                    currentMaxBitrate = if (isLive) 2_200_000 else 2_500_000
                    val restoreParams = TrackSelectionParameters.Builder(this@PlayerActivity)
                      .setPreferredTextLanguage(null)
                      .setMaxVideoBitrate(currentMaxBitrate)
                      .setMaxVideoSize(1280, 720) // Restaurar para 720p
                      .setMinVideoBitrate(if (isLive) 500_000 else 400_000)
                      .build()
                    exo.trackSelectionParameters = restoreParams
                    android.util.Log.i("PlayerActivity", "✅ Rede melhorou! Restaurando qualidade original (${currentMaxBitrate / 1000}kbps)")
                  }
                  android.util.Log.d("PlayerActivity", "✅ Reprodução estável, resetando detecção de Wi-Fi lento")
                }
              }, 30000) // 30 segundos
          }
        })
      }
    
    // ✅ Configurar estilização de legendas do PlayerView
    setupSubtitleStyle()
  }
  
  private fun toggleFullscreen() {
    isFullscreen = !isFullscreen
    if (isFullscreen) {
      // ✅ API MODERNA - Entrar em fullscreen
      // ✅ IMPORTANTE: No Fire Stick, garantir que diálogos ainda possam ser exibidos
      windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
      // ✅ BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE permite que barras apareçam temporariamente
      // quando necessário (ex: para exibir diálogos do sistema)
      windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      
      // ✅ Fire Stick: Garantir que a janela permite diálogos mesmo em fullscreen
      if (MaxiApp.isFireStick) {
        // Não bloquear diálogos em fullscreen no Fire Stick
        window.setFlags(
          android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
          android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        android.util.Log.d("PlayerActivity", "📺 Fire Stick: Fullscreen configurado para permitir diálogos")
      }
    } else {
      // ✅ API MODERNA - Sair de fullscreen
      windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
      
      // ✅ Fire Stick: Restaurar flags normais ao sair do fullscreen
      if (MaxiApp.isFireStick) {
        window.clearFlags(
          android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        android.util.Log.d("PlayerActivity", "📺 Fire Stick: Saindo do fullscreen, flags restauradas")
      }
    }
  }
  
  override fun onPause() {
    super.onPause()
    // ✅ MELHORIA 2: Pausar atualização de tempo restante
    stopRemainingTimeUpdates()
    // ✅ MELHORIA 3: Pausar atualização de indicador de buffer
    stopBufferIndicatorUpdates()
    // ✅ FASE 1: Pausar atualização de latência e estatísticas
    stopLatencyUpdates()
    stopStatsUpdates()
    stopLiveChannelInfoUpdates() // ✅ LIVE PROFESSIONAL
    // ✅ Pausar player quando Activity perde foco
    player?.let { exo ->
      if (exo.isPlaying) {
        exo.pause()
        android.util.Log.d("PlayerActivity", "⏸️ Player pausado em onPause")
      }
    }
  }
  
  override fun onStop() {
    super.onStop()
    // ✅ MELHORIA 2: Parar atualização de tempo restante
    stopRemainingTimeUpdates()
    // ✅ MELHORIA 3: Parar atualização de indicador de buffer
    stopBufferIndicatorUpdates()
    // ✅ FASE 1: Parar atualização de latência e estatísticas
    stopLatencyUpdates()
    stopStatsUpdates()
    // ✅ Parar player quando Activity para
    player?.let { exo ->
      exo.stop()
      android.util.Log.d("PlayerActivity", "⏹️ Player parado em onStop")
    }
  }
  
  override fun onResume() {
    super.onResume()
    // ✅ MELHORIA 2: Retomar atualização de tempo restante se for VOD/Series
    if (contentType == "vod" || contentType == "series") {
      player?.let {
        if (it.playbackState == Player.STATE_READY) {
          startRemainingTimeUpdates()
        }
      }
    }
    // ✅ MELHORIA 3: Retomar atualização de indicador de buffer
    player?.let {
      if (it.playbackState == Player.STATE_READY || it.playbackState == Player.STATE_BUFFERING) {
        startBufferIndicatorUpdates()
        // ✅ FASE 1: Retomar latência e estatísticas se for Live
        if (contentType == "live") {
          startLatencyUpdates()
          startStatsUpdates()
          startLiveChannelInfoUpdates() // ✅ LIVE PROFESSIONAL
        }
      }
    }
  }
  
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    // ✅ Quando singleTop reutiliza Activity, liberar player anterior ANTES de criar novo
    android.util.Log.i("PlayerActivity", "🔄 Nova Intent recebida (singleTop) - liberando player anterior...")
    player?.let { exo ->
      exo.stop()
      exo.clearMediaItems()
      exo.release()
      android.util.Log.i("PlayerActivity", "✅ Player anterior liberado")
    }
    player = null
    
    // Resetar variáveis de controle
    reconnectAttempts = 0
    bufferingCount = 0
    lastBufferingTime = 0L
    qualityReduced = false
    currentMaxBitrate = 2_200_000
    // ✅ FASE 2: Resetar variáveis de failover e detecção de qualidade
    failoverAttempts = 0
    qualityDegradedWarningShown = false
    lastVideoFormat = null
    qualityDegradedToast?.cancel()
    qualityDegradedToast = null
    
    // Recriar player com nova URL
    setIntent(intent)
    recreate() // Recriar Activity para garantir limpeza completa
  }
  
  override fun onDestroy() {
    super.onDestroy()
    
    // ✅ Salvar posição antes de destruir (apenas VOD/Series)
    if ((contentType == "vod" || contentType == "series") && contentId != null) {
      player?.let { exo ->
        if (exo.duration > 0) {
          val position = exo.currentPosition
          val duration = exo.duration
          lifecycleScope.launch {
            PlaybackPositionManager.savePosition(contentId!!, contentType, position, duration)
            android.util.Log.i("PlayerActivity", "💾 Posição salva em onDestroy: ${PlaybackPositionManager.formatTime(position)}")
          }
        }
      }
    }
    
    // ✅ Parar handler de salvar posição
    positionSaveHandler?.removeCallbacksAndMessages(null)
    
    // ⚽ Remover overlay de gramado se existir
    footballOverlay?.let {
      (it.parent as? android.view.ViewGroup)?.removeView(it)
      footballOverlay = null
    }
    
    // ⚽ Remover botão de estatísticas se existir
    rotationAnimator?.cancel()
    rotationAnimator = null
    footballStatsButton?.let {
      (it.parent as? android.view.ViewGroup)?.removeView(it)
      footballStatsButton = null
    }
    
    // ⚽ Remover overlay de estatísticas se existir
    footballStatsOverlay?.let {
      (it.parent as? android.view.ViewGroup)?.removeView(it)
      footballStatsOverlay = null
    }
    
    // ⚽ NOVO: Limpar ViewModel de estatísticas
    soccerStatsViewModel?.clearData()
    soccerStatsViewModel = null
    currentMatchId = null
    
    // ⚽ Resetar zoom (se pv estiver inicializado)
    if (::pv.isInitialized) {
      pv.scaleX = 1.0f
      pv.scaleY = 1.0f
    }
    currentZoomLevel = 1.0f
    
    // ✅ MELHORIA 2: Parar atualização de tempo restante
    stopRemainingTimeUpdates()
    // ✅ MELHORIA 3: Parar atualização de indicador de buffer
    stopBufferIndicatorUpdates()
    
    // ✅ Liberar player completamente quando Activity é destruída
    android.util.Log.i("PlayerActivity", "🧹 Liberando player em onDestroy...")
    player?.let { exo ->
      exo.stop()
      exo.clearMediaItems()
      exo.release()
      android.util.Log.i("PlayerActivity", "✅ Player liberado completamente em onDestroy")
    }
    player = null
  }
  
  private fun getStatusBarHeight(): Int {
    var result = 0
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
      result = resources.getDimensionPixelSize(resourceId)
    }
    return result
  }
  
  // ⚽ CRIAR OVERLAY DE GRAMADO PARA MODO FUTEBOL
  private fun createFootballOverlay(rootLayout: FrameLayout) {
    // Criar overlay sutil de gramado usando gradiente verde (leve, não pesa o player)
    footballOverlay = android.widget.ImageView(this).apply {
      // Criar bitmap de gramado usando gradiente (muito leve)
      val width = resources.displayMetrics.widthPixels
      val height = resources.displayMetrics.heightPixels
      
      // Criar gradiente verde que simula gramado (bem sutil, 5% de opacidade)
      val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
      val canvas = android.graphics.Canvas(bitmap)
      
      // Gradiente verde sutil para simular gramado
      val paint = android.graphics.Paint().apply {
        shader = android.graphics.LinearGradient(
          0f, 0f, 0f, height.toFloat(),
          intArrayOf(
            android.graphics.Color.argb(5, 34, 139, 34), // Verde escuro muito transparente (topo)
            android.graphics.Color.argb(8, 50, 205, 50), // Verde médio transparente (meio)
            android.graphics.Color.argb(5, 34, 139, 34)  // Verde escuro muito transparente (fundo)
          ),
          null,
          android.graphics.Shader.TileMode.CLAMP
        )
      }
      
      canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
      
      // Adicionar linhas sutis do campo (muito leves)
      val linePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(3, 255, 255, 255) // Branco muito transparente
        strokeWidth = 2f
        style = android.graphics.Paint.Style.STROKE
      }
      
      // Linha central (horizontal)
      canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, linePaint)
      
      // Círculo central (muito sutil)
      val centerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.argb(2, 255, 255, 255)
        strokeWidth = 1f
        style = android.graphics.Paint.Style.STROKE
      }
      canvas.drawCircle(width / 2f, height / 2f, (height * 0.15f).coerceAtMost(width * 0.15f), centerPaint)
      
      setImageBitmap(bitmap)
      alpha = 0.15f // Opacidade muito baixa para não interferir no vídeo
      scaleType = android.widget.ImageView.ScaleType.FIT_XY
      
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
    }
    
    // Adicionar overlay ao rootLayout (logo após PlayerView, antes dos overlays de informação)
    // Usar insert para colocar logo após o PlayerView (índice 0), mas antes dos overlays de informação
    val insertIndex = 1 // Logo após PlayerView (índice 0)
    rootLayout.addView(footballOverlay, insertIndex)
    android.util.Log.i("PlayerActivity", "⚽ Overlay de gramado adicionado na posição $insertIndex (opacidade: 15%)")
  }
  
  // ⚽ CRIAR BOTÃO DE ESTATÍSTICAS DE FUTEBOL (bola giratória)
  private fun createFootballStatsButton(rootLayout: FrameLayout) {
    android.util.Log.i("PlayerActivity", "⚽ createFootballStatsButton chamado")
    android.util.Log.i("PlayerActivity", "   - isFootballMode: $isFootballMode")
    android.util.Log.i("PlayerActivity", "   - footballStatsButtonEnabled: $footballStatsButtonEnabled")
    android.util.Log.i("PlayerActivity", "   - footballStatsButton já existe: ${footballStatsButton != null}")
    android.util.Log.i("PlayerActivity", "   - rootLayout: ${rootLayout != null}")
    android.util.Log.i("PlayerActivity", "   - Dispositivo: ${MaxiApp.deviceCategory}")
    android.util.Log.i("PlayerActivity", "   - isTv: ${MaxiApp.isTv}, isTvBox: ${MaxiApp.isTvBox}")
    
    // ✅ Verificar condições antes de criar
    if (!isFootballMode) {
      android.util.Log.w("PlayerActivity", "⚽ Modo futebol não está ativado - botão não será criado")
      return
    }
    if (!footballStatsButtonEnabled) {
      android.util.Log.w("PlayerActivity", "⚽ Botão de estatísticas desabilitado - botão não será criado")
      return
    }
    
    // ✅ Verificar se já existe (evitar duplicação)
    if (footballStatsButton != null) {
      android.util.Log.w("PlayerActivity", "⚽ Botão de estatísticas já existe - não criando duplicado")
      // ✅ Mas garantir que está visível
      footballStatsButton?.visibility = android.view.View.VISIBLE
      footballStatsButton?.bringToFront()
      return
    }
    
    android.util.Log.i("PlayerActivity", "⚽ Criando botão de estatísticas de futebol...")
    
    val buttonSize = if (MaxiApp.isTv) 56 else 48 // dp
    val density = resources.displayMetrics.density
    val sizePx = (buttonSize * density).toInt()
    val margin = (16f * density).toInt() // 16dp de margem
    
    // Criar botão customizado (bola de futebol)
    footballStatsButton = android.widget.ImageButton(this).apply {
      // Criar drawable de bola de futebol usando Canvas
      val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
      val canvas = android.graphics.Canvas(bitmap)
      
      // Desenhar bola de futebol (padrão hexágono/pentágono)
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
      
      // Desenhar círculo branco (bola)
      canvas.drawCircle(centerX, centerY, radius, paint)
      canvas.drawCircle(centerX, centerY, radius, strokePaint)
      
      // Desenhar linhas da bola de futebol (padrão simplificado)
      val linePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
        color = android.graphics.Color.BLACK
      }
      
      // Linhas horizontais e verticais
      canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, linePaint)
      canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, linePaint)
      
      // Linhas diagonais
      val diagonalOffset = radius * 0.7f
      canvas.drawLine(centerX - diagonalOffset, centerY - diagonalOffset, centerX + diagonalOffset, centerY + diagonalOffset, linePaint)
      canvas.drawLine(centerX - diagonalOffset, centerY + diagonalOffset, centerX + diagonalOffset, centerY - diagonalOffset, linePaint)
      
      setImageBitmap(bitmap)
      background = null // Sem background
      scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
      alpha = 0.8f // 80% de opacidade
      
      // Configurar foco e clique
      isFocusable = true
      isFocusableInTouchMode = true
      setOnClickListener {
        showFootballStatsOverlay()
      }
      
      setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
          // Zoom e borda vermelha quando focado
          animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .start()
          
          // Adicionar borda vermelha
          val borderDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setStroke((6f * density).toInt(), android.graphics.Color.RED)
            setColor(android.graphics.Color.TRANSPARENT)
          }
          background = borderDrawable
          
          // Glow vermelho
          elevation = 8f
        } else {
          // Voltar ao normal quando perder foco
          animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(200)
            .start()
          background = null
          elevation = 0f
        }
      }
      
      // ✅ Reposicionar botão acima do buffering (canto superior direito, mas acima do indicador de buffer)
      // Calcular posição baseada no tamanho do buffering (geralmente ~48dp em smartphones, ~56dp em TV)
      val bufferingOffset = if (MaxiApp.isTv) (72f * density).toInt() else (64f * density).toInt()
      val topMargin = bufferingOffset + margin
      
      // ✅ CORREÇÃO TV: Aumentar margem direita para considerar outros overlays
      // Em TV, outros overlays (qualityOverlay, liveChannelInfoOverlay) usam 40dp de margem direita
      // IMPORTANTE: Não somar overscanPadding aqui, pois o layout já considera overscan
      // O overscan já está aplicado no rootLayout, então precisamos apenas garantir que
      // o botão não fique coberto por outros overlays (que usam 40dp de margem)
      val rightMarginDp = if (MaxiApp.isTv) {
        // TV: margem maior para não ficar coberto por outros overlays
        // Usar 40dp (mesma margem dos outros overlays) + 8dp extra para espaçamento
        // NÃO somar overscanPadding aqui pois já está aplicado no layout principal
        val otherOverlaysMargin = 40 // dp - margem usada por qualityOverlay e liveChannelInfoOverlay
        otherOverlaysMargin + 8 // 8dp extra para garantir espaçamento
      } else {
        margin // Smartphone: margem normal (16dp)
      }
      val rightMargin = (rightMarginDp * density).toInt()
      
      layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
        gravity = android.view.Gravity.TOP or android.view.Gravity.END
        // ✅ Margem superior maior para ficar acima do buffering, margem direita aumentada para TV
        setMargins(0, topMargin, rightMargin, 0)
      }
      
      android.util.Log.i("PlayerActivity", "⚽ Posicionamento do botão:")
      android.util.Log.i("PlayerActivity", "   - Top margin: ${topMargin}px (${topMargin / density}dp)")
      android.util.Log.i("PlayerActivity", "   - Right margin: ${rightMargin}px (${rightMarginDp}dp)")
      android.util.Log.i("PlayerActivity", "   - Device: ${MaxiApp.deviceCategory}")
      android.util.Log.i("PlayerActivity", "   - isTv: ${MaxiApp.isTv}, isFireStick: ${MaxiApp.isFireStick}")
      android.util.Log.i("PlayerActivity", "   - Button size: ${sizePx}px (${buttonSize}dp)")
      android.util.Log.i("PlayerActivity", "   - Screen width: ${resources.displayMetrics.widthPixels}px")
      android.util.Log.i("PlayerActivity", "   - Button X position: ${resources.displayMetrics.widthPixels - rightMargin - sizePx}px")
    }
    
    // Adicionar animação de rotação contínua
    rotationAnimator = android.animation.ObjectAnimator.ofFloat(footballStatsButton, "rotation", 0f, 360f).apply {
      duration = 3000 // 3 segundos para uma rotação completa
      repeatCount = android.animation.ObjectAnimator.INFINITE
      interpolator = android.view.animation.LinearInterpolator()
      start()
    }
    
    rootLayout.addView(footballStatsButton)
    
    // ✅ CORREÇÃO TV: Garantir que o botão está visível e ACIMA de todos os outros overlays
    footballStatsButton?.visibility = android.view.View.VISIBLE
    footballStatsButton?.bringToFront() // Trazer para frente
    
    // ✅ CORREÇÃO TV: Forçar elevação para garantir que está acima de outros overlays
    footballStatsButton?.elevation = 16f // Elevação alta para ficar acima de outros overlays
    
    // ✅ CORREÇÃO TV: Aguardar um frame e garantir visibilidade novamente (workaround para timing)
    rootLayout.post {
      footballStatsButton?.visibility = android.view.View.VISIBLE
      footballStatsButton?.bringToFront()
      footballStatsButton?.elevation = 16f
      
      // Log final de verificação
      android.util.Log.i("PlayerActivity", "⚽ Verificação final do botão:")
      android.util.Log.i("PlayerActivity", "   - Visibility: ${footballStatsButton?.visibility}")
      android.util.Log.i("PlayerActivity", "   - Elevation: ${footballStatsButton?.elevation}")
      android.util.Log.i("PlayerActivity", "   - Parent: ${footballStatsButton?.parent?.javaClass?.simpleName}")
      android.util.Log.i("PlayerActivity", "   - X: ${footballStatsButton?.x}, Y: ${footballStatsButton?.y}")
      android.util.Log.i("PlayerActivity", "   - Width: ${footballStatsButton?.width}, Height: ${footballStatsButton?.height}")
    }
    
    android.util.Log.i("PlayerActivity", "⚽ Botão de estatísticas de futebol criado")
    android.util.Log.i("PlayerActivity", "   - Dispositivo: ${MaxiApp.deviceCategory}")
    android.util.Log.i("PlayerActivity", "   - isTv: ${MaxiApp.isTv}, isTvBox: ${MaxiApp.isTvBox}, isFireStick: ${MaxiApp.isFireStick}")
    android.util.Log.i("PlayerActivity", "   - Tamanho do botão: ${sizePx}px (${buttonSize}dp)")
    android.util.Log.i("PlayerActivity", "   - Visibilidade: ${footballStatsButton?.visibility}")
    android.util.Log.i("PlayerActivity", "   - Botão habilitado: $footballStatsButtonEnabled")
  }
  
  // ⚽ MOSTRAR OVERLAY DE ESTATÍSTICAS (versão melhorada com API real)
  private fun showFootballStatsOverlay() {
    if (footballStatsOverlay != null) {
      // Se já existe, apenas mostrar/esconder
      footballStatsOverlay?.visibility = if (isStatsOverlayVisible) android.view.View.GONE else android.view.View.VISIBLE
      isStatsOverlayVisible = !isStatsOverlayVisible
      return
    }
    
    val rootLayout = footballStatsButton?.parent as? FrameLayout ?: return
    val channelName = intent.getStringExtra("channelName") ?: "Canal de Futebol"
    
    // ⚽ NOVO: Se houver matchId e ViewModel, usar dados reais da API
    if (currentMatchId != null && soccerStatsViewModel != null) {
      android.util.Log.i("PlayerActivity", "⚽ Abrindo overlay com dados reais da API (matchId: $currentMatchId)")
      soccerStatsViewModel?.openOverlay(currentMatchId!!)
      // Por enquanto, usar overlay simples (overlay Compose será implementado depois)
      createSimpleFootballOverlay(rootLayout, channelName)
    } else {
      // Fallback: criar overlay simples com dados simulados
      android.util.Log.i("PlayerActivity", "⚽ Abrindo overlay simples (sem matchId)")
      createSimpleFootballOverlay(rootLayout, channelName)
    }
  }
  
  // ⚽ CRIAR OVERLAY SIMPLES DE ESTATÍSTICAS
  private fun createSimpleFootballOverlay(
    rootLayout: FrameLayout,
    channelName: String
  ) {
    val density = resources.displayMetrics.density
    
    // Criar overlay simples
    footballStatsOverlay = FrameLayout(this).apply {
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
      setBackgroundColor(android.graphics.Color.argb(200, 0, 0, 0)) // Fundo semi-transparente
      
      // Container simples
      val container = android.widget.LinearLayout(this@PlayerActivity).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        gravity = android.view.Gravity.CENTER
        setPadding((32f * density).toInt(), (32f * density).toInt(), (32f * density).toInt(), (32f * density).toInt())
        
        layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.WRAP_CONTENT,
          FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          gravity = android.view.Gravity.CENTER
        }
      }
      
      // Card simples
      val card = android.widget.FrameLayout(this@PlayerActivity).apply {
        setBackgroundColor(android.graphics.Color.argb(245, 20, 20, 20))
        layoutParams = FrameLayout.LayoutParams(
          (if (MaxiApp.isTv) 600 else 500).toInt(),
          (if (MaxiApp.isTv) 400 else 300).toInt()
        ).apply {
          gravity = android.view.Gravity.CENTER
        }
      }
      
      val contentLayout = android.widget.LinearLayout(this@PlayerActivity).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        setPadding((20f * density).toInt(), (20f * density).toInt(), (20f * density).toInt(), (20f * density).toInt())
      }
      
      // Título
      val titleView = android.widget.TextView(this@PlayerActivity).apply {
        text = "⚽ $channelName"
        textSize = if (MaxiApp.isTv) 20f else 18f
        setTextColor(android.graphics.Color.argb(255, 0, 212, 255))
        setTypeface(null, android.graphics.Typeface.BOLD)
        gravity = android.view.Gravity.CENTER
        setPadding(0, 0, 0, (16f * density).toInt())
      }
      contentLayout.addView(titleView)
      
      // Dados simulados simples
      val statsView = android.widget.TextView(this@PlayerActivity).apply {
        text = "📊 Estatísticas do Jogo\n\n" +
               "🟢 AO VIVO\n" +
               "⏱️ Tempo: 45'\n" +
               "⚽ Placar: 2 - 1\n" +
               "📈 Posse: 55% - 45%\n" +
               "🎯 Chutes: 8 - 5"
        textSize = if (MaxiApp.isTv) 16f else 14f
        setTextColor(android.graphics.Color.WHITE)
        gravity = android.view.Gravity.CENTER
        setPadding(0, (16f * density).toInt(), 0, 0)
      }
      contentLayout.addView(statsView)
      
      // Botão fechar
      val closeButton = android.widget.Button(this@PlayerActivity).apply {
        text = "✕ FECHAR"
        textSize = if (MaxiApp.isTv) 16f else 14f
        setTextColor(android.graphics.Color.WHITE)
        setTypeface(null, android.graphics.Typeface.BOLD)
        background = GradientDrawable().apply {
          setColor(android.graphics.Color.argb(255, 244, 67, 54))
          cornerRadius = 8f
        }
        setPadding((24f * density).toInt(), (12f * density).toInt(), (24f * density).toInt(), (12f * density).toInt())
        setOnClickListener {
          hideFootballStatsOverlay()
        }
        layoutParams = android.widget.LinearLayout.LayoutParams(
          FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          setMargins(0, (24f * density).toInt(), 0, 0)
        }
        isFocusable = true
        isFocusableInTouchMode = true
      }
      contentLayout.addView(closeButton)
      
      card.addView(contentLayout)
      container.addView(card)
      addView(container)
      
      // Fechar ao tocar fora
      setOnClickListener {
        hideFootballStatsOverlay()
      }
    }
    
    rootLayout.addView(footballStatsOverlay)
    isStatsOverlayVisible = true
    android.util.Log.i("PlayerActivity", "⚽ Overlay simples exibido")
  }
  
  // ⚽ OCULTAR OVERLAY DE ESTATÍSTICAS
  private fun hideFootballStatsOverlay() {
    footballStatsOverlay?.visibility = android.view.View.GONE
    isStatsOverlayVisible = false
    // ⚽ NOVO: Parar polling quando fechar overlay
    soccerStatsViewModel?.closeOverlay()
    android.util.Log.i("PlayerActivity", "⚽ Overlay fechado")
  }
  
  // ⚽ ZOOM EM EVENTOS IMPORTANTES
  private fun performEventZoom(eventType: String, askUser: Boolean = false) {
    if (!isFootballMode || !footballAutoZoomEnabled) return
    
    val zoomLevel = 1.5f // Zoom de 1.5x
    
    if (askUser) {
      // Perguntar ao usuário se quer zoom
      android.app.AlertDialog.Builder(this)
        .setTitle("⚽ Evento Importante")
        .setMessage(when (eventType) {
          "penalty" -> "Cobrança de pênalti detectada!\nDeseja dar zoom para ver melhor?"
          "corner" -> "Cobrança de escanteio detectada!\nDeseja dar zoom para ver melhor?"
          "goal" -> "Gol marcado!\nDeseja dar zoom para ver melhor?"
          "card" -> "Cartão aplicado!\nDeseja dar zoom para ver melhor?"
          else -> "Evento importante detectado!\nDeseja dar zoom para ver melhor?"
        })
        .setPositiveButton("Sim") { _, _ ->
          applyZoom(zoomLevel)
        }
        .setNegativeButton("Não", null)
        .show()
    } else {
      // Zoom automático
      applyZoom(zoomLevel)
    }
    
    // Voltar ao zoom normal após 10 segundos
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
      applyZoom(1.0f)
    }, 10000)
  }
  
  // ⚽ APLICAR ZOOM NO PLAYER
  private fun applyZoom(zoomLevel: Float) {
    player?.let {
      // Usar scale do PlayerView para zoom
      pv.scaleX = zoomLevel
      pv.scaleY = zoomLevel
      currentZoomLevel = zoomLevel
      
      android.util.Log.i("PlayerActivity", "⚽ Zoom aplicado: ${zoomLevel}x")
    }
  }
  
  // ⚽ SIMULAR DETECÇÃO DE EVENTOS (para teste - sem API)
  private fun simulateFootballEvents() {
    if (!isFootballMode) return
    
    // Simular eventos aleatórios a cada 30-60 segundos (apenas para teste)
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val runnable = object : Runnable {
      override fun run() {
        if (isFootballMode && player?.isPlaying == true) {
          // Simular evento aleatório (apenas para teste)
          val events = listOf("penalty", "corner", "goal", "card")
          val randomEvent = events.random()
          
          // Apenas 10% de chance de simular evento (para não ser muito frequente)
          if (kotlin.random.Random.nextInt(100) < 10) {
            performEventZoom(randomEvent, askUser = true)
          }
        }
        
        // Agendar próximo evento (30-60 segundos)
        handler.postDelayed(this, (30000 + kotlin.random.Random.nextInt(30000)).toLong())
      }
    }
    
    handler.postDelayed(runnable, 30000) // Primeiro evento após 30 segundos
  }
  
  // ✅ FASE 1: CONTROLES AVANÇADOS - Avançar/Retroceder 10 segundos
  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    // ✅ Detectar interação do usuário (D-pad) para mostrar latência/stats
    lastUserInteraction = System.currentTimeMillis()
    showLatencyStats = true
    updateLatency()
    updateStreamStats()
    
    if (MaxiApp.isTv && player != null) {
      when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> {
          // Retroceder 10 segundos
          val newPosition = (player!!.currentPosition - 10000).coerceAtLeast(0)
          player!!.seekTo(newPosition)
          showSeekIndicator(-10)
          return true
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
          // Avançar 10 segundos
          val duration = player!!.duration
          if (duration != C.TIME_UNSET) {
            val newPosition = (player!!.currentPosition + 10000).coerceAtMost(duration)
            player!!.seekTo(newPosition)
            showSeekIndicator(10)
          }
          return true
        }
        // ✅ BOTÕES A, CC, H REMOVIDOS - Agora estão na tela de detalhes (VodDetailsScreen)
      }
    }
    return super.onKeyDown(keyCode, event)
  }
  
  // ✅ FASE 1: Indicador visual de seek (avançar/retroceder)
  private fun showSeekIndicator(seconds: Int) {
    // Por enquanto apenas log, mas pode adicionar overlay visual depois
    android.util.Log.i("PlayerActivity", "⏩ Seek: ${if (seconds > 0) "+" else ""}$seconds segundos")
  }
  
  // ✅ FASE 1: Aplicar qualidade selecionada manualmente
  private fun applyQuality(quality: PlayerSettingsManager.VideoQuality) {
    val exo = player ?: return
    
    lifecycleScope.launch {
      try {
        PlayerSettingsManager.setVideoQuality(quality)
        currentMaxBitrate = quality.maxBitrate
        
        val (width, height) = when (quality) {
          PlayerSettingsManager.VideoQuality.HD -> 1280 to 720
          PlayerSettingsManager.VideoQuality.SD -> 854 to 480
          PlayerSettingsManager.VideoQuality.ULTRA_LOW -> 640 to 360
          else -> 1280 to 720
        }
        exo.trackSelectionParameters = TrackSelectionParameters.Builder(this@PlayerActivity)
          .setPreferredTextLanguage(null)
          .setMaxVideoBitrate(quality.maxBitrate)
          .setMinVideoBitrate(quality.minBitrate)
          .setMaxVideoSize(width, height)
          .build()
        
        android.util.Log.i("PlayerActivity", "✅ Qualidade manual aplicada: ${quality.displayName}")
        
        // ✅ MELHORIA 1: Mostrar indicador de qualidade após mudança manual
        val resolution = "${width}x${height}"
        showQualityIndicator(resolution, quality.maxBitrate)
      } catch (e: Exception) {
        android.util.Log.e("PlayerActivity", "❌ Erro ao aplicar qualidade: ${e.message}")
      }
    }
  }
  
  // ✅ FASE 1: Aplicar formato específico do stream
  private fun applyFormatQuality(format: Format) {
    val exo = player ?: return
    
    exo.trackSelectionParameters = TrackSelectionParameters.Builder(this)
      .setPreferredTextLanguage(null)
      .setMaxVideoBitrate(format.bitrate)
      .setMaxVideoSize(format.width, format.height)
      .setMinVideoBitrate(format.bitrate / 2) // Metade do bitrate como mínimo
      .build()
    
    android.util.Log.i("PlayerActivity", "✅ Formato aplicado: ${format.width}x${format.height} @ ${format.bitrate / 1000}Kbps")
  }
  
  // ✅ MELHORIA 2: Atualizar tempo restante (VOD/Series)
  private fun updateRemainingTime() {
    val exo = player ?: return
    
    // Apenas para VOD e Series
    if (contentType != "vod" && contentType != "series") {
      stopRemainingTimeUpdates()
      return
    }
    
    val duration = exo.duration
    val currentPosition = exo.currentPosition
    
    if (duration != androidx.media3.common.C.TIME_UNSET && duration > 0) {
      val remaining = duration - currentPosition
      val minutes = (remaining / 60000).toInt()
      val seconds = ((remaining % 60000) / 1000).toInt()
      
      remainingTimeOverlay?.text = "Tempo restante: ${String.format("%02d:%02d", minutes, seconds)}"
      remainingTimeOverlay?.visibility = android.view.View.VISIBLE
    } else {
      remainingTimeOverlay?.visibility = android.view.View.GONE
    }
  }
  
  // ✅ MELHORIA 2: Iniciar atualização de tempo restante
  private fun startRemainingTimeUpdates() {
    stopRemainingTimeUpdates() // Parar qualquer atualização anterior
    
    remainingTimeHandler = android.os.Handler(android.os.Looper.getMainLooper())
    remainingTimeHandler?.post(remainingTimeRunnable)
    android.util.Log.d("PlayerActivity", "⏱️ Iniciando atualização de tempo restante")
  }
  
  // ✅ MELHORIA 2: Parar atualização de tempo restante
  private fun stopRemainingTimeUpdates() {
    remainingTimeHandler?.removeCallbacks(remainingTimeRunnable)
    remainingTimeHandler = null
  }
  
  // ✅ MELHORIA 3: Atualizar indicador visual de buffer
  private fun updateBufferIndicator() {
    val exo = player ?: return
    
    // Calcular percentual de buffer disponível
    val bufferedPosition = exo.bufferedPosition
    val currentPosition = exo.currentPosition
    val duration = exo.duration
    
    if (duration != androidx.media3.common.C.TIME_UNSET && duration > 0) {
      // Para VOD/Series: calcular percentual de buffer restante
      val bufferedAhead = bufferedPosition - currentPosition
      val totalRemaining = duration - currentPosition
      val bufferPercent = if (totalRemaining > 0) {
        ((bufferedAhead.toFloat() / totalRemaining.toFloat()) * 100f).toInt()
      } else {
        100
      }
      
      // Determinar cor e texto baseado no nível de buffer
      val (color, text) = when {
        bufferPercent >= 50 -> {
          android.graphics.Color.argb(255, 76, 175, 80) to "Buffer: OK" // Verde
        }
        bufferPercent >= 20 -> {
          android.graphics.Color.argb(255, 255, 193, 7) to "Buffer: Médio" // Amarelo
        }
        else -> {
          android.graphics.Color.argb(255, 244, 67, 54) to "Buffer: Baixo" // Vermelho
        }
      }
      
      bufferIndicatorOverlay?.let { overlay ->
        overlay.text = text
        overlay.setTextColor(color)
        overlay.background = GradientDrawable().apply {
          setColor(android.graphics.Color.argb(200, 0, 0, 0)) // Fundo preto semi-transparente
          cornerRadius = 6f
          setStroke(2, color) // Borda com cor do status
        }
        overlay.visibility = android.view.View.VISIBLE
      }
    } else {
      // Para Live: verificar se está buffering
      if (exo.playbackState == Player.STATE_BUFFERING) {
        bufferIndicatorOverlay?.let { overlay ->
          overlay.text = "Carregando..."
          // ✅ COR MODERNA: Azul ciano em vez de amarelo
          overlay.setTextColor(android.graphics.Color.argb(255, 0, 212, 255)) // Azul ciano moderno
          overlay.background = GradientDrawable().apply {
            setColor(android.graphics.Color.argb(200, 0, 0, 0))
            cornerRadius = 6f
            setStroke(2, android.graphics.Color.argb(255, 0, 212, 255)) // Azul ciano moderno
          }
          overlay.visibility = android.view.View.VISIBLE
        }
      } else {
        bufferIndicatorOverlay?.visibility = android.view.View.GONE
      }
    }
  }
  
  // ✅ MELHORIA 3: Iniciar atualização de indicador de buffer
  private fun startBufferIndicatorUpdates() {
    stopBufferIndicatorUpdates() // Parar qualquer atualização anterior
    
    bufferIndicatorHandler = android.os.Handler(android.os.Looper.getMainLooper())
    bufferIndicatorHandler?.post(bufferIndicatorRunnable)
    android.util.Log.d("PlayerActivity", "📊 Iniciando atualização de indicador de buffer")
  }
  
  // ✅ MELHORIA 3: Parar atualização de indicador de buffer
  private fun stopBufferIndicatorUpdates() {
    bufferIndicatorHandler?.removeCallbacks(bufferIndicatorRunnable)
    bufferIndicatorHandler = null
  }
  
  // ✅ FASE 1: Calcular latência para Live (HLS)
  private fun calculateLatency(): Long {
    val exo = player ?: return 0L
    if (contentType != "live") return 0L // Apenas para live
    
    val currentPos = exo.currentPosition
    val bufferedPos = exo.bufferedPosition
    
    // Para HLS live, latência = diferença entre buffer e posição atual
    val latency = bufferedPos - currentPos
    return latency.coerceIn(0, 20000) // Máximo 20s
  }
  
  // ✅ FASE 1: Atualizar overlay de latência (só mostra quando usuário interagir)
  private fun updateLatency() {
    if (player == null || contentType != "live") {
      latencyOverlay?.visibility = android.view.View.GONE
      return
    }
    
    // ✅ Só mostrar se usuário interagiu recentemente (últimos 5 segundos)
    val timeSinceInteraction = System.currentTimeMillis() - lastUserInteraction
    val shouldShow = showLatencyStats && timeSinceInteraction < 5000
    
    if (!shouldShow) {
      latencyOverlay?.visibility = android.view.View.GONE
      return
    }
    
    val latencyMs = calculateLatency()
    val latencySeconds = latencyMs / 1000
    
    val (color, text) = when {
      latencySeconds < 3 -> android.graphics.Color.argb(255, 76, 175, 80) to "Latência: ${latencySeconds}s" // Verde
      latencySeconds < 5 -> android.graphics.Color.argb(255, 255, 193, 7) to "Latência: ${latencySeconds}s" // Amarelo
      else -> android.graphics.Color.argb(255, 244, 67, 54) to "Latência: ${latencySeconds}s" // Vermelho
    }
    
    latencyOverlay?.let { overlay ->
      overlay.text = text
      overlay.setTextColor(color)
      overlay.background = GradientDrawable().apply {
        setColor(android.graphics.Color.argb(200, 0, 0, 0)) // Fundo preto semi-transparente
        cornerRadius = 6f
        setStroke(2, color) // Borda com cor do status
      }
      overlay.visibility = android.view.View.VISIBLE
    }
  }
  
  // ✅ FASE 1: Iniciar atualização de latência
  private fun startLatencyUpdates() {
    stopLatencyUpdates() // Parar qualquer atualização anterior
    
    latencyHandler = android.os.Handler(android.os.Looper.getMainLooper())
    latencyHandler?.post(latencyRunnable)
    android.util.Log.d("PlayerActivity", "📊 Iniciando atualização de latência")
  }
  
  // ✅ FASE 1: Parar atualização de latência
  private fun stopLatencyUpdates() {
    latencyHandler?.removeCallbacks(latencyRunnable)
    latencyHandler = null
  }
  
  // ✅ FASE 1: Obter estatísticas do stream atual
  private fun getStreamStats(): StreamStats {
    val exo = player ?: return StreamStats()
    
    // Obter track de vídeo atual
    val videoTrack = exo.currentTracks.groups.firstOrNull { 
      it.type == C.TRACK_TYPE_VIDEO && it.isSelected 
    }
    
    val format = videoTrack?.getTrackFormat(0)
    val audioTrack = exo.currentTracks.groups.firstOrNull {
      it.type == C.TRACK_TYPE_AUDIO && it.isSelected
    }
    val audioFormat = audioTrack?.getTrackFormat(0)
    
    return StreamStats(
      bitrate = format?.bitrate ?: 0,
      resolution = "${format?.width ?: 0}x${format?.height ?: 0}",
      fps = format?.frameRate ?: 0f,
      codec = format?.codecs ?: "N/A",
      audioBitrate = audioFormat?.bitrate ?: 0,
      audioCodec = audioFormat?.codecs ?: "N/A",
      audioChannels = audioFormat?.channelCount ?: 0
    )
  }
  
  // ✅ FASE 1: Atualizar overlay de estatísticas (só mostra quando usuário interagir)
  private fun updateStreamStats() {
    val exo = player ?: return
    if (contentType != "live") {
      statsOverlay?.visibility = android.view.View.GONE
      return
    }
    
    // ✅ Só mostrar se usuário interagiu recentemente (últimos 5 segundos)
    val timeSinceInteraction = System.currentTimeMillis() - lastUserInteraction
    val shouldShow = showLatencyStats && timeSinceInteraction < 5000
    
    if (!shouldShow) {
      statsOverlay?.visibility = android.view.View.GONE
      return
    }
    
    val stats = getStreamStats()
    val latencyMs = calculateLatency()
    val latencySeconds = latencyMs / 1000
    
    // Calcular qualidade de conexão baseado em múltiplos fatores
    val bufferedPosition = exo.bufferedPosition
    val currentPosition = exo.currentPosition
    val bufferAhead = bufferedPosition - currentPosition
    
    // Estimar qualidade de conexão (usando função compartilhada)
    val estimatedQuality = estimateConnectionQuality(exo, latencyMs, bufferAhead, stats.bitrate)
    connectionQuality = estimatedQuality
    
    val statsText = buildString {
      append("📊 Estatísticas\n")
      append("━━━━━━━━━━━━━━━━\n")
      append("Resolução: ${stats.resolution}\n")
      append("Bitrate: ${stats.bitrate / 1000}Kbps\n")
      append("FPS: ${stats.fps.toInt()}\n")
      append("Codec: ${stats.codec}\n")
      append("━━━━━━━━━━━━━━━━\n")
      append("Áudio: ${stats.audioCodec}\n")
      append("Bitrate Áudio: ${stats.audioBitrate / 1000}Kbps\n")
      append("Canais: ${stats.audioChannels}\n")
      append("━━━━━━━━━━━━━━━━\n")
      append("Latência: ${latencySeconds}s\n")
      append("Buffer: ${bufferAhead / 1000}s\n")
      append("Qualidade: ${when (estimatedQuality) {
        ConnectionQuality.EXCELLENT -> "Excelente"
        ConnectionQuality.GOOD -> "Boa"
        ConnectionQuality.POOR -> "Ruim"
      }}\n")
    }
    
    statsOverlay?.let { overlay ->
      overlay.text = statsText
      overlay.background = GradientDrawable().apply {
        setColor(android.graphics.Color.argb(220, 0, 0, 0)) // Fundo preto semi-transparente
        cornerRadius = 8f
        setStroke(2, android.graphics.Color.argb(255, 0, 212, 255)) // Borda azul ciano
      }
    }
  }
  
  // ✅ FASE 1: Iniciar atualização de estatísticas
  private fun startStatsUpdates() {
    stopStatsUpdates() // Parar qualquer atualização anterior
    
    statsHandler = android.os.Handler(android.os.Looper.getMainLooper())
    statsHandler?.post(statsRunnable)
    android.util.Log.d("PlayerActivity", "📊 Iniciando atualização de estatísticas")
  }
  
  // ✅ FASE 1: Parar atualização de estatísticas
  private fun stopStatsUpdates() {
    statsHandler?.removeCallbacks(statsRunnable)
    statsHandler = null
  }
  
  // ✅ LIVE PROFESSIONAL: Atualizar informações do canal usando EPG
  private fun updateLiveChannelInfo() {
    if (contentType != "live") {
      liveChannelInfoOverlay?.visibility = android.view.View.GONE
      return
    }
    
    val channelName = currentChannelName ?: return
    
    // Buscar informações do EPG em background
    lifecycleScope.launch {
      try {
        // Carregar EPG se ainda não estiver carregado
        com.maxiptv.data.XRepo.loadEpg()
        val epgData = com.maxiptv.data.XRepo.epgData.value
        
        // Buscar programa atual do canal
        val currentProgramme = com.maxiptv.data.EpgParser.getCurrentProgramme(channelName, epgData)
        val nextProgramme = com.maxiptv.data.EpgParser.getNextProgramme(channelName, epgData)
        
        // Construir texto com informações do canal
        val infoText = buildString {
          append("📺 $channelName\n")
          if (currentProgramme != null) {
            append("━━━━━━━━━━━━━━━━\n")
            append("▶ ${currentProgramme.title}\n")
            if (currentProgramme.subTitle != null) {
              append("   ${currentProgramme.subTitle}\n")
            }
            append("   ${currentProgramme.startTime()} - ${currentProgramme.stopTime()}\n")
            if (nextProgramme != null) {
              append("━━━━━━━━━━━━━━━━\n")
              append("⏭ ${nextProgramme.title}\n")
              append("   ${nextProgramme.startTime()}\n")
            }
          } else {
            append("━━━━━━━━━━━━━━━━\n")
            append("Programação não disponível\n")
          }
        }
        
        // Atualizar overlay na UI thread
        runOnUiThread {
          liveChannelInfoOverlay?.text = infoText
          liveChannelInfoOverlay?.visibility = android.view.View.VISIBLE
          
          // Fade in
          liveChannelInfoOverlay?.animate()
            ?.alpha(1f)
            ?.setDuration(300)
            ?.start()
        }
      } catch (e: Exception) {
        android.util.Log.e("PlayerActivity", "❌ Erro ao atualizar informações do canal: ${e.message}")
        // Mostrar apenas nome do canal se EPG falhar
        runOnUiThread {
          liveChannelInfoOverlay?.text = "📺 $channelName"
          liveChannelInfoOverlay?.visibility = android.view.View.VISIBLE
        }
      }
    }
  }
  
  // ✅ LIVE PROFESSIONAL: Iniciar atualização de informações do canal
  private fun startLiveChannelInfoUpdates() {
    stopLiveChannelInfoUpdates() // Parar qualquer atualização anterior
    
    // Atualizar imediatamente
    updateLiveChannelInfo()
    
    // Atualizar a cada 30 segundos (EPG pode mudar)
    liveChannelInfoHandler = android.os.Handler(android.os.Looper.getMainLooper())
    liveChannelInfoHandler?.postDelayed(object : Runnable {
      override fun run() {
        updateLiveChannelInfo()
        liveChannelInfoHandler?.postDelayed(this, 30000) // 30 segundos
      }
    }, 30000)
    
    android.util.Log.d("PlayerActivity", "📺 Iniciando atualização de informações do canal")
  }
  
  // ✅ LIVE PROFESSIONAL: Parar atualização de informações do canal
  private fun stopLiveChannelInfoUpdates() {
    liveChannelInfoHandler?.removeCallbacksAndMessages(null)
    liveChannelInfoHandler = null
  }
  
  // ✅ FASE 1: Criar LoadControl adaptativo baseado em qualidade de conexão
  // Nota: Usa função compartilhada, mas mantém compatibilidade com isLive
  private fun createAdaptiveLoadControl(@Suppress("UNUSED_PARAMETER") isLive: Boolean): LoadControl {
    return createAdaptiveLoadControl(connectionQuality)
  }
  
  // ✅ FASE 1: Data class para estatísticas do stream
  private data class StreamStats(
    val bitrate: Int = 0,
    val resolution: String = "N/A",
    val fps: Float = 0f,
    val codec: String = "N/A",
    val audioBitrate: Int = 0,
    val audioCodec: String = "N/A",
    val audioChannels: Int = 0
  )
  
  // ✅ MELHORIA 1: Mostrar indicador visual de qualidade atual
  private fun showQualityIndicator(resolution: String, bitrate: Int?) {
    qualityOverlay?.let { overlay ->
      val bitrateText = bitrate?.let { " @ ${it / 1000}Kbps" } ?: ""
      overlay.text = "$resolution$bitrateText"
      
      // Fade in
      overlay.visibility = android.view.View.VISIBLE
      overlay.animate()
        .alpha(1f)
        .setDuration(300)
        .start()
      
      // Fade out após 2.5 segundos
      overlay.animate()
        .alpha(0f)
        .setDuration(500)
        .setStartDelay(2500)
        .withEndAction {
          overlay.visibility = android.view.View.GONE
        }
        .start()
      
      android.util.Log.d("PlayerActivity", "📺 Indicador de qualidade: $resolution$bitrateText")
    }
  }
  
  // ✅ FASE 2: Configurar estilização de legendas
  private fun setupSubtitleStyle() {
    try {
      val subtitleView = pv.subtitleView
      subtitleView?.let { sv ->
        // Usar estilo do sistema (CaptioningManager) que é compatível com todas as versões
        val captioningManager = getSystemService(Context.CAPTIONING_SERVICE) as? CaptioningManager
        captioningManager?.let {
          sv.setUserDefaultStyle()
          sv.setUserDefaultTextSize()
          
          // Tamanho da fonte adaptativo (sobrescrever padrão do sistema)
          val fontSize = if (MaxiApp.isTv) 24f else 18f
          sv.setFixedTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            fontSize
          )
          
          android.util.Log.i("PlayerActivity", "✅ Estilo de legendas configurado (fonte: ${fontSize}dp)")
        } ?: run {
          // Fallback se CaptioningManager não estiver disponível
          val fontSize = if (MaxiApp.isTv) 24f else 18f
          sv.setFixedTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            fontSize
          )
          android.util.Log.i("PlayerActivity", "✅ Estilo de legendas configurado (fallback, fonte: ${fontSize}dp)")
        }
      }
    } catch (e: Exception) {
      android.util.Log.w("PlayerActivity", "⚠️ Erro ao configurar estilo de legendas: ${e.message}")
    }
  }
  
  // ✅ BOTÕES A, CC, H REMOVIDOS - Agora estão na tela de detalhes (VodDetailsScreen)
  
  // ✅ FASE 2: Detectar degradação de qualidade (versão com Toast para PlayerActivity)
  private fun detectQualityDegradation(currentFormat: Format) {
    if (player == null || contentType != "live") return // Apenas para live
    
    // Usar função compartilhada para detecção básica
    val playerState = PlayerState().apply {
      lastVideoFormat = lastVideoFormat
      qualityDegradedWarningShown = qualityDegradedWarningShown
    }
    
    detectQualityDegradation(playerState, currentFormat)
    
    // Atualizar estado local
    lastVideoFormat = playerState.lastVideoFormat
    
    // Se degradação foi detectada, mostrar Toast (comportamento específico do PlayerActivity)
    if (playerState.qualityDegradedWarningShown && !qualityDegradedWarningShown) {
      qualityDegradedWarningShown = true
      
      val message = when {
        lastVideoFormat != null && currentFormat.bitrate < (lastVideoFormat!!.bitrate * 0.7) && 
        currentFormat.width < (lastVideoFormat!!.width * 0.8) -> "Qualidade reduzida devido à conexão (bitrate e resolução)"
        lastVideoFormat != null && currentFormat.bitrate < (lastVideoFormat!!.bitrate * 0.7) -> "Bitrate reduzido devido à conexão"
        lastVideoFormat != null && currentFormat.width < (lastVideoFormat!!.width * 0.8) -> "Resolução reduzida devido à conexão"
        else -> "Qualidade reduzida devido à conexão"
      }
      
      // Mostrar toast não intrusivo
      qualityDegradedToast?.cancel()
      qualityDegradedToast = android.widget.Toast.makeText(
        this,
        message,
        android.widget.Toast.LENGTH_SHORT
      ).apply {
        setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL, 0, 100)
        show()
      }
      
      // Resetar flag após 30 segundos
      android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        qualityDegradedWarningShown = false
      }, 30000)
    }
  }
  
  // ✅ FASE 2: Failover adaptado para Xtream Code API
  private fun retryWithFailover(originalUrl: String, attempt: Int) {
    val exo = player ?: return
    
    failoverAttempts = attempt
    
    when (attempt) {
      1 -> {
        // Tentativa 1: Adicionar timestamp para evitar cache
        val urlWithTimestamp = if (originalUrl.contains("?")) {
          "$originalUrl&t=${System.currentTimeMillis()}"
        } else {
          "$originalUrl?t=${System.currentTimeMillis()}"
        }
        android.util.Log.i("PlayerActivity", "🔄 Failover tentativa 1: Adicionando timestamp para evitar cache")
        retryStream(urlWithTimestamp)
      }
      2 -> {
        // Tentativa 2: Reduzir qualidade e tentar novamente
        android.util.Log.i("PlayerActivity", "🔄 Failover tentativa 2: Reduzindo qualidade")
        if (currentMaxBitrate > 1_000_000) {
          currentMaxBitrate = (currentMaxBitrate * 0.7).toInt() // Reduzir 30%
          exo.trackSelectionParameters = TrackSelectionParameters.Builder(this)
            .setMaxVideoBitrate(currentMaxBitrate)
            .setMinVideoBitrate((currentMaxBitrate * 0.3).toInt())
            .build()
        }
        retryStream(originalUrl)
      }
      3 -> {
        // Tentativa 3: Limpar buffer e tentar novamente com timestamp
        android.util.Log.i("PlayerActivity", "🔄 Failover tentativa 3: Limpando buffer e tentando novamente")
        exo.stop()
        exo.clearMediaItems()
        val urlWithTimestamp = if (originalUrl.contains("?")) {
          "$originalUrl&t=${System.currentTimeMillis()}"
        } else {
          "$originalUrl?t=${System.currentTimeMillis()}"
        }
        retryStream(urlWithTimestamp)
      }
      else -> {
        // Tentativa final: URL original sem modificações
        android.util.Log.i("PlayerActivity", "🔄 Failover tentativa final: URL original")
        retryStream(originalUrl)
      }
    }
  }
  
  // ✅ FASE 2: Retry stream com delay
  private fun retryStream(url: String) {
    val exo = player ?: return
    
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
      try {
        exo.stop()
        exo.clearMediaItems()
        
        val mediaItem = if (contentType == "live") {
          MediaItem.Builder()
            .setUri(url)
            .setLiveConfiguration(
              MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(2000) // ✅ Low Latency AJUSTADO: 2s de offset (era 0) - mais estável
                .setMinOffsetMs(1000) // ✅ Low Latency AJUSTADO: Offset mínimo 1s (era 0) - evitar travamentos
                .setMaxOffsetMs(5000) // ✅ Low Latency AJUSTADO: Máximo 5s de atraso (era 3s) - mais estabilidade
                .setMinPlaybackSpeed(0.95f) // ✅ Low Latency AJUSTADO: Velocidade mínima 0.95 (era 0.98) - mais tolerante
                .setMaxPlaybackSpeed(1.05f) // ✅ Low Latency AJUSTADO: Velocidade máxima 1.05 (era 1.02) - mais tolerante
                .build()
            )
            .build()
        } else {
          MediaItem.fromUri(url)
        }
        
        exo.setMediaItem(mediaItem)
        exo.prepare()
        exo.playWhenReady = true
        
        android.util.Log.i("PlayerActivity", "✅ Stream reiniciado após failover")
      } catch (e: Exception) {
        android.util.Log.e("PlayerActivity", "❌ Erro ao reiniciar stream: ${e.message}", e)
      }
    }, 2000) // Aguardar 2 segundos antes de tentar novamente
  }
  
  // ✅ BOTÕES MODERNOS QUANDO PAUSA
  private fun showPauseControls() {
    if (pauseControlsOverlay != null || player == null) return
    
    val rootLayout = pv.parent as? FrameLayout ?: return
    val density = resources.displayMetrics.density
    
    pauseControlsOverlay = FrameLayout(this).apply {
      layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
      )
      setBackgroundColor(android.graphics.Color.argb(150, 0, 0, 0)) // Fundo semi-transparente
      
      // Container central com botões
      val container = android.widget.LinearLayout(this@PlayerActivity).apply {
        orientation = android.widget.LinearLayout.HORIZONTAL
        gravity = android.view.Gravity.CENTER
        layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.WRAP_CONTENT,
          FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          gravity = android.view.Gravity.CENTER
        }
        
        // Botão Retroceder 10s
        val rewindButton = createModernButton("⏪", "10s") { 
          player?.let { 
            val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
            it.seekTo(newPos)
          }
        }
        addView(rewindButton)
        
        // Botão Play
        val playButton = createModernButton("▶", "Play") {
          player?.play()
        }
        addView(playButton)
        
        // Botão Avançar 10s
        val forwardButton = createModernButton("⏩", "10s") {
          player?.let {
            val newPos = if (it.duration > 0) {
              (it.currentPosition + 10000).coerceAtMost(it.duration)
            } else {
              it.currentPosition + 10000
            }
            it.seekTo(newPos)
          }
        }
        addView(forwardButton)
      }
      addView(container)
    }
    
    rootLayout.addView(pauseControlsOverlay)
    isPausedControlsVisible = true
  }
  
  private fun hidePauseControls() {
    pauseControlsOverlay?.let { overlay ->
      (overlay.parent as? FrameLayout)?.removeView(overlay)
      pauseControlsOverlay = null
      isPausedControlsVisible = false
    }
  }
  
  private fun createModernButton(text: String, label: String, onClick: () -> Unit): android.widget.Button {
    val buttonSize = if (MaxiApp.isTv) 80 else 64 // dp
    val density = resources.displayMetrics.density
    val sizePx = (buttonSize * density).toInt()
    val buttonText = "$text\n$label"
    
    return android.widget.Button(this).apply {
      this.text = buttonText
      textSize = if (MaxiApp.isTv) 18f else 14f
      setTextColor(android.graphics.Color.WHITE)
      setTypeface(null, android.graphics.Typeface.BOLD)
      setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
      
      // Gradiente moderno azul/ciano
      background = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(android.graphics.Color.argb(220, 0, 212, 255)) // Azul ciano
        setStroke((4 * density).toInt(), android.graphics.Color.WHITE)
      }
      
      layoutParams = android.widget.LinearLayout.LayoutParams(sizePx, sizePx).apply {
        setMargins((12 * density).toInt(), 0, (12 * density).toInt(), 0)
      }
      
      setOnClickListener { onClick() }
      
      // Foco para TV
      setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
          animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).start()
          elevation = 12f
        } else {
          animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
          elevation = 0f
        }
      }
      
      isFocusable = true
      isFocusableInTouchMode = true
    }
  }
}

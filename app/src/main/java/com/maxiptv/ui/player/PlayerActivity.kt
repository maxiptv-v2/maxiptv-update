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

class PlayerActivity : ComponentActivity() {
  private var player: ExoPlayer? = null
  private var isFullscreen = true // Inicia em fullscreen
  private lateinit var gestureDetector: GestureDetector
  private lateinit var windowInsetsController: WindowInsetsControllerCompat
  private var reconnectAttempts = 0 // Contador de tentativas de reconexão
  private val maxReconnectAttempts = 5 // Máximo de tentativas
  private var bufferingCount = 0 // Contador de eventos de buffering
  private var lastBufferingTime = 0L // Último tempo de buffering
  private var currentMaxBitrate = 2_200_000 // Bitrate máximo atual (começa em 2.2Mbps)
  private var qualityReduced = false // Flag para saber se qualidade já foi reduzida
  private var lastPosition = 0L // Última posição do player (para detectar travamento)
  private var lastPositionTime = 0L // Último tempo que a posição mudou
  // ✅ FASE 2: Variáveis para failover e detecção de qualidade
  private var originalStreamUrl: String = "" // URL original do stream para failover
  private var failoverAttempts = 0 // Contador de tentativas de failover
  private val maxFailoverAttempts = 4 // Máximo de tentativas de failover
  private var lastVideoFormat: Format? = null // Último formato de vídeo para detectar degradação
  private var qualityDegradedWarningShown = false // Flag para não mostrar aviso repetidamente
  private var qualityDegradedToast: android.widget.Toast? = null // Toast para aviso de qualidade degradada
  private lateinit var pv: PlayerView // PlayerView para acesso em outros métodos
  private var contentType: String = "live" // Tipo de conteúdo (live, vod, series)
  private var qualityButton: Button? = null // Botão de qualidade (mudado para Button para suportar texto "H")
  private var subtitleButton: Button? = null // Botão de legendas
  private var audioButton: Button? = null // ✅ MELHORIA 7: Botão de áudio
  private var isQualityButtonFocused = false
  private var isSubtitleButtonFocused = false
  private var isAudioButtonFocused = false // ✅ MELHORIA 7: Foco do botão de áudio
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
  // ✅ LIVE PROFESSIONAL: Overlay profissional para canais live
  private var liveChannelInfoOverlay: android.widget.TextView? = null // Overlay com informações do canal (Live)
  private var liveChannelInfoHandler: android.os.Handler? = null // Handler para atualizar informações do canal
  private var currentChannelName: String? = null // Nome do canal atual (Live)
  private var currentChannelLogo: String? = null // Logo do canal atual (Live)
  private var lastBufferSize = 0L // Último tamanho de buffer para calcular velocidade
  private var lastBufferTime = 0L // Último tempo de buffer
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
    
    // ✅ API MODERNA - WindowInsetsController (substitui systemUiVisibility depreciado)
    windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
    
    // Configurar fullscreen completo - sem nenhuma barra (TopBar, Status Bar, Navigation Bar)
    windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    
    // Manter tela ligada durante reprodução
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
    // ✅ FLAG_FULLSCREEN removido (deprecated em API 30+) - WindowInsetsController já faz isso
    
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
    
    // Criar botão de qualidade 3D com "H" dentro
    qualityButton = Button(this).apply {
      text = "H" // Texto "H" para HD
      contentDescription = "Selecionar Qualidade"
      
      // Estilo 3D do botão (efeito de profundidade)
      val shadowLayer = GradientDrawable().apply {
        setColor(Color.argb(180, 0, 0, 0)) // Sombra escura
        cornerRadius = 12f
      }
      
      val mainLayer = GradientDrawable().apply {
        setColor(Color.argb(255, 0, 212, 255)) // Azul ciano brilhante
        cornerRadius = 12f
        setStroke(2, Color.argb(255, 255, 255, 255)) // Borda branca
      }
      
      val highlightLayer = GradientDrawable().apply {
        setColor(Color.argb(100, 255, 255, 255)) // Destaque branco no topo (efeito 3D)
        cornerRadius = 12f
      }
      
      // Criar LayerDrawable para efeito 3D (sombra + botão + destaque)
      val layers = arrayOf(
        shadowLayer,
        mainLayer,
        highlightLayer
      )
      background = LayerDrawable(layers).apply {
        setLayerInset(0, 4, 4, 0, 0) // Sombra deslocada
        setLayerInset(1, 0, 0, 4, 4) // Botão principal
        setLayerInset(2, 2, 2, 6, 6) // Destaque no topo
      }
      
      // Estilo do texto
      setTextColor(Color.WHITE)
      setTypeface(null, Typeface.BOLD)
      textSize = if (MaxiApp.isTv) 20f else 18f // Tamanho do texto
      
      // Tamanho do botão
      val buttonSizeDp = if (MaxiApp.isTv) 56 else 48 // TV: maior, smartphone: menor
      // ✅ Posicionar na mesma linha da engrenagem nativa (que fica no canto inferior direito dos controles)
      // ✅ Considerar overscan: adicionar padding extra para evitar que botões fiquem para fora
      val overscanPaddingDp = when {
        MaxiApp.isFireStick -> MaxiApp.fireStickOverscanPadding.coerceAtLeast(20) // Fire Stick: mínimo 20dp
        MaxiApp.isNativeTv -> 24 // Native TV: 24dp
        MaxiApp.isTvBox -> 16 // TV Box: 16dp
        else -> 0 // Smartphone: sem overscan
      }
      // Converter dp para pixels
      val density = resources.displayMetrics.density
      val buttonSize = (buttonSizeDp * density).toInt() // Converter para pixels
      val bottomMarginDp = if (MaxiApp.isTv) 100 + overscanPaddingDp else 80
      val endMarginDp = if (MaxiApp.isTv) 100 + overscanPaddingDp else 80
      val bottomMargin = (bottomMarginDp * density).toInt() // Converter para pixels
      val endMargin = (endMarginDp * density).toInt() // Converter para pixels
      layoutParams = FrameLayout.LayoutParams(
        buttonSize,
        buttonSize,
        Gravity.BOTTOM or Gravity.END // Canto inferior direito (ao lado dos controles)
      ).apply {
        setMargins(0, 0, endMargin, bottomMargin) // left, top, right, bottom
      }
      
      // Padding interno mínimo
      setPadding(0, 0, 0, 0)
      
      // ✅ Foco com zoom e borda vermelha neon
      setOnFocusChangeListener { view, hasFocus ->
        isQualityButtonFocused = hasFocus
        if (hasFocus) {
          // Zoom quando focado
          view.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .start()
          
          // ✅ Borda vermelha neon mais visível
          val borderDrawable = GradientDrawable().apply {
            setColor(Color.argb(255, 0, 212, 255)) // Azul ciano de fundo
            cornerRadius = 12f
            setStroke(6, Color.argb(255, 255, 23, 68)) // Borda vermelha neon mais grossa e brilhante
          }
          view.background = borderDrawable
          
          // ✅ Adicionar sombra vermelha para efeito neon
          view.elevation = 16f
        } else {
          // Voltar ao normal quando perder foco
          view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(200)
            .start()
          
          // ✅ Remover elevação
          view.elevation = 0f
          
          // Restaurar estilo original
          val shadowLayer = GradientDrawable().apply {
            setColor(Color.argb(180, 0, 0, 0))
            cornerRadius = 12f
          }
          val mainLayer = GradientDrawable().apply {
            setColor(Color.argb(255, 0, 212, 255))
            cornerRadius = 12f
            setStroke(2, Color.argb(255, 255, 255, 255))
          }
          val highlightLayer = GradientDrawable().apply {
            setColor(Color.argb(100, 255, 255, 255))
            cornerRadius = 12f
          }
          view.background = LayerDrawable(arrayOf(shadowLayer, mainLayer, highlightLayer)).apply {
            setLayerInset(0, 4, 4, 0, 0)
            setLayerInset(1, 0, 0, 4, 4)
            setLayerInset(2, 2, 2, 6, 6)
          }
        }
      }
      
      // Click listener
      setOnClickListener {
        showQualityDialog()
      }
      
      // Focável para TV
      isFocusable = true
      isFocusableInTouchMode = false
      
      // ✅ Definir ID único para navegação de foco
      id = android.view.View.generateViewId()
      
      // Inicialmente escondido (só aparece quando controles estão visíveis)
      visibility = android.view.View.GONE
    }
    
    rootLayout.addView(qualityButton)
    
    // ✅ FASE 2: BOTÃO DE LEGENDAS/SUBTÍTULOS
    subtitleButton = Button(this).apply {
      text = "CC" // Closed Captions
      contentDescription = "Legendas/Subtítulos"
      
      // Estilo similar ao botão de qualidade
      val shadowLayer = GradientDrawable().apply {
        setColor(Color.argb(180, 0, 0, 0))
        cornerRadius = 12f
      }
      val mainLayer = GradientDrawable().apply {
        setColor(Color.argb(255, 0, 212, 255))
        cornerRadius = 12f
        setStroke(2, Color.argb(255, 255, 255, 255))
      }
      val highlightLayer = GradientDrawable().apply {
        setColor(Color.argb(100, 255, 255, 255))
        cornerRadius = 12f
      }
      background = LayerDrawable(arrayOf(shadowLayer, mainLayer, highlightLayer)).apply {
        setLayerInset(0, 4, 4, 0, 0)
        setLayerInset(1, 0, 0, 4, 4)
        setLayerInset(2, 2, 2, 6, 6)
      }
      
      setTextColor(Color.WHITE)
      setTypeface(null, Typeface.BOLD)
      textSize = if (MaxiApp.isTv) 18f else 16f
      
      // Tamanho e posição (ao lado do botão H, mais à esquerda)
      // ✅ Considerar overscan: adicionar padding extra para evitar que botões fiquem para fora
      val overscanPaddingDp = when {
        MaxiApp.isFireStick -> MaxiApp.fireStickOverscanPadding.coerceAtLeast(20) // Fire Stick: mínimo 20dp
        MaxiApp.isNativeTv -> 24 // Native TV: 24dp
        MaxiApp.isTvBox -> 16 // TV Box: 16dp
        else -> 0 // Smartphone: sem overscan
      }
      // Converter dp para pixels
      val density = resources.displayMetrics.density
      val buttonSizeDp = if (MaxiApp.isTv) 56 else 48
      val buttonSize = (buttonSizeDp * density).toInt() // Converter para pixels
      val bottomMarginDp = if (MaxiApp.isTv) 100 + overscanPaddingDp else 80
      val endMarginDp = if (MaxiApp.isTv) 180 + overscanPaddingDp else 140
      val bottomMargin = (bottomMarginDp * density).toInt() // Converter para pixels
      val endMargin = (endMarginDp * density).toInt() // Converter para pixels
      layoutParams = FrameLayout.LayoutParams(
        buttonSize,
        buttonSize,
        Gravity.BOTTOM or Gravity.END
      ).apply {
        setMargins(0, 0, endMargin, bottomMargin)
      }
      
      setPadding(0, 0, 0, 0)
      
      // ✅ Foco com zoom e borda vermelha neon
      setOnFocusChangeListener { view, hasFocus ->
        isSubtitleButtonFocused = hasFocus
        if (hasFocus) {
          view.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .start()
          
          // ✅ Borda vermelha neon mais visível
          val borderDrawable = GradientDrawable().apply {
            setColor(Color.argb(255, 0, 212, 255))
            cornerRadius = 12f
            setStroke(6, Color.argb(255, 255, 23, 68)) // Borda vermelha neon mais grossa e brilhante
          }
          view.background = borderDrawable
          
          // ✅ Adicionar sombra vermelha para efeito neon
          view.elevation = 16f
        } else {
          view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(200)
            .start()
          
          val shadowLayer = GradientDrawable().apply {
            setColor(Color.argb(180, 0, 0, 0))
            cornerRadius = 12f
          }
          val mainLayer = GradientDrawable().apply {
            setColor(Color.argb(255, 0, 212, 255))
            cornerRadius = 12f
            setStroke(2, Color.argb(255, 255, 255, 255))
          }
          val highlightLayer = GradientDrawable().apply {
            setColor(Color.argb(100, 255, 255, 255))
            cornerRadius = 12f
          }
          view.background = LayerDrawable(arrayOf(shadowLayer, mainLayer, highlightLayer)).apply {
            setLayerInset(0, 4, 4, 0, 0)
            setLayerInset(1, 0, 0, 4, 4)
            setLayerInset(2, 2, 2, 6, 6)
          }
        }
      }
      
      setOnClickListener {
        showSubtitleDialog()
      }
      
      isFocusable = true
      isFocusableInTouchMode = false
      
      // ✅ Definir ID único para navegação de foco
      id = android.view.View.generateViewId()
      
      visibility = android.view.View.GONE
    }
    
    rootLayout.addView(subtitleButton)
    
    // ✅ MELHORIA 7: BOTÃO DE ÁUDIO
    audioButton = Button(this).apply {
      text = "A" // Audio
      contentDescription = "Selecionar Áudio"
      
      // Estilo similar ao botão de legendas
      val shadowLayer = GradientDrawable().apply {
        setColor(Color.argb(180, 0, 0, 0))
        cornerRadius = 12f
      }
      val mainLayer = GradientDrawable().apply {
        setColor(Color.argb(255, 0, 212, 255))
        cornerRadius = 12f
        setStroke(2, Color.argb(255, 255, 255, 255))
      }
      val highlightLayer = GradientDrawable().apply {
        setColor(Color.argb(100, 255, 255, 255))
        cornerRadius = 12f
      }
      background = LayerDrawable(arrayOf(shadowLayer, mainLayer, highlightLayer)).apply {
        setLayerInset(0, 4, 4, 0, 0)
        setLayerInset(1, 0, 0, 4, 4)
        setLayerInset(2, 2, 2, 6, 6)
      }
      
      setTextColor(Color.WHITE)
      setTypeface(null, Typeface.BOLD)
      textSize = if (MaxiApp.isTv) 18f else 16f
      
      // Tamanho e posição (ao lado do botão CC, mais à esquerda)
      val overscanPaddingDp = when {
        MaxiApp.isFireStick -> MaxiApp.fireStickOverscanPadding.coerceAtLeast(20)
        MaxiApp.isNativeTv -> 24
        MaxiApp.isTvBox -> 16
        else -> 0
      }
      val density = resources.displayMetrics.density
      val buttonSizeDp = if (MaxiApp.isTv) 56 else 48
      val buttonSize = (buttonSizeDp * density).toInt()
      val bottomMarginDp = if (MaxiApp.isTv) 100 + overscanPaddingDp else 80
      val endMarginDp = if (MaxiApp.isTv) 260 + overscanPaddingDp else 200 // Mais à esquerda que CC
      val bottomMargin = (bottomMarginDp * density).toInt()
      val endMargin = (endMarginDp * density).toInt()
      layoutParams = FrameLayout.LayoutParams(
        buttonSize,
        buttonSize,
        Gravity.BOTTOM or Gravity.END
      ).apply {
        setMargins(0, 0, endMargin, bottomMargin)
      }
      
      setPadding(0, 0, 0, 0)
      
      // ✅ Foco com zoom e borda vermelha neon
      setOnFocusChangeListener { view, hasFocus ->
        isAudioButtonFocused = hasFocus
        if (hasFocus) {
          view.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .start()
          
          // ✅ Borda vermelha neon mais visível
          val borderDrawable = GradientDrawable().apply {
            setColor(Color.argb(255, 0, 212, 255))
            cornerRadius = 12f
            setStroke(6, Color.argb(255, 255, 23, 68)) // Borda vermelha neon mais grossa e brilhante
          }
          view.background = borderDrawable
          
          // ✅ Adicionar sombra vermelha para efeito neon
          view.elevation = 16f
        } else {
          view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(200)
            .start()
          
          val shadowLayer = GradientDrawable().apply {
            setColor(Color.argb(180, 0, 0, 0))
            cornerRadius = 12f
          }
          val mainLayer = GradientDrawable().apply {
            setColor(Color.argb(255, 0, 212, 255))
            cornerRadius = 12f
            setStroke(2, Color.argb(255, 255, 255, 255))
          }
          val highlightLayer = GradientDrawable().apply {
            setColor(Color.argb(100, 255, 255, 255))
            cornerRadius = 12f
          }
          view.background = LayerDrawable(arrayOf(shadowLayer, mainLayer, highlightLayer)).apply {
            setLayerInset(0, 4, 4, 0, 0)
            setLayerInset(1, 0, 0, 4, 4)
            setLayerInset(2, 2, 2, 6, 6)
          }
        }
      }
      
      setOnClickListener {
        showAudioDialog()
      }
      
      isFocusable = true
      isFocusableInTouchMode = false
      
      // ✅ Definir ID único para navegação de foco
      id = android.view.View.generateViewId()
      
      visibility = android.view.View.GONE
    }
    
    rootLayout.addView(audioButton)
    
    // ✅ Configurar ordem de navegação de foco entre botões (D-PAD LEFT/RIGHT)
    // A (esquerda) -> CC (meio) -> H (direita)
    audioButton?.let { aButton ->
      subtitleButton?.let { ccButton ->
        qualityButton?.let { hButton ->
          // ✅ Navegação completa em ambas as direções
          // A -> CC (direita)
          aButton.nextFocusRightId = ccButton.id
          // CC -> A (esquerda)
          ccButton.nextFocusLeftId = aButton.id
          // CC -> H (direita)
          ccButton.nextFocusRightId = hButton.id
          // H -> CC (esquerda)
          hButton.nextFocusLeftId = ccButton.id
          
          // ✅ Loop circular (opcional): H -> A (direita) e A -> H (esquerda)
          // Comentado para manter navegação linear simples
          // hButton.nextFocusRightId = aButton.id
          // aButton.nextFocusLeftId = hButton.id
          
          // ✅ Garantir que todos os botões são focáveis e configurados corretamente
          listOf(aButton, ccButton, hButton).forEach { button ->
            button.isFocusable = true
            button.isFocusableInTouchMode = false
            // ✅ Garantir que o botão pode receber foco via D-PAD
            button.isClickable = true
            button.isEnabled = true
          }
          
          android.util.Log.d("PlayerActivity", "✅ Navegação de foco configurada: A <-> CC <-> H")
          android.util.Log.d("PlayerActivity", "   A (ID: ${aButton.id}) -> CC (ID: ${ccButton.id}) -> H (ID: ${hButton.id})")
        }
      }
    }
    
    // ✅ Configurar estilização de legendas do PlayerView
    setupSubtitleStyle()
    
    // ✅ Listener para mostrar controles quando necessário (DEPOIS de criar o botão)
    pv.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
      android.util.Log.d("PlayerActivity", "Controles visíveis: $visibility")
      val isVisible = visibility == android.view.View.VISIBLE
      
      // Mostrar/esconder botões junto com controles (engrenagem nativa já aparece automaticamente)
      qualityButton?.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
      subtitleButton?.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
      audioButton?.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE // ✅ MELHORIA 7
      
      // ✅ Garantir que os botões são focáveis quando visíveis
      if (isVisible && MaxiApp.isTv) {
        // Garantir que os botões podem receber foco via D-PAD
        qualityButton?.isFocusable = true
        subtitleButton?.isFocusable = true
        audioButton?.isFocusable = true
        
        android.util.Log.d("PlayerActivity", "✅ Botões A, CC, H focáveis e prontos para navegação D-PAD")
      }
    })
    
    // ✅ API MODERNA - GestureDetector (substitui GestureDetectorCompat depreciado)
    gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
      override fun onDoubleTap(e: MotionEvent): Boolean {
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
    
    // ✅ MELHORIA 2: Mostrar/ocultar overlay de tempo restante baseado no tipo de conteúdo
    val isVodOrSeries = contentType == "vod" || contentType == "series"
    remainingTimeOverlay?.visibility = if (isVodOrSeries) android.view.View.VISIBLE else android.view.View.GONE
    
    // Log da URL para debug
    android.util.Log.i("PlayerActivity", "=== REPRODUZINDO URL ===")
    android.util.Log.i("PlayerActivity", "URL: $url")
    android.util.Log.i("PlayerActivity", "TIPO: $contentType")
    android.util.Log.i("PlayerActivity", "=======================")
    
    // ⚡ Configurar DataSource com timeouts diferentes para LIVE vs VOD/SERIES
    val isLive = contentType == "live"
    val connectTimeout = if (isLive) 8000 else 8000    // LIVE: 8s (aumentado para melhor estabilidade)
    val readTimeout = if (isLive) 10000 else 10000     // LIVE: 10s (aumentado para melhor estabilidade)
    
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
    
    // ⚡ CACHE OTIMIZADO: Configurações diferentes para LIVE vs VOD/SERIES
    // ✅ FASE 1: Buffer dinâmico baseado em qualidade de conexão estimada (inicia com GOOD)
    val loadControl: LoadControl = if (isLive) {
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
        val mediaItem = if (isLive) {
          MediaItem.Builder()
            .setUri(url)
            .setLiveConfiguration(
              MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(0) // ✅ Low Latency: Tentar pegar segmento mais recente
                .setMinOffsetMs(0) // ✅ Low Latency: Offset mínimo zero
                .setMaxOffsetMs(3000) // ✅ Low Latency OTIMIZADO: Máximo 3s de atraso (era 5s) - mais agressivo para esportes/notícias
                .setMinPlaybackSpeed(0.98f) // ✅ Low Latency: Velocidade mínima ajustada
                .setMaxPlaybackSpeed(1.02f) // ✅ Low Latency: Velocidade máxima ajustada
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
            
            // Aplicar qualidade de vídeo configurada
            val videoQuality = PlayerSettingsManager.getVideoQuality()
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
              android.util.Log.i("PlayerActivity", "✅ Qualidade aplicada: ${videoQuality.displayName} (${videoQuality.maxBitrate / 1000}Kbps)")
            } else {
              // Qualidade automática: usar valores padrão
              currentMaxBitrate = if (isLive) 2_200_000 else 2_500_000
              exo.trackSelectionParameters = TrackSelectionParameters.Builder(this@PlayerActivity)
                .setPreferredTextLanguage(null)
                .setMaxVideoBitrate(currentMaxBitrate)
                .setMaxVideoSize(1280, 720)
                .setMinVideoBitrate(if (isLive) 500_000 else 400_000)
                .build()
              android.util.Log.i("PlayerActivity", "✅ Qualidade automática aplicada")
            }
          } catch (e: Exception) {
            android.util.Log.e("PlayerActivity", "❌ Erro ao aplicar configurações: ${e.message}")
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
        exo.playWhenReady = true
        
        // ✅ RECONEXÃO AUTOMÁTICA quando canal trava
        exo.addListener(object : Player.Listener {
          override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
              Player.STATE_IDLE -> {
                android.util.Log.w("PlayerActivity", "⚠️ Player em IDLE")
              }
              Player.STATE_BUFFERING -> {
                val now = System.currentTimeMillis()
                
                // ⚡ DETECÇÃO DE WI-FI LENTO: Se buffering muito frequente, reduzir qualidade
                if (lastBufferingTime > 0 && now - lastBufferingTime < 5000) { 
                  // Buffering a cada 5 segundos ou menos = Wi-Fi lento
                  bufferingCount++
                  android.util.Log.w("PlayerActivity", "⚠️ Buffering frequente detectado ($bufferingCount eventos em ${(now - lastBufferingTime) / 1000}s)")
                  
                  // Se mais de 3 buffering em pouco tempo, reduzir qualidade
                  if (bufferingCount >= 3 && !qualityReduced && currentMaxBitrate > 1_000_000) {
                    qualityReduced = true
                    currentMaxBitrate = if (isLive) 1_200_000 else 1_500_000 // Reduzir para 1.2Mbps (LIVE) ou 1.5Mbps (VOD)
                    
                    android.util.Log.i("PlayerActivity", "📉 Wi-Fi lento detectado! Reduzindo qualidade para ${currentMaxBitrate / 1000}kbps")
                    
                    // ✅ Aplicar novo bitrate e forçar re-seleção de tracks
                    val newParams = TrackSelectionParameters.Builder(this@PlayerActivity)
                      .setPreferredTextLanguage(null)
                      .setMaxVideoBitrate(currentMaxBitrate)
                      .setMaxVideoSize(854, 480) // Reduzir resolução para 480p
                      .setMinVideoBitrate(if (isLive) 300_000 else 250_000) // Bitrate mínimo ainda menor
                      .build()
                    
                    exo.trackSelectionParameters = newParams
                    
                    // ✅ Forçar re-seleção de tracks para aplicar nova qualidade imediatamente
                    // O ExoPlayer aplica automaticamente quando em buffering, mas garantimos aqui
                    android.util.Log.i("PlayerActivity", "✅ Qualidade reduzida automaticamente para evitar travamentos")
                    android.util.Log.i("PlayerActivity", "   Novo bitrate: ${currentMaxBitrate / 1000}kbps, Resolução: 854x480")
                  }
                } else {
                  // Reset contador se buffering espaçado (rede normal)
                  if (lastBufferingTime > 0 && now - lastBufferingTime > 10000) {
                    bufferingCount = 0
                    qualityReduced = false
                    android.util.Log.d("PlayerActivity", "✅ Rede estável, resetando contador de buffering")
                  }
                }
                
                lastBufferingTime = now
                android.util.Log.i("PlayerActivity", "⏳ Bufferizando... (contador: $bufferingCount)")
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
                android.util.Log.i("PlayerActivity", "🔄 Tentativa de reconexão $reconnectAttempts/$maxReconnectAttempts em 2 segundos...")
                
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
                }, 2000)
              } else {
                android.util.Log.e("PlayerActivity", "❌ Máximo de tentativas atingido. Verifique sua conexão.")
              }
            }
          }
          
          override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
              // Reset contador quando voltar a tocar normalmente
              reconnectAttempts = 0
              
              // Player tocando: timeout normal para controles
              pv.controllerShowTimeoutMs = 5000
              
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
              
              // Se está tocando bem por mais de 30 segundos, resetar contador de buffering
              android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (exo.isPlaying && bufferingCount > 0) {
                  bufferingCount = 0
                  qualityReduced = false
                  android.util.Log.d("PlayerActivity", "✅ Reprodução estável, resetando detecção de Wi-Fi lento")
                }
              }, 30000) // 30 segundos
            } else {
              // ✅ Player pausado: sempre mostrar controles para exibir minutos
              if (exo.playbackState == Player.STATE_READY) {
                pv.showController()
                pv.controllerShowTimeoutMs = 0 // Nunca esconder quando pausado
                android.util.Log.d("PlayerActivity", "⏸️ Player pausado - controles sempre visíveis para mostrar minutos")
              }
            }
          }
        })
      }
  }
  private fun toggleFullscreen() {
    isFullscreen = !isFullscreen
    if (isFullscreen) {
      // ✅ API MODERNA - Entrar em fullscreen
      windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
      windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
      // ✅ API MODERNA - Sair de fullscreen
      windowInsetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
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
  
  // ✅ FASE 1: CONTROLES AVANÇADOS - Avançar/Retroceder 10 segundos
  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
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
        KeyEvent.KEYCODE_MENU -> {
          // Mostrar menu de qualidade
          showQualityDialog()
          return true
        }
        KeyEvent.KEYCODE_DPAD_UP -> {
          // Mostrar menu de legendas
          showSubtitleDialog()
          return true
        }
      }
    }
    return super.onKeyDown(keyCode, event)
  }
  
  // ✅ FASE 1: Indicador visual de seek (avançar/retroceder)
  private fun showSeekIndicator(seconds: Int) {
    // Por enquanto apenas log, mas pode adicionar overlay visual depois
    android.util.Log.i("PlayerActivity", "⏩ Seek: ${if (seconds > 0) "+" else ""}$seconds segundos")
  }
  
  // ✅ FASE 1: Dialog para seleção manual de qualidade
  private fun showQualityDialog() {
    val exo = player ?: return
    
    // ✅ SEMPRE mostrar opções de qualidade pré-definidas (mais confiável)
    val qualityOptions = mutableListOf<String>()
    
    // Adicionar opções pré-definidas (sempre disponíveis)
    PlayerSettingsManager.VideoQuality.values().forEach { quality ->
      qualityOptions.add("${quality.displayName} (${quality.maxBitrate / 1000}Kbps)")
    }
    
    // Buscar tracks disponíveis do stream (opcional)
    val currentTracks = exo.currentTracks
    val videoTracks = mutableListOf<Format>()
    
    currentTracks?.groups?.forEach { group ->
      if (group.type == C.TRACK_TYPE_VIDEO) {
        for (i in 0 until group.length) {
          val format = group.getTrackFormat(i)
          if (format.sampleMimeType?.startsWith("video/") == true) {
            videoTracks.add(format)
          }
        }
      }
    }
    
    val dialog = AlertDialog.Builder(this)
      .setTitle("Selecionar Qualidade")
      .setItems(qualityOptions.toTypedArray()) { dialogInterface, which ->
        // ✅ Manter zoom e overlay quando selecionar qualidade
        qualityButton?.let { button ->
          // Manter foco visual (zoom e borda vermelha)
          button.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .start()
          
          val borderDrawable = GradientDrawable().apply {
            setColor(Color.argb(255, 0, 212, 255))
            cornerRadius = 12f
            setStroke(4, Color.argb(255, 255, 0, 0)) // Borda vermelha neon
          }
          button.background = borderDrawable
          
          // Manter foco por mais tempo (zoom e overlay permanecem)
          button.postDelayed({
            if (!isQualityButtonFocused) {
              // Voltar ao normal após 2 segundos se não estiver focado
              button.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .start()
              
              val shadowLayer = GradientDrawable().apply {
                setColor(Color.argb(180, 0, 0, 0))
                cornerRadius = 12f
              }
              val mainLayer = GradientDrawable().apply {
                setColor(Color.argb(255, 0, 212, 255))
                cornerRadius = 12f
                setStroke(2, Color.argb(255, 255, 255, 255))
              }
              val highlightLayer = GradientDrawable().apply {
                setColor(Color.argb(100, 255, 255, 255))
                cornerRadius = 12f
              }
              button.background = LayerDrawable(arrayOf(shadowLayer, mainLayer, highlightLayer)).apply {
                setLayerInset(0, 4, 4, 0, 0)
                setLayerInset(1, 0, 0, 4, 4)
                setLayerInset(2, 2, 2, 6, 6)
              }
            }
          }, 2000)
        }
        
        // Aplicar qualidade selecionada
        val selectedQuality = PlayerSettingsManager.VideoQuality.values()[which]
        applyQuality(selectedQuality)
        
        dialogInterface.dismiss()
      }
      .setNegativeButton("Cancelar", null)
      .create()
    
    dialog.show()
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
          overlay.setTextColor(android.graphics.Color.argb(255, 255, 193, 7)) // Amarelo
          overlay.background = GradientDrawable().apply {
            setColor(android.graphics.Color.argb(200, 0, 0, 0))
            cornerRadius = 6f
            setStroke(2, android.graphics.Color.argb(255, 255, 193, 7))
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
  
  // ✅ FASE 1: Atualizar overlay de latência
  private fun updateLatency() {
    val exo = player ?: return
    if (contentType != "live") {
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
    val videoTrack = exo.currentTracks?.groups?.firstOrNull { 
      it.type == C.TRACK_TYPE_VIDEO && it.isSelected 
    }
    
    val format = videoTrack?.getTrackFormat(0)
    val audioTrack = exo.currentTracks?.groups?.firstOrNull {
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
  
  // ✅ FASE 1: Atualizar overlay de estatísticas
  private fun updateStreamStats() {
    val exo = player ?: return
    if (contentType != "live") {
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
        else -> "Desconhecida"
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
  private fun createAdaptiveLoadControl(isLive: Boolean): LoadControl {
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
  
  // ✅ FASE 2: Dialog para seleção de legendas/subtítulos
  private fun showSubtitleDialog() {
    val exo = player ?: return
    
    // Buscar tracks de texto (legendas) disponíveis
    val currentTracks = exo.currentTracks
    val textTracks = mutableListOf<Format>()
    
    currentTracks?.groups?.forEach { group ->
      if (group.type == C.TRACK_TYPE_TEXT) {
        for (i in 0 until group.length) {
          val format = group.getTrackFormat(i)
          if (format.sampleMimeType?.startsWith("text/") == true || 
              format.sampleMimeType?.startsWith("application/") == true) {
            textTracks.add(format)
          }
        }
      }
    }
    
    // Criar lista de opções
    val subtitleOptions = mutableListOf<String>()
    
    // Opção 1: Desativar legendas
    subtitleOptions.add("Desativar Legendas")
    
    // Opção 2: Automático (usar primeira disponível)
    if (textTracks.isNotEmpty()) {
      subtitleOptions.add("Automático (Primeira Disponível)")
    }
    
    // Opções 3+: Legendas específicas disponíveis
    textTracks.forEach { format ->
      val language = format.language ?: "Desconhecido"
      val label = format.label ?: ""
      val displayName = if (label.isNotEmpty()) "$label ($language)" else language
      subtitleOptions.add(displayName)
    }
    
    if (subtitleOptions.size <= 1) {
      // Nenhuma legenda disponível (só tem "Desativar")
      AlertDialog.Builder(this)
        .setTitle("Legendas/Subtítulos")
        .setMessage("Nenhuma legenda disponível para este conteúdo.")
        .setPositiveButton("OK", null)
        .show()
      return
    }
    
    AlertDialog.Builder(this)
      .setTitle("Selecionar Legendas/Subtítulos")
      .setItems(subtitleOptions.toTypedArray()) { dialogInterface, which ->
        when (which) {
          0 -> {
            // Desativar legendas
            subtitlesEnabled = false
            exo.trackSelectionParameters = TrackSelectionParameters.Builder(this)
              .setPreferredTextLanguage(null)
              .build()
            android.util.Log.i("PlayerActivity", "✅ Legendas desativadas")
            pv.subtitleView?.visibility = android.view.View.GONE
          }
          1 -> {
            // Automático (primeira disponível)
            if (textTracks.isNotEmpty()) {
              subtitlesEnabled = true
              val firstTrack = textTracks[0]
              exo.trackSelectionParameters = TrackSelectionParameters.Builder(this)
                .setPreferredTextLanguage(firstTrack.language)
                .build()
              android.util.Log.i("PlayerActivity", "✅ Legendas automáticas ativadas: ${firstTrack.language}")
              pv.subtitleView?.visibility = android.view.View.VISIBLE
            }
          }
          else -> {
            // Selecionar legenda específica
            val selectedIndex = which - 2
            if (selectedIndex >= 0 && selectedIndex < textTracks.size) {
              subtitlesEnabled = true
              val selectedTrack = textTracks[selectedIndex]
              exo.trackSelectionParameters = TrackSelectionParameters.Builder(this)
                .setPreferredTextLanguage(selectedTrack.language)
                .build()
              android.util.Log.i("PlayerActivity", "✅ Legenda selecionada: ${selectedTrack.language} (${selectedTrack.label})")
              pv.subtitleView?.visibility = android.view.View.VISIBLE
            }
          }
        }
        dialogInterface.dismiss()
      }
      .setNegativeButton("Cancelar", null)
      .show()
  }
  
  // ✅ MELHORIA 7: Dialog para seleção de tracks de áudio
  private fun showAudioDialog() {
    val exo = player ?: return
    
    // Buscar tracks de áudio disponíveis
    val currentTracks = exo.currentTracks
    val audioTracks = mutableListOf<Format>()
    
    currentTracks?.groups?.forEach { group ->
      if (group.type == C.TRACK_TYPE_AUDIO) {
        for (i in 0 until group.length) {
          val format = group.getTrackFormat(i)
          if (format.sampleMimeType?.startsWith("audio/") == true) {
            audioTracks.add(format)
          }
        }
      }
    }
    
    // Criar lista de opções
    val audioOptions = mutableListOf<String>()
    
    // Opção 1: Automático (usar primeira disponível)
    audioOptions.add("Automático (Primeira Disponível)")
    
    // Opções 2+: Tracks de áudio específicas disponíveis
    audioTracks.forEach { format ->
      val language = format.language ?: "Desconhecido"
      val label = format.label ?: ""
      val channels = format.channelCount
      val sampleRate = format.sampleRate
      val bitrate = format.bitrate
      
      val displayName = buildString {
        if (label.isNotEmpty()) {
          append(label)
          if (language != "Desconhecido") append(" ($language)")
        } else {
          append(language)
        }
        if (channels > 0) append(" - ${channels}ch")
        if (sampleRate > 0) append(" @ ${sampleRate / 1000}kHz")
        if (bitrate > 0) append(" ${bitrate / 1000}kbps")
      }
      audioOptions.add(displayName)
    }
    
    if (audioOptions.size <= 1 && audioTracks.isEmpty()) {
      // Nenhum track de áudio disponível
      AlertDialog.Builder(this)
        .setTitle("Áudio")
        .setMessage("Nenhum track de áudio alternativo disponível para este conteúdo.")
        .setPositiveButton("OK", null)
        .show()
      return
    }
    
    AlertDialog.Builder(this)
      .setTitle("Selecionar Track de Áudio")
      .setItems(audioOptions.toTypedArray()) { dialogInterface, which ->
        when (which) {
          0 -> {
            // Automático (primeira disponível)
            if (audioTracks.isNotEmpty()) {
              val firstTrack = audioTracks[0]
              exo.trackSelectionParameters = TrackSelectionParameters.Builder(this)
                .setPreferredAudioLanguage(firstTrack.language)
                .build()
              android.util.Log.i("PlayerActivity", "✅ Áudio automático ativado: ${firstTrack.language}")
            }
          }
          else -> {
            // Selecionar track específico
            val selectedIndex = which - 1
            if (selectedIndex >= 0 && selectedIndex < audioTracks.size) {
              val selectedTrack = audioTracks[selectedIndex]
              exo.trackSelectionParameters = TrackSelectionParameters.Builder(this)
                .setPreferredAudioLanguage(selectedTrack.language)
                .build()
              android.util.Log.i("PlayerActivity", "✅ Track de áudio selecionado: ${selectedTrack.language} (${selectedTrack.label})")
            }
          }
        }
        dialogInterface.dismiss()
      }
      .setNegativeButton("Cancelar", null)
      .show()
  }
  
  // ✅ FASE 2: Detectar degradação de qualidade (versão com Toast para PlayerActivity)
  private fun detectQualityDegradation(currentFormat: Format) {
    val exo = player ?: return
    if (contentType != "live") return // Apenas para live
    
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
                .setTargetOffsetMs(0) // ✅ Low Latency: Tentar pegar segmento mais recente
                .setMinOffsetMs(0) // ✅ Low Latency: Offset mínimo zero
                .setMaxOffsetMs(3000) // ✅ Low Latency OTIMIZADO: Máximo 3s de atraso (era 5s)
                .setMinPlaybackSpeed(0.98f) // ✅ Low Latency: Velocidade mínima ajustada
                .setMaxPlaybackSpeed(1.02f) // ✅ Low Latency: Velocidade máxima ajustada
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
}

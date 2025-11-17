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
import androidx.lifecycle.lifecycleScope
import com.maxiptv.MaxiApp
import com.maxiptv.data.PlayerSettingsManager
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Dns
import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.net.InetAddress
import java.net.Inet4Address
import android.app.AlertDialog
import android.widget.ImageButton
import android.view.Gravity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

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
  private lateinit var pv: PlayerView // PlayerView para acesso em outros métodos
  private var contentType: String = "live" // Tipo de conteúdo (live, vod, series)
  private var qualityButton: ImageButton? = null // Botão de qualidade
  
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
    
    // Configurar margens negativas para ocupar área da status bar
    pv.setPadding(0, -getStatusBarHeight(), 0, 0)
    
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
    
    // Criar botão de qualidade
    qualityButton = ImageButton(this).apply {
      // Ícone de qualidade (usar ícone de settings ou HD)
      setImageResource(android.R.drawable.ic_menu_preferences)
      contentDescription = "Selecionar Qualidade"
      
      // Estilo do botão
      val drawable = GradientDrawable().apply {
        setColor(Color.argb(200, 0, 0, 0)) // Fundo preto semi-transparente
        cornerRadius = 24f // Bordas arredondadas
        setStroke(2, Color.argb(255, 0, 212, 255)) // Borda azul ciano
      }
      background = drawable
      
      // Tamanho do botão
      val buttonSize = if (MaxiApp.isTv) 56 else 48 // TV: maior, smartphone: menor
      val topMargin = if (MaxiApp.isTv) 80 else 60 // Margem do topo
      val endMargin = if (MaxiApp.isTv) 24 else 16 // Margem da direita
      layoutParams = FrameLayout.LayoutParams(
        buttonSize,
        buttonSize,
        Gravity.TOP or Gravity.END // Canto superior direito
      ).apply {
        setMargins(0, topMargin, endMargin, 0) // left, top, right, bottom
      }
      
      // Padding interno
      setPadding(12, 12, 12, 12)
      
      // Cor do ícone (azul ciano)
      setColorFilter(Color.rgb(0, 212, 255))
      
      // Click listener
      setOnClickListener {
        showQualityDialog()
      }
      
      // Inicialmente escondido (só aparece quando controles estão visíveis)
      visibility = android.view.View.GONE
    }
    
    rootLayout.addView(qualityButton)
    
    // ✅ Listener para mostrar controles quando necessário (DEPOIS de criar o botão)
    pv.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
      android.util.Log.d("PlayerActivity", "Controles visíveis: $visibility")
      // Mostrar/esconder botão de qualidade junto com controles
      qualityButton?.visibility = if (visibility == android.view.View.VISIBLE) android.view.View.VISIBLE else android.view.View.GONE
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
    val loadControl: LoadControl = if (isLive) {
      // 📺 LIVE: Buffers balanceados para estabilidade sem travamentos
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          5000,   // minBufferMs: 5 segundos (buffer inicial adequado para estabilidade)
          12000,  // maxBufferMs: 12 segundos (buffer máximo para evitar travamentos)
          1500,   // bufferForPlaybackMs: 1.5 segundos (start rápido mas estável)
          3000    // bufferForPlaybackAfterRebufferMs: 3 segundos (buffer após reconexão)
        )
        .setPrioritizeTimeOverSizeThresholds(true) // Prioriza tempo real
        .setBackBuffer(5000, true) // 5s de back buffer (mais buffer para estabilidade)
        .build()
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
        
        // 🎬 CONFIGURAR MEDIAITEM COM LIVE CONFIGURATION
        val mediaItem = if (isLive) {
          MediaItem.Builder()
            .setUri(url)
            .setLiveConfiguration(
              MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs(C.TIME_UNSET) // Offset automático
                .setMinPlaybackSpeed(0.95f) // Velocidade mínima
                .setMaxPlaybackSpeed(1.05f) // Velocidade máxima
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
              }
              Player.STATE_ENDED -> {
                android.util.Log.i("PlayerActivity", "🏁 Reprodução finalizada")
              }
            }
          }
          
          override fun onVideoSizeChanged(videoSize: VideoSize) {
            android.util.Log.i("PlayerActivity", "📺 Tamanho do vídeo: ${videoSize.width}x${videoSize.height}")
          }
          
          override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.e("PlayerActivity", "❌ ERRO no player: ${error.message}")
            android.util.Log.e("PlayerActivity", "   Tipo: ${error.errorCode}")
            android.util.Log.e("PlayerActivity", "   Causa: ${error.cause}")
            
            // ⚡ RECONEXÃO AUTOMÁTICA MELHORADA (com limite de tentativas)
            if (reconnectAttempts < maxReconnectAttempts) {
              reconnectAttempts++
              android.util.Log.i("PlayerActivity", "🔄 Tentativa $reconnectAttempts/$maxReconnectAttempts em 2 segundos...")
              
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
    // ✅ Parar player quando Activity para
    player?.let { exo ->
      exo.stop()
      android.util.Log.d("PlayerActivity", "⏹️ Player parado em onStop")
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
    
    // Recriar player com nova URL
    setIntent(intent)
    recreate() // Recriar Activity para garantir limpeza completa
  }
  
  override fun onDestroy() {
    super.onDestroy()
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
    
    // Buscar tracks disponíveis
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
    
    if (videoTracks.isEmpty()) {
      // Se não há tracks disponíveis ainda, mostrar opções baseadas em configurações
      val qualityOptions = PlayerSettingsManager.VideoQuality.values().map { quality ->
        "${quality.displayName} (${quality.maxBitrate / 1000}Kbps)"
      }
      
      AlertDialog.Builder(this)
        .setTitle("Selecionar Qualidade")
        .setItems(qualityOptions.toTypedArray()) { _, which ->
          val selectedQuality = PlayerSettingsManager.VideoQuality.values()[which]
          applyQuality(selectedQuality)
        }
        .setNegativeButton("Cancelar", null)
        .show()
    } else {
      // Mostrar tracks disponíveis do stream
      val qualityOptions = videoTracks.mapIndexed { index, format ->
        val resolution = "${format.width}x${format.height}"
        val bitrate = format.bitrate / 1000 // Kbps
        val codec = format.codecs ?: "unknown"
        "$resolution @ ${bitrate}Kbps ($codec)"
      }
      
      AlertDialog.Builder(this)
        .setTitle("Selecionar Qualidade")
        .setItems(qualityOptions.toTypedArray()) { _, which ->
          val selectedFormat = videoTracks[which]
          applyFormatQuality(selectedFormat)
        }
        .setNegativeButton("Cancelar", null)
        .show()
    }
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
}

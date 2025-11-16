package com.maxiptv.ui.player
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.common.C
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.OnBackPressedCallback
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.VideoSize
import okhttp3.OkHttpClient
import okhttp3.Dns
import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.net.InetAddress
import java.net.Inet4Address

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
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
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
    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)  // Garantir fullscreen completo
    
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
          
          // Fechar o player
          finish()
        }
      }
    })
    
    val pv = PlayerView(this)
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
    pv.controllerShowTimeoutMs = 5000 // Controles somem após 5 segundos de inatividade
    pv.controllerHideOnTouch = false // Não esconder no toque
    
    // Mostrar controles ao tocar na tela
    pv.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
      android.util.Log.d("PlayerActivity", "Controles visíveis: $visibility")
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
    
    setContentView(pv)
    val url = intent.getStringExtra("url") ?: return
    val contentType = intent.getStringExtra("contentType") ?: "live" // live, vod ou series
    
    // Log da URL para debug
    android.util.Log.i("PlayerActivity", "=== REPRODUZINDO URL ===")
    android.util.Log.i("PlayerActivity", "URL: $url")
    android.util.Log.i("PlayerActivity", "TIPO: $contentType")
    android.util.Log.i("PlayerActivity", "=======================")
    
    // ⚡ Configurar DataSource com timeouts diferentes para LIVE vs VOD/SERIES
    val isLive = contentType == "live"
    val connectTimeout = if (isLive) 5000 else 8000    // VOD: 8s (REDUZIDO), LIVE: 5s (ULTRA REDUZIDO)
    val readTimeout = if (isLive) 5000 else 10000     // VOD: 10s (REDUZIDO), LIVE: 5s (ULTRA REDUZIDO)
    
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
      // 📺 LIVE: Buffers ULTRA MENORES para zero travamentos (IPTV precisa de buffers mínimos)
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          2000,   // minBufferMs: 2 segundos (ULTRA REDUZIDO - start instantâneo)
          6000,   // maxBufferMs: 6 segundos (ULTRA REDUZIDO - evita acúmulo)
          1000,   // bufferForPlaybackMs: 1 segundo (start ultra rápido)
          2000    // bufferForPlaybackAfterRebufferMs: 2 segundos (reconexão rápida)
        )
        .setPrioritizeTimeOverSizeThresholds(true) // Prioriza tempo real
        .setBackBuffer(3000, true) // 3s de back buffer (ULTRA REDUZIDO)
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
        
        // 📊 QUALIDADE ADAPTATIVA: Começa com valores padrão, reduz automaticamente se Wi-Fi lento
        currentMaxBitrate = if (isLive) 2_200_000 else 2_500_000
        exo.trackSelectionParameters = TrackSelectionParameters.Builder(this)
          .setPreferredTextLanguage(null) // Sem legendas
          .setMaxVideoBitrate(currentMaxBitrate) // Começa com bitrate padrão
          .setMaxVideoSize(1280, 720) // Limitar a 720p para performance
          .setMinVideoBitrate(if (isLive) 500_000 else 400_000) // Bitrate mínimo REDUZIDO
          .build()
        
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
                    
                    // Aplicar novo bitrate
                    exo.trackSelectionParameters = TrackSelectionParameters.Builder(this@PlayerActivity)
                      .setPreferredTextLanguage(null)
                      .setMaxVideoBitrate(currentMaxBitrate)
                      .setMaxVideoSize(854, 480) // Reduzir resolução para 480p
                      .setMinVideoBitrate(if (isLive) 300_000 else 250_000) // Bitrate mínimo ainda menor
                      .build()
                    
                    android.util.Log.i("PlayerActivity", "✅ Qualidade reduzida automaticamente para evitar travamentos")
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
                  android.util.Log.i("PlayerActivity", "📊 Qualidade: $resolution @ ${bitrate}kbps")
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
              
              // Se está tocando bem por mais de 30 segundos, resetar contador de buffering
              android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (exo.isPlaying && bufferingCount > 0) {
                  bufferingCount = 0
                  qualityReduced = false
                  android.util.Log.d("PlayerActivity", "✅ Reprodução estável, resetando detecção de Wi-Fi lento")
                }
              }, 30000) // 30 segundos
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
  
  override fun onStop() { super.onStop(); player?.pause() }
  override fun onDestroy() { super.onDestroy(); player?.release(); player = null }
  
  private fun getStatusBarHeight(): Int {
    var result = 0
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
      result = resources.getDimensionPixelSize(resourceId)
    }
    return result
  }
}

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
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // ✅ API MODERNA - WindowInsetsController (substitui systemUiVisibility depreciado)
    windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
    
    // Configurar fullscreen completo - sem nenhuma barra
    windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    
    // Manter tela ligada durante reprodução
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
    
    // ✅ API MODERNA - OnBackPressedCallback (substitui onBackPressed depreciado)
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (isFullscreen) {
          // Se está em fullscreen, volta para modo normal
          toggleFullscreen()
        } else {
          // Se não está em fullscreen, fecha o player
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
    val connectTimeout = if (isLive) 8000 else 15000   // VOD: 15s, LIVE: 8s (REDUZIDO)
    val readTimeout = if (isLive) 8000 else 15000      // VOD: 15s, LIVE: 8s (REDUZIDO)
    
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
    
    val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
      .setUserAgent("MaxiPTV/1.1.1 (Android)")
    
    val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
    
    // ⚡ CACHE OTIMIZADO: Configurações diferentes para LIVE vs VOD/SERIES
    val loadControl: LoadControl = if (isLive) {
      // 📺 LIVE: Buffers MENORES para menos travamentos (IPTV precisa de buffers pequenos)
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          3000,   // minBufferMs: 3 segundos (REDUZIDO - menos delay)
          10000,  // maxBufferMs: 10 segundos (REDUZIDO - evita acúmulo)
          1500,   // bufferForPlaybackMs: 1.5 segundos (start rápido)
          3000    // bufferForPlaybackAfterRebufferMs: 3 segundos
        )
        .setPrioritizeTimeOverSizeThresholds(true) // Prioriza tempo real
        .setBackBuffer(5000, true) // 5s de back buffer (REDUZIDO)
        .build()
    } else {
      // 🎬 VOD/SERIES: Buffers maiores para reprodução suave
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          15000,  // minBufferMs: 15 segundos (REDUZIDO)
          60000,  // maxBufferMs: 60 segundos (REDUZIDO)
          2500,   // bufferForPlaybackMs: 2.5 segundos
          5000    // bufferForPlaybackAfterRebufferMs: 5 segundos (REDUZIDO)
        )
        .setPrioritizeTimeOverSizeThresholds(false) // VOD prioriza tamanho
        .setBackBuffer(15000, true) // 15s de back buffer para VOD (REDUZIDO)
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
        
        // 📊 QUALIDADE ADAPTATIVA: Começar em qualidade média para evitar travamentos
        exo.trackSelectionParameters = TrackSelectionParameters.Builder(this)
          .setPreferredTextLanguage(null) // Sem legendas
          .setMaxVideoBitrate(if (isLive) 2_500_000 else 5_000_000) // LIVE: 2.5Mbps (REDUZIDO), VOD: 5Mbps
          .setMaxVideoSize(1280, 720) // Limitar a 720p para performance
          .setMinVideoBitrate(if (isLive) 500_000 else 800_000) // Bitrate mínimo
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
                android.util.Log.i("PlayerActivity", "⏳ Bufferizando...")
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

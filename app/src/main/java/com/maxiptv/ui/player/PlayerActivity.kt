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
    
    // Log da URL para debug
    android.util.Log.i("PlayerActivity", "=== REPRODUZINDO URL ===")
    android.util.Log.i("PlayerActivity", "URL: $url")
    android.util.Log.i("PlayerActivity", "=======================")
    
    val dataSourceFactory = DefaultHttpDataSource.Factory()
      .setAllowCrossProtocolRedirects(true)
      .setUserAgent("MaxiPTV/1.1.1 (Android)")
      .setConnectTimeoutMs(8000)  // ⚡ 8 segundos timeout conexão (mais rápido)
      .setReadTimeoutMs(8000)     // ⚡ 8 segundos timeout leitura (mais rápido)
      .setKeepPostFor302Redirects(true) // Manter método POST em redirects
    
    val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
    
    // ⚡ CACHE OTIMIZADO PARA LIVE v2 (menos buffer = menos travamentos)
    val loadControl: LoadControl = DefaultLoadControl.Builder()
      .setBufferDurationsMs(
        10000,  // minBufferMs: 10 segundos (reduzido de 15s)
        30000,  // maxBufferMs: 30 segundos (reduzido de 50s)
        1500,   // bufferForPlaybackMs: 1.5 segundos (reduzido de 2.5s)
        5000    // bufferForPlaybackAfterRebufferMs: 5 segundos (mantido)
      )
      .setPrioritizeTimeOverSizeThresholds(true) // Priorizar tempo sobre tamanho
      .setBackBuffer(10000, true) // Manter 10s de back buffer e limpar periodicamente
      .build()
    
    player = ExoPlayer.Builder(this)
      .setMediaSourceFactory(mediaSourceFactory)
      .setLoadControl(loadControl) // ✅ Aplicar cache otimizado
      .build().also { exo ->
        pv.player = exo
        exo.setMediaItem(MediaItem.fromUri(url))
        exo.prepare()
        exo.playWhenReady = true
        
        // Desabilitar legendas por padrão
        exo.trackSelectionParameters = exo.trackSelectionParameters
          .buildUpon()
          .setPreferredTextLanguage(null)
          .build()
        
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
              }
              Player.STATE_ENDED -> {
                android.util.Log.i("PlayerActivity", "🏁 Reprodução finalizada")
              }
            }
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
                  exo.setMediaItem(MediaItem.fromUri(url))
                  exo.prepare()
                  exo.playWhenReady = true
                  android.util.Log.i("PlayerActivity", "✅ Reconexão iniciada")
                } catch (e: Exception) {
                  android.util.Log.e("PlayerActivity", "❌ Falha na reconexão: ${e.message}")
                }
              }, 2000) // ⚡ 2 segundos (reduzido de 3s)
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

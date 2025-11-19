package com.maxiptv.ui.player

import android.content.Context
import androidx.media3.common.Format
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.DefaultLoadControl

/**
 * ✅ Utilitários compartilhados para ExoPlayer (LiveScreen e PlayerActivity)
 * Evita duplicação de código entre MiniPlayer e PlayerActivity
 */

// ✅ Enum para qualidade de conexão
enum class ConnectionQuality {
  EXCELLENT, GOOD, POOR
}

// ✅ Classe para manter estado mutável do player
class PlayerState {
  var connectionQuality = ConnectionQuality.GOOD
  var failoverAttempts = 0
  val maxFailoverAttempts = 4
  var currentMaxBitrate = 2_200_000 // Bitrate máximo atual
  var lastVideoFormat: Format? = null
  var qualityDegradedWarningShown = false
  var originalStreamUrl: String? = null
}

/**
 * ✅ Função para criar LoadControl adaptativo baseado na qualidade de conexão
 */
fun createAdaptiveLoadControl(quality: ConnectionQuality): LoadControl {
  return when (quality) {
    ConnectionQuality.EXCELLENT -> {
      // ✅ Buffer aumentado para conexão excelente (garantir estabilidade)
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          8000,   // minBufferMs: 8 segundos (AUMENTADO de 5s - mais estabilidade)
          18000,  // maxBufferMs: 18 segundos (AUMENTADO de 12s - mais buffer)
          3000,   // bufferForPlaybackMs: 3 segundos (AUMENTADO de 1.5s - evitar travamentos)
          5000    // bufferForPlaybackAfterRebufferMs: 5 segundos (AUMENTADO de 3s - reconexão mais estável)
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(8000, true) // 8s de back buffer (AUMENTADO de 5s)
        .build()
    }
    ConnectionQuality.GOOD -> {
      // ✅ Buffer aumentado para conexão boa (garantir estabilidade)
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          10000,  // minBufferMs: 10 segundos (AUMENTADO de 5s - mais estabilidade)
          20000,  // maxBufferMs: 20 segundos (AUMENTADO de 12s - mais buffer)
          4000,   // bufferForPlaybackMs: 4 segundos (AUMENTADO de 1.5s - evitar travamentos)
          6000    // bufferForPlaybackAfterRebufferMs: 6 segundos (AUMENTADO de 3s - reconexão mais estável)
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(10000, true) // 10s de back buffer (AUMENTADO de 5s)
        .build()
    }
    ConnectionQuality.POOR -> {
      // ✅ AUTO-BUFFER INTELIGENTE: AUMENTAR buffer quando conexão é ruim para evitar travamentos
      // Buffer MAIOR para conexão ruim (evita travamentos no Fire Stick e dispositivos com internet lenta)
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          15000,  // minBufferMs: 15 segundos (AUMENTADO de 10s - mais estabilidade)
          30000,  // maxBufferMs: 30 segundos (AUMENTADO de 20s - muito mais buffer)
          5000,   // bufferForPlaybackMs: 5 segundos (AUMENTADO de 3s - evitar travamentos)
          8000    // bufferForPlaybackAfterRebufferMs: 8 segundos (AUMENTADO de 5s - reconexão mais estável)
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(15000, true) // 15s de back buffer (AUMENTADO de 10s)
        .build()
    }
  }
}

/**
 * ✅ Função para detectar degradação de qualidade
 */
fun detectQualityDegradation(state: PlayerState, currentFormat: Format) {
  state.lastVideoFormat?.let { previousFormat ->
    val currentBitrate = currentFormat.bitrate
    val previousBitrate = previousFormat.bitrate
    val currentWidth = currentFormat.width
    val previousWidth = previousFormat.width
    
    // Detectar se qualidade caiu drasticamente
    val bitrateDrop = previousBitrate > 0 && currentBitrate < previousBitrate * 0.7 // Redução de mais de 30%
    val resolutionDrop = previousWidth > 0 && currentWidth < previousWidth * 0.8 // Redução de mais de 20%
    
    if ((bitrateDrop || resolutionDrop) && !state.qualityDegradedWarningShown) {
      state.qualityDegradedWarningShown = true
      
      val message = when {
        bitrateDrop && resolutionDrop -> "⚠️ Qualidade reduzida (bitrate e resolução)"
        bitrateDrop -> "⚠️ Bitrate reduzido devido à conexão"
        resolutionDrop -> "⚠️ Resolução reduzida devido à conexão"
        else -> "⚠️ Qualidade reduzida devido à conexão"
      }
      
      android.util.Log.w("PlayerUtils", message)
      
      // Resetar flag após 30 segundos
      android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        state.qualityDegradedWarningShown = false
      }, 30000)
    }
  }
  state.lastVideoFormat = currentFormat
}

/**
 * ✅ Função para estimar qualidade de conexão
 */
fun estimateConnectionQuality(
  player: ExoPlayer,
  latencyMs: Long,
  bufferAhead: Long,
  bitrate: Int
): ConnectionQuality {
  val latencySeconds = latencyMs / 1000
  return when {
    latencySeconds < 3 && bufferAhead > 5000 && bitrate > 1500000 -> ConnectionQuality.EXCELLENT
    latencySeconds < 5 && bufferAhead > 3000 && bitrate > 800000 -> ConnectionQuality.GOOD
    else -> ConnectionQuality.POOR
  }
}


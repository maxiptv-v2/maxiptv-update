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
      // Buffer maior para conexão excelente
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          8000,   // minBufferMs: 8 segundos
          15000,  // maxBufferMs: 15 segundos
          2000,   // bufferForPlaybackMs: 2 segundos
          4000    // bufferForPlaybackAfterRebufferMs: 4 segundos
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(6000, true) // 6s de back buffer
        .build()
    }
    ConnectionQuality.GOOD -> {
      // Buffer padrão
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          5000,   // minBufferMs: 5 segundos
          12000,  // maxBufferMs: 12 segundos
          1500,   // bufferForPlaybackMs: 1.5 segundos
          3000    // bufferForPlaybackAfterRebufferMs: 3 segundos
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(5000, true) // 5s de back buffer
        .build()
    }
    ConnectionQuality.POOR -> {
      // Buffer menor para conexão ruim
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          3000,   // minBufferMs: 3 segundos
          8000,   // maxBufferMs: 8 segundos
          1000,   // bufferForPlaybackMs: 1 segundo
          2000    // bufferForPlaybackAfterRebufferMs: 2 segundos
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(3000, true) // 3s de back buffer
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


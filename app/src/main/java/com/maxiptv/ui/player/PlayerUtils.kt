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
  var currentMaxBitrate = 1_500_000 // Bitrate máximo atual (REDUZIDO de 2.2Mbps para evitar travamentos)
  var lastVideoFormat: Format? = null
  var qualityDegradedWarningShown = false
  var originalStreamUrl: String? = null
  // ✅ Propriedades para adaptação automática de qualidade
  var bufferingCount = 0 // Contador de eventos de buffering
  var lastBufferingTime = 0L // Último tempo de buffering
  var qualityReductionLevel = 0 // Nível de redução de qualidade (0 = nenhuma, 1 = leve, 2 = média, 3 = alta)
}

/**
 * ✅ Função para criar LoadControl adaptativo baseado na qualidade de conexão
 */
fun createAdaptiveLoadControl(quality: ConnectionQuality): LoadControl {
  return when (quality) {
    ConnectionQuality.EXCELLENT -> {
      // ✅ Buffer OTIMIZADO para conexão excelente (reduzido para evitar travamentos)
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          8000,   // minBufferMs: 8 segundos (REDUZIDO de 10s - mais responsivo)
          20000,  // maxBufferMs: 20 segundos (REDUZIDO de 25s - evita acúmulo)
          4000,   // bufferForPlaybackMs: 4 segundos (mantido - bom equilíbrio)
          6000    // bufferForPlaybackAfterRebufferMs: 6 segundos (mantido - reconexão estável)
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(8000, true) // 8s de back buffer (REDUZIDO de 10s)
        .build()
    }
    ConnectionQuality.GOOD -> {
      // ✅ Buffer OTIMIZADO para conexão boa (reduzido para evitar travamentos)
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          6000,   // minBufferMs: 6 segundos (REDUZIDO de 12s - menos latência, mais estável)
          15000,  // maxBufferMs: 15 segundos (REDUZIDO de 30s - evita acúmulo excessivo)
          3000,   // bufferForPlaybackMs: 3 segundos (REDUZIDO de 5s - start mais rápido)
          5000    // bufferForPlaybackAfterRebufferMs: 5 segundos (REDUZIDO de 8s - reconexão mais rápida)
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(5000, true) // 5s de back buffer (REDUZIDO de 12s - menos memória)
        .build()
    }
    ConnectionQuality.POOR -> {
      // ✅ AUTO-BUFFER INTELIGENTE: AUMENTAR buffer quando conexão é ruim para evitar travamentos
      // Buffer MUITO MAIOR para conexão ruim (evita travamentos em canais live e futebol)
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          20000,  // minBufferMs: 20 segundos (AUMENTADO de 15s - muito mais estabilidade)
          40000,  // maxBufferMs: 40 segundos (AUMENTADO de 30s - buffer máximo para evitar travamentos)
          6000,   // bufferForPlaybackMs: 6 segundos (AUMENTADO de 5s - evitar travamentos)
          10000   // bufferForPlaybackAfterRebufferMs: 10 segundos (AUMENTADO de 8s - reconexão muito mais estável)
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(20000, true) // 20s de back buffer (AUMENTADO de 15s)
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


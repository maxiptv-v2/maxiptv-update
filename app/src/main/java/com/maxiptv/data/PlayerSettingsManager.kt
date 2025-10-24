package com.maxiptv.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * Configurações do Player - FASE 1
 * Gerencia configurações básicas de qualidade, áudio e player
 */
object PlayerSettingsManager {
  // Chaves de preferências
  private val K_VIDEO_QUALITY = stringPreferencesKey("video_quality")
  private val K_AUDIO_BOOST = booleanPreferencesKey("audio_boost")
  private val K_SILENT_MODE = booleanPreferencesKey("silent_mode")
  private val K_PLAYBACK_SPEED = stringPreferencesKey("playback_speed")
  private val K_AUTO_PLAY = booleanPreferencesKey("auto_play")
  private val K_VOLUME_MAX = intPreferencesKey("volume_max")
  
  // ============================================================================
  // QUALIDADE DE VÍDEO
  // ============================================================================
  
  suspend fun setVideoQuality(quality: VideoQuality) {
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_VIDEO_QUALITY] = quality.name
    }
    android.util.Log.i("PlayerSettings", "✅ Qualidade de vídeo definida: ${quality.displayName}")
  }
  
  suspend fun getVideoQuality(): VideoQuality {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val qualityName = prefs[K_VIDEO_QUALITY] ?: VideoQuality.AUTO.name
    return try {
      VideoQuality.valueOf(qualityName)
    } catch (e: Exception) {
      VideoQuality.AUTO
    }
  }
  
  enum class VideoQuality(val displayName: String, val maxBitrate: Int, val minBitrate: Int) {
    AUTO("Automática", 2_500_000, 400_000),
    HD("HD (720p)", 1_500_000, 800_000),
    SD("SD (480p)", 800_000, 400_000),
    ULTRA_LOW("Ultra Baixa", 400_000, 200_000)
  }
  
  // ============================================================================
  // CONTROLES DE ÁUDIO
  // ============================================================================
  
  suspend fun setAudioBoost(enabled: Boolean) {
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_AUDIO_BOOST] = enabled
    }
    android.util.Log.i("PlayerSettings", "✅ Boost de áudio: ${if (enabled) "Ativado" else "Desativado"}")
  }
  
  suspend fun isAudioBoostEnabled(): Boolean {
    val prefs = AppCtx.ctx.dataStore.data.first()
    return prefs[K_AUDIO_BOOST] ?: false
  }
  
  suspend fun setSilentMode(enabled: Boolean) {
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_SILENT_MODE] = enabled
    }
    android.util.Log.i("PlayerSettings", "✅ Modo silencioso: ${if (enabled) "Ativado" else "Desativado"}")
  }
  
  suspend fun isSilentModeEnabled(): Boolean {
    val prefs = AppCtx.ctx.dataStore.data.first()
    return prefs[K_SILENT_MODE] ?: false
  }
  
  suspend fun setMaxVolume(volume: Int) {
    val clampedVolume = volume.coerceIn(0, 100)
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_VOLUME_MAX] = clampedVolume
    }
    android.util.Log.i("PlayerSettings", "✅ Volume máximo definido: $clampedVolume%")
  }
  
  suspend fun getMaxVolume(): Int {
    val prefs = AppCtx.ctx.dataStore.data.first()
    return prefs[K_VOLUME_MAX] ?: 100
  }
  
  // ============================================================================
  // CONTROLES DE REPRODUÇÃO
  // ============================================================================
  
  suspend fun setPlaybackSpeed(speed: PlaybackSpeed) {
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_PLAYBACK_SPEED] = speed.name
    }
    android.util.Log.i("PlayerSettings", "✅ Velocidade de reprodução: ${speed.displayName}")
  }
  
  suspend fun getPlaybackSpeed(): PlaybackSpeed {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val speedName = prefs[K_PLAYBACK_SPEED] ?: PlaybackSpeed.NORMAL.name
    return try {
      PlaybackSpeed.valueOf(speedName)
    } catch (e: Exception) {
      PlaybackSpeed.NORMAL
    }
  }
  
  enum class PlaybackSpeed(val displayName: String, val multiplier: Float) {
    SLOW_0_5("0.5x (Lento)", 0.5f),
    SLOW_0_75("0.75x (Devagar)", 0.75f),
    NORMAL("1x (Normal)", 1.0f),
    FAST_1_25("1.25x (Rápido)", 1.25f),
    FAST_1_5("1.5x (Muito Rápido)", 1.5f),
    FAST_2("2x (Ultra Rápido)", 2.0f)
  }
  
  suspend fun setAutoPlay(enabled: Boolean) {
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_AUTO_PLAY] = enabled
    }
    android.util.Log.i("PlayerSettings", "✅ Auto-play: ${if (enabled) "Ativado" else "Desativado"}")
  }
  
  suspend fun isAutoPlayEnabled(): Boolean {
    val prefs = AppCtx.ctx.dataStore.data.first()
    return prefs[K_AUTO_PLAY] ?: true
  }
  
  // ============================================================================
  // UTILITÁRIOS
  // ============================================================================
  
  suspend fun resetToDefaults() {
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_VIDEO_QUALITY] = VideoQuality.AUTO.name
      prefs[K_AUDIO_BOOST] = false
      prefs[K_SILENT_MODE] = false
      prefs[K_PLAYBACK_SPEED] = PlaybackSpeed.NORMAL.name
      prefs[K_AUTO_PLAY] = true
      prefs[K_VOLUME_MAX] = 100
    }
    android.util.Log.i("PlayerSettings", "🔄 Configurações do player resetadas para padrão")
  }
  
  suspend fun getSettingsSummary(): String {
    val quality = getVideoQuality()
    val speed = getPlaybackSpeed()
    val audioBoost = isAudioBoostEnabled()
    val silentMode = isSilentModeEnabled()
    val autoPlay = isAutoPlayEnabled()
    val maxVolume = getMaxVolume()
    
    return buildString {
      appendLine("🎬 Qualidade: ${quality.displayName}")
      appendLine("⚡ Velocidade: ${speed.displayName}")
      appendLine("🔊 Boost Áudio: ${if (audioBoost) "Sim" else "Não"}")
      appendLine("🔇 Modo Silencioso: ${if (silentMode) "Sim" else "Não"}")
      appendLine("▶️ Auto-play: ${if (autoPlay) "Sim" else "Não"}")
      appendLine("📢 Volume Máx: $maxVolume%")
    }
  }
}

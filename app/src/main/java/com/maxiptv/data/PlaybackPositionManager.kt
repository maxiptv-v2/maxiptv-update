package com.maxiptv.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Sistema de Posições de Reprodução
 * Salva a posição atual de filmes e séries para permitir "continuar assistindo"
 */
@Serializable
data class PlaybackPosition(
  val contentId: Int,
  val contentType: String, // "vod" ou "series"
  val position: Long, // Posição em milissegundos
  val duration: Long, // Duração total em milissegundos
  val timestamp: Long = System.currentTimeMillis() // Quando foi salvo
)

object PlaybackPositionManager {
  private val K_PLAYBACK_POSITIONS = stringPreferencesKey("playback_positions")
  
  private val json = Json { ignoreUnknownKeys = true }
  
  /**
   * Salvar posição de reprodução
   */
  suspend fun savePosition(contentId: Int, contentType: String, position: Long, duration: Long) {
    // Só salvar se assistiu pelo menos 10 segundos e não terminou (pelo menos 5% restante)
    if (position < 10000 || position >= duration * 0.95) {
      android.util.Log.d("PlaybackPosition", "⏭️ Posição não salva: muito curta ($position ms) ou quase terminado")
      return
    }
    
    AppCtx.ctx.dataStore.edit { prefs ->
      val currentJson = prefs[K_PLAYBACK_POSITIONS] ?: "{}"
      val positions = try {
        json.decodeFromString<Map<String, PlaybackPosition>>(currentJson)
      } catch (e: Exception) {
        emptyMap()
      }
      
      val key = "${contentType}_$contentId"
      val newPosition = PlaybackPosition(contentId, contentType, position, duration)
      
      val updated = positions + (key to newPosition)
      prefs[K_PLAYBACK_POSITIONS] = json.encodeToString(updated)
    }
    
    val minutes = position / 60000
    val totalMinutes = duration / 60000
    android.util.Log.i("PlaybackPosition", "✅ Posição salva: $contentType $contentId - ${minutes}min / ${totalMinutes}min")
  }
  
  /**
   * Obter posição salva
   */
  suspend fun getPosition(contentId: Int, contentType: String): PlaybackPosition? {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val currentJson = prefs[K_PLAYBACK_POSITIONS] ?: return null
    
    return try {
      val positions = json.decodeFromString<Map<String, PlaybackPosition>>(currentJson)
      val key = "${contentType}_$contentId"
      positions[key]
    } catch (e: Exception) {
      android.util.Log.e("PlaybackPosition", "❌ Erro ao ler posições: ${e.message}")
      null
    }
  }
  
  /**
   * Remover posição salva (quando terminar de assistir)
   */
  suspend fun removePosition(contentId: Int, contentType: String) {
    AppCtx.ctx.dataStore.edit { prefs ->
      val currentJson = prefs[K_PLAYBACK_POSITIONS] ?: "{}"
      val positions = try {
        json.decodeFromString<Map<String, PlaybackPosition>>(currentJson)
      } catch (e: Exception) {
        emptyMap()
      }
      
      val key = "${contentType}_$contentId"
      val updated = positions - key
      prefs[K_PLAYBACK_POSITIONS] = json.encodeToString(updated)
    }
    
    android.util.Log.i("PlaybackPosition", "🗑️ Posição removida: $contentType $contentId")
  }
  
  /**
   * Formatar tempo para exibição (ex: "10:30")
   */
  fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
  }
  
  /**
   * Formatar tempo restante (ex: "1h 20min restantes")
   */
  fun formatRemainingTime(position: Long, duration: Long): String {
    val remaining = duration - position
    val hours = remaining / 3600000
    val minutes = (remaining % 3600000) / 60000
    
    return when {
      hours > 0 -> "${hours}h ${minutes}min restantes"
      minutes > 0 -> "${minutes}min restantes"
      else -> "menos de 1min restante"
    }
  }
}


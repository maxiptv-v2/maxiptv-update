package com.maxiptv.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Sistema de Favoritos - FASE 1
 * Gerencia favoritos de canais, filmes e séries usando DataStore existente
 */
object FavoritesManager {
  private val K_FAVORITE_CHANNELS = stringSetPreferencesKey("favorite_channels")
  private val K_FAVORITE_MOVIES = stringSetPreferencesKey("favorite_movies")
  private val K_FAVORITE_SERIES = stringSetPreferencesKey("favorite_series")
  
  private val json = Json { ignoreUnknownKeys = true }
  
  // ============================================================================
  // CANAIS FAVORITOS
  // ============================================================================
  
  suspend fun addFavoriteChannel(channelId: Int) {
    AppCtx.ctx.dataStore.edit { prefs ->
      val current = prefs[K_FAVORITE_CHANNELS] ?: emptySet()
      prefs[K_FAVORITE_CHANNELS] = current + channelId.toString()
    }
    android.util.Log.i("FavoritesManager", "✅ Canal $channelId adicionado aos favoritos")
  }
  
  suspend fun removeFavoriteChannel(channelId: Int) {
    AppCtx.ctx.dataStore.edit { prefs ->
      val current = prefs[K_FAVORITE_CHANNELS] ?: emptySet()
      prefs[K_FAVORITE_CHANNELS] = current - channelId.toString()
    }
    android.util.Log.i("FavoritesManager", "❌ Canal $channelId removido dos favoritos")
  }
  
  suspend fun isChannelFavorite(channelId: Int): Boolean {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val favorites = prefs[K_FAVORITE_CHANNELS] ?: emptySet()
    return channelId.toString() in favorites
  }
  
  suspend fun getFavoriteChannels(): Set<Int> {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val favorites = prefs[K_FAVORITE_CHANNELS] ?: emptySet()
    return favorites.mapNotNull { it.toIntOrNull() }.toSet()
  }
  
  // ============================================================================
  // FILMES FAVORITOS
  // ============================================================================
  
  suspend fun addFavoriteMovie(movieId: Int) {
    AppCtx.ctx.dataStore.edit { prefs ->
      val current = prefs[K_FAVORITE_MOVIES] ?: emptySet()
      prefs[K_FAVORITE_MOVIES] = current + movieId.toString()
    }
    android.util.Log.i("FavoritesManager", "✅ Filme $movieId adicionado aos favoritos")
  }
  
  suspend fun removeFavoriteMovie(movieId: Int) {
    AppCtx.ctx.dataStore.edit { prefs ->
      val current = prefs[K_FAVORITE_MOVIES] ?: emptySet()
      prefs[K_FAVORITE_MOVIES] = current - movieId.toString()
    }
    android.util.Log.i("FavoritesManager", "❌ Filme $movieId removido dos favoritos")
  }
  
  suspend fun isMovieFavorite(movieId: Int): Boolean {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val favorites = prefs[K_FAVORITE_MOVIES] ?: emptySet()
    return movieId.toString() in favorites
  }
  
  suspend fun getFavoriteMovies(): Set<Int> {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val favorites = prefs[K_FAVORITE_MOVIES] ?: emptySet()
    return favorites.mapNotNull { it.toIntOrNull() }.toSet()
  }
  
  // ============================================================================
  // SÉRIES FAVORITAS
  // ============================================================================
  
  suspend fun addFavoriteSeries(seriesId: Int) {
    AppCtx.ctx.dataStore.edit { prefs ->
      val current = prefs[K_FAVORITE_SERIES] ?: emptySet()
      prefs[K_FAVORITE_SERIES] = current + seriesId.toString()
    }
    android.util.Log.i("FavoritesManager", "✅ Série $seriesId adicionada aos favoritos")
  }
  
  suspend fun removeFavoriteSeries(seriesId: Int) {
    AppCtx.ctx.dataStore.edit { prefs ->
      val current = prefs[K_FAVORITE_SERIES] ?: emptySet()
      prefs[K_FAVORITE_SERIES] = current - seriesId.toString()
    }
    android.util.Log.i("FavoritesManager", "❌ Série $seriesId removida dos favoritos")
  }
  
  suspend fun isSeriesFavorite(seriesId: Int): Boolean {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val favorites = prefs[K_FAVORITE_SERIES] ?: emptySet()
    return seriesId.toString() in favorites
  }
  
  suspend fun getFavoriteSeries(): Set<Int> {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val favorites = prefs[K_FAVORITE_SERIES] ?: emptySet()
    return favorites.mapNotNull { it.toIntOrNull() }.toSet()
  }
  
  // ============================================================================
  // UTILITÁRIOS
  // ============================================================================
  
  suspend fun clearAllFavorites() {
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_FAVORITE_CHANNELS] = emptySet()
      prefs[K_FAVORITE_MOVIES] = emptySet()
      prefs[K_FAVORITE_SERIES] = emptySet()
    }
    android.util.Log.i("FavoritesManager", "🗑️ Todos os favoritos foram limpos")
  }
  
  suspend fun getFavoritesCount(): Triple<Int, Int, Int> {
    val channels = getFavoriteChannels().size
    val movies = getFavoriteMovies().size
    val series = getFavoriteSeries().size
    return Triple(channels, movies, series)
  }
}

package com.maxiptv.data
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object CacheManager {
  private val K_VOD_CACHE = stringPreferencesKey("vod_cache")
  private val K_VOD_CATS_CACHE = stringPreferencesKey("vod_cats_cache")
  private val K_SERIES_CACHE = stringPreferencesKey("series_cache")
  private val K_SERIES_CATS_CACHE = stringPreferencesKey("series_cats_cache")
  private val K_LIVE_CACHE = stringPreferencesKey("live_cache")
  private val K_LIVE_CATS_CACHE = stringPreferencesKey("live_cats_cache")
  private val K_CACHE_TIME = longPreferencesKey("cache_timestamp")
  
  private val json = Json { ignoreUnknownKeys = true }
  
  suspend fun saveVodCache(items: List<VodItem>) {
    AppCtx.ctx.dataStore.edit { 
      it[K_VOD_CACHE] = json.encodeToString(items)
      it[K_CACHE_TIME] = System.currentTimeMillis()
    }
  }
  
  suspend fun loadVodCache(): List<VodItem>? {
    if (!isCacheValid()) return null
    val prefs = AppCtx.ctx.dataStore.data.first()
    val cached = prefs[K_VOD_CACHE] ?: return null
    return try {
      json.decodeFromString<List<VodItem>>(cached)
    } catch (e: Exception) {
      null
    }
  }
  
  suspend fun saveSeriesCache(items: List<SeriesItem>) {
    AppCtx.ctx.dataStore.edit { 
      it[K_SERIES_CACHE] = json.encodeToString(items)
      it[K_CACHE_TIME] = System.currentTimeMillis()
    }
  }
  
  suspend fun loadSeriesCache(): List<SeriesItem>? {
    if (!isCacheValid()) return null
    val prefs = AppCtx.ctx.dataStore.data.first()
    val cached = prefs[K_SERIES_CACHE] ?: return null
    return try {
      json.decodeFromString<List<SeriesItem>>(cached)
    } catch (e: Exception) {
      null
    }
  }
  
  suspend fun saveLiveCache(items: List<LiveStream>) {
    AppCtx.ctx.dataStore.edit { 
      it[K_LIVE_CACHE] = json.encodeToString(items)
      it[K_CACHE_TIME] = System.currentTimeMillis()
    }
  }
  
  suspend fun loadLiveCache(): List<LiveStream>? {
    if (!isCacheValid()) return null
    val prefs = AppCtx.ctx.dataStore.data.first()
    val cached = prefs[K_LIVE_CACHE] ?: return null
    return try {
      json.decodeFromString<List<LiveStream>>(cached)
    } catch (e: Exception) {
      null
    }
  }
  
  // CATEGORIAS
  suspend fun saveVodCategories(cats: List<VodCategory>) {
    AppCtx.ctx.dataStore.edit { 
      it[K_VOD_CATS_CACHE] = json.encodeToString(cats)
    }
  }
  
  suspend fun loadVodCategories(): List<VodCategory>? {
    if (!isCacheValid()) return null
    val prefs = AppCtx.ctx.dataStore.data.first()
    val cached = prefs[K_VOD_CATS_CACHE] ?: return null
    return try {
      json.decodeFromString<List<VodCategory>>(cached)
    } catch (e: Exception) {
      null
    }
  }
  
  suspend fun saveSeriesCategories(cats: List<SeriesCategory>) {
    AppCtx.ctx.dataStore.edit { 
      it[K_SERIES_CATS_CACHE] = json.encodeToString(cats)
    }
  }
  
  suspend fun loadSeriesCategories(): List<SeriesCategory>? {
    if (!isCacheValid()) return null
    val prefs = AppCtx.ctx.dataStore.data.first()
    val cached = prefs[K_SERIES_CATS_CACHE] ?: return null
    return try {
      json.decodeFromString<List<SeriesCategory>>(cached)
    } catch (e: Exception) {
      null
    }
  }
  
  suspend fun saveLiveCategories(cats: List<LiveCategory>) {
    AppCtx.ctx.dataStore.edit { 
      it[K_LIVE_CATS_CACHE] = json.encodeToString(cats)
    }
  }
  
  suspend fun loadLiveCategories(): List<LiveCategory>? {
    if (!isCacheValid()) return null
    val prefs = AppCtx.ctx.dataStore.data.first()
    val cached = prefs[K_LIVE_CATS_CACHE] ?: return null
    return try {
      json.decodeFromString<List<LiveCategory>>(cached)
    } catch (e: Exception) {
      null
    }
  }
  
  private suspend fun isCacheValid(): Boolean {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val cacheTime = prefs[K_CACHE_TIME] ?: return false
    val now = System.currentTimeMillis()
    val hoursPassed = (now - cacheTime) / (1000 * 60 * 60)
    val isValid = hoursPassed < 24
    android.util.Log.i("CacheManager", "🔍 Cache validade: ${hoursPassed}h passadas, válido: $isValid")
    return isValid
  }
  
  /**
   * Força o carregamento do cache mesmo se expirado (para modo offline)
   */
  suspend fun loadCacheForced(): Boolean {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val hasLiveCache = prefs[K_LIVE_CACHE] != null
    val hasVodCache = prefs[K_VOD_CACHE] != null
    val hasSeriesCache = prefs[K_SERIES_CACHE] != null
    android.util.Log.i("CacheManager", "🔍 Cache disponível - Live: $hasLiveCache, VOD: $hasVodCache, Series: $hasSeriesCache")
    return hasLiveCache || hasVodCache || hasSeriesCache
  }
}


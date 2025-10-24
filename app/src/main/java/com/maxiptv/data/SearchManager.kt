package com.maxiptv.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

/**
 * Sistema de Busca - FASE 1
 * Busca básica com filtros locais usando dados já carregados
 */
object SearchManager {
  private val K_SEARCH_HISTORY = stringPreferencesKey("search_history")
  
  // ============================================================================
  // BUSCA EM CANAIS
  // ============================================================================
  
  fun searchChannels(channels: List<LiveStream>, query: String): List<LiveStream> {
    if (query.isBlank()) return emptyList()
    
    val normalizedQuery = query.lowercase().trim()
    return channels.filter { channel ->
      channel.name.lowercase().contains(normalizedQuery) ||
      channel.categoryName?.lowercase()?.contains(normalizedQuery) == true
    }.sortedBy { it.name }
  }
  
  // ============================================================================
  // BUSCA EM FILMES
  // ============================================================================
  
  fun searchMovies(movies: List<VodItem>, query: String): List<VodItem> {
    if (query.isBlank()) return emptyList()
    
    val normalizedQuery = query.lowercase().trim()
    return movies.filter { movie ->
      movie.name.lowercase().contains(normalizedQuery)
    }.sortedBy { it.name }
  }
  
  // ============================================================================
  // BUSCA EM SÉRIES
  // ============================================================================
  
  fun searchSeries(series: List<SeriesItem>, query: String): List<SeriesItem> {
    if (query.isBlank()) return emptyList()
    
    val normalizedQuery = query.lowercase().trim()
    return series.filter { serie ->
      serie.name.lowercase().contains(normalizedQuery)
    }.sortedBy { it.name }
  }
  
  // ============================================================================
  // BUSCA GLOBAL (TODOS OS TIPOS)
  // ============================================================================
  
  data class SearchResult(
    val channels: List<LiveStream>,
    val movies: List<VodItem>,
    val series: List<SeriesItem>
  )
  
  fun searchAll(
    channels: List<LiveStream>,
    movies: List<VodItem>,
    series: List<SeriesItem>,
    query: String
  ): SearchResult {
    if (query.isBlank()) return SearchResult(emptyList(), emptyList(), emptyList())
    
    return SearchResult(
      channels = searchChannels(channels, query),
      movies = searchMovies(movies, query),
      series = searchSeries(series, query)
    )
  }
  
  // ============================================================================
  // HISTÓRICO DE BUSCA
  // ============================================================================
  
  suspend fun addToSearchHistory(query: String) {
    if (query.isBlank()) return
    
    val normalizedQuery = query.trim()
    AppCtx.ctx.dataStore.edit { prefs ->
      val current = prefs[K_SEARCH_HISTORY] ?: ""
      val history = current.split(",").filter { it.isNotBlank() }.toMutableList()
      
      // Remover se já existe
      history.remove(normalizedQuery)
      
      // Adicionar no início
      history.add(0, normalizedQuery)
      
      // Manter apenas os últimos 10
      val limitedHistory = history.take(10)
      
      prefs[K_SEARCH_HISTORY] = limitedHistory.joinToString(",")
    }
    android.util.Log.i("SearchManager", "✅ Busca '$normalizedQuery' adicionada ao histórico")
  }
  
  suspend fun getSearchHistory(): List<String> {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val history = prefs[K_SEARCH_HISTORY] ?: ""
    return history.split(",").filter { it.isNotBlank() }
  }
  
  suspend fun clearSearchHistory() {
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_SEARCH_HISTORY] = ""
    }
    android.util.Log.i("SearchManager", "🗑️ Histórico de busca limpo")
  }
  
  // ============================================================================
  // BUSCA INTELIGENTE (SUGESTÕES)
  // ============================================================================
  
  fun getSearchSuggestions(
    channels: List<LiveStream>,
    movies: List<VodItem>,
    series: List<SeriesItem>,
    query: String
  ): List<String> {
    if (query.length < 2) return emptyList()
    
    val normalizedQuery = query.lowercase()
    val suggestions = mutableSetOf<String>()
    
    // Sugestões de canais
    channels.forEach { channel ->
      if (channel.name.lowercase().contains(normalizedQuery)) {
        suggestions.add(channel.name)
      }
    }
    
    // Sugestões de filmes
    movies.forEach { movie ->
      if (movie.name.lowercase().contains(normalizedQuery)) {
        suggestions.add(movie.name)
      }
    }
    
    // Sugestões de séries
    series.forEach { serie ->
      if (serie.name.lowercase().contains(normalizedQuery)) {
        suggestions.add(serie.name)
      }
    }
    
    return suggestions.take(5).sorted()
  }
}

package com.maxiptv.data

import com.maxiptv.data.SettingsRepo
data class AuthResponse(val user_info: UserInfo?)
data class UserInfo(val auth: Int?, val status: String?)

@kotlinx.serialization.Serializable
data class LiveCategory(val category_id: String, val category_name: String)
@kotlinx.serialization.Serializable
data class VodCategory(val category_id: String, val category_name: String)
@kotlinx.serialization.Serializable
data class SeriesCategory(val category_id: String, val category_name: String)

@kotlinx.serialization.Serializable
data class LiveStream(val stream_id: Int, val name: String, val category_id: String?, val stream_icon: String?) {
  @kotlinx.serialization.Transient
  var categoryName: String? = null
  fun toLiveUrl(): String {
    val (base, user, pass) = SettingsRepo.loadBlocking()
    // Remove player_api.php e adiciona live/
    val cleanBase = base.replace("/player_api.php", "").replace("player_api.php", "")
    val baseUrl = if (cleanBase.endsWith("/")) cleanBase else "$cleanBase/"
    // ✅ Xtream Code API: Formato padrão é HLS (.m3u8), mas ExoPlayer detecta automaticamente outros formatos
    return "${baseUrl}live/$user/$pass/$stream_id.m3u8"
  }
}
@kotlinx.serialization.Serializable
data class VodItem(val stream_id: Int, val name: String, val stream_icon: String?, val category_id: String?)

@kotlinx.serialization.Serializable
data class SeriesItem(val series_id: Int, val name: String, val cover: String?, val category_id: String?)

data class VodInfoResponse(val info: VodInfo?, val movie_data: Map<String,Any>?) {
  val streamUrl: String?
    get() {
      val id = (movie_data?.get("stream_id") as? Number)?.toInt() ?: return null
      val (base, user, pass) = SettingsRepo.loadBlocking()
      val cleanBase = base.replace("/player_api.php", "").replace("player_api.php", "")
      val baseUrl = if (cleanBase.endsWith("/")) cleanBase else "$cleanBase/"
      // ✅ Xtream Code API: Formato padrão é MP4, mas ExoPlayer suporta outros formatos automaticamente
      return "${baseUrl}movie/$user/$pass/$id.mp4"
    }
}
data class VodInfo(
  val name: String?, 
  val plot: String?, 
  val cover: String?,
  val rating: Int? = null,
  val rating_5based: Int? = null
) { 
  val isValid: Boolean get() = !name.isNullOrBlank() 
}

data class SeriesInfoResponse(
  val info: SeriesInfo?, 
  val seasons: List<SeasonInfo>?,
  val episodes: Map<String, List<Episode>>?  // Map de temporada -> lista de episódios
) {
  // Combinar seasons com seus episódios - SEMPRE RETORNA TODAS AS TEMPORADAS!
  fun getCombinedSeasons(): List<Season> {
    if (seasons.isNullOrEmpty()) {
      android.util.Log.w("SeriesInfo", "❌ Sem temporadas!")
      return emptyList()
    }
    
    android.util.Log.i("SeriesInfo", "📊 Processando ${seasons.size} temporadas de ${info?.name}")
    
    // SEMPRE retornar TODAS as temporadas, mesmo sem episódios
    val result = seasons.map { seasonInfo ->
      val key = seasonInfo.season_number.toString()
      val seasonEpisodes = episodes?.get(key) ?: emptyList()
      
      android.util.Log.i("SeriesInfo", "  T${seasonInfo.season_number}: ${seasonEpisodes.size} episódios")
      
      Season(seasonInfo.season_number, seasonEpisodes)
    }
    
    android.util.Log.i("SeriesInfo", "✅ Retornando ${result.size} temporadas")
    return result
  }
}
data class SeriesInfo(val name: String?, val plot: String?, val cover: String?)
data class SeasonInfo(val season_number: Int, val name: String?, val cover: String?)
data class Season(val season_number: Int, val episodes: List<Episode>)
data class Episode(val id: String, val title: String?, val episode_num: String?, val info: EpisodeInfo?) {
  val streamUrl: String?
    get() {
      val (base, user, pass) = SettingsRepo.loadBlocking()
      val cleanBase = base.replace("/player_api.php", "").replace("player_api.php", "")
      val baseUrl = if (cleanBase.endsWith("/")) cleanBase else "$cleanBase/"
      // ✅ Xtream Code API: Formato padrão é MP4, mas ExoPlayer suporta outros formatos automaticamente
      return "${baseUrl}series/$user/$pass/$id.mp4"
    }
}
data class EpisodeInfo(val plot: String?)

data class FeaturedItem(val title: String, val imageUrl: String?, val vodId: Int?)

// ============================================================================
// MODELOS DE EPG (Electronic Program Guide)
// ============================================================================

/**
 * Representa um programa de TV no EPG
 */
data class EpgProgramme(
    val channelId: String,          // ID do canal (ex: "GLOBO HD")
    val title: String,               // Título do programa
    val subTitle: String?,           // Subtítulo/episódio
    val description: String?,        // Descrição do programa
    val start: Long,                 // Timestamp de início (millis)
    val stop: Long,                  // Timestamp de fim (millis)
    val rating: String?              // Classificação indicativa
) {
    /**
     * Verifica se o programa está no ar agora
     */
    fun isCurrentlyAiring(): Boolean {
        val now = System.currentTimeMillis()
        return now in start..stop
    }
    
    /**
     * Formata o horário de início (HH:mm)
     */
    fun startTime(): String {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(start))
    }
    
    /**
     * Formata o horário de fim (HH:mm)
     */
    fun stopTime(): String {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(stop))
    }
}

// ============================================================================
// MODELOS DE AGRUPAMENTO POR IDIOMA (VOD e Séries)
// ============================================================================

/**
 * Representa uma variante de idioma de um filme/série
 */
data class Variant(
    val displayName: String,     // Nome completo com tags
    val streamId: Int,            // ID do stream
    val cover: String?,           // URL da capa
    val categoryId: String?,      // ID da categoria
    val lang: com.maxiptv.util.Lang  // Idioma detectado
)

/**
 * Agrupa todas as variantes de um filme/série pelo título base
 */
data class MediaGrouped(
    val baseTitle: String,
    val variants: MutableList<Variant> = mutableListOf()
) {
    /**
     * Retorna a variante preferida (DUBLADO > LEGENDADO > ORIGINAL)
     */
    fun preferred(): Variant? = variants.minByOrNull { com.maxiptv.util.langPriority(it.lang) }
    
    /**
     * Retorna a primeira capa disponível
     */
    val cover: String? get() = variants.firstOrNull()?.cover
    
    /**
     * Retorna o ID da categoria (assumindo que todas as variantes têm o mesmo)
     */
    val categoryId: String? get() = variants.firstOrNull()?.categoryId
}

/**
 * Representa uma variante de idioma de um episódio
 */
data class EpisodeVariant(
    val displayName: String,     // Nome completo com tags
    val episodeId: String,       // ID do episódio
    val streamUrl: String?,      // URL do stream
    val lang: com.maxiptv.util.Lang  // Idioma detectado
)

/**
 * Agrupa todas as variantes de um episódio pelo título base
 */
data class EpisodeGrouped(
    val season: Int,
    val number: Int,             // Número do episódio
    val baseTitle: String,
    val variants: MutableList<EpisodeVariant> = mutableListOf()
) {
    /**
     * Retorna a variante preferida (DUBLADO > LEGENDADO > ORIGINAL)
     */
    fun preferred(): EpisodeVariant? = variants.minByOrNull { com.maxiptv.util.langPriority(it.lang) }
}

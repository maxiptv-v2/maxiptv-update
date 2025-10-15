package com.maxiptv.data
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object XRepo {
  private var api: XtreamApi? = null
  private var base = ""
  private var user = ""
  private var pass = ""

  private val _liveCats = MutableStateFlow<List<LiveCategory>>(emptyList()); val liveCategories = _liveCats.asStateFlow()
  private val _live = MutableStateFlow<List<LiveStream>>(emptyList()); val liveStreams = _live.asStateFlow()

  private val _vodCats = MutableStateFlow<List<VodCategory>>(emptyList()); val vodCategories = _vodCats.asStateFlow()
  private val _vod = MutableStateFlow<List<VodItem>>(emptyList()); val vodItems = _vod.asStateFlow()
  private val _vodGrouped = MutableStateFlow<List<MediaGrouped>>(emptyList()); val vodGrouped = _vodGrouped.asStateFlow()
  val featured = MutableStateFlow<List<FeaturedItem>>(emptyList())

  private val _seriesCats = MutableStateFlow<List<SeriesCategory>>(emptyList()); val seriesCategories = _seriesCats.asStateFlow()
  private val _series = MutableStateFlow<List<SeriesItem>>(emptyList()); val seriesItems = _series.asStateFlow()
  private val _seriesGrouped = MutableStateFlow<List<MediaGrouped>>(emptyList()); val seriesGrouped = _seriesGrouped.asStateFlow()

  val vodInfo = MutableStateFlow<VodInfoResponse?>(null)
  val seriesInfo = MutableStateFlow<SeriesInfoResponse?>(null)

  fun configure(baseUrl: String, u: String, p: String) {
    // Remove player_api.php se existir na URL (para evitar duplicação)
    val cleanUrl = baseUrl.replace("/player_api.php", "").replace("player_api.php", "")
    base = if (cleanUrl.endsWith("/")) cleanUrl else "$cleanUrl/"; user = u; pass = p
    val moshi = Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()
    val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC } // Mudado para BASIC
    val debugInterceptor = okhttp3.Interceptor { chain ->
      val request = chain.request()
      val response = chain.proceed(request)
      
      // Log especial para get_series_info
      if (request.url.toString().contains("get_series_info")) {
        val bodyString = response.peekBody(Long.MAX_VALUE).string()
        android.util.Log.i("XRepo", "========== RAW JSON ==========")
        android.util.Log.i("XRepo", "URL: ${request.url}")
        android.util.Log.i("XRepo", "JSON (primeiros 2000 chars): ${bodyString.take(2000)}")
        android.util.Log.i("XRepo", "==============================")
      }
      
      response
    }
    val userAgentInterceptor = okhttp3.Interceptor { chain ->
      val request = chain.request().newBuilder()
        .header("User-Agent", "MaxiPTV/1.1.0 (Android)")
        .build()
      chain.proceed(request)
    }
    val http = OkHttpClient.Builder()
      .addInterceptor(userAgentInterceptor)
      .addInterceptor(debugInterceptor)
      .addInterceptor(log)
      .build()
    val retrofit = Retrofit.Builder().baseUrl(base).addConverterFactory(MoshiConverterFactory.create(moshi)).client(http).build()
    api = retrofit.create(XtreamApi::class.java)
  }

  suspend fun ensureLiveLoaded() {
    // Tentar carregar do cache primeiro
    if (_live.value.isEmpty()) {
      val cachedLive = CacheManager.loadLiveCache()
      val cachedCats = CacheManager.loadLiveCategories()
      if (cachedLive != null && cachedLive.isNotEmpty() && cachedCats != null) {
        _live.emit(cachedLive)
        _liveCats.emit(cachedCats)
        android.util.Log.i("XRepo", "✅ LIVE carregado do CACHE (${cachedLive.size} canais, ${cachedCats.size} categorias)")
        return
      } else {
        android.util.Log.w("XRepo", "⚠️ Cache LIVE não encontrado ou vazio")
      }
    } else {
      android.util.Log.i("XRepo", "✅ LIVE já carregado na memória")
      return
    }
    
    // Se não tem cache, buscar da API
    val a = api ?: return
    try {
      android.util.Log.i("XRepo", "🌐 Buscando LIVE da API...")
      val liveCats = a.liveCategories(user, pass)
      val liveStreams = a.liveStreams(user, pass)
      val catMap = liveCats.associateBy { it.category_id }
      val enhanced = liveStreams.onEach { it.categoryName = catMap[it.category_id]?.category_name }
      _liveCats.emit(liveCats)
      _live.emit(enhanced)
      CacheManager.saveLiveCache(enhanced)
      CacheManager.saveLiveCategories(liveCats)
      android.util.Log.i("XRepo", "✅ LIVE salvo no cache (${enhanced.size} canais, ${liveCats.size} categorias)")
    } catch (e: Exception) {
      android.util.Log.e("XRepo", "❌ Erro ao buscar LIVE da API: ${e.message}")
      e.printStackTrace()
    }
  }
  suspend fun ensureVodLoaded() {
    // Tentar carregar do cache primeiro
    if (_vod.value.isEmpty()) {
      val cachedVods = CacheManager.loadVodCache()
      val cachedCats = CacheManager.loadVodCategories()
      if (cachedVods != null && cachedVods.isNotEmpty() && cachedCats != null) {
        _vod.emit(cachedVods)
        _vodCats.emit(cachedCats)
        _vodGrouped.emit(groupVod(cachedVods))
        android.util.Log.i("XRepo", "✅ VOD carregado do CACHE (${cachedVods.size} itens, ${cachedCats.size} categorias)")
        return
      }
    } else {
      android.util.Log.i("XRepo", "✅ VOD já carregado na memória")
      return
    }
    
    // Se não tem cache, buscar da API
    val a = api ?: return
    try {
      android.util.Log.i("XRepo", "🌐 Buscando VOD da API...")
      val vodCats = a.vodCategories(user, pass)
      val vodStreams = a.vodStreams(user, pass)
      _vodCats.emit(vodCats)
      _vod.emit(vodStreams)
      _vodGrouped.emit(groupVod(vodStreams))
      CacheManager.saveVodCache(vodStreams)
      CacheManager.saveVodCategories(vodCats)
      android.util.Log.i("XRepo", "✅ VOD salvo no cache (${vodStreams.size} itens, ${vodCats.size} categorias)")
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
  suspend fun ensureSeriesLoaded() {
    // Tentar carregar do cache primeiro
    if (_series.value.isEmpty()) {
      val cachedSeries = CacheManager.loadSeriesCache()
      val cachedCats = CacheManager.loadSeriesCategories()
      if (cachedSeries != null && cachedSeries.isNotEmpty() && cachedCats != null) {
        _series.emit(cachedSeries)
        _seriesCats.emit(cachedCats)
        _seriesGrouped.emit(groupSeries(cachedSeries))
        android.util.Log.i("XRepo", "✅ SERIES carregado do CACHE (${cachedSeries.size} itens, ${cachedCats.size} categorias)")
        return
      }
    } else {
      android.util.Log.i("XRepo", "✅ SERIES já carregado na memória")
      return
    }
    
    // Se não tem cache, buscar da API
    val a = api ?: return
    try {
      android.util.Log.i("XRepo", "🌐 Buscando SERIES da API...")
      val seriesCats = a.seriesCategories(user, pass)
      val seriesItems = a.series(user, pass)
      _seriesCats.emit(seriesCats)
      _series.emit(seriesItems)
      _seriesGrouped.emit(groupSeries(seriesItems))
      CacheManager.saveSeriesCache(seriesItems)
      CacheManager.saveSeriesCategories(seriesCats)
      android.util.Log.i("XRepo", "✅ SERIES salvo no cache (${seriesItems.size} itens, ${seriesCats.size} categorias)")
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
  suspend fun ensureFeaturedLoaded() {
    val a = api ?: return
    try {
      val vodTop = a.vodStreams(user, pass).take(10)
      featured.emit(vodTop.map { FeaturedItem(it.name, it.stream_icon, it.stream_id) })
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
  suspend fun loadVodInfo(id: Int) {
    val a = api ?: return
    try {
      vodInfo.emit(a.vodInfo(user, pass, vodId = id))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
  suspend fun loadSeriesInfo(id: Int) {
    val a = api ?: return
    try {
      android.util.Log.i("XRepo", "🔍 Carregando série ID: $id")
      val response = a.seriesInfo(user, pass, seriesId = id)
      android.util.Log.i("XRepo", "📺 ${response.info?.name} → ${response.seasons?.size ?: 0} temporadas, ${response.episodes?.size ?: 0} chaves de episodes")
      seriesInfo.emit(response)
    } catch (e: Exception) {
      android.util.Log.e("XRepo", "❌ Erro: ${e.message}")
      e.printStackTrace()
    }
  }
  
  /**
   * Busca informações de uma série sem emitir no StateFlow (para uso em múltiplas chamadas)
   */
  suspend fun getSeriesInfoDirect(id: Int): SeriesInfoResponse? {
    val a = api ?: return null
    return try {
      a.seriesInfo(user, pass, seriesId = id)
    } catch (e: Exception) {
      android.util.Log.e("XRepo", "❌ Erro ao buscar série $id: ${e.message}")
      null
    }
  }

  // ============================================================================
  // FUNÇÕES DE AGRUPAMENTO POR IDIOMA (VOD e Séries)
  // ============================================================================

  /**
   * Agrupa filmes VOD por título base, mantendo todas as variantes de idioma
   */
  fun groupVod(items: List<VodItem>): List<MediaGrouped> {
    val map = LinkedHashMap<String, MediaGrouped>()
    for (it in items) {
      val rawName = it.name
      val base = com.maxiptv.util.normalizeTitle(rawName)
      val lang = com.maxiptv.util.detectLang(rawName)
      
      val v = Variant(
        displayName = rawName,
        streamId = it.stream_id,
        cover = it.stream_icon,
        categoryId = it.category_id,
        lang = lang
      )
      
      val g = map.getOrPut(base) { MediaGrouped(baseTitle = base) }
      g.variants.add(v)
    }
    
    val result = map.values.toList()
    android.util.Log.i("XRepo", "🎬 VOD agrupado: ${items.size} itens → ${result.size} grupos")
    return result
  }

  /**
   * Agrupa séries por título base, mantendo todas as variantes de idioma
   */
  fun groupSeries(items: List<SeriesItem>): List<MediaGrouped> {
    val map = LinkedHashMap<String, MediaGrouped>()
    for (it in items) {
      val rawName = it.name
      val base = com.maxiptv.util.normalizeTitle(rawName)
      val lang = com.maxiptv.util.detectLang(rawName)
      
      val v = Variant(
        displayName = rawName,
        streamId = it.series_id,
        cover = it.cover,
        categoryId = it.category_id,
        lang = lang
      )
      
      val g = map.getOrPut(base) { MediaGrouped(baseTitle = base) }
      g.variants.add(v)
    }
    
    val result = map.values.toList()
    android.util.Log.i("XRepo", "📺 SERIES agrupado: ${items.size} itens → ${result.size} grupos")
    
    // Log de exemplo com Arqueiro/Flash para debug
    val arrowGroup = result.find { it.baseTitle.contains("Arqueiro", ignoreCase = true) || it.baseTitle.contains("Arrow", ignoreCase = true) }
    if (arrowGroup != null) {
      android.util.Log.i("XRepo", "🏹 ARQUEIRO: ${arrowGroup.variants.size} variantes encontradas:")
      arrowGroup.variants.forEach { v ->
        android.util.Log.i("XRepo", "  - ID:${v.streamId} ${v.displayName}")
      }
    }
    
    val flashGroup = result.find { it.baseTitle.contains("Flash", ignoreCase = true) }
    if (flashGroup != null) {
      android.util.Log.i("XRepo", "⚡ FLASH: ${flashGroup.variants.size} variantes encontradas:")
      flashGroup.variants.forEach { v ->
        android.util.Log.i("XRepo", "  - ID:${v.streamId} ${v.displayName}")
      }
    }
    
    return result
  }

  /**
   * Agrupa episódios por (temporada, número, título base)
   */
  fun groupEpisodes(raw: List<Episode>): List<EpisodeGrouped> {
    val map = LinkedHashMap<String, EpisodeGrouped>()
    
    for (ep in raw) {
      val rawName = ep.title ?: ""
      val base = com.maxiptv.util.normalizeTitle(rawName)
      val lang = com.maxiptv.util.detectLang(rawName)
      
      // Extrair temporada e número do episódio
      val epNum = ep.episode_num?.toIntOrNull() ?: 0
      val season = 1 // A API Xtream não retorna season no Episode, vamos inferir depois
      
      val key = "$season|$epNum|$base"
      val g = map.getOrPut(key) { EpisodeGrouped(season = season, number = epNum, baseTitle = base) }
      
      g.variants.add(
        EpisodeVariant(
          displayName = rawName,
          episodeId = ep.id,
          streamUrl = ep.streamUrl,
          lang = lang
        )
      )
    }
    
    return map.values.sortedWith(compareBy({ it.season }, { it.number }))
  }
}

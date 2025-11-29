package com.maxiptv.data.soccer

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
// Tipos PlayerInfo, TeamInfo, LeagueInfo estão em SoccerModelsExtended.kt (com id: Long?)

/**
 * Repository para estatísticas de futebol
 * Usa API Sports (https://v3.football.api-sports.io/)
 * Documentação: https://www.api-sports.io/documentation/football/v3
 */
object SoccerRepository {
    private const val BASE_URL = "https://v3.football.api-sports.io/"
    private const val TAG = "SoccerRepository"
    
    // 🔑 Chave de API da API Sports
    private const val API_KEY = "bc683b9a8a5e3d87de16635ff3f04f1b"
    
    private val api: SoccerApi by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        
        val contentType = "application/json".toMediaType()
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MaxiPTV/1.1.1 (Android)")
                    .header("Accept", "application/json")
                    // Não adicionar Accept-Encoding manualmente - OkHttp descomprime gzip automaticamente
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SoccerApi::class.java)
    }
    
    /**
     * Busca detalhes completos de uma partida específica
     * Retorna: score, statistics, events, lineups, formation, status
     */
    suspend fun getMatchDetail(matchId: Long): MatchDetailFull? {
        return try {
            Log.d(TAG, "🔍 Buscando detalhes da partida $matchId na API Sports...")
            
            // Buscar fixture principal
            val fixtureResponse = api.getFixture(matchId, API_KEY)
            val fixture = fixtureResponse.response?.firstOrNull()
            if (fixture == null) {
                Log.e(TAG, "❌ Fixture não encontrada: $matchId")
                return null
            }
            
            // Buscar estatísticas separadamente (se não vierem no fixture)
            var statistics = fixture.statistics ?: emptyList()
            if (statistics.isEmpty()) {
                try {
                    val statsResponse = api.getFixtureStatistics(matchId, API_KEY)
                    // Converter ApiSportsStatistic[] para List<ApiSportsStatistic>
                    // O endpoint retorna lista de estatísticas por time, precisamos combinar
                    val statsList = mutableListOf<ApiSportsStatistic>()
                    statsResponse.response?.forEach { stat ->
                        statsList.add(stat)
                    }
                    statistics = statsList
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar estatísticas separadamente: ${e.message}")
                }
            }
            
            // Buscar eventos separadamente (se não vierem no fixture)
            var events = fixture.events
            if (events.isNullOrEmpty()) {
                try {
                    val eventsResponse = api.getFixtureEvents(matchId, API_KEY)
                    events = eventsResponse.response
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar eventos separadamente: ${e.message}")
                }
            }
            
            // Buscar lineups separadamente (se não vierem no fixture)
            var lineups = fixture.lineups
            if (lineups.isNullOrEmpty()) {
                try {
                    val lineupsResponse = api.getFixtureLineups(matchId, API_KEY)
                    lineups = lineupsResponse.response
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao buscar lineups separadamente: ${e.message}")
                }
            }
            
            // Criar fixture completo com todos os dados
            val completeFixture = fixture.copy(
                events = events,
                lineups = lineups,
                statistics = statistics
            )
            
            // Converter para MatchDetailFull
            val matchDetail = convertToMatchDetailFull(completeFixture, statistics)
            Log.d(TAG, "✅ Dados recebidos: ${matchDetail.homeTeamName} x ${matchDetail.awayTeamName}")
            return matchDetail
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar detalhes da partida $matchId", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Busca placares ao vivo
     * Retorna lista de partidas (ao vivo, pre-match, ou recentes)
     */
    suspend fun getOtherMatches(): List<MatchSummaryFull> {
        return try {
            Log.d(TAG, "🔍 Buscando partidas ao vivo na API Sports...")
            val response = api.getLiveFixtures("all", API_KEY)
            val fixtures = response.response ?: emptyList()
            
            val allMatches = fixtures.mapNotNull { fixture ->
                convertToMatchSummaryFull(fixture)
            }
            
            Log.d(TAG, "✅ ${allMatches.size} partidas encontradas")
            allMatches
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar placares ao vivo", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Busca preview completo de uma partida
     * Retorna: weather, predictions, content textual, excitement_rating
     */
    suspend fun getMatchPreview(matchId: Long): MatchPreviewFull? {
        return try {
            Log.d(TAG, "🔍 Buscando preview da partida $matchId na API Sports...")
            val response = api.getPredictions(matchId, API_KEY)
            val prediction = response.response?.firstOrNull()
            
            if (prediction == null) {
                Log.w(TAG, "⚠️ Preview não encontrado para partida $matchId")
                return null
            }
            
            // Converter para MatchPreviewFull
            val preview = convertToMatchPreviewFull(prediction)
            Log.d(TAG, "✅ Preview recebido")
            return preview
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar preview da partida $matchId", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Busca uma partida pelo nome dos times
     * Retorna o Match ID se encontrar uma correspondência nas partidas ao vivo/em andamento
     * 
     * ESTRATÉGIA DE BUSCA:
     * 1. Busca na API Sports (fixtures/live) - partidas ao vivo/em andamento
     * 2. Inclui partidas em "live", "1H", "2H", "HT" (Half Time), "pre-match", "finished", etc.
     */
    suspend fun findMatchByTeamNames(homeTeam: String, awayTeam: String): Long? {
        return try {
            Log.d(TAG, "🔍 Buscando partida por nomes: $homeTeam x $awayTeam")
            
            // Normalizar nomes dos times para comparação (remover acentos, lowercase, espaços)
            fun normalizeTeamName(name: String): String {
                return name.lowercase()
                    .replace("ã", "a").replace("á", "a").replace("à", "a").replace("â", "a")
                    .replace("é", "e").replace("ê", "e")
                    .replace("í", "i")
                    .replace("ó", "o").replace("ô", "o").replace("õ", "o")
                    .replace("ú", "u").replace("ü", "u")
                    .replace("ç", "c")
                    .replace(Regex("\\s+"), " ")
                    .trim()
            }
            
            val normalizedHome = normalizeTeamName(homeTeam)
            val normalizedAway = normalizeTeamName(awayTeam)
            
            Log.d(TAG, "   Nomes normalizados: '$normalizedHome' x '$normalizedAway'")
            
            // ESTRATÉGIA 1: Buscar em partidas ao vivo E recentes/finalizadas
            val allFixtures = mutableListOf<ApiSportsFixture>()
            
            // 1.1: Buscar partidas ao vivo
            try {
                val liveResponse = api.getLiveFixtures("all", API_KEY)
                val liveFixtures = liveResponse.response ?: emptyList()
                allFixtures.addAll(liveFixtures)
                Log.d(TAG, "   ✅ ${liveFixtures.size} partidas ao vivo encontradas")
            } catch (e: Exception) {
                Log.w(TAG, "   ⚠️ Erro ao buscar partidas ao vivo: ${e.message}")
            }
            
            // 1.2: Buscar partidas recentes (hoje e ontem) - IMPORTANTE: para encontrar jogos que já passaram
            try {
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(
                    java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }.time
                )
                
                // Buscar partidas de hoje
                val todayResponse = api.getFixturesByDate(today, API_KEY)
                val todayFixtures = todayResponse.response ?: emptyList()
                allFixtures.addAll(todayFixtures)
                Log.d(TAG, "   ✅ ${todayFixtures.size} partidas de hoje encontradas")
                
                // Buscar partidas de ontem (jogos que já passaram mas podem estar sendo transmitidos)
                val yesterdayResponse = api.getFixturesByDate(yesterday, API_KEY)
                val yesterdayFixtures = yesterdayResponse.response ?: emptyList()
                // Incluir todas as partidas finalizadas de ontem
                val finishedFixtures = yesterdayFixtures.filter { 
                    val status = it.fixture?.status?.short?.lowercase() ?: ""
                    status == "ft" || status == "aet" || status == "pen"
                }
                allFixtures.addAll(finishedFixtures)
                Log.d(TAG, "   ✅ ${finishedFixtures.size} partidas finalizadas de ontem encontradas")
            } catch (e: Exception) {
                Log.w(TAG, "   ⚠️ Erro ao buscar partidas recentes: ${e.message}")
            }
            
            // Remover duplicatas
            val fixtures = allFixtures.distinctBy { it.fixture?.id }.toList()
            Log.d(TAG, "   ✅ Total: ${fixtures.size} partidas únicas para buscar correspondência")
            
            // Buscar correspondência exata pelos nomes dos times
            for (fixture in fixtures) {
                val matchHome = fixture.teams?.home?.name ?: ""
                val matchAway = fixture.teams?.away?.name ?: ""
                
                val normalizedMatchHome = normalizeTeamName(matchHome)
                val normalizedMatchAway = normalizeTeamName(matchAway)
                
                // Verificar correspondência (direta ou invertida)
                // Usar contains para ser mais flexível (ex: "Sao Paulo FC" vs "Sao Paulo")
                val directMatch = (normalizedMatchHome.contains(normalizedHome) || normalizedHome.contains(normalizedMatchHome)) &&
                                 (normalizedMatchAway.contains(normalizedAway) || normalizedAway.contains(normalizedMatchAway))
                
                val invertedMatch = (normalizedMatchHome.contains(normalizedAway) || normalizedAway.contains(normalizedMatchHome)) &&
                                   (normalizedMatchAway.contains(normalizedHome) || normalizedHome.contains(normalizedMatchAway))
                
                if (directMatch || invertedMatch) {
                    val status = fixture.fixture?.status?.short?.lowercase() ?: ""
                    val isCancelled = status.contains("cancelled") || status.contains("postponed")
                    
                    // ✅ ACEITAR TODAS as partidas que correspondem aos times, mesmo que já tenham terminado
                    // Isso permite mostrar estatísticas de jogos que já passaram mas estão sendo transmitidos
                    if (!isCancelled) {
                        val fixtureId = fixture.fixture?.id ?: 0L
                        Log.d(TAG, "✅ Partida encontrada pelos nomes dos times: $matchHome x $matchAway (ID: $fixtureId, Status: ${fixture.fixture?.status?.short})")
                        return fixtureId
                    } else {
                        Log.d(TAG, "   ⚠️ Partida encontrada mas foi cancelada: ${fixture.fixture?.status?.short}")
                    }
                }
            }
            
            Log.d(TAG, "   ⚠️ Partida não encontrada em nenhuma fonte (ao vivo ou recente)")
            
            Log.d(TAG, "⚠️ Partida não encontrada em nenhuma fonte: $homeTeam x $awayTeam")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar partida por nomes: $homeTeam x $awayTeam", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Busca automaticamente o Match ID para um canal de futebol
     * Estratégia inteligente:
     * 1. Tenta extrair Match ID do nome do canal
     * 2. Tenta extrair nomes dos times e buscar
     * 3. Busca todas as partidas ao vivo e tenta identificar a mais relevante
     * 4. Prioriza partidas brasileiras, partidas ao vivo de verdade, etc
     * 
     * @param channelName Nome do canal (ex: "Amazon Prime 4 HD (Eventos)", "Premiere 1")
     * @return Match ID encontrado ou null
     */
    suspend fun findMatchForChannel(channelName: String): Long? {
        return try {
            // ✅ CORREÇÃO CRASH: Validar entrada
            if (channelName.isBlank()) {
                Log.w(TAG, "⚠️ Nome do canal vazio - retornando null")
                return null
            }
            
            Log.d(TAG, "🔍 Buscando Match ID para canal: '$channelName'")
            
            // ESTRATÉGIA 1: Tentar extrair Match ID diretamente do nome do canal
            val directMatchId = MatchIdExtractor.extractMatchId(channelName)
            if (directMatchId != null) {
                Log.d(TAG, "✅ Match ID extraído diretamente do nome: $directMatchId")
                return directMatchId
            }
            
            // ESTRATÉGIA 2: Tentar extrair nomes dos times e buscar
            val teamNames = MatchIdExtractor.extractTeamNames(channelName)
            if (teamNames != null) {
                Log.d(TAG, "🔍 Times extraídos do canal: ${teamNames.first} x ${teamNames.second}")
                val matchIdByTeams = findMatchByTeamNames(teamNames.first, teamNames.second)
                if (matchIdByTeams != null) {
                    Log.d(TAG, "✅ Match ID encontrado pelo nome dos times: $matchIdByTeams")
                    return matchIdByTeams
                }
            }
            
            // ESTRATÉGIA 3: Buscar partidas ao vivo E recentes/finalizadas
            Log.d(TAG, "🔍 Buscando partidas ao vivo e recentes para identificar correspondência...")
            val allFixtures = mutableListOf<ApiSportsFixture>()
            
            // 3.1: Buscar partidas ao vivo
            try {
                val liveResponse = api.getLiveFixtures("all", API_KEY)
                val liveFixtures = liveResponse.response ?: emptyList()
                allFixtures.addAll(liveFixtures)
                Log.d(TAG, "   ✅ ${liveFixtures.size} partidas ao vivo encontradas")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Erro ao buscar partidas ao vivo: ${e.message}")
            }
            
            // 3.2: Buscar partidas recentes (hoje e ontem) se não encontrou ao vivo ou para ter mais opções
            try {
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(
                    java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }.time
                )
                
                // Buscar partidas de hoje
                val todayResponse = api.getFixturesByDate(today, API_KEY)
                val todayFixtures = todayResponse.response ?: emptyList()
                allFixtures.addAll(todayFixtures.filter { it.fixture?.status?.short?.lowercase() != "ns" }) // Filtrar partidas não iniciadas
                Log.d(TAG, "   ✅ ${todayFixtures.size} partidas de hoje encontradas")
                
                // Buscar partidas de ontem (jogos recentes/finalizados)
                val yesterdayResponse = api.getFixturesByDate(yesterday, API_KEY)
                val yesterdayFixtures = yesterdayResponse.response ?: emptyList()
                // Incluir apenas partidas finalizadas ou recentes
                val recentFixtures = yesterdayFixtures.filter { 
                    val status = it.fixture?.status?.short?.lowercase() ?: ""
                    status == "ft" || status == "aet" || status == "pen"
                }
                allFixtures.addAll(recentFixtures)
                Log.d(TAG, "   ✅ ${recentFixtures.size} partidas recentes de ontem encontradas")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Erro ao buscar partidas recentes: ${e.message}")
            }
            
            if (allFixtures.isEmpty()) {
                Log.w(TAG, "⚠️ Nenhuma partida encontrada (ao vivo ou recente)")
                return null
            }
            
            val fixtures = allFixtures.distinctBy { it.fixture?.id }.toList() // Remover duplicatas
            Log.d(TAG, "   ✅ Total: ${fixtures.size} partidas únicas encontradas")
            
            // Normalizar nome do canal para busca
            val normalizedChannel = channelName.lowercase().trim()
            
            // Detectar se é canal brasileiro de futebol
            val isBrazilianChannel = normalizedChannel.contains("premiere") ||
                                     normalizedChannel.contains("premier") ||
                                     normalizedChannel.contains("sportv") ||
                                     normalizedChannel.contains("espn") ||
                                     normalizedChannel.contains("band") ||
                                     normalizedChannel.contains("cazé") ||
                                     normalizedChannel.contains("caze") ||
                                     normalizedChannel.contains("amazon") ||
                                     normalizedChannel.contains("prime")
            
            // Separar partidas ao vivo das outras
            val liveMatches = mutableListOf<ApiSportsFixture>()
            val otherMatches = mutableListOf<ApiSportsFixture>()
            
            for (fixture in fixtures) {
                val status = fixture.fixture?.status?.short?.lowercase() ?: ""
                val isLive = status == "live" || status == "1h" || status == "2h" || status == "ht"
                
                if (isLive) {
                    liveMatches.add(fixture)
                } else {
                    otherMatches.add(fixture)
                }
            }
            
            // Criar lista de partidas com pontuação de relevância
            val candidates = mutableListOf<Pair<ApiSportsFixture, Int>>()
            
            // PRIORIDADE: Processar partidas ao vivo primeiro (elas têm mais chances de ser a correta)
            for (fixture in liveMatches) {
                var score = 1000  // Base alta para partidas ao vivo
                val status = fixture.fixture?.status?.short?.lowercase() ?: ""
                val league = fixture.league?.name ?: ""
                val country = fixture.league?.country ?: ""
                
                // Ajustar score baseado no status (LIVE é o mais prioritário)
                when (status) {
                    "live" -> score += 200
                    "1h", "2h" -> score += 100
                    "ht" -> score += 50
                }
                
                // Se for canal brasileiro, priorizar partidas brasileiras
                if (isBrazilianChannel) {
                    if (country.lowercase() == "brazil" || country.lowercase() == "brasil") {
                        score += 300  // Grande bônus para partidas brasileiras
                    }
                    
                    // Priorizar ligas importantes do Brasil
                    val brazilianLeagues = listOf(
                        "brasileirão", "brasileirao", "serie a", "série a",
                        "copa do brasil", "copa libertadores", "copa sudamericana",
                        "paulistão", "paulista", "carioca", "gaúcho", "mineiro",
                        "brasileirão série b", "serie b", "copa verde"
                    )
                    if (brazilianLeagues.any { league.lowercase().contains(it) }) {
                        score += 200  // Grande bônus para ligas brasileiras importantes
                    }
                }
                
                // Bônus para qualquer liga importante (brasileiras ou internacionais)
                val importantLeagues = listOf(
                    "brasileirão", "brasileirao", "serie a", "série a",
                    "premier league", "la liga", "serie a", "bundesliga",
                    "champions league", "europa league", "copa libertadores",
                    "copa do brasil", "copa sudamericana", "world cup"
                )
                if (importantLeagues.any { league.lowercase().contains(it) }) {
                    score += 100
                }
                
                // Bônus para partidas com placar (já têm eventos acontecendo)
                if (fixture.goals?.home != null && fixture.goals?.away != null) {
                    score += 50
                }
                
                candidates.add(Pair(fixture, score))
            }
            
            // Processar outras partidas (finalizadas ou recentes) com pontuação menor
            for (fixture in otherMatches) {
                var score = 200  // Base média para partidas não ao vivo (podem estar sendo exibidas)
                val status = fixture.fixture?.status?.short?.lowercase() ?: ""
                val league = fixture.league?.name ?: ""
                val country = fixture.league?.country ?: ""
                
                // Priorizar partidas finalizadas recentes (FT) - podem estar sendo exibidas
                if (status == "ft" || status == "aet" || status == "pen") {
                    score += 100  // Bônus para partidas finalizadas
                }
                
                // Ainda priorizar partidas brasileiras em canais brasileiros
                if (isBrazilianChannel) {
                    if (country.lowercase() == "brazil" || country.lowercase() == "brasil") {
                        score += 150  // Maior bônus para brasileiras
                    }
                    
                    // Priorizar ligas importantes do Brasil
                    val brazilianLeagues = listOf(
                        "brasileirão", "brasileirao", "serie a", "série a",
                        "copa do brasil", "copa libertadores", "copa sudamericana",
                        "paulistão", "paulista", "carioca", "gaúcho", "mineiro",
                        "brasileirão série b", "serie b", "copa verde"
                    )
                    if (brazilianLeagues.any { league.lowercase().contains(it) }) {
                        score += 100
                    }
                }
                
                candidates.add(Pair(fixture, score))
            }
            
            // Ordenar por pontuação (maior primeiro)
            candidates.sortByDescending { it.second }
            
            // Se há candidatos, retornar o melhor
            if (candidates.isNotEmpty()) {
                val bestMatch = candidates.first()
                val matchId = bestMatch.first.fixture?.id ?: 0L
                val homeTeam = bestMatch.first.teams?.home?.name ?: ""
                val awayTeam = bestMatch.first.teams?.away?.name ?: ""
                val status = bestMatch.first.fixture?.status?.short ?: ""
                
                Log.d(TAG, "✅ Partida identificada para '$channelName':")
                Log.d(TAG, "   Match ID: $matchId")
                Log.d(TAG, "   Partida: $homeTeam x $awayTeam")
                Log.d(TAG, "   Status: $status")
                Log.d(TAG, "   Pontuação: ${bestMatch.second}")
                
                return matchId
            }
            
            Log.w(TAG, "⚠️ Nenhuma partida relevante encontrada para o canal '$channelName'")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar Match ID para canal '$channelName'", e)
            e.printStackTrace()
            null
        }
    }
    
    // ============================================================================
    // FUNÇÕES ADAPTADORAS: Converter modelos da API Sports para modelos existentes
    // ============================================================================
    
    /**
     * Converte ApiSportsFixture para MatchDetailFull
     */
    private fun convertToMatchDetailFull(
        fixture: ApiSportsFixture,
        statistics: List<ApiSportsStatistic>
    ): MatchDetailFull {
        val homeTeam = fixture.teams?.home
        val awayTeam = fixture.teams?.away
        
        // Converter estatísticas
        val matchStatistics = mutableListOf<MatchStatistic>()
        statistics.forEach { stat ->
            stat.statistics?.forEach { item ->
                val value = item.value ?: "0"
                matchStatistics.add(
                    MatchStatistic(
                        type = item.type,
                        home = if (stat.team?.id == homeTeam?.id) value else null,
                        away = if (stat.team?.id == awayTeam?.id) value else null
                    )
                )
            }
        }
        
        // Converter eventos
        val events = fixture.events?.mapNotNull { event ->
            MatchEvent(
                id = null,
                type = event.type?.lowercase(),
                detail = event.detail,
                comments = event.comments,
                time = MatchEventTime(
                    elapsed = event.time?.elapsed,
                    extra = event.time?.extra
                ),
                team = event.team?.let { t ->
                    TeamInfo(
                        id = t.id?.toLong(),
                        name = t.name
                    )
                },
                player = event.player?.let { p ->
                    // Converter ApiSportsPlayerInfo (id: Int?) para PlayerInfo (id: Long?)
                    PlayerInfo(
                        id = p.id?.toLong(),
                        name = p.name
                    )
                },
                assist = event.assist?.let {
                    PlayerInfo(
                        id = it.id?.toLong(),
                        name = it.name
                    )
                }
            )
        } ?: emptyList()
        
        return MatchDetailFull(
            id = fixture.fixture?.id,
            name = "${homeTeam?.name ?: ""} x ${awayTeam?.name ?: ""}",
            league = LeagueInfo(
                id = fixture.league?.id?.toLong(),
                name = fixture.league?.name
            ),
            home = TeamInfo(
                id = homeTeam?.id?.toLong(),
                name = homeTeam?.name
            ),
            away = TeamInfo(
                id = awayTeam?.id?.toLong(),
                name = awayTeam?.name
            ),
            date = fixture.fixture?.date?.substringBefore("T"),
            time = fixture.fixture?.date?.substringAfter("T")?.substringBefore("+"),
            status = MatchStatus(
                long = fixture.fixture?.status?.long,
                short = fixture.fixture?.status?.short,
                elapsed = fixture.fixture?.status?.elapsed
            ),
            score = MatchScore(
                home = fixture.goals?.home,
                away = fixture.goals?.away,
                current = MatchScoreDetail(
                    home = fixture.goals?.home,
                    away = fixture.goals?.away
                ),
                halftime = fixture.score?.halftime?.let {
                    MatchScoreDetail(
                        home = it.home,
                        away = it.away
                    )
                },
                fulltime = fixture.score?.fulltime?.let {
                    MatchScoreDetail(
                        home = it.home,
                        away = it.away
                    )
                }
            ),
            statistics = matchStatistics,
            events = events,
            lineups = null, // TODO: Converter lineups se necessário
            formation = null, // TODO: Converter formation se necessário
            match_preview = null
        )
    }
    
    /**
     * Converte ApiSportsFixture para MatchSummaryFull
     */
    private fun convertToMatchSummaryFull(fixture: ApiSportsFixture): MatchSummaryFull? {
        val fixtureInfo = fixture.fixture ?: return null
        val homeTeam = fixture.teams?.home
        val awayTeam = fixture.teams?.away
        
        return MatchSummaryFull(
            id = fixtureInfo.id ?: 0L,
            name = "${homeTeam?.name ?: ""} x ${awayTeam?.name ?: ""}",
            league = LeagueInfo(
                id = fixture.league?.id?.toLong(),
                name = fixture.league?.name
            ),
            home = TeamInfo(
                id = homeTeam?.id?.toLong(),
                name = homeTeam?.name
            ),
            away = TeamInfo(
                id = awayTeam?.id?.toLong(),
                name = awayTeam?.name
            ),
            starting_at = fixtureInfo.date,
            score = MatchScore(
                home = fixture.goals?.home,
                away = fixture.goals?.away
            ),
            status = MatchStatus(
                long = fixtureInfo.status?.long,
                short = fixtureInfo.status?.short,
                elapsed = fixtureInfo.status?.elapsed
            )
        )
    }
    
    /**
     * Converte ApiSportsPrediction para MatchPreviewFull
     */
    private fun convertToMatchPreviewFull(prediction: ApiSportsPrediction): MatchPreviewFull {
        val homeTeam = prediction.teams?.home
        val awayTeam = prediction.teams?.away
        val predData = prediction.predictions
        
        return MatchPreviewFull(
            match_id = null,
            league = LeagueInfo(
                id = if (prediction.league?.id != null) prediction.league!!.id!!.toLong() else null,
                name = prediction.league?.name
            ),
            home = TeamInfo(
                id = homeTeam?.id?.toLong(),
                name = homeTeam?.name
            ),
            away = TeamInfo(
                id = awayTeam?.id?.toLong(),
                name = awayTeam?.name
            ),
            word_count = null,
            date = null,
            time = null,
            match_data = MatchPreviewData(
                weather = null,
                excitement_rating = null,
                prediction = predData?.winner?.let {
                    MatchPrediction(
                        type = "match_winner",
                        choice = it.name
                    )
                }
            ),
            content = listOf(
                PreviewContent(
                    name = "advice",
                    content = predData?.advice
                )
            )
        )
    }
    
    /**
     * Busca odds (probabilidades de apostas) pré-jogo
     */
    suspend fun getOdds(matchId: Long): ApiSportsOdds? {
        return try {
            Log.d(TAG, "💰 Buscando odds pré-jogo para matchId: $matchId")
            val response = api.getOdds(matchId, API_KEY)
            
            if (response.response != null && response.response!!.isNotEmpty()) {
                val odds = response.response!![0]
                Log.d(TAG, "✅ Odds encontradas: ${odds.bookmakers?.size ?: 0} casas de aposta")
                odds
            } else {
                Log.w(TAG, "⚠️ Nenhuma odd encontrada")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar odds: ${e.message}", e)
            null
        }
    }
    
    /**
     * Busca odds (probabilidades de apostas) ao vivo
     */
    suspend fun getLiveOdds(matchId: Long): ApiSportsOdds? {
        return try {
            Log.d(TAG, "💰 Buscando odds ao vivo para matchId: $matchId")
            val response = api.getLiveOdds(matchId, API_KEY)
            
            if (response.response != null && response.response!!.isNotEmpty()) {
                val odds = response.response!![0]
                Log.d(TAG, "✅ Odds ao vivo encontradas: ${odds.bookmakers?.size ?: 0} casas de aposta")
                odds
            } else {
                Log.w(TAG, "⚠️ Nenhuma odd ao vivo encontrada")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar odds ao vivo: ${e.message}", e)
            null
        }
    }
}


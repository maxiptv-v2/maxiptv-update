package com.maxiptv.data.soccer

import android.util.Log
import com.maxiptv.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
// Tipos PlayerInfo, TeamInfo, LeagueInfo estão em SoccerModelsExtended.kt (com id: Long?)

/**
 * Repository para estatísticas de futebol
 * Usa football-data.org (https://api.football-data.org/v4/)
 * Documentação: https://www.football-data.org/docs/v4/index.html
 * 
 * 🔒 Quando USE_CLOUDFLARE_PROXY = true:
 * - Todas as requisições vão para o Cloudflare Worker
 * - O Worker adiciona a API key automaticamente
 * - Rate limiting é controlado pelo Cloudflare
 * - API key não fica exposta no APK
 */
object SoccerRepository {
    // 🔒 URL base: usa Cloudflare se configurado, senão usa API direta
    private val BASE_URL = BuildConfig.SOCCER_API_BASE_URL
    private val USE_CLOUDFLARE = BuildConfig.USE_CLOUDFLARE_PROXY
    private const val TAG = "SoccerRepository"
    
    // 🔑 Token da API football-data.org
    // ⚠️ Quando USE_CLOUDFLARE = true, esta chave não é usada (fica no Cloudflare)
    // ⚠️ Quando USE_CLOUDFLARE = false, esta chave é usada diretamente (menos seguro)
    private const val API_TOKEN = "c855e3746e41487284e34a756fc17b94"
    
    /**
     * Retorna o API token para usar nas requisições
     * Quando usar Cloudflare, retorna string vazia (o Worker adiciona automaticamente)
     * Quando não usar Cloudflare, retorna o API token real
     */
    private fun getApiTokenForRequest(): String {
        return if (USE_CLOUDFLARE) {
            "" // Cloudflare Worker adiciona automaticamente
        } else {
            API_TOKEN
        }
    }
    
    // ✅ Migrado para FootballDataApi
    private val api: FootballDataApi by lazy {
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
                val requestBuilder = chain.request().newBuilder()
                    .header("User-Agent", "MaxiPTV/1.1.1 (Android)")
                    .header("Accept", "application/json")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                    // Não adicionar Accept-Encoding manualmente - OkHttp descomprime gzip automaticamente
                
                // 🔒 Se NÃO estiver usando Cloudflare, adicionar API token diretamente
                // Se estiver usando Cloudflare, o Worker adiciona o API token automaticamente
                if (!USE_CLOUDFLARE) {
                    requestBuilder.header("X-Auth-Token", API_TOKEN)
                }
                
                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(FootballDataApi::class.java)
    }
    
    /**
     * Busca detalhes completos de uma partida específica
     * Retorna: score, statistics, events, lineups, formation, status
     */
    suspend fun getMatchDetail(matchId: Long): MatchDetailFull? {
        return try {
            Log.d(TAG, "🔍 Buscando detalhes da partida $matchId na API football-data.org...")
            
            // Buscar match da API football-data.org
            val match = api.getMatch(matchId, getApiTokenForRequest())
            if (match.id == null) {
                Log.e(TAG, "❌ Match não encontrada: $matchId")
                return null
            }
            
            // Converter FootballDataMatchDetail para MatchDetailFull
            val matchDetail = convertFootballDataToMatchDetailFull(match)
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
            Log.d(TAG, "🔍 Buscando partidas ao vivo na API football-data.org...")
            val response = api.getLiveMatches(getApiTokenForRequest(), "LIVE")
            val matches = response.matches ?: emptyList()
            
            val allMatches = matches.mapNotNull { match ->
                convertFootballDataToMatchSummaryFull(match)
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
     * NOTA: A API football-data.org não fornece preview/predictions, retorna null
     */
    suspend fun getMatchPreview(matchId: Long): MatchPreviewFull? {
        Log.w(TAG, "⚠️ Preview não disponível na API football-data.org para matchId: $matchId")
        return null
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
            
            val normalizedHome = normalizeTeamName(homeTeam)
            val normalizedAway = normalizeTeamName(awayTeam)
            
            Log.d(TAG, "   Nomes normalizados: '$normalizedHome' x '$normalizedAway'")
            
            // ESTRATÉGIA 1: Buscar em partidas ao vivo E recentes/finalizadas
            val allMatches = mutableListOf<FootballDataMatch>()
            
            // 1.1: Buscar partidas ao vivo
            try {
                val liveResponse = api.getLiveMatches(getApiTokenForRequest(), "LIVE")
                val liveMatches = liveResponse.matches ?: emptyList()
                allMatches.addAll(liveMatches)
                Log.d(TAG, "   ✅ ${liveMatches.size} partidas ao vivo encontradas")
            } catch (e: Exception) {
                Log.e(TAG, "   ❌ Erro ao buscar partidas ao vivo: ${e.message}", e)
                e.printStackTrace()
            }
            
            // 1.2: Buscar partidas de hoje
            try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = dateFormat.format(java.util.Date())
                
                Log.d(TAG, "   📅 Buscando partidas de hoje: $today")
                val dateResponse = api.getMatchesByDate(getApiTokenForRequest(), today)
                val todayMatches = dateResponse.matches ?: emptyList()
                
                // Incluir partidas ao vivo, agendadas (SCHEDULED) e finalizadas (FINISHED) de hoje
                val relevantTodayMatches = todayMatches.filter { 
                    val status = it.status?.uppercase() ?: ""
                    // Ao vivo
                    status == "LIVE" || status == "IN_PLAY" || status == "PAUSED" ||
                    // Agendadas (ainda não começaram)
                    status == "SCHEDULED" || status == "TIMED" ||
                    // Finalizadas recentemente (podem estar sendo exibidas)
                    status == "FINISHED"
                }
                allMatches.addAll(relevantTodayMatches)
                
                val liveCount = todayMatches.count { 
                    val status = it.status?.uppercase() ?: ""
                    status == "LIVE" || status == "IN_PLAY" || status == "PAUSED"
                }
                Log.d(TAG, "   ✅ ${relevantTodayMatches.size} partidas relevantes de hoje encontradas (${liveCount} ao vivo, de ${todayMatches.size} total)")
            } catch (e: Exception) {
                Log.e(TAG, "   ❌ Erro ao buscar partidas de hoje: ${e.message}", e)
                e.printStackTrace()
            }
            
            // Remover duplicatas
            val matches = allMatches.distinctBy { it.id }.toList()
            Log.d(TAG, "   ✅ Total: ${matches.size} partidas únicas para buscar correspondência")
            
            // ✅ MELHORIA: Função auxiliar para verificar correspondência de nomes (fora do loop)
            fun namesMatch(name1: String, name2: String): Boolean {
                if (name1.isEmpty() || name2.isEmpty()) return false
                // Correspondência exata
                if (name1 == name2) return true
                // Um contém o outro (para lidar com "EC Vitória" vs "Vitória")
                if (name1.contains(name2) || name2.contains(name1)) return true
                // Verificar palavras-chave principais (primeira palavra de cada nome)
                val words1 = name1.split(" ").filter { it.length >= 3 }
                val words2 = name2.split(" ").filter { it.length >= 3 }
                if (words1.isNotEmpty() && words2.isNotEmpty()) {
                    // Se a primeira palavra de um está no outro, considerar match
                    if (words1[0] in words2 || words2[0] in words1) {
                        // Verificar se pelo menos 1 palavra coincide
                        val commonWords = words1.intersect(words2.toSet())
                        if (commonWords.size >= 1) return true
                    }
                }
                return false
            }
            
            // Buscar correspondência exata pelos nomes dos times
            for (match in matches) {
                val matchHome = match.homeTeam?.name ?: ""
                val matchAway = match.awayTeam?.name ?: ""
                
                val normalizedMatchHome = normalizeTeamName(matchHome)
                val normalizedMatchAway = normalizeTeamName(matchAway)
                
                // ✅ MELHORIA: Verificar correspondência (direta ou invertida) com mais flexibilidade
                // Usar contains para ser mais flexível (ex: "Sao Paulo FC" vs "Sao Paulo", "EC Vitória" vs "Vitória")
                val directMatch = namesMatch(normalizedMatchHome, normalizedHome) &&
                                 namesMatch(normalizedMatchAway, normalizedAway)
                
                val invertedMatch = namesMatch(normalizedMatchHome, normalizedAway) &&
                                   namesMatch(normalizedMatchAway, normalizedHome)
                
                if (directMatch || invertedMatch) {
                    val status = match.status?.uppercase() ?: ""
                    val isCancelled = status.contains("CANCELED") || status.contains("POSTPONED") || status.contains("SUSPENDED")
                    
                    // ✅ ACEITAR TODAS as partidas que correspondem aos times, mesmo que já tenham terminado
                    // Isso permite mostrar estatísticas de jogos que já passaram mas estão sendo transmitidos
                    if (!isCancelled) {
                        val matchId = match.id ?: 0L
                        Log.d(TAG, "✅ Partida encontrada pelos nomes dos times: $matchHome x $matchAway (ID: $matchId, Status: ${match.status})")
                        return matchId
                    } else {
                        Log.d(TAG, "   ⚠️ Partida encontrada mas foi cancelada: ${match.status}")
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
     * Busca Match ID usando o título do EPG (mais preciso)
     * Estratégia: EPG → Detectar competição → Extrair times → Buscar IDs → Buscar match
     * 
     * @param epgTitle Título do programa do EPG (ex: "Flamengo x Palmeiras", "Champions League: Real Madrid x Barcelona")
     * @return Match ID encontrado ou null
     */
    suspend fun findMatchIdFromEpgTitle(epgTitle: String): Long? {
        return try {
            Log.d(TAG, "🔍 Buscando Match ID do EPG: '$epgTitle'")
            
            // 1. Detectar competição do EPG (se houver)
            val competitionCode = detectCompetitionFromEpg(epgTitle)
            if (competitionCode != null) {
                Log.d(TAG, "   🏆 Competição detectada: $competitionCode")
            }
            
            // 2. Extrair nomes dos times do título do EPG
            val teamNames = extractTeamsFromEpgTitle(epgTitle)
            if (teamNames == null) {
                Log.w(TAG, "⚠️ Não foi possível extrair times do EPG: '$epgTitle'")
                return null
            }
            
            val (team1Name, team2Name) = teamNames
            Log.d(TAG, "   Times extraídos: '$team1Name' x '$team2Name'")
            
            // 3. Buscar IDs dos times na API
            val team1Id = searchTeamId(team1Name)
            val team2Id = searchTeamId(team2Name)
            
            if (team1Id == null || team2Id == null) {
                Log.w(TAG, "⚠️ Não foi possível encontrar IDs dos times")
                if (team1Id == null) Log.w(TAG, "   Time 1 não encontrado: '$team1Name'")
                if (team2Id == null) Log.w(TAG, "   Time 2 não encontrado: '$team2Name'")
                return null
            }
            
            Log.d(TAG, "   IDs encontrados: $team1Id x $team2Id")
            
            // 4. Buscar match - priorizar competição se detectada
            if (competitionCode != null) {
                // 4.1. Buscar na competição específica (ao vivo primeiro)
                val liveMatchId = findMatchByTeamIdsInCompetition(team1Id, team2Id, competitionCode, liveOnly = true)
                if (liveMatchId != null) {
                    Log.d(TAG, "✅ Match ID encontrado (competição $competitionCode, ao vivo): $liveMatchId")
                    return liveMatchId
                }
                
                // 4.2. Buscar na competição (hoje)
                val todayMatchId = findMatchByTeamIdsInCompetition(team1Id, team2Id, competitionCode, liveOnly = false)
                if (todayMatchId != null) {
                    Log.d(TAG, "✅ Match ID encontrado (competição $competitionCode, hoje): $todayMatchId")
                    return todayMatchId
                }
            }
            
            // 5. Buscar match geral (ao vivo primeiro)
            val liveMatchId = findMatchByTeamIds(team1Id, team2Id, liveOnly = true)
            if (liveMatchId != null) {
                Log.d(TAG, "✅ Match ID encontrado (ao vivo): $liveMatchId")
                return liveMatchId
            }
            
            // 6. Se não encontrar ao vivo, buscar de hoje
            val todayMatchId = findMatchByTeamIds(team1Id, team2Id, liveOnly = false)
            if (todayMatchId != null) {
                Log.d(TAG, "✅ Match ID encontrado (hoje): $todayMatchId")
                return todayMatchId
            }
            
            Log.w(TAG, "⚠️ Match não encontrado para: $team1Name x $team2Name")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar Match ID do EPG: '$epgTitle'", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Detecta código de competição do título do EPG
     * Retorna código da competição (ex: "CL", "BSA", "CDB") ou null
     */
    private fun detectCompetitionFromEpg(epgTitle: String): String? {
        val titleLower = epgTitle.lowercase()
        
        // Mapeamento de termos para códigos de competição
        val competitionMap = mapOf(
            // Champions League
            "champions league" to "CL",
            "champions" to "CL",
            "uefa champions" to "CL",
            
            // Brasileirão
            "brasileirão" to "BSA",
            "brasileirao" to "BSA",
            "brasileirão série a" to "BSA",
            "brasileirao serie a" to "BSA",
            "campeonato brasileiro" to "BSA",
            
            // Copa do Brasil
            "copa do brasil" to "CDB",
            "copa brasil" to "CDB",
            
            // Premier League
            "premier league" to "PL",
            "premier" to "PL",
            
            // La Liga
            "la liga" to "PD",
            "liga espanhola" to "PD",
            
            // Serie A
            "serie a" to "SA",
            "serie a italiana" to "SA",
            
            // Bundesliga
            "bundesliga" to "BL1",
            
            // Ligue 1
            "ligue 1" to "FL1",
            "liga francesa" to "FL1"
        )
        
        for ((term, code) in competitionMap) {
            if (titleLower.contains(term)) {
                return code
            }
        }
        
        return null
    }
    
    /**
     * Extrai nomes dos times do título do EPG
     */
    private fun extractTeamsFromEpgTitle(epgTitle: String): Pair<String, String>? {
        val separators = listOf(" x ", " vs ", " VS ", " - ", " X ")
        for (sep in separators) {
            if (sep in epgTitle) {
                val parts = epgTitle.split(sep, limit = 2)
                if (parts.size == 2) {
                    val team1 = parts[0].trim()
                    val team2 = parts[1].trim()
                    if (team1.isNotEmpty() && team2.isNotEmpty()) {
                        return Pair(team1, team2)
                    }
                }
            }
        }
        return null
    }
    
    /**
     * Busca o ID de um time na API football-data.org
     * ESTRATÉGIA: Como a API não tem endpoint de busca por nome, buscamos times através das partidas disponíveis
     * 1. Buscar partidas ao vivo e de hoje
     * 2. Extrair times dessas partidas
     * 3. Comparar nomes
     */
    private suspend fun searchTeamId(teamName: String): Long? {
        return try {
            val normalizedName = normalizeTeamName(teamName)
            Log.d(TAG, "   🔍 Buscando time: '$teamName' (normalizado: '$normalizedName')")
            
            // Buscar partidas ao vivo e de hoje para extrair times
            val allMatches = mutableListOf<FootballDataMatch>()
            
            // 1. Buscar partidas ao vivo
            try {
                val liveResponse = api.getLiveMatches(getApiTokenForRequest(), "LIVE")
                allMatches.addAll(liveResponse.matches ?: emptyList())
            } catch (e: Exception) {
                Log.w(TAG, "   ⚠️ Erro ao buscar partidas ao vivo: ${e.message}")
            }
            
            // 2. Buscar partidas de hoje
            try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = dateFormat.format(java.util.Date())
                val todayResponse = api.getMatchesByDate(getApiTokenForRequest(), today)
                allMatches.addAll(todayResponse.matches ?: emptyList())
            } catch (e: Exception) {
                Log.w(TAG, "   ⚠️ Erro ao buscar partidas de hoje: ${e.message}")
            }
            
            // 3. Buscar em competições brasileiras conhecidas (Brasileirão)
            try {
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = dateFormat.format(java.util.Date())
                val brasileiraoResponse = api.getCompetitionMatches("BSA", getApiTokenForRequest(), null, today, today, null)
                val brasileiraoMatches = brasileiraoResponse.matches?.filter { match ->
                    // Filtrar apenas partidas de hoje ou ao vivo
                    val matchDate = match.utcDate?.substringBefore("T")
                    val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date())
                    matchDate == todayDate || match.status == "LIVE" || match.status == "IN_PLAY"
                } ?: emptyList()
                allMatches.addAll(brasileiraoMatches)
            } catch (e: Exception) {
                Log.w(TAG, "   ⚠️ Erro ao buscar partidas do Brasileirão: ${e.message}")
            }
            
            // 4. Extrair times únicos e comparar nomes
            val allTeams = mutableSetOf<FootballDataTeam>()
            allMatches.forEach { match ->
                match.homeTeam?.let { allTeams.add(it) }
                match.awayTeam?.let { allTeams.add(it) }
            }
            
            // 5. Buscar melhor match
            val bestMatch = allTeams.firstOrNull { team ->
                val teamNameLower = normalizeTeamName(team.name ?: "")
                val shortNameLower = normalizeTeamName(team.shortName ?: "")
                
                teamNameLower.contains(normalizedName) || 
                normalizedName.contains(teamNameLower) ||
                shortNameLower.contains(normalizedName) ||
                normalizedName.contains(shortNameLower) ||
                teamNameLower == normalizedName ||
                shortNameLower == normalizedName
            }
            
            if (bestMatch != null && bestMatch.id != null) {
                Log.d(TAG, "   ✅ Time encontrado: '$teamName' → ID: ${bestMatch.id} (${bestMatch.name})")
                return bestMatch.id
            }
            
            Log.w(TAG, "   ⚠️ Nenhum time encontrado para: '$teamName' (verificados ${allTeams.size} times)")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar time '$teamName': ${e.message}", e)
            null
        }
    }
    
    /**
     * Normaliza nome do time para comparação
     */
    private fun normalizeTeamName(name: String): String {
        return name.lowercase()
            .replace("ã", "a").replace("á", "a").replace("à", "a").replace("â", "a")
            .replace("é", "e").replace("ê", "e")
            .replace("í", "i")
            .replace("ó", "o").replace("ô", "o").replace("õ", "o")
            .replace("ú", "u").replace("ü", "u")
            .replace("ç", "c")
            .replace(Regex("^(ec|fc|sc|se|aa|ad|ae|af|ag|ah|ai|aj|ak|al|am|an|ao|ap|aq|ar|as|at|au|av|aw|ax|ay|az|ba|bb|bc|bd|be|bf|bg|bh|bi|bj|bk|bl|bm|bn|bo|bp|bq|br|bs|bt|bu|bv|bw|bx|by|bz|sp|rj|mg|rs|pr|sc|go|pe|ce|df|es|pb|rn|al|pi|ma|ms|mt|ac|ap|ro|rr|to|am|pa)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(ec|fc|sc|se|ba|sp|rj|mg|rs|pr|sc|go|pe|ce|df|es|pb|rn|al|pi|ma|ms|mt|ac|ap|ro|rr|to|am|pa)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * Busca match entre dois times usando seus IDs (busca geral)
     */
    private suspend fun findMatchByTeamIds(team1Id: Long, team2Id: Long, liveOnly: Boolean): Long? {
        return try {
            val matches = if (liveOnly) {
                // Buscar apenas ao vivo
                val liveResponse = api.getLiveMatches(getApiTokenForRequest(), "LIVE")
                liveResponse.matches ?: emptyList()
            } else {
                // Buscar jogos de hoje
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = dateFormat.format(java.util.Date())
                val dateResponse = api.getMatchesByDate(getApiTokenForRequest(), today)
                dateResponse.matches ?: emptyList()
            }
            
            for (match in matches) {
                val homeId = match.homeTeam?.id
                val awayId = match.awayTeam?.id
                
                if ((homeId == team1Id && awayId == team2Id) || 
                    (homeId == team2Id && awayId == team1Id)) {
                    return match.id
                }
            }
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao buscar match por IDs: ${e.message}")
            null
        }
    }
    
    /**
     * Busca match entre dois times em uma competição específica
     */
    private suspend fun findMatchByTeamIdsInCompetition(
        team1Id: Long, 
        team2Id: Long, 
        competitionCode: String,
        liveOnly: Boolean
    ): Long? {
        return try {
            val matches = if (liveOnly) {
                // Buscar apenas ao vivo na competição
                val response = api.getCompetitionMatches(
                    competitionCode,
                    getApiTokenForRequest(),
                    "LIVE",
                    null,
                    null,
                    null
                )
                response.matches ?: emptyList()
            } else {
                // Buscar jogos de hoje na competição
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val today = dateFormat.format(java.util.Date())
                val response = api.getCompetitionMatches(
                    competitionCode,
                    getApiTokenForRequest(),
                    null,
                    today,
                    today,
                    null
                )
                response.matches ?: emptyList()
            }
            
            for (match in matches) {
                val homeId = match.homeTeam?.id
                val awayId = match.awayTeam?.id
                
                if ((homeId == team1Id && awayId == team2Id) || 
                    (homeId == team2Id && awayId == team1Id)) {
                    return match.id
                }
            }
            
            null
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao buscar match por IDs na competição $competitionCode: ${e.message}")
            null
        }
    }
    
    /**
     * Busca automaticamente o Match ID para um canal de futebol
     * Estratégia inteligente:
     * 1. Tenta extrair Match ID do nome do canal
     * 2. Tenta usar EPG se disponível
     * 3. Tenta extrair nomes dos times e buscar
     * 4. Busca todas as partidas ao vivo e tenta identificar a mais relevante
     * 5. Prioriza partidas brasileiras, partidas ao vivo de verdade, etc
     * 
     * @param channelName Nome do canal (ex: "Amazon Prime 4 HD (Eventos)", "Premiere 1")
     * @param epgTitle Título do programa atual do EPG (opcional) - mais preciso
     * @return Match ID encontrado ou null
     */
    suspend fun findMatchForChannel(channelName: String, epgTitle: String? = null): Long? {
        return try {
            // ✅ CORREÇÃO CRASH: Validar entrada
            if (channelName.isBlank()) {
                Log.w(TAG, "⚠️ Nome do canal vazio - retornando null")
                return null
            }
            
            Log.d(TAG, "🔍 Buscando Match ID para canal: '$channelName'")
            if (epgTitle != null) {
                Log.d(TAG, "   EPG disponível: '$epgTitle'")
            }
            
            // ESTRATÉGIA 1: Usar EPG se disponível (mais preciso)
            if (epgTitle != null && epgTitle.isNotBlank()) {
                val epgMatchId = findMatchIdFromEpgTitle(epgTitle)
                if (epgMatchId != null) {
                    Log.d(TAG, "✅ Match ID encontrado via EPG: $epgMatchId")
                    return epgMatchId
                }
            }
            
            // ESTRATÉGIA 2: Tentar extrair Match ID diretamente do nome do canal
            val directMatchId = MatchIdExtractor.extractMatchId(channelName)
            if (directMatchId != null) {
                Log.d(TAG, "✅ Match ID extraído diretamente do nome: $directMatchId")
                return directMatchId
            }
            
            // ESTRATÉGIA 3: Tentar extrair nomes dos times do nome do canal e buscar
            val teamNames = MatchIdExtractor.extractTeamNames(channelName)
            if (teamNames != null) {
                Log.d(TAG, "🔍 Times extraídos do canal: '${teamNames.first}' x '${teamNames.second}'")
                Log.d(TAG, "   Buscando partida correspondente na API...")
                val matchIdByTeams = findMatchByTeamNames(teamNames.first, teamNames.second)
                if (matchIdByTeams != null) {
                    Log.d(TAG, "✅ Match ID encontrado pelo nome dos times: $matchIdByTeams")
                    return matchIdByTeams
                } else {
                    Log.w(TAG, "⚠️ Partida não encontrada para: '${teamNames.first}' x '${teamNames.second}'")
                }
            } else {
                Log.d(TAG, "⚠️ Não foi possível extrair nomes dos times do canal: '$channelName'")
            }
            
            // ESTRATÉGIA 3: Se não encontrou, retornar null (as estratégias 1 e 2 já cobrem os casos principais)
            Log.d(TAG, "⚠️ Nenhuma estratégia anterior encontrou Match ID - retornando null")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar Match ID para canal '$channelName'", e)
            e.printStackTrace()
            null
        }
    }
    /**
     * Busca odds (probabilidades de apostas) pré-jogo
     * NOTA: A API football-data.org não fornece odds no plano gratuito, retorna null
     */
    suspend fun getOdds(matchId: Long): Any? {
        Log.w(TAG, "⚠️ Odds não disponíveis na API football-data.org para matchId: $matchId")
        return null
    }
    
    /**
     * Alias para getOdds (compatibilidade)
     */
    suspend fun getMatchOdds(matchId: Long): Any? {
        return getOdds(matchId)
    }
    
    /**
     * Busca odds (probabilidades de apostas) ao vivo
     * NOTA: A API football-data.org não fornece odds no plano gratuito, retorna null
     */
    suspend fun getLiveOdds(matchId: Long): Any? {
        Log.w(TAG, "⚠️ Odds ao vivo não disponíveis na API football-data.org para matchId: $matchId")
        return null
    }
    
    /**
     * Converte FootballDataMatchDetail para MatchDetailFull
     */
    private fun convertFootballDataToMatchDetailFull(match: FootballDataMatchDetail): MatchDetailFull {
        val homeTeam = match.homeTeam
        val awayTeam = match.awayTeam
        val score = match.score
        
        return MatchDetailFull(
            id = match.id,
            name = "${homeTeam?.name ?: ""} x ${awayTeam?.name ?: ""}",
            league = LeagueInfo(
                id = match.competition?.id?.toLong(),
                name = match.competition?.name
            ),
            home = TeamInfo(
                id = homeTeam?.id,
                name = homeTeam?.name
            ),
            away = TeamInfo(
                id = awayTeam?.id,
                name = awayTeam?.name
            ),
            date = match.utcDate?.substringBefore("T"),
            time = match.utcDate?.substringAfter("T")?.substringBefore("Z"),
            status = MatchStatus(
                long = match.status,
                short = match.status,
                elapsed = null
            ),
            score = MatchScore(
                home = score?.fullTime?.home,
                away = score?.fullTime?.away,
                current = MatchScoreDetail(
                    home = score?.fullTime?.home,
                    away = score?.fullTime?.away
                ),
                halftime = score?.halfTime?.let {
                    MatchScoreDetail(
                        home = it.home,
                        away = it.away
                    )
                },
                fulltime = score?.fullTime?.let {
                    MatchScoreDetail(
                        home = it.home,
                        away = it.away
                    )
                }
            ),
            statistics = emptyList(), // A API football-data.org não fornece estatísticas no plano gratuito
            events = emptyList(), // A API football-data.org não fornece eventos no plano gratuito
            lineups = null, // A API football-data.org não fornece lineups no plano gratuito
            formation = null,
            match_preview = null
        )
    }
    
    /**
     * Converte FootballDataMatch para MatchSummaryFull
     */
    private fun convertFootballDataToMatchSummaryFull(match: FootballDataMatch): MatchSummaryFull? {
        if (match.id == null) return null
        
        val homeTeam = match.homeTeam
        val awayTeam = match.awayTeam
        val score = match.score
        
        val matchId = match.id
        if (matchId == null) return null
        return MatchSummaryFull(
            id = matchId,
            name = "${homeTeam?.name ?: ""} x ${awayTeam?.name ?: ""}",
            league = LeagueInfo(
                id = match.competition?.id?.toLong(),
                name = match.competition?.name
            ),
            home = TeamInfo(
                id = homeTeam?.id,
                name = homeTeam?.name
            ),
            away = TeamInfo(
                id = awayTeam?.id,
                name = awayTeam?.name
            ),
            starting_at = match.utcDate,
            score = MatchScore(
                home = score?.fullTime?.home,
                away = score?.fullTime?.away
            ),
            status = MatchStatus(
                long = match.status,
                short = match.status,
                elapsed = null
            )
        )
    }
}


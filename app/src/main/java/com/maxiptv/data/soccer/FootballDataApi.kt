package com.maxiptv.data.soccer

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API Service para football-data.org
 * Base URL: https://api.football-data.org/v4/
 * Documentação: https://www.football-data.org/docs/v4/index.html
 * 
 * IMPORTANTE: Esta API requer o header "X-Auth-Token" com o token
 */
interface FootballDataApi {
    
    /**
     * GET /matches/{id}
     * Retorna detalhes de uma partida específica
     */
    @GET("matches/{id}")
    suspend fun getMatch(
        @Path("id") matchId: Long,
        @Header("X-Auth-Token") apiToken: String
    ): FootballDataMatchDetail
    
    /**
     * GET /matches?status=LIVE
     * Retorna lista de partidas ao vivo
     */
    @GET("matches")
    suspend fun getLiveMatches(
        @Header("X-Auth-Token") apiToken: String,
        @Query("status") status: String = "LIVE"
    ): FootballDataMatchesResponse
    
    /**
     * GET /matches?dateFrom={date}&dateTo={date}
     * Retorna partidas entre duas datas
     */
    @GET("matches")
    suspend fun getMatchesByDateRange(
        @Header("X-Auth-Token") apiToken: String,
        @Query("dateFrom") dateFrom: String,
        @Query("dateTo") dateTo: String
    ): FootballDataMatchesResponse
    
    /**
     * GET /matches?date={date}
     * Retorna partidas de uma data específica (formato: YYYY-MM-DD)
     */
    @GET("matches")
    suspend fun getMatchesByDate(
        @Header("X-Auth-Token") apiToken: String,
        @Query("date") date: String
    ): FootballDataMatchesResponse
    
    /**
     * GET /teams
     * Lista times (sem filtro de nome - a API não suporta busca direta por nome)
     * NOTA: A API football-data.org não tem endpoint de busca por nome.
     * Usaremos uma estratégia alternativa: buscar times através de competições ou usar IDs conhecidos.
     */
    @GET("teams")
    suspend fun getTeams(
        @Header("X-Auth-Token") apiToken: String,
        @Query("limit") limit: Int? = null
    ): FootballDataTeamsResponse
    
    /**
     * GET /teams/{id}
     * Retorna detalhes de um time
     */
    @GET("teams/{id}")
    suspend fun getTeam(
        @Path("id") teamId: Long,
        @Header("X-Auth-Token") apiToken: String
    ): FootballDataTeam
    
    /**
     * GET /teams/{id}/matches
     * Retorna partidas de um time
     */
    @GET("teams/{id}/matches")
    suspend fun getTeamMatches(
        @Path("id") teamId: Long,
        @Header("X-Auth-Token") apiToken: String,
        @Query("status") status: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null
    ): FootballDataMatchesResponse
    
    /**
     * GET /competitions/{code}/matches
     * Retorna partidas de uma competição específica
     * Exemplos: CL (Champions League), BSA (Brasileirão), PL (Premier League), etc
     */
    @GET("competitions/{code}/matches")
    suspend fun getCompetitionMatches(
        @Path("code") competitionCode: String,
        @Header("X-Auth-Token") apiToken: String,
        @Query("status") status: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("matchday") matchday: Int? = null
    ): FootballDataMatchesResponse
    
    /**
     * GET /competitions
     * Lista todas as competições disponíveis
     */
    @GET("competitions")
    suspend fun getCompetitions(
        @Header("X-Auth-Token") apiToken: String
    ): FootballDataCompetitionsResponse
}

package com.maxiptv.data.soccer

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API Service para estatísticas de futebol
 * Base URL: https://v3.football.api-sports.io/
 * Documentação: https://www.api-sports.io/documentation/football/v3
 * 
 * IMPORTANTE: Esta API requer o header "x-apisports-key" com a chave de API
 */
interface SoccerApi {
    
    /**
     * GET /fixtures?id={id}
     * Retorna informações completas da partida: score, statistics, events, lineups, formation, status
     */
    @GET("fixtures")
    suspend fun getFixture(
        @Query("id") fixtureId: Long,
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<ApiSportsFixture>
    
    /**
     * GET /fixtures?live=all
     * Retorna lista de partidas ao vivo
     */
    @GET("fixtures")
    suspend fun getLiveFixtures(
        @Query("live") live: String = "all",
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<ApiSportsFixture>
    
    /**
     * GET /fixtures/statistics?fixture={id}
     * Retorna estatísticas da partida
     */
    @GET("fixtures/statistics")
    suspend fun getFixtureStatistics(
        @Query("fixture") fixtureId: Long,
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<ApiSportsStatistic>
    
    /**
     * GET /predictions?fixture={id}
     * Retorna preview com predições
     */
    @GET("predictions")
    suspend fun getPredictions(
        @Query("fixture") fixtureId: Long,
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<ApiSportsPrediction>
    
    /**
     * GET /fixtures?team={team_id}&next={n}
     * Busca próximas partidas de um time
     */
    @GET("fixtures")
    suspend fun getTeamFixtures(
        @Query("team") teamId: Int,
        @Query("next") next: Int = 1,
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<ApiSportsFixture>
    
    /**
     * GET /teams?search={name}
     * Busca times por nome
     */
    @GET("teams")
    suspend fun searchTeams(
        @Query("search") name: String,
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<TeamSearchResult>
    
    /**
     * GET /fixtures/events?fixture={id}
     * Retorna eventos da partida (gols, cartões, substituições)
     */
    @GET("fixtures/events")
    suspend fun getFixtureEvents(
        @Query("fixture") fixtureId: Long,
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<ApiSportsEvent>
    
    /**
     * GET /fixtures/lineups?fixture={id}
     * Retorna escalações da partida
     */
    @GET("fixtures/lineups")
    suspend fun getFixtureLineups(
        @Query("fixture") fixtureId: Long,
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<ApiSportsLineup>
    
    /**
     * GET /odds?fixture={id}
     * Retorna probabilidades de apostas (odds) pré-jogo de várias casas de aposta
     */
    @GET("odds")
    suspend fun getOdds(
        @Query("fixture") fixtureId: Long,
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<ApiSportsOdds>
    
    /**
     * GET /odds/live?fixture={id}
     * Retorna probabilidades de apostas (odds) ao vivo (atualizadas durante a partida)
     */
    @GET("odds/live")
    suspend fun getLiveOdds(
        @Query("fixture") fixtureId: Long,
        @Header("x-apisports-key") apiKey: String
    ): ApiSportsResponse<ApiSportsOdds>
}


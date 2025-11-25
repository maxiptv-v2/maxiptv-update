package com.maxiptv.data.soccer

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API Service para estatísticas de futebol
 * Base URL: https://api.soccerdataapi.com/
 * Documentação: https://soccerdataapi.com/docs/
 * 
 * IMPORTANTE: Esta API requer o header "Accept-Encoding: gzip" em todas as requisições
 */
interface SoccerApi {
    
    /**
     * GET /match/?match_id={id}&auth_token={token}
     * Retorna informações completas da partida: score, statistics, events, lineups, formation, status
     */
    @GET("match/")
    suspend fun getMatch(
        @Query("match_id") matchId: Long,
        @Query("auth_token") authToken: String
    ): MatchDetailFull
    
    /**
     * GET /livescores/?auth_token={token}
     * Retorna lista de partidas ao vivo
     */
    @GET("livescores/")
    suspend fun getLiveScores(
        @Query("auth_token") authToken: String
    ): LiveScoresResponseFull
    
    /**
     * GET /match-preview/?match_id={id}&auth_token={token}
     * Retorna preview com predições, clima, conteúdo textual
     */
    @GET("match-preview/")
    suspend fun getMatchPreview(
        @Query("match_id") matchId: Long,
        @Query("auth_token") authToken: String
    ): MatchPreviewFull
}


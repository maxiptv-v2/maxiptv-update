package com.maxiptv.data.soccer

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API Service para estatísticas de futebol
 * Base URL: https://api.sportmonks.com/v3/football/
 */
interface SoccerApi {
    
    @GET("matches/{match_id}")
    suspend fun getMatch(
        @Path("match_id") matchId: Long,
        @Query("api_token") apiToken: String = "836475b96827b5eb935418deeb0ce2377dae6669"
    ): MatchDetailResponse
    
    @GET("livescores")
    suspend fun getLiveScores(
        @Query("api_token") apiToken: String = "836475b96827b5eb935418deeb0ce2377dae6669"
    ): LiveScoresResponse
    
    @GET("match-previews/{match_id}")
    suspend fun getMatchPreview(
        @Path("match_id") matchId: Long,
        @Query("api_token") apiToken: String = "836475b96827b5eb935418deeb0ce2377dae6669"
    ): MatchPreviewResponse
    
    @GET("match-previews/upcoming")
    suspend fun getUpcomingPreviews(
        @Query("league_id") leagueId: String,
        @Query("api_token") apiToken: String = "836475b96827b5eb935418deeb0ce2377dae6669"
    ): UpcomingPreviewsResponse
    
    @GET("leagues/{league_id}")
    suspend fun getLeague(
        @Path("league_id") leagueId: String,
        @Query("api_token") apiToken: String = "836475b96827b5eb935418deeb0ce2377dae6669"
    ): LeagueDetailResponse
    
    @GET("seasons/{season_id}")
    suspend fun getSeason(
        @Path("season_id") seasonId: String,
        @Query("api_token") apiToken: String = "836475b96827b5eb935418deeb0ce2377dae6669"
    ): SeasonDetailResponse
    
    @GET("teams/{team_id}")
    suspend fun getTeam(
        @Path("team_id") teamId: String,
        @Query("api_token") apiToken: String = "836475b96827b5eb935418deeb0ce2377dae6669"
    ): TeamDetailResponse
    
    @GET("players/{player_id}")
    suspend fun getPlayer(
        @Path("player_id") playerId: String,
        @Query("api_token") apiToken: String = "836475b96827b5eb935418deeb0ce2377dae6669"
    ): PlayerDetailResponse
}


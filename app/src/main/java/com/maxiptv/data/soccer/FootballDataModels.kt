package com.maxiptv.data.soccer

import kotlinx.serialization.Serializable

/**
 * Modelos de dados para football-data.org API
 * Base URL: https://api.football-data.org/v4/
 * Documentação: https://www.football-data.org/docs/v4/index.html
 * 
 * Autenticação: Header X-Auth-Token
 */

// ============================================================================
// RESPONSE WRAPPER
// ============================================================================

@Serializable
data class FootballDataMatchesResponse(
    val filters: FiltersInfo? = null,
    val resultSet: ResultSetInfo? = null,
    val matches: List<FootballDataMatch>? = null
)

@Serializable
data class FiltersInfo(
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val permission: String? = null,
    val status: List<String>? = null
)

@Serializable
data class ResultSetInfo(
    val count: Int? = null,
    val competitions: String? = null,
    val first: String? = null,
    val last: String? = null,
    val played: Int? = null
)

// ============================================================================
// MATCH (PARTIDA)
// ============================================================================

@Serializable
data class FootballDataMatch(
    val id: Long? = null,
    val utcDate: String? = null,
    val status: String? = null, // "SCHEDULED", "LIVE", "IN_PLAY", "PAUSED", "FINISHED", "POSTPONED", "SUSPENDED", "CANCELED"
    val matchday: Int? = null,
    val stage: String? = null,
    val group: String? = null,
    val lastUpdated: String? = null,
    val homeTeam: FootballDataTeam? = null,
    val awayTeam: FootballDataTeam? = null,
    val score: FootballDataScore? = null,
    val odds: FootballDataOdds? = null,
    val referees: List<FootballDataReferee>? = null,
    val competition: FootballDataCompetition? = null,
    val season: FootballDataSeason? = null,
    val area: FootballDataArea? = null
)

@Serializable
data class FootballDataTeam(
    val id: Long? = null,
    val name: String? = null,
    val shortName: String? = null,
    val tla: String? = null,
    val crest: String? = null
)

@Serializable
data class FootballDataScore(
    val winner: String? = null, // "HOME_TEAM", "AWAY_TEAM", "DRAW"
    val duration: String? = null, // "REGULAR", "EXTRA_TIME", "PENALTY_SHOOTOUT"
    val fullTime: FootballDataScoreDetail? = null,
    val halfTime: FootballDataScoreDetail? = null,
    val regularTime: FootballDataScoreDetail? = null,
    val extraTime: FootballDataScoreDetail? = null,
    val penalties: FootballDataScoreDetail? = null
)

@Serializable
data class FootballDataScoreDetail(
    val home: Int? = null,
    val away: Int? = null
)

@Serializable
data class FootballDataOdds(
    val msg: String? = null
)

@Serializable
data class FootballDataReferee(
    val id: Long? = null,
    val name: String? = null,
    val type: String? = null,
    val nationality: String? = null
)

@Serializable
data class FootballDataCompetition(
    val id: Int? = null,
    val name: String? = null,
    val code: String? = null,
    val type: String? = null,
    val emblem: String? = null
)

@Serializable
data class FootballDataSeason(
    val id: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val currentMatchday: Int? = null,
    val winner: FootballDataTeam? = null
)

@Serializable
data class FootballDataArea(
    val id: Int? = null,
    val name: String? = null,
    val code: String? = null,
    val flag: String? = null
)

// ============================================================================
// TEAM RESPONSE
// ============================================================================

@Serializable
data class FootballDataTeamsResponse(
    val count: Int? = null,
    val filters: Map<String, String>? = null,
    val teams: List<FootballDataTeam>? = null
)

// ============================================================================
// MATCH DETAIL (detalhes completos de uma partida)
// ============================================================================

@Serializable
data class FootballDataMatchDetail(
    val id: Long? = null,
    val utcDate: String? = null,
    val status: String? = null,
    val matchday: Int? = null,
    val stage: String? = null,
    val group: String? = null,
    val lastUpdated: String? = null,
    val homeTeam: FootballDataTeam? = null,
    val awayTeam: FootballDataTeam? = null,
    val score: FootballDataScore? = null,
    val odds: FootballDataOdds? = null,
    val referees: List<FootballDataReferee>? = null,
    val competition: FootballDataCompetition? = null,
    val season: FootballDataSeason? = null,
    val area: FootballDataArea? = null
)

// ============================================================================
// COMPETITIONS RESPONSE
// ============================================================================

@Serializable
data class FootballDataCompetitionsResponse(
    val count: Int? = null,
    val filters: Map<String, String>? = null,
    val competitions: List<FootballDataCompetition>? = null
)


package com.maxiptv.data.soccer

import kotlinx.serialization.Serializable

/**
 * Modelos de dados para API de futebol
 * Adaptados para kotlinx.serialization (padrão do projeto)
 */

@Serializable
data class MatchDetailResponse(
    val data: MatchDetail?
)

@Serializable
data class MatchDetail(
    val id: Long,
    val name: String? = null,
    val homeTeam: TeamInfo? = null,
    val awayTeam: TeamInfo? = null,
    val statistics: List<MatchStatistic>? = null,
    val probabilities: Probabilities? = null,
    val events: List<MatchEvent>? = null
) {
    // Propriedades calculadas para facilitar acesso
    val homeTeamName: String get() = homeTeam?.name ?: ""
    val awayTeamName: String get() = awayTeam?.name ?: ""
    val homeTeamId: String get() = homeTeam?.id?.toString() ?: ""
    val awayTeamId: String get() = awayTeam?.id?.toString() ?: ""
    
    // Estatísticas calculadas
    val possessionHome: Int get() = statistics?.find { it.type == "possession" }?.homeValue ?: 0
    val possessionAway: Int get() = statistics?.find { it.type == "possession" }?.awayValue ?: 0
    val shotsHome: Int get() = statistics?.find { it.type == "shots_on_target" }?.homeValue ?: 0
    val shotsAway: Int get() = statistics?.find { it.type == "shots_on_target" }?.awayValue ?: 0
    val cornersHome: Int get() = statistics?.find { it.type == "corners" }?.homeValue ?: 0
    val cornersAway: Int get() = statistics?.find { it.type == "corners" }?.awayValue ?: 0
    val xGHome: Double get() = statistics?.find { it.type == "expected_goals" }?.homeValue?.toDouble() ?: 0.0
    val xGAway: Double get() = statistics?.find { it.type == "expected_goals" }?.awayValue?.toDouble() ?: 0.0
    
    val currentEvent: MatchEvent? get() = events?.lastOrNull()
}

@Serializable
data class TeamInfo(
    val id: Long? = null,
    val name: String? = null
)

@Serializable
data class MatchStatistic(
    val type: String? = null,
    val homeValue: Int? = null,
    val awayValue: Int? = null
)

@Serializable
data class Probabilities(
    val homeWin: Double? = null,
    val draw: Double? = null,
    val awayWin: Double? = null
) {
    val homeWinPercent: Double get() = (homeWin ?: 0.0) * 100
    val drawPercent: Double get() = (draw ?: 0.0) * 100
    val awayWinPercent: Double get() = (awayWin ?: 0.0) * 100
}

@Serializable
data class MatchEvent(
    val id: Long? = null,
    val type: String? = null, // "GOAL", "PENALTY", "CORNER", "YELLOW_CARD", "RED_CARD"
    val playerId: Long? = null,
    val teamId: Long? = null,
    val minute: Int? = null,
    val player: PlayerInfo? = null
) {
    val playerIdString: String get() = playerId?.toString() ?: ""
    val teamIdString: String get() = teamId?.toString() ?: ""
}

@Serializable
data class PlayerInfo(
    val id: Long? = null,
    val name: String? = null
)

@Serializable
data class LiveScoresResponse(
    val data: List<MatchSummary>? = null
)

@Serializable
data class MatchSummary(
    val id: Long,
    val name: String? = null,
    val homeTeam: TeamInfo? = null,
    val awayTeam: TeamInfo? = null,
    val league: LeagueInfo? = null,
    val startingAt: String? = null
) {
    val matchId: Long get() = id
    val homeTeamName: String get() = homeTeam?.name ?: ""
    val awayTeamName: String get() = awayTeam?.name ?: ""
    val leagueName: String get() = league?.name ?: ""
    val startTime: String get() = startingAt ?: ""
}

@Serializable
data class LeagueInfo(
    val id: Long? = null,
    val name: String? = null
)

@Serializable
data class MatchPreviewResponse(
    val data: MatchPreview?
)

@Serializable
data class MatchPreview(
    val id: Long,
    val homeWinProbability: Double? = null,
    val drawProbability: Double? = null,
    val awayWinProbability: Double? = null,
    val suggestedBets: List<String>? = null
) {
    val matchId: Long get() = id
    val homeWinPercent: Double get() = (homeWinProbability ?: 0.0) * 100
    val drawPercent: Double get() = (drawProbability ?: 0.0) * 100
    val awayWinPercent: Double get() = (awayWinProbability ?: 0.0) * 100
}

@Serializable
data class UpcomingPreviewsResponse(
    val data: List<MatchPreview>? = null
)

@Serializable
data class TeamDetailResponse(
    val data: TeamDetail?
)

@Serializable
data class TeamDetail(
    val id: Long,
    val name: String? = null,
    val recentForm: String? = null,
    val wins: Int? = null,
    val draws: Int? = null,
    val losses: Int? = null
) {
    val teamId: String get() = id.toString()
}

@Serializable
data class PlayerDetailResponse(
    val data: PlayerDetail?
)

@Serializable
data class PlayerDetail(
    val id: Long,
    val name: String? = null,
    val goals: Int? = null,
    val assists: Int? = null,
    val yellowCards: Int? = null,
    val redCards: Int? = null
) {
    val playerId: String get() = id.toString()
}

@Serializable
data class LeagueDetailResponse(
    val data: LeagueDetail?
)

@Serializable
data class LeagueDetail(
    val id: Long,
    val name: String? = null
) {
    val leagueId: String get() = id.toString()
}

@Serializable
data class SeasonDetailResponse(
    val data: SeasonDetail?
)

@Serializable
data class SeasonDetail(
    val id: Long,
    val name: String? = null
) {
    val seasonId: String get() = id.toString()
}


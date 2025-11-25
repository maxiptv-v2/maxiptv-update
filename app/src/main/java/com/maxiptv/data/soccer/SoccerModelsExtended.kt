package com.maxiptv.data.soccer

import kotlinx.serialization.Serializable

/**
 * Modelos de dados ESTENDIDOS para Soccer Data API
 * Baseado na documentação: https://soccerdataapi.com/docs/
 * 
 * O endpoint /match/ retorna uma estrutura completa com:
 * - score (placar atual, intervalo, final)
 * - statistics (array de estatísticas detalhadas)
 * - events (gols, cartões, substituições, etc)
 * - lineups (escalações completas)
 * - formation (formações dos times)
 * - status (status da partida)
 */

// ============================================================================
// RESPONSE PRINCIPAL DO ENDPOINT /match/
// ============================================================================

@Serializable
data class MatchDetailFull(
    val id: Long? = null,
    val name: String? = null,
    val league: LeagueInfo? = null,
    val home: TeamInfo? = null,
    val away: TeamInfo? = null,
    val date: String? = null,
    val time: String? = null,
    val status: MatchStatus? = null,
    val score: MatchScore? = null,
    val statistics: List<MatchStatistic>? = null,
    val events: List<MatchEvent>? = null,
    val lineups: MatchLineups? = null,
    val formation: MatchFormation? = null,
    val match_preview: MatchPreviewInfo? = null
) {
    val homeTeamName: String get() = home?.name ?: ""
    val awayTeamName: String get() = away?.name ?: ""
    val leagueName: String get() = league?.name ?: ""
    
    // Estatísticas calculadas (buscando pelos tipos corretos e removendo "%")
    val possessionHome: Int get() = statistics?.find { it.type?.lowercase()?.contains("possession") == true }?.home?.replace("%", "")?.toIntOrNull() ?: 0
    val possessionAway: Int get() = statistics?.find { it.type?.lowercase()?.contains("possession") == true }?.away?.replace("%", "")?.toIntOrNull() ?: 0
    
    val shotsHome: Int get() = statistics?.find { it.type?.lowercase()?.contains("shots") == true || it.type?.lowercase()?.contains("on_target") == true }?.home?.replace("%", "")?.toIntOrNull() ?: 0
    val shotsAway: Int get() = statistics?.find { it.type?.lowercase()?.contains("shots") == true || it.type?.lowercase()?.contains("on_target") == true }?.away?.replace("%", "")?.toIntOrNull() ?: 0
    
    val cornersHome: Int get() = statistics?.find { it.type?.lowercase()?.contains("corner") == true }?.home?.replace("%", "")?.toIntOrNull() ?: 0
    val cornersAway: Int get() = statistics?.find { it.type?.lowercase()?.contains("corner") == true }?.away?.replace("%", "")?.toIntOrNull() ?: 0
    
    val foulsHome: Int get() = statistics?.find { it.type?.lowercase()?.contains("foul") == true }?.home?.replace("%", "")?.toIntOrNull() ?: 0
    val foulsAway: Int get() = statistics?.find { it.type?.lowercase()?.contains("foul") == true }?.away?.replace("%", "")?.toIntOrNull() ?: 0
    
    val yellowCardsHome: Int get() = statistics?.find { it.type?.lowercase()?.contains("yellow") == true }?.home?.replace("%", "")?.toIntOrNull() ?: 0
    val yellowCardsAway: Int get() = statistics?.find { it.type?.lowercase()?.contains("yellow") == true }?.away?.replace("%", "")?.toIntOrNull() ?: 0
    
    val redCardsHome: Int get() = statistics?.find { it.type?.lowercase()?.contains("red") == true }?.home?.replace("%", "")?.toIntOrNull() ?: 0
    val redCardsAway: Int get() = statistics?.find { it.type?.lowercase()?.contains("red") == true }?.away?.replace("%", "")?.toIntOrNull() ?: 0
    
    val offsidesHome: Int get() = statistics?.find { it.type?.lowercase()?.contains("offside") == true }?.home?.replace("%", "")?.toIntOrNull() ?: 0
    val offsidesAway: Int get() = statistics?.find { it.type?.lowercase()?.contains("offside") == true }?.away?.replace("%", "")?.toIntOrNull() ?: 0
}

// ============================================================================
// PLACAR (SCORE)
// ============================================================================

@Serializable
data class MatchScore(
    val home: Int? = null,
    val away: Int? = null,
    val current: MatchScoreDetail? = null,
    val halftime: MatchScoreDetail? = null,
    val fulltime: MatchScoreDetail? = null
)

@Serializable
data class MatchScoreDetail(
    val home: Int? = null,
    val away: Int? = null
)

// ============================================================================
// STATUS DA PARTIDA
// ============================================================================

@Serializable
data class MatchStatus(
    val long: String? = null,  // "Not Started", "1st Half", "Half Time", "2nd Half", "Finished"
    val short: String? = null,  // "NS", "1H", "HT", "2H", "FT"
    val elapsed: Int? = null    // Minutos decorridos
)

// ============================================================================
// ESTATÍSTICAS (ATUALIZADA PARA FORMATO DA API)
// ============================================================================

@Serializable
data class MatchStatistic(
    val type: String? = null,  // "Possession", "Shots on Target", "Corners", etc
    val home: String? = null,  // Valor para time da casa (pode ser String ou Int)
    val away: String? = null   // Valor para time visitante (pode ser String ou Int)
)

// ============================================================================
// EVENTOS (GOALS, CARDS, SUBSTITUTIONS)
// ============================================================================

@Serializable
data class MatchEvent(
    val id: Long? = null,
    val type: String? = null,      // "goal", "card", "substitution"
    val detail: String? = null,    // "Normal Goal", "Yellow Card", etc
    val comments: String? = null,  // Informações adicionais
    val time: MatchEventTime? = null,
    val team: TeamInfo? = null,
    val player: PlayerInfo? = null,
    val assist: PlayerInfo? = null
)

@Serializable
data class MatchEventTime(
    val elapsed: Int? = null,      // Minuto do evento
    val extra: Int? = null         // Tempo adicional
)

// ============================================================================
// ESCALAÇÕES (LINEUPS)
// ============================================================================

@Serializable
data class MatchLineups(
    val home: TeamLineup? = null,
    val away: TeamLineup? = null
)

@Serializable
data class TeamLineup(
    val team: TeamInfo? = null,
    val starting_xi: List<PlayerLineup>? = null,
    val substitutes: List<PlayerLineup>? = null,
    val coach: CoachInfo? = null
)

@Serializable
data class PlayerLineup(
    val id: Long? = null,
    val name: String? = null,
    val number: Int? = null,
    val pos: String? = null,      // Position: "G", "D", "M", "F"
    val grid: String? = null      // Posição na grade (ex: "1:1")
)

@Serializable
data class CoachInfo(
    val id: Long? = null,
    val name: String? = null
)

// ============================================================================
// FORMAÇÕES
// ============================================================================

@Serializable
data class MatchFormation(
    val home: String? = null,  // Ex: "4-3-3"
    val away: String? = null   // Ex: "4-4-2"
)

// ============================================================================
// INFORMAÇÕES DE PREVIEW
// ============================================================================

@Serializable
data class MatchPreviewInfo(
    val has_previews: Boolean? = null,
    val word_count: Int? = null
)

// ============================================================================
// MATCH PREVIEW COMPLETO (endpoint /match-preview/)
// ============================================================================

@Serializable
data class MatchPreviewFull(
    val match_id: Long? = null,
    val league: LeagueInfo? = null,
    val home: TeamInfo? = null,
    val away: TeamInfo? = null,
    val word_count: Int? = null,
    val date: String? = null,
    val time: String? = null,
    val match_data: MatchPreviewData? = null,
    val content: List<PreviewContent>? = null
)

@Serializable
data class MatchPreviewData(
    val weather: WeatherInfo? = null,
    val excitement_rating: Double? = null,
    val prediction: MatchPrediction? = null
)

@Serializable
data class WeatherInfo(
    val temp_f: Double? = null,
    val temp_c: Double? = null,
    val description: String? = null
)

@Serializable
data class MatchPrediction(
    val type: String? = null,      // "match_winner", "over_under", etc
    val choice: String? = null     // "Home Win", "Over 2.5", etc
)

@Serializable
data class PreviewContent(
    val name: String? = null,      // "p1", "h1", "p2", etc
    val content: String? = null    // Texto do conteúdo
)

// ============================================================================
// RESPONSE DE LIVESCORES
// ============================================================================

@Serializable
data class LiveScoresResponseFull(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<MatchSummaryFull>? = null
)

@Serializable
data class MatchSummaryFull(
    val id: Long,
    val name: String? = null,
    val league: LeagueInfo? = null,
    val home: TeamInfo? = null,
    val away: TeamInfo? = null,
    val starting_at: String? = null,
    val score: MatchScore? = null,
    val status: MatchStatus? = null
) {
    val matchId: Long get() = id
    val homeTeamName: String get() = home?.name ?: ""
    val awayTeamName: String get() = away?.name ?: ""
    val leagueName: String get() = league?.name ?: ""
    val startTime: String get() = starting_at ?: ""
}


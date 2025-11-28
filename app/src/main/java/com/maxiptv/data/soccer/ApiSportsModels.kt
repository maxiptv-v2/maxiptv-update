package com.maxiptv.data.soccer

import kotlinx.serialization.Serializable

/**
 * Modelos de dados para API Sports (api-sports.io)
 * Base URL: https://v3.football.api-sports.io/
 * Documentação: https://www.api-sports.io/documentation/football/v3
 */

// ============================================================================
// RESPONSE WRAPPER (todas as respostas da API Sports têm esta estrutura)
// ============================================================================

@Serializable
data class ApiSportsResponse<T>(
    val get: String? = null,
    val parameters: Map<String, String>? = null,
    val errors: List<String>? = null,
    val results: Int? = null,
    val paging: PagingInfo? = null,
    val response: List<T>? = null
)

@Serializable
data class PagingInfo(
    val current: Int? = null,
    val total: Int? = null
)

// ============================================================================
// FIXTURE (PARTIDA) - Endpoint /fixtures
// ============================================================================

@Serializable
data class ApiSportsFixture(
    val fixture: FixtureInfo? = null,
    val league: ApiSportsLeague? = null,
    val teams: ApiSportsTeams? = null,
    val goals: ApiSportsGoals? = null,
    val score: ApiSportsScore? = null,
    val events: List<ApiSportsEvent>? = null,
    val lineups: List<ApiSportsLineup>? = null,
    val statistics: List<ApiSportsStatistic>? = null,
    val players: List<ApiSportsPlayerStats>? = null
)

@Serializable
data class FixtureInfo(
    val id: Long? = null,
    val referee: String? = null,
    val timezone: String? = null,
    val date: String? = null,
    val timestamp: Long? = null,
    val periods: PeriodsInfo? = null,
    val venue: VenueInfo? = null,
    val status: FixtureStatus? = null
)

@Serializable
data class PeriodsInfo(
    val first: Long? = null,
    val second: Long? = null
)

@Serializable
data class VenueInfo(
    val id: Int? = null,
    val name: String? = null,
    val city: String? = null
)

@Serializable
data class FixtureStatus(
    val long: String? = null,  // "Not Started", "First Half", "Halftime", "Second Half", "Finished"
    val short: String? = null, // "NS", "1H", "HT", "2H", "FT"
    val elapsed: Int? = null   // Minutos decorridos
)

@Serializable
data class ApiSportsLeague(
    val id: Int? = null,
    val name: String? = null,
    val country: String? = null,
    val logo: String? = null,
    val flag: String? = null,
    val season: Int? = null,
    val round: String? = null
)

@Serializable
data class ApiSportsTeams(
    val home: ApiSportsTeam? = null,
    val away: ApiSportsTeam? = null
)

@Serializable
data class ApiSportsTeam(
    val id: Int? = null,
    val name: String? = null,
    val logo: String? = null,
    val winner: Boolean? = null,
    val code: String? = null,
    val country: String? = null,
    val founded: Int? = null,
    val national: Boolean? = null
)

// Wrapper para resposta de busca de times (inclui team e venue)
@Serializable
data class TeamSearchResult(
    val team: ApiSportsTeam? = null,
    val venue: VenueInfo? = null
)

@Serializable
data class ApiSportsGoals(
    val home: Int? = null,
    val away: Int? = null
)

@Serializable
data class ApiSportsScore(
    val halftime: ApiSportsGoals? = null,
    val fulltime: ApiSportsGoals? = null,
    val extratime: ApiSportsGoals? = null,
    val penalty: ApiSportsGoals? = null
)

// ============================================================================
// EVENTS (EVENTOS DA PARTIDA)
// ============================================================================

@Serializable
data class ApiSportsEvent(
    val time: TimeInfo? = null,
    val team: ApiSportsTeam? = null,
    val player: ApiSportsPlayerInfo? = null,
    val assist: ApiSportsPlayerInfo? = null,
    val type: String? = null,      // "Goal", "Card", "subst"
    val detail: String? = null,    // "Normal Goal", "Yellow Card", etc
    val comments: String? = null
)

@Serializable
data class TimeInfo(
    val elapsed: Int? = null,
    val extra: Int? = null
)

@Serializable
data class ApiSportsPlayerInfo(
    val id: Int? = null,
    val name: String? = null
)

// ============================================================================
// STATISTICS (ESTATÍSTICAS)
// ============================================================================

@Serializable
data class ApiSportsStatistic(
    val team: ApiSportsTeam? = null,
    val statistics: List<StatisticItem>? = null
)

@Serializable
data class StatisticItem(
    val type: String? = null,  // "Shots on Goal", "Shots off Goal", "Total Shots", "Blocked Shots", "Shots insidebox", "Shots outsidebox", "Fouls", "Corner Kicks", "Offsides", "Ball Possession", "Yellow Cards", "Red Cards", "Goalkeeper Saves", "Total passes", "Passes accurate", "Passes %"
    val value: String? = null     // Pode ser Int ou String (ex: "65%") - API retorna como String ou Number, mas serializamos como String
)

// ============================================================================
// LINEUPS (ESCALAÇÕES)
// ============================================================================

@Serializable
data class ApiSportsLineup(
    val team: ApiSportsTeam? = null,
    val coach: CoachInfo? = null,
    val formation: String? = null,
    val startXI: List<LineupPlayer>? = null,
    val substitutes: List<LineupPlayer>? = null
)

@Serializable
data class LineupPlayer(
    val player: ApiSportsPlayerInfo? = null
)

// ============================================================================
// PLAYER STATS (ESTATÍSTICAS DOS JOGADORES)
// ============================================================================

@Serializable
data class ApiSportsPlayerStats(
    val team: ApiSportsTeam? = null,
    val players: List<PlayerStatsDetail>? = null
)

@Serializable
data class PlayerStatsDetail(
    val player: ApiSportsPlayerInfo? = null,
    val statistics: List<PlayerStatisticItem>? = null
)

@Serializable
data class PlayerStatisticItem(
    val games: GameStats? = null,
    val offsides: Int? = null,
    val shots: ShotStats? = null,
    val goals: GoalStats? = null,
    val passes: PassStats? = null,
    val tackles: TackleStats? = null,
    val duels: DuelStats? = null,
    val dribbles: DribbleStats? = null,
    val fouls: FoulStats? = null,
    val cards: CardStats? = null,
    val penalty: PenaltyStats? = null
)

@Serializable
data class GameStats(
    val minutes: Int? = null,
    val number: Int? = null,
    val position: String? = null,
    val rating: String? = null,
    val captain: Boolean? = null,
    val substitute: Boolean? = null
)

@Serializable
data class ShotStats(
    val total: Int? = null,
    val on: Int? = null
)

@Serializable
data class GoalStats(
    val total: Int? = null,
    val conceded: Int? = null,
    val assists: Int? = null,
    val saves: Int? = null
)

@Serializable
data class PassStats(
    val total: Int? = null,
    val key: Int? = null,
    val accuracy: String? = null
)

@Serializable
data class TackleStats(
    val total: Int? = null,
    val blocks: Int? = null,
    val interceptions: Int? = null
)

@Serializable
data class DuelStats(
    val total: Int? = null,
    val won: Int? = null
)

@Serializable
data class DribbleStats(
    val attempts: Int? = null,
    val success: Int? = null,
    val past: Int? = null
)

@Serializable
data class FoulStats(
    val drawn: Int? = null,
    val committed: Int? = null
)

@Serializable
data class CardStats(
    val yellow: Int? = null,
    val red: Int? = null
)

@Serializable
data class PenaltyStats(
    val won: Int? = null,
    val commited: Int? = null,
    val scored: Int? = null,
    val missed: Int? = null,
    val saved: Int? = null
)

// ============================================================================
// PREDICTIONS (PREVISÕES)
// ============================================================================

@Serializable
data class ApiSportsPrediction(
    val predictions: PredictionData? = null,
    val league: ApiSportsLeague? = null,
    val teams: ApiSportsTeams? = null,
    val comparison: ComparisonData? = null,
    val h2h: List<ApiSportsFixture>? = null
)

@Serializable
data class PredictionData(
    val winner: WinnerPrediction? = null,
    val win_or_draw: Boolean? = null,
    val under_over: String? = null,
    val goals: GoalsPrediction? = null,
    val advice: String? = null,
    val percent: PercentPrediction? = null
)

@Serializable
data class WinnerPrediction(
    val id: Int? = null,
    val name: String? = null,
    val comment: String? = null
)

@Serializable
data class GoalsPrediction(
    val home: String? = null,
    val away: String? = null
)

@Serializable
data class PercentPrediction(
    val home: String? = null,
    val draw: String? = null,
    val away: String? = null
)

@Serializable
data class ComparisonData(
    val form: FormComparison? = null,
    val att: AttComparison? = null,
    val def: DefComparison? = null,
    val fish_law: FishLawComparison? = null,
    val h2h: H2HComparison? = null,
    val goals: GoalsComparison? = null,
    val total: TotalComparison? = null
)

@Serializable
data class FormComparison(
    val home: String? = null,
    val away: String? = null
)

@Serializable
data class AttComparison(
    val home: String? = null,
    val away: String? = null
)

@Serializable
data class DefComparison(
    val home: String? = null,
    val away: String? = null
)

@Serializable
data class FishLawComparison(
    val home: String? = null,
    val away: String? = null
)

@Serializable
data class H2HComparison(
    val home: String? = null,
    val away: String? = null
)

@Serializable
data class GoalsComparison(
    val home: String? = null,
    val away: String? = null
)

@Serializable
data class TotalComparison(
    val home: String? = null,
    val away: String? = null
)

// ============================================================================
// ODDS (PROBABILIDADES DE APOSTAS)
// ============================================================================

@Serializable
data class ApiSportsOdds(
    val league: ApiSportsLeague? = null,
    val fixture: FixtureInfo? = null,
    val update: String? = null,
    val bookmakers: List<Bookmaker>? = null
)

@Serializable
data class Bookmaker(
    val id: Int? = null,
    val name: String? = null,
    val bets: List<Bet>? = null
)

@Serializable
data class Bet(
    val id: Int? = null,
    val name: String? = null,  // "Match Winner", "Over/Under", "Asian Handicap", etc
    val values: List<BetValue>? = null
)

@Serializable
data class BetValue(
    val value: String? = null,  // "Home", "Draw", "Away", "Over 2.5", etc
    val odd: String? = null     // "2.26", "3.30", etc
)


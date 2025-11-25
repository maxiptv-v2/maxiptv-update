package com.maxiptv.data.soccer

import kotlinx.serialization.Serializable

/**
 * ✅ MODELOS COMPARTILHADOS - Tipos auxiliares usados em múltiplos modelos
 * 
 * IMPORTANTE: Os modelos principais estão em SoccerModelsExtended.kt
 * Este arquivo mantém apenas tipos compartilhados para evitar duplicação
 */

// ============================================================================
// TIPOS COMPARTILHADOS (usados por SoccerModelsExtended.kt)
// ============================================================================

@Serializable
data class TeamInfo(
    val id: Long? = null,
    val name: String? = null
)

@Serializable
data class PlayerInfo(
    val id: Long? = null,
    val name: String? = null
)

@Serializable
data class LeagueInfo(
    val id: Long? = null,
    val name: String? = null
)

// ============================================================================
// MODELOS DE DETALHES (mantidos para compatibilidade se necessário)
// ============================================================================

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
data class LeagueDetail(
    val id: Long,
    val name: String? = null
) {
    val leagueId: String get() = id.toString()
}

@Serializable
data class SeasonDetail(
    val id: Long,
    val name: String? = null
) {
    val seasonId: String get() = id.toString()
}

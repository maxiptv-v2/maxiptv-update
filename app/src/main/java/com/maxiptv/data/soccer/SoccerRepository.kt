package com.maxiptv.data.soccer

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

/**
 * Repository para estatísticas de futebol
 * Usa API SportMonks (https://api.sportmonks.com/v3/football/)
 */
object SoccerRepository {
    private const val BASE_URL = "https://api.sportmonks.com/v3/football/"
    private const val TAG = "SoccerRepository"
    
    private val api: SoccerApi by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        
        val contentType = "application/json".toMediaType()
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MaxiPTV/1.1.1 (Android)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(SoccerApi::class.java)
    }
    
    /**
     * Busca detalhes de uma partida específica
     */
    suspend fun getMatchDetail(matchId: Long): MatchDetail? {
        return try {
            val response = api.getMatch(matchId)
            response.data
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar detalhes da partida $matchId", e)
            null
        }
    }
    
    /**
     * Busca placares ao vivo
     */
    suspend fun getOtherMatches(): List<MatchSummary> {
        return try {
            val response = api.getLiveScores()
            response.data ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar placares ao vivo", e)
            emptyList()
        }
    }
    
    /**
     * Busca preview de uma partida
     */
    suspend fun getMatchPreview(matchId: Long): MatchPreview? {
        return try {
            val response = api.getMatchPreview(matchId)
            response.data
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar preview da partida $matchId", e)
            null
        }
    }
    
    /**
     * Busca previews de partidas futuras de uma liga
     */
    suspend fun getUpcomingMatches(leagueId: String): List<MatchPreview> {
        return try {
            val response = api.getUpcomingPreviews(leagueId)
            response.data ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar partidas futuras da liga $leagueId", e)
            emptyList()
        }
    }
    
    /**
     * Busca detalhes de um time
     */
    suspend fun getTeam(teamId: String): TeamDetail? {
        return try {
            val response = api.getTeam(teamId)
            response.data
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar detalhes do time $teamId", e)
            null
        }
    }
    
    /**
     * Busca detalhes de um jogador
     */
    suspend fun getPlayer(playerId: String): PlayerDetail? {
        return try {
            val response = api.getPlayer(playerId)
            response.data
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar detalhes do jogador $playerId", e)
            null
        }
    }
}


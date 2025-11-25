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
 * Usa Soccer Data API (https://api.soccerdataapi.com/)
 * Documentação: https://soccerdataapi.com/docs/
 */
object SoccerRepository {
    private const val BASE_URL = "https://api.soccerdataapi.com/"
    private const val TAG = "SoccerRepository"
    
    // 🔑 Chave de API da Soccer Data API
    private const val API_AUTH_TOKEN = "836475b96827b5eb935418deeb0ce2377dae6669"
    
    private val api: SoccerApi by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
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
                    // ✅ OBRIGATÓRIO: Soccer Data API requer Accept-Encoding: gzip
                    .header("Accept-Encoding", "gzip")
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
     * Busca detalhes completos de uma partida específica
     * Retorna: score, statistics, events, lineups, formation, status
     */
    suspend fun getMatchDetail(matchId: Long): MatchDetailFull? {
        return try {
            Log.d(TAG, "🔍 Buscando detalhes da partida $matchId na Soccer Data API...")
            val response = api.getMatch(matchId, API_AUTH_TOKEN)
            Log.d(TAG, "✅ Dados recebidos: ${response.homeTeamName} x ${response.awayTeamName}")
            response
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar detalhes da partida $matchId", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Busca placares ao vivo
     * Retorna lista de partidas em andamento
     */
    suspend fun getOtherMatches(): List<MatchSummaryFull> {
        return try {
            Log.d(TAG, "🔍 Buscando partidas ao vivo...")
            val response = api.getLiveScores(API_AUTH_TOKEN)
            val matches = response.results ?: emptyList()
            Log.d(TAG, "✅ ${matches.size} partidas ao vivo encontradas")
            matches
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar placares ao vivo", e)
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Busca preview completo de uma partida
     * Retorna: weather, predictions, content textual, excitement_rating
     */
    suspend fun getMatchPreview(matchId: Long): MatchPreviewFull? {
        return try {
            Log.d(TAG, "🔍 Buscando preview da partida $matchId...")
            val response = api.getMatchPreview(matchId, API_AUTH_TOKEN)
            Log.d(TAG, "✅ Preview recebido (word_count: ${response.word_count})")
            response
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar preview da partida $matchId", e)
            e.printStackTrace()
            null
        }
    }
}


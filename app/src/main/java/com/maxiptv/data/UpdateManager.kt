package com.maxiptv.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Serializable
data class UpdateInfo(
    val version: String,
    val versionCode: Int,
    val buildNumber: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val minimumVersion: String,
    val forceUpdate: Boolean,
    val fileSize: String,
    val lastUpdated: String,
    val checksum: String
)

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/maxiptv-v2/maxiptv-update/main/update.json"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Verifica se há atualização disponível
     * @return UpdateInfo se houver atualização, null caso contrário
     */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // Adicionar timestamp na URL para evitar cache do Fire OS
            val urlWithTimestamp = "$UPDATE_JSON_URL?t=${System.currentTimeMillis()}"
            Log.d(TAG, "🔍 Verificando atualizações em: $urlWithTimestamp")
            
            val request = Request.Builder()
                .url(urlWithTimestamp)
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Pragma", "no-cache")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Erro ao buscar update.json: HTTP ${response.code}")
                return@withContext null
            }
            
            val jsonString = response.body?.string()
            if (jsonString.isNullOrBlank()) {
                Log.e(TAG, "❌ update.json vazio")
                return@withContext null
            }
            
            Log.d(TAG, "📥 update.json recebido: $jsonString")
            
            val updateInfo = json.decodeFromString<UpdateInfo>(jsonString)
            val currentVersionCode = getCurrentVersionCode(context)
            
            Log.d(TAG, "📊 Versão atual: $currentVersionCode")
            Log.d(TAG, "📊 Versão disponível: ${updateInfo.versionCode}")
            
            if (updateInfo.versionCode > currentVersionCode) {
                Log.i(TAG, "🆕 Nova versão disponível: ${updateInfo.version}")
                return@withContext updateInfo
            } else {
                Log.d(TAG, "✅ App está atualizado")
                return@withContext null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar atualização: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Obtém o versionCode atual do app
     */
    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter versionCode: ${e.message}")
            0
        }
    }
    
    /**
     * Obtém a versionName atual do app
     */
    fun getCurrentVersionName(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter versionName: ${e.message}")
            "1.0.0"
        }
    }
}


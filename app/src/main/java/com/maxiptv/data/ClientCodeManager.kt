package com.maxiptv.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Sistema de Códigos Simples para Download Automático
 * Gerencia códigos de 4 dígitos para cada cliente baixar o app
 */
@Serializable
data class SimpleClientCode(
    val usuario: String,
    val senha: String,
    val api: String,
    val apk: String,
    val expira_em: String,
    val ativo: Boolean = true,
    val usado: Boolean = false,
    val usado_em: Long? = null,
    val usado_device: String? = null
)

object ClientCodeManager {
    private val K_CLIENT_CODES = stringPreferencesKey("client_codes")
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    /**
     * Gerar código simples de 4 dígitos
     */
    fun generateSimpleCode(): String {
        return (1000..9999).random().toString()
    }
    
    /**
     * Criar código simples para usuário
     */
    suspend fun createSimpleCode(user: UserAccount): String {
        val code = generateSimpleCode()
        // URL do servidor PHP que valida e redireciona para o GitHub
        val apkUrl = "http://maipt12.unaux.com/download-v2.php?code=$code"
        
        val simpleCode = SimpleClientCode(
            usuario = user.username,
            senha = user.password,
            api = user.apiUrl,
            apk = apkUrl,
            expira_em = user.expiryDate,
            ativo = true,
            usado = false,
            usado_em = null,
            usado_device = null
        )
        
        android.util.Log.i("ClientCodeManager", "🔑 Gerando código: $code para ${user.username}")
        android.util.Log.d("ClientCodeManager", "   Usuario: ${user.username}")
        android.util.Log.d("ClientCodeManager", "   Senha: ${user.password}")
        android.util.Log.d("ClientCodeManager", "   API: ${user.apiUrl}")
        android.util.Log.d("ClientCodeManager", "   Expira: ${user.expiryDate}")
        android.util.Log.d("ClientCodeManager", "   APK URL: $apkUrl")
        
        // Salvar no JSONBin via SessionManager
        val saved = SessionManager.saveSimpleCode(code, simpleCode)
        
        if (saved) {
            android.util.Log.i("ClientCodeManager", "✅ Código simples $code gerado e salvo com sucesso para ${user.username}")
        } else {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao salvar código simples $code no JSONBin")
        }
        
        return code
    }
    
    /**
     * Marcar código simples como usado
     */
    suspend fun markSimpleCodeAsUsed(code: String, deviceName: String): Boolean {
        return try {
            val simpleCode = SessionManager.getSimpleCode(code)
            if (simpleCode == null) {
                android.util.Log.e("ClientCodeManager", "❌ Código simples não encontrado: $code")
                return false
            }
            
            val updatedCode = simpleCode.copy(
                usado = true,
                usado_em = System.currentTimeMillis(),
                usado_device = deviceName
            )
            
            val success = SessionManager.updateSimpleCode(code, updatedCode)
            if (success) {
                android.util.Log.i("ClientCodeManager", "✅ Código simples marcado como usado: $code em $deviceName")
            } else {
                android.util.Log.e("ClientCodeManager", "❌ Erro ao marcar código simples como usado: $code")
            }
            
            return success
            
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao marcar código simples como usado: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Validar código simples (para servidor PHP)
     */
    suspend fun validateSimpleCode(code: String): SimpleClientCode? {
        return try {
            val simpleCode = SessionManager.getSimpleCode(code)
            
            if (simpleCode == null) {
                android.util.Log.w("ClientCodeManager", "❌ Código simples não encontrado: $code")
                return null
            }
            
            // Verificar se código está ativo
            if (!simpleCode.ativo) {
                android.util.Log.w("ClientCodeManager", "❌ Código simples inativo: $code")
                return null
            }
            
            // Verificar se já foi usado
            if (simpleCode.usado) {
                android.util.Log.w("ClientCodeManager", "❌ Código simples já usado: $code")
                return null
            }
            
            // Verificar se conta do usuário não expirou
            if (isUserExpired(simpleCode.expira_em)) {
                android.util.Log.w("ClientCodeManager", "❌ Conta do usuário expirada: ${simpleCode.usuario}")
                return null
            }
            
            android.util.Log.i("ClientCodeManager", "✅ Código simples válido: $code")
            return simpleCode
            
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao validar código simples: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Verificar se usuário expirou
     */
    private fun isUserExpired(expiryDate: String): Boolean {
        return try {
            val parts = expiryDate.split("/")
            if (parts.size != 3) return true
            
            val day = parts[0].toInt()
            val month = parts[1].toInt() - 1 // Calendar months are 0-based
            val year = parts[2].toInt()
            
            val calendar = java.util.Calendar.getInstance()
            calendar.set(year, month, day, 23, 59, 59)
            
            val expiryTime = calendar.timeInMillis
            val currentTime = System.currentTimeMillis()
            
            return currentTime > expiryTime
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao verificar expiração: ${e.message}")
            return true
        }
    }
    
    /**
     * Obter todos os códigos simples (para admin)
     */
    suspend fun getAllSimpleCodes(): Map<String, SimpleClientCode> {
        return try {
            SessionManager.getAllSimpleCodes()
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao obter códigos simples: ${e.message}", e)
            emptyMap()
        }
    }
    
    /**
     * Remover código simples (para admin)
     */
    suspend fun removeSimpleCode(code: String): Boolean {
        return try {
            SessionManager.removeSimpleCode(code)
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao remover código simples: ${e.message}", e)
            false
        }
    }
}
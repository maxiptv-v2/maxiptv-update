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
 * Sistema de Códigos PHP para Download Automático
 * Gerencia códigos únicos para cada cliente baixar o app
 */
@Serializable
data class ClientCode(
    val userId: String,
    val username: String,
    val password: String,
    val apiUrl: String,
    val expiryDate: String,
    val createdAt: Long,
    val codeExpiresAt: Long, // 6 horas após criação
    val used: Boolean = false,
    val usedAt: Long? = null,
    val usedDevice: String? = null,
    val downloadsCount: Int = 0
)

object ClientCodeManager {
    private val K_CLIENT_CODES = stringPreferencesKey("client_codes")
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    /**
     * Gerar código único para cliente
     */
    fun generateClientCode(user: UserAccount): String {
        val timestamp = System.currentTimeMillis()
        val hash = "${user.id}_${timestamp}".hashCode().toString(36)
        return "MAXI_${hash.uppercase()}"
    }
    
    /**
     * Criar código para usuário
     */
    suspend fun createClientCode(user: UserAccount): String {
        val code = generateClientCode(user)
        val now = System.currentTimeMillis()
        val expiresAt = now + (6 * 60 * 60 * 1000) // 6 horas
        
        val clientCode = ClientCode(
            userId = user.id,
            username = user.username,
            password = user.password,
            apiUrl = user.apiUrl,
            expiryDate = user.expiryDate,
            createdAt = now,
            codeExpiresAt = expiresAt
        )
        
        // Salvar no JSONBin via SessionManager
        SessionManager.saveClientCode(code, clientCode)
        
        android.util.Log.i("ClientCodeManager", "✅ Código gerado: $code para ${user.username}")
        return code
    }
    
    /**
     * Validar código (para servidor PHP)
     */
    suspend fun validateClientCode(code: String): ClientCode? {
        return try {
            val clientCode = SessionManager.getClientCode(code)
            
            if (clientCode == null) {
                android.util.Log.w("ClientCodeManager", "❌ Código não encontrado: $code")
                return null
            }
            
            // Verificar se código expirou
            if (System.currentTimeMillis() > clientCode.codeExpiresAt) {
                android.util.Log.w("ClientCodeManager", "❌ Código expirado: $code")
                return null
            }
            
            // Verificar se já foi usado
            if (clientCode.used) {
                android.util.Log.w("ClientCodeManager", "❌ Código já usado: $code")
                return null
            }
            
            // Verificar se conta do usuário não expirou
            if (isUserExpired(clientCode.expiryDate)) {
                android.util.Log.w("ClientCodeManager", "❌ Conta do usuário expirada: ${clientCode.username}")
                return null
            }
            
            android.util.Log.i("ClientCodeManager", "✅ Código válido: $code")
            return clientCode
            
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao validar código: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Marcar código como usado
     */
    suspend fun markCodeAsUsed(code: String, deviceName: String): Boolean {
        return try {
            val clientCode = SessionManager.getClientCode(code)
            if (clientCode == null) {
                android.util.Log.e("ClientCodeManager", "❌ Código não encontrado para marcar como usado: $code")
                return false
            }
            
            val updatedCode = clientCode.copy(
                used = true,
                usedAt = System.currentTimeMillis(),
                usedDevice = deviceName,
                downloadsCount = clientCode.downloadsCount + 1
            )
            
            val success = SessionManager.updateClientCode(code, updatedCode)
            if (success) {
                android.util.Log.i("ClientCodeManager", "✅ Código marcado como usado: $code em $deviceName")
            } else {
                android.util.Log.e("ClientCodeManager", "❌ Erro ao marcar código como usado: $code")
            }
            
            return success
            
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao marcar código como usado: ${e.message}", e)
            return false
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
     * Obter todos os códigos (para admin)
     */
    suspend fun getAllClientCodes(): Map<String, ClientCode> {
        return try {
            SessionManager.getAllClientCodes()
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao obter códigos: ${e.message}", e)
            emptyMap()
        }
    }
    
    /**
     * Remover código (para admin)
     */
    suspend fun removeClientCode(code: String): Boolean {
        return try {
            SessionManager.removeClientCode(code)
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao remover código: ${e.message}", e)
            false
        }
    }
    
    /**
     * Verificar se código está próximo de expirar (1 hora)
     */
    fun isCodeExpiringSoon(code: ClientCode): Boolean {
        val oneHour = 60 * 60 * 1000 // 1 hora em millis
        val timeUntilExpiry = code.codeExpiresAt - System.currentTimeMillis()
        return timeUntilExpiry <= oneHour && timeUntilExpiry > 0
    }
    
    /**
     * Formatar tempo restante do código
     */
    fun getTimeRemaining(code: ClientCode): String {
        val timeUntilExpiry = code.codeExpiresAt - System.currentTimeMillis()
        
        if (timeUntilExpiry <= 0) {
            return "Expirado"
        }
        
        val hours = timeUntilExpiry / (1000 * 60 * 60)
        val minutes = (timeUntilExpiry % (1000 * 60 * 60)) / (1000 * 60)
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "Expirado"
        }
    }
}

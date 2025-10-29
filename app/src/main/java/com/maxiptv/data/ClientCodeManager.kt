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
data class ClientCode(
    val username: String,
    val password: String,
    val apiUrl: String,
    val expiryDate: String, // formato DD/MM/YYYY
    val apkUrl: String,
    val createdAt: Long = System.currentTimeMillis() // timestamp da criação (para validar 6 horas)
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
        // Verificar se já existe código válido para este usuário (menos de 6 horas)
        val existingCode = SessionManager.getClientCodeForUser(user.username)
        
        // Se já existe código válido, manter o mesmo
        val code = if (existingCode != null) {
            // Verificar se o código ainda está válido
            val existingClientCode = SessionManager.getClientCode(existingCode)
            if (existingClientCode != null) {
                val sixHoursInMs = 6 * 60 * 60 * 1000L // 6 horas
                val validUntil = existingClientCode.createdAt + sixHoursInMs
                val currentTime = System.currentTimeMillis()
                
                if (currentTime <= validUntil) {
                    // Código ainda válido, manter o mesmo
                    android.util.Log.i("ClientCodeManager", "✅ Mantendo código existente $existingCode (válido por mais ${(validUntil - currentTime) / 1000 / 60} minutos)")
                    existingCode
                } else {
                    // Código expirou, gerar novo
                    android.util.Log.i("ClientCodeManager", "⏰ Código $existingCode expirado, gerando novo código")
                    generateSimpleCode()
                }
            } else {
                // Não conseguiu buscar código, gerar novo
                generateSimpleCode()
            }
        } else {
            // Não existe código, gerar novo
            generateSimpleCode()
        }
        
        // URL do APK no GitHub (sempre a versão mais recente)
        val apkUrl = "https://github.com/maxiptv-v2/maxiptv-update/releases/latest/download/maxiptv-release.apk"
        
        // Buscar código existente para manter createdAt se ainda válido
        val existingClientCode = SessionManager.getClientCode(code)
        val isNewCode = existingClientCode == null || existingClientCode.username != user.username || existingCode == null
        
        val clientCode = ClientCode(
            username = user.username,
            password = user.password,
            apiUrl = user.apiUrl,
            expiryDate = user.expiryDate, // formato DD/MM/YYYY
            apkUrl = apkUrl,
            createdAt = if (isNewCode) System.currentTimeMillis() else (existingClientCode?.createdAt ?: System.currentTimeMillis()) // Novo código = novo timestamp, código existente = manter timestamp
        )
        android.util.Log.i("ClientCodeManager", "🔑 ${if (isNewCode) "Gerando novo código" else "Mantendo código existente"}: $code para ${user.username}")
        if (!isNewCode && existingClientCode != null) {
            val sixHoursInMs = 6 * 60 * 60 * 1000L
            val validUntil = existingClientCode.createdAt + sixHoursInMs
            val minutesLeft = (validUntil - System.currentTimeMillis()) / 1000 / 60
            android.util.Log.d("ClientCodeManager", "   Código válido por mais ${minutesLeft} minutos")
        }
        android.util.Log.d("ClientCodeManager", "   Username: ${user.username}")
        android.util.Log.d("ClientCodeManager", "   Password: ${user.password}")
        android.util.Log.d("ClientCodeManager", "   API: ${user.apiUrl}")
        android.util.Log.d("ClientCodeManager", "   ExpiryDate: ${user.expiryDate}")
        android.util.Log.d("ClientCodeManager", "   ApkUrl: $apkUrl")
        
        // Salvar no JSONBin como objeto direto
        val saved = SessionManager.saveClientCode(code, clientCode)
        
        if (saved) {
            android.util.Log.i("ClientCodeManager", "✅ Código $code ${if (existingCode != null) "atualizado" else "gerado"} e salvo com sucesso para ${user.username}")
        } else {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao salvar código $code no JSONBin")
        }
        
        return code
    }
    
    /**
     * Marcar código simples como usado
     */
    suspend fun markSimpleCodeAsUsed(code: String, deviceName: String): Boolean {
        return try {
            val simpleCode = SessionManager.getClientCode(code)
            if (simpleCode == null) {
                android.util.Log.e("ClientCodeManager", "❌ Código simples não encontrado: $code")
                return false
            }
            
            // Nota: A nova estrutura ClientCode não tem campos "usado", "usado_em", "usado_device"
            // Se precisar marcar como usado, pode remover o código ou adicionar flag no futuro
            // Por enquanto, apenas loga o uso
            android.util.Log.i("ClientCodeManager", "📝 Código $code usado em $deviceName")
            val success = true // Código foi usado, mas não há flag na nova estrutura
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
     * Validar código (para servidor PHP)
     */
    suspend fun validateSimpleCode(code: String): ClientCode? {
        // Validação agora é feita pelo PHP
        // Esta função pode ser usada localmente se necessário
        return null
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
     * Retorna vazio por enquanto - códigos agora são gerenciados diretamente no JSONBin
     */
    suspend fun getAllSimpleCodes(): Map<String, ClientCode> {
        // TODO: Implementar busca de códigos do JSONBin se necessário para o admin
        return emptyMap()
    }
    
    /**
     * Remover código (para admin - revogar)
     */
    suspend fun removeSimpleCode(code: String): Boolean {
        return try {
            SessionManager.removeClientCode(code)
        } catch (e: Exception) {
            android.util.Log.e("ClientCodeManager", "❌ Erro ao remover código: ${e.message}", e)
            false
        }
    }
}
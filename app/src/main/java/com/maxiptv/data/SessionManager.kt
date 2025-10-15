package com.maxiptv.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
data class ActiveSession(
    val username: String,
    val deviceId: String,
    val deviceName: String,
    val loginTime: Long,
    val lastHeartbeat: Long
)

@Serializable
data class GlobalUser(
    val id: String,
    val username: String,
    val password: String,
    val apiUrl: String,
    val expiryDate: String
)

@Serializable
data class SessionsDatabase(
    val sessions: MutableMap<String, ActiveSession> = mutableMapOf(),
    val users: MutableList<GlobalUser> = mutableListOf()
)

object SessionManager {
    private const val TAG = "SessionManager"
    
    // IMPORTANTE: Substitua estes valores após criar conta no JSONBin.io
    private const val JSONBIN_API_KEY = "\$2a\$10\$3pxLra119/KvUF12CkD0kuHvXq/BPF4.YyEuqe/sVcNBoSMtMz1Ae"
    private const val JSONBIN_BIN_ID = "68ec647643b1c97be964e96b"
    private const val JSONBIN_BASE_URL = "https://api.jsonbin.io/v3"
    
    private val client = OkHttpClient()
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private var heartbeatJob: Job? = null
    private const val HEARTBEAT_INTERVAL = 30000L // 30 segundos
    private const val SESSION_TIMEOUT = 120000L // 2 minutos sem heartbeat = logout automático
    
    /**
     * Tenta fazer login. Retorna Pair(sucesso, mensagem)
     */
    suspend fun tryLogin(username: String, deviceId: String, deviceName: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🔐 Tentando login: $username em $deviceName (ID: $deviceId)")
            
            // 1. Buscar sessões atuais
            val database = fetchSessions() ?: SessionsDatabase()
            Log.d(TAG, "📊 Total de sessões no banco: ${database.sessions.size}")
            
            // 2. Verificar se já existe sessão ativa para este usuário
            val existingSession = database.sessions[username]
            
            if (existingSession != null) {
                Log.d(TAG, "🔍 Sessão existente encontrada:")
                Log.d(TAG, "   - Device ID existente: ${existingSession.deviceId}")
                Log.d(TAG, "   - Device ID atual: $deviceId")
                Log.d(TAG, "   - Device Name existente: ${existingSession.deviceName}")
                Log.d(TAG, "   - Último heartbeat: ${existingSession.lastHeartbeat}")
                
                val timeSinceLastHeartbeat = System.currentTimeMillis() - existingSession.lastHeartbeat
                Log.d(TAG, "   - Tempo desde último heartbeat: ${timeSinceLastHeartbeat}ms (timeout: ${SESSION_TIMEOUT}ms)")
                
                // Se for o MESMO dispositivo, permitir login (re-login)
                if (existingSession.deviceId == deviceId) {
                    Log.i(TAG, "✅ Mesmo dispositivo, permitindo re-login")
                } 
                // Se for dispositivo DIFERENTE, verificar timeout
                else if (timeSinceLastHeartbeat < SESSION_TIMEOUT) {
                    Log.w(TAG, "❌ Login bloqueado: usuário já está ativo em ${existingSession.deviceName}")
                    return@withContext Pair(false, "Este usuário já está logado em ${existingSession.deviceName}. Desconecte o outro dispositivo primeiro.")
                } else {
                    Log.i(TAG, "⏰ Sessão anterior expirou (sem heartbeat), permitindo novo login")
                }
            } else {
                Log.i(TAG, "✨ Nenhuma sessão existente, criando nova")
            }
            
            // 3. Criar nova sessão
            val newSession = ActiveSession(
                username = username,
                deviceId = deviceId,
                deviceName = deviceName,
                loginTime = System.currentTimeMillis(),
                lastHeartbeat = System.currentTimeMillis()
            )
            
            database.sessions[username] = newSession
            Log.d(TAG, "💾 Salvando nova sessão para $username")
            
            // 4. Salvar no JSONBin
            val saved = saveSessions(database)
            
            if (saved) {
                Log.i(TAG, "✅ Login realizado com sucesso!")
                startHeartbeat(username, deviceId)
                return@withContext Pair(true, "Login realizado com sucesso!")
            } else {
                Log.e(TAG, "❌ Erro ao salvar sessão no JSONBin")
                return@withContext Pair(false, "Erro ao salvar sessão. Verifique sua conexão e tente novamente.")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no login: ${e.message}", e)
            return@withContext Pair(false, "Erro de conexão: ${e.message}")
        }
    }
    
    /**
     * Fazer logout
     */
    suspend fun logout(username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🚪 Fazendo logout: $username")
            stopHeartbeat()
            
            val database = fetchSessions() ?: SessionsDatabase()
            Log.d(TAG, "📊 Sessões antes do logout: ${database.sessions.size}")
            
            val removed = database.sessions.remove(username)
            if (removed != null) {
                Log.i(TAG, "✅ Sessão removida: ${removed.deviceName}")
            } else {
                Log.w(TAG, "⚠️ Nenhuma sessão encontrada para remover")
            }
            
            Log.d(TAG, "📊 Sessões depois do logout: ${database.sessions.size}")
            
            val saved = saveSessions(database)
            if (saved) {
                Log.i(TAG, "✅ Logout salvo com sucesso no JSONBin")
            } else {
                Log.e(TAG, "❌ Erro ao salvar logout no JSONBin")
            }
            
            return@withContext saved
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no logout: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Obter todas as sessões ativas (para admin)
     */
    suspend fun getAllActiveSessions(): List<ActiveSession> = withContext(Dispatchers.IO) {
        try {
            val database = fetchSessions() ?: return@withContext emptyList()
            
            // Filtrar sessões expiradas
            val now = System.currentTimeMillis()
            return@withContext database.sessions.values.filter { 
                (now - it.lastHeartbeat) < SESSION_TIMEOUT 
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar sessões: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Forçar logout de um usuário (admin)
     */
    suspend fun forceLogout(username: String): Boolean {
        return logout(username)
    }
    
    /**
     * Buscar sessões do JSONBin
     */
    private fun fetchSessions(): SessionsDatabase? {
        try {
            Log.d(TAG, "🌐 Buscando sessões do JSONBin...")
            val request = Request.Builder()
                .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID/latest")
                .addHeader("X-Master-Key", JSONBIN_API_KEY)
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "❌ Erro ao buscar sessões: HTTP ${response.code} - ${response.message}")
                    return null
                }
                
                val body = response.body?.string() ?: return null
                Log.d(TAG, "📥 Resposta JSONBin recebida (${body.length} chars)")
                
                val jsonResponse = Json.parseToJsonElement(body).toString()
                
                // JSONBin retorna: { "record": { ... }, "metadata": { ... } }
                // Precisamos extrair apenas "record"
                val recordStart = jsonResponse.indexOf("\"record\":")
                if (recordStart == -1) {
                    Log.w(TAG, "⚠️ Nenhum record encontrado, retornando database vazia")
                    return SessionsDatabase()
                }
                
                val recordJson = jsonResponse.substring(recordStart + 9)
                val recordEnd = recordJson.indexOf(",\"metadata\"")
                val finalJson = if (recordEnd != -1) recordJson.substring(0, recordEnd) else recordJson
                
                val database = json.decodeFromString<SessionsDatabase>(finalJson.trim())
                Log.i(TAG, "✅ Sessões carregadas: ${database.sessions.size} ativas")
                return database
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao decodificar sessões: ${e.message}", e)
            return SessionsDatabase()
        }
    }
    
    /**
     * Salvar sessões no JSONBin
     */
    private fun saveSessions(database: SessionsDatabase): Boolean {
        try {
            Log.d(TAG, "💾 Salvando ${database.sessions.size} sessões e ${database.users.size} usuários no JSONBin...")
            // Sempre usar json.encodeToString para incluir usuários
            val jsonContent = json.encodeToString(database)
            Log.d(TAG, "📤 JSON a enviar: $jsonContent")
            
            val mediaType = "application/json".toMediaType()
            val requestBody = jsonContent.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID")
                .addHeader("X-Master-Key", JSONBIN_API_KEY)
                .addHeader("Content-Type", "application/json")
                .put(requestBody)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "sem detalhes"
                    Log.e(TAG, "❌ Erro ao salvar sessões: HTTP ${response.code} - ${response.message}")
                    Log.e(TAG, "   Detalhes: $errorBody")
                    return false
                }
                Log.i(TAG, "✅ Sessões salvas com sucesso no JSONBin")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar sessões: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Enviar heartbeat para manter sessão ativa
     */
    private suspend fun sendHeartbeat(username: String, deviceId: String) = withContext(Dispatchers.IO) {
        try {
            val database = fetchSessions() ?: return@withContext
            val session = database.sessions[username]
            
            if (session != null && session.deviceId == deviceId) {
                val updatedSession = session.copy(lastHeartbeat = System.currentTimeMillis())
                database.sessions[username] = updatedSession
                saveSessions(database)
                Log.d(TAG, "💓 Heartbeat enviado para $username")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no heartbeat: ${e.message}", e)
        }
    }
    
    /**
     * Iniciar heartbeat automático
     */
    private fun startHeartbeat(username: String, deviceId: String) {
        stopHeartbeat()
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL)
                sendHeartbeat(username, deviceId)
            }
        }
        Log.i(TAG, "💓 Heartbeat iniciado para $username")
    }
    
    /**
     * Parar heartbeat
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        Log.i(TAG, "💓 Heartbeat parado")
    }
    
    // ==================== GERENCIAMENTO DE USUÁRIOS GLOBAIS ====================
    
    /**
     * Buscar todos os usuários cadastrados globalmente
     */
    suspend fun getAllUsers(): List<GlobalUser> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "👥 Buscando usuários globais do JSONBin...")
            val database = fetchSessions() ?: return@withContext emptyList()
            Log.i(TAG, "✅ ${database.users.size} usuários encontrados")
            return@withContext database.users
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar usuários: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * Adicionar ou atualizar usuário global
     */
    suspend fun saveUser(user: GlobalUser): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "💾 Salvando usuário global: ${user.username}")
            val database = fetchSessions() ?: SessionsDatabase()
            
            // Remover usuário existente com mesmo ID
            database.users.removeAll { it.id == user.id }
            
            // Adicionar novo usuário
            database.users.add(user)
            
            val saved = saveSessions(database)
            if (saved) {
                Log.i(TAG, "✅ Usuário ${user.username} salvo com sucesso!")
            }
            return@withContext saved
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar usuário: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Remover usuário global
     */
    suspend fun deleteUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🗑️ Removendo usuário global: $userId")
            val database = fetchSessions() ?: return@withContext false
            
            val removed = database.users.removeAll { it.id == userId }
            if (removed) {
                val saved = saveSessions(database)
                if (saved) {
                    Log.i(TAG, "✅ Usuário removido com sucesso!")
                }
                return@withContext saved
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao remover usuário: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Verificar credenciais de usuário global
     */
    suspend fun validateUser(username: String, password: String): GlobalUser? = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🔍 Validando usuário: $username")
            val users = getAllUsers()
            val user = users.firstOrNull { it.username == username && it.password == password }
            
            if (user != null) {
                Log.i(TAG, "✅ Usuário validado: ${user.username}")
            } else {
                Log.w(TAG, "❌ Usuário ou senha incorretos")
            }
            
            return@withContext user
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao validar usuário: ${e.message}", e)
            return@withContext null
        }
    }
}


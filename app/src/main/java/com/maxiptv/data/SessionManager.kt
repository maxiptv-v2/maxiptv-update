package com.maxiptv.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement
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
    // simpleCodes removido - agora códigos são objeto direto no JSONBin
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
        encodeDefaults = true  // FORÇAR SERIALIZAÇÃO DE VALORES PADRÃO
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
                // Se for dispositivo DIFERENTE, BLOQUEAR SEMPRE (sem timeout automático)
                else {
                    Log.w(TAG, "❌ Login bloqueado: usuário já está ativo em ${existingSession.deviceName}")
                    return@withContext Pair(false, "Este usuário já está logado em ${existingSession.deviceName}. Desconecte o outro dispositivo primeiro.")
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
                    val errorBody = response.body?.string() ?: "sem detalhes"
                    Log.e(TAG, "❌ Erro ao buscar sessões: HTTP ${response.code} - ${response.message}")
                    Log.e(TAG, "   Detalhes: $errorBody")
                    return SessionsDatabase() // Retorna vazio em vez de null
                }
                
                val body = response.body?.string()
                if (body == null || body.isEmpty()) {
                    Log.w(TAG, "⚠️ Resposta vazia do JSONBin")
                    return SessionsDatabase()
                }
                
                Log.d(TAG, "📥 Resposta JSONBin recebida (${body.length} chars)")
                Log.d(TAG, "📄 Primeiros 500 chars: ${body.take(500)}")
                
                // ✅ PARSING ROBUSTO - extrair apenas sessions e users (ignorar códigos)
                try {
                    // JSONBin retorna: { "record": {...}, "metadata": {...} }
                    @kotlinx.serialization.Serializable
                    data class JsonBinResponse(val record: Map<String, kotlinx.serialization.json.JsonElement>)
                    
                    val jsonResponse = json.decodeFromString<JsonBinResponse>(body)
                    val recordMap = jsonResponse.record
                    
                    // Extrair apenas sessions e users (ignorar códigos de 4 dígitos)
                    val sessionsJson = recordMap["sessions"]?.jsonObject ?: buildJsonObject { }
                    val usersJson = recordMap["users"]?.jsonArray ?: kotlinx.serialization.json.buildJsonArray { }
                    
                    // Decodificar sessions
                    val sessions = mutableMapOf<String, ActiveSession>()
                    sessionsJson.forEach { (key, value) ->
                        try {
                            sessions[key] = json.decodeFromString(value.toString())
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Erro ao decodificar sessão $key: ${e.message}")
                        }
                        // Ignorar códigos (chaves de 4 dígitos) - eles não são sessions
                    }
                    
                    // Decodificar users
                    val users = mutableListOf<GlobalUser>()
                    usersJson.forEach { userElement ->
                        try {
                            users.add(json.decodeFromString(userElement.toString()))
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Erro ao decodificar usuário: ${e.message}")
                        }
                    }
                    
                    val database = SessionsDatabase(sessions, users)
                    
                    Log.i(TAG, "✅ Sessões carregadas: ${database.sessions.size} ativas, ${database.users.size} usuários")
                    Log.d(TAG, "🔑 Códigos preservados no record (não processados aqui)")
                    return database
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erro ao decodificar JSON: ${e.message}")
                    Log.e(TAG, "   JSON problemático: ${body.take(1000)}")
                    
                    // Fallback: tentar extrair record manualmente
                    try {
                        val recordRegex = """"record"\s*:\s*(\{.*?\})\s*,\s*"metadata"""".toRegex(RegexOption.DOT_MATCHES_ALL)
                        val match = recordRegex.find(body)
                        if (match != null) {
                            val recordJson = match.groupValues[1]
                            val database = json.decodeFromString<SessionsDatabase>(recordJson)
                            
                            Log.i(TAG, "✅ Sessões extraídas via fallback: ${database.sessions.size} ativas, ${database.users.size} usuários")
                            return database
                        }
                    } catch (fallbackError: Exception) {
                        Log.e(TAG, "❌ Fallback também falhou: ${fallbackError.message}")
                    }
                    
                    return SessionsDatabase()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro geral ao buscar sessões: ${e.message}", e)
            e.printStackTrace()
            return SessionsDatabase()
        }
    }
    
    /**
     * Salvar sessões no JSONBin (preservando códigos existentes)
     */
    private fun saveSessions(database: SessionsDatabase): Boolean {
        return runBlocking(Dispatchers.IO) {
            try {
                Log.d(TAG, "💾 Salvando ${database.sessions.size} sessões e ${database.users.size} usuários no JSONBin...")
                
                // Buscar record atual para preservar códigos
                val request = Request.Builder()
                    .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID/latest")
                    .addHeader("X-Master-Key", JSONBIN_API_KEY)
                    .get()
                    .build()
                
                val record = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
                var logsPreserved = false
                var pendingLoginsPreserved = false
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            try {
                                @kotlinx.serialization.Serializable
                                data class JsonBinResponse(val record: Map<String, kotlinx.serialization.json.JsonElement>)
                                
                                val jsonResponse = json.decodeFromString<JsonBinResponse>(body)
                                // CRÍTICO: Preservar TODOS os campos do record, exceto sessions e users que serão sobrescritos
                                // Isso garante que _login_logs, _pending_logins, códigos e outros dados sejam preservados
                                jsonResponse.record.forEach { (key, value) ->
                                    // Preservar TUDO exceto sessions e users (que serão sobrescritos abaixo)
                                    if (key != "sessions" && key != "users") {
                                        record[key] = value
                                        when {
                                            // Logs de debug
                                            key == "_login_logs" -> {
                                                logsPreserved = true
                                                Log.d(TAG, "📝 Preservando logs de debug (${if (value is kotlinx.serialization.json.JsonArray) value.size.toString() else "?"} logs)")
                                            }
                                            // Códigos pendentes
                                            key == "_pending_logins" -> {
                                                pendingLoginsPreserved = true
                                                Log.d(TAG, "⏳ Preservando códigos pendentes")
                                            }
                                            // Códigos de cliente (3-10 caracteres alfanuméricos)
                                            key.matches(Regex("^[A-Za-z0-9]{3,10}$")) -> {
                                                Log.d(TAG, "🔑 Preservando código: $key")
                                            }
                                            // Outros campos (preservar também)
                                            else -> {
                                                Log.d(TAG, "📦 Preservando campo: $key")
                                            }
                                        }
                                    }
                                }
                                
                                // Verificar se preservou logs e códigos pendentes
                                if (!logsPreserved) {
                                    Log.w(TAG, "⚠️ Logs não encontrados no record atual - isso é normal se for a primeira vez")
                                }
                                if (!pendingLoginsPreserved) {
                                    Log.d(TAG, "ℹ️ Códigos pendentes não encontrados no record atual")
                                } else {
                                    // Nada a fazer se encontrou
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Erro ao ler record para preservar códigos: ${e.message}")
                                Log.w(TAG, "   Continuando sem preservar dados existentes...")
                            }
                        } else {
                            Log.w(TAG, "⚠️ Body vazio ao buscar record atual")
                        }
                    } else {
                        Log.w(TAG, "⚠️ Erro ao buscar record atual: HTTP ${response.code}")
                    }
                }
                val sessionsUsersJson = json.encodeToJsonElement(database)
                if (sessionsUsersJson is kotlinx.serialization.json.JsonObject) {
                    sessionsUsersJson.forEach { (key, value) ->
                        record[key] = value
                    }
                }
                
                // Construir JSON final preservando códigos + sessions/users
                val jsonObject = buildJsonObject {
                    record.forEach { (key, value) ->
                        put(key, value)
                    }
                }
                
                val jsonContent = jsonObject.toString()
                Log.d(TAG, "📤 JSON completo a enviar (com códigos preservados)")
                
                val mediaType = "application/json".toMediaType()
                val requestBody = jsonContent.toRequestBody(mediaType)
                
                val putRequest = Request.Builder()
                    .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID")
                    .addHeader("X-Master-Key", JSONBIN_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .put(requestBody)
                    .build()
                
                client.newCall(putRequest).execute().use { putResponse ->
                    if (!putResponse.isSuccessful) {
                        val errorBody = putResponse.body?.string() ?: "sem detalhes"
                        Log.e(TAG, "❌ Erro ao salvar sessões: HTTP ${putResponse.code} - ${putResponse.message}")
                        Log.e(TAG, "   Detalhes: $errorBody")
                        return@runBlocking false
                    }
                    Log.i(TAG, "✅ Sessões salvas com sucesso no JSONBin (códigos preservados)")
                    return@runBlocking true
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao salvar sessões: ${e.message}", e)
                return@runBlocking false
            }
        }
    }
    
    /**
     * Enviar heartbeat para manter sessão ativa
     */
    private suspend fun sendHeartbeat(username: String, deviceId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "💓 Enviando heartbeat para $username...")
            val database = fetchSessions()
            if (database == null) {
                Log.e(TAG, "❌ Não foi possível buscar sessões para heartbeat")
                return@withContext
            }
            
            val session = database.sessions[username]
            
            if (session == null) {
                Log.w(TAG, "⚠️ Nenhuma sessão encontrada para $username (pode ter expirado)")
                return@withContext
            }
            
            if (session.deviceId != deviceId) {
                Log.w(TAG, "⚠️ Device ID não corresponde, parando heartbeat")
                stopHeartbeat()
                return@withContext
            }
            
            // Atualizar heartbeat
            val updatedSession = session.copy(lastHeartbeat = System.currentTimeMillis())
            database.sessions[username] = updatedSession
            
            val saved = saveSessions(database)
            if (saved) {
                Log.d(TAG, "✅ Heartbeat enviado com sucesso para $username")
            } else {
                Log.e(TAG, "❌ Falha ao salvar heartbeat para $username")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no heartbeat: ${e.message}", e)
            e.printStackTrace()
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
            
            // Remover usuário existente com mesmo ID OU mesmo username (evitar duplicados)
            val removed = database.users.removeAll { it.id == user.id || it.username == user.username }
            if (removed) {
                Log.d(TAG, "🔄 Removendo usuário duplicado: ${user.username}")
            }
            
            // Adicionar novo usuário
            database.users.add(user)
            Log.d(TAG, "➕ Adicionando usuário: ${user.username} (ID: ${user.id})")
            
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
    
    // ==================== GERENCIAMENTO DE CÓDIGOS SIMPLES ====================
    
    /**
     * Salvar código simples no JSONBin (FUNÇÃO ANTIGA - não usar mais)
     * @deprecated Use saveClientCode em vez disso
     */
    @Deprecated("Use saveClientCode")
    suspend fun saveSimpleCode(code: String, simpleCode: com.maxiptv.data.ClientCode): Boolean = withContext(Dispatchers.IO) {
        try {
            // Função antiga - não usar mais
            return@withContext false
            /* 
            Log.i(TAG, "💾 Salvando código simples: $code para ${simpleCode.usuario}")
            val database = fetchSessions() ?: SessionsDatabase()
            
            database.simpleCodes[code] = simpleCode
            
            val saved = saveSessions(database)
            if (saved) {
                Log.i(TAG, "✅ Código simples $code salvo com sucesso!")
            } else {
                Log.e(TAG, "❌ Erro ao salvar código simples $code")
            }
            
            return@withContext saved
            */
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar código simples: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Atualizar código simples (FUNÇÃO ANTIGA)
     */
    @Deprecated("Use saveClientCode para atualizar")
    suspend fun updateSimpleCode(code: String, simpleCode: com.maxiptv.data.ClientCode): Boolean = withContext(Dispatchers.IO) {
        try {
            // Função antiga - não usar mais
            return@withContext false
            /*
            Log.i(TAG, "🔄 Atualizando código simples: $code")
            val database = fetchSessions() ?: return@withContext false
            
            database.simpleCodes[code] = simpleCode
            
            val saved = saveSessions(database)
            if (saved) {
                Log.i(TAG, "✅ Código simples $code atualizado com sucesso!")
            } else {
                Log.e(TAG, "❌ Erro ao atualizar código simples $code")
            }
            
            return@withContext saved
            */
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao atualizar código simples: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Obter código simples (FUNÇÃO ANTIGA)
     */
    @Deprecated("Códigos agora são objeto direto no JSONBin")
    /**
     * Buscar código do JSONBin (usando nova estrutura - códigos como objeto direto)
     */
    suspend fun getClientCode(code: String): com.maxiptv.data.ClientCode? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Buscando código: $code")
            
            val request = Request.Builder()
                .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID/latest")
                .addHeader("X-Master-Key", JSONBIN_API_KEY)
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            @kotlinx.serialization.Serializable
                            data class JsonBinResponse(val record: Map<String, kotlinx.serialization.json.JsonElement>)
                            
                            val jsonResponse = json.decodeFromString<JsonBinResponse>(body)
                            val codeValue = jsonResponse.record[code]
                            
                            if (codeValue != null) {
                                // Decodificar o código
                                val clientCode = json.decodeFromJsonElement<com.maxiptv.data.ClientCode>(codeValue)
                                Log.d(TAG, "✅ Código encontrado: $code para ${clientCode.username}")
                                return@withContext clientCode
                            } else {
                                Log.w(TAG, "❌ Código não encontrado: $code")
                                return@withContext null
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Erro ao decodificar código: ${e.message}", e)
                            return@withContext null
                        }
                    }
                }
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar código: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Função antiga - mantida para compatibilidade
     * @deprecated Use getClientCode em vez disso
     */
    @Deprecated("Use getClientCode")
    suspend fun getSimpleCode(code: String): com.maxiptv.data.ClientCode? = getClientCode(code)
    
    /**
     * Obter todos os códigos simples (FUNÇÃO ANTIGA)
     */
    @Deprecated("Códigos agora são objeto direto no JSONBin")
    suspend fun getAllSimpleCodes(): Map<String, com.maxiptv.data.ClientCode> = withContext(Dispatchers.IO) {
        try {
            // Função antiga - não usar mais
            return@withContext emptyMap()
            /*
            Log.i(TAG, "📋 Buscando todos os códigos simples...")
            val database = fetchSessions() ?: return@withContext emptyMap()
            
            Log.i(TAG, "✅ ${database.simpleCodes.size} códigos simples encontrados")
            return@withContext database.simpleCodes
            */
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar códigos simples: ${e.message}", e)
            return@withContext emptyMap()
        }
    }
    
    /**
     * Remover código simples (FUNÇÃO ANTIGA - não implementada)
     */
    @Deprecated("Implementar remoção de objeto direto quando necessário")
    suspend fun removeSimpleCode(code: String): Boolean = withContext(Dispatchers.IO) {
        // TODO: Implementar remoção de código do objeto direto no JSONBin
        return@withContext false
    }
    
    /**
     * Salvar código de cliente como objeto direto no JSONBin
     * Os códigos ficam diretamente no record como { "1234": {...}, "5678": {...} }
     */
    suspend fun saveClientCode(code: String, clientCode: com.maxiptv.data.ClientCode): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "💾 Salvando código: $code para ${clientCode.username}")
            
            // Buscar record atual do JSONBin
            val request = Request.Builder()
                .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID/latest")
                .addHeader("X-Master-Key", JSONBIN_API_KEY)
                .get()
                .build()
            
            val record = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            @kotlinx.serialization.Serializable
                            data class JsonBinResponse(val record: Map<String, kotlinx.serialization.json.JsonElement>)
                            
                            val jsonResponse = json.decodeFromString<JsonBinResponse>(body)
                            // Copiar todos os campos do record
                            // IMPORTANTE: Remover qualquer código existente para este username ANTES de adicionar o novo
                            jsonResponse.record.forEach { (key, value) ->
                                // Se for um código de 4 dígitos
                                if (key.matches(Regex("^\\d{4}$"))) {
                                    try {
                                        // Verificar se este código pertence ao mesmo usuário
                                        val codeObject = value.jsonObject
                                        val codeUsername = codeObject["username"]?.jsonPrimitive?.contentOrNull
                                        
                                        // Se for o mesmo usuário mas código diferente, REMOVER (evitar duplicados)
                                        if (codeUsername == clientCode.username && key != code) {
                                            Log.d(TAG, "🗑️ Removendo código antigo $key do usuário ${clientCode.username}")
                                            // Não adicionar ao record (remove o código antigo)
                                        } else {
                                            // Manter códigos de outros usuários e outras chaves
                                            record[key] = value
                                        }
                                    } catch (e: Exception) {
                                        // Se não conseguir decodificar, manter por segurança
                                        record[key] = value
                                    }
                                } else {
                                    // Manter outras chaves (sessions, users, etc.)
                                    record[key] = value
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Erro ao ler record existente: ${e.message}")
                        }
                    }
                }
            }
            
            // Adicionar novo código como objeto direto
            record[code] = json.encodeToJsonElement(clientCode)
            
            // Salvar tudo de volta usando JsonObject
            val jsonObject = buildJsonObject {
                record.forEach { (key, value) ->
                    put(key, value)
                }
            }
            
            val jsonContent = jsonObject.toString()
            
            val mediaType = "application/json".toMediaType()
            val requestBody = jsonContent.toRequestBody(mediaType)
            
            val putRequest = Request.Builder()
                .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID")
                .addHeader("X-Master-Key", JSONBIN_API_KEY)
                .addHeader("Content-Type", "application/json")
                .put(requestBody)
                .build()
            
            client.newCall(putRequest).execute().use { putResponse ->
                val success = putResponse.isSuccessful
                if (success) {
                    Log.i(TAG, "✅ Código $code salvo com sucesso!")
                } else {
                    val errorBody = putResponse.body?.string() ?: "sem detalhes"
                    Log.e(TAG, "❌ Erro ao salvar código: HTTP ${putResponse.code} - $errorBody")
                }
                return@withContext success
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao salvar código: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * Buscar código existente para um usuário (para não gerar código duplicado)
     */
    suspend fun getClientCodeForUser(username: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Buscando código existente para usuário: $username")
            
            val request = Request.Builder()
                .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID/latest")
                .addHeader("X-Master-Key", JSONBIN_API_KEY)
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            @kotlinx.serialization.Serializable
                            data class JsonBinResponse(val record: Map<String, kotlinx.serialization.json.JsonElement>)
                            
                            val jsonResponse = json.decodeFromString<JsonBinResponse>(body)
                            
                            // Procurar código que tenha o username correspondente
                            jsonResponse.record.forEach { (key, value) ->
                                if (key.matches(Regex("^\\d{4}$"))) {
                                    try {
                                        // Converter JsonElement para ClientCode corretamente
                                        val codeObject = value.jsonObject
                                        val codeUsername = codeObject["username"]?.jsonPrimitive?.contentOrNull
                                        
                                        if (codeUsername == username) {
                                            // Verificar se o código ainda é válido (menos de 6 horas)
                                            val createdAtJson = codeObject["createdAt"]?.jsonPrimitive?.contentOrNull
                                            val createdAt = createdAtJson?.toLongOrNull() ?: 0L
                                            
                                            if (createdAt > 0) {
                                                val sixHoursInMs = 6 * 60 * 60 * 1000L // 6 horas
                                                val validUntil = createdAt + sixHoursInMs
                                                val currentTime = System.currentTimeMillis()
                                                
                                                if (currentTime > validUntil) {
                                                    Log.d(TAG, "⏰ Código $key expirado para $username (criado há mais de 6 horas)")
                                                    // Código expirou, não retornar (será gerado novo)
                                                } else {
                                                    Log.d(TAG, "✅ Código existente encontrado e válido: $key para $username")
                                                    return@withContext key
                                                }
                                            } else {
                                                // Código antigo sem createdAt - considerar válido por compatibilidade
                                                Log.d(TAG, "✅ Código existente encontrado (sem timestamp): $key para $username")
                                                return@withContext key
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.w(TAG, "⚠️ Erro ao decodificar código $key: ${e.message}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Erro ao buscar código: ${e.message}")
                        }
                    }
                }
            }
            
            Log.d(TAG, "⚠️ Nenhum código encontrado para usuário: $username")
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao buscar código: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Remover código do JSONBin (revogar)
     */
    suspend fun removeClientCode(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🗑️ Removendo código: $code")
            
            // Buscar record atual
            val request = Request.Builder()
                .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID/latest")
                .addHeader("X-Master-Key", JSONBIN_API_KEY)
                .get()
                .build()
            
            val record = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            @kotlinx.serialization.Serializable
                            data class JsonBinResponse(val record: Map<String, kotlinx.serialization.json.JsonElement>)
                            
                            val jsonResponse = json.decodeFromString<JsonBinResponse>(body)
                            // Copiar todos exceto o código a ser removido
                            jsonResponse.record.forEach { (key, value) ->
                                if (key != code) {
                                    record[key] = value
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Erro ao ler record: ${e.message}")
                        }
                    }
                }
            }
            
            // Salvar sem o código removido
            val jsonObject = buildJsonObject {
                record.forEach { (key, value) ->
                    put(key, value)
                }
            }
            
            val jsonContent = jsonObject.toString()
            
            val mediaType = "application/json".toMediaType()
            val requestBody = jsonContent.toRequestBody(mediaType)
            
            val putRequest = Request.Builder()
                .url("$JSONBIN_BASE_URL/b/$JSONBIN_BIN_ID")
                .addHeader("X-Master-Key", JSONBIN_API_KEY)
                .addHeader("Content-Type", "application/json")
                .put(requestBody)
                .build()
            
            client.newCall(putRequest).execute().use { putResponse ->
                val success = putResponse.isSuccessful
                if (success) {
                    Log.i(TAG, "✅ Código $code removido com sucesso!")
                } else {
                    val errorBody = putResponse.body?.string() ?: "sem detalhes"
                    Log.e(TAG, "❌ Erro ao remover código: HTTP ${putResponse.code} - $errorBody")
                }
                return@withContext success
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao remover código: ${e.message}", e)
            return@withContext false
        }
    }
}


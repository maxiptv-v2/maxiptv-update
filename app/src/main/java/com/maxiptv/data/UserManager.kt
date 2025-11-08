package com.maxiptv.data
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class UserAccount(
  val id: String,
  val username: String,
  val password: String,
  val apiUrl: String,
  val expiryDate: String,
  val activeDeviceId: String? = null,
  val activeDeviceName: String? = null,
  val lastLoginTime: Long? = null
)

object UserManager {
  private val K_USERS = stringPreferencesKey("users_list")
  private val K_CURRENT = stringPreferencesKey("current_user")
  private val K_DEVICE_ID = stringPreferencesKey("device_id")
  
  @Serializable
  data class UserSyncResult(
    val added: Int,
    val updated: Int,
    val totalRemote: Int
  )
  
  suspend fun getDeviceId(): String {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val existing = prefs[K_DEVICE_ID]
    if (existing != null) return existing
    
    // Gerar novo ID único para este dispositivo
    val newId = java.util.UUID.randomUUID().toString()
    AppCtx.ctx.dataStore.edit { it[K_DEVICE_ID] = newId }
    return newId
  }
  
  fun getDeviceName(): String {
    return try {
      "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
    } catch (e: Exception) {
      "Dispositivo Desconhecido"
    }
  }
  
  suspend fun addUser(user: UserAccount) {
    android.util.Log.i("UserManager", "➕ Adicionando usuário: ${user.username} (ID: ${user.id})")
    val users = getUsers().toMutableList()
    android.util.Log.i("UserManager", "📊 Total de usuários antes: ${users.size}")
    users.removeAll { it.id == user.id }
    users.add(user)
    android.util.Log.i("UserManager", "📊 Total de usuários depois: ${users.size}")
    saveUsers(users)
    android.util.Log.i("UserManager", "✅ Usuário salvo com sucesso!")
  }
  
  suspend fun removeUser(userId: String) {
    val users = getUsers().filter { it.id != userId }
    saveUsers(users)
  }
  
  suspend fun updateUser(user: UserAccount) {
    addUser(user)
  }
  
  suspend fun syncUsersFromJsonBin(): UserSyncResult = withContext(Dispatchers.IO) {
    try {
      android.util.Log.i("UserManager", "🔄 Sincronizando usuários com JSONBin (uso global)...")
      
      val remoteUsers = SessionManager.getAllUsers()
      android.util.Log.i("UserManager", "🌐 Usuários remotos: ${remoteUsers.size}")
      
      val localUsersList = getUsers().toMutableList()
      val localByUsername = localUsersList.mapIndexed { index, user -> user.username to index }.toMap()
      
      var added = 0
      var updated = 0
      
      remoteUsers.forEach { remote ->
        val existingIndex = localByUsername[remote.username]
        
        if (existingIndex == null) {
          val newUser = UserAccount(
            id = remote.id.ifBlank { java.util.UUID.randomUUID().toString() },
            username = remote.username,
            password = remote.password,
            apiUrl = remote.apiUrl,
            expiryDate = remote.expiryDate,
            activeDeviceId = null,
            activeDeviceName = null,
            lastLoginTime = null
          )
          localUsersList.add(newUser)
          added++
          android.util.Log.i("UserManager", "➕ Usuário adicionado a partir do JSONBin: ${remote.username}")
        } else {
          val existingUser = localUsersList[existingIndex]
          val needsUpdate = existingUser.password != remote.password ||
                            existingUser.apiUrl != remote.apiUrl ||
                            existingUser.expiryDate != remote.expiryDate
          
          if (needsUpdate) {
            localUsersList[existingIndex] = existingUser.copy(
              password = remote.password,
              apiUrl = remote.apiUrl,
              expiryDate = remote.expiryDate
            )
            updated++
            android.util.Log.i("UserManager", "🔄 Usuário atualizado a partir do JSONBin: ${remote.username}")
          } else {
            android.util.Log.d("UserManager", "✓ Usuário já sincronizado: ${remote.username}")
          }
        }
      }
      
      if (added > 0 || updated > 0) {
        saveUsers(localUsersList)
        android.util.Log.i("UserManager", "💾 Usuários sincronizados salvos no DataStore (adicionados=$added, atualizados=$updated)")
      } else {
        android.util.Log.i("UserManager", "ℹ️ Nenhuma alteração necessária na sincronização de usuários")
      }
      
      UserSyncResult(added = added, updated = updated, totalRemote = remoteUsers.size)
    } catch (e: Exception) {
      android.util.Log.e("UserManager", "❌ Erro ao sincronizar usuários do JSONBin: ${e.message}", e)
      UserSyncResult(added = 0, updated = 0, totalRemote = 0)
    }
  }
  
  suspend fun getUsers(): List<UserAccount> {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val json = prefs[K_USERS]
    android.util.Log.i("UserManager", "📥 Carregando usuários do DataStore...")
    if (json == null) {
      android.util.Log.w("UserManager", "⚠️ Nenhum dado encontrado no DataStore")
      return emptyList()
    }
    android.util.Log.d("UserManager", "📄 JSON carregado: $json")
    return try {
      val users = Json.decodeFromString<List<UserAccount>>(json)
      android.util.Log.i("UserManager", "✅ ${users.size} usuários carregados: ${users.map { it.username }}")
      users
    } catch (e: Exception) {
      android.util.Log.e("UserManager", "❌ Erro ao decodificar usuários: ${e.message}", e)
      emptyList()
    }
  }
  
  suspend fun getActiveUsers(): List<UserAccount> {
    return getUsers().filter { it.activeDeviceId != null }
  }
  
  private suspend fun saveUsers(users: List<UserAccount>) {
    android.util.Log.i("UserManager", "💾 Salvando ${users.size} usuários no DataStore...")
    android.util.Log.d("UserManager", "👥 Usuários: ${users.map { it.username }}")
    val json = Json.encodeToString(users)
    android.util.Log.d("UserManager", "📄 JSON a salvar: $json")
    AppCtx.ctx.dataStore.edit { prefs ->
      prefs[K_USERS] = json
      android.util.Log.i("UserManager", "✅ Dados salvos no DataStore com sucesso!")
    }
    
    // Verificar se foi salvo corretamente
    val verification = AppCtx.ctx.dataStore.data.first()[K_USERS]
    if (verification == json) {
      android.util.Log.i("UserManager", "✅ Verificação: Dados persistidos corretamente!")
    } else {
      android.util.Log.e("UserManager", "❌ ERRO: Dados não foram persistidos corretamente!")
      android.util.Log.e("UserManager", "   Esperado: $json")
      android.util.Log.e("UserManager", "   Encontrado: $verification")
    }
  }
  
  suspend fun setCurrentUser(user: UserAccount) {
    AppCtx.ctx.dataStore.edit { it[K_CURRENT] = user.id }
    SettingsRepo.save(user.apiUrl, user.username, user.password, user.expiryDate)
  }
  
  suspend fun getCurrentUser(): UserAccount? {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val currentId = prefs[K_CURRENT] ?: return null
    return getUsers().firstOrNull { it.id == currentId }
  }
  
  suspend fun forceLogout(userId: String) {
    val users = getUsers().toMutableList()
    val index = users.indexOfFirst { it.id == userId }
    if (index != -1) {
      users[index] = users[index].copy(
        activeDeviceId = null,
        activeDeviceName = null,
        lastLoginTime = null
      )
      saveUsers(users)
    }
  }
  
  suspend fun login(username: String, password: String): Pair<UserAccount?, String?> {
    val users = getUsers()
    val user = users.firstOrNull { it.username == username && it.password == password } ?: return Pair(null, "Usuário ou senha incorretos")
    
    val deviceId = getDeviceId()
    
    // Verificar se já está logado em outro dispositivo
    if (user.activeDeviceId != null && user.activeDeviceId != deviceId) {
      return Pair(null, "Este usuário já está logado em ${user.activeDeviceName}. Desconecte o outro dispositivo primeiro.")
    }
    
    // Atualizar informações de login
    val updatedUser = user.copy(
      activeDeviceId = deviceId,
      activeDeviceName = getDeviceName(),
      lastLoginTime = System.currentTimeMillis()
    )
    
    updateUser(updatedUser)
    setCurrentUser(updatedUser)
    
    return Pair(updatedUser, null)
  }
  
  suspend fun logout() {
    val currentUser = getCurrentUser()
    if (currentUser != null) {
      forceLogout(currentUser.id)
    }
    AppCtx.ctx.dataStore.edit { it.remove(K_CURRENT) }
  }
  
  fun getDaysUntilExpiry(expiryDate: String): Int? {
    return try {
      val parts = expiryDate.split("/")
      if (parts.size != 3) return null
      
      val day = parts[0].toInt()
      val month = parts[1].toInt() - 1 // Calendar months are 0-based
      val year = parts[2].toInt()
      
      val calendar = java.util.Calendar.getInstance()
      calendar.set(year, month, day, 23, 59, 59)
      
      val expiryTime = calendar.timeInMillis
      val currentTime = System.currentTimeMillis()
      val diffInDays = ((expiryTime - currentTime) / (1000 * 60 * 60 * 24)).toInt()
      
      diffInDays
    } catch (e: Exception) {
      null
    }
  }
}







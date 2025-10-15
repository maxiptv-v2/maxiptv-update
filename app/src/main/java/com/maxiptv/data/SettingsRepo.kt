package com.maxiptv.data
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object SettingsRepo {
  private val K_BASE = stringPreferencesKey("base")
  private val K_USER = stringPreferencesKey("user")
  private val K_PASS = stringPreferencesKey("pass")
  private val K_EXP  = stringPreferencesKey("exp")
  private val K_LAST_CACHE = stringPreferencesKey("last_cache_time")

  suspend fun save(b: String, u: String, p: String, e: String) {
    AppCtx.ctx.dataStore.edit { it[K_BASE] = b; it[K_USER] = u; it[K_PASS] = p; it[K_EXP] = e }
    XRepo.configure(b, u, p)
  }
  suspend fun test(b: String, u: String, p: String): Boolean {
    XRepo.configure(b, u, p)
    return try {
      val apiField = XRepo::class.java.getDeclaredField("api").apply { isAccessible = true }
      val api = apiField.get(null) as XtreamApi
      val a = api.auth(u, p); (a.user_info?.auth ?: 0) == 1
    } catch (_: Exception) { false }
  }
  fun loadBlocking(): Triple<String,String,String> = runBlocking {
    val prefs = AppCtx.ctx.dataStore.data.first()
    Triple(prefs[K_BASE] ?: "", prefs[K_USER] ?: "", prefs[K_PASS] ?: "")
  }
  
  suspend fun updateCacheTime() {
    AppCtx.ctx.dataStore.edit { it[K_LAST_CACHE] = System.currentTimeMillis().toString() }
  }
  
  suspend fun isCacheValid(): Boolean {
    val prefs = AppCtx.ctx.dataStore.data.first()
    val lastCache = prefs[K_LAST_CACHE]?.toLongOrNull() ?: 0
    val now = System.currentTimeMillis()
    val hoursPassed = (now - lastCache) / (1000 * 60 * 60)
    return hoursPassed < 24
  }
}

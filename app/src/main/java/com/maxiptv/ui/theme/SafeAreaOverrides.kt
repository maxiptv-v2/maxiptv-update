package com.maxiptv.ui.theme

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

data class SafeAreaOverride(
  val profile: String,
  val topDp: Float,
  val bottomDp: Float,
  val startDp: Float,
  val endDp: Float,
  val scaleFactor: Float
)

object SafeAreaOverrides {
  private const val PREF_NAME = "safe_area_overrides"
  private const val KEY_PREFIX = "override_"

  private val stateMap: ConcurrentHashMap<String, MutableStateFlow<SafeAreaOverride?>> = ConcurrentHashMap()

  private fun prefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

  fun overrideFlow(context: Context, fingerprint: String): StateFlow<SafeAreaOverride?> {
    return stateMap.computeIfAbsent(fingerprint) {
      MutableStateFlow(loadFromPrefs(context, fingerprint))
    }
  }

  fun getCurrentOverride(context: Context, fingerprint: String): SafeAreaOverride? {
    return stateMap[fingerprint]?.value ?: loadFromPrefs(context, fingerprint)
  }

  fun update(context: Context, fingerprint: String, override: SafeAreaOverride?) {
    val editor = prefs(context).edit()
    val key = KEY_PREFIX + fingerprint
    if (override == null) {
      editor.remove(key)
    } else {
      val json = JSONObject().apply {
        put("profile", override.profile)
        put("topDp", override.topDp.toDouble())
        put("bottomDp", override.bottomDp.toDouble())
        put("startDp", override.startDp.toDouble())
        put("endDp", override.endDp.toDouble())
        put("scaleFactor", override.scaleFactor.toDouble())
      }
      editor.putString(key, json.toString())
    }
    editor.apply()

    val flow = stateMap.computeIfAbsent(fingerprint) { MutableStateFlow(null) }
    flow.value = override
  }

  fun hasOverride(context: Context, fingerprint: String): Boolean {
    return getCurrentOverride(context, fingerprint) != null
  }

  private fun loadFromPrefs(context: Context, fingerprint: String): SafeAreaOverride? {
    val jsonStr = prefs(context).getString(KEY_PREFIX + fingerprint, null) ?: return null
    return try {
      val obj = JSONObject(jsonStr)
      SafeAreaOverride(
        profile = obj.optString("profile", "remote"),
        topDp = obj.optDouble("topDp", 0.0).toFloat(),
        bottomDp = obj.optDouble("bottomDp", 0.0).toFloat(),
        startDp = obj.optDouble("startDp", 0.0).toFloat(),
        endDp = obj.optDouble("endDp", 0.0).toFloat(),
        scaleFactor = obj.optDouble("scaleFactor", 1.0).toFloat()
      )
    } catch (e: Exception) {
      null
    }
  }
}

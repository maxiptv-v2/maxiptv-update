package com.maxiptv.data

import android.content.Context
import android.os.Build
import com.maxiptv.BuildConfig
import com.maxiptv.MaxiApp
import com.maxiptv.ui.theme.SafeAreaMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.sqrt

object DeviceLogger {
  private const val PREF_NAME = "device_log_prefs"
  private const val KEY_LAST_VERSION = "last_version"
  private const val KEY_LAST_SENT_AT = "last_sent_at"
  private const val FINGERPRINT_ENDPOINT = "https://maxiptv-update-1.onrender.com/device-fingerprint.php"
  private const val MIN_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 horas
  
  suspend fun logDevice(context: Context) {
    withContext(Dispatchers.IO) {
      val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
      val lastVersion = prefs.getString(KEY_LAST_VERSION, null)
      val lastSentAt = prefs.getLong(KEY_LAST_SENT_AT, 0L)
      val now = System.currentTimeMillis()
      if (lastVersion == BuildConfig.VERSION_NAME && now - lastSentAt < MIN_INTERVAL_MS) {
        android.util.Log.d("DeviceLogger", "Skipping device log (recently sent)")
        return@withContext
      }
      
      val resources = context.resources
      val metrics = resources.displayMetrics
      val configuration = resources.configuration
      val prefsOverscan = context.getSharedPreferences("screen_layout_prefs", Context.MODE_PRIVATE)
      val scaleFactor = prefsOverscan.getFloat("scaleFactor_v2", 1f)
      val paddingPx = prefsOverscan.getInt("padding_v2", 0)
      val storedDiagonal = prefsOverscan.getFloat("diagonal_v2", -1f)
      val classification = MaxiApp.deviceCategory
      val safeAreaSnapshot = SafeAreaMetrics.snapshot(context)
      
      val xdpi = if (metrics.xdpi > 0f) metrics.xdpi else metrics.densityDpi.toFloat()
      val ydpi = if (metrics.ydpi > 0f) metrics.ydpi else metrics.densityDpi.toFloat()
      val widthInches = metrics.widthPixels / xdpi
      val heightInches = metrics.heightPixels / ydpi
      val diagonalInches = if (storedDiagonal > 0) storedDiagonal.toDouble() else sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
      
      val payload = JSONObject().apply {
        put("manufacturer", Build.MANUFACTURER)
        put("model", Build.MODEL)
        put("brand", Build.BRAND)
        put("product", Build.PRODUCT)
        put("androidVersion", Build.VERSION.RELEASE ?: "unknown")
        put("androidSdk", Build.VERSION.SDK_INT)
        put("deviceCategory", classification)
        put("appVersion", BuildConfig.VERSION_NAME)
        put("appVersionCode", BuildConfig.VERSION_CODE)
        put("timestamp", now)
        
        put("flags", JSONObject().apply {
          put("isTv", MaxiApp.isTv)
          put("isNativeTv", MaxiApp.isNativeTv)
          put("isFireStick", MaxiApp.isFireStick)
          put("isTvBox", MaxiApp.isTvBox)
          put("isProjector", MaxiApp.isProjector)
          put("isTablet", MaxiApp.isTablet)
          put("isPhone", MaxiApp.isPhone)
        })
        
        put("display", JSONObject().apply {
          put("resolutionPx", "${metrics.widthPixels}x${metrics.heightPixels}")
          put("widthPx", metrics.widthPixels)
          put("heightPx", metrics.heightPixels)
          put("density", metrics.density)
          put("densityDpi", metrics.densityDpi)
          put("xdpi", metrics.xdpi)
          put("ydpi", metrics.ydpi)
          put("screenWidthDp", configuration.screenWidthDp)
          put("screenHeightDp", configuration.screenHeightDp)
          put("calculatedDiagonalInches", diagonalInches)
        })
        
        put("overscanCache", JSONObject().apply {
          put("scaleFactor", scaleFactor)
          put("paddingPx", paddingPx)
          put("diagonalCached", if (storedDiagonal > 0) storedDiagonal.toDouble() else JSONObject.NULL)
          put("hasCache", scaleFactor != 1f || paddingPx != 0 || storedDiagonal > 0f)
        })
      }
      
      safeAreaSnapshot?.let { snapshot ->
        payload.put("safeArea", JSONObject().apply {
          put("version", snapshot.version)
          put("profile", snapshot.profile)
          put("topDp", snapshot.topDp)
          put("bottomDp", snapshot.bottomDp)
          put("startDp", snapshot.startDp)
          put("endDp", snapshot.endDp)
          put("topPx", snapshot.topPx)
          put("bottomPx", snapshot.bottomPx)
          put("startPx", snapshot.startPx)
          put("endPx", snapshot.endPx)
          put("scaleFactor", snapshot.scaleFactor)
          put("diagonalInches", snapshot.diagonalInches.toDouble())
          put("density", snapshot.density.toDouble())
          put("screenWidthDp", snapshot.screenWidthDp)
          put("screenHeightDp", snapshot.screenHeightDp)
          put("updatedAt", snapshot.updatedAt)
        })
      }
      
      try {
        val url = URL(FINGERPRINT_ENDPOINT)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { os ->
          os.write(payload.toString().toByteArray(Charsets.UTF_8))
        }
        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
          connection.inputStream?.close()
          prefs.edit()
            .putString(KEY_LAST_VERSION, BuildConfig.VERSION_NAME)
            .putLong(KEY_LAST_SENT_AT, now)
            .apply()
          android.util.Log.i("DeviceLogger", "Fingerprint enviado com sucesso")
        } else {
          connection.errorStream?.close()
          android.util.Log.w("DeviceLogger", "Falha ao enviar fingerprint: HTTP $responseCode")
        }
        connection.disconnect()
      } catch (e: Exception) {
        android.util.Log.e("DeviceLogger", "Erro ao enviar fingerprint: ${e.message}")
      }
    }
  }
}

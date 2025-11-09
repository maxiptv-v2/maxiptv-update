package com.maxiptv.data

import android.content.Context
import android.os.Build
import com.maxiptv.BuildConfig
import com.maxiptv.MaxiApp
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
  private const val LOG_ENDPOINT = "https://maxiptv-update-1.onrender.com/device-log.php"
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
      val classification = when {
        MaxiApp.isFireStick -> "firestick"
        MaxiApp.isTv -> "tv"
        MaxiApp.isTablet -> "tablet"
        MaxiApp.isPhone -> "phone"
        else -> "unknown"
      }
      
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
        put("classification", classification)
        put("isTvMode", MaxiApp.isTv)
        put("isFireStick", MaxiApp.isFireStick)
        put("isTablet", MaxiApp.isTablet)
        put("isPhone", MaxiApp.isPhone)
        put("appVersion", BuildConfig.VERSION_NAME)
        put("sdkInt", Build.VERSION.SDK_INT)
        put("resolution", "${metrics.widthPixels}x${metrics.heightPixels}")
        put("densityDpi", metrics.densityDpi)
        put("screenWidthDp", configuration.screenWidthDp)
        put("screenHeightDp", configuration.screenHeightDp)
        put("scaleFactor", scaleFactor)
        put("paddingPx", paddingPx)
        put("diagonalInches", String.format("%.2f", diagonalInches))
        put("timestamp", now)
      }
      
      try {
        val url = URL(LOG_ENDPOINT)
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
          android.util.Log.i("DeviceLogger", "Device log enviado com sucesso")
        } else {
          connection.errorStream?.close()
          android.util.Log.w("DeviceLogger", "Falha ao enviar log: HTTP $responseCode")
        }
        connection.disconnect()
      } catch (e: Exception) {
        android.util.Log.e("DeviceLogger", "Erro ao enviar log: ${e.message}")
      }
    }
  }
}

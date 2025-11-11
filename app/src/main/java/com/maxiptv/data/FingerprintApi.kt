package com.maxiptv.data

import android.content.Context
import android.util.Log
import com.maxiptv.BuildConfig
import com.maxiptv.ui.theme.SafeAreaOverride
import com.maxiptv.ui.theme.SafeAreaSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FingerprintApi {
  private const val TAG = "FingerprintApi"
  private const val BASE_URL = "https://maxiptv-update-1.onrender.com"

  suspend fun fetchOverride(
    context: Context,
    fingerprintInfo: DeviceFingerprintInfo
  ): SafeAreaOverride? = withContext(Dispatchers.IO) {
    try {
      val url = URL("$BASE_URL/device-profile.php?fingerprint=${fingerprintInfo.key}")
      val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 7000
        readTimeout = 7000
        setRequestProperty("User-Agent", buildUserAgent())
      }

      val code = connection.responseCode
      if (code == 200) {
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val json = JSONObject(body)
        val profile = json.optJSONObject("profile") ?: return@withContext null
        val safeArea = profile.optJSONObject("safeArea") ?: return@withContext null

        return@withContext SafeAreaOverride(
          profile = safeArea.optString("profile", "remote"),
          topDp = safeArea.optDouble("topDp", 0.0).toFloat(),
          bottomDp = safeArea.optDouble("bottomDp", 0.0).toFloat(),
          startDp = safeArea.optDouble("startDp", 0.0).toFloat(),
          endDp = safeArea.optDouble("endDp", 0.0).toFloat(),
          scaleFactor = safeArea.optDouble("scaleFactor", 1.0).toFloat()
        )
      }
      connection.disconnect()
      null
    } catch (e: Exception) {
      Log.w(TAG, "Erro ao buscar override: ${e.message}")
      null
    }
  }

  suspend fun submitProfile(
    context: Context,
    fingerprintInfo: DeviceFingerprintInfo,
    snapshot: SafeAreaSnapshot
  ) = withContext(Dispatchers.IO) {
    try {
      val url = URL("$BASE_URL/device-profile.php")
      val overscanAdjusted = snapshot.scaleFactor != 1f ||
        snapshot.topDp != 0f ||
        snapshot.bottomDp != 0f ||
        snapshot.startDp != 0f ||
        snapshot.endDp != 0f

      val payload = JSONObject().apply {
        put("fingerprint", fingerprintInfo.key)
        put("device", JSONObject().apply {
          put("manufacturer", fingerprintInfo.manufacturer)
          put("brand", fingerprintInfo.brand)
          put("model", fingerprintInfo.model)
          put("product", fingerprintInfo.product)
          put("sdkInt", fingerprintInfo.sdkInt)
        })
        put("screen", JSONObject().apply {
          put("widthPx", fingerprintInfo.widthPx)
          put("heightPx", fingerprintInfo.heightPx)
          put("densityDpi", fingerprintInfo.densityDpi)
          put("density", fingerprintInfo.density.toDouble())
        })
        put("scaleFactor", snapshot.scaleFactor)
        put("overscanAdjusted", overscanAdjusted)
        put("source", "android_app_${BuildConfig.VERSION_NAME}")
        put("profile", JSONObject().apply {
          put("profile", snapshot.profile)
          put("topDp", snapshot.topDp)
          put("bottomDp", snapshot.bottomDp)
          put("startDp", snapshot.startDp)
          put("endDp", snapshot.endDp)
          put("scaleFactor", snapshot.scaleFactor)
        })
      }

      val json = payload.toString()
      val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 7000
        readTimeout = 7000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("User-Agent", buildUserAgent())
      }

      connection.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
      val code = connection.responseCode
      if (code !in 200..299) {
        Log.w(TAG, "Falha ao enviar perfil: HTTP $code")
      }
      connection.inputStream?.close()
      connection.disconnect()
    } catch (e: Exception) {
      Log.w(TAG, "Erro ao enviar perfil: ${e.message}")
    }
  }

  private fun buildUserAgent(): String {
    return "MaxiPTV/${BuildConfig.VERSION_NAME} (Android)"
  }
}

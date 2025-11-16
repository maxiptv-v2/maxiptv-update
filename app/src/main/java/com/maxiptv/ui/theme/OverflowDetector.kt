package com.maxiptv.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maxiptv.MaxiApp
import com.maxiptv.data.DeviceFingerprint
import kotlinx.coroutines.delay

/**
 * Sistema de detecção automática de overflow (elementos fora da tela)
 * Detecta quando elementos estão saindo da tela e aplica correção incremental
 * NÃO interfere com o device fingerprint - usa o mesmo sistema de chaves
 */
object OverflowDetector {
  private const val PREF_NAME = "overflow_detection"
  private const val KEY_ENABLED = "overflow_detection_enabled"
  private const val KEY_DETECTED_START = "detected_start_overflow_dp"
  private const val KEY_DETECTED_END = "detected_end_overflow_dp"
  private const val KEY_DETECTION_COUNT = "overflow_detection_count"
  private const val MAX_DETECTIONS = 5 // Limite de detecções para evitar ajustes infinitos
  
  /**
   * Verifica se detecção de overflow está habilitada
   */
  fun isEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_ENABLED, true) // Habilitado por padrão
  }
  
  /**
   * Habilita/desabilita detecção de overflow
   */
  fun setEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
  }
  
  /**
   * Carrega correções de overflow detectadas
   */
  fun loadOverflowCorrections(context: Context, fingerprint: String): Pair<Float, Float>? {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val enabled = prefs.getBoolean(KEY_ENABLED, true)
    if (!enabled) return null
    
    val count = prefs.getInt("${KEY_DETECTION_COUNT}_$fingerprint", 0)
    if (count == 0 || count > MAX_DETECTIONS) return null
    
    val startDp = prefs.getFloat("${KEY_DETECTED_START}_$fingerprint", 0f)
    val endDp = prefs.getFloat("${KEY_DETECTED_END}_$fingerprint", 0f)
    
    // Só retorna se houver correção detectada
    if (startDp > 0f || endDp > 0f) {
      return Pair(startDp, endDp)
    }
    
    return null
  }
  
  /**
   * Salva correção de overflow detectada
   * Incrementa valores existentes para ajuste gradual
   */
  fun saveOverflowCorrection(
    context: Context,
    fingerprint: String,
    startOverflowDp: Float,
    endOverflowDp: Float
  ) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val currentCount = prefs.getInt("${KEY_DETECTION_COUNT}_$fingerprint", 0)
    
    // Só salva se ainda não atingiu o limite
    if (currentCount >= MAX_DETECTIONS) {
      android.util.Log.w("OverflowDetector", "Limite de detecções atingido ($MAX_DETECTIONS) para $fingerprint")
      return
    }
    
    // Carrega valores existentes e incrementa (ajuste gradual)
    val existingStart = prefs.getFloat("${KEY_DETECTED_START}_$fingerprint", 0f)
    val existingEnd = prefs.getFloat("${KEY_DETECTED_END}_$fingerprint", 0f)
    
    val newStart = existingStart + startOverflowDp.coerceAtMost(8f) // Máximo 8dp por detecção
    val newEnd = existingEnd + endOverflowDp.coerceAtMost(8f) // Máximo 8dp por detecção
    
    prefs.edit()
      .putBoolean(KEY_ENABLED, true)
      .putFloat("${KEY_DETECTED_START}_$fingerprint", newStart)
      .putFloat("${KEY_DETECTED_END}_$fingerprint", newEnd)
      .putInt("${KEY_DETECTION_COUNT}_$fingerprint", currentCount + 1)
      .putLong("last_overflow_detection_$fingerprint", System.currentTimeMillis())
      .apply()
    
    android.util.Log.i("OverflowDetector", "✅ Correção de overflow salva: start=${newStart}dp, end=${newEnd}dp (detecção ${currentCount + 1}/$MAX_DETECTIONS)")
  }
  
  /**
   * Reseta detecções de overflow para um dispositivo específico
   */
  fun resetOverflowCorrections(context: Context, fingerprint: String) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .remove("${KEY_DETECTED_START}_$fingerprint")
      .remove("${KEY_DETECTED_END}_$fingerprint")
      .remove("${KEY_DETECTION_COUNT}_$fingerprint")
      .remove("last_overflow_detection_$fingerprint")
      .apply()
    android.util.Log.i("OverflowDetector", "🔄 Correções de overflow resetadas para $fingerprint")
  }
}

/**
 * Composable que detecta overflow e aplica correção automaticamente
 * Só funciona em TVs (Fire Stick, TV Box) e não interfere com fingerprint manual
 */
@Composable
fun AutoOverflowCorrection(
  onOverflowDetected: (startDp: Float, endDp: Float) -> Unit = { _, _ -> }
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  
  // Só funciona em TVs
  if (!MaxiApp.isTv) return
  
  val fingerprint = remember { DeviceFingerprint.collect(context) }
  
  // Verifica se já existe override manual (não interfere se existir)
  val hasManualOverride = remember(fingerprint.key) {
    SafeAreaOverrides.hasOverride(context, fingerprint.key)
  }
  
  // Se tem override manual, não aplica detecção automática
  if (hasManualOverride) {
    android.util.Log.d("OverflowDetector", "Override manual detectado - detecção automática desabilitada")
    return
  }
  
  // Carrega correções de overflow já detectadas
  val overflowCorrections = remember(fingerprint.key) {
    OverflowDetector.loadOverflowCorrections(context, fingerprint.key)
  }
  
  // Aplica correções se existirem
  LaunchedEffect(overflowCorrections, fingerprint.key) {
    overflowCorrections?.let { (startDp, endDp) ->
      if (startDp > 0f || endDp > 0f) {
        android.util.Log.i("OverflowDetector", "📐 Aplicando correção de overflow: start=${startDp}dp, end=${endDp}dp")
        onOverflowDetected(startDp, endDp)
      }
    }
  }
}

/**
 * Função auxiliar para detectar overflow em um elemento específico
 * Pode ser chamada manualmente quando detectar elementos fora da tela
 */
fun detectAndSaveOverflow(
  context: Context,
  elementStart: Float, // Posição inicial do elemento em pixels
  elementEnd: Float,   // Posição final do elemento em pixels
  screenWidth: Int,    // Largura da tela em pixels
  density: Float        // Densidade da tela (density)
) {
  if (!MaxiApp.isTv) return
  
  val fingerprint = DeviceFingerprint.collect(context)
  
  // Se tem override manual, não aplica detecção automática
  if (SafeAreaOverrides.hasOverride(context, fingerprint.key)) {
    return
  }
  
  // Detecta overflow nas bordas
  val startOverflow = if (elementStart < 0) -elementStart / density else 0f
  val endOverflow = if (elementEnd > screenWidth) (elementEnd - screenWidth) / density else 0f
  
  if (startOverflow > 0f || endOverflow > 0f) {
    android.util.Log.w("OverflowDetector", "⚠️ Overflow detectado: start=${startOverflow}dp, end=${endOverflow}dp")
    OverflowDetector.saveOverflowCorrection(context, fingerprint.key, startOverflow, endOverflow)
  }
}


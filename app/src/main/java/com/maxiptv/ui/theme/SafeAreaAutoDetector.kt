package com.maxiptv.ui.theme

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.maxiptv.MaxiApp
import com.maxiptv.data.DeviceFingerprint
import kotlinx.coroutines.delay

/**
 * Sistema de detecção automática de overscan
 * Detecta quando há cortes nas bordas e ajusta automaticamente
 * Salva localmente para aplicar nas próximas vezes
 */
object SafeAreaAutoDetector {
  private const val PREF_NAME = "safe_area_auto_detection"
  private const val KEY_ENABLED = "auto_detection_enabled"
  private const val KEY_DETECTED_TOP = "detected_top_dp"
  private const val KEY_DETECTED_BOTTOM = "detected_bottom_dp"
  private const val KEY_DETECTED_START = "detected_start_dp"
  private const val KEY_DETECTED_END = "detected_end_dp"
  private const val KEY_DETECTED_SCALE = "detected_scale_factor"
  private const val KEY_DETECTION_COUNT = "detection_count"
  private const val KEY_LAST_DETECTION = "last_detection_time"
  
  // Limite de detecções para evitar ajustes infinitos
  private const val MAX_DETECTIONS = 3
  
  /**
   * Verifica se já existe uma detecção salva para este dispositivo
   */
  fun hasDetectedSettings(context: Context, fingerprint: String): Boolean {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val count = prefs.getInt(KEY_DETECTION_COUNT, 0)
    return count > 0 && count <= MAX_DETECTIONS
  }
  
  /**
   * Carrega as configurações detectadas automaticamente
   */
  fun loadDetectedSettings(context: Context): SafeAreaOverride? {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val enabled = prefs.getBoolean(KEY_ENABLED, false)
    if (!enabled) return null
    
    val count = prefs.getInt(KEY_DETECTION_COUNT, 0)
    if (count == 0 || count > MAX_DETECTIONS) return null
    
    return SafeAreaOverride(
      profile = "auto_detected",
      topDp = prefs.getFloat(KEY_DETECTED_TOP, 0f),
      bottomDp = prefs.getFloat(KEY_DETECTED_BOTTOM, 0f),
      startDp = prefs.getFloat(KEY_DETECTED_START, 0f),
      endDp = prefs.getFloat(KEY_DETECTED_END, 0f),
      scaleFactor = prefs.getFloat(KEY_DETECTED_SCALE, 1f)
    )
  }
  
  /**
   * Salva as configurações detectadas automaticamente
   */
  fun saveDetectedSettings(
    context: Context,
    topDp: Float,
    bottomDp: Float,
    startDp: Float,
    endDp: Float,
    scaleFactor: Float
  ) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val currentCount = prefs.getInt(KEY_DETECTION_COUNT, 0)
    
    // Só salva se ainda não atingiu o limite
    if (currentCount >= MAX_DETECTIONS) {
      android.util.Log.w("SafeAreaAutoDetector", "Limite de detecções atingido ($MAX_DETECTIONS)")
      return
    }
    
    prefs.edit()
      .putBoolean(KEY_ENABLED, true)
      .putFloat(KEY_DETECTED_TOP, topDp)
      .putFloat(KEY_DETECTED_BOTTOM, bottomDp)
      .putFloat(KEY_DETECTED_START, startDp)
      .putFloat(KEY_DETECTED_END, endDp)
      .putFloat(KEY_DETECTED_SCALE, scaleFactor)
      .putInt(KEY_DETECTION_COUNT, currentCount + 1)
      .putLong(KEY_LAST_DETECTION, System.currentTimeMillis())
      .apply()
    
    android.util.Log.i("SafeAreaAutoDetector", "Configurações detectadas salvas: top=$topDp, bottom=$bottomDp, start=$startDp, end=$endDp, scale=$scaleFactor")
  }
  
  /**
   * Calcula ajustes baseados no tipo de dispositivo e tamanho da tela
   * Usa valores conservadores que funcionam na maioria dos casos
   */
  fun calculateInitialAdjustments(
    context: Context,
    currentPadding: PaddingValues,
    currentScale: Float
  ): SafeAreaOverride? {
    // Só detecta em TVs/projetores
    if (!MaxiApp.isTv && !MaxiApp.isProjector) {
      return null
    }
    
    val metrics = context.resources.displayMetrics
    // Lê dimensões reais da tela quando o app abre
    val diagonalInches = calculateDiagonalInchesImproved(metrics.widthPixels, metrics.heightPixels, metrics.xdpi, metrics.ydpi)
    
    // Se já tem override remoto ou manual, não sobrescreve
    val fingerprint = DeviceFingerprint.collect(context)
    if (SafeAreaOverrides.hasOverride(context, fingerprint.key)) {
      return null
    }
    
    // Valores base conservadores baseados no tipo de dispositivo
    val result = when {
      MaxiApp.isFireStick -> {
        val h = (MaxiApp.fireStickOverscanPadding * 1.2f).coerceAtLeast(24f)
        val vTop = (MaxiApp.fireStickSafeAreaPadding * 0.6f).coerceAtLeast(12f)
        val vBottom = (MaxiApp.fireStickSafeAreaPadding * 1.4f).coerceAtLeast(28f)
        Triple(Triple(vTop, vBottom, h), h, 0.96f)
      }
      MaxiApp.isProjector -> {
        val h = when {
          diagonalInches >= 100 -> 80f
          diagonalInches >= 80 -> 72f
          diagonalInches >= 70 -> 68f
          diagonalInches >= 65 -> 64f
          diagonalInches >= 60 -> 60f
          diagonalInches >= 55 -> 54f
          diagonalInches >= 50 -> 50f
          diagonalInches >= 45 -> 46f
          diagonalInches >= 40 -> 42f
          diagonalInches >= 32 -> 38f
          else -> 32f
        }
        val v = when {
          diagonalInches >= 100 -> 58f
          diagonalInches >= 80 -> 52f
          diagonalInches >= 70 -> 48f
          diagonalInches >= 65 -> 44f
          diagonalInches >= 60 -> 40f
          diagonalInches >= 55 -> 36f
          diagonalInches >= 50 -> 32f
          diagonalInches >= 45 -> 28f
          diagonalInches >= 40 -> 26f
          diagonalInches >= 32 -> 24f
          else -> 20f
        }
        Triple(Triple(v, v, h), h, 0.9f)
      }
      MaxiApp.isNativeTv -> {
        val h = when {
          diagonalInches >= 85 -> 68f
          diagonalInches >= 75 -> 64f
          diagonalInches >= 70 -> 60f
          diagonalInches >= 65 -> 56f
          diagonalInches >= 60 -> 52f
          diagonalInches >= 55 -> 48f
          diagonalInches >= 50 -> 44f
          diagonalInches >= 45 -> 40f
          diagonalInches >= 43 -> 38f
          diagonalInches >= 40 -> 36f
          diagonalInches >= 32 -> 32f
          else -> 28f
        }
        val v = when {
          diagonalInches >= 85 -> 50f
          diagonalInches >= 75 -> 46f
          diagonalInches >= 70 -> 42f
          diagonalInches >= 65 -> 40f
          diagonalInches >= 60 -> 36f
          diagonalInches >= 55 -> 34f
          diagonalInches >= 50 -> 30f
          diagonalInches >= 45 -> 28f
          diagonalInches >= 43 -> 26f
          diagonalInches >= 40 -> 24f
          diagonalInches >= 32 -> 22f
          else -> 20f
        }
        Triple(Triple(v, v, h), h, when {
          diagonalInches >= 85 -> 0.86f
          diagonalInches >= 75 -> 0.88f
          diagonalInches >= 70 -> 0.89f
          diagonalInches >= 65 -> 0.9f
          diagonalInches >= 60 -> 0.91f
          diagonalInches >= 55 -> 0.92f
          diagonalInches >= 50 -> 0.93f
          diagonalInches >= 45 -> 0.95f
          diagonalInches >= 43 -> 0.955f
          diagonalInches >= 40 -> 0.96f
          diagonalInches >= 32 -> 0.97f
          else -> 0.98f
        })
      }
      MaxiApp.isTvBox -> {
        val h = when {
          diagonalInches >= 75 -> 50f
          diagonalInches >= 70 -> 48f
          diagonalInches >= 65 -> 44f
          diagonalInches >= 60 -> 40f
          diagonalInches >= 55 -> 36f
          diagonalInches >= 50 -> 34f
          diagonalInches >= 45 -> 30f
          diagonalInches >= 43 -> 28f
          diagonalInches >= 40 -> 26f
          diagonalInches >= 32 -> 24f
          else -> 20f
        }
        val v = when {
          diagonalInches >= 75 -> 38f
          diagonalInches >= 70 -> 36f
          diagonalInches >= 65 -> 32f
          diagonalInches >= 60 -> 30f
          diagonalInches >= 55 -> 26f
          diagonalInches >= 50 -> 24f
          diagonalInches >= 45 -> 22f
          diagonalInches >= 43 -> 20f
          diagonalInches >= 40 -> 18f
          diagonalInches >= 32 -> 16f
          else -> 14f
        }
        Triple(Triple(v, v, h), h, when {
          diagonalInches >= 75 -> 0.90f
          diagonalInches >= 70 -> 0.91f
          diagonalInches >= 65 -> 0.92f
          diagonalInches >= 60 -> 0.93f
          diagonalInches >= 55 -> 0.94f
          diagonalInches >= 50 -> 0.95f
          diagonalInches >= 45 -> 0.96f
          diagonalInches >= 43 -> 0.965f
          diagonalInches >= 40 -> 0.97f
          diagonalInches >= 32 -> 0.98f
          else -> 0.99f
        })
      }
      else -> return null
    }
    
    // Desempacota o Triple aninhado corretamente
    val (verticalTriple, baseStart, baseScale) = result
    val (baseTop, baseBottom, baseEnd) = verticalTriple
    
    // Se os valores atuais são muito diferentes dos calculados, pode indicar overscan
    val currentTop = currentPadding.calculateTopPadding().value
    val currentBottom = currentPadding.calculateBottomPadding().value
    val currentStart = currentPadding.calculateLeftPadding(LayoutDirection.Ltr).value
    val currentEnd = currentPadding.calculateRightPadding(LayoutDirection.Ltr).value
    
    // Se já tem padding significativo, não ajusta
    if (currentTop > 0.1f || currentBottom > 0.1f || currentStart > 0.1f || currentEnd > 0.1f) {
      return null
    }
    
    // Retorna ajustes calculados
    return SafeAreaOverride(
      profile = "auto_detected",
      topDp = baseTop,
      bottomDp = baseBottom,
      startDp = baseStart,
      endDp = baseEnd,
      scaleFactor = baseScale
    )
  }
  
  /**
   * Calcula o tamanho diagonal da TV baseado nas dimensões reais em pixels
   * Não confia no DPI reportado - usa estimativa baseada em resoluções conhecidas
   * Lê as dimensões reais da tela quando o app abre
   */
  fun calculateDiagonalInchesImproved(widthPx: Int, heightPx: Int, xDpi: Float, yDpi: Float): Double {
    // Primeiro, tenta usar DPI se for razoável (entre 20 e 200 DPI para TVs)
    val xDpiActual = if (xDpi > 20f && xDpi < 200f) xDpi else 0f
    val yDpiActual = if (yDpi > 20f && yDpi < 200f) yDpi else 0f
    
    if (xDpiActual > 0f && yDpiActual > 0f) {
      val widthInches = (widthPx / xDpiActual).toDouble()
      val heightInches = (heightPx / yDpiActual).toDouble()
      val calculated = kotlin.math.sqrt((widthInches * widthInches) + (heightInches * heightInches))
      
      // Valida se o resultado é razoável (entre 20 e 150 polegadas)
      if (calculated >= 20.0 && calculated <= 150.0) {
        return calculated
      }
    }
    
    // Se DPI não for confiável, usa estimativa baseada em resoluções conhecidas
    // Tabela de referência: resolução -> tamanho típico em polegadas
    val resolution = "${widthPx}x${heightPx}"
    
    // Estimativa baseada em resoluções comuns de TV
    val estimatedDiagonal = when {
      // 4K (3840x2160) - TVs grandes
      widthPx >= 3840 && heightPx >= 2160 -> {
        // 4K geralmente em TVs de 50"+
        estimateFromResolution(widthPx, heightPx, minSize = 50.0, maxSize = 100.0)
      }
      // Full HD (1920x1080) - mais comum
      widthPx >= 1920 && heightPx >= 1080 -> {
        // Full HD pode ser de 32" até 85"
        estimateFromResolution(widthPx, heightPx, minSize = 32.0, maxSize = 85.0)
      }
      // HD (1280x720) - TVs menores ou antigas
      widthPx >= 1280 && heightPx >= 720 -> {
        estimateFromResolution(widthPx, heightPx, minSize = 24.0, maxSize = 55.0)
      }
      // Resoluções menores
      widthPx >= 1024 && heightPx >= 768 -> {
        estimateFromResolution(widthPx, heightPx, minSize = 20.0, maxSize = 42.0)
      }
      // Fallback: estimativa conservadora baseada na área da tela
      else -> {
        estimateFromResolution(widthPx, heightPx, minSize = 20.0, maxSize = 65.0)
      }
    }
    
    android.util.Log.d("SafeAreaAutoDetector", 
      "Diagonal estimada: ${estimatedDiagonal.toInt()}\" (resolução: $resolution, DPI reportado: x=$xDpi, y=$yDpi)")
    
    return estimatedDiagonal
  }
  
  /**
   * Estima tamanho diagonal baseado na resolução e área da tela
   * Como a mesma resolução pode ter TVs de tamanhos diferentes,
   * usa uma estimativa conservadora que funciona bem na maioria dos casos
   */
  private fun estimateFromResolution(widthPx: Int, heightPx: Int, minSize: Double, maxSize: Double): Double {
    // Área total em pixels
    val totalPixels = widthPx * heightPx
    
    // Para Full HD (1920x1080), o tamanho mais comum é entre 40" e 55"
    // Usamos 45" como estimativa conservadora (meio-termo)
    val basePixelsFullHD = 1920.0 * 1080.0
    val baseSizeFullHD = 45.0 // Tamanho médio conservador para Full HD
    
    // Para 4K (3840x2160), o tamanho mais comum é entre 55" e 75"
    val basePixels4K = 3840.0 * 2160.0
    val baseSize4K = 65.0 // Tamanho médio para 4K
    
    val estimated = when {
      // 4K ou superior
      totalPixels >= basePixels4K -> {
        val ratio = kotlin.math.sqrt(totalPixels / basePixels4K)
        (baseSize4K * ratio).coerceIn(minSize, maxSize)
      }
      // Full HD ou próximo
      totalPixels >= basePixelsFullHD * 0.8 -> {
        // Para Full HD, usa tamanho médio conservador
        // Como não dá para saber exato, usa o meio da faixa permitida
        val midRange = (minSize + maxSize) / 2.0
        midRange.coerceIn(minSize, maxSize)
      }
      // HD ou menor
      else -> {
        // Para resoluções menores, estima proporcionalmente
        val ratio = kotlin.math.sqrt(totalPixels / basePixelsFullHD)
        (baseSizeFullHD * ratio).coerceIn(minSize, maxSize)
      }
    }
    
    return estimated
  }
}

/**
 * Composable que detecta e aplica ajustes automáticos de safe area
 * Só funciona na primeira vez que o app abre em um dispositivo novo
 */
@Composable
fun AutoDetectSafeArea(
  currentPadding: PaddingValues,
  currentScale: Float,
  onDetected: (SafeAreaOverride) -> Unit
) {
  val context = LocalContext.current
  val fingerprint = remember { DeviceFingerprint.collect(context) }
  var hasChecked by remember { mutableStateOf(false) }
  
  LaunchedEffect(Unit) {
    // Aguarda um pouco para a tela renderizar
    delay(2000)
    
    if (hasChecked) return@LaunchedEffect
    hasChecked = true
    
    // Só detecta se:
    // 1. É TV/projetor
    // 2. Não tem override remoto
    // 3. Não tem detecção anterior salva
    if (!MaxiApp.isTv && !MaxiApp.isProjector) {
      return@LaunchedEffect
    }
    
    if (SafeAreaOverrides.hasOverride(context, fingerprint.key)) {
      android.util.Log.d("AutoDetectSafeArea", "Já tem override remoto, pulando detecção automática")
      return@LaunchedEffect
    }
    
    if (SafeAreaAutoDetector.hasDetectedSettings(context, fingerprint.key)) {
      android.util.Log.d("AutoDetectSafeArea", "Já tem detecção anterior, aplicando...")
      val detected = SafeAreaAutoDetector.loadDetectedSettings(context)
      detected?.let { onDetected(it) }
      return@LaunchedEffect
    }
    
    // Calcula ajustes iniciais baseados no dispositivo
    val adjustments = SafeAreaAutoDetector.calculateInitialAdjustments(
      context,
      currentPadding,
      currentScale
    )
    
    adjustments?.let {
      android.util.Log.i("AutoDetectSafeArea", "Aplicando ajustes automáticos detectados")
      onDetected(it)
      
      // Salva localmente
      SafeAreaAutoDetector.saveDetectedSettings(
        context,
        it.topDp,
        it.bottomDp,
        it.startDp,
        it.endDp,
        it.scaleFactor
      )
      
      // Também salva no SafeAreaOverrides para aplicar
      SafeAreaOverrides.update(context, fingerprint.key, it)
    }
  }
}


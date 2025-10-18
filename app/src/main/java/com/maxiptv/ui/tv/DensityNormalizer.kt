package com.maxiptv.ui.tv

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Normalizador de densidade para UI consistente em todos os dispositivos
 * 
 * Garante que elementos tenham tamanhos adequados em:
 * - Smartphones (densidade padrão)
 * - Tablets (densidade padrão)
 * - TV Box Android (densidade reduzida para textos maiores)
 * - Fire Stick (densidade reduzida)
 * - Android TV (densidade reduzida)
 * - Chromecast com Google TV (densidade reduzida)
 */
@Composable
fun DensityNormalizer(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val defaultDensity = LocalDensity.current
    
    // Detecta o tipo de dispositivo
    val deviceType = detectDeviceType(context)
    
    // Ajusta densidade baseado no dispositivo
    val normalizedDensity = when (deviceType) {
        DeviceType.TV_BOX, 
        DeviceType.FIRE_STICK, 
        DeviceType.ANDROID_TV, 
        DeviceType.CHROMECAST -> {
            // Reduz densidade em 15% para tornar textos/elementos 15% maiores
            // Isso compensa a distância de visualização (10-foot UI)
            Density(
                density = defaultDensity.density * 0.85f,
                fontScale = defaultDensity.fontScale * 1.15f
            )
        }
        DeviceType.TABLET -> {
            // Tablets mantém densidade padrão
            defaultDensity
        }
        DeviceType.SMARTPHONE -> {
            // Smartphones mantém densidade padrão
            defaultDensity
        }
    }
    
    CompositionLocalProvider(LocalDensity provides normalizedDensity) {
        content()
    }
}

/**
 * Tipos de dispositivos suportados
 */
enum class DeviceType {
    SMARTPHONE,
    TABLET,
    TV_BOX,
    FIRE_STICK,
    ANDROID_TV,
    CHROMECAST
}

/**
 * Detecta o tipo de dispositivo com precisão
 */
fun detectDeviceType(context: Context): DeviceType {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    val isTvMode = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    
    val manufacturer = android.os.Build.MANUFACTURER.lowercase()
    val model = android.os.Build.MODEL.lowercase()
    val brand = android.os.Build.BRAND.lowercase()
    val product = android.os.Build.PRODUCT.lowercase()
    
    // Detecção específica de dispositivos
    return when {
        // Fire TV / Fire Stick (Amazon)
        manufacturer.contains("amazon") || 
        model.contains("fire") ||
        product.contains("fire") -> DeviceType.FIRE_STICK
        
        // Chromecast com Google TV
        model.contains("chromecast") ||
        product.contains("chromecast") -> DeviceType.CHROMECAST
        
        // Android TV (Google TV, Sony, etc.)
        isTvMode && (
            brand.contains("google") ||
            manufacturer.contains("sony") ||
            manufacturer.contains("philips") ||
            manufacturer.contains("tcl") ||
            manufacturer.contains("nvidia") || // NVIDIA Shield
            model.contains("shield") ||
            model.contains("bravia") ||
            product.contains("atv") // Android TV genérico
        ) -> DeviceType.ANDROID_TV
        
        // TV Box genéricas (qualquer dispositivo com UI_MODE_TYPE_TELEVISION)
        isTvMode -> DeviceType.TV_BOX
        
        // Tablet (largura >= 600dp e não é TV)
        !isTvMode && context.resources.configuration.screenWidthDp >= 600 -> DeviceType.TABLET
        
        // Smartphone (padrão)
        else -> DeviceType.SMARTPHONE
    }
}

/**
 * Extensão para obter informações do dispositivo (para debug/logs)
 */
fun Context.getDeviceInfo(): String {
    val deviceType = detectDeviceType(this)
    val screenWidthDp = resources.configuration.screenWidthDp
    val screenHeightDp = resources.configuration.screenHeightDp
    val densityDpi = resources.displayMetrics.densityDpi
    
    return """
        Tipo: $deviceType
        Marca: ${android.os.Build.MANUFACTURER}
        Modelo: ${android.os.Build.MODEL}
        Produto: ${android.os.Build.PRODUCT}
        Tela: ${screenWidthDp}x${screenHeightDp}dp
        Densidade: ${densityDpi}dpi
    """.trimIndent()
}


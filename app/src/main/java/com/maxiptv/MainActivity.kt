package com.maxiptv
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.maxiptv.ui.screens.*
import com.maxiptv.ui.theme.MaxiTheme
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 📺 CONFIGURAÇÕES PARA TODAS AS TVs (Fire Stick + TV Box)
    if (MaxiApp.isTv) {
      android.util.Log.i("MainActivity", "📺 Configurando D-pad para TV (${if (MaxiApp.isFireStick) "Fire Stick" else "TV Box"})")
      // API moderna para fullscreen (sem deprecated)
      androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
      window.statusBarColor = android.graphics.Color.TRANSPARENT
      window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }
    
    // 📱 CONFIGURAÇÕES ESPECÍFICAS PARA SMARTPHONES
    if (MaxiApp.isPhone) {
      android.util.Log.i("MainActivity", "📱 Configurando para smartphone com touchscreen")
      // Orientação automática para smartphones
      requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
      // Manter barras de sistema visíveis para smartphones
      androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
    }
    
    adjustScreenLayoutForLargeDisplays()
    
    setContent {
      MaxiTheme {
        val nav = rememberNavController()
        
        // 🎯 INTERCEPTAR NAVEGAÇÃO INTELIGENTE do PlayerActivity
        LaunchedEffect(Unit) {
          // Verificar se veio do PlayerActivity com navegação inteligente
          val navigateTo = intent.getStringExtra("navigateTo")
          val returnFromPlayer = intent.getBooleanExtra("returnFromPlayer", false)
          
          if (returnFromPlayer && !navigateTo.isNullOrEmpty()) {
            android.util.Log.i("MainActivity", "🎯 Navegação inteligente recebida: $navigateTo")
            nav.navigate(navigateTo) {
              // Limpar o stack até a tela de destino
              popUpTo("home") { inclusive = false }
            }
          }
        }
        
        Surface(modifier = Modifier.fillMaxSize()) {
          HomeNav(nav, this@MainActivity)
        }
      }
    }
  }
  
  private fun adjustScreenLayoutForLargeDisplays() {
    val prefs = getSharedPreferences("screen_layout_prefs", Context.MODE_PRIVATE)
    val rootView = window.decorView.rootView ?: return
    
    fun applyPersisted(scale: Float, paddingPx: Int) {
      rootView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
      rootView.scaleX = scale
      rootView.scaleY = scale
      android.util.Log.i("MainActivity", "🧠 Layout overscan reutilizado (scale=$scale padding=$paddingPx)")
    }
    
    val savedScale = prefs.getFloat("scaleFactor_v2", -1f)
    val savedPadding = prefs.getInt("padding_v2", -1)
    if (savedScale in 0.5f..1.1f && savedPadding >= 0) {
      rootView.post { applyPersisted(savedScale, savedPadding) }
      return
    }
    
    fun resetIfNeeded() {
      prefs.edit()
        .remove("scaleFactor_v2")
        .remove("padding_v2")
        .remove("diagonal_v2")
        .remove("device_signature_v2")
        .apply()
      rootView.setPadding(0, 0, 0, 0)
      rootView.scaleX = 1f
      rootView.scaleY = 1f
      android.util.Log.i("MainActivity", "🔄 Overscan automático desativado (perfil limpo)")
    }
    
    if (MaxiApp.isFireStick || MaxiApp.isPhone || MaxiApp.isTablet) {
      resetIfNeeded()
      android.util.Log.d("MainActivity", "ℹ️ Overscan automático ignorado (Fire Stick / Phone / Tablet)")
      return
    }
    
    rootView.post {
      val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager ?: return@post
      val metrics = resources.displayMetrics
      val manufacturer = Build.MANUFACTURER.lowercase()
      val model = Build.MODEL.lowercase()
      val brand = Build.BRAND.lowercase()
      val product = Build.PRODUCT.lowercase()
      
      val isTvMode = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
      val isProjector = listOf(manufacturer, model, product, brand).any { it.contains("projector") }
      
    val tvBrandKeywords = listOf(
      "philco", "philips", "sharp", "aoc", "tcl", "hisense", "skyworth",
      "coocaa", "vizion", "aiwa", "sony", "panasonic", "samsung", "lg",
      "xiaomi tv", "mi tv", "hkc", "seiki", "jvc", "pioneer", "toshiba",
      "spectre", "insignia", "onida", "vu tv", "androidtv", "smart tv"
      )
      val isKnownAndroidTvBrand = tvBrandKeywords.any { keyword ->
        manufacturer.contains(keyword) || brand.contains(keyword) || model.contains(keyword) || product.contains(keyword)
      }
      
      val isBoxOrStick = model.contains("box") ||
        product.contains("box") ||
        model.contains("stick") ||
        product.contains("stick") ||
        brand.contains("box")
      
    fun isLargeDisplay(diagonal: Double, dpi: Int): Boolean =
      diagonal >= 39.5 && dpi <= 260
    
    val xDpi = if (metrics.xdpi > 0f) metrics.xdpi else metrics.densityDpi.toFloat()
    val yDpi = if (metrics.ydpi > 0f) metrics.ydpi else metrics.densityDpi.toFloat()
    val widthInches = metrics.widthPixels / xDpi
    val heightInches = metrics.heightPixels / yDpi
    val diagonalInches = sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
    
    val allowOverscan = when {
      isProjector -> true
      isBoxOrStick -> false
      isTvMode -> true
      isKnownAndroidTvBrand && metrics.densityDpi <= 280 -> true
      isLargeDisplay(diagonalInches, metrics.densityDpi) -> true
      brand.contains("philco") || model.contains("philco") -> true
      else -> false
    }
      
      if (!allowOverscan) {
        android.util.Log.d(
          "MainActivity",
          "ℹ️ Overscan automático ignorado (brand=$brand model=$model diag=${"%.1f".format(diagonalInches)}\" dpi=${metrics.densityDpi})"
        )
        resetIfNeeded()
        return@post
      }
      
      val xdpi = if (metrics.xdpi > 0f) metrics.xdpi else metrics.densityDpi.toFloat()
      val ydpi = if (metrics.ydpi > 0f) metrics.ydpi else metrics.densityDpi.toFloat()
      val widthInches = metrics.widthPixels / xdpi
      val heightInches = metrics.heightPixels / ydpi
      val diagonalInches = sqrt(widthInches * widthInches + heightInches * heightInches)
      val isLargeDisplay = diagonalInches >= 39.5
      val isLowDensity = metrics.densityDpi <= 260
      
      val scaleFactor = when {
        diagonalInches >= 85 -> 0.80f
        diagonalInches >= 70 -> 0.84f
        diagonalInches >= 60 -> 0.87f
        diagonalInches >= 50 -> 0.90f
        diagonalInches >= 40 -> 0.94f
        else -> 0.97f
      }
      
      val paddingDp = when {
        diagonalInches >= 80 -> 24
        diagonalInches >= 60 -> 20
        diagonalInches >= 50 -> 16
        diagonalInches >= 40 -> 12
        else -> 8
      }
      val paddingPx = (paddingDp * metrics.density).roundToInt()
      
      rootView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
      rootView.scaleX = 1f
      rootView.scaleY = 1f
      rootView.animate()
        .scaleX(scaleFactor)
        .scaleY(scaleFactor)
        .setDuration(450)
        .start()
      
      prefs.edit()
        .putFloat("scaleFactor_v2", scaleFactor)
        .putInt("padding_v2", paddingPx)
        .putFloat("diagonal_v2", diagonalInches.toFloat())
        .putString("device_signature_v2", "$manufacturer|$model|$product")
        .apply()
      
      android.util.Log.i(
        "MainActivity",
        "✅ Overscan ajustado automaticamente (diag=${"%.1f".format(diagonalInches)}\" dpi=${metrics.densityDpi} scale=$scaleFactor padding=${paddingPx}px)"
      )
    }
  }
  
  // 🎯 INTERCEPTAR NOVOS INTENTS (quando PlayerActivity navega de volta)
  override fun onNewIntent(newIntent: android.content.Intent) {
    super.onNewIntent(newIntent)
    setIntent(newIntent)
    
    // Processar navegação inteligente se necessário
    val navigateTo = newIntent.getStringExtra("navigateTo")
    val returnFromPlayer = newIntent.getBooleanExtra("returnFromPlayer", false)
    
    if (returnFromPlayer && !navigateTo.isNullOrEmpty()) {
      android.util.Log.i("MainActivity", "🎯 Novo Intent recebido: $navigateTo")
      // A navegação será processada no LaunchedEffect acima
    }
  }
  
  // 🎮 INTERCEPTAR EVENTOS DE D-PAD PARA TODAS AS TVs
  override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
    if (MaxiApp.isTv) {
      val deviceType = if (MaxiApp.isFireStick) "Fire Stick" else "TV Box"
      android.util.Log.i("MainActivity", "📺 D-pad pressionado em $deviceType: $keyCode")
      // Log específico para debug de TVs
      when (keyCode) {
        android.view.KeyEvent.KEYCODE_DPAD_UP -> android.util.Log.i("MainActivity", "⬆️ D-pad UP")
        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> android.util.Log.i("MainActivity", "⬇️ D-pad DOWN")
        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> android.util.Log.i("MainActivity", "⬅️ D-pad LEFT")
        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> android.util.Log.i("MainActivity", "➡️ D-pad RIGHT")
        android.view.KeyEvent.KEYCODE_DPAD_CENTER -> android.util.Log.i("MainActivity", "🔘 D-pad CENTER")
        android.view.KeyEvent.KEYCODE_BACK -> android.util.Log.i("MainActivity", "🔙 BACK")
        android.view.KeyEvent.KEYCODE_MENU -> android.util.Log.i("MainActivity", "📋 MENU")
      }
    }
    return super.onKeyDown(keyCode, event)
  }
}

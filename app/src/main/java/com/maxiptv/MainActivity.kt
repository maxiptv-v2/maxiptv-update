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
    val prefs = getSharedPreferences("screen_prefs", Context.MODE_PRIVATE)
    val savedScale = prefs.getFloat("scaleFactor", -1f)
    val savedPadding = prefs.getInt("padding", -1)
    val rootView = window.decorView.rootView ?: return
    
    if (savedScale in 0.5f..1.1f && savedPadding >= 0) {
      android.util.Log.i("MainActivity", "🧠 Aplicando layout salvo para TV/projetor (scale=$savedScale padding=$savedPadding)")
      rootView.setPadding(savedPadding, savedPadding, savedPadding, savedPadding)
      rootView.scaleX = savedScale
      rootView.scaleY = savedScale
      return
    }
    
    if (MaxiApp.isFireStick || MaxiApp.isPhone || MaxiApp.isTablet) {
      android.util.Log.d("MainActivity", "ℹ️ Ignorando ajuste automático (Fire Stick / Phone / Tablet detectado)")
      return
    }
    
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager ?: return
    val metrics = resources.displayMetrics
    val manufacturer = Build.MANUFACTURER.lowercase()
    val model = Build.MODEL.lowercase()
    val brand = Build.BRAND.lowercase()
    val product = Build.PRODUCT.lowercase()
    
    val isTvBox = model.contains("box") || product.contains("box") || brand.contains("box") || model.contains("stick")
    if (isTvBox) {
      android.util.Log.d("MainActivity", "ℹ️ Dispositivo identificado como TV Box/Stick - sem ajuste extra")
      return
    }
    
    val xdpi = if (metrics.xdpi > 0) metrics.xdpi else metrics.densityDpi.toFloat()
    val ydpi = if (metrics.ydpi > 0) metrics.ydpi else metrics.densityDpi.toFloat()
    val widthInches = metrics.widthPixels / xdpi
    val heightInches = metrics.heightPixels / ydpi
    val diagonalInches = sqrt(widthInches * widthInches + heightInches * heightInches)
    
    val isTvMode = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    val isProjector = manufacturer.contains("projector") ||
                      model.contains("projector") ||
                      product.contains("projector") ||
                      brand.contains("projector")
    val isLargeDisplay = diagonalInches >= 40
    val isLowDensity = metrics.densityDpi <= 240
    
    val shouldAdjust = isTvMode || isProjector || (isLargeDisplay && isLowDensity)
    
    if (!shouldAdjust) {
      android.util.Log.d("MainActivity", "ℹ️ Nenhum ajuste necessário (diag=${"%.1f".format(diagonalInches)}\" dpi=${metrics.densityDpi})")
      return
    }
    
    val scaleFactor = when {
      diagonalInches >= 80 -> 0.80f
      diagonalInches >= 60 -> 0.85f
      diagonalInches >= 50 -> 0.88f
      diagonalInches >= 40 -> 0.92f
      else -> 0.95f
    }
    
    val paddingDp = when {
      diagonalInches >= 80 -> 24
      diagonalInches >= 60 -> 20
      diagonalInches >= 50 -> 16
      diagonalInches >= 40 -> 12
      else -> 8
    }
    
    val paddingPx = (paddingDp * metrics.density).toInt()
    
    rootView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
    rootView.animate()
      .scaleX(scaleFactor)
      .scaleY(scaleFactor)
      .setDuration(500)
      .start()
    
    prefs.edit()
      .putFloat("scaleFactor", scaleFactor)
      .putInt("padding", paddingPx)
      .putFloat("diagonalInches", diagonalInches.toFloat())
      .putInt("densityDpi", metrics.densityDpi)
      .putString("manufacturer", manufacturer)
      .putString("model", model)
      .apply()
    
    android.util.Log.i("MainActivity", "✅ Ajuste automático aplicado: scale=$scaleFactor padding=${paddingPx}px diag=${"%.1f".format(diagonalInches)}\"")
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

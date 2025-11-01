package com.maxiptv
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

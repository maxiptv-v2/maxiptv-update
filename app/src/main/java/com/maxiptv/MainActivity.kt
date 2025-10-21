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
          HomeNav(nav)
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
}

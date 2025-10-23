package com.maxiptv.ui.screens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.maxiptv.data.UserManager
import com.maxiptv.data.SessionManager

@Composable
fun HomeNav(nav: NavHostController) {
  // 🔐 VERIFICAR SE JÁ EXISTE USUÁRIO LOGADO ao iniciar
  var initialRoute by remember { mutableStateOf<String?>(null) }
  
  LaunchedEffect(Unit) {
    android.util.Log.i("HomeNav", "🔍 Verificando sessão existente...")
    val currentUser = UserManager.getCurrentUser()
    
    if (currentUser != null) {
      android.util.Log.i("HomeNav", "✅ Usuário logado encontrado: ${currentUser.username}")
      
      // 🔄 REATIVAR HEARTBEAT para controle de login simultâneo
      try {
        val deviceId = UserManager.getDeviceId()
        val deviceName = UserManager.getDeviceName()
        
        android.util.Log.i("HomeNav", "💓 Reativando heartbeat para ${currentUser.username}...")
        val (success, message) = SessionManager.tryLogin(
          username = currentUser.username,
          deviceId = deviceId,
          deviceName = deviceName
        )
        
        if (success) {
          android.util.Log.i("HomeNav", "✅ Heartbeat reativado! Sessão global restaurada")
        } else {
          android.util.Log.w("HomeNav", "⚠️ Erro ao reativar heartbeat: $message")
        }
      } catch (e: Exception) {
        android.util.Log.e("HomeNav", "❌ Erro ao reativar sessão: ${e.message}")
      }
      
      android.util.Log.i("HomeNav", "🏠 Navegando direto para HOME")
      initialRoute = "home"
    } else {
      android.util.Log.i("HomeNav", "❌ Nenhum usuário logado")
      android.util.Log.i("HomeNav", "🔑 Navegando para LOGIN")
      initialRoute = "login"
    }
  }
  
  // Aguardar verificação antes de renderizar
  if (initialRoute == null) {
    // Mostrar splash/loading enquanto verifica
    return
  }
  
  NavHost(
    navController = nav, 
    startDestination = initialRoute!!
  ) {
    composable("login") { 
      LoginScreen(onLoginSuccess = { 
        nav.navigate("home") {
          popUpTo("login") { inclusive = true }
        }
      }) 
    }
    composable("home") { HomeScreen(nav) }
    composable("live") { LiveScreen(nav) }
    composable("vod") { VodScreen(nav) }
    composable("adult") { AdultContentScreen(nav) }
    composable("series") { SeriesScreen(nav) }
    composable("series/{seriesId}") { backStack ->
      val id = backStack.arguments?.getString("seriesId")?.toIntOrNull() ?: 0
      SeriesDetailsScreen(nav, id)
    }
    composable("vod/{vodId}") { backStack ->
      val id = backStack.arguments?.getString("vodId")?.toIntOrNull() ?: 0
      VodDetailsScreen(nav, id)
    }
  }
}

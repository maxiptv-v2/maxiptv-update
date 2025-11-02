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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeNav(nav: NavHostController, activity: androidx.activity.ComponentActivity? = null) {
  // 🔐 VERIFICAR SE JÁ EXISTE USUÁRIO LOGADO ao iniciar
  var initialRoute by remember { mutableStateOf<String?>(null) }
  
  // Ler código ou credenciais do Intent (se vier do downloader)
  val intentCode = activity?.intent?.getStringExtra("code") ?: ""
  val intentUsuario = activity?.intent?.getStringExtra("usuario") ?: ""
  val intentSenha = activity?.intent?.getStringExtra("senha") ?: ""
  val intentApi = activity?.intent?.getStringExtra("api") ?: ""
  val hasIntentCode = intentCode.isNotBlank() && intentCode.length == 4
  val hasIntentCredentials = intentUsuario.isNotBlank() && intentSenha.isNotBlank()
  
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
      // 🚀 LOGIN AUTOMÁTICO: Tentar buscar código pendente ANTES de mostrar login
      android.util.Log.i("HomeNav", "❌ Nenhum usuário logado - Tentando login automático...")
      
      try {
        // Buscar código pendente do download
        val pendingUrl = "https://maxiptv-update-1.onrender.com/get-pending-code.php"
        android.util.Log.d("HomeNav", "🔍 Buscando código pendente: $pendingUrl")
        val pendingConnection = java.net.URL(pendingUrl).openConnection() as java.net.HttpURLConnection
        pendingConnection.requestMethod = "GET"
        pendingConnection.connectTimeout = 10000
        pendingConnection.readTimeout = 10000
        
        android.util.Log.d("HomeNav", "📡 Resposta HTTP: ${pendingConnection.responseCode}")
        if (pendingConnection.responseCode == 200) {
          val pendingResponse = pendingConnection.inputStream.bufferedReader().use { it.readText() }
          android.util.Log.d("HomeNav", "📥 Resposta: $pendingResponse")
          val pendingJson = org.json.JSONObject(pendingResponse)
          
          if (pendingJson.getString("status") == "ok") {
            val pendingCode = pendingJson.getString("code")
            android.util.Log.i("HomeNav", "✅ Código pendente encontrado: $pendingCode")
            
            // Buscar credenciais usando o código (endpoint auto_login.php)
            val url = "https://maxiptv-update-1.onrender.com/auto_login.php?code=$pendingCode"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            
            if (connection.responseCode == 200) {
              val response = connection.inputStream.bufferedReader().use { it.readText() }
              val json = org.json.JSONObject(response)
              
              if (json.getString("status") == "ok") {
                // auto_login.php retorna: user, password, apiUrl, valid_until
                val user = json.optString("user", json.optString("usuario", ""))
                val pass = json.optString("password", json.optString("senha", ""))
                val api = json.optString("apiUrl", json.optString("api", ""))
                val expiryDate = json.optString("valid_until", json.optString("expiryDate", json.optString("expira_em", "")))
                
                android.util.Log.i("HomeNav", "✅ Login automático iniciado para: $user")
                
                // Buscar usuário ou criar se não existir (funções suspend)
                var userAccount = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                  UserManager.getUsers().firstOrNull { it.username == user }
                }
                
                if (userAccount == null) {
                  // Criar novo usuário se não existir
                  android.util.Log.i("HomeNav", "📝 Criando novo usuário: $user")
                  userAccount = com.maxiptv.data.UserAccount(
                    id = java.util.UUID.randomUUID().toString(),
                    username = user,
                    password = pass,
                    apiUrl = api,
                    expiryDate = expiryDate,
                    activeDeviceId = null,
                    activeDeviceName = null,
                    lastLoginTime = null
                  )
                  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    UserManager.addUser(userAccount)
                  }
                }
                
                // Fazer login usando UserManager (suspend)
                val (loggedUser, error) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                  UserManager.login(user, pass)
                }
                
                if (loggedUser != null && error == null) {
                  android.util.Log.i("HomeNav", "✅ Login automático bem-sucedido: ${loggedUser.username}")
                  
                  // Salvar credenciais no SettingsRepo (suspend)
                  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.maxiptv.data.SettingsRepo.save(
                      b = api,
                      u = user,
                      p = pass,
                      e = expiryDate
                    )
                  }
                  
                  // Criar sessão no JSONBin (suspend)
                  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val deviceId = UserManager.getDeviceId()
                    val deviceName = UserManager.getDeviceName()
                    SessionManager.tryLogin(
                      username = user,
                      deviceId = deviceId,
                      deviceName = deviceName
                    )
                  }
                  
                  android.util.Log.i("HomeNav", "🏠 Login automático completo! Navegando para HOME")
                  // Navegar diretamente para home usando navController
                  nav.navigate("home") {
                    popUpTo(0) { inclusive = true } // Limpar toda a stack
                  }
                  initialRoute = "home"
                  return@LaunchedEffect
                } else {
                  android.util.Log.e("HomeNav", "❌ Erro no login automático: $error")
                  // Navegar para login se falhar
                  nav.navigate("login") {
                    popUpTo(0) { inclusive = true }
                  }
                  initialRoute = "login"
                  return@LaunchedEffect
                }
              } else {
                android.util.Log.e("HomeNav", "❌ auto_login.php retornou status != ok")
                android.util.Log.d("HomeNav", "   Resposta: $response")
              }
            } else {
              android.util.Log.e("HomeNav", "❌ auto_login.php retornou HTTP != 200: ${connection.responseCode}")
            }
          } else {
            android.util.Log.d("HomeNav", "⚠️ get-pending-code.php retornou status != ok: ${pendingJson.optString("status", "unknown")}")
          }
        } else {
          android.util.Log.e("HomeNav", "❌ get-pending-code.php retornou HTTP != 200: ${pendingConnection.responseCode}")
        }
      } catch (e: Exception) {
        android.util.Log.e("HomeNav", "❌ Erro ao buscar código pendente: ${e.message}", e)
        android.util.Log.d("HomeNav", "ℹ️ Continuando sem código pendente - mostrará tela de login")
      }
      
      // Se não encontrou código pendente, mostrar tela de login
      android.util.Log.i("HomeNav", "🔑 Nenhum código pendente - Navegando para LOGIN")
      // Se o NavHost já foi criado, navegar diretamente
      if (initialRoute != null) {
        nav.navigate("login") {
          popUpTo(0) { inclusive = true }
        }
      }
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
      LoginScreen(
        onLoginSuccess = { 
          nav.navigate("home") {
            popUpTo("login") { inclusive = true }
          }
        },
        initialCode = intentCode,
        initialUsuario = intentUsuario,
        initialSenha = intentSenha,
        initialApi = intentApi,
        hasInitialCredentials = hasIntentCode || hasIntentCredentials
      ) 
    }
    composable("home") { HomeScreen(nav) }
    composable("live") { LiveScreen(nav) }
    composable("vod") { VodScreen(nav) }
    composable("adult") { AdultContentScreen(nav) }
    composable("series") { SeriesScreen(nav) }
    composable("favorites") { FavoritesScreen(nav) }
    composable("search") { SearchScreen(nav) }
    composable("player-settings") { PlayerSettingsScreen(nav) }
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

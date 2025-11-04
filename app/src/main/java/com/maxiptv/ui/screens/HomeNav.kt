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
  var shouldNavigateToHome by remember { mutableStateOf(false) }
  
  // Ler código ou credenciais do Intent (se vier do downloader)
  val intentCode = activity?.intent?.getStringExtra("code") ?: ""
  val intentUsuario = activity?.intent?.getStringExtra("usuario") ?: ""
  val intentSenha = activity?.intent?.getStringExtra("senha") ?: ""
  val intentApi = activity?.intent?.getStringExtra("api") ?: ""
  val hasIntentCode = intentCode.isNotBlank() && intentCode.length >= 3 && intentCode.length <= 10 && intentCode.all { it.isLetterOrDigit() }
  val hasIntentCredentials = intentUsuario.isNotBlank() && intentSenha.isNotBlank()
  
  LaunchedEffect(Unit) {
    android.util.Log.i("HomeNav", "🔍 Verificando sessão existente...")
    
    // 🚀 SEMPRE tentar login automático primeiro (mesmo se já tiver usuário logado)
    // Isso permite que após baixar o APK, o app faça login automático
    android.util.Log.i("HomeNav", "🚀 Tentando login automático (verificando código pendente)...")
    
    var autoLoginSuccess = false
    
    try {
      // Buscar código pendente do download
      val pendingUrl = "https://maxiptv-update-1.onrender.com/get-pending-code.php"
      android.util.Log.d("HomeNav", "🔍 Buscando código pendente: $pendingUrl")
      val pendingConnection = java.net.URL(pendingUrl).openConnection() as java.net.HttpURLConnection
      pendingConnection.requestMethod = "GET"
      pendingConnection.connectTimeout = 10000
      pendingConnection.readTimeout = 10000
      pendingConnection.connect()
      
      android.util.Log.d("HomeNav", "📡 Resposta HTTP: ${pendingConnection.responseCode}")
      if (pendingConnection.responseCode == 200) {
          val pendingResponse = pendingConnection.inputStream.bufferedReader().use { it.readText() }
          android.util.Log.d("HomeNav", "📥 Resposta COMPLETA: $pendingResponse")
          val pendingJson = org.json.JSONObject(pendingResponse)
          
          val pendingStatus = pendingJson.optString("status", "")
          android.util.Log.d("HomeNav", "📊 Status recebido: $pendingStatus")
          
          if (pendingStatus == "ok") {
            val pendingCode = pendingJson.optString("code", "")
            android.util.Log.i("HomeNav", "✅ Código pendente encontrado: $pendingCode")
            
            if (pendingCode.isBlank()) {
              android.util.Log.e("HomeNav", "❌ Código pendente está vazio!")
              return@LaunchedEffect
            }
            
            // Buscar credenciais usando o código (endpoint auto_login.php)
            val url = "https://maxiptv-update-1.onrender.com/auto_login.php?code=$pendingCode"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            
            if (connection.responseCode == 200) {
              val response = connection.inputStream.bufferedReader().use { it.readText() }
              android.util.Log.d("HomeNav", "📥 Resposta auto_login.php COMPLETA: $response")
              val json = org.json.JSONObject(response)
              
              // auto_login.php retorna: { "status": "success", "autologin": { "username", "password", "api_url", "expires_in" } }
              val status = json.optString("status", "")
              android.util.Log.d("HomeNav", "📊 Status auto_login.php: $status")
              
              if (status == "success") {
                val autologin = json.optJSONObject("autologin")
                android.util.Log.d("HomeNav", "📊 Objeto autologin: ${autologin != null}")
                
                if (autologin != null) {
                  val user = autologin.optString("username", "")
                  val pass = autologin.optString("password", "")
                  val api = autologin.optString("api_url", "")
                  android.util.Log.d("HomeNav", "📊 Credenciais extraidas:")
                  android.util.Log.d("HomeNav", "   User: $user")
                  android.util.Log.d("HomeNav", "   Pass: ${if (pass.isNotBlank()) "***" else "VAZIO"}")
                  android.util.Log.d("HomeNav", "   API: $api")
                  
                  // expiresIn = 21600 segundos (6 horas) - já validado no PHP
                  
                  // Verificar se recebeu todos os campos necessários
                  if (user.isNotBlank() && pass.isNotBlank() && api.isNotBlank()) {
                    // Buscar expiryDate do objeto autologin
                    val expiryDate = autologin.optString("expiryDate", "")
                    android.util.Log.d("HomeNav", "   ExpiryDate: $expiryDate")
                    
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
                      
                      // Configurar XRepo ANTES de salvar credenciais (importante para buscar canais)
                      android.util.Log.d("HomeNav", "⚙️ Configurando XRepo com API: $api")
                      com.maxiptv.data.XRepo.configure(api, user, pass)
                      android.util.Log.d("HomeNav", "✅ XRepo configurado")
                      
                      // Salvar credenciais no SettingsRepo (suspend)
                      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        android.util.Log.d("HomeNav", "💾 Salvando credenciais no SettingsRepo...")
                        com.maxiptv.data.SettingsRepo.save(
                          b = api,
                          u = user,
                          p = pass,
                          e = expiryDate
                        )
                        android.util.Log.d("HomeNav", "✅ Credenciais salvas")
                      }
                      
                      // Criar sessão no JSONBin (suspend)
                      kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        android.util.Log.d("HomeNav", "🔐 Criando sessão no JSONBin...")
                        val deviceId = UserManager.getDeviceId()
                        val deviceName = UserManager.getDeviceName()
                        val (success, message) = SessionManager.tryLogin(
                          username = user,
                          deviceId = deviceId,
                          deviceName = deviceName
                        )
                        if (success) {
                          android.util.Log.d("HomeNav", "✅ Sessão criada: $message")
                        } else {
                          android.util.Log.w("HomeNav", "⚠️ Sessão não criada: $message")
                        }
                      }
                      
                      android.util.Log.i("HomeNav", "🏠 Login automático completo! Definindo navegação para HOME")
                      // Marcar para navegar (navegação será feita quando NavHost estiver pronto)
                      autoLoginSuccess = true
                      initialRoute = "home"
                      shouldNavigateToHome = true
                      android.util.Log.d("HomeNav", "   initialRoute = home, shouldNavigateToHome = true")
                      return@LaunchedEffect
                    } else {
                      android.util.Log.e("HomeNav", "❌ Erro no login automático")
                      android.util.Log.e("HomeNav", "   loggedUser: $loggedUser")
                      android.util.Log.e("HomeNav", "   error: $error")
                      autoLoginSuccess = false
                      // Não retornar aqui - continuar para verificar usuário existente
                    }
                  } else {
                    android.util.Log.e("HomeNav", "❌ auto_login.php retornou campos incompletos ou vazios")
                    android.util.Log.e("HomeNav", "   User vazio: ${user.isBlank()}")
                    android.util.Log.e("HomeNav", "   Pass vazio: ${pass.isBlank()}")
                    android.util.Log.e("HomeNav", "   API vazio: ${api.isBlank()}")
                    android.util.Log.d("HomeNav", "   Resposta completa: $response")
                  }
                } else {
                  android.util.Log.e("HomeNav", "❌ auto_login.php não retornou objeto 'autologin'")
                  android.util.Log.d("HomeNav", "   Resposta completa: $response")
                  android.util.Log.d("HomeNav", "   Chaves disponíveis: ${json.keys().asSequence().joinToString()}")
                }
              } else {
                android.util.Log.e("HomeNav", "❌ auto_login.php retornou status != 'success'")
                android.util.Log.e("HomeNav", "   Status recebido: '$status'")
                android.util.Log.d("HomeNav", "   Resposta completa: $response")
              }
            } else {
              android.util.Log.e("HomeNav", "❌ auto_login.php retornou HTTP != 200: ${connection.responseCode}")
              try {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "sem erro"
                android.util.Log.e("HomeNav", "   Corpo do erro: $errorBody")
              } catch (e: Exception) {
                android.util.Log.e("HomeNav", "   Erro ao ler corpo do erro: ${e.message}")
              }
            }
          } else {
            android.util.Log.w("HomeNav", "⚠️ get-pending-code.php retornou status != ok")
            android.util.Log.w("HomeNav", "   Status recebido: '$pendingStatus'")
            android.util.Log.w("HomeNav", "   Resposta completa: $pendingResponse")
          }
      } else {
        android.util.Log.e("HomeNav", "❌ get-pending-code.php retornou HTTP != 200: ${pendingConnection.responseCode}")
        try {
          val errorBody = pendingConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: "sem erro"
          android.util.Log.e("HomeNav", "   Corpo do erro: $errorBody")
        } catch (e: Exception) {
          android.util.Log.e("HomeNav", "   Erro ao ler corpo do erro: ${e.message}")
        }
      }
    } catch (e: Exception) {
      android.util.Log.e("HomeNav", "❌ Erro ao buscar código pendente: ${e.message}", e)
      android.util.Log.e("HomeNav", "   Tipo de erro: ${e.javaClass.simpleName}")
      android.util.Log.e("HomeNav", "   Stack trace: ${e.stackTraceToString()}")
      android.util.Log.d("HomeNav", "ℹ️ Continuando sem código pendente")
    }
    
    // Se login automático não funcionou, verificar se já tem usuário logado
    if (!autoLoginSuccess) {
      android.util.Log.i("HomeNav", "🔍 Login automático não funcionou - Verificando usuário existente...")
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
        // Se não encontrou código pendente e não tem usuário logado, mostrar tela de login
        android.util.Log.i("HomeNav", "🔑 Nenhum código pendente e nenhum usuário logado - Navegando para LOGIN")
        initialRoute = "login"
      }
    }
  }
  
  // Aguardar verificação antes de renderizar
  if (initialRoute == null) {
    // Mostrar splash/loading enquanto verifica
    return
  }
  
  // Navegar para home se login automático foi bem-sucedido
  LaunchedEffect(shouldNavigateToHome) {
    if (shouldNavigateToHome) {
      android.util.Log.i("HomeNav", "🚀 Executando navegação para home após login automático")
      android.util.Log.d("HomeNav", "   Aguardando NavHost estar pronto...")
      kotlinx.coroutines.delay(300) // Delay maior para garantir que NavHost está completamente pronto
      
      try {
        android.util.Log.d("HomeNav", "   Tentando navegar para 'home'...")
        nav.navigate("home") {
          popUpTo(0) { inclusive = true } // Limpar toda a stack
        }
        android.util.Log.i("HomeNav", "✅ Navegação para 'home' executada com sucesso!")
        shouldNavigateToHome = false
      } catch (e: Exception) {
        android.util.Log.e("HomeNav", "❌ ERRO ao navegar para home: ${e.message}")
        android.util.Log.e("HomeNav", "   Stack trace: ${e.stackTraceToString()}")
        // Tentar novamente após mais delay
        kotlinx.coroutines.delay(500)
        try {
          nav.navigate("home") {
            popUpTo(0) { inclusive = true }
          }
          android.util.Log.i("HomeNav", "✅ Navegação retentada com sucesso!")
          shouldNavigateToHome = false
        } catch (e2: Exception) {
          android.util.Log.e("HomeNav", "❌ ERRO na segunda tentativa: ${e2.message}")
        }
      }
    }
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
  
  /**
   * Verificar se data de expiração está vencida (formato DD/MM/YYYY)
   */
  fun isExpired(expiryDate: String): Boolean {
    return try {
      if (expiryDate.isBlank()) return false // Se não tem data, não está expirado
      
      val parts = expiryDate.split("/")
      if (parts.size != 3) return true // Formato inválido = considerado expirado
      
      val day = parts[0].toInt()
      val month = parts[1].toInt() - 1 // Calendar months are 0-based
      val year = parts[2].toInt()
      
      val calendar = java.util.Calendar.getInstance()
      calendar.set(year, month, day, 23, 59, 59)
      
      val expiryTime = calendar.timeInMillis
      val currentTime = System.currentTimeMillis()
      
      currentTime > expiryTime
    } catch (e: Exception) {
      android.util.Log.e("HomeNav", "❌ Erro ao verificar expiração: ${e.message}")
      true // Em caso de erro, considerar expirado por segurança
    }
  }
}

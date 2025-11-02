package com.maxiptv.ui.screens
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxiptv.R
import com.maxiptv.data.UserManager
import com.maxiptv.data.UserAccount
import com.maxiptv.data.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  onLoginSuccess: () -> Unit,
  initialCode: String = "",
  initialUsuario: String = "",
  initialSenha: String = "",
  initialApi: String = "",
  hasInitialCredentials: Boolean = false
) {
  var username by remember { mutableStateOf(initialUsuario) }
  var password by remember { mutableStateOf(initialSenha) }
  var code by remember { mutableStateOf(initialCode) }
  var passwordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var tapCount by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  
  // 🛡️ PROTEÇÃO MÁXIMA: Não criar NENHUM usuário automaticamente
  // Todos os usuários devem ser adicionados APENAS pelo painel admin (5 toques no logo)
  // Isso garante que o JSONBin NUNCA será sobrescrito e os usuários cadastrados são preservados
  
  // 🔄 LOGIN AUTOMÁTICO se vier do downloader com código
  LaunchedEffect(hasInitialCredentials) {
    android.util.Log.i("LoginScreen", "🔐 LoginScreen carregada")
    
    // Se veio código via Intent do downloader, buscar credenciais automaticamente
    if (initialCode.isNotBlank() && initialCode.length == 4 && initialCode.all { it.isDigit() }) {
      android.util.Log.i("LoginScreen", "🔑 Código recebido do downloader: $initialCode")
      
      // Buscar credenciais usando o código
      isLoading = true
      
      try {
        android.util.Log.i("LoginScreen", "🔑 Buscando credenciais do código: $initialCode")
        
        val url = "https://maxiptv-update-1.onrender.com/auto_login.php?code=$initialCode"
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()
        
        if (connection.responseCode == 200) {
          val response = connection.inputStream.bufferedReader().use { it.readText() }
          val json = org.json.JSONObject(response)
          
          if (json.getString("status") == "ok") {
            // auto_login.php retorna: user, password, apiUrl, valid_until
            val user = json.optString("user", json.optString("usuario", ""))
            val pass = json.optString("password", json.optString("senha", ""))
            val api = json.optString("apiUrl", json.optString("api", ""))
            
            android.util.Log.i("LoginScreen", "✅ Credenciais recebidas: $user")
            
            // Fazer login automático
            username = user
            password = pass
            doLogin(user, pass, api, onLoginSuccess) { msg ->
              errorMessage = msg
              isLoading = false
            }
            return@LaunchedEffect
          } else {
            val mensagem = json.optString("mensagem", "Código inválido")
            errorMessage = mensagem
            android.util.Log.e("LoginScreen", "❌ Erro no código: $mensagem")
            isLoading = false
            return@LaunchedEffect
          }
        } else {
          val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Erro ao conectar"
          android.util.Log.e("LoginScreen", "❌ Erro HTTP ${connection.responseCode}: $errorText")
          errorMessage = errorText
          isLoading = false
          return@LaunchedEffect
        }
      } catch (e: Exception) {
        android.util.Log.e("LoginScreen", "❌ Erro: ${e.message}", e)
        errorMessage = "Erro ao buscar credenciais: ${e.message}"
        isLoading = false
        return@LaunchedEffect
      }
    } else if (hasInitialCredentials && initialUsuario.isNotBlank() && initialSenha.isNotBlank()) {
      // Fallback: se vier credenciais diretas (compatibilidade)
      android.util.Log.i("LoginScreen", "🔑 Credenciais recebidas diretamente do downloader!")
      android.util.Log.i("LoginScreen", "   Usuario: $initialUsuario")
      android.util.Log.i("LoginScreen", "   API: $initialApi")
      
      isLoading = true
      doLogin(initialUsuario, initialSenha, initialApi, onLoginSuccess) { msg ->
        errorMessage = msg
        isLoading = false
      }
    } else {
      // Tentar buscar código pendente baseado no IP (quando app abre após download)
      android.util.Log.i("LoginScreen", "🔍 Tentando buscar código pendente do download...")
      
      try {
        val pendingUrl = "https://maxiptv-update-1.onrender.com/get-pending-code.php"
        val pendingConnection = java.net.URL(pendingUrl).openConnection() as java.net.HttpURLConnection
        pendingConnection.requestMethod = "GET"
        pendingConnection.connect()
        
        if (pendingConnection.responseCode == 200) {
          val pendingResponse = pendingConnection.inputStream.bufferedReader().use { it.readText() }
          val pendingJson = org.json.JSONObject(pendingResponse)
          
          if (pendingJson.getString("status") == "ok") {
            val pendingCode = pendingJson.getString("code")
            android.util.Log.i("LoginScreen", "✅ Código pendente encontrado: $pendingCode")
            
            // Buscar credenciais usando o código (endpoint auto_login.php)
            val url = "https://maxiptv-update-1.onrender.com/auto_login.php?code=$pendingCode"
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connect()
            
            if (connection.responseCode == 200) {
              val response = connection.inputStream.bufferedReader().use { it.readText() }
              val json = org.json.JSONObject(response)
              
              if (json.getString("status") == "ok") {
                // auto_login.php retorna: user, password, apiUrl, valid_until
                val user = json.optString("user", json.optString("usuario", ""))
                val pass = json.optString("password", json.optString("senha", ""))
                val api = json.optString("apiUrl", json.optString("api", ""))
                
                android.util.Log.i("LoginScreen", "✅ Login automático via código pendente: $user")
                
                // Fazer login automático
                isLoading = true
                doLogin(user, pass, api, onLoginSuccess) { msg ->
                  errorMessage = msg
                  isLoading = false
                }
                return@LaunchedEffect
              }
            }
          }
        }
      } catch (e: Exception) {
        android.util.Log.d("LoginScreen", "ℹ️ Nenhum código pendente ou erro: ${e.message}")
      }
      
      android.util.Log.i("LoginScreen", "👥 Usuários devem ser gerenciados pelo painel admin")
    }
  }
  
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // Logo e Título
    Icon(
      imageVector = Icons.Default.PlayArrow,
      contentDescription = "MaxiPTV Logo",
      modifier = Modifier
        .size(120.dp)
        .clickable(
          indication = null,
          interactionSource = remember { MutableInteractionSource() }
        ) { 
          tapCount++
          if (tapCount >= 5) {
            // Abrir AdminActivity
            val intent = Intent(context, AdminActivity::class.java)
            context.startActivity(intent)
            tapCount = 0
          }
        },
      tint = MaterialTheme.colorScheme.primary
    )
    
    Spacer(Modifier.height(16.dp))
    
    Text(
      text = "MaxiPTV",
      fontSize = 32.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    
    Text(
      text = "Sistema IPTV Premium",
      fontSize = 14.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    Spacer(Modifier.height(48.dp))
    
    // Campo Código (para login automático)
    OutlinedTextField(
      value = code,
      onValueChange = { code = it; errorMessage = "" },
      label = { Text("Código de 4 dígitos (opcional)") },
      leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
      placeholder = { Text("1234") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      enabled = !isLoading,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    
    Text(
      text = "ou",
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(vertical = 8.dp)
    )
    
    // Campo Usuário
    OutlinedTextField(
      value = username,
      onValueChange = { username = it; errorMessage = "" },
      label = { Text("Usuário") },
      leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      enabled = !isLoading
    )
    
    Spacer(Modifier.height(16.dp))
    
    // Campo Senha
    OutlinedTextField(
      value = password,
      onValueChange = { password = it; errorMessage = "" },
      label = { Text("Senha") },
      leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
      trailingIcon = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
          Icon(
            imageVector = if (passwordVisible) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha"
          )
        }
      },
      visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      enabled = !isLoading
    )
    
    Spacer(Modifier.height(8.dp))
    
    // Mensagem de erro
    if (errorMessage.isNotEmpty()) {
      Text(
        text = errorMessage,
        color = MaterialTheme.colorScheme.error,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 8.dp)
      )
    }
    
    Spacer(Modifier.height(24.dp))
    
    // Botão Entrar
    Button(
      onClick = {
        isLoading = true
        errorMessage = ""
        
        scope.launch {
          try {
            // Se tem código de 4 dígitos, buscar credenciais no PHP
            if (code.length == 4 && code.all { it.isDigit() }) {
              android.util.Log.i("LoginScreen", "🔑 Buscando credenciais do código: $code")
              
              val url = "https://maxiptv-update-1.onrender.com/?code=$code"
              val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
              connection.requestMethod = "GET"
              connection.connect()
              
              if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                
                if (json.getString("status") == "ok") {
                  val user = json.getString("usuario")
                  val pass = json.getString("senha")
                  val api = json.getString("api")
                  
                  android.util.Log.i("LoginScreen", "✅ Credenciais recebidas: $user")
                  
                  // Usar credenciais recebidas para login
                  username = user
                  password = pass
                  doLogin(user, pass, api, onLoginSuccess) { msg -> errorMessage = msg }
                  isLoading = false
                  return@launch
                } else {
                  val mensagem = json.optString("mensagem", "Código inválido")
                  errorMessage = mensagem
                  android.util.Log.e("LoginScreen", "❌ Erro no código: $mensagem")
                  isLoading = false
                  return@launch
                }
              } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Erro ao conectar"
                android.util.Log.e("LoginScreen", "❌ Erro HTTP ${connection.responseCode}: $errorText")
                errorMessage = errorText
                isLoading = false
                return@launch
              }
            }
            
            // Login manual com usuário e senha
            if (username.isBlank() || password.isBlank()) {
              errorMessage = "Preencha usuário e senha ou digite o código"
              isLoading = false
              return@launch
            }
            
            // Verificar credenciais no JSONBin
            val globalUser = SessionManager.validateUser(username, password)
            if (globalUser == null) {
              errorMessage = "Usuário ou senha incorretos"
              isLoading = false
              return@launch
            }
            
            doLogin(username, password, globalUser.apiUrl, onLoginSuccess) { msg -> errorMessage = msg }
            isLoading = false
            
          } catch (e: Exception) {
            android.util.Log.e("LoginScreen", "❌ Erro: ${e.message}", e)
            errorMessage = "Erro ao conectar: ${e.message}"
            isLoading = false
          }
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      enabled = !isLoading
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(24.dp),
          color = Color.White
        )
      } else {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("ENTRAR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
    
    Spacer(Modifier.height(24.dp))
    
    Text(
      text = "Acesso restrito a usuários autorizados",
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

// Função auxiliar para fazer login
suspend fun doLogin(user: String, pass: String, api: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
  // 1. Criar/atualizar usuário local
  val localUser = UserAccount(
    id = java.util.UUID.randomUUID().toString(),
    username = user,
    password = pass,
    apiUrl = api,
    expiryDate = ""
  )
  UserManager.addUser(localUser)
  UserManager.setCurrentUser(localUser)
  
  // 2. Verificar sessão global (bloqueio multi-dispositivo)
  val deviceId = UserManager.getDeviceId()
  val deviceName = UserManager.getDeviceName()
  val (sessionSuccess, sessionMessage) = SessionManager.tryLogin(user, deviceId, deviceName)
  
  if (sessionSuccess) {
    onSuccess()
  } else {
    UserManager.logout()
    onError(sessionMessage)
  }
}


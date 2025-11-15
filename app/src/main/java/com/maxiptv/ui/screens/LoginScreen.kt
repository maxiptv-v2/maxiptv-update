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
import com.maxiptv.MaxiApp
import com.maxiptv.utils.DateUtils
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
  
  // 📱 Detectar se é smartphone para ajustar layout
  val isPhone = MaxiApp.isPhone
  
  // 🛡️ PROTEÇÃO MÁXIMA: Não criar NENHUM usuário automaticamente
  // Todos os usuários devem ser adicionados APENAS pelo painel admin (5 toques no logo)
  // Isso garante que o JSONBin NUNCA será sobrescrito e os usuários cadastrados são preservados
  
  // 🔄 LOGIN AUTOMÁTICO se vier do downloader com código
  LaunchedEffect(hasInitialCredentials) {
    android.util.Log.i("LoginScreen", "🔐 LoginScreen carregada")
    
    // Se veio código via Intent do downloader, buscar credenciais automaticamente
    if (initialCode.isNotBlank() && initialCode.length >= 3 && initialCode.length <= 10 && initialCode.all { it.isLetterOrDigit() }) {
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
          
          // auto_login.php retorna: { user, password, api, expiryDate }
          val user = json.optString("user", "")
          val pass = json.optString("password", "")
          val api = json.optString("api", "")
          val expiryDate = json.optString("expiryDate", "")
          
          // Verificar se recebeu todos os campos necessários
          if (user.isNotBlank() && pass.isNotBlank() && api.isNotBlank()) {
            // Validar data de expiração antes de fazer login
            if (expiryDate.isNotBlank() && DateUtils.isExpired(expiryDate)) {
              android.util.Log.e("LoginScreen", "❌ Usuário expirado: $expiryDate")
              errorMessage = "Usuário expirado. Data de validade: $expiryDate"
              isLoading = false
              return@LaunchedEffect
            }
            
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
            errorMessage = "Campos incompletos na resposta do servidor"
            android.util.Log.e("LoginScreen", "❌ Erro: campos vazios (user=$user, pass=${pass.isNotBlank()}, api=$api)")
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
      // LoginScreen NÃO tenta buscar código pendente
      // Isso é feito apenas pelo HomeNav para evitar duplicação
      // (get-pending-code.php remove o código após retornar - one-time use)
      android.util.Log.i("LoginScreen", "ℹ️ Login manual - aguardando entrada do usuário")
    }
  }
  
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(
        horizontal = if (isPhone) 16.dp else 24.dp,
        vertical = if (isPhone) 16.dp else 24.dp
      ),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // Logo e Título
    Icon(
      imageVector = Icons.Default.PlayArrow,
      contentDescription = "MaxiPTV Logo",
      modifier = Modifier
        .size(if (isPhone) 80.dp else 120.dp)
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
    
    Spacer(Modifier.height(if (isPhone) 12.dp else 16.dp))
    
    Text(
      text = "MaxiPTV",
      fontSize = if (isPhone) 24.sp else 32.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    
    Text(
      text = "Sistema IPTV Premium",
      fontSize = if (isPhone) 12.sp else 14.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    Spacer(Modifier.height(if (isPhone) 32.dp else 48.dp))
    
    // Campo Código (para login automático)
    OutlinedTextField(
      value = code,
      onValueChange = { code = it; errorMessage = "" },
      label = { Text("Código (3-10 caracteres, opcional)") },
      leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
      placeholder = { Text("6789") },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
      enabled = !isLoading,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
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
    
    Spacer(Modifier.height(if (isPhone) 12.dp else 16.dp))
    
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
    
    Spacer(Modifier.height(if (isPhone) 8.dp else 8.dp))
    
    // Mensagem de erro
    if (errorMessage.isNotEmpty()) {
      Text(
        text = errorMessage,
        color = MaterialTheme.colorScheme.error,
        fontSize = if (isPhone) 12.sp else 14.sp,
        modifier = Modifier.padding(vertical = if (isPhone) 4.dp else 8.dp)
      )
    }
    
    Spacer(Modifier.height(if (isPhone) 16.dp else 24.dp))
    
    // Botão Entrar
    Button(
      onClick = {
        isLoading = true
        errorMessage = ""
        
        scope.launch {
          try {
            // Se tem código (3-10 caracteres alfanuméricos), buscar credenciais no PHP
            if (code.isNotBlank() && code.length >= 3 && code.length <= 10 && code.all { it.isLetterOrDigit() }) {
              android.util.Log.i("LoginScreen", "🔑 Buscando credenciais do código: $code")
              
              // Usar sempre auto_login.php para consistência
              val url = "https://maxiptv-update-1.onrender.com/auto_login.php?code=$code"
              val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
              connection.requestMethod = "GET"
              connection.connect()
              
              if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                
                // auto_login.php retorna: { user, password, api, expiryDate }
                val user = json.optString("user", "")
                val pass = json.optString("password", "")
                val api = json.optString("api", "")
                val expiryDate = json.optString("expiryDate", "")
                
                // Verificar se recebeu todos os campos necessários
                if (user.isNotBlank() && pass.isNotBlank() && api.isNotBlank()) {
                  // Validar data de expiração antes de fazer login
                  if (expiryDate.isNotBlank() && DateUtils.isExpired(expiryDate)) {
                    android.util.Log.e("LoginScreen", "❌ Usuário expirado: $expiryDate")
                    errorMessage = "Usuário expirado. Data de validade: $expiryDate"
                    isLoading = false
                    return@launch
                  }
                  
                  android.util.Log.i("LoginScreen", "✅ Credenciais recebidas: $user")
                  
                  // Usar credenciais recebidas para login
                  username = user
                  password = pass
                  doLogin(user, pass, api, onLoginSuccess) { msg -> errorMessage = msg }
                  isLoading = false
                  return@launch
                } else {
                  errorMessage = "Campos incompletos na resposta do servidor"
                  android.util.Log.e("LoginScreen", "❌ Erro: campos vazios ou inválidos")
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
        .height(if (isPhone) 50.dp else 56.dp),
      enabled = !isLoading
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          modifier = Modifier.size(if (isPhone) 20.dp else 24.dp),
          color = Color.White
        )
      } else {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(if (isPhone) 18.dp else 20.dp))
        Spacer(Modifier.width(if (isPhone) 6.dp else 8.dp))
        Text("ENTRAR", fontSize = if (isPhone) 14.sp else 16.sp, fontWeight = FontWeight.Bold)
      }
    }
    
    Spacer(Modifier.height(if (isPhone) 16.dp else 24.dp))
    
    Text(
      text = "Acesso restrito a usuários autorizados",
      fontSize = if (isPhone) 10.sp else 12.sp,
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
  
  // 2. Configurar XRepo ANTES de verificar sessão (importante para buscar canais)
  android.util.Log.d("LoginScreen", "⚙️ Configurando XRepo com API: $api")
  com.maxiptv.data.XRepo.configure(api, user, pass)
  android.util.Log.d("LoginScreen", "✅ XRepo configurado")
  
  // 3. Salvar credenciais no SettingsRepo
  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    android.util.Log.d("LoginScreen", "💾 Salvando credenciais no SettingsRepo...")
    com.maxiptv.data.SettingsRepo.save(
      b = api,
      u = user,
      p = pass,
      e = ""
    )
    android.util.Log.d("LoginScreen", "✅ Credenciais salvas")
  }
  
  // 4. Verificar sessão global (bloqueio multi-dispositivo)
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

// Função isExpired movida para DateUtils para evitar duplicação


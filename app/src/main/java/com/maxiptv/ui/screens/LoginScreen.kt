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
fun LoginScreen(onLoginSuccess: () -> Unit) {
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var tapCount by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  
  // Criar usuário padrão se não existir (apenas criar, não fazer login automático)
  LaunchedEffect(Unit) {
    try {
      // Verificar se usuário padrão existe no JSONBin
      val globalUsers = SessionManager.getAllUsers()
      if (globalUsers.isEmpty()) {
        // Primeira vez - criar usuário padrão no JSONBin
        val defaultGlobalUser = com.maxiptv.data.GlobalUser(
          id = "default",
          username = "max",
          password = "1h2yd90",
          apiUrl = "https://canais.is/player_api.php",
          expiryDate = "31/12/2030"
        )
        SessionManager.saveUser(defaultGlobalUser)
      }
      
      // Criar localmente também se não existir
      val localUsers = UserManager.getUsers()
      if (localUsers.isEmpty()) {
        val defaultUser = UserAccount(
          id = "default",
          username = "max",
          password = "1h2yd90",
          apiUrl = "https://canais.is/player_api.php",
          expiryDate = "31/12/2030"
        )
        UserManager.addUser(defaultUser)
      }
      // NÃO fazer login automático - usuário precisa digitar
    } catch (e: Exception) {
      // Ignora erros na primeira inicialização
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
        if (username.isBlank() || password.isBlank()) {
          errorMessage = "Preencha usuário e senha"
          return@Button
        }
        
        isLoading = true
        errorMessage = ""
        scope.launch {
          // 1. Verificar credenciais no JSONBin (usuários globais)
          val globalUser = SessionManager.validateUser(username, password)
          if (globalUser == null) {
            errorMessage = "Usuário ou senha incorretos"
            isLoading = false
            return@launch
          }
          
          // 2. Criar/atualizar usuário local com dados do global
          val localUser = UserAccount(
            id = globalUser.id,
            username = globalUser.username,
            password = globalUser.password,
            apiUrl = globalUser.apiUrl,
            expiryDate = globalUser.expiryDate
          )
          UserManager.addUser(localUser)
          UserManager.setCurrentUser(localUser)
          
          // 3. Verificar sessão global no SessionManager (bloqueio multi-dispositivo)
          val deviceId = UserManager.getDeviceId()
          val deviceName = UserManager.getDeviceName()
          val (sessionSuccess, sessionMessage) = SessionManager.tryLogin(username, deviceId, deviceName)
          
          if (sessionSuccess) {
            onLoginSuccess()
          } else {
            // Reverter login local se sessão global falhou
            UserManager.logout()
            errorMessage = sessionMessage
          }
          isLoading = false
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


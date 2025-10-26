package com.maxiptv.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxiptv.data.UserAccount
import com.maxiptv.data.UserManager
import com.maxiptv.data.SessionManager
import com.maxiptv.data.ActiveSession
import com.maxiptv.data.ClientCodeManager
import com.maxiptv.ui.theme.MaxiTheme
import kotlinx.coroutines.launch
import java.util.UUID

class AdminActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaxiTheme {
        AdminPanelScreen(onClose = { finish() })
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onClose: () -> Unit) {
  var isAuthenticated by remember { mutableStateOf(false) }
  var passwordInput by remember { mutableStateOf("") }
  var users by remember { mutableStateOf<List<UserAccount>>(emptyList()) }
  var showAddDialog by remember { mutableStateOf(false) }
  var editingUser by remember { mutableStateOf<UserAccount?>(null) }
  var showCodeDialog by remember { mutableStateOf(false) }
  var generatedCode by remember { mutableStateOf("") }
  var codeUsername by remember { mutableStateOf("") }
  val scope = rememberCoroutineScope()
  
  var activeUsers by remember { mutableStateOf<List<UserAccount>>(emptyList()) }
  var globalSessions by remember { mutableStateOf<List<ActiveSession>>(emptyList()) }
  var globalUsers by remember { mutableStateOf<List<com.maxiptv.data.GlobalUser>>(emptyList()) }
  
  // Função para mostrar diálogo do código
  fun showCodeDialog(code: String, username: String) {
    generatedCode = code
    codeUsername = username
    showCodeDialog = true
  }
  
  // Função para mostrar diálogo de revogação
  fun showRevokeDialog(count: Int, username: String) {
    generatedCode = "$count códigos revogados"
    codeUsername = username
    showCodeDialog = true
  }
  
  LaunchedEffect(isAuthenticated) {
    if (isAuthenticated) {
      android.util.Log.i("AdminActivity", "🔍 Carregando usuários...")
      
      // Sincronizar usuários do JSONBin automaticamente ao abrir
      scope.launch {
        try {
          android.util.Log.i("AdminActivity", "🔄 Sincronizando usuários do JSONBin...")
          val jsonBinUsers = SessionManager.getAllUsers()
          
          if (jsonBinUsers.isNotEmpty()) {
            jsonBinUsers.forEach { globalUser ->
              val existingUser = UserManager.getUsers().find { it.username == globalUser.username }
              
              if (existingUser == null) {
                android.util.Log.i("AdminActivity", "➕ Adicionando usuário: ${globalUser.username}")
                val newUser = UserAccount(
                  id = globalUser.id,
                  username = globalUser.username,
                  password = globalUser.password,
                  apiUrl = globalUser.apiUrl,
                  expiryDate = globalUser.expiryDate
                )
                UserManager.addUser(newUser)
              }
            }
            
            android.util.Log.i("AdminActivity", "✅ Sincronização concluída!")
          }
        } catch (e: Exception) {
          android.util.Log.e("AdminActivity", "❌ Erro na sincronização: ${e.message}", e)
        }
      }
      
      // Forçar recarregamento completo
      users = UserManager.getUsers()
      activeUsers = UserManager.getActiveUsers()
      globalSessions = SessionManager.getAllActiveSessions()
      globalUsers = SessionManager.getAllUsers()
      
      android.util.Log.i("AdminActivity", "📊 Usuários carregados:")
      android.util.Log.i("AdminActivity", "  - Locais: ${users.size}")
      android.util.Log.i("AdminActivity", "  - Ativos: ${activeUsers.size}")
      android.util.Log.i("AdminActivity", "  - Globais: ${globalUsers.size}")
      
      // Debug detalhado dos usuários
      users.forEachIndexed { index, user ->
        android.util.Log.i("AdminActivity", "  Usuário $index: ${user.username} (ID: ${user.id})")
      }
    }
  }
  
  // Sincronizar usuários do JSONBin automaticamente
  LaunchedEffect(globalUsers) {
    if (globalUsers.isNotEmpty()) {
      val missingUsers = globalUsers.filter { globalUser ->
        !users.any { localUser -> localUser.username == globalUser.username }
      }
      
      if (missingUsers.isNotEmpty()) {
        android.util.Log.i("AdminActivity", "🔄 Sincronizando ${missingUsers.size} usuários do JSONBin...")
        
        missingUsers.forEach { globalUser ->
          val localUser = UserAccount(
            id = globalUser.id,
            username = globalUser.username,
            password = globalUser.password,
            apiUrl = globalUser.apiUrl,
            expiryDate = globalUser.expiryDate
          )
          UserManager.addUser(localUser)
          android.util.Log.i("AdminActivity", "✅ Sincronizado: ${globalUser.username}")
        }
        
        // Atualizar lista local
        users = UserManager.getUsers()
        android.util.Log.i("AdminActivity", "✅ Sincronização concluída! Total: ${users.size} usuários")
      }
    }
  }
  
  Box(Modifier.fillMaxSize()) {
    // Background com gradiente
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF0D1117),
              Color(0xFF161B22),
              Color(0xFF21262D)
            )
          )
        )
    )
    
    if (!isAuthenticated) {
      // Tela de Login Admin - Design Profissional
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Card de Login
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .shadow(
              elevation = 24.dp,
              spotColor = Color(0xFF00D4FF),
              ambientColor = Color(0xFF00D4FF)
            )
            .border(
              width = 2.dp,
              brush = Brush.linearGradient(
                colors = listOf(
                  Color(0xFF00D4FF),
                  Color(0xFF0099CC),
                  Color(0xFF006699)
                )
              ),
              shape = RoundedCornerShape(16.dp)
            ),
          colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
          ),
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Ícone Admin
            Box(
              modifier = Modifier
                .size(80.dp)
                .background(
                  Brush.radialGradient(
                    colors = listOf(
                      Color(0xFF00D4FF),
                      Color(0xFF0099CC)
                    )
                  ),
                  RoundedCornerShape(40.dp)
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White
              )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
              text = "ADMINISTRAÇÃO",
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00D4FF)
            )
            
            Text(
              text = "Acesso Restrito",
              fontSize = 16.sp,
              color = Color(0xFF888888),
              modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Campo Senha
            Column {
              Text(
                "Senha de Administrador",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF00D4FF),
                modifier = Modifier.padding(bottom = 12.dp)
              )
              
              BasicTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                textStyle = TextStyle(fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(Color(0xFF00D4FF)),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFF2D2D2D))
                  .border(
                    2.dp,
                    if (passwordInput.isNotEmpty()) Color(0xFF00D4FF) else Color(0xFF444444),
                    RoundedCornerShape(12.dp)
                  )
                  .padding(16.dp)
              )
              
              if (passwordInput.isNotEmpty()) {
                Text(
                  "✓ Digitado: $passwordInput",
                  color = Color(0xFF00FF88),
                  fontSize = 12.sp,
                  modifier = Modifier.padding(top = 8.dp)
                )
              }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Botões
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                  contentColor = Color(0xFF888888)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444))
              ) {
                Text("Cancelar", fontWeight = FontWeight.Medium)
              }
              
              Button(
                onClick = {
                  if (passwordInput == "201015") {
                    isAuthenticated = true
                    passwordInput = ""
                  }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF00D4FF),
                  contentColor = Color.White
                )
              ) {
                Text("Acessar", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    } else {
      // Painel Admin Autenticado - Design Profissional
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Header Profissional
          item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(elevation = 8.dp),
            colors = CardDefaults.cardColors(
              containerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(16.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF00D4FF),
                    modifier = Modifier.size(28.dp)
                  )
                  Spacer(Modifier.width(12.dp))
                  Text(
                    text = "PAINEL ADMINISTRATIVO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D4FF)
                  )
                }
                
                Text(
                  text = "Gerenciamento de Usuários e Sessões",
                  fontSize = 14.sp,
                  color = Color(0xFF888888),
                  modifier = Modifier.padding(top = 4.dp)
                )
              }
              
              Row(
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Estatísticas
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(end = 16.dp)
                ) {
                  Text(
                    text = "${users.size}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = "Total",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                  )
                }
                
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(end = 16.dp)
                ) {
                  Text(
                    text = "${activeUsers.size}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF88)
                  )
                  Text(
                    text = "Ativos",
                    fontSize = 12.sp,
                    color = Color(0xFF888888)
                  )
                }
                
                // Botão Voltar
                IconButton(
                  onClick = onClose,
                  modifier = Modifier
                    .background(
                      Color(0xFF2D2D2D),
                      RoundedCornerShape(12.dp)
                    )
                    .size(48.dp)
                ) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = Color(0xFF888888)
                  )
                }
              }
            }
          }
          }
          
          // Seção de Configurações Atuais
          item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Column(
              modifier = Modifier.padding(16.dp)
            ) {
              Text(
                text = "Configurações Atuais",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
              )
              
              var currentUser by remember { mutableStateOf<UserAccount?>(null) }
              
              LaunchedEffect(Unit) {
                currentUser = UserManager.getCurrentUser()
              }
              
              currentUser?.let { user ->
                // Usuário
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(Modifier.width(8.dp))
                  Column {
                    Text("Usuário", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user.username, fontSize = 16.sp)
                  }
                }
                
                // API URL
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(Modifier.width(8.dp))
                  Column {
                    Text("API URL", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user.apiUrl, fontSize = 16.sp, maxLines = 1)
                  }
                }
                
                // Validade
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(Modifier.width(8.dp))
                  Column {
                    Text("Validade", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user.expiryDate, fontSize = 16.sp)
                  }
                }
              }
            }
          }
          }
          
          // Seção de Sessões Globais Ativas
          if (globalSessions.isNotEmpty()) {
            item {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 16.dp)
            ) {
              Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF00FF88),
                modifier = Modifier.size(24.dp)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "SESSÕES ATIVAS GLOBAIS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF88)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "(${globalSessions.size})",
                fontSize = 16.sp,
                color = Color(0xFF888888)
              )
            }
            }
            
            items(globalSessions) { session ->
              GlobalSessionCard(
                session = session,
                onForceLogout = {
                  scope.launch {
                    SessionManager.forceLogout(session.username)
                    globalSessions = SessionManager.getAllActiveSessions()
                    users = UserManager.getUsers()
                    activeUsers = UserManager.getActiveUsers()
                  }
                }
              )
            }
          }
          
          // Seção de Usuários Ativos (Locais - manter para compatibilidade)
          if (activeUsers.isNotEmpty()) {
            item {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 16.dp)
            ) {
              Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF4488FF),
                modifier = Modifier.size(24.dp)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "USUÁRIOS LOCAIS ATIVOS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4488FF)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "(${activeUsers.size})",
                fontSize = 16.sp,
                color = Color(0xFF888888)
              )
            }
            }
            
            items(activeUsers) { user ->
              ActiveUserCard(
                user = user,
                onEdit = { editingUser = user },
                onForceLogout = {
                  scope.launch {
                    UserManager.forceLogout(user.id)
                    users = UserManager.getUsers()
                    activeUsers = UserManager.getActiveUsers()
                  }
                }
              )
            }
          }
          
          // Lista de TODOS os Usuários Cadastrados
          item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
          ) {
            Icon(
              Icons.AutoMirrored.Filled.List,
              contentDescription = null,
              tint = Color(0xFF00D4FF),
              modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = "TODOS OS USUÁRIOS",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00D4FF)
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = "(${users.size})",
              fontSize = 16.sp,
              color = Color(0xFF888888)
            )
            
            Spacer(Modifier.weight(1f))
            
            // Botão para recarregar usuários
            Button(
              onClick = {
                scope.launch {
                  android.util.Log.i("AdminActivity", "🔄 Recarregando usuários...")
                  users = UserManager.getUsers()
                  activeUsers = UserManager.getActiveUsers()
                  globalUsers = SessionManager.getAllUsers()
                  android.util.Log.i("AdminActivity", "✅ Usuários recarregados: ${users.size}")
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
              modifier = Modifier.padding(start = 8.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Recarregar", fontSize = 12.sp)
            }
          }
          }
          
          if (users.isEmpty()) {
            item {
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = Color(0xFF2D2D2D)
                )
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(48.dp)
                  )
                  Spacer(Modifier.height(16.dp))
                  Text(
                    text = "Nenhum usuário encontrado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF888888)
                  )
                  Spacer(Modifier.height(8.dp))
                  Text(
                    text = "Clique em 'Adicionar Novo Usuário' para começar",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            }
          } else {
            items(users) { user ->
              android.util.Log.d("AdminActivity", "🎨 Renderizando UserCard para: ${user.username}")
              UserCard(
                user = user,
                onEdit = { editingUser = user },
                onDelete = {
                  scope.launch {
                    // Remover localmente
                    UserManager.removeUser(user.id)
                    
                    // Remover globalmente do JSONBin
                    SessionManager.deleteUser(user.id)
                    
                    // Atualizar lista
                    users = UserManager.getUsers()
                    activeUsers = UserManager.getActiveUsers()
                  }
                },
                onGenerateCode = {
                  scope.launch {
                    try {
                      android.util.Log.i("AdminActivity", "🔍 Buscando código existente para ${user.username}")
                      
                      // Verificar se já existe código para este usuário
                      val allCodes = ClientCodeManager.getAllSimpleCodes()
                      val userCodes = allCodes.filter { (_, code) -> code.usuario == user.username }
                      
                      val code = if (userCodes.isNotEmpty()) {
                        // Usar código existente
                        val existingCode = userCodes.keys.first()
                        android.util.Log.i("AdminActivity", "✅ Código existente encontrado: $existingCode")
                        existingCode
                      } else {
                        // Gerar novo código se não existir
                        android.util.Log.i("AdminActivity", "🔑 Gerando novo código para ${user.username}")
                        val newCode = ClientCodeManager.createSimpleCode(user)
                        android.util.Log.i("AdminActivity", "✅ Novo código gerado: $newCode")
                        newCode
                      }
                      
                      showCodeDialog(code, user.username)
                    } catch (e: Exception) {
                      android.util.Log.e("AdminActivity", "❌ Erro ao gerar código simples: ${e.message}", e)
                    }
                  }
                },
                onRevokeCode = {
                  scope.launch {
                    try {
                      android.util.Log.i("AdminActivity", "🚫 Revogando códigos para ${user.username}")
                      // Buscar todos os códigos do usuário e revogar
                      val allCodes = ClientCodeManager.getAllSimpleCodes()
                      val userCodes = allCodes.filter { (_, code) -> code.usuario == user.username }
                      
                      userCodes.forEach { (code, _) ->
                        ClientCodeManager.removeSimpleCode(code)
                        android.util.Log.i("AdminActivity", "✅ Código $code revogado para ${user.username}")
                      }
                      
                      if (userCodes.isNotEmpty()) {
                        showRevokeDialog(userCodes.size, user.username)
                      } else {
                        android.util.Log.w("AdminActivity", "⚠️ Nenhum código encontrado para ${user.username}")
                      }
                    } catch (e: Exception) {
                      android.util.Log.e("AdminActivity", "❌ Erro ao revogar códigos: ${e.message}", e)
                    }
                  }
                }
              )
            }
          }
          
          // Botão Adicionar Usuário - Profissional
          item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .shadow(elevation = 8.dp),
            colors = CardDefaults.cardColors(
              containerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(16.dp)
          ) {
            Button(
              onClick = { showAddDialog = true },
              modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
              )
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .background(
                      Brush.radialGradient(
                        colors = listOf(
                          Color(0xFF00D4FF),
                          Color(0xFF0099CC)
                        )
                      ),
                      RoundedCornerShape(24.dp)
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column {
                  Text(
                    text = "Adicionar Novo Usuário",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Text(
                    text = "Criar conta com API e validade",
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
                  )
                }
                
                Spacer(Modifier.weight(1f))
                
                Icon(
                  Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = null,
                  tint = Color(0xFF00D4FF),
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          }
          }
        }
      
      // Dialog para adicionar/editar usuário
      if (showAddDialog || editingUser != null) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
          contentAlignment = Alignment.Center
        ) {
          UserDialog(
            user = editingUser,
            onDismiss = {
              showAddDialog = false
              editingUser = null
            },
            onSave = { user ->
              scope.launch {
                android.util.Log.i("AdminActivity", "💾 Salvando usuário: ${user.username}")
                
                // Salvar localmente
                UserManager.addUser(user)
                android.util.Log.i("AdminActivity", "✅ Salvo localmente")
                
                // Salvar globalmente no JSONBin
                val globalUser = com.maxiptv.data.GlobalUser(
                  id = user.id,
                  username = user.username,
                  password = user.password,
                  apiUrl = user.apiUrl,
                  expiryDate = user.expiryDate
                )
                val saved = SessionManager.saveUser(globalUser)
                if (saved) {
                  android.util.Log.i("AdminActivity", "✅ Salvo globalmente no JSONBin")
                } else {
                  android.util.Log.e("AdminActivity", "❌ ERRO ao salvar no JSONBin")
                }
                
                // Atualizar lista
                users = UserManager.getUsers()
                activeUsers = UserManager.getActiveUsers()
                showAddDialog = false
                editingUser = null
              }
            }
          )
        }
      }
      
      // Dialog para mostrar código gerado
      if (showCodeDialog) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
          contentAlignment = Alignment.Center
        ) {
          Card(
            modifier = Modifier
              .fillMaxWidth(0.9f)
              .shadow(elevation = 24.dp),
            colors = CardDefaults.cardColors(
              containerColor = Color(0xFF1A1A1A)
            ),
            shape = RoundedCornerShape(20.dp)
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              // Header
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .background(
                      Brush.radialGradient(
                        colors = listOf(
                          Color(0xFF00D4FF),
                          Color(0xFF0099CC)
                        )
                      ),
                      RoundedCornerShape(24.dp)
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column {
                  Text(
                    text = "CÓDIGO GERADO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D4FF)
                  )
                  Text(
                    text = "Para: $codeUsername",
                    fontSize = 14.sp,
                    color = Color(0xFF888888)
                  )
                }
                
                Spacer(Modifier.weight(1f))
                
                IconButton(
                  onClick = { showCodeDialog = false },
                  modifier = Modifier
                    .background(Color(0xFF2D2D2D), RoundedCornerShape(12.dp))
                    .size(40.dp)
                ) {
                  Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF888888))
                }
              }
              
              // Código
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                  containerColor = Color(0xFF2D2D2D)
                ),
                shape = RoundedCornerShape(12.dp)
              ) {
                Column(
                  modifier = Modifier.padding(20.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = "Código PHP:",
                    fontSize = 14.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(bottom = 8.dp)
                  )
                  
                  Text(
                    text = generatedCode,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D4FF),
                    modifier = Modifier
                      .background(
                        Color(0xFF1A1A1A),
                        RoundedCornerShape(8.dp)
                      )
                      .padding(12.dp)
                  )
                  
                  Text(
                    text = "⏰ Válido por 6 horas",
                    fontSize = 12.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(top = 8.dp)
                  )
                }
              }
              
              // Instruções
              Text(
                text = "📋 Instruções:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
              )
              
              Text(
                text = "1. Cliente acessa o downloader\n2. Digita este código\n3. Baixa o app automaticamente\n4. Login automático",
                fontSize = 14.sp,
                color = Color(0xFFBBBBBB),
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 24.dp)
              )
              
              // Botões
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                OutlinedButton(
                  onClick = { showCodeDialog = false },
                  modifier = Modifier.weight(1f),
                  colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF888888)
                  ),
                  border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF444444))
                ) {
                  Text("Fechar", fontWeight = FontWeight.Medium)
                }
                
                Button(
                  onClick = {
                    // Copiar código para clipboard
                    showCodeDialog = false
                  },
                  modifier = Modifier.weight(1f),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D4FF),
                    contentColor = Color.White
                  )
                ) {
                  Text("Copiar", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ActiveUserCard(user: UserAccount, onEdit: () -> Unit, onForceLogout: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Person,
              contentDescription = null,
              tint = Color(0xFF00FF00),
              modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = user.username,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          
          Spacer(Modifier.height(4.dp))
          
          Text(
            text = "🖥️ ${user.activeDeviceName ?: "Dispositivo"}",
            fontSize = 14.sp,
            color = Color(0xFFBBBBBB)
          )
          
          val lastLogin = user.lastLoginTime?.let {
            val diff = System.currentTimeMillis() - it
            val minutes = (diff / (1000 * 60)).toInt()
            when {
              minutes < 1 -> "Agora"
              minutes < 60 -> "$minutes min atrás"
              else -> "${minutes / 60}h atrás"
            }
          } ?: "Desconhecido"
          
          Text(
            text = "⏰ Login: $lastLogin",
            fontSize = 12.sp,
            color = Color(0xFF999999)
          )
        }
        
        Row {
          Button(
            onClick = onEdit,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Editar", fontSize = 12.sp)
          }
          
          Button(
            onClick = onForceLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
          ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Deslogar", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

@Composable
fun UserCard(user: UserAccount, onEdit: () -> Unit, onDelete: () -> Unit, onGenerateCode: () -> Unit, onRevokeCode: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      // Nome do usuário
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.Person,
          contentDescription = null,
          tint = Color(0xFF00D4FF),
          modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = user.username,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
      
      Spacer(Modifier.height(8.dp))
      
      // Informações do usuário
      Text(
        text = "🔑 Senha: ${user.password}",
        fontSize = 14.sp,
        color = Color(0xFFBBBBBB)
      )
      Text(
        text = "📅 Expira: ${user.expiryDate}",
        fontSize = 14.sp,
        color = Color(0xFFBBBBBB)
      )
      Text(
        text = "🌐 API: ${user.apiUrl}",
        fontSize = 12.sp,
        color = Color(0xFF888888),
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
      )
      
      Spacer(Modifier.height(12.dp))
      
      // Botões em Row horizontal
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        Button(
          onClick = onGenerateCode,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D4FF)),
          modifier = Modifier.weight(1f).padding(end = 4.dp)
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Código", fontSize = 11.sp)
        }
        
        Button(
          onClick = onRevokeCode,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Revogar", fontSize = 11.sp)
        }
        
        Button(
          onClick = onEdit,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
          modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Editar", fontSize = 11.sp)
        }
        
        Button(
          onClick = onDelete,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
          modifier = Modifier.weight(1f).padding(start = 4.dp)
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Excluir", fontSize = 11.sp)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDialog(user: UserAccount?, onDismiss: () -> Unit, onSave: (UserAccount) -> Unit) {
  var username by remember { mutableStateOf(user?.username ?: "") }
  var password by remember { mutableStateOf(user?.password ?: "") }
  var apiUrl by remember { mutableStateOf(user?.apiUrl ?: "https://") }
  var expiryDate by remember { mutableStateOf(user?.expiryDate ?: "") }
  
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .fillMaxHeight(0.9f)
      .padding(16.dp)
      .shadow(elevation = 24.dp),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFF1A1A1A)
    ),
    shape = RoundedCornerShape(20.dp)
  ) {
    LazyColumn(
      modifier = Modifier.padding(24.dp)
    ) {
      item {
      // Header do Dialog
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 24.dp)
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(
              Brush.radialGradient(
                colors = listOf(
                  Color(0xFF00D4FF),
                  Color(0xFF0099CC)
                )
              ),
              RoundedCornerShape(24.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            if (user == null) Icons.Default.Add else Icons.Default.Edit,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
          )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column {
          Text(
            text = if (user == null) "ADICIONAR USUÁRIO" else "EDITAR USUÁRIO",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D4FF)
          )
          Text(
            text = if (user == null) "Criar nova conta de acesso" else "Modificar dados do usuário",
            fontSize = 14.sp,
            color = Color(0xFF888888)
          )
        }
        
        Spacer(Modifier.weight(1f))
        
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .background(Color(0xFF2D2D2D), RoundedCornerShape(12.dp))
            .size(40.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color(0xFF888888))
        }
      }
      }
      
      // Campos do formulário
      item {
        Column(
          verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        // Campo Usuário
        Column {
          Text("Nome de Usuário", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00D4FF), modifier = Modifier.padding(bottom = 8.dp))
          BasicTextField(
            value = username,
            onValueChange = { username = it },
            textStyle = TextStyle(fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(Color(0xFF00D4FF)),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF2D2D2D))
              .border(
                2.dp,
                if (username.isNotEmpty()) Color(0xFF00D4FF) else Color(0xFF444444),
                RoundedCornerShape(12.dp)
              )
              .padding(16.dp)
          )
          if (username.isNotEmpty()) {
            Text("✓ Digitado: $username", color = Color(0xFF00FF88), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
          }
        }
        
        // Campo Senha
        Column {
          Text("Senha de Acesso", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00D4FF), modifier = Modifier.padding(bottom = 8.dp))
          BasicTextField(
            value = password,
            onValueChange = { password = it },
            textStyle = TextStyle(fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(Color(0xFF00D4FF)),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF2D2D2D))
              .border(
                2.dp,
                if (password.isNotEmpty()) Color(0xFF00D4FF) else Color(0xFF444444),
                RoundedCornerShape(12.dp)
              )
              .padding(16.dp)
          )
          if (password.isNotEmpty()) {
            Text("✓ Digitado: $password", color = Color(0xFF00FF88), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
          }
        }
        
        // Campo API URL
        Column {
          Text("URL da API (Xtream Code)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00D4FF), modifier = Modifier.padding(bottom = 8.dp))
          BasicTextField(
            value = apiUrl,
            onValueChange = { apiUrl = it },
            textStyle = TextStyle(fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(Color(0xFF00D4FF)),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF2D2D2D))
              .border(
                2.dp,
                if (apiUrl.isNotEmpty()) Color(0xFF00D4FF) else Color(0xFF444444),
                RoundedCornerShape(12.dp)
              )
              .padding(16.dp)
          )
          if (apiUrl.isNotEmpty()) {
            Text("✓ Digitado: $apiUrl", color = Color(0xFF00FF88), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
          }
        }
        
        // Campo Data
        Column {
          Text("Data de Vencimento (DD/MM/AAAA)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00D4FF), modifier = Modifier.padding(bottom = 8.dp))
          BasicTextField(
            value = expiryDate,
            onValueChange = { expiryDate = it },
            textStyle = TextStyle(fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(Color(0xFF00D4FF)),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF2D2D2D))
              .border(
                2.dp,
                if (expiryDate.isNotEmpty()) Color(0xFF00D4FF) else Color(0xFF444444),
                RoundedCornerShape(12.dp)
              )
              .padding(16.dp)
          )
          if (expiryDate.isNotEmpty()) {
            Text("✓ Digitado: $expiryDate", color = Color(0xFF00FF88), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
          }
        }
        }
      }
      
      item {
        Spacer(Modifier.height(24.dp))
      }
      
      // Botões
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF888888)
          ),
          border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF444444))
        ) {
          Text("Cancelar", fontWeight = FontWeight.Medium)
        }
        
        Button(
          onClick = {
            if (username.isNotBlank() && password.isNotBlank() && apiUrl.isNotBlank()) {
              val newUser = UserAccount(
                id = user?.id ?: UUID.randomUUID().toString(),
                username = username,
                password = password,
                apiUrl = apiUrl,
                expiryDate = expiryDate
              )
              onSave(newUser)
            }
          },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00D4FF),
            contentColor = Color.White
          )
        ) {
          Text("Salvar", fontWeight = FontWeight.Bold)
        }
        }
      }
    }
  }
}


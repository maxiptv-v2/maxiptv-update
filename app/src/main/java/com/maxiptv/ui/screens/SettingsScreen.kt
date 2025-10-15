package com.maxiptv.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.maxiptv.data.SettingsRepo
import com.maxiptv.data.UserManager
import kotlinx.coroutines.launch

@Composable 
fun SettingsScreen(nav: NavHostController) {
  val scope = rememberCoroutineScope()
  var currentUser by remember { mutableStateOf<com.maxiptv.data.UserAccount?>(null) }
  var showEditDialog by remember { mutableStateOf(false) }
  var testingApi by remember { mutableStateOf(false) }
  var testResult by remember { mutableStateOf<String?>(null) }
  
  LaunchedEffect(Unit) {
    currentUser = UserManager.getCurrentUser()
  }
  
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "⚙️ Configurações",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold
    )
    
    Divider()
    
    // Card com informações atuais
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Configurações Atuais",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        
        if (currentUser != null) {
          ListItem(
            headlineContent = { Text("Usuário") },
            supportingContent = { Text(currentUser!!.username) },
            leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
          )
          
          ListItem(
            headlineContent = { Text("API URL") },
            supportingContent = { Text(currentUser!!.apiUrl, maxLines = 2) },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
          )
          
          ListItem(
            headlineContent = { Text("Validade") },
            supportingContent = { Text(currentUser!!.expiryDate.ifBlank { "Sem vencimento" }) },
            leadingContent = { Icon(Icons.Default.DateRange, contentDescription = null) }
          )
        } else {
          Text("Nenhum usuário logado", color = MaterialTheme.colorScheme.error)
        }
      }
    }
    
    // Botões de ação
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = { showEditDialog = true },
        modifier = Modifier.weight(1f),
        enabled = currentUser != null
      ) {
        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Editar")
      }
      
      Button(
        onClick = {
          scope.launch {
            testingApi = true
            testResult = null
            
            val (base, user, pass) = SettingsRepo.loadBlocking()
            val isValid = SettingsRepo.test(base, user, pass)
            
            testResult = if (isValid) "✅ API válida!" else "❌ API inválida!"
            testingApi = false
          }
        },
        modifier = Modifier.weight(1f),
        enabled = !testingApi && currentUser != null
      ) {
        if (testingApi) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("Testar API")
      }
    }
    
    // Resultado do teste
    if (testResult != null) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = if (testResult!!.contains("✅")) 
            MaterialTheme.colorScheme.primaryContainer 
          else 
            MaterialTheme.colorScheme.errorContainer
        )
      ) {
        Text(
          text = testResult!!,
          modifier = Modifier.padding(16.dp),
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
  
  // Dialog de edição
  if (showEditDialog && currentUser != null) {
    UserDialog(
      user = currentUser,
      onDismiss = { showEditDialog = false },
      onSave = { updatedUser ->
        scope.launch {
          UserManager.updateUser(updatedUser)
          UserManager.setCurrentUser(updatedUser)
          currentUser = updatedUser
          showEditDialog = false
        }
      }
    )
  }
}

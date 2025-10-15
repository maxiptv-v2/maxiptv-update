package com.maxiptv.ui.screens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.maxiptv.MaxiApp
import com.maxiptv.data.XRepo
import com.maxiptv.data.LiveStream
import coil.compose.AsyncImage
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog

@Composable
fun LiveScreen(nav: NavHostController) {
  val cats by XRepo.liveCategories.collectAsState(emptyList())
  val streams by XRepo.liveStreams.collectAsState(emptyList())
  var selectedCat by remember { mutableStateOf<String?>(null) }
  var current by remember { mutableStateOf<LiveStream?>(null) }
  
  // ✅ Estados para PIN de categoria adulta
  var showPinDialog by remember { mutableStateOf(false) }
  var pinInput by remember { mutableStateOf("") }
  var showPinError by remember { mutableStateOf(false) }
  var isAdultUnlocked by remember { mutableStateOf(false) }
  var pendingAdultCategory by remember { mutableStateOf<String?>(null) }
  
  LaunchedEffect(Unit) { 
    XRepo.ensureLiveLoaded()
  }
  
  // ✅ Filtrar categorias adultas (buscar por XXX, ADULTO, 18+)
  val adultCategoryIds = listOf("18", "82", "80", "79", "78", "81", "ADULT", "XXX")
  val normalCats = cats.filter { 
    val isAdult = it.category_id in adultCategoryIds || 
                  it.category_name.contains(Regex("(?i)(adult|xxx|18\\+|porn|sex)"))
    !isAdult
  }
  
  // ✅ Adicionar categoria adulta no início
  val categoriesWithAdult = listOf("🔞 ADULTO" to "ADULT") + normalCats.map { it.category_name to it.category_id }
  
  Column(Modifier.fillMaxSize()) {
    CategoryChips(
      categories = categoriesWithAdult, 
      selectedId = selectedCat, 
      onSelect = { catId ->
        if (catId == "ADULT") {
          // ✅ Verificar PIN para categoria adulta
          if (isAdultUnlocked) {
            // Já desbloqueado, mostrar canais adultos
            selectedCat = "ADULT"
          } else {
            // Mostrar dialog de PIN
            pendingAdultCategory = catId
            showPinDialog = true
            pinInput = ""
            showPinError = false
          }
        } else {
          selectedCat = catId
        }
      }
    )
    Row(Modifier.weight(1f)) {
      Surface(tonalElevation = 2.dp, modifier = Modifier.width(380.dp).fillMaxHeight()) {
        val filtered = when {
          selectedCat == "ADULT" && isAdultUnlocked -> {
            // ✅ Mostrar canais adultos quando desbloqueado
            streams.filter { 
              it.category_id in adultCategoryIds || 
              it.name.contains(Regex("(?i)(adult|xxx|18\\+|porn|sex)"))
            }
          }
          selectedCat == null -> streams
          else -> streams.filter { it.category_id == selectedCat }
        }
        val isTv = MaxiApp.isTv
        val headlineSize = if (isTv) 18.sp else 16.sp
        val supportingSize = if (isTv) 14.sp else 12.sp
        val iconSize = if (isTv) 48.dp else 40.dp  // Reduzido para evitar corte
        
        LazyColumn { 
          items(filtered) { s ->
            ListItem(
              headlineContent = { 
                Text(
                  text = s.name,
                  fontSize = headlineSize,
                  fontWeight = FontWeight.SemiBold,
                  fontFamily = FontFamily.SansSerif
                ) 
              }, 
              supportingContent = { 
                Text(
                  text = s.categoryName ?: "-",
                  fontSize = supportingSize,
                  fontFamily = FontFamily.SansSerif
                ) 
              },
              leadingContent = {
                Box(
                  modifier = Modifier
                    .size(iconSize + 8.dp)  // Espaço extra para evitar corte
                    .padding(4.dp),
                  contentAlignment = Alignment.Center
                ) {
                  AsyncImage(
                    model = s.stream_icon,
                    contentDescription = s.name,
                    modifier = Modifier
                      .size(iconSize - 4.dp),  // Ícone um pouco menor que o container
                    contentScale = ContentScale.Inside  // Garante que a imagem inteira seja visível
                  )
                }
              },
              modifier = Modifier
                .clickable { current = s }
                .focusable()
            )
            Divider()
          } 
        }
      }
      Box(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
        PlayerSurface(currentUrl = current?.toLiveUrl())
      }
    }
  }
  
  // ✅ Modal de PIN para categoria adulta
  if (showPinDialog) {
    Dialog(onDismissRequest = { 
      showPinDialog = false 
      pendingAdultCategory = null
    }) {
      Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp,
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Cadeado",
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFFF6B6B)
          )
          
          Text(
            text = "🔞 Conteúdo Adulto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6B6B)
          )
          
          Text(
            text = "Digite o PIN para acessar canais adultos:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          OutlinedTextField(
            value = pinInput,
            onValueChange = { pinInput = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = showPinError,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          
          if (showPinError) {
            Text(
              text = "PIN incorreto! Tente novamente.",
              color = Color(0xFFFF5252),
              style = MaterialTheme.typography.bodySmall
            )
          }
          
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { 
                showPinDialog = false
                pendingAdultCategory = null
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Cancelar")
            }
            
            Button(
              onClick = {
                if (pinInput == "0000") {
                  // ✅ PIN correto - desbloquear categoria adulta
                  isAdultUnlocked = true
                  selectedCat = pendingAdultCategory
                  showPinDialog = false
                  pendingAdultCategory = null
                  showPinError = false
                } else {
                  // ❌ PIN incorreto
                  showPinError = true
                  pinInput = ""
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Confirmar")
            }
          }
        }
      }
    }
  }
}

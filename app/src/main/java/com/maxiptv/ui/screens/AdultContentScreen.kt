package com.maxiptv.ui.screens
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.maxiptv.data.XRepo
import coil.compose.AsyncImage

@Composable
fun AdultContentScreen(nav: NavHostController) {
  val vod by XRepo.vodItems.collectAsState(emptyList())
  var isUnlocked by remember { mutableStateOf(false) }
  var pinInput by remember { mutableStateOf("") }
  var showError by remember { mutableStateOf(false) }
  var selectedFilter by remember { mutableStateOf("Todos") }

  LaunchedEffect(Unit) { 
    XRepo.ensureVodLoaded() 
  }

  // Filtrar apenas conteúdo adulto (categorias XXX)
  val adultContent = vod.filter { 
    it.category_id in listOf("18", "82", "80", "79", "78", "81")
  }

  // Definir filtros baseados nas categorias reais
  val filters = listOf(
    "Todos" to adultContent,
    "Brasileirinhas" to adultContent.filter { it.category_id == "78" },
    "OnlyFans" to adultContent.filter { it.category_id == "81" },
    "Sexy Hot" to adultContent.filter { it.category_id == "82" },
    "Buttman" to adultContent.filter { it.category_id == "80" },
    "Sexxy" to adultContent.filter { it.category_id == "79" },
    // Filtros por palavras-chave
    "Asiáticas" to adultContent.filter { 
      it.name.contains(Regex("(?i)(asian|japonesa|oriental|chinese|korean)")) 
    },
    "Lésbicas" to adultContent.filter { 
      it.name.contains(Regex("(?i)(lesbian|lesbica|sapphic)")) 
    },
    "Amador" to adultContent.filter { 
      it.name.contains(Regex("(?i)(amateur|amador|caseiro)")) 
    },
    "Gay" to adultContent.filter { 
      it.name.contains(Regex("(?i)(gay|boys|twink)")) 
    }
  )

  // Modal de PIN
  if (!isUnlocked) {
    Dialog(onDismissRequest = { nav.popBackStack() }) {
      Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
          )
          
          Text(
            text = "🔞 Conteúdo Adulto",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
          )
          
          Text(
            text = "Digite o PIN para acessar",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          OutlinedTextField(
            value = pinInput,
            onValueChange = { 
              if (it.length <= 4) {
                pinInput = it
                showError = false
              }
            },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = showError,
            supportingText = if (showError) { { Text("PIN incorreto!") } } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedButton(
              onClick = { nav.popBackStack() },
              modifier = Modifier.weight(1f)
            ) {
              Text("Cancelar")
            }
            
            Button(
              onClick = {
                if (pinInput == "0000") {
                  isUnlocked = true
                  pinInput = ""
                } else {
                  showError = true
                }
              },
              modifier = Modifier.weight(1f),
              enabled = pinInput.length == 4
            ) {
              Text("Confirmar")
            }
          }
        }
      }
    }
  } else {
    // Conteúdo desbloqueado
    Column(Modifier.fillMaxSize()) {
      // Título
      Text(
        text = "🔞 Conteúdo Adulto",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
      )
      
      // Filtros
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        filters.take(6).forEach { (name, _) ->
          FilterChip(
            selected = selectedFilter == name,
            onClick = { selectedFilter = name },
            label = { Text(name) }
          )
        }
      }
      
      Spacer(Modifier.height(8.dp))
      
      // Segunda linha de filtros
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        filters.drop(6).forEach { (name, _) ->
          FilterChip(
            selected = selectedFilter == name,
            onClick = { selectedFilter = name },
            label = { Text(name) }
          )
        }
      }
      
      Spacer(Modifier.height(16.dp))
      
      // Grade de filmes filtrados
      val filtered = filters.find { it.first == selectedFilter }?.second ?: emptyList()
      
      if (filtered.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Nenhum conteúdo encontrado",
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
          items(filtered) { v ->
            var isFocused by remember { mutableStateOf(false) }
            
            Card(
              onClick = { nav.navigate("vod/${v.stream_id}") }, 
              modifier = Modifier
                .padding(8.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
              .then(
                if (isFocused) Modifier.border(4.dp, Color(0xFFFF0000), MaterialTheme.shapes.medium)
                else Modifier
              )
            ) {
              AsyncImage(model = v.stream_icon, contentDescription = v.name, modifier = Modifier.height(220.dp).fillMaxWidth())
              Text(v.name, modifier = Modifier.padding(8.dp), maxLines = 2)
            }
          }
        }
      }
    }
  }
}


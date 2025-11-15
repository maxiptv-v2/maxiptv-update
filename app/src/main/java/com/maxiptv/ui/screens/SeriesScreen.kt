package com.maxiptv.ui.screens
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.maxiptv.MaxiApp
import com.maxiptv.data.XRepo
import coil.compose.AsyncImage

@Composable
fun SeriesScreen(nav: NavHostController) {
  val cats by XRepo.seriesCategories.collectAsState(emptyList())
  val series by XRepo.seriesItems.collectAsState(emptyList())
  var selectedCat by remember { mutableStateOf<String?>(null) }
  LaunchedEffect(Unit) { XRepo.ensureSeriesLoaded() }
  
  val isFireStick = MaxiApp.isFireStick
  val isTv = MaxiApp.isTv
  val isPhone = MaxiApp.isPhone
  
  Column(Modifier.fillMaxSize()) {
    // TopBar com Logo e Botão Voltar (APENAS no Fire Stick para evitar que status bar cubra categorias)
    if (isFireStick) {
      val horizontalPadding = 12.dp
      
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(
            vertical = 12.dp,
            horizontal = horizontalPadding
          )
      ) {
        // Logo à esquerda
        Row(
          modifier = Modifier.align(Alignment.CenterStart),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Start
        ) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Logo",
            modifier = Modifier.size(40.dp),
            tint = Color(0xFF00D4FF)
          )
          
          Spacer(Modifier.width(12.dp))
          
          Text(
            text = "Max IPTV",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D4FF)
          )
        }
        
        // Botão Voltar à direita
        IconButton(
          onClick = { nav.popBackStack() },
          modifier = Modifier.align(Alignment.CenterEnd)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Voltar",
            tint = Color(0xFF00D4FF),
            modifier = Modifier.size(40.dp)
          )
        }
      }
    }
    
    CategoryChips(categories = cats.map { it.category_name to it.category_id }, selectedId = selectedCat, onSelect = { selectedCat = it })
    LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(12.dp), modifier = Modifier.weight(1f)) {
      val filtered = series.filter { selectedCat == null || it.category_id == selectedCat }
      
      // Agrupar por título base e priorizar SEM TAG (dublado) > DUAL > LEGENDADO
      val grouped = filtered.groupBy { 
        it.name.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
      }.mapValues { (_, versions) ->
        // Prioridade: SEM TAG (0) > [DUB] (1) > [DUAL] (2) > [LEG] (3)
        versions.sortedBy { v ->
          when {
            !v.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE)) -> 0  // SEM TAG = DUBLADO
            v.name.contains(Regex("\\[(DUB|DUBLADO)\\]", RegexOption.IGNORE_CASE)) -> 1
            v.name.contains(Regex("\\[DUAL\\]", RegexOption.IGNORE_CASE)) -> 2
            else -> 3 // LEGENDADO
          }
        }.first()
      }.values.toList()
      
      items(grouped) { s ->
        var isFocused by remember { mutableStateOf(false) }
        val isTv = MaxiApp.isTv
        val titleSize = if (isTv) 16.sp else 14.sp
        
        Card(
          onClick = { nav.navigate("series/${s.series_id}") }, 
          modifier = Modifier
            .padding(8.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .then(
              if (isFocused) Modifier.border(4.dp, Color(0xFFFF0000), MaterialTheme.shapes.medium)
              else Modifier
            )
        ) {
          AsyncImage(model = s.cover, contentDescription = s.name, modifier = Modifier.height(220.dp).fillMaxWidth(), contentScale = ContentScale.Crop)
          Text(
            text = s.name,
            modifier = Modifier.padding(10.dp),
            maxLines = 2,
            fontSize = titleSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

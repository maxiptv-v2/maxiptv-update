package com.maxiptv.ui.screens
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxiptv.MaxiApp

@Composable
fun CategoryChips(categories: List<Pair<String,String>>, selectedId: String?, onSelect: (String?) -> Unit) {
  val isTv = MaxiApp.isTv
  val isFireStick = MaxiApp.isFireStick
  val fontSize = if (isTv) 16.sp else 14.sp
  // Fire Stick: TopBar explícito cria espaço, então só precisa de pequeno espaçamento
  // TV Box: Não tem TopBar, então usa padding normal
  val topPadding = if (isFireStick) 8.dp else if (isTv) 16.dp else 12.dp
  val bottomPadding = if (isTv) 16.dp else 12.dp
  val horizontalPadding = if (isTv) 20.dp else 16.dp
  
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(
    start = horizontalPadding,
    end = horizontalPadding,
    top = topPadding,
    bottom = bottomPadding
  )) {
    AssistChip(
      onClick = { onSelect(null) }, 
      label = { 
        Text(
          "Todas",
          fontSize = fontSize,
          fontWeight = FontWeight.SemiBold,
          fontFamily = FontFamily.SansSerif
        ) 
      }, 
      modifier = Modifier.padding(end = 8.dp)
    )
    categories.forEach { (name, id) ->
      FilterChip(
        selected = selectedId == id, 
        onClick = { onSelect(id) }, 
        label = { 
          Text(
            name,
            fontSize = fontSize,
            fontWeight = if (selectedId == id) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif
          ) 
        }, 
        modifier = Modifier.padding(end = 8.dp)
      )
    }
  }
}

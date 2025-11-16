package com.maxiptv.ui.screens
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxiptv.MaxiApp
import com.maxiptv.ui.components.fillMaxWidthAdjusted

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
  
  Row(Modifier.fillMaxWidthAdjusted().horizontalScroll(rememberScrollState()).padding( // ✅ Fire Stick/Native TV: 90% da largura real
    start = horizontalPadding,
    end = horizontalPadding,
    top = topPadding,
    bottom = bottomPadding
  )) {
    var isTodasFocused by remember { mutableStateOf(false) }
    val todasScale by animateFloatAsState(
      targetValue = if (isTodasFocused) 1.15f else 1.0f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
      ),
      label = "todasZoom"
    )
    
    Box(
      modifier = Modifier
        .padding(end = 8.dp)
        .graphicsLayer {
          scaleX = todasScale
          scaleY = todasScale
        }
    ) {
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
        modifier = Modifier
          .onFocusChanged { isTodasFocused = it.isFocused }
          .focusable()
      )
      // Overlay branco transparente quando focado
      if (isTodasFocused) {
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(
              Color.White.copy(alpha = 0.3f),
              RoundedCornerShape(8.dp)
            )
        )
      }
    }
    categories.forEach { (name, id) ->
      var isFocused by remember { mutableStateOf(false) }
      val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1.0f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow
        ),
        label = "categoryZoom"
      )
      
      Box(
        modifier = Modifier
          .padding(end = 8.dp)
          .graphicsLayer {
            scaleX = scale
            scaleY = scale
          }
      ) {
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
          modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
        )
        // Overlay branco transparente quando focado
        if (isFocused) {
          Box(
            modifier = Modifier
              .matchParentSize()
              .background(
                Color.White.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
              )
          )
        }
      }
    }
  }
}

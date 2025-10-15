package com.maxiptv.ui.screens
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.maxiptv.data.FeaturedItem

@Composable
fun BannerCarousel(items: List<FeaturedItem>, onClickVod: (Int) -> Unit) {
  Row(Modifier.fillMaxWidth().height(220.dp).padding(12.dp).horizontalScroll(rememberScrollState())) {
    items.forEach { item ->
      var isFocused by remember { mutableStateOf(false) }
      
      Card(
        onClick = { item.vodId?.let(onClickVod) }, 
        modifier = Modifier
          .width(360.dp)
          .padding(end = 12.dp)
          .onFocusChanged { isFocused = it.isFocused }
          .focusable()
          .then(
            if (isFocused)
              Modifier
                .border(4.dp, Color(0xFFFF0000), RoundedCornerShape(8.dp))
                .shadow(
                  elevation = 16.dp,
                  spotColor = Color(0xFFFF0000).copy(alpha = 0.8f),
                  ambientColor = Color(0xFFFF0000).copy(alpha = 0.6f)
                )
            else
              Modifier
          ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(8.dp)
      ) {
        AsyncImage(
          model = item.imageUrl, 
          contentDescription = item.title, 
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

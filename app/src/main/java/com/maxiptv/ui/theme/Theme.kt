package com.maxiptv.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val light = lightColorScheme(
  primary =  Color(0xFF0D47A1),
  secondary = Color(0xFFB0BEC5),
  background = Color(0xFF0A0F1A),
  surface = Color(0xFF121826),
  onPrimary = Color.White,
  onSecondary = Color.Black,
  onBackground = Color(0xFFE0E6EE),
  onSurface = Color(0xFFE0E6EE)
)

@Composable fun MaxiTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = light, typography = Typography(), content = content)
}

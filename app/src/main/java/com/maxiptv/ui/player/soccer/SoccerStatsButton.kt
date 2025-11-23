package com.maxiptv.ui.player.soccer

import androidx.compose.animation.core.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Botão 3D flutuante com animação de rotação contínua
 * Aparece no canto superior direito quando modo futebol está ativo
 */
import androidx.compose.foundation.clickable

@Composable
fun SoccerStatsButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.2f else 1f,
        label = "buttonScale"
    )
    
    Box(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .focusable(true)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
            drawCircle(
                color = Color(0xFF1E88E5),
                radius = size.minDimension / 2
            )
        }
    }
}


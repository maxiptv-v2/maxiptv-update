package com.maxiptv.ui.player.soccer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.maxiptv.data.soccer.PlayerDetail
import com.maxiptv.data.soccer.MatchEvent

/**
 * Card de jogador com zoom automático baseado em eventos
 * Faz zoom quando há eventos importantes (gol, pênalti, cartão, etc.)
 */
@Composable
fun ZoomablePlayerCard(
    player: PlayerDetail,
    event: MatchEvent?
) {
    val targetScale = when(event?.type) {
        "GOAL", "PENALTY", "CORNER" -> 1.3f
        "YELLOW_CARD", "RED_CARD" -> 1.1f
        else -> 1f
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(500),
        label = "playerCardScale"
    )

    Box(modifier = Modifier.scale(scale)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E88E5).copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = player.name ?: "Jogador",
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}


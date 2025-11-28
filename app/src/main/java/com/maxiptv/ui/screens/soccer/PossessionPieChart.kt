package com.maxiptv.ui.screens.soccer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Componente de gráfico de pizza para posse de bola
 */
@Composable
fun PossessionPieChart(
    homePossession: Int,
    awayPossession: Int,
    homeTeamName: String = "Casa",
    awayTeamName: String = "Visitante",
    homeColor: Color = Color(0xFF4CAF50),
    awayColor: Color = Color(0xFFFF5252),
    deviceType: String = "tv"
) {
    val fontSize = when (deviceType) {
        "tv" -> 14.sp
        "phone" -> 11.sp
        else -> 12.sp
    }
    
    val chartSize = when (deviceType) {
        "tv" -> 120.dp
        "phone" -> 80.dp
        else -> 100.dp
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚽ Posse de Bola",
            fontSize = when (deviceType) {
                "tv" -> 18.sp
                "phone" -> 14.sp
                else -> 16.sp
            },
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            color = Color(0xFFFFD700),
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 12.dp),
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f),
                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                    blurRadius = 4f
                )
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Legenda e valor - Casa
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(homeColor)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = homeTeamName,
                    fontSize = fontSize,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    letterSpacing = 0.2.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                            blurRadius = 3f
                        )
                    )
                )
                Text(
                    text = "$homePossession%",
                    fontSize = when (deviceType) {
                        "tv" -> 22.sp
                        "phone" -> 18.sp
                        else -> 20.sp
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    color = homeColor,
                    letterSpacing = 0.5.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f),
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
            }
            
            // Gráfico de pizza
            Box(
                modifier = Modifier.size(chartSize),
                contentAlignment = Alignment.Center
            ) {
                PieChartCanvas(
                    homePercentage = homePossession,
                    awayPercentage = awayPossession,
                    homeColor = homeColor,
                    awayColor = awayColor
                )
                
                // Valor central
                Text(
                    text = "${homePossession}%",
                    fontSize = when (deviceType) {
                        "tv" -> 18.sp
                        "phone" -> 14.sp
                        else -> 16.sp
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Legenda e valor - Visitante
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(awayColor)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = awayTeamName,
                    fontSize = fontSize,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    letterSpacing = 0.2.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                            blurRadius = 3f
                        )
                    )
                )
                Text(
                    text = "$awayPossession%",
                    fontSize = when (deviceType) {
                        "tv" -> 22.sp
                        "phone" -> 18.sp
                        else -> 20.sp
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                    color = awayColor,
                    letterSpacing = 0.5.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f),
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun PieChartCanvas(
    homePercentage: Int,
    awayPercentage: Int,
    homeColor: Color,
    awayColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 - 8.dp.toPx()
        
        val homeAngle = (homePercentage / 100f) * 360f
        val awayAngle = (awayPercentage / 100f) * 360f
        
        // Desenhar arco do time da casa (verde)
        drawArc(
            color = homeColor,
            startAngle = -90f,
            sweepAngle = homeAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
        
        // Desenhar arco do time visitante (vermelho)
        drawArc(
            color = awayColor,
            startAngle = -90f + homeAngle,
            sweepAngle = awayAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
        
        // Borda externa
        drawCircle(
            color = Color(0xFF2A2A2A),
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}


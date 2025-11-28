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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente de gráfico de barras para estatísticas de futebol
 * Exibe comparação visual entre time da casa e visitante
 */
@Composable
fun StatisticBarChart(
    label: String,
    homeValue: Int,
    awayValue: Int,
    maxValue: Int = 100,
    homeColor: Color = Color(0xFF4CAF50),
    awayColor: Color = Color(0xFFFF5252),
    deviceType: String = "tv"
) {
    val fontSize = when (deviceType) {
        "tv" -> 12.sp
        "phone" -> 10.sp
        else -> 11.sp
    }
    
    val barHeight = when (deviceType) {
        "tv" -> 24.dp
        "phone" -> 18.dp
        else -> 20.dp
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Label
        Text(
            text = label,
            fontSize = fontSize,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        // Gráfico de barras
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra do time da casa (esquerda)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                BarChartBar(
                    value = homeValue,
                    maxValue = maxValue,
                    color = homeColor,
                    alignment = Alignment.End
                )
                // Valor numérico
                Text(
                    text = "$homeValue",
                    fontSize = fontSize,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(horizontal = 4.dp)
                )
            }
            
            // Separador
            Text(
                text = "X",
                fontSize = fontSize,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            
            // Barra do time visitante (direita)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                BarChartBar(
                    value = awayValue,
                    maxValue = maxValue,
                    color = awayColor,
                    alignment = Alignment.Start
                )
                // Valor numérico
                Text(
                    text = "$awayValue",
                    fontSize = fontSize,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BarChartBar(
    value: Int,
    maxValue: Int,
    color: Color,
    alignment: Alignment.Horizontal
) {
    val percentage = if (maxValue > 0) (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f) else 0f
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2A2A2A))
    ) {
        val barWidth = size.width * percentage
        val barHeight = size.height
        
        val x = when (alignment) {
            Alignment.Start -> 0f
            Alignment.End -> size.width - barWidth
            Alignment.CenterHorizontally -> (size.width - barWidth) / 2
            else -> (size.width - barWidth) / 2
        }
        
        drawRect(
            color = color,
            topLeft = Offset(x, 0f),
            size = Size(barWidth, barHeight)
        )
        
        // Gradiente sutil
        drawRect(
            color = color.copy(alpha = 0.3f),
            topLeft = Offset(x, 0f),
            size = Size(barWidth, barHeight / 2)
        )
    }
}


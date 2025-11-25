package com.maxiptv.ui.player.soccer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxiptv.data.soccer.*

/**
 * Overlay completo com estatísticas de futebol
 * Mostra estatísticas da partida atual, probabilidades, sugestões de apostas
 * e lista de outros jogos ao vivo
 */
@Composable
fun SoccerStatsOverlay(
    modifier: Modifier = Modifier,
    currentMatch: MatchDetailFull,
    matchPreview: MatchPreviewFull?,
    otherMatches: List<MatchSummaryFull>,
    onClose: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20).copy(alpha = 0.8f)) // verde "grama realista"
    ) {
        // Card central com stats
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.7f)
                .fillMaxHeight(0.6f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101010))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "${currentMatch.homeTeamName} X ${currentMatch.awayTeamName}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Estatísticas principais (usando campos do MatchDetailFull)
                Text(
                    text = "Posse: ${currentMatch.possessionHome}% x ${currentMatch.possessionAway}%",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "Finalizações: ${currentMatch.shotsHome} x ${currentMatch.shotsAway}",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "Escanteios: ${currentMatch.cornersHome} x ${currentMatch.cornersAway}",
                    color = Color.White,
                    fontSize = 16.sp
                )

                // Preview/Predições (se disponível)
                matchPreview?.match_data?.let { previewData ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Previsões:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    previewData.prediction?.let { pred ->
                        Text(
                            text = "Predição: ${pred.choice ?: ""}",
                            color = Color.Green,
                            fontSize = 16.sp
                        )
                    }
                    if (previewData.excitement_rating != null) {
                        Text(
                            text = "⭐ Rating: ${String.format("%.1f", previewData.excitement_rating)}/10",
                            color = Color(0xFFFFD700),
                            fontSize = 16.sp
                        )
                    }
                }

                // Eventos (usando estrutura do MatchDetailFull)
                currentMatch.events?.firstOrNull()?.let { event ->
                    event.player?.let { player ->
                        Spacer(modifier = Modifier.height(16.dp))
                        // Mostrar último evento
                        Text(
                            text = "Último evento: ${event.type ?: ""} - ${player.name ?: ""}",
                            color = Color.Cyan,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Lista lateral de outros jogos
        if (otherMatches.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(180.dp)
                    .padding(8.dp)
            ) {
                items(otherMatches) { match ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF202020))
                    ) {
                        Text(
                            text = "${match.homeTeamName} X ${match.awayTeamName}",
                            modifier = Modifier.padding(8.dp),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Botão fechar
        TextButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Sair", color = Color.White)
        }
    }
}


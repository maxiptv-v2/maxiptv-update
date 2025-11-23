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
    currentMatch: MatchDetail,
    matchPreview: MatchPreview?,
    otherMatches: List<MatchSummary>,
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
                
                // Estatísticas principais
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
                Text(
                    text = "xG: ${String.format("%.2f", currentMatch.xGHome)} x ${String.format("%.2f", currentMatch.xGAway)}",
                    color = Color.White,
                    fontSize = 16.sp
                )

                // Probabilidades
                matchPreview?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Probabilidades:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${currentMatch.homeTeamName}: ${String.format("%.0f", it.homeWinPercent)}%",
                        color = Color.Green,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Empate: ${String.format("%.0f", it.drawPercent)}%",
                        color = Color.Yellow,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${currentMatch.awayTeamName}: ${String.format("%.0f", it.awayWinPercent)}%",
                        color = Color.Red,
                        fontSize = 16.sp
                    )

                    // Sugestões de apostas
                    if (!it.suggestedBets.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sugestões de apostas:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        it.suggestedBets.forEach { bet ->
                            Text(
                                text = "- $bet",
                                color = Color.Cyan,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Zoom no jogador se houver evento
                currentMatch.currentEvent?.let { event ->
                    event.playerId?.let { playerId ->
                        Spacer(modifier = Modifier.height(16.dp))
                        // Buscar dados do jogador (simplificado por enquanto)
                        val player = PlayerDetail(
                            id = playerId,
                            name = event.player?.name ?: "Jogador",
                            goals = null,
                            assists = null,
                            yellowCards = null,
                            redCards = null
                        )
                        ZoomablePlayerCard(player = player, event = event)
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


package com.maxiptv.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.maxiptv.MaxiApp
import com.maxiptv.data.PlayerSettingsManager
import kotlinx.coroutines.launch

@Composable
fun PlayerSettingsScreen(nav: NavHostController) {
  var videoQuality by remember { mutableStateOf(PlayerSettingsManager.VideoQuality.AUTO) }
  var audioBoost by remember { mutableStateOf(false) }
  var silentMode by remember { mutableStateOf(false) }
  var playbackSpeed by remember { mutableStateOf(PlayerSettingsManager.PlaybackSpeed.NORMAL) }
  var autoPlay by remember { mutableStateOf(true) }
  var maxVolume by remember { mutableStateOf(100) }
  var isLoading by remember { mutableStateOf(true) }
  
  val scope = rememberCoroutineScope()
  val isTv = MaxiApp.isTv
  val isPhone = MaxiApp.isPhone
  
  // Carregar configurações
  LaunchedEffect(Unit) {
    try {
      videoQuality = PlayerSettingsManager.getVideoQuality()
      audioBoost = PlayerSettingsManager.isAudioBoostEnabled()
      silentMode = PlayerSettingsManager.isSilentModeEnabled()
      playbackSpeed = PlayerSettingsManager.getPlaybackSpeed()
      autoPlay = PlayerSettingsManager.isAutoPlayEnabled()
      maxVolume = PlayerSettingsManager.getMaxVolume()
      isLoading = false
      android.util.Log.i("PlayerSettingsScreen", "✅ Configurações carregadas")
    } catch (e: Exception) {
      android.util.Log.e("PlayerSettingsScreen", "❌ Erro ao carregar configurações: ${e.message}")
      isLoading = false
    }
  }
  
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF0A0F1A))
      .padding(
        horizontal = if (isTv) 32.dp else if (isPhone) 16.dp else 24.dp,
        vertical = if (isTv) 24.dp else if (isPhone) 16.dp else 20.dp
      )
  ) {
    // Header com botão voltar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      var isBackFocused by remember { mutableStateOf(false) }
      Button(
        onClick = { nav.popBackStack() },
        modifier = Modifier
          .onFocusChanged { isBackFocused = it.isFocused }
          .focusable()
          .then(
            if (isBackFocused)
              Modifier
                .border(4.dp, Color(0xFF00D4FF), RoundedCornerShape(8.dp))
                .shadow(
                  elevation = 15.dp,
                  spotColor = Color(0xFF00D4FF).copy(alpha = 0.9f),
                  ambientColor = Color(0xFF00D4FF).copy(alpha = 0.7f),
                  shape = RoundedCornerShape(8.dp)
                )
            else
              Modifier
          ),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF00D4FF)
        )
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
        Spacer(Modifier.width(8.dp))
        Text("Voltar", fontWeight = FontWeight.Bold)
      }
      
      Spacer(Modifier.width(16.dp))
      
      Text(
        text = "⚙️ Configurações do Player",
        fontSize = when {
          isTv -> 28.sp
          isPhone -> 20.sp
          else -> 24.sp
        },
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
    
    Spacer(Modifier.height(if (isTv) 24.dp else if (isPhone) 16.dp else 20.dp))
    
    if (isLoading) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(
          color = Color(0xFF00D4FF),
          strokeWidth = 3.dp
        )
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(if (isTv) 20.dp else if (isPhone) 16.dp else 18.dp)
      ) {
        // Qualidade de Vídeo
        item {
          SettingsSection(
            title = "🎬 Qualidade de Vídeo",
            description = "Escolha a qualidade ideal para sua conexão"
          ) {
            var isQualityFocused by remember { mutableStateOf(false) }
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isQualityFocused = it.isFocused }
                .focusable()
                .then(
                  if (isQualityFocused)
                    Modifier
                      .border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(12.dp))
                      .shadow(
                        elevation = 12.dp,
                        spotColor = Color(0xFF00D4FF).copy(alpha = 0.9f),
                        ambientColor = Color(0xFF00D4FF).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                      )
                  else
                    Modifier
                ),
              colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
              )
            ) {
              Column(
                modifier = Modifier.padding(16.dp)
              ) {
                PlayerSettingsManager.VideoQuality.values().forEach { quality ->
                  var isOptionFocused by remember { mutableStateOf(false) }
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .onFocusChanged { isOptionFocused = it.isFocused }
                      .focusable()
                      .then(
                        if (isOptionFocused)
                          Modifier
                            .border(2.dp, Color(0xFF00D4FF), RoundedCornerShape(8.dp))
                            .background(Color(0xFF00D4FF).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        else
                          Modifier
                      )
                      .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    RadioButton(
                      selected = videoQuality == quality,
                      onClick = {
                        videoQuality = quality
                        scope.launch {
                          PlayerSettingsManager.setVideoQuality(quality)
                        }
                      },
                      colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF00D4FF),
                        unselectedColor = Color(0xFF666666)
                      )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                      Text(
                        text = quality.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                      )
                      Text(
                        text = "${quality.maxBitrate / 1000}Kbps - ${quality.minBitrate / 1000}Kbps",
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                      )
                    }
                  }
                }
              }
            }
          }
        }
        
        // Controles de Áudio
        item {
          SettingsSection(
            title = "🔊 Controles de Áudio",
            description = "Ajuste o áudio para melhor experiência"
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              // Boost de Áudio
              SettingsSwitch(
                title = "Boost de Áudio",
                description = "Amplifica o áudio para TVs com som baixo",
                checked = audioBoost,
                onCheckedChange = { checked ->
                  audioBoost = checked
                  scope.launch {
                    PlayerSettingsManager.setAudioBoost(checked)
                  }
                }
              )
              
              // Modo Silencioso
              SettingsSwitch(
                title = "Modo Silencioso",
                description = "Inicia sempre sem som",
                checked = silentMode,
                onCheckedChange = { checked ->
                  silentMode = checked
                  scope.launch {
                    PlayerSettingsManager.setSilentMode(checked)
                  }
                }
              )
              
              // Volume Máximo
              SettingsSlider(
                title = "Volume Máximo",
                description = "Limite máximo de volume (${maxVolume}%)",
                value = maxVolume.toFloat(),
                onValueChange = { value ->
                  maxVolume = value.toInt()
                  scope.launch {
                    PlayerSettingsManager.setMaxVolume(maxVolume)
                  }
                },
                valueRange = 10f..100f,
                steps = 8
              )
            }
          }
        }
        
        // Controles de Reprodução
        item {
          SettingsSection(
            title = "⏯️ Controles de Reprodução",
            description = "Configure como o conteúdo é reproduzido"
          ) {
            Column(
              verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              // Velocidade de Reprodução
              var isSpeedFocused by remember { mutableStateOf(false) }
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .onFocusChanged { isSpeedFocused = it.isFocused }
                  .focusable()
                  .then(
                    if (isSpeedFocused)
                      Modifier
                        .border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(12.dp))
                        .shadow(
                          elevation = 12.dp,
                          spotColor = Color(0xFF00D4FF).copy(alpha = 0.9f),
                          ambientColor = Color(0xFF00D4FF).copy(alpha = 0.7f),
                          shape = RoundedCornerShape(12.dp)
                        )
                    else
                      Modifier
                  ),
                colors = CardDefaults.cardColors(
                  containerColor = Color(0xFF1A1A1A)
                )
              ) {
                Column(
                  modifier = Modifier.padding(16.dp)
                ) {
                  Text(
                    text = "Velocidade de Reprodução",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                  )
                  Spacer(Modifier.height(8.dp))
                  Text(
                    text = "Atual: ${playbackSpeed.displayName}",
                    fontSize = 14.sp,
                    color = Color(0xFF00D4FF)
                  )
                  Spacer(Modifier.height(12.dp))
                  
                  LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    items(PlayerSettingsManager.PlaybackSpeed.values()) { speed ->
                      var isOptionFocused by remember { mutableStateOf(false) }
                      FilterChip(
                        selected = playbackSpeed == speed,
                        onClick = {
                          playbackSpeed = speed
                          scope.launch {
                            PlayerSettingsManager.setPlaybackSpeed(speed)
                          }
                        },
                        label = { Text(speed.displayName) },
                        modifier = Modifier
                          .onFocusChanged { isOptionFocused = it.isFocused }
                          .focusable()
                          .then(
                            if (isOptionFocused)
                              Modifier
                                .border(2.dp, Color(0xFF00D4FF), RoundedCornerShape(20.dp))
                            else
                              Modifier
                          )
                      )
                    }
                  }
                }
              }
              
              // Auto-play
              SettingsSwitch(
                title = "Auto-play",
                description = "Reproduz próximo episódio automaticamente",
                checked = autoPlay,
                onCheckedChange = { checked ->
                  autoPlay = checked
                  scope.launch {
                    PlayerSettingsManager.setAutoPlay(checked)
                  }
                }
              )
            }
          }
        }
        
        // Botões de Ação
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Resetar Configurações
            var isResetFocused by remember { mutableStateOf(false) }
            OutlinedButton(
              onClick = {
                scope.launch {
                  PlayerSettingsManager.resetToDefaults()
                  // Recarregar configurações
                  videoQuality = PlayerSettingsManager.getVideoQuality()
                  audioBoost = PlayerSettingsManager.isAudioBoostEnabled()
                  silentMode = PlayerSettingsManager.isSilentModeEnabled()
                  playbackSpeed = PlayerSettingsManager.getPlaybackSpeed()
                  autoPlay = PlayerSettingsManager.isAutoPlayEnabled()
                  maxVolume = PlayerSettingsManager.getMaxVolume()
                }
              },
              modifier = Modifier
                .weight(1f)
                .onFocusChanged { isResetFocused = it.isFocused }
                .focusable()
                .then(
                  if (isResetFocused)
                    Modifier
                      .border(3.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
                      .shadow(
                        elevation = 12.dp,
                        spotColor = Color(0xFFFF9800).copy(alpha = 0.9f),
                        ambientColor = Color(0xFFFF9800).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                      )
                  else
                    Modifier
                ),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFFF9800)
              )
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Resetar")
              Spacer(Modifier.width(8.dp))
              Text("Resetar", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
fun SettingsSection(
  title: String,
  description: String,
  content: @Composable () -> Unit
) {
  Column {
    Text(
      text = title,
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White
    )
    Spacer(Modifier.height(4.dp))
    Text(
      text = description,
      fontSize = 14.sp,
      color = Color(0xFF888888)
    )
    Spacer(Modifier.height(12.dp))
    content()
  }
}

@Composable
fun SettingsSwitch(
  title: String,
  description: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  var isFocused by remember { mutableStateOf(false) }
  
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .onFocusChanged { isFocused = it.isFocused }
      .focusable()
      .then(
        if (isFocused)
          Modifier
            .border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(12.dp))
            .shadow(
              elevation = 12.dp,
              spotColor = Color(0xFF00D4FF).copy(alpha = 0.9f),
              ambientColor = Color(0xFF00D4FF).copy(alpha = 0.7f),
              shape = RoundedCornerShape(12.dp)
            )
        else
          Modifier
      ),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFF1A1A1A)
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = title,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = description,
          fontSize = 12.sp,
          color = Color(0xFF888888)
        )
      }
      
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color(0xFF00D4FF),
          checkedTrackColor = Color(0xFF00D4FF).copy(alpha = 0.5f),
          uncheckedThumbColor = Color(0xFF666666),
          uncheckedTrackColor = Color(0xFF333333)
        )
      )
    }
  }
}

@Composable
fun SettingsSlider(
  title: String,
  description: String,
  value: Float,
  onValueChange: (Float) -> Unit,
  valueRange: ClosedFloatingPointRange<Float>,
  steps: Int
) {
  var isFocused by remember { mutableStateOf(false) }
  
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .onFocusChanged { isFocused = it.isFocused }
      .focusable()
      .then(
        if (isFocused)
          Modifier
            .border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(12.dp))
            .shadow(
              elevation = 12.dp,
              spotColor = Color(0xFF00D4FF).copy(alpha = 0.9f),
              ambientColor = Color(0xFF00D4FF).copy(alpha = 0.7f),
              shape = RoundedCornerShape(12.dp)
            )
        else
          Modifier
      ),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFF1A1A1A)
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = description,
        fontSize = 12.sp,
        color = Color(0xFF888888)
      )
      Spacer(Modifier.height(12.dp))
      
      Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
          thumbColor = Color(0xFF00D4FF),
          activeTrackColor = Color(0xFF00D4FF),
          inactiveTrackColor = Color(0xFF333333)
        )
      )
    }
  }
}

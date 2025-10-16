package com.maxiptv.ui.screens
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.maxiptv.MaxiApp
import com.maxiptv.data.UserManager
import com.maxiptv.data.SessionManager
import com.maxiptv.data.XRepo
import com.maxiptv.data.UpdateManager
import com.maxiptv.data.UpdateInfo
import com.maxiptv.data.ApkDownloader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(nav: NavHostController) {
  val featured by XRepo.featured.collectAsState(emptyList())
  val liveChannels by XRepo.liveStreams.collectAsState(emptyList())
  val liveCategories by XRepo.liveCategories.collectAsState(emptyList())
  var focusedButton by remember { mutableStateOf<String?>(null) }
  var showExpiryWarning by remember { mutableStateOf(false) }
  var daysUntilExpiry by remember { mutableStateOf(0) }
  var isLoggingOut by remember { mutableStateOf(false) }
  var showLogoutDialog by remember { mutableStateOf(false) }
  var currentTime by remember { mutableStateOf("") }
  var showLiveCarousel by remember { mutableStateOf(true) }
  var eventosCanal by remember { mutableStateOf<com.maxiptv.data.LiveStream?>(null) }
  var conteudosCanal by remember { mutableStateOf<com.maxiptv.data.LiveStream?>(null) }
  val scope = rememberCoroutineScope()
  
  // Estados para auto-update
  var updateAvailable by remember { mutableStateOf<UpdateInfo?>(null) }
  var showUpdateDialog by remember { mutableStateOf(false) }
  var isDownloading by remember { mutableStateOf(false) }
  val context = LocalContext.current
  
  // Verificar atualizações ao abrir o app
  LaunchedEffect(Unit) {
    try {
      android.util.Log.i("HomeScreen", "🔍 Verificando atualizações...")
      val update = UpdateManager.checkForUpdate(context)
      if (update != null) {
        android.util.Log.i("HomeScreen", "🆕 Atualização disponível: ${update.version}")
        updateAvailable = update
        showUpdateDialog = true
      } else {
        android.util.Log.i("HomeScreen", "✅ App está atualizado")
      }
    } catch (e: Exception) {
      android.util.Log.e("HomeScreen", "❌ Erro ao verificar update: ${e.message}")
    }
  }
  
  // Buscar canais "Eventos do Dia" e "Conteúdos em Alta" da categoria "Avisos do Servidor"
  LaunchedEffect(liveChannels, liveCategories) {
    android.util.Log.i("HomeScreen", "🔍 Buscando canais de Avisos...")
    android.util.Log.i("HomeScreen", "📊 Total de categorias: ${liveCategories.size}")
    android.util.Log.i("HomeScreen", "📊 Total de canais: ${liveChannels.size}")
    
    // Listar todas as categorias para debug
    liveCategories.forEach { 
      android.util.Log.d("HomeScreen", "   Categoria: ${it.category_name} (ID: ${it.category_id})")
    }
    
    val avisosCategory = liveCategories.firstOrNull { 
      it.category_name.contains("AVISOS", ignoreCase = true) || 
      it.category_name.contains("Avisos", ignoreCase = true)
    }
    
    if (avisosCategory != null) {
      android.util.Log.i("HomeScreen", "✅ Categoria encontrada: ${avisosCategory.category_name}")
      val canaisAvisos = liveChannels.filter { it.category_id == avisosCategory.category_id }
      android.util.Log.i("HomeScreen", "📺 Canais na categoria Avisos: ${canaisAvisos.size}")
      
      canaisAvisos.forEach {
        android.util.Log.d("HomeScreen", "   Canal: ${it.name}")
      }
      
      eventosCanal = canaisAvisos.firstOrNull { it.name.contains("Eventos", ignoreCase = true) }
      conteudosCanal = canaisAvisos.firstOrNull { it.name.contains("Conteúdos", ignoreCase = true) }
      
      android.util.Log.i("HomeScreen", "📺 Eventos do Dia: ${eventosCanal?.name}")
      android.util.Log.i("HomeScreen", "   URL: ${eventosCanal?.toLiveUrl()}")
      android.util.Log.i("HomeScreen", "🔥 Conteúdos em Alta: ${conteudosCanal?.name}")
      android.util.Log.i("HomeScreen", "   URL: ${conteudosCanal?.toLiveUrl()}")
    } else {
      android.util.Log.w("HomeScreen", "❌ Categoria 'Avisos' não encontrada!")
    }
  }
  
  // Atualizar relógio a cada segundo
  LaunchedEffect(Unit) {
    while (true) {
      val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
      currentTime = sdf.format(Date())
      delay(1000)
    }
  }
  
  // Alternar carrossel a cada 10 segundos
  LaunchedEffect(Unit) {
    while (true) {
      delay(10000)
      showLiveCarousel = !showLiveCarousel
    }
  }
  
  LaunchedEffect(Unit) { 
    XRepo.ensureFeaturedLoaded()
    XRepo.ensureLiveLoaded()
    
    // Verificar validade
    scope.launch {
      val user = UserManager.getCurrentUser()
      user?.let {
        val days = UserManager.getDaysUntilExpiry(it.expiryDate)
        if (days != null && days <= 5 && days >= 0) {
          daysUntilExpiry = days
          showExpiryWarning = true
          // Auto-ocultar após 15 segundos
          delay(15000)
          showExpiryWarning = false
        }
      }
    }
  }
  
  val isTv = MaxiApp.isTv
  val isPhone = MaxiApp.isPhone
  val isFireStick = MaxiApp.isFireStick
  
  // Dialog de confirmação de logout
  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = null,
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(32.dp)
          )
          Spacer(Modifier.width(12.dp))
          Text("Confirmar Saída", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Text(
          "Deseja realmente sair do aplicativo?\n\nVocê precisará fazer login novamente.",
          fontSize = 16.sp
        )
      },
      confirmButton = {
        var isConfirmFocused by remember { mutableStateOf(false) }
        Button(
          onClick = {
            showLogoutDialog = false
            isLoggingOut = true
            scope.launch {
              val user = UserManager.getCurrentUser()
              user?.let {
                // Fazer logout no SessionManager (JSONBin)
                SessionManager.logout(it.username)
              }
              // Fazer logout local
              UserManager.logout()
              isLoggingOut = false
              // Voltar para tela de login
              nav.navigate("login") {
                popUpTo(0) { inclusive = true }
              }
            }
          },
          modifier = Modifier
            .onFocusChanged { isConfirmFocused = it.isFocused }
            .focusable()
            .then(
              if (isConfirmFocused) 
                Modifier
                  .border(4.dp, Color(0xFFFFFF00), RoundedCornerShape(8.dp))
                  .shadow(
                    elevation = 15.dp,
                    spotColor = Color(0xFFFFFF00).copy(alpha = 0.9f),
                    ambientColor = Color(0xFFFFFF00).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                  )
              else 
                Modifier
            ),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF5252)
          )
        ) {
          Icon(Icons.Default.Check, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("SIM, SAIR", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        var isDismissFocused by remember { mutableStateOf(false) }
        OutlinedButton(
          onClick = { showLogoutDialog = false },
          modifier = Modifier
            .onFocusChanged { isDismissFocused = it.isFocused }
            .focusable()
            .then(
              if (isDismissFocused) 
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
            )
        ) {
          Icon(Icons.Default.Close, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("CANCELAR", fontWeight = FontWeight.Bold)
        }
      }
    )
  }
  
  // Dialog de atualização disponível
  if (showUpdateDialog && updateAvailable != null) {
    AlertDialog(
      onDismissRequest = { showUpdateDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFF00D4FF),
            modifier = Modifier.size(32.dp)
          )
          Spacer(Modifier.width(12.dp))
          Text("🆕 Atualização Disponível!", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column {
          Text(
            "Nova versão: ${updateAvailable!!.version}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D4FF)
          )
          Spacer(Modifier.height(8.dp))
          Text(
            "Versão atual: ${UpdateManager.getCurrentVersionName(context)}",
            fontSize = 14.sp,
            color = Color.Gray
          )
          Spacer(Modifier.height(16.dp))
          Text(
            updateAvailable!!.releaseNotes,
            fontSize = 14.sp
          )
          Spacer(Modifier.height(12.dp))
          Text(
            "Tamanho: ${updateAvailable!!.fileSize}",
            fontSize = 12.sp,
            color = Color.Gray
          )
        }
      },
      confirmButton = {
        var isUpdateFocused by remember { mutableStateOf(false) }
        Button(
          onClick = {
            isDownloading = true
            showUpdateDialog = false
            ApkDownloader.downloadAndInstall(
              context,
              updateAvailable!!.downloadUrl,
              updateAvailable!!.version
            )
          },
          enabled = !isDownloading,
          modifier = Modifier
            .onFocusChanged { isUpdateFocused = it.isFocused }
            .focusable()
            .then(
              if (isUpdateFocused) 
                Modifier
                  .border(4.dp, Color(0xFF00FF00), RoundedCornerShape(8.dp))
                  .shadow(
                    elevation = 15.dp,
                    spotColor = Color(0xFF00FF00).copy(alpha = 0.9f),
                    ambientColor = Color(0xFF00FF00).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                  )
              else 
                Modifier
            ),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00D4FF)
          )
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("ATUALIZAR AGORA", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        var isLaterFocused by remember { mutableStateOf(false) }
        OutlinedButton(
          onClick = { showUpdateDialog = false },
          modifier = Modifier
            .onFocusChanged { isLaterFocused = it.isFocused }
            .focusable()
            .then(
              if (isLaterFocused) 
                Modifier
                  .border(4.dp, Color(0xFFFF9800), RoundedCornerShape(8.dp))
                  .shadow(
                    elevation = 15.dp,
                    spotColor = Color(0xFFFF9800).copy(alpha = 0.9f),
                    ambientColor = Color(0xFFFF9800).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                  )
              else 
                Modifier
            )
        ) {
          Icon(Icons.Default.Close, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("DEPOIS", fontWeight = FontWeight.Bold)
        }
      }
    )
  }
  
  // Dialog de atualização disponível
  if (showUpdateDialog && updateAvailable != null) {
    AlertDialog(
      onDismissRequest = { showUpdateDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = Color(0xFF00FF00),
            modifier = Modifier.size(32.dp)
          )
          Spacer(Modifier.width(12.dp))
          Text("🆕 Atualização Disponível!", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column {
          Text(
            "Nova versão ${updateAvailable!!.version} disponível!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FF00)
          )
          Spacer(Modifier.height(12.dp))
          Text(
            "Versão atual: ${UpdateManager.getCurrentVersionName(context)}",
            fontSize = 14.sp,
            color = Color.Gray
          )
          Spacer(Modifier.height(8.dp))
          Text(
            "Tamanho: ${updateAvailable!!.fileSize}",
            fontSize = 14.sp,
            color = Color.Gray
          )
          Spacer(Modifier.height(16.dp))
          Text(
            "📋 Novidades:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(Modifier.height(8.dp))
          Text(
            updateAvailable!!.releaseNotes,
            fontSize = 14.sp,
            lineHeight = 20.sp
          )
        }
      },
      confirmButton = {
        var isConfirmFocused by remember { mutableStateOf(false) }
        Button(
          onClick = {
            showUpdateDialog = false
            isDownloading = true
            ApkDownloader.downloadAndInstall(
              context,
              updateAvailable!!.downloadUrl,
              updateAvailable!!.version
            )
          },
          enabled = !isDownloading,
          modifier = Modifier
            .onFocusChanged { isConfirmFocused = it.isFocused }
            .focusable()
            .then(
              if (isConfirmFocused) 
                Modifier
                  .border(4.dp, Color(0xFF00FF00), RoundedCornerShape(8.dp))
                  .shadow(
                    elevation = 15.dp,
                    spotColor = Color(0xFF00FF00).copy(alpha = 0.9f),
                    ambientColor = Color(0xFF00FF00).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                  )
              else 
                Modifier
            ),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00FF00),
            contentColor = Color.Black
          )
        ) {
          if (isDownloading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = Color.Black,
              strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
          } else {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
          }
          Text(if (isDownloading) "BAIXANDO..." else "ATUALIZAR AGORA", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        var isDismissFocused by remember { mutableStateOf(false) }
        OutlinedButton(
          onClick = { showUpdateDialog = false },
          modifier = Modifier
            .onFocusChanged { isDismissFocused = it.isFocused }
            .focusable()
            .then(
              if (isDismissFocused) 
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
            )
        ) {
          Icon(Icons.Default.Close, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("DEPOIS", fontWeight = FontWeight.Bold)
        }
      }
    )
  }
  
  Box(Modifier.fillMaxSize()) {
  Column(Modifier.fillMaxSize()) {
      // TopBar com Botão SAIR e Relógio Digital
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF1A1A1A))
          .padding(
            horizontal = if (isTv) 32.dp else if (isPhone) 16.dp else 24.dp,
            vertical = if (isTv) 16.dp else if (isPhone) 12.dp else 14.dp
          )
      ) {
        // Botão SAIR no canto esquerdo
        LogoutButton(
          isFocused = focusedButton == "logout",
          deviceType = when {
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          isLoading = isLoggingOut,
          onFocusChanged = { focusedButton = if (it) "logout" else null },
          onClick = { showLogoutDialog = true },
          modifier = Modifier.align(Alignment.CenterStart)
        )
        
        // Relógio Digital centralizado
        DigitalClock(
          time = currentTime,
          deviceType = when {
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          modifier = Modifier.align(Alignment.Center)
        )
      }
      
      Spacer(Modifier.height(if (isTv) 16.dp else if (isPhone) 8.dp else 12.dp))
      
      // Logo Max IPTV com Neon (centralizada)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = if (isTv) 16.dp else if (isPhone) 8.dp else 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Logo",
            modifier = Modifier.size(
              when {
                isTv -> 48.dp
                isPhone -> 32.dp
                else -> 40.dp
              }
            ),
            tint = Color(0xFF00D4FF)
          )
          
          Spacer(Modifier.width(12.dp))
          
          NeonText(
            text = "Max IPTV",
            fontSize = when {
              isTv -> 36.sp
              isPhone -> 24.sp
              else -> 30.sp
            }
          )
        }
      }
      
      Spacer(Modifier.height(if (isTv) 12.dp else if (isPhone) 8.dp else 10.dp))
      
      // Carrossel Duplo (Eventos do Dia ↔ Conteúdos em Alta)
      if (eventosCanal != null && conteudosCanal != null) {
        DualCarousel(
          showEventos = showLiveCarousel,
          eventosCanal = eventosCanal!!,
          conteudosCanal = conteudosCanal!!,
          deviceType = when {
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          }
        )
      } else {
        // Fallback: mostrar espaço vazio enquanto carrega os canais
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(if (isTv) 320.dp else if (isPhone) 200.dp else 260.dp)
            .padding(horizontal = if (isTv) 32.dp else if (isPhone) 16.dp else 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1A))
            .border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Carregando canais...",
            color = Color(0xFF00D4FF),
            fontSize = when {
              isTv -> 18.sp
              isPhone -> 14.sp
              else -> 16.sp
            }
          )
        }
      }
      
      Spacer(Modifier.height(if (isTv) 20.dp else if (isPhone) 12.dp else 16.dp))
      
      // Botões de Categoria com Ícones (responsivos por dispositivo)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(
            horizontal = when {
              isTv -> 32.dp
              isPhone -> 12.dp
              else -> 20.dp
            }
          ),
        horizontalArrangement = Arrangement.spacedBy(
          when {
            isTv -> 20.dp
            isPhone -> 8.dp
            else -> 12.dp
          }
        )
      ) {
        CategoryButton(
          text = "Live",
          emoji = "📡",
          isFocused = focusedButton == "live",
          deviceType = when {
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          onFocusChanged = { focusedButton = if (it) "live" else null },
          onClick = { nav.navigate("live") },
          modifier = Modifier.weight(1f)
        )
        
        CategoryButton(
          text = "Filmes",
          emoji = "🎬",
          isFocused = focusedButton == "vod",
          deviceType = when {
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          onFocusChanged = { focusedButton = if (it) "vod" else null },
          onClick = { nav.navigate("vod") },
          modifier = Modifier.weight(1f)
        )
        
        CategoryButton(
          text = "Séries",
          emoji = "📺",
          isFocused = focusedButton == "series",
          deviceType = when {
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          onFocusChanged = { focusedButton = if (it) "series" else null },
        onClick = { nav.navigate("series") },
          modifier = Modifier.weight(1f)
        )
      }
    }
    
    // Alerta de Vencimento (Card Neon Verde)
    AnimatedVisibility(
      visible = showExpiryWarning,
      enter = slideInVertically() + fadeIn(),
      exit = slideOutVertically() + fadeOut(),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      ExpiryWarningCard(daysUntilExpiry)
    }
  }
}

@Composable
fun ExpiryWarningCard(days: Int) {
  val infiniteTransition = rememberInfiniteTransition(label = "glow")
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )
  
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .shadow(
        elevation = 24.dp,
        spotColor = Color(0xFF00FF00).copy(alpha = glowAlpha),
        ambientColor = Color(0xFF00FF00).copy(alpha = glowAlpha)
      )
      .border(
        width = 3.dp,
        color = Color(0xFF00FF00).copy(alpha = glowAlpha),
      ),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFF1A1A1A)
    )
  ) {
    Row(
        modifier = Modifier
        .padding(20.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Warning,
        contentDescription = null,
        tint = Color(0xFF00FF00),
        modifier = Modifier.size(48.dp)
      )
      
      Spacer(Modifier.width(16.dp))
      
      Column {
        Text(
          text = "⚠️ ATENÇÃO",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF00FF00)
        )
        
        Spacer(Modifier.height(4.dp))
        
        val message = if (days == 0) {
          "Sua assinatura vence HOJE!"
        } else {
          "Sua assinatura vence em $days ${if (days == 1) "dia" else "dias"}!"
        }
        
        Text(
          text = message,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        
        Spacer(Modifier.height(4.dp))
        
        Text(
          text = "Entre em contato para renovar",
          fontSize = 14.sp,
          color = Color(0xFFCCCCCC)
        )
      }
    }
  }
}

@Composable
fun NeonText(text: String, fontSize: androidx.compose.ui.unit.TextUnit) {
  val infiniteTransition = rememberInfiniteTransition(label = "neon")
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "neon"
  )
  
  Box(
    modifier = Modifier.shadow(
      elevation = 20.dp,
      spotColor = Color(0xFF00D4FF).copy(alpha = glowAlpha),
      ambientColor = Color(0xFF00D4FF).copy(alpha = glowAlpha),
      shape = RoundedCornerShape(8.dp)
    )
  ) {
    Text(
      text = text,
      fontSize = fontSize,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.SansSerif,
      color = Color.White,
      style = MaterialTheme.typography.headlineLarge.copy(
        shadow = androidx.compose.ui.graphics.Shadow(
          color = Color(0xFF00D4FF).copy(alpha = glowAlpha),
          offset = androidx.compose.ui.geometry.Offset(0f, 0f),
          blurRadius = 20f
        )
      )
    )
  }
}

@Composable
fun CategoryButton(
  text: String,
  emoji: String,
  isFocused: Boolean,
  deviceType: String,
  onFocusChanged: (Boolean) -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "greenGlow")
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "greenGlow"
  )
  
  val buttonHeight = when (deviceType) {
    "tv" -> 80.dp
    "phone" -> 65.dp
    else -> 72.dp
  }
  
  val fontSize = when (deviceType) {
    "tv" -> 18.sp
    "phone" -> 12.sp
    else -> 15.sp
  }
  
  val emojiSize = when (deviceType) {
    "tv" -> 24.sp
    "phone" -> 16.sp
    else -> 20.sp
  }
  
  val padding = when (deviceType) {
    "tv" -> PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    "phone" -> PaddingValues(horizontal = 6.dp, vertical = 8.dp)
    else -> PaddingValues(horizontal = 12.dp, vertical = 12.dp)
  }
  
  Button(
    onClick = onClick,
    modifier = modifier
      .height(buttonHeight)
      .onFocusChanged { onFocusChanged(it.isFocused) }
      .focusable()
      .shadow(
        elevation = 12.dp,
        spotColor = Color(0xFF00FF00).copy(alpha = glowAlpha),
        ambientColor = Color(0xFF00FF00).copy(alpha = glowAlpha)
      )
      .border(
        width = 2.dp,
        color = Color(0xFF00FF00).copy(alpha = glowAlpha),
        shape = RoundedCornerShape(8.dp)
      )
      .then(
        if (isFocused) 
          Modifier
            .border(8.dp, Color(0xFFFF0000), RoundedCornerShape(8.dp))
            .shadow(
              elevation = 25.dp,
              spotColor = Color(0xFFFF0000).copy(alpha = 1f),
              ambientColor = Color(0xFFFF0000).copy(alpha = 0.8f)
            )
        else 
          Modifier
      ),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color(0xFF00FF00),
      contentColor = Color.Black
    ),
    shape = RoundedCornerShape(8.dp),
    elevation = ButtonDefaults.buttonElevation(
      defaultElevation = 0.dp // Removido elevation padrão para usar shadow customizado
    ),
    contentPadding = padding
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = emoji,
        fontSize = emojiSize
      )
      Spacer(Modifier.height(if (deviceType == "phone") 2.dp else 4.dp))
      Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        maxLines = 1
      )
    }
  }
}

@Composable
fun LogoutButton(
  isFocused: Boolean,
  deviceType: String,
  isLoading: Boolean,
  onFocusChanged: (Boolean) -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "redGlow")
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "redGlow"
  )
  
  val buttonSize = when (deviceType) {
    "tv" -> 56.dp
    "phone" -> 44.dp
    else -> 50.dp
  }
  
  val iconSize = when (deviceType) {
    "tv" -> 28.dp
    "phone" -> 20.dp
    else -> 24.dp
  }
  
  Button(
    onClick = onClick,
    enabled = !isLoading,
    modifier = modifier
      .size(buttonSize)
      .onFocusChanged { onFocusChanged(it.isFocused) }
      .focusable()
      .shadow(
        elevation = 12.dp,
        spotColor = Color(0xFFFF5252).copy(alpha = glowAlpha),
        ambientColor = Color(0xFFFF5252).copy(alpha = glowAlpha),
        shape = RoundedCornerShape(12.dp)
      )
      .border(
        width = 2.dp,
        color = Color(0xFFFF5252).copy(alpha = glowAlpha),
        shape = RoundedCornerShape(12.dp)
      )
      .then(
        if (isFocused) 
          Modifier
            .border(6.dp, Color(0xFF00D4FF), RoundedCornerShape(12.dp))
            .shadow(
              elevation = 25.dp,
              spotColor = Color(0xFF00D4FF).copy(alpha = 1f),
              ambientColor = Color(0xFF00D4FF).copy(alpha = 0.8f),
              shape = RoundedCornerShape(12.dp)
            )
        else 
          Modifier
      ),
    colors = ButtonDefaults.buttonColors(
      containerColor = Color(0xFFFF5252),
      contentColor = Color.White,
      disabledContainerColor = Color(0xFF888888),
      disabledContentColor = Color.White
    ),
    shape = RoundedCornerShape(12.dp),
    contentPadding = PaddingValues(0.dp)
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(iconSize),
        color = Color.White,
        strokeWidth = 2.dp
      )
    } else {
      Icon(
        imageVector = Icons.Default.ExitToApp,
        contentDescription = "Sair",
        modifier = Modifier.size(iconSize)
      )
    }
  }
}

@Composable
fun DigitalClock(
  time: String,
  deviceType: String,
  modifier: Modifier = Modifier
) {
  val fontSize = when (deviceType) {
    "tv" -> 28.sp
    "phone" -> 18.sp
    else -> 22.sp
  }
  
  val infiniteTransition = rememberInfiniteTransition(label = "clockGlow")
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.6f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "clockGlow"
  )
  
  Box(
    modifier = modifier
      .shadow(
        elevation = 12.dp,
        spotColor = Color(0xFF00D4FF).copy(alpha = glowAlpha),
        ambientColor = Color(0xFF00D4FF).copy(alpha = glowAlpha),
        shape = RoundedCornerShape(8.dp)
      )
      .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
      .border(
        width = 2.dp,
        color = Color(0xFF00D4FF).copy(alpha = glowAlpha),
        shape = RoundedCornerShape(8.dp)
      )
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Text(
      text = time,
      fontSize = fontSize,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      color = Color(0xFF00D4FF),
      style = MaterialTheme.typography.headlineMedium.copy(
        shadow = androidx.compose.ui.graphics.Shadow(
          color = Color(0xFF00D4FF).copy(alpha = glowAlpha),
          offset = androidx.compose.ui.geometry.Offset(0f, 0f),
          blurRadius = 10f
        )
      )
    )
  }
}

@Composable
fun DualCarousel(
  showEventos: Boolean,
  eventosCanal: com.maxiptv.data.LiveStream,
  conteudosCanal: com.maxiptv.data.LiveStream,
  deviceType: String
) {
  val titleSize = when (deviceType) {
    "tv" -> 24.sp
    "phone" -> 16.sp
    else -> 20.sp
  }
  
  val carouselHeight = when (deviceType) {
    "tv" -> 400.dp  // Aumentado de 320dp para 400dp (25% maior)
    "phone" -> 200.dp
    else -> 260.dp
  }
  
  val context = androidx.compose.ui.platform.LocalContext.current
  
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(carouselHeight)
  ) {
    // Título do carrossel com animação
    AnimatedContent(
      targetState = showEventos,
      transitionSpec = {
        fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
      },
      label = "carouselTitle"
    ) { isEventos ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = if (deviceType == "tv") 32.dp else if (deviceType == "phone") 16.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (isEventos) Icons.Default.PlayArrow else Icons.Default.Star,
          contentDescription = null,
          tint = if (isEventos) Color(0xFFFF5252) else Color(0xFFFFD700),
          modifier = Modifier.size(if (deviceType == "tv") 32.dp else if (deviceType == "phone") 20.dp else 24.dp)
        )
        
        Spacer(Modifier.width(12.dp))
        
        Text(
          text = if (isEventos) "📺 EVENTOS DO DIA" else "🔥 CONTEÚDOS EM ALTA",
          fontSize = titleSize,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
          color = if (isEventos) Color(0xFFFF5252) else Color(0xFFFFD700)
        )
      }
    }
    
    Spacer(Modifier.height(if (deviceType == "tv") 16.dp else if (deviceType == "phone") 8.dp else 12.dp))
    
    // Player embutido com animação de transição
    AnimatedContent(
      targetState = showEventos,
      transitionSpec = {
        slideInHorizontally(
          initialOffsetX = { fullWidth -> fullWidth },
          animationSpec = tween(600)
        ) + fadeIn(animationSpec = tween(600)) togetherWith
        slideOutHorizontally(
          targetOffsetX = { fullWidth -> -fullWidth },
          animationSpec = tween(600)
        ) + fadeOut(animationSpec = tween(600))
      },
      label = "carouselContent"
    ) { isEventos ->
      val canal = if (isEventos) eventosCanal else conteudosCanal
      
      // Player embutido (silencioso, apenas visualização)
      EmbeddedPlayer(
        channel = canal,
        deviceType = deviceType
      )
    }
  }
}

@Composable
fun EmbeddedPlayer(
  channel: com.maxiptv.data.LiveStream,
  deviceType: String
) {
  val context = LocalContext.current
  val playerHeight = when (deviceType) {
    "tv" -> 300.dp  // Aumentado de 220dp para 300dp (36% maior)
    "phone" -> 140.dp
    else -> 180.dp
  }
  
  val exoPlayer = remember(channel.stream_id) {
    // ⚡ DataSource otimizado para carrossel (timeouts mais curtos)
    val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
      .setAllowCrossProtocolRedirects(true)
      .setUserAgent("MaxiPTV/1.1.1 (Android)")
      .setConnectTimeoutMs(6000)  // ⚡ 6 segundos (mais rápido que player principal)
      .setReadTimeoutMs(6000)     // ⚡ 6 segundos
      .setKeepPostFor302Redirects(true)
    
    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
      .setDataSourceFactory(dataSourceFactory)
    
    // ⚡ LoadControl super leve para carrossel (menos buffer)
    val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
      .setBufferDurationsMs(
        5000,   // minBufferMs: 5 segundos (bem leve)
        15000,  // maxBufferMs: 15 segundos (limitado)
        1000,   // bufferForPlaybackMs: 1 segundo (inicia rápido)
        2000    // bufferForPlaybackAfterRebufferMs: 2 segundos
      )
      .setPrioritizeTimeOverSizeThresholds(true)
      .setBackBuffer(5000, true) // Back buffer curto e limpar sempre
      .build()
    
    androidx.media3.exoplayer.ExoPlayer.Builder(context)
      .setMediaSourceFactory(mediaSourceFactory)
      .setLoadControl(loadControl) // ⚡ Aplicar cache leve
      .build().apply {
        val mediaItem = androidx.media3.common.MediaItem.fromUri(channel.toLiveUrl())
        setMediaItem(mediaItem)
        volume = 0f // SEM ÁUDIO
        repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        prepare()
        playWhenReady = true // INICIA TOCANDO AUTOMATICAMENTE
        android.util.Log.i("EmbeddedPlayer", "▶️ Player criado (LEVE) para carrossel: ${channel.name}")
        android.util.Log.i("EmbeddedPlayer", "   URL: ${channel.toLiveUrl()}")
      }
  }
  
  // Garantir que o player está tocando
  LaunchedEffect(channel.stream_id) {
    android.util.Log.i("EmbeddedPlayer", "🔄 Canal alterado: ${channel.name}")
    val mediaItem = androidx.media3.common.MediaItem.fromUri(channel.toLiveUrl())
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true
  }
  
  DisposableEffect(Unit) {
    onDispose {
      android.util.Log.i("EmbeddedPlayer", "⏹️ Player liberado: ${channel.name}")
      exoPlayer.stop()
      exoPlayer.release()
    }
  }
  
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(playerHeight)
      .padding(horizontal = if (deviceType == "tv") 32.dp else if (deviceType == "phone") 16.dp else 24.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(Color.Black)
      .border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(16.dp))
      .shadow(
        elevation = 16.dp,
        spotColor = Color(0xFF00D4FF).copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
      ),
    contentAlignment = Alignment.Center
  ) {
    // Para TV Box, usar largura total para eliminar barras pretas laterais
    val playerContainer = if (deviceType == "tv") {
      Modifier
        .fillMaxWidth()
        .fillMaxHeight()
    } else {
      Modifier.fillMaxSize()
    }
    // Player View (APENAS VISUALIZAÇÃO, SEM CLIQUE)
    AndroidView(
      factory = { ctx ->
        androidx.media3.ui.PlayerView(ctx).apply {
          player = exoPlayer
          useController = false // SEM CONTROLES
          
          // Para TV Box, usar ZOOM para preencher toda a tela (eliminar barras pretas)
          if (deviceType == "tv") {
            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
          } else {
            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
          }
          
          layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
          )
        }
      },
      modifier = playerContainer
    )
    
    // Badge com nome do canal (no canto inferior esquerdo)
    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(16.dp)
        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      Text(
        text = channel.name,
        fontSize = when (deviceType) {
          "tv" -> 16.sp
          "phone" -> 12.sp
          else -> 14.sp
        },
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
    
    // Indicador de "AO VIVO" piscante (canto superior direito)
    val infiniteTransition = rememberInfiniteTransition(label = "liveIndicator")
    val liveAlpha by infiniteTransition.animateFloat(
      initialValue = 0.3f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(
        animation = tween(800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "liveAlpha"
    )
    
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(16.dp)
        .background(Color(0xFFFF5252).copy(alpha = liveAlpha), RoundedCornerShape(6.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Text(
        text = "● AO VIVO",
        fontSize = when (deviceType) {
          "tv" -> 14.sp
          "phone" -> 10.sp
          else -> 12.sp
        },
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}

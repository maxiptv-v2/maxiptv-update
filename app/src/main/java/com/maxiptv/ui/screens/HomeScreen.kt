package com.maxiptv.ui.screens
import com.maxiptv.ui.screens.soccer.StatisticBarChart
import com.maxiptv.ui.screens.soccer.PossessionPieChart
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
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
import com.maxiptv.ui.components.fillMaxWidthAdjusted
import com.maxiptv.data.UpdateManager
import com.maxiptv.data.UpdateInfo
import com.maxiptv.data.ApkDownloader
import com.maxiptv.data.DeviceLogger
import com.maxiptv.ui.player.PlayerActivity
import com.maxiptv.ui.player.soccer.SoccerStatsButton
import com.maxiptv.data.soccer.SoccerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 🔥 Função para abrir o Downloader app no Fire OS com URL pré-preenchida
 * O Downloader é um app popular no Fire Stick para baixar arquivos
 */
private fun openDownloaderApp(context: Context, downloadUrl: String) {
  try {
    android.util.Log.i("HomeScreen", "🔥 Fire OS: Tentando abrir Downloader com URL: $downloadUrl")
    
    val packageManager = context.packageManager
    val downloaderPackage = "com.esaba.downloader"
    
    // Verificar se o Downloader está instalado pelo package name
    try {
      packageManager.getPackageInfo(downloaderPackage, 0)
      
      // ✅ Downloader instalado - abrir diretamente com Intent específico
      android.util.Log.i("HomeScreen", "✅ Downloader encontrado, abrindo com URL...")
      
      // Método 1: Tentar abrir com ACTION_VIEW e URL como data (método mais comum)
      // IMPORTANTE: Verificar se o Downloader pode resolver este Intent antes de tentar
      val intent1 = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
        setPackage(downloaderPackage) // ✅ CRÍTICO: Especificar package para não abrir navegador
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        // NÃO adicionar CATEGORY_BROWSABLE para evitar que o Android escolha navegador
      }
      
      try {
        // ✅ Verificar se o Downloader pode resolver este Intent (não deixar Android escolher navegador)
        val resolveInfo = packageManager.resolveActivity(intent1, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null && resolveInfo.activityInfo.packageName == downloaderPackage) {
          context.startActivity(intent1)
          android.util.Log.i("HomeScreen", "✅ Downloader aberto com ACTION_VIEW (URL pode estar preenchida)!")
          Toast.makeText(
            context,
            "Downloader aberto! Se a URL não preencheu, ela está copiada. Cole e clique em GO.",
            Toast.LENGTH_LONG
          ).show()
          // Copiar URL para clipboard como backup
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          val clip = ClipData.newPlainText("MaxiPTV Update URL", downloadUrl)
          clipboard.setPrimaryClip(clip)
          return
        } else {
          android.util.Log.w("HomeScreen", "⚠️ ACTION_VIEW não resolve para Downloader (pode abrir navegador) - pulando método 1")
        }
      } catch (e: Exception) {
        android.util.Log.w("HomeScreen", "⚠️ ACTION_VIEW falhou: ${e.message}")
      }
      
      // Método 2: Tentar com Intent explícito e passar URL via Intent extras
      val intent2 = Intent().apply {
        setClassName(downloaderPackage, "com.esaba.downloader.MainActivity")
        action = Intent.ACTION_VIEW
        data = Uri.parse(downloadUrl) // Passar URL diretamente
        putExtra("url", downloadUrl) // Extra para garantir
        putExtra("text", downloadUrl) // Alguns apps usam "text"
        putExtra(Intent.EXTRA_TEXT, downloadUrl) // Padrão Android
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      
      try {
        context.startActivity(intent2)
        android.util.Log.i("HomeScreen", "✅ Downloader aberto com Intent explícito!")
        Toast.makeText(
          context,
          "Downloader aberto! A URL já está preenchida. Clique em GO.",
          Toast.LENGTH_LONG
        ).show()
        return
      } catch (e: Exception) {
        android.util.Log.w("HomeScreen", "⚠️ Intent explícito falhou, tentando método alternativo: ${e.message}")
      }
      
      // Método 3: Tentar com ACTION_SEND (compartilhar URL)
      val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, downloadUrl)
        setPackage(downloaderPackage)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      
      try {
        if (shareIntent.resolveActivity(packageManager) != null) {
          context.startActivity(shareIntent)
          android.util.Log.i("HomeScreen", "✅ Downloader aberto via ACTION_SEND!")
          Toast.makeText(
            context,
            "Downloader aberto! A URL já está preenchida. Clique em GO.",
            Toast.LENGTH_LONG
          ).show()
          return
        }
      } catch (e: Exception) {
        android.util.Log.w("HomeScreen", "⚠️ ACTION_SEND falhou, tentando método 4: ${e.message}")
      }
      
      // Método 4: Abrir app diretamente e copiar URL (usuário cola manualmente)
      val openAppIntent = packageManager.getLaunchIntentForPackage(downloaderPackage)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        // Tentar passar URL como extra mesmo no launch intent
        putExtra("url", downloadUrl)
        putExtra(Intent.EXTRA_TEXT, downloadUrl)
      }
      
      if (openAppIntent != null) {
        // Copiar URL para clipboard antes de abrir (fallback caso o extra não funcione)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MaxiPTV Update URL", downloadUrl)
        clipboard.setPrimaryClip(clip)
        
        try {
          context.startActivity(openAppIntent)
          android.util.Log.i("HomeScreen", "✅ Downloader aberto (URL copiada para clipboard)")
          Toast.makeText(
            context,
            "Downloader aberto! A URL foi copiada. Se não preencheu automaticamente, cole no campo de URL e clique em GO.",
            Toast.LENGTH_LONG
          ).show()
          return
        } catch (e: Exception) {
          android.util.Log.e("HomeScreen", "❌ Erro ao abrir Downloader: ${e.message}")
        }
      }
      
    } catch (e: PackageManager.NameNotFoundException) {
      // Downloader NÃO instalado
      android.util.Log.w("HomeScreen", "⚠️ Downloader não está instalado")
      
      // Copiar URL para clipboard
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("MaxiPTV Update URL", downloadUrl)
      clipboard.setPrimaryClip(clip)
      
      Toast.makeText(
        context,
        "URL copiada! Instale o Downloader da Amazon App Store.",
        Toast.LENGTH_LONG
      ).show()
      
      // Tentar abrir Amazon App Store na página do Downloader
      try {
        val appStoreIntent = Intent(
          Intent.ACTION_VIEW,
          Uri.parse("amzn://apps/android?p=com.esaba.downloader")
        )
        appStoreIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(appStoreIntent)
        android.util.Log.i("HomeScreen", "✅ Amazon App Store aberta (Downloader)")
      } catch (e2: Exception) {
        // Se falhar, tentar web browser
        try {
          val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.amazon.com/AFTVnews-com-Downloader/dp/B01N0BP507")
          )
          webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
          context.startActivity(webIntent)
          android.util.Log.i("HomeScreen", "✅ Browser aberto para baixar Downloader")
        } catch (e3: Exception) {
          android.util.Log.e("HomeScreen", "❌ Erro ao abrir App Store/Browser: ${e3.message}")
        }
      }
    }
  } catch (e: Exception) {
    android.util.Log.e("HomeScreen", "❌ Erro ao abrir Downloader: ${e.message}", e)
    
    // Fallback: copiar URL e mostrar mensagem
    try {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("MaxiPTV Update URL", downloadUrl)
      clipboard.setPrimaryClip(clip)
      
      Toast.makeText(
        context,
        "URL copiada! Cole no Downloader manualmente.",
        Toast.LENGTH_LONG
      ).show()
    } catch (e2: Exception) {
      android.util.Log.e("HomeScreen", "❌ Erro ao copiar URL: ${e2.message}")
      Toast.makeText(
        context,
        "Erro ao abrir Downloader. Tente instalar manualmente.",
        Toast.LENGTH_SHORT
      ).show()
    }
  }
}

@Composable
fun HomeScreen(nav: NavHostController) {
  val liveChannels by XRepo.liveStreams.collectAsState(emptyList())
  val liveCategories by XRepo.liveCategories.collectAsState(emptyList())
  var focusedButton by remember { mutableStateOf<String?>(null) }
  var showExpiryWarning by remember { mutableStateOf(false) }
  var daysUntilExpiry by remember { mutableStateOf(0) }
  var isLoggingOut by remember { mutableStateOf(false) }
  var showLogoutDialog by remember { mutableStateOf(false) }
  var showLiveCarousel by remember { mutableStateOf(true) }
  var eventosCanal by remember { mutableStateOf<com.maxiptv.data.LiveStream?>(null) }
  var conteudosCanal by remember { mutableStateOf<com.maxiptv.data.LiveStream?>(null) }
  val scope = rememberCoroutineScope()
  
  // Estados para auto-update
  var updateAvailable by remember { mutableStateOf<UpdateInfo?>(null) }
  var showUpdateDialog by remember { mutableStateOf(false) }
  var isDownloading by remember { mutableStateOf(false) }
  var updateError by remember { mutableStateOf<String?>(null) }
  var showErrorDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current
  
  LaunchedEffect(Unit) {
    try {
      DeviceLogger.logDevice(context)
    } catch (e: Exception) {
      android.util.Log.e("HomeScreen", "Erro ao registrar dispositivo: ${e.message}")
    }
  }
  
  // Registrar callback de erro para Fire OS usando DisposableEffect
  DisposableEffect(MaxiApp.isFireStick) {
    if (MaxiApp.isFireStick) {
      ApkDownloader.setErrorCallback { errorMessage ->
        updateError = errorMessage
        showErrorDialog = true
        isDownloading = false
        android.util.Log.e("HomeScreen", "🔥 Erro de atualização no Fire OS: $errorMessage")
      }
    }
    onDispose {
      ApkDownloader.clearErrorCallback()
    }
  }
  
  // Verificar atualizações ao abrir o app
  // ✅ CORRIGIDO: Aumentar delay para garantir que PackageManager atualizou versão após instalação
  // ✅ Tentar múltiplas vezes antes de considerar que há atualização disponível
  LaunchedEffect(Unit) {
    try {
      // Aguardar 5 segundos para garantir que PackageManager atualizou versão após instalação
      // Isso é especialmente importante após uma atualização automática
      delay(5000)
      
      // Tentar verificar versão múltiplas vezes (até 3 tentativas) para garantir precisão
      var attempts = 0
      var update: UpdateInfo? = null
      
      while (attempts < 3 && update == null) {
        attempts++
        android.util.Log.i("HomeScreen", "🔍 Verificando atualizações (tentativa $attempts/3)...")
        
        val currentVersion = UpdateManager.getCurrentVersionName(context)
        val currentVersionCode = try {
          val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
          } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
          }
        } catch (e: Exception) {
          android.util.Log.w("HomeScreen", "⚠️ Erro ao obter versionCode na tentativa $attempts: ${e.message}")
          0
        }
        
        android.util.Log.i("HomeScreen", "📊 Versão atual instalada: $currentVersion (code: $currentVersionCode)")
        
        update = UpdateManager.checkForUpdate(context)
        
        if (update != null) {
          android.util.Log.i("HomeScreen", "🆕 Atualização disponível: ${update.version} (code: ${update.versionCode})")
          android.util.Log.i("HomeScreen", "   Versão atual: $currentVersion (code: $currentVersionCode)")
          
          // Verificar novamente se realmente precisa atualizar (pode ser cache do PackageManager)
          if (update.versionCode > currentVersionCode) {
            android.util.Log.i("HomeScreen", "✅ Confirmação: atualização realmente necessária")
            updateAvailable = update
            showUpdateDialog = true
            break
          } else {
            android.util.Log.i("HomeScreen", "⚠️ PackageManager ainda não atualizou - aguardando mais...")
            update = null
            if (attempts < 3) {
              delay(3000) // Aguardar mais 3 segundos antes da próxima tentativa
            }
          }
        } else {
          android.util.Log.i("HomeScreen", "✅ App está atualizado (tentativa $attempts)")
          break
        }
      }
      
      if (update == null && attempts >= 3) {
        android.util.Log.w("HomeScreen", "⚠️ Não foi possível confirmar status de atualização após 3 tentativas")
      }
    } catch (e: Exception) {
      android.util.Log.e("HomeScreen", "❌ Erro ao verificar update: ${e.message}", e)
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
  
  // 🔥 ESPAÇAMENTO INTERNO
  val horizontalPadding = when {
    isFireStick -> (MaxiApp.fireStickSafeAreaPadding / 2).dp.coerceAtLeast(12.dp)
    isTv -> 24.dp
    isPhone -> 16.dp
    else -> 20.dp
  }
  
  val verticalPadding = when {
    isFireStick -> (MaxiApp.fireStickSafeAreaPadding / 2).dp.coerceAtLeast(10.dp)
    isTv -> 14.dp
    isPhone -> 12.dp
    else -> 14.dp
  }
  
  // Dialog de confirmação de logout
  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
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
  
  // ✅ Dialog de atualização disponível (UNIFICADO com suporte Fire OS)
  if (showUpdateDialog && updateAvailable != null) {
    // 🔥 Detectar Fire OS para UI diferenciada
    val isFireOS = MaxiApp.isFireStick
    val primaryColor = if (isFireOS) Color(0xFFFF9800) else Color(0xFF00FF00) // Laranja Amazon vs Verde
    val iconColor = if (isFireOS) Color(0xFFFF9800) else Color(0xFF00FF00)
    
    AlertDialog(
      onDismissRequest = { showUpdateDialog = false },
      title = {
        Column {
          // 🔥 Badge Fire OS (apenas se for Fire OS)
          if (isFireOS) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFF9800), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                "🔥 FIRE OS AMAZON DETECTADO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
            Spacer(Modifier.height(12.dp))
          }
          
          // Título principal
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = null,
              tint = iconColor,
              modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("🆕 Atualização Disponível!", fontWeight = FontWeight.Bold)
          }
        }
      },
      text = {
        Column {
          Text(
            "Nova versão ${updateAvailable!!.version} disponível!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
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
          
          // 🔥 Instruções específicas para Fire OS
          if (isFireOS) {
            Spacer(Modifier.height(16.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                .padding(12.dp)
            ) {
              Column {
                Text(
                  "ℹ️ Fire OS detectado!",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFFF9800)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                  "O app Downloader será aberto automaticamente com a URL já preenchida.",
                  fontSize = 13.sp,
                  color = Color.White,
                  lineHeight = 18.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                  "Clique em GO no Downloader para baixar e instalar.",
                  fontSize = 13.sp,
                  color = Color(0xFFFFD700),
                  fontWeight = FontWeight.Bold,
                  lineHeight = 18.sp
                )
              }
            }
          }
          
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
            // 🔥 Fire OS: Abrir Downloader | Android: Download direto
            if (isFireOS) {
              openDownloaderApp(context, updateAvailable!!.downloadUrl)
            } else {
              isDownloading = true
              ApkDownloader.downloadAndInstall(
                context,
                updateAvailable!!.downloadUrl,
                updateAvailable!!.version
              )
            }
          },
          enabled = if (isFireOS) true else !isDownloading, // Fire OS sempre habilitado
          modifier = Modifier
            .onFocusChanged { isConfirmFocused = it.isFocused }
            .focusable()
            .then(
              if (isConfirmFocused) 
                Modifier
                  .border(4.dp, primaryColor, RoundedCornerShape(8.dp))
                  .shadow(
                    elevation = 15.dp,
                    spotColor = primaryColor.copy(alpha = 0.9f),
                    ambientColor = primaryColor.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                  )
              else 
                Modifier
            ),
          colors = ButtonDefaults.buttonColors(
            containerColor = primaryColor, // Laranja para Fire OS, Verde para Android
            contentColor = Color.Black
          )
        ) {
          if (!isFireOS && isDownloading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = Color.Black,
              strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
          } else {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = null
            )
            Spacer(Modifier.width(8.dp))
          }
          // 🔥 Texto diferente para Fire OS
          Text(
            if (isFireOS) "ABRIR DOWNLOADER" 
            else if (isDownloading) "BAIXANDO..." 
            else "ATUALIZAR AGORA",
            fontWeight = FontWeight.Bold
          )
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
  
  // Dialog de erro de atualização (apenas Fire OS)
  if (showErrorDialog && updateError != null && MaxiApp.isFireStick) {
    AlertDialog(
      onDismissRequest = { 
        showErrorDialog = false
        updateError = null
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFFF5722),
            modifier = Modifier.size(32.dp)
          )
          Spacer(Modifier.width(12.dp))
          Text(
            "Erro ao Atualizar",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFFFF5722)
          )
        }
      },
      text = {
        Column {
          Text(
            updateError ?: "Erro desconhecido",
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = Color.White
          )
          Spacer(Modifier.height(12.dp))
          Text(
            "O app não será fechado. Você pode tentar novamente ou instalar manualmente pelo arquivo baixado.",
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = Color.Gray
          )
        }
      },
      confirmButton = {
        var isOkFocused by remember { mutableStateOf(false) }
        Button(
          onClick = { 
            showErrorDialog = false
            updateError = null
          },
          modifier = Modifier
            .onFocusChanged { isOkFocused = it.isFocused }
            .focusable()
            .then(
              if (isOkFocused)
                Modifier
                  .border(4.dp, Color(0xFFFF5722), RoundedCornerShape(8.dp))
                  .shadow(
                    elevation = 15.dp,
                    spotColor = Color(0xFFFF5722).copy(alpha = 0.9f),
                    ambientColor = Color(0xFFFF5722).copy(alpha = 0.7f),
                    shape = RoundedCornerShape(8.dp)
                  )
              else
                Modifier
            ),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF5722)
          )
        ) {
          Text("ENTENDI", fontWeight = FontWeight.Bold)
        }
      },
      containerColor = Color(0xFF1A1A1A),
      titleContentColor = Color(0xFFFF5722),
      textContentColor = Color.White
    )
  }
  
  Box(
    modifier = Modifier.fillMaxSize()
  ) {
  Column(
      modifier = Modifier.fillMaxSize()
      ) {
      // Logo Max IPTV com Neon e Botão SAIR (TopBar removida)
      Box(
        modifier = Modifier
          .fillMaxWidthAdjusted() // ✅ Fire Stick/Native TV: 90% da largura real
          .padding(
            vertical = if (isFireStick) 12.dp else if (isTv) 8.dp else if (isPhone) 4.dp else 6.dp,
            horizontal = horizontalPadding
          )
      ) {
        // Logo à esquerda
        Row(
          modifier = Modifier.align(Alignment.CenterStart),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Start
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
        
        // Botão SAIR posicionado à direita (um pouco à direita do logo)
        LogoutButton(
          isFocused = focusedButton == "logout",
          deviceType = when {
            isFireStick -> "firestick"
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          isLoading = isLoggingOut,
          onFocusChanged = { focusedButton = if (it) "logout" else null },
          onClick = { showLogoutDialog = true },
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = if (isTv) 16.dp else if (isPhone) 8.dp else 12.dp)
        )
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
            .padding(horizontal = horizontalPadding)
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
          .fillMaxWidthAdjusted() // ✅ Fire Stick/Native TV: 90% da largura real
          .padding(
            horizontal = horizontalPadding
          ),
        horizontalArrangement = Arrangement.spacedBy(
          when {
            isFireStick -> 24.dp  // APENAS Fire Stick - mais espaço
            isTv -> 20.dp  // TV Box Android/Genéricos mantêm original
            isPhone -> 8.dp  // Smartphones mantêm original
            else -> 12.dp  // Tablets mantêm original
          }
        )
      ) {
        CategoryButton(
          text = "Live",
          emoji = "📡",
          isFocused = focusedButton == "live",
          deviceType = when {
            isFireStick -> "firestick"
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          onFocusChanged = { focusedButton = if (it) "live" else null },
          onClick = { nav.navigate("live") }
        )
        
        CategoryButton(
          text = "Filmes",
          emoji = "🎬",
          isFocused = focusedButton == "vod",
          deviceType = when {
            isFireStick -> "firestick"
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          onFocusChanged = { focusedButton = if (it) "vod" else null },
          onClick = { nav.navigate("vod") }
        )
        
        CategoryButton(
          text = "Séries",
          emoji = "📺",
          isFocused = focusedButton == "series",
          deviceType = when {
            isFireStick -> "firestick"
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          onFocusChanged = { focusedButton = if (it) "series" else null },
        onClick = { nav.navigate("series") }
        )
        
        CategoryButton(
          text = "Favoritos",
          emoji = "⭐",
          isFocused = focusedButton == "favorites",
          deviceType = when {
            isFireStick -> "firestick"
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          onFocusChanged = { focusedButton = if (it) "favorites" else null },
          onClick = { nav.navigate("favorites") }
        )
        
        CategoryButton(
          text = "Buscar",
          emoji = "🔍",
          isFocused = focusedButton == "search",
          deviceType = when {
            isFireStick -> "firestick"
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          onFocusChanged = { focusedButton = if (it) "search" else null },
          onClick = { nav.navigate("search") }
        )
        
        CategoryButton(
          text = "Config",
          emoji = "⚙️",
          isFocused = focusedButton == "settings",
          deviceType = when {
            isFireStick -> "firestick"
            isTv -> "tv"
            isPhone -> "phone"
            else -> "tablet"
          },
          onFocusChanged = { focusedButton = if (it) "settings" else null },
          onClick = { nav.navigate("player-settings") }
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
  // ✅ Novo design baseado na imagem: botões com ícones e texto abaixo
  CategoryButtonNew(
    text = text,
    isFocused = isFocused,
    deviceType = deviceType,
    onFocusChanged = onFocusChanged,
    onClick = onClick,
    modifier = modifier
  )
}

@Composable
fun CategoryButtonNew(
  text: String,
  isFocused: Boolean,
  deviceType: String,
  onFocusChanged: (Boolean) -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
  
  // ✅ Animação de zoom quando focado
  val scale by animateFloatAsState(
    targetValue = if (isFocused) 1.15f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "zoom"
  )
  
  // ✅ Ícones baseados na imagem (usando apenas ícones disponíveis no Material Icons)
  val icon = when (text.lowercase()) {
    "live" -> Icons.Filled.PlayArrow
    "filmes" -> Icons.Filled.PlayArrow // ✅ Play para filmes
    "séries", "series" -> Icons.Filled.PlayArrow // ✅ Play para séries
    "buscar" -> Icons.Filled.Search
    "config" -> Icons.Filled.Settings
    "favoritos" -> Icons.Filled.Star
    else -> Icons.Filled.PlayArrow // ✅ Fallback para PlayArrow
  }
  
  // ✅ Tamanhos proporcionais por dispositivo (baseado na imagem)
  val iconSize = when (deviceType) {
    "firestick" -> 48.dp
    "tv" -> 44.dp
    "phone" -> if (isLandscape) 24.dp else 32.dp
    else -> 36.dp
  }
  
  val buttonSize = when (deviceType) {
    "firestick" -> 120.dp  // Botão quadrado maior para TV
    "tv" -> 110.dp
    "phone" -> if (isLandscape) 60.dp else 80.dp
    else -> 90.dp
  }
  
  val textSize = when (deviceType) {
    "firestick" -> 16.sp
    "tv" -> 15.sp
    "phone" -> if (isLandscape) 10.sp else 12.sp
    else -> 13.sp
  }
  
  // ✅ Fundo escuro como na imagem
  val darkBackground = Color(0xFF1A1A1A) // Dark gray similar à imagem
  
  Box(
    modifier = modifier
      .size(buttonSize)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .onFocusChanged { onFocusChanged(it.isFocused) }
      .focusable()
      .clickable { onClick() }
      .shadow(
        elevation = if (isFocused) 12.dp else 4.dp,
        spotColor = Color(0xFF00D4FF).copy(alpha = if (isFocused) 0.8f else 0.3f),
        ambientColor = Color(0xFF00D4FF).copy(alpha = if (isFocused) 0.6f else 0.2f),
        shape = RoundedCornerShape(12.dp)
      )
      .then(
        if (isFocused)
          Modifier.border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(12.dp))
        else
          Modifier
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxSize()
    ) {
      // ✅ Ícone com brilho azul (baseado na imagem)
      Box(
        modifier = Modifier
          .size(iconSize)
          .background(darkBackground, RoundedCornerShape(12.dp))
          .shadow(
            elevation = 8.dp,
            spotColor = Color(0xFF00D4FF).copy(alpha = 0.9f),
            ambientColor = Color(0xFF00D4FF).copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = text,
          tint = Color(0xFF00D4FF), // ✅ Azul brilhante como na imagem
          modifier = Modifier.size(iconSize * 0.6f)
        )
      }
      
      Spacer(Modifier.height(8.dp))
      
      // ✅ Texto abaixo em azul brilhante (baseado na imagem)
      Text(
        text = text,
        fontSize = textSize,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        color = Color(0xFF00D4FF), // ✅ Azul brilhante como na imagem
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
        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
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
    "tv" -> 320.dp  // Reduzido de 400dp para 320dp (proporcional)
    "phone" -> 200.dp
    else -> 260.dp
  }
  
  
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
    "tv" -> 240.dp  // Reduzido de 300dp para 240dp (proporcional)
    "phone" -> 140.dp
    else -> 180.dp
  }
  
  // ⚽ ESTADO PARA CONTROLAR DIÁLOGO DE ESTATÍSTICAS
  var showFootballStatsDialog by remember { mutableStateOf(false) }
  
  // ⚽ DETECTAR SE É CANAL DE FUTEBOL
  val isFootballChannel = remember(channel.name) {
    val channelNameLower = channel.name.lowercase().trim()
    val isSpecificFootballChannel = com.maxiptv.data.soccer.MatchIdExtractor.isFootballChannel(channel.name)
    val genericTerms = listOf("sport", "futebol", "futbol")
    val hasGenericTerm = genericTerms.any { 
      channelNameLower.contains(it.lowercase()) && 
      !channelNameLower.contains("news") && 
      !channelNameLower.contains("noticias")
    }
    val isFootball = isSpecificFootballChannel || hasGenericTerm
    
    if (isFootball) {
      android.util.Log.i("EmbeddedPlayer", "⚽ CANAL DE FUTEBOL DETECTADO: ${channel.name}")
    }
    
    isFootball
  }
  
  // ⚽ EXTRAIR MATCH ID SE FOR FUTEBOL
  val matchId = remember(channel.name) {
    if (isFootballChannel) {
      com.maxiptv.data.soccer.MatchIdExtractor.extractMatchId(channel.name)
    } else {
      null
    }
  }
  
  // ⚽ ESTADOS PARA DADOS DE ESTATÍSTICAS (usando API Sports)
  var matchDetail by remember { mutableStateOf<com.maxiptv.data.soccer.MatchDetailFull?>(null) }
  var matchPreview by remember { mutableStateOf<com.maxiptv.data.soccer.MatchPreviewFull?>(null) }
  var otherMatches by remember { mutableStateOf<List<com.maxiptv.data.soccer.MatchSummaryFull>>(emptyList()) }
  var matchOdds by remember { mutableStateOf<com.maxiptv.data.soccer.ApiSportsOdds?>(null) }
  var isLoadingStats by remember { mutableStateOf(false) }
  var statsError by remember { mutableStateOf<String?>(null) }
  
  // ✅ CORRIGIDO: rememberCoroutineScope() precisa estar no nível do Composable
  val scope = rememberCoroutineScope()
  
  // ⚽ FUNÇÃO PARA ABRIR DIÁLOGO DE ESTATÍSTICAS
  val openStatsDialog: () -> Unit = {
    android.util.Log.i("EmbeddedPlayer", "⚽ Abrindo diálogo de estatísticas para matchId: $matchId")
    
    isLoadingStats = true
    statsError = null
    showFootballStatsDialog = true
    
    // Buscar dados da API
    scope.launch {
      try {
        android.util.Log.i("EmbeddedPlayer", "⚽ INICIANDO BUSCA NA API SPORTS")
        android.util.Log.i("EmbeddedPlayer", "   MatchId inicial: $matchId")
        android.util.Log.i("EmbeddedPlayer", "   Canal: ${channel.name}")
        
        // ✅ NOVO: Se não houver Match ID, usar busca inteligente para identificar a partida
        var finalMatchId = matchId
        if (finalMatchId == null) {
          android.util.Log.i("EmbeddedPlayer", "🔍 Match ID não encontrado, buscando automaticamente para o canal...")
          
          // Tentar buscar Match ID usando busca inteligente por canal
          finalMatchId = com.maxiptv.data.soccer.SoccerRepository.findMatchForChannel(channel.name)
          
          if (finalMatchId != null) {
            android.util.Log.i("EmbeddedPlayer", "   ✅ Match ID identificado automaticamente: $finalMatchId")
          } else {
            android.util.Log.w("EmbeddedPlayer", "   ⚠️ Não foi possível identificar a partida para este canal")
          }
        }
        
        if (finalMatchId == null) {
          android.util.Log.e("EmbeddedPlayer", "❌ Match ID não disponível - não é possível buscar estatísticas")
          statsError = "Partida não encontrada. Verifique se o jogo está ao vivo."
          isLoadingStats = false
          return@launch
        }
        
        // Buscar detalhes da partida
        android.util.Log.i("EmbeddedPlayer", "📡 1/3 - Buscando getMatchDetail($finalMatchId)...")
        val detail = com.maxiptv.data.soccer.SoccerRepository.getMatchDetail(finalMatchId)
        android.util.Log.i("EmbeddedPlayer", "   ✅ getMatchDetail retornou: ${detail?.homeTeamName} x ${detail?.awayTeamName}")
        matchDetail = detail
        
        // Buscar preview da partida
        android.util.Log.i("EmbeddedPlayer", "📡 2/3 - Buscando getMatchPreview($finalMatchId)...")
        val preview = com.maxiptv.data.soccer.SoccerRepository.getMatchPreview(finalMatchId)
        android.util.Log.i("EmbeddedPlayer", "   ✅ getMatchPreview retornou")
        matchPreview = preview
        
        // Buscar outros jogos ao vivo
        android.util.Log.i("EmbeddedPlayer", "📡 3/4 - Buscando getOtherMatches()...")
        val other = com.maxiptv.data.soccer.SoccerRepository.getOtherMatches()
        android.util.Log.i("EmbeddedPlayer", "   ✅ getOtherMatches retornou ${other.size} partidas")
        otherMatches = other
        
        // Buscar odds (probabilidades de apostas)
        android.util.Log.i("EmbeddedPlayer", "📡 4/4 - Buscando odds (probabilidades de apostas)...")
        val odds = com.maxiptv.data.soccer.SoccerRepository.getLiveOdds(finalMatchId) ?: com.maxiptv.data.soccer.SoccerRepository.getOdds(finalMatchId)
        if (odds != null) {
          android.util.Log.i("EmbeddedPlayer", "   ✅ Odds encontradas: ${odds.bookmakers?.size ?: 0} casas de aposta")
        } else {
          android.util.Log.w("EmbeddedPlayer", "   ⚠️ Nenhuma odd encontrada")
        }
        matchOdds = odds
        
        isLoadingStats = false
        android.util.Log.i("EmbeddedPlayer", "✅ TODAS AS ESTATÍSTICAS CARREGADAS COM SUCESSO!")
      } catch (e: Exception) {
        isLoadingStats = false
        statsError = e.message ?: "Erro desconhecido ao carregar estatísticas"
        android.util.Log.e("EmbeddedPlayer", "❌ Erro ao carregar estatísticas: ${e.message}", e)
      }
    }
  }
  
  // ✅ QUALIDADE ADAPTATIVA: Estado para rastrear qualidade e buffering (usar objeto mutável dentro do remember)
  val qualityState = remember { 
    object {
      var currentMaxBitrate = 2_200_000
      var qualityReductionLevel = 0
      var bufferingCount = 0
      var lastBufferingTime = 0L
      var connectionQuality = com.maxiptv.ui.player.ConnectionQuality.GOOD
    }
  }
  
  val exoPlayer = remember(channel.stream_id, isFootballChannel) {
    // ⚽ FUTEBOL: Configurações otimizadas (timeouts maiores, buffer maior)
    // ⚡ NORMAL: Configurações leves para carrossel
    val connectTimeout = if (isFootballChannel) 15000 else 6000  // ⚽ FUTEBOL: 15s vs 6s
    val readTimeout = if (isFootballChannel) 20000 else 6000       // ⚽ FUTEBOL: 20s vs 6s
    
    val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
      .setAllowCrossProtocolRedirects(true)
      .setUserAgent("MaxiPTV/1.1.1 (Android)")
      .setConnectTimeoutMs(connectTimeout)
      .setReadTimeoutMs(readTimeout)
      .setKeepPostFor302Redirects(true)
    
    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
      .setDataSourceFactory(dataSourceFactory)
    
    // ✅ QUALIDADE ADAPTATIVA: LoadControl dinâmico baseado em qualidade de conexão
    val initialLoadControl = com.maxiptv.ui.player.createAdaptiveLoadControl(qualityState.connectionQuality)
    
    androidx.media3.exoplayer.ExoPlayer.Builder(context)
      .setMediaSourceFactory(mediaSourceFactory)
      .setLoadControl(initialLoadControl)
      .build().apply {
        // ✅ QUALIDADE ADAPTATIVA: Configuração inicial de bitrate
        trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder(context)
          .setPreferredTextLanguage(null)
          .setMaxVideoBitrate(qualityState.currentMaxBitrate)
          .setMaxVideoSize(1280, 720) // 720p inicial
          .setMinVideoBitrate(500_000)
          .build()
        
        val mediaItem = androidx.media3.common.MediaItem.fromUri(channel.toLiveUrl())
        setMediaItem(mediaItem)
        volume = 0f // SEM ÁUDIO
        repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        
        // ✅ QUALIDADE ADAPTATIVA: Listener para detectar WiFi lento e reduzir qualidade automaticamente
        addListener(object : androidx.media3.common.Player.Listener {
          override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
              androidx.media3.common.Player.STATE_BUFFERING -> {
                val now = System.currentTimeMillis()
                val bufferAhead = bufferedPosition - currentPosition
                val timeSinceLastBuffering = if (qualityState.lastBufferingTime > 0) now - qualityState.lastBufferingTime else Long.MAX_VALUE
                
                // ✅ FATOR 1: Detecção de buffering frequente
                if (qualityState.lastBufferingTime > 0 && timeSinceLastBuffering < 5000) {
                  qualityState.bufferingCount++
                  android.util.Log.w("EmbeddedPlayer", "⚠️ Buffering frequente detectado (${qualityState.bufferingCount} eventos)")
                } else if (timeSinceLastBuffering > 10000) {
                  qualityState.bufferingCount = 0 // Reset se rede estável
                }
                
                // ✅ FATOR 2: Buffer muito baixo (< 2 segundos)
                val bufferLow = bufferAhead < 2000
                
                // ✅ FATOR 3: Estimativa de qualidade de conexão
                val latencyMs = bufferAhead.coerceAtLeast(0)
                val estimatedQuality = com.maxiptv.ui.player.estimateConnectionQuality(
                  this@apply,
                  latencyMs,
                  bufferAhead,
                  videoFormat?.bitrate ?: 0
                )
                qualityState.connectionQuality = estimatedQuality
                
                // ✅ REDUÇÃO AUTOMÁTICA DE QUALIDADE quando WiFi lento
                val shouldReduceQuality = when {
                  qualityState.bufferingCount >= 2 && (estimatedQuality == com.maxiptv.ui.player.ConnectionQuality.POOR || bufferLow) -> {
                    android.util.Log.w("EmbeddedPlayer", "🚨 Redução IMEDIATA: buffering frequente + conexão ruim")
                    true
                  }
                  qualityState.bufferingCount >= 2 || (bufferLow && estimatedQuality == com.maxiptv.ui.player.ConnectionQuality.POOR) -> {
                    android.util.Log.w("EmbeddedPlayer", "⚠️ Redução LEVE: buffering ou buffer baixo")
                    true
                  }
                  estimatedQuality == com.maxiptv.ui.player.ConnectionQuality.POOR && qualityState.qualityReductionLevel == 0 -> {
                    android.util.Log.w("EmbeddedPlayer", "📉 Redução PREVENTIVA: conexão ruim")
                    true
                  }
                  else -> false
                }
                
                // ✅ Aplicar redução de qualidade gradualmente (MESMA LÓGICA DO PLAYERACTIVITY PARA LIVE)
                if (shouldReduceQuality && qualityState.currentMaxBitrate > 800_000) {
                  // ✅ Mini player sempre é Live TV, usa mesmos valores do PlayerActivity para Live
                  val newBitrate = when (qualityState.qualityReductionLevel) {
                    0 -> 1_500_000  // Nível 1: 2.2Mbps → 1.5Mbps (LIVE)
                    1 -> 1_000_000  // Nível 2: 1.5Mbps → 1.0Mbps (LIVE)
                    2 -> 600_000    // Nível 3: 1.0Mbps → 600kbps (LIVE)
                    else -> qualityState.currentMaxBitrate
                  }
                  
                  if (newBitrate < qualityState.currentMaxBitrate) {
                    qualityState.qualityReductionLevel++
                    qualityState.currentMaxBitrate = newBitrate
                    
                    val newResolution = when (qualityState.qualityReductionLevel) {
                      1 -> Pair(1280, 720)  // 720p
                      2 -> Pair(854, 480)   // 480p
                      else -> Pair(640, 360) // 360p
                    }
                    
                    android.util.Log.i("EmbeddedPlayer", "📉 WiFi lento detectado! Reduzindo qualidade (nível ${qualityState.qualityReductionLevel})")
                    android.util.Log.i("EmbeddedPlayer", "   Bitrate: ${qualityState.currentMaxBitrate / 1000}kbps, Resolução: ${newResolution.first}x${newResolution.second}")
                    
                    // ✅ Aplicar novo bitrate automaticamente
                    trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder(context)
                      .setPreferredTextLanguage(null)
                      .setMaxVideoBitrate(qualityState.currentMaxBitrate)
                      .setMaxVideoSize(newResolution.first, newResolution.second)
                      .setMinVideoBitrate((qualityState.currentMaxBitrate * 0.3).toInt())
                      .build()
                  }
                }
                
                qualityState.lastBufferingTime = now
                android.util.Log.d("EmbeddedPlayer", "⏳ Buffering (contador: ${qualityState.bufferingCount}, buffer: ${bufferAhead}ms, qualidade: $estimatedQuality)")
              }
              androidx.media3.common.Player.STATE_READY -> {
                // ✅ RESTAURAR qualidade quando rede melhorar
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                  if (isPlaying && qualityState.bufferingCount == 0 && qualityState.connectionQuality != com.maxiptv.ui.player.ConnectionQuality.POOR) {
                    if (qualityState.qualityReductionLevel > 0) {
                      qualityState.qualityReductionLevel = 0
                      qualityState.currentMaxBitrate = 2_200_000
                      trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder(context)
                        .setPreferredTextLanguage(null)
                        .setMaxVideoBitrate(qualityState.currentMaxBitrate)
                        .setMaxVideoSize(1280, 720)
                        .setMinVideoBitrate(500_000)
                        .build()
                      android.util.Log.i("EmbeddedPlayer", "✅ Rede melhorou! Qualidade restaurada (${qualityState.currentMaxBitrate / 1000}kbps)")
                    }
                  }
                }, 30000) // 30 segundos de reprodução estável
              }
            }
          }
          
          override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            val format = videoFormat
            if (format != null) {
              android.util.Log.d("EmbeddedPlayer", "📊 Qualidade atual: ${format.width}x${format.height} @ ${format.bitrate / 1000}Kbps")
            }
          }
        })
        
        prepare()
        playWhenReady = true // INICIA TOCANDO AUTOMATICAMENTE
        android.util.Log.i("EmbeddedPlayer", "▶️ Player criado${if (isFootballChannel) " (MODO FUTEBOL)" else " (LEVE)"} para carrossel: ${channel.name}")
        android.util.Log.i("EmbeddedPlayer", "   URL: ${channel.toLiveUrl()}")
        android.util.Log.i("EmbeddedPlayer", "   ✅ Qualidade adaptativa HABILITADA")
        if (isFootballChannel && matchId != null) {
          android.util.Log.i("EmbeddedPlayer", "⚽ MatchId: $matchId")
        }
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
  
  // ⚽ Função para abrir em fullscreen
  val openFullscreen: () -> Unit = {
    android.util.Log.i("EmbeddedPlayer", "🖥️ Abrindo em fullscreen: ${channel.name}")
    val playerIntent = Intent(context, PlayerActivity::class.java)
      .putExtra("url", channel.toLiveUrl())
      .putExtra("contentType", "live")
      .putExtra("channelName", channel.name) // ⚽ Passar nome do canal para detecção de futebol
    
    // ⚽ Passar matchId se disponível
    if (matchId != null) {
      android.util.Log.i("EmbeddedPlayer", "⚽ Passando matchId para PlayerActivity: $matchId")
      // O PlayerActivity vai extrair o matchId do channelName, mas podemos passar também
    }
    
    context.startActivity(playerIntent)
  }
  
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(playerHeight)
      .padding(horizontal = if (deviceType == "tv") 48.dp else if (deviceType == "phone") 16.dp else 24.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(Color.Black)
      .border(3.dp, if (isFootballChannel) Color(0xFFFFD700) else Color(0xFF00D4FF), RoundedCornerShape(16.dp)) // ⚽ Dourado para futebol
      .shadow(
        elevation = 16.dp,
        spotColor = if (isFootballChannel) Color(0xFFFFD700).copy(alpha = 0.6f) else Color(0xFF00D4FF).copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp)
      )
      .clickable { openFullscreen() } // ✅ Adicionar clique para abrir em fullscreen
      .focusable() // ✅ Tornar focável para TV
      .onFocusChanged { focusState ->
        if (focusState.isFocused) {
          android.util.Log.d("EmbeddedPlayer", "🎯 Miniplayer focado")
        }
      },
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
    
    // Indicador de "AO VIVO" piscante (canto superior direito) - SEM FUNDO, APENAS TEXTO VERMELHO
    val infiniteTransition = rememberInfiniteTransition(label = "liveIndicator")
    val liveAlpha by infiniteTransition.animateFloat(
      initialValue = 0.7f,
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
    ) {
      Text(
        text = "● AO VIVO",
        fontSize = when (deviceType) {
          "tv" -> 14.sp
          "phone" -> 10.sp
          else -> 12.sp
        },
        fontWeight = FontWeight.Bold,
        color = Color(0xFFFF0000).copy(alpha = liveAlpha), // Vermelho piscante, sem fundo
        style = androidx.compose.ui.text.TextStyle(
          shadow = androidx.compose.ui.graphics.Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
            blurRadius = 3f
          )
        )
      )
    }
    
    // ⚽ Indicador de FUTEBOL (canto superior esquerdo, se for canal de futebol)
    if (isFootballChannel) {
      val footballGlow = rememberInfiniteTransition(label = "footballGlow")
      val footballAlpha by footballGlow.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(1000, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "footballAlpha"
      )
      
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(16.dp)
          .background(Color(0xFFFFD700).copy(alpha = footballAlpha), RoundedCornerShape(6.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          text = "⚽ FUTEBOL",
          fontSize = when (deviceType) {
            "tv" -> 14.sp
            "phone" -> 10.sp
            else -> 12.sp
          },
          fontWeight = FontWeight.Bold,
          color = Color.Black
        )
      }
    }
    
    // ⚽ BOTÃO DE ESTATÍSTICAS DE FUTEBOL (canto superior direito, abaixo do "AO VIVO")
    if (isFootballChannel) {
      var isFootballButtonFocused by remember { mutableStateOf(false) }
      
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(
            top = when (deviceType) {
              "tv" -> 60.dp  // Abaixo do badge "AO VIVO"
              "phone" -> 40.dp
              else -> 50.dp
            },
            end = 16.dp
          )
          .clickable { openStatsDialog() }
          .focusable()
          .onFocusChanged { isFootballButtonFocused = it.isFocused }
          .then(
            if (isFootballButtonFocused) {
              Modifier
                .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(36.dp))
                .shadow(
                  elevation = 16.dp,
                  spotColor = Color(0xFFFFD700).copy(alpha = 0.9f),
                  ambientColor = Color(0xFFFFD700).copy(alpha = 0.7f),
                  shape = CircleShape
                )
            } else {
              Modifier
            }
          )
      ) {
        SoccerStatsButton(
          onClick = { openStatsDialog() },
          modifier = Modifier.size(
            when (deviceType) {
              "tv" -> 56.dp
              "phone" -> 40.dp
              else -> 48.dp
            }
          )
        )
      }
    }
    
    // ✅ Botão de FULLSCREEN (canto inferior direito)
    var isFullscreenButtonFocused by remember { mutableStateOf(false) }
    
    Box(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .clickable { openFullscreen() }
        .focusable()
        .onFocusChanged { isFullscreenButtonFocused = it.isFocused }
        .then(
          if (isFullscreenButtonFocused) {
            Modifier
              .border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(8.dp))
              .shadow(
                elevation = 12.dp,
                spotColor = Color(0xFF00D4FF).copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp)
              )
          } else {
            Modifier
          }
        )
        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Text(
        text = "FULLSCREEN",
        fontSize = when (deviceType) {
          "tv" -> 14.sp
          "phone" -> 10.sp
          else -> 12.sp
        },
        fontWeight = FontWeight.Bold,
        color = Color(0xFF00D4FF)
      )
    }
    
    // ⚽ DIÁLOGO DE ESTATÍSTICAS DE FUTEBOL
    if (showFootballStatsDialog && isFootballChannel) {
      FootballStatsDialog(
        channelName = channel.name,
        matchId = matchId,
        matchDetail = matchDetail,
        matchPreview = matchPreview,
        otherMatches = otherMatches,
        matchOdds = matchOdds,
        isLoading = isLoadingStats,
        error = statsError,
        onDismiss = { 
          showFootballStatsDialog = false
          // Limpar dados quando fechar
          matchDetail = null
          matchPreview = null
          otherMatches = emptyList()
          matchOdds = null
          statsError = null
        },
        deviceType = deviceType,
        isVisible = showFootballStatsDialog // ✅ Passar estado de visibilidade para controlar foco
      )
    }
  }
}

// ⚽ DIÁLOGO DE ESTATÍSTICAS DE FUTEBOL PARA MINI PLAYER
@Composable
fun FootballStatsDialog(
  channelName: String,
  matchId: Long?,
  matchDetail: com.maxiptv.data.soccer.MatchDetailFull?,
  matchPreview: com.maxiptv.data.soccer.MatchPreviewFull?,
  otherMatches: List<com.maxiptv.data.soccer.MatchSummaryFull>,
  matchOdds: com.maxiptv.data.soccer.ApiSportsOdds?,
  isLoading: Boolean,
  error: String?,
  onDismiss: () -> Unit,
  deviceType: String,
  isVisible: Boolean = true // ✅ Novo parâmetro para rastrear quando o diálogo está visível
) {
  // ✅ FOCO D-PAD: FocusRequester para focar o botão de fechar quando o diálogo abrir
  val closeButtonFocusRequester = remember { FocusRequester() }
  var isCloseButtonFocused by remember { mutableStateOf(false) }
  val isTv = deviceType == "tv" || MaxiApp.isTv
  
  // ✅ FOCO AUTOMÁTICO: Focar o botão de fechar quando o diálogo abrir (apenas em TV)
  // Executar toda vez que o diálogo aparecer (isVisible muda para true)
  LaunchedEffect(isVisible) {
    if (isTv && isVisible) {
      android.util.Log.i("FootballStatsDialog", "⚽ Diálogo de estatísticas aberto - solicitando foco no botão FECHAR...")
      
      // Aguardar o diálogo estar totalmente renderizado e visível
      kotlinx.coroutines.delay(600) // Delay para garantir que o AlertDialog está totalmente renderizado
      
      // Múltiplas tentativas para garantir que o foco seja aplicado
      repeat(8) { attempt ->
        try {
          closeButtonFocusRequester.requestFocus()
          android.util.Log.i("FootballStatsDialog", "✅ Tentativa ${attempt + 1}/8: Foco D-pad solicitado no botão FECHAR (TV)")
          kotlinx.coroutines.delay(100) // Pequeno delay entre tentativas
        } catch (e: Exception) {
          android.util.Log.e("FootballStatsDialog", "❌ Erro na tentativa ${attempt + 1} ao focar botão FECHAR", e)
        }
      }
    }
  }
  
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0F0F0F), // Fundo mais escuro e elegante
    titleContentColor = Color(0xFFFFD700),
    textContentColor = Color.White,
    shape = RoundedCornerShape(20.dp), // Bordas mais arredondadas
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "⚽ $channelName",
          fontSize = when (deviceType) {
            "tv" -> 22.sp
            "phone" -> 17.sp
            else -> 19.sp
          },
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.SansSerif,
          color = Color(0xFFFFD700),
          letterSpacing = 0.5.sp,
          style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
              color = Color.Black.copy(alpha = 0.8f),
              offset = androidx.compose.ui.geometry.Offset(2f, 2f),
              blurRadius = 6f
            )
          )
        )
        if (matchId != null) {
          Spacer(Modifier.width(8.dp))
          Text(
            text = "(Match ID: $matchId)",
            fontSize = when (deviceType) {
              "tv" -> 12.sp
              "phone" -> 10.sp
              else -> 11.sp
            },
            color = Color.Gray
          )
        }
      }
    },
    text = {
      // ✅ SCROLL COM D-PAD: ScrollState otimizado para navegação com D-pad em TV
      val scrollState = rememberScrollState()
      
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(scrollState)
          // ✅ NÃO tornar o conteúdo focável inicialmente - o botão FECHAR deve receber foco primeiro
          // O scroll funcionará mesmo sem focusable, quando o usuário navegar para dentro do conteúdo
      ) {
        if (isLoading) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(
              color = Color(0xFFFFD700),
              modifier = Modifier.size(when (deviceType) {
                "tv" -> 48.dp
                "phone" -> 32.dp
                else -> 40.dp
              })
            )
          }
          Spacer(Modifier.height(8.dp))
          Text(
            text = "Carregando estatísticas...",
            fontSize = when (deviceType) {
              "tv" -> 18.sp
              "phone" -> 14.sp
              else -> 16.sp
            },
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif,
            color = Color(0xFFFFD700),
            letterSpacing = 0.3.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = androidx.compose.ui.text.TextStyle(
              shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.6f),
                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                blurRadius = 3f
              )
            )
          )
        } else if (error != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = Color(0xFFFF5252),
              modifier = Modifier.size(when (deviceType) {
                "tv" -> 24.dp
                "phone" -> 20.dp
                else -> 22.dp
              })
            )
            Spacer(Modifier.width(8.dp))
            Text(
              text = error,
              fontSize = when (deviceType) {
                "tv" -> 16.sp
                "phone" -> 12.sp
                else -> 14.sp
              },
              color = Color(0xFFFF5252)
            )
          }
          Spacer(Modifier.height(8.dp))
          Text(
            text = "Verifique se o MatchId está correto no nome do canal ou tente novamente.",
            fontSize = when (deviceType) {
              "tv" -> 14.sp
              "phone" -> 10.sp
              else -> 12.sp
            },
            color = Color.Gray
          )
        } else if (matchDetail != null) {
          // ============================================================
          // CABEÇALHO: Times e Status
          // ============================================================
          Text(
            text = "${matchDetail.homeTeamName} X ${matchDetail.awayTeamName}",
            fontSize = when (deviceType) {
              "tv" -> 22.sp
              "phone" -> 17.sp
              else -> 19.sp
            },
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            color = Color.White,
            letterSpacing = 0.8.sp,
            style = androidx.compose.ui.text.TextStyle(
              shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.9f),
                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                blurRadius = 8f
              )
            )
          )
          
          // Status da partida
          matchDetail.status?.let { status ->
            Spacer(Modifier.height(4.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = status.long ?: status.short ?: "",
                fontSize = when (deviceType) {
                  "tv" -> 16.sp
                  "phone" -> 13.sp
                  else -> 15.sp
                },
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFFFFD700),
                letterSpacing = 0.5.sp,
                style = androidx.compose.ui.text.TextStyle(
                  shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.7f),
                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                    blurRadius = 4f
                  )
                )
              )
              if (status.elapsed != null) {
                Text(
                  text = "${status.elapsed}'",
                  fontSize = when (deviceType) {
                    "tv" -> 16.sp
                    "phone" -> 13.sp
                    else -> 15.sp
                  },
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif,
                  color = Color(0xFF64B5F6),
                  letterSpacing = 0.3.sp,
                  style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                      color = Color.Black.copy(alpha = 0.6f),
                      offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                      blurRadius = 3f
                    )
                  )
                )
              }
            }
          }
          
          // ============================================================
          // PLACAR DESTACADO
          // ============================================================
          matchDetail.score?.let { score ->
            Spacer(Modifier.height(16.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  brush = Brush.linearGradient(
                    colors = listOf(
                      Color(0xFF1A1A1A),
                      Color(0xFF0F0F0F)
                    )
                  ),
                  shape = RoundedCornerShape(16.dp)
                )
                .border(3.dp, Color(0xFFFFD700).copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .shadow(
                  elevation = 16.dp,
                  spotColor = Color(0xFFFFD700).copy(alpha = 0.3f),
                  shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp),
              contentAlignment = Alignment.Center
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${score.home ?: score.current?.home ?: 0}",
                  fontSize = when (deviceType) {
                    "tv" -> 36.sp
                    "phone" -> 28.sp
                    else -> 32.sp
                  },
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.SansSerif,
                  color = Color(0xFF4CAF50),
                  letterSpacing = 1.sp,
                  style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                      color = Color.Black.copy(alpha = 0.9f),
                      offset = androidx.compose.ui.geometry.Offset(3f, 3f),
                      blurRadius = 10f
                    )
                  )
                )
                Text(
                  text = "X",
                  fontSize = when (deviceType) {
                    "tv" -> 24.sp
                    "phone" -> 18.sp
                    else -> 22.sp
                  },
                  color = Color.Gray
                )
                Text(
                  text = "${score.away ?: score.current?.away ?: 0}",
                  fontSize = when (deviceType) {
                    "tv" -> 36.sp
                    "phone" -> 28.sp
                    else -> 32.sp
                  },
                  fontWeight = FontWeight.ExtraBold,
                  fontFamily = FontFamily.SansSerif,
                  color = Color(0xFFFF5252),
                  letterSpacing = 1.sp,
                  style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                      color = Color.Black.copy(alpha = 0.9f),
                      offset = androidx.compose.ui.geometry.Offset(3f, 3f),
                      blurRadius = 10f
                    )
                  )
                )
              }
            }
          }
          
          Spacer(Modifier.height(16.dp))
          
          // ============================================================
          // POSSE DE BOLA (GRÁFICO DE PIZZA)
          // ============================================================
          val possessionStat = matchDetail.statistics?.find { 
            it.type?.lowercase()?.contains("possession") == true || 
            it.type?.lowercase()?.contains("posse") == true 
          }
          if (possessionStat != null) {
            val homePoss = possessionStat.home?.replace("%", "")?.toIntOrNull() ?: 0
            val awayPoss = possessionStat.away?.replace("%", "")?.toIntOrNull() ?: 0
            if (homePoss > 0 || awayPoss > 0) {
              PossessionPieChart(
                homePossession = homePoss,
                awayPossession = awayPoss,
                homeTeamName = matchDetail.homeTeamName,
                awayTeamName = matchDetail.awayTeamName,
                deviceType = deviceType
              )
            }
          }
          
          // ============================================================
          // ESTATÍSTICAS PRINCIPAIS (GRÁFICOS DE BARRAS)
          // ============================================================
          val mainStats = matchDetail.statistics?.filter { stat ->
            val statType = stat.type?.lowercase() ?: ""
            // Estatísticas principais que ficam bem em gráficos de barras
            statType.contains("shots") || 
            statType.contains("on target") ||
            statType.contains("corner") ||
            statType.contains("foul") ||
            statType.contains("offside") ||
            statType.contains("yellow card") ||
            statType.contains("red card") ||
            statType.contains("attack") ||
            statType.contains("dangerous attack")
          } ?: emptyList()
          
          if (mainStats.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
              text = "📊 Estatísticas Detalhadas",
              fontSize = when (deviceType) {
                "tv" -> 18.sp
                "phone" -> 14.sp
                else -> 16.sp
              },
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              color = Color(0xFFFFD700),
              letterSpacing = 0.5.sp,
              style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                  color = Color.Black.copy(alpha = 0.7f),
                  offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                  blurRadius = 4f
                )
              )
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Mostrar estatísticas principais com gráficos de barras
            mainStats.forEach { stat ->
              val homeVal = stat.home?.replace("%", "")?.toIntOrNull() ?: 0
              val awayVal = stat.away?.replace("%", "")?.toIntOrNull() ?: 0
              
              if (homeVal > 0 || awayVal > 0) {
                // Determinar valor máximo para o gráfico (20% de margem)
                val maxValue = (maxOf(homeVal, awayVal, 10) * 1.2f).toInt()
                
                StatisticBarChart(
                  label = stat.type ?: "",
                  homeValue = homeVal,
                  awayValue = awayVal,
                  maxValue = maxValue,
                  deviceType = deviceType
                )
              }
            }
          }
          
          // ============================================================
          // OUTRAS ESTATÍSTICAS (TEXTO SIMPLES)
          // ============================================================
          val otherStats = matchDetail.statistics?.filter { stat ->
            val statType = stat.type?.lowercase() ?: ""
            val isPossession = statType.contains("possession") || statType.contains("posse")
            val isMainStat = statType.contains("shots") || 
                           statType.contains("on target") ||
                           statType.contains("corner") ||
                           statType.contains("foul") ||
                           statType.contains("offside") ||
                           statType.contains("yellow card") ||
                           statType.contains("red card") ||
                           statType.contains("attack") ||
                           statType.contains("dangerous attack")
            !isPossession && !isMainStat
          } ?: emptyList()
          
          if (otherStats.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
              text = "📈 Outras Estatísticas",
              fontSize = when (deviceType) {
                "tv" -> 18.sp
                "phone" -> 14.sp
                else -> 16.sp
              },
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              color = Color(0xFFFFD700),
              letterSpacing = 0.5.sp,
              style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                  color = Color.Black.copy(alpha = 0.7f),
                  offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                  blurRadius = 4f
                )
              )
            )
            
            Spacer(Modifier.height(8.dp))
            
            otherStats.forEach { stat ->
              val homeVal = stat.home?.replace("%", "")?.toIntOrNull() ?: 0
              val awayVal = stat.away?.replace("%", "")?.toIntOrNull() ?: 0
              
              if (homeVal > 0 || awayVal > 0 || stat.home?.contains("%") == true || stat.away?.contains("%") == true) {
                StatsRow(
                  label = "${stat.type ?: ""}:",
                  homeValue = stat.home ?: "0",
                  awayValue = stat.away ?: "0",
                  deviceType = deviceType
                )
              }
            }
          }
          
          // ============================================================
          // EVENTOS DA PARTIDA (Gols, Cartões, Substituições)
          // ============================================================
          if (!matchDetail.events.isNullOrEmpty()) {
            Spacer(Modifier.height(16.dp))
          Text(
            text = "⚽ Eventos da Partida",
            fontSize = when (deviceType) {
              "tv" -> 18.sp
              "phone" -> 14.sp
              else -> 16.sp
            },
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = Color(0xFFFFD700),
            letterSpacing = 0.5.sp,
            style = androidx.compose.ui.text.TextStyle(
              shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.7f),
                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                blurRadius = 4f
              )
            )
          )
            
            Spacer(Modifier.height(8.dp))
            
            matchDetail.events.sortedBy { it.time?.elapsed ?: 0 }.forEach { event ->
              val minute = event.time?.elapsed ?: 0
              val extra = event.time?.extra
              val timeStr = if (extra != null && extra > 0) "$minute+$extra'" else "${minute}'"
              
              val icon = when (event.type?.lowercase()) {
                "goal" -> "⚽"
                "card" -> if (event.detail?.contains("Yellow", ignoreCase = true) == true) "🟨" else "🟥"
                "subst" -> "🔄"
                else -> "•"
              }
              
              val eventText = when (event.type?.lowercase()) {
                "goal" -> "$icon $timeStr - ${event.player?.name ?: "Gol"} (${event.team?.name ?: ""})${if (event.assist != null) " | Assistência: ${event.assist.name}" else ""}"
                "card" -> "$icon $timeStr - ${event.player?.name ?: ""} (${event.team?.name ?: ""}) - ${event.detail ?: ""}"
                "subst" -> "$icon $timeStr - ${event.player?.name ?: ""} entra, ${event.assist?.name ?: ""} sai (${event.team?.name ?: ""})"
                else -> "$icon $timeStr - ${event.detail ?: ""} (${event.team?.name ?: ""})"
              }
              
              Text(
                text = eventText,
                fontSize = when (deviceType) {
                  "tv" -> 13.sp
                  "phone" -> 10.sp
                  else -> 12.sp
                },
                color = Color.White,
                modifier = Modifier.padding(vertical = 2.dp)
              )
            }
          }
          
          // ============================================================
          // FORMAÇÕES
          // ============================================================
          matchDetail.formation?.let { formation ->
            Spacer(Modifier.height(16.dp))
            Text(
              text = "📐 Formações Táticas",
              fontSize = when (deviceType) {
                "tv" -> 18.sp
                "phone" -> 14.sp
                else -> 16.sp
              },
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              color = Color(0xFFFFD700),
              letterSpacing = 0.5.sp,
              style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                  color = Color.Black.copy(alpha = 0.7f),
                  offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                  blurRadius = 4f
                )
              )
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
              text = "${matchDetail.homeTeamName}: ${formation.home ?: "N/A"}",
              fontSize = when (deviceType) {
                "tv" -> 13.sp
                "phone" -> 11.sp
                else -> 12.sp
              },
              color = Color(0xFF4CAF50)
            )
            
            Text(
              text = "${matchDetail.awayTeamName}: ${formation.away ?: "N/A"}",
              fontSize = when (deviceType) {
                "tv" -> 13.sp
                "phone" -> 11.sp
                else -> 12.sp
              },
              color = Color(0xFFFF5252)
            )
          }
          
          // ============================================================
          // PREVIEW: CLIMA E PREDIÇÕES
          // ============================================================
          matchPreview?.match_data?.let { previewData ->
            Spacer(Modifier.height(16.dp))
            Text(
              text = "🌤️ Preview da Partida",
              fontSize = when (deviceType) {
                "tv" -> 18.sp
                "phone" -> 14.sp
                else -> 16.sp
              },
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              color = Color(0xFFFFD700),
              letterSpacing = 0.5.sp,
              style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                  color = Color.Black.copy(alpha = 0.7f),
                  offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                  blurRadius = 4f
                )
              )
            )
            
            Spacer(Modifier.height(8.dp))
            
            previewData.weather?.let { weather ->
              Text(
                text = "Clima: ${weather.description ?: ""} - ${weather.temp_c?.toInt() ?: 0}°C",
                fontSize = when (deviceType) {
                  "tv" -> 13.sp
                  "phone" -> 11.sp
                  else -> 12.sp
                },
                color = Color.White
              )
            }
            
            if (previewData.excitement_rating != null) {
              Text(
                text = "⭐ Rating: ${String.format("%.1f", previewData.excitement_rating)}/10",
                fontSize = when (deviceType) {
                  "tv" -> 13.sp
                  "phone" -> 11.sp
                  else -> 12.sp
                },
                color = Color(0xFFFFD700)
              )
            }
            
            previewData.prediction?.let { prediction ->
              Text(
                text = "🎯 Predição: ${prediction.choice ?: ""}",
                fontSize = when (deviceType) {
                  "tv" -> 13.sp
                  "phone" -> 11.sp
                  else -> 12.sp
                },
                color = Color(0xFF4CAF50)
              )
            }
          }
          
          // ============================================================
          // OUTROS JOGOS AO VIVO
          // ============================================================
          if (otherMatches.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
              text = "📺 Outros Jogos ao Vivo",
              fontSize = when (deviceType) {
                "tv" -> 18.sp
                "phone" -> 14.sp
                else -> 16.sp
              },
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              color = Color(0xFFFFD700),
              letterSpacing = 0.5.sp,
              style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                  color = Color.Black.copy(alpha = 0.7f),
                  offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                  blurRadius = 4f
                )
              )
            )
            
            Spacer(Modifier.height(8.dp))
            
            otherMatches.take(3).forEach { match ->
              val scoreText = if (match.score != null) {
                " (${match.score.home ?: 0} x ${match.score.away ?: 0})"
              } else {
                ""
              }
              
              Text(
                text = "• ${match.homeTeamName} X ${match.awayTeamName}$scoreText",
                fontSize = when (deviceType) {
                  "tv" -> 13.sp
                  "phone" -> 10.sp
                  else -> 12.sp
                },
                color = Color.Gray
              )
            }
            
            if (otherMatches.size > 3) {
              Text(
                text = "... e mais ${otherMatches.size - 3} jogos",
                fontSize = when (deviceType) {
                  "tv" -> 12.sp
                  "phone" -> 9.sp
                  else -> 11.sp
                },
                color = Color.Gray,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
              )
            }
          }
          
          // ============================================================
          // ODDS (PROBABILIDADES DE APOSTAS) - CARDS MELHORADOS
          // ============================================================
          if (matchOdds != null && !matchOdds.bookmakers.isNullOrEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
              text = "💰 Odds e Probabilidades",
              fontSize = when (deviceType) {
                "tv" -> 18.sp
                "phone" -> 14.sp
                else -> 16.sp
              },
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.SansSerif,
              color = Color(0xFFFFD700),
              letterSpacing = 0.5.sp,
              style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                  color = Color.Black.copy(alpha = 0.7f),
                  offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                  blurRadius = 4f
                )
              )
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Mostrar até 3 casas de aposta principais com cards
            matchOdds.bookmakers!!.take(3).forEach { bookmaker ->
              Spacer(Modifier.height(12.dp))
              
              // Card da casa de aposta
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(
                    Color(0xFF2A2A2A),
                    RoundedCornerShape(12.dp)
                  )
                  .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                Column {
                  // Nome da casa de aposta
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = "🏢",
                      fontSize = when (deviceType) {
                        "tv" -> 18.sp
                        "phone" -> 14.sp
                        else -> 16.sp
                      },
                      modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                      text = bookmaker.name ?: "Casa de Aposta",
                      fontSize = when (deviceType) {
                        "tv" -> 15.sp
                        "phone" -> 12.sp
                        else -> 13.sp
                      },
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF4CAF50)
                    )
                  }
                  
                  Spacer(Modifier.height(8.dp))
                  
                  // Tipos de aposta principais (Match Winner, Over/Under, etc)
                  bookmaker.bets?.filter { bet ->
                    bet.name?.contains("Match Winner", ignoreCase = true) == true ||
                    bet.name?.contains("Over/Under", ignoreCase = true) == true ||
                    bet.name?.contains("Both Teams", ignoreCase = true) == true ||
                    bet.name?.contains("Double Chance", ignoreCase = true) == true
                  }?.take(3)?.forEach { bet ->
                    Spacer(Modifier.height(8.dp))
                    
                    // Função para traduzir nome do tipo de aposta
                    fun translateBetName(betName: String?): String {
                      return when {
                        betName == null -> ""
                        betName.contains("Match Winner", ignoreCase = true) -> "Vencedor da Partida"
                        betName.contains("Over/Under", ignoreCase = true) -> "Mais/Menos Gols"
                        betName.contains("Both Teams Score", ignoreCase = true) -> "Ambos Marcam"
                        betName.contains("Double Chance", ignoreCase = true) -> "Dupla Chance"
                        else -> betName
                      }
                    }
                    
                    // Função para traduzir valores das odds
                    fun translateOddValue(value: String?): String {
                      if (value == null) return ""
                      
                      val valueLower = value.lowercase()
                      
                      return when {
                        valueLower == "home" -> "Casa"
                        valueLower == "away" -> "Visitante"
                        valueLower == "draw" -> "Empate"
                        valueLower == "yes" -> "Sim"
                        valueLower == "no" -> "Não"
                        valueLower.contains("home/draw") -> "Casa/Empate"
                        valueLower.contains("home/away") -> "Casa/Visitante"
                        valueLower.contains("draw/away") -> "Empate/Visitante"
                        valueLower.contains("over") -> value.replace(Regex("over", RegexOption.IGNORE_CASE), "Mais")
                        valueLower.contains("under") -> value.replace(Regex("under", RegexOption.IGNORE_CASE), "Menos")
                        else -> value
                      }
                    }
                    
                    // Nome do tipo de aposta (traduzido)
                    Text(
                      text = translateBetName(bet.name),
                      fontSize = when (deviceType) {
                        "tv" -> 13.sp
                        "phone" -> 10.sp
                        else -> 11.sp
                      },
                      color = Color.White,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    // Valores das odds em cards pequenos
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      bet.values?.take(3)?.forEach { value ->
                        Box(
                          modifier = Modifier
                            .weight(1f)
                            .background(
                              Color(0xFF1A1A1A),
                              RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp, horizontal = 8.dp)
                        ) {
                          Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                          ) {
                            Text(
                              text = translateOddValue(value.value),
                              fontSize = when (deviceType) {
                                "tv" -> 10.sp
                                "phone" -> 8.sp
                                else -> 9.sp
                              },
                              color = Color.Gray
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                              text = value.odd ?: "",
                              fontSize = when (deviceType) {
                                "tv" -> 14.sp
                                "phone" -> 11.sp
                                else -> 12.sp
                              },
                              fontWeight = FontWeight.Bold,
                              color = Color(0xFFFFD700)
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            
            // Aviso legal
            Spacer(Modifier.height(12.dp))
            Text(
              text = "ℹ️ Informações apenas para fins informativos",
              fontSize = when (deviceType) {
                "tv" -> 10.sp
                "phone" -> 8.sp
                else -> 9.sp
              },
              color = Color.Gray,
              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
          }
        } else {
          Text(
            text = "Estatísticas não disponíveis no momento.",
            fontSize = when (deviceType) {
              "tv" -> 16.sp
              "phone" -> 12.sp
              else -> 14.sp
            },
            color = Color.Gray
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isCloseButtonFocused && isTv) Color(0xFF00D4FF) else Color(0xFFFFD700),
          contentColor = Color.Black
        ),
        modifier = Modifier
          .focusRequester(closeButtonFocusRequester)
          .focusable()
          .onFocusChanged { 
            isCloseButtonFocused = it.isFocused
            if (isCloseButtonFocused && isTv) {
              android.util.Log.i("FootballStatsDialog", "✅ Botão FECHAR recebeu foco (D-pad)")
            }
          }
          .then(
            if (isCloseButtonFocused && isTv) {
              Modifier
                .border(3.dp, Color(0xFF00D4FF), RoundedCornerShape(8.dp))
                .shadow(
                  elevation = 12.dp,
                  spotColor = Color(0xFF00D4FF).copy(alpha = 0.9f),
                  shape = RoundedCornerShape(8.dp)
                )
            } else {
              Modifier
            }
          )
      ) {
        Text(
          text = "FECHAR",
          fontWeight = FontWeight.ExtraBold,
          fontFamily = FontFamily.SansSerif,
          fontSize = when (deviceType) {
            "tv" -> 18.sp
            "phone" -> 14.sp
            else -> 16.sp
          },
          letterSpacing = 1.sp
        )
      }
    },
    modifier = Modifier
      .fillMaxWidth(if (deviceType == "tv") 0.85f else if (deviceType == "phone") 0.95f else 0.9f)
  )
}

@Composable
fun StatsRow(
  label: String,
  homeValue: String,
  awayValue: String,
  deviceType: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      fontSize = when (deviceType) {
        "tv" -> 15.sp
        "phone" -> 12.sp
        else -> 14.sp
      },
      fontWeight = FontWeight.Medium,
      fontFamily = FontFamily.SansSerif,
      color = Color(0xFFCFD8DC),
      letterSpacing = 0.2.sp,
      modifier = Modifier.weight(1f),
      style = androidx.compose.ui.text.TextStyle(
        shadow = androidx.compose.ui.graphics.Shadow(
          color = Color.Black.copy(alpha = 0.5f),
          offset = androidx.compose.ui.geometry.Offset(1f, 1f),
          blurRadius = 3f
        )
      )
    )
    
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = homeValue,
        fontSize = when (deviceType) {
          "tv" -> 16.sp
          "phone" -> 13.sp
          else -> 15.sp
        },
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        color = Color(0xFF4CAF50),
        letterSpacing = 0.3.sp,
        style = androidx.compose.ui.text.TextStyle(
          shadow = androidx.compose.ui.graphics.Shadow(
            color = Color.Black.copy(alpha = 0.6f),
            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
            blurRadius = 4f
          )
        )
      )
      
      Text(
        text = "X",
        fontSize = when (deviceType) {
          "tv" -> 15.sp
          "phone" -> 12.sp
          else -> 14.sp
        },
        fontWeight = FontWeight.Bold,
        color = Color(0xFF78909C),
        letterSpacing = 1.sp
      )
      
      Text(
        text = awayValue,
        fontSize = when (deviceType) {
          "tv" -> 16.sp
          "phone" -> 13.sp
          else -> 15.sp
        },
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        color = Color(0xFFFF5252),
        letterSpacing = 0.3.sp,
        style = androidx.compose.ui.text.TextStyle(
          shadow = androidx.compose.ui.graphics.Shadow(
            color = Color.Black.copy(alpha = 0.6f),
            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
            blurRadius = 4f
          )
        )
      )
    }
  }
}

package com.maxiptv.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.maxiptv.MaxiApp
import com.maxiptv.data.XRepo
import com.maxiptv.data.LiveStream
import com.maxiptv.ui.player.PlayerActivity
import coil.compose.AsyncImage
import android.content.Intent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog

@Composable
fun LiveScreen(nav: NavHostController) {
  val cats by XRepo.liveCategories.collectAsState(emptyList())
  val streams by XRepo.liveStreams.collectAsState(emptyList())
  var selectedCat by remember { mutableStateOf<String?>(null) }
  var current by remember { mutableStateOf<LiveStream?>(null) }
  
  // ✅ Estados para PIN de categoria adulta
  var showPinDialog by remember { mutableStateOf(false) }
  var pinInput by remember { mutableStateOf("") }
  var showPinError by remember { mutableStateOf(false) }
  var isAdultUnlocked by remember { mutableStateOf(false) }
  var pendingAdultCategory by remember { mutableStateOf<String?>(null) }
  
  LaunchedEffect(Unit) { 
    XRepo.ensureLiveLoaded()
  }
  
  // ✅ Filtrar categorias adultas (buscar por XXX, ADULTO, 18+)
  val adultCategoryIds = listOf("18", "82", "80", "79", "78", "81", "ADULT", "XXX")
  val normalCats = cats.filter { 
    val isAdult = it.category_id in adultCategoryIds || 
                  it.category_name.contains(Regex("(?i)(adult|xxx|18\\+|porn|sex)"))
    !isAdult
  }
  
  // ✅ Adicionar categoria adulta no início
  val categoriesWithAdult = listOf("🔞 ADULTO" to "ADULT") + normalCats.map { it.category_name to it.category_id }
  
  Column(Modifier.fillMaxSize()) {
    CategoryChips(
      categories = categoriesWithAdult, 
      selectedId = selectedCat, 
      onSelect = { catId ->
        if (catId == "ADULT") {
          // ✅ Verificar PIN para categoria adulta
          if (isAdultUnlocked) {
            // Já desbloqueado, mostrar canais adultos
            selectedCat = "ADULT"
          } else {
            // Mostrar dialog de PIN
            pendingAdultCategory = catId
            showPinDialog = true
            pinInput = ""
            showPinError = false
          }
        } else {
          selectedCat = catId
        }
      }
    )
    val context = LocalContext.current
    val isTv = MaxiApp.isTv
    
    if (isTv) {
      // 📺 Layout TV com Mini Player
      Row(Modifier.weight(1f)) {
        // Lista de canais (lado esquerdo)
        Surface(tonalElevation = 2.dp, modifier = Modifier.width(380.dp).fillMaxHeight()) {
          val filtered = when {
            selectedCat == "ADULT" && isAdultUnlocked -> {
              streams.filter { 
                it.category_id in adultCategoryIds || 
                it.name.contains(Regex("(?i)(adult|xxx|18\\+|porn|sex)"))
              }
            }
            selectedCat == null -> streams
            else -> streams.filter { it.category_id == selectedCat }
          }
          
          val headlineSize = 18.sp
          val supportingSize = 14.sp
          val iconSize = 48.dp
          
          LazyColumn { 
            items(filtered) { s ->
              ListItem(
                headlineContent = { 
                  Text(
                    text = s.name,
                    fontSize = headlineSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                  ) 
                }, 
                supportingContent = { 
                  Text(
                    text = s.categoryName ?: "-",
                    fontSize = supportingSize,
                    fontFamily = FontFamily.SansSerif
                  ) 
                },
                leadingContent = {
                  Box(
                    modifier = Modifier
                      .size(iconSize + 8.dp)
                      .padding(4.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    AsyncImage(
                      model = s.stream_icon,
                      contentDescription = s.name,
                      modifier = Modifier
                        .size(iconSize - 4.dp),
                      contentScale = ContentScale.Inside
                    )
                  }
                },
                modifier = Modifier
                  .clickable { 
                    // 1x OK = tocar canal onde está o foco
                    android.util.Log.i("LiveScreen", "🎯 Canal clicado: ${s.name}")
                    current = s
                    android.util.Log.i("LiveScreen", "🎯 Canal atual mudou para: ${current?.name}")
                  }
                  .focusable()
                  .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                      // Quando ganha foco, tocar o canal
                      android.util.Log.i("LiveScreen", "🎯 Canal com foco: ${s.name}")
                      current = s
                    }
                  }
              )
              HorizontalDivider()
            } 
          }
        }
        
        // Mini Player (lado direito - espaço azul vazio)
        Box(
          modifier = Modifier
            .width(400.dp)
            .fillMaxHeight()
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          if (current != null) {
            MiniPlayer(
              channel = current!!,
              onFullscreen = { 
                // Parar mini player completamente antes de abrir fullscreen
                android.util.Log.i("MiniPlayer", "🎯 Parando mini player para fullscreen")
                
                // Abrir PlayerActivity em fullscreen (mesmo player)
                val intent = Intent(context, PlayerActivity::class.java).apply {
                  putExtra("url", current!!.toLiveUrl())
                  putExtra("title", current!!.name)
                  putExtra("isLive", true)
                  // Flag para reutilizar activity existente
                  flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
              }
            )
          } else {
            // Espaço vazio quando nenhum canal selecionado
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Selecione um canal para visualizar",
                fontSize = 20.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    } else {
      // 📱 Layout original para smartphone/tablet
      Row(Modifier.weight(1f)) {
        Surface(tonalElevation = 2.dp, modifier = Modifier.width(380.dp).fillMaxHeight()) {
          val filtered = when {
            selectedCat == "ADULT" && isAdultUnlocked -> {
              streams.filter { 
                it.category_id in adultCategoryIds || 
                it.name.contains(Regex("(?i)(adult|xxx|18\\+|porn|sex)"))
              }
            }
            selectedCat == null -> streams
            else -> streams.filter { it.category_id == selectedCat }
          }
          val headlineSize = 16.sp
          val supportingSize = 12.sp
          val iconSize = 40.dp
          
          LazyColumn { 
            items(filtered) { s ->
              ListItem(
                headlineContent = { 
                  Text(
                    text = s.name,
                    fontSize = headlineSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif
                  ) 
                }, 
                supportingContent = { 
                  Text(
                    text = s.categoryName ?: "-",
                    fontSize = supportingSize,
                    fontFamily = FontFamily.SansSerif
                  ) 
                },
                leadingContent = {
                  Box(
                    modifier = Modifier
                      .size(iconSize + 8.dp)
                      .padding(4.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    AsyncImage(
                      model = s.stream_icon,
                      contentDescription = s.name,
                      modifier = Modifier
                        .size(iconSize - 4.dp),
                      contentScale = ContentScale.Inside
                    )
                  }
                },
                modifier = Modifier
                  .clickable { current = s }
                  .focusable()
              )
              HorizontalDivider()
            } 
          }
        }
        Box(Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
          PlayerSurface(currentUrl = current?.toLiveUrl())
        }
      }
    }
  }
  
  // ✅ Modal de PIN para categoria adulta
  if (showPinDialog) {
    Dialog(onDismissRequest = { 
      showPinDialog = false 
      pendingAdultCategory = null
    }) {
      Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 8.dp,
        modifier = Modifier.padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Cadeado",
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFFF6B6B)
          )
          
          Text(
            text = "🔞 Conteúdo Adulto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6B6B)
          )
          
          Text(
            text = "Digite o PIN para acessar canais adultos:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          
          OutlinedTextField(
            value = pinInput,
            onValueChange = { pinInput = it },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = showPinError,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          
          if (showPinError) {
            Text(
              text = "PIN incorreto! Tente novamente.",
              color = Color(0xFFFF5252),
              style = MaterialTheme.typography.bodySmall
            )
          }
          
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { 
                showPinDialog = false
                pendingAdultCategory = null
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Cancelar")
            }
            
            Button(
              onClick = {
                if (pinInput == "0000") {
                  // ✅ PIN correto - desbloquear categoria adulta
                  isAdultUnlocked = true
                  selectedCat = pendingAdultCategory
                  showPinDialog = false
                  pendingAdultCategory = null
                  showPinError = false
                } else {
                  // ❌ PIN incorreto
                  showPinError = true
                  pinInput = ""
                }
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Confirmar")
            }
          }
        }
      }
    }
  }
}

@Composable
fun MiniPlayer(
  channel: LiveStream,
  onFullscreen: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  
  // ExoPlayer para mini player - UM ÚNICO player que muda de canal
  val exoPlayer = remember {
    val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
      .setAllowCrossProtocolRedirects(true)
      .setUserAgent("MaxiPTV/1.1.1 (Android)")
      .setConnectTimeoutMs(8000)
      .setReadTimeoutMs(8000)
      .setKeepPostFor302Redirects(true)
    
    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
      .setDataSourceFactory(dataSourceFactory)
    
    // LoadControl otimizado para mini player
    val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
      .setBufferDurationsMs(
        3000,   // minBufferMs: 3 segundos
        10000,  // maxBufferMs: 10 segundos
        1500,   // bufferForPlaybackMs: 1.5 segundos
        3000    // bufferForPlaybackAfterRebufferMs: 3 segundos
      )
      .setPrioritizeTimeOverSizeThresholds(true)
      .setBackBuffer(3000, true)
      .build()
    
    androidx.media3.exoplayer.ExoPlayer.Builder(context)
      .setMediaSourceFactory(mediaSourceFactory)
      .setLoadControl(loadControl)
      .build().apply {
        volume = 0.3f // Volume baixo no mini player
        repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        android.util.Log.i("MiniPlayer", "▶️ Mini Player criado")
      }
  }
  
  // Atualizar canal quando mudar - MUDAR MÍDIA NO MESMO PLAYER
  LaunchedEffect(channel.stream_id) {
    android.util.Log.i("MiniPlayer", "🔄 Canal alterado no mini player: ${channel.name}")
    exoPlayer.stop() // Parar player atual
    val mediaItem = androidx.media3.common.MediaItem.fromUri(channel.toLiveUrl())
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true
  }
  
  // Parar mini player completamente quando sair da tela
  DisposableEffect(Unit) {
    onDispose {
      android.util.Log.i("MiniPlayer", "⏹️ Mini Player parado - saindo da tela")
      exoPlayer.stop()
    }
  }
  
  Box(
    modifier = Modifier
      .width(400.dp)
      .height(300.dp) // Altura fixa para não crescer
      .padding(8.dp)
      .clickable { 
        // 2x OK = fullscreen
        android.util.Log.i("MiniPlayer", "🎯 2x OK no mini player - abrindo fullscreen")
        onFullscreen()
      }
      .focusable()
      .onFocusChanged { focusState ->
        if (focusState.isFocused) {
          android.util.Log.i("MiniPlayer", "🎯 Mini player com foco - pronto para 2x OK")
        }
      },
    contentAlignment = Alignment.Center
  ) {
    // Player View
    androidx.compose.ui.viewinterop.AndroidView(
      factory = { ctx ->
        androidx.media3.ui.PlayerView(ctx).apply {
          player = exoPlayer
          useController = false // SEM CONTROLES
          resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
          layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
          )
        }
      },
      modifier = Modifier
        .width(384.dp) // 400dp - 16dp padding
        .height(284.dp) // 300dp - 16dp padding
    )
    
    // Overlay com informações do canal e programa atual (apenas no mini player)
    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(12.dp)
        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
      Column {
        // Nome do canal
        Text(
          text = channel.name,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        
        // Categoria do canal
        Text(
          text = channel.categoryName ?: "Canal",
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFFFFD54F), // Amarelo para destacar
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
    
    // Instrução para fullscreen
    Box(
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(16.dp)
        .background(Color(0xFF00D4FF).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Text(
        text = "OK = Fullscreen",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}

package com.maxiptv.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
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
import com.maxiptv.ui.components.fillMaxWidthAdjusted
import com.maxiptv.data.LiveStream
import com.maxiptv.data.EpgProgramme
import com.maxiptv.data.EpgParser
import com.maxiptv.ui.player.PlayerActivity
import coil.compose.AsyncImage
import android.content.Intent
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
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
  
  // 🔥 ESTADO PARA FULLSCREEN - MESMO PLAYER, SÓ MUDA O LAYOUT!
  var isFullscreen by remember { mutableStateOf(false) }
  
  // Context precisa ser lido FORA do remember
  val context = LocalContext.current
  val isTv = MaxiApp.isTv
  val isFireStick = MaxiApp.isFireStick
  val isPhone = MaxiApp.isPhone
  
  // 🔥 PLAYER COMPARTILHADO - UM ÚNICO ExoPlayer com RETRY AUTOMÁTICO E BUFFERS OTIMIZADOS
  val sharedPlayer = remember {
    val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
      .setAllowCrossProtocolRedirects(true)
      .setUserAgent("MaxiPTV/1.1.1 (Android)")
      .setConnectTimeoutMs(8000) // Aumentado para melhor estabilidade (8s)
      .setReadTimeoutMs(10000)    // Aumentado para melhor estabilidade (10s)
      .setKeepPostFor302Redirects(true)
    
    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
      .setDataSourceFactory(dataSourceFactory)
    
    // ⚡ BUFFERS BALANCEADOS PARA LIVE (igual ao PlayerActivity - aumentados para estabilidade)
    val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
      .setBufferDurationsMs(
        5000,   // minBufferMs: 5 segundos (buffer inicial adequado para estabilidade)
        12000,  // maxBufferMs: 12 segundos (buffer máximo para evitar travamentos)
        1500,   // bufferForPlaybackMs: 1.5 segundos (start rápido mas estável)
        3000    // bufferForPlaybackAfterRebufferMs: 3 segundos (buffer após reconexão)
      )
      .setPrioritizeTimeOverSizeThresholds(true)
      .setBackBuffer(5000, true) // 5s de back buffer (mais buffer para estabilidade)
      .build()
    
    androidx.media3.exoplayer.ExoPlayer.Builder(context)
      .setMediaSourceFactory(mediaSourceFactory)
      .setLoadControl(loadControl)
      .build().apply {
        volume = 0.3f // Começa baixo no mini player
        repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
        
        // 📊 QUALIDADE ADAPTATIVA OTIMIZADA
        trackSelectionParameters = androidx.media3.common.TrackSelectionParameters.Builder(context)
          .setPreferredTextLanguage(null)
          .setMaxVideoBitrate(2_200_000) // 2.2Mbps (qualidade balanceada)
          .setMaxVideoSize(1280, 720)   // Limitar a 720p
          .setMinVideoBitrate(500_000)  // Bitrate mínimo
          .build()
        
        // 🔄 RETRY AUTOMÁTICO - Reconectar quando travar
        addListener(object : androidx.media3.common.Player.Listener {
          override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.w("SharedPlayer", "⚠️ Erro no player: ${error.message}")
            android.util.Log.i("SharedPlayer", "🔄 Tentando reconectar em 2 segundos...")
            
            // Aguardar 2 segundos e tentar novamente
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
              android.util.Log.i("SharedPlayer", "🔄 Reconectando...")
              prepare()
              playWhenReady = true
            }, 2000)
          }
          
          override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
              androidx.media3.common.Player.STATE_IDLE -> 
                android.util.Log.i("SharedPlayer", "⏸️ Player IDLE")
              androidx.media3.common.Player.STATE_BUFFERING -> 
                android.util.Log.i("SharedPlayer", "⏳ Buffering...")
              androidx.media3.common.Player.STATE_READY -> 
                android.util.Log.i("SharedPlayer", "✅ Player pronto!")
              androidx.media3.common.Player.STATE_ENDED -> 
                android.util.Log.i("SharedPlayer", "🏁 Fim da stream")
            }
          }
        })
        
        android.util.Log.i("SharedPlayer", "🎯 Player compartilhado criado com retry automático")
      }
  }
  
  // 📡 Carregar EPG em background
  val epgData by XRepo.epgData.collectAsState()
  val scope = rememberCoroutineScope()
  
  // 🔥 INTERCEPTAR BACK BUTTON (só na TV) - DEPOIS do player ser criado
  if (isTv) {
    androidx.activity.compose.BackHandler(enabled = isFullscreen) {
      // 1x BACK em fullscreen = volta para mini player
      android.util.Log.i("LiveScreen", "🔙 BACK pressionado - saindo do fullscreen")
      sharedPlayer.volume = 0.3f
      isFullscreen = false
      // Continua na mesma tela (LiveScreen)
    }
  }
  
  LaunchedEffect(Unit) { 
    XRepo.ensureLiveLoaded()
    // Carregar EPG em background (não bloqueia a UI)
    scope.launch {
      XRepo.loadEpg()
    }
  }
  
  // Cleanup do player compartilhado quando sair da tela
  DisposableEffect(Unit) {
    onDispose {
      android.util.Log.i("LiveScreen", "🧹 Parando player compartilhado - saindo da tela")
      sharedPlayer.stop()
    }
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
  
  // 🔥 SE FULLSCREEN, MOSTRAR SÓ O PLAYER (TELA TODA, SEM TopBar/Categorias)
  if (isFullscreen && current != null) {
    // Fullscreen limpo - só o player com controles nativos
    // BACK do controle remoto sai do fullscreen (BackHandler acima)
    // IMPORTANTE: Nenhuma TopBar é renderizada em fullscreen
    androidx.compose.ui.viewinterop.AndroidView(
      factory = { ctx ->
        androidx.media3.ui.PlayerView(ctx).apply {
          player = sharedPlayer
          useController = true // CONTROLES ATIVADOS EM FULLSCREEN
          controllerShowTimeoutMs = 3000
          controllerHideOnTouch = true
          resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
          setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
      },
      modifier = Modifier.fillMaxSize().background(Color.Black)
    )
    return // IMPORTANTE: Sair da função ANTES de renderizar TopBar ou qualquer outro elemento
  }
  
  // LAYOUT NORMAL (com TopBar, Categorias, Mini Player)
  // IMPORTANTE: TopBar só é renderizada quando NÃO está em fullscreen
  Column(Modifier.fillMaxSize()) {
    // TopBar com Logo e Botão Voltar (APENAS no Fire Stick e APENAS quando NÃO está em fullscreen)
    if (isFireStick && !isFullscreen) {
      val horizontalPadding = 12.dp
      
      Box(
        modifier = Modifier
          .fillMaxWidthAdjusted() // ✅ Fire Stick/Native TV: 90% da largura real
          .padding(
            vertical = 12.dp,
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
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Logo",
            modifier = Modifier.size(40.dp),
            tint = Color(0xFF00D4FF)
          )
          
          Spacer(Modifier.width(12.dp))
          
          Text(
            text = "Max IPTV",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D4FF)
          )
        }
        
        // Botão Voltar à direita
        IconButton(
          onClick = { nav.popBackStack() },
          modifier = Modifier.align(Alignment.CenterEnd)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Voltar",
            tint = Color(0xFF00D4FF),
            modifier = Modifier.size(40.dp)
          )
        }
      }
    }
    
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
    
    if (isTv) {
      // 📺 Layout TV com Mini Player
      Row(Modifier.weight(1f)) {
        // Lista de canais (lado esquerdo - reduzida)
        Surface(tonalElevation = 2.dp, modifier = Modifier.width(320.dp).fillMaxHeight()) {
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
            items(filtered, key = { it.stream_id }) { s ->
              var isFocused by remember { mutableStateOf(false) }
              val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.1f else 1.0f,
                animationSpec = spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy,
                  stiffness = Spring.StiffnessLow
                ),
                label = "liveChannelZoom"
              )
              
              Box(
                modifier = Modifier
                  .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                  }
              ) {
                ListItem(
                  headlineContent = { 
                    Text(
                      text = s.name,
                      fontSize = headlineSize,
                      fontWeight = FontWeight.SemiBold,
                      fontFamily = FontFamily.SansSerif,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis
                    ) 
                  }, 
                  supportingContent = { 
                    Text(
                      text = s.categoryName ?: "-",
                      fontSize = supportingSize,
                      fontFamily = FontFamily.SansSerif,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
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
                      isFocused = focusState.isFocused
                      if (focusState.isFocused) {
                        // Quando ganha foco, tocar o canal
                        android.util.Log.i("LiveScreen", "🎯 Canal com foco: ${s.name}")
                        current = s
                      }
                    }
                )
                // Overlay branco transparente quando focado (clareado e afinado)
                if (isFocused) {
                  Box(
                    modifier = Modifier
                      .matchParentSize()
                      .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(4.dp)
                      )
                  )
                }
              }
              HorizontalDivider()
            } 
          }
        }
        
        // Mini Player (lado direito - espaço azul vazio)
        Box(
          modifier = Modifier
          .weight(1f) // Ocupar todo o espaço restante
          .fillMaxHeight()
          .padding(top = 0.dp, start = 8.dp, end = 8.dp, bottom = 8.dp), // SEM padding no topo - mini player no topo máximo
          contentAlignment = Alignment.TopCenter // Alinhado ao topo centralizado
        ) {
          if (current != null) {
            MiniPlayer(
              player = sharedPlayer,
              channel = current!!,
              epgData = epgData,
              onFullscreen = { 
                // 2x OK = Ativar fullscreen (MESMO PLAYER, SÓ MUDA LAYOUT)
                android.util.Log.i("MiniPlayer", "🎯 Ativando fullscreen - volume 100%")
                sharedPlayer.volume = 1.0f // Volume máximo em fullscreen
                isFullscreen = true // Trocar para layout fullscreen
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
            items(filtered, key = { it.stream_id }) { s ->
              // Smartphone usa touch - sem zoom e overlay (mantém como estava)
              ListItem(
                headlineContent = { 
                  Text(
                    text = s.name,
                    fontSize = headlineSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  ) 
                }, 
                supportingContent = { 
                  Text(
                    text = s.categoryName ?: "-",
                    fontSize = supportingSize,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
  player: androidx.media3.exoplayer.ExoPlayer,
  channel: LiveStream,
  epgData: Map<String, List<EpgProgramme>>,
  onFullscreen: () -> Unit
) {
  // Atualizar canal quando mudar - MUDAR MÍDIA NO MESMO PLAYER
  LaunchedEffect(channel.stream_id) {
    android.util.Log.i("MiniPlayer", "🔄 Canal alterado no mini player: ${channel.name}")
    player.stop() // Parar player atual
    val mediaItem = androidx.media3.common.MediaItem.fromUri(channel.toLiveUrl())
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true
  }
  
  val exoPlayer = player // Renomear localmente para evitar conflito
  
  Box(
    modifier = Modifier
      .fillMaxSize() // Ocupar todo o espaço azul
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
    // Player View - USANDO PLAYER COMPARTILHADO
    androidx.compose.ui.viewinterop.AndroidView(
      factory = { ctx ->
        androidx.media3.ui.PlayerView(ctx).apply {
          this.player = exoPlayer // Usar o player compartilhado renomeado
          useController = false // SEM CONTROLES
          resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
          // Desabilitar overlay nativo do ExoPlayer para mostrar nosso EPG
          setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
          layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
          )
        }
      },
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(16.dp)) // Bordas arredondadas para visual moderno
    )
    
    // 🎨 Overlay elegante com EPG (APENAS no mini player)
    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .background(
          androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(
              Color.Transparent,
              Color.Black.copy(alpha = 0.85f)
            )
          )
        )
        .padding(16.dp, 8.dp, 16.dp, 16.dp) // Padding reduzido no topo
    ) {
      // Buscar programa atual e próximo do EPG
      val currentProgramme = EpgParser.getCurrentProgramme(channel.name, epgData)
      val nextProgramme = EpgParser.getNextProgramme(channel.name, epgData)
      
      // Log para debug do EPG
      android.util.Log.i("MiniPlayer", "📺 Canal: ${channel.name}")
      android.util.Log.i("MiniPlayer", "📡 EPG carregado: ${epgData.size} canais")
      android.util.Log.i("MiniPlayer", "🎬 Programa atual: ${currentProgramme?.title ?: "NÃO ENCONTRADO"}")
      android.util.Log.i("MiniPlayer", "🎬 Próximo programa: ${nextProgramme?.title ?: "NÃO ENCONTRADO"}")
      
      Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        // 📺 Nome do canal com visual profissional
        Text(
          text = channel.name,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          letterSpacing = 0.5.sp,
          style = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
              color = Color.Black.copy(alpha = 0.9f),
              offset = androidx.compose.ui.geometry.Offset(2f, 2f),
              blurRadius = 6f
            )
          )
        )
        
        // 🎬 Programa atual (se disponível no EPG)
        if (currentProgramme != null) {
          Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
          ) {
            // Badge "AO VIVO" estilo moderno
            Box(
              modifier = Modifier
                .background(
                  androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFF1744), Color(0xFFE91E63))
                  ),
                  RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "● AO VIVO",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.8.sp
              )
            }
            
            // Horário do programa
            Text(
              text = "${currentProgramme.startTime()} - ${currentProgramme.stopTime()}",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF90CAF9), // Azul claro
              letterSpacing = 0.3.sp
            )
          }
          
          // Título do programa atual
          Text(
            text = currentProgramme.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFF59D), // Amarelo suave
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
            style = androidx.compose.ui.text.TextStyle(
              shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.7f),
                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                blurRadius = 3f
              )
            )
          )
          
          // 📺 Próxima atração com visual melhorado
          if (nextProgramme != null) {
            Row(
              horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
              verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
              // Label "Em seguida"
              Text(
                text = "EM SEGUIDA",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78909C), // Cinza azulado
                letterSpacing = 0.8.sp
              )
              
              Text(
                text = "•",
                fontSize = 10.sp,
                color = Color(0xFF78909C)
              )
              
              Text(
                text = "${nextProgramme.startTime()}h",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFB0BEC5)
              )
              
              Text(
                text = nextProgramme.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCFD8DC), // Cinza claro
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
        // ❌ OVERLAY ANTIGO REMOVIDO - SÓ EPG AGORA!
      }
    }
  }
}

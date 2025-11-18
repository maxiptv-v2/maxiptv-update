package com.maxiptv.ui.screens
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.maxiptv.data.XRepo
import com.maxiptv.data.SettingsRepo
import com.maxiptv.ui.player.PlayerActivity
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.maxiptv.data.FavoritesManager
import com.maxiptv.MaxiApp
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale

@Composable
fun VodDetailsScreen(nav: NavHostController, vodId: Int) {
  val info by XRepo.vodInfo.collectAsState(null)
  val allVods by XRepo.vodItems.collectAsState(emptyList())
  val ctx = LocalContext.current
  var showOptionsDialog by remember { mutableStateOf(false) }
  var selectedLanguage by remember { mutableStateOf("") }
  var selectedQuality by remember { mutableStateOf("FHD") }
  var isFavorite by remember { mutableStateOf(false) }
  
  val scope = rememberCoroutineScope()
  
  // Verificar se é favorito
  LaunchedEffect(vodId) {
    isFavorite = FavoritesManager.isMovieFavorite(vodId)
  }
  
  // 🎯 DETECTAR RETORNO DO PLAYER para navegação inteligente
  LaunchedEffect(Unit) {
    // Configurar listener para resultado do PlayerActivity
    // (implementação será feita via Activity Result API)
  }
  
  // Detectar idiomas disponíveis buscando TODAS as versões na API
  val availableLanguages = remember(info, allVods) {
    val currentTitle = info?.info?.name ?: ""
    val baseTitle = currentTitle.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
    
    buildList {
      // Buscar todas as versões deste filme
      val versions = allVods.filter { 
        it.name.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim() == baseTitle
      }
      
      var hasOriginal = false
      var hasLegendado = false
      
      versions.forEach { version ->
        when {
          version.name.contains(Regex("\\[(LEG|LEGENDADO)\\]", RegexOption.IGNORE_CASE)) -> {
            hasLegendado = true
          }
          version.name.contains(Regex("\\[(DUB|DUBLADO)\\]", RegexOption.IGNORE_CASE)) -> {
            if (!contains("Dublado")) add("Dublado")
          }
          version.name.contains(Regex("\\[DUAL\\]", RegexOption.IGNORE_CASE)) -> {
            hasLegendado = true
            if (!contains("Dublado")) add("Dublado")
          }
          else -> {
            // Versão sem tag = Original
            hasOriginal = true
          }
        }
      }
      
      // Adicionar na ordem de prioridade: Original > Legendado
      if (hasOriginal && !contains("Original")) add("Original")
      if (hasLegendado && !contains("Legendado")) add("Legendado")
      
      // Se não tem nenhuma opção, adicionar Original como padrão
      if (isEmpty()) add("Original")
    }.also { langs ->
      if (selectedLanguage.isEmpty() && langs.isNotEmpty()) {
        selectedLanguage = langs.first()
      }
    }
  }
  
  LaunchedEffect(vodId) { XRepo.loadVodInfo(vodId) }
  
  // ✅ Banner de fundo estilo Netflix
  Box(modifier = Modifier.fillMaxSize()) {
    // Banner de fundo com blur e transparência estilo Netflix
    AsyncImage(
      model = info?.info?.cover,
      contentDescription = null,
      modifier = Modifier
        .fillMaxSize()
        .blur(radius = 25.dp) // ✅ Blur real aplicado
        .graphicsLayer {
          alpha = 0.3f // ✅ Transparência ajustada (suave, não interfere nos botões)
          // Efeito de escala para criar profundidade
          scaleX = 1.1f
          scaleY = 1.1f
        },
      contentScale = ContentScale.Crop // Não distorce, corta mantendo proporção
    )
    
    // ✅ Overlay escuro ajustado para melhor contraste e legibilidade
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Black.copy(alpha = 0.7f),  // Topo mais escuro
              Color.Black.copy(alpha = 0.55f), // Meio
              Color.Black.copy(alpha = 0.8f)   // Embaixo mais escuro para os botões
            )
          )
        )
    )
    
    // ✅ Overlay adicional com gradiente radial para efeito de blur suave nas bordas
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color.Transparent,
              Color.Black.copy(alpha = 0.3f),
              Color.Black.copy(alpha = 0.5f)
            ),
            radius = 900f
          )
        )
    )
    
    // Conteúdo principal por cima do banner
    Column(Modifier.fillMaxSize().padding(16.dp)) {
    Row(Modifier.fillMaxWidth()) {
      AsyncImage(
        model = info?.info?.cover,
        contentDescription = info?.info?.name,
        modifier = Modifier.width(120.dp).height(180.dp)
      )
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1f)) {
        Text(
          info?.info?.name ?: "Filme", 
          style = MaterialTheme.typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        
        // ✅ Verificar se API retorna avaliação/rating (Xtream Code pode retornar em movie_data)
        val rating = info?.movie_data?.let { data ->
          // Log para debug - ver quais campos estão disponíveis
          android.util.Log.d("VodDetails", "📊 Campos disponíveis em movie_data: ${data.keys.joinToString()}")
          
          // Tentar diferentes campos possíveis de rating da API Xtream Code
          // Campos comuns: rating, imdb_rating, tmdb_rating, rate, score, vote_average
          val foundRating = (data["rating"] as? String)?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["imdb_rating"] as? String)?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["tmdb_rating"] as? String)?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["rate"] as? String)?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["score"] as? Number)?.toString()?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["vote_average"] as? Number)?.toString()?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
            ?: (data["rating"] as? Number)?.toString()?.takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
          
          if (foundRating != null) {
            android.util.Log.i("VodDetails", "⭐ Avaliação encontrada: $foundRating")
          } else {
            android.util.Log.d("VodDetails", "⚠️ Nenhuma avaliação encontrada nos campos disponíveis")
          }
          
          foundRating
        }
        
        // Mostrar rating se disponível (formato: ⭐ 8.5/10 ou ⭐ 8.5)
        if (rating != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Avaliação",
              tint = Color(0xFFFFD700), // Dourado
              modifier = Modifier.size(if (MaxiApp.isTv) 24.dp else 20.dp)
            )
            Text(
              text = rating,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFD700), // Dourado
              fontSize = if (MaxiApp.isTv) 18.sp else 16.sp
            )
            // Se não tiver "/10", adicionar "/10" para padronizar (se for numérico)
            if (!rating.contains("/") && rating.toDoubleOrNull() != null) {
              Text(
                text = "/10",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFD700).copy(alpha = 0.7f),
                fontSize = if (MaxiApp.isTv) 14.sp else 12.sp
              )
            }
          }
          Spacer(Modifier.height(8.dp))
        }
        
        Text(
          info?.info?.plot ?: "Sem descrição", 
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
          color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showOptionsDialog = true }) {
          Text("🎬 $selectedLanguage | $selectedQuality")
    }
    
        Spacer(Modifier.height(16.dp))
    
        // ✅ BOTÕES 3D ESTILO NEON: Assistir, Favoritar e Configurações
        // Colocados DEBAIXO do botão "Original | FHD"
        var isAssistirFocused by remember { mutableStateOf(false) }
        var isFavoritarFocused by remember { mutableStateOf(false) }
        var isConfigFocused by remember { mutableStateOf(false) }
        
        Row(
          modifier = Modifier
            .then(if (MaxiApp.isTv) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth()),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Botão 3D Assistir
          Neon3DButton(
            text = "Assistir",
            onClick = { 
              // Buscar o stream_id correto baseado no idioma escolhido
              val currentTitle = info?.info?.name ?: ""
              val baseTitle = currentTitle.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
              
              val targetVersion = allVods.find { vod ->
                val vodBase = vod.name.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
                val matchesTitle = vodBase == baseTitle
                val matchesLanguage = when (selectedLanguage) {
                  "Legendado" -> vod.name.contains(Regex("\\[(LEG|LEGENDADO)\\]", RegexOption.IGNORE_CASE))
                  "Dublado" -> vod.name.contains(Regex("\\[(DUB|DUBLADO)\\]", RegexOption.IGNORE_CASE))
                  "Original" -> !vod.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
                  else -> !vod.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
                }
                matchesTitle && matchesLanguage
              }
              
              val streamId = targetVersion?.stream_id ?: vodId
              val (base, user, pass) = SettingsRepo.loadBlocking()
              val cleanBase = base.replace("/player_api.php", "").replace("player_api.php", "")
              val baseUrl = if (cleanBase.endsWith("/")) cleanBase else "$cleanBase/"
              val url = "${baseUrl}movie/$user/$pass/$streamId.mp4"
              
              android.util.Log.i("VodDetails", "Idioma escolhido: $selectedLanguage")
              android.util.Log.i("VodDetails", "Stream ID: $streamId (${targetVersion?.name ?: "padrão"})")
              
              val playerIntent = Intent(ctx, PlayerActivity::class.java)
                .putExtra("url", url)
                .putExtra("contentType", "vod")
                .putExtra("returnToCategory", "vod")
                .putExtra("categoryId", vodId.toString())
              
              ctx.startActivity(playerIntent)
            },
            isFocused = isAssistirFocused,
            onFocusChanged = { isAssistirFocused = it },
            modifier = Modifier.weight(1f)
          )
          
          // Botão 3D Favoritar
          Neon3DButton(
            text = "Favoritar",
            onClick = {
              scope.launch {
                if (isFavorite) {
                  FavoritesManager.removeFavoriteMovie(vodId)
                  isFavorite = false
                  android.util.Log.i("VodDetails", "❌ Filme $vodId removido dos favoritos")
                } else {
                  FavoritesManager.addFavoriteMovie(vodId)
                  isFavorite = true
                  android.util.Log.i("VodDetails", "✅ Filme $vodId adicionado aos favoritos")
                }
              }
            },
            isFocused = isFavoritarFocused,
            onFocusChanged = { isFavoritarFocused = it },
            modifier = Modifier.weight(1f),
            isActive = isFavorite // ✅ Mostrar estado ativo quando favoritado
          )
          
          // Botão 3D Configurações
          Neon3DButton(
            text = "Configurações",
            onClick = { showOptionsDialog = true },
            isFocused = isConfigFocused,
            onFocusChanged = { isConfigFocused = it },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
    
    // Dialog de opções
    if (showOptionsDialog) {
      androidx.compose.ui.window.Dialog(onDismissRequest = { showOptionsDialog = false }) {
        Surface(shape = MaterialTheme.shapes.medium) {
          Column(Modifier.padding(24.dp)) {
            Text("Opções de Reprodução", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            
            if (availableLanguages.isNotEmpty()) {
              Text("Idioma:", style = MaterialTheme.typography.labelLarge)
              Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                availableLanguages.forEach { lang ->
                  FilterChip(
                    selected = selectedLanguage == lang, 
                    onClick = { selectedLanguage = lang }, 
                    label = { Text(lang) }
                  )
                }
              }
              Spacer(Modifier.height(12.dp))
            }
            
            Text("Qualidade:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
              // 🎨 FILTERCHIPS COM FOCO MAIS FORTE
              var isFhdFocused by remember { mutableStateOf(false) }
              var isHdFocused by remember { mutableStateOf(false) }
              
              FilterChip(
                selected = selectedQuality == "FHD", 
                onClick = { selectedQuality = "FHD" }, 
                label = { Text("FHD") },
                modifier = Modifier
                  .onFocusChanged { isFhdFocused = it.isFocused }
                  .focusable()
                  .then(
                    if (isFhdFocused) 
                      Modifier
                        .border(3.dp, Color(0xFF4CAF50), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .shadow(
                          elevation = 12.dp,
                          spotColor = Color(0xFF4CAF50).copy(alpha = 0.8f),
                          ambientColor = Color(0xFF4CAF50).copy(alpha = 0.6f),
                          shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                    else 
                      Modifier
                  )
              )
              FilterChip(
                selected = selectedQuality == "HD", 
                onClick = { selectedQuality = "HD" }, 
                label = { Text("HD") },
                modifier = Modifier
                  .onFocusChanged { isHdFocused = it.isFocused }
                  .focusable()
                  .then(
                    if (isHdFocused) 
                      Modifier
                        .border(3.dp, Color(0xFF4CAF50), androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .shadow(
                          elevation = 12.dp,
                          spotColor = Color(0xFF4CAF50).copy(alpha = 0.8f),
                          ambientColor = Color(0xFF4CAF50).copy(alpha = 0.6f),
                          shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                    else 
                      Modifier
                  )
              )
            }
            
            Spacer(Modifier.height(16.dp))
            Button(onClick = { showOptionsDialog = false }, modifier = Modifier.fillMaxWidth()) {
              Text("Confirmar")
            }
          }
        }
      }
    }
    } // Fechar Column do conteúdo principal
  } // Fechar Box do banner de fundo
}

// ✅ Componente de Botão 3D Estilo Neon
@Composable
fun Neon3DButton(
  text: String,
  onClick: () -> Unit,
  isFocused: Boolean,
  onFocusChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  isActive: Boolean = false // ✅ Estado ativo (para Favoritar quando favoritado)
) {
  val scale by animateFloatAsState(
    targetValue = if (isFocused) 1.15f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessLow
    ),
    label = "neonButtonScale"
  )
  
  val buttonSize = if (MaxiApp.isTv) 80.dp else 64.dp
  val textSize = if (MaxiApp.isTv) 14.sp else 12.sp
  
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Box(
      modifier = Modifier
        .size(buttonSize)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
        .clip(RoundedCornerShape(20.dp))
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color(0xFF1A1A1A), // Preto profundo
              Color(0xFF0A0A0A)  // Preto mais escuro
            )
          )
        )
        .then(
          if (isFocused || isActive) {
            Modifier
              .border(
                width = 4.dp,
                brush = Brush.linearGradient(
                  colors = if (isActive && !isFocused) {
                    // Quando favoritado mas sem foco: dourado
                    listOf(
                      Color(0xFFFFD700), // Dourado
                      Color(0xFFFFA500), // Laranja dourado
                      Color(0xFFFFD700), // Dourado
                      Color(0xFFFFC107)  // Âmbar
                    )
                  } else {
                    // Quando focado: neon colorido
                    listOf(
                      Color(0xFFFF1744), // Vermelho neon
                      Color(0xFFE91E63), // Rosa neon
                      Color(0xFF00D4FF), // Azul ciano
                      Color(0xFF2196F3)  // Azul neon
                    )
                  }
                ),
                shape = RoundedCornerShape(20.dp)
              )
              .shadow(
                elevation = 24.dp,
                spotColor = if (isActive && !isFocused) Color(0xFFFFD700).copy(alpha = 0.8f) else Color(0xFF00D4FF).copy(alpha = 0.8f),
                ambientColor = if (isActive && !isFocused) Color(0xFFFFD700).copy(alpha = 0.6f) else Color(0xFFFF1744).copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
              )
          } else {
            Modifier
              .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                  colors = listOf(
                    Color(0xFFFF1744).copy(alpha = 0.5f),
                    Color(0xFF00D4FF).copy(alpha = 0.5f)
                  )
                ),
                shape = RoundedCornerShape(20.dp)
              )
              .shadow(
                elevation = 8.dp,
                spotColor = Color(0xFF00D4FF).copy(alpha = 0.4f),
                ambientColor = Color(0xFFFF1744).copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
              )
          }
        )
        .clickable { onClick() }
        .focusable()
        .onFocusChanged { onFocusChanged(it.isFocused) },
      contentAlignment = Alignment.Center
    ) {
      // Efeito de brilho interno
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors = listOf(
                Color.White.copy(alpha = if (isFocused) 0.15f else 0.05f),
                Color.Transparent
              ),
              radius = 200f
            ),
            shape = RoundedCornerShape(20.dp)
          )
      )
    }
    
    // Texto abaixo do botão
    Text(
      text = text,
      fontSize = textSize,
      fontWeight = FontWeight.Bold,
      color = when {
        isFocused -> Color(0xFF00D4FF) // Azul ciano quando focado
        isActive -> Color(0xFFFFD700)  // Dourado quando ativo (favoritado)
        else -> Color.White             // Branco padrão
      },
      modifier = Modifier
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
    )
  }
}

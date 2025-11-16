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
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
          info?.info?.plot ?: "Sem descrição", 
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showOptionsDialog = true }) {
          Text("🎬 $selectedLanguage | $selectedQuality")
    }
    
        Spacer(Modifier.height(12.dp))
    
        // 🎨 BOTÕES NA MESMA LINHA: Assistir, Favoritar e Opções
        // Colocados DEBAIXO do botão "Original | FHD"
    var isAssistirFocused by remember { mutableStateOf(false) }
        var isFavoriteFocused by remember { mutableStateOf(false) }
        var isOpcoesFocused by remember { mutableStateOf(false) }
        
        Row(
          modifier = Modifier
            .then(if (MaxiApp.isTv) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth()),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Botão Assistir
    Button(
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
        
        // 🎯 Usar startActivityForResult para navegação inteligente
        val playerIntent = Intent(ctx, PlayerActivity::class.java)
          .putExtra("url", url)
          .putExtra("contentType", "vod")
          .putExtra("returnToCategory", "vod")
          .putExtra("categoryId", vodId.toString())
        
        // Para Compose, vamos usar uma abordagem diferente
        // O PlayerActivity vai navegar de volta automaticamente
        ctx.startActivity(playerIntent)
      },
      modifier = Modifier
              .weight(1.5f)  // Assistir ocupa mais espaço
        .onFocusChanged { isAssistirFocused = it.isFocused }
        .focusable()
        .then(
          if (isAssistirFocused) 
            Modifier
              .border(4.dp, Color(0xFFFF5722), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
              .shadow(
                elevation = 16.dp,
                spotColor = Color(0xFFFF5722).copy(alpha = 0.9f),
                ambientColor = Color(0xFFFF5722).copy(alpha = 0.7f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
              )
          else 
            Modifier
        )
    ) { 
      Text("▶ Assistir") 
    }
    
          // Botão Favoritar (menor)
      Button(
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
        modifier = Modifier
              .weight(1f)  // Favoritar ocupa menos espaço
          .onFocusChanged { isFavoriteFocused = it.isFocused }
          .focusable()
          .then(
            if (isFavoriteFocused)
              Modifier
                .border(3.dp, Color(0xFFFFD700), RoundedCornerShape(8.dp))
                .shadow(
                  elevation = 12.dp,
                  spotColor = Color(0xFFFFD700).copy(alpha = 0.9f),
                  ambientColor = Color(0xFFFFD700).copy(alpha = 0.7f),
                  shape = RoundedCornerShape(8.dp)
                )
            else
              Modifier
          ),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isFavorite) Color(0xFFFFD700) else Color(0xFF666666),
          contentColor = if (isFavorite) Color.Black else Color.White
        )
      ) {
        Icon(
          imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.FavoriteBorder,
              contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
              modifier = Modifier.size(20.dp)  // Ícone menor
        )
            Spacer(Modifier.width(4.dp))  // Espaçamento menor
        Text(
              text = if (isFavorite) "⭐" else "⭐",
              fontSize = 14.sp,  // Texto menor
          fontWeight = FontWeight.Bold
        )
      }
      
          // Botão Opções
      OutlinedButton(
        onClick = { showOptionsDialog = true },
            modifier = Modifier
              .weight(1f)  // Opções ocupa menos espaço
              .onFocusChanged { isOpcoesFocused = it.isFocused }
              .focusable()
              .then(
                if (isOpcoesFocused)
                  Modifier
                    .border(3.dp, Color(0xFF2196F3), RoundedCornerShape(8.dp))
                    .shadow(
                      elevation = 12.dp,
                      spotColor = Color(0xFF2196F3).copy(alpha = 0.9f),
                      ambientColor = Color(0xFF2196F3).copy(alpha = 0.7f),
                      shape = RoundedCornerShape(8.dp)
                    )
                else
                  Modifier
              )
      ) {
            Icon(
              Icons.Default.Settings,
              contentDescription = "Opções",
              modifier = Modifier.size(20.dp)  // Ícone menor
            )
            Spacer(Modifier.width(4.dp))  // Espaçamento menor
            Text("⚙️", fontSize = 14.sp, fontWeight = FontWeight.Bold)  // Texto menor
          }
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
  }
}

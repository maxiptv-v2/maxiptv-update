package com.maxiptv.ui.screens
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.maxiptv.data.XRepo
import com.maxiptv.data.SettingsRepo
import com.maxiptv.ui.player.PlayerActivity
import coil.compose.AsyncImage

@Composable
fun VodDetailsScreen(nav: NavHostController, vodId: Int) {
  val info by XRepo.vodInfo.collectAsState(null)
  val allVods by XRepo.vodItems.collectAsState(emptyList())
  val ctx = LocalContext.current
  var showOptionsDialog by remember { mutableStateOf(false) }
  var selectedLanguage by remember { mutableStateOf("") }
  var selectedQuality by remember { mutableStateOf("FHD") }
  
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
        Text(info?.info?.name ?: "Filme", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(info?.info?.plot ?: "Sem descrição", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { showOptionsDialog = true }) {
          Text("🎬 $selectedLanguage | $selectedQuality")
        }
      }
    }
    
    Spacer(Modifier.height(16.dp))
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
        
        ctx.startActivity(Intent(ctx, PlayerActivity::class.java).putExtra("url", url))
      },
      modifier = Modifier.fillMaxWidth()
    ) { 
      Text("▶ Assistir") 
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
              FilterChip(selected = selectedQuality == "FHD", onClick = { selectedQuality = "FHD" }, label = { Text("FHD") })
              FilterChip(selected = selectedQuality == "HD", onClick = { selectedQuality = "HD" }, label = { Text("HD") })
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

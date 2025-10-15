package com.maxiptv.ui.screens
import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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
import com.maxiptv.ui.player.PlayerActivity
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SeriesDetailsScreen(nav: NavHostController, seriesId: Int) {
  val info by XRepo.seriesInfo.collectAsState(null)
  val allSeries by XRepo.seriesItems.collectAsState(emptyList())
  val ctx = LocalContext.current
  var selectedSeason by remember { mutableStateOf<Int?>(null) }
  var showLanguageDialog by remember { mutableStateOf(false) }
  var selectedLanguage by remember { mutableStateOf("") }
  var selectedQuality by remember { mutableStateOf("FHD") }
  var allSeasonsMerged by remember { mutableStateOf<List<com.maxiptv.data.Season>>(emptyList()) }
  
  // Detectar idiomas disponíveis buscando TODAS as versões na API
  val availableLanguages = remember(info, allSeries) {
    val currentTitle = info?.info?.name ?: ""
    val baseTitle = currentTitle.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
    
    buildList {
      // Buscar todas as versões desta série
      val versions = allSeries.filter { 
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
  
  
  // BUSCAR TEMPORADAS DE TODAS AS VARIANTES (DUB + LEG + TODAS AS OUTRAS)
  LaunchedEffect(seriesId, allSeries, selectedLanguage) {
    if (allSeries.isEmpty()) return@LaunchedEffect
    if (selectedLanguage.isEmpty()) return@LaunchedEffect
    
    android.util.Log.i("SeriesDetails", "========================================")
    android.util.Log.i("SeriesDetails", "🔍 Iniciando busca para série ID: $seriesId")
    
    // 1. Carregar info da série atual
    XRepo.loadSeriesInfo(seriesId)
    
    // Aguardar um pouco para o seriesInfo ser atualizado
    kotlinx.coroutines.delay(500)
    
    // 2. Encontrar título base (SEM FILTROS DE IDIOMA)
    val currentTitle = XRepo.seriesInfo.value?.info?.name ?: return@LaunchedEffect
    val baseTitle = currentTitle
      .replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "")
      .replace(Regex("\\s*\\(\\d{4}\\)\\s*$"), "") // Remove ano
      .trim()
    
    android.util.Log.i("SeriesDetails", "📊 Título base: '$baseTitle'")
    
    // 3. Buscar TODAS as variantes (SEM FILTRAR IDIOMA)
    val variants = allSeries.filter { 
      val candidateBase = it.name
        .replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*\\(\\d{4}\\)\\s*$"), "")
        .trim()
      candidateBase == baseTitle
    }
    
    android.util.Log.i("SeriesDetails", "📚 Encontradas ${variants.size} variantes:")
    variants.forEach { v ->
      android.util.Log.i("SeriesDetails", "  - ID:${v.series_id} | ${v.name}")
    }
    
    if (variants.isEmpty()) {
      android.util.Log.w("SeriesDetails", "⚠️ Nenhuma variante encontrada!")
      return@LaunchedEffect
    }
    
    // 4. Filtrar variante baseada no idioma selecionado
    val filteredVariants = variants.filter { variant ->
      when (selectedLanguage) {
        "Legendado" -> variant.name.contains(Regex("\\[(LEG|LEGENDADO)\\]", RegexOption.IGNORE_CASE))
        "Dublado" -> variant.name.contains(Regex("\\[(DUB|DUBLADO)\\]", RegexOption.IGNORE_CASE))
        "Original" -> !variant.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
        else -> !variant.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
      }
    }
    
    android.util.Log.i("SeriesDetails", "🎯 Variante selecionada ($selectedLanguage): ${filteredVariants.firstOrNull()?.name ?: "nenhuma"}")
    
    // 5. Buscar temporadas da variante selecionada
    val allSeasonsMap = mutableMapOf<Int, MutableList<com.maxiptv.data.Episode>>()
    
    for (variant in filteredVariants) {
      try {
        android.util.Log.i("SeriesDetails", "🌐 Buscando ${variant.name}...")
        
        val response = withContext(Dispatchers.IO) {
          XRepo.getSeriesInfoDirect(variant.series_id)
        }
        
        if (response != null && response.seasons != null) {
          android.util.Log.i("SeriesDetails", "  ✅ ${response.seasons.size} temporadas recebidas")
          
          // Adicionar TODOS os episódios de TODAS as temporadas
          response.episodes?.forEach { (seasonKey, eps) ->
            val seasonNum = seasonKey.toIntOrNull()
            if (seasonNum != null) {
              val currentList = allSeasonsMap.getOrPut(seasonNum) { mutableListOf() }
              currentList.addAll(eps)
              android.util.Log.i("SeriesDetails", "    T$seasonNum: +${eps.size} episódios (total: ${currentList.size})")
            }
          }
        } else {
          android.util.Log.w("SeriesDetails", "  ⚠️ Resposta nula ou sem temporadas")
        }
      } catch (e: Exception) {
        android.util.Log.e("SeriesDetails", "  ❌ Erro: ${e.message}")
        e.printStackTrace()
      }
    }
    
    android.util.Log.i("SeriesDetails", "✅ TEMPORADAS MESCLADAS: ${allSeasonsMap.keys.sorted().joinToString(", ")}")
    
    // 5. Converter para lista de Season
    allSeasonsMerged = allSeasonsMap.entries
      .sortedBy { it.key }
      .map { (seasonNum, eps) ->
        com.maxiptv.data.Season(seasonNum, eps.distinctBy { it.id })
      }
    
    android.util.Log.i("SeriesDetails", "🎬 TOTAL FINAL: ${allSeasonsMerged.size} temporadas com episódios")
    android.util.Log.i("SeriesDetails", "========================================")
    
    // Selecionar primeira temporada
    if (selectedSeason == null && allSeasonsMerged.isNotEmpty()) {
      selectedSeason = allSeasonsMerged.first().season_number
    }
  }
  
  Column(
    Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    if (info == null) {
      CircularProgressIndicator()
      Text("Carregando série...", modifier = Modifier.padding(top = 16.dp))
    } else {
      Row(Modifier.fillMaxWidth()) {
        AsyncImage(
          model = info?.info?.cover,
          contentDescription = info?.info?.name,
          modifier = Modifier.width(120.dp).height(180.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
          Text(info?.info?.name ?: "Série", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(8.dp))
          Text(info?.info?.plot ?: "Sem descrição", style = MaterialTheme.typography.bodyMedium)
          Spacer(Modifier.height(8.dp))
          Button(onClick = { showLanguageDialog = true }) {
            Text("🎬 $selectedLanguage | $selectedQuality")
          }
        }
      }
      Spacer(Modifier.height(16.dp))
      
      // Dialog de seleção de idioma/qualidade
      if (showLanguageDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showLanguageDialog = false }) {
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
              Button(onClick = { showLanguageDialog = false }, modifier = Modifier.fillMaxWidth()) {
                Text("Confirmar")
              }
            }
          }
        }
      }
      
      // Usar allSeasonsMerged que tem TODAS as temporadas de TODAS as variantes (DUB+LEG)
      val combinedSeasons = if (allSeasonsMerged.isNotEmpty()) allSeasonsMerged else (info?.getCombinedSeasons() ?: emptyList())
      
      if (combinedSeasons.isEmpty()) {
        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        Text("Carregando todas as temporadas...", modifier = Modifier.padding(top = 8.dp))
      } else {
        // Seletor de temporadas - Escolha qual temporada quer ver
        Text("Escolha a temporada (${combinedSeasons.size} disponíveis):", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
          Modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
          horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
          combinedSeasons.forEach { season ->
            FilterChip(
              selected = selectedSeason == season.season_number,
              onClick = { selectedSeason = season.season_number },
              label = { Text("T${season.season_number}") }
            )
          }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Mostrar episódios APENAS da temporada selecionada
        val currentSeason = combinedSeasons.find { it.season_number == selectedSeason }
        if (currentSeason != null) {
          Text("📺 Episódios - Temporada ${currentSeason.season_number}", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(8.dp))
          
          currentSeason.episodes.forEach { ep ->
            var isFocused by remember { mutableStateOf(false) }
            
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .focusable()
                .then(
                  if (isFocused) 
                    Modifier
                      .border(6.dp, Color(0xFF9C27B0), RoundedCornerShape(8.dp))
                      .shadow(
                        elevation = 20.dp,
                        spotColor = Color(0xFF9C27B0).copy(alpha = 0.8f),
                        ambientColor = Color(0xFF9C27B0).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                      )
                  else 
                    Modifier
                )
            ) {
              ListItem(
                headlineContent = { Text("Episódio ${ep.episode_num ?: "?"}: ${ep.title ?: "Sem título"}") },
                supportingContent = { Text(ep.info?.plot ?: "") },
                trailingContent = { 
                  Button(onClick = { 
                    // Buscar série na versão correta (dublado/legendado)
                    val currentTitle = info?.info?.name ?: ""
                    val baseTitle = currentTitle.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
                    
                    val targetSeries = allSeries.find { series ->
                      val seriesBase = series.name.replace(Regex("\\s*\\[(LEG|DUB|DUAL|LEGENDADO|DUBLADO)\\]", RegexOption.IGNORE_CASE), "").trim()
                      val matchesTitle = seriesBase == baseTitle
                      val matchesLanguage = when (selectedLanguage) {
                        "Legendado" -> series.name.contains(Regex("\\[(LEG|LEGENDADO)\\]", RegexOption.IGNORE_CASE))
                        "Dublado" -> series.name.contains(Regex("\\[(DUB|DUBLADO)\\]", RegexOption.IGNORE_CASE))
                        "Original" -> !series.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
                        else -> !series.name.contains(Regex("\\[(LEG|LEGENDADO|DUB|DUBLADO|DUAL)\\]", RegexOption.IGNORE_CASE))
                      }
                      matchesTitle && matchesLanguage
                    }
                    
                    ep.streamUrl?.let { url ->
                      android.util.Log.i("SeriesDetails", "Idioma: $selectedLanguage, Série: ${targetSeries?.name ?: "padrão"}")
                      ctx.startActivity(Intent(ctx, PlayerActivity::class.java).putExtra("url", url))
                    }
                  }) { 
                    Text("▶") 
                  } 
                }
              )
            }
          }
        } else {
          Text("Selecione uma temporada acima", style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
  }
}

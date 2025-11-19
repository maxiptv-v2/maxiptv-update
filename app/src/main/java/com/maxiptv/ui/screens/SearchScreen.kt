package com.maxiptv.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.maxiptv.MaxiApp
import com.maxiptv.data.SearchManager
import com.maxiptv.data.XRepo
import com.maxiptv.data.LiveStream
import com.maxiptv.data.VodItem
import com.maxiptv.data.SeriesItem
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(nav: NavHostController) {
  val liveChannels by XRepo.liveStreams.collectAsState(emptyList())
  val vodItems by XRepo.vodItems.collectAsState(emptyList())
  val seriesItems by XRepo.seriesItems.collectAsState(emptyList())
  
  var searchQuery by remember { mutableStateOf("") }
  var searchHistory by remember { mutableStateOf<List<String>>(emptyList()) }
  var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
  var showSuggestions by remember { mutableStateOf(false) }
  
  val scope = rememberCoroutineScope()
  val isTv = MaxiApp.isTv
  val isPhone = MaxiApp.isPhone
  
  // Carregar histórico de busca
  LaunchedEffect(Unit) {
    searchHistory = SearchManager.getSearchHistory()
  }
  
  // Atualizar sugestões quando query muda
  LaunchedEffect(searchQuery) {
    if (searchQuery.length >= 2) {
      searchSuggestions = SearchManager.getSearchSuggestions(
        liveChannels, vodItems, seriesItems, searchQuery
      )
      showSuggestions = searchSuggestions.isNotEmpty()
    } else {
      showSuggestions = false
    }
  }
  
  // Realizar busca
  val searchResults = remember(searchQuery, liveChannels, vodItems, seriesItems) {
    if (searchQuery.isBlank()) {
      SearchManager.SearchResult(emptyList(), emptyList(), emptyList())
    } else {
      SearchManager.searchAll(liveChannels, vodItems, seriesItems, searchQuery)
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
        text = "🔍 Buscar Conteúdo",
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
    
    // Campo de busca
    var isSearchFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { 
        searchQuery = it
        showSuggestions = it.length >= 2
      },
      modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { isSearchFocused = it.isFocused }
        .focusable()
        .then(
          if (isSearchFocused)
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
      placeholder = {
        Text(
          "Digite o nome do canal, filme ou série...",
          color = Color(0xFF888888)
        )
      },
      leadingIcon = {
        Icon(
          Icons.Default.Search,
          contentDescription = "Buscar",
          tint = Color(0xFF00D4FF)
        )
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { 
              searchQuery = ""
              showSuggestions = false
            }
          ) {
            Icon(
              Icons.Default.Clear,
              contentDescription = "Limpar",
              tint = Color(0xFF888888)
            )
          }
        }
      },
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF00D4FF),
        unfocusedBorderColor = Color(0xFF666666),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
      ),
      singleLine = true
    )
    
    // Sugestões de busca
    if (showSuggestions && searchSuggestions.isNotEmpty()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
          containerColor = Color(0xFF1A1A1A)
        )
      ) {
        Column(
          modifier = Modifier.padding(12.dp)
        ) {
          Text(
            text = "💡 Sugestões:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D4FF)
          )
          Spacer(Modifier.height(8.dp))
          searchSuggestions.forEach { suggestion ->
            Text(
              text = "• $suggestion",
              fontSize = 14.sp,
              color = Color.White,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  searchQuery = suggestion
                  showSuggestions = false
                  scope.launch {
                    SearchManager.addToSearchHistory(suggestion)
                  }
                }
                .padding(vertical = 4.dp)
            )
          }
        }
      }
    }
    
    // Histórico de busca (quando não há query)
    if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        colors = CardDefaults.cardColors(
          containerColor = Color(0xFF1A1A1A)
        )
      ) {
        Column(
          modifier = Modifier.padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "🕒 Buscas Recentes:",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00D4FF)
            )
            TextButton(
              onClick = {
                scope.launch {
                  SearchManager.clearSearchHistory()
                  searchHistory = emptyList()
                }
              }
            ) {
              Text("Limpar", fontSize = 12.sp, color = Color(0xFFFF5252))
            }
          }
          Spacer(Modifier.height(8.dp))
          searchHistory.forEach { historyItem ->
            Text(
              text = "• $historyItem",
              fontSize = 14.sp,
              color = Color.White,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  searchQuery = historyItem
                  scope.launch {
                    SearchManager.addToSearchHistory(historyItem)
                  }
                }
                .padding(vertical = 4.dp)
            )
          }
        }
      }
    }
    
    Spacer(Modifier.height(if (isTv) 24.dp else if (isPhone) 16.dp else 20.dp))
    
    // Resultados da busca
    if (searchQuery.isNotEmpty()) {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(if (isTv) 24.dp else if (isPhone) 16.dp else 20.dp)
      ) {
        // Canais encontrados
        if (searchResults.channels.isNotEmpty()) {
          item {
            SearchSection(
              title = "📡 Canais (${searchResults.channels.size})",
              items = searchResults.channels,
              onItemClick = { channel ->
                nav.navigate("live")
              },
              deviceType = when {
                isTv -> "tv"
                isPhone -> "phone"
                else -> "tablet"
              }
            )
          }
        }
        
        // Filmes encontrados
        if (searchResults.movies.isNotEmpty()) {
          item {
            SearchSection(
              title = "🎬 Filmes (${searchResults.movies.size})",
              items = searchResults.movies,
              onItemClick = { movie ->
                nav.navigate("vod/${movie.stream_id}")
              },
              deviceType = when {
                isTv -> "tv"
                isPhone -> "phone"
                else -> "tablet"
              }
            )
          }
        }
        
        // Séries encontradas
        if (searchResults.series.isNotEmpty()) {
          item {
            SearchSection(
              title = "📺 Séries (${searchResults.series.size})",
              items = searchResults.series,
              onItemClick = { series ->
                nav.navigate("series/${series.series_id}")
              },
              deviceType = when {
                isTv -> "tv"
                isPhone -> "phone"
                else -> "tablet"
              }
            )
          }
        }
        
        // Mensagem se não encontrou nada
        if (searchResults.channels.isEmpty() && 
            searchResults.movies.isEmpty() && 
            searchResults.series.isEmpty()) {
          item {
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = null,
                  modifier = Modifier.size(if (isTv) 80.dp else if (isPhone) 60.dp else 70.dp),
                  tint = Color(0xFF666666)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                  text = "Nenhum resultado encontrado",
                  fontSize = when {
                    isTv -> 20.sp
                    isPhone -> 16.sp
                    else -> 18.sp
                  },
                  color = Color(0xFF666666)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                  text = "Tente usar palavras diferentes",
                  fontSize = when {
                    isTv -> 16.sp
                    isPhone -> 14.sp
                    else -> 15.sp
                  },
                  color = Color(0xFF888888)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun <T> SearchSection(
  title: String,
  items: List<T>,
  onItemClick: (T) -> Unit,
  deviceType: String
) {
  Column {
    Text(
      text = title,
      fontSize = when {
        deviceType == "tv" -> 22.sp
        deviceType == "phone" -> 16.sp
        else -> 18.sp
      },
      fontWeight = FontWeight.Bold,
      color = Color.White,
      modifier = Modifier.padding(bottom = if (deviceType == "tv") 16.dp else if (deviceType == "phone") 12.dp else 14.dp)
    )
    
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(if (deviceType == "tv") 16.dp else if (deviceType == "phone") 12.dp else 14.dp)
    ) {
      items(items, key = { when (it) {
        is LiveStream -> it.stream_id
        is com.maxiptv.data.VodItem -> it.stream_id
        is com.maxiptv.data.SeriesItem -> it.series_id
        else -> it.hashCode()
      }}) { item ->
        when (item) {
          is LiveStream -> SearchChannelCard(
            channel = item,
            onClick = { onItemClick(item) },
            deviceType = deviceType
          )
          is VodItem -> SearchMovieCard(
            movie = item,
            onClick = { onItemClick(item) },
            deviceType = deviceType
          )
          is SeriesItem -> SearchSeriesCard(
            series = item,
            onClick = { onItemClick(item) },
            deviceType = deviceType
          )
        }
      }
    }
  }
}

@Composable
fun SearchChannelCard(
  channel: LiveStream,
  onClick: () -> Unit,
  deviceType: String
) {
  var isFocused by remember { mutableStateOf(false) }
  
  Card(
    modifier = Modifier
      .width(if (deviceType == "tv") 200.dp else if (deviceType == "phone") 140.dp else 170.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .focusable()
      .clickable { onClick() }
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
    Column {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(channel.stream_icon)
          .size(if (deviceType == "tv") 240 else if (deviceType == "phone") 160 else 200, 
                if (deviceType == "tv") 180 else if (deviceType == "phone") 120 else 150) // ⚡ Qualidade reduzida
          .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
          .diskCachePolicy(coil.request.CachePolicy.ENABLED)
          .build(),
        contentDescription = null,
        modifier = Modifier
          .fillMaxWidth()
          .height(if (deviceType == "tv") 120.dp else if (deviceType == "phone") 80.dp else 100.dp)
          .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        contentScale = ContentScale.Crop
      )
      
      Column(
        modifier = Modifier.padding(if (deviceType == "tv") 12.dp else if (deviceType == "phone") 8.dp else 10.dp)
      ) {
        Text(
          text = channel.name,
          fontSize = when {
            deviceType == "tv" -> 14.sp
            deviceType == "phone" -> 12.sp
            else -> 13.sp
          },
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
          text = "📡",
          fontSize = when {
            deviceType == "tv" -> 16.sp
            deviceType == "phone" -> 12.sp
            else -> 14.sp
          }
        )
      }
    }
  }
}

@Composable
fun SearchMovieCard(
  movie: VodItem,
  onClick: () -> Unit,
  deviceType: String
) {
  var isFocused by remember { mutableStateOf(false) }
  
  Card(
    modifier = Modifier
      .width(if (deviceType == "tv") 200.dp else if (deviceType == "phone") 140.dp else 170.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .focusable()
      .clickable { onClick() }
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
    Column {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(movie.stream_icon)
          .size(150, 225) // ⚡ Qualidade reduzida
          .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
          .diskCachePolicy(coil.request.CachePolicy.ENABLED)
          .build(),
        contentDescription = null,
        modifier = Modifier
          .fillMaxWidth()
          .height(if (deviceType == "tv") 120.dp else if (deviceType == "phone") 80.dp else 100.dp)
          .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        contentScale = ContentScale.Crop
      )
      
      Column(
        modifier = Modifier.padding(if (deviceType == "tv") 12.dp else if (deviceType == "phone") 8.dp else 10.dp)
      ) {
        Text(
          text = movie.name,
          fontSize = when {
            deviceType == "tv" -> 14.sp
            deviceType == "phone" -> 12.sp
            else -> 13.sp
          },
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
          text = "🎬",
          fontSize = when {
            deviceType == "tv" -> 16.sp
            deviceType == "phone" -> 12.sp
            else -> 14.sp
          }
        )
      }
    }
  }
}

@Composable
fun SearchSeriesCard(
  series: SeriesItem,
  onClick: () -> Unit,
  deviceType: String
) {
  var isFocused by remember { mutableStateOf(false) }
  
  Card(
    modifier = Modifier
      .width(if (deviceType == "tv") 200.dp else if (deviceType == "phone") 140.dp else 170.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .focusable()
      .clickable { onClick() }
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
    Column {
      AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
          .data(series.cover)
          .size(150, 225) // ⚡ Qualidade reduzida
          .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
          .diskCachePolicy(coil.request.CachePolicy.ENABLED)
          .build(),
        contentDescription = null,
        modifier = Modifier
          .fillMaxWidth()
          .height(if (deviceType == "tv") 120.dp else if (deviceType == "phone") 80.dp else 100.dp)
          .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
        contentScale = ContentScale.Crop
      )
      
      Column(
        modifier = Modifier.padding(if (deviceType == "tv") 12.dp else if (deviceType == "phone") 8.dp else 10.dp)
      ) {
        Text(
          text = series.name,
          fontSize = when {
            deviceType == "tv" -> 14.sp
            deviceType == "phone" -> 12.sp
            else -> 13.sp
          },
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
          text = "📺",
          fontSize = when {
            deviceType == "tv" -> 16.sp
            deviceType == "phone" -> 12.sp
            else -> 14.sp
          }
        )
      }
    }
  }
}

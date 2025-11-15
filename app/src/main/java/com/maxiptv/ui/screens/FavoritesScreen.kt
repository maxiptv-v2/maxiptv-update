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
import com.maxiptv.MaxiApp
import com.maxiptv.data.FavoritesManager
import com.maxiptv.data.XRepo
import com.maxiptv.data.LiveStream
import com.maxiptv.data.VodItem
import com.maxiptv.data.SeriesItem
import kotlinx.coroutines.launch

@Composable
fun FavoritesScreen(nav: NavHostController) {
  val liveChannels by XRepo.liveStreams.collectAsState(emptyList())
  val vodItems by XRepo.vodItems.collectAsState(emptyList())
  val seriesItems by XRepo.seriesItems.collectAsState(emptyList())
  
  var favoriteChannels by remember { mutableStateOf<Set<Int>>(emptySet()) }
  var favoriteMovies by remember { mutableStateOf<Set<Int>>(emptySet()) }
  var favoriteSeries by remember { mutableStateOf<Set<Int>>(emptySet()) }
  var isLoading by remember { mutableStateOf(true) }
  
  val scope = rememberCoroutineScope()
  val isTv = MaxiApp.isTv
  val isPhone = MaxiApp.isPhone
  
  // Carregar favoritos
  LaunchedEffect(Unit) {
    try {
      favoriteChannels = FavoritesManager.getFavoriteChannels()
      favoriteMovies = FavoritesManager.getFavoriteMovies()
      favoriteSeries = FavoritesManager.getFavoriteSeries()
      isLoading = false
      android.util.Log.i("FavoritesScreen", "✅ Favoritos carregados: ${favoriteChannels.size} canais, ${favoriteMovies.size} filmes, ${favoriteSeries.size} séries")
    } catch (e: Exception) {
      android.util.Log.e("FavoritesScreen", "❌ Erro ao carregar favoritos: ${e.message}")
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
        text = "⭐ Meus Favoritos",
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
        verticalArrangement = Arrangement.spacedBy(if (isTv) 24.dp else if (isPhone) 16.dp else 20.dp)
      ) {
        // Canais Favoritos
        if (favoriteChannels.isNotEmpty()) {
          item {
            FavoritesSection(
              title = "📡 Canais Favoritos",
              items = favoriteChannels.mapNotNull { id ->
                liveChannels.find { it.stream_id == id }
              },
              onItemClick = { channel ->
                nav.navigate("live")
              },
              onRemoveFavorite = { channelId ->
                scope.launch {
                  FavoritesManager.removeFavoriteChannel(channelId)
                  favoriteChannels = FavoritesManager.getFavoriteChannels()
                }
              },
              deviceType = when {
                isTv -> "tv"
                isPhone -> "phone"
                else -> "tablet"
              }
            )
          }
        }
        
        // Filmes Favoritos
        if (favoriteMovies.isNotEmpty()) {
          item {
            FavoritesSection(
              title = "🎬 Filmes Favoritos",
              items = favoriteMovies.mapNotNull { id ->
                vodItems.find { it.stream_id == id }
              },
              onItemClick = { movie ->
                nav.navigate("vod/${movie.stream_id}")
              },
              onRemoveFavorite = { movieId ->
                scope.launch {
                  FavoritesManager.removeFavoriteMovie(movieId)
                  favoriteMovies = FavoritesManager.getFavoriteMovies()
                }
              },
              deviceType = when {
                isTv -> "tv"
                isPhone -> "phone"
                else -> "tablet"
              }
            )
          }
        }
        
        // Séries Favoritas
        if (favoriteSeries.isNotEmpty()) {
          item {
            FavoritesSection(
              title = "📺 Séries Favoritas",
              items = favoriteSeries.mapNotNull { id ->
                seriesItems.find { it.series_id == id }
              },
              onItemClick = { series ->
                nav.navigate("series/${series.series_id}")
              },
              onRemoveFavorite = { seriesId ->
                scope.launch {
                  FavoritesManager.removeFavoriteSeries(seriesId)
                  favoriteSeries = FavoritesManager.getFavoriteSeries()
                }
              },
              deviceType = when {
                isTv -> "tv"
                isPhone -> "phone"
                else -> "tablet"
              }
            )
          }
        }
        
        // Mensagem se não há favoritos
        if (favoriteChannels.isEmpty() && favoriteMovies.isEmpty() && favoriteSeries.isEmpty()) {
          item {
            Box(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.FavoriteBorder,
                  contentDescription = null,
                  modifier = Modifier.size(if (isTv) 80.dp else if (isPhone) 60.dp else 70.dp),
                  tint = Color(0xFF666666)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                  text = "Nenhum favorito ainda",
                  fontSize = when {
                    isTv -> 20.sp
                    isPhone -> 16.sp
                    else -> 18.sp
                  },
                  color = Color(0xFF666666)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                  text = "Adicione canais, filmes e séries aos favoritos",
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
fun <T> FavoritesSection(
  title: String,
  items: List<T>,
  onItemClick: (T) -> Unit,
  onRemoveFavorite: (Int) -> Unit,
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
          is LiveStream -> FavoriteChannelCard(
            channel = item,
            onRemove = { onRemoveFavorite(item.stream_id) },
            onChannelClick = { onItemClick(item) },
            deviceType = deviceType
          )
          is VodItem -> FavoriteMovieCard(
            movie = item,
            onRemove = { onRemoveFavorite(item.stream_id) },
            onMovieClick = { onItemClick(item) },
            deviceType = deviceType
          )
          is SeriesItem -> FavoriteSeriesCard(
            series = item,
            onRemove = { onRemoveFavorite(item.series_id) },
            onSeriesClick = { onItemClick(item) },
            deviceType = deviceType
          )
        }
      }
    }
  }
}

@Composable
fun FavoriteChannelCard(
  channel: LiveStream,
  onRemove: () -> Unit,
  onChannelClick: () -> Unit,
  deviceType: String
) {
  var isFocused by remember { mutableStateOf(false) }
  
  Card(
    modifier = Modifier
      .width(if (deviceType == "tv") 200.dp else if (deviceType == "phone") 140.dp else 170.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .focusable()
      .clickable { onChannelClick() }
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
        model = channel.stream_icon,
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
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "📡",
            fontSize = when {
              deviceType == "tv" -> 16.sp
              deviceType == "phone" -> 12.sp
              else -> 14.sp
            }
          )
          
          IconButton(
            onClick = onRemove,
            modifier = Modifier.size(if (deviceType == "tv") 32.dp else if (deviceType == "phone") 24.dp else 28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remover",
              tint = Color(0xFFFF5252),
              modifier = Modifier.size(if (deviceType == "tv") 20.dp else if (deviceType == "phone") 16.dp else 18.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun FavoriteMovieCard(
  movie: VodItem,
  onRemove: () -> Unit,
  onMovieClick: () -> Unit,
  deviceType: String
) {
  var isFocused by remember { mutableStateOf(false) }
  
  Card(
    modifier = Modifier
      .width(if (deviceType == "tv") 200.dp else if (deviceType == "phone") 140.dp else 170.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .focusable()
      .clickable { onMovieClick() }
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
        model = movie.stream_icon,
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
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "🎬",
            fontSize = when {
              deviceType == "tv" -> 16.sp
              deviceType == "phone" -> 12.sp
              else -> 14.sp
            }
          )
          
          IconButton(
            onClick = onRemove,
            modifier = Modifier.size(if (deviceType == "tv") 32.dp else if (deviceType == "phone") 24.dp else 28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remover",
              tint = Color(0xFFFF5252),
              modifier = Modifier.size(if (deviceType == "tv") 20.dp else if (deviceType == "phone") 16.dp else 18.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun FavoriteSeriesCard(
  series: SeriesItem,
  onRemove: () -> Unit,
  onSeriesClick: () -> Unit,
  deviceType: String
) {
  var isFocused by remember { mutableStateOf(false) }
  
  Card(
    modifier = Modifier
      .width(if (deviceType == "tv") 200.dp else if (deviceType == "phone") 140.dp else 170.dp)
      .onFocusChanged { isFocused = it.isFocused }
      .focusable()
      .clickable { onSeriesClick() }
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
        model = series.cover,
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
        
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "📺",
            fontSize = when {
              deviceType == "tv" -> 16.sp
              deviceType == "phone" -> 12.sp
              else -> 14.sp
            }
          )
          
          IconButton(
            onClick = onRemove,
            modifier = Modifier.size(if (deviceType == "tv") 32.dp else if (deviceType == "phone") 24.dp else 28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Remover",
              tint = Color(0xFFFF5252),
              modifier = Modifier.size(if (deviceType == "tv") 20.dp else if (deviceType == "phone") 16.dp else 18.dp)
            )
          }
        }
      }
    }
  }
}

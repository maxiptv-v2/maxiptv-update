package com.maxiptv.ui.screens
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun HomeNav(nav: NavHostController) {
  NavHost(navController = nav, startDestination = "login") {
    composable("login") { 
      LoginScreen(onLoginSuccess = { 
        nav.navigate("home") {
          popUpTo("login") { inclusive = true }
        }
      }) 
    }
    composable("home") { HomeScreen(nav) }
    composable("live") { LiveScreen(nav) }
    composable("vod") { VodScreen(nav) }
    composable("adult") { AdultContentScreen(nav) }
    composable("series") { SeriesScreen(nav) }
    composable("series/{seriesId}") { backStack ->
      val id = backStack.arguments?.getString("seriesId")?.toIntOrNull() ?: 0
      SeriesDetailsScreen(nav, id)
    }
    composable("vod/{vodId}") { backStack ->
      val id = backStack.arguments?.getString("vodId")?.toIntOrNull() ?: 0
      VodDetailsScreen(nav, id)
    }
  }
}

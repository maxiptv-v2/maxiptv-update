package com.maxiptv
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.maxiptv.ui.screens.*
import com.maxiptv.ui.theme.MaxiTheme
import com.maxiptv.ui.tv.DensityNormalizer

class MainActivity : ComponentActivity() {
  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MaxiTheme {
        DensityNormalizer {
          val nav = rememberNavController()
          
          Surface(modifier = Modifier.fillMaxSize()) {
            HomeNav(nav)
          }
        }
      }
    }
  }
}

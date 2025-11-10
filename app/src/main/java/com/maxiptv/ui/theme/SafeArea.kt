package com.maxiptv.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxiptv.MaxiApp

@Composable
fun MaxiSafeArea(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val safePadding = remember(
    MaxiApp.isFireStick,
    MaxiApp.isNativeTv,
    MaxiApp.isTvBox,
    MaxiApp.isPhone,
    MaxiApp.isTablet
  ) {
    when {
      MaxiApp.isFireStick -> PaddingValues(
        horizontal = MaxiApp.fireStickOverscanPadding.dp,
        vertical = MaxiApp.fireStickSafeAreaPadding.dp
      )
      MaxiApp.isNativeTv -> PaddingValues(horizontal = 28.dp, vertical = 22.dp)
      MaxiApp.isTvBox -> PaddingValues(horizontal = 24.dp, vertical = 20.dp)
      else -> PaddingValues() // Smartphones/tablets mantêm layout original
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(safePadding)
  ) {
    content()
  }
}


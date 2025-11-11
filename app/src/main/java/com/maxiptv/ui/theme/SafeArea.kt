package com.maxiptv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maxiptv.MaxiApp
import kotlin.math.pow
import kotlin.math.sqrt

private data class SafePaddingResult(
  val padding: PaddingValues,
  val profile: String
)

@Composable
fun MaxiSafeArea(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val context = LocalContext.current

  val diagonalInches = remember(context) {
    val metrics = context.resources.displayMetrics
    val xDpi = if (metrics.xdpi > 0f) metrics.xdpi else metrics.densityDpi.toFloat()
    val yDpi = if (metrics.ydpi > 0f) metrics.ydpi else metrics.densityDpi.toFloat()
    val widthInches = metrics.widthPixels / xDpi
    val heightInches = metrics.heightPixels / yDpi
    val width = widthInches.toDouble()
    val height = heightInches.toDouble()
    sqrt(width.pow(2.0) + height.pow(2.0))
  }

  val paddingResult = remember(
    MaxiApp.isFireStick,
    MaxiApp.isNativeTv,
    MaxiApp.isTvBox,
    MaxiApp.isProjector,
    diagonalInches
  ) {
    when {
      MaxiApp.isFireStick -> {
        val horizontal = MaxiApp.fireStickOverscanPadding.coerceAtLeast(20)
        val top = (MaxiApp.fireStickSafeAreaPadding / 2).coerceAtLeast(10)
        val bottom = (MaxiApp.fireStickSafeAreaPadding + 12).coerceAtLeast(28)
        SafePaddingResult(
          padding = PaddingValues(
            start = horizontal.dp,
            top = top.dp,
            end = horizontal.dp,
            bottom = bottom.dp
          ),
          profile = "fire_stick"
        )
      }

      MaxiApp.isProjector -> {
        val horizontal = when {
          diagonalInches >= 80 -> 72.dp
          diagonalInches >= 65 -> 64.dp
          diagonalInches >= 55 -> 54.dp
          else -> 48.dp
        }
        val vertical = when {
          diagonalInches >= 80 -> 52.dp
          diagonalInches >= 65 -> 44.dp
          diagonalInches >= 55 -> 36.dp
          else -> 30.dp
        }
        SafePaddingResult(
          padding = PaddingValues(horizontal = horizontal, vertical = vertical),
          profile = "projector"
        )
      }

      MaxiApp.isNativeTv -> {
        val horizontal = when {
          diagonalInches >= 75 -> 64.dp
          diagonalInches >= 65 -> 56.dp
          diagonalInches >= 55 -> 48.dp
          diagonalInches >= 45 -> 40.dp
          else -> 32.dp
        }
        val vertical = when {
          diagonalInches >= 75 -> 46.dp
          diagonalInches >= 65 -> 40.dp
          diagonalInches >= 55 -> 34.dp
          diagonalInches >= 45 -> 28.dp
          else -> 24.dp
        }
        SafePaddingResult(
          padding = PaddingValues(horizontal = horizontal, vertical = vertical),
          profile = "native_tv"
        )
      }

      MaxiApp.isTvBox -> {
        val horizontal = when {
          diagonalInches >= 65 -> 44.dp
          diagonalInches >= 55 -> 36.dp
          diagonalInches >= 45 -> 30.dp
          else -> 24.dp
        }
        val vertical = when {
          diagonalInches >= 65 -> 32.dp
          diagonalInches >= 55 -> 26.dp
          diagonalInches >= 45 -> 22.dp
          else -> 18.dp
        }
        SafePaddingResult(
          padding = PaddingValues(horizontal = horizontal, vertical = vertical),
          profile = "tv_box"
        )
      }

      else -> SafePaddingResult(
        padding = PaddingValues(),
        profile = "generic"
      )
    }
  }

  val safePadding = paddingResult.padding
  val paddingProfile = paddingResult.profile

  val scaleFactor = remember(
    MaxiApp.isNativeTv,
    MaxiApp.isTvBox,
    MaxiApp.isProjector,
    diagonalInches
  ) {
    when {
      MaxiApp.isNativeTv -> when {
        diagonalInches >= 75 -> 0.88f
        diagonalInches >= 65 -> 0.90f
        diagonalInches >= 55 -> 0.92f
        diagonalInches >= 45 -> 0.95f
        else -> 0.97f
      }

      MaxiApp.isProjector -> when {
        diagonalInches >= 80 -> 0.86f
        diagonalInches >= 65 -> 0.88f
        diagonalInches >= 55 -> 0.90f
        else -> 0.92f
      }

      MaxiApp.isTvBox -> when {
        diagonalInches >= 65 -> 0.92f
        diagonalInches >= 55 -> 0.94f
        diagonalInches >= 45 -> 0.96f
        else -> 0.98f
      }

      else -> 1f
    }
  }

  LaunchedEffect(safePadding, scaleFactor, diagonalInches, paddingProfile) {
    SafeAreaMetrics.save(
      context = context,
      padding = safePadding,
      scaleFactor = scaleFactor,
      diagonalInches = diagonalInches,
      profile = paddingProfile
    )
  }

  val baseModifier = Modifier
    .fillMaxSize()
    .padding(safePadding)

  val scaledModifier = if (scaleFactor < 0.999f) {
    baseModifier.graphicsLayer {
      scaleX = scaleFactor
      scaleY = scaleFactor
      transformOrigin = TransformOrigin(0f, 0f)
    }
  } else {
    baseModifier
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Box(modifier = scaledModifier, content = content)
  }
}


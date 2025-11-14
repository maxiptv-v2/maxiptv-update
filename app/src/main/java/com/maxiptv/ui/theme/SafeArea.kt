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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.maxiptv.MaxiApp
import com.maxiptv.data.DeviceFingerprint
import kotlin.math.pow
import kotlin.math.sqrt

private data class SafePaddingResult(
  val padding: PaddingValues,
  val profile: String,
  val scaleFactor: Float,
  val overscanAdjusted: Boolean
)

@Composable
fun MaxiSafeArea(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  val context = LocalContext.current
  val layoutDirection = LocalLayoutDirection.current
  val fingerprintInfo = remember { DeviceFingerprint.collect(context) }
  val overrideState by SafeAreaOverrides.overrideFlow(context, fingerprintInfo.key).collectAsState()
  
  // Verifica se tem detecção automática salva localmente
  // Prioridade: override salvo localmente > detecção automática > valores padrão
  val autoDetectedOverride = remember(overrideState, context, fingerprintInfo.key) {
    if (overrideState == null) {
      SafeAreaAutoDetector.loadDetectedSettings(context)
    } else {
      null
    }
  }

  val diagonalInches = remember(context) {
    val metrics = context.resources.displayMetrics
    // Lê dimensões reais da tela quando o app abre
    // Não confia apenas no DPI reportado - usa estimativa inteligente baseada em resolução
    SafeAreaAutoDetector.calculateDiagonalInchesImproved(
      metrics.widthPixels,
      metrics.heightPixels,
      metrics.xdpi,
      metrics.ydpi
    )
  }

  val paddingResult = remember(
    overrideState,
    autoDetectedOverride,
    MaxiApp.isFireStick,
    MaxiApp.isNativeTv,
    MaxiApp.isTvBox,
    MaxiApp.isProjector,
    diagonalInches
  ) {
    // Prioridade: override salvo localmente > detecção automática > valores padrão
    overrideState?.let { override ->
      SafePaddingResult(
        padding = PaddingValues(
          start = override.startDp.dp,
          top = override.topDp.dp,
          end = override.endDp.dp,
          bottom = override.bottomDp.dp
        ),
        profile = override.profile,
        scaleFactor = override.scaleFactor,
        overscanAdjusted = true
      )
    } ?: autoDetectedOverride?.let { autoOverride ->
      SafePaddingResult(
        padding = PaddingValues(
          start = autoOverride.startDp.dp,
          top = autoOverride.topDp.dp,
          end = autoOverride.endDp.dp,
          bottom = autoOverride.bottomDp.dp
        ),
        profile = autoOverride.profile,
        scaleFactor = autoOverride.scaleFactor,
        overscanAdjusted = true
      )
    } ?: run {
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
            profile = "fire_stick_auto",
            scaleFactor = 0.96f,
            overscanAdjusted = true
          )
        }

        MaxiApp.isProjector -> {
          val horizontal = when {
            diagonalInches >= 100 -> 80.dp
            diagonalInches >= 80 -> 72.dp
            diagonalInches >= 70 -> 68.dp
            diagonalInches >= 65 -> 64.dp
            diagonalInches >= 60 -> 60.dp
            diagonalInches >= 55 -> 54.dp
            diagonalInches >= 50 -> 50.dp
            diagonalInches >= 45 -> 46.dp
            diagonalInches >= 40 -> 42.dp
            diagonalInches >= 32 -> 38.dp
            else -> 32.dp
          }
          val vertical = when {
            diagonalInches >= 100 -> 58.dp
            diagonalInches >= 80 -> 52.dp
            diagonalInches >= 70 -> 48.dp
            diagonalInches >= 65 -> 44.dp
            diagonalInches >= 60 -> 40.dp
            diagonalInches >= 55 -> 36.dp
            diagonalInches >= 50 -> 32.dp
            diagonalInches >= 45 -> 28.dp
            diagonalInches >= 40 -> 26.dp
            diagonalInches >= 32 -> 24.dp
            else -> 20.dp
          }
          SafePaddingResult(
            padding = PaddingValues(horizontal = horizontal, vertical = vertical),
            profile = "projector_auto",
            scaleFactor = 0.9f,
            overscanAdjusted = true
          )
        }

        MaxiApp.isNativeTv -> {
          val horizontal = when {
            diagonalInches >= 85 -> 68.dp
            diagonalInches >= 75 -> 64.dp
            diagonalInches >= 70 -> 60.dp
            diagonalInches >= 65 -> 56.dp
            diagonalInches >= 60 -> 52.dp
            diagonalInches >= 55 -> 48.dp
            diagonalInches >= 50 -> 44.dp
            diagonalInches >= 45 -> 40.dp
            diagonalInches >= 43 -> 38.dp
            diagonalInches >= 40 -> 36.dp
            diagonalInches >= 32 -> 32.dp
            else -> 28.dp
          }
          val vertical = when {
            diagonalInches >= 85 -> 50.dp
            diagonalInches >= 75 -> 46.dp
            diagonalInches >= 70 -> 42.dp
            diagonalInches >= 65 -> 40.dp
            diagonalInches >= 60 -> 36.dp
            diagonalInches >= 55 -> 34.dp
            diagonalInches >= 50 -> 30.dp
            diagonalInches >= 45 -> 28.dp
            diagonalInches >= 43 -> 26.dp
            diagonalInches >= 40 -> 24.dp
            diagonalInches >= 32 -> 22.dp
            else -> 20.dp
          }
          SafePaddingResult(
            padding = PaddingValues(horizontal = horizontal, vertical = vertical),
            profile = "native_tv_auto",
            scaleFactor = when {
              diagonalInches >= 85 -> 0.86f
              diagonalInches >= 75 -> 0.88f
              diagonalInches >= 70 -> 0.89f
              diagonalInches >= 65 -> 0.9f
              diagonalInches >= 60 -> 0.91f
              diagonalInches >= 55 -> 0.92f
              diagonalInches >= 50 -> 0.93f
              diagonalInches >= 45 -> 0.95f
              diagonalInches >= 43 -> 0.955f
              diagonalInches >= 40 -> 0.96f
              diagonalInches >= 32 -> 0.97f
              else -> 0.98f
            },
            overscanAdjusted = true
          )
        }

        MaxiApp.isTvBox -> {
          val horizontal = when {
            diagonalInches >= 75 -> 50.dp
            diagonalInches >= 70 -> 48.dp
            diagonalInches >= 65 -> 44.dp
            diagonalInches >= 60 -> 40.dp
            diagonalInches >= 55 -> 36.dp
            diagonalInches >= 50 -> 34.dp
            diagonalInches >= 45 -> 30.dp
            diagonalInches >= 43 -> 28.dp
            diagonalInches >= 40 -> 26.dp
            diagonalInches >= 32 -> 24.dp
            else -> 20.dp
          }
          val vertical = when {
            diagonalInches >= 75 -> 38.dp
            diagonalInches >= 70 -> 36.dp
            diagonalInches >= 65 -> 32.dp
            diagonalInches >= 60 -> 30.dp
            diagonalInches >= 55 -> 26.dp
            diagonalInches >= 50 -> 24.dp
            diagonalInches >= 45 -> 22.dp
            diagonalInches >= 43 -> 20.dp
            diagonalInches >= 40 -> 18.dp
            diagonalInches >= 32 -> 16.dp
            else -> 14.dp
          }
          SafePaddingResult(
            padding = PaddingValues(horizontal = horizontal, vertical = vertical),
            profile = "tv_box_auto",
            scaleFactor = when {
              diagonalInches >= 75 -> 0.90f
              diagonalInches >= 70 -> 0.91f
              diagonalInches >= 65 -> 0.92f
              diagonalInches >= 60 -> 0.93f
              diagonalInches >= 55 -> 0.94f
              diagonalInches >= 50 -> 0.95f
              diagonalInches >= 45 -> 0.96f
              diagonalInches >= 43 -> 0.965f
              diagonalInches >= 40 -> 0.97f
              diagonalInches >= 32 -> 0.98f
              else -> 0.99f
            },
            overscanAdjusted = true
          )
        }

        else -> SafePaddingResult(
          padding = PaddingValues(),
          profile = "generic",
          scaleFactor = 1f,
          overscanAdjusted = false
        )
      }
    }
  }

  val safePadding = paddingResult.padding
  val scaleFactor = paddingResult.scaleFactor
  val paddingProfile = paddingResult.profile

  LaunchedEffect(safePadding, scaleFactor, diagonalInches, paddingProfile, layoutDirection) {
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
    
    // Detecção automática na primeira vez (só se não tiver override salvo)
    if (overrideState == null && autoDetectedOverride == null) {
      AutoDetectSafeArea(
        currentPadding = safePadding,
        currentScale = scaleFactor
      ) { detectedOverride ->
        // Quando detecta, atualiza o override localmente
        SafeAreaOverrides.update(context, fingerprintInfo.key, detectedOverride)
      }
    }
  }
}


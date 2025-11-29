package com.maxiptv.ui.screens
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.maxiptv.ui.player.PlayerActivity

@Composable
fun PlayerSurface(currentUrl: String?, channelName: String? = null) {
  val ctx = LocalContext.current
  var lastOpenedUrl by remember { mutableStateOf<String?>(null) }
  
  LaunchedEffect(currentUrl) {
    currentUrl?.let { url ->
      // ✅ Proteção: evitar abrir múltiplas Activities se o URL não mudou
      if (url != lastOpenedUrl) {
        try {
          lastOpenedUrl = url
          android.util.Log.i("PlayerSurface", "📺 Abrindo PlayerActivity para: $url")
          val i = Intent(ctx, PlayerActivity::class.java)
          i.putExtra("url", url)
          i.putExtra("contentType", "live") // PlayerSurface é usado para Live
          channelName?.let { name ->
            i.putExtra("channelName", name) // Passar nome do canal para identificar futebol
          }
          ctx.startActivity(i)
        } catch (e: Exception) {
          android.util.Log.e("PlayerSurface", "❌ Erro ao abrir PlayerActivity: ${e.message}", e)
          lastOpenedUrl = null // Reset para permitir tentar novamente
        }
      } else {
        android.util.Log.d("PlayerSurface", "⏭️ URL não mudou ($url), ignorando...")
      }
    }
  }
}

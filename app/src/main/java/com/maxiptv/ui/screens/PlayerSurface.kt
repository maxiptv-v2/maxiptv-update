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
  var activityStarted by remember { mutableStateOf(false) }
  
  LaunchedEffect(currentUrl) {
    currentUrl?.let { url ->
      // ✅ Proteção: evitar abrir múltiplas Activities se o URL não mudou
      // ✅ CORREÇÃO: Só abrir Activity se o URL realmente mudou E não foi aberta ainda
      if (url != lastOpenedUrl && !activityStarted) {
        try {
          lastOpenedUrl = url
          activityStarted = true
          android.util.Log.i("PlayerSurface", "📺 Abrindo PlayerActivity para: $url")
          val i = Intent(ctx, PlayerActivity::class.java)
          i.putExtra("url", url)
          i.putExtra("contentType", "live") // PlayerSurface é usado para Live
          channelName?.let { name ->
            i.putExtra("channelName", name) // Passar nome do canal para identificar futebol
          }
          // ✅ CORREÇÃO SMARTPHONE: Usar FLAG_ACTIVITY_SINGLE_TOP mas NÃO CLEAR_TOP
          // CLEAR_TOP pode causar fechamento do player em smartphones
          i.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
          ctx.startActivity(i)
        } catch (e: Exception) {
          android.util.Log.e("PlayerSurface", "❌ Erro ao abrir PlayerActivity: ${e.message}", e)
          lastOpenedUrl = null // Reset para permitir tentar novamente
          activityStarted = false
        }
      } else if (url == lastOpenedUrl) {
        android.util.Log.d("PlayerSurface", "⏭️ URL não mudou ($url), ignorando...")
      } else {
        // URL mudou mas Activity já foi iniciada - atualizar apenas se necessário
        android.util.Log.d("PlayerSurface", "⏭️ Activity já iniciada para URL diferente, ignorando nova abertura...")
      }
    } ?: run {
      // Se currentUrl for null, resetar estado
      if (lastOpenedUrl != null) {
        android.util.Log.d("PlayerSurface", "🔄 URL tornou-se null, resetando estado...")
        lastOpenedUrl = null
        activityStarted = false
      }
    }
  }
}

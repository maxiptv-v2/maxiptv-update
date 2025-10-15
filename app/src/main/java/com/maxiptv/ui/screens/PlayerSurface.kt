package com.maxiptv.ui.screens
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.maxiptv.ui.player.PlayerActivity

@Composable
fun PlayerSurface(currentUrl: String?) {
  val ctx = LocalContext.current
  LaunchedEffect(currentUrl) {
    currentUrl?.let {
      val i = Intent(ctx, PlayerActivity::class.java)
      i.putExtra("url", it)
      i.putExtra("contentType", "live") // PlayerSurface é usado para Live
      ctx.startActivity(i)
    }
  }
}

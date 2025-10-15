package com.maxiptv
import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import com.maxiptv.data.AppCtx
import com.maxiptv.data.SettingsRepo
import com.maxiptv.data.XRepo

class MaxiApp : Application() {
  companion object { 
    var isTv: Boolean = false
    var isFireStick: Boolean = false
    var isPhone: Boolean = false
    var isTablet: Boolean = false
  }
  
  override fun onCreate() {
    super.onCreate()
    try {
      val ui = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
      val isTvMode = ui.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
      
      // Detecção mais precisa de dispositivos
      val manufacturer = android.os.Build.MANUFACTURER.lowercase()
      val model = android.os.Build.MODEL.lowercase()
      
      isTv = isTvMode || manufacturer.contains("amazon") || model.contains("fire") || model.contains("tv")
      isFireStick = manufacturer.contains("amazon") || model.contains("fire")
      isPhone = !isTv && resources.configuration.screenWidthDp <= 600
      isTablet = !isTv && resources.configuration.screenWidthDp > 600
      
      AppCtx.ctx = applicationContext
      
      // Configurar com credenciais padrão primeiro (para não crashar)
      XRepo.configure(BuildConfig.DEFAULT_PLAYER_API, BuildConfig.DEFAULT_USER, BuildConfig.DEFAULT_PASS)
      
      // Tentar carregar credenciais salvas (se houver)
      try {
        val (b,u,p) = SettingsRepo.loadBlocking()
        if (b.isNotBlank() && u.isNotBlank() && p.isNotBlank()) {
          XRepo.configure(b, u, p)
        }
      } catch (e: Exception) {
        // Ignora erros ao carregar configurações (usa padrão)
        android.util.Log.e("MaxiApp", "Erro ao carregar settings: ${e.message}")
      }
    } catch (e: Exception) {
      // Evita crash total do app
      android.util.Log.e("MaxiApp", "Erro na inicialização: ${e.message}")
    }
  }
}

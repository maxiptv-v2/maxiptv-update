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
      val brand = android.os.Build.BRAND.lowercase()
      val product = android.os.Build.PRODUCT.lowercase()
      
      // Detecta Fire Stick especificamente (ANTES da detecção de TV)
      isFireStick = manufacturer.contains("amazon") || 
                    model.contains("fire") || 
                    product.contains("fire")
      
      // Detecta TV (PRIORIZA UI Mode e características específicas)
      isTv = isTvMode ||  // UI Mode TV é o mais confiável
             isFireStick ||
             model.contains("chromecast") ||
             product.contains("chromecast") ||
             model.contains("tv") ||
             product.contains("atv") ||
             model.contains("android tv") ||
             product.contains("android tv")
      
      // Detecta Phone e Tablet baseado no tamanho da tela (APENAS se NÃO for TV)
      val screenWidth = resources.configuration.screenWidthDp
      val screenHeight = resources.configuration.screenHeightDp
      val smallestWidth = minOf(screenWidth, screenHeight)
      
      // Só aplica detecção por tamanho se NÃO for TV
      if (!isTv) {
        isPhone = smallestWidth <= 600
        isTablet = smallestWidth > 600
      } else {
        // Se é TV, força Phone e Tablet como false
        isPhone = false
        isTablet = false
      }
      
      // Log detalhado para debug
      android.util.Log.i("MaxiApp", "═══════════════════════════════════════")
      android.util.Log.i("MaxiApp", "📱 DETECÇÃO DE DISPOSITIVO")
      android.util.Log.i("MaxiApp", "Fabricante: $manufacturer")
      android.util.Log.i("MaxiApp", "Modelo: $model")
      android.util.Log.i("MaxiApp", "Marca: $brand")
      android.util.Log.i("MaxiApp", "Produto: $product")
      android.util.Log.i("MaxiApp", "UI Mode: ${if (isTvMode) "TELEVISION" else "NORMAL"}")
      android.util.Log.i("MaxiApp", "Largura: ${screenWidth}dp")
      android.util.Log.i("MaxiApp", "Altura: ${screenHeight}dp")
      android.util.Log.i("MaxiApp", "Menor largura: ${smallestWidth}dp")
      android.util.Log.i("MaxiApp", "───────────────────────────────────────")
      android.util.Log.i("MaxiApp", "✅ Tipo detectado: ${when {
        isFireStick -> "Fire Stick"
        isTv -> "TV Box / Android TV / Chromecast"
        isTablet -> "Tablet"
        isPhone -> "Smartphone"
        else -> "Desconhecido"
      }}")
      android.util.Log.i("MaxiApp", "═══════════════════════════════════════")
      
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

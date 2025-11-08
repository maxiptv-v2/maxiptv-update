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
    
    // 🔥 CONFIGURAÇÕES ESPECÍFICAS PARA FIRE STICK (AJUSTÁVEIS POR TAMANHO DE TV)
    var fireStickOverscanPadding: Int = 48 // dp - será ajustado automaticamente
    var fireStickSafeAreaPadding: Int = 24 // dp - será ajustado automaticamente
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
      
      // 📱 DETECÇÃO MELHORADA DE SMARTPHONE E TABLET
      val screenWidth = resources.configuration.screenWidthDp
      val screenHeight = resources.configuration.screenHeightDp
      val smallestWidth = minOf(screenWidth, screenHeight)
      
      // Detecção mais precisa baseada em características do dispositivo
      val hasTouchscreen = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TOUCHSCREEN)
      val isPhoneLike = manufacturer.contains("samsung") || 
                       manufacturer.contains("xiaomi") || 
                       manufacturer.contains("huawei") || 
                       manufacturer.contains("motorola") ||
                       manufacturer.contains("lg") ||
                       manufacturer.contains("oneplus") ||
                       manufacturer.contains("oppo") ||
                       manufacturer.contains("vivo") ||
                       manufacturer.contains("realme")
      
      // Só aplica detecção por tamanho se NÃO for TV
      if (!isTv) {
        // Detecção mais inteligente de smartphone
        isPhone = (smallestWidth <= 600 && hasTouchscreen && isPhoneLike) ||
                 (smallestWidth <= 480 && hasTouchscreen) // Smartphones pequenos
        isTablet = smallestWidth > 600 && hasTouchscreen && !isPhone
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
      android.util.Log.i("MaxiApp", "Touchscreen: $hasTouchscreen")
      android.util.Log.i("MaxiApp", "Phone-like: $isPhoneLike")
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
      
      // 🔥 CONFIGURAÇÃO AUTOMÁTICA POR TAMANHO DE TV (FIRE STICK)
      if (isFireStick) {
        // Calcular padding baseado no tamanho da tela
        val diagonalInches = kotlin.math.sqrt((screenWidth * screenWidth + screenHeight * screenHeight).toDouble()) / 160.0
        
        fireStickOverscanPadding = when {
          diagonalInches < 40 -> 32  // TV pequena (32"-39")
          diagonalInches < 50 -> 40  // TV média (40"-49") 
          diagonalInches < 60 -> 48  // TV grande (50"-59")
          else -> 56  // TV muito grande (60"+)
        }
        
        fireStickSafeAreaPadding = fireStickOverscanPadding / 2
        
        android.util.Log.i("MaxiApp", "🔥 CONFIGURAÇÃO FIRE STICK:")
        android.util.Log.i("MaxiApp", "Tamanho da tela: ${diagonalInches.toInt()}\"")
        android.util.Log.i("MaxiApp", "Overscan Padding: ${fireStickOverscanPadding}dp")
        android.util.Log.i("MaxiApp", "Safe Area Padding: ${fireStickSafeAreaPadding}dp")
        android.util.Log.i("MaxiApp", "Layout otimizado para controle remoto")
      }
      
      android.util.Log.i("MaxiApp", "═══════════════════════════════════════")
      
      AppCtx.ctx = applicationContext
      
      // Configurar com credenciais padrão primeiro (para não crashar)
      XRepo.configure(BuildConfig.DEFAULT_PLAYER_API, BuildConfig.DEFAULT_USER, BuildConfig.DEFAULT_PASS)
      
      // Tentar carregar credenciais salvas (se houver)
      try {
        val (b,u,p) = SettingsRepo.loadBlocking()
        if (b.isNotBlank() && u.isNotBlank() && p.isNotBlank()) {
          XRepo.configure(b, u, p)
          android.util.Log.i("MaxiApp", "✅ Credenciais carregadas: $u")
        } else {
          android.util.Log.i("MaxiApp", "⚠️ Nenhuma credencial salva, usando padrão")
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

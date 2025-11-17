# 🎬 Melhorias Profissionais para ExoPlayer

## 📊 ANÁLISE DO ESTADO ATUAL

### ✅ Já Implementado
- ✅ Controles básicos (play/pause/seek)
- ✅ Qualidade adaptativa automática
- ✅ Reconexão automática
- ✅ Detecção de Wi-Fi lento
- ✅ EPG overlay no mini player
- ✅ Configurações de qualidade e velocidade (mas **não aplicadas no player**)
- ✅ Fullscreen com gestos (duplo toque)

### ⚠️ Faltando (Funcionalidades Profissionais)

---

## 🎯 MELHORIAS PRIORITÁRIAS

### 1. **CONTROLES CUSTOMIZADOS AVANÇADOS** 🔴 CRÍTICO
**Problema:** Controles padrão do ExoPlayer são básicos.

**Implementar:**
- ✅ Botão avançar/retroceder 10 segundos
- ✅ Botão avançar/retroceder 30 segundos
- ✅ Indicador de buffer visual
- ✅ Indicador de qualidade atual
- ✅ Botão de qualidade (seleção manual)
- ✅ Botão de legendas (se disponível)
- ✅ Botão de velocidade de reprodução
- ✅ Controles adaptados para TV (D-PAD)

**Código:**
```kotlin
// Criar CustomPlayerControlView
class CustomPlayerControlView(context: Context) : FrameLayout(context) {
    private val player: ExoPlayer? = null
    
    // Botões customizados
    private val forward10Button: ImageButton
    private val rewind10Button: ImageButton
    private val qualityButton: ImageButton
    private val subtitleButton: ImageButton
    private val speedButton: ImageButton
    
    fun setupPlayer(player: ExoPlayer) {
        // Configurar listeners
        forward10Button.setOnClickListener {
            player.seekForward(10000) // 10 segundos
        }
        rewind10Button.setOnClickListener {
            player.seekBack(10000) // 10 segundos
        }
    }
}
```

---

### 2. **SELECÇÃO MANUAL DE QUALIDADE** 🔴 CRÍTICO
**Problema:** Configurações de qualidade existem mas não são aplicadas.

**Implementar:**
- ✅ Dialog para selecionar qualidade manualmente
- ✅ Listar todas as qualidades disponíveis
- ✅ Aplicar qualidade selecionada imediatamente
- ✅ Mostrar qualidade atual nos controles

**Código:**
```kotlin
// Em PlayerActivity.kt
private fun showQualityDialog() {
    val tracks = player?.currentTracks
    val videoTracks = tracks?.groups?.flatMap { it.tracks }
        ?.filter { it.format.sampleMimeType?.startsWith("video/") == true }
        ?.map { it.format }
    
    val qualityOptions = videoTracks?.map { format ->
        val resolution = "${format.width}x${format.height}"
        val bitrate = format.bitrate / 1000 // Kbps
        "$resolution @ ${bitrate}Kbps"
    } ?: emptyList()
    
    // Mostrar dialog com opções
    AlertDialog.Builder(this)
        .setTitle("Selecionar Qualidade")
        .setItems(qualityOptions.toTypedArray()) { _, which ->
            val selectedTrack = videoTracks[which]
            // Aplicar qualidade
            player?.trackSelectionParameters = 
                TrackSelectionParameters.Builder(this)
                    .setMaxVideoBitrate(selectedTrack.bitrate)
                    .setMaxVideoSize(selectedTrack.width, selectedTrack.height)
                    .build()
        }
        .show()
}
```

---

### 3. **VELOCIDADE DE REPRODUÇÃO** 🔴 CRÍTICO
**Problema:** Configurações de velocidade existem mas não são aplicadas.

**Implementar:**
- ✅ Aplicar velocidade configurada no PlayerSettingsManager
- ✅ Botão rápido para alternar velocidades
- ✅ Dialog para selecionar velocidade
- ✅ Indicador visual da velocidade atual

**Código:**
```kotlin
// Em PlayerActivity.kt - onCreate()
LaunchedEffect(Unit) {
    val speed = PlayerSettingsManager.getPlaybackSpeed()
    player?.playbackParameters = PlaybackParameters(speed.multiplier)
    android.util.Log.i("PlayerActivity", "✅ Velocidade aplicada: ${speed.displayName}")
}

// Botão rápido nos controles
speedButton.setOnClickListener {
    val currentSpeed = player?.playbackParameters?.speed ?: 1.0f
    val nextSpeed = when {
        currentSpeed < 1.0f -> 1.0f
        currentSpeed < 1.5f -> 1.5f
        currentSpeed < 2.0f -> 2.0f
        else -> 0.75f
    }
    player?.playbackParameters = PlaybackParameters(nextSpeed)
}
```

---

### 4. **LEGENDAS/SUBTÍTULOS** 🟡 IMPORTANTE
**Problema:** Não há suporte para legendas.

**Implementar:**
- ✅ Detectar legendas disponíveis no stream
- ✅ Dialog para selecionar legenda
- ✅ Botão para mostrar/esconder legendas
- ✅ Estilização de legendas (tamanho, cor, posição)

**Código:**
```kotlin
// Detectar legendas disponíveis
private fun getAvailableSubtitles(): List<Format> {
    val tracks = player?.currentTracks
    return tracks?.groups?.flatMap { it.tracks }
        ?.filter { it.format.sampleMimeType?.startsWith("text/") == true }
        ?.map { it.format } ?: emptyList()
}

// Aplicar legenda selecionada
private fun selectSubtitle(format: Format?) {
    player?.trackSelectionParameters = 
        TrackSelectionParameters.Builder(this)
            .setPreferredTextLanguage(format?.language)
            .build()
}

// Estilização de legendas
private fun setupSubtitleStyle() {
    val subtitleView = playerView.subtitleView
    subtitleView?.setStyle(
        CaptionStyleCompat(
            Color.WHITE, // Cor do texto
            Color.TRANSPARENT, // Cor de fundo
            Color.TRANSPARENT, // Cor da borda
            CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
            Color.WHITE, // Cor da sombra
            Typeface.DEFAULT_BOLD // Fonte
        )
    )
    subtitleView?.setFixedTextSize(
        TypedValue.COMPLEX_UNIT_DIP, 
        18f // Tamanho da fonte
    )
}
```

---

### 5. **GESTOS AVANÇADOS** 🟡 IMPORTANTE
**Problema:** Apenas duplo toque e clique simples.

**Implementar:**
- ✅ Swipe horizontal: avançar/retroceder
- ✅ Swipe vertical esquerda: ajustar brilho
- ✅ Swipe vertical direita: ajustar volume
- ✅ Pinch to zoom: zoom no vídeo
- ✅ Indicadores visuais durante gestos

**Código:**
```kotlin
// GestureDetector melhorado
private val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
    private var startX = 0f
    private var startY = 0f
    
    override fun onDown(e: MotionEvent): Boolean {
        startX = e.x
        startY = e.y
        return true
    }
    
    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        val deltaX = e2.x - startX
        val deltaY = e2.y - startY
        
        // Swipe horizontal: avançar/retroceder
        if (abs(deltaX) > abs(deltaY) && abs(deltaX) > 50) {
            val seekAmount = (deltaX / 10).toLong() // 10 pixels = 1 segundo
            player?.seekTo(player?.currentPosition?.plus(seekAmount) ?: 0)
            showSeekIndicator(seekAmount)
            return true
        }
        
        // Swipe vertical esquerda: brilho
        if (abs(deltaY) > abs(deltaX) && e2.x < width / 2 && abs(deltaY) > 50) {
            adjustBrightness(deltaY)
            return true
        }
        
        // Swipe vertical direita: volume
        if (abs(deltaY) > abs(deltaX) && e2.x > width / 2 && abs(deltaY) > 50) {
            adjustVolume(deltaY)
            return true
        }
        
        return false
    }
    
    override fun onDoubleTap(e: MotionEvent): Boolean {
        // Duplo toque: play/pause
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
        return true
    }
})
```

---

### 6. **PICTURE-IN-PICTURE (PiP)** 🟡 IMPORTANTE
**Problema:** Não há suporte para PiP.

**Implementar:**
- ✅ Ativar PiP quando app vai para background
- ✅ Controles básicos no PiP
- ✅ Botão para entrar/sair do PiP
- ✅ Suportar PiP apenas em VOD/Series (não em Live)

**Código:**
```kotlin
// No AndroidManifest.xml (já tem supportsPictureInPicture="true")

// Em PlayerActivity.kt
private fun enterPictureInPictureMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }
}

override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    // Entrar em PiP quando usuário sai do app (apenas VOD/Series)
    if (contentType != "live") {
        enterPictureInPictureMode()
    }
}

override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration?
) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    if (isInPictureInPictureMode) {
        // Esconder controles completos
        playerView.useController = false
    } else {
        // Restaurar controles
        playerView.useController = true
    }
}
```

---

### 7. **HISTÓRICO DE REPRODUÇÃO** 🟢 RECOMENDADO
**Problema:** Não há histórico de assistidos.

**Implementar:**
- ✅ Salvar posição de reprodução (VOD/Series)
- ✅ Continuar de onde parou
- ✅ Lista de últimos assistidos
- ✅ Marcar como assistido

**Código:**
```kotlin
// Classe para gerenciar histórico
object PlaybackHistoryManager {
    private val dataStore = AppCtx.ctx.dataStore
    
    suspend fun savePlaybackPosition(
        contentId: String,
        contentType: String, // "vod" ou "series"
        position: Long,
        duration: Long
    ) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("playback_${contentType}_$contentId")] = position.toString()
            prefs[stringPreferencesKey("duration_${contentType}_$contentId")] = duration.toString()
            prefs[longPreferencesKey("last_watched_${contentType}_$contentId")] = System.currentTimeMillis()
        }
    }
    
    suspend fun getPlaybackPosition(contentId: String, contentType: String): Long {
        val prefs = dataStore.data.first()
        return prefs[stringPreferencesKey("playback_${contentType}_$contentId")]?.toLongOrNull() ?: 0L
    }
    
    suspend fun shouldShowResumeDialog(contentId: String, contentType: String): Boolean {
        val position = getPlaybackPosition(contentId, contentType)
        return position > 10000 // Mais de 10 segundos assistidos
    }
}

// Em PlayerActivity.kt - ao iniciar VOD/Series
if (contentType != "live") {
    val savedPosition = PlaybackHistoryManager.getPlaybackPosition(contentId, contentType)
    if (savedPosition > 0 && PlaybackHistoryManager.shouldShowResumeDialog(contentId, contentType)) {
        // Mostrar dialog: "Continuar de onde parou?"
        showResumeDialog(savedPosition)
    }
}

// Salvar posição periodicamente
player?.addListener(object : Player.Listener {
    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        if (contentType != "live") {
            val currentPos = player?.currentPosition ?: 0L
            val duration = player?.duration ?: 0L
            PlaybackHistoryManager.savePlaybackPosition(contentId, contentType, currentPos, duration)
        }
    }
})
```

---

### 8. **ESTATÍSTICAS DE REDE** 🟢 RECOMENDADO
**Problema:** Não há informações sobre qualidade da conexão.

**Implementar:**
- ✅ Mostrar bitrate atual
- ✅ Mostrar resolução atual
- ✅ Mostrar buffer disponível
- ✅ Mostrar taxa de download
- ✅ Indicador de qualidade da conexão

**Código:**
```kotlin
// Overlay de estatísticas
private fun showNetworkStats() {
    val format = player?.videoFormat
    val bitrate = format?.bitrate ?: 0
    val resolution = "${format?.width ?: 0}x${format?.height ?: 0}"
    val bufferPosition = player?.bufferedPosition ?: 0L
    val currentPosition = player?.currentPosition ?: 0L
    val bufferPercent = if (player?.duration ?: 0L > 0) {
        ((bufferPosition - currentPosition) * 100 / player!!.duration).toInt()
    } else 0
    
    // Mostrar em overlay
    statsOverlay.text = """
        📊 Estatísticas:
        Resolução: $resolution
        Bitrate: ${bitrate / 1000}Kbps
        Buffer: $bufferPercent%
    """.trimIndent()
}
```

---

### 9. **CONTROLES PARA TV (D-PAD)** 🟢 RECOMENDADO
**Problema:** Controles não são otimizados para TV.

**Implementar:**
- ✅ Navegação com D-PAD nos controles
- ✅ Foco visual nos botões
- ✅ Atalhos de teclado (setas, OK, etc)
- ✅ Menu de opções com D-PAD

**Código:**
```kotlin
// Em PlayerActivity.kt
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (MaxiApp.isTv) {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // Retroceder 10 segundos
                player?.seekBack(10000)
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // Avançar 10 segundos
                player?.seekForward(10000)
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                // Aumentar volume
                val currentVolume = player?.volume ?: 0f
                player?.volume = (currentVolume + 0.1f).coerceAtMost(1.0f)
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Diminuir volume
                val currentVolume = player?.volume ?: 0f
                player?.volume = (currentVolume - 0.1f).coerceAtLeast(0f)
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                // Mostrar menu de opções
                showOptionsMenu()
                return true
            }
        }
    }
    return super.onKeyDown(keyCode, event)
}
```

---

### 10. **CHROMECAST** 🟢 RECOMENDADO
**Problema:** Não há suporte para Chromecast.

**Implementar:**
- ✅ Detectar dispositivos Chromecast
- ✅ Botão de cast nos controles
- ✅ Controles remotos durante cast
- ✅ Sincronização de posição

**Código:**
```kotlin
// Adicionar dependência no build.gradle.kts
implementation("com.google.android.gms:play-services-cast-framework:21.5.0")

// Configurar Cast
class CastManager {
    private val castContext: CastContext
    
    fun setupCast(player: ExoPlayer) {
        val castPlayer = CastPlayer(castContext)
        val playerSelector = DefaultTrackSelector(context)
        val loadControl = DefaultLoadControl.Builder().build()
        
        val mediaItem = MediaItem.fromUri(url)
        castPlayer.setMediaItem(mediaItem)
        castPlayer.prepare()
    }
}
```

---

## 📋 OUTRAS MELHORIAS

### 11. **AUDIO BOOST**
- Aumentar ganho de áudio dinamicamente
- Equalizador básico
- Normalização de áudio

### 12. **ZOOM NO VÍDEO**
- Pinch to zoom
- Ajuste de escala manual
- Reset de zoom

### 13. **MODO AUDIO APENAS**
- Reproduzir apenas áudio quando em background
- Notificação de mídia
- Controles na notificação

### 14. **COMPARTILHAMENTO**
- Compartilhar link do conteúdo
- Screenshot do frame atual
- Compartilhar posição atual

### 15. **AUTOPLAY PRÓXIMO EPISÓDIO**
- Detectar fim do episódio
- Contador regressivo
- Pular ou assistir próximo

---

## 🎯 PLANO DE IMPLEMENTAÇÃO

### Fase 1 - Crítico (1 semana)
1. ✅ Aplicar velocidade de reprodução configurada
2. ✅ Aplicar qualidade configurada
3. ✅ Seleção manual de qualidade
4. ✅ Controles avançados (avançar/retroceder 10s)

### Fase 2 - Importante (1-2 semanas)
5. ✅ Legendas/Subtítulos
6. ✅ Gestos avançados
7. ✅ Histórico de reprodução
8. ✅ Picture-in-Picture

### Fase 3 - Recomendado (2-3 semanas)
9. ✅ Estatísticas de rede
10. ✅ Controles para TV
11. ✅ Chromecast
12. ✅ Autoplay próximo episódio

---

## 📊 RESUMO

**Total de Funcionalidades:** 15 melhorias profissionais

**Prioridade Alta:** 4 funcionalidades
**Prioridade Média:** 4 funcionalidades  
**Prioridade Baixa:** 7 funcionalidades

**Tempo Estimado:** 4-6 semanas para implementação completa

**Impacto:** Transformará o player em uma experiência profissional de nível Netflix/YouTube.


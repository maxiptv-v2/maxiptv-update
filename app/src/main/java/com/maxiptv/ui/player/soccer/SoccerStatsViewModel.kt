package com.maxiptv.ui.player.soccer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxiptv.MaxiApp
import com.maxiptv.data.soccer.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar estatísticas de futebol
 * Faz polling automático adaptativo baseado no tipo de dispositivo:
 * - TV Box genéricas/dispositivos com menor desempenho: 45 segundos
 * - Fire Stick/TV Box premium: 30 segundos
 * - Smartphones/Tablets: 25 segundos
 */
class SoccerStatsViewModel : ViewModel() {
    
    private val TAG = "SoccerStatsViewModel"
    
    // ✅ Usar tipos da API Sports
    var currentMatchDetail: MatchDetailFull? = null
        private set
    
    var currentMatchPreview: MatchPreviewFull? = null
        private set
    
    var otherMatches: List<MatchSummaryFull> = emptyList()
        private set
    
    private var pollingJob: Job? = null
    private var currentMatchId: Long? = null
    
    /**
     * Calcula intervalo de polling baseado no tipo de dispositivo
     * Dispositivos com menor desempenho (TV Box genéricas) usam intervalo maior
     */
    private val pollingInterval: Long
        get() = when {
            // TV Box genéricas ou dispositivos com menor desempenho: polling mais espaçado
            MaxiApp.isTvBox && !MaxiApp.isFireStick -> 45_000L // 45 segundos
            // Fire Stick ou TV Box premium: intervalo médio
            MaxiApp.isFireStick || MaxiApp.isNativeTv -> 30_000L // 30 segundos
            // Smartphones/Tablets: intervalo padrão
            else -> 25_000L // 25 segundos
        }
    
    /**
     * Abre overlay e inicia polling de estatísticas
     */
    fun openOverlay(matchId: Long) {
        if (currentMatchId == matchId && pollingJob?.isActive == true) {
            Log.d(TAG, "Overlay já está aberto para partida $matchId")
            return
        }
        
        currentMatchId = matchId
        pollingJob?.cancel()
        
        // Buscar dados imediatamente
        viewModelScope.launch {
            try {
                currentMatchDetail = SoccerRepository.getMatchDetail(matchId)
                currentMatchPreview = SoccerRepository.getMatchPreview(matchId)
                otherMatches = SoccerRepository.getOtherMatches()
                Log.d(TAG, "Dados iniciais carregados para partida $matchId")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar dados iniciais", e)
            }
        }
        
        // Iniciar polling com intervalo adaptativo baseado no dispositivo
        val interval = pollingInterval
        Log.d(TAG, "Iniciando polling com intervalo de ${interval / 1000}s para dispositivo: ${MaxiApp.deviceCategory}")
        
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    delay(interval)
                    
                    if (currentMatchId != null) {
                        Log.d(TAG, "Atualizando estatísticas da partida $currentMatchId")
                        currentMatchDetail = SoccerRepository.getMatchDetail(currentMatchId!!)
                        currentMatchPreview = SoccerRepository.getMatchPreview(currentMatchId!!)
                        otherMatches = SoccerRepository.getOtherMatches()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao atualizar estatísticas", e)
                    // Continuar tentando mesmo em caso de erro
                    // Em caso de erro, aguardar um pouco mais antes de tentar novamente
                    delay(5_000L)
                }
            }
        }
    }
    
    /**
     * Fecha overlay e para polling
     */
    fun closeOverlay() {
        Log.d(TAG, "Fechando overlay e parando polling")
        pollingJob?.cancel()
        pollingJob = null
        currentMatchId = null
        // Não limpar dados imediatamente, pode ser útil manter por um tempo
    }
    
    /**
     * Limpa todos os dados
     */
    fun clearData() {
        closeOverlay()
        currentMatchDetail = null
        currentMatchPreview = null
        otherMatches = emptyList()
    }
    
    override fun onCleared() {
        super.onCleared()
        closeOverlay()
    }
}


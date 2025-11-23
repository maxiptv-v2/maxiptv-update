package com.maxiptv.ui.player.soccer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxiptv.data.soccer.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel para gerenciar estatísticas de futebol
 * Faz polling automático a cada 25 segundos para atualizar dados
 */
class SoccerStatsViewModel : ViewModel() {
    
    private val TAG = "SoccerStatsViewModel"
    
    var currentMatchDetail: MatchDetail? = null
        private set
    
    var currentMatchPreview: MatchPreview? = null
        private set
    
    var otherMatches: List<MatchSummary> = emptyList()
        private set
    
    private var pollingJob: Job? = null
    private var currentMatchId: Long? = null
    
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
        
        // Iniciar polling a cada 25 segundos
        pollingJob = viewModelScope.launch {
            while (true) {
                try {
                    delay(25_000L) // 25 segundos
                    
                    if (currentMatchId != null) {
                        Log.d(TAG, "Atualizando estatísticas da partida $currentMatchId")
                        currentMatchDetail = SoccerRepository.getMatchDetail(currentMatchId!!)
                        currentMatchPreview = SoccerRepository.getMatchPreview(currentMatchId!!)
                        otherMatches = SoccerRepository.getOtherMatches()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao atualizar estatísticas", e)
                    // Continuar tentando mesmo em caso de erro
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


package com.maxiptv.data.soccer

/**
 * Utilitário para extrair matchId de nomes de canais de futebol
 * Tenta identificar o ID da partida a partir do nome do canal
 */
object MatchIdExtractor {
    
    /**
     * Extrai matchId do nome do canal
     * Formato esperado: "Premiere 1 - Flamengo x Palmeiras - Match 12345"
     * ou "ESPN - Partida 67890" ou similar
     */
    fun extractMatchId(channelName: String): Long? {
        // Padrões comuns para matchId em nomes de canais
        val patterns = listOf(
            Regex("match[\\s_-]*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("partida[\\s_-]*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("game[\\s_-]*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("id[\\s_-]*(\\d+)", RegexOption.IGNORE_CASE),
            Regex("#(\\d{5,})"), // IDs com pelo menos 5 dígitos após #
        )
        
        for (pattern in patterns) {
            val match = pattern.find(channelName)
            if (match != null) {
                val id = match.groupValues[1].toLongOrNull()
                if (id != null) {
                    return id
                }
            }
        }
        
        return null
    }
    
    /**
     * Verifica se o canal é de futebol baseado em palavras-chave
     */
    fun isFootballChannel(channelName: String): Boolean {
        val name = channelName.lowercase()

        return name.contains("premiere") ||
               name.contains("premier") ||  // ✅ Adicionado: "Premier" (sem "e" no final)
               name.contains("sportv") ||
               name.contains("band sport") ||
               name.contains("espn") ||
               name.contains("espm") ||
               name.contains("cazé") ||
               name.contains("caze") ||
               name.contains("brasileirao") ||
               name.contains("copa") ||
               name.contains("amazon") ||
               name.contains("prime")
    }
    
    /**
     * Tenta encontrar matchId padrão para canais conhecidos
     * Retorna null se não conseguir determinar
     */
    fun getDefaultMatchId(channelName: String): Long? {
        // Por enquanto, retorna null - será implementado conforme necessário
        // Pode ser expandido para mapear canais específicos para matchIds conhecidos
        return null
    }
}


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
    
    /**
     * Extrai os nomes dos times do nome do canal
     * Formato esperado: "Premiere 1 - Sao Paulo x Fluminense"
     * ou "ESPN - Flamengo x Palmeiras" ou similar
     * Retorna Pair(homeTeam, awayTeam) ou null se não conseguir extrair
     */
    fun extractTeamNames(channelName: String): Pair<String, String>? {
        val name = channelName.trim()
        
        // Primeiro, tentar encontrar o padrão "Time1 x Time2" ou "Time1 X Time2"
        // Procurar pela última ocorrência de " x " ou " X " (para evitar pegar números de canais)
        val xPattern = Regex("([^xX]+)[xX]([^xX]+)", RegexOption.IGNORE_CASE)
        val xMatch = xPattern.findAll(name).lastOrNull()
        
        if (xMatch != null) {
            var team1 = xMatch.groupValues[1].trim()
            var team2 = xMatch.groupValues[2].trim()
            
            // Remover prefixos comuns de canais e números de canais
            val prefixPattern = Regex("^(Premiere|ESPN|Sportv|Band Sport|Cazé|Caze|Amazon|Prime|\\d+)[\\s-]*", RegexOption.IGNORE_CASE)
            team1 = team1.replace(prefixPattern, "").trim()
            team2 = team2.replace(prefixPattern, "").trim()
            
            // Remover hífens e espaços extras no início
            team1 = team1.replace(Regex("^[\\s-]+"), "").trim()
            team2 = team2.replace(Regex("^[\\s-]+"), "").trim()
            
            // Verificar se os nomes não estão vazios e têm pelo menos 3 caracteres
            if (team1.length >= 3 && team2.length >= 3) {
                return Pair(team1, team2)
            }
        }
        
        // Fallback: tentar padrão "Time1 vs Time2"
        val vsPattern = Regex("([^vV]+)[vV][sS]([^vV]+)", RegexOption.IGNORE_CASE)
        val vsMatch = vsPattern.findAll(name).lastOrNull()
        
        if (vsMatch != null) {
            var team1 = vsMatch.groupValues[1].trim()
            var team2 = vsMatch.groupValues[2].trim()
            
            val prefixPattern = Regex("^(Premiere|ESPN|Sportv|Band Sport|Cazé|Caze|Amazon|Prime|\\d+)[\\s-]*", RegexOption.IGNORE_CASE)
            team1 = team1.replace(prefixPattern, "").trim()
            team2 = team2.replace(prefixPattern, "").trim()
            
            team1 = team1.replace(Regex("^[\\s-]+"), "").trim()
            team2 = team2.replace(Regex("^[\\s-]+"), "").trim()
            
            if (team1.length >= 3 && team2.length >= 3) {
                return Pair(team1, team2)
            }
        }
        
        return null
    }
}


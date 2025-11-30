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
     * Ignora variações de HD/FHD e é case-insensitive
     * 
     * @param channelName Nome do canal
     * @param epgTitle Título do programa atual do EPG (opcional) - se contiver termos de futebol, considera como canal de futebol
     */
    fun isFootballChannel(channelName: String, epgTitle: String? = null): Boolean {
        // Normalizar nome do canal: remover variações HD/FHD/etc e converter para lowercase
        val normalizedName = channelName.lowercase()
            .replace(Regex("\\s*(hd|fhd|uhd|4k|sd|full hd|high definition|plus|\\+)\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*(hd|fhd|uhd|4k|sd|full hd|high definition|plus|\\+)\\s+", RegexOption.IGNORE_CASE), " ")
            .trim()
        
        // Verificar canais específicos de futebol (case-insensitive, ignora HD/FHD)
        val footballChannels = listOf(
            "premiere", "premier",
            "sportv", "sport tv", "sport",
            "band sport", "bandsport", "band",
            "espn",
            "espm",
            "cazé", "caze", "caze tv",
            "brasileirao", "brasileirão",
            "copa",
            "amazon",
            "prime",
            "globo",  // ✅ Adicionado: Globo (será verificado com EPG)
            "record", // ✅ Adicionado: Record (será verificado com EPG)
            "sbt",    // ✅ Adicionado: SBT (será verificado com EPG)
            "rede tv", // ✅ Adicionado: Rede TV (será verificado com EPG)
            "futebol", "futbol" // ✅ Adicionado: Termos genéricos
        )
        
        // Verificar se começa com ou contém algum canal de futebol
        val isSpecificChannel = footballChannels.any { channel ->
            normalizedName.startsWith(channel) || 
            normalizedName.contains(" $channel ") || 
            normalizedName.endsWith(" $channel") ||
            normalizedName == channel ||
            normalizedName.contains(channel) && (normalizedName.contains("futebol") || normalizedName.contains("futbol") || normalizedName.contains("sport"))
        }
        
        // Se for canal aberto (Globo, Record, SBT, Rede TV), verificar se o EPG indica jogo de futebol
        val openChannels = listOf("globo", "record", "sbt", "rede tv", "redetv")
        if (openChannels.any { normalizedName.contains(it) } && epgTitle != null) {
            val epgLower = epgTitle.lowercase()
            val footballTerms = listOf(
                "futebol", "futbol", "futebol brasileiro",
                "brasileirão", "brasileirao", "brasileirão série a", "brasileirao serie a",
                "copa", "copa do brasil", "copa libertadores", "copa sul-americana",
                "campeonato", "campeonato brasileiro",
                "jogo", "partida", "match",
                "flamengo", "palmeiras", "corinthians", "são paulo", "sao paulo", "santos",
                "fluminense", "vasco", "botafogo", "atlético", "atletico", "atletico mineiro", "atletico paranaense",
                "grêmio", "gremio", "internacional", "cruzeiro", "bahia", "fortaleza",
                "athletico", "athletico paranaense", "coritiba", "goias", "cuiaba",
                "america", "america mineiro", "bragantino", "red bull bragantino"
            )
            
            val epgHasFootball = footballTerms.any { term ->
                epgLower.contains(term)
            }
            
            if (epgHasFootball) {
                return true
            }
        }
        
        // Verificar se o nome do canal contém padrão de jogo (Time1 x Time2)
        val hasGamePattern = Regex("([a-záàâãéèêíìîóòôõúùûç]+)\\s*[xX]\\s*([a-záàâãéèêíìîóòôõúùûç]+)", RegexOption.IGNORE_CASE).containsMatchIn(channelName)
        if (hasGamePattern) {
            return true
        }
        
        return isSpecificChannel
    }
    
    /**
     * Tenta encontrar matchId padrão para canais conhecidos
     * Retorna null se não conseguir determinar
     */
    fun getDefaultMatchId(@Suppress("UNUSED_PARAMETER") channelName: String): Long? {
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
        
        // Primeiro, remover prefixos de canais do início (ex: "ESPN 1 - ", "Premiere - ", "ESPN1 FHD - ")
        // Melhorado para capturar variações como "ESPN 1", "ESPN1", "ESPN1 FHD", etc.
        val cleanName = name.replace(Regex("^(Premiere|ESPN|Sportv|Band Sport|Cazé|Caze|Amazon|Prime|Globo|Record|SBT|Rede TV)\\s*\\d*\\s*(FHD|HD|UHD|4K|SD|Full HD|High Definition|Plus|\\+)?[\\s-]*", RegexOption.IGNORE_CASE), "").trim()
        
        // Se ainda tiver hífen no início, remover
        val finalCleanName = cleanName.replace(Regex("^[\\s-]+"), "").trim()
        
        // Tentar encontrar o padrão "Time1 x Time2" ou "Time1 X Time2"
        // Procurar pela última ocorrência de " x " ou " X " (para evitar pegar números de canais)
        val xPattern = Regex("([^xX]+)[xX]([^xX]+)", RegexOption.IGNORE_CASE)
        val xMatch = xPattern.findAll(finalCleanName).lastOrNull()
        
        if (xMatch != null) {
            var team1 = xMatch.groupValues[1].trim()
            var team2 = xMatch.groupValues[2].trim()
            
            // Remover prefixos comuns de canais e números de canais (caso ainda tenham)
            val prefixPattern = Regex("^(Premiere|ESPN|Sportv|Band Sport|Cazé|Caze|Amazon|Prime|Globo|Record|SBT|Rede TV|\\d+)[\\s-]*", RegexOption.IGNORE_CASE)
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


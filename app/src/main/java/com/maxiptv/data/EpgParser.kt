package com.maxiptv.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Parser para XMLTV EPG (Electronic Program Guide)
 */
object EpgParser {
    private const val EPG_URL = "http://canais.is/xmltv.php"
    // ✅ BRASIL: Timezone de Brasília (padrão para canais brasileiros)
    private val brasiliaTimeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    // ✅ CORREÇÃO: Formato com timezone (padrão XMLTV: "yyyyMMddHHmmss -0300")
    // Usar UTC para parse e depois converter para Brasília
    private val dateFormatWithTimezone = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC") // Parse em UTC para preservar horário original
    }
    // ✅ CORREÇÃO: Formato sem timezone (assume horário de Brasília)
    private val dateFormatWithoutTimezone = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).apply {
        timeZone = brasiliaTimeZone
    }
    
    /**
     * Baixa e faz parse do EPG completo
     */
    suspend fun fetchEpg(): Map<String, List<EpgProgramme>> = withContext(Dispatchers.IO) {
        try {
            Log.i("EpgParser", "📡 Baixando EPG de $EPG_URL...")
            
            // Usar conexão com User-Agent para evitar bloqueio
            val connection = URL(EPG_URL).openConnection()
            connection.setRequestProperty("User-Agent", "MaxiPTV/1.1.1 (Android)")
            connection.setRequestProperty("Accept", "application/xml, text/xml, */*")
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            
            val xml = connection.getInputStream().bufferedReader().use { it.readText() }
            Log.i("EpgParser", "✅ EPG baixado (${xml.length} bytes)")
            parseXmlTv(xml)
        } catch (e: Exception) {
            Log.e("EpgParser", "❌ Erro ao baixar EPG: ${e.message}")
            e.printStackTrace()
            emptyMap()
        }
    }
    
    /**
     * Faz parse do XML XMLTV
     */
    private fun parseXmlTv(xml: String): Map<String, List<EpgProgramme>> {
        val programmes = mutableMapOf<String, MutableList<EpgProgramme>>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            
            var eventType = parser.eventType
            var currentProgramme: EpgProgramme? = null
            var currentChannelId: String? = null
            var currentStart: Long? = null
            var currentStop: Long? = null
            var currentTitle: String? = null
            var currentSubTitle: String? = null
            var currentDesc: String? = null
            var currentRating: String? = null
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "programme" -> {
                                // Resetar dados do programa
                                currentChannelId = parser.getAttributeValue(null, "channel")
                                val startStr = parser.getAttributeValue(null, "start")
                                val stopStr = parser.getAttributeValue(null, "stop")
                                
                                currentStart = parseDate(startStr)
                                currentStop = parseDate(stopStr)
                                currentTitle = null
                                currentSubTitle = null
                                currentDesc = null
                                currentRating = null
                            }
                            "title" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    currentTitle = parser.text
                                }
                            }
                            "sub-title" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    currentSubTitle = parser.text
                                }
                            }
                            "desc" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    currentDesc = parser.text
                                }
                            }
                            "value" -> {
                                parser.next()
                                if (parser.eventType == XmlPullParser.TEXT) {
                                    currentRating = parser.text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "programme") {
                            // Criar o programa se tiver dados válidos
                            if (currentChannelId != null && 
                                currentTitle != null && 
                                currentStart != null && 
                                currentStop != null) {
                                
                                val programme = EpgProgramme(
                                    channelId = currentChannelId,
                                    title = currentTitle,
                                    subTitle = currentSubTitle,
                                    description = currentDesc,
                                    start = currentStart,
                                    stop = currentStop,
                                    rating = currentRating
                                )
                                
                                programmes.getOrPut(currentChannelId) { mutableListOf() }.add(programme)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            
            Log.i("EpgParser", "✅ EPG parseado: ${programmes.size} canais, ${programmes.values.sumOf { it.size }} programas")
        } catch (e: Exception) {
            Log.e("EpgParser", "❌ Erro ao parsear EPG: ${e.message}")
        }
        
        return programmes
    }
    
    /**
     * Converte string de data XMLTV para timestamp
     * ✅ BRASIL: Respeita o timezone que vem da API e armazena timestamp correto
     * O formato XMLTV pode vir como:
     * - "yyyyMMddHHmmss -0300" (com timezone do Brasil)
     * - "yyyyMMddHHmmss" (sem timezone - assume horário de Brasília)
     * 
     * IMPORTANTE: 
     * - Preserva o horário original que vem da API
     * - Armazena timestamp em UTC para comparações corretas
     * - Na exibição, sempre mostra em horário de Brasília
     */
    private fun parseDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            val parsed: java.util.Date?
            
            // ✅ Verificar se a string tem timezone (tem espaço seguido de + ou - e 4 dígitos)
            val hasTimezone = dateStr.matches(Regex("\\d{14}\\s+[+-]\\d{4}"))
            
            if (hasTimezone) {
                // Tem timezone: parsear respeitando o timezone que vem da API
                // O SimpleDateFormat faz parse e converte para UTC automaticamente
                parsed = dateFormatWithTimezone.parse(dateStr)
                
                // ✅ Log para debug - mostrar timezone original
                val timezoneMatch = Regex("([+-]\\d{4})").find(dateStr)
                val originalTimezone = timezoneMatch?.value ?: "N/A"
                Log.d("EpgParser", "📅 Data da API com timezone '$originalTimezone': '$dateStr'")
            } else {
                // Sem timezone: assumir horário de Brasília (padrão para canais brasileiros)
                parsed = dateFormatWithoutTimezone.parse(dateStr)
                Log.d("EpgParser", "📅 Data sem timezone (assumindo Brasília): '$dateStr'")
            }
            
            if (parsed != null) {
                // Timestamp em UTC (correto para comparações)
                val timestamp = parsed.time
                
                // ✅ LOG para debug: mostrar como será exibido em Brasília
                val brasiliaFormatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = brasiliaTimeZone
                }
                val brasiliaTime = brasiliaFormatter.format(java.util.Date(timestamp))
                Log.d("EpgParser", "   → Exibido em Brasília: $brasiliaTime")
                
                timestamp
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("EpgParser", "⚠️ Erro ao parsear data: $dateStr - ${e.message}")
            null
        }
    }
    
    /**
     * Normaliza o nome do canal para melhor matching
     */
    private fun normalizeChannelName(name: String): String {
        return name.lowercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
            .replace("hd", "")
            .replace("fhd", "")
            .replace("4k", "")
            .replace("uhd", "")
            .replace("tv", "")
            .replace("canal", "")
            .replace("são", "sao")
            .replace("paulo", "sp")
            .replace("rio", "rj")
            .replace("janeiro", "rj")
            .replace("belo", "bh")
            .replace("horizonte", "bh")
            .replace("minas", "mg")
            .replace("gerais", "mg")
    }
    
    /**
     * Busca o programa atual para um canal
     */
    fun getCurrentProgramme(channelId: String, epgData: Map<String, List<EpgProgramme>>): EpgProgramme? {
        Log.i("EpgParser", "🔍 Buscando programa atual para: '$channelId'")
        Log.i("EpgParser", "📡 EPG tem ${epgData.size} canais disponíveis: ${epgData.keys.take(5)}")
        
        // Tentar busca exata primeiro
        epgData[channelId]?.firstOrNull { it.isCurrentlyAiring() }?.let { 
            Log.i("EpgParser", "✅ Encontrado programa exato: ${it.title}")
            return it 
        }
        
        // Se não encontrar, tentar busca flexível
        val normalizedChannelId = normalizeChannelName(channelId)
        
        Log.i("EpgParser", "🔍 Tentando busca flexível com: '$normalizedChannelId'")
        
        // Tentar mapeamentos específicos para canais conhecidos
        // IMPORTANTE: EPG tem "GLOBO SAO PAULO" e "RECORD Sao Paulo" - vamos mapear TODOS os canais regionais para esses
        val channelMappings = mapOf(
            "globo" to listOf("globosaopaulo", "globo", "redeglobo", "tvglobo", "globosp", "globorj", "globobh", "globors"),
            "record" to listOf("recordsaopaulo", "record", "recordtv", "tvrecord", "recordsp", "recordrj", "recordbh"),
            "sbt" to listOf("sbt", "tvsbt", "sbthd", "sbtsp", "sbtrj"),
            "band" to listOf("bandhd", "band", "bandeirantes", "tvband", "bandsp", "bandrj"),
            "rede" to listOf("rede", "redetv", "redetvhd", "redesp", "rederj"),
            "cultura" to listOf("cultura", "tvcultura", "culturapaulista", "culturap"),
            "futura" to listOf("futura", "tvfutura", "futurahd"),
            "gazeta" to listOf("gazeta", "tvgazeta", "gazetahd", "gazetasp"),
            "cnn" to listOf("cnn", "cnnbrasil", "cnnbr"),
            "globo news" to listOf("globonews", "globonews", "gnews"),
            "sportv" to listOf("sportv", "sportvhd", "sportv1", "sportv2"),
            "multishow" to listOf("multishow", "multishowhd"),
            "gnt" to listOf("gnt", "gnthd"),
            "viva" to listOf("viva", "vivahd"),
            "canal brasil" to listOf("canalbrasil", "canalbrasilhd")
        )
        
        // Verificar mapeamentos específicos
        for ((key, variations) in channelMappings) {
            if (normalizedChannelId.contains(key)) {
                Log.i("EpgParser", "🎯 Tentando mapeamento específico para '$key'")
                for (variation in variations) {
                    for ((epgChannelId, programmes) in epgData) {
                        val normalizedEpgId = epgChannelId.lowercase()
                            .replace(" ", "")
                            .replace("-", "")
                            .replace("_", "")
                            .replace("hd", "")
                            .replace("fhd", "")
                            .replace("4k", "")
                            .replace("uhd", "")
                            .replace("tv", "")
                            .replace("canal", "")
                            .replace("são", "sao")
                            .replace("paulo", "sp")
                            .replace("rio", "rj")
                            .replace("janeiro", "rj")
                            .replace("belo", "bh")
                            .replace("horizonte", "bh")
                            .replace("minas", "mg")
                            .replace("gerais", "mg")
                        
                        if (normalizedEpgId.contains(variation) || variation.contains(normalizedEpgId)) {
                            Log.i("EpgParser", "🎯 Match específico encontrado: '$epgChannelId' -> '$variation'")
                            programmes.firstOrNull { it.isCurrentlyAiring() }?.let { 
                                Log.i("EpgParser", "✅ Programa encontrado: ${it.title}")
                                return it 
                            }
                        }
                    }
                }
            }
        }
        
        // Busca flexível geral
        for ((epgChannelId, programmes) in epgData) {
            val normalizedEpgId = epgChannelId.lowercase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace("hd", "")
                .replace("fhd", "")
                .replace("4k", "")
                .replace("uhd", "")
                .replace("tv", "")
                .replace("canal", "")
                .replace("são", "sao")
                .replace("paulo", "sp")
                .replace("rio", "rj")
                .replace("janeiro", "rj")
                .replace("belo", "bh")
                .replace("horizonte", "bh")
                .replace("minas", "mg")
                .replace("gerais", "mg")
            
            if (normalizedEpgId.contains(normalizedChannelId) || normalizedChannelId.contains(normalizedEpgId)) {
                Log.i("EpgParser", "🎯 Match flexível encontrado: '$epgChannelId' -> '$normalizedEpgId'")
                programmes.firstOrNull { it.isCurrentlyAiring() }?.let { 
                    Log.i("EpgParser", "✅ Programa encontrado: ${it.title}")
                    return it 
                }
            }
        }
        
        Log.w("EpgParser", "❌ Nenhum programa encontrado para: '$channelId'")
        Log.w("EpgParser", "📋 Canais EPG disponíveis: ${epgData.keys.joinToString(", ")}")
        return null
    }
    
    /**
     * Busca o próximo programa para um canal
     */
    fun getNextProgramme(channelId: String, epgData: Map<String, List<EpgProgramme>>): EpgProgramme? {
        val now = System.currentTimeMillis()
        
        Log.i("EpgParser", "🔍 Buscando próximo programa para: '$channelId'")
        
        // Tentar busca exata primeiro
        epgData[channelId]?.firstOrNull { it.start > now }?.let { 
            Log.i("EpgParser", "✅ Próximo programa encontrado: ${it.title}")
            return it 
        }
        
        // Se não encontrar, tentar busca flexível
        val normalizedChannelId = channelId.lowercase()
            .replace(" ", "")
            .replace("-", "")
            .replace("_", "")
            .replace("hd", "")
            .replace("fhd", "")
            .replace("4k", "")
            .replace("uhd", "")
            .replace("tv", "")
            .replace("canal", "")
            .replace("são", "sao")
            .replace("paulo", "sp")
            .replace("rio", "rj")
            .replace("janeiro", "rj")
            .replace("belo", "bh")
            .replace("horizonte", "bh")
            .replace("minas", "mg")
            .replace("gerais", "mg")
        
        // Tentar mapeamentos específicos para canais conhecidos
        // IMPORTANTE: EPG tem "GLOBO SAO PAULO" e "RECORD Sao Paulo" - vamos mapear TODOS os canais regionais para esses
        val channelMappings = mapOf(
            "globo" to listOf("globosaopaulo", "globo", "redeglobo", "tvglobo", "globosp", "globorj", "globobh", "globors"),
            "record" to listOf("recordsaopaulo", "record", "recordtv", "tvrecord", "recordsp", "recordrj", "recordbh"),
            "sbt" to listOf("sbt", "tvsbt", "sbthd", "sbtsp", "sbtrj"),
            "band" to listOf("bandhd", "band", "bandeirantes", "tvband", "bandsp", "bandrj"),
            "rede" to listOf("rede", "redetv", "redetvhd", "redesp", "rederj"),
            "cultura" to listOf("cultura", "tvcultura", "culturapaulista", "culturap"),
            "futura" to listOf("futura", "tvfutura", "futurahd"),
            "gazeta" to listOf("gazeta", "tvgazeta", "gazetahd", "gazetasp"),
            "cnn" to listOf("cnn", "cnnbrasil", "cnnbr"),
            "globo news" to listOf("globonews", "globonews", "gnews"),
            "sportv" to listOf("sportv", "sportvhd", "sportv1", "sportv2"),
            "multishow" to listOf("multishow", "multishowhd"),
            "gnt" to listOf("gnt", "gnthd"),
            "viva" to listOf("viva", "vivahd"),
            "canal brasil" to listOf("canalbrasil", "canalbrasilhd")
        )
        
        // Verificar mapeamentos específicos
        for ((key, variations) in channelMappings) {
            if (normalizedChannelId.contains(key)) {
                Log.i("EpgParser", "🎯 Tentando mapeamento específico para '$key'")
                for (variation in variations) {
                    for ((epgChannelId, programmes) in epgData) {
                        val normalizedEpgId = epgChannelId.lowercase()
                            .replace(" ", "")
                            .replace("-", "")
                            .replace("_", "")
                            .replace("hd", "")
                            .replace("fhd", "")
                            .replace("4k", "")
                            .replace("uhd", "")
                            .replace("tv", "")
                            .replace("canal", "")
                            .replace("são", "sao")
                            .replace("paulo", "sp")
                            .replace("rio", "rj")
                            .replace("janeiro", "rj")
                            .replace("belo", "bh")
                            .replace("horizonte", "bh")
                            .replace("minas", "mg")
                            .replace("gerais", "mg")
                        
                        if (normalizedEpgId.contains(variation) || variation.contains(normalizedEpgId)) {
                            Log.i("EpgParser", "🎯 Match específico encontrado: '$epgChannelId' -> '$variation'")
                            programmes.firstOrNull { it.start > now }?.let { 
                                Log.i("EpgParser", "✅ Próximo programa encontrado: ${it.title}")
                                return it 
                            }
                        }
                    }
                }
            }
        }
        
        // Busca flexível geral
        for ((epgChannelId, programmes) in epgData) {
            val normalizedEpgId = epgChannelId.lowercase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace("hd", "")
                .replace("fhd", "")
                .replace("4k", "")
                .replace("uhd", "")
                .replace("tv", "")
                .replace("canal", "")
                .replace("são", "sao")
                .replace("paulo", "sp")
                .replace("rio", "rj")
                .replace("janeiro", "rj")
                .replace("belo", "bh")
                .replace("horizonte", "bh")
                .replace("minas", "mg")
                .replace("gerais", "mg")
            
            if (normalizedEpgId.contains(normalizedChannelId) || normalizedChannelId.contains(normalizedEpgId)) {
                Log.i("EpgParser", "🎯 Match flexível encontrado: '$epgChannelId' -> '$normalizedEpgId'")
                programmes.firstOrNull { it.start > now }?.let { 
                    Log.i("EpgParser", "✅ Próximo programa encontrado: ${it.title}")
                    return it 
                }
            }
        }
        
        Log.w("EpgParser", "❌ Nenhum próximo programa encontrado para: '$channelId'")
        return null
    }
}


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

/**
 * Parser para XMLTV EPG (Electronic Program Guide)
 */
object EpgParser {
    private const val EPG_URL = "http://canais.is/xmltv.php"
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.getDefault())
    
    /**
     * Baixa e faz parse do EPG completo
     */
    suspend fun fetchEpg(): Map<String, List<EpgProgramme>> = withContext(Dispatchers.IO) {
        try {
            Log.i("EpgParser", "📡 Baixando EPG de $EPG_URL...")
            val xml = URL(EPG_URL).readText()
            Log.i("EpgParser", "✅ EPG baixado (${xml.length} bytes)")
            parseXmlTv(xml)
        } catch (e: Exception) {
            Log.e("EpgParser", "❌ Erro ao baixar EPG: ${e.message}")
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
     */
    private fun parseDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            dateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            Log.w("EpgParser", "⚠️ Erro ao parsear data: $dateStr")
            null
        }
    }
    
    /**
     * Busca o programa atual para um canal
     */
    fun getCurrentProgramme(channelId: String, epgData: Map<String, List<EpgProgramme>>): EpgProgramme? {
        return epgData[channelId]?.firstOrNull { it.isCurrentlyAiring() }
    }
    
    /**
     * Busca o próximo programa para um canal
     */
    fun getNextProgramme(channelId: String, epgData: Map<String, List<EpgProgramme>>): EpgProgramme? {
        val now = System.currentTimeMillis()
        return epgData[channelId]?.firstOrNull { it.start > now }
    }
}


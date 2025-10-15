package com.maxiptv.util

/**
 * Enumeração de idiomas/legendas detectados
 */
enum class Lang {
    DUBLADO,
    LEGENDADO,
    ORIGINAL,
    DESCONHECIDO
}

// Regex para detectar DUBLADO
private val tagLangDub = Regex("(?i)\\b(DUBL(?:ADO)?|DUB|NACIONAL|PT\\s?-?BR|PTBR)\\b")

// Regex para detectar LEGENDADO
private val tagLangLeg = Regex("(?i)\\b(LEG(?:ENDADO)?|LEG|SUB|SUBBED)\\b")

// Regex para detectar ORIGINAL
private val tagLangOri = Regex("(?i)\\b(ORIGINAL|ING|EN|LATAM)\\b")

// Regex para remover tags de qualidade e outros "lixos"
private val junkRegex = Regex("(?i)(\\[.*?\\]|\\(.*?\\))")

// Regex para remover todas as tags de idioma
private val langTagAll = Regex("(?i)\\b(DUBL(?:ADO)?|DUB|NACIONAL|PT\\s?-?BR|PTBR|LEG(?:ENDADO)?|LEG|SUB|SUBBED|ORIGINAL|ING|EN|LATAM)\\b")

// Regex para remover indicadores de temporadas (ex: "Temporadas 3-5", "T1-T4", "Season 1-3")
private val seasonRangeRegex = Regex("(?i)(temporadas?\\s*\\d+-\\d+|temporadas?\\s*\\d+\\s*a\\s*\\d+|t\\d+-t?\\d+|season\\s*\\d+-\\d+|-\\s*temporadas?\\s*\\d+)", RegexOption.IGNORE_CASE)

/**
 * Detecta o idioma/legenda de um título
 */
fun detectLang(name: String): Lang = when {
    tagLangDub.containsMatchIn(name) -> Lang.DUBLADO
    tagLangLeg.containsMatchIn(name) -> Lang.LEGENDADO
    tagLangOri.containsMatchIn(name) -> Lang.ORIGINAL
    else -> Lang.DESCONHECIDO
}

/**
 * Normaliza um título removendo tags de idioma, qualidade, indicadores de temporadas, etc.
 * Retorna o título "base" para agrupamento
 */
fun normalizeTitle(raw: String): String =
    raw.replace(junkRegex, " ")            // Remove [tags] e (parênteses)
        .replace(langTagAll, " ")          // Remove idiomas
        .replace(seasonRangeRegex, " ")    // Remove "Temporadas 3-5", "T1-T4", etc
        .replace(Regex("\\s*-\\s*$"), "")  // Remove traço no final
        .replace(Regex("\\s+"), " ")       // Normaliza espaços
        .trim()

/**
 * Retorna a prioridade de um idioma (menor = maior prioridade)
 * DUBLADO tem prioridade máxima (0)
 */
fun langPriority(l: Lang): Int = when (l) {
    Lang.DUBLADO -> 0
    Lang.DESCONHECIDO -> 1    // Sem tag = boa prioridade (geralmente dublado)
    Lang.ORIGINAL -> 2
    Lang.LEGENDADO -> 3       // Menor prioridade
}

/**
 * Converte Lang para string amigável
 */
fun langToString(l: Lang): String = when (l) {
    Lang.DUBLADO -> "Dublado"
    Lang.LEGENDADO -> "Legendado"
    Lang.ORIGINAL -> "Original"
    Lang.DESCONHECIDO -> "Padrão"
}


package com.sdk.voiceai.stt

object LanguageDetector {

    val SUPPORTED_LANGUAGES = listOf("en", "es", "fr", "de", "it", "pt", "nl", "ru", "zh", "ja")

    private data class LangProfile(val lang: String, val commonWords: Set<String>)

    private val profiles = listOf(
        LangProfile("en", setOf("the", "and", "is", "in", "it", "of", "to", "a")),
        LangProfile("es", setOf("el", "la", "de", "que", "y", "en", "los", "un")),
        LangProfile("fr", setOf("le", "la", "les", "de", "et", "un", "une", "est")),
        LangProfile("de", setOf("der", "die", "das", "und", "ist", "in", "ein", "zu")),
        LangProfile("it", setOf("il", "la", "di", "che", "e", "un", "per", "non")),
        LangProfile("pt", setOf("o", "a", "de", "que", "e", "do", "da", "em")),
        LangProfile("nl", setOf("de", "het", "een", "van", "en", "in", "is", "dat")),
        LangProfile("ru", setOf("в", "и", "не", "на", "я", "что", "с", "как")),
        LangProfile("zh", setOf("的", "了", "是", "在", "我", "有", "和", "人")),
        LangProfile("ja", setOf("の", "に", "は", "を", "た", "が", "で", "て")),
    )

    fun detect(text: String): String? {
        val tokens = text.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null
        val best = profiles.maxByOrNull { profile -> tokens.count { it in profile.commonWords } }
        val score = tokens.count { it in (best?.commonWords ?: emptySet()) }
        return if (score > 0) best?.lang else null
    }
}

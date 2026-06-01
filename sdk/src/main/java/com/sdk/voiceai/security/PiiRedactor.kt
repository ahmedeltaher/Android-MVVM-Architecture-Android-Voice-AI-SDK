package com.sdk.voiceai.security

object PiiRedactor {

    private val PHONE_PATTERN = Regex(
        """(\+?1[\s.\-]?)?(\(?\d{3}\)?[\s.\-]?)?\d{3}[\s.\-]?\d{4}"""
    )

    private val EMAIL_PATTERN = Regex(
        """[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}"""
    )

    private val CREDIT_CARD_PATTERN = Regex(
        """\b(?:\d{4}[\s\-]?){3}\d{4}\b"""
    )

    private val BUILT_IN_PATTERNS: List<Regex> = listOf(
        CREDIT_CARD_PATTERN,
        EMAIL_PATTERN,
        PHONE_PATTERN
    )

    private const val REDACTION_PLACEHOLDER = "[REDACTED]"

    fun redact(text: String, customPatterns: List<Regex> = emptyList()): String {
        val allPatterns = BUILT_IN_PATTERNS + customPatterns
        var result = text
        for (pattern in allPatterns) {
            result = pattern.replace(result, REDACTION_PLACEHOLDER)
        }
        return result
    }
}

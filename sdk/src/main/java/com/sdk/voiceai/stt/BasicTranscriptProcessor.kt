package com.sdk.voiceai.stt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class BasicTranscriptProcessor {
    fun process(text: String): String {
        if (text.isBlank()) return text
        val trimmed = text.trim()
        val capitalized = trimmed.replaceFirstChar { it.uppercase() }
        return if (capitalized.last() in setOf('.', '?', '!', ',', ';', ':')) {
            capitalized
        } else {
            "$capitalized."
        }
    }
}

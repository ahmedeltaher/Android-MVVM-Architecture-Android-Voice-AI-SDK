package com.sdk.voiceai.stt

import kotlinx.coroutines.flow.Flow
import java.util.Locale

data class SttConfig(
    val locale: Locale = Locale.getDefault(),
    val partialResults: Boolean = true,
)

data class TranscriptResult(
    val text: String,
    val confidence: Float?,
    val isFinal: Boolean,
    val language: String? = null,
)

interface SpeechToTextEngine {
    suspend fun transcribe(audioBytes: ByteArray, config: SttConfig): TranscriptResult
    fun transcribeStream(config: SttConfig): Flow<TranscriptResult>
    fun cancel()
}

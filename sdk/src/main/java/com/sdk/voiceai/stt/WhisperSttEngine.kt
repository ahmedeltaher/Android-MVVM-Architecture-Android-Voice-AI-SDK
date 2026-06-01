package com.sdk.voiceai.stt

import com.sdk.voiceai.audio.AudioConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Serializable
private data class WhisperResponse(val text: String)

class WhisperSttEngine(
    private val apiKey: String,
    private val httpClient: OkHttpClient,
    private val model: String = "whisper-1",
    private val customVocabulary: List<String> = emptyList(),
) : SpeechToTextEngine {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun transcribe(audioBytes: ByteArray, config: SttConfig): TranscriptResult =
        withContext(Dispatchers.IO) {
            val wavBytes = AudioConverter.pcmToWav(audioBytes)
            doTranscribe(wavBytes, config, retryOnServerError = true)
        }

    private fun doTranscribe(
        wavBytes: ByteArray,
        config: SttConfig,
        retryOnServerError: Boolean,
    ): TranscriptResult {
        val tempFile = File.createTempFile("whisper_audio_", ".wav")
        try {
            FileOutputStream(tempFile).use { it.write(wavBytes) }

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    tempFile.name,
                    tempFile.asRequestBody("audio/wav".toMediaType()),
                )
                .addFormDataPart("model", model)

            val language = config.locale.language
            if (language.isNotEmpty()) {
                multipartBuilder.addFormDataPart("language", language)
            }
            if (customVocabulary.isNotEmpty()) {
                multipartBuilder.addFormDataPart("prompt", customVocabulary.joinToString(", "))
            }

            val request = Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions")
                .header("Authorization", "Bearer $apiKey")
                .post(multipartBuilder.build())
                .build()

            Timber.d("WhisperSttEngine: sending request (model=$model)")

            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string() ?: ""
                when {
                    response.isSuccessful -> {
                        val whisperResponse = json.decodeFromString<WhisperResponse>(bodyString)
                        TranscriptResult(text = whisperResponse.text, confidence = null, isFinal = true)
                    }
                    code in 500..599 -> {
                        Timber.w("WhisperSttEngine: server error $code, retry=$retryOnServerError")
                        if (retryOnServerError) doTranscribe(wavBytes, config, retryOnServerError = false)
                        else throw IOException("Whisper API server error: $code — $bodyString")
                    }
                    else -> throw IOException("Whisper API error: $code — $bodyString")
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    override fun transcribeStream(config: SttConfig): Flow<TranscriptResult> = flow {
        throw UnsupportedOperationException("WhisperSttEngine requires audio bytes; Whisper has no live-stream mode.")
    }

    override fun cancel() { /* one-shot HTTP, nothing to cancel */ }
}

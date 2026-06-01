package com.sdk.voiceai.tts

import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ElevenLabsVoice(val voiceId: String, val name: String, val previewUrl: String?)

@Serializable
private data class TtsRequest(val text: String, val model_id: String)

@Serializable
private data class ElevenLabsVoiceDto(
    val voice_id: String,
    val name: String,
    val preview_url: String? = null,
)

@Serializable
private data class ElevenLabsVoicesResponse(val voices: List<ElevenLabsVoiceDto>)

class ElevenLabsTtsEngine(
    private val apiKey: String,
    private val voiceId: String = "21m00Tcm4TlvDq8ikWAM",
    private val httpClient: OkHttpClient,
    private val modelId: String = "eleven_monolingual_v1",
) : TextToSpeechEngine {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var mediaPlayer: MediaPlayer? = null

    private var cachedVoices: List<ElevenLabsVoice>? = null
    private var cacheTimestamp: Long = 0L

    suspend fun fetchVoices(): List<ElevenLabsVoice> {
        val now = System.currentTimeMillis()
        val cached = cachedVoices
        if (cached != null && (now - cacheTimestamp) < 86_400_000L) {
            Timber.d("ElevenLabsTtsEngine: returning cached voices (${cached.size})")
            return cached
        }

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/voices")
                .header("xi-api-key", apiKey)
                .get()
                .build()

            Timber.d("ElevenLabsTtsEngine: fetching voices list")
            val responseBody = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    throw IllegalStateException("ElevenLabs voices error ${response.code}: $err")
                }
                response.body?.string() ?: throw IllegalStateException("ElevenLabs empty response body")
            }

            val parsed = json.decodeFromString(ElevenLabsVoicesResponse.serializer(), responseBody)
            val voices = parsed.voices.map { dto ->
                ElevenLabsVoice(
                    voiceId = dto.voice_id,
                    name = dto.name,
                    previewUrl = dto.preview_url,
                )
            }
            cachedVoices = voices
            cacheTimestamp = System.currentTimeMillis()
            Timber.d("ElevenLabsTtsEngine: fetched ${voices.size} voices")
            voices
        }
    }

    override suspend fun speak(text: String, config: TtsConfig) {
        val mp3Bytes = withContext(Dispatchers.IO) {
            val body = json.encodeToString(TtsRequest.serializer(), TtsRequest(text = text, model_id = modelId))
            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                .header("xi-api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "audio/mpeg")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            Timber.d("ElevenLabsTtsEngine: requesting speech (voiceId=$voiceId)")
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    throw IllegalStateException("ElevenLabs TTS error ${response.code}: $err")
                }
                response.body?.bytes() ?: throw IllegalStateException("ElevenLabs empty response body")
            }
        }

        withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile("elevenlabs_tts_", ".mp3")
            try {
                tempFile.writeBytes(mp3Bytes)
                suspendCancellableCoroutine { cont ->
                    val mp = MediaPlayer()
                    mediaPlayer = mp
                    try {
                        mp.setDataSource(tempFile.absolutePath)
                        mp.prepare()
                        mp.setOnCompletionListener {
                            Timber.d("ElevenLabsTtsEngine: playback complete")
                            cont.resume(Unit)
                        }
                        mp.setOnErrorListener { _, what, extra ->
                            cont.resumeWithException(IllegalStateException("MediaPlayer error: what=$what, extra=$extra"))
                            true
                        }
                        mp.start()
                    } catch (e: Exception) {
                        mp.release()
                        mediaPlayer = null
                        cont.resumeWithException(e)
                    }
                    cont.invokeOnCancellation {
                        mp.stop()
                        mp.release()
                        mediaPlayer = null
                    }
                }
            } finally {
                tempFile.delete()
                mediaPlayer?.release()
                mediaPlayer = null
            }
        }
    }

    override fun stop() {
        mediaPlayer?.takeIf { it.isPlaying }?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun shutdown() = stop()
}

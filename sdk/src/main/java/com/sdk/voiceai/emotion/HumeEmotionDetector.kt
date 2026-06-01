package com.sdk.voiceai.emotion

import com.sdk.voiceai.audio.AudioConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Serializable
private data class HumePrediction(val emotions: List<HumeEmotion>)

@Serializable
private data class HumeEmotion(val name: String, val score: Float)

private val HUME_TO_SDK = mapOf(
    "Joy" to Emotion.HAPPY, "Excitement" to Emotion.HAPPY, "Amusement" to Emotion.HAPPY,
    "Sadness" to Emotion.SAD, "Disappointment" to Emotion.SAD,
    "Anger" to Emotion.ANGRY, "Frustration" to Emotion.ANGRY,
    "Fear" to Emotion.FEARFUL, "Anxiety" to Emotion.FEARFUL,
    "Surprise" to Emotion.SURPRISED, "Awe" to Emotion.SURPRISED,
    "Disgust" to Emotion.DISGUSTED, "Contempt" to Emotion.DISGUSTED,
)

class HumeEmotionDetector(
    private val apiKey: String,
    private val httpClient: OkHttpClient,
) : VoiceEmotionDetector {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun detect(audio: ByteArray, config: EmotionConfig): EmotionResult =
        withContext(Dispatchers.IO) {
            val wavBytes = AudioConverter.pcmToWav(audio)
            val tempFile = File.createTempFile("hume_audio_", ".wav")
            try {
                FileOutputStream(tempFile).use { it.write(wavBytes) }
                doDetect(tempFile, config, retry = true)
            } finally {
                tempFile.delete()
            }
        }

    private fun doDetect(file: File, config: EmotionConfig, retry: Boolean): EmotionResult {
        val request = Request.Builder()
            .url("https://api.hume.ai/v0/batch/jobs")
            .header("X-Hume-Api-Key", apiKey)
            .post(
                MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name,
                        file.readBytes().toRequestBody("audio/wav".toMediaType()))
                    .build()
            )
            .build()

        Timber.d("HumeEmotionDetector: sending request")
        httpClient.newCall(request).execute().use { response ->
            val code = response.code
            val body = response.body?.string() ?: ""
            return when {
                response.isSuccessful -> {
                    val predictions = json.decodeFromString<List<HumePrediction>>(body)
                    buildResult(predictions, config)
                }
                code in 500..599 && retry -> {
                    Timber.w("HumeEmotionDetector: server error $code, retrying")
                    doDetect(file, config, retry = false)
                }
                else -> throw IOException("Hume API error $code: $body")
            }
        }
    }

    private fun buildResult(predictions: List<HumePrediction>, config: EmotionConfig): EmotionResult {
        val emotions = predictions.flatMap { it.emotions }
        val sdkScores = mutableMapOf<Emotion, Float>()
        emotions.forEach { humeEmotion ->
            val sdkEmotion = HUME_TO_SDK[humeEmotion.name] ?: Emotion.NEUTRAL
            sdkScores[sdkEmotion] = (sdkScores[sdkEmotion] ?: 0f) + humeEmotion.score
        }
        val filtered = sdkScores.filter { config.targetEmotions.contains(it.key) }
        val dominant = filtered.maxByOrNull { it.value }?.key ?: Emotion.NEUTRAL
        val confidence = filtered[dominant] ?: 0.3f
        return EmotionResult(dominant, filtered.ifEmpty { mapOf(Emotion.NEUTRAL to 1f) }, confidence)
    }
}

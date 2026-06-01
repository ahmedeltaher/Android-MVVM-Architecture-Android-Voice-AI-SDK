package com.sdk.voiceai

import android.content.Context
import com.sdk.voiceai.ai.AIEngine
import com.sdk.voiceai.ai.ClaudeAIEngine
import com.sdk.voiceai.emotion.VoiceEmotionDetector
import com.sdk.voiceai.model.VoiceAIConfig
import com.sdk.voiceai.model.VoiceAIError
import com.sdk.voiceai.stt.SpeechToTextEngine
import com.sdk.voiceai.stt.AndroidSttEngine
import com.sdk.voiceai.tts.AndroidTtsEngine
import com.sdk.voiceai.tts.TextToSpeechEngine
import okhttp3.CertificatePinner
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TooManySessionsException(message: String) : RuntimeException(message)

class VoiceAISDK private constructor(
    private val context: Context,
    private val config: VoiceAIConfig,
    private val httpClient: OkHttpClient,
) {
    private val activeSessions = AtomicInteger(0)

    fun createSession(): VoiceAISession {
        if (activeSessions.get() >= MAX_SESSIONS) {
            throw TooManySessionsException("Maximum $MAX_SESSIONS concurrent sessions reached")
        }
        activeSessions.incrementAndGet()
        val stt = (config.sttEngine as? SpeechToTextEngine) ?: AndroidSttEngine(context)
        val tts = (config.ttsEngine as? TextToSpeechEngine) ?: AndroidTtsEngine(context)
        val ai  = (config.aiEngine  as? AIEngine)           ?: ClaudeAIEngine(
            apiKey     = config.anthropicApiKey,
            httpClient = httpClient,
            baseUrl    = config.apiBaseUrl,
        )
        val emo = config.emotionDetector as? VoiceEmotionDetector
        return VoiceAISession(context, config, stt, ai, tts, emo) {
            activeSessions.decrementAndGet()
        }
    }

    companion object {
        private const val MAX_SESSIONS = 4
        fun deleteAllLocalData(context: Context) {
            context.getSharedPreferences("voice_ai_keys", Context.MODE_PRIVATE).edit().clear().apply()
            Timber.d("All local VoiceAI data deleted")
        }

        fun openPermissionSettings(context: Context) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", context.packageName, null)
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    class Builder(private val context: Context) {
        private var anthropicApiKey: String = ""
        private var configBlock: (VoiceAIConfig.() -> VoiceAIConfig)? = null
        private var debugLogging: Boolean = false

        fun anthropicApiKey(key: String) = apply { anthropicApiKey = key }
        fun debugLogging(enabled: Boolean) = apply { debugLogging = enabled }
        fun config(block: VoiceAIConfig.() -> VoiceAIConfig) = apply { configBlock = block }

        fun build(): VoiceAISDK {
            require(anthropicApiKey.isNotBlank()) {
                VoiceAIError.InvalidConfig("anthropicApiKey must not be blank").message
            }

            val baseConfig = VoiceAIConfig(
                anthropicApiKey = anthropicApiKey,
                debugLogging = debugLogging,
            )
            val finalConfig = configBlock?.invoke(baseConfig) ?: baseConfig

            if (finalConfig.emotionDetectionEnabled &&
                finalConfig.emotionConsentRationale.isBlank()) {
                Timber.w("emotionDetectionEnabled=true but emotionConsentRationale is blank. " +
                         "Set a rationale for GDPR/CCPA compliance.")
            }

            val pinnerBuilder = CertificatePinner.Builder()
            finalConfig.certificatePins.forEach { pin ->
                pinnerBuilder.add("api.anthropic.com", pin)
            }

            val httpClient = OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = if (debugLogging) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
                    }
                )
                .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
                .certificatePinner(pinnerBuilder.build())
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return VoiceAISDK(context.applicationContext, finalConfig, httpClient)
        }
    }
}

package com.sdk.voiceai

import android.content.Context
import com.sdk.voiceai.model.VoiceAIConfig

@DslMarker
annotation class VoiceAIDslMarker

@VoiceAIDslMarker
class VoiceAIBuilder(private val context: Context) {
    var anthropicApiKey: String = ""
    var debugLogging: Boolean = false
    private var configMutator: (VoiceAIConfig.() -> VoiceAIConfig)? = null

    fun config(block: VoiceAIConfig.() -> VoiceAIConfig) {
        configMutator = block
    }

    fun build(): VoiceAISDK = VoiceAISDK.Builder(context)
        .anthropicApiKey(anthropicApiKey)
        .debugLogging(debugLogging)
        .apply { configMutator?.let { config(it) } }
        .build()
}

/**
 * Top-level DSL function for building a [VoiceAISDK] instance.
 *
 * ```kotlin
 * val sdk = voiceAISDK(context) {
 *     anthropicApiKey = BuildConfig.ANTHROPIC_API_KEY
 *     debugLogging = BuildConfig.DEBUG
 *     config { copy(systemPrompt = "You are a concise voice assistant.") }
 * }
 * ```
 */
fun voiceAISDK(context: Context, block: VoiceAIBuilder.() -> Unit): VoiceAISDK =
    VoiceAIBuilder(context).apply(block).build()

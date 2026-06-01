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

fun voiceAISDK(context: Context, block: VoiceAIBuilder.() -> Unit): VoiceAISDK =
    VoiceAIBuilder(context).apply(block).build()

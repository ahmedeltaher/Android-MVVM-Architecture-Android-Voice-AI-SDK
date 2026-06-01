package com.sdk.voiceai.di

import android.content.Context
import com.sdk.voiceai.VoiceAISDK
import com.sdk.voiceai.model.VoiceAIConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceAIModule {

    @Provides
    @Singleton
    fun provideVoiceAIConfig(): VoiceAIConfig {
        // Consumers override this binding in their own module to inject an API key.
        return VoiceAIConfig(anthropicApiKey = "")
    }

    @Provides
    @Singleton
    fun provideVoiceAISDK(
        @ApplicationContext context: Context,
        config: VoiceAIConfig,
    ): VoiceAISDK = VoiceAISDK.Builder(context)
        .anthropicApiKey(config.anthropicApiKey)
        .build()
}

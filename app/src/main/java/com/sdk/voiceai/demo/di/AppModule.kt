package com.sdk.voiceai.demo.di

import android.content.Context
import com.sdk.voiceai.VoiceAISDK
import com.sdk.voiceai.model.VoiceAIConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.sdk.voiceai.demo.BuildConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideVoiceAISDK(@ApplicationContext context: Context): VoiceAISDK =
        VoiceAISDK.Builder(context)
            .anthropicApiKey(BuildConfig.ANTHROPIC_API_KEY)
            .debugLogging(BuildConfig.DEBUG)
            .build()
}

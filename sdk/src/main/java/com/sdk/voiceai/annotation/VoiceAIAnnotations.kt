package com.sdk.voiceai.annotation

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class VoiceAIStable

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@RequiresOptIn(
    message = "This Voice AI API is experimental and may change without notice.",
    level = RequiresOptIn.Level.WARNING,
)
annotation class VoiceAIExperimental

package com.sdk.voiceai.model

sealed interface VoiceAIError {
    val message: String
    val cause: Throwable?

    data class PermissionDenied(
        override val message: String = "RECORD_AUDIO permission not granted",
        override val cause: Throwable? = null,
    ) : VoiceAIError

    data class NoNetwork(
        override val message: String = "No network connection available",
        override val cause: Throwable? = null,
    ) : VoiceAIError

    data class SttFailed(
        override val message: String,
        override val cause: Throwable? = null,
    ) : VoiceAIError

    data class AiFailed(
        override val message: String,
        override val cause: Throwable? = null,
    ) : VoiceAIError

    data class TtsFailed(
        override val message: String,
        override val cause: Throwable? = null,
    ) : VoiceAIError

    data class RateLimited(
        override val message: String = "API rate limit reached. Please wait before retrying.",
        override val cause: Throwable? = null,
    ) : VoiceAIError

    data class InvalidConfig(
        override val message: String,
        override val cause: Throwable? = null,
    ) : VoiceAIError

    data class SessionAlreadyActive(
        override val message: String = "Session is already active. Call stop() first.",
        override val cause: Throwable? = null,
    ) : VoiceAIError
}

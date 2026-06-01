package com.sdk.voiceai.model

import com.sdk.voiceai.emotion.Emotion

enum class MessageRole { USER, ASSISTANT }

data class ConversationMessage(
    val role: MessageRole,
    val content: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val detectedEmotion: Emotion? = null,
)

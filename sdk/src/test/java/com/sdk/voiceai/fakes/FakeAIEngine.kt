package com.sdk.voiceai.fakes

import com.sdk.voiceai.ai.AIConfig
import com.sdk.voiceai.ai.AIEngine
import com.sdk.voiceai.model.ConversationMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

class FakeAIEngine(
    var nextResponse: String = "I am a fake AI response.",
) : AIEngine {

    var callCount: Int = 0
    var lastMessages: List<ConversationMessage> = emptyList()

    override fun respond(messages: List<ConversationMessage>, config: AIConfig): Flow<String> {
        callCount++
        lastMessages = messages
        // Emit response word-by-word
        return nextResponse.split(" ").asFlow().map { "$it " }
    }
}

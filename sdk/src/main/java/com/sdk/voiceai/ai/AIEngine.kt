package com.sdk.voiceai.ai

import com.sdk.voiceai.model.ConversationMessage
import kotlinx.coroutines.flow.Flow

/**
 * Configuration for an AI engine turn.
 *
 * @property model The identifier of the AI model to use for this turn.
 * @property systemPrompt An optional system-level instruction prepended to the conversation.
 *   When `null` the engine's default system prompt (if any) is used.
 * @property maxTokens The maximum number of tokens the model may generate in its reply.
 */
data class AIConfig(
    val model: String = "claude-3-5-sonnet-20241022",
    val systemPrompt: String? = null,
    val maxTokens: Int = 1024,
)

/**
 * Core AI engine interface. Implementations wrap LLMs that generate conversation replies.
 *
 * Each implementation is responsible for translating the SDK's [ConversationMessage] history
 * and [AIConfig] into provider-specific requests and streaming the resulting text back as a
 * [Flow] of incremental chunks.
 */
interface AIEngine {
    /**
     * Streams an AI response to [messages]. Emits text chunks in order as they arrive.
     *
     * @param messages The full conversation history including the latest user turn.
     * @param config Turn-specific AI configuration.
     * @return A [Flow] of response text chunks to be concatenated in order.
     */
    fun respond(messages: List<ConversationMessage>, config: AIConfig): Flow<String>
}

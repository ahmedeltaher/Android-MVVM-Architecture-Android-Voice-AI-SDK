package com.sdk.voiceai.ai

import com.sdk.voiceai.model.ConversationMessage
import timber.log.Timber

object ContextTrimmer {

    fun estimateTokens(messages: List<ConversationMessage>): Int =
        (messages.sumOf { it.content.length } * 0.25).toInt()

    fun trim(
        messages: List<ConversationMessage>,
        maxTokens: Int = 160_000,
    ): Pair<List<ConversationMessage>, Boolean> {
        if (estimateTokens(messages) <= maxTokens) return Pair(messages, false)

        val mutable = messages.toMutableList()
        var dropped = 0
        while (mutable.size >= 2 && estimateTokens(mutable) > maxTokens) {
            mutable.removeAt(0)
            mutable.removeAt(0)
            dropped += 2
        }
        if (estimateTokens(mutable) > maxTokens) {
            dropped += mutable.size
            mutable.clear()
        }
        Timber.d("ContextTrimmer: dropped $dropped messages to fit within $maxTokens tokens")
        return Pair(mutable.toList(), true)
    }
}

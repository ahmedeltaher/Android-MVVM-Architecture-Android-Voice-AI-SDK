package com.sdk.voiceai.ai

import com.sdk.voiceai.model.ConversationMessage
import timber.log.Timber

object ContextTrimmer {

    fun estimateTokens(messages: List<ConversationMessage>): Int {
        return (messages.sumOf { it.content.length } * 0.25).toInt()
    }

    fun trim(
        messages: List<ConversationMessage>,
        maxTokens: Int = 160_000
    ): Pair<List<ConversationMessage>, Boolean> {
        if (estimateTokens(messages) <= maxTokens) {
            return Pair(messages, false)
        }

        val mutable = messages.toMutableList()
        var droppedCount = 0

        while (estimateTokens(mutable) > maxTokens && mutable.size >= 2) {
            mutable.removeAt(0)
            mutable.removeAt(0)
            droppedCount += 2
        }

        if (estimateTokens(mutable) > maxTokens && mutable.isNotEmpty()) {
            droppedCount += mutable.size
            mutable.clear()
        }

        Timber.d("ContextTrimmer: dropped $droppedCount messages to fit within $maxTokens tokens")

        return Pair(mutable.toList(), true)
    }
}

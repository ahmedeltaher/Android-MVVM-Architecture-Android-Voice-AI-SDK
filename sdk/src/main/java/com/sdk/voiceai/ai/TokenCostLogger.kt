package com.sdk.voiceai.ai

import timber.log.Timber

object TokenCostLogger {

    private val inputCostPerMillion = mapOf(
        "sonnet"  to 3.0,
        "opus"    to 15.0,
        "haiku"   to 0.25,
        "default" to 3.0,
    )
    private val outputCostPerMillion = mapOf(
        "sonnet"  to 15.0,
        "opus"    to 75.0,
        "haiku"   to 1.25,
        "default" to 15.0,
    )

    fun log(model: String, inputTokens: Int, outputTokens: Int, debugLogging: Boolean) {
        if (!debugLogging) return
        val key = inputCostPerMillion.keys.firstOrNull { model.contains(it, ignoreCase = true) } ?: "default"
        val inputRate  = inputCostPerMillion[key]!!
        val outputRate = outputCostPerMillion[key]!!
        val cost = (inputTokens / 1_000_000.0 * inputRate) + (outputTokens / 1_000_000.0 * outputRate)
        val costStr = "%.4f".format(cost)
        Timber.d("Token usage — input=$inputTokens output=$outputTokens est. cost=\$$costStr")
    }
}

package com.sdk.voiceai.audio

import kotlin.math.sqrt

class AudioLevelMeter(private val smoothingFactor: Float = 0.2f) {

    private var smoothedLevel = 0f

    fun process(pcm: ByteArray): Float {
        val rms = computeRms(pcm)
        val normalized = (rms / 32767f).coerceIn(0f, 1f)
        smoothedLevel = smoothedLevel * (1f - smoothingFactor) + normalized * smoothingFactor
        return smoothedLevel
    }

    fun reset() {
        smoothedLevel = 0f
    }

    private fun computeRms(pcm: ByteArray): Float {
        var sumSquares = 0.0
        var i = 0
        while (i < pcm.size - 1) {
            val sample = (pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)
            sumSquares += sample.toDouble() * sample.toDouble()
            i += 2
        }
        val count = pcm.size / 2
        return if (count == 0) 0f else sqrt(sumSquares / count).toFloat()
    }
}

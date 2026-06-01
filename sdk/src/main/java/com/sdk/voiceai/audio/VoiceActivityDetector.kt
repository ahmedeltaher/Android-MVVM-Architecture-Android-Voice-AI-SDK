package com.sdk.voiceai.audio

import kotlin.math.sqrt

class VoiceActivityDetector(
    private val silenceTimeoutMs: Long = 1200L,
    private val speechThresholdRms: Float = 500f,
    private val silenceThresholdRms: Float = 300f,
) {

    enum class VadState { SILENCE, SPEECH }

    private var state = VadState.SILENCE
    private var silenceStartMs = 0L

    sealed interface VadEvent {
        data object SpeechStart : VadEvent
        data object SpeechEnd : VadEvent
        data object None : VadEvent
    }

    fun process(chunk: ByteArray, nowMs: Long = System.currentTimeMillis()): VadEvent {
        val rms = computeRms(chunk)
        return when (state) {
            VadState.SILENCE -> {
                if (rms > speechThresholdRms) {
                    state = VadState.SPEECH
                    VadEvent.SpeechStart
                } else {
                    VadEvent.None
                }
            }
            VadState.SPEECH -> {
                if (rms < silenceThresholdRms) {
                    if (silenceStartMs == 0L) silenceStartMs = nowMs
                    if (nowMs - silenceStartMs >= silenceTimeoutMs) {
                        state = VadState.SILENCE
                        silenceStartMs = 0L
                        VadEvent.SpeechEnd
                    } else VadEvent.None
                } else {
                    silenceStartMs = 0L
                    VadEvent.None
                }
            }
        }
    }

    fun reset() {
        state = VadState.SILENCE
        silenceStartMs = 0L
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

package com.sdk.voiceai.emotion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class OnDeviceEmotionDetector : VoiceEmotionDetector {

    override suspend fun detect(audio: ByteArray, config: EmotionConfig): EmotionResult =
        withContext(Dispatchers.Default) {
            val samples = pcmToSamples(audio)
            if (samples.isEmpty()) return@withContext neutral(config)

            val energy = computeRms(samples).coerceIn(0f, 1f)
            val zcr = computeZeroCrossingRate(samples)

            val dominant = when {
                energy > 0.6f && zcr > 0.15f               -> Emotion.ANGRY
                energy > 0.5f && zcr > 0.20f               -> Emotion.SURPRISED
                energy > 0.4f && zcr > 0.12f               -> Emotion.HAPPY
                zcr > 0.22f                                  -> Emotion.DISGUSTED
                energy in 0.15f..0.3f && zcr > 0.18f       -> Emotion.FEARFUL
                energy < 0.15f                              -> Emotion.SAD
                else                                        -> Emotion.NEUTRAL
            }

            val filtered = if (config.targetEmotions.contains(dominant)) dominant
            else config.targetEmotions.firstOrNull() ?: Emotion.NEUTRAL

            EmotionResult(
                dominantEmotion = filtered,
                scores = buildScores(filtered, config.targetEmotions),
                confidence = energy.coerceIn(0.3f, 0.95f),
            )
        }

    private fun pcmToSamples(pcm: ByteArray): FloatArray {
        val count = pcm.size / 2
        val samples = FloatArray(count)
        for (i in 0 until count) {
            val lo = pcm[i * 2].toInt() and 0xFF
            val hi = pcm[i * 2 + 1].toInt()
            samples[i] = ((hi shl 8) or lo).toShort() / 32767f
        }
        return samples
    }

    private fun computeRms(samples: FloatArray): Float {
        val sumSq = samples.fold(0.0) { acc, s -> acc + s * s }
        return sqrt(sumSq / samples.size).toFloat()
    }

    private fun computeZeroCrossingRate(samples: FloatArray): Float {
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0f) != (samples[i - 1] >= 0f)) crossings++
        }
        return crossings.toFloat() / samples.size
    }

    private fun buildScores(dominant: Emotion, targets: Set<Emotion>): Map<Emotion, Float> {
        val rest = (targets - dominant).toList()
        val restShare = if (rest.isEmpty()) 0f else 0.3f / rest.size
        return targets.associateWith { if (it == dominant) 0.7f else restShare }
    }

    private fun neutral(config: EmotionConfig): EmotionResult {
        val dom = if (config.targetEmotions.contains(Emotion.NEUTRAL)) Emotion.NEUTRAL
        else config.targetEmotions.first()
        return EmotionResult(dom, buildScores(dom, config.targetEmotions), 0.3f)
    }
}

package com.sdk.voiceai.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioConverter {

    fun pcmToWav(pcmBytes: ByteArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val totalDataLen = pcmBytes.size + 36
        val out = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        header.put("RIFF".toByteArray())
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray())

        // fmt sub-chunk
        header.put("fmt ".toByteArray())
        header.putInt(16)               // sub-chunk size
        header.putShort(1)              // PCM format
        header.putShort(1)              // mono
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)   // byte rate
        header.putShort(2)              // block align
        header.putShort(16)             // bits per sample

        // data sub-chunk
        header.put("data".toByteArray())
        header.putInt(pcmBytes.size)

        out.write(header.array())
        out.write(pcmBytes)
        return out.toByteArray()
    }
}

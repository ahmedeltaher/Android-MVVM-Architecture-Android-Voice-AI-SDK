package com.sdk.voiceai.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import timber.log.Timber
import kotlin.coroutines.coroutineContext

internal const val SAMPLE_RATE = 16000
internal const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
internal const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
internal const val CHUNK_DURATION_MS = 160

class AudioRecorder(private val audioEnhancement: Boolean = true) {

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    val bufferSize: Int by lazy {
        maxOf(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2,
            4096,
        )
    }

    val chunkSize: Int = SAMPLE_RATE * 2 * CHUNK_DURATION_MS / 1000

    @SuppressLint("MissingPermission")
    fun start(): Flow<ByteArray> = flow {
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize,
        ).also { audioRecord = it }

        check(recorder.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord failed to initialize"
        }

        if (audioEnhancement) {
            applyEffects(recorder.audioSessionId)
        }

        recorder.startRecording()
        Timber.d("AudioRecorder started (bufferSize=$bufferSize, chunkSize=$chunkSize)")

        val buffer = ByteArray(chunkSize)
        try {
            while (coroutineContext.isActive) {
                val read = recorder.read(buffer, 0, buffer.size)
                if (read > 0) {
                    emit(buffer.copyOf(read))
                }
            }
        } finally {
            release()
        }
    }.flowOn(Dispatchers.IO)

    fun stop() {
        release()
    }

    private fun applyEffects(sessionId: Int) {
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(sessionId)
            noiseSuppressor?.enabled = true
            Timber.d("NoiseSuppressor applied")
        }
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(sessionId)
            echoCanceler?.enabled = true
            Timber.d("AcousticEchoCanceler applied")
        }
    }

    private fun release() {
        noiseSuppressor?.release()
        echoCanceler?.release()
        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) stop()
            release()
        }
        audioRecord = null
        Timber.d("AudioRecorder released")
    }
}

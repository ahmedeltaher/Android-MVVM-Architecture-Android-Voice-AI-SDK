package com.sdk.voiceai.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidTtsEngine(context: Context) : TextToSpeechEngine {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (!ready) Timber.e("TextToSpeech initialization failed with status $status")
        }
    }

    override suspend fun speak(text: String, config: TtsConfig) {
        val engine = tts ?: return
        if (!ready) {
            Timber.w("TTS not ready, skipping: $text")
            return
        }

        engine.language = config.locale
        engine.setSpeechRate(config.speechRate)
        engine.setPitch(config.pitch)

        val utteranceId = UUID.randomUUID().toString()

        suspendCancellableCoroutine { cont ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(uid: String?) {
                    if (uid == utteranceId) cont.resume(Unit)
                }
                override fun onError(uid: String?) {
                    if (uid == utteranceId) cont.resumeWithException(
                        RuntimeException("TTS utterance error for: $text")
                    )
                }
            })

            val params = android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, config.volume)
            }
            engine.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)

            cont.invokeOnCancellation { engine.stop() }
        }
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}

package com.sdk.voiceai.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidSttEngine(private val context: Context) : SpeechToTextEngine {

    private var recognizer: SpeechRecognizer? = null

    override suspend fun transcribe(audioBytes: ByteArray, config: SttConfig): TranscriptResult {
        // Android SpeechRecognizer is live-mic only; audioBytes unused here.
        return suspendCancellableCoroutine { cont ->
            val sr = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
            val intent = buildIntent(config)

            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val text = matches?.firstOrNull().orEmpty()
                    val confidence = confidences?.firstOrNull()
                    Timber.d("STT result: $text (confidence=$confidence)")
                    sr.destroy()
                    recognizer = null
                    cont.resume(TranscriptResult(text = text, confidence = confidence, isFinal = true))
                }

                override fun onError(error: Int) {
                    sr.destroy()
                    recognizer = null
                    cont.resumeWithException(
                        RuntimeException("SpeechRecognizer error code: $error")
                    )
                }
            })

            sr.startListening(intent)

            cont.invokeOnCancellation {
                sr.cancel()
                sr.destroy()
            }
        }
    }

    override fun transcribeStream(config: SttConfig): Flow<TranscriptResult> = callbackFlow {
        val sr = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        val intent = buildIntent(config)

        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                trySend(TranscriptResult(text = partial, confidence = null, isFinal = false))
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                val confidence = results
                    ?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    ?.firstOrNull()
                trySend(TranscriptResult(text = text, confidence = confidence, isFinal = true))
                close()
            }

            override fun onError(error: Int) {
                close(RuntimeException("SpeechRecognizer error: $error"))
            }
        })

        sr.startListening(intent)

        awaitClose {
            sr.cancel()
            sr.destroy()
        }
    }

    override fun cancel() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    private fun buildIntent(config: SttConfig) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.locale.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, config.partialResults)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
}

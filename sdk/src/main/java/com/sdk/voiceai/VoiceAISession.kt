package com.sdk.voiceai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sdk.voiceai.ai.AIConfig
import com.sdk.voiceai.ai.AIEngine
import com.sdk.voiceai.audio.AudioFocusManager
import com.sdk.voiceai.audio.AudioLevelMeter
import com.sdk.voiceai.audio.AudioRecorder
import com.sdk.voiceai.audio.VoiceActivityDetector
import com.sdk.voiceai.emotion.EmotionConfig
import com.sdk.voiceai.emotion.EmotionEvent
import com.sdk.voiceai.emotion.EmotionResult
import com.sdk.voiceai.emotion.VoiceEmotionDetector
import com.sdk.voiceai.model.ConversationMessage
import com.sdk.voiceai.model.InputMode
import com.sdk.voiceai.model.MessageRole
import com.sdk.voiceai.model.SessionState
import com.sdk.voiceai.model.VoiceAIConfig
import com.sdk.voiceai.model.VoiceAIError
import com.sdk.voiceai.model.VoiceAIEvent
import com.sdk.voiceai.security.PiiRedactor
import com.sdk.voiceai.stt.SpeechToTextEngine
import com.sdk.voiceai.stt.SttConfig
import com.sdk.voiceai.tts.TextToSpeechEngine
import com.sdk.voiceai.tts.TtsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream

class VoiceAISession internal constructor(
    private val context: Context,
    private val config: VoiceAIConfig,
    private val sttEngine: SpeechToTextEngine,
    private val aiEngine: AIEngine,
    private val ttsEngine: TextToSpeechEngine,
    private val emotionDetector: VoiceEmotionDetector? = null,
    private val onDestroy: () -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<VoiceAIEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<VoiceAIEvent> = _events.asSharedFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationMessage>> = _conversationHistory.asStateFlow()

    private val _partialTranscript = MutableStateFlow<String?>(null)
    val partialTranscript: StateFlow<String?> = _partialTranscript.asStateFlow()

    private val _emotionState = MutableStateFlow<EmotionResult?>(null)
    val emotionState: StateFlow<EmotionResult?> = _emotionState.asStateFlow()

    private val _emotionEvents = MutableSharedFlow<EmotionEvent>(extraBufferCapacity = 16)
    val emotionEvents: SharedFlow<EmotionEvent> = _emotionEvents.asSharedFlow()

    private val audioRecorder = AudioRecorder(config.audioEnhancement)
    private val vad = VoiceActivityDetector(silenceTimeoutMs = config.silenceTimeoutMs)
    private val levelMeter = AudioLevelMeter()
    private val audioFocusManager = AudioFocusManager(context)

    private var recordingJob: Job? = null
    private var aiJob: Job? = null
    private var isStopped = false

    // ── Lifecycle ────────────────────────────────────────────────────────────

    fun start() {
        if (_state.value != SessionState.Idle && _state.value !is SessionState.Error) {
            Timber.w("Session already active, ignoring start()")
            return
        }
        if (!hasAudioPermission()) {
            scope.launch { emitError(VoiceAIError.PermissionDenied()) }
            return
        }
        isStopped = false
        val focusGranted = audioFocusManager.requestFocus { pause() }
        if (!focusGranted) Timber.w("Audio focus not granted — proceeding anyway")
        if (config.inputMode == InputMode.HANDS_FREE) startHandsFreeListening()
    }

    fun stop() {
        isStopped = true
        recordingJob?.cancel()
        recordingJob = null
        aiJob?.cancel()
        aiJob = null
        sttEngine.cancel()
        ttsEngine.stop()
        vad.reset()
        levelMeter.reset()
        audioFocusManager.abandonFocus()
        _partialTranscript.value = null
        _audioLevel.value = 0f
        _state.value = SessionState.Idle
        Timber.d("Session stopped")
    }

    fun pause() {
        if (_state.value != SessionState.Idle && _state.value != SessionState.Paused) {
            recordingJob?.cancel()
            recordingJob = null
            ttsEngine.stop()
            _state.value = SessionState.Paused
        }
    }

    fun resume() {
        if (_state.value == SessionState.Paused && !isStopped) {
            _state.value = SessionState.Idle
            if (config.inputMode == InputMode.HANDS_FREE) startHandsFreeListening()
        }
    }

    fun holdToRecord() {
        if (config.inputMode != InputMode.PUSH_TO_TALK) return
        if (!hasAudioPermission()) {
            scope.launch { emitError(VoiceAIError.PermissionDenied()) }
            return
        }
        isStopped = false
        startPushToTalkCapture()
    }

    fun releaseRecord() {
        if (config.inputMode != InputMode.PUSH_TO_TALK) return
        recordingJob?.cancel()
        recordingJob = null
    }

    fun clearHistory() {
        _conversationHistory.value = emptyList()
        Timber.d("Conversation history cleared")
    }

    fun updateSystemPrompt(prompt: String?) {
        // Reflected into aiConfig() on next turn — config is immutable so we track override
        _dynamicSystemPrompt = prompt
    }

    private var _dynamicSystemPrompt: String? = null

    // ── Push-to-talk ─────────────────────────────────────────────────────────

    private fun startPushToTalkCapture() {
        val buffer = ByteArrayOutputStream()
        _state.value = SessionState.Listening
        recordingJob = scope.launch {
            audioRecorder.start().collect { chunk ->
                if (isStopped) return@collect
                _audioLevel.value = levelMeter.process(chunk)
                buffer.write(chunk)
            }
            val pcm = buffer.toByteArray()
            if (pcm.isNotEmpty()) processSpeech(pcm)
        }
    }

    // ── Hands-free (VAD) ─────────────────────────────────────────────────────

    private fun startHandsFreeListening() {
        recordingJob = scope.launch {
            _state.value = SessionState.Listening
            val audioBuffer = ByteArrayOutputStream()
            var isCapturing = false

            audioRecorder.start().collect { chunk ->
                if (isStopped) return@collect
                _audioLevel.value = levelMeter.process(chunk)

                when (val event = vad.process(chunk)) {
                    is VoiceActivityDetector.VadEvent.SpeechStart -> {
                        isCapturing = true
                        audioBuffer.reset()
                        // Interrupt TTS if AI is currently speaking
                        if (_state.value == SessionState.Speaking) {
                            ttsEngine.stop()
                            aiJob?.cancel()
                        }
                        _events.emit(VoiceAIEvent.SpeechStart)
                        Timber.d("VAD: speech start")
                    }
                    is VoiceActivityDetector.VadEvent.SpeechEnd -> {
                        isCapturing = false
                        _events.emit(VoiceAIEvent.SpeechEnd)
                        val pcm = audioBuffer.toByteArray()
                        audioBuffer.reset()
                        if (pcm.isNotEmpty()) processSpeech(pcm)
                    }
                    is VoiceActivityDetector.VadEvent.None -> {
                        if (isCapturing) audioBuffer.write(chunk)
                    }
                }
            }
        }
    }

    // ── Full pipeline ────────────────────────────────────────────────────────

    private suspend fun processSpeech(pcm: ByteArray) {
        _state.value = SessionState.Processing
        _audioLevel.value = 0f

        // Emotion detection (parallel with STT, non-blocking)
        var detectedEmotion = _emotionState.value?.dominantEmotion
        if (config.emotionDetectionEnabled && emotionDetector != null) {
            try {
                val result = emotionDetector.detect(pcm, EmotionConfig())
                val previous = _emotionState.value?.dominantEmotion
                _emotionState.value = result
                detectedEmotion = result.dominantEmotion
                if (previous != null && previous != result.dominantEmotion) {
                    _emotionEvents.emit(EmotionEvent.EmotionChanged(previous, result.dominantEmotion))
                }
            } catch (e: Exception) {
                _emotionEvents.emit(EmotionEvent.EmotionDetectionFailed(e))
                Timber.w(e, "Emotion detection failed (non-fatal)")
            }
        }

        // STT
        val sttConfig = SttConfig(locale = config.locale, partialResults = false)
        val transcript = try {
            sttEngine.transcribe(pcm, sttConfig)
        } catch (e: Exception) {
            Timber.e(e, "STT failed")
            emitError(VoiceAIError.SttFailed(e.message ?: "STT error", e))
            restartListening()
            return
        }

        var rawText = transcript.text
        if (rawText.isBlank()) { restartListening(); return }

        // PII redaction
        if (config.piiRedaction) rawText = PiiRedactor.redact(rawText)

        // Transcript post-processing
        val processedText = if (config.transcriptProcessing) {
            transcriptPostProcess(rawText)
        } else rawText

        _events.emit(VoiceAIEvent.FinalTranscript(processedText, transcript.confidence))
        _partialTranscript.value = null

        val userMessage = ConversationMessage(
            role = MessageRole.USER,
            content = processedText,
            detectedEmotion = detectedEmotion,
        )
        appendHistory(userMessage)

        // AI
        val effectiveSystemPrompt = buildSystemPrompt(detectedEmotion)
        val aiConfig = AIConfig(model = config.aiModel, systemPrompt = effectiveSystemPrompt)

        val responseBuilder = StringBuilder()
        val sentenceBuffer = StringBuilder()

        _state.value = SessionState.Speaking
        _events.emit(VoiceAIEvent.SpeakingStarted)

        aiJob = scope.launch {
            try {
                aiEngine.respond(conversationHistory.value, aiConfig).collect { chunk ->
                    responseBuilder.append(chunk)
                    sentenceBuffer.append(chunk)
                    _events.emit(VoiceAIEvent.PartialResponse(chunk))

                    // Sentence-boundary streaming TTS: speak each complete sentence immediately
                    val sentences = extractCompleteSentences(sentenceBuffer)
                    sentences.forEach { sentence ->
                        val filtered = applyResponseFilter(sentence)
                        if (filtered.isNotBlank()) {
                            val ttsConfig = TtsConfig(locale = config.locale)
                            try {
                                ttsEngine.speak(filtered, ttsConfig)
                            } catch (e: Exception) {
                                Timber.w(e, "TTS sentence failed")
                            }
                        }
                    }
                }

                // Speak any remaining text in buffer
                val remaining = sentenceBuffer.toString().trim()
                if (remaining.isNotBlank()) {
                    val filtered = applyResponseFilter(remaining)
                    if (filtered.isNotBlank()) {
                        try {
                            ttsEngine.speak(filtered, TtsConfig(locale = config.locale))
                        } catch (e: Exception) {
                            Timber.w(e, "TTS remainder failed")
                        }
                    }
                }

                val fullResponse = responseBuilder.toString()
                _events.emit(VoiceAIEvent.FullResponse(fullResponse))
                appendHistory(ConversationMessage(MessageRole.ASSISTANT, fullResponse))

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                Timber.e(e, "AI failed")
                emitError(VoiceAIError.AiFailed(e.message ?: "AI error", e))
            } finally {
                _events.emit(VoiceAIEvent.SpeakingStopped)
                if (!isStopped) restartListening()
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun buildSystemPrompt(detectedEmotion: com.sdk.voiceai.emotion.Emotion?): String? {
        val base = _dynamicSystemPrompt ?: config.systemPrompt
        return if (config.emotionAwareAI && detectedEmotion != null && detectedEmotion.name != "NEUTRAL") {
            val emotionNote = "The user currently sounds ${detectedEmotion.name.lowercase()}. Adapt your response tone accordingly."
            if (base != null) "$base\n\n$emotionNote" else emotionNote
        } else base
    }

    private fun extractCompleteSentences(buffer: StringBuilder): List<String> {
        val sentences = mutableListOf<String>()
        var lastEnd = 0
        val text = buffer.toString()
        val terminators = setOf('.', '!', '?')
        for (i in text.indices) {
            if (text[i] in terminators && (i + 1 >= text.length || text[i + 1] == ' ' || text[i + 1] == '\n')) {
                sentences.add(text.substring(lastEnd, i + 1).trim())
                lastEnd = i + 1
            }
        }
        if (lastEnd > 0) {
            buffer.delete(0, lastEnd)
        }
        return sentences.filter { it.isNotBlank() }
    }

    private suspend fun applyResponseFilter(text: String): String {
        return try {
            config.responseFilter?.invoke(text) ?: text
        } catch (e: Exception) {
            Timber.w(e, "responseFilter threw — using original text")
            text
        }
    }

    private fun transcriptPostProcess(text: String): String {
        val trimmed = text.trim()
        val capitalized = trimmed.replaceFirstChar { it.uppercase() }
        return if (capitalized.last() in setOf('.', '?', '!', ',', ';', ':')) capitalized
        else "$capitalized."
    }

    private fun restartListening() {
        if (!isStopped && config.inputMode == InputMode.HANDS_FREE) {
            _state.value = SessionState.Listening
            vad.reset()
        } else if (!isStopped) {
            _state.value = SessionState.Idle
        }
    }

    private fun appendHistory(message: ConversationMessage) {
        _conversationHistory.update { current ->
            val updated = current + message
            // Trim oldest turns when over limit (keep pairs: user+assistant)
            if (updated.size > config.maxHistoryTurns * 2) {
                val trimmed = updated.drop(2)
                scope.launch { _events.emit(VoiceAIEvent.ContextCondensed) }
                trimmed
            } else updated
        }
    }

    private suspend fun emitError(error: VoiceAIError) {
        _events.emit(VoiceAIEvent.ErrorOccurred(error))
        _state.value = SessionState.Error(error)
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    fun destroy() {
        stop()
        audioFocusManager.abandonFocus()
        ttsEngine.shutdown()
        scope.cancel()
        onDestroy()
    }
}

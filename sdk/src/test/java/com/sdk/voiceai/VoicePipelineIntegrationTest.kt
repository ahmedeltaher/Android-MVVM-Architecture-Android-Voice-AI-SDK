package com.sdk.voiceai

import androidx.test.core.app.ApplicationProvider
import com.sdk.voiceai.fakes.FakeAIEngine
import com.sdk.voiceai.fakes.FakeSttEngine
import com.sdk.voiceai.fakes.FakeTtsEngine
import com.sdk.voiceai.model.ConversationMessage
import com.sdk.voiceai.model.InputMode
import com.sdk.voiceai.model.MessageRole
import com.sdk.voiceai.model.VoiceAIConfig
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoicePipelineIntegrationTest {

    private fun makeSession(): VoiceAISession {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = VoiceAIConfig(
            anthropicApiKey = "key",
            inputMode = InputMode.PUSH_TO_TALK,
        )
        return VoiceAISession(
            context = context,
            config = config,
            sttEngine = FakeSttEngine(),
            aiEngine = FakeAIEngine(nextResponse = "Hello"),
            ttsEngine = FakeTtsEngine(),
        )
    }

    @Test
    fun sessionStartsInIdleState() = runTest(UnconfinedTestDispatcher()) {
        val session = makeSession()
        assertEquals(com.sdk.voiceai.model.SessionState.Idle, session.state.value)
        session.destroy()
    }

    @Test
    fun clearHistoryProducesEmptyList() = runTest(UnconfinedTestDispatcher()) {
        val session = makeSession()
        // Add a message directly to history via internal state manipulation
        val field = VoiceAISession::class.java.getDeclaredField("_conversationHistory")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val historyFlow = field.get(session) as kotlinx.coroutines.flow.MutableStateFlow<List<ConversationMessage>>
        historyFlow.value = listOf(
            ConversationMessage(
                role = MessageRole.USER,
                content = "Test message",
                timestampMs = System.currentTimeMillis(),
            )
        )
        assertTrue(session.conversationHistory.value.isNotEmpty())
        session.clearHistory()
        assertTrue(session.conversationHistory.value.isEmpty())
        session.destroy()
    }

    @Test
    fun destroyDoesNotThrow() {
        val session = makeSession()
        assertDoesNotThrow {
            session.destroy()
        }
    }

    @Test
    fun fakeSttEngineRecordsCallCount() = runTest(UnconfinedTestDispatcher()) {
        val fakeStt = FakeSttEngine()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = VoiceAIConfig(
            anthropicApiKey = "key",
            inputMode = InputMode.PUSH_TO_TALK,
        )
        VoiceAISession(
            context = context,
            config = config,
            sttEngine = fakeStt,
            aiEngine = FakeAIEngine(nextResponse = "Hello"),
            ttsEngine = FakeTtsEngine(),
        ).also { session ->
            val audioBytes = ByteArray(1024) { 0 }
            fakeStt.transcribe(audioBytes, com.sdk.voiceai.stt.SttConfig())
            assertEquals(1, fakeStt.callCount)
            session.destroy()
        }
    }

    @Test
    fun fakeTtsEngineRecordsSpokenText() = runTest(UnconfinedTestDispatcher()) {
        val fakeTts = FakeTtsEngine()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = VoiceAIConfig(
            anthropicApiKey = "key",
            inputMode = InputMode.PUSH_TO_TALK,
        )
        VoiceAISession(
            context = context,
            config = config,
            sttEngine = FakeSttEngine(),
            aiEngine = FakeAIEngine(nextResponse = "Hello"),
            ttsEngine = fakeTts,
        ).also { session ->
            fakeTts.speak("Hello world", com.sdk.voiceai.tts.TtsConfig())
            assertTrue(fakeTts.spokenTexts.contains("Hello world"))
            session.destroy()
        }
    }
}

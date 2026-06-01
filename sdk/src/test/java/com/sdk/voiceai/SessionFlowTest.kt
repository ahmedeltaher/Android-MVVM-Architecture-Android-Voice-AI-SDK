package com.sdk.voiceai

import app.cash.turbine.test
import com.sdk.voiceai.fakes.FakeAIEngine
import com.sdk.voiceai.fakes.FakeSttEngine
import com.sdk.voiceai.fakes.FakeTtsEngine
import com.sdk.voiceai.model.InputMode
import com.sdk.voiceai.model.SessionState
import com.sdk.voiceai.model.VoiceAIConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionFlowTest {

    private fun createSession(inputMode: InputMode = InputMode.PUSH_TO_TALK): VoiceAISession {
        val context = mockk<android.content.Context>(relaxed = true)
        val pm = mockk<android.content.pm.PackageManager>(relaxed = true)
        every { context.packageManager } returns pm
        every { context.applicationContext } returns context
        every {
            context.checkPermission(android.Manifest.permission.RECORD_AUDIO, any(), any())
        } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        every {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
        } returns android.content.pm.PackageManager.PERMISSION_GRANTED

        return VoiceAISession(
            context = context,
            config = VoiceAIConfig(anthropicApiKey = "test", inputMode = inputMode),
            sttEngine = FakeSttEngine(),
            aiEngine = FakeAIEngine(),
            ttsEngine = FakeTtsEngine(),
        )
    }

    @Test
    fun stateFlowEmitsIdle() = runTest(UnconfinedTestDispatcher()) {
        val session = createSession()
        session.state.test {
            assertEquals(SessionState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        session.destroy()
    }

    @Test
    fun stopFromIdleRemainsIdle() = runTest(UnconfinedTestDispatcher()) {
        val session = createSession()
        session.stop()
        assertEquals(SessionState.Idle, session.state.value)
        session.destroy()
    }

    @Test
    fun pauseOnIdleNoOp() = runTest(UnconfinedTestDispatcher()) {
        val session = createSession()
        session.pause()
        assertEquals(SessionState.Idle, session.state.value)
        session.destroy()
    }

    @Test
    fun clearHistoryEmitsEmptyList() = runTest(UnconfinedTestDispatcher()) {
        val session = createSession()
        session.conversationHistory.test {
            assertTrue(awaitItem().isEmpty())
            session.clearHistory()
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(session.conversationHistory.value.isEmpty())
        session.destroy()
    }

    @Test
    fun audioLevelDefaultsToZero() = runTest(UnconfinedTestDispatcher()) {
        val session = createSession()
        assertEquals(0f, session.audioLevel.value)
        session.destroy()
    }

    @Test
    fun emotionStateDefaultsToNull() = runTest(UnconfinedTestDispatcher()) {
        val session = createSession()
        assertNull(session.emotionState.value)
        session.destroy()
    }
}

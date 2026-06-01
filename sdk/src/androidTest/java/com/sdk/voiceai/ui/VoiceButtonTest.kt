package com.sdk.voiceai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.sdk.voiceai.model.SessionState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun voiceButtonIdleHasMicOffDescription() {
        composeTestRule.setContent {
            MaterialTheme {
                VoiceButton(state = SessionState.Idle, onClick = {})
            }
        }
        composeTestRule
            .onNodeWithContentDescription("Tap to start voice session")
            .assertIsDisplayed()
    }

    @Test
    fun voiceButtonListeningHasMicDescription() {
        composeTestRule.setContent {
            MaterialTheme {
                VoiceButton(state = SessionState.Listening, onClick = {})
            }
        }
        composeTestRule
            .onNodeWithContentDescription("Listening — tap to stop")
            .assertIsDisplayed()
    }

    @Test
    fun voiceButtonClickCallsCallback() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                VoiceButton(state = SessionState.Idle, onClick = { clicked = true })
            }
        }
        composeTestRule
            .onNodeWithContentDescription("Tap to start voice session")
            .performClick()
        assertTrue(clicked)
    }

    @Test
    fun voiceButtonProcessingShowsThinking() {
        composeTestRule.setContent {
            MaterialTheme {
                VoiceButton(state = SessionState.Processing, onClick = {})
            }
        }
        composeTestRule
            .onNodeWithContentDescription("Processing your request")
            .assertIsDisplayed()
    }
}

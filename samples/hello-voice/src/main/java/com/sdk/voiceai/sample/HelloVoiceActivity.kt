package com.sdk.voiceai.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdk.voiceai.VoiceAISDK
import com.sdk.voiceai.VoiceAISession
import com.sdk.voiceai.model.SessionState
import com.sdk.voiceai.ui.ConversationView
import com.sdk.voiceai.ui.VoiceButton
import com.sdk.voiceai.ui.VoiceSessionPermissionGate

class HelloVoiceActivity : ComponentActivity() {

    private lateinit var sdk: VoiceAISDK
    private lateinit var session: VoiceAISession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sdk = VoiceAISDK.Builder(this)
            .anthropicApiKey(BuildConfig.ANTHROPIC_API_KEY)
            .debugLogging(true)
            .build()

        session = sdk.createSession()

        setContent {
            MaterialTheme {
                VoiceSessionPermissionGate(
                    rationale = "Microphone access is required for voice conversations.",
                ) {
                    HelloVoiceScreen(session = session)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session.destroy()
    }
}

@Composable
fun HelloVoiceScreen(session: VoiceAISession) {
    val state by session.state.collectAsState()
    val messages by session.conversationHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConversationView(
            messages = messages,
            modifier = Modifier.weight(1f),
        )

        VoiceButton(
            state = state,
            onClick = {
                when (state) {
                    is SessionState.Idle, is SessionState.Error -> session.start()
                    else -> session.stop()
                }
            },
            modifier = Modifier.padding(bottom = 32.dp),
        )
    }
}

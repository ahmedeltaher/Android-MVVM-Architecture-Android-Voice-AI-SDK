package com.sdk.voiceai.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdk.voiceai.VoiceAISDK
import com.sdk.voiceai.VoiceAISession
import com.sdk.voiceai.SessionState
import com.sdk.voiceai.ui.ConversationView
import com.sdk.voiceai.ui.VoiceButton
import com.sdk.voiceai.ui.VoiceSessionPermissionGate
import kotlinx.coroutines.flow.collectAsState

class HelloVoiceActivity : ComponentActivity() {

    private lateinit var sdk: VoiceAISDK
    private lateinit var session: VoiceAISession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sdk = VoiceAISDK.Builder(this)
            .anthropicApiKey(BuildConfig.ANTHROPIC_API_KEY)
            .build()

        session = sdk.createSession()

        setContent {
            MaterialTheme {
                VoiceSessionPermissionGate {
                    HelloVoiceScreen(session)
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
    val conversationHistory by session.conversationHistory.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ConversationView(
            messages = conversationHistory,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        VoiceButton(
            isActive = state is SessionState.Listening || state is SessionState.Processing || state is SessionState.Speaking,
            onClick = {
                if (state is SessionState.Idle || state is SessionState.Error) {
                    session.start()
                } else {
                    session.stop()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp, top = 16.dp)
        )
    }
}

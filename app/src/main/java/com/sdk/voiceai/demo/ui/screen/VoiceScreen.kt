package com.sdk.voiceai.demo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdk.voiceai.model.SessionState
import com.sdk.voiceai.model.VoiceAIError
import com.sdk.voiceai.ui.ConversationView
import com.sdk.voiceai.ui.LiveCaptionBanner
import com.sdk.voiceai.ui.VoiceButton
import com.sdk.voiceai.ui.VoiceStatusIndicator
import com.sdk.voiceai.ui.WaveformVisualizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    viewModel: VoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.sessionState.collectAsStateWithLifecycle()
    val history by viewModel.conversationHistory.collectAsStateWithLifecycle()
    val audioLevel by viewModel.audioLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors in snackbar
    LaunchedEffect(state) {
        if (state is SessionState.Error) {
            val error = (state as SessionState.Error).error
            snackbarHostState.showSnackbar(error.message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Voice AI SDK") },
                actions = {
                    IconButton(onClick = viewModel::clearHistory) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear conversation")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding(),
        ) {
            // Live caption banner at top
            LiveCaptionBanner(
                transcript = partialTranscript,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Conversation fills available space
            ConversationView(
                messages = history,
                partialTranscript = if (state is SessionState.Listening) partialTranscript else null,
                modifier = Modifier.weight(1f),
            )

            // Bottom controls area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Waveform only visible while listening
                if (state is SessionState.Listening) {
                    WaveformVisualizer(
                        audioLevel = audioLevel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 32.dp),
                    )
                } else {
                    Spacer(modifier = Modifier.height(60.dp))
                }

                VoiceStatusIndicator(state = state)

                VoiceButton(
                    state = state,
                    onClick = viewModel::toggleSession,
                )
            }
        }
    }
}

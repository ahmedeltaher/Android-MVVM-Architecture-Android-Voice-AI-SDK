package com.sdk.voiceai.demo.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdk.voiceai.VoiceAISDK
import com.sdk.voiceai.VoiceAISession
import com.sdk.voiceai.model.ConversationMessage
import com.sdk.voiceai.model.SessionState
import com.sdk.voiceai.model.VoiceAIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val sdk: VoiceAISDK,
) : ViewModel() {

    private val session: VoiceAISession = sdk.createSession()

    val sessionState: StateFlow<SessionState> = session.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Idle)

    val conversationHistory: StateFlow<List<ConversationMessage>> = session.conversationHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val audioLevel: StateFlow<Float> = session.audioLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val partialTranscript: StateFlow<String?> = session.partialTranscript
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            session.events.collect { event ->
                // Events can be consumed here for analytics / logging
                when (event) {
                    is VoiceAIEvent.ErrorOccurred -> {
                        // Errors are also surfaced via sessionState
                    }
                    else -> Unit
                }
            }
        }
    }

    fun toggleSession() {
        when (session.state.value) {
            is SessionState.Idle,
            is SessionState.Error -> session.start()
            else -> session.stop()
        }
    }

    fun clearHistory() = session.clearHistory()

    override fun onCleared() {
        super.onCleared()
        session.destroy()
    }
}

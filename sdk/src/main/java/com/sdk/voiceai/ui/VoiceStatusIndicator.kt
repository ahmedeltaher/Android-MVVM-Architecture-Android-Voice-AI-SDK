package com.sdk.voiceai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.sdk.voiceai.model.SessionState

@Composable
fun VoiceStatusIndicator(
    state: SessionState,
    modifier: Modifier = Modifier,
) {
    val label = when (state) {
        is SessionState.Idle -> "Idle"
        is SessionState.Listening -> "Listening…"
        is SessionState.Processing -> "Thinking…"
        is SessionState.Speaking -> "Speaking…"
        is SessionState.Paused -> "Paused"
        is SessionState.Error -> "Error"
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = when (state) {
            is SessionState.Listening -> MaterialTheme.colorScheme.primary
            is SessionState.Processing -> MaterialTheme.colorScheme.tertiary
            is SessionState.Speaking -> MaterialTheme.colorScheme.secondary
            is SessionState.Error -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier.semantics { contentDescription = label },
    )
}

@Preview
@Composable
private fun VoiceStatusPreview() {
    MaterialTheme {
        VoiceStatusIndicator(state = SessionState.Listening)
    }
}

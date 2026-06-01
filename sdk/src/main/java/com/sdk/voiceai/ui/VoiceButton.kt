package com.sdk.voiceai.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sdk.voiceai.model.SessionState

/**
 * A floating action button that reflects the current [SessionState] of a voice AI session.
 *
 * State changes are announced to accessibility services (TalkBack) via a polite live region.
 * The FAB meets the 56 dp minimum interactive component size required by Material 3.
 *
 * @param state         Current [SessionState]; drives colour, scale, icon and accessibility label.
 * @param onClick       Invoked when the button is tapped (start, stop, interrupt, or retry).
 * @param modifier      Optional [Modifier] applied outside the scale transform.
 * @param activeColor   Container colour while [SessionState.Listening]. Defaults to [MaterialTheme.colorScheme.primary].
 * @param processingColor Container colour while [SessionState.Processing]. Defaults to [MaterialTheme.colorScheme.tertiary].
 * @param speakingColor Container colour while [SessionState.Speaking]. Defaults to [MaterialTheme.colorScheme.secondary].
 * @param idleColor     Container colour for all other states. Defaults to [MaterialTheme.colorScheme.surfaceVariant].
 */
@Immutable
@Composable
fun VoiceButton(
    state: SessionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    processingColor: Color = MaterialTheme.colorScheme.tertiary,
    speakingColor: Color = MaterialTheme.colorScheme.secondary,
    idleColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val isListening = state is SessionState.Listening
    val isProcessing = state is SessionState.Processing
    val isSpeaking = state is SessionState.Speaking

    val containerColor by animateColorAsState(
        targetValue = when {
            isListening -> activeColor
            isProcessing -> processingColor
            isSpeaking -> speakingColor
            else -> idleColor
        },
        animationSpec = tween(300),
        label = "buttonColor",
    )

    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = tween(200),
        label = "buttonScale",
    )

    val description = when (state) {
        is SessionState.Listening -> "Listening — tap to stop"
        is SessionState.Processing -> "Processing your request"
        is SessionState.Speaking -> "Speaking — tap to interrupt"
        is SessionState.Idle, is SessionState.Paused -> "Tap to start voice session"
        is SessionState.Error -> "Error — tap to retry"
    }

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        containerColor = containerColor,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
    ) {
        Icon(
            imageVector = if (isListening) Icons.Filled.Mic else Icons.Filled.MicOff,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Preview
@Composable
private fun VoiceButtonPreview() {
    MaterialTheme {
        VoiceButton(state = SessionState.Listening, onClick = {})
    }
}

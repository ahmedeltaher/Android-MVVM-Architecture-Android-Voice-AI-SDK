package com.sdk.voiceai.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sdk.voiceai.emotion.Emotion
import com.sdk.voiceai.emotion.EmotionResult

private fun emotionColor(emotion: Emotion): Color = when (emotion) {
    Emotion.HAPPY     -> Color(0xFFFFD600)
    Emotion.SAD       -> Color(0xFF1565C0)
    Emotion.ANGRY     -> Color(0xFFD32F2F)
    Emotion.FEARFUL   -> Color(0xFF7B1FA2)
    Emotion.SURPRISED -> Color(0xFFE65100)
    Emotion.DISGUSTED -> Color(0xFF388E3C)
    Emotion.NEUTRAL   -> Color(0xFF9E9E9E)
}

@Composable
fun EmotionIndicator(
    emotionResult: EmotionResult?,
    modifier: Modifier = Modifier,
) {
    if (emotionResult == null) return

    val targetColor = emotionColor(emotionResult.dominantEmotion)
    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(300),
        label = "emotionColor",
    )
    val label = emotionResult.dominantEmotion.name.lowercase().replaceFirstChar { it.uppercase() }

    Row(
        modifier = modifier.semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Preview
@Composable
private fun EmotionIndicatorPreview() {
    MaterialTheme {
        EmotionIndicator(
            emotionResult = EmotionResult(
                dominantEmotion = Emotion.HAPPY,
                scores = mapOf(Emotion.HAPPY to 0.7f, Emotion.NEUTRAL to 0.3f),
                confidence = 0.85f,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

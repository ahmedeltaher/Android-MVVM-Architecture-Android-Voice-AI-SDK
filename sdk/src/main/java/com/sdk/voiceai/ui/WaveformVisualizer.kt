package com.sdk.voiceai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun WaveformVisualizer(
    audioLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 30,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 3.dp,
) {
    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidthPx = barWidth.toPx()
        val gap = (totalWidth - barCount * barWidthPx) / (barCount + 1)
        val centerY = totalHeight / 2f

        for (i in 0 until barCount) {
            val x = gap + i * (barWidthPx + gap)
            // Each bar has a slightly different height to create a wave effect
            val relativePos = i.toFloat() / barCount
            val envelope = 1f - (2 * relativePos - 1f) * (2 * relativePos - 1f)
            val barHeight = (totalHeight * audioLevel * envelope).coerceAtLeast(4f)

            drawLine(
                color = barColor.copy(alpha = 0.7f + 0.3f * envelope),
                start = Offset(x + barWidthPx / 2, centerY - barHeight / 2),
                end = Offset(x + barWidthPx / 2, centerY + barHeight / 2),
                strokeWidth = barWidthPx,
            )
        }
    }
}

@Preview
@Composable
private fun WaveformPreview() {
    MaterialTheme {
        WaveformVisualizer(
            audioLevel = 0.6f,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        )
    }
}

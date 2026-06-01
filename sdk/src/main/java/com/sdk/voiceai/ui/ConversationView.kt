package com.sdk.voiceai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sdk.voiceai.model.ConversationMessage
import com.sdk.voiceai.model.MessageRole

@Composable
fun ConversationView(
    messages: List<ConversationMessage>,
    modifier: Modifier = Modifier,
    partialTranscript: String? = null,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, partialTranscript) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(messages) { message ->
            MessageBubble(message = message)
        }
        partialTranscript?.let { partial ->
            item {
                MessageBubble(
                    message = ConversationMessage(
                        role = MessageRole.USER,
                        content = "$partial…",
                    ),
                    isPartial = true,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ConversationMessage,
    isPartial: Boolean = false,
) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = if (isPartial) 2 else Int.MAX_VALUE,
            )
        }
    }
}

@Preview
@Composable
private fun ConversationViewPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            ConversationView(
                messages = listOf(
                    ConversationMessage(MessageRole.USER, "Hello! How are you?"),
                    ConversationMessage(MessageRole.ASSISTANT, "I'm doing great, thanks for asking! How can I help you today?"),
                ),
                partialTranscript = "Tell me about",
            )
        }
    }
}

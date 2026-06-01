package com.sdk.voiceai.ai

import com.sdk.voiceai.model.ConversationMessage
import com.sdk.voiceai.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
private data class ApiMessage(val role: String, val content: String)

@Serializable
private data class MessagesRequest(
    val model: String,
    val max_tokens: Int,
    val system: String? = null,
    val messages: List<ApiMessage>,
    val stream: Boolean = true,
)

class ClaudeAIEngine(
    private val apiKey: String,
    private val httpClient: OkHttpClient,
    baseUrl: String = "https://api.anthropic.com",
) : AIEngine {

    private val messagesUrl = "${baseUrl.trimEnd('/')}/v1/messages"

    companion object {
        private const val API_VERSION = "2023-06-01"
        private const val MAX_RETRIES = 3
    }

    override fun respond(messages: List<ConversationMessage>, config: AIConfig): Flow<String> = flow {
        val body = MessagesRequest(
            model = config.model,
            max_tokens = config.maxTokens,
            system = config.systemPrompt,
            messages = messages.map {
                ApiMessage(
                    role = if (it.role == MessageRole.USER) "user" else "assistant",
                    content = it.content,
                )
            },
        )

        val requestBody = json.encodeToString(MessagesRequest.serializer(), body)
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(messagesUrl)
            .header("x-api-key", apiKey)
            .header("anthropic-version", API_VERSION)
            .header("content-type", "application/json")
            .post(requestBody)
            .build()

        var attempt = 0
        var delayMs = 1000L
        while (attempt < MAX_RETRIES) {
            val response = httpClient.newCall(request).execute()
            if (response.code == 429) {
                attempt++
                if (attempt < MAX_RETRIES) {
                    Timber.w("Rate limited, retrying in ${delayMs}ms (attempt $attempt)")
                    kotlinx.coroutines.delay(delayMs)
                    delayMs *= 2
                    continue
                } else {
                    throw IOException("Rate limited after $MAX_RETRIES retries")
                }
            }
            if (!response.isSuccessful) {
                throw IOException("Anthropic API error ${response.code}: ${response.body?.string()}")
            }

            val responseBody = response.body ?: throw IOException("Empty response body")
            val fullText = StringBuilder()

            responseBody.source().use { source ->
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break

                    try {
                        val event = json.parseToJsonElement(data)
                        val type = event.jsonObject["type"]?.jsonPrimitive?.content
                        if (type == "content_block_delta") {
                            val delta = event.jsonObject["delta"]?.jsonObject
                            val text = delta?.get("text")?.jsonPrimitive?.content ?: continue
                            fullText.append(text)
                            emit(text)
                        }
                    } catch (e: Exception) {
                        Timber.w("SSE parse skip: $data")
                    }
                }
            }
            Timber.d("Claude response complete (${fullText.length} chars)")
            return@flow
        }
    }
}

private val kotlinx.serialization.json.JsonElement.jsonObject
    get() = this as kotlinx.serialization.json.JsonObject
private val kotlinx.serialization.json.JsonElement.jsonPrimitive
    get() = this as kotlinx.serialization.json.JsonPrimitive

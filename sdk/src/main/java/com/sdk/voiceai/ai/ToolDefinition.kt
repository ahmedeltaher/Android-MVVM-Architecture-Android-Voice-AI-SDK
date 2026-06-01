package com.sdk.voiceai.ai

data class ToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true,
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>,
)

data class ToolCallRequest(
    val toolName: String,
    val arguments: Map<String, String>,
)

data class ToolCallResult(
    val toolName: String,
    val result: String,
)

fun interface ToolCallHandler {
    suspend fun onToolCall(request: ToolCallRequest): ToolCallResult
}

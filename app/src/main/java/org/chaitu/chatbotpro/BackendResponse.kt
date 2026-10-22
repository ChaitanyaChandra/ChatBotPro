package org.chaitu.chatbotpro

data class BackendResponse(
    val text: String,
    val thinkingText: String? = null,
    val toolCalls: List<ExternalToolCall> = emptyList()
)

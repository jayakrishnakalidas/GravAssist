package com.gravassist.api

import com.google.gson.annotations.SerializedName

data class ChatMessageDto(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatCompletionRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessageDto>,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("max_tokens") val maxTokens: Int = 500
)

data class ChatCompletionChoice(
    @SerializedName("index") val index: Int,
    @SerializedName("message") val message: ChatMessageDto
)

data class UsageDto(
    @SerializedName("prompt_tokens") val promptTokens: Int = 0,
    @SerializedName("completion_tokens") val completionTokens: Int = 0,
    @SerializedName("total_tokens") val totalTokens: Int = 0
)

data class ChatCompletionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("choices") val choices: List<ChatCompletionChoice>,
    @SerializedName("usage") val usage: UsageDto?
)

data class ModelItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("object") val obj: String?
)

data class ModelsResponse(
    @SerializedName("data") val data: List<ModelItemDto>
)

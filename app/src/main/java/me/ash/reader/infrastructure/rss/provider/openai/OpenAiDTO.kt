package me.ash.reader.infrastructure.rss.provider.openai

import com.google.gson.annotations.SerializedName

object OpenAiDTO {
    data class ModelList(
        val data: List<Model>
    )

    data class Model(
        val id: String,
        @SerializedName("object")
        val objectType: String,
        val owned_by: String,
        val permission: List<Any>
    )

    data class ChatCompletionRequest(
        val model: String,
        val messages: List<Message>,
        val stream: Boolean = false
    )

    data class Message(
        val role: String,
        val content: String
    )

    data class ChatCompletionResponse(
        val id: String,
        @SerializedName("object")
        val objectType: String,
        val created: Long,
        val model: String,
        val choices: List<Choice>,
        val usage: Usage
    )

    data class Choice(
        val index: Int,
        val message: Message,
        @SerializedName("finish_reason")
        val finish_reason: String
    )

    data class Usage(
        val prompt_tokens: Int,
        val completion_tokens: Int,
        val total_tokens: Int
    )
}
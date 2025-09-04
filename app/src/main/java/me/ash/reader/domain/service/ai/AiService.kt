package me.ash.reader.domain.service.ai

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.withTimeout
import me.ash.reader.domain.data.SyncLogger
import me.ash.reader.infrastructure.di.USER_AGENT_STRING
import me.ash.reader.infrastructure.preference.AiCredentials
import me.ash.reader.infrastructure.rss.provider.openai.OpenAiDTO
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.executeAsync
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val syncLogger: SyncLogger,
) {

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = "You are an assistant in an RSS reader app, summarizing article content. Provide summaries in the article's language if 99% recognizable; otherwise, use English. Keep summaries up to 100 words, 3 paragraphs, with up to 3 bullet points per paragraph. For readability use bullet points, titles, quotes and new lines using Markdown only. Use only single language. Keep full quotes if any. Output should be in Markdown. Use proper Markdown syntax for all formatting elements."
    }
    private val gson = Gson()

    suspend fun listModels(credentials: AiCredentials): Result<List<String>> {
        return runCatching {
            withTimeout(credentials.timeout.toLongOrNull()?.times(1000) ?: 30000) {
                val request = Request.Builder()
                    .url("${credentials.baseUrl.trimEnd('/')}/models")
                    .header("Authorization", "Bearer ${credentials.apiKey}")
                    .header("User-Agent", USER_AGENT_STRING)
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).executeAsync()
                if (!response.isSuccessful) {
                    throw Exception("Failed to fetch models: ${response.code} ${response.message}")
                }
                val body = response.body.string()
                val modelList = gson.fromJson(body, OpenAiDTO.ModelList::class.java)
                modelList.data.map { it.id }
            }
        }.onFailure {
            syncLogger.log(it)
        }
    }

    suspend fun getSummary(credentials: AiCredentials, content: String): Result<String> {
        return runCatching {
              withTimeout(credentials.timeout.toLongOrNull()?.times(1000) ?: 30000) {
                  val systemPrompt = if (credentials.systemPrompt.isNotBlank()) {
                      credentials.systemPrompt
                  } else {
                      DEFAULT_SYSTEM_PROMPT
                  }
                 val requestBody = OpenAiDTO.ChatCompletionRequest(
                                     model = credentials.modelId,
                                     messages = listOf(
                                         OpenAiDTO.Message("system", systemPrompt),
                                         OpenAiDTO.Message("user", content)
                                     )
                                 )
                val jsonBody = gson.toJson(requestBody)

                val request = Request.Builder()
                    .url("${credentials.baseUrl.trimEnd('/')}/chat/completions")
                    .header("Authorization", "Bearer ${credentials.apiKey}")
                    .header("User-Agent", USER_AGENT_STRING)
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).executeAsync()
                if (!response.isSuccessful) {
                    throw Exception("Failed to get summary: ${response.code} ${response.message} ${response.body.string()}")
                }
                val body = response.body.string()
                val completionResponse = gson.fromJson(body, OpenAiDTO.ChatCompletionResponse::class.java)
                val summary = completionResponse.choices.firstOrNull()?.message?.content ?: ""
                Log.d("AiService", "Generated summary: $summary")
                summary
            }
        }.onFailure {
            syncLogger.log(it)
        }
    }
}

package me.ash.reader.infrastructure.preference

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.ash.reader.domain.model.account.security.DESUtils
import me.ash.reader.ui.ext.dataStore
import me.ash.reader.ui.ext.DataStoreKey

private const val DEFAULT_SYSTEM_PROMPT = "You are an assistant in an RSS reader app, summarizing article content. Provide summaries in the article's language if 99% recognizable; otherwise, use English. Keep summaries up to 100 words, 3 paragraphs, with up to 3 bullet points per paragraph. For readability use bullet points, titles, quotes and new lines using Markdown only. Use only single language. Keep full quotes if any. Output should be in Markdown. Use proper Markdown syntax for all formatting elements."

val LocalAiCredentials = compositionLocalOf { AiCredentialsPreference.default }

data class AiCredentials(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1/",
    val timeout: String = "30",
    val modelId: String = "",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
)

object AiCredentialsPreference {
    val default = AiCredentials()

    fun put(context: Context, scope: CoroutineScope, value: AiCredentials) {
        scope.launch {
            context.dataStore.edit {
                it[DataStoreKey.keys[DataStoreKey.aiApiKey]!!.key as Preferences.Key<String>] = if(value.apiKey.isNotBlank()) DESUtils.encrypt(value.apiKey) else ""
                it[DataStoreKey.keys[DataStoreKey.aiBaseUrl]!!.key as Preferences.Key<String>] = value.baseUrl
                it[DataStoreKey.keys[DataStoreKey.aiTimeout]!!.key as Preferences.Key<String>] = value.timeout
                it[DataStoreKey.keys[DataStoreKey.aiModelId]!!.key as Preferences.Key<String>] = value.modelId
                it[DataStoreKey.keys[DataStoreKey.aiSystemPrompt]!!.key as Preferences.Key<String>] = value.systemPrompt
            }
        }
    }

    fun fromPreferences(preferences: Preferences): AiCredentials {
        val apiKeyEncrypted = preferences[DataStoreKey.keys[DataStoreKey.aiApiKey]!!.key as Preferences.Key<String>] ?: ""
        val apiKey = if (apiKeyEncrypted.isNotEmpty()) {
            runCatching { DESUtils.decrypt(apiKeyEncrypted) }.getOrDefault("")
        } else ""
        val baseUrl = preferences[DataStoreKey.keys[DataStoreKey.aiBaseUrl]!!.key as Preferences.Key<String>] ?: "https://api.openai.com/v1/"
        val timeout = preferences[DataStoreKey.keys[DataStoreKey.aiTimeout]!!.key as Preferences.Key<String>] ?: "30"
        val modelId = preferences[DataStoreKey.keys[DataStoreKey.aiModelId]!!.key as Preferences.Key<String>] ?: ""
        val systemPrompt = preferences[DataStoreKey.keys[DataStoreKey.aiSystemPrompt]!!.key as Preferences.Key<String>] ?: DEFAULT_SYSTEM_PROMPT
        return AiCredentials(apiKey, baseUrl, timeout, modelId, systemPrompt)
    }
}
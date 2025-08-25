package me.ash.reader.ui.page.settings.ai

import android.util.Log

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ash.reader.domain.service.ai.AiService
import me.ash.reader.infrastructure.preference.AiCredentials
import me.ash.reader.infrastructure.preference.AiCredentialsPreference
import me.ash.reader.infrastructure.preference.SettingsProvider
import javax.inject.Inject

data class AiCredentialsUiState(
    val credentials: AiCredentials = AiCredentials(),
    val models: List<String> = emptyList(),
    val isLoadingModels: Boolean = false
)

@HiltViewModel
class AiCredentialsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider,
    private val aiService: AiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiCredentialsUiState())
    val uiState = _uiState.asStateFlow()

    private var fetchModelsJob: Job? = null

    init {
        settingsProvider.settingsFlow
            .onEach { settings ->
                _uiState.update { it.copy(credentials = settings.aiCredentials) }
            }
            .debounce(500)
            .distinctUntilChanged { old, new ->
                old.aiCredentials.apiKey == new.aiCredentials.apiKey && old.aiCredentials.baseUrl == new.aiCredentials.baseUrl
            }
            .onEach {
                fetchModels()
            }
            .launchIn(viewModelScope)
    }

    private fun fetchModels() {
        fetchModelsJob?.cancel()
        val credentials = _uiState.value.credentials
        if (credentials.apiKey.isBlank() || credentials.baseUrl.isBlank()) {
            _uiState.update { it.copy(models = emptyList()) }
            return
        }
        _uiState.update { it.copy(isLoadingModels = true) }
        fetchModelsJob = viewModelScope.launch {
            aiService.listModels(credentials)
                .onSuccess { models ->
                    Log.d("AiCredentialsViewModel", "Fetched models: $models")
                    _uiState.update {
                        val currentModel = it.credentials.modelId
                        it.copy(
                            models = models,
                            isLoadingModels = false,
                            credentials = it.credentials.copy(
                                modelId = if (currentModel in models) currentModel else models.firstOrNull() ?: ""
                            )
                        )
                    }
                    AiCredentialsPreference.put(context, viewModelScope, _uiState.value.credentials)
                }
                .onFailure {
                    _uiState.update { it.copy(models = emptyList(), isLoadingModels = false) }
                }
        }
    }

    fun updateApiKey(apiKey: String) {
        val currentCredentials = _uiState.value.credentials
        if (currentCredentials.apiKey != apiKey) {
            _uiState.update { it.copy(credentials = it.credentials.copy(apiKey = apiKey)) }
            saveCredentials()
        }
    }

    fun updateBaseUrl(baseUrl: String) {
        val currentCredentials = _uiState.value.credentials
        if (currentCredentials.baseUrl != baseUrl) {
            _uiState.update { it.copy(credentials = it.credentials.copy(baseUrl = baseUrl)) }
            saveCredentials()
        }
    }

    fun updateTimeout(timeout: String) {
        val currentCredentials = _uiState.value.credentials
        if (currentCredentials.timeout != timeout) {
            _uiState.update { it.copy(credentials = it.credentials.copy(timeout = timeout)) }
            saveCredentials()
        }
    }

    fun updateModelId(modelId: String) {
        val currentCredentials = _uiState.value.credentials
        if (currentCredentials.modelId != modelId) {
            _uiState.update { it.copy(credentials = it.credentials.copy(modelId = modelId)) }
            saveCredentials()
        }
    }

    private fun saveCredentials() {
        AiCredentialsPreference.put(
            context,
            viewModelScope,
            _uiState.value.credentials
        )
    }
}

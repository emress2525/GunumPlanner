package com.unifiedai.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedai.app.core.model.ModelDescriptor
import com.unifiedai.app.core.network.HttpClientFactory
import com.unifiedai.app.core.security.SecretStore
import com.unifiedai.app.data.provider.cloud.ollamaBaseUrlOrNull
import com.unifiedai.app.data.provider.ollama.OllamaProvider
import com.unifiedai.app.data.provider.local.LocalModelInstallState
import com.unifiedai.app.data.provider.local.LocalModelController
import com.unifiedai.app.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val savedSecrets: Set<String> = emptySet(),
    val ollamaStatus: String? = null,
    val ollamaModels: List<ModelDescriptor> = emptyList(),
    val localModelState: LocalModelInstallState = LocalModelInstallState.Missing,
    val error: String? = null,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val secretStore: SecretStore,
    private val localModelManager: LocalModelController,
) : ViewModel() {
    private val mutable = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            localModelManager.state.collect { modelState ->
                mutable.update { it.copy(localModelState = modelState) }
            }
        }
        viewModelScope.launch {
            repository.settings.collect { settings ->
                mutable.update { it.copy(settings = settings, savedSecrets = providerIds.filter(secretStore::contains).toSet()) }
            }
        }
    }

    fun downloadLocalModel() {
        viewModelScope.launch {
            mutable.update { it.copy(error = null) }
            localModelManager.download()
            if (localModelManager.state.value is LocalModelInstallState.Error) {
                val error = localModelManager.state.value as LocalModelInstallState.Error
                mutable.update { it.copy(error = error.message) }
            }
        }
    }

    fun deleteLocalModel() {
        viewModelScope.launch {
            localModelManager.delete()
            mutable.update { it.copy(error = null) }
        }
    }

    fun setOllamaUrl(url: String) = launchUpdate { repository.setOllamaUrl(url) }
    fun setCloudAllowed(value: Boolean) = launchUpdate { repository.setCloudAllowed(value) }
    fun setHistoryEnabled(value: Boolean) = launchUpdate { repository.setHistoryEnabled(value) }
    fun setMemoryEnabled(value: Boolean) = launchUpdate { repository.setMemoryEnabled(value) }

    fun setProviderPolicy(id: String, policy: ProviderPolicy) = launchUpdate { repository.setProviderPolicy(id, policy) }

    fun saveSecret(id: String, value: String) {
        if (id !in providerIds) return
        val secret = value.trim()
        if (secret.isBlank()) return
        secretStore.put(id, secret)
        mutable.update { it.copy(savedSecrets = providerIds.filter(secretStore::contains).toSet()) }
    }

    fun removeSecret(id: String) {
        secretStore.remove(id)
        mutable.update { it.copy(savedSecrets = providerIds.filter(secretStore::contains).toSet()) }
    }

    fun testOllama() {
        viewModelScope.launch {
            mutable.update { it.copy(ollamaStatus = "Bağlantı deneniyor…", error = null) }
            runCatching {
                val current = repository.current()
                val ollamaUrl = ollamaBaseUrlOrNull(current.ollamaUrl)
                    ?: error("Ollama adresi http:// veya https:// ile başlamalı.")
                val provider = OllamaProvider(ollamaUrl, HttpClientFactory.create())
                val models = provider.listModels()
                mutable.update { it.copy(ollamaStatus = "Bağlandı • ${models.size} model", ollamaModels = models) }
            }.onFailure { t ->
                mutable.update { it.copy(ollamaStatus = "Bağlantı başarısız", error = t.message ?: "Ollama erişilemiyor") }
            }
        }
    }

    private fun launchUpdate(block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() }.onFailure { t -> mutable.update { it.copy(error = t.message) } } }
    }

    private companion object {
        val providerIds = listOf("gemini", "openai", "anthropic", "deepseek")
    }
}

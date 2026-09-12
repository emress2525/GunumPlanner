package com.unifiedai.app.data.provider.cloud

import com.unifiedai.app.core.model.*
import com.unifiedai.app.core.security.SecretStore
import com.unifiedai.app.data.provider.ollama.OllamaProvider
import com.unifiedai.app.data.repository.*
import com.unifiedai.app.domain.provider.AiProvider
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient

internal fun ollamaBaseUrlOrNull(raw: String): HttpUrl? =
    raw.trim().takeIf { it.isNotEmpty() }?.toHttpUrlOrNull()

class ProviderFactory(
    private val settingsRepository: SettingsRepository,
    private val secretStore: SecretStore,
    private val usageRepository: ProviderUsageRepository,
    private val httpClient: OkHttpClient,
    private val onDeviceProvider: AiProvider? = null,
) {
    suspend fun create(mode: ChatMode): Map<String, AiProvider> {
        val settings = settingsRepository.current()
        val result = linkedMapOf<String, AiProvider>()
        onDeviceProvider?.let { result[it.providerId] = it }
        ollamaBaseUrlOrNull(settings.ollamaUrl)?.let { ollamaUrl ->
            result["ollama"] = OllamaProvider(ollamaUrl, httpClient)
        }
        if (mode == ChatMode.LOCAL_ONLY || !settings.cloudAllowed) return result

        addOpenAi(result, settings, mode)
        addGemini(result, settings, mode)
        addAnthropic(result, settings, mode)
        addDeepSeek(result, settings, mode)
        return result
    }

    private fun eligible(policy: ProviderPolicy, mode: ChatMode): Boolean =
        policy.enabled && (mode != ChatMode.COUNCIL || policy.allowInCouncil)

    private fun addOpenAi(result: MutableMap<String, AiProvider>, settings: AppSettings, mode: ChatMode) {
        if (!eligible(settings.openAi, mode)) return
        val key = secretStore.get("openai") ?: return
        val raw = OpenAiCompatibleProvider(
            providerId = "openai",
            displayName = "OpenAI",
            baseUrl = "https://api.openai.com/".toHttpUrl(),
            apiKey = key,
            models = emptyList(),
            client = httpClient,
        )
        result[raw.providerId] = GuardedProvider(raw, settings.openAi.dailyRequestLimit, usageRepository)
    }

    private fun addGemini(result: MutableMap<String, AiProvider>, settings: AppSettings, mode: ChatMode) {
        if (!eligible(settings.gemini, mode)) return
        val key = secretStore.get("gemini") ?: return
        val raw = GeminiProvider(
            baseUrl = "https://generativelanguage.googleapis.com/".toHttpUrl(),
            apiKey = key,
            configuredModels = emptyList(),
            client = httpClient,
        )
        result[raw.providerId] = GuardedProvider(raw, settings.gemini.dailyRequestLimit, usageRepository)
    }

    private fun addAnthropic(result: MutableMap<String, AiProvider>, settings: AppSettings, mode: ChatMode) {
        if (!eligible(settings.anthropic, mode)) return
        val key = secretStore.get("anthropic") ?: return
        val raw = AnthropicProvider(
            baseUrl = "https://api.anthropic.com/".toHttpUrl(),
            apiKey = key,
            configuredModels = emptyList(),
            client = httpClient,
        )
        result[raw.providerId] = GuardedProvider(raw, settings.anthropic.dailyRequestLimit, usageRepository)
    }

    private fun addDeepSeek(result: MutableMap<String, AiProvider>, settings: AppSettings, mode: ChatMode) {
        if (!eligible(settings.deepSeek, mode)) return
        val key = secretStore.get("deepseek") ?: return
        val raw = createDeepSeekProvider(
            baseUrl = "https://api.deepseek.com/".toHttpUrl(),
            apiKey = key,
            models = emptyList(),
            client = httpClient,
        )
        result[raw.providerId] = GuardedProvider(raw, settings.deepSeek.dailyRequestLimit, usageRepository)
    }
}

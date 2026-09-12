package com.unifiedai.app.data.provider.ondevice

import com.unifiedai.app.core.model.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class OnDeviceProviderTest {
    @Test fun readyRuntime_exposesCodingCapableLocalModel_andStreams() = runTest {
        val runtime = object : OnDeviceRuntime {
            override val ready: Boolean = true
            override suspend fun generate(prompt: String): String = "kod cevabı"
            override fun stream(prompt: String) = flowOf("kod ", "cevabı")
            override suspend fun cancel() = Unit
        }
        val provider = OnDeviceProvider(runtime)

        assertEquals(ProviderHealth.Healthy, provider.healthCheck())
        val model = provider.listModels().single()
        assertTrue(model.isLocal)
        assertTrue(ModelCapability.CODING in model.capabilities)

        val request = ChatRequest("r1", listOf(ChatMessage("u1", MessageRole.USER, "kod yaz")))
        val chunks = provider.stream(request, model).toList()
        assertEquals("kod cevabı", chunks.filterNot { it.isFinal }.joinToString("") { it.textDelta })
        assertTrue(chunks.last().isFinal)
    }

    @Test fun missingRuntime_isUnhealthy_andDoesNotAdvertiseModel() = runTest {
        val runtime = object : OnDeviceRuntime {
            override val ready: Boolean = false
            override suspend fun generate(prompt: String): String = error("should not run")
            override fun stream(prompt: String) = flowOf<String>()
            override suspend fun cancel() = Unit
        }
        val provider = OnDeviceProvider(runtime)
        assertTrue(provider.healthCheck() is ProviderHealth.Unhealthy)
        assertTrue(provider.listModels().isEmpty())
    }
}

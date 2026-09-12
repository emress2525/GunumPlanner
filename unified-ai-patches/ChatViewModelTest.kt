package com.unifiedai.app.feature.chat

import com.unifiedai.app.core.model.*
import com.unifiedai.app.data.repository.*
import com.unifiedai.app.domain.chat.*
import com.unifiedai.app.domain.orchestration.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun blankInput_isIgnored() = runTest(dispatcher) {
        val engine = FakeEngine()
        val vm = newViewModel(engine)
        advanceUntilIdle()

        vm.setInput("   ")
        vm.send()
        advanceUntilIdle()

        assertEquals(0, engine.streamCalls)
    }

    @Test fun send_streamsIntoSingleAssistantDraft_andCompletes() = runTest(dispatcher) {
        val engine = FakeEngine(
            streamFactory = { request -> flow {
                emit(OrchestrationStreamEvent.Stage("Yanıt oluşturuluyor"))
                emit(OrchestrationStreamEvent.Delta("A", "local", "m1"))
                emit(OrchestrationStreamEvent.Delta("B", "local", "m1"))
                emit(OrchestrationStreamEvent.Completed(completed(request.requestId, "AB")))
            } },
        )
        val repo = FakeChatRepository()
        val vm = newViewModel(engine, repo)
        advanceUntilIdle()

        vm.setInput("merhaba")
        vm.send()
        advanceUntilIdle()

        assertEquals(0, engine.executeCalls)
        assertEquals(1, engine.streamCalls)
        val messages = vm.state.value.messages
        assertEquals(1, messages.count { it.role == MessageRole.USER })
        assertEquals(1, messages.count { it.role == MessageRole.ASSISTANT })
        val assistant = messages.single { it.role == MessageRole.ASSISTANT }
        assertEquals("AB", assistant.text)
        assertEquals("local", assistant.providerId)
        assertEquals("m1", assistant.modelId)
        assertTrue(assistant.completed)
    }

    @Test fun stop_keepsPartialDraftIncomplete_andCancelsProvider() = runTest(dispatcher) {
        val engine = FakeEngine(
            streamFactory = { flow {
                emit(OrchestrationStreamEvent.Delta("kısmi", "local", "m1"))
                awaitCancellation()
            } },
        )
        val vm = newViewModel(engine)
        advanceUntilIdle()
        vm.setInput("uzun cevap")
        vm.send()
        runCurrent()

        vm.stop()
        advanceUntilIdle()

        val assistant = vm.state.value.messages.single { it.role == MessageRole.ASSISTANT }
        assertEquals("kısmi", assistant.text)
        assertFalse(assistant.completed)
        assertEquals(1, engine.cancelCalls)
        assertFalse(vm.state.value.isGenerating)
    }

    @Test fun failure_preservesUserMessage_andRetrySucceeds() = runTest(dispatcher) {
        var attempt = 0
        val engine = FakeEngine(
            streamFactory = { request -> flow {
                attempt++
                if (attempt == 1) error("provider down")
                emit(OrchestrationStreamEvent.Completed(completed(request.requestId, "geri geldi")))
            } },
        )
        val vm = newViewModel(engine)
        advanceUntilIdle()
        vm.setInput("soru")
        vm.send()
        advanceUntilIdle()

        assertTrue(vm.state.value.canRetry)
        assertEquals(1, vm.state.value.messages.count { it.role == MessageRole.USER })

        vm.retry()
        advanceUntilIdle()

        assertFalse(vm.state.value.canRetry)
        assertEquals("geri geldi", vm.state.value.messages.last { it.role == MessageRole.ASSISTANT }.text)
        assertEquals(2, engine.streamCalls)
    }

    @Test fun defaultMode_isRestoredFromSettings() = runTest(dispatcher) {
        val vm = newViewModel(
            engine = FakeEngine(),
            settings = FakeSettingsRepository(AppSettings(defaultMode = ChatMode.COUNCIL)),
        )
        advanceUntilIdle()
        assertEquals(ChatMode.COUNCIL, vm.state.value.mode)
    }

    private fun newViewModel(
        engine: FakeEngine,
        repo: FakeChatRepository = FakeChatRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ) = ChatViewModel(
        engine,
        repo,
        settings,
        object : AttachmentLoader {
            override suspend fun load(uri: String): AttachmentExtraction =
                AttachmentExtraction.Rejected("unused")
        },
    )

    private fun completed(requestId: String, text: String): OrchestrationResult {
        val model = ModelDescriptor(
            providerId = "local",
            modelId = "m1",
            displayName = "m1",
            capabilities = setOf(ModelCapability.CHAT),
            isLocal = true,
        )
        return OrchestrationResult(
            finalText = text,
            contributions = emptyList(),
            status = OrchestrationStatus.COMPLETED,
            usedModels = listOf(model),
        )
    }

    private class FakeEngine(
        private val streamFactory: (ChatRequest) -> Flow<OrchestrationStreamEvent> = { request ->
            flow { emit(OrchestrationStreamEvent.Completed(completedStatic(request.requestId, "ok"))) }
        },
    ) : ChatEngine {
        var executeCalls = 0
        var streamCalls = 0
        var cancelCalls = 0

        override suspend fun execute(request: ChatRequest, mode: ChatMode): OrchestrationResult {
            executeCalls++
            error("ChatViewModel must use stream()")
        }

        override fun stream(request: ChatRequest, mode: ChatMode): Flow<OrchestrationStreamEvent> {
            streamCalls++
            return streamFactory(request)
        }

        override suspend fun cancel(requestId: String) {
            cancelCalls++
        }

        companion object {
            private fun completedStatic(requestId: String, text: String): OrchestrationResult {
                val model = ModelDescriptor("local", "m1", "m1", setOf(ModelCapability.CHAT), true)
                return OrchestrationResult(text, emptyList(), OrchestrationStatus.COMPLETED, listOf(model))
            }
        }
    }

    private class FakeChatRepository : ChatRepository {
        private val messages = ConcurrentHashMap<String, MutableStateFlow<List<StoredMessage>>>()
        private val conversations = MutableStateFlow<List<ConversationSummary>>(emptyList())

        override fun observeConversations(): Flow<List<ConversationSummary>> = conversations
        override fun observeMessages(conversationId: String): Flow<List<StoredMessage>> =
            messages.computeIfAbsent(conversationId) { MutableStateFlow(emptyList()) }

        override suspend fun createConversation(id: String, title: String, mode: ChatMode, now: Long) {
            messages.computeIfAbsent(id) { MutableStateFlow(emptyList()) }
            conversations.value = conversations.value + ConversationSummary(id, title, mode, now)
        }

        override suspend fun appendMessage(message: StoredMessage) {
            val flow = messages.computeIfAbsent(message.conversationId) { MutableStateFlow(emptyList()) }
            flow.value = flow.value + message
        }

        override suspend fun updateAssistantMessage(message: StoredMessage) {
            val flow = messages.computeIfAbsent(message.conversationId) { MutableStateFlow(emptyList()) }
            flow.value = flow.value.map { if (it.id == message.id) message else it }
        }

        override suspend fun updateConversationTitle(conversationId: String, title: String, now: Long) {
            conversations.value = conversations.value.map {
                if (it.id == conversationId) it.copy(title = title, updatedAt = now) else it
            }
        }

        override suspend fun deleteConversation(conversationId: String) {
            messages.remove(conversationId)
            conversations.value = conversations.value.filterNot { it.id == conversationId }
        }
    }

    private class FakeSettingsRepository(initial: AppSettings = AppSettings(defaultMode = ChatMode.LOCAL_ONLY)) : SettingsRepository {
        private val state = MutableStateFlow(initial)
        override val settings: Flow<AppSettings> = state
        override suspend fun current(): AppSettings = state.value
        override suspend fun setOllamaUrl(url: String) { state.value = state.value.copy(ollamaUrl = url) }
        override suspend fun setDefaultMode(mode: ChatMode) { state.value = state.value.copy(defaultMode = mode) }
        override suspend fun setHistoryEnabled(enabled: Boolean) { state.value = state.value.copy(historyEnabled = enabled) }
        override suspend fun setMemoryEnabled(enabled: Boolean) { state.value = state.value.copy(memoryEnabled = enabled) }
        override suspend fun setCloudAllowed(enabled: Boolean) { state.value = state.value.copy(cloudAllowed = enabled) }
        override suspend fun setProviderPolicy(providerId: String, policy: ProviderPolicy) = Unit
    }
}

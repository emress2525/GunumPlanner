package com.unifiedai.app.data.provider.local

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LiteRtLmRuntime(
    private val context: Context,
    private val modelPath: String,
) : LocalInferenceRuntime {
    private val mutex = Mutex()
    @Volatile private var engine: Engine? = null

    override suspend fun generate(prompt: String): String = mutex.withLock {
        withContext(Dispatchers.Default) {
            val currentEngine = engine ?: createEngine().also { engine = it }
            val config = ConversationConfig(
                systemInstruction = Contents.of(Content.Text("Kullanıcının dilinde yardımcı, doğru ve kısa yanıt ver.")),
                samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.7),
            )
            currentEngine.createConversation(config).use { conversation ->
                conversation.sendMessage(prompt).toString()
            }
        }
    }

    private fun createEngine(): Engine {
        return Engine(
            EngineConfig(
                modelPath = modelPath,
                backend = Backend.CPU(),
                maxNumTokens = 1280,
                cacheDir = context.cacheDir.absolutePath,
            ),
        ).also { it.initialize() }
    }
}

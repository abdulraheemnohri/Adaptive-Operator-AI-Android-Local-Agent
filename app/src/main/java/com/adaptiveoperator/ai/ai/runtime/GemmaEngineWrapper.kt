package com.adaptiveoperator.ai.ai.runtime

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LiteRtLmJniException
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one and only AI model in this app (see the project's Hard Architecture Rule).
 * Thin, lifecycle-aware wrapper around `com.google.ai.edge.litertlm.Engine` /
 * `Conversation` -- the real Kotlin API shipped by google-ai-edge/LiteRT-LM
 * (https://ai.google.dev/edge/litert-lm/android), not a hypothetical one.
 *
 * Responsibilities kept deliberately narrow:
 *  - load/unload the engine against a verified, installed .litertlm file
 *  - pick a backend and step down NPU -> GPU -> CPU if native init throws
 *  - expose a single active Conversation for ContextBuilder / AgentOrchestrator to drive
 *
 * Tool calling, planning, and multi-turn task state live in the `agent` package --
 * this class only knows how to talk to Gemma, not what Gemma's answers mean.
 */
@Singleton
class GemmaEngineWrapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lock = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    var activeBackend: InferenceBackend = InferenceBackend.AUTOMATIC
        private set

    val isLoaded: Boolean
        get() = engine != null

    /**
     * Loads [modelPath] and opens a fresh Conversation. Safe to call again after
     * [unload]; the previous engine (if any) is closed first. Per the docs,
     * engine.initialize() can take several seconds on a cold cache -- callers should
     * show the "Preparing Operator..." warm-up UI (Section 13) while awaiting this.
     */
    suspend fun load(modelPath: String, config: GenerationConfig) = lock.withLock {
        unloadLocked()

        val backendOrder: List<InferenceBackend> = when (config.backend) {
            InferenceBackend.AUTOMATIC -> listOf(InferenceBackend.NPU, InferenceBackend.GPU, InferenceBackend.CPU)
            else -> listOf(config.backend, InferenceBackend.CPU)
        }

        var lastError: Exception? = null
        for (candidate in backendOrder.distinct()) {
            try {
                val nativeBackend = when (candidate) {
                    InferenceBackend.NPU -> Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
                    InferenceBackend.GPU -> Backend.GPU()
                    InferenceBackend.CPU, InferenceBackend.AUTOMATIC -> Backend.CPU()
                }

                val engineConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = nativeBackend,
                    maxNumTokens = config.contextLength
                )

                val newEngine = Engine(engineConfig)
                newEngine.initialize()

                val samplerConfig = SamplerConfig(
                    config.topK,
                    config.topP.toDouble(),
                    config.temperature.toDouble(),
                    0
                )
                val convConfig = ConversationConfig(
                    samplerConfig = samplerConfig
                )
                val newConversation = newEngine.createConversation(convConfig)

                engine = newEngine
                conversation = newConversation
                activeBackend = candidate
                return@withLock
            } catch (e: LiteRtLmJniException) {
                lastError = e // NPU/GPU init genuinely failed on this device -- fall through to the next tier
            } catch (e: IllegalStateException) {
                lastError = e
            }
        }
        throw IllegalStateException("Failed to initialize LiteRT-LM engine on any backend", lastError)
    }

    /** Streams the model's response token-by-token via the Conversation Flow API. */
    fun sendMessage(text: String): Flow<String> {
        val conv = conversation ?: throw IllegalStateException("Engine not loaded -- call load() first")
        return conv.sendMessageAsync(Message.of(text)).map { msg ->
            msg.contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }
        }
    }

    suspend fun unload() = lock.withLock { unloadLocked() }

    private fun unloadLocked() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
    }
}

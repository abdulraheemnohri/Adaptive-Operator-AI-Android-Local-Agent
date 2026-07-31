package com.adaptiveoperator.ai.ai.runtime

enum class ModelLifecyclePolicy { AUTOMATIC, KEEP_LOADED, UNLOAD_AFTER_30S, UNLOAD_AFTER_1M, UNLOAD_AFTER_5M }

/**
 * Section 12 recommended defaults: Automatic backend, Thinking ON, temperature 1.0,
 * top-p 0.95, top-k 64, Automatic lifecycle. Exposed as one immutable value class so
 * Settings can persist/restore it as a single DataStore entry.
 */
data class GenerationConfig(
    val backend: InferenceBackend = InferenceBackend.AUTOMATIC,
    val temperature: Float = 1.0f,
    val topP: Float = 0.95f,
    val topK: Int = 64,
    val thinkingEnabled: Boolean = true,
    val maxOutputTokens: Int = 1024,
    val contextLength: Int = 32_768,
    val visualTokenBudget: Int = 256,
    val sessionTimeoutSeconds: Int = 120,
    val lifecyclePolicy: ModelLifecyclePolicy = ModelLifecyclePolicy.AUTOMATIC,
    val thermalProtectionEnabled: Boolean = true
)

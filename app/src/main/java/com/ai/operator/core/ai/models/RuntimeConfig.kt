package com.ai.operator.core.ai.models

enum class BackendType {
    AUTOMATIC, NPU, GPU, CPU
}

enum class ModelLifecyclePolicy {
    AUTOMATIC, KEEP_WARM, TIMEOUT_30S, TIMEOUT_5M
}

enum class ModelStatus {
    UNINSTALLED, DOWNLOADING, VERIFYING, COMPATIBILITY_FAILED, INSTALLED, READY, LOADING, OPERATIONAL, ERROR
}

data class RuntimeConfig(
    val backend: BackendType = BackendType.AUTOMATIC,
    val threads: Int = 4,
    val contextLength: Int = 128000,
    val maxOutputTokens: Int = 2048,
    val temperature: Float = 1.0f,
    val topP: Float = 0.95f,
    val topK: Int = 64,
    val isThinkingModeEnabled: Boolean = true,
    val visualTokenBudget: Int = 512,
    val modelLifecycle: ModelLifecyclePolicy = ModelLifecyclePolicy.AUTOMATIC
)

data class RuntimeInfo(
    val modelId: String,
    val status: ModelStatus,
    val activeBackend: BackendType,
    val averageLatencyMs: Double,
    val tokensPerSecond: Float,
    val allocatedMemoryMb: Long
)

data class ResourceStatus(
    val cpuUsagePercent: Float,
    val gpuUsagePercent: Float,
    val ramUsedMb: Long,
    val systemTemperatureCelsius: Float,
    val batteryLevelPercent: Int,
    val isThermalThrottling: Boolean
)

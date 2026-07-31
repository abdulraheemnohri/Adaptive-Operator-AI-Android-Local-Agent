package com.ai.operator.core.ai

import com.ai.operator.core.ai.models.DownloadProgress
import com.ai.operator.core.ai.models.ModelManifest
import com.ai.operator.core.ai.models.ModelStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class LocalModelManagerImpl(
    private val modelDirectory: File,
    private val deviceAnalyzer: DeviceAnalyzer
) : ModelManager {

    private val availableModels = listOf(
        ModelManifest(
            id = "gemma-4-e2b-it",
            name = "Gemma 4 E2B-it",
            version = "1.0",
            runtime = "litert-lm",
            modalities = listOf("text", "image", "audio"),
            contextLength = 128000,
            toolCalling = true,
            thinking = true,
            fileUrl = "https://huggingface.co/google/gemma-4-e2b-it-litert/resolve/main/model.bin",
            fileSize = 1449551462L, // ~1.35 GB
            sha256Checksum = "a170321da170321da170321da170321da170321da170321da170321da170321d"
        )
    )

    private val modelStatusFlows = mutableMapOf<String, MutableStateFlow<ModelStatus>>()
    private val downloadProgressFlows = mutableMapOf<String, MutableStateFlow<DownloadProgress>>()
    private val downloadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        availableModels.forEach { model ->
            val status = if (isModelFullyInstalled(model.id)) {
                ModelStatus.OPERATIONAL
            } else {
                ModelStatus.UNINSTALLED
            }
            modelStatusFlows[model.id] = MutableStateFlow(status)
            downloadProgressFlows[model.id] = MutableStateFlow(
                DownloadProgress(
                    modelId = model.id,
                    isDownloading = false,
                    isPaused = false,
                    bytesDownloaded = 0L,
                    totalBytes = model.fileSize,
                    speedBytesPerSec = 0L,
                    etaSeconds = 0L,
                    progressPercent = 0
                )
            )
        }
    }

    private fun isModelFullyInstalled(modelId: String): Boolean {
        val destDir = File(modelDirectory, modelId)
        val manifestFile = File(destDir, "metadata.json")
        val weightsFile = File(destDir, "model.bin")
        return manifestFile.exists() && weightsFile.exists()
    }

    override fun getAvailableModels(): List<ModelManifest> {
        return availableModels
    }

    override fun getModelStatus(modelId: String): Flow<ModelStatus> {
        return modelStatusFlows[modelId]?.asStateFlow()
            ?: MutableStateFlow(ModelStatus.UNINSTALLED).asStateFlow()
    }

    override fun getDownloadProgress(modelId: String): Flow<DownloadProgress> {
        return downloadProgressFlows[modelId]?.asStateFlow()
            ?: MutableStateFlow(
                DownloadProgress(modelId, false, false, 0, 0, 0, 0, 0)
            ).asStateFlow()
    }

    override suspend fun startDownload(modelId: String, useWifiOnly: Boolean): Result<Unit> {
        val model = availableModels.find { it.id == modelId }
            ?: return Result.failure(IllegalArgumentException("Model not found"))

        val statusFlow = modelStatusFlows[modelId] ?: return Result.failure(IllegalStateException("Flow not initialized"))
        val progressFlow = downloadProgressFlows[modelId] ?: return Result.failure(IllegalStateException("Flow not initialized"))

        // Storage check
        val (availableSpace, _) = deviceAnalyzer.getMemoryAvailability()
        if (availableSpace < model.fileSize + 200_000_000L) { // Require model size + 200MB buffer
            statusFlow.value = ModelStatus.ERROR
            progressFlow.value = progressFlow.value.copy(
                isDownloading = false,
                error = "Insufficient storage space available on device."
            )
            return Result.failure(IllegalStateException("Insufficient storage space"))
        }

        if (statusFlow.value == ModelStatus.OPERATIONAL) {
            return Result.success(Unit)
        }

        downloadJobs[modelId]?.cancel()
        statusFlow.value = ModelStatus.DOWNLOADING

        val job = scope.launch {
            var currentBytes = progressFlow.value.bytesDownloaded
            val totalBytes = model.fileSize
            val speed = 18_400_000L // 18.4 MB/s simulate speed

            progressFlow.value = progressFlow.value.copy(
                isDownloading = true,
                isPaused = false,
                error = null
            )

            while (currentBytes < totalBytes) {
                delay(1000)
                currentBytes = (currentBytes + speed).coerceAtMost(totalBytes)
                val remainingBytes = totalBytes - currentBytes
                val eta = if (speed > 0) remainingBytes / speed else 0L
                val progressPercent = ((currentBytes * 100) / totalBytes).toInt()

                progressFlow.value = progressFlow.value.copy(
                    bytesDownloaded = currentBytes,
                    speedBytesPerSec = speed,
                    etaSeconds = eta,
                    progressPercent = progressPercent
                )

                if (currentBytes >= totalBytes) {
                    break
                }
            }

            statusFlow.value = ModelStatus.VERIFYING
            progressFlow.value = progressFlow.value.copy(isDownloading = false)

            delay(1500) // Integrity checking
            val verifyResult = verifyModel(modelId)
            if (verifyResult.isSuccess && verifyResult.getOrThrow()) {
                val installResult = installModel(modelId)
                if (installResult.isSuccess) {
                    statusFlow.value = ModelStatus.OPERATIONAL
                } else {
                    statusFlow.value = ModelStatus.ERROR
                    progressFlow.value = progressFlow.value.copy(error = "Installation failed.")
                }
            } else {
                statusFlow.value = ModelStatus.COMPATIBILITY_FAILED
                progressFlow.value = progressFlow.value.copy(error = "Checksum verification failed. File corrupted.")
            }
        }

        downloadJobs[modelId] = job
        return Result.success(Unit)
    }

    override suspend fun pauseDownload(modelId: String) {
        val job = downloadJobs[modelId]
        if (job != null && job.isActive) {
            job.cancel()
            val statusFlow = modelStatusFlows[modelId]
            val progressFlow = downloadProgressFlows[modelId]
            statusFlow?.value = ModelStatus.UNINSTALLED
            progressFlow?.value = progressFlow?.value?.copy(
                isDownloading = false,
                isPaused = true
            )!!
        }
    }

    override suspend fun resumeDownload(modelId: String) {
        startDownload(modelId, false)
    }

    override suspend fun cancelDownload(modelId: String) {
        val job = downloadJobs[modelId]
        job?.cancel()
        downloadJobs.remove(modelId)

        val statusFlow = modelStatusFlows[modelId]
        val progressFlow = downloadProgressFlows[modelId]
        statusFlow?.value = ModelStatus.UNINSTALLED
        progressFlow?.value = DownloadProgress(
            modelId = modelId,
            isDownloading = false,
            isPaused = false,
            bytesDownloaded = 0L,
            totalBytes = availableModels.find { it.id == modelId }?.fileSize ?: 0L,
            speedBytesPerSec = 0L,
            etaSeconds = 0L,
            progressPercent = 0
        )
    }

    override suspend fun verifyModel(modelId: String): Result<Boolean> {
        val model = availableModels.find { it.id == modelId }
            ?: return Result.failure(IllegalArgumentException("Model not found"))

        // Return simulated verification outcome
        return Result.success(true)
    }

    override suspend fun installModel(modelId: String): Result<Unit> {
        val destDir = File(modelDirectory, modelId)
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        val manifestFile = File(destDir, "metadata.json")
        val weightsFile = File(destDir, "model.bin")

        try {
            manifestFile.writeText("""
                {
                  "id": "$modelId",
                  "name": "Gemma 4 E2B-it",
                  "version": "1.0",
                  "runtime": "litert-lm",
                  "modalities": ["text", "image", "audio"],
                  "contextLength": 128000,
                  "toolCalling": true,
                  "thinking": true
                }
            """.trimIndent())
            weightsFile.writeText("SIMULATED MODEL WEIGHTS")
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun uninstallModel(modelId: String): Result<Unit> {
        cancelDownload(modelId)
        val destDir = File(modelDirectory, modelId)
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }
        modelStatusFlows[modelId]?.value = ModelStatus.UNINSTALLED
        return Result.success(Unit)
    }
}

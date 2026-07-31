package com.ai.operator.core.ai

import com.ai.operator.core.ai.models.DownloadProgress
import com.ai.operator.core.ai.models.ModelManifest
import com.ai.operator.core.ai.models.ModelStatus
import kotlinx.coroutines.flow.Flow

interface ModelManager {
    fun getAvailableModels(): List<ModelManifest>
    fun getModelStatus(modelId: String): Flow<ModelStatus>
    fun getDownloadProgress(modelId: String): Flow<DownloadProgress>

    suspend fun startDownload(modelId: String, useWifiOnly: Boolean): Result<Unit>
    suspend fun pauseDownload(modelId: String)
    suspend fun resumeDownload(modelId: String)
    suspend fun cancelDownload(modelId: String)

    suspend fun verifyModel(modelId: String): Result<Boolean>
    suspend fun installModel(modelId: String): Result<Unit>
    suspend fun uninstallModel(modelId: String): Result<Unit>
}

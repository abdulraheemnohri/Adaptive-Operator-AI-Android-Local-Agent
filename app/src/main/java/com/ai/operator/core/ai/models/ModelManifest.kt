package com.ai.operator.core.ai.models

data class ModelManifest(
    val id: String,
    val name: String,
    val version: String,
    val runtime: String, // e.g., "litert-lm"
    val modalities: List<String>, // e.g., ["text", "image", "audio"]
    val contextLength: Int,
    val toolCalling: Boolean,
    val thinking: Boolean,
    val fileUrl: String,
    val fileSize: Long,
    val sha256Checksum: String
)

data class DownloadProgress(
    val modelId: String,
    val isDownloading: Boolean,
    val isPaused: Boolean,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val progressPercent: Int,
    val error: String? = null
)

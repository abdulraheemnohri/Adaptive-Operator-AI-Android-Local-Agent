package com.adaptiveoperator.ai.ai.runtime

/**
 * Everything the app needs to know about a Gemma package before it will load it.
 * Mirrors Section 9 (Model Verification): id, version, size, checksum, and the
 * hardware/runtime it was built for.
 *
 * The default instance points at the public LiteRT Community build of Gemma 4 E2B-it.
 * That repo is gated on Hugging Face, so downloading it requires a read-scoped HF
 * access token -- see ModelDownloader and README "Setup" for where that token goes.
 * Swap `huggingFaceRepo` / `fileName` here (or add more entries) to track a different
 * quantization or a self-hosted mirror without touching any other class.
 */
data class ModelSpec(
    val modelId: String = "gemma-4-e2b-it",
    val displayName: String = "Gemma 4 E2B-it",
    val version: String = "1.0.0",
    val huggingFaceRepo: String = "litert-community/gemma-4-E2B-it-litert-lm",
    val fileName: String = "gemma-4-E2B-it.litertlm",
    val expectedSizeBytes: Long = 2_770_000_000L, // ~2.58 GiB; refreshed after first successful download
    val sha256: String? = null,                    // filled in from metadata.json once verified once
    val contextLength: Int = 32_768,
    val runtime: String = "LiteRT-LM"
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$huggingFaceRepo/resolve/main/$fileName"
}

enum class ModelInstallState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    INSTALLING,
    WARMING_UP,
    READY,
    FAILED,
    CORRUPTED
}

data class ModelDownloadProgress(
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val bytesPerSecond: Long = 0L
) {
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f else (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

    val etaSeconds: Long
        get() = if (bytesPerSecond <= 0L) -1L else (totalBytes - bytesDownloaded) / bytesPerSecond
}

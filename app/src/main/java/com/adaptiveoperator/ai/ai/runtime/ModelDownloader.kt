package com.adaptiveoperator.ai.ai.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadEvent {
    data class Progress(val progress: ModelDownloadProgress) : DownloadEvent()
    data object Completed : DownloadEvent()
    data object Paused : DownloadEvent()
    data class Failed(val reason: String, val retryable: Boolean) : DownloadEvent()
}

/**
 * Section 5: pause / resume / cancel / retry / speed / ETA, all backed by real HTTP
 * range requests so a paused 1.3 GB download resumes from the last byte instead of
 * restarting. Corruption is caught downstream by ModelVerifier, not here.
 */
@Singleton
class ModelDownloader @Inject constructor(
    private val httpClient: OkHttpClient
) {
    @Volatile private var cancelled = false
    @Volatile private var paused = false

    fun cancel() { cancelled = true }
    fun pause() { paused = true }
    fun resume() { paused = false }

    /**
     * Streams [spec.downloadUrl] into [destinationFile], appending to any partial file
     * already on disk. [hfToken] is a Hugging Face read token -- required because the
     * LiteRT Community Gemma repos are gated. Emits progress roughly 4x/second.
     */
    suspend fun download(
        spec: ModelSpec,
        destinationFile: File,
        hfToken: String?,
        onEvent: suspend (DownloadEvent) -> Unit
    ) = withContext(Dispatchers.IO) {
        cancelled = false
        paused = false
        destinationFile.parentFile?.mkdirs()

        var alreadyDownloaded = if (destinationFile.exists()) destinationFile.length() else 0L
        var attempt = 0
        val maxAttempts = 5

        while (attempt < maxAttempts) {
            attempt++
            try {
                val requestBuilder = Request.Builder()
                    .url(spec.downloadUrl)
                    .header("Range", "bytes=$alreadyDownloaded-")
                hfToken?.let { requestBuilder.header("Authorization", "Bearer $it") }

                httpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        onEvent(DownloadEvent.Failed("HTTP ${response.code}", retryable = response.code >= 500))
                        return@withContext
                    }

                    val body = response.body ?: run {
                        onEvent(DownloadEvent.Failed("Empty response body", retryable = true))
                        return@withContext
                    }

                    val contentLength = body.contentLength()
                    val totalBytes = if (response.code == 206) alreadyDownloaded + contentLength else contentLength

                    RandomAccessFile(destinationFile, "rw").use { output ->
                        output.seek(alreadyDownloaded)
                        val source = body.source()
                        val buffer = ByteArray(64 * 1024)
                        var lastEmit = System.currentTimeMillis()
                        var bytesSinceLastEmit = 0L

                        while (true) {
                            if (cancelled) {
                                onEvent(DownloadEvent.Failed("Cancelled", retryable = false))
                                return@withContext
                            }
                            if (paused) {
                                onEvent(DownloadEvent.Paused)
                                return@withContext
                            }

                            val read = source.read(buffer)
                            if (read == -1) break

                            output.write(buffer, 0, read)
                            alreadyDownloaded += read
                            bytesSinceLastEmit += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmit >= 250) {
                                val bps = (bytesSinceLastEmit * 1000L) / (now - lastEmit)
                                onEvent(
                                    DownloadEvent.Progress(
                                        ModelDownloadProgress(alreadyDownloaded, totalBytes, bps)
                                    )
                                )
                                lastEmit = now
                                bytesSinceLastEmit = 0L
                            }
                        }
                    }

                    onEvent(DownloadEvent.Progress(ModelDownloadProgress(alreadyDownloaded, totalBytes, 0)))
                    onEvent(DownloadEvent.Completed)
                    return@withContext
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt >= maxAttempts) {
                    onEvent(DownloadEvent.Failed(e.message ?: "Unknown network error", retryable = false))
                    return@withContext
                }
                // Automatic retry with backoff, re-checking how much is already on disk.
                kotlinx.coroutines.delay(1000L * attempt)
                alreadyDownloaded = if (destinationFile.exists()) destinationFile.length() else 0L
            }
        }
    }
}

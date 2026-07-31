package com.adaptiveoperator.ai.ai.runtime

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ModelManagerUiState(
    val installState: ModelInstallState = ModelInstallState.NOT_DOWNLOADED,
    val progress: ModelDownloadProgress = ModelDownloadProgress(),
    val spec: ModelSpec = ModelSpec(),
    val errorMessage: String? = null,
    val backendInUse: InferenceBackend? = null
)

/**
 * The app-private model directory from Section 8:
 *   <filesDir>/models/gemma4-e2b/{model file, metadata.json}
 * Nothing under here is exposed via a content provider or external storage.
 */
@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: ModelDownloader,
    private val verifier: ModelVerifier,
    private val engine: GemmaEngineWrapper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(ModelManagerUiState())
    val uiState: StateFlow<ModelManagerUiState> = _uiState.asStateFlow()

    private fun modelDir(spec: ModelSpec): File =
        File(context.filesDir, "models/${spec.modelId}").apply { mkdirs() }

    private fun modelFile(spec: ModelSpec): File = File(modelDir(spec), spec.fileName)

    init {
        scope.launch {
            val spec = _uiState.value.spec
            if (modelFile(spec).exists()) {
                _uiState.value = _uiState.value.copy(installState = ModelInstallState.VERIFYING)
                verifyAndPromote(spec)
            }
        }
    }

    fun startDownload(hfToken: String? = null) {
        val spec = _uiState.value.spec
        scope.launch {
            _uiState.value = _uiState.value.copy(installState = ModelInstallState.DOWNLOADING, errorMessage = null)
            downloader.download(spec, modelFile(spec), hfToken) { event ->
                when (event) {
                    is DownloadEvent.Progress -> {
                        _uiState.value = _uiState.value.copy(progress = event.progress)
                    }
                    DownloadEvent.Completed -> {
                        _uiState.value = _uiState.value.copy(installState = ModelInstallState.VERIFYING)
                        verifyAndPromote(spec)
                    }
                    DownloadEvent.Paused -> {
                        _uiState.value = _uiState.value.copy(installState = ModelInstallState.PAUSED)
                    }
                    is DownloadEvent.Failed -> {
                        _uiState.value = _uiState.value.copy(
                            installState = ModelInstallState.FAILED,
                            errorMessage = event.reason
                        )
                    }
                }
            }
        }
    }

    fun pauseDownload() = downloader.pause()
    fun resumeDownload() = startDownload()
    fun cancelDownload() {
        downloader.cancel()
        modelFile(_uiState.value.spec).delete()
        _uiState.value = _uiState.value.copy(
            installState = ModelInstallState.NOT_DOWNLOADED,
            progress = ModelDownloadProgress()
        )
    }

    fun removeModel() {
        scope.launch { engine.unload() }
        modelFile(_uiState.value.spec).delete()
        _uiState.value = _uiState.value.copy(
            installState = ModelInstallState.NOT_DOWNLOADED,
            progress = ModelDownloadProgress()
        )
    }

    private suspend fun verifyAndPromote(spec: ModelSpec) {
        when (val result = verifier.verify(modelFile(spec), spec)) {
            is VerificationResult.Valid -> {
                _uiState.value = _uiState.value.copy(installState = ModelInstallState.INSTALLING)
                loadAndWarmUp(spec)
            }
            is VerificationResult.FileMissing -> {
                _uiState.value = _uiState.value.copy(
                    installState = ModelInstallState.NOT_DOWNLOADED,
                    errorMessage = "Downloaded file went missing before it could be verified"
                )
            }
            else -> {
                // Section 9: corrupted files are deleted, never loaded.
                modelFile(spec).delete()
                _uiState.value = _uiState.value.copy(
                    installState = ModelInstallState.CORRUPTED,
                    errorMessage = result.toString()
                )
            }
        }
    }

    private suspend fun loadAndWarmUp(spec: ModelSpec) {
        _uiState.value = _uiState.value.copy(installState = ModelInstallState.WARMING_UP)
        try {
            engine.load(modelFile(spec).absolutePath, GenerationConfig())
            _uiState.value = _uiState.value.copy(
                installState = ModelInstallState.READY,
                backendInUse = engine.activeBackend
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                installState = ModelInstallState.FAILED,
                errorMessage = e.message ?: "Model failed to initialize on every available backend"
            )
        }
    }
}

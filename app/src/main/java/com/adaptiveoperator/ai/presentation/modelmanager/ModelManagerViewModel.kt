package com.adaptiveoperator.ai.presentation.modelmanager

import androidx.lifecycle.ViewModel
import com.adaptiveoperator.ai.ai.runtime.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val modelManager: ModelManager
) : ViewModel() {

    val uiState = modelManager.uiState

    /** [hfToken] is a Hugging Face read-scoped access token -- required because the
     *  LiteRT Community Gemma repo is gated. See README "Setup" for where to get one. */
    fun startDownload(hfToken: String?) = modelManager.startDownload(hfToken)
    fun pause() = modelManager.pauseDownload()
    fun resume() = modelManager.resumeDownload()
    fun cancel() = modelManager.cancelDownload()
    fun remove() = modelManager.removeModel()
}

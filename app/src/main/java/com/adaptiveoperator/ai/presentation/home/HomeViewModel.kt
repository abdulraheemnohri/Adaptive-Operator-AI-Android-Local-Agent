package com.adaptiveoperator.ai.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptiveoperator.ai.agent.AgentOrchestrator
import com.adaptiveoperator.ai.ai.runtime.ModelInstallState
import com.adaptiveoperator.ai.ai.runtime.ModelManager
import com.adaptiveoperator.ai.memory.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class HomeUiState(
    val modelDisplayName: String = "Gemma 4 E2B-it",
    val modelInstallState: ModelInstallState = ModelInstallState.NOT_DOWNLOADED,
    val backendLabel: String = "--",
    val experienceCount: Int = 0,
    val skillCount: Int = 0,
    val operatorReady: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    modelManager: ModelManager,
    private val memoryRepository: MemoryRepository,
    private val agentOrchestrator: AgentOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        modelManager.uiState.onEach { model ->
            _uiState.value = _uiState.value.copy(
                modelDisplayName = model.spec.displayName,
                modelInstallState = model.installState,
                backendLabel = model.backendInUse?.name ?: "--",
                operatorReady = model.installState == ModelInstallState.READY
            )
        }.launchIn(viewModelScope)

        memoryRepository.recentTasks(500).onEach { tasks ->
            _uiState.value = _uiState.value.copy(experienceCount = tasks.size)
        }.launchIn(viewModelScope)

        memoryRepository.skills().onEach { skills ->
            _uiState.value = _uiState.value.copy(skillCount = skills.size)
        }.launchIn(viewModelScope)
    }

    /** Home's "Operator: [Start]" card (Section 49) jumps straight to running a task
     *  rather than requiring the user to visit Chat first. */
    fun quickStart(request: String) = agentOrchestrator.start(request)
}

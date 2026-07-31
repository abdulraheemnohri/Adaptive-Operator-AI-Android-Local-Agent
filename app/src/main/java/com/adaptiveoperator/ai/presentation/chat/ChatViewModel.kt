package com.adaptiveoperator.ai.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptiveoperator.ai.agent.AgentOrchestrator
import com.adaptiveoperator.ai.agent.AgentStatus
import com.adaptiveoperator.ai.memory.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Pair<String, String>> = emptyList(), // role to content
    val status: AgentStatus = AgentStatus.IDLE,
    val awaitingConfirmationSummary: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agentOrchestrator: AgentOrchestrator,
    memoryRepository: MemoryRepository
) : ViewModel() {

    val uiState = combine(
        memoryRepository.conversationHistory(),
        agentOrchestrator.state
    ) { history, agentState ->
        ChatUiState(
            messages = history.map { it.role to it.content },
            status = agentState.status,
            awaitingConfirmationSummary = agentState.awaitingConfirmation?.let { "${it.tool}(${it.arguments})" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    fun send(text: String) {
        if (text.isBlank()) return
        agentOrchestrator.start(text)
    }

    fun approvePendingAction() = agentOrchestrator.confirmPendingAction()
    fun rejectPendingAction() = agentOrchestrator.rejectPendingAction()
    fun stop() = agentOrchestrator.stop()
}

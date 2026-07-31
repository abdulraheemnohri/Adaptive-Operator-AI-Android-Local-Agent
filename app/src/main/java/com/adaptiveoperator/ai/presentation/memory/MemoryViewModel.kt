package com.adaptiveoperator.ai.presentation.memory

import androidx.lifecycle.ViewModel
import com.adaptiveoperator.ai.memory.repository.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    memoryRepository: MemoryRepository
) : ViewModel() {
    val recentTasks = memoryRepository.recentTasks(100)
}

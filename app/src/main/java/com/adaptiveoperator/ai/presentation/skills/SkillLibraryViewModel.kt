package com.adaptiveoperator.ai.presentation.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptiveoperator.ai.memory.repository.MemoryRepository
import com.adaptiveoperator.ai.skills.SkillEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillLibraryViewModel @Inject constructor(
    memoryRepository: MemoryRepository,
    private val skillEngine: SkillEngine
) : ViewModel() {

    val skills = memoryRepository.allSkills()

    fun delete(skillId: Long) = viewModelScope.launch { skillEngine.deleteSkill(skillId) }
}

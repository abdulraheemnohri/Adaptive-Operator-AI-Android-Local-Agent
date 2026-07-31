package com.adaptiveoperator.ai.presentation.settings

import androidx.lifecycle.ViewModel
import com.adaptiveoperator.ai.android.voice.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val ttsManager: TextToSpeechManager
) : ViewModel()

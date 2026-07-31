package com.adaptiveoperator.ai.android.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class SpeakMode { ALWAYS, OPERATOR_RESULTS_ONLY, ERRORS_ONLY, SILENT }

data class TtsSettings(
    val enabled: Boolean = true,
    val speechRate: Float = 1.0f,   // Section 31 slider range 0.8-1.5
    val pitch: Float = 1.0f,
    val autoSpeak: Boolean = true,
    val mode: SpeakMode = SpeakMode.OPERATOR_RESULTS_ONLY,
    val headphonesOnly: Boolean = false
)

/** Section 31: Android's built-in TTS engine, no bundled voice model. */
@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    private val _settings = MutableStateFlow(TtsSettings())
    val settings: StateFlow<TtsSettings> = _settings.asStateFlow()

    fun updateSettings(update: (TtsSettings) -> TtsSettings) {
        _settings.value = update(_settings.value)
        applySettingsToEngine()
    }

    suspend fun initialize() = suspendCancellableCoroutine<Unit> { cont ->
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.getDefault()
                applySettingsToEngine()
            }
            if (cont.isActive) cont.resume(Unit)
        }
    }

    private fun applySettingsToEngine() {
        tts?.setSpeechRate(_settings.value.speechRate)
        tts?.setPitch(_settings.value.pitch)
    }

    fun speak(text: String, isError: Boolean = false) {
        val s = _settings.value
        if (!s.enabled) return
        val shouldSpeak = when (s.mode) {
            SpeakMode.SILENT -> false
            SpeakMode.ERRORS_ONLY -> isError
            SpeakMode.OPERATOR_RESULTS_ONLY -> true
            SpeakMode.ALWAYS -> true
        }
        if (!shouldSpeak || !ready) return
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}

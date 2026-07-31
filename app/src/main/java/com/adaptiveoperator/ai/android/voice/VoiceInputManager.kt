package com.adaptiveoperator.ai.android.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class VoiceInputEvent {
    data object ListeningStarted : VoiceInputEvent()
    data class PartialResult(val text: String) : VoiceInputEvent()
    data class FinalResult(val text: String) : VoiceInputEvent()
    data class Error(val message: String) : VoiceInputEvent()
    data object Done : VoiceInputEvent()
}

/**
 * Section 30: microphone -> Android Speech Recognition -> text -> Gemma. No dedicated
 * speech model ships with this app, per the Hard Architecture Rule -- this is a thin
 * wrapper over the platform's own `SpeechRecognizer`.
 */
@Singleton
class VoiceInputManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun listen(languageTag: String = Locale.getDefault().toLanguageTag()): Flow<VoiceInputEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(VoiceInputEvent.Error("No speech recognition service available on this device"))
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                trySend(VoiceInputEvent.ListeningStarted)
            }
            override fun onResults(results: android.os.Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text != null) trySend(VoiceInputEvent.FinalResult(text))
                trySend(VoiceInputEvent.Done)
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text != null) trySend(VoiceInputEvent.PartialResult(text))
            }
            override fun onError(error: Int) {
                trySend(VoiceInputEvent.Error(errorMessage(error)))
                trySend(VoiceInputEvent.Done)
            }
            override fun onEndOfSpeech() {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }

        recognizer.setRecognitionListener(listener)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)

        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that -- no speech matched"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Network error (on-device recognizers vary by OEM; some still need connectivity)"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission not granted"
        else -> "Speech recognition error ($code)"
    }
}

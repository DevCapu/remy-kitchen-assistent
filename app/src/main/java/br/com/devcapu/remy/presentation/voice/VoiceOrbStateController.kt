package br.com.devcapu.remy.presentation.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceOrbStateController {
    private val lock = Any()
    private var isListening: Boolean = false
    private var isSpeaking: Boolean = false

    private val _state = MutableStateFlow(VoiceOrbUiState())
    val state: StateFlow<VoiceOrbUiState> = _state.asStateFlow()

    fun onListeningChanged(isListening: Boolean) {
        synchronized(lock) {
            this.isListening = isListening
            updateModeState(clearAudio = !isListening || isSpeaking)
        }
    }

    fun onSpeakingChanged(isSpeaking: Boolean) {
        synchronized(lock) {
            this.isSpeaking = isSpeaking
            updateModeState(clearAudio = true)
        }
    }

    fun onAudioRmsChanged(rmsdB: Float) {
        synchronized(lock) {
            val current = _state.value
            if (current.mode != VoiceOrbMode.LISTENING) return
            _state.value =
                current.copy(
                    audioLevel = normalizeRmsDb(rmsdB),
                    hasLiveAudioSignal = true,
                )
        }
    }

    fun onRecognitionError() {
        synchronized(lock) {
            isListening = false
            updateModeState(clearAudio = true)
        }
    }

    fun onRecognitionEnded() {
        synchronized(lock) {
            isListening = false
            updateModeState(clearAudio = true)
        }
    }

    fun reset() {
        synchronized(lock) {
            isListening = false
            isSpeaking = false
            _state.value = VoiceOrbUiState()
        }
    }

    private fun updateModeState(clearAudio: Boolean) {
        val nextMode =
            when {
                isSpeaking -> VoiceOrbMode.SPEAKING
                isListening -> VoiceOrbMode.LISTENING
                else -> VoiceOrbMode.IDLE
            }

        val current = _state.value
        val preserveAudio =
            !clearAudio &&
                nextMode == VoiceOrbMode.LISTENING &&
                current.mode == VoiceOrbMode.LISTENING

        _state.value =
            if (preserveAudio) {
                current
            } else {
                VoiceOrbUiState(
                    mode = nextMode,
                    audioLevel = 0f,
                    hasLiveAudioSignal = false,
                )
            }
    }
}

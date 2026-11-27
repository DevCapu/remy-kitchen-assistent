package br.com.devcapu.remy.conversation.wakeword

enum class WakeWordError {
    PERMISSION_DENIED,
    MODEL_LOAD_FAILED,
    AUDIO_ERROR,
}

enum class WakeWordStatus {
    IDLE,
    LOADING_MODEL,
    LISTENING,
    PAUSED_FOR_COMMAND,
    ERROR,
}

data class WakeWordUiState(
    val status: WakeWordStatus = WakeWordStatus.IDLE,
    val isEnabled: Boolean = false,
    val error: WakeWordError? = null,
)

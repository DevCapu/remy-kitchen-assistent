package br.com.devcapu.remy.conversation.wakeword

interface WakeWordEngine {
    fun loadModel(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    )

    fun startListening(
        onWakeWordDetected: () -> Unit,
        onError: (Throwable) -> Unit,
    )

    fun stopListening(onStopped: () -> Unit = {})

    fun release()
}

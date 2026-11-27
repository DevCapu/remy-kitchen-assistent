package br.com.devcapu.remy.conversation.wakeword

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WakeWordCoordinator(
    private val engine: WakeWordEngine,
    private val onStartCommandRecognition: () -> Unit,
) {
    private val lock = Any()
    private val _state = MutableStateFlow(WakeWordUiState())
    val state: StateFlow<WakeWordUiState> = _state.asStateFlow()

    private var isModelLoaded = false
    private var isLoadingModel = false
    private var commandSessionToken = 0L

    fun enable(hasAudioPermission: Boolean) {
        val action =
            synchronized(lock) {
                if (!hasAudioPermission) {
                    _state.value =
                        WakeWordUiState(
                            status = WakeWordStatus.ERROR,
                            isEnabled = false,
                            error = WakeWordError.PERMISSION_DENIED,
                        )
                    return
                }

                val current = _state.value
                val isAlreadyActive =
                    current.isEnabled &&
                        (
                            current.status == WakeWordStatus.LOADING_MODEL ||
                                current.status == WakeWordStatus.LISTENING ||
                                current.status == WakeWordStatus.PAUSED_FOR_COMMAND
                        )
                if (isAlreadyActive) {
                    return
                }

                when {
                    isModelLoaded -> {
                        _state.value =
                            WakeWordUiState(
                                status = WakeWordStatus.LISTENING,
                                isEnabled = true,
                                error = null,
                            )
                        EnableAction.START_LISTENING
                    }

                    isLoadingModel -> {
                        _state.value =
                            WakeWordUiState(
                                status = WakeWordStatus.LOADING_MODEL,
                                isEnabled = true,
                                error = null,
                            )
                        EnableAction.NONE
                    }

                    else -> {
                        isLoadingModel = true
                        _state.value =
                            WakeWordUiState(
                                status = WakeWordStatus.LOADING_MODEL,
                                isEnabled = true,
                                error = null,
                            )
                        EnableAction.LOAD_MODEL
                    }
                }
            }

        when (action) {
            EnableAction.START_LISTENING -> startListeningInternal()
            EnableAction.LOAD_MODEL -> loadModelInternal()
            EnableAction.NONE -> Unit
        }
    }

    fun disable() {
        synchronized(lock) {
            commandSessionToken++
            _state.value =
                WakeWordUiState(
                    status = WakeWordStatus.IDLE,
                    isEnabled = false,
                    error = null,
                )
        }
        engine.stopListening()
    }

    fun onWakeWordDetected() {
        val token =
            synchronized(lock) {
                val current = _state.value
                if (!current.isEnabled || current.status != WakeWordStatus.LISTENING) {
                    return
                }
                val nextToken = ++commandSessionToken
                _state.value =
                    current.copy(
                        status = WakeWordStatus.PAUSED_FOR_COMMAND,
                        error = null,
                    )
                nextToken
            }

        engine.stopListening {
            handleStoppedForCommand(token)
        }
    }

    fun onManualCommandStart() {
        val stopToken =
            synchronized(lock) {
                val current = _state.value
                when (current.status) {
                    WakeWordStatus.LISTENING -> {
                        val nextToken = ++commandSessionToken
                        _state.value =
                            current.copy(
                                status = WakeWordStatus.PAUSED_FOR_COMMAND,
                                error = null,
                            )
                        nextToken
                    }

                    WakeWordStatus.LOADING_MODEL -> {
                        _state.value =
                            current.copy(
                                status = WakeWordStatus.PAUSED_FOR_COMMAND,
                                error = null,
                            )
                        null
                    }

                    WakeWordStatus.IDLE,
                    WakeWordStatus.PAUSED_FOR_COMMAND,
                    WakeWordStatus.ERROR,
                    -> null
                }
            }

        if (stopToken != null) {
            engine.stopListening {
                handleStoppedForCommand(stopToken)
            }
        } else {
            onStartCommandRecognition()
        }
    }

    fun onCommandSessionFinished() {
        val action =
            synchronized(lock) {
                val current = _state.value
                if (!current.isEnabled) {
                    _state.value =
                        WakeWordUiState(
                            status = WakeWordStatus.IDLE,
                            isEnabled = false,
                            error = null,
                        )
                    return
                }

                when {
                    isModelLoaded -> {
                        if (current.status == WakeWordStatus.LISTENING) {
                            EnableAction.NONE
                        } else {
                            commandSessionToken++
                            _state.value =
                                current.copy(
                                    status = WakeWordStatus.LISTENING,
                                    error = null,
                                )
                            EnableAction.START_LISTENING
                        }
                    }

                    isLoadingModel -> {
                        _state.value =
                            current.copy(
                                status = WakeWordStatus.LOADING_MODEL,
                                error = null,
                            )
                        EnableAction.NONE
                    }

                    else -> {
                        isLoadingModel = true
                        _state.value =
                            current.copy(
                                status = WakeWordStatus.LOADING_MODEL,
                                error = null,
                            )
                        EnableAction.LOAD_MODEL
                    }
                }
            }

        when (action) {
            EnableAction.START_LISTENING -> startListeningInternal()
            EnableAction.LOAD_MODEL -> loadModelInternal()
            EnableAction.NONE -> Unit
        }
    }

    fun onEngineError(throwable: Throwable) {
        val shouldHandle =
            synchronized(lock) {
                val current = _state.value
                if (!current.isEnabled && current.status == WakeWordStatus.IDLE) {
                    false
                } else {
                    commandSessionToken++
                    _state.value =
                        WakeWordUiState(
                            status = WakeWordStatus.ERROR,
                            isEnabled = false,
                            error = WakeWordError.AUDIO_ERROR,
                        )
                    true
                }
            }

        if (shouldHandle) {
            engine.stopListening()
        }
    }

    fun onPauseSession() {
        synchronized(lock) {
            commandSessionToken++
            _state.value =
                WakeWordUiState(
                    status = WakeWordStatus.IDLE,
                    isEnabled = false,
                    error = null,
                )
        }
        engine.stopListening()
    }

    fun release() {
        synchronized(lock) {
            commandSessionToken++
            isModelLoaded = false
            isLoadingModel = false
            _state.value =
                WakeWordUiState(
                    status = WakeWordStatus.IDLE,
                    isEnabled = false,
                    error = null,
                )
        }
        engine.release()
    }

    private fun loadModelInternal() {
        engine.loadModel(
            onSuccess = {
                val shouldStartListening =
                    synchronized(lock) {
                        isLoadingModel = false
                        isModelLoaded = true
                        val current = _state.value
                        if (current.isEnabled && current.status == WakeWordStatus.LOADING_MODEL) {
                            _state.value =
                                current.copy(
                                    status = WakeWordStatus.LISTENING,
                                    error = null,
                                )
                            true
                        } else {
                            false
                        }
                    }

                if (shouldStartListening) {
                    startListeningInternal()
                }
            },
            onError = {
                synchronized(lock) {
                    isLoadingModel = false
                    isModelLoaded = false
                    val current = _state.value
                    if (current.isEnabled && current.status == WakeWordStatus.LOADING_MODEL) {
                        _state.value =
                            WakeWordUiState(
                                status = WakeWordStatus.ERROR,
                                isEnabled = false,
                                error = WakeWordError.MODEL_LOAD_FAILED,
                            )
                    }
                }
            },
        )
    }

    private fun startListeningInternal() {
        engine.startListening(
            onWakeWordDetected = ::onWakeWordDetected,
            onError = ::onEngineError,
        )
    }

    private fun handleStoppedForCommand(token: Long) {
        val shouldStart =
            synchronized(lock) {
                commandSessionToken == token && _state.value.status == WakeWordStatus.PAUSED_FOR_COMMAND
            }
        if (shouldStart) {
            onStartCommandRecognition()
        }
    }

    private enum class EnableAction {
        START_LISTENING,
        LOAD_MODEL,
        NONE,
    }
}

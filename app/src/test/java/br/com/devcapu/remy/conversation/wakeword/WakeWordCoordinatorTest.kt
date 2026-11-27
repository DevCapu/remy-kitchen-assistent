package br.com.devcapu.remy.conversation.wakeword

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordCoordinatorTest {
    @Test
    fun `enable without audio permission transitions to permission denied error without loading model`() {
        val engine = FakeWakeWordEngine()
        val coordinator = WakeWordCoordinator(engine = engine, onStartCommandRecognition = {})

        coordinator.enable(hasAudioPermission = false)

        val state = coordinator.state.value
        assertEquals(WakeWordStatus.ERROR, state.status)
        assertFalse(state.isEnabled)
        assertEquals(WakeWordError.PERMISSION_DENIED, state.error)
        assertEquals(0, engine.loadModelCalls)
        assertEquals(0, engine.startListeningCalls)
    }

    @Test
    fun `enable loads model once transitions to listening and reuses loaded model on re-enable`() {
        val engine = FakeWakeWordEngine()
        val coordinator = WakeWordCoordinator(engine = engine, onStartCommandRecognition = {})

        coordinator.enable(hasAudioPermission = true)
        assertEquals(WakeWordStatus.LOADING_MODEL, coordinator.state.value.status)
        assertTrue(coordinator.state.value.isEnabled)
        assertEquals(1, engine.loadModelCalls)

        // Idempotent while loading
        coordinator.enable(hasAudioPermission = true)
        assertEquals(1, engine.loadModelCalls)

        engine.completeModelLoadSuccess()
        assertEquals(WakeWordStatus.LISTENING, coordinator.state.value.status)
        assertTrue(coordinator.state.value.isEnabled)
        assertNull(coordinator.state.value.error)
        assertEquals(1, engine.startListeningCalls)

        coordinator.disable()
        assertEquals(WakeWordStatus.IDLE, coordinator.state.value.status)
        assertFalse(coordinator.state.value.isEnabled)

        coordinator.enable(hasAudioPermission = true)
        assertEquals(WakeWordStatus.LISTENING, coordinator.state.value.status)
        assertTrue(coordinator.state.value.isEnabled)
        assertEquals(1, engine.loadModelCalls)
        assertEquals(2, engine.startListeningCalls)
    }

    @Test
    fun `enable transitions to model load failed error when model load fails`() {
        val engine = FakeWakeWordEngine()
        val coordinator = WakeWordCoordinator(engine = engine, onStartCommandRecognition = {})

        coordinator.enable(hasAudioPermission = true)
        engine.completeModelLoadError(IllegalStateException("unpack failed"))

        val state = coordinator.state.value
        assertEquals(WakeWordStatus.ERROR, state.status)
        assertFalse(state.isEnabled)
        assertEquals(WakeWordError.MODEL_LOAD_FAILED, state.error)
        assertEquals(0, engine.startListeningCalls)
    }

    @Test
    fun `disable while loading model prevents startListening when model finishes loading`() {
        val engine = FakeWakeWordEngine()
        val coordinator = WakeWordCoordinator(engine = engine, onStartCommandRecognition = {})

        coordinator.enable(hasAudioPermission = true)
        assertEquals(WakeWordStatus.LOADING_MODEL, coordinator.state.value.status)

        coordinator.disable()
        assertEquals(WakeWordStatus.IDLE, coordinator.state.value.status)
        assertFalse(coordinator.state.value.isEnabled)

        engine.completeModelLoadSuccess()
        assertEquals(WakeWordStatus.IDLE, coordinator.state.value.status)
        assertFalse(coordinator.state.value.isEnabled)
        assertEquals(0, engine.startListeningCalls)
    }

    @Test
    fun `onWakeWordDetected stops engine before starting command recognition and is idempotent`() {
        val events = mutableListOf<String>()
        val engine = FakeWakeWordEngine(events = events, autoCompleteStop = false)
        val coordinator =
            WakeWordCoordinator(
                engine = engine,
                onStartCommandRecognition = { events += "startCommandRecognition" },
            )

        coordinator.enable(hasAudioPermission = true)
        engine.completeModelLoadSuccess()
        events.clear()

        // Simulate duplicate detections from onPartialResult + onResult
        engine.triggerWakeWord()
        engine.triggerWakeWord()

        assertEquals(WakeWordStatus.PAUSED_FOR_COMMAND, coordinator.state.value.status)
        assertTrue(coordinator.state.value.isEnabled)
        assertEquals(listOf("stopListeningBegin"), events)

        engine.completeStopListening()

        assertEquals(
            listOf("stopListeningBegin", "stopListeningDone", "startCommandRecognition"),
            events,
        )
    }

    @Test
    fun `onCommandSessionFinished resumes listening when enabled and returns to idle when disabled`() {
        val engine = FakeWakeWordEngine()
        var commandStartCount = 0
        val coordinator =
            WakeWordCoordinator(
                engine = engine,
                onStartCommandRecognition = { commandStartCount++ },
            )

        coordinator.enable(hasAudioPermission = true)
        engine.completeModelLoadSuccess()
        engine.triggerWakeWord()
        assertEquals(1, commandStartCount)
        assertEquals(WakeWordStatus.PAUSED_FOR_COMMAND, coordinator.state.value.status)

        coordinator.onCommandSessionFinished()
        assertEquals(WakeWordStatus.LISTENING, coordinator.state.value.status)
        assertTrue(coordinator.state.value.isEnabled)
        assertEquals(2, engine.startListeningCalls)

        engine.triggerWakeWord()
        coordinator.disable()
        coordinator.onCommandSessionFinished()
        assertEquals(WakeWordStatus.IDLE, coordinator.state.value.status)
        assertFalse(coordinator.state.value.isEnabled)
        assertEquals(2, engine.startListeningCalls)
    }

    @Test
    fun `onManualCommandStart pauses vosk before starting command and resumes after finish`() {
        val events = mutableListOf<String>()
        val engine = FakeWakeWordEngine(events = events, autoCompleteStop = false)
        val coordinator =
            WakeWordCoordinator(
                engine = engine,
                onStartCommandRecognition = { events += "startCommandRecognition" },
            )

        coordinator.enable(hasAudioPermission = true)
        engine.completeModelLoadSuccess()
        events.clear()

        coordinator.onManualCommandStart()
        assertEquals(WakeWordStatus.PAUSED_FOR_COMMAND, coordinator.state.value.status)
        assertTrue(coordinator.state.value.isEnabled)
        assertEquals(listOf("stopListeningBegin"), events)

        engine.completeStopListening()
        assertEquals(
            listOf("stopListeningBegin", "stopListeningDone", "startCommandRecognition"),
            events,
        )

        coordinator.onCommandSessionFinished()
        assertEquals(WakeWordStatus.LISTENING, coordinator.state.value.status)
        assertEquals(2, engine.startListeningCalls)
    }

    @Test
    fun `onManualCommandStart during idle starts command immediately without enabling wake word`() {
        val engine = FakeWakeWordEngine()
        var commandStartCount = 0
        val coordinator =
            WakeWordCoordinator(
                engine = engine,
                onStartCommandRecognition = { commandStartCount++ },
            )

        coordinator.onManualCommandStart()
        assertEquals(1, commandStartCount)
        assertEquals(WakeWordStatus.IDLE, coordinator.state.value.status)
        assertFalse(coordinator.state.value.isEnabled)
        assertEquals(0, engine.stopListeningCalls)

        coordinator.onCommandSessionFinished()
        assertEquals(WakeWordStatus.IDLE, coordinator.state.value.status)
        assertEquals(0, engine.startListeningCalls)
    }

    @Test
    fun `onEngineError transitions to audio error and recovers on next enable`() {
        val engine = FakeWakeWordEngine()
        val coordinator = WakeWordCoordinator(engine = engine, onStartCommandRecognition = {})

        coordinator.enable(hasAudioPermission = true)
        engine.completeModelLoadSuccess()

        engine.triggerError(RuntimeException("AudioRecord failed"))

        val errorState = coordinator.state.value
        assertEquals(WakeWordStatus.ERROR, errorState.status)
        assertFalse(errorState.isEnabled)
        assertEquals(WakeWordError.AUDIO_ERROR, errorState.error)
        assertEquals(1, engine.stopListeningCalls)

        coordinator.enable(hasAudioPermission = true)
        assertEquals(WakeWordStatus.LISTENING, coordinator.state.value.status)
        assertTrue(coordinator.state.value.isEnabled)
        assertNull(coordinator.state.value.error)
        assertEquals(2, engine.startListeningCalls)
    }

    @Test
    fun `onPauseSession and release stop listening and do not resume later`() {
        val events = mutableListOf<String>()
        val engine = FakeWakeWordEngine(events = events, autoCompleteStop = false)
        var commandStartCount = 0
        val coordinator =
            WakeWordCoordinator(
                engine = engine,
                onStartCommandRecognition = { commandStartCount++ },
            )

        coordinator.enable(hasAudioPermission = true)
        engine.completeModelLoadSuccess()
        engine.triggerWakeWord()

        // Pause before stop completes: pending command callback must be cancelled
        coordinator.onPauseSession()
        engine.completeStopListening()

        assertEquals(0, commandStartCount)
        assertEquals(WakeWordStatus.IDLE, coordinator.state.value.status)
        assertFalse(coordinator.state.value.isEnabled)

        coordinator.onCommandSessionFinished()
        assertEquals(WakeWordStatus.IDLE, coordinator.state.value.status)
        assertEquals(1, engine.startListeningCalls)

        coordinator.release()
        assertEquals(1, engine.releaseCalls)
        assertEquals(WakeWordStatus.IDLE, coordinator.state.value.status)

        // After release, model must be loaded again if re-enabled
        coordinator.enable(hasAudioPermission = true)
        assertEquals(WakeWordStatus.LOADING_MODEL, coordinator.state.value.status)
        assertEquals(2, engine.loadModelCalls)
    }

    private class FakeWakeWordEngine(
        private val events: MutableList<String> = mutableListOf(),
        private val autoCompleteStop: Boolean = true,
    ) : WakeWordEngine {
        var loadModelCalls: Int = 0
        var startListeningCalls: Int = 0
        var stopListeningCalls: Int = 0
        var releaseCalls: Int = 0

        private var pendingLoadSuccess: (() -> Unit)? = null
        private var pendingLoadError: ((Throwable) -> Unit)? = null
        private var activeWakeWordCallback: (() -> Unit)? = null
        private var activeErrorCallback: ((Throwable) -> Unit)? = null
        private val pendingStopCallbacks = mutableListOf<() -> Unit>()

        override fun loadModel(
            onSuccess: () -> Unit,
            onError: (Throwable) -> Unit,
        ) {
            loadModelCalls++
            events += "loadModel"
            pendingLoadSuccess = onSuccess
            pendingLoadError = onError
        }

        override fun startListening(
            onWakeWordDetected: () -> Unit,
            onError: (Throwable) -> Unit,
        ) {
            startListeningCalls++
            events += "startListening"
            activeWakeWordCallback = onWakeWordDetected
            activeErrorCallback = onError
        }

        override fun stopListening(onStopped: () -> Unit) {
            stopListeningCalls++
            events += "stopListeningBegin"
            if (autoCompleteStop) {
                events += "stopListeningDone"
                onStopped()
            } else {
                pendingStopCallbacks += onStopped
            }
        }

        override fun release() {
            releaseCalls++
            events += "release"
            pendingLoadSuccess = null
            pendingLoadError = null
            activeWakeWordCallback = null
            activeErrorCallback = null
            pendingStopCallbacks.clear()
        }

        fun completeModelLoadSuccess() {
            val callback = pendingLoadSuccess
            pendingLoadSuccess = null
            pendingLoadError = null
            callback?.invoke()
        }

        fun completeModelLoadError(throwable: Throwable) {
            val callback = pendingLoadError
            pendingLoadSuccess = null
            pendingLoadError = null
            callback?.invoke(throwable)
        }

        fun completeStopListening() {
            val callbacks = pendingStopCallbacks.toList()
            pendingStopCallbacks.clear()
            events += "stopListeningDone"
            callbacks.forEach { it() }
        }

        fun triggerWakeWord() {
            activeWakeWordCallback?.invoke()
        }

        fun triggerError(throwable: Throwable) {
            activeErrorCallback?.invoke(throwable)
        }
    }
}

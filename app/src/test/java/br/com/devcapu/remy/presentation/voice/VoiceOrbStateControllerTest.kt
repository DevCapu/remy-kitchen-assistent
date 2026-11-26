package br.com.devcapu.remy.presentation.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceOrbStateControllerTest {
    @Test
    fun `initial state is idle with zero audio level and no live signal`() {
        val controller = VoiceOrbStateController()

        val state = controller.state.value
        assertEquals(VoiceOrbMode.IDLE, state.mode)
        assertEquals(0f, state.audioLevel, 0.0001f)
        assertFalse(state.hasLiveAudioSignal)
    }

    @Test
    fun `transitions cleanly across full cycle idle to listening to speaking to idle`() {
        val controller = VoiceOrbStateController()

        controller.onListeningChanged(true)
        assertEquals(VoiceOrbMode.LISTENING, controller.state.value.mode)

        controller.onAudioRmsChanged(4f)
        assertEquals(0.5f, controller.state.value.audioLevel, 0.0001f)
        assertTrue(controller.state.value.hasLiveAudioSignal)

        controller.onListeningChanged(false)
        assertEquals(VoiceOrbMode.IDLE, controller.state.value.mode)

        controller.onSpeakingChanged(true)
        assertEquals(VoiceOrbMode.SPEAKING, controller.state.value.mode)
        assertEquals(0f, controller.state.value.audioLevel, 0.0001f)
        assertFalse(controller.state.value.hasLiveAudioSignal)

        controller.onSpeakingChanged(false)
        assertEquals(VoiceOrbMode.IDLE, controller.state.value.mode)
        assertEquals(0f, controller.state.value.audioLevel, 0.0001f)
        assertFalse(controller.state.value.hasLiveAudioSignal)
    }

    @Test
    fun `returns to idle and clears audio on recognition error`() {
        val controller = VoiceOrbStateController()
        controller.onListeningChanged(true)
        controller.onAudioRmsChanged(8f)

        controller.onRecognitionError()

        val state = controller.state.value
        assertEquals(VoiceOrbMode.IDLE, state.mode)
        assertEquals(0f, state.audioLevel, 0.0001f)
        assertFalse(state.hasLiveAudioSignal)
    }

    @Test
    fun `returns to idle and clears audio on recognition ended`() {
        val controller = VoiceOrbStateController()
        controller.onListeningChanged(true)
        controller.onAudioRmsChanged(6f)

        controller.onRecognitionEnded()

        val state = controller.state.value
        assertEquals(VoiceOrbMode.IDLE, state.mode)
        assertEquals(0f, state.audioLevel, 0.0001f)
        assertFalse(state.hasLiveAudioSignal)
    }

    @Test
    fun `returns to idle and clears audio when listening stops`() {
        val controller = VoiceOrbStateController()
        controller.onListeningChanged(true)
        controller.onAudioRmsChanged(6f)

        controller.onListeningChanged(false)

        val state = controller.state.value
        assertEquals(VoiceOrbMode.IDLE, state.mode)
        assertEquals(0f, state.audioLevel, 0.0001f)
        assertFalse(state.hasLiveAudioSignal)
    }

    @Test
    fun `speaking takes precedence over listening and returns consistently when speaking finishes`() {
        val controller = VoiceOrbStateController()
        controller.onListeningChanged(true)
        controller.onAudioRmsChanged(10f)

        controller.onSpeakingChanged(true)
        val speakingState = controller.state.value
        assertEquals(VoiceOrbMode.SPEAKING, speakingState.mode)
        assertEquals(0f, speakingState.audioLevel, 0.0001f)
        assertFalse(speakingState.hasLiveAudioSignal)

        controller.onSpeakingChanged(false)
        val resumedListeningState = controller.state.value
        assertEquals(VoiceOrbMode.LISTENING, resumedListeningState.mode)
        assertEquals(0f, resumedListeningState.audioLevel, 0.0001f)
        assertFalse(resumedListeningState.hasLiveAudioSignal)

        controller.onSpeakingChanged(true)
        controller.onListeningChanged(false)
        assertEquals(VoiceOrbMode.SPEAKING, controller.state.value.mode)

        controller.onSpeakingChanged(false)
        assertEquals(VoiceOrbMode.IDLE, controller.state.value.mode)
    }

    @Test
    fun `reset restores idle state from listening and speaking`() {
        val controller = VoiceOrbStateController()
        controller.onListeningChanged(true)
        controller.onAudioRmsChanged(7f)

        controller.reset()
        assertEquals(VoiceOrbUiState(), controller.state.value)

        controller.onListeningChanged(true)
        controller.onSpeakingChanged(true)

        controller.reset()
        assertEquals(VoiceOrbUiState(), controller.state.value)
    }

    @Test
    fun `normalizeRmsDb clamps range and handles non-finite inputs`() {
        assertEquals(0f, normalizeRmsDb(-2f), 0.0001f)
        assertEquals(0f, normalizeRmsDb(-10f), 0.0001f)
        assertEquals(0.5f, normalizeRmsDb(4f), 0.0001f)
        assertEquals(1f, normalizeRmsDb(10f), 0.0001f)
        assertEquals(1f, normalizeRmsDb(25f), 0.0001f)
        assertEquals(0f, normalizeRmsDb(Float.NaN), 0.0001f)
        assertEquals(0f, normalizeRmsDb(Float.POSITIVE_INFINITY), 0.0001f)
        assertEquals(0f, normalizeRmsDb(Float.NEGATIVE_INFINITY), 0.0001f)
    }

    @Test
    fun `onAudioRmsChanged only updates state while listening`() {
        val controller = VoiceOrbStateController()

        controller.onAudioRmsChanged(8f)
        assertEquals(VoiceOrbMode.IDLE, controller.state.value.mode)
        assertEquals(0f, controller.state.value.audioLevel, 0.0001f)
        assertFalse(controller.state.value.hasLiveAudioSignal)

        controller.onSpeakingChanged(true)
        controller.onAudioRmsChanged(8f)
        assertEquals(VoiceOrbMode.SPEAKING, controller.state.value.mode)
        assertEquals(0f, controller.state.value.audioLevel, 0.0001f)
        assertFalse(controller.state.value.hasLiveAudioSignal)

        controller.onSpeakingChanged(false)
        controller.onListeningChanged(true)
        controller.onAudioRmsChanged(4f)
        assertEquals(VoiceOrbMode.LISTENING, controller.state.value.mode)
        assertEquals(0.5f, controller.state.value.audioLevel, 0.0001f)
        assertTrue(controller.state.value.hasLiveAudioSignal)
    }

    @Test
    fun `resolveOrbMotionFactor returns zero across all modes when reduceMotion is enabled`() {
        assertEquals(
            0f,
            resolveOrbMotionFactor(
                mode = VoiceOrbMode.IDLE,
                audioLevel = 0.8f,
                hasLiveAudioSignal = true,
                reduceMotion = true,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveOrbMotionFactor(
                mode = VoiceOrbMode.LISTENING,
                audioLevel = 0.8f,
                hasLiveAudioSignal = true,
                reduceMotion = true,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveOrbMotionFactor(
                mode = VoiceOrbMode.LISTENING,
                audioLevel = 0f,
                hasLiveAudioSignal = false,
                reduceMotion = true,
            ),
            0.0001f,
        )
        assertEquals(
            0f,
            resolveOrbMotionFactor(
                mode = VoiceOrbMode.SPEAKING,
                audioLevel = 0f,
                hasLiveAudioSignal = false,
                reduceMotion = true,
            ),
            0.0001f,
        )
    }

    @Test
    fun `resolveOrbMotionFactor reflects mode and live audio level when animations are enabled`() {
        val idleMotion =
            resolveOrbMotionFactor(
                mode = VoiceOrbMode.IDLE,
                audioLevel = 0f,
                hasLiveAudioSignal = false,
                reduceMotion = false,
            )
        val fallbackListening =
            resolveOrbMotionFactor(
                mode = VoiceOrbMode.LISTENING,
                audioLevel = 0f,
                hasLiveAudioSignal = false,
                reduceMotion = false,
            )
        val quietListening =
            resolveOrbMotionFactor(
                mode = VoiceOrbMode.LISTENING,
                audioLevel = 0.1f,
                hasLiveAudioSignal = true,
                reduceMotion = false,
            )
        val loudListening =
            resolveOrbMotionFactor(
                mode = VoiceOrbMode.LISTENING,
                audioLevel = 0.9f,
                hasLiveAudioSignal = true,
                reduceMotion = false,
            )
        val speakingMotion =
            resolveOrbMotionFactor(
                mode = VoiceOrbMode.SPEAKING,
                audioLevel = 0f,
                hasLiveAudioSignal = false,
                reduceMotion = false,
            )

        assertEquals(0f, idleMotion, 0.0001f)
        assertTrue(fallbackListening > 0f)
        assertTrue(loudListening > quietListening)
        assertTrue(speakingMotion > 0f)
    }
}

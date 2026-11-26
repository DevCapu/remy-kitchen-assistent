package br.com.devcapu.remy.presentation.voice

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import br.com.devcapu.remy.R
import br.com.devcapu.remy.ui.theme.RemyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class VoiceOrbTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resources
        get() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    @Test
    fun rendersDistinctSemanticsAndStatusTextAcrossModes() {
        val state = mutableStateOf(VoiceOrbUiState(mode = VoiceOrbMode.IDLE))

        composeRule.setContent {
            RemyTheme(darkTheme = true, dynamicColor = false) {
                VoiceOrb(
                    state = state.value,
                    onClick = {},
                    reduceMotion = false,
                )
            }
        }

        val idleCd = resources.getString(R.string.voice_orb_cd_idle)
        val listeningCd = resources.getString(R.string.voice_orb_cd_listening)
        val speakingCd = resources.getString(R.string.voice_orb_cd_speaking)

        val idleState = resources.getString(R.string.voice_orb_state_idle)
        val listeningState = resources.getString(R.string.voice_orb_state_listening)
        val speakingState = resources.getString(R.string.voice_orb_state_speaking)

        val idleStatus = resources.getString(R.string.voice_orb_status_idle)
        val listeningStatus = resources.getString(R.string.voice_orb_status_listening)
        val speakingStatus = resources.getString(R.string.voice_orb_status_speaking)

        assertNotEquals(idleCd, listeningCd)
        assertNotEquals(listeningCd, speakingCd)
        assertNotEquals(idleStatus, listeningStatus)
        assertNotEquals(listeningStatus, speakingStatus)

        composeRule
            .onNodeWithTag(VOICE_ORB_TEST_TAG)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(idleCd)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, idleState))
        composeRule
            .onNodeWithTag(VOICE_ORB_STATUS_TEST_TAG)
            .assertIsDisplayed()
            .assertTextEquals(idleStatus)

        composeRule.runOnIdle {
            state.value =
                VoiceOrbUiState(
                    mode = VoiceOrbMode.LISTENING,
                    audioLevel = 0.75f,
                    hasLiveAudioSignal = true,
                )
        }
        composeRule
            .onNodeWithTag(VOICE_ORB_TEST_TAG)
            .assertContentDescriptionEquals(listeningCd)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, listeningState),
            )
        composeRule
            .onNodeWithTag(VOICE_ORB_STATUS_TEST_TAG)
            .assertTextEquals(listeningStatus)

        composeRule.runOnIdle {
            state.value = VoiceOrbUiState(mode = VoiceOrbMode.SPEAKING)
        }
        composeRule
            .onNodeWithTag(VOICE_ORB_TEST_TAG)
            .assertContentDescriptionEquals(speakingCd)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, speakingState),
            )
        composeRule
            .onNodeWithTag(VOICE_ORB_STATUS_TEST_TAG)
            .assertTextEquals(speakingStatus)
    }

    @Test
    fun buttonRoleAndClickActionInvokeCallback() {
        var clickCount = 0

        composeRule.setContent {
            RemyTheme(darkTheme = true, dynamicColor = false) {
                VoiceOrb(
                    state = VoiceOrbUiState(mode = VoiceOrbMode.IDLE),
                    onClick = { clickCount++ },
                    reduceMotion = false,
                )
            }
        }

        composeRule
            .onNodeWithTag(VOICE_ORB_TEST_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(1, clickCount)
    }

    @Test
    fun rendersStablyWithReduceMotionEnabled() {
        val state =
            mutableStateOf(
                VoiceOrbUiState(
                    mode = VoiceOrbMode.LISTENING,
                    audioLevel = 0.9f,
                    hasLiveAudioSignal = true,
                ),
            )

        composeRule.setContent {
            RemyTheme(darkTheme = true, dynamicColor = false) {
                VoiceOrb(
                    state = state.value,
                    onClick = {},
                    reduceMotion = true,
                )
            }
        }

        composeRule
            .onNodeWithTag(VOICE_ORB_TEST_TAG)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(resources.getString(R.string.voice_orb_cd_listening))
        composeRule
            .onNodeWithTag(VOICE_ORB_STATUS_TEST_TAG)
            .assertTextEquals(resources.getString(R.string.voice_orb_status_listening))

        composeRule.runOnIdle {
            state.value = VoiceOrbUiState(mode = VoiceOrbMode.SPEAKING)
        }
        composeRule
            .onNodeWithTag(VOICE_ORB_TEST_TAG)
            .assertIsDisplayed()
            .assertContentDescriptionEquals(resources.getString(R.string.voice_orb_cd_speaking))
        composeRule
            .onNodeWithTag(VOICE_ORB_STATUS_TEST_TAG)
            .assertTextEquals(resources.getString(R.string.voice_orb_status_speaking))
    }
}

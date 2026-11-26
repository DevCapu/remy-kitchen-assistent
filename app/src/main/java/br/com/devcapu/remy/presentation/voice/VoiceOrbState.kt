package br.com.devcapu.remy.presentation.voice

enum class VoiceOrbMode {
    IDLE,
    LISTENING,
    SPEAKING,
}

data class VoiceOrbUiState(
    val mode: VoiceOrbMode = VoiceOrbMode.IDLE,
    val audioLevel: Float = 0f,
    val hasLiveAudioSignal: Boolean = false,
)

private const val MIN_RMS_DB = -2f
private const val RMS_DB_RANGE = 12f
private const val LISTENING_BASE_MOTION = 0.2f
private const val LISTENING_LEVEL_WEIGHT = 0.8f
private const val LISTENING_FALLBACK_MOTION = 0.35f
private const val SPEAKING_MOTION = 0.65f

fun normalizeRmsDb(rmsdB: Float): Float {
    if (!rmsdB.isFinite()) return 0f
    return ((rmsdB - MIN_RMS_DB) / RMS_DB_RANGE).coerceIn(0f, 1f)
}

fun resolveOrbMotionFactor(
    mode: VoiceOrbMode,
    audioLevel: Float,
    hasLiveAudioSignal: Boolean,
    reduceMotion: Boolean,
): Float {
    if (reduceMotion) return 0f
    return when (mode) {
        VoiceOrbMode.IDLE -> 0f
        VoiceOrbMode.LISTENING -> {
            if (hasLiveAudioSignal) {
                val clampedLevel = if (audioLevel.isFinite()) audioLevel.coerceIn(0f, 1f) else 0f
                LISTENING_BASE_MOTION + (clampedLevel * LISTENING_LEVEL_WEIGHT)
            } else {
                LISTENING_FALLBACK_MOTION
            }
        }
        VoiceOrbMode.SPEAKING -> SPEAKING_MOTION
    }
}

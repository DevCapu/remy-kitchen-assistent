package br.com.devcapu.remy.infra

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import android.content.Context
import android.util.Log
import br.com.devcapu.remy.BuildConfig
import br.com.devcapu.remy.conversation.VoiceRecognitionService

private const val TAG = "PorcupineManager"

object PorcupineManagerSingleton {
    private var porcupineManager: PorcupineManager? = null

    fun getInstance(
        context: Context,
        voiceRecognitionService: VoiceRecognitionService
    ): PorcupineManager {
        if (porcupineManager == null) {
            Log.d(TAG, "🎤 Porcupine: Inicializando pela primeira vez")
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey("+7TLL4X54MnicGk+a6UO+OQpBiR0QNN7adeumAxW7gut3rJEgDF0HA==")
                .setKeywords(arrayOf(Porcupine.BuiltInKeyword.COMPUTER))
                .build(context) {
                    Log.d(TAG, "🎤 Porcupine: Palavra de ativação detectada!")
                    porcupineManager?.stop()
                    Log.d(TAG, "🎤 Controle do microfone: Porcupine está passando controle para Speech Recognition")
                    voiceRecognitionService.startListening()
                }
        }
        return porcupineManager!!
    }
}
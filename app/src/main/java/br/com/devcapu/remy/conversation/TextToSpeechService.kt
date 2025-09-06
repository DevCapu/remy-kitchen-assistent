package br.com.devcapu.remy.conversation

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Serviço responsável por converter texto em fala.
 * Utilizado para guiar o usuário durante o preparo da receita.
 * TODO: Trocar a voice para uma voz mais natural (ex: Google Wavenet)
 * Funcionalidades: Timer,
 */
class TextToSpeechService(
    private val context: Context,
    locale: Locale = Locale("pt", "BR")
) {
    private val tts: TextToSpeech by lazy {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(locale)
                _isTtsReady.value = (result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED)
            }
        }
    }

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        setupTtsListener()
    }

    private fun setupTtsListener() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    /**
     * Fala o texto fornecido.
     * @param text Texto a ser falado
     * @param queueMode Modo de enfileiramento (QUEUE_FLUSH para interromper a fala atual, QUEUE_ADD para adicionar à fila)
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (_isTtsReady.value) {
            val utteranceId = UUID.randomUUID().toString()
            tts.speak(text, queueMode, null, utteranceId)
        }
    }

    /**
     * Para a fala atual.
     */
    fun stop() {
        if (tts.isSpeaking) {
            tts.stop()
            _isSpeaking.value = false
        }
    }

    /**
     * Libera os recursos do TTS quando não for mais necessário.
     */
    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        private var instance: TextToSpeechService? = null

        fun getInstance(context: Context): TextToSpeechService {
            if (instance == null) {
                instance = TextToSpeechService(context.applicationContext)
            }
            return instance!!
        }
    }
}

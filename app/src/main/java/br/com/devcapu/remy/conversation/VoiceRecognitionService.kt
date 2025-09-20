package br.com.devcapu.remy.conversation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.RecognizerIntent.EXTRA_LANGUAGE
import android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL
import android.speech.RecognizerIntent.EXTRA_MAX_RESULTS
import android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS
import android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
import android.speech.SpeechRecognizer
import android.speech.SpeechRecognizer.RESULTS_RECOGNITION
import android.util.Log
import java.util.Locale

const val TAG = "VoiceRecognitionService"

class VoiceRecognitionService(
    private val context: Context,
    private val callback: VoiceRecognitionCallback
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private lateinit var recognitionIntent: Intent

    interface VoiceRecognitionCallback {
        fun onCommandRecognized(command: VoiceCommand)
        fun onTextRecognized(text: String) // Added this line
        fun onError(error: String)
        fun onListeningStateChanged(isListening: Boolean)
    }

    enum class VoiceCommand {
        NEXT_STEP,
        PREVIOUS_STEP,
        REPEAT_STEP,
        INGREDIENTS,
        START_TIMER,
        STOP_TIMER,
        UNKNOWN,
    }

    init {
        initializeSpeechRecognizer()
        prepareIntent()
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                .apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            callback.onListeningStateChanged(true)
                            Log.d(TAG, "🎤 Speech Recognition: Pronto para escutar")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(TAG, "🎤 Speech Recognition: Detectou início de fala")
                        }

                        override fun onRmsChanged(rmsdB: Float) = Unit
                        override fun onBufferReceived(buffer: ByteArray?) = Unit

                        override fun onEndOfSpeech() {
                            Log.d(TAG, "🎤 Speech Recognition: Detectou fim de fala")
                            isListening = false
                            callback.onListeningStateChanged(false)
                        }

                        override fun onError(error: Int) {
                            val errorMessage = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio"
                                SpeechRecognizer.ERROR_CLIENT -> "Erro do cliente"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissões insuficientes"
                                SpeechRecognizer.ERROR_NETWORK -> "Erro de rede"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de rede"
                                SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma correspondência encontrada"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconhecedor ocupado"
                                SpeechRecognizer.ERROR_SERVER -> "Erro do servidor"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout de fala"
                                else -> error
                            }
                            Log.e(TAG, "🎤 Speech Recognition: Erro - $errorMessage")
                            callback.onError(error.toString())
                        }

                        override fun onResults(results: Bundle) {
                            val matches = results.getStringArrayList(RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val command = matches[0]
                                Log.d(TAG, "🎤 Speech Recognition: Comando reconhecido - '$command'")
                                val parsedCommand = parseCommand(command)
                                Log.d(
                                    TAG,
                                    "🎤 Speech Recognition: Comando interpretado - $parsedCommand"
                                )
                                callback.onCommandRecognized(parsedCommand)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(RESULTS_RECOGNITION)
                            if (matches != null && matches.isNotEmpty()) {
                                Log.d(
                                    this@VoiceRecognitionService.javaClass.simpleName,
                                    "onPartialResults: ${matches[0]}"
                                )
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    })
                }
        }
    }

    private fun prepareIntent() {
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM)
            putExtra(EXTRA_LANGUAGE, Locale("pt", "BR"))
            putExtra(EXTRA_PARTIAL_RESULTS, true)
            putExtra(EXTRA_MAX_RESULTS, 1)
        }
    }

    fun startListening() {
        Log.d(TAG, "🎤 Controle do microfone: Speech Recognition está assumindo o controle")
        speechRecognizer?.startListening(recognitionIntent)
    }

    fun stopListening() {
        Log.d(TAG, "🎤 Controle do microfone: Speech Recognition está liberando o controle")
        speechRecognizer?.stopListening()
        isListening = false
        callback.onListeningStateChanged(false)
    }

    private fun parseCommand(text: String) = when {
        text.contains("próximo") || text.contains("proximo") -> VoiceCommand.NEXT_STEP
        text.contains("anterior") || text.contains("volta") -> VoiceCommand.PREVIOUS_STEP
        text.contains("ingredientes") -> VoiceCommand.INGREDIENTS
        text.contains("repetir") || text.contains("repete") -> VoiceCommand.REPEAT_STEP
        text.contains("timer") || text.contains("cronômetro") || text.contains("cronometro") -> {
            if (text.contains("parar") || text.contains("pausar") || text.contains("cancelar")) {
                VoiceCommand.STOP_TIMER
            } else {
                VoiceCommand.START_TIMER
            }
        }

        else -> VoiceCommand.UNKNOWN
    }


    /**
     * Aqui acho que deveria ser atrelado ao ciclo de vida da Application, ai podemos chamar esse
     * método no onTerminate() da Application.
     */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

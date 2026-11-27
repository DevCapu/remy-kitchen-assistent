package br.com.devcapu.remy.infra

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import br.com.devcapu.remy.conversation.wakeword.VOSK_GRAMMAR_JSON
import br.com.devcapu.remy.conversation.wakeword.VoskWakeWordMatcher
import br.com.devcapu.remy.conversation.wakeword.WakeWordEngine
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "VoskWakeWordEngine"

class VoskWakeWordEngine(
    context: Context,
) : WakeWordEngine {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "VoskWakeWordExecutor").apply { isDaemon = true }
        }

    private val loadLock = Any()
    private val pendingLoadCallbacks = mutableListOf<Pair<() -> Unit, (Throwable) -> Unit>>()
    private var isLoadingModel = false

    @Volatile
    private var released = false

    @Volatile
    private var isListeningActive = false

    @Volatile
    private var activeSessionId = 0L

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var activeWakeWordCallback: (() -> Unit)? = null
    private var activeErrorCallback: ((Throwable) -> Unit)? = null

    override fun loadModel(
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val shouldStartUnpack =
            synchronized(loadLock) {
                if (released) {
                    mainHandler.post {
                        onError(IllegalStateException("VoskWakeWordEngine já foi liberado"))
                    }
                    return
                }
                if (model != null) {
                    mainHandler.post { onSuccess() }
                    return
                }
                pendingLoadCallbacks += onSuccess to onError
                if (isLoadingModel) {
                    false
                } else {
                    isLoadingModel = true
                    true
                }
            }

        if (!shouldStartUnpack) return

        try {
            StorageService.unpack(
                appContext,
                MODEL_ASSET_PATH,
                MODEL_TARGET_DIR,
                { unpackedModel -> handleModelUnpacked(unpackedModel) },
                { exception -> handleModelUnpackError(exception) },
            )
        } catch (throwable: Throwable) {
            handleModelUnpackError(throwable)
        }
    }

    override fun startListening(
        onWakeWordDetected: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        if (released) return
        executeOnWorker {
            if (released) return@executeOnWorker
            startListeningInternal(onWakeWordDetected, onError)
        }
    }

    override fun stopListening(onStopped: () -> Unit) {
        isListeningActive = false
        val scheduled =
            executeOnWorker {
                stopInternal()
                mainHandler.post { onStopped() }
            }
        if (!scheduled) {
            mainHandler.post { onStopped() }
        }
    }

    override fun release() {
        synchronized(loadLock) {
            if (released) return
            released = true
            isLoadingModel = false
            pendingLoadCallbacks.clear()
        }
        isListeningActive = false

        executeOnWorker {
            stopInternal()
            val modelToClose =
                synchronized(loadLock) {
                    val current = model
                    model = null
                    current
                }
            runCatching { modelToClose?.close() }
        }
        executor.shutdown()
    }

    private fun handleModelUnpacked(unpackedModel: Model) {
        val callbacks =
            synchronized(loadLock) {
                isLoadingModel = false
                val drained = pendingLoadCallbacks.toList()
                pendingLoadCallbacks.clear()
                if (released) {
                    runCatching { unpackedModel.close() }
                    emptyList()
                } else {
                    model = unpackedModel
                    drained
                }
            }

        if (callbacks.isNotEmpty()) {
            mainHandler.post {
                callbacks.forEach { (onSuccess, _) -> onSuccess() }
            }
        }
    }

    private fun handleModelUnpackError(throwable: Throwable) {
        Log.e(TAG, "Falha ao desempacotar modelo Vosk", throwable)
        val callbacks =
            synchronized(loadLock) {
                isLoadingModel = false
                val drained = pendingLoadCallbacks.toList()
                pendingLoadCallbacks.clear()
                if (released) emptyList() else drained
            }

        if (callbacks.isNotEmpty()) {
            mainHandler.post {
                callbacks.forEach { (_, onError) -> onError(throwable) }
            }
        }
    }

    private fun startListeningInternal(
        onWakeWordDetected: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        stopInternal()

        val loadedModel = synchronized(loadLock) { model }
        if (loadedModel == null) {
            mainHandler.post {
                onError(IllegalStateException("Modelo Vosk não carregado"))
            }
            return
        }

        val sessionId = ++activeSessionId
        activeWakeWordCallback = onWakeWordDetected
        activeErrorCallback = onError

        try {
            val newRecognizer = Recognizer(loadedModel, SAMPLE_RATE, VOSK_GRAMMAR_JSON)
            val newSpeechService = SpeechService(newRecognizer, SAMPLE_RATE)
            recognizer = newRecognizer
            speechService = newSpeechService
            isListeningActive = true

            val started = newSpeechService.startListening(createRecognitionListener(sessionId))
            if (!started) {
                throw IOException("Não foi possível iniciar SpeechService do Vosk")
            }
            Log.d(TAG, "Vosk escutando wake word")
        } catch (throwable: Throwable) {
            Log.e(TAG, "Erro ao iniciar escuta Vosk", throwable)
            stopInternal()
            if (!released) {
                mainHandler.post { onError(throwable) }
            }
        }
    }

    private fun stopInternal() {
        activeSessionId++
        isListeningActive = false
        val serviceToStop = speechService
        val recognizerToClose = recognizer
        speechService = null
        recognizer = null

        runCatching { serviceToStop?.stop() }
        runCatching { serviceToStop?.shutdown() }
        runCatching { recognizerToClose?.close() }
    }

    private fun createRecognitionListener(sessionId: Long): RecognitionListener =
        object : RecognitionListener {
            override fun onPartialResult(hypothesis: String?) {
                handleHypothesis(sessionId, hypothesis)
            }

            override fun onResult(hypothesis: String?) {
                handleHypothesis(sessionId, hypothesis)
            }

            override fun onFinalResult(hypothesis: String?) {
                handleHypothesis(sessionId, hypothesis)
            }

            override fun onError(exception: Exception?) {
                if (released || !isListeningActive || sessionId != activeSessionId) return
                val error = exception ?: IllegalStateException("Erro desconhecido no Vosk")
                Log.e(TAG, "Erro durante escuta Vosk", error)
                val callback = activeErrorCallback
                mainHandler.post { callback?.invoke(error) }
            }

            override fun onTimeout() {
                if (released || !isListeningActive || sessionId != activeSessionId) return
                Log.d(TAG, "Timeout Vosk em escuta contínua; reiniciando")
                executeOnWorker {
                    if (!released && isListeningActive && sessionId == activeSessionId) {
                        val wakeCallback = activeWakeWordCallback
                        val errorCallback = activeErrorCallback
                        if (wakeCallback != null && errorCallback != null) {
                            startListeningInternal(wakeCallback, errorCallback)
                        }
                    }
                }
            }
        }

    private fun handleHypothesis(
        sessionId: Long,
        hypothesis: String?,
    ) {
        if (released || !isListeningActive || sessionId != activeSessionId) return
        if (VoskWakeWordMatcher.matchesHypothesis(hypothesis)) {
            Log.d(TAG, "Wake word detectada pelo Vosk")
            val callback = activeWakeWordCallback
            mainHandler.post {
                if (!released && sessionId == activeSessionId) {
                    callback?.invoke()
                }
            }
        }
    }

    private fun executeOnWorker(task: () -> Unit): Boolean {
        if (executor.isShutdown) return false
        return runCatching {
            executor.execute(task)
            true
        }.getOrDefault(false)
    }

    companion object {
        const val MODEL_ASSET_PATH = "vosk-model-small-pt-0.3"
        const val MODEL_TARGET_DIR = "vosk-model-pt"
        const val SAMPLE_RATE = 16000.0f
    }
}

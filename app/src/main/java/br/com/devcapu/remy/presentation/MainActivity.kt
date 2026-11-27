package br.com.devcapu.remy.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import br.com.devcapu.remy.conversation.LanguageConfigChecker
import br.com.devcapu.remy.conversation.TextToSpeechService
import br.com.devcapu.remy.conversation.VoiceRecognitionService
import br.com.devcapu.remy.conversation.wakeword.WakeWordCoordinator
import br.com.devcapu.remy.infra.VoskWakeWordEngine
import br.com.devcapu.remy.navigation.Routes
import br.com.devcapu.remy.presentation.auth.screen.LoginScreen
import br.com.devcapu.remy.presentation.auth.screen.SignUpScreen
import br.com.devcapu.remy.presentation.recipe.screen.RecipeDetailsScreen
import br.com.devcapu.remy.presentation.recipe.screen.RecipeListScreen
import br.com.devcapu.remy.presentation.voice.VoiceOrbMode
import br.com.devcapu.remy.presentation.voice.VoiceOrbStateController
import br.com.devcapu.remy.ui.theme.RemyTheme
import java.util.concurrent.atomic.AtomicReference

private enum class PendingAudioAction {
    WAKE_WORD,
    MANUAL_COMMAND,
}

class MainActivity : ComponentActivity() {
    private val backStack = mutableStateListOf<Routes>(Routes.Login)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LanguageConfigChecker.apply {
            setPortugueseLocale(this@MainActivity, "BR")
            showConfigurationSummary(this@MainActivity)
        }

        enableEdgeToEdge()
        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            var currentRoute by remember { mutableStateOf(backStack.lastOrNull()) }
            var isChefMode by remember { mutableStateOf(false) }
            val ttsService = remember { TextToSpeechService.getInstance(this) }
            val canSpeak by ttsService.isTtsReady.collectAsState()
            val isSpeaking by ttsService.isSpeaking.collectAsState()
            val voiceOrbController = remember { VoiceOrbStateController() }
            val voiceOrbState by voiceOrbController.state.collectAsState()
            var voiceCommand by remember { mutableStateOf<VoiceRecognitionService.VoiceCommand?>(null) }
            var pendingAudioAction by remember { mutableStateOf<PendingAudioAction?>(null) }

            val voiceRecognitionServiceRef = remember { AtomicReference<VoiceRecognitionService?>(null) }
            val wakeWordEngine = remember { VoskWakeWordEngine(applicationContext) }
            val wakeWordCoordinator =
                remember(wakeWordEngine, ttsService) {
                    WakeWordCoordinator(
                        engine = wakeWordEngine,
                        onStartCommandRecognition = {
                            ttsService.stop()
                            voiceRecognitionServiceRef.get()?.startListening()
                        },
                    )
                }
            val wakeWordState by wakeWordCoordinator.state.collectAsState()

            val voiceRecognitionCallback =
                remember(voiceOrbController, wakeWordCoordinator) {
                    object : VoiceRecognitionService.VoiceRecognitionCallback {
                        override fun onCommandRecognized(command: VoiceRecognitionService.VoiceCommand) {
                            voiceCommand = command
                        }

                        override fun onTextRecognized(text: String) = Unit

                        override fun onError(error: String) {
                            voiceOrbController.onRecognitionError()
                            wakeWordCoordinator.onCommandSessionFinished()
                        }

                        override fun onListeningStateChanged(isListening: Boolean) {
                            voiceOrbController.onListeningChanged(isListening)
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            voiceOrbController.onAudioRmsChanged(rmsdB)
                        }
                    }
                }
            val voiceRecognitionService =
                remember(voiceRecognitionCallback) {
                    VoiceRecognitionService(this, voiceRecognitionCallback).also {
                        voiceRecognitionServiceRef.set(it)
                    }
                }

            val audioPermissionLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    val action = pendingAudioAction
                    pendingAudioAction = null
                    if (granted) {
                        when (action) {
                            PendingAudioAction.WAKE_WORD ->
                                wakeWordCoordinator.enable(hasAudioPermission = true)
                            PendingAudioAction.MANUAL_COMMAND ->
                                wakeWordCoordinator.onManualCommandStart()
                            null -> Unit
                        }
                    } else {
                        wakeWordCoordinator.enable(hasAudioPermission = false)
                    }
                }

            val hasAudioPermission: () -> Boolean = {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            }

            LaunchedEffect(isSpeaking) {
                voiceOrbController.onSpeakingChanged(isSpeaking)
            }

            LaunchedEffect(isChefMode) {
                if (!isChefMode) {
                    wakeWordCoordinator.onPauseSession()
                    voiceRecognitionService.stopListening()
                    ttsService.stop()
                    voiceOrbController.reset()
                }
            }

            DisposableEffect(lifecycleOwner, wakeWordCoordinator, voiceRecognitionService) {
                val observer =
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            wakeWordCoordinator.onPauseSession()
                            voiceRecognitionService.stopListening()
                        }
                    }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    wakeWordCoordinator.release()
                    voiceRecognitionService.destroy()
                }
            }

            RemyTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        if (currentRoute !is Routes.Details) return@Scaffold

                        FloatingActionButton(onClick = { isChefMode = !isChefMode }) {
                            Icon(
                                imageVector = if (isChefMode) Icons.Default.Pause else Icons.Default.LocalFireDepartment,
                                contentDescription = "Iniciar escuta",
                            )
                        }
                    },
                    floatingActionButtonPosition = FabPosition.End,
                    containerColor = Color.Black,
                ) { innerPadding ->
                    NavDisplay(
                        modifier = Modifier.padding(innerPadding),
                        backStack = backStack,
                        onBack = {
                            backStack.removeLastOrNull()
                            isChefMode = false
                            wakeWordCoordinator.onPauseSession()
                            voiceRecognitionService.stopListening()
                            ttsService.stop()
                            voiceOrbController.reset()
                        },
                        entryProvider = { key ->
                            currentRoute = key
                            when (key) {
                                is Routes.Login ->
                                    NavEntry(key) {
                                        LoginScreen(
                                            modifier = Modifier.fillMaxSize(),
                                            onLoginSuccess = {
                                                backStack.clear()
                                                backStack.add(Routes.List)
                                            },
                                            onNavigateToSignUp = {
                                                backStack.clear()
                                                backStack.add(Routes.SignUp)
                                            },
                                        )
                                    }

                                is Routes.SignUp ->
                                    NavEntry(key) {
                                        SignUpScreen(
                                            modifier = Modifier.fillMaxSize(),
                                            onSignUpSuccess = {
                                                backStack.clear()
                                                backStack.add(Routes.List)
                                            },
                                            onNavigateToLogin = {
                                                backStack.clear()
                                                backStack.add(Routes.Login)
                                            },
                                        )
                                    }

                                is Routes.List ->
                                    NavEntry(key) {
                                        RecipeListScreen(
                                            modifier = Modifier.fillMaxSize(),
                                            onClickItem = { recipe ->
                                                backStack.add(Routes.Details(recipe))
                                            },
                                        )
                                    }

                                is Routes.Details ->
                                    NavEntry(key) {
                                        RecipeDetailsScreen(
                                            recipe = key.recipe,
                                            modifier = Modifier.fillMaxSize(),
                                            isChefMode = isChefMode,
                                            canSpeak = canSpeak,
                                            onSpeak = { text, queueMode -> ttsService.speak(text, queueMode) },
                                            onStopSpeaking = { ttsService.stop() },
                                            isListening = wakeWordState.isEnabled,
                                            onToggleListening = {
                                                if (wakeWordState.isEnabled) {
                                                    wakeWordCoordinator.disable()
                                                } else if (hasAudioPermission()) {
                                                    wakeWordCoordinator.enable(hasAudioPermission = true)
                                                } else {
                                                    pendingAudioAction = PendingAudioAction.WAKE_WORD
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            },
                                            voiceCommand = voiceCommand,
                                            onVoiceCommandHandled = {
                                                voiceCommand = null
                                                wakeWordCoordinator.onCommandSessionFinished()
                                            },
                                            voiceOrbState = voiceOrbState,
                                            onVoiceOrbClick = {
                                                when (voiceOrbState.mode) {
                                                    VoiceOrbMode.SPEAKING -> ttsService.stop()
                                                    VoiceOrbMode.LISTENING -> {
                                                        voiceRecognitionService.stopListening()
                                                        wakeWordCoordinator.onCommandSessionFinished()
                                                    }

                                                    VoiceOrbMode.IDLE -> {
                                                        if (hasAudioPermission()) {
                                                            wakeWordCoordinator.onManualCommandStart()
                                                        } else {
                                                            pendingAudioAction = PendingAudioAction.MANUAL_COMMAND
                                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                        }
                                                    }
                                                }
                                            },
                                            wakeWordState = wakeWordState,
                                        )
                                    }
                            }
                        },
                    )
                }
            }
        }
    }
}

package br.com.devcapu.remy.presentation

import ai.picovoice.porcupine.PorcupineManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import br.com.devcapu.remy.conversation.LanguageConfigChecker
import br.com.devcapu.remy.conversation.TextToSpeechService
import br.com.devcapu.remy.conversation.VoiceRecognitionService
import br.com.devcapu.remy.infra.PorcupineManagerSingleton
import br.com.devcapu.remy.navigation.Routes
import br.com.devcapu.remy.presentation.auth.screen.LoginScreen
import br.com.devcapu.remy.presentation.auth.screen.SignUpScreen
import br.com.devcapu.remy.presentation.recipe.screen.RecipeDetailsScreen
import br.com.devcapu.remy.presentation.recipe.screen.RecipeListScreen
import br.com.devcapu.remy.ui.theme.RemyTheme

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
            var currentRoute by remember { mutableStateOf(backStack.lastOrNull()) }
            var isChefMode by remember { mutableStateOf(false) }
            val ttsService = remember { TextToSpeechService.getInstance(this) }
            val canSpeak by ttsService.isTtsReady.collectAsState()
            var isListening by remember { mutableStateOf(false) }
            var voiceCommand by remember { mutableStateOf<VoiceRecognitionService.VoiceCommand?>(null) }
            val voiceRecognitionCallback = remember {
                object : VoiceRecognitionService.VoiceRecognitionCallback {
                    override fun onCommandRecognized(command: VoiceRecognitionService.VoiceCommand) {
                        voiceCommand = command
                    }
                    override fun onTextRecognized(text: String) = Unit
                    override fun onError(error: String) {

                    }
                    override fun onListeningStateChanged(listening: Boolean) = Unit
                }
            }
            val voiceRecognitionService = remember { VoiceRecognitionService(this, voiceRecognitionCallback) }
            val porcupineManager: PorcupineManager = remember { PorcupineManagerSingleton.getInstance(this, voiceRecognitionService) }

            RemyTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        if (currentRoute !is Routes.Details) return@Scaffold

                        FloatingActionButton(onClick = { isChefMode = !isChefMode }) {
                            Icon(
                                imageVector = if (isChefMode) Icons.Default.Pause else Icons.Default.LocalFireDepartment,
                                contentDescription = "Iniciar escuta"
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
                            ttsService.stop()
                            porcupineManager.stop()
                            isListening = false
                        },
                        entryProvider = { key ->
                            currentRoute = key
                            when (key) {
                                is Routes.Login -> NavEntry(key) {
                                    LoginScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        onLoginSuccess = {
                                            backStack.clear()
                                            backStack.add(Routes.List)
                                        },
                                        onNavigateToSignUp = {
                                            backStack.clear()
                                            backStack.add(Routes.SignUp)
                                        }
                                    )
                                }

                                is Routes.SignUp -> NavEntry(key) {
                                    SignUpScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        onSignUpSuccess = {
                                            backStack.clear()
                                            backStack.add(Routes.List)
                                        },
                                        onNavigateToLogin = {
                                            backStack.clear()
                                            backStack.add(Routes.Login)
                                        }
                                    )
                                }

                                is Routes.List -> NavEntry(key) {
                                    RecipeListScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        onClickItem = { recipe ->
                                            backStack.add(Routes.Details(recipe))
                                        }
                                    )
                                }

                                is Routes.Details -> NavEntry(key) {
                                    RecipeDetailsScreen(
                                        recipe = key.recipe,
                                        modifier = Modifier.fillMaxSize(),
                                        isChefMode = isChefMode,
                                        canSpeak = canSpeak,
                                        onSpeak = { text, queueMode -> ttsService.speak(text, queueMode) },
                                        onStopSpeaking = { ttsService.stop() },
                                        isListening = isListening,
                                        onToggleListening = {
                                            isListening = !isListening
                                            if (isListening) {
                                                porcupineManager.start()
                                            } else {
                                                porcupineManager.stop()
                                            }
                                        },
                                        voiceCommand = voiceCommand,
                                        onVoiceCommandHandled = {
                                            voiceCommand = null
                                            porcupineManager.start()
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
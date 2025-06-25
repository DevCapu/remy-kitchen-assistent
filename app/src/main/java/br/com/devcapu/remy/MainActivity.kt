package br.com.devcapu.remy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import br.com.devcapu.remy.conversation.LanguageConfigChecker
import br.com.devcapu.remy.conversation.VoiceRecognitionService
import br.com.devcapu.remy.navigation.Routes
import br.com.devcapu.remy.recipe.allRecipes
import br.com.devcapu.remy.recipe.presentation.screen.RecipeDetailsScreen
import br.com.devcapu.remy.recipe.presentation.screen.RecipeListScreen
import br.com.devcapu.remy.ui.theme.RemyTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity(), VoiceRecognitionService.VoiceRecognitionCallback {

    private var lastCommand by mutableStateOf<VoiceRecognitionService.VoiceCommand?>(null)
    private lateinit var voiceRecognitionService: VoiceRecognitionService

    // Movendo o backStack para o nível da Activity para acessar no onCommandRecognized
    private val backStack = mutableStateListOf<Routes>(Routes.List)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LanguageConfigChecker.checkPortugueseConfiguration(this)
        LanguageConfigChecker.showConfigurationSummary(this)

        voiceRecognitionService = VoiceRecognitionService(this, this)

        enableEdgeToEdge()
        setContent {
            RemyTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { voiceRecognitionService.startListening() }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Iniciar escuta")
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center,
                    containerColor = Color.Black,
                ) { innerPadding ->
                    NavDisplay(
                        modifier = Modifier.padding(innerPadding),
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
                        entryProvider = { key ->
                            when (key) {
                                is Routes.List -> NavEntry(key) {
                                    RecipeListScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        onClickItem = { recipeId ->
                                            val recipe =
                                                allRecipes.firstOrNull { it.id == recipeId }
                                            if (recipe != null) {
                                                backStack.add(Routes.Details(recipe.id))
                                            } else {
                                                Log.e(TAG, "Recipe not found: $recipeId")
                                            }
                                        }
                                    )
                                }

                                is Routes.Details -> NavEntry(key) {
                                    RecipeDetailsScreen(
                                        recipe = allRecipes.firstOrNull { it.id == key.id }
                                            ?: error("Recipe not found: ${key.id}"),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onCommandRecognized(command: VoiceRecognitionService.VoiceCommand) {
        Log.d(TAG, "Comando reconhecido: ${command.name}")
        lastCommand = command
        voiceRecognitionService.startListening()
    }

    override fun onError(error: String) {
        Log.e(TAG, "Erro no reconhecimento de voz: $error")
    }

    override fun onListeningStateChanged(isListening: Boolean) {

    }

    override fun onDestroy() {
        super.onDestroy()
        voiceRecognitionService.destroy()
    }

    fun bla() {
        val command = VoiceRecognitionService.VoiceCommand.NEXT_STEP
        when (command) {
            VoiceRecognitionService.VoiceCommand.NEXT_STEP -> {
                // Seleciona uma receita aleatória e navega para seus detalhes
                allRecipes.randomOrNull()?.let { randomRecipe ->
                    Log.d(TAG, "Navegando para receita aleatória: ${randomRecipe.id}")
                    backStack.add(Routes.Details(randomRecipe.id))
                }
            }

            else -> Unit
        }
    }
}
package br.com.devcapu.remy

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import br.com.devcapu.remy.navigation.Routes
import br.com.devcapu.remy.recipe.allRecipes
import br.com.devcapu.remy.recipe.presentation.screen.RecipeDetailsScreen
import br.com.devcapu.remy.recipe.presentation.screen.RecipeListScreen
import br.com.devcapu.remy.ui.theme.RemyTheme

class MainActivity : ComponentActivity() {

    private val backStack = mutableStateListOf<Routes>(Routes.List)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {

            var currentRoute by remember { mutableStateOf(backStack.lastOrNull()) }
            var isChefMode by remember { mutableStateOf(false) }

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
                        },
                        entryProvider = { key ->
                            currentRoute = key
                            when (key) {
                                is Routes.List -> NavEntry(key) {
                                    RecipeListScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        onClickItem = { recipeId ->
                                            allRecipes
                                                .firstOrNull { it.id == recipeId }
                                                ?.let {
                                                    backStack.add(Routes.Details(recipeId))
                                                }
                                        }
                                    )
                                }

                                is Routes.Details -> NavEntry(key) {
                                    RecipeDetailsScreen(
                                        recipe = allRecipes.firstOrNull { it.id == key.id }
                                            ?: error("Recipe not found: ${key.id}"),
                                        modifier = Modifier.fillMaxSize(),
                                        isChefMode = isChefMode
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
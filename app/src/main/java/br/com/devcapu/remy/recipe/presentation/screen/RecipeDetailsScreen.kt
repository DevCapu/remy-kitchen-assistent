package br.com.devcapu.remy.recipe.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.devcapu.remy.recipe.Recipe
import br.com.devcapu.remy.recipe.allRecipes

@Composable
fun RecipeDetailsScreen(
    modifier: Modifier = Modifier,
    recipe: Recipe = allRecipes[0],
    isChefMode: Boolean = false
) {
    val ingredients = recipe.ingredients

    var selectedStepIndex by remember { mutableIntStateOf(0) }
    var recipeHeaderHeight by remember { mutableIntStateOf(0) }
    val screenHeightDp = LocalWindowInfo.current.containerSize.height

    LazyColumn(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RecipeItem(
                modifier = Modifier.onGloballyPositioned {
                    recipeHeaderHeight = it.size.height
                },
                recipe = recipe, chefMode = isChefMode
            ) { }
        }

        if (isChefMode) {
            val currentStep = recipe.steps[selectedStepIndex]
            val ingredientsOnCurrentStep = ingredients.count { currentStep.contains(it.name, true) }

            if (ingredientsOnCurrentStep > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Ingredientes", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            ingredients
                                .filter { recipe.steps[selectedStepIndex].contains(it.name, true) }
                                .forEach { ingredient ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            ingredient.name,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text("${ingredient.quantity} ${ingredient.unit.orEmpty()}")
                                    }
                                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                                }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text("Ingredientes", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        ingredients.forEach { ingredient ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ingredient.name, style = MaterialTheme.typography.labelLarge)
                                Text("${ingredient.quantity} ${ingredient.unit.orEmpty()}")
                            }
                            HorizontalDivider(Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.padding(top = 8.dp)) {
                if (!isChefMode) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Modo de preparo", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        recipe.steps.forEachIndexed { idx, step ->
                            Text(
                                style = MaterialTheme.typography.displaySmall,
                                text = "${idx + 1}. $step"
                            )
                        }
                    }
                } else {
                    val height = screenHeightDp - recipeHeaderHeight

                    CircularList(
                        modifier = Modifier
                            .height(with(LocalDensity.current) { height.toDp() })
                            .padding(16.dp)
                            .fillMaxWidth(),
                        items = recipe.steps,
                        isEndless = false,
                        selectedStepIndex = selectedStepIndex
                    ) {
                        selectedStepIndex = it
                    }
                }

            }
        }
    }
}

@Composable
fun CircularList(
    items: List<String>,
    modifier: Modifier = Modifier,
    isEndless: Boolean = false,
    selectedStepIndex: Int,
    onItemClick: (index: Int) -> Unit = { }
) {
    val listState = rememberLazyListState(
        if (isEndless) Int.MAX_VALUE / 2 else 0
    )

    LaunchedEffect(selectedStepIndex) {
        listState.animateScrollToItem(selectedStepIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            count = if (isEndless) Int.MAX_VALUE else items.size,
            itemContent = {
                val index = it % items.size
                val textModifier = if (index == selectedStepIndex) {
                    Modifier
                        .background(
                            color = Color(0x3B55FF00),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                }
                Text(
                    modifier = textModifier.clickable { onItemClick(index) },
                    style = MaterialTheme.typography.headlineMedium,
                    text = items[index],
                    color = if (index == selectedStepIndex) Color.Black else Color.Black.copy(alpha = 0.5f)
                )
            }
        )
    }
}

@Preview(showSystemUi = true, showBackground = false, name = "Tela")
@Composable
private fun RecipeScreenPreview() {
    RecipeDetailsScreen(isChefMode = true)
}
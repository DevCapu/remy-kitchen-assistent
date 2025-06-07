package br.com.devcapu.remy.recipe.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.devcapu.remy.recipe.Recipe
import br.com.devcapu.remy.recipe.allRecipes

@Composable
fun RecipeDetailsScreen(
    recipe: Recipe = allRecipes[0],
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RecipeItem(recipe)
        }
        item {
            Card(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Ingredientes", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    recipe.ingredients.forEach { ingredient ->
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
        item {
            Card(modifier = Modifier.padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Modo de preparo", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    recipe.steps.forEachIndexed { idx, step ->
                        Text("${idx + 1}. $step")
                    }
                }
            }
        }
    }
}

@Preview(backgroundColor = 0x000000, showBackground = true)
@Composable
private fun RecipeDetailsScreenPreview() {
    RecipeDetailsScreen(allRecipes[3])
}
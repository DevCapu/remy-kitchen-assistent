package br.com.devcapu.remy.recipe.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.devcapu.remy.recipe.Recipe

@Composable
fun RecipeListScreen(
    modifier: Modifier = Modifier,
    onClickItem: (Recipe) -> Unit = { },
    viewModel: RecipeListViewModel = viewModel()
) {
    val recipes = viewModel.recipes.collectAsState().value
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .background(Color.Black),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(recipes.size) { index ->
            RecipeItem(recipe = recipes[index]) { onClickItem(recipes[index]) }
        }
    }
}


@Composable
fun RecipeItem(
    modifier: Modifier = Modifier,
    chefMode: Boolean = false,
    recipe: Recipe,
    onClickItem: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(true) { onClickItem() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(6.dp, Color.White),
        shape = MaterialTheme.shapes.large
    ) {
        if (!chefMode) {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(RecipeDrawableProvider.getDrawableResId(recipe.id)),
                contentDescription = recipe.name,
                contentScale = ContentScale.Crop
            )
        }

        Text(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            text = recipe.name,
            style = MaterialTheme.typography.displaySmall
        )
        HorizontalDivider(
            Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            recipe.totalTime?.let {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tempo",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(recipe.totalTime)
                }
            }

            VerticalDivider(
                Modifier.height(32.dp),
                thickness = 1.dp,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Serve",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("${recipe.servings} pessoas")
            }

        }
    }
}

@Preview(showSystemUi = true, showBackground = false, name = "Tela")
@Composable
private fun RecipeScreenPreview() {
    RecipeListScreen()
}

@Preview
@Composable
private fun RecipeItemPreview() {
    RecipeItem(
        recipe = Recipe(
            name = "Preview",
            ingredients = emptyList(),
            steps = listOf("Passo 1", "Passo 2")
        )
    ) { }
}
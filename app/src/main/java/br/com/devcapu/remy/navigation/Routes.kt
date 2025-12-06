package br.com.devcapu.remy.navigation

import androidx.navigation3.runtime.NavKey
import br.com.devcapu.remy.data.recipe.Recipe
import kotlinx.serialization.Serializable

@Serializable
sealed class Routes: NavKey {
    object List : Routes()
    class Details(val recipe: Recipe) : Routes()
}
package br.com.devcapu.remy.presentation.recipe.provider

import br.com.devcapu.remy.R

object RecipeDrawableProvider {
    private val recipeDrawableMap = mapOf(
        "virado-paulista" to R.drawable.virada_paulista,
        "bife-a-role" to R.drawable.bife_a_role,
        "feijoada-completa" to R.drawable.feijoada,
        "espaguete-bolonhesa" to R.drawable.macarrao_bolonhesa,
        "pescada-assada" to R.drawable.pescada
    )

    fun getDrawableResId(recipeId: String): Int {
        return recipeDrawableMap[recipeId] ?: R.drawable.ic_launcher_background
    }
}
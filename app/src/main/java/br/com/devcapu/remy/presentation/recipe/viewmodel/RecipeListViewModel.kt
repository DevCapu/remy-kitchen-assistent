package br.com.devcapu.remy.presentation.recipe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.devcapu.remy.data.recipe.Recipe
import br.com.devcapu.remy.data.recipe.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecipeListViewModel : ViewModel() {
    private val repository = RecipeRepository()
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes

    init {
        fetchRecipes()
    }

    private fun fetchRecipes() {
        viewModelScope.launch {
            try {
                _recipes.value = repository.fetchRecipes()
            } catch (e: Exception) {
                e.printStackTrace()
                // Em caso de erro, mantém lista vazia
                _recipes.value = emptyList()
            }
        }
    }
}
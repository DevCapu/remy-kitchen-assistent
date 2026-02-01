package br.com.devcapu.remy.infra.repository

import br.com.devcapu.remy.data.recipe.Recipe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class RecipeRepository {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.ALL
            logger = Logger.DEFAULT
        }
    }

    suspend fun fetchRecipes(): List<Recipe> {
        val response: String =
            client.get("https://raw.githubusercontent.com/DevCapu/remy-kitchen-assistent/refs/heads/main/remy-recipes.json")
                .body()

        return Json.decodeFromString(ListSerializer(Recipe.serializer()), response)
    }
}

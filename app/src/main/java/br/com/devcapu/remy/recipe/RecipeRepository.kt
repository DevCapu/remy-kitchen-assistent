package br.com.devcapu.remy.recipe

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

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
        val response: String = client.get("https://raw.githubusercontent.com/DevCapu/remy-kitchen-assistent/refs/heads/main/remy-recipes.json").body()
        // O arquivo já está em JSON puro, não precisa decodificar base64
        return Json.decodeFromString(ListSerializer(Recipe.serializer()), response)
    }
}

@Serializable
private data class GithubFileResponse(
    val content: String
)

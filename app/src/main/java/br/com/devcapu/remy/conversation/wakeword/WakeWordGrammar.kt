package br.com.devcapu.remy.conversation.wakeword

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

const val WAKE_WORD_PHRASE = "ei remy"
private const val UNKNOWN_TOKEN = "[unk]"

val ACCEPTED_WAKE_PHRASES: Set<String> = setOf("ei remy", "ei remi")

val VOSK_GRAMMAR_JSON: String =
    JsonArray(
        (ACCEPTED_WAKE_PHRASES.toList() + UNKNOWN_TOKEN).map(::JsonPrimitive),
    ).toString()

object VoskWakeWordMatcher {
    private val whitespaceRegex = Regex("\\s+")
    private val acceptedTokenSequences: List<List<String>> =
        ACCEPTED_WAKE_PHRASES.map { phrase ->
            phrase.lowercase(Locale.ROOT).split(whitespaceRegex)
        }

    fun matchesHypothesis(rawJson: String?): Boolean {
        if (rawJson.isNullOrBlank()) return false
        val hypothesis = extractHypothesisText(rawJson) ?: return false
        val normalizedTokens =
            hypothesis
                .trim()
                .lowercase(Locale.ROOT)
                .split(whitespaceRegex)
                .filter { it.isNotEmpty() }

        if (normalizedTokens.isEmpty()) return false

        return acceptedTokenSequences.any { targetTokens ->
            containsContiguousSequence(normalizedTokens, targetTokens)
        }
    }

    private fun extractHypothesisText(rawJson: String): String? {
        val jsonObject =
            runCatching { Json.parseToJsonElement(rawJson) as? JsonObject }
                .getOrNull()
                ?: return null

        val textValue = jsonObject.stringValueOrNull("text")
        if (!textValue.isNullOrBlank()) return textValue

        return jsonObject.stringValueOrNull("partial")?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.stringValueOrNull(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun containsContiguousSequence(
        tokens: List<String>,
        target: List<String>,
    ): Boolean {
        if (target.isEmpty() || tokens.size < target.size) return false
        val lastStartIndex = tokens.size - target.size
        for (startIndex in 0..lastStartIndex) {
            var matched = true
            for (offset in target.indices) {
                if (tokens[startIndex + offset] != target[offset]) {
                    matched = false
                    break
                }
            }
            if (matched) return true
        }
        return false
    }
}

package br.com.devcapu.remy.conversation.wakeword

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoskWakeWordMatcherTest {
    @Test
    fun `matches exact wake phrase in text and partial json`() {
        assertTrue(VoskWakeWordMatcher.matchesHypothesis("""{"text": "ei remy"}"""))
        assertTrue(VoskWakeWordMatcher.matchesHypothesis("""{"partial": "ei remy"}"""))
    }

    @Test
    fun `matches phonetic variant ei remi in text and partial json`() {
        assertTrue(VoskWakeWordMatcher.matchesHypothesis("""{"text": "ei remi"}"""))
        assertTrue(VoskWakeWordMatcher.matchesHypothesis("""{"partial": "ei remi"}"""))
    }

    @Test
    fun `normalizes extra spaces mixed case and surrounding unknown tokens`() {
        assertTrue(VoskWakeWordMatcher.matchesHypothesis("""{"text": "  EI   Remy  "}"""))
        assertTrue(VoskWakeWordMatcher.matchesHypothesis("""{"text": "[unk] ei remy"}"""))
        assertTrue(VoskWakeWordMatcher.matchesHypothesis("""{"partial": "ei remi [unk]"}"""))
    }

    @Test
    fun `rejects unknown token and empty hypotheses`() {
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"text": "[unk]"}"""))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"partial": "[unk]"}"""))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"text": ""}"""))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"partial": ""}"""))
    }

    @Test
    fun `rejects single words split tokens substrings and unrelated speech`() {
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"text": "ei"}"""))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"text": "remy"}"""))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"text": "ei [unk] remy"}"""))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"text": "xei remy"}"""))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"text": "ei remyx"}"""))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"text": "próximo passo da receita"}"""))
    }

    @Test
    fun `handles null empty and malformed json safely without throwing`() {
        assertFalse(VoskWakeWordMatcher.matchesHypothesis(null))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis(""))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("   "))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("{malformed"))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("[]"))
        assertFalse(VoskWakeWordMatcher.matchesHypothesis("""{"text": 123}"""))
    }

    @Test
    fun `grammar json contains accepted phrases and unknown token`() {
        assertTrue(VOSK_GRAMMAR_JSON.contains(""""ei remy""""))
        assertTrue(VOSK_GRAMMAR_JSON.contains(""""ei remi""""))
        assertTrue(VOSK_GRAMMAR_JSON.contains(""""[unk]""""))
    }
}

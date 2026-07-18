package com.example

import android.content.Context
import java.util.*

/**
 * High-performance, low-memory Local AI Engine.
 * Uses pattern matching, intent classification, structured grammar rules,
 * and a localized vocabulary base for near-0ms local AI responses.
 */
class LocalAiEngine {

    private val intentMap = mapOf(
        "hey" to listOf("Hey! 👋", "Hello hello! How can I help?", "Hi there, what's on your mind? ✨"),
        "how are you" to listOf("Doing absolute best, what about you? 🚀", "All great! Life is beautiful.", "Feeling productive and ready! 😄"),
        "thik" to listOf("Sahi h bhai, no issues! 👍", "Thik h, copy that.", "Bilkul, done deal."),
        "kya ho rha" to listOf("Bas typing works and coding 😂", "Chilling and thinking about the future.", "Aap batao, what's cooking? ☕"),
        "bye" to listOf("Catch you later! TC 👋", "Chal milte h baad me! Bbye.", "Take care, talk soon!"),
        "ok" to listOf("Acknowledged! 👍", "Haan thik h.", "Perfect, got it!"),
        "thanks" to listOf("No problem! Always here to assist ✨", "Anytime buddy! 🙌", "Mention not! Glad I could help.")
    )

    private val slangMap = mapOf(
        "bro" to "bhai",
        "friend" to "yaar",
        "great" to "ek dum mast",
        "cool" to "sahi h",
        "good" to "badhiya",
        "brother" to "bhaiya",
        "family" to "fam",
        "really" to "seriously",
        "awesome" to "cut-to-cut superb",
        "tired" to "exhausted"
    )

    private val grammarRules = mapOf(
        "i is" to "I am",
        "you is" to "you are",
        "they is" to "they are",
        "he are" to "he is",
        "she are" to "she is",
        "i am do" to "i am doing",
        "me is" to "I am",
        "does you" to "do you",
        "dont is" to "is not",
        "should has" to "should have",
        "could has" to "could have",
        "would has" to "would have"
    )

    private val emojiMap = mapOf(
        "happy" to "😊",
        "sad" to "😢",
        "love" to "❤️",
        "fire" to "🔥",
        "ok" to "👌",
        "cool" to "😎",
        "party" to "🥳",
        "nice" to "✨",
        "haha" to "😂",
        "lol" to "😂",
        "funny" to "🤣",
        "wow" to "🤩",
        "sorry" to "🙏",
        "agree" to "🤝",
        "rocket" to "🚀",
        "coffee" to "☕",
        "drink" to "🥤",
        "perfect" to "💯",
        "danger" to "⚠️",
        "work" to "💻"
    )

    /**
     * Attempts to fix basic grammar locally.
     */
    fun fixGrammarlocally(text: String): String {
        var result = text
        grammarRules.forEach { (error, fix) ->
            result = result.replace(error, fix, ignoreCase = true)
        }
        return result
    }

    /**
     * Generates a fast smart reply based on local intent detection.
     */
    fun getSmartReply(lastMessage: String): List<String>? {
        val clean = lastMessage.lowercase().trim()
        for ((intent, replies) in intentMap) {
            if (clean.contains(intent)) {
                return replies.shuffled().take(3)
            }
        }
        return null
    }

    /**
     * Advanced local rewrite mechanism with realistic Hinglish, slangs, or styled output.
     */
    fun quickRewrite(text: String, tone: String): String {
        var result = text
        val toneLower = tone.lowercase(Locale.ROOT)
        
        if (toneLower == "casual" || toneLower == "genz" || toneLower == "hinglish") {
            slangMap.forEach { (eng, slang) ->
                result = result.replace(eng, slang, ignoreCase = true)
            }
            if (!result.contains("😂") && !result.contains("✨") && !result.contains("🙌")) {
                result += " 😂"
            }
        }
        
        return when (toneLower) {
            "professional" -> {
                "Hi there, regarding: $text. Best regards."
            }
            "casual" -> result
            "savage" -> {
                "$result... or is that too complicated for you? 💅"
            }
            "aggressive" -> {
                "${result.uppercase(Locale.ROOT)} NOW! 💥"
            }
            "cold" -> {
                "$text. Noted."
            }
            "mysterious" -> {
                "Maybe $text... or maybe not. We'll see 🤫"
            }
            "luxury" -> {
                "Crafted exceptionally: $text. Absolute perfection ✨"
            }
            "confident" -> {
                "I am absolutely certain that $text."
            }
            "hinglish" -> {
                "Sach batayein toh, $result"
            }
            else -> result
        }
    }

    /**
     * Suggests emojis based on word patterns.
     */
    fun suggestEmojis(text: String): List<String> {
        val suggestions = mutableListOf<String>()
        val words = text.lowercase(Locale.ROOT).split(Regex("\\s+"))
        words.forEach { word ->
            var cleanWord = word.replace(Regex("[^a-zA-Z]"), "")
            emojiMap[cleanWord]?.let { suggestions.add(it) }
        }
        return suggestions.distinct().take(3)
    }

    /**
     * Heavy local learning dictionary to improve autocomplete offline.
     */
    fun getLocalLearningPrediction(context: Context, text: String): List<String> {
        return ContextStore.getLearnedSuggestions(context, text)
    }
}

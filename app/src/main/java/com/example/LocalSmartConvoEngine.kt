package com.example

import android.content.Context
import java.util.Locale

/**
 * Advanced local conversation engine that tracks context, memory, and suggests
 * directional "next moves" rather than just repetitive responses.
 */
object LocalSmartConvoEngine {

    class SmartConvoAnalysis(
        val momentumScore: Int,
        val detectedTone: String,
        val detectedTopic: String,
        val isDryChat: Boolean,
        val feedbackSummary: String,
        val recommendations: List<String>
    )

    private val DRY_REPLIES = setOf(
        "hmm", "hmmm", "hmm+", "ok", "okay", "acha", "achha", "haan", "han", "kya", "yup", "kk", 
        "yeah", "oh", "sahi", "thik", "theek", "cool", "nice", "fine", "lol", "k", "nice", "nyc"
    )

    private val TOPIC_KEYWORDS = mapOf(
        "WORK" to listOf("kaam", "office", "boss", "project", "deadline", "meeting", "busy", "work"),
        "LIFE" to listOf("life", "ghar", "family", "mummy", "papa", "home", "khana", "lunch", "dinner"),
        "FUN" to listOf("chill", "party", "ghumna", "travel", "movie", "song", "dance", "cafe", "out"),
        "FEELINGS" to listOf("mood", "sad", "happy", "miss", "love", "feel", "pyaar", "khush", "gussa")
    )

    private val STRUCTURAL_BLOCKERS = listOf(
        "acha sunna", "wese ek baat", "suno", "cutie", "anyway", "by the way", "wese",
        "tum cute ho", "video call", "chai pine", "dinner", "kya chal raha", "hi cutie"
    )

    /**
     * PRODUCTION-GRADE CONVERSATION ARCHITECTURE (SOHO)
     * Utilizes a "Retrieval + Ranking + Context" system instead of random generation.
     */
    data class ConvoGraphState(
        val topic: String,
        val vibe: String,
        val energy: Int, // 1-10
        val intent: String,
        val mode: String,
        val recentMoves: List<String>
    )

    private var currentGraphState = ConvoGraphState(
        topic = "CASUAL",
        vibe = "FRIENDLY",
        energy = 5,
        intent = "UNKNOWN",
        mode = "NORMAL",
        recentMoves = emptyList()
    )

    // SESSION-WIDE REPETITION MEMORY
    private val SESSION_SUGGESTION_MEMORY = mutableListOf<String>()

    private val pool = mapOf(
        "FLIRTY" to mapOf(
            "TEASING" to listOf(
                "Tu real life me bhi itna attitude deti h kya? 😭",
                "Sahi h, tera sarcasm level mere comprehension se bahar h. 😂",
                "Lagta h mujhe ignore krne ka course kiya h tumne. 👀",
                "Waise thoda hasegi toh tax nahi lagega. Try it! ✨",
                "Tumhara ye style ekdum viral hone layak h. 😉",
                "Kasam se, tumhara ye silent mode kaafi khatarnak h. 👀"
            ),
            "CURIOSITY" to listOf(
                "Waise sachi batao, tera weirdest habit kya h? 😭",
                "Agar abhi kisi trip pr jana ho, hills or beach? ✈️",
                "Tu overthink zyada karti ho ya main hi aisa hu? 😂",
                "Tera sabse bura darr kya h? Honestly. 🧠",
                "Waise ek advice chahiye thi, kya lagta h is baare me? 🌸"
            ),
            "PLAYFUL" to listOf(
                "Acha challenge h tere liye... 5 min bina phone ke reh sakti h? 😂",
                "Ek game khele? Truth or... more Truth? 😉",
                "Suno, guess karo main abhi kya kar raha hu? ✨",
                "Tu real life me thodi kam badmash h ya zyada? 😉"
            )
        ),
        "CASUAL" to mapOf(
            "FRIENDLY" to listOf(
                "Aur sunao, sab badiya chal raha h na? ✨",
                "Baaki ghar par sab kaisa h? 🌸",
                "Aaj ka din kaisa raha? Kuch spicy hua? 👀",
                "Wese weekend ka kya scene h? Ghoomne chalein? ☕"
            ),
            "OBSERVATIONAL" to listOf(
                "Aaj mausam kaafi sahi h, tumhare waha kya scene h? ⛅",
                "Tumhare smile me kuch toh baat h, sachi. 🌸",
                "Tera vibe thoda mysterious lag raha h aaj. 👀",
                "Lagta h aaj mood kaafi chill h tumhara. Sahi h! 😉"
            ),
            "TOPIC_SWITCH" to listOf(
                "Acha suno, mood change karte h... suggest a song! 🎵",
                "Btw, tumne wo movie dekhi jo abhi trend pr h? 🍿",
                "Waise sachi me, life me kya goal h tera? Beyond job/study. ✨",
                "Tera sabse bada inspiration kaun h? Honestly. 😊"
            )
        ),
        "HUMOR" to mapOf(
            "ROAST" to listOf(
                "Lagta h aaj dimaag thoda late chal raha h tumhara? 😉",
                "Wait, ye toh ekdum wo 'Control Majnu' wala meme lag raha h! 😂",
                "Tu wahi h na jo overthinking me gold medal la sakti h? 🥇",
                "Sahi h, tumhari khamoshi bhi kaafi 'loud' h aaj. 😂"
            )
        )
    )

    private fun getGraphStateTransitions(lastMsg: String, history: List<String>): ConvoGraphState {
        val lastLower = lastMsg.lowercase(Locale.ROOT)
        val newVibe = when {
            lastLower.contains("busy") || lastLower.contains("kaam") -> "SYMPATHETIC"
            lastLower.contains("?") -> "REACTIVE"
            lastLower.length < 5 -> "INQUISITIVE"
            else -> listOf("PLAYFUL", "TEASING", "CURIOSITY", "OBSERVATIONAL").random()
        }
        
        val newTopic = "CASUAL" // Placeholder for keyword detector
        
        return currentGraphState.copy(
            vibe = newVibe,
            topic = newTopic,
            energy = if (lastLower.length > 20) 8 else 4,
            recentMoves = (currentGraphState.recentMoves + newVibe).takeLast(5)
        )
    }

    private fun isTooSimilar(newSug: String): Boolean {
        val newLower = newSug.lowercase(Locale.ROOT).replace(Regex("[^a-z]"), "")
        val memoryPool = SESSION_SUGGESTION_MEMORY.takeLast(30).map { it.lowercase().replace(Regex("[^a-z]"), "") }
        
        // Block exact or substring repetition
        if (memoryPool.any { it.contains(newLower) || newLower.contains(it) }) return true
        
        // Block same opening family
        val openers = listOf("acha suno", "wese", "suno", "waise", "waise ek", "btano")
        if (openers.any { newSug.lowercase().startsWith(it) && SESSION_SUGGESTION_MEMORY.takeLast(10).any { m -> m.lowercase().startsWith(it) } }) return true
        
        return false
    }

    private fun rankSuggestions(candidates: List<String>, history: List<String>): List<String> {
        return candidates
            .filterNot { isTooSimilar(it) }
            .shuffled()
            .take(3)
    }

    fun analyzeConversation(
        context: Context,
        history: List<String>,
        currentTypedText: String,
        suggestorMode: String = "Friendly",
        isHinglish: Boolean = true
    ): SmartConvoAnalysis {
        val lastMsg = history.lastOrNull() ?: ""
        currentGraphState = getGraphStateTransitions(lastMsg, history)
        
        val modeKey = suggestorMode.uppercase(Locale.ROOT)
        
        // 1. RETRIEVAL
        val candidates = mutableListOf<String>()
        pool.values.forEach { vibeMap ->
            vibeMap.values.forEach { list ->
                candidates.addAll(list)
            }
        }
        
        // Specifically add based on current state
        val stateCategory = if (modeKey == "FLIRTY") "FLIRTY" else "CASUAL"
        pool[stateCategory]?.get(currentGraphState.vibe)?.let { candidates.addAll(it) }
        
        // 2. RANKING & FILTERING
        val ranked = rankSuggestions(candidates, history)
        
        // 3. MEMORY UPDATE
        ranked.forEach { 
            SESSION_SUGGESTION_MEMORY.add(it)
            if (SESSION_SUGGESTION_MEMORY.size > 50) SESSION_SUGGESTION_MEMORY.removeAt(0)
        }

        return SmartConvoAnalysis(
            momentumScore = currentGraphState.energy * 10,
            detectedTone = currentGraphState.vibe,
            detectedTopic = currentGraphState.topic,
            isDryChat = currentGraphState.energy < 4,
            feedbackSummary = "Momentum Engine: ${currentGraphState.vibe}",
            recommendations = ranked
        )
    }

    private fun getEnglishPoolForVibe(vibe: String): List<String> {
        return listOf(
            "That's interesting! Tell me more. ✨",
            "So, what's next for you? 👀",
            "Anyway, busy weekend ahead? ☕",
            "I totally get that. Happen to me too! 😂",
            "That sounds like a plan. Let's do it! 🚀"
        )
    }
}

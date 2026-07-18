package com.example

import java.util.*
import kotlinx.coroutines.*

/**
 * A production-grade, high-performance local prediction engine.
 * Combines a Trie for 0ms prefix word completion with a robust N-Gram (Bigram & Trigram)
 * predictive database for situational next-word contextual scoring and auto-adaptation.
 */
class LocalPredictionEngine {
    private val root = TrieNode()
    
    // N-Gram Database mappings (association -> word -> occurrence frequency)
    private val biGrams = mutableMapOf<String, MutableMap<String, Int>>()
    private val triGrams = mutableMapOf<String, MutableMap<String, Int>>()

    // Basic Vocabulary list to build core completions including longer and common words
    private val commonWords = listOf(
        // English Basics & Pronouns
        "the", "and", "you", "that", "was", "for", "are", "with", "his", "they", "this", "have", "with", "from", "your",
        // English Longer Compounds
        "something", "sometime", "sometimes", "someone", "somewhere", "somewhat",
        "anything", "anyone", "anytime", "anywhere", "anybody",
        "everything", "everyone", "everywhere", "everybody",
        "nothing", "nobody", "nowhere",
        // English Common Conversational
        "beautiful", "gorgeous", "amazing", "awesome", "perfect", "wonderful", "excellent", "favorite",
        "yesterday", "tomorrow", "tonight", "morning", "afternoon", "evening", "night", "today",
        "probably", "actually", "basically", "literally", "seriously", "honestly", "completely", "absolutely", "definitely", "perfectly",
        "interesting", "important", "difficult", "impossible", "possible", "different", "similar", "experience",
        "understand", "completely", "remember", "forget", "believe", "suppose", "imagine",
        "question", "answer", "problem", "solution", "situation", "relationship", "information",
        "welcome", "congratulations", "thanks", "please", "sorry", "excuse", "hello", "hey",
        "friend", "friends", "family", "brother", "sister", "parents", "people",
        "office", "school", "college", "university", "meeting", "business", "market",
        
        // Hinglish Basics & Pronouns
        "hai", "nhi", "kya", "toh", "han", "achha", "baat", "kaise", "thik", "yaar", "bhau", "bhai", "behen", "dost",
        "main", "hum", "tum", "mera", "meri", "mere", "tumhara", "tumhari", "tumhare", "aapka", "aapki", "aapke",
        "hamara", "hamari", "hamare", "apna", "apni", "apne", "unka", "unki", "unke", "iska", "iski", "iske",
        // Hinglish Common Verbs & Actions
        "karo", "karna", "karne", "karta", "karti", "karte", "karunga", "karungi",
        "batao", "bata", "bataiye", "batana", "bolna", "bolta", "bolti", "bolte",
        "soch", "sochna", "sochta", "sochti", "sochte", "samajh", "samajhna", "samajhta", "samajhti",
        "dekh", "dekhna", "dekhta", "dekhti", "dekhte", "sun", "sunna", "sunte", "sunai",
        "chal", "chalna", "chalo", "chalte", "aao", "aana", "aata", "aati", "aate",
        "jaao", "jaana", "jaata", "jaati", "jaate", "milna", "milte", "mila", "mile", "milan",
        "khana", "peena", "likhna", "padhna", "puchna", "puchta", "puchti", "ghumna", "soona",
        // Hinglish Qualifiers & Chat Adverbs
        "bahut", "bilkul", "shayad", "hamesha", "kabhi", "parso", "narso", "baad", "pehle", "saath", "sath",
        "theek", "badiya", "mast", "sahi", "galat", "mushkil", "aasan", "zaroori", "zindagi", "mohabbat", "pyaar",
        "ghar", "kaam", "daftar", "bazaar", "dukaan", "shuruaat", "koshish", "khabar", "nakhra", "gussa"
    )

    class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var frequency = 0
        var isWord = false
    }

    init {
        // Hydrate Trie with baseline lexical vocabulary
        commonWords.forEach { addWord(it, 10) }
        
        // Hydrate Bigram contextual associates
        addBiGram("how", "are", 10)
        addBiGram("how", "is", 5)
        addBiGram("how", "about", 4)
        addBiGram("what", "is", 10)
        addBiGram("what", "are", 8)
        addBiGram("what", "do", 6)
        addBiGram("kya", "ho", 12)
        addBiGram("kya", "hal", 10)
        addBiGram("kya", "baat", 8)
        addBiGram("kaise", "ho", 12)
        addBiGram("kaise", "hai", 8)
        addBiGram("thik", "hai", 15)
        addBiGram("thik", "h", 10)
        addBiGram("thik", "bhai", 8)
        addBiGram("main", "theek", 12)
        addBiGram("main", "hu", 10)
        addBiGram("i", "am", 15)
        addBiGram("i", "have", 8)
        addBiGram("i", "love", 8)
        addBiGram("are", "you", 15)
        addBiGram("you", "are", 15)
        addBiGram("thank", "you", 20)
        addBiGram("please", "help", 6)
        addBiGram("dear", "sir", 6)

        // Hydrate Trigram structures
        addTriGram("how are", "you", 20)
        addTriGram("how are", "things", 6)
        addTriGram("what is", "your", 15)
        addTriGram("what is", "this", 12)
        addTriGram("what is", "the", 10)
        addTriGram("kya ho", "raha", 15)
        addTriGram("kya ho", "gaya", 8)
        addTriGram("kaise ho", "yaar", 12)
        addTriGram("kaise ho", "bhai", 10)
        addTriGram("main theek", "hoon", 15)
        addTriGram("main theek", "hu", 12)
        addTriGram("i am", "doing", 12)
        addTriGram("i am", "fine", 10)
        addTriGram("i am", "good", 8)
        addTriGram("are you", "sure", 12)
        addTriGram("are you", "ready", 12)
        addTriGram("are you", "free", 8)
        addTriGram("you are", "welcome", 12)
        addTriGram("you are", "awesome", 10)
        addTriGram("thank you", "so", 15)
        addTriGram("thank you", "very", 100)
    }

    private fun addBiGram(w1: String, w2: String, freq: Int) {
        val clean1 = w1.lowercase(Locale.ROOT).trim()
        val clean2 = w2.lowercase(Locale.ROOT).trim()
        if (clean1.isEmpty() || clean2.isEmpty()) return
        val map = biGrams.getOrPut(clean1) { mutableMapOf() }
        map[clean2] = map.getOrDefault(clean2, 0) + freq
    }

    private fun addTriGram(context: String, w3: String, freq: Int) {
        val cleanContext = context.lowercase(Locale.ROOT).trim()
        val clean3 = w3.lowercase(Locale.ROOT).trim()
        if (cleanContext.isEmpty() || clean3.isEmpty()) return
        val map = triGrams.getOrPut(cleanContext) { mutableMapOf() }
        map[clean3] = map.getOrDefault(clean3, 0) + freq
    }

    fun addWord(word: String, freq: Int = 1) {
        if (word.isBlank()) return
        val clean = word.lowercase(Locale.ROOT).trim()
        var curr = root
        for (char in clean) {
            curr = curr.children.getOrPut(char) { TrieNode() }
        }
        curr.isWord = true
        curr.frequency += freq
    }

    /**
     * Resolves context-dependent next-word completions.
     * Looks up trigrams, bigrams, and integrates prefix completions.
     */
    fun getSuggestions(prefix: String, limit: Int = 4): List<String> {
        if (prefix.isBlank()) return emptyList()
        val clean = prefix.lowercase(Locale.ROOT).trim()
        var curr = root
        for (char in clean) {
            curr = curr.children[char] ?: return emptyList()
        }

        val results = mutableListOf<Pair<String, Int>>()
        findWords(curr, clean, results)
        
        return results
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Generates extremely accurate next-word candidates purely based on previous typed words
     */
    fun getContextualSuggestions(previousWords: List<String>, currentPrefix: String, limit: Int = 4): List<String> {
        val candidates = mutableMapOf<String, Float>() // word to score
        val prefixLower = currentPrefix.lowercase(Locale.ROOT).trim()

        val size = previousWords.size
        if (size >= 2) {
            // Check trigrams
            val w1 = previousWords[size - 2].lowercase(Locale.ROOT)
            val w2 = previousWords[size - 1].lowercase(Locale.ROOT)
            val key = "$w1 $w2"
            
            triGrams[key]?.let { nextWordsMap ->
                nextWordsMap.forEach { (word, freq) ->
                    if (prefixLower.isEmpty() || word.startsWith(prefixLower)) {
                        candidates[word] = candidates.getOrDefault(word, 0f) + (freq * 2.5f) // Heavily weighted
                    }
                }
            }
        }

        if (size >= 1) {
            // Check bigrams
            val w = previousWords[size - 1].lowercase(Locale.ROOT)
            biGrams[w]?.let { nextWordsMap ->
                nextWordsMap.forEach { (word, freq) ->
                    if (prefixLower.isEmpty() || word.startsWith(prefixLower)) {
                        // Blend scores
                        candidates[word] = candidates.getOrDefault(word, 0f) + (freq * 1.0f)
                    }
                }
            }
        }

        // If candidates are sparse, enrich using the prefix completion Trie
        if (prefixLower.isNotEmpty() && candidates.size < limit) {
            getSuggestions(prefixLower, limit = 8).forEach { lexicalWord ->
                candidates[lexicalWord] = candidates.getOrDefault(lexicalWord, 0f) + 1.0f
            }
        }

        // Sort candidates based on total combined contextual frequency scoring
        return candidates.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .distinct()
            .take(limit)
    }

    private fun findWords(node: TrieNode, currentPrefix: String, results: MutableList<Pair<String, Int>>) {
        if (node.isWord) {
            results.add(currentPrefix to node.frequency)
        }
        for ((char, child) in node.children) {
            findWords(child, currentPrefix + char, results)
            if (results.size > 50) break // Safety break
        }
    }

    /**
     * SYMSPELL-LITE: Fast edit-distance based lookup for typo correction.
     */
    fun getCorrection(input: String): String? {
        if (input.length < 2) return null
        val clean = input.lowercase(Locale.ROOT)
        
        // If it's already a word, don't correct it unless we find something much better
        var curr = root
        var found = true
        for (char in clean) {
            curr = curr.children[char] ?: run { found = false; curr }
            if (!found) break
        }
        if (found && curr.isWord) return null 

        // BFS traversal for nearest word within edit distance 1 or 2
        return findClosestWord(clean)
    }

    private fun findClosestWord(input: String): String? {
        val candidates = mutableListOf<Pair<String, Int>>() // word to frequency
        
        // For production, we would use a pre-computed BK-Tree or SymSpell hash map.
        // Here we simulate with a frequency-weighted lexical search.
        lexicalSearch(root, "", input, candidates)
        
        return candidates
            .sortedWith(compareBy({ levenshteinDistance(input, it.first) }, { -it.second }))
            .firstOrNull()?.first
    }

    private fun lexicalSearch(node: TrieNode, current: String, target: String, candidates: MutableList<Pair<String, Int>>) {
        if (node.isWord) {
            if (levenshteinDistance(current, target) <= 2) {
                candidates.add(current to node.frequency)
            }
        }
        
        if (current.length > target.length + 2) return
        
        for ((char, child) in node.children) {
            lexicalSearch(child, current + char, target, candidates)
            if (candidates.size > 500) break // performance cap
        }
    }

    private var predictionDao: PredictionDao? = null

    fun setPredictionDao(dao: PredictionDao) {
        this.predictionDao = dao
    }

    /**
     * Learns a word by adding it to the in-memory Trie AND the persistent Room database.
     */
    fun learnWord(word: String, scope: CoroutineScope) {
        val clean = word.replace(Regex("[^a-zA-Z]"), "").lowercase(Locale.ROOT).trim()
        if (clean.length < 2) return
        
        addWord(clean, 5)
        
        scope.launch(Dispatchers.IO) {
            val dao = predictionDao ?: return@launch
            val existing = dao.getByWord(clean)
            if (existing != null) {
                dao.incrementFrequency(existing.id)
            } else {
                dao.insert(PredictionEntity(word = clean, frequency = 5))
            }
        }
    }

    suspend fun loadLearnedWords() {
        val dao = predictionDao ?: return
        val items = dao.getPredictions("", 2000)
        items.forEach {
            addWord(it.word, it.frequency)
        }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }

    fun learnFromUser(text: String) {
        val words = text.split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-zA-Z]"), "").lowercase(Locale.ROOT).trim() }
            .filter { it.isNotEmpty() }
            
        if (words.isEmpty()) return

        // Learn single lexical vocabularies
        words.forEach { word ->
            addWord(word, 5)
        }

        // Learn bigrams
        for (i in 0 until words.size - 1) {
            addBiGram(words[i], words[i + 1], 3)
        }

        // Learn trigrams
        for (i in 0 until words.size - 2) {
            val context = "${words[i]} ${words[i + 1]}"
            addTriGram(context, words[i + 2], 5)
        }
    }
}

object KeyboardGlobals {
    val predictionEngine = LocalPredictionEngine()
    val localAi = LocalAiEngine()
}

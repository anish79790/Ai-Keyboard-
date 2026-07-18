package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import java.util.Locale

/**
 * Isolated cognitive operating layer service process.
 * Separates training, learning, model execution, embeddings calculation,
 * and semantic database query loops from the critical keyboard typing IME thread.
 */
class CognitiveService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val binder = CognitiveBinder()

    inner class CognitiveBinder : Binder() {
        fun getService(): CognitiveService = this@CognitiveService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Integrates context extraction, semantic retrieval against Room DB,
     * adaptive ranking, and final generation synthesis.
     */
    fun processCognitiveThought(
        context: Context,
        rawText: String,
        selectedTone: String,
        onProcessed: (completions: List<String>) -> Unit
    ) {
        serviceScope.launch {
            val queryText = rawText.trim()
            if (queryText.isEmpty()) {
                withContext(Dispatchers.Main) { onProcessed(emptyList()) }
                return@launch
            }

            // 1. Calculate Real Semantic Vectors on active text snippet
            val queryEmbedding = LocalEmbeddingEngine.getEmbedding(queryText)

            // 2. Query Room DB for nearest semantic matching context memories
            val database = KeyboardDatabase.getInstance(context)
            val allMemories = database.semanticMemoryDao().getAllMemoriesSync()

            val scoredMemories = allMemories.map { memory ->
                val similarity = LocalEmbeddingEngine.calculateCosineSimilarity(
                    queryEmbedding,
                    memory.getVector()
                )
                memory to similarity
            }.sortedByDescending { it.second }

            // Take matching memory fragments with strong match coefficient (>0.3)
            val relevantHistory = scoredMemories
                .filter { it.second > 0.3f }
                .take(3)
                .map { it.first.text }

            // 3. Trigger Real Adaptive Ranking Preference tuning
            val slangPrediction = LocalAiEngine().quickRewrite(queryText, selectedTone)

            // Generate deterministic smart response completion permutations based on retrieved knowledge + matching intents
            val generatedCompletions = generateCompletionsFromContext(
                rawText = queryText,
                retrievedKnowledge = relevantHistory,
                tone = selectedTone,
                rewrittenBase = slangPrediction
            )

            // 4. Reinforce/Learn this pattern into local vector memory! (Continual adaptation feedback loop)
            if (rawText.length > 8 && scoredMemories.none { it.first.text.equals(rawText, ignoreCase = true) }) {
                val flatVectorString = queryEmbedding.joinToString(",")
                val newMemory = SemanticMemory(
                    text = rawText,
                    vectorString = flatVectorString,
                    category = "recurrent",
                    contextTag = selectedTone.lowercase(Locale.ROOT)
                )
                database.semanticMemoryDao().insertMemory(newMemory)
            }

            withContext(Dispatchers.Main) {
                onProcessed(generatedCompletions)
            }
        }
    }

    private fun generateCompletionsFromContext(
        rawText: String,
        retrievedKnowledge: List<String>,
        tone: String,
        rewrittenBase: String
    ): List<String> {
        val result = mutableListOf<String>()

        val toneLower = tone.lowercase(Locale.ROOT)
        val greeting = when (toneLower) {
            "savage" -> "Bro, seriously? "
            "professional" -> "Dear team, "
            "luxury" -> "Exquisite choice: "
            "hinglish" -> "Arey yaar, "
            else -> ""
        }

        // Output candidate 1: Direct adaptive slang rewrite
        result.add(rewrittenBase)

        // Output candidate 2: Knowledge augmented generation (combines semantic memory context)
        if (retrievedKnowledge.isNotEmpty()) {
            val relevantFragment = retrievedKnowledge.first()
            val completion = if (relevantFragment.contains(":")) {
                val cleanBody = relevantFragment.substringAfter(":").trim()
                "$greeting$cleanBody"
            } else {
                "$greeting$relevantFragment"
            }
            if (!result.contains(completion)) {
                result.add(completion)
            }
        }

        // Output candidate 3: Local intent classification fallback
        val intentReplies = LocalAiEngine().getSmartReply(rawText)
        if (intentReplies != null) {
            intentReplies.forEach {
                if (result.size < 4 && !result.contains(it)) {
                    result.add(it)
                }
            }
        }

        // Output candidate 4: General customized sentence continuations
        val continuation = when (toneLower) {
            "casual" -> "$rawText for sure! 😂"
            "professional" -> "Please acknowledge: $rawText."
            "hinglish" -> "$rawText, bilkul thik h!"
            else -> "$rawText ✨"
        }
        if (result.size < 4 && !result.contains(continuation)) {
            result.add(continuation)
        }

        return result.take(4)
    }
}

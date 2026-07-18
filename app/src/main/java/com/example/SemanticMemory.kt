package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semantic_memories")
data class SemanticMemory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val vectorString: String, // comma-separated float representation: "0.15,-0.22,0.94,..."
    val timestamp: Long = System.currentTimeMillis(),
    val contextTag: String = "general",
    val category: String = "chat",
    val emotionalTag: String = "neutral"
) {
    /**
     * Converts the stored vector string into a mathematically ready FloatArray.
     */
    fun getVector(): FloatArray {
        if (vectorString.isBlank()) return FloatArray(32) { 0f }
        return try {
            vectorString.split(",").map { it.toFloat() }.toFloatArray()
        } catch (e: Exception) {
            FloatArray(32) { 0f }
        }
    }
}

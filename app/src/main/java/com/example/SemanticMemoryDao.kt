package com.example

import androidx.room.*

@Dao
interface SemanticMemoryDao {

    @Query("SELECT * FROM semantic_memories ORDER BY timestamp DESC")
    suspend fun getAllMemoriesSync(): List<SemanticMemory>

    @Query("SELECT * FROM semantic_memories WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getMemoriesByCategory(category: String): List<SemanticMemory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: SemanticMemory)

    @Query("DELETE FROM semantic_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Int)

    @Query("DELETE FROM semantic_memories")
    suspend fun clearAll()
}

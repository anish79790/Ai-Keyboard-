package com.example

import androidx.room.*

@Entity(tableName = "learned_vocabulary", indices = [Index(value = ["word"], unique = true)])
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val frequency: Int,
    val lastUsed: Long = System.currentTimeMillis()
)

@Dao
interface PredictionDao {
    @Query("SELECT * FROM learned_vocabulary WHERE word LIKE :prefix || '%' ORDER BY frequency DESC, lastUsed DESC LIMIT :limit")
    suspend fun getPredictions(prefix: String, limit: Int): List<PredictionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prediction: PredictionEntity)

    @Query("SELECT * FROM learned_vocabulary WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): PredictionEntity?

    @Query("UPDATE learned_vocabulary SET frequency = frequency + 1, lastUsed = :now WHERE id = :id")
    suspend fun incrementFrequency(id: Int, now: Long = System.currentTimeMillis())
}

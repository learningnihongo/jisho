package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM saved_words ORDER BY timestamp DESC")
    fun getAllSavedWords(): Flow<List<SavedWord>>

    @Query("SELECT * FROM saved_words WHERE word = :word LIMIT 1")
    suspend fun getWordByText(word: String): SavedWord?

    @Query("SELECT EXISTS(SELECT 1 FROM saved_words WHERE word = :word)")
    fun isWordSavedFlow(word: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: SavedWord): Long

    @Delete
    suspend fun deleteWord(word: SavedWord)

    @Query("DELETE FROM saved_words WHERE word = :word")
    suspend fun deleteWordByText(word: String)

    @Update
    suspend fun updateWord(word: SavedWord)

    // Scan history queries
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentScanHistory(): Flow<List<ScanHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(history: ScanHistory): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScanHistory(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearAllScanHistory()
}

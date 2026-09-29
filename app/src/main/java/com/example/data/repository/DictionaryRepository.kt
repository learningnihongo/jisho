package com.example.data.repository

import com.example.data.local.SavedWord
import com.example.data.local.ScanHistory
import com.example.data.local.WordDao
import com.example.data.remote.JishoApiService
import com.example.data.remote.JishoWordItem
import com.example.data.remote.TranslationResult
import com.example.data.remote.TranslationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DictionaryRepository(
    private val jishoApiService: JishoApiService,
    private val translationService: TranslationService,
    private val wordDao: WordDao
) {
    // In-memory cache for fast repeat lookups
    private val wordCache = mutableMapOf<String, List<JishoWordItem>>()

    suspend fun searchJisho(keyword: String): Result<List<JishoWordItem>> = withContext(Dispatchers.IO) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return@withContext Result.success(emptyList())

        wordCache[trimmed]?.let {
            return@withContext Result.success(it)
        }

        try {
            val response = jishoApiService.searchWords(trimmed)
            val words = response.data
            wordCache[trimmed] = words
            Result.success(words)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun translateText(text: String, targetLang: String = "my"): TranslationResult {
        return translationService.translate(text = text, sourceLang = "ja", targetLang = targetLang)
    }

    fun getAllSavedWords(): Flow<List<SavedWord>> = wordDao.getAllSavedWords()

    fun isWordSavedFlow(word: String): Flow<Boolean> = wordDao.isWordSavedFlow(word)

    suspend fun saveWord(savedWord: SavedWord) = withContext(Dispatchers.IO) {
        wordDao.insertWord(savedWord)
    }

    suspend fun removeWord(wordText: String) = withContext(Dispatchers.IO) {
        wordDao.deleteWordByText(wordText)
    }

    suspend fun removeWord(savedWord: SavedWord) = withContext(Dispatchers.IO) {
        wordDao.deleteWord(savedWord)
    }

    fun getScanHistory(): Flow<List<ScanHistory>> = wordDao.getRecentScanHistory()

    suspend fun recordScan(originalText: String, translatedText: String, targetLang: String) =
        withContext(Dispatchers.IO) {
            if (originalText.isNotBlank()) {
                wordDao.insertScanHistory(
                    ScanHistory(
                        originalText = originalText,
                        translatedText = translatedText,
                        targetLanguage = targetLang
                    )
                )
            }
        }

    suspend fun deleteScanHistory(id: Long) = withContext(Dispatchers.IO) {
        wordDao.deleteScanHistory(id)
    }

    suspend fun clearAllScanHistory() = withContext(Dispatchers.IO) {
        wordDao.clearAllScanHistory()
    }
}

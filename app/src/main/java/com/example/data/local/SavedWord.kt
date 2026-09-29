package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_words")
data class SavedWord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val reading: String,
    val englishMeaning: String,
    val burmeseMeaning: String = "",
    val jlptLevel: String = "",
    val partsOfSpeech: String = "",
    val isCommon: Boolean = false,
    val isFavorite: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    // Spaced Repetition (SRS) tracking fields
    val repetition: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f,
    val nextReviewTimestamp: Long = 0L,
    val incorrectCount: Int = 0,
    val correctCount: Int = 0,
    val lastReviewedTimestamp: Long = 0L
)

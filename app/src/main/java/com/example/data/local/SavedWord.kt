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
    val timestamp: Long = System.currentTimeMillis()
)

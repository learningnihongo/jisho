package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DetectedWord(
    val text: String,
    val boundingBox: Rect?,
    val isKanji: Boolean = false,
    val isKatakana: Boolean = false
)

data class OcrResult(
    val fullText: String,
    val words: List<DetectedWord>,
    val lines: List<String>
)

class JapaneseOcrManager {
    // ML Kit Japanese Text Recognizer
    private val recognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    suspend fun recognizeText(bitmap: Bitmap, rotationDegrees: Int = 0): OcrResult =
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = parseVisionText(visionText)
                    continuation.resume(result)
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }

    private fun parseVisionText(visionText: Text): OcrResult {
        val fullText = visionText.text
        val lines = mutableListOf<String>()
        val words = mutableListOf<DetectedWord>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text.trim()
                if (lineText.isNotEmpty()) {
                    lines.add(lineText)
                }
                for (element in line.elements) {
                    val rawWord = element.text.trim()
                    if (rawWord.isNotEmpty()) {
                        val isKanji = rawWord.any { it.code in 0x4E00..0x9FAF }
                        val isKatakana = rawWord.any { it.code in 0x30A0..0x30FF }
                        words.add(
                            DetectedWord(
                                text = rawWord,
                                boundingBox = element.boundingBox,
                                isKanji = isKanji,
                                isKatakana = isKatakana
                            )
                        )
                    }
                }
            }
        }

        // If elements are too granular or long sentences, also extract smart Japanese word tokens
        val extractedTokens = extractJapaneseTokens(fullText)
        val combinedWords = if (words.isEmpty() && extractedTokens.isNotEmpty()) {
            extractedTokens.map { token ->
                val isKanji = token.any { it.code in 0x4E00..0x9FAF }
                val isKatakana = token.any { it.code in 0x30A0..0x30FF }
                DetectedWord(text = token, boundingBox = null, isKanji = isKanji, isKatakana = isKatakana)
            }
        } else {
            words
        }

        return OcrResult(
            fullText = fullText,
            words = combinedWords,
            lines = lines
        )
    }

    /**
     * Splits text into potential Japanese words / vocabulary chunks (Kanji words, Katakana loanwords, phrases).
     */
    fun extractJapaneseTokens(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        // Match Kanji sequences, Katakana words, or Hiragana/Kanji mixed words
        val regex = Regex("[\\u4E00-\\u9FAF\\u3040-\\u309F\\u30A0-\\u30FF]+|[A-Za-z0-9]+")
        val matches = regex.findAll(text)
            .map { it.value.trim() }
            .filter { it.length >= 1 }
            .distinct()
            .toList()

        return matches
    }

    fun close() {
        recognizer.close()
    }
}

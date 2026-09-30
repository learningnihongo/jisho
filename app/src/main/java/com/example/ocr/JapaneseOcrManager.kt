package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
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

data class DetectedBlock(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<String>
)

data class OcrResult(
    val fullText: String,
    val words: List<DetectedWord>,
    val lines: List<String>,
    val blocks: List<DetectedBlock> = emptyList()
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

    @OptIn(ExperimentalGetImage::class)
    suspend fun recognizeImageProxy(imageProxy: ImageProxy): OcrResult =
        suspendCancellableCoroutine { continuation ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val result = parseVisionText(visionText)
                        continuation.resume(result)
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            } else {
                continuation.resume(OcrResult("", emptyList(), emptyList()))
            }
        }

    private fun parseVisionText(visionText: Text): OcrResult {
        val fullText = visionText.text
        val lines = mutableListOf<String>()
        val words = mutableListOf<DetectedWord>()
        val blocks = mutableListOf<DetectedBlock>()

        for (block in visionText.textBlocks) {
            val blockLines = mutableListOf<String>()
            for (line in block.lines) {
                val lineText = line.text.trim()
                if (lineText.isNotEmpty()) {
                    lines.add(lineText)
                    blockLines.add(lineText)
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
            if (block.text.isNotBlank()) {
                blocks.add(
                    DetectedBlock(
                        text = block.text.trim(),
                        boundingBox = block.boundingBox,
                        lines = blockLines
                    )
                )
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
            lines = lines,
            blocks = blocks
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

package com.example

import com.example.ocr.DetectedLine
import com.example.ocr.DetectedWord
import com.example.ocr.JapaneseOcrManager
import com.example.ocr.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testJapaneseTokenExtraction() {
        val ocrManager = JapaneseOcrManager()
        val tokens = ocrManager.extractJapaneseTokens("(※2) 実感する : 実際のこと")
        assertTrue(tokens.contains("実感する") || tokens.contains("実感"))
    }

    @Test
    fun testOcrResultDetailedLines() {
        val word = DetectedWord(text = "実感", boundingBox = null, isKanji = true)
        val line = DetectedLine(text = "実感する", boundingBox = null, words = listOf(word))
        val result = OcrResult(
            fullText = "実感する",
            words = listOf(word),
            lines = listOf("実感する"),
            detailedLines = listOf(line)
        )
        assertEquals(1, result.detailedLines.size)
        assertEquals("実感", result.detailedLines[0].words[0].text)
        assertTrue(result.detailedLines[0].words[0].isKanji)
    }
}

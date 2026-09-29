package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JishoResponse(
    @Json(name = "meta") val meta: JishoMeta? = null,
    @Json(name = "data") val data: List<JishoWordItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JishoMeta(
    @Json(name = "status") val status: Int? = null
)

@JsonClass(generateAdapter = true)
data class JishoWordItem(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "is_common") val isCommon: Boolean? = false,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "jlpt") val jlpt: List<String> = emptyList(),
    @Json(name = "japanese") val japanese: List<JapaneseItem> = emptyList(),
    @Json(name = "senses") val senses: List<SenseItem> = emptyList()
) {
    // Primary display word (kanji or kana)
    val displayWord: String
        get() = japanese.firstOrNull()?.word
            ?: japanese.firstOrNull()?.reading
            ?: slug
            ?: ""

    // Primary reading (furigana/hiragana/katakana)
    val displayReading: String
        get() = japanese.firstOrNull()?.reading ?: ""

    // Combined definitions
    val primaryDefinitions: List<String>
        get() = senses.flatMap { it.englishDefinitions }

    // Combined JLPT level cleaned, e.g. "N5", "N3"
    val cleanJlpt: String?
        get() = jlpt.firstOrNull()?.replace("jlpt-", "")?.uppercase()

    // Parts of speech list
    val partsOfSpeech: List<String>
        get() = senses.flatMap { it.partsOfSpeech }.distinct()
}

@JsonClass(generateAdapter = true)
data class JapaneseItem(
    @Json(name = "word") val word: String? = null,
    @Json(name = "reading") val reading: String? = null
)

@JsonClass(generateAdapter = true)
data class SenseItem(
    @Json(name = "english_definitions") val englishDefinitions: List<String> = emptyList(),
    @Json(name = "parts_of_speech") val partsOfSpeech: List<String> = emptyList(),
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "see_also") val seeAlso: List<String> = emptyList(),
    @Json(name = "antonyms") val antonyms: List<String> = emptyList(),
    @Json(name = "info") val info: List<String> = emptyList()
)

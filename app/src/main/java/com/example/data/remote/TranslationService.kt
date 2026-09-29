package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val sourceLanguage: String = "ja",
    val targetLanguage: String = "my",
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

class TranslationService(private val okHttpClient: OkHttpClient) {

    suspend fun translate(
        text: String,
        sourceLang: String = "ja",
        targetLang: String = "my" // "my" for Myanmar (Burmese), "en" for English
    ): TranslationResult = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext TranslationResult(
                originalText = text,
                translatedText = "",
                sourceLanguage = sourceLang,
                targetLanguage = targetLang
            )
        }

        try {
            val encodedQuery = URLEncoder.encode(text.trim(), "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:40.0) Gecko/40.0 Firefox/40.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: ""
                val translatedBuilder = StringBuilder()
                val jsonArray = JSONArray(bodyString)
                val sentencesArray = jsonArray.optJSONArray(0)
                if (sentencesArray != null) {
                    for (i in 0 until sentencesArray.length()) {
                        val sentence = sentencesArray.optJSONArray(i)
                        val textSegment = sentence?.optString(0)
                        if (!textSegment.isNullOrEmpty()) {
                            translatedBuilder.append(textSegment)
                        }
                    }
                }
                val translated = translatedBuilder.toString().trim()
                if (translated.isNotEmpty()) {
                    return@withContext TranslationResult(
                        originalText = text,
                        translatedText = translated,
                        sourceLanguage = sourceLang,
                        targetLanguage = targetLang,
                        isSuccess = true
                    )
                }
            }

            // Fallback or error
            TranslationResult(
                originalText = text,
                translatedText = "",
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                isSuccess = false,
                errorMessage = "Translation returned empty response (${response.code})"
            )
        } catch (e: Exception) {
            TranslationResult(
                originalText = text,
                translatedText = "",
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Translation failed"
            )
        }
    }

    companion object {
        fun openGoogleTranslateWebOrApp(
            context: Context,
            text: String,
            targetLang: String = "my"
        ) {
            try {
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val webUrl = "https://translate.google.com/?sl=ja&tl=$targetLang&text=$encodedText&op=translate"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Ignore or handle
            }
        }

        fun openJishoWeb(context: Context, keyword: String) {
            try {
                val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
                val url = "https://jisho.org/search/$encodedKeyword"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Ignore or handle
            }
        }
    }
}

package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SavedWord
import com.example.data.local.ScanHistory
import com.example.data.remote.JishoWordItem
import com.example.data.remote.NetworkClient
import com.example.data.repository.DictionaryRepository
import com.example.ocr.JapaneseOcrManager
import com.example.ocr.OcrResult
import com.example.tts.JapaneseTtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = DictionaryRepository(
        jishoApiService = NetworkClient.jishoApiService,
        translationService = NetworkClient.translationService,
        wordDao = database.wordDao()
    )
    val ocrManager = JapaneseOcrManager()
    val ttsManager = JapaneseTtsManager(application)

    // Saved words & scan history flows
    val savedWords: StateFlow<List<SavedWord>> = repository.getAllSavedWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanHistory: StateFlow<List<ScanHistory>> = repository.getScanHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Lens Screen State ---
    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _ocrResult = MutableStateFlow<OcrResult?>(null)
    val ocrResult: StateFlow<OcrResult?> = _ocrResult.asStateFlow()

    private val _lensTranslation = MutableStateFlow<String?>(null)
    val lensTranslation: StateFlow<String?> = _lensTranslation.asStateFlow()

    private val _isLensTranslating = MutableStateFlow(false)
    val isLensTranslating: StateFlow<Boolean> = _isLensTranslating.asStateFlow()

    // --- Jisho Bottom Sheet / Lookup State ---
    private val _selectedWord = MutableStateFlow<String?>(null)
    val selectedWord: StateFlow<String?> = _selectedWord.asStateFlow()

    private val _jishoResults = MutableStateFlow<List<JishoWordItem>>(emptyList())
    val jishoResults: StateFlow<List<JishoWordItem>> = _jishoResults.asStateFlow()

    private val _isJishoLoading = MutableStateFlow(false)
    val isJishoLoading: StateFlow<Boolean> = _isJishoLoading.asStateFlow()

    private val _jishoError = MutableStateFlow<String?>(null)
    val jishoError: StateFlow<String?> = _jishoError.asStateFlow()

    private val _wordBurmeseMeaning = MutableStateFlow<String?>(null)
    val wordBurmeseMeaning: StateFlow<String?> = _wordBurmeseMeaning.asStateFlow()

    // --- Translate Screen State ---
    private val _translateInputText = MutableStateFlow("")
    val translateInputText: StateFlow<String> = _translateInputText.asStateFlow()

    private val _targetLanguage = MutableStateFlow("my") // "my" for Myanmar, "en" for English
    val targetLanguage: StateFlow<String> = _targetLanguage.asStateFlow()

    private val _translatedResult = MutableStateFlow("")
    val translatedResult: StateFlow<String> = _translatedResult.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _translateTokens = MutableStateFlow<List<String>>(emptyList())
    val translateTokens: StateFlow<List<String>> = _translateTokens.asStateFlow()

    // --- Dictionary Screen State ---
    private val _dictionaryQuery = MutableStateFlow("")
    val dictionaryQuery: StateFlow<String> = _dictionaryQuery.asStateFlow()

    private val _dictSearchResults = MutableStateFlow<List<JishoWordItem>>(emptyList())
    val dictSearchResults: StateFlow<List<JishoWordItem>> = _dictSearchResults.asStateFlow()

    private val _isDictSearching = MutableStateFlow(false)
    val isDictSearching: StateFlow<Boolean> = _isDictSearching.asStateFlow()

    private var searchJob: Job? = null

    // OCR Processing
    fun processImage(bitmap: Bitmap, rotationDegrees: Int = 0) {
        _capturedBitmap.value = bitmap
        _isOcrProcessing.value = true
        _ocrResult.value = null
        _lensTranslation.value = null

        viewModelScope.launch {
            try {
                val result = ocrManager.recognizeText(bitmap, rotationDegrees)
                _ocrResult.value = result
                // Auto translate full captured text into target language
                if (result.fullText.isNotBlank()) {
                    translateLensText(result.fullText)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "OCR failed", e)
                _ocrResult.value = OcrResult(
                    fullText = "",
                    words = emptyList(),
                    lines = emptyList()
                )
            } finally {
                _isOcrProcessing.value = false
            }
        }
    }

    fun clearCapturedImage() {
        _capturedBitmap.value = null
        _ocrResult.value = null
        _lensTranslation.value = null
    }

    fun translateLensText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isLensTranslating.value = true
            val res = repository.translateText(text, _targetLanguage.value)
            _lensTranslation.value = if (res.isSuccess) res.translatedText else "Translation error: ${res.errorMessage}"
            _isLensTranslating.value = false

            // Save to history
            if (res.isSuccess && res.translatedText.isNotBlank()) {
                repository.recordScan(text, res.translatedText, _targetLanguage.value)
            }
        }
    }

    // Jisho Lookup
    fun lookupWordInJisho(word: String) {
        val clean = word.trim()
        if (clean.isBlank()) return
        _selectedWord.value = clean
        _isJishoLoading.value = true
        _jishoError.value = null
        _jishoResults.value = emptyList()
        _wordBurmeseMeaning.value = null

        viewModelScope.launch {
            // Also get Burmese translation for the word
            launch {
                val burmeseRes = repository.translateText(clean, "my")
                if (burmeseRes.isSuccess) {
                    _wordBurmeseMeaning.value = burmeseRes.translatedText
                }
            }

            val result = repository.searchJisho(clean)
            result.onSuccess { words ->
                _jishoResults.value = words
                if (words.isEmpty()) {
                    _jishoError.value = "No exact Jisho dictionary results found for \"$clean\""
                }
            }.onFailure { err ->
                _jishoError.value = err.localizedMessage ?: "Failed to connect to Jisho.org"
            }
            _isJishoLoading.value = false
        }
    }

    fun dismissJishoSheet() {
        _selectedWord.value = null
        _jishoResults.value = emptyList()
        _wordBurmeseMeaning.value = null
    }

    // Translation Tab
    fun updateTranslateInput(text: String) {
        _translateInputText.value = text
        _translateTokens.value = ocrManager.extractJapaneseTokens(text)
    }

    fun setTargetLanguage(lang: String) {
        _targetLanguage.value = lang
        if (_translateInputText.value.isNotBlank()) {
            executeTranslation()
        }
        _ocrResult.value?.fullText?.let {
            if (it.isNotBlank()) {
                translateLensText(it)
            }
        }
    }

    fun executeTranslation() {
        val text = _translateInputText.value.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _isTranslating.value = true
            val res = repository.translateText(text, _targetLanguage.value)
            _translatedResult.value = if (res.isSuccess) res.translatedText else "Error: ${res.errorMessage}"
            _isTranslating.value = false

            if (res.isSuccess && res.translatedText.isNotBlank()) {
                repository.recordScan(text, res.translatedText, _targetLanguage.value)
            }
        }
    }

    // Dictionary Tab
    fun onDictionaryQueryChanged(query: String) {
        _dictionaryQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _dictSearchResults.value = emptyList()
            _isDictSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(400) // debounce typing
            _isDictSearching.value = true
            val result = repository.searchJisho(query)
            result.onSuccess {
                _dictSearchResults.value = it
            }.onFailure {
                _dictSearchResults.value = emptyList()
            }
            _isDictSearching.value = false
        }
    }

    // Bookmark / Save word
    fun toggleSaveWord(wordItem: JishoWordItem, burmeseMeaning: String? = null) {
        viewModelScope.launch {
            val displayWord = wordItem.displayWord
            val existing = savedWords.value.firstOrNull { it.word == displayWord }
            if (existing != null) {
                repository.removeWord(existing)
            } else {
                val english = wordItem.primaryDefinitions.take(3).joinToString(", ")
                val reading = wordItem.displayReading
                val jlpt = wordItem.cleanJlpt ?: ""
                val pos = wordItem.partsOfSpeech.take(2).joinToString(", ")
                repository.saveWord(
                    SavedWord(
                        word = displayWord,
                        reading = reading,
                        englishMeaning = english,
                        burmeseMeaning = burmeseMeaning ?: _wordBurmeseMeaning.value ?: "",
                        jlptLevel = jlpt,
                        partsOfSpeech = pos,
                        isCommon = wordItem.isCommon ?: false
                    )
                )
            }
        }
    }

    fun deleteSavedWord(word: SavedWord) {
        viewModelScope.launch {
            repository.removeWord(word)
        }
    }

    fun deleteScanHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteScanHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllScanHistory()
        }
    }

    fun speak(text: String) {
        ttsManager.speak(text)
    }

    override fun onCleared() {
        super.onCleared()
        ocrManager.close()
        ttsManager.shutdown()
    }
}

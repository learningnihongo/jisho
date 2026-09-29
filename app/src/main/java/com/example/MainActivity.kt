package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.dictionary.DictionaryScreen
import com.example.ui.jisho.JishoDetailBottomSheet
import com.example.ui.lens.LensScreen
import com.example.ui.saved.SavedScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.translate.TranslateScreen

enum class AppScreen(val title: String) {
    LENS("Lens"),
    TRANSLATE("Translate"),
    DICTIONARY("Dictionary"),
    SAVED("Saved")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(AppScreen.LENS) }

    // State from ViewModel
    val capturedBitmap by viewModel.capturedBitmap.collectAsStateWithLifecycle()
    val ocrResult by viewModel.ocrResult.collectAsStateWithLifecycle()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsStateWithLifecycle()
    val lensTranslation by viewModel.lensTranslation.collectAsStateWithLifecycle()
    val isLensTranslating by viewModel.isLensTranslating.collectAsStateWithLifecycle()

    val selectedWord by viewModel.selectedWord.collectAsStateWithLifecycle()
    val jishoResults by viewModel.jishoResults.collectAsStateWithLifecycle()
    val isJishoLoading by viewModel.isJishoLoading.collectAsStateWithLifecycle()
    val jishoError by viewModel.jishoError.collectAsStateWithLifecycle()
    val burmeseMeaning by viewModel.wordBurmeseMeaning.collectAsStateWithLifecycle()

    val translateInput by viewModel.translateInputText.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val translatedResult by viewModel.translatedResult.collectAsStateWithLifecycle()
    val isTranslating by viewModel.isTranslating.collectAsStateWithLifecycle()
    val translateTokens by viewModel.translateTokens.collectAsStateWithLifecycle()

    val dictQuery by viewModel.dictionaryQuery.collectAsStateWithLifecycle()
    val dictResults by viewModel.dictSearchResults.collectAsStateWithLifecycle()
    val isDictSearching by viewModel.isDictSearching.collectAsStateWithLifecycle()

    val savedWords by viewModel.savedWords.collectAsStateWithLifecycle()
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()

    // Handle Back Press
    BackHandler(enabled = currentScreen != AppScreen.LENS || capturedBitmap != null) {
        if (capturedBitmap != null) {
            viewModel.clearCapturedImage()
        } else if (currentScreen != AppScreen.LENS) {
            currentScreen = AppScreen.LENS
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Hide bottom bar when viewing full camera scan with captured image if desired, or keep it accessible
            NavigationBar(
                modifier = Modifier.testTag("app_bottom_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.LENS,
                    onClick = { currentScreen = AppScreen.LENS },
                    icon = { Icon(Icons.Default.CenterFocusStrong, contentDescription = "Camera Lens") },
                    label = { Text("Lens") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_lens")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.TRANSLATE,
                    onClick = { currentScreen = AppScreen.TRANSLATE },
                    icon = { Icon(Icons.Default.Translate, contentDescription = "Translate") },
                    label = { Text("Translate") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_translate")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.DICTIONARY,
                    onClick = { currentScreen = AppScreen.DICTIONARY },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Dictionary") },
                    label = { Text("Dictionary") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_dictionary")
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SAVED,
                    onClick = { currentScreen = AppScreen.SAVED },
                    icon = {
                        if (savedWords.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ) {
                                        Text(text = if (savedWords.size > 99) "99+" else savedWords.size.toString())
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Bookmarks, contentDescription = "Saved")
                            }
                        } else {
                            Icon(Icons.Default.Bookmarks, contentDescription = "Saved")
                        }
                    },
                    label = { Text("Saved") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_item_saved")
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.LENS -> {
                LensScreen(
                    capturedBitmap = capturedBitmap,
                    ocrResult = ocrResult,
                    isProcessing = isOcrProcessing,
                    lensTranslation = lensTranslation,
                    isLensTranslating = isLensTranslating,
                    targetLanguage = targetLanguage,
                    onTargetLanguageChange = { viewModel.setTargetLanguage(it) },
                    onProcessBitmap = { bmp, rot -> viewModel.processImage(bmp, rot) },
                    onClearCapture = { viewModel.clearCapturedImage() },
                    onWordSelected = { word -> viewModel.lookupWordInJisho(word) },
                    onSpeak = { text -> viewModel.speak(text) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.TRANSLATE -> {
                TranslateScreen(
                    inputText = translateInput,
                    targetLanguage = targetLanguage,
                    translatedResult = translatedResult,
                    isTranslating = isTranslating,
                    tokens = translateTokens,
                    onInputChanged = { viewModel.updateTranslateInput(it) },
                    onTargetLanguageChanged = { viewModel.setTargetLanguage(it) },
                    onTranslate = { viewModel.executeTranslation() },
                    onWordSelected = { word -> viewModel.lookupWordInJisho(word) },
                    onSpeak = { text -> viewModel.speak(text) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.DICTIONARY -> {
                DictionaryScreen(
                    searchQuery = dictQuery,
                    results = dictResults,
                    isSearching = isDictSearching,
                    onQueryChanged = { viewModel.onDictionaryQueryChanged(it) },
                    onWordSelected = { word -> viewModel.lookupWordInJisho(word) },
                    onSpeak = { text -> viewModel.speak(text) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.SAVED -> {
                SavedScreen(
                    savedWords = savedWords,
                    scanHistory = scanHistory,
                    onWordSelected = { word -> viewModel.lookupWordInJisho(word) },
                    onDeleteWord = { word -> viewModel.deleteSavedWord(word) },
                    onDeleteHistory = { id -> viewModel.deleteScanHistory(id) },
                    onClearAllHistory = { viewModel.clearAllHistory() },
                    onSpeak = { text -> viewModel.speak(text) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Global Jisho Lookup Detail Bottom Sheet
        if (selectedWord != null) {
            JishoDetailBottomSheet(
                selectedWord = selectedWord,
                isLoading = isJishoLoading,
                errorMessage = jishoError,
                results = jishoResults,
                burmeseMeaning = burmeseMeaning,
                savedWords = savedWords,
                onDismiss = { viewModel.dismissJishoSheet() },
                onSpeak = { text -> viewModel.speak(text) },
                onToggleSave = { wordItem -> viewModel.toggleSaveWord(wordItem) }
            )
        }
    }
}

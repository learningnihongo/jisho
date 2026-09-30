package com.example.ui.saved

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.QuizSortMode
import com.example.data.local.SavedWord
import com.example.data.local.ScanHistory
import com.example.data.local.SrsAlgorithm
import com.example.data.local.SrsRating
import com.example.data.local.SrsStage
import com.example.data.remote.TranslationService
import com.example.ui.jisho.getJlptColor
import java.util.Date

@Composable
fun SavedScreen(
    savedWords: List<SavedWord>,
    scanHistory: List<ScanHistory>,
    onWordSelected: (String) -> Unit,
    onDeleteWord: (SavedWord) -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onClearAllHistory: () -> Unit,
    onSpeak: (String) -> Unit,
    onQuizReview: (SavedWord, SrsRating) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    // Write UTF-8 BOM so Excel and Anki render Kanji and Myanmar fonts properly
                    outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    outputStream.write(generateAnkiCsv(savedWords).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Exported ${savedWords.size} words to CSV for Anki!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val strugglingCount = remember(savedWords) {
        savedWords.count { SrsAlgorithm.getSrsStage(it) == SrsStage.STRUGGLING }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Saved & Spaced Repetition",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Vocabulary notebook with SRS Quiz practice",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs: Saved Words, SRS Quiz Practice, Scan History
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Words (${savedWords.size})") },
                icon = { Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_saved_words")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Flashcard Quiz")
                        if (strugglingCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$strugglingCount",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                icon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_srs_quiz")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("History (${scanHistory.size})") },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_scan_history")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // TAB 0: Saved Words Notebook
                if (savedWords.isEmpty()) {
                    EmptySavedWordsView()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            // SRS Quick Overview Banner
                            SrsOverviewHeader(savedWords = savedWords, onStartQuiz = { selectedTab = 1 })
                        }

                        item {
                            // Header actions: Anki CSV Export
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "All Saved Words (${savedWords.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = {
                                            exportLauncher.launch("lens_jisho_anki_vocab.csv")
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("export_csv_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Export CSV", fontSize = 12.sp)
                                    }

                                    IconButton(
                                        onClick = {
                                            val csv = generateAnkiCsv(savedWords)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/comma-separated-values"
                                                putExtra(Intent.EXTRA_SUBJECT, "LensJisho Vocabulary Export")
                                                putExtra(Intent.EXTRA_TEXT, csv)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Vocabulary CSV"))
                                        },
                                        modifier = Modifier.size(38.dp).testTag("share_csv_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share CSV",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        items(savedWords, key = { it.id }) { word ->
                            SavedWordItemCard(
                                word = word,
                                onClick = { onWordSelected(word.word) },
                                onDelete = { onDeleteWord(word) },
                                onSpeak = onSpeak
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
            1 -> {
                // TAB 1: Spaced Repetition (SRS) Quiz Practice
                if (savedWords.isEmpty()) {
                    EmptySavedWordsView()
                } else {
                    SrsQuizPracticeSection(
                        savedWords = savedWords,
                        onQuizReview = onQuizReview,
                        onSpeak = onSpeak
                    )
                }
            }
            2 -> {
                // TAB 2: Scan History
                if (scanHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No scan history yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Translations from Camera Lens or Translate tab will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Scans",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                TextButton(
                                    onClick = { showClearConfirm = true },
                                    modifier = Modifier.testTag("clear_history_button")
                                ) {
                                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        items(scanHistory, key = { it.id }) { item ->
                            ScanHistoryCard(
                                history = item,
                                onClick = { onWordSelected(item.originalText) },
                                onDelete = { onDeleteHistory(item.id) },
                                onSpeak = onSpeak
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Scan History?") },
            text = { Text("Are you sure you want to remove all translation scan history?") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Spaced Repetition (SRS) Quiz Practice Section
 * Actively prioritizes words the user is struggling to recall.
 */
data class SessionReviewedWord(
    val word: SavedWord,
    val rating: SrsRating,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SrsQuizPracticeSection(
    savedWords: List<SavedWord>,
    onQuizReview: (SavedWord, SrsRating) -> Unit,
    onSpeak: (String) -> Unit
) {
    var sortMode by remember { mutableStateOf(QuizSortMode.STRUGGLING) }
    var jlptFilter by remember { mutableStateOf("ALL") }
    var isSortDropdownOpen by remember { mutableStateOf(false) }
    var isFlipped by remember { mutableStateOf(false) }

    var initialDeckSize by remember { mutableIntStateOf(0) }

    // Dynamic session queue: re-queues struggling cards when "Again" is clicked
    val sessionQueue = remember { mutableStateListOf<SavedWord>() }

    // Stores reviews in current session for the Last 5 Words section
    val sessionReviews = remember { mutableStateListOf<SessionReviewedWord>() }

    // Rebuild queue when savedWords, sortMode, or jlptFilter changes
    fun rebuildQueue() {
        val sorted = SrsAlgorithm.filterAndSortQuizQueue(savedWords, sortMode, jlptFilter)
        sessionQueue.clear()
        sessionQueue.addAll(sorted)
        initialDeckSize = sorted.size
        isFlipped = false
    }

    LaunchedEffect(savedWords.size, sortMode, jlptFilter) {
        if (sessionQueue.isEmpty()) {
            rebuildQueue()
        }
    }

    val currentWord = sessionQueue.firstOrNull()

    // 3D Card Rotation Animation
    val cardRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "flashcard_3d_flip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // SRS Stats overview
        val strugglingWords = savedWords.filter { SrsAlgorithm.getSrsStage(it) == SrsStage.STRUGGLING }
        val now = System.currentTimeMillis()
        val dueWords = savedWords.filter { it.nextReviewTimestamp <= now }
        val masteredWords = savedWords.filter { SrsAlgorithm.getSrsStage(it) == SrsStage.MASTERED }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${strugglingWords.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
                    Text(text = "⚠️ ခက်ခဲဆဲ", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${dueWords.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    Text(text = "⏰ စစ်ဆေးရန်", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${sessionQueue.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 16.sp)
                    Text(text = "🎯 တန်းစီဇယား", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${masteredWords.size}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 16.sp)
                    Text(text = "🏆 ကျွမ်းကျင်", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 'Quiz Sorting' Dropdown Menu Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isSortDropdownOpen = true }
                .testTag("quiz_sorting_dropdown_trigger")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Quiz Sorting",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Quiz Sorting (ဦးစားပေး စီစဉ်မှု):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${sortMode.iconEmoji} ${sortMode.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${sortMode.myanmarTitle})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "${sessionQueue.size} words",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Open Sorting Dropdown",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DropdownMenu(
                expanded = isSortDropdownOpen,
                onDismissRequest = { isSortDropdownOpen = false },
                modifier = Modifier.widthIn(min = 280.dp)
            ) {
                QuizSortMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(mode.iconEmoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = mode.title,
                                        fontWeight = if (sortMode == mode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sortMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.myanmarTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        },
                        onClick = {
                            sortMode = mode
                            isSortDropdownOpen = false
                            rebuildQueue()
                        }
                    )
                }
            }
        }

        // JLPT Level Sub-filters when JLPT Sorting is selected
        if (sortMode == QuizSortMode.JLPT_LEVEL) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "N5", "N4", "N3", "N2", "N1").forEach { level ->
                    FilterChip(
                        selected = jlptFilter == level,
                        onClick = {
                            jlptFilter = level
                            rebuildQueue()
                        },
                        label = {
                            Text(
                                text = if (level == "ALL") "All Levels" else level,
                                fontSize = 11.sp,
                                fontWeight = if (jlptFilter == level) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quiz Progress Bar
        val deckTotal = initialDeckSize.coerceAtLeast(1)
        val completedCount = (deckTotal - sessionQueue.size).coerceAtLeast(0)
        val progressPercent = (completedCount.toFloat() / deckTotal.toFloat()).coerceIn(0f, 1f)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎮 Quiz Progress: $completedCount/$deckTotal စကားလုံးပြီးစီး",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "${(progressPercent * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progressPercent },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (currentWord == null) {
            // Session completed celebration
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Flashcard Quiz Completed! 🎉",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ယခု အသုတ်အတွက် ခက်ခဲသော စကားလုံးအားလုံးကို အောင်မြင်စွာ လေ့ကျင့်ပြီးပါပြီ။",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$deckTotal", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = "လေ့ကျင့်ခဲ့သည့် စကားလုံး", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${sessionReviews.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.tertiary)
                            Text(text = "စုစုပေါင်း ဖြေဆိုမှု", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { rebuildQueue() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ထပ်မံ ကစားမည် (Play Again)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val stage = SrsAlgorithm.getSrsStage(currentWord)

            // Interactive 3D Flip Flashcard Box
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    2.dp,
                    if (stage == SrsStage.STRUGGLING) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .graphicsLayer {
                        rotationY = cardRotation
                        cameraDistance = 14f * density
                    }
                    .clickable { isFlipped = !isFlipped }
                    .testTag("srs_active_flashcard")
            ) {
                if (cardRotation <= 90f) {
                    // FRONT OF FLASHCARD: Target Japanese Word + JLPT + Audio
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Card Top Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(stage.colorHex).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = stage.myanmarLabel,
                                    color = Color(stage.colorHex),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentWord.jlptLevel.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = getJlptColor(currentWord.jlptLevel)
                                    ) {
                                        Text(
                                            text = currentWord.jlptLevel,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = "ကျန်ရှိ: ${sessionQueue.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Target Word Calligraphy Center
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SelectionContainer {
                                Text(
                                    text = currentWord.word,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            IconButton(
                                onClick = { onSpeak(currentWord.word) },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                                    .size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Bottom Flip Prompt Hint
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🔄 ကတ်ပြားကို နှိပ်၍ အဖြေဖွင့်ပါ (Tap to flip)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // BACK OF FLASHCARD: Reading Furigana + Burmese Meaning + English (Mirrored rotationY = 180f)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Card Top Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "【အဖြေနှင့် အဓိပ္ပာယ်】",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = { onSpeak(currentWord.word) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Reading & Meanings
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (currentWord.reading.isNotBlank()) {
                                Text(
                                    text = "【${currentWord.reading}】",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            if (currentWord.burmeseMeaning.isNotBlank()) {
                                Text(
                                    text = "🇲🇲 ${currentWord.burmeseMeaning}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Text(
                                text = currentWord.englishMeaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Memory stats footer
                        Text(
                            text = "မှတ်မိမှု: ${currentWord.correctCount}x · ခက်ခဲခဲ့: ${currentWord.incorrectCount}x · အဆင့်: ${stage.myanmarLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Anki / SM-2 Spaced Repetition Grading Buttons (visible when flipped or can grade directly)
            if (isFlipped) {
                Text(
                    text = "မှတ်မိမှု အခြေအနေကို အဆင့်သတ်မှတ်ပါ (SRS Rating):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. AGAIN (မမှတ်မိ / ခက်သည်) -> Prioritizes and stays in active queue!
                    Button(
                        onClick = {
                            sessionReviews.add(0, SessionReviewedWord(currentWord, SrsRating.AGAIN))
                            onQuizReview(currentWord, SrsRating.AGAIN)
                            // Remove current and re-insert into queue (at position 2 or end) so user gets quizzed again!
                            sessionQueue.removeAt(0)
                            if (sessionQueue.size >= 2) {
                                sessionQueue.add(2, currentWord)
                            } else {
                                sessionQueue.add(currentWord)
                            }
                            isFlipped = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("srs_btn_again")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ခက်သည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("< 1m", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    // 2. HARD (အတော်ခက်)
                    Button(
                        onClick = {
                            sessionReviews.add(0, SessionReviewedWord(currentWord, SrsRating.HARD))
                            onQuizReview(currentWord, SrsRating.HARD)
                            sessionQueue.removeAt(0)
                            isFlipped = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("srs_btn_hard")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("အတော်ခက်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("1d", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    // 3. GOOD (မှတ်မိသည်)
                    Button(
                        onClick = {
                            sessionReviews.add(0, SessionReviewedWord(currentWord, SrsRating.GOOD))
                            onQuizReview(currentWord, SrsRating.GOOD)
                            sessionQueue.removeAt(0)
                            isFlipped = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("srs_btn_good")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("မှတ်မိ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("3d", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    // 4. EASY (လွယ်ကူ)
                    Button(
                        onClick = {
                            sessionReviews.add(0, SessionReviewedWord(currentWord, SrsRating.EASY))
                            onQuizReview(currentWord, SrsRating.EASY)
                            sessionQueue.removeAt(0)
                            isFlipped = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("srs_btn_easy")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("လွယ်ကူ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("5d+", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            } else {
                Button(
                    onClick = { isFlipped = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("srs_btn_show_answer")
                ) {
                    Text("အဖြေစစ်ဆေးမည် (Show Answer)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // SECTION: Last 5 Words Reviewed in the Current Session
        if (sessionReviews.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Last 5 Reviewed in Session:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${sessionReviews.size.coerceAtMost(5)}/5 words",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("srs_recent_reviews_list")
            ) {
                sessionReviews.take(5).forEach { reviewItem ->
                    SessionReviewItemCard(
                        review = reviewItem,
                        onSpeak = onSpeak
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Card showing an individual word reviewed in the current quiz session,
 * highlighting whether it was marked 'Again', 'Hard', 'Good', or 'Easy'.
 */
@Composable
fun SessionReviewItemCard(
    review: SessionReviewedWord,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ratingColor = when (review.rating) {
        SrsRating.AGAIN -> MaterialTheme.colorScheme.error
        SrsRating.HARD -> Color(0xFFF57C00)
        SrsRating.GOOD -> Color(0xFF2E7D32)
        SrsRating.EASY -> MaterialTheme.colorScheme.primary
    }

    val ratingBg = ratingColor.copy(alpha = 0.12f)

    val ratingLabel = when (review.rating) {
        SrsRating.AGAIN -> "Again (ခက်သည်)"
        SrsRating.HARD -> "Hard (အတော်ခက်)"
        SrsRating.GOOD -> "Good (မှတ်မိ)"
        SrsRating.EASY -> "Easy (လွယ်ကူ)"
    }

    val ratingIcon = when (review.rating) {
        SrsRating.AGAIN -> "⚠️"
        SrsRating.HARD -> "⏳"
        SrsRating.GOOD -> "✅"
        SrsRating.EASY -> "⭐"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, ratingColor.copy(alpha = 0.45f)),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("session_review_${review.word.word}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Audio button + Word + Reading + Meaning
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = { onSpeak(review.word.word) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Pronounce",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = review.word.word,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (review.word.reading.isNotBlank() && review.word.reading != review.word.word) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "【${review.word.reading}】",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    val meaning = review.word.burmeseMeaning.ifBlank { review.word.englishMeaning }
                    if (meaning.isNotBlank()) {
                        Text(
                            text = meaning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Highlighted SRS Rating Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ratingBg,
                border = BorderStroke(1.dp, ratingColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = ratingIcon, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ratingLabel,
                        color = ratingColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SrsOverviewHeader(
    savedWords: List<SavedWord>,
    onStartQuiz: () -> Unit
) {
    val struggling = savedWords.count { SrsAlgorithm.getSrsStage(it) == SrsStage.STRUGGLING }
    val now = System.currentTimeMillis()
    val due = savedWords.count { it.nextReviewTimestamp <= now }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().testTag("flashcard_quiz_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎮", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Flashcard Quiz Game (ကစားမည်)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (struggling > 0) "⚠️ ခက်ခဲသော စကားလုံး $struggling လုံး ရှိပါသည်။ Flashcard ဖြင့် လေ့ကျင့်ပါ" else "သိမ်းဆည်းထားသော စကားလုံးများကို 3D Flip Flashcard ဖြင့် ကစားလေ့ကျင့်ပါ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onStartQuiz,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (struggling > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("play_flashcard_quiz_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play Quiz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptySavedWordsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Bookmarks,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No saved words yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Look up words in Lens or Dictionary and tap the bookmark icon to save them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
fun SavedWordItemCard(
    word: SavedWord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stage = SrsAlgorithm.getSrsStage(word)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (stage == SrsStage.STRUGGLING) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("saved_word_card_${word.word}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (word.reading.isNotBlank() && word.reading != word.word) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "【${word.reading}】",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // SRS Stage badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(stage.colorHex).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = stage.myanmarLabel,
                            color = Color(stage.colorHex),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Burmese meaning
                if (word.burmeseMeaning.isNotBlank()) {
                    Text(
                        text = "🇲🇲 ${word.burmeseMeaning}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = word.englishMeaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Badges row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (word.jlptLevel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = getJlptColor(word.jlptLevel)
                        ) {
                            Text(
                                text = word.jlptLevel,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (word.incorrectCount > 0) {
                        Text(
                            text = "Failed: ${word.incorrectCount}x",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (word.correctCount > 0) {
                        Text(
                            text = "Passed: ${word.correctCount}x",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Speak
                IconButton(
                    onClick = { onSpeak(word.word) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Copy
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Saved Word", "${word.word} [${word.reading}]"))
                        Toast.makeText(context, "Copied: ${word.word}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_saved_${word.word}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScanHistoryCard(
    history: ScanHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateString = remember(history.timestamp) {
        DateFormat.format("MMM dd, yyyy · hh:mm a", Date(history.timestamp)).toString()
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("scan_history_card_${history.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onSpeak(history.originalText) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp).testTag("delete_history_${history.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            SelectionContainer {
                Text(
                    text = history.originalText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            SelectionContainer {
                Text(
                    text = "→ ${history.translatedText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Generate standard CSV export for Anki Deck import.
 */
fun generateAnkiCsv(words: List<SavedWord>): String {
    val sb = StringBuilder()
    sb.append("#separator:Comma\n")
    sb.append("#html:true\n")
    sb.append("#tags column:5\n")
    sb.append("Front,Furigana,MyanmarMeaning,EnglishMeaning,Tags\n")

    for (w in words) {
        val front = escapeCsv(w.word)
        val reading = escapeCsv(w.reading)
        val myanmar = escapeCsv(w.burmeseMeaning)
        val english = escapeCsv(w.englishMeaning)
        val tags = escapeCsv("LensJisho ${w.jlptLevel}".trim())
        sb.append("$front,$reading,$myanmar,$english,$tags\n")
    }
    return sb.toString()
}

fun escapeCsv(value: String): String {
    var result = value.replace("\"", "\"\"")
    if (result.contains(",") || result.contains("\n") || result.contains("\"")) {
        result = "\"$result\""
    }
    return result
}

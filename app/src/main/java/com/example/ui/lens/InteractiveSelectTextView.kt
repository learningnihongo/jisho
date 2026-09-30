package com.example.ui.lens

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.remote.NetworkClient
import com.example.ocr.DetectedLine
import com.example.ocr.DetectedWord
import com.example.ocr.OcrResult
import kotlinx.coroutines.launch

/**
 * Full-screen Google Lens Interactive Text Selection Canvas.
 * Implements the exact UX seen in Google Lens:
 * - Frosted white bounding boxes over all detected lines.
 * - Tap to select any Japanese word or phrase.
 * - Vivid blue selection box with circular blue teardrop selection handles.
 * - Floating dark callout pill menu: [Copy | Listen | Translate | Search | Jisho].
 * - Slide-up bottom sheet with Google search bar, instant Burmese translation, Furigana reading, and quick actions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveSelectTextView(
    bitmap: Bitmap,
    ocrResult: OcrResult,
    targetLanguage: String,
    onTargetLanguageChange: (String) -> Unit,
    onWordSelected: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Find initial word to select (e.g. first Kanji or first detected word, or first line)
    val initialSelection = remember(ocrResult) {
        ocrResult.words.firstOrNull { it.isKanji }?.text
            ?: ocrResult.words.firstOrNull()?.text
            ?: ocrResult.lines.firstOrNull()
            ?: ""
    }

    var selectedText by remember { mutableStateOf(initialSelection) }
    var selectedWordItem by remember {
        mutableStateOf(ocrResult.words.firstOrNull { it.text == initialSelection })
    }
    var selectedBoundingBox by remember {
        mutableStateOf<Rect?>(selectedWordItem?.boundingBox)
    }

    // Dynamic translation for selected text
    var selectedTextTranslation by remember { mutableStateOf<String?>(null) }
    var isTranslatingSelected by remember { mutableStateOf(false) }
    var isSheetExpanded by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Fetch translation when selectedText changes
    LaunchedEffect(selectedText, targetLanguage) {
        if (selectedText.isNotBlank()) {
            isTranslatingSelected = true
            selectedTextTranslation = null
            try {
                val res = NetworkClient.translationService.translate(
                    text = selectedText,
                    sourceLang = "ja",
                    targetLang = targetLanguage
                )
                if (res.isSuccess) {
                    selectedTextTranslation = res.translatedText
                } else {
                    selectedTextTranslation = null
                }
            } catch (e: Exception) {
                selectedTextTranslation = null
            } finally {
                isTranslatingSelected = false
            }
        } else {
            selectedTextTranslation = null
        }
    }

    fun selectWord(wordText: String, boundingBox: Rect?) {
        selectedText = wordText
        selectedBoundingBox = boundingBox
        selectedWordItem = ocrResult.words.firstOrNull { it.text == wordText }
        isSheetExpanded = true
    }

    var areBoxesVisible by remember { mutableStateOf(true) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("interactive_select_text_container")
    ) {
        // 1. TOP HEADER (Google Lens Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .zIndex(20f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("lens_select_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Camera",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Lens",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• စာသားရွေးပါ",
                            color = Color(0xFF60A5FA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Eye Icon Toggle for Bounding Boxes (Show/Hide boxes for clear view)
                IconButton(
                    onClick = {
                        areBoxesVisible = !areBoxesVisible
                        Toast.makeText(
                            context,
                            if (areBoxesVisible) "ကွက်လပ်ဘောင်များ ပြသထားပါသည် (Boxes visible)" else "ကွက်လပ်ဘောင်များ ဖျောက်ထားပါသည် (Boxes hidden)",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (areBoxesVisible) Color.Black.copy(alpha = 0.55f) else Color(0xFF1E293B).copy(alpha = 0.85f), CircleShape)
                        .border(if (areBoxesVisible) 0.dp else 1.dp, if (areBoxesVisible) Color.Transparent else Color.White.copy(alpha = 0.3f), CircleShape)
                        .testTag("toggle_select_boxes_button")
                ) {
                    Icon(
                        imageVector = if (areBoxesVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (areBoxesVisible) "Hide Bounding Boxes" else "Show Bounding Boxes",
                        tint = if (areBoxesVisible) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Select All Button
                IconButton(
                    onClick = {
                        selectedText = ocrResult.fullText
                        selectedBoundingBox = null
                        isSheetExpanded = true
                        Toast.makeText(context, "စာသားအားလုံး ရွေးချယ်ပြီးပါပြီ (Selected All)", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("select_all_text_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Select All",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Copy Selected Button
                IconButton(
                    onClick = {
                        if (selectedText.isNotBlank()) {
                            clipboardManager.setText(AnnotatedString(selectedText))
                            Toast.makeText(context, "ကူးယူပြီးပါပြီ (Copied): $selectedText", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .testTag("copy_selected_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Text",
                        tint = Color.White
                    )
                }
            }
        }

        // 2. IMAGE & BOUNDING BOX SELECTION OVERLAY CANVAS
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isLandscape) (if (isSheetExpanded) 130.dp else 20.dp) else (if (isSheetExpanded) 240.dp else 40.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            // Tap outside selection collapses bottom sheet or resets
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val containerWidthPx = with(density) { maxWidth.toPx() }
            val containerHeightPx = with(density) { maxHeight.toPx() }

            val imgWidth = bitmap.width.toFloat().coerceAtLeast(1f)
            val imgHeight = bitmap.height.toFloat().coerceAtLeast(1f)

            val scale = minOf(containerWidthPx / imgWidth, containerHeightPx / imgHeight)
            val scaledWidth = imgWidth * scale
            val scaledHeight = imgHeight * scale
            val offsetX = (containerWidthPx - scaledWidth) / 2f
            val offsetY = (containerHeightPx - scaledHeight) / 2f

            val viewportWidthDp = with(density) { scaledWidth.toDp() }
            val viewportHeightDp = with(density) { scaledHeight.toDp() }
            val viewportLeftDp = with(density) { offsetX.toDp() }
            val viewportTopDp = with(density) { offsetY.toDp() }

            val hasSelectedRegion = selectedText.isNotBlank()

            // Smooth animations for focus ring and border glow
            val infiniteTransition = rememberInfiniteTransition(label = "viewport_focus_pulse")
            val pulseGlowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.55f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_glow"
            )
            val focusRingPulse by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 5.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "focus_ring_pulse"
            )

            // Background Photo
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Freeze Frame for Text Selection",
                modifier = Modifier
                    .size(
                        width = viewportWidthDp,
                        height = viewportHeightDp
                    )
                    .clip(RoundedCornerShape(8.dp))
            )

            // VISUAL INDICATOR: Active Lens Viewport Focus Ring & Border Change when a text region is selected
            if (hasSelectedRegion && areBoxesVisible) {
                // Outer illuminated viewport focus border
                Box(
                    modifier = Modifier
                        .offset(x = viewportLeftDp, y = viewportTopDp)
                        .size(width = viewportWidthDp, height = viewportHeightDp)
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFF00E5FF).copy(alpha = pulseGlowAlpha),
                                    Color(0xFF3B82F6).copy(alpha = pulseGlowAlpha),
                                    Color(0xFF00E5FF).copy(alpha = pulseGlowAlpha)
                                )
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .zIndex(12f)
                ) {
                    // High-tech Camera Focus Corner Reticles in all 4 corners
                    val bracketLength = 22.dp
                    val bracketThickness = 3.5.dp
                    val bracketColor = Color(0xFF00E5FF)

                    // Top-Left Corner Bracket
                    Box(modifier = Modifier.align(Alignment.TopStart)) {
                        Box(modifier = Modifier.size(width = bracketLength, height = bracketThickness).background(bracketColor, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.size(width = bracketThickness, height = bracketLength).background(bracketColor, RoundedCornerShape(2.dp)))
                    }

                    // Top-Right Corner Bracket
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        Box(modifier = Modifier.align(Alignment.TopEnd).size(width = bracketLength, height = bracketThickness).background(bracketColor, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.align(Alignment.TopEnd).size(width = bracketThickness, height = bracketLength).background(bracketColor, RoundedCornerShape(2.dp)))
                    }

                    // Bottom-Left Corner Bracket
                    Box(modifier = Modifier.align(Alignment.BottomStart)) {
                        Box(modifier = Modifier.align(Alignment.BottomStart).size(width = bracketLength, height = bracketThickness).background(bracketColor, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.align(Alignment.BottomStart).size(width = bracketThickness, height = bracketLength).background(bracketColor, RoundedCornerShape(2.dp)))
                    }

                    // Bottom-Right Corner Bracket
                    Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(width = bracketLength, height = bracketThickness).background(bracketColor, RoundedCornerShape(2.dp)))
                        Box(modifier = Modifier.align(Alignment.BottomEnd).size(width = bracketThickness, height = bracketLength).background(bracketColor, RoundedCornerShape(2.dp)))
                    }
                }

                // Top Viewport Focus Status Pill Indicator
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF00E5FF).copy(alpha = pulseGlowAlpha)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .offset(x = 0.dp, y = (viewportTopDp + 8.dp).coerceAtLeast(8.dp))
                        .align(Alignment.TopCenter)
                        .zIndex(22f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00E5FF), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🎯 Text Region Focused: $selectedText",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Frosted White Highlight Boxes for each detected line (as in Google Lens Screenshot 2)
            ocrResult.detailedLines.forEach { line ->
                val lineBox = line.boundingBox
                if (lineBox != null) {
                    val lineLeftDp = with(density) { (offsetX + lineBox.left * scale).toDp() }
                    val lineTopDp = with(density) { (offsetY + lineBox.top * scale).toDp() }
                    val lineWidthDp = with(density) { ((lineBox.right - lineBox.left) * scale).toDp().coerceAtLeast(24.dp) }
                    val lineHeightDp = with(density) { ((lineBox.bottom - lineBox.top) * scale).toDp().coerceAtLeast(18.dp) }

                    Box(
                        modifier = Modifier
                            .offset(x = lineLeftDp, y = lineTopDp)
                            .size(width = lineWidthDp, height = lineHeightDp)
                            .background(Color.White.copy(alpha = 0.24f), RoundedCornerShape(4.dp))
                            .border(0.8.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                selectWord(line.text, lineBox)
                            }
                    )
                }
            }

            // Word Elements & Tappable/Selected Word Overlays
            ocrResult.words.forEach { word ->
                val box = word.boundingBox
                if (box != null) {
                    val wLeftDp = with(density) { (offsetX + box.left * scale).toDp() }
                    val wTopDp = with(density) { (offsetY + box.top * scale).toDp() }
                    val wWidthDp = with(density) { ((box.right - box.left) * scale).toDp().coerceAtLeast(16.dp) }
                    val wHeightDp = with(density) { ((box.bottom - box.top) * scale).toDp().coerceAtLeast(14.dp) }

                    val isThisWordSelected = selectedText == word.text

                    if (isThisWordSelected && areBoxesVisible) {
                        // VISUAL INDICATOR: Expanding animated focus pulse halo ring around the selected region
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = wLeftDp - focusRingPulse.dp,
                                    y = wTopDp - focusRingPulse.dp
                                )
                                .size(
                                    width = wWidthDp + (focusRingPulse * 2).dp,
                                    height = wHeightDp + (focusRingPulse * 2).dp
                                )
                                .border(
                                    width = 1.6.dp,
                                    color = Color(0xFF00E5FF).copy(alpha = (1f - focusRingPulse / 7f).coerceIn(0.2f, 0.95f)),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .zIndex(14f)
                        )

                        // VIBRANT BLUE SELECTION BOX WITH TEARDROP HANDLES
                        Box(
                            modifier = Modifier
                                .offset(x = wLeftDp, y = wTopDp)
                                .size(width = wWidthDp, height = wHeightDp)
                                .background(Color(0xFF2563EB).copy(alpha = 0.68f), RoundedCornerShape(3.dp))
                                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(3.dp))
                                .zIndex(15f)
                        ) {
                            // Left selection pin handle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .offset(x = (-5).dp, y = 6.dp)
                                    .size(11.dp)
                                    .background(Color(0xFF2563EB), CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                            // Right selection pin handle
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 5.dp, y = 6.dp)
                                    .size(11.dp)
                                    .background(Color(0xFF2563EB), CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }
                    } else {
                        // Transparent clickable hit target over the word
                        Box(
                            modifier = Modifier
                                .offset(x = wLeftDp, y = wTopDp)
                                .size(width = wWidthDp, height = wHeightDp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectWord(word.text, box)
                                }
                        )
                    }
                }
            }

            // 3. FLOATING CALLOUT TOOLBAR (Appears directly above the selected word, matching Screenshot 2)
            if (selectedText.isNotBlank()) {
                val targetBox = selectedBoundingBox ?: ocrResult.words.firstOrNull { it.text == selectedText }?.boundingBox
                val bubbleLeftDp = if (targetBox != null) {
                    val centerXPx = offsetX + (targetBox.left + targetBox.right) / 2f * scale
                    with(density) { (centerXPx).toDp() - 120.dp }.coerceIn(8.dp, maxWidth - 260.dp)
                } else {
                    16.dp
                }

                val bubbleTopDp = if (targetBox != null) {
                    val topPx = offsetY + targetBox.top * scale
                    with(density) { (topPx).toDp() - 48.dp }.coerceAtLeast(8.dp)
                } else {
                    16.dp
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF1F2024),
                    shadowElevation = 10.dp,
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .offset(x = bubbleLeftDp, y = bubbleTopDp)
                        .zIndex(25f)
                        .testTag("floating_action_toolbar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Copy Action
                        Text(
                            text = "Copy",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(selectedText))
                                    Toast.makeText(context, "ကူးယူပြီးပါပြီ (Copied): $selectedText", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("bubble_copy_action")
                        )

                        // Listen Action
                        Text(
                            text = "Listen",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSpeak(selectedText) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("bubble_listen_action")
                        )

                        // Translate Action
                        Text(
                            text = "Translate",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    isSheetExpanded = true
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("bubble_translate_action")
                        )

                        // Search Action
                        Text(
                            text = "Search",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(selectedText)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("bubble_search_action")
                        )

                        // Jisho Lookup Action
                        Text(
                            text = "Jisho",
                            color = Color(0xFF60A5FA),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onWordSelected(selectedText) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("bubble_jisho_action")
                        )

                        // More overflow menu
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Jisho.org တွင် ရှာရန်") },
                                    onClick = {
                                        showMoreMenu = false
                                        val uri = Uri.parse("https://jisho.org/search/${Uri.encode(selectedText)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Google Translate သို့ ပို့မည်") },
                                    onClick = {
                                        showMoreMenu = false
                                        val uri = Uri.parse("https://translate.google.com/?sl=ja&tl=$targetLanguage&text=${Uri.encode(selectedText)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. FLOATING TRANSLATE FAB (文A Button at bottom right, matching Screenshot 2)
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E293B),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(
                    x = (-16).dp,
                    y = if (isLandscape) {
                        if (isSheetExpanded) (-140).dp else (-32).dp
                    } else {
                        if (isSheetExpanded) (-248).dp else (-56).dp
                    }
                )
                .size(48.dp)
                .zIndex(20f)
                .clickable {
                    onTargetLanguageChange(if (targetLanguage == "my") "en" else "my")
                    Toast.makeText(
                        context,
                        if (targetLanguage == "my") "Language: English" else "Language: Myanmar (မြန်မာ)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .testTag("floating_translate_lang_fab")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (targetLanguage == "my") "文A" else "A文",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 5. GOOGLE LENS STYLE SLIDE-UP BOTTOM SHEET (Matching bottom half of Screenshot 2)
        AnimatedVisibility(
            visible = isSheetExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(30f)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFF17181C),
                tonalElevation = 12.dp,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isLandscape) 190.dp else 360.dp)
                    .navigationBarsPadding()
                    .testTag("lens_bottom_result_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Top drag pill handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // GOOGLE LENS SEARCH BAR (Matching Screenshot 2)
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF26282E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Google "G" Badge
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF4285F4),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("G", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = selectedText.ifBlank { "စာသားတစ်ခု နှိပ်၍ ရွေးချယ်ပါ..." },
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (selectedText.isNotBlank()) {
                                IconButton(
                                    onClick = { onSpeak(selectedText) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak",
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(selectedText))
                                        Toast.makeText(context, "ကူးယူပြီးပါပြီ (Copied)", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Mode Chips: [All | Jisho Def | Myanmar 🇲🇲 | English 🇬🇧 | Save]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF383A42),
                            modifier = Modifier
                                .clickable { onWordSelected(selectedText) }
                                .testTag("chip_jisho_sheet")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📖 Jisho အဘိဓာန်", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (targetLanguage == "my") Color(0xFF1E3A8A) else Color(0xFF383A42),
                            modifier = Modifier
                                .clickable { onTargetLanguageChange("my") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🇲🇲 မြန်မာ", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (targetLanguage == "en") Color(0xFF1E3A8A) else Color(0xFF383A42),
                            modifier = Modifier
                                .clickable { onTargetLanguageChange("en") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🇬🇧 English", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // RESULT CARD: Selected Word Translation / Meaning Details
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF202127),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (selectedWordItem?.isKanji == true) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF3B82F6).copy(alpha = 0.3f)
                                        ) {
                                            Text(
                                                text = "Kanji 漢字",
                                                color = Color(0xFF93C5FD),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FilledTonalButton(
                                        onClick = { onWordSelected(selectedText) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Jisho Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (isTranslatingSelected) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Color(0xFF60A5FA), strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ဘာသာပြန်ဆိုနေပါသည်…", color = Color.Gray, fontSize = 12.sp)
                                }
                            } else if (!selectedTextTranslation.isNullOrBlank()) {
                                SelectionContainer {
                                    Text(
                                        text = "${if (targetLanguage == "my") "🇲🇲 မြန်မာ: " else "🇬🇧 en: "}${selectedTextTranslation}",
                                        color = Color(0xFF93C5FD),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "စာသားရွေးချယ်ပြီး Jisho အဘိဓာန် သို့မဟုတ် မြန်မာဘာသာပြန် ရယူပါ",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Tappable word pills strip for easy precision selection
                    val otherWords = ocrResult.words.filter { it.text.isNotBlank() }.take(10)
                    if (otherWords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "စကင်ဖတ်မိသော အခြားစကားလုံးများ (Tap to select):",
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            otherWords.forEach { word ->
                                val isSelected = selectedText == word.text
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF2E3038),
                                    modifier = Modifier
                                        .clickable { selectWord(word.text, word.boundingBox) }
                                ) {
                                    Text(
                                        text = word.text,
                                        color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

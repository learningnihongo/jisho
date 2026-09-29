package com.example.ui.lens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.remote.TranslationService
import com.example.ocr.OcrResult
import java.io.InputStream
import java.util.concurrent.Executors

@Composable
fun LensScreen(
    capturedBitmap: Bitmap?,
    ocrResult: OcrResult?,
    isProcessing: Boolean,
    lensTranslation: String?,
    isLensTranslating: Boolean,
    targetLanguage: String,
    onTargetLanguageChange: (String) -> Unit,
    onProcessBitmap: (Bitmap, Int) -> Unit,
    onClearCapture: () -> Unit,
    onWordSelected: (String) -> Unit,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Photo picker launcher (0-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = decodeUriToBitmap(context, uri)
            if (bitmap != null) {
                onProcessBitmap(bitmap, 0)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (capturedBitmap != null) {
            // View mode: Captured image with OCR result and interactive word pills
            CapturedResultView(
                bitmap = capturedBitmap,
                ocrResult = ocrResult,
                isProcessing = isProcessing,
                lensTranslation = lensTranslation,
                isLensTranslating = isLensTranslating,
                targetLanguage = targetLanguage,
                onTargetLanguageChange = onTargetLanguageChange,
                onWordSelected = onWordSelected,
                onClear = onClearCapture,
                onSpeak = onSpeak
            )
        } else {
            // Live Camera or Permission Prompt
            if (hasCameraPermission) {
                LiveCameraView(
                    onPhotoCaptured = { bitmap, rotation ->
                        onProcessBitmap(bitmap, rotation)
                    },
                    onOpenGallery = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onUseDemoSample = {
                        val sampleBitmap = createDemoJapaneseBitmap()
                        onProcessBitmap(sampleBitmap, 0)
                    }
                )
            } else {
                CameraPermissionView(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onOpenGallery = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onUseDemoSample = {
                        val sampleBitmap = createDemoJapaneseBitmap()
                        onProcessBitmap(sampleBitmap, 0)
                    }
                )
            }
        }
    }
}

@Composable
fun LiveCameraView(
    onPhotoCaptured: (Bitmap, Int) -> Unit,
    onOpenGallery: () -> Unit,
    onUseDemoSample: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            },
            modifier = Modifier.fillMaxSize()
        )

        // Viewfinder Frame & Animated Laser Scan Line
        ViewfinderOverlay()

        // Top Controls (Flash, Switch Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                IconButton(
                    onClick = {
                        isFlashOn = !isFlashOn
                        camera?.cameraControl?.enableTorch(isFlashOn)
                    },
                    modifier = Modifier.testTag("flash_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (isFlashOn) Color.Yellow else Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Japanese Lens OCR",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier.testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Controls (Gallery, Shutter, Demo Sample)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick Hint
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Point at Japanese text & tap to scan",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Picker Button
                IconButton(
                    onClick = onOpenGallery,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("gallery_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Choose from Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                        .padding(6.dp)
                        .background(Color.White, CircleShape)
                        .clickable {
                            imageCapture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        val bitmap = imageProxy.toBitmap()
                                        val rotation = imageProxy.imageInfo.rotationDegrees
                                        imageProxy.close()
                                        ContextCompat.getMainExecutor(context).execute {
                                            onPhotoCaptured(bitmap, rotation)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                    }
                                }
                            )
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Capture Lens Scan",
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Sample Japanese text button (Useful for instant test)
                IconButton(
                    onClick = onUseDemoSample,
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("demo_sample_button")
                ) {
                    Text(
                        text = "Sample\n日本語",
                        color = Color.White,
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ViewfinderOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_scan")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_offset"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val frameWidth = maxWidth * 0.85f
        val frameHeight = maxHeight * 0.45f

        Box(
            modifier = Modifier
                .size(width = frameWidth, height = frameHeight)
                .align(Alignment.Center)
                .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
        ) {
            // Laser scan bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = frameHeight * laserOffset)
                    .background(Color(0xFF00E5FF))
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CapturedResultView(
    bitmap: Bitmap,
    ocrResult: OcrResult?,
    isProcessing: Boolean,
    lensTranslation: String?,
    isLensTranslating: Boolean,
    targetLanguage: String,
    onTargetLanguageChange: (String) -> Unit,
    onWordSelected: (String) -> Unit,
    onClear: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.testTag("lens_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Scan")
            }

            Text(
                text = "Lens Recognition",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Language Switcher (Myanmar / English)
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (targetLanguage == "my") MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.clickable { onTargetLanguageChange("my") }
                ) {
                    Text(
                        text = "မြန်မာ",
                        color = if (targetLanguage == "my") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (targetLanguage == "en") MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier.clickable { onTargetLanguageChange("en") }
                ) {
                    Text(
                        text = "English",
                        color = if (targetLanguage == "en") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Captured Image Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured Lens Photo",
                    modifier = Modifier.fillMaxSize()
                )

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Detecting Japanese text…", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val clipboardManager = LocalClipboardManager.current
            val hasRecognizedText = ocrResult?.fullText?.isNotBlank() == true

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 1: Recognized Japanese Text Card (ဖတ်ရှုတွေ့ရှိသော ဂျပန်စာသား)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("lens_recognized_japanese_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🇯🇵", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Recognized Japanese Text (ဂျပန်စာသား)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (hasRecognizedText) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onSpeak(ocrResult!!.fullText) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak Japanese",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(ocrResult!!.fullText))
                                        Toast.makeText(context, "ဂျပန်စာသား ကူးယူပြီးပါပြီ (Copied)", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Text",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (hasRecognizedText) {
                        Text(
                            text = ocrResult!!.fullText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions for Scanned Text: Search in Jisho.org & Open Web
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onWordSelected(ocrResult.fullText) },
                                modifier = Modifier.weight(1f).testTag("search_jisho_full_text_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Jisho တွင် ရှာဖွေမည်", fontSize = 12.sp, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse("https://jisho.org/search/${Uri.encode(ocrResult.fullText)}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("open_jisho_web_full_text_btn")
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Jisho Web", fontSize = 12.sp)
                            }
                        }
                    } else if (isProcessing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reading Japanese text from camera…", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text(
                            text = "No Japanese text detected yet. Try capturing closer or with better lighting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 2: Translation Text Results Card (ဘာသာပြန် စာသား ရလဒ်များ - Text Results)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().testTag("lens_translation_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (targetLanguage == "my") "ဘာသာပြန်ဆိုချက် (Translation Result - မြန်မာ)" else "Translation Result (English)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!lensTranslation.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(lensTranslation))
                                    Toast.makeText(context, "ဘာသာပြန် ကူးယူပြီးပါပြီ (Copied)", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Translation",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isLensTranslating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Translating Japanese to Myanmar…", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else if (!lensTranslation.isNullOrBlank()) {
                        Text(
                            text = lensTranslation,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 24.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(lensTranslation))
                                    Toast.makeText(context, "ဘာသာပြန် ကူးယူပြီးပါပြီ (Translation copied)", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("copy_lens_translation_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ဘာသာပြန် ကူးယူရန်", fontSize = 12.sp)
                            }

                            if (hasRecognizedText) {
                                OutlinedButton(
                                    onClick = {
                                        TranslationService.openGoogleTranslateWebOrApp(
                                            context,
                                            ocrResult!!.fullText,
                                            targetLanguage
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("open_translate_lens_btn")
                                ) {
                                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Google Translate", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (!hasRecognizedText) "No text detected in this photo. Try capturing clearer text." else "Ready to translate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: Detected Japanese Words (Tap to look up in Jisho.org)
            Text(
                text = "Detected Words (Jisho.org တွင် တစ်လုံးချင်း စစ်ဆေးရန်):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))

            val words = ocrResult?.words ?: emptyList()
            if (words.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    words.forEach { wordItem ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (wordItem.isKanji) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (wordItem.isKanji) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else Color.Transparent),
                            modifier = Modifier
                                .clickable { onWordSelected(wordItem.text) }
                                .testTag("word_chip_${wordItem.text}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = wordItem.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (wordItem.isKanji) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🔍 Jisho",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else if (!isProcessing) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No distinct words found. Try taking a photo closer to the Japanese text.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Retake Button
            FilledTonalButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth().testTag("retake_scan_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Another Text")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun CameraPermissionView(
    onRequestPermission: () -> Unit,
    onOpenGallery: () -> Unit,
    onUseDemoSample: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "To scan Japanese text with Google Lens style OCR, please grant camera access, or pick an existing image from your gallery.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().testTag("grant_camera_permission_button")
        ) {
            Text("Grant Camera Permission")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenGallery,
            modifier = Modifier.fillMaxWidth().testTag("pick_gallery_perm_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick Image from Gallery")
        }

        Spacer(modifier = Modifier.height(12.dp))

        FilledTonalButton(
            onClick = onUseDemoSample,
            modifier = Modifier.fillMaxWidth().testTag("try_sample_image_button")
        ) {
            Text("Try Demo Japanese Image")
        }
    }
}

fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

/**
 * Creates a demo bitmap containing sample Japanese text for testing in environments without physical camera.
 */
fun createDemoJapaneseBitmap(): Bitmap {
    val width = 720
    val height = 480
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)

    val paint = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 46f
        isAntiAlias = true
        isFakeBoldText = true
    }

    val subPaint = Paint().apply {
        color = AndroidColor.GRAY
        textSize = 34f
        isAntiAlias = true
    }

    canvas.drawText("日本語の勉強はとても面白いです。", 50f, 120f, paint)
    canvas.drawText("毎朝、コーヒーを飲みます。", 50f, 200f, paint)
    canvas.drawText("東京と京都へ旅行に行きたいです。", 50f, 280f, paint)
    canvas.drawText("桜の花が綺麗に咲いています。", 50f, 360f, paint)
    canvas.drawText("ありがとう ございます (Thank you)", 50f, 430f, subPaint)

    return bitmap
}

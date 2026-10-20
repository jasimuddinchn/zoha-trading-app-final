package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.ProfitGreen
import com.example.util.ImeiUtils
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Fast camera barcode & IMEI scanner overlay for stock entry, edit, and sales searches.
 * Supports:
 * - Live camera scanning with auto-detection of 15-digit IMEI barcodes and QR codes
 * - Pick from Gallery (🖼️) button to extract IMEIs from screenshots, photos, and barcodes
 * - ML Kit Barcode Scanning + ML Kit Text Recognition (OCR)
 * - Multiple IMEI selection dialog for Dual-SIM / Multi-IMEI images/barcodes
 */
@Composable
fun ImeiCameraScannerDialog(
    onDismiss: () -> Unit,
    onImeiScanned: (imei1: String, imei2: String?) -> Unit,
    initialHelperTitle: String = "Scan IMEI"
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("imei_camera_scanner_dialog")
        ) {
            if (hasCameraPermission) {
                CameraScannerContent(
                    context = context,
                    onDismiss = onDismiss,
                    onImeiScanned = onImeiScanned,
                    initialHelperTitle = initialHelperTitle
                )
            } else {
                CameraPermissionRequestContent(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScannerContent(
    context: Context,
    onDismiss: () -> Unit,
    onImeiScanned: (imei1: String, imei2: String?) -> Unit,
    initialHelperTitle: String = "Scan IMEI"
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isProcessingSuccess by remember { mutableStateOf(false) }
    var isAnalyzingGallery by remember { mutableStateOf(false) }

    // Multi-IMEI candidate list for Selection Dialog
    var candidateImeis by remember { mutableStateOf<List<String>>(emptyList()) }
    var showMultiImeiDialog by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {}
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    /**
     * Process an image picked from device gallery:
     * 1. Extracts barcodes (Code 128, 39, QR, EAN)
     * 2. Runs OCR text recognition (*#06# screenshots, printed receipts, box labels)
     * 3. Extracts all valid 15-digit IMEIs
     */
    fun processGalleryImage(uri: Uri) {
        isAnalyzingGallery = true
        isProcessingSuccess = true // Pause camera live detection

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)

                val barcodeOptions = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(barcodeOptions)
                val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                barcodeScanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val imeisFromBarcodes = mutableListOf<String>()
                        for (barcode in barcodes) {
                            val raw = barcode.rawValue ?: barcode.displayValue ?: ""
                            imeisFromBarcodes.addAll(ImeiUtils.extractImeisFromText(raw))
                        }

                        // Run OCR text recognition on the same image
                        textRecognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                isAnalyzingGallery = false
                                val imeisFromText = ImeiUtils.extractImeisFromText(visionText.text)
                                val allFound = (imeisFromBarcodes + imeisFromText).distinct()

                                if (allFound.isEmpty()) {
                                    isProcessingSuccess = false
                                    Toast.makeText(
                                        context,
                                        "No 15-digit IMEI found in selected photo. Please ensure barcode or *#06# is clear.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else if (allFound.size == 1) {
                                    triggerHapticFeedback()
                                    isProcessingSuccess = true
                                    onImeiScanned(allFound.first(), null)
                                } else {
                                    triggerHapticFeedback()
                                    candidateImeis = allFound
                                    showMultiImeiDialog = true
                                }
                            }
                            .addOnFailureListener {
                                isAnalyzingGallery = false
                                val allFound = imeisFromBarcodes.distinct()
                                if (allFound.isEmpty()) {
                                    isProcessingSuccess = false
                                    Toast.makeText(
                                        context,
                                        "No 15-digit IMEI detected in image.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else if (allFound.size == 1) {
                                    triggerHapticFeedback()
                                    isProcessingSuccess = true
                                    onImeiScanned(allFound.first(), null)
                                } else {
                                    triggerHapticFeedback()
                                    candidateImeis = allFound
                                    showMultiImeiDialog = true
                                }
                            }
                    }
                    .addOnFailureListener {
                        // Barcode failed, fallback directly to OCR
                        textRecognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                isAnalyzingGallery = false
                                val imeisFromText = ImeiUtils.extractImeisFromText(visionText.text).distinct()
                                if (imeisFromText.isEmpty()) {
                                    isProcessingSuccess = false
                                    Toast.makeText(
                                        context,
                                        "No 15-digit IMEI found in selected photo.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else if (imeisFromText.size == 1) {
                                    triggerHapticFeedback()
                                    isProcessingSuccess = true
                                    onImeiScanned(imeisFromText.first(), null)
                                } else {
                                    triggerHapticFeedback()
                                    candidateImeis = imeisFromText
                                    showMultiImeiDialog = true
                                }
                            }
                            .addOnFailureListener {
                                isAnalyzingGallery = false
                                isProcessingSuccess = false
                                Toast.makeText(
                                    context,
                                    "Could not read image. Please try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isAnalyzingGallery = false
                    isProcessingSuccess = false
                    Log.e("ImeiScanner", "Error analyzing gallery image", e)
                    Toast.makeText(context, "Failed to load selected photo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Gallery Image Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            processGalleryImage(uri)
        }
    }

    // Laser scan animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. CameraX PreviewView with TextureView (COMPATIBLE mode) to prevent surface buffer abandonment
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                            .build()
                        val scanner = BarcodeScanning.getClient(options)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !isProcessingSuccess && !showMultiImeiDialog && !isAnalyzingGallery) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        if (barcodes.isNotEmpty() && !isProcessingSuccess && !showMultiImeiDialog) {
                                            val foundList = mutableListOf<String>()
                                            for (barcode in barcodes) {
                                                val raw = barcode.rawValue ?: barcode.displayValue ?: ""
                                                val extracted = ImeiUtils.extractImeisFromText(raw)
                                                foundList.addAll(extracted)
                                            }

                                            val distinctImeis = foundList.distinct()
                                            if (distinctImeis.isNotEmpty()) {
                                                if (distinctImeis.size == 1) {
                                                    // Single IMEI found: immediately finish and return
                                                    isProcessingSuccess = true
                                                    triggerHapticFeedback()
                                                    val singleImei = distinctImeis.first()
                                                    previewView.post {
                                                        try {
                                                            provider.unbindAll()
                                                        } catch (_: Exception) {}
                                                        onImeiScanned(singleImei, null)
                                                    }
                                                } else {
                                                    // Multiple IMEIs detected: trigger clean selection dialog
                                                    isProcessingSuccess = true
                                                    triggerHapticFeedback()
                                                    previewView.post {
                                                        candidateImeis = distinctImeis
                                                        showMultiImeiDialog = true
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ImeiScanner", "Barcode scanning error", e)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        provider.unbindAll()
                        val boundCamera = provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        camera = boundCamera
                    } catch (exc: Exception) {
                        Log.e("ImeiScanner", "Camera binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            onRelease = {
                try {
                    cameraProvider?.unbindAll()
                } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Viewfinder Overlay Mask
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.70f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = initialHelperTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pick from Gallery Button
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("scanner_gallery_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Pick from Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Flashlight toggle
                    IconButton(
                        onClick = {
                            val newTorchState = !isFlashlightOn
                            camera?.cameraControl?.enableTorch(newTorchState)
                            isFlashlightOn = newTorchState
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (isFlashlightOn) GoldAccent else Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("scanner_flashlight_btn")
                    ) {
                        Icon(
                            imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashlightOn) Color(0xFF0F172A) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Close button
                    IconButton(
                        onClick = {
                            try {
                                cameraProvider?.unbindAll()
                            } catch (_: Exception) {}
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("scanner_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // MIDDLE: Scanning Box Reticle
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .aspectRatio(1.35f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = 2.dp,
                        color = if (isProcessingSuccess) ProfitGreen else GoldAccent,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("scanner_viewfinder_box"),
                contentAlignment = Alignment.Center
            ) {
                // Corner accents for high-tech HUD look
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // Animated laser scan line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = (laserPosition * 180).toInt() * 3
                                )
                            }
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        if (isProcessingSuccess) ProfitGreen else Color(0xFFEF4444),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                if (isProcessingSuccess && !showMultiImeiDialog && !isAnalyzingGallery) {
                    Surface(
                        color = ProfitGreen.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "✓ IMEI Detected!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Analyzing Gallery Loading Indicator
                if (isAnalyzingGallery) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, GoldAccent),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = GoldAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Extracting IMEI from photo...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // BOTTOM FOOTER: Entirely clean and blank
            Spacer(modifier = Modifier.height(48.dp))
        }

        // 3. Multiple IMEI Selection Dialog
        if (showMultiImeiDialog && candidateImeis.size > 1) {
            MultiImeiSelectionDialog(
                imeiList = candidateImeis,
                onSelectImei = { selected, secondary ->
                    showMultiImeiDialog = false
                    candidateImeis = emptyList()
                    try {
                        cameraProvider?.unbindAll()
                    } catch (_: Exception) {}
                    onImeiScanned(selected, secondary)
                },
                onDismiss = {
                    showMultiImeiDialog = false
                    candidateImeis = emptyList()
                    isProcessingSuccess = false // Resume live camera scanner
                }
            )
        }
    }
}

/**
 * Clean Single-Selection Radio List Dialog when multiple IMEIs are detected (e.g. Dual-SIM barcodes).
 * Strictly inserts only the user-selected IMEI string.
 */
@Composable
fun MultiImeiSelectionDialog(
    imeiList: List<String>,
    onSelectImei: (selectedImei: String, otherImei: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("multi_imei_selection_dialog"),
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, GoldAccent),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(GoldAccent.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Multiple IMEIs Found",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select one to insert:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of IMEIs with Radio Button Single Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    imeiList.forEachIndexed { index, candidate ->
                        val isSelected = selectedIndex == index
                        Surface(
                            onClick = { selectedIndex = index },
                            color = if (isSelected) Color(0xFF1E293B) else Color(0xFF141E30),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) GoldAccent else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("multi_imei_option_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedIndex = index },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = GoldAccent,
                                        unselectedColor = Color(0xFF64748B)
                                    ),
                                    modifier = Modifier.testTag("multi_imei_radio_$index")
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "IMEI ${index + 1}: $candidate",
                                        color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = ImeiUtils.formatDisplay(candidate),
                                        color = if (isSelected) GoldAccent else Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("multi_imei_cancel_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val selected = imeiList.getOrNull(selectedIndex) ?: imeiList.first()
                            val secondary = imeiList.filterIndexed { i, _ -> i != selectedIndex }.firstOrNull()
                            onSelectImei(selected, secondary)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("confirm_select_imei_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text(
                            text = "Insert Selected",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRequestContent(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FlipCameraAndroid,
            contentDescription = null,
            tint = GoldAccent,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera Permission Required",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Camera access is needed to scan handset IMEI barcodes directly from device screens or retail boxes.",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("grant_camera_permission_btn")
        ) {
            Text(
                text = "Allow Camera Access",
                color = Color(0xFF0F172A),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("dismiss_camera_permission_btn")
        ) {
            Text(
                text = "Cancel",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

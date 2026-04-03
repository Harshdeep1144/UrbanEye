package com.example.urbaneye.ui.screens.detection

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urbaneye.ui.screens.BoundingBox
import com.example.urbaneye.ui.screens.PotholeAnalyzer
import com.example.urbaneye.ui.viewmodel.DetectionViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionScreen(
    viewModel: DetectionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val density = LocalDensity.current

    // --- Permission State ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    // Request permission on launch if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Status Bar Configuration for Detection
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> = remember {
        ProcessCameraProvider.getInstance(context)
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var boundingBoxes by remember { mutableStateOf<List<BoundingBox>>(emptyList()) }
    var inferenceTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor,
                                    PotholeAnalyzer(ctx) { results: List<BoundingBox>, time: Long ->
                                        boundingBoxes = results
                                        inferenceTime = time
                                        viewModel.onPotholesDetected(results)
                                    })
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlays (Bounding Boxes)
            Canvas(modifier = Modifier.fillMaxSize()) {
                boundingBoxes.forEach { box ->
                    val left = box.x1 * size.width
                    val top = box.y1 * size.height
                    val width = (box.x2 - box.x1) * size.width
                    val height = (box.y2 - box.y1) * size.height

                    val strokeWidthPx = with(density) { 3.dp.toPx() }

                    drawRect(
                        color = Color.White,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(width = strokeWidthPx)
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val text = "${box.clsName.uppercase()} ${(box.cnf * 100).toInt()}%"
                        val paint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 36f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        drawText(text, left, top - 15f, paint)
                    }
                }
            }
        } else {
            // Placeholder when permission is denied
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Camera permission is required", color = Color.White)
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }

        // Header and Bottom Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    onClick = onNavigateBack
                ) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "DETECTION MODE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val potholeCount = boundingBoxes.count { it.clsName.contains("Pothole", ignoreCase = true) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Live Road Analytics",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                if (potholeCount == 0) "ROAD CLEAR" else "$potholeCount POTHOLES DETECTED",
                                color = if (potholeCount == 0) Color.Green else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(if (potholeCount == 0) Color.Green else Color.Red, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoLabel(label = "Latency", value = "${inferenceTime}ms")
                        InfoLabel(label = "Confidence", value = if (boundingBoxes.isEmpty()) "0%" else "${(boundingBoxes.maxOf { it.cnf } * 100).toInt()}%")
                        InfoLabel(label = "Status", value = "Scanning")
                    }
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun InfoLabel(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}
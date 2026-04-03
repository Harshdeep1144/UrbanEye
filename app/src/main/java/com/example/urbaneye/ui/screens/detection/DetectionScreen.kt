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

    // Permissions logic
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var boundingBoxes by remember { mutableStateOf<List<BoundingBox>>(emptyList()) }
    var inferenceTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
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
                                it.setAnalyzer(cameraExecutor, PotholeAnalyzer(ctx) { results, time ->
                                    boundingBoxes = results
                                    inferenceTime = time
                                    viewModel.onPotholesDetected(results)
                                })
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                        } catch (e: Exception) { e.printStackTrace() }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Bounding Box Overlays
            Canvas(modifier = Modifier.fillMaxSize()) {
                boundingBoxes.forEach { box ->
                    val left = box.x1 * size.width
                    val top = box.y1 * size.height
                    val width = (box.x2 - box.x1) * size.width
                    val height = (box.y2 - box.y1) * size.height
                    val strokeWidthPx = 3.dp.toPx()

                    drawRect(
                        color = Color.Yellow,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(width = strokeWidthPx)
                    )

                    drawContext.canvas.nativeCanvas.apply {
                        val text = "${box.clsName.uppercase()} ${(box.cnf * 100).toInt()}%"
                        val paint = Paint().apply {
                            color = android.graphics.Color.YELLOW
                            textSize = 40f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        drawText(text, left, top - 15f, paint)
                    }
                }
            }
        }

        // UI Overlay (Header and Analytics Card)
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("DETECTION LIVE", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    val count = boundingBoxes.count { it.clsName.contains("Pothole", ignoreCase = true) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(if(count > 0) Color.Red else Color.Green, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(if(count > 0) "$count POTHOLES FOUND" else "ROAD SCANNING...", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        InfoItem("Latency", "${inferenceTime}ms")
                        InfoItem("Confidence", if(boundingBoxes.isEmpty()) "0%" else "${(boundingBoxes.maxOf { it.cnf } * 100).toInt()}%")
                        InfoItem("GPS", "Active")
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}
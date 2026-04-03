package com.example.urbaneye.ui.screens

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urbaneye.ui.viewmodel.DetectionViewModel
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionScreen(
    viewModel: DetectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Urban Eye Detection") })
        }
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
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
                                    it.setAnalyzer(cameraExecutor, PotholeAnalyzer(ctx) { results: List<BoundingBox>, time: Long ->
                                        boundingBoxes = results
                                        inferenceTime = time
                                        // Notify ViewModel for potential reporting
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

                Canvas(modifier = Modifier.fillMaxSize()) {
                    boundingBoxes.forEach { box ->
                        val left = box.x1 * size.width
                        val top = box.y1 * size.height
                        val width = (box.x2 - box.x1) * size.width
                        val height = (box.y2 - box.y1) * size.height

                        drawRect(
                            color = Color.Red,
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 4.dp.toPx())
                        )

                        // Draw label and confidence
                        drawContext.canvas.nativeCanvas.apply {
                            val text = "${box.clsName} ${(box.cnf * 100).toInt()}%"
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.RED
                                textSize = 40f
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            drawText(text, left, top - 10f, paint)
                        }
                    }
                }
                
                if (boundingBoxes.isEmpty()) {
                    Text(
                        "Scanning Road...",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val potholeCount = boundingBoxes.count { it.clsName.equals("Pothole", ignoreCase = true) }
                    val condition = when {
                        potholeCount == 0 -> "Good"
                        potholeCount <= 2 -> "Moderate"
                        else -> "Critical"
                    }
                    
                    Text("Road Condition: $condition", style = MaterialTheme.typography.titleMedium)
                    Text("Potholes Detected: $potholeCount", style = MaterialTheme.typography.bodyMedium)
                    Text("Inference Time: $inferenceTime ms", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

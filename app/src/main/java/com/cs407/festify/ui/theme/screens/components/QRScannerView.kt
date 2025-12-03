package com.cs407.festify.ui.theme.screens.components

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Composable for camera preview with QR code scanning using CameraX and ML Kit
 */
@Composable
fun QRScannerView(
    modifier: Modifier = Modifier,
    onQRCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var hasScanned by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Image analysis for QR scanning
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val barcodeScanner = BarcodeScanning.getClient()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy) { barcode ->
                        if (!hasScanned) {
                            hasScanned = true
                            barcode.rawValue?.let { qrData ->
                                onQRCodeScanned(qrData)
                            }
                        }
                    }
                }

                // Camera selector
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

            } catch (e: Exception) {
                Log.e("QRScanner", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Scanning frame overlay
        ScanningFrameOverlay()
    }
}

/**
 * Process image proxy for barcode detection
 */
@androidx.camera.core.ExperimentalGetImage
private fun processImageProxy(
    barcodeScanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (Barcode) -> Unit
) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(
            image,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.let { barcode ->
                    onBarcodeDetected(barcode)
                }
            }
            .addOnFailureListener { e ->
                Log.e("QRScanner", "Barcode scanning failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } ?: imageProxy.close()
}

/**
 * Overlay with scanning frame
 */
@Composable
fun ScanningFrameOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameSize = size.minDimension * 0.6f
        val left = (size.width - frameSize) / 2
        val top = (size.height - frameSize) / 2
        val right = left + frameSize
        val bottom = top + frameSize

        // Draw corner brackets
        val cornerLength = frameSize * 0.1f
        val strokeWidth = 8f

        // Top-left corner
        drawPath(
            path = Path().apply {
                moveTo(left, top + cornerLength)
                lineTo(left, top)
                lineTo(left + cornerLength, top)
            },
            color = Color.White,
            style = Stroke(width = strokeWidth)
        )

        // Top-right corner
        drawPath(
            path = Path().apply {
                moveTo(right - cornerLength, top)
                lineTo(right, top)
                lineTo(right, top + cornerLength)
            },
            color = Color.White,
            style = Stroke(width = strokeWidth)
        )

        // Bottom-left corner
        drawPath(
            path = Path().apply {
                moveTo(left, bottom - cornerLength)
                lineTo(left, bottom)
                lineTo(left + cornerLength, bottom)
            },
            color = Color.White,
            style = Stroke(width = strokeWidth)
        )

        // Bottom-right corner
        drawPath(
            path = Path().apply {
                moveTo(right - cornerLength, bottom)
                lineTo(right, bottom)
                lineTo(right, bottom - cornerLength)
            },
            color = Color.White,
            style = Stroke(width = strokeWidth)
        )
    }
}

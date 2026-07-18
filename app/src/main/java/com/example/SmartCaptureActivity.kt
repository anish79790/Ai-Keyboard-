package com.example

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.ui.theme.MyApplicationTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay

class SmartCaptureActivity : ComponentActivity() {

    private var captureService: MediaCaptureService? = null
    private var isBound = false
    private var toolSource: String = ""

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaCaptureService.LocalBinder
            captureService = binder.getService()
            isBound = true
            Log.d("SmartCapture", "Service bound. Triggering frame capture...")
            captureFrame()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            captureService = null
            isBound = false
        }
    }

    private var fullScreenshot = mutableStateOf<Bitmap?>(null)
    private var isProcessing = mutableStateOf(false)
    private var statusMsg = mutableStateOf("Initializing display capture...")

    private val startMediaProjection = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Log.d("SmartCapture", "Permission granted. Starting Foreground Service.")
            val serviceIntent = Intent(this, MediaCaptureService::class.java).apply {
                putExtra("result_code", result.resultCode)
                putExtra("data", result.data)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            Log.e("SmartCapture", "Permission denied by user")
            Toast.makeText(this, "Screen capture permission denied.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolSource = intent.getStringExtra(EXTRA_TOOL_SOURCE) ?: ""
        
        Log.i("SmartCapture", "Launched SmartCaptureActivity for source: $toolSource")

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    val bitmap = fullScreenshot.value
                    if (bitmap == null) {
                        CaptureLoadingScreen(statusMsg.value)
                    } else {
                        SelectionOverlay(
                            bitmap = bitmap,
                            isProcessing = isProcessing.value,
                            onAreaSelected = { rect ->
                                runOcr(bitmap, rect)
                            },
                            onCancel = {
                                cleanupAndFinish()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun captureFrame() {
        val mp = captureService?.mediaProjection
        if (mp == null) {
            Log.e("SmartCapture", "MediaProjection is NULL in service")
            statusMsg.value = "Capture engine error (Null MP)"
            return
        }

        try {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            Log.d("SmartCapture", "Display metrics: ${width}x${height} @ ${density}dpi")

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            val vDisplay = mp.createVirtualDisplay(
                "ScreenGrabber", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface, null, null
            )

            reader.setOnImageAvailableListener({ ir ->
                var image: Image? = null
                try {
                    image = ir.acquireLatestImage()
                    if (image != null && fullScreenshot.value == null) {
                        val planes = image.planes
                        if (planes.isNotEmpty()) {
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            val rowPadding = rowStride - pixelStride * width

                            val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                            bitmap.copyPixelsFromBuffer(buffer)

                            val finalBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                            
                            runOnUiThread {
                                if (fullScreenshot.value == null) {
                                    fullScreenshot.value = finalBitmap
                                    Log.i("SmartCapture", "Full screenshot captured successfully.")
                                    try {
                                        ir.setOnImageAvailableListener(null, null)
                                        vDisplay?.release()
                                    } catch (e: Exception) {}
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmartCapture", "Bitmap processing failed: ${e.message}")
                } finally {
                    try { image?.close() } catch (e: Exception) {}
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.e("SmartCapture", "Capture setup exception: ${e.message}")
            statusMsg.value = "Capture setup failed: ${e.message}"
        }
    }

    private fun runOcr(source: Bitmap, rect: Rect) {
        if (isProcessing.value) return
        isProcessing.value = true
        
        Log.d("SmartCapture", "Running OCR on selection: $rect")

        val left = rect.left.toInt().coerceIn(0, source.width - 1)
        val top = rect.top.toInt().coerceIn(0, source.height - 1)
        var w = rect.width.toInt().coerceIn(1, source.width - left)
        var h = rect.height.toInt().coerceIn(1, source.height - top)

        try {
            val cropped = Bitmap.createBitmap(source, left, top, w, h)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputImage = InputImage.fromBitmap(cropped, 0)

            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = visionText.text
                    Log.i("SmartCapture", "OCR Success. Found ${result.length} characters.")
                    if (result.isNotBlank()) {
                        val intent = Intent(EXTRA_ACTION_TEXT_EXTRACTED).apply {
                            putExtra(EXTRA_EXTRACTED_TEXT, result)
                            putExtra(EXTRA_TOOL_SOURCE, toolSource)
                        }
                        sendBroadcast(intent)
                        Toast.makeText(this, "Analysing conversation context...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "No text found. Try a different area.", Toast.LENGTH_SHORT).show()
                    }
                    cleanupAndFinish()
                }
                .addOnFailureListener { e ->
                    Log.e("SmartCapture", "ML Kit OCR Failed: ${e.message}")
                    Toast.makeText(this, "OCR Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    cleanupAndFinish()
                }
        } catch (e: Exception) {
            Log.e("SmartCapture", "OCR Pipeline Crash: ${e.message}")
            isProcessing.value = false
            Toast.makeText(this, "Pipeline error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cleanupAndFinish() {
        Log.d("SmartCapture", "Cleaning up capture resources.")
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        val serviceIntent = Intent(this, MediaCaptureService::class.java)
        stopService(serviceIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) unbindService(serviceConnection)
    }

    companion object {
        const val EXTRA_ACTION_TEXT_EXTRACTED = "com.example.ACTION_TEXT_EXTRACTED"
        const val EXTRA_EXTRACTED_TEXT = "extracted_text"
        const val EXTRA_TOOL_SOURCE = "tool_source"
    }
}

@Composable
fun CaptureLoadingScreen(msg: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = Color.White)
            Text(msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SelectionOverlay(
    bitmap: Bitmap,
    isProcessing: Boolean,
    onAreaSelected: (Rect) -> Unit,
    onCancel: () -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var startPoint by remember { mutableStateOf(Offset.Zero) }
    var endPoint by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { canvasSize = it.size.toSize() }) {
        Canvas(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        startPoint = offset
                        endPoint = offset
                        isDragging = true
                    },
                    onDrag = { change, _ ->
                        endPoint = change.position
                    },
                    onDragEnd = {
                        isDragging = false
                    }
                )
            }
        ) {
            drawImage(imageBitmap)
            drawRect(color = Color.Black.copy(alpha = 0.65f))

            if (isDragging || (startPoint != Offset.Zero && endPoint != Offset.Zero)) {
                val rect = Rect(
                    left = minOf(startPoint.x, endPoint.x),
                    top = minOf(startPoint.y, endPoint.y),
                    right = maxOf(startPoint.x, endPoint.x),
                    bottom = maxOf(startPoint.y, endPoint.y)
                )

                // "Hole" in the overlay
                drawRect(
                    color = Color.Transparent,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                )
                
                drawRect(
                    color = Color(0xFF4CAF50),
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Top UI
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    "Drag to select Chat Area",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Actions
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val rect = Rect(
                        left = minOf(startPoint.x, endPoint.x),
                        top = minOf(startPoint.y, endPoint.y),
                        right = maxOf(startPoint.x, endPoint.x),
                        bottom = maxOf(startPoint.y, endPoint.y)
                    )
                    onAreaSelected(rect)
                },
                enabled = !isProcessing && startPoint != Offset.Zero && startPoint != endPoint,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Extract Context")
                }
            }
        }
    }
}

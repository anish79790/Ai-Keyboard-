package com.example

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

class FloatingAssistantService : Service() {

    private lateinit var windowManager: WindowManager
    private var params: WindowManager.LayoutParams? = null
    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    private fun showOverlay() {
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        val lifecycleOwner = MyLifecycleOwner()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            
            setContent {
                MaterialTheme {
                    FloatingAssistantUI(
                        onMove = { dx, dy ->
                            params?.let {
                                it.x += dx.roundToInt()
                                it.y += dy.roundToInt()
                                windowManager.updateViewLayout(this@apply, it)
                            }
                        },
                        onSelect = { suggestion ->
                           // Broadast the intent to keyboard to insert
                           val intent = Intent("com.example.INSERT_TEXT").apply {
                               putExtra("text", suggestion)
                               setPackage(packageName)
                           }
                           sendBroadcast(intent)
                        }
                    )
                }
            }
        }

        windowManager.addView(composeView, params)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        composeView?.let { windowManager.removeView(it) }
    }
}

@Composable
fun FloatingAssistantUI(onMove: (Float, Float) -> Unit, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val messages by ConvoContextStore.latestMessages.collectAsState()
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            val lastMsg = messages.last()
            // Pull suggestions from our existing engine logic
            val results = getFallbackSuggestions(context, "flirty", lastMsg, true, messages)
            suggestions = results.take(3)
        }
    }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onMove(dragAmount.x, dragAmount.y)
            }
        }
    ) {
        if (expanded) {
            Surface(
                modifier = Modifier
                    .width(200.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 12.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SOHO Assistant ✨",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { expanded = false }, modifier = Modifier.size(24.dp)) {
                            Text("×", color = Color.Gray, fontSize = 20.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (suggestions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        suggestions.forEach { sug ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelect(sug) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    sug,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${messages.size} msgs captured from ${ConvoContextStore.currentAppPackage?.substringAfterLast(".") ?: "App"}",
                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable { expanded = true },
                contentAlignment = Alignment.Center
            ) {
                Text("💬", fontSize = 24.sp)
                if (messages.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(Color.Red, CircleShape)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Minimal LifecycleOwner for Compose in Service
class MyLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, androidx.savedstate.SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = androidx.savedstate.SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = store
    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
    fun performRestore(state: android.os.Bundle?) = savedStateRegistryController.performRestore(state)
}

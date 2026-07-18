package com.example

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class AiKeyboardService : InputMethodService() {
    private val lifecycleOwner = IMEComposeLifecycleOwner()

    // Thread-safe dynamic state for Compose to react to editor focus and text changes
    private val currentTextState = androidx.compose.runtime.mutableStateOf("")
    private val activeInputConnection = androidx.compose.runtime.mutableStateOf<InputConnection?>(null)
    private val isSensitiveState = androidx.compose.runtime.mutableStateOf(false)

    // HEALTH MONITORING
    private var commitSuccessCount = 0
    private var commitFailureCount = 0
    private var lastHealthLogTime = 0L

    private var clipboardManager: android.content.ClipboardManager? = null
    private var clipboardListener: android.content.ClipboardManager.OnPrimaryClipChangedListener? = null

    // Broadcast receiver for Smart Capture text
    private var smartCaptureReceiver: android.content.BroadcastReceiver? = null
    // Hold latest captured text for UI to consume
    val latestCapturedText = androidx.compose.runtime.mutableStateOf("")
    val latestCaptureSource = androidx.compose.runtime.mutableStateOf("")

    private var suggestionInsertReceiver: android.content.BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        suggestionInsertReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == "com.example.INSERT_TEXT") {
                    val text = intent.getStringExtra("text")
                    if (!text.isNullOrBlank()) {
                        activeInputConnection.value?.commitText(text, 1)
                    }
                }
            }
        }
        val insertFilter = android.content.IntentFilter("com.example.INSERT_TEXT")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(suggestionInsertReceiver, insertFilter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(suggestionInsertReceiver, insertFilter)
        }

        smartCaptureReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == "com.example.ACTION_TEXT_EXTRACTED") {
                    val text = intent.getStringExtra("extracted_text")
                    val source = intent.getStringExtra("tool_source")
                    if (!text.isNullOrBlank()) {
                        latestCapturedText.value = text
                        latestCaptureSource.value = source ?: ""
                    }
                }
            }
        }
        val filter = android.content.IntentFilter("com.example.ACTION_TEXT_EXTRACTED")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smartCaptureReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(smartCaptureReceiver, filter)
        }
        
        Thread {
            try {
                val db = KeyboardDatabase.getInstance(applicationContext)
                KeyboardGlobals.predictionEngine.setPredictionDao(db.predictionDao())
                
                kotlinx.coroutines.runBlocking {
                    KeyboardGlobals.predictionEngine.loadLearnedWords()
                }

                KeyboardSoundEngine.init(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

        try {
            clipboardManager = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            clipboardListener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
                try {
                    val clip = clipboardManager?.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0)?.text?.toString()
                        if (!text.isNullOrBlank()) {
                            ContextStore.addClipboardItem(this@AiKeyboardService, text)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace() // Satisfies AppOps background restricted clipboard access safety
                }
            }
            clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateInputView(): View {
        // CRITICAL FIX: To prevent the "ViewTreeLifecycleOwner not found" crash,
        // we must set the lifecycle, viewmodel, and saved-state registry owners 
        // on the Window's Decor View (the root window context).
        window?.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(lifecycleOwner)
            decor.setViewTreeViewModelStoreOwner(lifecycleOwner)
            decor.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        val view = ComposeView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                AiKeyboardView(
                    inputConnection = activeInputConnection.value,
                    extractedText = currentTextState.value,
                    isSensitive = isSensitiveState.value,
                    onKeyClick = { text ->
                        val conn = activeInputConnection.value ?: currentInputConnection
                        if (conn != null) {
                            val success = conn.commitText(text, 1)
                            if (success) {
                                commitSuccessCount++
                            } else {
                                commitFailureCount++
                                android.util.Log.e("AiKeyboardHealth", "CRITICAL: FAILED commitText for key: '$text'. Total failures: $commitFailureCount")
                            }
                            
                            val now = System.currentTimeMillis()
                            if (now - lastHealthLogTime > 60000) { // Log every minute if active
                                lastHealthLogTime = now
                                android.util.Log.i("AiKeyboardHealth", "STATS: Success=$commitSuccessCount, Failures=$commitFailureCount, IC_Valid=${currentInputConnection != null}")
                            }
                        } else {
                            commitFailureCount++
                            android.util.Log.e("AiKeyboardHealth", "CRITICAL: NO InputConnection for key: '$text'")
                        }
                    },
                    onDeleteClick = {
                        val conn = activeInputConnection.value ?: currentInputConnection
                        if (conn != null) {
                            // PERFORMANCE OPTIMIZED DELETE: Use deleteSurroundingText for better IME stability
                            val selection = conn.getSelectedText(0)
                            if (selection != null && selection.isNotEmpty()) {
                                conn.commitText("", 1)
                            } else {
                                conn.deleteSurroundingText(1, 0)
                            }
                        } else {
                             android.util.Log.e("AiKeyboard", "NO InputConnection for DELETE")
                        }
                    },
                    onActionClick = {
                        val text = currentTextState.value
                        if (text.isNotBlank()) {
                            ContextStore.addMessage(this@AiKeyboardService, text)
                        }

                        val conn = activeInputConnection.value ?: currentInputConnection
                        val imeOptions = currentInputEditorInfo?.imeOptions ?: 0
                        val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
                        if (actionId != 0 && actionId != EditorInfo.IME_ACTION_NONE) {
                            conn?.performEditorAction(actionId)
                        } else {
                            // Fallback newline
                            conn?.commitText("\n", 1)
                        }
                    },
                    onTextChanged = { newText ->
                        currentTextState.value = newText
                    },
                    onAiAction = { /* Handled fully in Compose suggestions flow */ },
                    onStartSmartCapture = { source ->
                        try {
                            val intent = android.content.Intent(this@AiKeyboardService, SmartCaptureActivity::class.java).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                putExtra(SmartCaptureActivity.EXTRA_TOOL_SOURCE, source)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("AiKeyboard", "Failed to start capture activity: ${e.message}")
                        }
                    },
                    smartCapturedText = latestCapturedText.value,
                    smartCaptureSource = latestCaptureSource.value,
                    getHealthStats = {
                        mapOf(
                            "Commits" to commitSuccessCount.toString(),
                            "Failures" to commitFailureCount.toString(),
                            "Memory" to getMemoryInfo(),
                            "IC State" to if (currentInputConnection != null) "Active" else "Null"
                        )
                    }
                )
            }
        }
        return view
    }

    private fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val max = runtime.maxMemory() / 1024 / 1024
        return "$used MB / $max MB"
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        // Dynamic re-association of input connection and active text
        activeInputConnection.value = currentInputConnection
        currentTextState.value = getCurrentText()

        // Privacy hardening: Check if we are in a sensitive field
        val inputType = info?.inputType ?: 0
        val isPassword = (inputType and EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT &&
                         ((inputType and EditorInfo.TYPE_MASK_VARIATION) == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                          (inputType and EditorInfo.TYPE_MASK_VARIATION) == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                          (inputType and EditorInfo.TYPE_MASK_VARIATION) == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        
        val noLearning = (info?.imeOptions ?: 0) and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
        
        isSensitiveState.value = isPassword || noLearning
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        
        activeInputConnection.value = null
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // Ensure standard text extraction is triggered on selection changes
        currentTextState.value = getCurrentText()
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false // Keeps the keyboard from expanding fullscreen in landscape
    }

    override fun onDestroy() {
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        try {
            if (suggestionInsertReceiver != null) {
                unregisterReceiver(suggestionInsertReceiver)
                suggestionInsertReceiver = null
            }
        } catch (e: Exception) {}
        try {
            if (smartCaptureReceiver != null) {
                unregisterReceiver(smartCaptureReceiver)
                smartCaptureReceiver = null
            }
        } catch (e: Exception) {}
        try {
            clipboardListener?.let {
                clipboardManager?.removePrimaryClipChangedListener(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    private fun getCurrentText(): String {
        return try {
            val extractedText = currentInputConnection?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
            extractedText?.text?.toString() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

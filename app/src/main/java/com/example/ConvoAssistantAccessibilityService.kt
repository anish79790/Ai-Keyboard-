package com.example

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.mutableStateOf

object ConvoContextStore {
    val latestMessages = MutableStateFlow<List<String>>(emptyList())
    var currentAppPackage: String? = null
    var floatingAssistantEnabled = mutableStateOf(false)
    
    fun updateHistory(messages: List<String>) {
        if (messages != latestMessages.value) {
            latestMessages.value = messages
        }
    }

    fun toggleFloatingAssistant(context: android.content.Context) {
        floatingAssistantEnabled.value = !floatingAssistantEnabled.value
        if (floatingAssistantEnabled.value) {
            context.startService(android.content.Intent(context, FloatingAssistantService::class.java))
        } else {
            context.stopService(android.content.Intent(context, FloatingAssistantService::class.java))
        }
    }
}

class ConvoAssistantAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == null) return
        
        ConvoContextStore.currentAppPackage = event.packageName.toString()

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                extractRecentMessages()
            }
        }
    }

    private fun extractRecentMessages() {
        val rootNode = rootInActiveWindow ?: return
        val messages = mutableListOf<String>()
        
        // Target common chat apps message list structures
        findMessageTexts(rootNode, messages)
        
        if (messages.isNotEmpty()) {
            // Take the last 15 messages for better context
            val recent = messages.takeLast(15)
            ConvoContextStore.updateHistory(recent)
            
            // PASS TO MOMENTUM ENGINE
            val analysis = LocalSmartConvoEngine.analyzeConversation(
                context = this,
                history = recent,
                currentTypedText = "",
                suggestorMode = "Friendly"
            )
            Log.d("ConvoAssistant", "Momentum: ${analysis.momentumScore}, Vibe: ${analysis.detectedTone}, Dry: ${analysis.isDryChat}")
        }
    }

    private fun findMessageTexts(node: AccessibilityNodeInfo, list: MutableList<String>) {
        if (node.childCount == 0) {
            val text = node.text?.toString() ?: return
            val className = node.className?.toString() ?: ""
            
            // Heuristic for chat messages:
            // 1. Not a system button (usually < 100 chars, not just "OK", etc)
            // 2. Class is likely a TextView
            // 3. Not a numeric timestamp
            if (text.length > 1 && (className.contains("TextView") || className.contains("Button"))) {
                val isTimestamp = text.contains(Regex("^\\d{1,2}:\\d{2}"))
                val isSystem = isUiElement(text)
                if (!isTimestamp && !isSystem) {
                    list.add(text)
                }
            }
            return
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findMessageTexts(child, list)
                child.recycle()
            }
        }
    }

    private fun isUiElement(text: String): Boolean {
        val uiTokens = listOf("reply", "message", "type", "send", "camera", "voice", "call", "video")
        return uiTokens.any { text.lowercase().contains(it) }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

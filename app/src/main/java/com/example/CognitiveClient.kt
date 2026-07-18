package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.*

/**
 * Thread-safe client connection gatekeeper to wrap CognitiveService interactions.
 * Connects asynchronously and guarantees complete isolation from the main typing loop.
 * Safeguards rendering performance by adopting immediate local fail-safes if the service is unbound.
 */
class CognitiveClient private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: CognitiveClient? = null

        fun getInstance(context: Context): CognitiveClient {
            return INSTANCE ?: synchronized(this) {
                val instance = CognitiveClient(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val clientScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var cognitiveService: CognitiveService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val serviceBinder = binder as? CognitiveService.CognitiveBinder
            if (serviceBinder != null) {
                cognitiveService = serviceBinder.getService()
                isBound = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cognitiveService = null
            isBound = false
        }
    }

    init {
        bindToCognitiveService()
    }

    private fun bindToCognitiveService() {
        try {
            val intent = Intent(context, CognitiveService::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Processes requests. Invokes on-service async calculation, or immediately uses
     * zero-delay in-process calculation to avoid lagging the UI if service is not ready.
     */
    fun processSmartThought(
        rawText: String,
        selectedTone: String,
        onComplete: (List<String>) -> Unit
    ) {
        val activeService = cognitiveService
        if (isBound && activeService != null) {
            activeService.processCognitiveThought(context, rawText, selectedTone, onComplete)
        } else {
            // Lazy fallback: trigger in-process isolated calculation
            clientScope.launch(Dispatchers.Default) {
                val slangReplacement = LocalAiEngine().quickRewrite(rawText, selectedTone)
                val results = mutableListOf<String>()
                results.add(slangReplacement)
                
                val directReplies = LocalAiEngine().getSmartReply(rawText)
                if (directReplies != null) {
                    results.addAll(directReplies)
                }
                
                withContext(Dispatchers.Main) {
                    onComplete(results.take(4))
                }
            }
        }
    }

    fun unbind() {
        if (isBound) {
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isBound = false
            cognitiveService = null
        }
    }
}

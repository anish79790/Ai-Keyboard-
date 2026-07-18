package com.example

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.KeyEvent
import android.view.SoundEffectConstants
import android.view.inputmethod.InputConnection
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.*
import kotlin.coroutines.*

object TypingSpeedTracker {
    var isFastTyping by mutableStateOf(false)
    private var lastKeyTime = 0L
    private var fastKeyCount = 0

    fun recordKey() {
        val now = System.currentTimeMillis()
        val diff = now - lastKeyTime
        if (lastKeyTime > 0 && diff < 400) {
            fastKeyCount++
            if (fastKeyCount > 2) {
                if (!isFastTyping) isFastTyping = true
            }
        } else {
            fastKeyCount = 0
            if (isFastTyping) isFastTyping = false
        }
        lastKeyTime = now
    }

    fun tickReset() {
        if (System.currentTimeMillis() - lastKeyTime > 1200) {
            if (isFastTyping) {
                isFastTyping = false
                fastKeyCount = 0
            }
        }
    }
}

object AiBackgroundSuggestor {
    private var lastAiFetchJob: Job? = null

    fun fetchBackgroundSuggestions(
        context: android.content.Context,
        input: String,
        mode: String,
        isHinglish: Boolean,
        onResult: (List<String>) -> Unit,
        coroutineScope: CoroutineScope
    ) {
        lastAiFetchJob?.cancel()
        lastAiFetchJob = coroutineScope.launch {
            val history = ContextStore.getActiveConvoTexts(context)
            val contextHistory = if (history.isEmpty()) "Start of chat" else history.joinToString("\n")
            
            val languageInstruction = if (isHinglish) {
                "Generate ENTIRELY in Hinglish (Hindi words in English letters, e.g. 'Kaise ho?', 'Kya haal hai?'). No Devanagari."
            } else {
                "Generate natural, casual conversational English."
            }

            val promptText = """
                Task: Advanced Conversation Continuity Engine. 
                Current Input: "$input"
                History: $contextHistory
                Mode: $mode
                
                Goal: Generate 3 DIFFERENT conversational directions to evolve the chat. 
                Avoid repeating same structures or flirty clichés. 
                
                Diversity Pattern:
                - Option 1: Playful teasing or funny observation.
                - Option 2: Thought-provoking question or sincere curiosity.
                - Option 3: Natural topic shift (Life, Work, Music, Future plans, etc).
                
                Return ONLY 3 raw lines. $languageInstruction
            """.trimIndent()

            try {
                val response = generateAiResponse(context, promptText)
                if (response != null && !response.startsWith("Error:")) {
                    val lines = response.lines()
                        .map { it.replace(Regex("^[-*•0-9.\\s]+"), "").trim().removeSurrounding("\"") }
                        .filter { it.isNotBlank() && it.length > 3 }
                        .take(3)
                    if (lines.isNotEmpty()) {
                        val history = ContextStore.getActiveConvoTexts(context).map { it.lowercase() }
                        val filteredLines = lines.filter { line ->
                            val lineLower = line.lowercase()
                            !history.any { hist -> hist.contains(lineLower) || lineLower.contains(hist) }
                        }
                        if (filteredLines.isNotEmpty()) {
                            onResult(filteredLines)
                        } else {
                            onResult(lines) // Fallback if all were somehow filtered (unlikely)
                        }
                    }
                }
            } catch (e: Exception) { }
        }
    }
}

val LocalIsFastTyping = compositionLocalOf { false }

val LocalKeyHeight = compositionLocalOf { 50.dp }
val LocalTextFontSizeMultiplier = compositionLocalOf { 1.0f }
val LocalHapticLevelSetting = compositionLocalOf { "Medium" }
val LocalSoundEnabledSetting = compositionLocalOf { true }
val LocalSoundProfileSetting = compositionLocalOf { "Mechanical" }
val LocalKeySoundOverridesSetting = compositionLocalOf { emptyMap<String, String>() }
val LocalKeyRemappingsSetting = compositionLocalOf { emptyMap<String, String>() }
val LocalKeycapColorsSetting = compositionLocalOf { emptyMap<String, String>() }
val LocalKeyboardAnimationSetting = compositionLocalOf { "Standard Mechanical" }
val LocalDebounceIntervalSetting = compositionLocalOf { 10L }
val LocalKeyHeightScaleSetting = compositionLocalOf { 1.0f }
val LocalKeyWidthMultiSetting = compositionLocalOf { 1.0f }
val LocalKeyShapeSetting = compositionLocalOf { "Rounded" }
val LocalLightingEffectSetting = compositionLocalOf { "None" }
val LocalKeyStyleSetting = compositionLocalOf { "Mechanical" }
val LocalKeyboardSizeSetting = compositionLocalOf { "Normal" }
val LocalKeyboardLayoutMode = compositionLocalOf { "Standard" }

val String.rowSpacing: Dp
    get() = when (this) {
        "UltraCompact" -> 1.5.dp
        "Compact" -> 2.5.dp
        else -> 4.dp
    }

val String.keyboardPaddingVertical: Dp
    get() = when (this) {
        "UltraCompact" -> 1.dp
        "Compact" -> 2.5.dp
        else -> 4.dp
    }

val String.keyboardPaddingHorizontal: Dp
    get() = when (this) {
        "UltraCompact" -> 2.dp
        "Compact" -> 3.dp
        else -> 4.dp
    }

data class SharedAnimationState(
    val rgbHue: State<Float>,
    val neonAlpha: State<Float>
)
val LocalSharedAnimationValues = staticCompositionLocalOf<SharedAnimationState?> { null }

enum class KeyboardType { ALPHABET, SYMBOLS, ALT_SYMBOLS, EMOJI }
enum class ShiftState { LOWERCASE, SHIFTED, CAPSLOCK }

sealed class SuggestionsState {
    object Idle : SuggestionsState()
    object Loading : SuggestionsState()
    data class Success(val list: List<String>) : SuggestionsState()
    data class Error(val message: String) : SuggestionsState()
    object BotScreen : SuggestionsState()
    object SettingsScreen : SuggestionsState()
    object LibraryV3ProScreen : SuggestionsState()
    object TextEditingScreen : SuggestionsState()
    object ThemeScreen : SuggestionsState()
    object KeyboardSizeSettingScreen : SuggestionsState()
    object PassScreen : SuggestionsState()
    object ExtractTextScreen : SuggestionsState()
    object ModesScreen : SuggestionsState()
    object VoiceTranslateScreen : SuggestionsState()
    object TranslatePanelScreen : SuggestionsState()
    object ManageAccessScreen : SuggestionsState()
    object MechanicalKeysScreen : SuggestionsState()
    object SoundManagerScreen : SuggestionsState()
    object AiSelectScreen : SuggestionsState()
    object ToolsGridScreen : SuggestionsState()
    object ClipboardScreen : SuggestionsState()
    object AutoReplierScreen : SuggestionsState()
    object CustomizerStudioScreen : SuggestionsState()
    object KeyShapeScreen : SuggestionsState()
    object LightingFxScreen : SuggestionsState()
    object AnimationsScreen : SuggestionsState()
    object TextCaseScreen : SuggestionsState()
    object AsciiArtScreen : SuggestionsState()
    object FontGenScreen : SuggestionsState()
    object EmojiSearchScreen : SuggestionsState()
    object HistorySearchScreen : SuggestionsState()
    object UnitConverterScreen : SuggestionsState()
    object AppLauncherScreen : SuggestionsState()
    
    // Missing screens added
    object AutoCorrectManagerScreen : SuggestionsState()
    object PassGenScreen : SuggestionsState()
    object MathSolverScreen : SuggestionsState()
    object KaomojiScreen : SuggestionsState()
    object SymbolsLibraryScreen : SuggestionsState()
    object SpeedStatsScreen : SuggestionsState()
    object QuickNotesScreen : SuggestionsState()
    object EmojiCombinerScreen : SuggestionsState()
    object DictionaryScreen : SuggestionsState()
    object FontsScreen : SuggestionsState()
    object DiagnosticsScreen : SuggestionsState()
    object DiagnosticCoreScreen : SuggestionsState()
    object UserGuideScreen : SuggestionsState()
    object AdvancedCustomizationScreen : SuggestionsState()
    object OnelinerWorkspace : SuggestionsState()
    object SmartCaptureMode : SuggestionsState()
    object ConversationBuilderScreen : SuggestionsState()
}

@Composable
fun Modifier.consumeClicks(): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = { }
)

@Composable
fun rememberProjectImageBitmap(uriString: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(uriString) {
        if (uriString.isBlank()) return@remember null
        try {
            val inputStream = if (uriString.startsWith("/")) {
                java.io.FileInputStream(java.io.File(uriString))
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriString))
            }
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.asImageBitmap()
        } catch (e: Exception) { null }
    }
}

// Global scope logic to handle suggestions cleanly from Compose
fun fetchSuggestions(
    context: android.content.Context,
    mode: String,
    targetLanguage: String,
    inputText: String,
    onStateChange: (SuggestionsState) -> Unit,
    onModeChange: (String) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    var cleanInput = inputText
    var isScreenOcr = false
    if (inputText.startsWith("Context from screen:")) {
        cleanInput = inputText.removePrefix("Context from screen:").trim()
        isScreenOcr = true
    }

    if (!isScreenOcr && (mode == "grammar" || mode == "genz") && cleanInput.isBlank()) {
        val displayMsg = if (targetLanguage == "Hinglish") {
            "Pehle thoda text likho tabhi toh AI use convert ya correct karega!"
        } else {
            "Please type some text first to let AI process or convert it!"
        }
        onStateChange(SuggestionsState.Error(displayMsg))
        return
    }

    onStateChange(SuggestionsState.Loading)
    onModeChange(mode)

    val history = ContextStore.getHistory(context)
    val contextHistory = if (history.isEmpty()) {
        "None (this is the start of the chat)"
    } else {
        history.joinToString("\n") { "Message: $it" }
    }

    // Language guidelines for prompting Gemini
    val isHinglish = targetLanguage == "Hinglish"
    val languageInstruction = if (isHinglish) {
        "You MUST generate suggestions ENTIRELY in Hinglish (Hindi/Urdu words written using Latin/English letters, e.g., 'Yar mai to reply kr rha tha, suno na' or 'chalo fir some interesting gossip batao'). No Devanagari. Tone: Human, modern, confident, emotionally intelligent. Mix realistically like a young adult in Delhi/Mumbai."
    } else {
        "You MUST generate suggestions in socially intelligent, modern conversational casual English. Tone: Confident, human-like, witty. Avoid bot-speak. Use texting patterns like 'fr', 'no cap', and varied sentence lengths. No Devanagari."
    }

    val prompt = if (isScreenOcr) {
        """
        You are a highly intelligent social interaction assistant. 
        Context: "$cleanInput"
        History: $contextHistory
        
        Generate exactly 3 short, natural, distinctive next moves for the user. 
        DO NOT repeat themes. If the chat is flirty, mix in one curious and one random funny line.
        
        Directions to rotate:
        - Humorous Banter / Roasts
        - Deep/Playful Questions
        - Storytelling hooks ("Pata h aaj kya hua...")
        - Relatable life observations
        
        Return ONLY 3 clean lines. $languageInstruction
        """.trimIndent()
    } else {
        when (mode) {
            "grammar" -> {
                val base = "Rewrite the following with perfect grammar and punctuation, improving flow while keeping it natural: \"$cleanInput\"."
                val specific = if (isHinglish) "Use Roman Hinglish. Return ONLY 3 unique, better versions." else "Use casual English. Return ONLY 3 unique, better versions."
                "$base $specific"
            }
            "genz" -> {
                val base = "Rewrite in natural Gen Z slang: \"$cleanInput\"."
                "$base $languageInstruction Return ONLY 3 variations."
            }
            "friendly" -> "Suggest 3 unique friendly ways to reply to: \"$cleanInput\". History: $contextHistory. Mix a question, a comment, and a fun detail. $languageInstruction"
            "flirty" -> "Suggest 3 distinct flirty directions for: \"$cleanInput\". History: $contextHistory. 1 Teasing, 1 Curious, 1 Playful. AVOID CLICHÉS. $languageInstruction"
            "roast" -> "3 witty roasts for: \"$cleanInput\". History: $contextHistory. $languageInstruction"
            "love_guru" -> "Love Guru wisdom for: \"$cleanInput\". History: $contextHistory. $languageInstruction Provide 3 variations."
            "angry" -> "3 cold/dismissive replies to: \"$cleanInput\". History: $contextHistory. $languageInstruction"
            else -> ""
        }
    }

    coroutineScope.launch {
        try {
            val response = generateAiResponse(context, prompt)
            if (response != null && !response.startsWith("Error:")) {
                val suggestions = response.lines()
                    .map { it.replace(Regex("^[-*•0-9.\\s]+"), "").trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
                    .take(3)
                if (suggestions.isNotEmpty()) {
                    onStateChange(SuggestionsState.Success(suggestions))
                } else {
                    // Fallback to local
                    val localRecs = LocalSmartConvoEngine.analyzeConversation(
                        context, emptyList(), cleanInput, if (isScreenOcr) "friendly" else mode, isHinglish
                    ).recommendations.take(3)
                    onStateChange(SuggestionsState.Success(localRecs))
                }
            } else {
                // Fallback to local instead of showing an error screen
                val localRecs = LocalSmartConvoEngine.analyzeConversation(
                    context, emptyList(), cleanInput, if (isScreenOcr) "friendly" else mode, isHinglish
                ).recommendations.take(3)
                onStateChange(SuggestionsState.Success(localRecs))
            }
        } catch (e: Exception) {
            android.util.Log.e("SmartCapture", "Error executing AI model generation", e)
            val localRecs = LocalSmartConvoEngine.analyzeConversation(
                context, emptyList(), cleanInput, if (isScreenOcr) "friendly" else mode, isHinglish
            ).recommendations.take(3)
            onStateChange(SuggestionsState.Success(localRecs))
        }
    }
}

fun fetchAiFix(
    context: android.content.Context,
    text: String,
    onResult: (String) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    coroutineScope.launch {
        val prompt = "Correct: \"$text\". ONLY corrected text."
        val corrected = try {
            generateAiResponse(context, prompt)?.trim() ?: text
        } catch (e: Exception) {
            text
        }
        onResult(corrected)
    }
}

fun fetchToneReply(
    context: android.content.Context,
    tone: String,
    onStateChange: (SuggestionsState) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    onStateChange(SuggestionsState.Loading)
    coroutineScope.launch {
        val history = ContextStore.getHistory(context)
        val lastMessage = history.lastOrNull() ?: "Hello!"
        val prompt = "Generate 3 $tone style Hinglish replies to: \"$lastMessage\". ONLY 3 lines."
        val responseText = try {
            generateAiResponse(context, prompt) ?: ""
        } catch (e: Exception) {
            ""
        }
        val lines = responseText.lines().filter { it.isNotBlank() }.take(3)
        if (lines.isNotEmpty()) onStateChange(SuggestionsState.Success(lines))
        else onStateChange(SuggestionsState.Error("No results"))
    }
}

fun commitWordSuggestion(inputConnection: android.view.inputmethod.InputConnection?, currentText: String, suggestion: String) {
    if (inputConnection == null) return
    val trimmed = currentText.trimEnd()
    val lastWord = trimmed.split(Regex("\\s+")).lastOrNull() ?: ""
    if (lastWord.isNotEmpty() && suggestion.startsWith(lastWord, ignoreCase = true)) {
        inputConnection.deleteSurroundingText(lastWord.length, 0)
    }
    inputConnection.commitText("$suggestion ", 1)
}

@Composable
fun KeyboardScreenHeader(
    title: String,
    emoji: String = "",
    onCloseClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (emoji.isNotEmpty()) Text(emoji, fontSize = 18.sp)
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onCloseClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun SmallUtilityButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ToolShortcutChip(
    icon: String,
    label: String,
    isLandscape: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(if (isLandscape) 6.dp else 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .height(if (isLandscape) 20.dp else 32.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isLandscape) 4.dp else 6.dp, vertical = if (isLandscape) 1.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(icon, fontSize = if (isLandscape) 9.sp else 12.sp)
            Text(
                text = label,
                fontSize = if (isLandscape) 8.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    pressedOverride: Boolean? = null,
    onLongClick: (() -> Unit)? = null,
    onAlternateClick: ((String) -> Unit)? = null,
    isRepeatable: Boolean = false,
    onClick: () -> Unit
) {
    // Read composition locals
    val soundProfile = LocalSoundProfileSetting.current
    val soundOverrides = LocalKeySoundOverridesSetting.current
    val soundEnabled = LocalSoundEnabledSetting.current
    val remappings = LocalKeyRemappingsSetting.current
    val keyColors = LocalKeycapColorsSetting.current
    val animationStyle = LocalKeyboardAnimationSetting.current
    val debounceMs = LocalDebounceIntervalSetting.current
    val keyShapeSetting = LocalKeyShapeSetting.current
    val lightingEffect = LocalLightingEffectSetting.current
    val keyStyleSetting = LocalKeyStyleSetting.current
    val sharedAnims = LocalSharedAnimationValues.current
    val keyShapeSpec = getKeyShapeStyle(keyShapeSetting)

    val cleanKey = text.lowercase()
    val view = LocalView.current
    var lastClickTime by remember { mutableStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = pressedOverride ?: interactionSource.collectIsPressedAsState().value
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    val symbolsMap = SymbolDataSource.longPressMapping
    val alternatives = symbolsMap[cleanKey] ?: emptyList()
    var showAlternatives by remember { mutableStateOf(false) }

    val playKeySoundAndClick = {
        TypingSpeedTracker.recordKey()
        if (soundEnabled) {
            val targetSound = soundOverrides[cleanKey] ?: soundProfile
            KeyboardSoundEngine.playSound(targetSound, cleanKey)
        } else {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
        onClick()
    }
    
    // Auto-repeat logic for repeatable keys (like Backspace)
    LaunchedEffect(isPressed) {
        if (isPressed && isRepeatable) {
            var currentDelay = 400L
            while (isPressed) {
                playKeySoundAndClick()
                delay(currentDelay)
                if (currentDelay > 50L) {
                    currentDelay = (currentDelay * 0.82f).toLong().coerceAtLeast(50L)
                }
            }
        }
    }
    
    // Determine remapped display label
    val displayLabel = remappings[cleanKey]?.let {
        if (text.any { it.isUpperCase() }) it.uppercase() else it
    } ?: text

    // Determine custom color override
    val isBackspace = text == "⌫" || text.uppercase() == "[BACKSPACE]" || text == "[DEL]"
    val customColorHex = keyColors[cleanKey]
    val finalContainerColor = if (isBackspace) {
        Color(0xFFE53935)
    } else if (customColorHex != null && customColorHex.isNotBlank()) {
        try { Color(android.graphics.Color.parseColor(customColorHex)) } catch (e: Exception) { containerColor }
    } else {
        containerColor
    }
    
    val finalContentColor = if (isBackspace) {
        Color.White
    } else {
        contentColor
    }

    // Compute dynamic spring values from chosen animation profiles
    val animDamping = when (animationStyle) {
        "Bouncy Spring" -> 0.28f
        "Squishy Jelly" -> 0.40f
        "Linear Pop" -> 0.70f
        else -> 0.48f // Standard Mechanical
    }
    
    val animStiffness = when (animationStyle) {
        "Bouncy Spring" -> 450f
        "Squishy Jelly" -> 650f
        "Linear Pop" -> 1600f
        else -> 1200f // Standard Mechanical
    }

    val targetTravel = if (isPressed) {
        if (TypingSpeedTracker.isFastTyping) {
            0.dp
        } else {
            when (animationStyle) {
                "Linear Pop" -> (-2).dp // lifts up slightly
                "Squishy Jelly" -> 5.dp
                "Bouncy Spring" -> 5.dp
                else -> 4.dp // Standard Mechanical
            }
        }
    } else {
        0.dp
    }

    val targetScale = if (isPressed) {
        if (TypingSpeedTracker.isFastTyping) {
            1.0f
        } else {
            when (animationStyle) {
                "Linear Pop" -> 1.15f // pops out
                "Squishy Jelly" -> 0.81f // squishes flat
                "Bouncy Spring" -> 0.90f // bounces tight
                else -> 0.94f // Standard Mechanical
            }
        }
    } else {
        1.0f
    }

    val isFast = LocalIsFastTyping.current
    
    // Smooth low-latency animation curves (instant/mechanical feeling)
    val animSpec = remember(animationStyle, isPressed, isFast) {
        if (isFast) {
            snap<Dp>()
        } else {
            when (animationStyle) {
                "Bouncy Spring" -> spring<Dp>(dampingRatio = 0.28f, stiffness = 450f)
                "Squishy Jelly" -> spring<Dp>(dampingRatio = 0.40f, stiffness = 650f)
                "Linear Pop" -> tween<Dp>(durationMillis = 35, easing = LinearOutSlowInEasing)
                else -> tween<Dp>(durationMillis = 42, easing = LinearOutSlowInEasing) // Snappy mechanical
            }
        }
    }

    val animSpecFloat = remember(animationStyle, isPressed, isFast) {
        if (isFast) {
            snap<Float>()
        } else {
            when (animationStyle) {
                "Bouncy Spring" -> spring<Float>(dampingRatio = 0.28f, stiffness = 450f)
                "Squishy Jelly" -> spring<Float>(dampingRatio = 0.40f, stiffness = 650f)
                "Linear Pop" -> tween<Float>(durationMillis = 35, easing = LinearOutSlowInEasing)
                else -> tween<Float>(durationMillis = 42, easing = LinearOutSlowInEasing) // Snappy mechanical
            }
        }
    }

    val travelY by animateDpAsState(
        targetValue = targetTravel,
        animationSpec = animSpec,
        label = "MechanicalTravel"
    )

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = animSpecFloat,
        label = "MechanicalSquish"
    )

    // Base switch bezel color
    val bezelColor = if (finalContainerColor == MaterialTheme.colorScheme.surface) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        finalContainerColor.copy(alpha = 0.5f)
    }

    val bezelBackgroundModifier = if (lightingEffect != "None" && sharedAnims != null) {
        Modifier.drawBehind {
            if (isFast) {
                drawRect(bezelColor)
                return@drawBehind
            }
            val color = when (lightingEffect) {
                "RGB Wave" -> Color.hsv(sharedAnims.rgbHue.value, 0.85f, 0.95f)
                "Neon Glow" -> Color(0xFFE040FB).copy(alpha = sharedAnims.neonAlpha.value)
                "White Highlight" -> Color.White.copy(alpha = 0.5f)
                "KeyPress Glow" -> if (isPressed) Color(0xFFFFD700).copy(alpha = 0.85f) else Color.Transparent
                "Ripple Wave Touch" -> {
                    if (isPressed) Color(0xFFFF9900) else Color(0xFFFF5500).copy(alpha = 0.2f)
                }
                else -> Color.Transparent
            }
            drawRect(color)
        }
    } else {
        Modifier.background(bezelColor)
    }

    val keyboardSize = LocalKeyboardSizeSetting.current
    val keyPaddingVertical = when (keyboardSize) {
        "UltraCompact" -> 0.4.dp
        "Compact" -> 1.0.dp
        else -> 1.5.dp
    }
    val keyPaddingHorizontal = when (keyboardSize) {
        "UltraCompact" -> 0.2.dp
        "Compact" -> 0.4.dp
        else -> 0.5.dp
    }

    Box(
        modifier = modifier
            .padding(horizontal = keyPaddingHorizontal, vertical = keyPaddingVertical)
            .height(LocalKeyHeight.current)
            .clip(keyShapeSpec)
            .then(bezelBackgroundModifier),
        contentAlignment = Alignment.TopCenter
    ) {
        // Instant visual preview/magnifier on Touch Down
        if (isPressed && displayLabel.length == 1 && displayLabel != " " && !showAlternatives && !TypingSpeedTracker.isFastTyping) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, -112),
                properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(width = 50.dp, height = 50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = displayLabel.uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        if (showAlternatives && alternatives.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, -140),
                onDismissRequest = { showAlternatives = false },
                properties = PopupProperties(focusable = true, dismissOnClickOutside = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 10.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        alternatives.forEach { alt ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { 
                                        onAlternateClick?.invoke(alt)
                                        showAlternatives = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(alt, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // The depressed mechanical keycap surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = travelY)
                .scale(scale)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastClickTime > debounceMs) {
                            playKeySoundAndClick()
                            lastClickTime = now
                        }
                    },
                    onLongClick = {
                        if (onLongClick != null) {
                            onLongClick()
                        } else if (alternatives.isNotEmpty()) {
                            showAlternatives = true
                        }
                    }
                ),
            shape = keyShapeSpec,
            color = if (isPressed) finalContainerColor.copy(alpha = 0.85f) else finalContainerColor,
            border = if (keyStyleSetting == "Outlined") BorderStroke(1.dp, bezelColor) else null,
            shadowElevation = if (isPressed) 0.dp else 2.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayLabel,
                    fontSize = if (displayLabel.length > 1) 12.sp else 19.sp,
                    fontWeight = if (displayLabel.length > 1) FontWeight.Bold else FontWeight.Medium,
                    color = finalContentColor
                )
                
                // Small hint for alternatives
                if (alternatives.isNotEmpty()) {
                    Text(
                        text = alternatives.first(),
                        fontSize = 8.sp,
                        color = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AlphabetKeyboard(
    shiftState: ShiftState,
    onKeyClick: (String) -> Unit,
    onShiftClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onSpaceLongClick: (() -> Unit)? = null,
    onSwitchToSymbols: () -> Unit,
    onSwitchToEmoji: () -> Unit,
    onActionClick: () -> Unit,
    onToggleBars: () -> Unit,
    showNumberRow: Boolean = false,
    onSwipeCompletions: ((List<String>) -> Unit)? = null
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val row0 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    // Gesture/Swipe States
    val coroutineScope = rememberCoroutineScope()
    val soundProfile = LocalSoundProfileSetting.current
    val soundOverrides = LocalKeySoundOverridesSetting.current
    val soundEnabled = LocalSoundEnabledSetting.current
    val hapticSetting = LocalHapticLevelSetting.current
    val view = LocalView.current

    var activePressedKey by remember { mutableStateOf<String?>(null) }
    var isSpaceLongPressed by remember { mutableStateOf(false) }

    // High performance typing speed tracking
    val keystrokeTimes = remember { mutableStateListOf<Long>() }
    var isFastTyping by remember { mutableStateOf(false) }

    val playInstantFeedback = { charKey: String ->
        val clean = charKey.lowercase()
        if (soundEnabled) {
            val targetSound = soundOverrides[clean] ?: soundProfile
            KeyboardSoundEngine.playSound(targetSound, clean)
        } else {
            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        }
        if (hapticSetting != "None") {
            val constant = when (hapticSetting) {
                "Soft" -> android.view.HapticFeedbackConstants.CLOCK_TICK
                "Strong" -> android.view.HapticFeedbackConstants.LONG_PRESS
                else -> android.view.HapticFeedbackConstants.KEYBOARD_TAP
            }
            view.performHapticFeedback(constant, android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
        }
    }

    var deleteJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var longPressJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var longPressChar by remember { mutableStateOf<String?>(null) }
    var longPressAlternatives by remember { mutableStateOf<List<String>>(emptyList()) }
    var longPressSelectedIdx by remember { mutableStateOf(0) }
    var longPressStartX by remember { mutableStateOf(0f) }

    var swipePath by remember { mutableStateOf(emptyList<androidx.compose.ui.geometry.Offset>()) }
    var isSwiping by remember { mutableStateOf(false) }
    val swipedKeysList = remember { mutableStateListOf<String>() }
    var keyboardWidth by remember { mutableStateOf(1f) }
    var keyboardHeight by remember { mutableStateOf(1f) }

    val isSplitModeActive = LocalKeyboardLayoutMode.current == "Split"

    // Key mapping mathematical utility
    fun getCharForOffset(offset: androidx.compose.ui.geometry.Offset, width: Float, height: Float): String {
        if (width <= 1f || height <= 1f) return ""
        val rowsCount = if (showNumberRow) 5 else 4
        val rowHeight = height / rowsCount
        val rowIndex = (offset.y / rowHeight).toInt().coerceIn(0, rowsCount - 1)
        
        val actualRowIndex = if (showNumberRow) rowIndex else rowIndex + 1
        
        return when (actualRowIndex) {
            0 -> { // Row 0
                if (isSplitModeActive) {
                    val keyColWidth = width / 12f
                    if (offset.x < keyColWidth * 5f) {
                        val col = (offset.x / keyColWidth).toInt().coerceIn(0, 4)
                        row0[col]
                    } else if (offset.x > keyColWidth * 7f) {
                        val col = ((offset.x - keyColWidth * 2f) / keyColWidth).toInt().coerceIn(5, 9)
                        row0[col]
                    } else { "" }
                } else {
                    val col = (offset.x / (width / 10f)).toInt().coerceIn(0, 9)
                    row0[col]
                }
            }
            1 -> { // Row 1
                if (isSplitModeActive) {
                    val keyColWidth = width / 12f
                    if (offset.x < keyColWidth * 5f) {
                        val col = (offset.x / keyColWidth).toInt().coerceIn(0, 4)
                        row1[col]
                    } else if (offset.x > keyColWidth * 7f) {
                        val col = ((offset.x - keyColWidth * 2f) / keyColWidth).toInt().coerceIn(5, 9)
                        row1[col]
                    } else { "" }
                } else {
                    val col = (offset.x / (width / 10f)).toInt().coerceIn(0, 9)
                    row1[col]
                }
            }
            2 -> { // Row 2 (Spacer 0.5f on each side)
                if (isSplitModeActive) {
                    val keyColWidth = width / 12f
                    if (offset.x < keyColWidth * 0.25f) { "" }
                    else if (offset.x < keyColWidth * 4.25f) {
                        val col = ((offset.x - keyColWidth * 0.25f) / keyColWidth).toInt().coerceIn(0, 3)
                        row2[col]
                    } else if (offset.x > keyColWidth * 6.75f) {
                        val col = ((offset.x - keyColWidth * 2.75f) / keyColWidth).toInt().coerceIn(4, 8)
                        row2[col]
                    } else { "" }
                } else {
                    val leftPad = width / 20f
                    val activeWidth = width - (leftPad * 2f)
                    val col = ((offset.x - leftPad) / (activeWidth / 9f)).toInt().coerceIn(0, 8)
                    row2[col]
                }
            }
            3 -> { // Row 3
                if (isSplitModeActive) {
                    val keyColWidth = width / 12f
                    if (offset.x < keyColWidth * 1.3f) {
                        "Shift"
                    } else if (offset.x < keyColWidth * 4.9f) {
                        val col = ((offset.x - keyColWidth * 1.3f) / (keyColWidth * 0.9f)).toInt().coerceIn(0, 2)
                        row3[col]
                    } else if (offset.x > keyColWidth * 6.7f && offset.x < keyColWidth * 10.3f) {
                        val col = ((offset.x - keyColWidth * 3.1f) / (keyColWidth * 0.9f)).toInt().coerceIn(3, 6)
                        row3[col]
                    } else if (offset.x >= keyColWidth * 10.3f) {
                        "Delete"
                    } else { "" }
                } else {
                    val shiftWidth = width * 1.5f / 9.9f
                    val deleteWidth = width * 1.4f / 9.9f
                    val keyWidth = width * 1.0f / 9.9f
                    if (offset.x < shiftWidth) {
                        "Shift"
                    } else if (offset.x > (width - deleteWidth)) {
                        "Delete"
                    } else {
                        val col = ((offset.x - shiftWidth) / keyWidth).toInt().coerceIn(0, 6)
                        row3[col]
                    }
                }
            }
            4 -> { // Row 4 (Space bar and other system keys)
                if (isSplitModeActive) {
                    val keyColWidth = width / 12f
                    if (offset.x < keyColWidth * 1.1f) {
                        "?123"
                    } else if (offset.x < keyColWidth * 1.9f) {
                        "😀"
                    } else if (offset.x < keyColWidth * 3.9f) {
                        " " // Space
                    } else if (offset.x > keyColWidth * 5.9f && offset.x < keyColWidth * 7.9f) {
                        " " // Space
                    } else if (offset.x >= keyColWidth * 7.9f && offset.x < keyColWidth * 10.3f) {
                        "."
                    } else if (offset.x >= keyColWidth * 10.3f) {
                        "\n" // Enter
                    } else { "" }
                } else {
                    val swWidth = width * 1.5f / 9.9f
                    val emWidth = width * 1.0f / 9.9f
                    val spWidth = width * 4.5f / 9.9f
                    val colWidth = width * 0.7f / 9.9f
                    
                    if (offset.x < swWidth) {
                        "?123"
                    } else if (offset.x < swWidth + emWidth) {
                        "😀"
                    } else if (offset.x < swWidth + emWidth + spWidth) {
                        " " // Space
                    } else if (offset.x < swWidth + emWidth + spWidth + colWidth) {
                        "↕️" // Bars toggle
                    } else if (offset.x < swWidth + emWidth + spWidth + (colWidth * 2f)) {
                        "."
                    } else {
                        "\n" // Enter
                    }
                }
            }
            else -> ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                keyboardWidth = it.size.width.toFloat()
                keyboardHeight = it.size.height.toFloat()
            }
            .pointerInput(showNumberRow, keyboardWidth, keyboardHeight) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.isEmpty()) continue
                        
                        val change = changes.first()
                        val pos = change.position
                        
                        if (change.pressed) {
                            if (change.previousPressed.not()) {
                                // Touch Down (Touch Instantaneous Reaction)
                                isSwiping = false
                                swipePath = listOf(pos)
                                swipedKeysList.clear()
                                val ch = getCharForOffset(pos, keyboardWidth, keyboardHeight)
                                activePressedKey = ch
                                
                                // Reset Long Press
                                longPressChar = null
                                longPressSelectedIdx = 0
                                longPressJob?.cancel()
                                isSpaceLongPressed = false
                                
                                if (ch.isNotEmpty()) {
                                    playInstantFeedback(ch)
                                    
                                    // High-performance adaptive speed tracking
                                    val nowTime = System.currentTimeMillis()
                                    keystrokeTimes.add(nowTime)
                                    if (keystrokeTimes.size > 4) keystrokeTimes.removeAt(0)
                                    val avgInterval = if (keystrokeTimes.size >= 2) {
                                        var total = 0L
                                        for (i in 1 until keystrokeTimes.size) {
                                            total += (keystrokeTimes[i] - keystrokeTimes[i - 1])
                                        }
                                        total / (keystrokeTimes.size - 1)
                                    } else {
                                        1000L
                                    }
                                    isFastTyping = avgInterval < 240L
                                }
                                
                                if (ch == "Delete") {
                                    deleteJob?.cancel()
                                    deleteJob = coroutineScope.launch {
                                        var currentDelay = 350L
                                        while (isActive) {
                                            onDeleteClick()
                                            delay(currentDelay)
                                            if (currentDelay > 40L) {
                                                currentDelay = (currentDelay * 0.8f).toLong().coerceAtLeast(40L)
                                            }
                                        }
                                    }
                                } else if (ch == " ") {
                                    longPressJob = coroutineScope.launch {
                                        delay(360L) // low latency long press
                                        isSpaceLongPressed = true
                                        onSpaceLongClick?.invoke()
                                    }
                                } else if (ch.isNotEmpty() && ch.length == 1) {
                                    swipedKeysList.add(ch)
                                    // Start Long Press Timer
                                    longPressJob = coroutineScope.launch {
                                        delay(450L) // Long press threshold
                                        val alts = SymbolDataSource.longPressMapping[ch.lowercase()]
                                        if (alts != null) {
                                            longPressChar = ch
                                            longPressAlternatives = alts
                                            longPressSelectedIdx = 0
                                            longPressStartX = pos.x
                                            isSwiping = false // Long press overrides swiping
                                        }
                                    }
                                }
                            } else {
                                // Touch Move / Drag (Realtime key hovering/glide highlighting)
                                swipePath = swipePath + pos
                                val ch = getCharForOffset(pos, keyboardWidth, keyboardHeight)
                                activePressedKey = ch
                                
                                if (ch != "Delete") {
                                    deleteJob?.cancel()
                                    deleteJob = null
                                }
                                
                                if (longPressChar != null) {
                                    // Handle sliding selection in long press popup
                                    val deltaX = pos.x - longPressStartX
                                    val step = 60f 
                                    
                                    val rawIdx = if (deltaX > 20f) {
                                        ((deltaX - 20f) / step).toInt() + 1
                                    } else if (deltaX < -20f) {
                                        0 
                                    } else {
                                        0
                                    }
                                    longPressSelectedIdx = rawIdx.coerceIn(0, longPressAlternatives.size)
                                } else {
                                    val displacement = (pos - (swipePath.firstOrNull() ?: pos)).getDistance()
                                    if (ch.isNotEmpty() && ch.length == 1 && swipedKeysList.lastOrNull() != ch) {
                                        if (displacement > 20f) { // Slop tolerance before cancelling long press
                                            swipedKeysList.add(ch)
                                            isSwiping = true
                                            longPressJob?.cancel() 
                                        }
                                    }
                                }
                            }
                            change.consume()
                        } else if (change.previousPressed) {
                            // Touch Up / Finger Raised
                            activePressedKey = null
                            deleteJob?.cancel()
                            deleteJob = null
                            longPressJob?.cancel()
                            
                            val lpChar = longPressChar
                            val lpIdx = longPressSelectedIdx
                            longPressChar = null // Clear state immediately
                            
                            if (isSpaceLongPressed) {
                                isSpaceLongPressed = false
                            } else if (lpChar != null) {
                                val alts = (listOf(lpChar) + longPressAlternatives).distinct()
                                if (lpIdx < alts.size) {
                                    onKeyClick(alts[lpIdx])
                                }
                            } else {
                                val totalDisplacement = if (swipePath.size > 1) {
                                    val start = swipePath.first()
                                    val end = swipePath.last()
                                    androidx.compose.ui.geometry.Offset(end.x - start.x, end.y - start.y).getDistance()
                                } else 0f
                                
                                if (isSwiping && swipedKeysList.size >= 2 && totalDisplacement > 40f) {
                                    // Glide Solver!
                                    val swipedString = swipedKeysList.joinToString("")
                                    val completions = solveSwipeLetters(swipedString)
                                    if (completions.isNotEmpty()) {
                                        onSwipeCompletions?.invoke(completions)
                                    }
                                } else {
                                    // Single Click
                                    val finalPos = swipePath.lastOrNull() ?: pos
                                    val finalChar = getCharForOffset(finalPos, keyboardWidth, keyboardHeight)
                                    if (finalChar.isNotEmpty()) {
                                        if (finalChar != "Delete") {
                                            when (finalChar) {
                                                "Shift" -> onShiftClick()
                                                " " -> onSpaceClick()
                                                "?123" -> onSwitchToSymbols()
                                                "😀" -> onSwitchToEmoji()
                                                "↕️" -> onToggleBars()
                                                "\n" -> onActionClick()
                                                else -> onKeyClick(finalChar)
                                            }
                                        }
                                    }
                                }
                            }
                            // Clear trails
                            swipePath = emptyList()
                            isSwiping = false
                            change.consume()
                        }
                    }
                }
            }
    ) {
        val keyboardSize = LocalKeyboardSizeSetting.current
        val isSplit = LocalKeyboardLayoutMode.current == "Split"

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = keyboardSize.keyboardPaddingHorizontal,
                    vertical = keyboardSize.keyboardPaddingVertical
                ),
            verticalArrangement = Arrangement.spacedBy(keyboardSize.rowSpacing)
        ) {
            if (showNumberRow) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isSplit) {
                        row0.forEachIndexed { idx, key ->
                            key(key + idx) {
                                KeyButton(
                                    text = key,
                                    modifier = Modifier.weight(1f),
                                    pressedOverride = (activePressedKey == key),
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                ) { }
                            }
                            if (idx == 4) Spacer(modifier = Modifier.weight(2f))
                        }
                    } else {
                        row0.forEach { key ->
                            key(key) {
                                KeyButton(
                                    text = key,
                                    modifier = Modifier.weight(1f),
                                    pressedOverride = (activePressedKey == key),
                                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                ) { /* touch intercept handles actual execution */ }
                            }
                        }
                    }
                }
            }
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isSplit) {
                    row1.forEachIndexed { idx, key ->
                        key(key + idx) {
                            KeyButton(
                                text = if (shiftState != ShiftState.LOWERCASE) key.uppercase() else key,
                                pressedOverride = (activePressedKey?.lowercase() == key.lowercase()),
                                modifier = Modifier.weight(1f)
                            ) { }
                        }
                        if (idx == 4) Spacer(modifier = Modifier.weight(2f))
                    }
                } else {
                    row1.forEach { key ->
                        key(key) {
                            KeyButton(
                                text = if (shiftState != ShiftState.LOWERCASE) key.uppercase() else key,
                                pressedOverride = (activePressedKey?.lowercase() == key.lowercase()),
                                modifier = Modifier.weight(1f)
                            ) { /* intercept handles clicks */ }
                        }
                    }
                }
            }

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
            ) {
                if (isSplit) {
                    Spacer(modifier = Modifier.weight(0.25f))
                    row2.forEachIndexed { idx, key ->
                        key(key + idx) {
                            KeyButton(
                                text = if (shiftState != ShiftState.LOWERCASE) key.uppercase() else key,
                                pressedOverride = (activePressedKey?.lowercase() == key.lowercase()),
                                modifier = Modifier.weight(1f)
                            ) { }
                        }
                        if (idx == 3) Spacer(modifier = Modifier.weight(2.5f))
                    }
                    Spacer(modifier = Modifier.weight(0.25f))
                } else {
                    Spacer(modifier = Modifier.weight(0.5f))
                    row2.forEach { key ->
                        key(key) {
                            KeyButton(
                                text = if (shiftState != ShiftState.LOWERCASE) key.uppercase() else key,
                                pressedOverride = (activePressedKey?.lowercase() == key.lowercase()),
                                modifier = Modifier.weight(1f)
                            ) { /* intercept clicks */ }
                        }
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }
            }

            // Row 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val shiftColor = when (shiftState) {
                    ShiftState.SHIFTED -> MaterialTheme.colorScheme.secondaryContainer
                    ShiftState.CAPSLOCK -> MaterialTheme.colorScheme.primaryContainer
                    ShiftState.LOWERCASE -> MaterialTheme.colorScheme.surfaceVariant
                }
                val shiftTextColor = when (shiftState) {
                    ShiftState.SHIFTED -> MaterialTheme.colorScheme.onSecondaryContainer
                    ShiftState.CAPSLOCK -> MaterialTheme.colorScheme.onPrimaryContainer
                    ShiftState.LOWERCASE -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                if (isSplit) {
                    key("Shift") {
                        KeyButton(
                            text = if (shiftState == ShiftState.CAPSLOCK) "⇪" else "⇧",
                            pressedOverride = (activePressedKey == "Shift"),
                            modifier = Modifier.weight(1.3f),
                            containerColor = shiftColor,
                            contentColor = shiftTextColor
                        ) { }
                    }

                    row3.forEachIndexed { idx, key ->
                        key(key + idx) {
                            KeyButton(
                                text = if (shiftState != ShiftState.LOWERCASE) key.uppercase() else key,
                                pressedOverride = (activePressedKey?.lowercase() == key.lowercase()),
                                modifier = Modifier.weight(0.9f)
                            ) { }
                        }
                        if (idx == 2) Spacer(modifier = Modifier.weight(1.8f))
                    }

                    key("Delete") {
                        KeyButton(
                            text = "⌫",
                            pressedOverride = (activePressedKey == "Delete"),
                            modifier = Modifier.weight(1.2f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            isRepeatable = true
                        ) { }
                    }
                } else {
                    key("Shift") {
                        KeyButton(
                            text = if (shiftState == ShiftState.CAPSLOCK) "⇪" else "⇧",
                            pressedOverride = (activePressedKey == "Shift"),
                            modifier = Modifier.weight(1.5f),
                            containerColor = shiftColor,
                            contentColor = shiftTextColor
                        ) { /* intercept handles clicks */ }
                    }

                    row3.forEach { key ->
                        key(key) {
                            KeyButton(
                                text = if (shiftState != ShiftState.LOWERCASE) key.uppercase() else key,
                                pressedOverride = (activePressedKey?.lowercase() == key.lowercase()),
                                modifier = Modifier.weight(1f)
                            ) { /* intercept clicks */ }
                        }
                    }

                    key("Delete") {
                        KeyButton(
                            text = "⌫",
                            pressedOverride = (activePressedKey == "Delete"),
                            modifier = Modifier.weight(1.4f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            isRepeatable = true
                        ) { /* intercept delete clicks */ }
                    }
                }
            }

            // Row 4
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isSplit) {
                    key("?123") {
                        KeyButton(
                            text = "?123",
                            pressedOverride = (activePressedKey == "?123"),
                            modifier = Modifier.weight(1.1f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) { }
                    }

                    key("😀") {
                        KeyButton(
                            text = "😀",
                            pressedOverride = (activePressedKey == "😀"),
                            modifier = Modifier.weight(0.8f)
                        ) { }
                    }

                    key("SpaceLeft") {
                        KeyButton(
                            text = "Space",
                            pressedOverride = (activePressedKey == " "),
                            modifier = Modifier.weight(2.0f),
                            onLongClick = onSpaceLongClick
                        ) { }
                    }

                    Spacer(modifier = Modifier.weight(2.0f))

                    key("SpaceRight") {
                        KeyButton(
                            text = "Space",
                            pressedOverride = (activePressedKey == " "),
                            modifier = Modifier.weight(2.0f),
                            onLongClick = onSpaceLongClick
                        ) { }
                    }

                    key(".") {
                        KeyButton(
                            text = ".",
                            pressedOverride = (activePressedKey == "."),
                            modifier = Modifier.weight(0.6f)
                        ) { }
                    }

                    key("Enter") {
                        KeyButton(
                            text = "⏎",
                            pressedOverride = (activePressedKey == "\n"),
                            modifier = Modifier.weight(1.1f),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) { }
                    }
                } else {
                    key("?123") {
                        KeyButton(
                            text = "?123",
                            pressedOverride = (activePressedKey == "?123"),
                            modifier = Modifier.weight(if (isLandscape) 1.2f else 1.5f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) { /* handled */ }
                    }

                    key("😀") {
                        KeyButton(
                            text = "😀",
                            pressedOverride = (activePressedKey == "😀"),
                            modifier = Modifier.weight(1f)
                        ) { /* handled */ }
                    }

                    key("Space") {
                        KeyButton(
                            text = "Space",
                            pressedOverride = (activePressedKey == " "),
                            modifier = Modifier.weight(if (isLandscape) 4.2f else 4.5f),
                            onLongClick = onSpaceLongClick
                        ) { /* handled */ }
                    }
                    
                    key("↕️") {
                        KeyButton(
                            text = "↕️",
                            pressedOverride = (activePressedKey == "↕️"),
                            modifier = Modifier.weight(if (isLandscape) 0.6f else 0.7f),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ) { /* handled */ }
                    }

                    key(".") {
                        KeyButton(
                            text = ".",
                            pressedOverride = (activePressedKey == "."),
                            modifier = Modifier.weight(if (isLandscape) 0.6f else 0.7f)
                        ) { /* handled */ }
                    }

                    key("Enter") {
                        KeyButton(
                            text = "⏎",
                            pressedOverride = (activePressedKey == "\n"),
                            modifier = Modifier.weight(if (isLandscape) 1.2f else 1.5f),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) { /* handled */ }
                    }
                }
            }
        }

        // RGB swipe trail
        if (isSwiping && swipePath.size > 1) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(alpha = 0.85f)
            ) {
                for (i in 0 until swipePath.size - 1) {
                    val p1 = swipePath[i]
                    val p2 = swipePath[i + 1]
                    val hue = (i * 10f) % 360f
                    val color = androidx.compose.ui.graphics.Color.hsv(hue, 0.9f, 1.0f, alpha = 0.7f)
                    drawLine(
                        color = color,
                        start = p1,
                        end = p2,
                        strokeWidth = 8.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        if (longPressChar != null) {
            LongPressSymbolPopup(
                mainKey = longPressChar!!,
                alternatives = longPressAlternatives,
                selectedIndex = longPressSelectedIdx,
                xOffset = longPressStartX,
                onDismiss = { longPressChar = null }
            )
        }
    }
}

fun solveSwipeLetters(letters: String): List<String> {
    if (letters.length < 2) return emptyList()
    val pattern = letters.lowercase().trim()
    val dictionary = listOf(
        "the", "and", "you", "that", "was", "for", "are", "with", "his", "they",
        "hello", "where", "when", "what", "please", "thanks", "great", "awesome",
        "hai", "nhi", "kya", "toh", "han", "achha", "baat", "kaise", "thik", "yaar",
        "main", "hum", "tum", "mera", "beta", "bhai", "shuru", "karo", "abhi", "kal"
    ).distinct()

    val results = mutableListOf<Pair<String, Int>>()
    for (word in dictionary) {
        val first = word.firstOrNull() ?: continue
        if (pattern.isNotEmpty() && first != pattern.first()) continue
        
        var pIdx = 0
        var matched = 0
        for (char in word) {
            val found = pattern.indexOf(char, pIdx)
            if (found != -1) {
                pIdx = found
                matched++
            }
        }
        if (matched == word.length) {
            val diff = Math.abs(word.length - pattern.length)
            results.add(word to (100 - (diff * 12)).coerceAtLeast(10))
        }
    }
    return results.sortedByDescending { it.second }.map { it.first }.take(4)
}

@Composable
fun LongPressSymbolPopup(
    mainKey: String,
    alternatives: List<String>,
    selectedIndex: Int,
    xOffset: Float,
    onDismiss: () -> Unit
) {
    val options = (listOf(mainKey) + alternatives).distinct()
    Popup(
        alignment = Alignment.TopCenter,
        offset = androidx.compose.ui.unit.IntOffset(0, -180),
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp),
            shadowElevation = 10.dp,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEachIndexed { index, alt ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 54.dp else 44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = alt,
                            fontSize = if (isSelected) 24.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SymbolsKeyboard(
    isAltSymbols: Boolean,
    onKeyClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onSpaceClick: () -> Unit,
    onSpaceLongClick: (() -> Unit)? = null,
    onSwitchToAlphabet: () -> Unit,
    onSwitchToAltSymbols: () -> Unit,
    onSwitchToEmoji: () -> Unit,
    onActionClick: () -> Unit
) {
    val row1 = if (!isAltSymbols) listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
               else listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
               
    val row2 = if (!isAltSymbols) listOf("@", "#", "$", "%", "&", "*", "-", "+", "=")
               else listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥")
               
    val row3 = if (!isAltSymbols) listOf(".", ",", "?", "!", "'")
               else listOf(".", ",", "¡", "¿", "“", "”")

    val keyboardSize = LocalKeyboardSizeSetting.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = keyboardSize.keyboardPaddingHorizontal,
                vertical = keyboardSize.keyboardPaddingVertical
            ),
        verticalArrangement = Arrangement.spacedBy(keyboardSize.rowSpacing)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            row1.forEach { key ->
                KeyButton(text = key, modifier = Modifier.weight(1f)) { onKeyClick(key) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(0.5f))
            row2.forEach { key ->
                KeyButton(text = key, modifier = Modifier.weight(1f)) { onKeyClick(key) }
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            KeyButton(
                text = if (!isAltSymbols) "=\\<" else "?123",
                modifier = Modifier.weight(1.5f),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) { onSwitchToAltSymbols() }

            row3.forEach { key ->
                KeyButton(text = key, modifier = Modifier.weight(1.4f)) { onKeyClick(key) }
            }

            KeyButton(
                text = "⌫",
                modifier = Modifier.weight(1.4f),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                isRepeatable = true
            ) { onDeleteClick() }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            KeyButton(
                text = "ABC",
                modifier = Modifier.weight(1.5f),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) { onSwitchToAlphabet() }

            KeyButton(text = "😀", modifier = Modifier.weight(1f)) { onSwitchToEmoji() }

            KeyButton(text = "Space", modifier = Modifier.weight(5f)) { onSpaceClick() }

            KeyButton(
                text = "⏎",
                modifier = Modifier.weight(2.5f),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) { onActionClick() }
        }
    }
}

@Composable
fun EmojiKeyboard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onEmojiClick: (String) -> Unit,
    onSwitchToAlphabet: () -> Unit
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var selectedCategory by remember { mutableStateOf(EmojiDataSource.categories[1]) }
    val recentEmojis = remember { ContextStore.getRecentEmojis(context) }

    val displayEmojis = remember(searchQuery, selectedCategory, recentEmojis) {
        if (searchQuery.isNotBlank()) {
            EmojiDataSource.emojiData.values.flatten()
                .filter { item ->
                    item.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                }
                .distinctBy { it.emoji }
        } else if (selectedCategory == "Recent") {
            recentEmojis.map { EmojiItem(it, emptyList()) }
        } else {
            EmojiDataSource.emojiData[selectedCategory] ?: emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 2.dp)
    ) {
        // Top Unified Command and Category Bar (No bottom bar needed!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Small ABC Switch Tonal Button
            Button(
                onClick = onSwitchToAlphabet,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                contentPadding = PaddingValues(horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    "ABC", 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Categories horizontal slider (takes middle room)
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(EmojiDataSource.categories) { category ->
                    val isSelected = selectedCategory == category && searchQuery.isBlank()
                    Surface(
                        selected = isSelected,
                        onClick = { 
                            selectedCategory = category
                            onSearchQueryChange("")
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category, 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Small Backspace Tonal Button
            Button(
                onClick = { onEmojiClick("\b") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                contentPadding = PaddingValues(horizontal = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    "⌫", 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Compact Slim Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍", fontSize = 10.sp)
                BasicTextField(
                    value = searchQuery, 
                    onValueChange = onSearchQueryChange, 
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") }, 
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close, 
                            contentDescription = "Clear", 
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }

        // Expanded Scrollable Grid of Emojis
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = if (isLandscape) 34.dp else 40.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ) {
            items(displayEmojis) { item ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { 
                            onEmojiClick(item.emoji)
                            ContextStore.addRecentEmoji(context, item.emoji)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.emoji, 
                        fontSize = if (isLandscape) 18.sp else 22.sp
                    )
                }
            }
        }
    }
}

fun getKeyShapeStyle(shapeName: String): androidx.compose.ui.graphics.Shape {
    return when (shapeName) {
        "Square", "Rectangle" -> RoundedCornerShape(0.dp)
        "Capsule" -> RoundedCornerShape(50)
        "Cut", "Octagon" -> androidx.compose.foundation.shape.CutCornerShape(8.dp)
        "Classic Retro" -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
        "SemiRound" -> RoundedCornerShape(12.dp)
        "Steep Angled" -> androidx.compose.foundation.shape.CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
        "Modern Shield" -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
        "Circular Dome" -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        "Wave Peak" -> RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
        "Rounded Pentagon" -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
        "Super Rounded" -> RoundedCornerShape(18.dp)
        "Love Heart" -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
        "Flower Bloom" -> RoundedCornerShape(topStart = 8.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 8.dp)
        "Star Badge" -> androidx.compose.foundation.shape.CutCornerShape(12.dp)
        else -> RoundedCornerShape(8.dp) // Default: "Rounded" / "Normal"
    }
}

object KeyboardSoundEngine {
    private var soundPool: android.media.SoundPool? = null
    private val soundIds = java.util.concurrent.ConcurrentHashMap<String, Int>()
    @Volatile private var isInitialized = false

    private fun generateWavBytes(sampleRate: Int, durationSeconds: Float, generator: (Float) -> Float): ByteArray {
        val numSamples = (sampleRate * durationSeconds).toInt()
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize
        val header = ByteArray(44)
        
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        
        header[4] = (totalSize and 0xff).toByte()
        header[5] = ((totalSize shr 8) and 0xff).toByte()
        header[6] = ((totalSize shr 16) and 0xff).toByte()
        header[7] = ((totalSize shr 24) and 0xff).toByte()
        
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        
        header[20] = 1 // PCM
        header[21] = 0
        
        header[22] = 1 // Mono
        header[23] = 0
        
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        
        val byteRate = sampleRate * 2
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        
        header[32] = 2
        header[33] = 0
        header[34] = 16
        header[35] = 0
        
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        
        header[40] = (dataSize and 0xff).toByte()
        header[41] = ((dataSize shr 8) and 0xff).toByte()
        header[42] = ((dataSize shr 16) and 0xff).toByte()
        header[43] = ((dataSize shr 24) and 0xff).toByte()
        
        val wav = ByteArray(44 + dataSize)
        System.arraycopy(header, 0, wav, 0, 44)
        
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val amp = generator(t).coerceIn(-1.0f, 1.0f)
            val shortVal = (amp * 32767).toInt()
            wav[44 + i * 2] = (shortVal and 0xff).toByte()
            wav[44 + i * 2 + 1] = ((shortVal shr 8) and 0xff).toByte()
        }
        return wav
    }

    fun init(context: android.content.Context) {
        if (isInitialized) return
        try {
            val attrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = android.media.SoundPool.Builder()
                .setMaxStreams(7)
                .setAudioAttributes(attrs)
                .build()
            
            val cacheDir = context.cacheDir
            
            // Build Cherry MX mechanical switch clack sound
            val mechBytes = generateWavBytes(22050, 0.05f) { t ->
                val rand = java.util.Random()
                val clickLeaf = if (t < 0.003f) (rand.nextFloat() * 2f - 1f) * 0.75f else 0.0f
                val woodClack = (rand.nextFloat() * 2f - 1f) * kotlin.math.exp(-t * 850f) * 0.28f
                val thockResonance = kotlin.math.sin(2 * kotlin.math.PI * 380f * t).toFloat() * kotlin.math.exp(-t * 400f) * 0.45f
                val thockLow = kotlin.math.sin(2 * kotlin.math.PI * 110f * t).toFloat() * kotlin.math.exp(-t * 500f) * 0.35f
                clickLeaf + woodClack + thockResonance + thockLow
            }
            
            // Build Creamy Bubble (Custom sweet pop)
            val bubbleBytes = generateWavBytes(22050, 0.05f) { t ->
                val fSweep = 400f + (1100f - 400f) * (1.0f - kotlin.math.exp(-t * 220f))
                val wave = kotlin.math.sin(2 * kotlin.math.PI * fSweep * t).toFloat() * kotlin.math.exp(-t * 180f) * 0.75f
                wave
            }
            
            // Build Vintage Typewriter (clunky old typewriter hammer thud)
            val vintageBytes = generateWavBytes(22050, 0.08f) { t ->
                val rand = java.util.Random()
                val clickSpike = if (t < 0.004f) (rand.nextFloat() * 2f - 1f) * 0.85f else 0.0f
                val paperTap = (rand.nextFloat() * 2f - 1f) * kotlin.math.exp(-t * 900f) * 0.35f
                val hammerTap = kotlin.math.sin(2 * kotlin.math.PI * 185f * t).toFloat() * kotlin.math.exp(-t * 300f) * 0.5f
                clickSpike + paperTap + hammerTap
            }
            
            // Build Sweet Chime
            val chimeBytes = generateWavBytes(22050, 0.07f) { t ->
                val sine1 = kotlin.math.sin(2 * kotlin.math.PI * 1450f * t).toFloat() * kotlin.math.exp(-t * 120f) * 0.40f
                val sine2 = kotlin.math.sin(2 * kotlin.math.PI * 2900f * t).toFloat() * kotlin.math.exp(-t * 190f) * 0.15f
                sine1 + sine2
            }
            
            // Build Standard Tick Tap
            val stdBytes = generateWavBytes(22050, 0.02f) { t ->
                val rand = java.util.Random()
                (rand.nextFloat() * 2f - 1f) * kotlin.math.exp(-t * 1600f) * 0.45f
            }

            val fMech = java.io.File(cacheDir, "k_m.wav").apply { writeBytes(mechBytes) }
            val fBubb = java.io.File(cacheDir, "k_b.wav").apply { writeBytes(bubbleBytes) }
            val fVint = java.io.File(cacheDir, "k_v.wav").apply { writeBytes(vintageBytes) }
            val fChim = java.io.File(cacheDir, "k_c.wav").apply { writeBytes(chimeBytes) }
            val fStd  = java.io.File(cacheDir, "k_s.wav").apply { writeBytes(stdBytes) }

            // Build Piano Soft Tap
            val pianoBytes = generateWavBytes(22050, 0.08f) { t ->
                val sineC = kotlin.math.sin(2 * kotlin.math.PI * 261.63f * t).toFloat() * kotlin.math.exp(-t * 90f) * 0.35f
                val sineE = kotlin.math.sin(2 * kotlin.math.PI * 329.63f * t).toFloat() * kotlin.math.exp(-t * 120f) * 0.20f
                val attack = if (t < 0.005f) t / 0.005f else 1.0f
                (sineC + sineE) * attack
            }
            // Build Marble Click
            val marbleBytes = generateWavBytes(22050, 0.015f) { t ->
                val rand = java.util.Random()
                (rand.nextFloat() * 2f - 1f) * kotlin.math.exp(-t * 3000f) * 0.65f
            }
            // Build Muted Mechanical
            val mutedMechBytes = generateWavBytes(22050, 0.04f) { t ->
                val rand = java.util.Random()
                val lowClick = (rand.nextFloat() * 2f - 1f) * kotlin.math.exp(-t * 1100f) * 0.15f
                val thud = kotlin.math.sin(2 * kotlin.math.PI * 220f * t).toFloat() * kotlin.math.exp(-t * 350f) * 0.40f
                lowClick + thud
            }
            // Build Soft Bubble
            val softBubbleBytes = generateWavBytes(22050, 0.04f) { t ->
                val fSweep = 300f + (750f - 300f) * (1.0f - kotlin.math.exp(-t * 180f))
                kotlin.math.sin(2 * kotlin.math.PI * fSweep * t).toFloat() * kotlin.math.exp(-t * 140f) * 0.50f
            }
            // Build Crystal Glass
            val crystalBytes = generateWavBytes(22050, 0.12f) { t ->
                val ring = kotlin.math.sin(2 * kotlin.math.PI * 3150f * t).toFloat() * kotlin.math.exp(-t * 40f) * 0.30f
                val undertone = kotlin.math.sin(2 * kotlin.math.PI * 1575f * t).toFloat() * kotlin.math.exp(-t * 25f) * 0.12f
                ring + undertone
            }
            // Build Retro Typewriter
            val retroTypeBytes = generateWavBytes(22050, 0.07f) { t ->
                val rand = java.util.Random()
                val latch = if (t < 0.006f) (rand.nextFloat() * 2f - 1f) * 0.70f else 0.0f
                val springRes = kotlin.math.sin(2 * kotlin.math.PI * 440f * t).toFloat() * kotlin.math.exp(-t * 220f) * 0.25f
                latch + springRes
            }
            // Build Velvet Tap
            val velvetBytes = generateWavBytes(22050, 0.03f) { t ->
                kotlin.math.sin(2 * kotlin.math.PI * 130f * t).toFloat() * kotlin.math.exp(-t * 600f) * 0.55f
            }
            // Build Deep Tactile
            val deepTactileBytes = generateWavBytes(22050, 0.06f) { t ->
                val thump = kotlin.math.sin(2 * kotlin.math.PI * 90f * t).toFloat() * kotlin.math.exp(-t * 180f) * 0.65f
                val click = kotlin.math.sin(2 * kotlin.math.PI * 350f * t).toFloat() * kotlin.math.exp(-t * 500f) * 0.20f
                thump + click
            }
            // Build Synth Pulse
            val synthPulseBytes = generateWavBytes(22050, 0.10f) { t ->
                val pSweep = 1200f * kotlin.math.exp(-t * 90f)
                kotlin.math.sin(2 * kotlin.math.PI * pSweep * t).toFloat() * kotlin.math.exp(-t * 65f) * 0.45f
            }
            // Build Ambient Soft Tech
            val ambientTechBytes = generateWavBytes(22050, 0.08f) { t ->
                val osc1 = kotlin.math.sin(2 * kotlin.math.PI * 880f * t).toFloat() * kotlin.math.exp(-t * 110f) * 0.25f
                val osc2 = kotlin.math.sin(2 * kotlin.math.PI * 1760f * t).toFloat() * kotlin.math.exp(-t * 160f) * 0.05f
                osc1 + osc2
            }

            val fPiano = java.io.File(cacheDir, "k_p.wav").apply { writeBytes(pianoBytes) }
            val fMarble = java.io.File(cacheDir, "k_mr.wav").apply { writeBytes(marbleBytes) }
            val fMMech = java.io.File(cacheDir, "k_mm.wav").apply { writeBytes(mutedMechBytes) }
            val fSBub = java.io.File(cacheDir, "k_sb.wav").apply { writeBytes(softBubbleBytes) }
            val fCry = java.io.File(cacheDir, "k_cr.wav").apply { writeBytes(crystalBytes) }
            val fRet = java.io.File(cacheDir, "k_rt.wav").apply { writeBytes(retroTypeBytes) }
            val fVel = java.io.File(cacheDir, "k_ve.wav").apply { writeBytes(velvetBytes) }
            val fDTac = java.io.File(cacheDir, "k_dt.wav").apply { writeBytes(deepTactileBytes) }
            val fSyn = java.io.File(cacheDir, "k_sy.wav").apply { writeBytes(synthPulseBytes) }
            val fAmb = java.io.File(cacheDir, "k_at.wav").apply { writeBytes(ambientTechBytes) }

            val p = soundPool ?: return
            soundIds["Mechanical"] = p.load(fMech.absolutePath, 1)
            soundIds["Bubble"] = p.load(fBubb.absolutePath, 1)
            soundIds["Vintage"] = p.load(fVint.absolutePath, 1)
            soundIds["Chime"] = p.load(fChim.absolutePath, 1)
            soundIds["Standard"] = p.load(fStd.absolutePath, 1)

            soundIds["Piano Soft Tap"] = p.load(fPiano.absolutePath, 1)
            soundIds["Marble Click"] = p.load(fMarble.absolutePath, 1)
            soundIds["Muted Mechanical"] = p.load(fMMech.absolutePath, 1)
            soundIds["Soft Bubble"] = p.load(fSBub.absolutePath, 1)
            soundIds["Crystal Glass"] = p.load(fCry.absolutePath, 1)
            soundIds["Retro Typewriter"] = p.load(fRet.absolutePath, 1)
            soundIds["Velvet Tap"] = p.load(fVel.absolutePath, 1)
            soundIds["Deep Tactile"] = p.load(fDTac.absolutePath, 1)
            soundIds["Synth Pulse"] = p.load(fSyn.absolutePath, 1)
            soundIds["Ambient Soft Tech"] = p.load(fAmb.absolutePath, 1)
            
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSound(profile: String, keyChar: String = "default") {
        try {
            val p = soundPool ?: return
            val id = soundIds[profile] ?: when(profile) {
                "ASMR Slime" -> soundIds["Bubble"]
                "Water Drop" -> soundIds["Bubble"]
                "Raindrop" -> soundIds["Standard"]
                "Cute Meow" -> soundIds["Chime"]
                "Wood Block" -> soundIds["Mechanical"]
                "Cyberpunk Clack" -> soundIds["Mechanical"]
                "Metal Clank" -> soundIds["Vintage"]
                "Retro Arcade" -> soundIds["Chime"]
                "Futuristic Laser" -> soundIds["Chime"]
                "Space Laser" -> soundIds["Chime"]
                else -> soundIds["Mechanical"]
            } ?: return
            
            val basePitch = when(profile) {
                "ASMR Slime" -> 0.65f
                "Water Drop" -> 1.45f
                "Raindrop" -> 1.85f
                "Cute Meow" -> 1.35f
                "Wood Block" -> 0.75f
                "Cyberpunk Clack" -> 1.15f
                "Metal Clank" -> 0.85f
                "Retro Arcade" -> 1.65f
                "Futuristic Laser" -> 2.15f
                "Space Laser" -> 0.45f
                "Piano Soft Tap" -> 1.0f
                "Marble Click" -> 1.50f
                "Muted Mechanical" -> 0.90f
                "Soft Bubble" -> 1.10f
                "Crystal Glass" -> 1.25f
                "Retro Typewriter" -> 0.95f
                "Velvet Tap" -> 0.85f
                "Deep Tactile" -> 0.80f
                "Synth Pulse" -> 1.20f
                "Ambient Soft Tech" -> 1.30f
                else -> 1.0f
            }
            
            val pitch = (0.94f + (java.util.Random().nextFloat() * 0.12f)) * basePitch
            val finalVolume = when(profile) {
                "Raindrop", "ASMR Slime" -> 0.65f
                "Piano Soft Tap" -> 0.75f
                "Velvet Tap", "Muted Mechanical" -> 0.70f
                "Marble Click" -> 0.90f
                "Soft Bubble", "Crystal Glass" -> 0.85f
                else -> 0.95f
            }
            p.play(id, finalVolume, finalVolume, 1, 0, pitch)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun TelemetryScreenView(
    liveCpu: Float,
    liveFps: Float,
    liveLatencyMs: Float,
    stats: Map<String, String>,
    onCloseClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KeyboardScreenHeader("Diagnostics", "🛰️", onCloseClick)
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("CPU: ${liveCpu.toInt()}%")
                Text("FPS: ${liveFps.toInt()}")
                Text("Latency: ${liveLatencyMs.toInt()} ms")
                stats.forEach { (k, v) -> Text("$k: $v") }
            }
        }
    }
}

@Composable
fun CustomizerStudioScreenView(
    onCloseClick: () -> Unit,
    currentAnimation: String,
    onAnimationChange: (String) -> Unit,
    keyRemapping: Map<String, String>,
    onKeyRemappingChange: (Map<String, String>) -> Unit,
    keycapColor: Map<String, String>,
    onKeycapColorChange: (Map<String, String>) -> Unit
) {
    var activeTab by remember { mutableStateOf("Kinetic Feel") }
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight().consumeClicks().background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)).padding(8.dp)) {
        KeyboardScreenHeader("Customizer Studio", "🎨", onCloseClick)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            listOf("Kinetic Feel", "Key Mapping", "Colorcap").forEach { tab ->
                val selected = activeTab == tab
                Box(modifier = Modifier.weight(1f).clickable { activeTab = tab }.padding(8.dp), contentAlignment = Alignment.Center) {
                    Text(tab, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (activeTab) {
                "Kinetic Feel" -> {
                    listOf("Standard Mechanical", "Linear Pop", "Squishy Jelly", "Bouncy Spring").forEach { anim ->
                        val isSelected = currentAnimation == anim
                        Row(modifier = Modifier.fillMaxWidth().clickable { onAnimationChange(anim) }.padding(8.dp)) {
                            Text(anim, modifier = Modifier.weight(1f))
                            if (isSelected) Text("✅")
                        }
                    }
                }
                "Key Mapping" -> {
                    // Simplified Mapping UI
                    Text("Configured Mappings: ${keyRemapping.size}")
                }
                "Colorcap" -> {
                    // Simplified Colorcap UI
                    Text("Configured Colors: ${keycapColor.size}")
                }
            }
        }
    }
}

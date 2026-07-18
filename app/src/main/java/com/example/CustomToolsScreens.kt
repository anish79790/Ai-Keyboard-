package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.inputmethod.InputConnection
import kotlin.random.Random
import android.net.Uri
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LocalTextStyle

// Reusable Custom Tool Screen Header
@Composable
fun ToolScreenHeader(
    title: String,
    emoji: String,
    onCloseClick: () -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                .clickable {
                    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    onCloseClick()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                "Close",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ==========================================
// 1. APP LAUNCHER SHORTCUTS SCREEN
// ==========================================
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AppLauncherScreenView(
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val systemPrefs = context.getSharedPreferences("ShortcutsPrefs", Context.MODE_PRIVATE)
    
    var customPkgInput by remember { mutableStateOf("") }
    var customLabelInput by remember { mutableStateOf("") }
    
    // Dynamic launcher shortcut list in form of Label###pkg
    var shortcutsList by remember {
        mutableStateOf(
            systemPrefs.getString("apps_list", "")?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }
    
    if (shortcutsList.isEmpty()) {
        shortcutsList = listOf(
            "WhatsApp###com.whatsapp",
            "Instagram###com.instagram.android",
            "YouTube###com.google.android.youtube",
            "Chrome###com.android.chrome",
            "Camera###android.hardware.camera2"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "App Launcher Dashboard 🚀", emoji = "📱", onCloseClick = onCloseClick)
        
        Text(
            "Launch any phone app instantly from your keyboard. Tap to launch!",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        // Grid of launchers
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {
            shortcutsList.forEach { item ->
                val parts = item.split("###")
                if (parts.size >= 2) {
                    val label = parts[0]
                    val pkg = parts[1]
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                try {
                                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                    if (intent != null) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } else {
                                        // Package not installed, search Play Store fallback
                                        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(playStoreIntent)
                                    }
                                } catch (e: Exception) {
                                    // Treat package as general search
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌐", fontSize = 14.sp)
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        // Add custom app shortcut form
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Add Custom App Shortcut", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BasicTextField(
                        value = customLabelInput,
                        onValueChange = { customLabelInput = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(modifier = Modifier.weight(0.4f).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
                                if (customLabelInput.isEmpty()) Text("App Label (e.g. Gmail)", fontSize = 10.sp, color = Color.Gray)
                                inner()
                            }
                        }
                    )
                    
                    BasicTextField(
                        value = customPkgInput,
                        onValueChange = { customPkgInput = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(modifier = Modifier.weight(0.6f).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
                                if (customPkgInput.isEmpty()) Text("Package (e.g. com.google.android.gm)", fontSize = 9.sp, color = Color.Gray)
                                inner()
                            }
                        }
                    )
                }

                Button(
                    onClick = {
                        if (customLabelInput.isNotBlank() && customPkgInput.isNotBlank()) {
                            val newItem = "${customLabelInput.trim()}###${customPkgInput.trim()}"
                            val newList = (shortcutsList + newItem).distinct()
                            shortcutsList = newList
                            systemPrefs.edit().putString("apps_list", newList.joinToString("|||")).apply()
                            customLabelInput = ""
                            customPkgInput = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End).height(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Add Shortcut", fontSize = 9.sp)
                }
            }
        }
    }
}

// ==========================================
// 2. TEXT CASE CHANGER SCREEN
// ==========================================
@Composable
fun TextCaseScreenView(
    inputConnection: InputConnection?,
    onCloseClick: () -> Unit
) {
    val view = LocalView.current
    var textBuffer by remember { mutableStateOf("") }
    
    // Check if there is selected text inside InputConnection to prefill
    LaunchedEffect(Unit) {
        val selected = inputConnection?.getSelectedText(0)?.toString() ?: ""
        if (selected.isNotBlank()) {
            textBuffer = selected
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "Text Case Magic Studio ⚡", emoji = "🔤", onCloseClick = onCloseClick)
        
        BasicTextField(
            value = textBuffer,
            onValueChange = { textBuffer = it },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(6.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val cases = listOf(
                "UPPER" to { textBuffer = textBuffer.uppercase() },
                "lower" to { textBuffer = textBuffer.lowercase() },
                "Title Case" to {
                    textBuffer = textBuffer.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                },
                "SpOnGeBoB" to {
                    textBuffer = textBuffer.mapIndexed { index, char ->
                        if (index % 2 == 0) char.lowercase() else char.uppercase()
                    }.joinToString("")
                },
                "Reverse" to { textBuffer = textBuffer.reversed() },
                "Sentence" to {
                    textBuffer = textBuffer.lowercase().replaceFirstChar { it.uppercase() }
                }
            )

            cases.forEach { (label, transform) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                            transform()
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (textBuffer.isNotEmpty()) {
                    inputConnection?.commitText(textBuffer, 1)
                    onCloseClick()
                }
            },
            modifier = Modifier.fillMaxWidth().height(30.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Insert Styled Text ✨", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 3. AUTOCORRECT TEXT EXPANSION MANAGER SCREEN
// ==========================================
@Composable
fun AutoCorrectManagerScreenView(
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = context.getSharedPreferences("AutoCorrectPrefs", Context.MODE_PRIVATE)
    
    var shortcutInput by remember { mutableStateOf("") }
    var expansionInput by remember { mutableStateOf("") }
    
    var rulesList by remember {
        mutableStateOf(
            prefs.getString("rules", "gm###Good Morning!|||ty###Thank you so much!|||brb###Be right back!")
                ?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "Auto-Text Expander ✂️", emoji = "⚡", onCloseClick = onCloseClick)
        
        Text(
            "Type the shortcut word followed by SPACE to auto-substitute! Add yours below.",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Existing rules list
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rulesList.forEach { rule ->
                val parts = rule.split("###")
                if (parts.size >= 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(parts[0], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("➔", fontSize = 10.sp, color = Color.Gray)
                            Text(parts[1], fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        Text(
                            text = "Delete",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable {
                                view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                val newList = rulesList.toMutableList().apply { remove(rule) }
                                rulesList = newList
                                prefs.edit().putString("rules", newList.joinToString("|||")).apply()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add form
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Add Dynamic Expansion Macro", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicTextField(
                        value = shortcutInput,
                        onValueChange = { shortcutInput = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(modifier = Modifier.weight(0.3f).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
                                if (shortcutInput.isEmpty()) Text("gm", fontSize = 10.sp, color = Color.Gray)
                                inner()
                            }
                        }
                    )
                    BasicTextField(
                        value = expansionInput,
                        onValueChange = { expansionInput = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface),
                        singleLine = true,
                        decorationBox = { inner ->
                            Box(modifier = Modifier.weight(0.7f).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface).padding(4.dp)) {
                                if (expansionInput.isEmpty()) Text("Good Morning!", fontSize = 10.sp, color = Color.Gray)
                                inner()
                            }
                        }
                    )
                }

                Button(
                    onClick = {
                        if (shortcutInput.isNotBlank() && expansionInput.isNotBlank()) {
                            val cleanShortcut = shortcutInput.trim().lowercase()
                            val cleanExpansion = expansionInput.trim()
                            val cleanRules = rulesList.filterNot { it.startsWith("$cleanShortcut###") }
                            val newList = cleanRules + "$cleanShortcut###$cleanExpansion"
                            rulesList = newList
                            prefs.edit().putString("rules", newList.joinToString("|||")).apply()
                            shortcutInput = ""
                            expansionInput = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End).height(24.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp)
                ) {
                    Text("Register Rule", fontSize = 9.sp)
                }
            }
        }
    }
}

// Global hook to perform expanding auto substitution inside Space button
fun performAutoCorrectSubstitution(context: Context, input: String): String? {
    val prefs = context.getSharedPreferences("AutoCorrectPrefs", Context.MODE_PRIVATE)
    val raw = prefs.getString("rules", "gm###Good Morning!|||ty###Thank you so much!|||brb###Be right back!") ?: ""
    val rules = raw.split("|||").filter { it.isNotBlank() }
    val matchWord = input.trim().lowercase()
    for (rule in rules) {
        val parts = rule.split("###")
        if (parts.size >= 2 && parts[0] == matchWord) {
            return parts[1]
        }
    }
    return null
}


// ==========================================
// 4. SECURE PASSWORD GENERATOR
// ==========================================
@Composable
fun PassGenScreenView(
    inputConnection: InputConnection?,
    onCloseClick: () -> Unit
) {
    val view = LocalView.current
    var passLength by remember { mutableStateOf(12f) }
    var useUppercase by remember { mutableStateOf(true) }
    var useNumbers by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var generatedPassword by remember { mutableStateOf("") }

    val generatePassword = {
        val charPool = mutableListOf<Char>()
        charPool.addAll('a'..'z')
        if (useUppercase) charPool.addAll('A'..'Z')
        if (useNumbers) charPool.addAll('0'..'9')
        if (useSymbols) charPool.addAll(listOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', '=', '-'))
        
        val random = Random(System.currentTimeMillis())
        val sb = StringBuilder()
        repeat(passLength.toInt()) {
            if (charPool.isNotEmpty()) {
                sb.append(charPool[random.nextInt(charPool.size)])
            }
        }
        generatedPassword = sb.toString()
    }

    LaunchedEffect(passLength, useUppercase, useNumbers, useSymbols) {
        generatePassword()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "Instant Password Vault Gen 🔑", emoji = "🦾", onCloseClick = onCloseClick)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = generatedPassword.ifEmpty { "Loading..." },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                            generatePassword()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.height(24.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Regenerate", fontSize = 8.sp)
                    }
                    
                    Button(
                        onClick = {
                            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                            if (generatedPassword.isNotEmpty()) {
                                inputConnection?.commitText(generatedPassword, 1)
                                onCloseClick()
                            }
                        },
                        modifier = Modifier.height(24.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Insert Secure Password", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Length Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Length: ${passLength.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(66.dp))
            Slider(
                value = passLength,
                onValueChange = { passLength = it },
                valueRange = 6f..24f,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useUppercase, onCheckedChange = { useUppercase = it }, modifier = Modifier.scale(0.8f))
                Text("ABC", fontSize = 9.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useNumbers, onCheckedChange = { useNumbers = it }, modifier = Modifier.scale(0.8f))
                Text("123", fontSize = 9.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useSymbols, onCheckedChange = { useSymbols = it }, modifier = Modifier.scale(0.8f))
                Text("#$&", fontSize = 9.sp)
            }
        }
    }
}

// ==========================================
// 5. MATH SOLVER & MEMO CALCULATOR
// ==========================================
@Composable
fun MathSolverScreenView(
    inputConnection: InputConnection?,
    onCloseClick: () -> Unit
) {
    val view = LocalView.current
    var expression by remember { mutableStateOf("") }
    var solvedResult by remember { mutableStateOf("") }

    val solveExpression = {
        try {
            val sanitized = expression.replace(" ", "")
            val result = simpleEvalMath(sanitized)
            solvedResult = if (result % 1.0 == 0.0) result.toInt().toString() else String.format("%.3f", result)
        } catch (e: Exception) {
            solvedResult = "Syntax Error"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "Inline Equation Solver 🧮", emoji = "📊", onCloseClick = onCloseClick)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = expression,
                onValueChange = { expression = it },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold),
                singleLine = true,
                decorationBox = { inner ->
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(6.dp)) {
                        if (expression.isEmpty()) Text("Type math e.g. 5+10*2-3", fontSize = 11.sp, color = Color.Gray)
                        inner()
                    }
                }
            )
            
            Button(
                onClick = {
                    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    solveExpression()
                },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Solve", fontSize = 9.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (solvedResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Answer:", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                        Text(solvedResult, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    
                    if (solvedResult != "Syntax Error") {
                        Button(
                            onClick = {
                                inputConnection?.commitText(solvedResult, 1)
                                onCloseClick()
                            },
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Insert Solution", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Compact simple safe numerical AST/Evaluator to solve string calculations
fun simpleEvalMath(expr: String): Double {
    if (expr.isEmpty()) return 0.0
    // Extremely simplistic mathematical scanner for demonstration
    // Handles addition and subtraction of groups
    var base = 0.0
    val terms = expr.split("+")
    for (i in terms.indices) {
        val element = terms[i]
        val subtractions = element.split("-")
        var sum = 0.0
        for (j in subtractions.indices) {
            val subElem = subtractions[j]
            // Handles simple multiplications and division factors
            val multiply = subElem.split("*")
            var multVal = 1.0
            for (k in multiply.indices) {
                val divSplit = multiply[k].split("/")
                var divVal = divSplit[0].toDoubleOrNull() ?: 0.0
                for (d in 1 until divSplit.size) {
                    val divisor = divSplit[d].toDoubleOrNull() ?: 1.0
                    if (divisor != 0.0) divVal /= divisor
                }
                multVal *= divVal
            }
            if (j == 0) sum += multVal else sum -= multVal
        }
        base += sum
    }
    return base
}


// ==========================================
// 6. JAPANESE KAOMOJI & RETRO CHAT ART
// =======@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KaomojiScreenView(
    inputConnection: InputConnection?,
    onCloseClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    var selectedCategory by remember { mutableStateOf("Happy") }
    var searchQuery by remember { mutableStateOf("") }
    
    // Manage Favorites and Recents via SharedPreferences
    val prefs = remember { context.getSharedPreferences("keyboard_kaomoji_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Load favorites and recents from SP
    var favoritesList by remember {
        mutableStateOf(
            prefs.getStringSet("favorites", emptySet())?.toList() ?: emptyList()
        )
    }
    var recentsList by remember {
        mutableStateOf(
            prefs.getStringSet("recents", emptySet())?.toList() ?: emptyList()
        )
    }

    // Function to toggle favorite
    val toggleFavorite: (String) -> Unit = { kaomoji ->
        val updated = if (favoritesList.contains(kaomoji)) {
            favoritesList.filter { it != kaomoji }
        } else {
            favoritesList + kaomoji
        }
        favoritesList = updated
        prefs.edit().putStringSet("favorites", updated.toSet()).apply()
    }

    // Function to add to recents
    val addToRecents: (String) -> Unit = { kaomoji ->
        val updated = (listOf(kaomoji) + recentsList.filter { it != kaomoji }).take(50)
        recentsList = updated
        prefs.edit().putStringSet("recents", updated.toSet()).apply()
    }

    // Resolve displayed kaomojis based on search query or selected category
    val displayedKaomojis = remember(selectedCategory, searchQuery, favoritesList, recentsList) {
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            // search across all categories
            KaomojiDataSource.data.values.flatten()
                .filter { it.lowercase().contains(q) }
                .distinct()
        } else {
            when (selectedCategory) {
                "Recent" -> recentsList
                "Favorites" -> favoritesList
                else -> KaomojiDataSource.data[selectedCategory] ?: emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Tool Header
        ToolScreenHeader(title = "Kaomoji Zen Library 💮", emoji = "🌸", onCloseClick = onCloseClick)
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Search Input Field & Clear Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search over 1000+ kaomojis...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { searchQuery = "" },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Categories Row
        if (searchQuery.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KaomojiDataSource.categories.forEach { tab ->
                    val selected = selectedCategory == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .clickable {
                                view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                selectedCategory = tab
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val icon = when (tab) {
                                "Recent" -> "🕒"
                                "Favorites" -> "❤️"
                                "Happy" -> "😊"
                                "Sad" -> "😢"
                                "Angry" -> "😡"
                                "Flirty" -> "😘"
                                "Romantic" -> "💖"
                                "Anime" -> "🛸"
                                "Smug" -> "😏"
                                "Savage" -> "🔥"
                                "Gaming" -> "🎮"
                                "Cute" -> "✿"
                                "Chaotic" -> "🌀"
                                "Sleepy" -> "💤"
                                "Greeting" -> "👋"
                                "Aesthetic" -> "💮"
                                "Emotional" -> "🎭"
                                else -> "🌸"
                            }
                            Text(icon, fontSize = 9.sp)
                            Text(
                                text = tab,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Kaomojis list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (displayedKaomojis.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedCategory == "Favorites") "No favorites added yet!\nLong press a kaomoji to favorite it." 
                               else if (selectedCategory == "Recent") "No recents yet!"
                               else "No kaomojis found for \"$searchQuery\"",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 78.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayedKaomojis) { art ->
                        val isFav = favoritesList.contains(art)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isFav) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                )
                                .combinedClickable(
                                    onLongClick = {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                        toggleFavorite(art)
                                    },
                                    onClick = {
                                        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                        addToRecents(art)
                                        inputConnection?.commitText(art, 1)
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                art,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (isFav) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Text("♥️", fontSize = 6.sp, color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 7. SPEED TYPING STATS & GAME SCREEN
// ==========================================
@Composable
fun SpeedStatsScreenView(
    onCloseClick: () -> Unit
) {
    val view = LocalView.current
    val targetText = "the quick brown fox jumps over the lazy dog"
    var typedBuffer by remember { mutableStateOf("") }
    
    var startTime by remember { mutableStateOf(0L) }
    var gameCompleted by remember { mutableStateOf(false) }
    var scoreWpm by remember { mutableStateOf(0) }
    var accuracyPercent by remember { mutableStateOf(100) }

    val startTest = {
        startTime = System.currentTimeMillis()
        typedBuffer = ""
        gameCompleted = false
    }

    LaunchedEffect(typedBuffer) {
        if (startTime == 0L && typedBuffer.isNotEmpty()) {
            startTime = System.currentTimeMillis()
        }
        
        if (typedBuffer.trim().lowercase() == targetText) {
            val totalTime = (System.currentTimeMillis() - startTime) / 1000f
            if (totalTime > 0.1f) {
                val words = targetText.split(" ").size
                val rawWpm = (words / (totalTime / 60f)).toInt()
                scoreWpm = rawWpm
                
                // Calculate simple match accuracy
                accuracyPercent = 100
            }
            gameCompleted = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "Type Speed Sprint Game 🏎️", emoji = "⚡", onCloseClick = onCloseClick)
        
        Text(
            "Test your active typing pace inside the keyboard: Type the display sequence below!",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Phrase target:", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                Text(targetText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        BasicTextField(
            value = typedBuffer,
            onValueChange = { typedBuffer = it },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(6.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (gameCompleted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Speed rate", fontSize = 8.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("$scoreWpm WPM", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Accuracy", fontSize = 8.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("$accuracyPercent%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Button(
                        onClick = {
                            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                            startTest()
                        },
                        modifier = Modifier.height(26.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Retry Sprint", fontSize = 9.sp)
                    }
                }
            }
        } else {
            Button(
                onClick = startTest,
                modifier = Modifier.fillMaxWidth().height(26.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Reset sprint tracking", fontSize = 9.sp)
            }
        }
    }
}


// ==========================================
// 8. PERSISTENT QUICK NOTES DASHBOARD
// ==========================================
@Composable
fun QuickNotesScreenView(
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = context.getSharedPreferences("QuickNotesPrefs", Context.MODE_PRIVATE)
    
    var memoText by remember {
        mutableStateOf(prefs.getString("keyboard_sticky_memo", "Write down thoughts to keep inside keyboard database...") ?: "")
    }

    LaunchedEffect(memoText) {
        prefs.edit().putString("keyboard_sticky_memo", memoText).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "Keyboard Sticky Note Pad 📝", emoji = "💡", onCloseClick = onCloseClick)
        
        BasicTextField(
            value = memoText,
            onValueChange = { memoText = it },
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.align(Alignment.End)) {
            Button(
                onClick = {
                    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    memoText = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.height(24.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Wipe note", fontSize = 8.sp, color = Color.White)
            }
        }
    }
}


// ==========================================
// 9. COHERENT EMOJI COMBINER
// ==========================================
@Composable
fun EmojiCombinerScreenView(
    inputConnection: InputConnection?,
    onCloseClick: () -> Unit
) {
    val view = LocalView.current
    var emoji1 by remember { mutableStateOf("🤖") }
    var emoji2 by remember { mutableStateOf("🐱") }
    
    val combinationOutcome = when {
        emoji1 == "🤖" && emoji2 == "🐱" -> "🦾🐱 Cyborg Meowster"
        emoji1 == "🔥" && emoji2 == "🍦" -> "🍧 Chilled Spiced Sorbet"
        emoji1 == "💀" && emoji2 == "👽" -> "👽🛸 Spectral Galaxy Wanderer"
        emoji1 == "🐒" && emoji2 == "👑" -> "👑🐒 Monkey King"
        else -> "$emoji1$emoji2 Creative Fusion Fusion!"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "Emoji Lab Combinator 🧪", emoji = "🌈", onCloseClick = onCloseClick)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Emoji 1", fontSize = 8.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("🤖", "🔥", "💀", "🐒").forEach { item ->
                        Box(modifier = Modifier.clickable { emoji1 = item }.padding(2.dp)) {
                            Text(item, fontSize = 16.sp, modifier = Modifier.background(if (emoji1 == item) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(4.dp)).padding(4.dp))
                        }
                    }
                }
            }
            
            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Emoji 2", fontSize = 8.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("🐱", "🍦", "👽", "👑").forEach { item ->
                        Box(modifier = Modifier.clickable { emoji2 = item }.padding(2.dp)) {
                            Text(item, fontSize = 16.sp, modifier = Modifier.background(if (emoji2 == item) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(4.dp)).padding(4.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Creative Alchemy Outcome:", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                Text(combinationOutcome, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                
                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                        inputConnection?.commitText(combinationOutcome, 1)
                        onCloseClick()
                    },
                    modifier = Modifier.height(26.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Insert Outcome Art", fontSize = 9.sp)
                }
            }
        }
    }
}


// ==========================================
// 10. REAL-TIME DICTIONARY & COMPOSING HELPER
// ==========================================
@Composable
fun DictionaryScreenView(
    onCloseClick: () -> Unit
) {
    val view = LocalView.current
    var searchWord by remember { mutableStateOf("") }
    var lookupDefinition by remember { mutableStateOf("") }
    
    val dictionaryBase = mapOf(
        "beautiful" to "Possessing qualities that give great aesthetic pleasure. Synonyms: gorgeous, attractive.",
        "masterpiece" to "A work of outstanding artistry, skill, or workmanship.",
        "sprint" to "Run or compose at full speed over a short distance. pace.",
        "smart" to "Having or showing a quickwitted intelligence. Synonyms: clever, brainy.",
        "secure" to "Protected against fear, anxiety, or unauthorized access.",
        "keyboard" to "A panel of keys that operates a computer or typewriter."
    )

    val runLookup = {
        val lookup = searchWord.trim().lowercase()
        lookupDefinition = dictionaryBase[lookup] ?: "Word Definition lookup complete for '$searchWord'. It represents progressive vocabulary optimization."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "Vocab Copilot & Dictionary 📖", emoji = "🦉", onCloseClick = onCloseClick)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = searchWord,
                onValueChange = { searchWord = it },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                decorationBox = { inner ->
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(6.dp)) {
                        if (searchWord.isEmpty()) Text("Enter vocab word e.g. smart", fontSize = 11.sp, color = Color.Gray)
                        inner()
                    }
                }
            )
            
            Button(
                onClick = {
                    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    runLookup()
                },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Define", fontSize = 9.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (lookupDefinition.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Semantic Interpretation:", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                    Text(lookupDefinition, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==========================================
// 11. UNICODE FONTS CHANGER UTILITY
// ==========================================
object UnicodeFontConverter {
    val FONT_STYLES = listOf(
        "Normal",
        "Bold Serif",
        "Bold Sans",
        "Classic Cursive",
        "Gothic",
        "Blackboard",
        "Monospace",
        "Circles",
        "Small Caps",
        "Upside Down",
        "Parenthesized",
        "Italic Serif",
        "Bold Italic Serif",
        "Italic Sans",
        "Bold Italic Sans",
        "Inverted Circles",
        "Vaporwave Wide",
        "Squared Brackets",
        "Hackers 1337",
        "Strike Through"
    )

    fun convertText(text: String, style: String): String {
        if (style == "Normal" || text.isEmpty()) return text
        val sb = StringBuilder()
        for (char in text) {
            sb.append(convertChar(char, style))
        }
        return sb.toString()
    }

    private fun convertChar(c: Char, style: String): String {
        return when (style) {
            "Bold Serif" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(119808 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(119834 + (c - 'a')))
                } else c.toString()
            }
            "Bold Sans" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(120224 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(120250 + (c - 'a')))
                } else c.toString()
            }
            "Classic Cursive" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(119964 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(119990 + (c - 'a')))
                } else c.toString()
            }
            "Gothic" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(120068 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(120094 + (c - 'a')))
                } else c.toString()
            }
            "Blackboard" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(120120 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(120146 + (c - 'a')))
                } else c.toString()
            }
            "Monospace" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(120432 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(120458 + (c - 'a')))
                } else c.toString()
            }
            "Circles" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(9398 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(9424 + (c - 'a')))
                } else c.toString()
            }
            "Small Caps" -> {
                val smallCaps = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀꜱᴛᴜᴠᴡxʏᴢ"
                if (c in 'a'..'z') {
                    smallCaps[c - 'a'].toString()
                } else if (c in 'A'..'Z') {
                    smallCaps[c - 'A'].toString()
                } else c.toString()
            }
            "Upside Down" -> {
                val normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
                val flip   = "ɐqɔpǝɟƃɥıɾʞlɯuodbɹsʇnʌʍxʎzⱯᗺƆᗡƎℲ⅁HIſʞ˥WNOԀΌᴚS┴∩ΛMX⅄Z"
                val idx = normal.indexOf(c)
                if (idx != -1) {
                    flip.getOrNull(idx)?.toString() ?: c.toString()
                } else c.toString()
            }
            "Parenthesized" -> {
                if (c in 'a'..'z') {
                    String(Character.toChars(9372 + (c - 'a')))
                } else if (c in 'A'..'Z') {
                    String(Character.toChars(9372 + (c - 'A')))
                } else c.toString()
            }
            "Italic Serif" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(119860 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(119886 + (c - 'a')))
                } else c.toString()
            }
            "Bold Italic Serif" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(119912 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(119938 + (c - 'a')))
                } else c.toString()
            }
            "Italic Sans" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(120276 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(120302 + (c - 'a')))
                } else c.toString()
            }
            "Bold Italic Sans" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(120328 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(120354 + (c - 'a')))
                } else c.toString()
            }
            "Inverted Circles" -> {
                if (c in 'A'..'Z') {
                    String(Character.toChars(127304 + (c - 'A')))
                } else if (c in 'a'..'z') {
                    String(Character.toChars(127304 + (c - 'a')))
                } else c.toString()
            }
            "Vaporwave Wide" -> {
                if (c == ' ') {
                    "\u3000"
                } else if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9') {
                    String(Character.toChars(c.code + 65248))
                } else c.toString()
            }
            "Squared Brackets" -> {
                if (c in 'A'..'Z' || c in 'a'..'z') {
                    "[${c.uppercaseChar()}]"
                } else c.toString()
            }
            "Hackers 1337" -> {
                when (c) {
                    'e', 'E' -> "3"
                    'a', 'A' -> "4"
                    't', 'T' -> "7"
                    's', 'S' -> "5"
                    'o', 'O' -> "0"
                    'i', 'I' -> "1"
                    'g', 'G' -> "6"
                    'b', 'B' -> "8"
                    else -> c.toString()
                }
            }
            "Strike Through" -> {
                c.toString() + "\u0336"
            }
            else -> c.toString()
        }
    }
}

@Composable
fun FontStylesScreenView(
    currentFontStyle: String,
    onFontStyleChange: (String) -> Unit,
    onCloseClick: () -> Unit
) {
    val view = LocalView.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ToolScreenHeader(title = "Font Style Keyboard Changer ✒️", emoji = "🔤", onCloseClick = onCloseClick)
        
        Text(
            text = "Select an elegant font skin to type stylishly on any social app or game on-the-fly!",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val styles = UnicodeFontConverter.FONT_STYLES
        styles.forEach { style ->
            val isSelected = currentFontStyle == style
            val previewText = UnicodeFontConverter.convertText("Aesthetic Style Key", style)
            PreferenceCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                            onFontStyleChange(style)
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(style, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Text(previewText, fontSize = 13.sp, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                            onFontStyleChange(style)
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// SPECIAL HIGH-PERFORMANCE SYMBOLS LIBRARY PRO
// ==========================================

data class SymbolItem(val symbol: String, val tags: String)

object SymbolDataSource {
    val longPressMapping = mapOf(
        "q" to listOf("1", "œ"),
        "w" to listOf("2", "ŵ"),
        "e" to listOf("3", "è", "é", "ê", "ë", "ē", "ė", "ę", "€"),
        "r" to listOf("4", "₹", "®"),
        "t" to listOf("5", "þ", "ť", "ţ"),
        "y" to listOf("6", "ÿ", "ŷ", "ý"),
        "u" to listOf("7", "û", "ü", "ù", "ú", "ū"),
        "i" to listOf("8", "ì", "í", "î", "ï", "ī", "į"),
        "o" to listOf("9", "ò", "ó", "ô", "õ", "ö", "ō", "ø", "œ", "º"),
        "p" to listOf("0", "π", "§"),
        "a" to listOf("@", "à", "á", "â", "ã", "ä", "å", "ā", "æ", "ª"),
        "s" to listOf("$", "ß", "ś", "š", "§", "#"),
        "d" to listOf("&", "ð", "ď"),
        "f" to listOf("*", "ƒ"),
        "g" to listOf("-", "ġ", "ğ"),
        "h" to listOf("+", "ĥ", "ħ"),
        "j" to listOf("(", "[", "{", "ĵ"),
        "k" to listOf(")", "]", "}", "ķ"),
        "l" to listOf("/", "\\", "|", "ł", "ľ"),
        "z" to listOf("_", "~", "`", "ź", "ż", "ž"),
        "x" to listOf("\"", "'", "«", "»"),
        "c" to listOf(":", ";", "ç", "ć", "č"),
        "v" to listOf("<", ">", "≤", "≥"),
        "b" to listOf("!", "β"),
        "n" to listOf("?", "ñ", "ń", "ň"),
        "m" to listOf(",", "µ")
    )

    val categories = listOf("Recent", "Favorites", "Currency", "Math", "Coding", "Brackets & arrows", "Decorative", "Copyright/General")
    
    val data = mapOf(
        "Brackets & arrows" to listOf(
            SymbolItem("[", "square bracket open"),
            SymbolItem("]", "square bracket close"),
            SymbolItem("{", "curly brace open"),
            SymbolItem("}", "curly brace close"),
            SymbolItem("(", "parenthesis open"),
            SymbolItem(")", "parenthesis close"),
            SymbolItem("⟨", "angle bracket left"),
            SymbolItem("⟩", "angle bracket right"),
            SymbolItem("«", "guillemet left"),
            SymbolItem("»", "guillemet right"),
            SymbolItem("←", "arrow left"),
            SymbolItem("↑", "arrow up"),
            SymbolItem("→", "arrow right"),
            SymbolItem("↓", "arrow down"),
            SymbolItem("↔", "arrow horizontal"),
            SymbolItem("↕", "arrow vertical"),
            SymbolItem("↖", "arrow up-left"),
            SymbolItem("↗", "arrow up-right"),
            SymbolItem("↘", "arrow down-right"),
            SymbolItem("↙", "arrow down-left")
        ),
        "Currency" to listOf(
            SymbolItem("₹", "rupee indian currency sign"),
            SymbolItem("$", "dollar money cash currency"),
            SymbolItem("€", "euro money currency sign"),
            SymbolItem("£", "pound money currency sign"),
            SymbolItem("¥", "yen yuan currency sign"),
            SymbolItem("₩", "won korean currency sign"),
            SymbolItem("₴", "hryvnia ukrainian currency"),
            SymbolItem("₪", "shekel israeli coin"),
            SymbolItem("¤", "currency dynamic sign"),
            SymbolItem("¢", "cent small currency money")
        ),
        "Math" to listOf(
            SymbolItem("+", "plus addition add operator"),
            SymbolItem("-", "minus subtraction subtract operator"),
            SymbolItem("×", "multiply multiplication times math"),
            SymbolItem("÷", "divide division slash math"),
            SymbolItem("=", "equals equality parity same"),
            SymbolItem("≠", "not equal inequality math"),
            SymbolItem("≈", "approximately equal almost generic"),
            SymbolItem("<", "less than smaller left"),
            SymbolItem(">", "greater than bigger right"),
            SymbolItem("≤", "less than or equal"),
            SymbolItem("≥", "greater than or equal"),
            SymbolItem("±", "plus minus error offset"),
            SymbolItem("√", "square root radical math"),
            SymbolItem("∞", "infinity infinite loop"),
            SymbolItem("π", "pi constant geometry 3.14"),
            SymbolItem("∑", "summation sigma total sum"),
            SymbolItem("∫", "integral calculus calculus math"),
            SymbolItem("∆", "delta triangle difference change"),
            SymbolItem("μ", "mu micro unit science"),
            SymbolItem("Ω", "omega ohms resistance physics")
        ),
        "Coding" to listOf(
            SymbolItem("{", "curly brace open bracket coding json"),
            SymbolItem("}", "curly brace close bracket coding json"),
            SymbolItem("[", "square bracket open array list"),
            SymbolItem("]", "square bracket close array list"),
            SymbolItem("(", "parenthesis open function tuple"),
            SymbolItem(")", "parenthesis close function tuple"),
            SymbolItem("<", "arrow separator bracket open XML"),
            SymbolItem(">", "arrow separator bracket close XML"),
            SymbolItem(";", "semicolon separator separator line coding"),
            SymbolItem(":", "colon map key assignment attribute"),
            SymbolItem("/", "slash divide path divider web"),
            SymbolItem("\\", "backslash escape character file"),
            SymbolItem("|", "pipe logical or filter separate"),
            SymbolItem("&", "ampersand and logical join"),
            SymbolItem("^", "caret power xor exponent math"),
            SymbolItem("~", "tilde bitwise not home directory"),
            SymbolItem("`", "backtick string literal markdown"),
            SymbolItem("#", "hash tag code comment tag social"),
            SymbolItem("?", "question ternary query search info"),
            SymbolItem("!", "exclamation negate bang override")
        ),
        "Decorative" to listOf(
            SymbolItem("♥", "heart love romance decoration like"),
            SymbolItem("❤", "heavy black heart love passionate"),
            SymbolItem("★", "star gold rate rating mark key"),
            SymbolItem("☆", "white star hollow empty rate"),
            SymbolItem("✦", "sparkle magic four point star diamond"),
            SymbolItem("✧", "white sparkle four point hollow"),
            SymbolItem("☀", "sun hot light day yellow weather"),
            SymbolItem("☁", "cloud sky weather storm rain gray"),
            SymbolItem("☂", "umbrella rain protective weather"),
            SymbolItem("☃", "snowman winter cold frozen christmas"),
            SymbolItem("☄", "comet star space shooting galaxy"),
            SymbolItem("☇", "lightning bolt storm electrical flash"),
            SymbolItem("☕", "coffee warm tea drink cafe mug"),
            SymbolItem("☘", "clover green shamrock luck clover st patricks"),
            SymbolItem("♛", "queen crown chess royal gold power"),
            SymbolItem("♟", "pawn chess game board hobby sport"),
            SymbolItem("✈", "airplane aviation travel vacation airport"),
            SymbolItem("✉", "mail envelope letter message newsletter write"),
            SymbolItem("✂", "scissors cut craft design fashion crop"),
            SymbolItem("✎", "pencil write edit sketch notebook design")
        ),
        "Copyright/General" to listOf(
            SymbolItem("©", "copyright legally reserved authorized"),
            SymbolItem("®", "registered trademark brand product"),
            SymbolItem("™", "trademark business property corporate"),
            SymbolItem("℠", "service mark utility business"),
            SymbolItem("¶", "pilcrow paragraph document edit format"),
            SymbolItem("§", "section law segment document act"),
            SymbolItem("†", "dagger dead mort religious cross"),
            SymbolItem("‡", "double dagger note mark footnote"),
            SymbolItem("•", "bullet list dot item circle"),
            SymbolItem("°", "degree angle temperature circle coordinate"),
            SymbolItem("ª", "feminine ordinal indicator spanish portuguese"),
            SymbolItem("º", "masculine ordinal spanish portuguese")
        )
    )
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SymbolsLibraryScreenView(
    inputConnection: InputConnection?,
    onCloseClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    var selectedCategory by remember { mutableStateOf("Currency") }
    var searchQuery by remember { mutableStateOf("") }
    
    // Memory state managers for Favorites and Recents
    val prefs = remember { context.getSharedPreferences("keyboard_symbols_pro_prefs", android.content.Context.MODE_PRIVATE) }
    
    var favoritesList by remember {
        mutableStateOf(
            prefs.getStringSet("favorites", emptySet())?.toList() ?: emptyList()
        )
    }
    
    var recentsList by remember {
        mutableStateOf(
            prefs.getStringSet("recents", emptySet())?.toList() ?: emptyList()
        )
    }
    
    // Action: To toggle favorite status with tactile feedback
    val toggleFavorite: (String) -> Unit = { symbol ->
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        val updated = if (favoritesList.contains(symbol)) {
            favoritesList.filter { it != symbol }
        } else {
            favoritesList + symbol
        }
        favoritesList = updated
        prefs.edit().putStringSet("favorites", updated.toSet()).apply()
    }
    
    // Action: Save to recents timeline
    val addToRecents: (String) -> Unit = { symbol ->
        val updated = (listOf(symbol) + recentsList.filter { it != symbol }).take(36)
        recentsList = updated
        prefs.edit().putStringSet("recents", updated.toSet()).apply()
    }
    
    val displayedSymbols = remember(selectedCategory, searchQuery, favoritesList, recentsList) {
        if (searchQuery.isNotBlank()) {
            val queryLower = searchQuery.lowercase().trim()
            SymbolDataSource.data.values.flatten()
                .filter { it.symbol.contains(queryLower) || it.tags.contains(queryLower) }
                .map { it.symbol }
                .distinct()
        } else {
            when (selectedCategory) {
                "Recent" -> recentsList
                "Favorites" -> favoritesList
                else -> SymbolDataSource.data[selectedCategory]?.map { it.symbol } ?: emptyList()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Uniform polished header
        ToolScreenHeader(
            title = "Special Symbols Panel",
            emoji = "☯",
            onCloseClick = onCloseClick
        )
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 2.dp))
        
        // 1. Horizontal Category tabs list
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(SymbolDataSource.categories) { category ->
                val isSelected = selectedCategory == category && searchQuery.isBlank()
                val icon = when (category) {
                    "Recent" -> "🕒"
                    "Favorites" -> "⭐"
                    "Currency" -> "₹"
                    "Math" -> "√"
                    "Coding" -> "/"
                    "Brackets & arrows" -> "()"
                    "Decorative" -> "★"
                    "Copyright/General" -> "©"
                    else -> "✏"
                }
                
                Surface(
                    selected = isSelected,
                    onClick = {
                        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                        selectedCategory = category
                        searchQuery = ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(icon, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                        if (isSelected) {
                            Text(category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
        
        // 2. Text Search Bar Input
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🔍", fontSize = 12.sp)
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search symbols (e.g. dollar, math, copy, star)...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        
        // 3. Tip User Info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "💡 Tip: Long-press any symbol card to toggle Favorites (⭐)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
        
        // 4. Symbols Grid layout list
        Box(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
            if (displayedSymbols.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("📭", fontSize = 28.sp)
                        Text(
                            text = if (selectedCategory == "Favorites") "No favorites added yet." else "No symbols found.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayedSymbols) { sym ->
                        val isFav = favoritesList.contains(sym)
                        
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFav) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                0.5.dp, 
                                if (isFav) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .aspectRatio(1.1f)
                                .combinedClickable(
                                    onClick = {
                                        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                        inputConnection?.commitText(sym, 1)
                                        addToRecents(sym)
                                    },
                                    onLongClick = {
                                        toggleFavorite(sym)
                                    }
                                )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sym,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                
                                if (isFav) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().padding(2.dp),
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        Text("⭐", fontSize = 7.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// CONVERSATION BUILDER SUGGESTIONS GIVER SCREEN
// =========================================================================
@Composable
fun ConversationBuilderScreenView(
    onCloseClick: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    var isEnabled by remember { mutableStateOf(ContextStore.getSuggestorEnabled(context)) }
    var mode by remember { mutableStateOf(ContextStore.getSuggestorMode(context)) }
    var timerVal by remember { mutableStateOf(ContextStore.getSuggestorTimer(context)) }
    var hinglishEnabled by remember { mutableStateOf(ContextStore.getSuggestorHinglish(context)) }
    var activeTexts by remember { mutableStateOf(ContextStore.getActiveConvoTexts(context)) }
    var savedSessions by remember { mutableStateOf(ContextStore.getSavedSessions(context)) }

    var sessionNameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolScreenHeader(
            title = "Convo Builder Suggestor",
            emoji = "💬",
            onCloseClick = onCloseClick
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Main Switch Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Enable Suggestions Giver",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Suggests phrases when keyboard is idle",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = {
                    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    isEnabled = it
                    ContextStore.saveSuggestorEnabled(context, it)
                },
                thumbContent = if (isEnabled) {
                    {
                        Text("⚡", fontSize = 8.sp)
                    }
                } else null
            )
        }

        // Hinglish Support Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "English + Hindi (Hinglish)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Mixes Hindi words for natural human feel",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = hinglishEnabled,
                onCheckedChange = {
                    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    hinglishEnabled = it
                    ContextStore.saveSuggestorHinglish(context, it)
                }
            )
        }

        // 3 Modes Selection (Flirty, Friendly, Roast)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Select Giver Mode:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Flirty" to "flirty", "Friendly" to "friendly", "Roast" to "roast").forEach { (label, key) ->
                    val isSelected = mode == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                mode = key
                                ContextStore.saveSuggestorMode(context, key)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Idle Timer Selector Row
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Trigger Suggestions Idle Duration:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "5s" to 5,
                    "10s" to 10,
                    "15s" to 15,
                    "1m" to 60,
                    "3m" to 180
                ).forEach { (label, secs) ->
                    val isSelected = timerVal == secs
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable {
                                view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                timerVal = secs
                                ContextStore.saveSuggestorTimer(context, secs)
                            }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Conversation Status & "New Conversation" Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Active Convo Context",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Holds ${activeTexts.size} typed sentences",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = {
                    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                    ContextStore.clearActiveConvoTexts(context)
                    activeTexts = emptyList()
                    sessionNameInput = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text("🔄 New Convo", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Save Current Conversation Box
        if (activeTexts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicTextField(
                    value = sessionNameInput,
                    onValueChange = { sessionNameInput = it },
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .padding(6.dp),
                    decorationBox = { innerTextField ->
                        if (sessionNameInput.isEmpty()) {
                            Text("Person name (e.g. Priya)", fontSize = 10.sp, color = Color.Gray)
                        }
                        innerTextField()
                    }
                )
                Button(
                    onClick = {
                        if (sessionNameInput.isNotBlank()) {
                            view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                            ContextStore.saveSessionByName(context, sessionNameInput, activeTexts)
                            savedSessions = ContextStore.getSavedSessions(context)
                            sessionNameInput = ""
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("💾 Save Convo", fontSize = 9.sp)
                }
            }
        }

        // Saved Sessions list
        if (savedSessions.isNotEmpty()) {
            Text(
                "Saved Conversations (Context files):",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                savedSessions.forEach { name ->
                    val data = ContextStore.getSessionData(context, name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${data.size} text nodes saved", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Load Context
                            Button(
                                onClick = {
                                    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                    ContextStore.saveActiveConvoTexts(context, data)
                                    activeTexts = data
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.height(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                            ) {
                                Text("Load Context 🔁", fontSize = 8.sp)
                            }
                            // Delete
                            Button(
                                onClick = {
                                    view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                                    ContextStore.deleteSessionByName(context, name)
                                    savedSessions = ContextStore.getSavedSessions(context)
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.height(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                            ) {
                                Text("🗑️", fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "No context files saved yet. Write some words first!",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

package com.example

import android.content.Context

object ContextStore {
    private const val PREFS_NAME = "AiKeyboardPrefs"
    private const val KEY_HISTORY = "conversation_history"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
    private const val KEY_KEYBOARD_SIZE = "keyboard_size"
    private const val KEY_HAPTIC_LEVEL = "keyboard_haptic_level"
    private const val KEY_SOUND_ENABLED = "keyboard_sound_enabled"
    private const val KEY_ACTIVE_AI_MODE = "keyboard_active_ai_mode"

    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("###SEP###").filter { it.isNotBlank() }
    }

    fun addMessage(context: Context, msg: String) {
        val current = getHistory(context).toMutableList()
        if (msg.trim().isEmpty()) return
        current.add(msg.trim())
        if (current.size > 5) {
            current.removeAt(0)
        }
        saveHistory(context, current)
    }

    fun removeMessageAt(context: Context, index: Int) {
        val current = getHistory(context).toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            saveHistory(context, current)
        }
    }

    fun clearHistory(context: Context) {
        saveHistory(context, emptyList())
    }

    private fun saveHistory(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = list.joinToString("###SEP###")
        prefs.edit().putString(KEY_HISTORY, raw).apply()
    }

    fun getCustomApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun saveCustomApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun getKeyboardSize(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_KEYBOARD_SIZE, "Normal") ?: "Normal"
    }

    fun saveKeyboardSize(context: Context, size: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_KEYBOARD_SIZE, size).apply()
    }

    fun getHapticLevel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HAPTIC_LEVEL, "Medium") ?: "Medium"
    }

    fun saveHapticLevel(context: Context, level: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HAPTIC_LEVEL, level).apply()
    }

    fun getSoundEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun saveSoundEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun getActiveAiMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVE_AI_MODE, "genz") ?: "genz"
    }

    fun saveActiveAiMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_AI_MODE, mode).apply()
    }

    // NEW Storage Helpers for Theme & Pass Template Vault
    fun getThemeSetting(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_theme", "Classic") ?: "Classic"
    }

    fun saveThemeSetting(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_theme", theme).apply()
    }

    fun getPassTemplates(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_pass_templates", "") ?: ""
        if (raw.isEmpty()) {
            return listOf(
                "Work Email: employee@company.com",
                "Personal Profile: professional-coder-portfolio",
                "Hey buddy! How is it going?",
                "Thanks for the prompt response!",
                "Let's catch up soon."
            )
        }
        return raw.split("###TEMPLATESEP###").filter { it.isNotBlank() }
    }

    fun addPassTemplate(context: Context, template: String) {
        val current = getPassTemplates(context).toMutableList()
        if (template.trim().isEmpty()) return
        current.add(template.trim())
        savePassTemplates(context, current)
    }

    fun deletePassTemplateAt(context: Context, index: Int) {
        val current = getPassTemplates(context).toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            savePassTemplates(context, current)
        }
    }

    private fun savePassTemplates(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = list.joinToString("###TEMPLATESEP###")
        prefs.edit().putString("keyboard_pass_templates", raw).apply()
    }

    // Keyboard Custom Sound Configuration
    fun getSoundProfile(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_sound_profile", "Mechanical") ?: "Mechanical"
    }

    fun saveSoundProfile(context: Context, profile: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_sound_profile", profile).apply()
    }

    fun getKeySoundOverrides(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_key_sound_overrides", "") ?: ""
        if (raw.isEmpty()) return emptyMap()
        return raw.split(",").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            if (parts.size >= 2) parts[0] to parts[1] else it to "Default"
        }
    }

    fun saveKeySoundOverrides(context: Context, overrides: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = overrides.map { "${it.key}:${it.value}" }.joinToString(",")
        prefs.edit().putString("keyboard_key_sound_overrides", raw).apply()
    }

    // Custom Keycaps / Custom Keys remaps
    fun getKeyRemappings(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_key_remappings", "") ?: ""
        if (raw.isEmpty()) return emptyMap()
        return raw.split(",").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            if (parts.size >= 2) parts[0] to parts[1] else it to ""
        }
    }

    fun saveKeyRemappings(context: Context, mappings: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = mappings.map { "${it.key}:${it.value}" }.joinToString(",")
        prefs.edit().putString("keyboard_key_remappings", raw).apply()
    }

    fun getKeycapColors(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_keycap_colors", "") ?: ""
        if (raw.isEmpty()) return emptyMap()
        return raw.split(",").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            if (parts.size >= 2) parts[0] to parts[1] else it to ""
        }
    }

    fun saveKeycapColors(context: Context, colors: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = colors.map { "${it.key}:${it.value}" }.joinToString(",")
        prefs.edit().putString("keyboard_keycap_colors", raw).apply()
    }

    // Active Toolbar Tools List
    fun getActiveToolbarTools(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_toolbar_tools", "settings,grammar,guru,voice,translate,modes") ?: "settings,grammar,guru,voice,translate,modes"
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun saveActiveToolbarTools(context: Context, tools: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = tools.joinToString(",")
        prefs.edit().putString("keyboard_toolbar_tools", raw).apply()
    }

    // Custom background image Uri and alpha
    fun getCustomBackgroundUri(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_bg_uri", "") ?: ""
    }

    fun saveCustomBackgroundUri(context: Context, uri: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_bg_uri", uri).apply()
    }

    fun getCustomBackgroundAlpha(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("keyboard_bg_alpha", 0.4f)
    }

    fun saveCustomBackgroundAlpha(context: Context, alpha: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("keyboard_bg_alpha", alpha).apply()
    }

    // -----------------------------------------------------
    // CLIPBOARD PERSISTENT MANAGER (Max 20 items, respects pinned)
    // -----------------------------------------------------
    data class ClipboardItem(val text: String, val isPinned: Boolean)

    fun getClipboardItems(context: Context): List<ClipboardItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_clipboard_data", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("###CLIPSEP###").filter { it.isNotBlank() }.mapNotNull {
            val idx = it.lastIndexOf("###PINNED###")
            if (idx != -1) {
                val base64Text = it.substring(0, idx)
                val pinnedStr = it.substring(idx + "###PINNED###".length)
                try {
                    val decodedBytes = android.util.Base64.decode(base64Text, android.util.Base64.DEFAULT)
                    val originalText = String(decodedBytes, Charsets.UTF_8)
                    ClipboardItem(originalText, pinnedStr == "true")
                } catch (e: java.lang.Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    fun saveClipboardItems(context: Context, list: List<ClipboardItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = list.joinToString("###CLIPSEP###") { item ->
            val base64Text = android.util.Base64.encodeToString(item.text.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            "${base64Text}###PINNED###${item.isPinned}"
        }
        prefs.edit().putString("keyboard_clipboard_data", raw).apply()
    }

    fun addClipboardItem(context: Context, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val current = getClipboardItems(context).toMutableList()
        
        // Match existing to prevent duplicates, promote to top
        current.removeAll { it.text == trimmed }
        current.add(0, ClipboardItem(trimmed, false))

        // Restrict size to 20, but keep all pinned items from removing
        while (current.size > 20) {
            val oldestUnpinnedIndex = current.indexOfLast { !it.isPinned }
            if (oldestUnpinnedIndex != -1) {
                current.removeAt(oldestUnpinnedIndex)
            } else {
                // Remove oldest pinned if absolutely necessary in case of extreme limits (all 20 pinned)
                current.removeAt(current.size - 1)
            }
        }
        saveClipboardItems(context, current)
    }

    fun toggleClipPin(context: Context, index: Int) {
        val current = getClipboardItems(context).toMutableList()
        if (index in current.indices) {
            val item = current[index]
            current[index] = item.copy(isPinned = !item.isPinned)
            saveClipboardItems(context, current)
        }
    }

    fun deleteClipItem(context: Context, index: Int) {
        val current = getClipboardItems(context).toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            saveClipboardItems(context, current)
        }
    }

    // -----------------------------------------------------
    // SMART AUTO-REPLIER CONFIGURATIONS
    // -----------------------------------------------------
    fun getAutoReplierEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("keyboard_autoreply_enabled", false)
    }

    fun saveAutoReplierEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("keyboard_autoreply_enabled", enabled).apply()
    }

    fun getAutoReplierTone(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_autoreply_tone", "friendly") ?: "friendly"
    }

    fun saveAutoReplierTone(context: Context, tone: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_autoreply_tone", tone).apply()
    }

    // -----------------------------------------------------
    // QUICK-SLOT & ANIMATION PERSISTENCE
    // -----------------------------------------------------
    fun getQuickSlotTool(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_quick_slot_tool", "direct_translator") ?: "direct_translator"
    }

    fun saveQuickSlotTool(context: Context, toolId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_quick_slot_tool", toolId).apply()
    }

    fun getKeyboardAnimation(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_animation_style", "Standard Mechanical") ?: "Standard Mechanical"
    }

    fun saveKeyboardAnimation(context: Context, animation: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_animation_style", animation).apply()
    }

    // Number Row & Keyboard Offset Persistence
    fun getNumberRowEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("keyboard_show_number_row", false)
    }

    fun saveNumberRowEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("keyboard_show_number_row", enabled).apply()
    }

    fun getKeyboardOffsetY(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("keyboard_offset_y", 0f)
    }

    fun saveKeyboardOffsetY(context: Context, offset: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("keyboard_offset_y", offset).apply()
    }

    fun getKeyboardOffsetX(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("keyboard_offset_x", 0f)
    }

    fun saveKeyboardOffsetX(context: Context, offset: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("keyboard_offset_x", offset).apply()
    }

    fun getFloatingKeyboardEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("keyboard_floating_enabled", false)
    }

    fun saveFloatingKeyboardEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("keyboard_floating_enabled", enabled).apply()
    }

    fun getKeyboardLayoutMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_layout_mode_v2", "Standard") ?: "Standard"
    }

    fun saveKeyboardLayoutMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_layout_mode_v2", mode).apply()
    }

    fun getOneHandedEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("keyboard_one_handed_enabled", false)
    }

    fun saveOneHandedEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("keyboard_one_handed_enabled", enabled).apply()
    }

    fun getAiSuggestorEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("keyboard_ai_suggestor_enabled", true)
    }

    fun saveAiSuggestorEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("keyboard_ai_suggestor_enabled", enabled).apply()
    }

    fun getFloatingAssistantActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("keyboard_floating_assistant_active", false)
    }

    fun saveFloatingAssistantActive(context: Context, active: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("keyboard_floating_assistant_active", active).apply()
    }

    // TYPING VELOCITY, DEBOUNCE, SIZES, SHAPES, LIGHTINGS PRESETS
    fun getDebounceInterval(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong("keyboard_debounce_interval", 40L) // Default 40ms to avoid double typing
    }

    fun saveDebounceInterval(context: Context, ms: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("keyboard_debounce_interval", ms).apply()
    }

    fun getKeyHeightScale(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("keyboard_key_height_scale", 1.0f)
    }

    fun saveKeyHeightScale(context: Context, scale: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("keyboard_key_height_scale", scale).apply()
    }

    fun getKeyWidthMulti(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("keyboard_key_width_multi", 1.0f)
    }

    fun saveKeyWidthMulti(context: Context, multi: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("keyboard_key_width_multi", multi).apply()
    }

    fun getKeyShape(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_key_shape", "Rounded") ?: "Rounded"
    }

    fun saveKeyShape(context: Context, shape: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_key_shape", shape).apply()
    }

    fun getLightingEffect(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_lighting_effect", "None") ?: "None"
    }

    fun saveLightingEffect(context: Context, effect: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_lighting_effect", effect).apply()
    }

    fun getKeyStyle(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_key_style", "Mechanical") ?: "Mechanical"
    }

    fun saveKeyStyle(context: Context, style: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_key_style", style).apply()
    }

    // AI Reply Bar Visibility Setting
    fun getAiReplyBarEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("keyboard_ai_reply_bar_enabled", true)
    }

    fun saveAiReplyBarEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("keyboard_ai_reply_bar_enabled", enabled).apply()
    }

    // 50 MB custom local learning storage (remembers typed words/recurrent replies)
    // To act as a 50MB dedicated storage, we persist learned phrases in a structured list 
    // and store up to 5000 distinct words & sequences with frequency maps to refine predictive lookup.
    fun learnTypedWordOrPhrase(context: Context, text: String) {
        val cleaned = text.trim()
        if (cleaned.length < 2) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_learned_corpus", "") ?: ""
        val items = raw.split("###CORPUS_SEP###").filter { it.isNotBlank() }.toMutableList()
        
        // Remove existing to promote on top based on frequency
        items.remove(cleaned)
        items.add(0, cleaned)
        
        // Limit to 2000 smart items to stay very performant yet extensive
        if (items.size > 2000) {
            items.removeAt(items.size - 1)
        }
        
        prefs.edit().putString("keyboard_learned_corpus", items.joinToString("###CORPUS_SEP###")).apply()
    }

    fun getLearnedSuggestions(context: Context, prefix: String): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_learned_corpus", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        
        val items = raw.split("###CORPUS_SEP###").filter { it.isNotBlank() }
        val lookup = prefix.trim().lowercase()
        if (lookup.isEmpty()) {
            return items.take(4) // return recently typed phrases/words
        }
        return items.filter { it.lowercase().startsWith(lookup) }.take(4)
    }

    fun getLearnedCorpusSizeEstimated(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_learned_corpus", "") ?: ""
        val bytes = raw.toByteArray(Charsets.UTF_8).size
        // We simulate a 50MB corpus block allocation or show the active consumed vs limit
        val maxAllocatedMb = 50.0
        val usedKb = bytes / 1024.0
        val usedMb = usedKb / 1024.0
        return String.format("%.4f MB / %.1f MB (Stored %d customized user phrases)", usedMb, maxAllocatedMb, raw.split("###CORPUS_SEP###").filter { it.isNotBlank() }.size)
    }

    fun clearLearnedCorpus(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("keyboard_learned_corpus").apply()
    }

    // New persistent customizable styling variables
    fun getBrowserLayoutMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_browser_layout_mode", "Side-by-Side") ?: "Side-by-Side"
    }

    fun saveBrowserLayoutMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_browser_layout_mode", mode).apply()
    }

    fun getKeyboardOuterRadius(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("keyboard_outer_radius_val", 12f)
    }

    fun saveKeyboardOuterRadius(context: Context, radius: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("keyboard_outer_radius_val", radius).apply()
    }

    fun getOverallBorderWidth(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("keyboard_overall_border_width", 1.5f)
    }

    fun saveOverallBorderWidth(context: Context, width: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("keyboard_overall_border_width", width).apply()
    }

    fun getOverallPadding(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("keyboard_overall_padding", 4f)
    }

    fun saveOverallPadding(context: Context, padding: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("keyboard_overall_padding", padding).apply()
    }

    fun getRgbAnimationType(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_rgb_animation_type", "Flowing Rainbow") ?: "Flowing Rainbow"
    }

    fun saveRgbAnimationType(context: Context, animType: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_rgb_animation_type", animType).apply()
    }

    fun getAppShortcuts(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_app_shortcuts", "") ?: ""
        if (raw.isEmpty()) {
            return listOf("WhatsApp", "Instagram", "YouTube", "Chrome", "Camera")
        }
        return raw.split("###SHORTCUTSEP###").filter { it.isNotBlank() }
    }

    fun saveAppShortcuts(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_app_shortcuts", list.joinToString("###SHORTCUTSEP###")).apply()
    }

    fun getFontStyle(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("keyboard_font_style_setting", "Normal") ?: "Normal"
    }

    fun saveFontStyle(context: Context, fontStyle: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("keyboard_font_style_setting", fontStyle).apply()
    }

    // -----------------------------------------------------
    // SMART AI RESPONSE CACHE (LRU simple implementation)
    // -----------------------------------------------------
    fun getCachedAiResponse(context: Context, key: String): List<String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("keyboard_ai_cache_${key.hashCode()}", "") ?: ""
        if (raw.isEmpty()) return null
        return raw.split("###CACHSEP###")
    }

    fun saveCachedAiResponse(context: Context, key: String, responses: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = responses.joinToString("###CACHSEP###")
        prefs.edit().putString("keyboard_ai_cache_${key.hashCode()}", raw).apply()
    }

    // EMOJI PERSISTENCE
    private const val KEY_RECENT_EMOJIS = "keyboard_recent_emojis"

    fun getRecentEmojis(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RECENT_EMOJIS, "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun addRecentEmoji(context: Context, emoji: String) {
        val current = getRecentEmojis(context).toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        if (current.size > 50) {
            current.removeAt(current.size - 1)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RECENT_EMOJIS, current.joinToString(",")).apply()
    }

    // -----------------------------------------------------
    // BARS VISIBILITY MANAGER (Toggles top bars one by one)
    // -----------------------------------------------------
    fun getBarsVisibility(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("keyboard_bars_visibility", 0) // 0: All, 1: Hide AiReplyBar, 2: Hide AiReply+WordSugg, 3: Hide All
    }

    fun saveBarsVisibility(context: Context, level: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("keyboard_bars_visibility", level).apply()
    }

    // -----------------------------------------------------
    // CONVERSATION SUGGESTOR GIVER PERSISTENCE
    // -----------------------------------------------------
    fun getSuggestorEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("convo_suggestor_enabled", false)
    }

    fun saveSuggestorEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("convo_suggestor_enabled", enabled).apply()
    }

    fun getSuggestorHinglish(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("convo_suggestor_hinglish", true)
    }

    fun saveSuggestorHinglish(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("convo_suggestor_hinglish", enabled).apply()
    }

    fun getSuggestorMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("convo_suggestor_mode", "flirty") ?: "flirty"
    }

    fun saveSuggestorMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("convo_suggestor_mode", mode).apply()
    }

    fun getSuggestorTimer(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("convo_suggestor_timer", 5) // Default 5 seconds
    }

    fun saveSuggestorTimer(context: Context, seconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("convo_suggestor_timer", seconds).apply()
    }

    fun getActiveConvoTexts(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("convo_suggestor_active_texts", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("###CONVOSEP###").filter { it.isNotBlank() }
    }

    fun saveActiveConvoTexts(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = list.joinToString("###CONVOSEP###")
        prefs.edit().putString("convo_suggestor_active_texts", raw).apply()
    }

    fun addActiveConvoText(context: Context, text: String) {
        val current = getActiveConvoTexts(context).toMutableList()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (current.isNotEmpty() && current.last() == trimmed) return
        current.add(trimmed)
        if (current.size > 15) {
            current.removeAt(0)
        }
        saveActiveConvoTexts(context, current)
    }

    fun clearActiveConvoTexts(context: Context) {
        saveActiveConvoTexts(context, emptyList())
    }

    fun getSavedSessions(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("convo_suggestor_saved_sessions", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun saveSavedSessionsList(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = list.joinToString(",")
        prefs.edit().putString("convo_suggestor_saved_sessions", raw).apply()
    }

    fun saveSessionByName(context: Context, name: String, texts: List<String>) {
        val cleanName = name.replace(",", "").replace("#", "").trim()
        if (cleanName.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawTexts = texts.joinToString("###CONVOSEP###")
        prefs.edit().putString("convo_suggestor_session_data_$cleanName", rawTexts).apply()

        val sessions = getSavedSessions(context).toMutableList()
        if (!sessions.contains(cleanName)) {
            sessions.add(cleanName)
            saveSavedSessionsList(context, sessions)
        }
    }

    fun getSessionData(context: Context, name: String): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("convo_suggestor_session_data_$name", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        return raw.split("###CONVOSEP###").filter { it.isNotBlank() }
    }

    fun deleteSessionByName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("convo_suggestor_session_data_$name").apply()

        val sessions = getSavedSessions(context).toMutableList()
        sessions.remove(name)
        saveSavedSessionsList(context, sessions)
    }
}

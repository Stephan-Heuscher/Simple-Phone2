package com.example.simplephone.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages app settings and preferences
 */
class SettingsRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "simple_phone_prefs"
        private const val KEY_FILTER_HOURS = "filter_hours"
        private const val KEY_USE_HUGE_TEXT = "use_huge_text"
        private const val KEY_MISSED_CALLS_HOURS = "missed_calls_hours"
        private const val KEY_USE_DARK_MODE = "use_dark_mode"
        private const val KEY_CONFIRM_BEFORE_CALL = "confirm_before_call"
        private const val KEY_USE_HAPTIC_FEEDBACK = "use_haptic_feedback"
        private const val KEY_USE_VOICE_ANNOUNCEMENTS = "use_voice_announcements"
        
        private const val DEFAULT_FILTER_HOURS = 2
        private const val DEFAULT_MISSED_CALLS_HOURS = 24
    }
    
    var filterHours: Int
        get() = prefs.getInt(KEY_FILTER_HOURS, DEFAULT_FILTER_HOURS)
        set(value) = prefs.edit().putInt(KEY_FILTER_HOURS, value).apply()
    
    var useHugeText: Boolean
        get() = prefs.getBoolean(KEY_USE_HUGE_TEXT, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_HUGE_TEXT, value).apply()
    
    var missedCallsHours: Int
        get() = prefs.getInt(KEY_MISSED_CALLS_HOURS, DEFAULT_MISSED_CALLS_HOURS)
        set(value) = prefs.edit().putInt(KEY_MISSED_CALLS_HOURS, value).apply()
    
    var useDarkMode: Boolean
        get() = prefs.getBoolean(KEY_USE_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_DARK_MODE, value).apply()
    
    var confirmBeforeCall: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_BEFORE_CALL, false)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_BEFORE_CALL, value).apply()
    
    var useHapticFeedback: Boolean
        get() = prefs.getBoolean(KEY_USE_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_HAPTIC_FEEDBACK, value).apply()
    
    var useVoiceAnnouncements: Boolean
        get() = prefs.getBoolean(KEY_USE_VOICE_ANNOUNCEMENTS, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_VOICE_ANNOUNCEMENTS, value).apply()
}

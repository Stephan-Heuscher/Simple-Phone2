package ch.heuscher.simplephone.data

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
        private const val KEY_MISSED_CALLS_HOURS = "missed_calls_hours"
        private const val KEY_DARK_MODE_OPTION = "dark_mode_option"
        private const val KEY_CONFIRM_BEFORE_CALL = "confirm_before_call"
        private const val KEY_USE_HAPTIC_FEEDBACK = "use_haptic_feedback"
        private const val KEY_USE_VOICE_ANNOUNCEMENTS = "use_voice_announcements"
        private const val KEY_FAVORITES_ORDER = "favorites_order"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_IS_DEMO_MODE = "is_demo_mode"
        private const val KEY_BLOCK_UNKNOWN_CALLERS = "block_unknown_callers"
        private const val KEY_ANSWER_SPEAKER_TABLE = "answer_speaker_table"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_LAST_BLOCKED_NUMBER = "last_blocked_number"
        private const val KEY_SIMPLIFIED_CONTACT_CALL_SCREEN = "simplified_contact_call_screen"
        
        // Zoom factors per screen size
        private const val KEY_ZOOM_COMPACT = "zoom_compact"
        private const val KEY_ZOOM_MEDIUM = "zoom_medium"
        private const val KEY_ZOOM_EXPANDED = "zoom_expanded"
        
        private const val DEFAULT_MISSED_CALLS_HOURS = 4
        
        // Dark Mode Options
        const val DARK_MODE_SYSTEM = 0
        const val DARK_MODE_LIGHT = 1
        const val DARK_MODE_DARK = 2
        
        // Display Mode Options
        const val DISPLAY_MODE_STANDARD = 0
        const val DISPLAY_MODE_LARGE_TEXT = 1
        const val DISPLAY_MODE_BIG_PHOTOS = 2
        const val DISPLAY_MODE_GRID = 3
    }
    
    var missedCallsHours: Int
        get() = prefs.getInt(KEY_MISSED_CALLS_HOURS, DEFAULT_MISSED_CALLS_HOURS)
        set(value) = prefs.edit().putInt(KEY_MISSED_CALLS_HOURS, value).apply()

    var darkModeOption: Int
        get() = prefs.getInt(KEY_DARK_MODE_OPTION, DARK_MODE_SYSTEM)
        set(value) = prefs.edit().putInt(KEY_DARK_MODE_OPTION, value).apply()

    var confirmBeforeCall: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_BEFORE_CALL, false)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_BEFORE_CALL, value).apply()

    var useHapticFeedback: Boolean
        get() = prefs.getBoolean(KEY_USE_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_HAPTIC_FEEDBACK, value).apply()

    var useVoiceAnnouncements: Boolean
        get() = prefs.getBoolean(KEY_USE_VOICE_ANNOUNCEMENTS, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_VOICE_ANNOUNCEMENTS, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isDemoMode: Boolean
        get() = prefs.getBoolean(KEY_IS_DEMO_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_DEMO_MODE, value).apply()

    var blockUnknownCallers: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_UNKNOWN_CALLERS, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_UNKNOWN_CALLERS, value).apply()
        
    var lastBlockedNumber: String?
        get() = prefs.getString(KEY_LAST_BLOCKED_NUMBER, null)
        set(value) = prefs.edit().putString(KEY_LAST_BLOCKED_NUMBER, value).apply()

    var answerOnSpeakerIfFlat: Boolean
        get() = prefs.getBoolean(KEY_ANSWER_SPEAKER_TABLE, false)
        set(value) = prefs.edit().putBoolean(KEY_ANSWER_SPEAKER_TABLE, value).apply()

    var displayMode: Int
        get() = prefs.getInt(KEY_DISPLAY_MODE, DISPLAY_MODE_STANDARD)
        set(value) = prefs.edit().putInt(KEY_DISPLAY_MODE, value).apply()

    var simplifiedContactCallScreen: Boolean
        get() = prefs.getBoolean(KEY_SIMPLIFIED_CONTACT_CALL_SCREEN, true)
        set(value) = prefs.edit().putBoolean(KEY_SIMPLIFIED_CONTACT_CALL_SCREEN, value).apply()

    // Derived properties for backward compatibility / ease of use
    val useHugeText: Boolean
        get() = displayMode == DISPLAY_MODE_LARGE_TEXT
        
    val useHugeContactPicture: Boolean
        get() = displayMode == DISPLAY_MODE_BIG_PHOTOS
        
    val useGridContactImages: Boolean
        get() = displayMode == DISPLAY_MODE_GRID

    fun getFavoritesOrder(): List<String> {
        // Store as CSV to preserve order
        val stored = prefs.getString(KEY_FAVORITES_ORDER + "_list", null)
        if (stored != null) {
            return stored.split(",").filter { it.isNotEmpty() }
        }
        return emptyList()
    }

    fun saveFavoritesOrder(ids: List<String>) {
        val joined = ids.joinToString(",")
        prefs.edit().putString(KEY_FAVORITES_ORDER + "_list", joined).apply()
    }

    /**
     * Get zoom factor for specific window width size class
     */
    fun getZoomFactor(widthSizeClass: androidx.compose.material3.windowsizeclass.WindowWidthSizeClass): Float {
        val key = when (widthSizeClass) {
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact -> KEY_ZOOM_COMPACT
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium -> KEY_ZOOM_MEDIUM
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded -> KEY_ZOOM_EXPANDED
            else -> KEY_ZOOM_COMPACT
        }
        return prefs.getFloat(key, 1.0f)
    }

    /**
     * Set zoom factor for specific window width size class
     */
    fun setZoomFactor(widthSizeClass: androidx.compose.material3.windowsizeclass.WindowWidthSizeClass, factor: Float) {
        val key = when (widthSizeClass) {
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact -> KEY_ZOOM_COMPACT
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium -> KEY_ZOOM_MEDIUM
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded -> KEY_ZOOM_EXPANDED
            else -> KEY_ZOOM_COMPACT
        }
        prefs.edit().putFloat(key, factor).apply()
    }
}


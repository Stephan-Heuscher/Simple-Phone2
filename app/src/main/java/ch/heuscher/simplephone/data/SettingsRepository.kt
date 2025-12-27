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
        private const val KEY_FILTER_HOURS = "filter_hours"
        private const val KEY_USE_HUGE_TEXT = "use_huge_text"
        private const val KEY_MISSED_CALLS_HOURS = "missed_calls_hours"
        private const val KEY_DARK_MODE_OPTION = "dark_mode_option" // Changed key
        private const val KEY_CONFIRM_BEFORE_CALL = "confirm_before_call"
        private const val KEY_USE_HAPTIC_FEEDBACK = "use_haptic_feedback"
        private const val KEY_USE_VOICE_ANNOUNCEMENTS = "use_voice_announcements"
        private const val KEY_FAVORITES_ORDER = "favorites_order"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_IS_DEMO_MODE = "is_demo_mode"
        private const val KEY_BLOCK_UNKNOWN_CALLERS = "block_unknown_callers"
        private const val KEY_ANSWER_SPEAKER_TABLE = "answer_speaker_table"
        private const val KEY_USE_HUGE_CONTACT_PICTURE = "use_huge_contact_picture"
        private const val KEY_USE_GRID_CONTACT_IMAGES = "use_grid_contact_images"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_DISPLAY_MODE_MIGRATED = "display_mode_migrated"
        
        private const val DEFAULT_FILTER_HOURS = 2
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
    
    /**
     * Save favorites order as comma-separated contact IDs
     */
    fun saveFavoritesOrder(contactIds: List<String>) {
        prefs.edit().putString(KEY_FAVORITES_ORDER, contactIds.joinToString(",")).apply()
    }
    
    /**
     * Get saved favorites order as list of contact IDs
     */
    fun getFavoritesOrder(): List<String> {
        val orderString = prefs.getString(KEY_FAVORITES_ORDER, null) ?: return emptyList()
        return orderString.split(",").filter { it.isNotBlank() }
    }
    
    var filterHours: Int
        get() = prefs.getInt(KEY_FILTER_HOURS, DEFAULT_FILTER_HOURS)
        set(value) = prefs.edit().putInt(KEY_FILTER_HOURS, value).apply()
    
    var missedCallsHours: Int
        get() = prefs.getInt(KEY_MISSED_CALLS_HOURS, DEFAULT_MISSED_CALLS_HOURS)
        set(value) = prefs.edit().putInt(KEY_MISSED_CALLS_HOURS, value).apply()
    
    var darkModeOption: Int
        get() = prefs.getInt(KEY_DARK_MODE_OPTION, DARK_MODE_SYSTEM)
        set(value) = prefs.edit().putInt(KEY_DARK_MODE_OPTION, value).apply()
    
    // Deprecated boolean property, mapped to new int option for compatibility if needed
    var useDarkMode: Boolean
        get() = darkModeOption == DARK_MODE_DARK
        set(value) {
            darkModeOption = if (value) DARK_MODE_DARK else DARK_MODE_LIGHT
        }
    
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

    var answerOnSpeakerIfFlat: Boolean
        get() = prefs.getBoolean(KEY_ANSWER_SPEAKER_TABLE, false)
        set(value) = prefs.edit().putBoolean(KEY_ANSWER_SPEAKER_TABLE, value).apply()

    /**
     * Unified display mode (replaces useHugeText, useHugeContactPicture, useGridContactImages)
     * 0 = Standard (default)
     * 1 = Large Text
     * 2 = Big Photos
     * 3 = Grid View
     */
    var displayMode: Int
        get() {
            // Migrate from old boolean settings on first access
            if (!prefs.getBoolean(KEY_DISPLAY_MODE_MIGRATED, false)) {
                val oldGrid = prefs.getBoolean(KEY_USE_GRID_CONTACT_IMAGES, false)
                val oldHugePic = prefs.getBoolean(KEY_USE_HUGE_CONTACT_PICTURE, false)
                val oldHugeText = prefs.getBoolean(KEY_USE_HUGE_TEXT, false)
                
                val migratedMode = when {
                    oldGrid -> DISPLAY_MODE_GRID
                    oldHugePic -> DISPLAY_MODE_BIG_PHOTOS
                    oldHugeText -> DISPLAY_MODE_LARGE_TEXT
                    else -> DISPLAY_MODE_STANDARD
                }
                
                prefs.edit()
                    .putInt(KEY_DISPLAY_MODE, migratedMode)
                    .putBoolean(KEY_DISPLAY_MODE_MIGRATED, true)
                    .apply()
                
                return migratedMode
            }
            return prefs.getInt(KEY_DISPLAY_MODE, DISPLAY_MODE_STANDARD)
        }
        set(value) = prefs.edit().putInt(KEY_DISPLAY_MODE, value).apply()

    // Backward-compatible computed properties (derived from displayMode)
    val useHugeText: Boolean
        get() = displayMode == DISPLAY_MODE_LARGE_TEXT

    val useHugeContactPicture: Boolean
        get() = displayMode == DISPLAY_MODE_BIG_PHOTOS

    val useGridContactImages: Boolean
        get() = displayMode == DISPLAY_MODE_GRID
}

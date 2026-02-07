package ch.heuscher.simplephone.data

import android.content.Context
import android.content.SharedPreferences
import ch.heuscher.simplephone.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class holding all application settings.
 * This allows the UI to observe a single stream of state.
 */
data class AppSettings(
    val missedCallsHours: Int,
    val darkModeOption: Int,
    val confirmBeforeCall: Boolean,
    val useHapticFeedback: Boolean,
    val useVoiceAnnouncements: Boolean,
    val blockUnknownCallers: Boolean,
    val displayMode: Int,
    val simplifiedContactCallScreen: Boolean,
    val silenceCallOnTouch: Boolean,
    val ringtoneSilenceTimeout: Int,
    val isDemoMode: Boolean,
    val onboardingCompleted: Boolean,
    val lastBlockedNumber: String?
)

/**
 * Manages app settings and preferences.
 * For Gentle Phone: Remote settings from caregiver override local settings and changes are synced back.
 * For Simple Phone: Only local SharedPreferences are used.
 */
class SettingsRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    // Remote settings repository (Firebase for gentlephone, stub for simplephone)
    private val remoteSettings = RemoteSettingsRepository(context)
    
    // Cache of remote settings - these override local settings when present
    private var remoteCache: Map<String, Any>? = null
    
    // Reactive state of settings
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

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

        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_LAST_BLOCKED_NUMBER = "last_blocked_number"
        private const val KEY_SIMPLIFIED_CONTACT_CALL_SCREEN = "simplified_contact_call_screen"
        private const val KEY_RINGTONE_SILENCE_TIMEOUT = "ringtone_silence_timeout"
        
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

        private const val KEY_SILENCE_CALL_ON_TOUCH = "silence_call_on_touch"
        
        // Remote setting keys (used in Firestore)
        private const val REMOTE_KEY_DARK_MODE = "darkMode"
        private const val REMOTE_KEY_DISPLAY_MODE = "displayMode"
        private const val REMOTE_KEY_CONFIRM_BEFORE_CALL = "confirmBeforeCall"
        private const val REMOTE_KEY_HAPTIC_FEEDBACK = "hapticFeedback"
        private const val REMOTE_KEY_VOICE_ANNOUNCEMENTS = "voiceAnnouncements"
        private const val REMOTE_KEY_BLOCK_UNKNOWN = "blockUnknown"
        private const val REMOTE_KEY_SILENCE_ON_TOUCH = "silenceOnTouch"
        private const val REMOTE_KEY_RINGTONE_TIMEOUT = "ringtoneTimeout"
    }
    
    init {
        // Start listening for remote settings changes if enabled
        if (BuildConfig.REMOTE_SETTINGS_ENABLED) {
            startRemoteSettingsSync()
        }
        
        // Listen for local preference changes to update flow if changed elsewhere (unlikely but safe)
        prefs.registerOnSharedPreferenceChangeListener { _, _ ->
            refreshSettings()
        }
    }
    
    /**
     * Start listening for real-time remote settings changes from Firestore.
     */
    private fun startRemoteSettingsSync() {
        remoteSettings.listenForSettingsChanges { settings ->
            remoteCache = settings
            refreshSettings()
        }
    }
    
    /**
     * Manually sync remote settings (call on app launch).
     */
    suspend fun syncRemoteSettings() {
        if (BuildConfig.REMOTE_SETTINGS_ENABLED) {
            remoteCache = remoteSettings.fetchRemoteSettings()
            refreshSettings()
        }
    }
    
    private fun refreshSettings() {
        _settings.value = loadSettings()
    }
    
    private fun loadSettings(): AppSettings {
        return AppSettings(
            missedCallsHours = prefs.getInt(KEY_MISSED_CALLS_HOURS, DEFAULT_MISSED_CALLS_HOURS), // No remote override for this yet
            darkModeOption = getRemoteInt(REMOTE_KEY_DARK_MODE) ?: prefs.getInt(KEY_DARK_MODE_OPTION, DARK_MODE_SYSTEM),
            confirmBeforeCall = getRemoteBool(REMOTE_KEY_CONFIRM_BEFORE_CALL) ?: prefs.getBoolean(KEY_CONFIRM_BEFORE_CALL, false),
            useHapticFeedback = getRemoteBool(REMOTE_KEY_HAPTIC_FEEDBACK) ?: prefs.getBoolean(KEY_USE_HAPTIC_FEEDBACK, true),
            useVoiceAnnouncements = getRemoteBool(REMOTE_KEY_VOICE_ANNOUNCEMENTS) ?: prefs.getBoolean(KEY_USE_VOICE_ANNOUNCEMENTS, false),
            blockUnknownCallers = getRemoteBool(REMOTE_KEY_BLOCK_UNKNOWN) ?: prefs.getBoolean(KEY_BLOCK_UNKNOWN_CALLERS, false),
            displayMode = getRemoteInt(REMOTE_KEY_DISPLAY_MODE) ?: prefs.getInt(KEY_DISPLAY_MODE, DISPLAY_MODE_STANDARD),
            simplifiedContactCallScreen = prefs.getBoolean(KEY_SIMPLIFIED_CONTACT_CALL_SCREEN, true), // No remote override yet
            silenceCallOnTouch = getRemoteBool(REMOTE_KEY_SILENCE_ON_TOUCH) ?: prefs.getBoolean(KEY_SILENCE_CALL_ON_TOUCH, false),
            ringtoneSilenceTimeout = getRemoteInt(REMOTE_KEY_RINGTONE_TIMEOUT) ?: prefs.getInt(KEY_RINGTONE_SILENCE_TIMEOUT, 0),
            isDemoMode = prefs.getBoolean(KEY_IS_DEMO_MODE, false),
            onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false),
            lastBlockedNumber = prefs.getString(KEY_LAST_BLOCKED_NUMBER, null)
        )
    }
    
    /**
     * Get pairing code for caregiver linking (only works for Gentle Phone).
     */
    fun getPairingCode(): String = remoteSettings.getPairingCode()
    
    /**
     * Check if remote settings are enabled for this build.
     */
    fun isRemoteSettingsEnabled(): Boolean = BuildConfig.REMOTE_SETTINGS_ENABLED
    
    // Helper to get remote Int value
    private fun getRemoteInt(key: String): Int? = (remoteCache?.get(key) as? Number)?.toInt()
    
    // Helper to get remote Boolean value
    private fun getRemoteBool(key: String): Boolean? = remoteCache?.get(key) as? Boolean
    
    // Helper to update remote setting
    private fun updateRemote(key: String, value: Any) {
        if (isRemoteSettingsEnabled()) {
            remoteSettings.updateRemoteSetting(key, value)
        }
    }
    
    // ========== SETTINGS WITH REMOTE OVERRIDE & SYNC ==========
    
    var missedCallsHours: Int
        get() = settings.value.missedCallsHours
        set(value) {
            prefs.edit().putInt(KEY_MISSED_CALLS_HOURS, value).apply()
            // Not remotely synced currently
        }

    var darkModeOption: Int
        get() = settings.value.darkModeOption
        set(value) {
            prefs.edit().putInt(KEY_DARK_MODE_OPTION, value).apply()
            updateRemote(REMOTE_KEY_DARK_MODE, value)
        }

    var confirmBeforeCall: Boolean
        get() = settings.value.confirmBeforeCall
        set(value) {
            prefs.edit().putBoolean(KEY_CONFIRM_BEFORE_CALL, value).apply()
            updateRemote(REMOTE_KEY_CONFIRM_BEFORE_CALL, value)
        }

    var useHapticFeedback: Boolean
        get() = settings.value.useHapticFeedback
        set(value) {
            prefs.edit().putBoolean(KEY_USE_HAPTIC_FEEDBACK, value).apply()
            updateRemote(REMOTE_KEY_HAPTIC_FEEDBACK, value)
        }

    var useVoiceAnnouncements: Boolean
        get() = settings.value.useVoiceAnnouncements
        set(value) {
            prefs.edit().putBoolean(KEY_USE_VOICE_ANNOUNCEMENTS, value).apply()
            updateRemote(REMOTE_KEY_VOICE_ANNOUNCEMENTS, value)
        }

    var onboardingCompleted: Boolean
        get() = settings.value.onboardingCompleted
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isDemoMode: Boolean
        get() = settings.value.isDemoMode
        set(value) = prefs.edit().putBoolean(KEY_IS_DEMO_MODE, value).apply()

    var blockUnknownCallers: Boolean
        get() = settings.value.blockUnknownCallers
        set(value) {
            prefs.edit().putBoolean(KEY_BLOCK_UNKNOWN_CALLERS, value).apply()
            updateRemote(REMOTE_KEY_BLOCK_UNKNOWN, value)
        }
        
    var lastBlockedNumber: String?
        get() = settings.value.lastBlockedNumber
        set(value) = prefs.edit().putString(KEY_LAST_BLOCKED_NUMBER, value).apply()


    var displayMode: Int
        get() = settings.value.displayMode
        set(value) {
            prefs.edit().putInt(KEY_DISPLAY_MODE, value).apply()
            updateRemote(REMOTE_KEY_DISPLAY_MODE, value)
        }

    var simplifiedContactCallScreen: Boolean
        get() = settings.value.simplifiedContactCallScreen
        set(value) {
            prefs.edit().putBoolean(KEY_SIMPLIFIED_CONTACT_CALL_SCREEN, value).apply()
            // Not remotely synced currently, but could be
        }

    var silenceCallOnTouch: Boolean
        get() = settings.value.silenceCallOnTouch
        set(value) {
            prefs.edit().putBoolean(KEY_SILENCE_CALL_ON_TOUCH, value).apply()
            updateRemote(REMOTE_KEY_SILENCE_ON_TOUCH, value)
        }

    var ringtoneSilenceTimeout: Int
        get() = settings.value.ringtoneSilenceTimeout
        set(value) {
            prefs.edit().putInt(KEY_RINGTONE_SILENCE_TIMEOUT, value).apply()
            updateRemote(REMOTE_KEY_RINGTONE_TIMEOUT, value)
        }

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

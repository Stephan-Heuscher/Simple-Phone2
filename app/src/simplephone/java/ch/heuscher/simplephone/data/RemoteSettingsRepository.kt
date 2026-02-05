package ch.heuscher.simplephone.data

import android.content.Context

/**
 * Stub implementation for Simple Phone - no remote settings functionality.
 * This class provides the same interface but returns null/empty values.
 */
class RemoteSettingsRepository(context: Context) {
    
    /**
     * No pairing code for Simple Phone
     */
    fun getPairingCode(): String = ""
    
    /**
     * No remote settings for Simple Phone
     */
    suspend fun fetchRemoteSettings(): Map<String, Any>? = null
    
    /**
     * No-op listener for Simple Phone
     */
    fun listenForSettingsChanges(onSettingsChanged: (Map<String, Any>?) -> Unit) {
        // No-op - Simple Phone doesn't support remote settings
    }
}

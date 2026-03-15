package ch.heuscher.simplephone.data

import android.content.Context

/**
 * Stub implementation for simple phone - no remote settings functionality.
 * This class provides the same interface but returns null/empty values.
 */
class RemoteSettingsRepository(context: Context) {
    
    /**
     * No pairing code for simple phone
     */
    fun getPairingCode(): String = ""
    
    /**
     * No temporary pairing code for simple phone
     */
    suspend fun generateTemporaryPairingCode(): String = ""
    
    /**
     * No remote settings for simple phone
     */
    suspend fun fetchRemoteSettings(): Map<String, Any>? = null
    
    /**
     * No-op listener for simple phone
     */
    fun listenForSettingsChanges(onSettingsChanged: (Map<String, Any>?) -> Unit) {
        // No-op - simple phone doesn't support remote settings
    }

    /**
     * No-op update for simple phone
     */
    fun updateRemoteSetting(key: String, value: Any) {
        // No-op
    }

    /**
     * No-op upload for simple phone
     */
    fun uploadFavorites(favorites: List<ch.heuscher.simplephone.model.Contact>) {
        // No-op
    }
}

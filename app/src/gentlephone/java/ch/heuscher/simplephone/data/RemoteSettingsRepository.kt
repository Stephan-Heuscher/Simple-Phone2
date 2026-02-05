package ch.heuscher.simplephone.data

import android.content.Context
import android.provider.Settings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Manages remote settings from Firebase Firestore for Gentle Phone.
 * Allows caregivers to remotely configure the phone settings via a web portal.
 */
class RemoteSettingsRepository(private val context: Context) {
    
    private val firestore by lazy { Firebase.firestore }
    private val deviceId: String by lazy { getOrCreateDeviceId() }
    
    companion object {
        private const val PREFS_NAME = "gentle_phone_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val COLLECTION_DEVICES = "devices"
        private const val DOC_SETTINGS = "settings"
    }
    
    /**
     * Get or create a unique device ID for pairing with caregiver.
     * This ID is used to identify this specific phone in Firestore.
     */
    private fun getOrCreateDeviceId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        
        if (id == null) {
            // Generate a new UUID for this device
            id = UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        
        return id
    }
    
    /**
     * Get the pairing code to display to the user.
     * Caregiver enters this code in the web portal to link to this phone.
     */
    fun getPairingCode(): String = deviceId
    
    /**
     * Fetch remote settings from Firestore.
     * Returns null if no remote settings exist or on error.
     */
    suspend fun fetchRemoteSettings(): Map<String, Any>? {
        return try {
            val document = firestore
                .collection(COLLECTION_DEVICES)
                .document(deviceId)
                .collection(DOC_SETTINGS)
                .document("current")
                .get()
                .await()
            
            if (document.exists()) {
                document.data
            } else {
                // Create initial document so it appears in Firestore for caregiver
                registerDevice()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Register this device in Firestore so caregivers can find it.
     */
    private suspend fun registerDevice() {
        try {
            val deviceInfo = mapOf(
                "deviceName" to Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME),
                "pairedAt" to System.currentTimeMillis(),
                "appVersion" to context.packageManager.getPackageInfo(context.packageName, 0).versionName
            )
            
            firestore
                .collection(COLLECTION_DEVICES)
                .document(deviceId)
                .set(deviceInfo)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Listen for real-time settings changes from Firestore.
     * This allows caregivers to change settings and have them apply immediately.
     */
    fun listenForSettingsChanges(onSettingsChanged: (Map<String, Any>?) -> Unit) {
        firestore
            .collection(COLLECTION_DEVICES)
            .document(deviceId)
            .collection(DOC_SETTINGS)
            .document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    onSettingsChanged(snapshot.data)
                }
            }
    }
}

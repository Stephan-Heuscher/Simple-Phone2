package ch.heuscher.simplephone.watch

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class WatchInCallService : InCallService() {
    
    override fun onCallAdded(call: Call) {
        Log.d("WatchInCallService", "Call added on watch telecom: ${call.state}")
        // We let the Phone app's messages handle the actual UI state, 
        // but we need to exist so the system dialer doesn't take over!
        
        // Bring our custom call activity to the front just in case
        val intent = Intent(this, WatchCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("IS_OUTGOING", call.state != Call.STATE_RINGING)
            val handle = call.details.handle?.schemeSpecificPart
            if (handle != null) {
                putExtra("CALLER_NAME", handle)
            }
            val audioState = callAudioState
            if (audioState != null) {
                putExtra("AUDIO_ROUTE", audioState.route)
            }
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("WatchInCallService", "Failed to start WatchCallActivity", e)
        }
    }

    override fun onCallRemoved(call: Call) {
        Log.d("WatchInCallService", "Call removed from watch telecom")
    }
}

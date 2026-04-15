package ch.heuscher.simplephone.watch


import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

class WatchInCallService : InCallService() {
    
    override fun onCallAdded(call: Call) {
        Log.d("WatchInCallService", "Call added on watch telecom: state=${call.state}, handle=${call.details.handle}")
        // DO NOT launch WatchCallActivity here!
        // This service exists so the system dialer doesn't take over on the watch,
        // but we rely on the phone app's Wearable Message (/sync_call_state) to
        // launch the UI. That path only fires for calls managed by Simple Phone,
        // preventing us from hijacking WhatsApp, Webex, or other VoIP calls.
    }

    override fun onCallRemoved(call: Call) {
        Log.d("WatchInCallService", "Call removed from watch telecom")
    }
}

package ch.heuscher.simplephone.watch

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchWearableListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("WatchWearableListener", "Received message: ${messageEvent.path}")

        when (messageEvent.path) {
            "/sync_call_state" -> {
                val jsonPayload = String(messageEvent.data)
                // singleTask launchMode + SINGLE_TOP routes subsequent updates
                // through onNewIntent on the existing instance, so no new screen
                // is pushed on top of the running call activity.
                val activityIntent = Intent(this, WatchCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("SYNC_STATE_JSON", jsonPayload)
                }
                startActivity(activityIntent)
            }
        }
    }
}

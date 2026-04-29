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
                
                val activityIntent = Intent(this, WatchCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("SYNC_STATE_JSON", jsonPayload)
                }
                startActivity(activityIntent)
                
                val broadcastIntent = Intent("ch.heuscher.simplephone.watch.SYNC_CALL_STATE").apply {
                    setPackage(packageName)
                    putExtra("SYNC_STATE_JSON", jsonPayload)
                }
                sendBroadcast(broadcastIntent)
            }
        }
    }
}

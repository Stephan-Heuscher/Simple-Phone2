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
            "/incoming_call" -> {
                val callerName = String(messageEvent.data)
                val intent = Intent(this, WatchCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("CALLER_NAME", callerName)
                }
                startActivity(intent)
            }
            "/call_answered" -> {
                val intent = Intent("ch.heuscher.simplephone.watch.CALL_ANSWERED")
                sendBroadcast(intent)
            }
            "/call_ended" -> {
                // Send broadcast to close the call screen if it's open
                val intent = Intent("ch.heuscher.simplephone.watch.CALL_ENDED")
                sendBroadcast(intent)
            }
        }
    }
}

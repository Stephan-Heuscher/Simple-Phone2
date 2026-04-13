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
            "/outgoing_call" -> {
                val callerName = String(messageEvent.data)
                val intent = Intent(this, WatchCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("CALLER_NAME", callerName)
                    putExtra("IS_OUTGOING", true)
                }
                startActivity(intent)
            }
            "/call_answered" -> {
                val intent = Intent("ch.heuscher.simplephone.watch.CALL_ANSWERED").apply {
                    setPackage(packageName)
                }
                sendBroadcast(intent)
            }
            "/audio_state" -> {
                val route = String(messageEvent.data).toIntOrNull() ?: return
                val intent = Intent("ch.heuscher.simplephone.watch.AUDIO_STATE").apply {
                    setPackage(packageName)
                    putExtra("AUDIO_ROUTE", route)
                }
                sendBroadcast(intent)
            }
            "/audio_status" -> {
                val data = String(messageEvent.data)
                val intent = Intent("ch.heuscher.simplephone.watch.AUDIO_STATUS").apply {
                    setPackage(packageName)
                    putExtra("AUDIO_STATUS_DATA", data)
                }
                sendBroadcast(intent)
            }
            "/call_ended" -> {
                // Send broadcast to close the call screen if it's open
                val intent = Intent("ch.heuscher.simplephone.watch.CALL_ENDED").apply {
                    setPackage(packageName)
                }
                sendBroadcast(intent)
            }
            "/call_info" -> {
                val callerName = String(messageEvent.data)
                val intent = Intent("ch.heuscher.simplephone.watch.CALL_INFO").apply {
                    setPackage(packageName)
                    putExtra("CALLER_NAME", callerName)
                }
                sendBroadcast(intent)
            }
        }
    }
}

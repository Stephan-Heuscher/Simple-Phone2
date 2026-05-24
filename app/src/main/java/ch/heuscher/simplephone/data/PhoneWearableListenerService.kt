package ch.heuscher.simplephone.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import ch.heuscher.simplephone.call.CallService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PhoneWearableListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("PhoneWearableListener", "Received message: ${messageEvent.path}")
        
        when (messageEvent.path) {
            "/find_my_phone" -> {
                Log.d("PhoneWearableListener", "Watch requested to find phone. Launching FindPhoneActivity.")
                try {
                    val intent = android.content.Intent(this, Class.forName("ch.heuscher.simplephone.FindPhoneActivity")).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("PhoneWearableListener", "Failed to launch FindPhoneActivity", e)
                }
            }
            "/answer_call" -> {
                Log.d("PhoneWearableListener", "Watch requested to answer call")
                CallService.watchAnswered = true
                CallService.watchAnsweredAt = android.os.SystemClock.elapsedRealtime()
                CallService.answerCall()
            }
            "/reject_call" -> {
                Log.d("PhoneWearableListener", "Watch requested to reject call")
                CallService.rejectCall()
            }
            "/silence_ringer" -> {
                Log.d("PhoneWearableListener", "Watch requested to silence ringer")
                CallService.silenceRinger()
            }
            "/end_call" -> {
                Log.d("PhoneWearableListener", "Watch requested to end call")
                CallService.endCall()
            }
            "/set_audio_route" -> {
                val route = String(messageEvent.data).toIntOrNull() ?: return
                Log.d("PhoneWearableListener", "Watch requested to set audio route to $route")
                CallService.watchRequestedAudioRoute = route
                CallService.setAudioRoute(route)
            }
            "/volume_up" -> {
                Log.d("PhoneWearableListener", "Watch requested volume up")
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                CallService.requestAudioStatus()
            }
            "/volume_down" -> {
                Log.d("PhoneWearableListener", "Watch requested volume down")
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                CallService.requestAudioStatus()
            }
            "/toggle_mute" -> {
                Log.d("PhoneWearableListener", "Watch requested toggle mute")
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
                // Trigger an update back to the watch
                CallService.requestAudioStatus()
            }
            "/request_audio_status" -> {
                Log.d("PhoneWearableListener", "Watch requested audio status")
                CallService.requestAudioStatus()
            }
            "/initiate_call" -> {
                val rawNumber = String(messageEvent.data, Charsets.UTF_8).trim()
                // Contacts often include spaces, parentheses, dashes or dots in
                // the formatted number. Strip those before validating so we
                // don't silently drop e.g. "+41 79 123 45 67".
                val number = rawNumber.replace(Regex("[\\s()\\-./]"), "")
                Log.d("PhoneWearableListener", "Watch requested to initiate call to $rawNumber (cleaned: $number)")

                if (!number.matches(Regex("[0-9+*#,;]+"))) {
                    Log.e("PhoneWearableListener", "Invalid phone number requested: $rawNumber")
                    return
                }

                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    CallService.watchInitiated = true
                    CallService.watchInitiatedAt = android.os.SystemClock.elapsedRealtime()
                    
                    val uri = android.net.Uri.parse("tel:$number")
                    
                    try {
                        @Suppress("MissingPermission")
                        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                        telecomManager.placeCall(uri, null)
                    } catch (e: Exception) {
                        Log.e("PhoneWearableListener", "Error placing call via TelecomManager, falling back to intent", e)
                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = uri
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    }
                } else {
                    Log.e("PhoneWearableListener", "Missing CALL_PHONE permission")
                }
            }
        }
    }
}
// Trigger deployment v2

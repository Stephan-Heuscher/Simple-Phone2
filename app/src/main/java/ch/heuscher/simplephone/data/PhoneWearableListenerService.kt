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

    private var currentRingtone: Ringtone? = null
    private var isRinging = false
    private var originalVolume: Int = -1
    
    private val serviceJob = kotlinx.coroutines.SupervisorJob()
    private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + serviceJob)

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("PhoneWearableListener", "Received message: ${messageEvent.path}")
        
        when (messageEvent.path) {
            "/find_my_phone" -> startRingingAndVibrating()
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
                val number = String(messageEvent.data, Charsets.UTF_8).trim()
                Log.d("PhoneWearableListener", "Watch requested to initiate call to $number")
                
                // Sanity check for phone number characters
                if (!number.matches(Regex("[0-9+*#,;]+"))) {
                    Log.e("PhoneWearableListener", "Invalid phone number requested: $number")
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

    private fun startRingingAndVibrating() {
        if (isRinging) {
            Log.d("PhoneWearableListener", "Already ringing, ignoring repeat request")
            return
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Temporarily max out the volume
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        isRinging = true

        // Play alarm sound
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        currentRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
        currentRingtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        if (currentRingtone?.isPlaying == false) {
            currentRingtone?.play()
        }

        // Vibrate
        val pattern = longArrayOf(0, 500, 500, 500, 500, 500, 500, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 1)
        }

        // Stop after 15 seconds
        serviceScope.launch {
            delay(15000)
            currentRingtone?.stop()
            vibrator.cancel()
            // Restore original volume
            if (originalVolume != -1) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            }
            isRinging = false
            originalVolume = -1
        }
    }
}

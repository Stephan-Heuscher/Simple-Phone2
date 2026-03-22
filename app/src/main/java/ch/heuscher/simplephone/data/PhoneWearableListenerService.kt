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

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("PhoneWearableListener", "Received message: ${messageEvent.path}")
        
        when (messageEvent.path) {
            "/find_my_phone" -> startRingingAndVibrating()
            "/answer_call" -> {
                Log.d("PhoneWearableListener", "Watch requested to answer call")
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
        }
    }

    private fun startRingingAndVibrating() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Temporarily max out the volume
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

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
        CoroutineScope(Dispatchers.Main).launch {
            delay(15000)
            currentRingtone?.stop()
            vibrator.cancel()
            // Restore original volume
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
        }
    }
}

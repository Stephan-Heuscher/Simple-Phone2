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
import ch.heuscher.simplephone.FindPhoneActivity
import ch.heuscher.simplephone.call.CallService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Background listener service for Google Play Services Wear OS Data Layer messages.
 *
 * This service runs in the background on the phone, listening for incoming data layer message events
 * sent from the companion Wear OS application (e.g., call controls, utility actions).
 * It is automatically instantiated and managed by Google Play Services when a message is received
 * matching the service's registered intent filters in the AndroidManifest.xml.
 */
class PhoneWearableListenerService : WearableListenerService() {

    /**
     * Handles incoming messages from the Wear OS device via the Google Play Services Wearable API.
     *
     * This method acts as the entry point for the cross-device API contract. Based on the path of the
     * received [MessageEvent], it routes instructions to local device actions or [CallService] commands.
     *
     * Detailed specification of cross-device API contract paths handled here:
     * - `/find_my_phone`:
     *   - **Description**: Triggers a helper activity to locate the phone.
     *   - **Payload**: None.
     *   - **Behavior**: Launches [FindPhoneActivity] with `FLAG_ACTIVITY_NEW_TASK` and `FLAG_ACTIVITY_CLEAR_TOP`
     *     which plays an audible ringtone/sound and/or triggers vibration to locate the phone.
     *   - **Failure handling**: Catches and logs exceptions if the activity fails to start.
     * - `/answer_call`:
     *   - **Description**: Instructs the phone to answer the current active call.
     *   - **Payload**: None.
     *   - **Behavior**: Sets `CallService.watchAnswered = true` and `CallService.watchAnsweredAt` to
     *     the current elapsed real time, then calls `CallService.answerCall()`.
     * - `/reject_call`:
     *   - **Description**: Instructs the phone to reject the current incoming call.
     *   - **Payload**: None.
     *   - **Behavior**: Invokes `CallService.rejectCall()`.
     * - `/silence_ringer`:
     *   - **Description**: Instructs the phone to silence the active ringer.
     *   - **Payload**: None.
     *   - **Behavior**: Invokes `CallService.silenceRinger()`.
     * - `/end_call`:
     *   - **Description**: Instructs the phone to hang up/terminate the active call.
     *   - **Payload**: None.
     *   - **Behavior**: Invokes `CallService.endCall()`.
     * - `/set_audio_route`:
     *   - **Description**: Changes the active audio routing (e.g., to speaker, earpiece, bluetooth).
     *   - **Payload**: Byte array representing a string-encoded integer corresponding to a `CallAudioState` route.
     *   - **Behavior**: Decodes the route integer, records it in `CallService.watchRequestedAudioRoute`, and
     *     invokes `CallService.setAudioRoute(route)`.
     *   - **Failure handling**: Ignores message if payload cannot be parsed as an integer.
     * - `/volume_up`:
     *   - **Description**: Increases the phone's voice call audio stream volume.
     *   - **Payload**: None.
     *   - **Behavior**: Adjusts the volume using `AudioManager.STREAM_VOICE_CALL` and `AudioManager.ADJUST_RAISE`.
     *     Shows the system volume UI and requests updated audio status back to the wearable.
     * - `/volume_down`:
     *   - **Description**: Decreases the phone's voice call audio stream volume.
     *   - **Payload**: None.
     *   - **Behavior**: Adjusts the volume using `AudioManager.STREAM_VOICE_CALL` and `AudioManager.ADJUST_LOWER`.
     *     Shows the system volume UI and requests updated audio status back to the wearable.
     * - `/toggle_mute`:
     *   - **Description**: Toggles the phone's microphone mute status.
     *   - **Payload**: None.
     *   - **Behavior**: Inverts the state of `AudioManager.isMicrophoneMute` and requests updated audio status
     *     back to the wearable.
     * - `/request_audio_status`:
     *   - **Description**: Requests the current audio routing and volume status from the phone.
     *   - **Payload**: None.
     *   - **Behavior**: Invokes `CallService.requestAudioStatus()` to send a status update message back to the watch.
     * - `/initiate_call`:
     *   - **Description**: Initiates an outgoing voice call to the specified number.
     *   - **Payload**: Byte array representing a UTF-8 string containing the phone number.
     *   - **Behavior**:
     *     1. Decodes and trims the payload string.
     *     2. Cleans formatting characters (spaces, parentheses, dashes, dots, slashes).
     *     3. Validates that the number consists of valid dialing characters (`[0-9+*#,;]+`).
     *     4. Verifies `CALL_PHONE` permission.
     *     5. If granted, sets `CallService.watchInitiated = true` and `CallService.watchInitiatedAt`, then attempts
     *        to place the call via `TelecomManager.placeCall()`.
     *     6. If TelecomManager fails or throws an exception, falls back to launching `Intent.ACTION_DIAL` to open
     *        the dialer pre-populated with the number.
     *
     * @param messageEvent The message event received containing the path and optional payload data.
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("PhoneWearableListener", "Received message: ${messageEvent.path}")
        
        when (messageEvent.path) {
            "/find_my_phone" -> handleFindPhone()
            "/answer_call" -> handleAnswerCall()
            "/reject_call" -> handleRejectCall()
            "/silence_ringer" -> handleSilenceRinger()
            "/end_call" -> handleEndCall()
            "/set_audio_route" -> handleSetAudioRoute(messageEvent)
            "/volume_up" -> handleVolumeUp()
            "/volume_down" -> handleVolumeDown()
            "/toggle_mute" -> handleToggleMute()
            "/request_audio_status" -> handleRequestAudioStatus()
            "/initiate_call" -> handleInitiateCall(messageEvent)
        }
    }

    private fun handleFindPhone() {
        Log.d("PhoneWearableListener", "Watch requested to find phone. Launching FindPhoneActivity.")
        try {
            val intent = android.content.Intent(this, FindPhoneActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("PhoneWearableListener", "Failed to launch FindPhoneActivity", e)
        }
    }

    private fun handleAnswerCall() {
        Log.d("PhoneWearableListener", "Watch requested to answer call")
        CallService.watchAnswered = true
        CallService.watchAnsweredAt = android.os.SystemClock.elapsedRealtime()
        CallService.answerCall()
    }

    private fun handleRejectCall() {
        Log.d("PhoneWearableListener", "Watch requested to reject call")
        CallService.rejectCall()
    }

    private fun handleSilenceRinger() {
        Log.d("PhoneWearableListener", "Watch requested to silence ringer")
        CallService.silenceRinger()
    }

    private fun handleEndCall() {
        Log.d("PhoneWearableListener", "Watch requested to end call")
        CallService.endCall()
    }

    private fun handleSetAudioRoute(messageEvent: MessageEvent) {
        val route = String(messageEvent.data).toIntOrNull() ?: return
        Log.d("PhoneWearableListener", "Watch requested to set audio route to $route")
        CallService.watchRequestedAudioRoute = route
        CallService.setAudioRoute(route)
    }

    private fun handleVolumeUp() {
        Log.d("PhoneWearableListener", "Watch requested volume up")
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        CallService.requestAudioStatus()
    }

    private fun handleVolumeDown() {
        Log.d("PhoneWearableListener", "Watch requested volume down")
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        CallService.requestAudioStatus()
    }

    private fun handleToggleMute() {
        Log.d("PhoneWearableListener", "Watch requested toggle mute")
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute
        // Trigger an update back to the watch
        CallService.requestAudioStatus()
    }

    private fun handleRequestAudioStatus() {
        Log.d("PhoneWearableListener", "Watch requested audio status")
        CallService.requestAudioStatus()
    }

    private fun handleInitiateCall(messageEvent: MessageEvent) {
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
// Trigger deployment v2

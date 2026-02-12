package ch.heuscher.simplephone.call

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import ch.heuscher.simplephone.data.ContactRepository

/**
 * InCallService that handles incoming and outgoing calls.
 * This service is bound by the system when calls are made/received.
 */
class CallService : InCallService() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var isVibrating = false
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    
    // Proximity Sensor for Aggressive Speaker Switch
    private var sensorManager: android.hardware.SensorManager? = null
    private var proximitySensor: android.hardware.Sensor? = null
    private var isProximitySensorRegistered = false
    private var isPhoneAtEar = false // Track proximity state for logic updates
    
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var silenceRunnable: Runnable? = null

    companion object {
        // Track recent callers for repeat caller exception (number -> timestamp)
        private val recentCallers = mutableMapOf<String, Long>()
        private const val REPEAT_CALLER_WINDOW_MS = 15 * 60 * 1000L // 15 minutes
        private const val TAG = "CallService"
        private var instance: CallService? = null
        
        // Singleton to track current call state
        var currentCall: Call? = null
        
        var callState: Int = Call.STATE_DISCONNECTED
            
        var callerNumber: String? = null
            
        var callerName: String? = null

        var currentAudioState: CallAudioState? = null
            
        // Rename to shouldHighlightSpeaker for clarity, though we can keep the variable name if we want minimal diff, 
        // but user requested "remove all other changes" which implies a cleaner implementation.
        // Let's use the existing variable name 'shouldSuggestSpeaker' but change semantics to 'shouldHighlightSpeaker' to avoid massive rename across files immediately?
        // No, let's do it right. Rename to shouldHighlightSpeaker.
        
        var shouldHighlightSpeaker: Boolean = false
        
        // Removed userDeclinedSpeakerSuggestion as it's no longer needed (passive glow)
            
        // Listeners for call state changes
        private val callStateListeners = mutableListOf<CallStateListener>()
        
        fun addCallStateListener(listener: CallStateListener) {
            callStateListeners.add(listener)
            // Immediately notify with current state
            listener.onCallStateChanged(callState, callerNumber, callerName, currentAudioState)
            listener.onShouldSuggestSpeakerChanged(shouldHighlightSpeaker)
        }
        
        fun removeCallStateListener(listener: CallStateListener) {
            callStateListeners.remove(listener)
        }
        
        fun notifyCallStateChanged(disconnectCause: android.telecom.DisconnectCause? = null) {
            callStateListeners.forEach { 
                it.onCallStateChanged(callState, callerNumber, callerName, currentAudioState, disconnectCause) 
                it.onShouldSuggestSpeakerChanged(shouldHighlightSpeaker)
            }
        }
        
        // Removed dismissSpeakerSuggestion as it is no longer needed
        
        fun getShouldHighlightSpeakerState(): Boolean {
             return shouldHighlightSpeaker
        }
        
        fun answerCall() {
            val call = currentCall ?: return
            call.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
            
            val supportedRouteMask = currentAudioState?.supportedRouteMask ?: 0
            val route = if (supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0) {
                 CallAudioState.ROUTE_BLUETOOTH
            } else if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
                 CallAudioState.ROUTE_WIRED_HEADSET
            } else {
                 CallAudioState.ROUTE_EARPIECE
            }
            setAudioRoute(route)
        }
        
        fun rejectCall() {
            currentCall?.reject(false, null)
        }
        
        fun endCall() {
            currentCall?.disconnect()
        }
        
        fun setAudioRoute(route: Int) {
            @Suppress("DEPRECATION")
            instance?.setAudioRoute(route)
        }
        
        fun toggleMute() {
            // Mute is handled through audio routing
        }
        
        fun toggleSpeaker() {
            // Speaker is handled through audio routing
        }
        
        fun requestBluetoothAudio(device: android.bluetooth.BluetoothDevice) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                instance?.connectBluetoothDevice(device)
            } else {
                setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
            }
        }
        
        /**
         * Silence the ringer - called when volume down is pressed during an incoming call
         */
        fun silenceRinger() {
            instance?.stopRingingExternal()
        }

        fun sendDtmf(digit: Char) {
            currentCall?.playDtmfTone(digit)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                currentCall?.stopDtmfTone()
            }, 200)
        }
        

        }
        

    
    private val sensorEventListener = object : android.hardware.SensorEventListener {
        override fun onSensorChanged(event: android.hardware.SensorEvent?) {
            event ?: return
            
            if (event.sensor.type == android.hardware.Sensor.TYPE_PROXIMITY) {
                val distance = event.values[0]
                val maxRange = event.sensor.maximumRange
                // Some sensors report 0 for near, and maxRange for far. 
                // Others might have thresholds. Usually < 5cm is near.
                // Safest check: if distance < maxRange and distance < 5cm => NEAR
                
                val isNear = distance < maxRange && distance < 5.0f // 5cm threshold
                isPhoneAtEar = isNear
                
                updateSpeakerHighlightState()
            }
        }

        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {
            // No-op
        }
    }
    
    private fun updateSpeakerHighlightState() {
        val currentRoute = CallService.currentAudioState?.route ?: CallAudioState.ROUTE_EARPIECE
        Log.d(TAG, "UpdateGlow: AtEar=$isPhoneAtEar, Route=$currentRoute, State=$callState, Highlight=$shouldHighlightSpeaker")
        
        if (isPhoneAtEar) {
            // Phone is AT EAR
            // If on SPEAKER, switch to EARPIECE
            if (currentRoute == CallAudioState.ROUTE_SPEAKER) {
                Log.d(TAG, "UpdateGlow: Auto-switch to EARPIECE (Near)")
                setAudioRoute(CallAudioState.ROUTE_EARPIECE)
            }
            
            // Never highlight if near
            if (CallService.shouldHighlightSpeaker) {
                Log.d(TAG, "UpdateGlow: Setting FALSE (At Ear)")
                CallService.shouldHighlightSpeaker = false
                CallService.notifyCallStateChanged()
            }
        } else {
            // Phone is AWAY
            val isActiveOrDialing = CallService.callState == Call.STATE_ACTIVE || 
                                  CallService.callState == Call.STATE_DIALING || 
                                  CallService.callState == Call.STATE_CONNECTING
                                  
            if (isActiveOrDialing) {
                 val shouldHighlight = currentRoute != CallAudioState.ROUTE_SPEAKER
                 
                 if (CallService.shouldHighlightSpeaker != shouldHighlight) {
                     Log.d(TAG, "UpdateGlow: Changing to $shouldHighlight (Away, Not Speaker)")
                     CallService.shouldHighlightSpeaker = shouldHighlight
                     CallService.notifyCallStateChanged()
                 } else {
                     Log.d(TAG, "UpdateGlow: No change (Already $shouldHighlight)")
                 }
            } else {
                Log.d(TAG, "UpdateGlow: Not active/dialing")
            }
        }
    }
                


    fun connectBluetoothDevice(device: android.bluetooth.BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            super.requestBluetoothAudio(device)
        }
    }
    
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d(CallService.TAG, "Call state changed: $state")
            CallService.callState = state
            updateCallInfo(call)
            
            // Acquire/Release wake lock based on state
            updateWakeLock(state)
            
            if (state == Call.STATE_DISCONNECTED) {
                cancelOngoingCallNotification()
                CallService.notifyCallStateChanged(call.details.disconnectCause)
            } else {
                if (state == Call.STATE_ACTIVE || state == Call.STATE_DIALING) {
                    showOngoingCallNotification(call)
                }
                CallService.notifyCallStateChanged()
                updateSpeakerHighlightState() // Re-evaluate glow when call state changes (e.g. Ringing -> Active)
            }

            
            if (state != Call.STATE_RINGING) {
                stopRinging()
            }
            
            if (state == Call.STATE_DISCONNECTED) {
                CallService.currentCall = null
                CallService.callerNumber = null
                CallService.callerName = null
                stopRinging()
            }
        }
        
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            updateCallInfo(call)
            CallService.notifyCallStateChanged()
        }
    }
    


    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        // Initialize WakeLock for proximity sensor
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (powerManager.isWakeLockLevelSupported(android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                "SimplePhone:ServiceProximityWakeLock"
            )
        }
        


        
        // Initialize Sensor Manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_PROXIMITY)
        if (proximitySensor == null) {
            Log.w(TAG, "No proximity sensor found!")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopRinging()
        stopProximitySensor()
    }

    /**
     * Check if we should ring based on DND settings.
     * Delegates to DndRingPolicy for the actual decision logic (which is fully unit-tested).
     * Returns true if we should ring, false if DND blocks it.
     */
    private fun shouldRingForCall(callerNumber: String?): Boolean {
        val settingsRepository = ch.heuscher.simplephone.data.SettingsRepository(this)
        val contactRepository = ContactRepository(this)

        val policy = DndRingPolicy(
            blockUnknownCallers = settingsRepository.blockUnknownCallers,
            contactLookup = { number -> contactRepository.getContactByNumber(number) },
            recentCallers = recentCallers
        )

        // Build DND state from system
        val dndState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val interruptionFilter = notificationManager.currentInterruptionFilter

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                val sysPolicy = notificationManager.notificationPolicy
                DndRingPolicy.DndState(
                    interruptionFilter = interruptionFilter,
                    priorityCategoryCallsEnabled = sysPolicy.priorityCategories and
                            NotificationManager.Policy.PRIORITY_CATEGORY_CALLS != 0,
                    priorityCallSenders = sysPolicy.priorityCallSenders,
                    priorityCategoryRepeatCallersEnabled = sysPolicy.priorityCategories and
                            NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS != 0
                )
            } else {
                DndRingPolicy.DndState(interruptionFilter = interruptionFilter)
            }
        } else {
            DndRingPolicy.DndState() // Pre-M: default to FILTER_ALL (ring)
        }

        val decision = policy.shouldRing(callerNumber, dndState)
        Log.d(TAG, "DND Decision: shouldRing=${decision.shouldRing}, reason=${decision.reason}")
        return decision.shouldRing
    }

    private fun startRinging(callerNumber: String?) {
        // Check DND settings first
        if (!shouldRingForCall(callerNumber)) {
            Log.d(TAG, "Not ringing due to DND settings")
            return
        }
        
        // Start ringtone
        if (ringtone == null) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, uri)
            // Ensure ringtone uses correct audio attributes to respect system volume/DND
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
        }
        if (ringtone?.isPlaying == false) {
            ringtone?.play()
            
            // Schedule automatic silence if timeout is set
            val settingsRepository = ch.heuscher.simplephone.data.SettingsRepository(this)
            val timeoutSeconds = settingsRepository.ringtoneSilenceTimeout
            if (timeoutSeconds > 0) {
                Log.d(TAG, "Scheduling automatic silence after $timeoutSeconds seconds")
                silenceRunnable?.let { handler.removeCallbacks(it) }
                silenceRunnable = Runnable {
                    Log.d(TAG, "Automatic silence timeout reached")
                    stopRinging()
                }
                handler.postDelayed(silenceRunnable!!, timeoutSeconds * 1000L)
            }
        }
        
        // Start vibration
        startVibrating()
    }

    private fun startVibrating() {
        if (isVibrating) return
        
        // Double check AudioManager just in case we shouldn't vibrate
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        if (audioManager.ringerMode == android.media.AudioManager.RINGER_MODE_SILENT) {
             Log.d(TAG, "Not vibrating due to RingerMode SILENT")
             return
        }

        vibrator?.let { vib ->
            if (vib.hasVibrator()) {
                isVibrating = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Vibration pattern: wait 0ms, vibrate 500ms, wait 500ms, repeat
                    val pattern = longArrayOf(0, 500, 500, 500, 500)
                    vib.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat from index 0
                } else {
                    @Suppress("DEPRECATION")
                    val pattern = longArrayOf(0, 500, 500, 500, 500)
                    vib.vibrate(pattern, 0) // 0 = repeat from index 0
                }
            }
        }
    }

    private fun stopVibrating() {
        if (isVibrating) {
            vibrator?.cancel()
            isVibrating = false
        }
    }

    private fun stopRinging() {
        if (ringtone?.isPlaying == true) {
            ringtone?.stop()
        }
        stopVibrating()
        
        // Cancel any pending silence timer
        silenceRunnable?.let { 
            handler.removeCallbacks(it)
            silenceRunnable = null
        }
    }
    
    /**
     * Internal method called by companion object to silence the ringer
     */
    internal fun stopRingingExternal() {
        stopRinging()
    }
    
    private fun updateCallInfo(call: Call) {
        val details = call.details
        val handle = details?.handle
        CallService.callerNumber = handle?.schemeSpecificPart
        
        // Try to get caller name from gateway info or caller display name
        CallService.callerName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            details?.contactDisplayName
        } else {
            null
        }
    }

    override fun onCallAdded(call: Call) {
        Log.d(TAG, "Call added")
        
        // Block incoming calls if we already have an active/ringing call
        val activeCall = CallService.currentCall
        if (activeCall != null && activeCall.state != Call.STATE_DISCONNECTED && call.state == Call.STATE_RINGING) {
            Log.i(TAG, "Blocking incoming call: Already in a call (state=${activeCall.state})")
            showMissedCallNotification(call)
            call.reject(false, null)
            return
        }

        shouldHighlightSpeaker = false
        
        // Check blocking immediately
        updateCallInfo(call)
        
        // Only check for blocking on INCOMING calls
        if (call.state == Call.STATE_RINGING) {
            if (!shouldRingForCall(CallService.callerNumber)) {
                 // If shouldRing returns false, it might be DND or our Blocking.
                 // We should check if it's our blocking specifically to reject.
                 // Re-checking blocking logic to be precise
                 val settingsRepository = ch.heuscher.simplephone.data.SettingsRepository(this)
                 var isBlocked = false
                 if (settingsRepository.blockUnknownCallers) {
                     val number = CallService.callerNumber?.replace(Regex("[^0-9+]"), "")
                     if (number.isNullOrEmpty()) {
                         isBlocked = true
                     } else {
                         val contactRepository = ContactRepository(this)
                         val contact = contactRepository.getContactByNumber(number)
                         if (contact == null) isBlocked = true
                     }
                 }
                 
                 if (isBlocked) {
                     Log.i(TAG, "Rejecting blocked call from ${CallService.callerNumber}")
                     val blockedNumber = CallService.callerNumber ?: "Unknown"
                     settingsRepository.lastBlockedNumber = blockedNumber
                     showBlockedCallNotification(blockedNumber)
                     
                     call.reject(false, null)
                     // It will be logged by system or we can let it be logged as rejected
                     return
                 }
            }
        }

        CallService.currentCall = call
        call.registerCallback(callCallback)
        CallService.callState = call.state
        updateCallInfo(call)
        
        // Acquire wake lock for any new call
        updateWakeLock(call.state)
        
        // Start monitoring sensor for phone orientation / proximity
        startProximitySensor()
        
        CallService.notifyCallStateChanged()
        
        if (call.state == Call.STATE_RINGING) {
            startRinging(CallService.callerNumber)
        }
        
        // Launch the incoming call activity
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra("caller_number", CallService.callerNumber)
            putExtra("caller_name", CallService.callerName)
            putExtra("is_incoming", call.state == Call.STATE_RINGING)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        Log.d(TAG, "Call removed")
        call.unregisterCallback(callCallback)
        stopRinging()
        
        // Release wake lock
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        

        
        // Release wake lock
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        
        stopProximitySensor()
        shouldHighlightSpeaker = false
        
        // Show our own missed call notification since we are the default dialer.
        // The system won't show one when we handle calls.
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
        val isDefaultDialer = telecomManager.defaultDialerPackage == packageName

        // callDirection requires API 29+
        if (isDefaultDialer && 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            call.details.callDirection == Call.Details.DIRECTION_INCOMING && 
            call.details.disconnectCause.code == android.telecom.DisconnectCause.MISSED) {
            showMissedCallNotification(call)
        }
        
        if (currentCall == call) {
            // Capture disconnect cause before clearing currentCall
            val disconnectCause = call.details.disconnectCause
            
            currentCall = null
            callState = Call.STATE_DISCONNECTED
            callerNumber = null
            callerName = null
            notifyCallStateChanged(disconnectCause)
        }
    }
    
    private fun updateWakeLock(state: Int) {
        if (state == Call.STATE_ACTIVE || 
            state == Call.STATE_DIALING || 
            state == Call.STATE_RINGING) {
            if (wakeLock?.isHeld == false) {
                try {
                    wakeLock?.acquire(60 * 60 * 1000L /* 1 hour max */)
                    Log.d(TAG, "WakeLock acquired")
                } catch (e: Exception) {
                    Log.e(TAG, "Error acquiring wake lock", e)
                }
            }
        } else if (state == Call.STATE_DISCONNECTED) {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        }
    }
    

    
    private fun showMissedCallNotification(call: Call) {
        val context = this
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "missed_calls"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Missed Calls",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for missed calls"
                enableLights(true)
                lightColor = android.graphics.Color.RED
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val handle = call.details.handle
        val number = handle?.schemeSpecificPart ?: "Unknown"
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            call.details.contactDisplayName ?: ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(number)
        } else {
            ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(number)
        }
        
        // Try to get contact photo
        val contactRepository = ContactRepository(context)
        val contact = contactRepository.getContactByNumber(number)
        
        // Load contact photo bitmap
        var contactBitmap: Bitmap? = null
        if (contact?.imageUri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(contact.imageUri))
                contactBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                // Make it circular for the notification
                contactBitmap = createCircularBitmap(contactBitmap!!)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load contact photo", e)
            }
        }
        
        val displayName = contact?.name ?: name
        
        val intent = Intent(context, ch.heuscher.simplephone.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent, 
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Call Back Action
        val callBackIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        val callBackPendingIntent = android.app.PendingIntent.getActivity(
            context, 1, callBackIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notificationId = number.replace(Regex("[^0-9]"), "").hashCode()

        // Ignore Action (Dismiss)
        val ignoreIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "IGNORE"
            putExtra("notification_id", notificationId)
        }
        val ignorePendingIntent = android.app.PendingIntent.getBroadcast(
            context, 2, ignoreIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("Missed Call")
            .setContentText(displayName)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.sym_action_call, "Call Back", callBackPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Ignore", ignorePendingIntent)
        
        // Add large icon (contact photo) if available
        if (contactBitmap != null) {
            notificationBuilder.setLargeIcon(contactBitmap)
        }
        
        val notification = notificationBuilder.build()
            
        notificationManager.notify(notificationId, notification)
    }

    private val ONGOING_NOTIFICATION_ID = 12345

    private fun showOngoingCallNotification(call: Call) {
        val context = this
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "ongoing_calls"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Ongoing Calls",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for ongoing calls"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 100, intent, 
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_call_outgoing)
            .setContentTitle("Ongoing Call")
            .setContentText(callerName ?: ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(callerNumber ?: "Unknown"))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
            
        notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
    }

    private val BLOCKED_CALL_NOTIFICATION_ID = 54321

    private fun showBlockedCallNotification(number: String) {
        val context = this
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "blocked_calls"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Blocked Calls",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for blocked calls"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, ch.heuscher.simplephone.MainActivity::class.java).apply {
             flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context, 0, intent, 
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle(context.getString(ch.heuscher.simplephone.R.string.notification_blocked_call_title))
            .setContentText(context.getString(ch.heuscher.simplephone.R.string.notification_blocked_call_content, ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(number)))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(BLOCKED_CALL_NOTIFICATION_ID, notification)
    }

    private fun cancelOngoingCallNotification() {
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(ONGOING_NOTIFICATION_ID)
    }
    
    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // Draw circle
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(rect, size / 2f, size / 2f, paint)
        
        // Draw bitmap with SRC_IN to clip to circle
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        // Center crop the bitmap
        val srcLeft = (bitmap.width - size) / 2
        val srcTop = (bitmap.height - size) / 2
        val srcRect = Rect(srcLeft, srcTop, srcLeft + size, srcTop + size)
        val dstRect = Rect(0, 0, size, size)
        
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        
        return output
    }
    
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        Log.d(TAG, "Audio state changed: ${audioState?.route}")
        CallService.currentAudioState = audioState
        updateSpeakerHighlightState() // Re-evaluate glow when audio route changes
        CallService.notifyCallStateChanged()
    }
    private fun startProximitySensor() {
        if (!isProximitySensorRegistered && proximitySensor != null) {
            sensorManager?.registerListener(sensorEventListener, proximitySensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL)
            isProximitySensorRegistered = true
            Log.d(TAG, "Proximity sensor registered")
        }
    }
    
    private fun stopProximitySensor() {
        if (isProximitySensorRegistered) {
            sensorManager?.unregisterListener(sensorEventListener)
            isProximitySensorRegistered = false
            Log.d(TAG, "Proximity sensor unregistered")
        }
    }

    }



interface CallStateListener {
    fun onCallStateChanged(state: Int, number: String?, name: String?, audioState: CallAudioState?, disconnectCause: android.telecom.DisconnectCause? = null)
    fun onShouldSuggestSpeakerChanged(shouldSuggest: Boolean) {}
}

package com.example.simplephone.call

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
import com.example.simplephone.data.ContactRepository

/**
 * InCallService that handles incoming and outgoing calls.
 * This service is bound by the system when calls are made/received.
 */
class CallService : InCallService() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var isVibrating = false

    companion object {
        // Track recent callers for repeat caller exception (number -> timestamp)
        private val recentCallers = mutableMapOf<String, Long>()
        private const val REPEAT_CALLER_WINDOW_MS = 15 * 60 * 1000L // 15 minutes
        private const val TAG = "CallService"
        private var instance: CallService? = null
        
        // Singleton to track current call state
        var currentCall: Call? = null
            private set
        
        var callState: Int = Call.STATE_DISCONNECTED
            private set
            
        var callerNumber: String? = null
            private set
            
        var callerName: String? = null
            private set

        var currentAudioState: CallAudioState? = null
            private set
            
        // Listeners for call state changes
        private val callStateListeners = mutableListOf<CallStateListener>()
        
        fun addCallStateListener(listener: CallStateListener) {
            callStateListeners.add(listener)
            // Immediately notify with current state
            listener.onCallStateChanged(callState, callerNumber, callerName, currentAudioState)
        }
        
        fun removeCallStateListener(listener: CallStateListener) {
            callStateListeners.remove(listener)
        }
        
        private fun notifyCallStateChanged() {
            callStateListeners.forEach { 
                it.onCallStateChanged(callState, callerNumber, callerName, currentAudioState) 
            }
        }
        
        fun answerCall() {
            currentCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
        }
        
        fun rejectCall() {
            currentCall?.reject(false, null)
        }
        
        fun endCall() {
            currentCall?.disconnect()
        }
        
        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }
        
        fun toggleMute() {
            // Mute is handled through audio routing
        }
        
        fun toggleSpeaker() {
            // Speaker is handled through audio routing
        }
    }
    
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d(TAG, "Call state changed: $state")
            callState = state
            updateCallInfo(call)
            notifyCallStateChanged()
            
            if (state != Call.STATE_RINGING) {
                stopRinging()
            }
            
            if (state == Call.STATE_DISCONNECTED) {
                currentCall = null
                callerNumber = null
                callerName = null
                stopRinging()
            }
        }
        
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            updateCallInfo(call)
            notifyCallStateChanged()
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
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopRinging()
    }

    /**
     * Check if we should ring based on DND settings
     * Returns true if we should ring, false if DND blocks it
     */
    private fun shouldRingForCall(callerNumber: String?): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Check DND status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val interruptionFilter = notificationManager.currentInterruptionFilter
            
            // If DND is off (INTERRUPTION_FILTER_ALL), always ring
            if (interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                return true
            }
            
            // If DND is on but allows alarms only or none, don't ring
            if (interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
                interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                return false
            }
            
            // INTERRUPTION_FILTER_PRIORITY - check if this is a repeat caller
            if (interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
                // Check for repeat caller exception
                if (callerNumber != null) {
                    val normalizedNumber = callerNumber.replace(Regex("[^0-9+]"), "")
                    val lastCallTime = recentCallers[normalizedNumber]
                    val now = System.currentTimeMillis()
                    
                    if (lastCallTime != null && (now - lastCallTime) <= REPEAT_CALLER_WINDOW_MS) {
                        // This is a repeat caller within 15 minutes - ring anyway
                        Log.d(TAG, "Repeat caller detected, allowing ring despite DND")
                        return true
                    }
                    
                    // Record this call for repeat caller detection
                    recentCallers[normalizedNumber] = now
                    
                    // Clean up old entries
                    val cutoff = now - REPEAT_CALLER_WINDOW_MS
                    recentCallers.entries.removeIf { it.value < cutoff }
                }
                
                // DND is on and this is not a repeat caller - respect DND
                // The system's DND policy will determine if we should ring
                // We'll check if calls are allowed in priority mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val policy = notificationManager.notificationPolicy
                    if (policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_CALLS != 0) {
                        // Calls are allowed in priority mode
                        return true
                    }
                    if (policy.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS != 0) {
                        // Repeat callers are allowed but this isn't one
                        return false
                    }
                }
                return false
            }
        }
        
        return true
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
        }
        if (ringtone?.isPlaying == false) {
            ringtone?.play()
        }
        
        // Start vibration
        startVibrating()
    }

    private fun startVibrating() {
        if (isVibrating) return
        
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
    }
    
    private fun updateCallInfo(call: Call) {
        val details = call.details
        val handle = details?.handle
        callerNumber = handle?.schemeSpecificPart
        
        // Try to get caller name from gateway info or caller display name
        callerName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            details?.contactDisplayName
        } else {
            null
        }
    }

    override fun onCallAdded(call: Call) {
        Log.d(TAG, "Call added")
        currentCall = call
        call.registerCallback(callCallback)
        callState = call.state
        updateCallInfo(call)
        notifyCallStateChanged()
        
        if (call.state == Call.STATE_RINGING) {
            startRinging(callerNumber)
        }
        
        // Launch the incoming call activity
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("caller_number", callerNumber)
            putExtra("caller_name", callerName)
            putExtra("is_incoming", call.state == Call.STATE_RINGING)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        Log.d(TAG, "Call removed")
        call.unregisterCallback(callCallback)
        stopRinging()
        
        // Show our own missed call notification since we are the default dialer.
        // The system won't show one when we handle calls.
        if (call.details.callDirection == Call.Details.DIRECTION_INCOMING && 
            call.details.disconnectCause.code == android.telecom.DisconnectCause.MISSED) {
            showMissedCallNotification(call)
        }
        
        if (currentCall == call) {
            currentCall = null
            callState = Call.STATE_DISCONNECTED
            callerNumber = null
            callerName = null
            notifyCallStateChanged()
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
            call.details.contactDisplayName ?: number
        } else {
            number
        }
        
        // Try to get contact photo
        val contactRepository = ContactRepository(context)
        val contacts = contactRepository.getContacts()
        val normalizedNumber = number.replace(Regex("[^0-9+]"), "")
        val contact = contacts.find { 
            val normalizedContactNumber = it.number.replace(Regex("[^0-9+]"), "")
            if (normalizedNumber.length > 6 && normalizedContactNumber.length > 6) {
                normalizedNumber.endsWith(normalizedContactNumber) || normalizedContactNumber.endsWith(normalizedNumber)
            } else {
                normalizedNumber == normalizedContactNumber
            }
        }
        
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
        
        val intent = Intent(context, com.example.simplephone.MainActivity::class.java).apply {
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
        
        // Ignore Action (Dismiss)
        val ignoreIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "IGNORE"
            putExtra("notification_id", number.hashCode())
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
            
        notificationManager.notify(number.hashCode(), notification)
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
        currentAudioState = audioState
        notifyCallStateChanged()
    }
}

interface CallStateListener {
    fun onCallStateChanged(state: Int, number: String?, name: String?, audioState: CallAudioState?)
}

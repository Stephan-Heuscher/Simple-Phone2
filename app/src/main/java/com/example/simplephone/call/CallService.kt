package com.example.simplephone.call

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
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

    companion object {
        private const val TAG = "CallService"
        
        // Singleton to track current call state
        var currentCall: Call? = null
            private set
        
        var callState: Int = Call.STATE_DISCONNECTED
            private set
            
        var callerNumber: String? = null
            private set
            
        var callerName: String? = null
            private set
            
        // Listeners for call state changes
        private val callStateListeners = mutableListOf<CallStateListener>()
        
        fun addCallStateListener(listener: CallStateListener) {
            callStateListeners.add(listener)
        }
        
        fun removeCallStateListener(listener: CallStateListener) {
            callStateListeners.remove(listener)
        }
        
        private fun notifyCallStateChanged() {
            callStateListeners.forEach { 
                it.onCallStateChanged(callState, callerNumber, callerName) 
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
            
            if (state == Call.STATE_DISCONNECTED) {
                currentCall = null
                callerNumber = null
                callerName = null
            }
        }
        
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            updateCallInfo(call)
            notifyCallStateChanged()
        }
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
        
        // Check for missed call
        if (call.details.callDirection == Call.Details.DIRECTION_INCOMING && 
            (call.details.disconnectCause.code == android.telecom.DisconnectCause.MISSED || 
             call.details.disconnectCause.code == android.telecom.DisconnectCause.REJECTED)) {
             
             // Only show notification if it was actually missed (not rejected by user)
             // But some phones report rejected as missed, so we might need to be careful
             // For now, let's trust the disconnect cause
             if (call.details.disconnectCause.code == android.telecom.DisconnectCause.MISSED) {
                 showMissedCallNotification(call)
             }
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
        
        val notificationBuilder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("Missed Call")
            .setContentText(displayName)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
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
    }
}

interface CallStateListener {
    fun onCallStateChanged(state: Int, number: String?, name: String?)
}

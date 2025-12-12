package com.example.simplephone.call

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log

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
        if (currentCall == call) {
            currentCall = null
            callState = Call.STATE_DISCONNECTED
            callerNumber = null
            callerName = null
            notifyCallStateChanged()
        }
    }
    
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        Log.d(TAG, "Audio state changed: ${audioState?.route}")
    }
}

interface CallStateListener {
    fun onCallStateChanged(state: Int, number: String?, name: String?)
}

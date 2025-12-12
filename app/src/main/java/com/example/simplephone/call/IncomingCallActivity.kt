package com.example.simplephone.call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.Call
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.simplephone.data.ContactRepository
import com.example.simplephone.model.Contact
import com.example.simplephone.ui.components.ContactAvatar
import com.example.simplephone.ui.theme.GreenCall
import com.example.simplephone.ui.theme.RedHangup
import com.example.simplephone.ui.theme.SimplePhoneTheme

/**
 * Full-screen activity for incoming and active calls.
 * Designed for accessibility with large buttons and clear visuals.
 */
class IncomingCallActivity : ComponentActivity(), CallStateListener {
    
    private var callerNumber by mutableStateOf<String?>(null)
    private var callerName by mutableStateOf<String?>(null)
    private var isIncoming by mutableStateOf(true)
    private var callState by mutableStateOf(Call.STATE_RINGING)
    private var contact by mutableStateOf<Contact?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        
        callerNumber = intent.getStringExtra("caller_number")
        callerName = intent.getStringExtra("caller_name")
        isIncoming = intent.getBooleanExtra("is_incoming", true)
        
        // Try to find contact from phone number
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
            == PackageManager.PERMISSION_GRANTED) {
            val contactRepository = ContactRepository(this)
            val contacts = contactRepository.getContacts()
            contact = contacts.find { normalizeNumber(it.number) == normalizeNumber(callerNumber ?: "") }
            if (contact != null && callerName == null) {
                callerName = contact?.name
            }
        }
        
        CallService.addCallStateListener(this)
        
        setContent {
            SimplePhoneTheme(darkThemeOption = 2) { // Force Dark Mode for calls
                CallScreen(
                    callerNumber = callerNumber,
                    callerName = callerName ?: callerNumber ?: "Unknown",
                    contact = contact,
                    isIncoming = isIncoming && callState == Call.STATE_RINGING,
                    callState = callState,
                    onAnswer = {
                        CallService.answerCall()
                        isIncoming = false
                    },
                    onReject = {
                        CallService.rejectCall()
                        finish()
                    },
                    onHangup = {
                        CallService.endCall()
                        finish()
                    }
                )
            }
        }
    }
    
    private fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        CallService.removeCallStateListener(this)
    }
    
    override fun onCallStateChanged(state: Int, number: String?, name: String?) {
        callState = state
        if (number != null) callerNumber = number
        if (name != null) callerName = name
        
        if (state == Call.STATE_DISCONNECTED) {
            finish()
        }
    }
}

@Composable
fun CallScreen(
    callerNumber: String?,
    callerName: String,
    contact: Contact?,
    isIncoming: Boolean,
    callState: Int,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onHangup: () -> Unit
) {
    val statusText = when (callState) {
        Call.STATE_RINGING -> "Incoming Call"
        Call.STATE_DIALING -> "Calling..."
        Call.STATE_ACTIVE -> "On Call"
        Call.STATE_HOLDING -> "On Hold"
        Call.STATE_CONNECTING -> "Connecting..."
        else -> ""
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Status
        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        // Middle: Contact info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Contact avatar
            if (contact != null) {
                ContactAvatar(
                    contact = contact,
                    size = 160.dp,
                    showFavoriteStar = false
                )
            } else {
                // Default avatar
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = callerName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Caller name - very large
            Text(
                text = callerName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            // Caller number (if different from name)
            if (callerNumber != null && callerNumber != callerName) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = callerNumber,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
        
        // Bottom: Action buttons
        if (isIncoming) {
            // Incoming call: Reject and Answer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Reject button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(RedHangup)
                            .clickable { onReject() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "Reject call",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Reject",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RedHangup
                    )
                }
                
                // Answer button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(GreenCall)
                            .clickable { onAnswer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Answer call",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Answer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GreenCall
                    )
                }
            }
        } else {
            // Active call: Only hangup button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(RedHangup)
                        .clickable { onHangup() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "End Call",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RedHangup
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

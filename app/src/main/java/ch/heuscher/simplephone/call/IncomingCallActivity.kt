package ch.heuscher.simplephone.call

import android.content.Context
import android.os.PowerManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.view.WindowManager
import android.view.KeyEvent
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.compose.ui.platform.LocalContext
import ch.heuscher.simplephone.R
import ch.heuscher.simplephone.ui.utils.vibrate
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.ContactAvatar
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.RedHangup
import ch.heuscher.simplephone.ui.theme.SimplePhoneTheme

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
    private var audioState by mutableStateOf<CallAudioState?>(null)
    
    private var wakeLock: PowerManager.WakeLock? = null // Removed, but kept variable to avoid compilation error if used elsewhere, actually I should remove it.
    // Wait, I am replacing the whole class body parts.
    
    private var textToSpeech: android.speech.tts.TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Removed local WakeLock logic as it is now handled in CallService
        
        // Initialize TextToSpeech
        textToSpeech = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                textToSpeech?.language = java.util.Locale.getDefault()
            }
        }

        // Show over lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        callerNumber = intent.getStringExtra("caller_number")
        callerName = intent.getStringExtra("caller_name")
        isIncoming = intent.getBooleanExtra("is_incoming", true)
        audioState = CallService.currentAudioState
        
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
                    audioState = audioState,
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
                    },
                    onAudioRouteSelected = { route ->
                        CallService.setAudioRoute(route)
                    }
                )
            }
        }
    }
    
    private fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (isIncoming && callState == Call.STATE_RINGING) {
                // Silence the ringer using CallService
                CallService.silenceRinger()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        CallService.removeCallStateListener(this)
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
    
    override fun onCallStateChanged(state: Int, number: String?, name: String?, audioState: CallAudioState?, disconnectCause: android.telecom.DisconnectCause?) {
        callState = state
        if (number != null) callerNumber = number
        if (name != null) callerName = name
        this.audioState = audioState
        
        // WakeLock is now managed by CallService

        if (state == Call.STATE_DISCONNECTED) {
            if (disconnectCause != null && disconnectCause.code != android.telecom.DisconnectCause.LOCAL && disconnectCause.code != android.telecom.DisconnectCause.REMOTE) {
                // Something went wrong or busy
                val reason = disconnectCause.label?.toString() ?: when(disconnectCause.code) {
                    android.telecom.DisconnectCause.BUSY -> "User is Busy"
                    android.telecom.DisconnectCause.REJECTED -> "Call Rejected"
                    android.telecom.DisconnectCause.ERROR -> "Call Error"
                    else -> "Call Ended"
                }
                
                // Show transparent message (Toast is effectively a transparent overlay message)
                android.widget.Toast.makeText(this, reason, android.widget.Toast.LENGTH_LONG).show()
                
                // Speak it
                textToSpeech?.speak(reason, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                
                // Delay finish slightly to allow TTS to start
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    finish()
                }, 3000)
            } else {
                finish()
            }
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
    audioState: CallAudioState?,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onHangup: () -> Unit,
    onAudioRouteSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    
    // i18n status text
    val statusText = when (callState) {
        Call.STATE_RINGING -> stringResource(R.string.incoming_call)
        Call.STATE_DIALING -> stringResource(R.string.calling)
        Call.STATE_ACTIVE -> stringResource(R.string.on_call)
        Call.STATE_HOLDING -> stringResource(R.string.on_hold)
        Call.STATE_CONNECTING -> stringResource(R.string.connecting)
        else -> ""
    }
    
    // Check if this is a known contact (has a real name, not just a number)
    val isKnownContact = contact != null
    val displayName = if (isKnownContact) callerName else (callerNumber ?: stringResource(R.string.unknown_contact))
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top: Status - large and clear
        Text(
            text = statusText,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Middle: Contact info - main focus area
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Contact avatar - show favorite star for favorites
            if (contact != null) {
                ContactAvatar(
                    contact = contact,
                    size = 180.dp,
                    showFavoriteStar = contact.isFavorite
                )
            } else {
                // Default avatar for unknown contacts
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Caller name - very large, 2 lines max with horizontal scroll
            val nameScrollState = androidx.compose.foundation.rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.horizontalScroll(nameScrollState)
                    )
                    // Show subtle scrollbar if content overflows
                    if (nameScrollState.maxValue > 0) {
                        ch.heuscher.simplephone.ui.components.HorizontalScrollbar(
                            scrollState = nameScrollState,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            // Only show phone number for UNKNOWN contacts
            if (!isKnownContact && callerNumber != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = callerNumber,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Audio Controls - simplified, only show when active
        if (audioState != null && callState == Call.STATE_ACTIVE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val route = audioState.route
                val supportedRouteMask = audioState.supportedRouteMask
                
                // Speaker
                if (supportedRouteMask and CallAudioState.ROUTE_SPEAKER != 0) {
                    val isSelected = route == CallAudioState.ROUTE_SPEAKER
                    AudioRouteButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = stringResource(R.string.speaker),
                        isSelected = isSelected,
                        onClick = { 
                            vibrate(context)
                            onAudioRouteSelected(CallAudioState.ROUTE_SPEAKER) 
                        }
                    )
                }
                
                // Bluetooth
                if (supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0) {
                    val isSelected = route == CallAudioState.ROUTE_BLUETOOTH
                    AudioRouteButton(
                        icon = Icons.Filled.Bluetooth,
                        label = stringResource(R.string.bluetooth),
                        isSelected = isSelected,
                        onClick = { 
                            vibrate(context)
                            onAudioRouteSelected(CallAudioState.ROUTE_BLUETOOTH) 
                        }
                    )
                }
                
                // Earpiece / Wired Headset
                if (supportedRouteMask and CallAudioState.ROUTE_EARPIECE != 0 || supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
                    val isSelected = route == CallAudioState.ROUTE_EARPIECE || route == CallAudioState.ROUTE_WIRED_HEADSET
                    val targetRoute = if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) CallAudioState.ROUTE_WIRED_HEADSET else CallAudioState.ROUTE_EARPIECE
                    AudioRouteButton(
                        icon = Icons.Filled.PhoneInTalk,
                        label = stringResource(R.string.earpiece),
                        isSelected = isSelected,
                        onClick = { 
                            vibrate(context)
                            onAudioRouteSelected(targetRoute) 
                        }
                    )
                }
            }
        }

        // Bottom: Action buttons - larger and clearer
        if (isIncoming) {
            // Incoming call: Reject and Answer buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Reject button
                CallActionButton(
                    icon = Icons.Filled.CallEnd,
                    label = stringResource(R.string.reject),
                    backgroundColor = RedHangup,
                    onClick = {
                        vibrate(context)
                        onReject()
                    }
                )
                
                // Answer button
                CallActionButton(
                    icon = Icons.Filled.Call,
                    label = stringResource(R.string.answer),
                    backgroundColor = GreenCall,
                    onClick = {
                        vibrate(context)
                        onAnswer()
                    }
                )
            }
        } else {
            // Active call: Only hangup button - centered
            CallActionButton(
                icon = Icons.Filled.CallEnd,
                label = stringResource(R.string.end_call),
                backgroundColor = RedHangup,
                onClick = {
                    vibrate(context)
                    onHangup()
                }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Large, accessible call action button (Answer/Reject/End Call)
 */
@Composable
fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = backgroundColor
        )
    }
}

/**
 * Audio route selection button with label
 */
@Composable
fun AudioRouteButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
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
import ch.heuscher.simplephone.ui.theme.HighContrastBlue
import ch.heuscher.simplephone.ui.theme.RedHangup
import ch.heuscher.simplephone.ui.theme.SimplePhoneTheme
import ch.heuscher.simplephone.data.SettingsRepository

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
    private var shouldSuggestSpeaker by mutableStateOf(false)
    
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
            contact = contactRepository.getContactByNumber(callerNumber)
            if (contact != null && callerName == null) {
                callerName = contact?.name
            }
        }
        
        CallService.addCallStateListener(this)
        
        // Read setting for simplified call screen
        val settingsRepository = SettingsRepository(this)
        val useSimplifiedScreen = settingsRepository.simplifiedContactCallScreen
        
        setContent {
            
            SimplePhoneTheme(darkThemeOption = 2) { // Force Dark Mode for calls
                CallScreen(
                    callerNumber = callerNumber,
                    callerName = callerName ?: ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(callerNumber ?: ""),
                    contact = contact,
                    isIncoming = isIncoming && callState == Call.STATE_RINGING,
                    callState = callState,
                    audioState = audioState,
                    shouldHighlightSpeaker = shouldSuggestSpeaker, // Mapping to UI param
                    useSimplifiedScreen = useSimplifiedScreen,
                    onAnswer = {
                        CallService.answerCall()
                        isIncoming = false
                    },
                    onReject = {
                        CallService.rejectCall()
                        finish()
                    },
                    onSilence = {
                        CallService.silenceRinger()
                    },
                    onHangup = {
                        CallService.endCall()
                        finish()
                    },
                    onAudioRouteSelected = { route ->
                        CallService.setAudioRoute(route)
                    },
                    onBluetoothDeviceSelected = { device ->
                        CallService.requestBluetoothAudio(device)
                    },
                    onDtmfClick = { digit ->
                        CallService.sendDtmf(digit)
                    }
                )
            }
        }
    }
    
    private fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check setting for silence on touch/keys
        val settingsRepository = SettingsRepository(this)
        if (settingsRepository.silenceCallOnTouch && isIncoming && callState == Call.STATE_RINGING) {
             // Silence on ANY key press (except power which is handled by system, but VOLUME handled here)
             CallService.silenceRinger()
             // We don't return true (consume) for all keys to avoid breaking system behavior,
             // except for volume which is standard behavior to consume.
             if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                 return true
             }
        }
        
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (isIncoming && callState == Call.STATE_RINGING) {
                // Silence the ringer using CallService
                CallService.silenceRinger()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            val settingsRepository = SettingsRepository(this)
            if (settingsRepository.silenceCallOnTouch && isIncoming && callState == Call.STATE_RINGING) {
                CallService.silenceRinger()
            }
        }
        return super.dispatchTouchEvent(ev)
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
            shouldSuggestSpeaker = false // clear dialog
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
    
    override fun onShouldSuggestSpeakerChanged(shouldSuggest: Boolean) {
        shouldSuggestSpeaker = shouldSuggest
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
    shouldHighlightSpeaker: Boolean = false,
    useSimplifiedScreen: Boolean = false,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onSilence: () -> Unit = {},
    onHangup: () -> Unit,

    onAudioRouteSelected: (Int) -> Unit,
    onBluetoothDeviceSelected: (android.bluetooth.BluetoothDevice) -> Unit,
    onDtmfClick: (Char) -> Unit = {}
) {
    val context = LocalContext.current
    var showKeypad by remember { mutableStateOf(false) }
    
    // Show keypad overlay if active
    if (showKeypad && callState == Call.STATE_ACTIVE) {
        DtmfKeypadOverlay(
            onDismiss = { showKeypad = false },
            onKeyClick = { key ->
                vibrate(context)
                onDtmfClick(key)
            }
        )
        return
    }
    
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
            
            // Caller name - very large, 2 lines max
            Text(
                text = displayName,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            
            // Only show phone number for UNKNOWN contacts
            if (!isKnownContact && callerNumber != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(callerNumber),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Audio Controls - visible during dialing, ringing, active/holding
        val showAudioControls = audioState != null && (
            callState == Call.STATE_ACTIVE || 
            callState == Call.STATE_DIALING || 
            callState == Call.STATE_RINGING || 
            callState == Call.STATE_CONNECTING ||
            callState == Call.STATE_HOLDING
        )
        
        if (showAudioControls) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center, // Centered if few items, but scrollable if many
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add some padding at the start so items aren't stuck to the edge if scrolled
                Spacer(modifier = Modifier.width(16.dp))
                
                val route = audioState.route
                val supportedRouteMask = audioState.supportedRouteMask
                
                // Speaker
                if (supportedRouteMask and CallAudioState.ROUTE_SPEAKER != 0) {
                    val isSelected = route == CallAudioState.ROUTE_SPEAKER
                    AudioRouteButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = stringResource(R.string.speaker),
                        isSelected = isSelected,
                        shouldHighlight = shouldHighlightSpeaker && !isSelected, // Highlight if suggested and NOT already selected
                        onClick = { 
                            vibrate(context)
                            onAudioRouteSelected(CallAudioState.ROUTE_SPEAKER) 
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
                
                // Bluetooth Devices
                var showedBluetooth = false
                
                // API 28+ supports listing devices
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val devices = audioState.supportedBluetoothDevices
                    if (!devices.isNullOrEmpty()) {
                        showedBluetooth = true
                        devices.forEach { device ->
                            // Check if this specific device is active
                            // Note: activeBluetoothDevice is available on API 28+
                            val isSelected = audioState.activeBluetoothDevice?.address == device.address
                            
                            // Use device name or address or fallback
                            @Suppress("MissingPermission") // Logic usually requires permissions but inside CallService context it works, visual check needed
                            val label = device.name ?: device.address ?: "Bluetooth"
                            
                            AudioRouteButton(
                                icon = Icons.Filled.Bluetooth,
                                label = label,
                                isSelected = isSelected,
                                onClick = {
                                    vibrate(context)
                                    onBluetoothDeviceSelected(device)
                                },
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                }
                
                // Fallback for pre-API 28 OR if no devices list available but ROUTE_BLUETOOTH is supported
                if (!showedBluetooth && (supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0)) {
                    val isSelected = route == CallAudioState.ROUTE_BLUETOOTH
                    AudioRouteButton(
                        icon = Icons.Filled.Bluetooth,
                        label = stringResource(R.string.bluetooth),
                        isSelected = isSelected,
                        onClick = { 
                            vibrate(context)
                            onAudioRouteSelected(CallAudioState.ROUTE_BLUETOOTH) 
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
                
                // Earpiece / Wired Headset
                if (supportedRouteMask and CallAudioState.ROUTE_EARPIECE != 0 || supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
                    val isSelected = route == CallAudioState.ROUTE_EARPIECE || route == CallAudioState.ROUTE_WIRED_HEADSET
                    val targetRoute = if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) CallAudioState.ROUTE_WIRED_HEADSET else CallAudioState.ROUTE_EARPIECE
                    val label = if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) "Headset" else stringResource(R.string.earpiece)
                    
                    AudioRouteButton(
                        icon = Icons.Filled.PhoneInTalk,
                        label = label,
                        isSelected = isSelected,
                        onClick = { 
                            vibrate(context)
                            onAudioRouteSelected(targetRoute) 
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp)) // End padding
            }
        }

        // Bottom: Action buttons - larger and clearer
        if (isIncoming) {
            // Check if simplified UI should be used (for known contacts only)
            val showSimplified = useSimplifiedScreen && isKnownContact
            
            if (showSimplified) {
                // Simplified UI for contacts: Big Answer button + small Silence button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Big Answer button
                    CallActionButton(
                        icon = Icons.Filled.Call,
                        label = stringResource(R.string.answer),
                        backgroundColor = GreenCall,
                        onClick = {
                            vibrate(context)
                            onAnswer()
                        },
                        size = 140.dp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Smaller Silence button
                    Button(
                        onClick = {
                            vibrate(context)
                            onSilence()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighContrastBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth(0.5f)
                    ) {
                        Text(
                            text = stringResource(R.string.silence),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Standard UI: Reject and Answer buttons + Silence button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Silence button with icon
                    Button(
                        onClick = {
                            vibrate(context)
                            onSilence()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighContrastBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth(0.6f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.silence),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Active call: Keypad button and hangup button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Keypad button
                Button(
                    onClick = { 
                        vibrate(context)
                        showKeypad = true 
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Filled.Dialpad, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keypad", style = MaterialTheme.typography.titleMedium)
                }
                
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
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 110.dp
) {
    val iconSize = size * 0.5f // Icon is 50% of button size
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
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
    shouldHighlight: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
        val pulseScale by if (shouldHighlight) {
            infiniteTransition.animateFloat(
                initialValue = 1.15f, // Visible even if animations are OFF
                targetValue = 1.35f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(800),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ), label = "pulse"
            )
        } else {
            remember { mutableFloatStateOf(1f) }
        }

        val pulseAlpha by if (shouldHighlight) {
            infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0.2f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(800),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ), label = "alpha"
            )
        } else {
            remember { mutableFloatStateOf(0f) }
        }

        Box(contentAlignment = Alignment.Center) {
            // Glow effect behind
            if (shouldHighlight) {
                 Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha // Animate alpha
                        }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                 )
            }
            
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
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * DTMF Keypad overlay for in-call tone sending
 */
@Composable
fun DtmfKeypadOverlay(
    onDismiss: () -> Unit,
    onKeyClick: (Char) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val keys = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('*', '0', '#')
            )
            
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        DtmfKeyButton(key = key, onClick = { onKeyClick(key) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Hide Keypad", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

}

@Composable
fun DtmfKeyButton(key: Char, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = key.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

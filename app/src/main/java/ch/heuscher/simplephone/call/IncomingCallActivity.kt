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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
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
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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
    
    // Audio Controls - visible during dialing, ringing, active/holding
    val showAudioControls = audioState != null && (
        callState == Call.STATE_ACTIVE || 
        callState == Call.STATE_DIALING || 
        callState == Call.STATE_RINGING || 
        callState == Call.STATE_CONNECTING ||
        callState == Call.STATE_HOLDING
    )
    
    if (isLandscape) {
        // LANDSCAPE LAYOUT: Side panel design
        CallScreenLandscape(
            context = context,
            statusText = statusText,
            displayName = displayName,
            contact = contact,
            callerNumber = callerNumber,
            isKnownContact = isKnownContact,
            isIncoming = isIncoming,
            callState = callState,
            audioState = audioState,
            showAudioControls = showAudioControls,
            shouldHighlightSpeaker = shouldHighlightSpeaker,
            useSimplifiedScreen = useSimplifiedScreen,
            onAnswer = onAnswer,
            onReject = onReject,
            onSilence = onSilence,
            onHangup = onHangup,
            onAudioRouteSelected = onAudioRouteSelected,
            onBluetoothDeviceSelected = onBluetoothDeviceSelected,
            onShowKeypad = { showKeypad = true }
        )
    } else {
        // PORTRAIT LAYOUT: Compact inline design
        CallScreenPortrait(
            context = context,
            statusText = statusText,
            displayName = displayName,
            contact = contact,
            callerNumber = callerNumber,
            isKnownContact = isKnownContact,
            isIncoming = isIncoming,
            callState = callState,
            audioState = audioState,
            showAudioControls = showAudioControls,
            shouldHighlightSpeaker = shouldHighlightSpeaker,
            useSimplifiedScreen = useSimplifiedScreen,
            onAnswer = onAnswer,
            onReject = onReject,
            onSilence = onSilence,
            onHangup = onHangup,
            onAudioRouteSelected = onAudioRouteSelected,
            onBluetoothDeviceSelected = onBluetoothDeviceSelected,
            onShowKeypad = { showKeypad = true }
        )
    }
}

/**
 * Portrait layout: Compact inline row for audio controls + keypad
 */
@Composable
private fun CallScreenPortrait(
    context: android.content.Context,
    statusText: String,
    displayName: String,
    contact: Contact?,
    callerNumber: String?,
    isKnownContact: Boolean,
    isIncoming: Boolean,
    callState: Int,
    audioState: CallAudioState?,
    showAudioControls: Boolean,
    shouldHighlightSpeaker: Boolean,
    useSimplifiedScreen: Boolean,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onSilence: () -> Unit,
    onHangup: () -> Unit,
    onAudioRouteSelected: (Int) -> Unit,
    onBluetoothDeviceSelected: (android.bluetooth.BluetoothDevice) -> Unit,
    onShowKeypad: () -> Unit
) {
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
                    size = 160.dp, // Slightly smaller in portrait to save space
                    showFavoriteStar = contact.isFavorite
                )
            } else {
                // Default avatar for unknown contacts
                Box(
                    modifier = Modifier
                        .size(160.dp)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Caller name - very large, 2 lines max
            Text(
                text = displayName,
                style = MaterialTheme.typography.displaySmall, // Slightly smaller for portrait
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
        
        // Compact Audio + Keypad controls row (portrait mode)
        if (showAudioControls) {
            AudioControlsCompactRow(
                context = context,
                audioState = audioState!!,
                shouldHighlightSpeaker = shouldHighlightSpeaker,
                showKeypadButton = callState == Call.STATE_ACTIVE,
                onAudioRouteSelected = onAudioRouteSelected,
                onBluetoothDeviceSelected = onBluetoothDeviceSelected,
                onShowKeypad = onShowKeypad
            )
        }

        // Bottom: Action buttons - larger and clearer
        if (isIncoming) {
            IncomingCallButtons(
                context = context,
                useSimplifiedScreen = useSimplifiedScreen && isKnownContact,
                onAnswer = onAnswer,
                onReject = onReject,
                onSilence = onSilence
            )
        } else {
            // Active call: hangup button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
 * Landscape layout: Side panel design with audio controls on the right
 */
@Composable
private fun CallScreenLandscape(
    context: android.content.Context,
    statusText: String,
    displayName: String,
    contact: Contact?,
    callerNumber: String?,
    isKnownContact: Boolean,
    isIncoming: Boolean,
    callState: Int,
    audioState: CallAudioState?,
    showAudioControls: Boolean,
    shouldHighlightSpeaker: Boolean,
    useSimplifiedScreen: Boolean,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onSilence: () -> Unit,
    onHangup: () -> Unit,
    onAudioRouteSelected: (Int) -> Unit,
    onBluetoothDeviceSelected: (android.bluetooth.BluetoothDevice) -> Unit,
    onShowKeypad: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding() // Fix for landscape audio panel overlap
            .padding(16.dp)
    ) {
        // Main content: Left side (contact) + Right side (controls)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Left side: Contact info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Status text
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Contact avatar
                if (contact != null) {
                    ContactAvatar(
                        contact = contact,
                        size = 120.dp, // Smaller in landscape
                        showFavoriteStar = contact.isFavorite
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Caller name
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Phone number for unknown contacts
                if (!isKnownContact && callerNumber != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(callerNumber),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f)) // Push buttons to bottom of this column
                
                // Action buttons (Inside left column now)
                if (isIncoming) {
                    IncomingCallButtonsLandscape(
                        context = context,
                        useSimplifiedScreen = useSimplifiedScreen && isKnownContact,
                        onAnswer = onAnswer,
                        onReject = onReject,
                        onSilence = onSilence
                    )
                } else {
                    // Active call: hangup button (horizontal in landscape)
                    CallActionButtonHorizontal(
                        icon = Icons.Filled.CallEnd,
                        label = stringResource(R.string.end_call),
                        color = RedHangup,
                        onClick = {
                            vibrate(context)
                            onHangup()
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            }
            
            // Right side: Audio controls panel (vertical)
            if (showAudioControls) {
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AudioControlsVerticalPanel(
                        context = context,
                        audioState = audioState!!,
                        shouldHighlightSpeaker = shouldHighlightSpeaker,
                        showKeypadButton = callState == Call.STATE_ACTIVE,
                        onAudioRouteSelected = onAudioRouteSelected,
                        onBluetoothDeviceSelected = onBluetoothDeviceSelected,
                        onShowKeypad = onShowKeypad
                    )
                }
            }
        }
        
    }
}

/**
 * Landscape specific action button with horizontal layout (Icon + Text)
 */
@Composable
private fun CallActionButtonHorizontal(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier.height(80.dp),
        shape = CircleShape
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Compact inline row for audio controls + keypad button (portrait mode)
 */
@Composable
private fun AudioControlsCompactRow(
    context: android.content.Context,
    audioState: CallAudioState,
    shouldHighlightSpeaker: Boolean,
    showKeypadButton: Boolean,
    onAudioRouteSelected: (Int) -> Unit,
    onBluetoothDeviceSelected: (android.bluetooth.BluetoothDevice) -> Unit,
    onShowKeypad: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        
        val route = audioState.route
        val supportedRouteMask = audioState.supportedRouteMask
        
        // Speaker (48dp compact)
        if (supportedRouteMask and CallAudioState.ROUTE_SPEAKER != 0) {
            val isSelected = route == CallAudioState.ROUTE_SPEAKER
            AudioRouteButtonCompact(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                label = stringResource(R.string.speaker),
                isSelected = isSelected,
                shouldHighlight = shouldHighlightSpeaker && !isSelected,
                onClick = { 
                    vibrate(context)
                    onAudioRouteSelected(CallAudioState.ROUTE_SPEAKER) 
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Bluetooth Devices (compact)
        var showedBluetooth = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val devices = audioState.supportedBluetoothDevices
            if (!devices.isNullOrEmpty()) {
                showedBluetooth = true
                devices.forEach { device ->
                    val isSelected = audioState.activeBluetoothDevice?.address == device.address
                    @Suppress("MissingPermission")
                    val label = device.name ?: stringResource(R.string.bluetooth)
                    
                    AudioRouteButtonCompact(
                        icon = Icons.Filled.Bluetooth,
                        label = label,
                        isSelected = isSelected,
                        onClick = {
                            vibrate(context)
                            onBluetoothDeviceSelected(device)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
        
        // Fallback Bluetooth
        if (!showedBluetooth && (supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0)) {
            val isSelected = route == CallAudioState.ROUTE_BLUETOOTH
            AudioRouteButtonCompact(
                icon = Icons.Filled.Bluetooth,
                label = stringResource(R.string.bluetooth),
                isSelected = isSelected,
                onClick = { 
                    vibrate(context)
                    onAudioRouteSelected(CallAudioState.ROUTE_BLUETOOTH) 
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Earpiece / Wired Headset
        if (supportedRouteMask and CallAudioState.ROUTE_EARPIECE != 0 || supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
            val isSelected = route == CallAudioState.ROUTE_EARPIECE || route == CallAudioState.ROUTE_WIRED_HEADSET
            val targetRoute = if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) CallAudioState.ROUTE_WIRED_HEADSET else CallAudioState.ROUTE_EARPIECE
            val label = if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) stringResource(R.string.wired_headset) else stringResource(R.string.earpiece)
            
            AudioRouteButtonCompact(
                icon = Icons.Filled.PhoneInTalk,
                label = label,
                isSelected = isSelected,
                onClick = { 
                    vibrate(context)
                    onAudioRouteSelected(targetRoute) 
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Keypad button inline (only during active call)
        if (showKeypadButton) {
            AudioRouteButtonCompact(
                icon = Icons.Filled.Dialpad,
                label = stringResource(R.string.keypad),
                isSelected = false,
                onClick = {
                    vibrate(context)
                    onShowKeypad()
                }
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
    }
}

/**
 * Vertical panel for audio controls (landscape mode)
 */
@Composable
private fun AudioControlsVerticalPanel(
    context: android.content.Context,
    audioState: CallAudioState,
    shouldHighlightSpeaker: Boolean,
    showKeypadButton: Boolean,
    onAudioRouteSelected: (Int) -> Unit,
    onBluetoothDeviceSelected: (android.bluetooth.BluetoothDevice) -> Unit,
    onShowKeypad: () -> Unit
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
            shouldHighlight = shouldHighlightSpeaker && !isSelected,
            onClick = { 
                vibrate(context)
                onAudioRouteSelected(CallAudioState.ROUTE_SPEAKER) 
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    // Bluetooth Devices
    var showedBluetooth = false
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        val devices = audioState.supportedBluetoothDevices
        if (!devices.isNullOrEmpty()) {
            showedBluetooth = true
            // Show only first 2 bluetooth devices in landscape to save space
            devices.take(2).forEach { device ->
                val isSelected = audioState.activeBluetoothDevice?.address == device.address
                @Suppress("MissingPermission")
                val label = device.name ?: stringResource(R.string.bluetooth)
                
                AudioRouteButton(
                    icon = Icons.Filled.Bluetooth,
                    label = label,
                    isSelected = isSelected,
                    onClick = {
                        vibrate(context)
                        onBluetoothDeviceSelected(device)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    
    // Fallback Bluetooth
    if (!showedBluetooth && (supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH != 0)) {
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
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    // Earpiece / Wired Headset  
    if (supportedRouteMask and CallAudioState.ROUTE_EARPIECE != 0 || supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
        val isSelected = route == CallAudioState.ROUTE_EARPIECE || route == CallAudioState.ROUTE_WIRED_HEADSET
        val targetRoute = if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) CallAudioState.ROUTE_WIRED_HEADSET else CallAudioState.ROUTE_EARPIECE
        val label = if (supportedRouteMask and CallAudioState.ROUTE_WIRED_HEADSET != 0) stringResource(R.string.wired_headset) else stringResource(R.string.earpiece)
        
        AudioRouteButton(
            icon = Icons.Filled.PhoneInTalk,
            label = label,
            isSelected = isSelected,
            onClick = { 
                vibrate(context)
                onAudioRouteSelected(targetRoute) 
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    // Keypad button (only during active call)
    if (showKeypadButton) {
        AudioRouteButton(
            icon = Icons.Filled.Dialpad,
            label = stringResource(R.string.keypad),
            isSelected = false,
            onClick = {
                vibrate(context)
                onShowKeypad()
            }
        )
    }
}

/**
 * Compact 48dp audio route button for portrait mode
 */
@Composable
private fun AudioRouteButtonCompact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    shouldHighlight: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "compact_pulse")
    val pulseScale by if (shouldHighlight) {
        infiniteTransition.animateFloat(
            initialValue = 1.1f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ), label = "pulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val pulseAlpha by if (shouldHighlight) {
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ), label = "alpha"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Glow effect behind
            if (shouldHighlight) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
            
            FilledIconButton(
                onClick = onClick,
                modifier = Modifier.size(48.dp), // Compact 48dp size
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp) // Smaller icon for compact button
                )
            }
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall, // Smaller label for compact mode
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Incoming call buttons for portrait mode
 */
@Composable
private fun IncomingCallButtons(
    context: android.content.Context,
    useSimplifiedScreen: Boolean,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onSilence: () -> Unit
) {
    if (useSimplifiedScreen) {
        // Simplified UI for contacts: Big Answer button + small Silence button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
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
                CallActionButton(
                    icon = Icons.Filled.CallEnd,
                    label = stringResource(R.string.reject),
                    backgroundColor = RedHangup,
                    onClick = {
                        vibrate(context)
                        onReject()
                    }
                )
                
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
}

/**
 * Incoming call buttons for landscape mode
 */
@Composable
private fun IncomingCallButtonsLandscape(
    context: android.content.Context,
    useSimplifiedScreen: Boolean,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onSilence: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!useSimplifiedScreen) {
            CallActionButton(
                icon = Icons.Filled.CallEnd,
                label = stringResource(R.string.reject),
                backgroundColor = RedHangup,
                onClick = {
                    vibrate(context)
                    onReject()
                },
                size = 80.dp
            )
        }
        
        CallActionButton(
            icon = Icons.Filled.Call,
            label = stringResource(R.string.answer),
            backgroundColor = GreenCall,
            onClick = {
                vibrate(context)
                onAnswer()
            },
            size = if (useSimplifiedScreen) 100.dp else 80.dp
        )
        
        // Silence button as icon in landscape
        FilledIconButton(
            onClick = {
                vibrate(context)
                onSilence()
            },
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = HighContrastBlue,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = stringResource(R.string.silence),
                modifier = Modifier.size(28.dp)
            )
        }
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

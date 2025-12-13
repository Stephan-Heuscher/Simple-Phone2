package ch.heuscher.simplephone.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.heuscher.simplephone.model.AudioOutput
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.ContactAvatarLarge
import ch.heuscher.simplephone.ui.theme.AccessibleWhite
import ch.heuscher.simplephone.ui.theme.RedHangup
import ch.heuscher.simplephone.ui.theme.SpeakerActive
import ch.heuscher.simplephone.ui.theme.SpeakerInactive

import androidx.compose.ui.platform.LocalContext
import ch.heuscher.simplephone.ui.utils.vibrate

/**
 * In-call screen UI - ultra accessible with large buttons and high contrast
 * Shows only available audio outputs (speaker, bluetooth devices if connected)
 */
@Composable
fun InCallScreen(
    contact: Contact,
    currentAudioOutput: AudioOutput = AudioOutput.EARPIECE,
    availableAudioOutputs: List<AudioOutput> = listOf(
        AudioOutput.EARPIECE,
        AudioOutput.SPEAKER
    ),
    onHangup: () -> Unit,
    onAudioOutputChange: (AudioOutput) -> Unit,
    useHapticFeedback: Boolean = true
) {
    val context = LocalContext.current
    fun vibrate() {
        if (useHapticFeedback) {
            vibrate(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Contact info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            // Contact Avatar - Large
            ContactAvatarLarge(contact = contact)

            Spacer(modifier = Modifier.height(24.dp))

            // Contact Name - Very large and bold
            Text(
                text = contact.name,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Middle section: Audio output options - always show since we have Phone + Speaker at minimum
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Audio Output",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Audio output buttons in a row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(availableAudioOutputs) { audioOutput ->
                    AudioOutputButton(
                        audioOutput = audioOutput,
                        isSelected = audioOutput == currentAudioOutput,
                        onClick = { vibrate(); onAudioOutputChange(audioOutput) }
                    )
                }
            }
        }

        // Bottom section: Hangup button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 48.dp)
        ) {
            HangupButton(onClick = { vibrate(); onHangup() })
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "End Call",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RedHangup,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AudioOutputButton(
    audioOutput: AudioOutput,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val icon = when (audioOutput) {
        AudioOutput.EARPIECE -> Icons.Filled.Phone
        AudioOutput.SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
        AudioOutput.BLUETOOTH -> Icons.Filled.BluetoothAudio
        AudioOutput.WIRED_HEADSET -> Icons.Filled.Headset
        AudioOutput.HEARING_AID -> Icons.Filled.Hearing
    }

    val label = when (audioOutput) {
        AudioOutput.EARPIECE -> "Phone"
        AudioOutput.SPEAKER -> "Speaker"
        AudioOutput.BLUETOOTH -> "Bluetooth"
        AudioOutput.WIRED_HEADSET -> "Headset"
        AudioOutput.HEARING_AID -> "Hearing Aid"
    }

    val backgroundColor = when {
        isPressed -> SpeakerActive.copy(alpha = 0.5f)
        isSelected -> SpeakerActive
        else -> SpeakerInactive
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .semantics {
                    contentDescription = "$label audio output"
                    role = Role.Button
                }
                .pointerInteropFilter { event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isPressed = true
                            onClick() // Trigger on press!
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isPressed = false
                            true
                        }
                        else -> false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccessibleWhite,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) SpeakerActive else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HangupButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(if (isPressed) RedHangup.copy(alpha = 0.7f) else RedHangup)
            .semantics {
                contentDescription = "End call"
                role = Role.Button
            }
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onClick() // Trigger on press!
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CallEnd,
            contentDescription = null,
            tint = AccessibleWhite,
            modifier = Modifier.size(56.dp)
        )
    }
}

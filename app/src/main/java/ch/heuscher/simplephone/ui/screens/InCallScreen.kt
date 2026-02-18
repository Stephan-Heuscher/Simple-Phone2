package ch.heuscher.simplephone.ui.screens

import androidx.compose.ui.res.stringResource
import ch.heuscher.simplephone.R


import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Dialpad
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
import ch.heuscher.simplephone.ui.components.isTabletLayout
import ch.heuscher.simplephone.ui.theme.AccessibleWhite
import ch.heuscher.simplephone.ui.theme.RedHangup
import ch.heuscher.simplephone.ui.theme.SpeakerActive
import ch.heuscher.simplephone.ui.theme.SpeakerInactive

import androidx.compose.ui.platform.LocalContext
import ch.heuscher.simplephone.ui.utils.vibrate

/**
 * In-call screen UI - ultra accessible with large buttons and high contrast.
 * On tablets/foldables: split layout with contact info (left) and controls (right).
 * On phones: existing vertical stack layout.
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
    onDtmfClick: (Char) -> Unit = {},
    useHapticFeedback: Boolean = true
) {
    val context = LocalContext.current
    fun vibrate() {
        if (useHapticFeedback) {
            vibrate(context)
        }
    }

    var showKeypad by remember { mutableStateOf(false) }
    val isTablet = isTabletLayout()

    if (isTablet) {
        // ── Tablet / Foldable: Two-pane layout ──
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // LEFT PANE: Contact info (always visible, even when keypad is open)
            InCallContactPane(
                contact = contact,
                modifier = Modifier.weight(1f)
            )

            // Subtle divider between panes
            androidx.compose.material3.VerticalDivider()

            // RIGHT PANE: Controls or Keypad
            if (showKeypad) {
                InCallKeypadPane(
                    onDismiss = { showKeypad = false },
                    onKeyClick = { key -> vibrate(); onDtmfClick(key) },
                    onHangup = { vibrate(); onHangup() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                InCallControlPane(
                    currentAudioOutput = currentAudioOutput,
                    availableAudioOutputs = availableAudioOutputs,
                    onAudioOutputChange = { vibrate(); onAudioOutputChange(it) },
                    onShowKeypad = { vibrate(); showKeypad = true },
                    onHangup = { vibrate(); onHangup() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        // ── Phone: Original vertical layout ──
        if (showKeypad) {
            KeypadOverlay(
                onDismiss = { showKeypad = false },
                onKeyClick = { key -> vibrate(); onDtmfClick(key) }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section: Contact info
                InCallContactPane(contact = contact, modifier = Modifier)

                // Middle section: Audio controls
                InCallAudioSection(
                    currentAudioOutput = currentAudioOutput,
                    availableAudioOutputs = availableAudioOutputs,
                    onAudioOutputChange = { vibrate(); onAudioOutputChange(it) },
                    onShowKeypad = { vibrate(); showKeypad = true }
                )

                // Bottom section: Hangup button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 48.dp)
                ) {
                    HangupButton(onClick = { vibrate(); onHangup() })
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.end_call),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RedHangup,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Left pane of the in-call screen: Large avatar + contact name.
 * Used in both phone mode (as the top section) and tablet mode (as the left pane).
 */
@Composable
fun InCallContactPane(contact: Contact, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(32.dp)
    ) {
        ContactAvatarLarge(contact = contact)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = contact.name,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Audio output buttons + keypad toggle.
 * Shared between phone and tablet layouts (tablet embeds this in the control pane).
 */
@Composable
fun InCallAudioSection(
    currentAudioOutput: AudioOutput,
    availableAudioOutputs: List<AudioOutput>,
    onAudioOutputChange: (AudioOutput) -> Unit,
    onShowKeypad: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.audio_output_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            items(availableAudioOutputs) { audioOutput ->
                AudioOutputButton(
                    audioOutput = audioOutput,
                    isSelected = audioOutput == currentAudioOutput,
                    onClick = { onAudioOutputChange(audioOutput) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(
            onClick = onShowKeypad,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.Dialpad, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.keypad))
        }
    }
}

/**
 * Right pane for tablet mode: Audio controls + Hangup button all in one panel.
 */
@Composable
fun InCallControlPane(
    currentAudioOutput: AudioOutput,
    availableAudioOutputs: List<AudioOutput>,
    onAudioOutputChange: (AudioOutput) -> Unit,
    onShowKeypad: () -> Unit,
    onHangup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier.padding(24.dp)
    ) {
        InCallAudioSection(
            currentAudioOutput = currentAudioOutput,
            availableAudioOutputs = availableAudioOutputs,
            onAudioOutputChange = onAudioOutputChange,
            onShowKeypad = onShowKeypad
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HangupButton(onClick = onHangup)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.end_call),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RedHangup,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tablet-mode keypad pane: DTMF keypad with dismiss and hangup at the bottom. 
 * Replaces the right pane while keeping the contact visible on the left.
 */
@Composable
fun InCallKeypadPane(
    onDismiss: () -> Unit,
    onKeyClick: (Char) -> Unit,
    onHangup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
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
                    KeypadButton(key = key, onClick = { onKeyClick(key) })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Row: Back button + Hangup button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Button(onClick = onDismiss) {
                Text(stringResource(R.string.hide_keypad))
            }
            HangupButton(onClick = onHangup)
        }
    }
}

@Composable
fun KeypadOverlay(
    onDismiss: () -> Unit,
    onKeyClick: (Char) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .clickable(enabled = false) {} // Prevent clicks passing through
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
                        KeypadButton(key = key, onClick = { onKeyClick(key) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            androidx.compose.material3.Button(onClick = onDismiss) {
                Text(stringResource(R.string.hide_keypad))
            }
        }
    }
}

@Composable
fun KeypadButton(key: Char, onClick: () -> Unit) {
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
            fontWeight = FontWeight.Bold
        )
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
        AudioOutput.EARPIECE -> stringResource(R.string.earpiece)
        AudioOutput.SPEAKER -> stringResource(R.string.speaker)
        AudioOutput.BLUETOOTH -> stringResource(R.string.bluetooth)
        AudioOutput.WIRED_HEADSET -> stringResource(R.string.wired_headset)
        AudioOutput.HEARING_AID -> stringResource(R.string.hearing_aid)
    }

    val backgroundColor = when {
        isPressed -> SpeakerActive.copy(alpha = 0.5f)
        isSelected -> SpeakerActive
        else -> SpeakerInactive
    }

    val cdAudioOutput = stringResource(R.string.cd_audio_output, label)

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
                    contentDescription = cdAudioOutput
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
    val cdEndCall = stringResource(R.string.cd_end_call)
    
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(if (isPressed) RedHangup.copy(alpha = 0.7f) else RedHangup)
            .semantics {
                contentDescription = cdEndCall
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

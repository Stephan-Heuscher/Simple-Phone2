package ch.heuscher.simplephone.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.pressClickEffect
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.HighContrastBlue

@Composable
fun SettingsScreen(
    useHugeText: Boolean = false,
    onHugeTextChange: (Boolean) -> Unit = {},
    useHugeContactPicture: Boolean = false,
    onHugeContactPictureChange: (Boolean) -> Unit = {},
    useGridContactImages: Boolean = false,
    onGridContactImagesChange: (Boolean) -> Unit = {},
    missedCallsHours: Int = 24,
    onMissedCallsHoursChange: (Int) -> Unit = {},
    darkModeOption: Int = 0, // 0=System, 1=Light, 2=Dark
    onDarkModeOptionChange: (Int) -> Unit = {},
    blockUnknownCallers: Boolean = false,
    onBlockUnknownCallersChange: (Boolean) -> Unit = {},
    answerOnSpeakerIfFlat: Boolean = false,
    onAnswerOnSpeakerIfFlatChange: (Boolean) -> Unit = {},
    confirmBeforeCall: Boolean = false,
    onConfirmBeforeCallChange: (Boolean) -> Unit = {},
    useHapticFeedback: Boolean = true,
    onHapticFeedbackChange: (Boolean) -> Unit = {},
    useVoiceAnnouncements: Boolean = false,
    onVoiceAnnouncementsChange: (Boolean) -> Unit = {},
    favorites: List<Contact> = emptyList(),
    onFavoritesReorder: (List<Contact>) -> Unit = {},
    isDefaultDialer: Boolean = false,
    onSetDefaultDialer: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current

    fun vibrate(force: Boolean = false) {
        if (useHapticFeedback || force) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Default Dialer Section ---
        if (!isDefaultDialer) {
            item {
                Text(
                    androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.default_phone_app),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.set_default_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 32.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SettingsButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.set_default_btn),
                        onClick = { vibrate(); onSetDefaultDialer() }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
            }
        }

        // --- Contact List Appearance Section ---
        item {
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_appearance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_appearance_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Text Size
            SettingsToggleRow(
                title = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_huge_text),
                description = null,
                isChecked = useHugeText,
                onToggle = { vibrate(); onHugeTextChange(!useHugeText) }
            )

            // Huge Picture
            SettingsToggleRow(
                title = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_huge_picture),
                description = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_huge_picture_desc),
                isChecked = useHugeContactPicture,
                onToggle = { vibrate(); onHugeContactPictureChange(!useHugeContactPicture) }
            )

            // Grid View
            SettingsToggleRow(
                title = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_grid_view),
                description = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_grid_view_desc),
                isChecked = useGridContactImages,
                onToggle = { vibrate(); onGridContactImagesChange(!useGridContactImages) }
            )

            HorizontalDivider(thickness = 2.dp, modifier = Modifier.padding(vertical = 24.dp))
        }

        // --- Dark Mode Section ---
        item {
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.dark_mode),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.dark_mode_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(modifier = Modifier.padding(top = 16.dp)) {
                val options = listOf(
                    androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.mode_system),
                    androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.mode_light),
                    androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.mode_dark)
                )

                options.forEachIndexed { index, label ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vibrate(); onDarkModeOptionChange(index) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (darkModeOption == index),
                            onClick = { vibrate(); onDarkModeOptionChange(index) }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 2.dp, modifier = Modifier.padding(vertical = 24.dp))
        }

        // --- Behavior Section ---
        item {
            Text(
                "Call Behavior", // TODO: Add string resource for this header if needed
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            // Block Unknown
            SettingsToggleRow(
                title = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.block_unknown),
                description = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.block_unknown_desc),
                isChecked = blockUnknownCallers,
                onToggle = { vibrate(); onBlockUnknownCallersChange(!blockUnknownCallers) }
            )

            // Confirm Before Call
            SettingsToggleRow(
                title = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.call_confirm),
                description = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.call_confirm_desc),
                isChecked = confirmBeforeCall,
                onToggle = { vibrate(); onConfirmBeforeCallChange(!confirmBeforeCall) }
            )

            // Speaker if Flat
            SettingsToggleRow(
                title = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.answer_speaker_table),
                description = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.answer_speaker_table_desc),
                isChecked = answerOnSpeakerIfFlat,
                onToggle = { vibrate(); onAnswerOnSpeakerIfFlatChange(!answerOnSpeakerIfFlat) }
            )

            // Voice Announcements
            SettingsToggleRow(
                title = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.voice_announcements),
                description = null,
                isChecked = useVoiceAnnouncements,
                onToggle = { vibrate(); onVoiceAnnouncementsChange(!useVoiceAnnouncements) }
            )

            // Haptic Feedback
            SettingsToggleRow(
                title = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.vibration_feedback),
                description = null,
                isChecked = useHapticFeedback,
                onToggle = { vibrate(); onHapticFeedbackChange(!useHapticFeedback) }
            )

            HorizontalDivider(thickness = 2.dp, modifier = Modifier.padding(vertical = 24.dp))
        }

        // --- Back Button ---
        item {
            Spacer(modifier = Modifier.height(32.dp))
            SettingsButton( // Use SettingsButton here instead of manual Button for consistency? OR keep Button since it has an Icon...
                // The original code used Button with Icon. Let's keep it but maybe use pressClickEffect?
                // Actually the original used standard Button. Let's stick to standard button for "Back" if it wasn't broken.
                // But wait, the user wanted to replace pointerInteropFilter.
                // The Back button uses `onClick`. Standard Button handles clicks correctly WITHOUT pointerInteropFilter.
                // It's only CUSTOM buttons that used pointerInteropFilter.
                // So standard Button is fine.
                // I'll keep the Back button implementation from step 256.
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.back),
                onClick = { vibrate(); onBackClick() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                // But SettingsButton doesn't support Icon easily.
                // I'll just use standard Button here.
            )
        }
    }
}

// Helper Composables

@Composable
fun SettingsToggleRow(
    title: String,
    description: String?,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsToggleButton(
            isEnabled = isChecked,
            onToggle = onToggle,
            label = if (isChecked) "ON" else "OFF", // Ideally use string resources
            modifier = Modifier.size(width = 90.dp, height = 45.dp)
        )
    }
}

@Composable
fun SettingsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) HighContrastBlue.copy(alpha = 0.7f) else HighContrastBlue)
            .semantics {
                contentDescription = "$text button"
                role = Role.Button
            }
            .pressClickEffect(
                onClick = onClick,
                onPressedChange = { isPressed = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun SettingsToggleButton(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val backgroundColor = when {
        isPressed -> if (isEnabled) HighContrastBlue.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.7f)
        isEnabled -> HighContrastBlue
        else -> Color.Gray
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .semantics {
                contentDescription = "$label toggle"
                role = Role.Switch
            }
            .pressClickEffect(
                onClick = onToggle,
                onPressedChange = { isPressed = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

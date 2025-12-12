package com.example.simplephone.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import com.example.simplephone.model.Contact
import com.example.simplephone.ui.components.ContactAvatar
import com.example.simplephone.ui.theme.HighContrastBlue
import com.example.simplephone.ui.theme.GreenCall

@Composable
fun SettingsScreen(
    useHugeText: Boolean = false,
    onHugeTextChange: (Boolean) -> Unit = {},
    missedCallsHours: Int = 24,
    onMissedCallsHoursChange: (Int) -> Unit = {},
    useDarkMode: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
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
    // Remember favorites order for reordering
    val mutableFavorites = remember(favorites) { mutableStateListOf(*favorites.toTypedArray()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Default Dialer Section ---
        if (!isDefaultDialer) {
            item {
                Text(
                    "Default Phone App",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Set this app as your default phone app:",
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
                        text = "SET DEFAULT",
                        onClick = onSetDefaultDialer
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
            }
        }

        // --- Text Size Section ---
        item {
            Text(
                "Text Size",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Make contact names very large (same size as picture):",
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
                HugeTextToggleButton(
                    isEnabled = useHugeText,
                    onToggle = { onHugeTextChange(!useHugeText) }
                )
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Dark Mode Section ---
        item {
            Text(
                "Dark Mode",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Use high-contrast dark theme:",
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
                SettingsToggleButton(
                    isEnabled = useDarkMode,
                    onToggle = { onDarkModeChange(!useDarkMode) },
                    label = if (useDarkMode) "DARK" else "LIGHT"
                )
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Call Confirmation Section ---
        item {
            Text(
                "Call Confirmation",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Ask before making a call (prevents accidental calls):",
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
                SettingsToggleButton(
                    isEnabled = confirmBeforeCall,
                    onToggle = { onConfirmBeforeCallChange(!confirmBeforeCall) },
                    label = if (confirmBeforeCall) "ON" else "OFF"
                )
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Haptic Feedback Section ---
        item {
            Text(
                "Vibration Feedback",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Vibrate when buttons are pressed:",
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
                SettingsToggleButton(
                    isEnabled = useHapticFeedback,
                    onToggle = { onHapticFeedbackChange(!useHapticFeedback) },
                    label = if (useHapticFeedback) "ON" else "OFF"
                )
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Voice Announcements Section ---
        item {
            Text(
                "Voice Announcements",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Speak contact names when calling:",
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
                SettingsToggleButton(
                    isEnabled = useVoiceAnnouncements,
                    onToggle = { onVoiceAnnouncementsChange(!useVoiceAnnouncements) },
                    label = if (useVoiceAnnouncements) "ON" else "OFF"
                )
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Missed Calls Section ---
        item {
            Text(
                "Missed Calls Display",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Show missed calls from the last:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth()
            ) {
                BigIconButton(
                    icon = Icons.Filled.Remove,
                    contentDescription = "Decrease hours",
                    onClick = { if (missedCallsHours > 1) onMissedCallsHoursChange(missedCallsHours - 1) },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    "$missedCallsHours hours",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                BigIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Increase hours",
                    onClick = { onMissedCallsHoursChange(missedCallsHours + 1) },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Favorites Order Section ---
        item {
            Text(
                "Favorites Order",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Use the arrows to reorder your favorites:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }

        itemsIndexed(mutableFavorites) { index, contact ->
            FavoriteReorderRow(
                contact = contact,
                canMoveUp = index > 0,
                canMoveDown = index < mutableFavorites.size - 1,
                useHugeText = useHugeText,
                onMoveUp = {
                    if (index > 0) {
                        val item = mutableFavorites.removeAt(index)
                        mutableFavorites.add(index - 1, item)
                        onFavoritesReorder(mutableFavorites.toList())
                    }
                },
                onMoveDown = {
                    if (index < mutableFavorites.size - 1) {
                        val item = mutableFavorites.removeAt(index)
                        mutableFavorites.add(index + 1, item)
                        onFavoritesReorder(mutableFavorites.toList())
                    }
                }
            )
            if (index < mutableFavorites.size - 1) {
                HorizontalDivider()
            }
        }

        item {
            HorizontalDivider(thickness = 2.dp, modifier = Modifier.padding(vertical = 24.dp))
        }

    }
}

/**
 * Row for reordering favorites with big up/down arrows
 */
@Composable
fun FavoriteReorderRow(
    contact: Contact,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    useHugeText: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Avatar + Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ContactAvatar(
                contact = contact,
                size = 56.dp,
                showFavoriteStar = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            val textStyle = if (useHugeText) {
                MaterialTheme.typography.displayMedium
            } else {
                MaterialTheme.typography.headlineMedium
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                val scrollState = androidx.compose.foundation.rememberScrollState()
                Text(
                    text = contact.name,
                    style = textStyle,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                )
            }
        }

        // Right side: Big Up/Down arrows with 16dp spacing for better accessibility
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BigArrowButton(
                icon = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Move ${contact.name} up",
                onClick = onMoveUp,
                enabled = canMoveUp
            )

            BigArrowButton(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Move ${contact.name} down",
                onClick = onMoveDown,
                enabled = canMoveDown
            )
        }
    }
}

/**
 * Large arrow button for reordering - triggers on press
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BigArrowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = when {
        !enabled -> Color.Gray.copy(alpha = 0.3f)
        isPressed -> HighContrastBlue.copy(alpha = 0.7f)
        else -> HighContrastBlue
    }

    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .then(
                if (enabled) {
                    Modifier.pointerInteropFilter { event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                isPressed = true
                                onClick()
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                isPressed = false
                                true
                            }
                            else -> false
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.Gray,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Large icon button for filter controls - triggers on press
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BigIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(if (isPressed) HighContrastBlue.copy(alpha = 0.7f) else HighContrastBlue)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onClick()
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
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Large toggle button for huge text setting - triggers on press
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HugeTextToggleButton(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isPressed -> if (isEnabled) GreenCall.copy(alpha = 0.7f) else HighContrastBlue.copy(alpha = 0.7f)
        isEnabled -> GreenCall
        else -> HighContrastBlue
    }

    Box(
        modifier = modifier
            .size(width = 160.dp, height = 80.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .semantics {
                contentDescription = if (isEnabled) "Huge text is ON, tap to turn off" else "Huge text is OFF, tap to turn on"
                role = Role.Switch
            }
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onToggle()
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
        Text(
            text = if (isEnabled) "ON" else "OFF",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Generic toggle button for settings - triggers on press
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsToggleButton(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isPressed -> if (isEnabled) GreenCall.copy(alpha = 0.7f) else HighContrastBlue.copy(alpha = 0.7f)
        isEnabled -> GreenCall
        else -> HighContrastBlue
    }

    Box(
        modifier = modifier
            .size(width = 160.dp, height = 80.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .semantics {
                contentDescription = "$label is ${if (isEnabled) "ON" else "OFF"}, tap to toggle"
                role = Role.Switch
            }
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onToggle()
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
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Generic button for settings actions - triggers on press
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = if (isPressed) HighContrastBlue.copy(alpha = 0.7f) else HighContrastBlue

    Box(
        modifier = modifier
            .size(width = 240.dp, height = 80.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .semantics {
                contentDescription = "$text button"
                role = Role.Button
            }
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onClick()
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
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

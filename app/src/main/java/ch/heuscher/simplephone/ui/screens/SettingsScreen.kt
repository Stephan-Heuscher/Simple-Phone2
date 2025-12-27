package ch.heuscher.simplephone.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.ContactAvatar
import ch.heuscher.simplephone.ui.components.pressClickEffect
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.HighContrastBlue

@Composable
fun SettingsScreen(
    displayMode: Int = 0, // 0=Standard, 1=LargeText, 2=BigPhotos, 3=Grid
    onDisplayModeChange: (Int) -> Unit = {},
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
    // Remember favorites order for reordering
    val mutableFavorites = remember { mutableStateListOf<Contact>() }
    LaunchedEffect(favorites) {
        mutableFavorites.clear()
        mutableFavorites.addAll(favorites)
    }

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

    // Track item height for drag-swap calculation (approximate)
    var itemHeightPx by remember { mutableFloatStateOf(0f) }

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

        // --- Display Mode Section (unified layout selector) ---
        item {
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_appearance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.settings_appearance_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            // 2x2 Grid of Display Mode Options (for accessibility - bigger touch targets)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Standard + Large Text
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.display_mode_standard),
                        isSelected = displayMode == 0,
                        onClick = { vibrate(); onDisplayModeChange(0) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.display_mode_large_text),
                        isSelected = displayMode == 1,
                        onClick = { vibrate(); onDisplayModeChange(1) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Row 2: Big Photos + Grid
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.display_mode_big_photos),
                        isSelected = displayMode == 2,
                        onClick = { vibrate(); onDisplayModeChange(2) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.display_mode_grid),
                        isSelected = displayMode == 3,
                        onClick = { vibrate(); onDisplayModeChange(3) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
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

            Column(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // System Default
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.mode_system),
                        isSelected = darkModeOption == 0,
                        onClick = { vibrate(); onDarkModeOptionChange(0) },
                        modifier = Modifier.weight(1f)
                    )

                    // Light
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.mode_light),
                        isSelected = darkModeOption == 1,
                        onClick = { vibrate(); onDarkModeOptionChange(1) },
                        modifier = Modifier.weight(1f)
                    )

                    // Dark
                    SettingsOptionButton(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.mode_dark),
                        isSelected = darkModeOption == 2,
                        onClick = { vibrate(); onDarkModeOptionChange(2) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Missed Calls Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.missed_calls_display),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.missed_calls_desc),
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
                    onClick = { vibrate(); if (missedCallsHours > 1) onMissedCallsHoursChange(missedCallsHours - 1) },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.hours_format, missedCallsHours),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                BigIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Increase hours",
                    onClick = { vibrate(); onMissedCallsHoursChange(missedCallsHours + 1) },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Favorites Order Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.favorites_order),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.favorites_order_desc),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }

        itemsIndexed(mutableFavorites, key = { _, contact -> contact.id }) { index, contact ->
            var isDragging by remember { mutableStateOf(false) }
            var offsetY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = offsetY
                        shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                    }
                    .onGloballyPositioned { coordinates ->
                        if (itemHeightPx == 0f) itemHeightPx = coordinates.size.height.toFloat()
                    }
            ) {
                FavoriteReorderRow(
                    contact = contact,
                    canMoveUp = index > 0,
                    canMoveDown = index < mutableFavorites.size - 1,
                    useHugeText = displayMode == 1, // Large Text mode
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
                    },
                    itemHeightPx = itemHeightPx,
                    isDraggingUpdate = { isDragging = it },
                    offsetYUpdate = { offsetY = it },
                    vibrateFunc = { force -> vibrate(force) }
                )
            }
            if (index < mutableFavorites.size - 1) {
                HorizontalDivider()
            }
        }

        // --- Advanced Options Header ---
        item {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.advanced_settings),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Block Unknown Numbers Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.block_unknown),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.block_unknown_desc),
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
                BigToggleButton(
                    isEnabled = blockUnknownCallers,
                    onToggle = { vibrate(); onBlockUnknownCallersChange(!blockUnknownCallers) },
                    label = if (blockUnknownCallers) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Answer on Speaker if Flat ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.answer_speaker_table),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.answer_speaker_table_desc),
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
                BigToggleButton(
                    isEnabled = answerOnSpeakerIfFlat,
                    onToggle = { vibrate(); onAnswerOnSpeakerIfFlatChange(!answerOnSpeakerIfFlat) },
                    label = if (answerOnSpeakerIfFlat) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Call Confirmation Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.call_confirm),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.call_confirm_desc),
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
                BigToggleButton(
                    isEnabled = confirmBeforeCall,
                    onToggle = { vibrate(); onConfirmBeforeCallChange(!confirmBeforeCall) },
                    label = if (confirmBeforeCall) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Voice Announcements Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.voice_announcements),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.voice_desc),
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
                BigToggleButton(
                    isEnabled = useVoiceAnnouncements,
                    onToggle = { vibrate(); onVoiceAnnouncementsChange(!useVoiceAnnouncements) },
                    label = if (useVoiceAnnouncements) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Haptic Feedback Section ---
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.vibration_feedback),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.vibration_desc),
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
                BigToggleButton(
                    isEnabled = useHapticFeedback,
                    onToggle = {
                        vibrate(force = true)
                        onHapticFeedbackChange(!useHapticFeedback)
                    },
                    label = if (useHapticFeedback) androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.on) else androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.off)
                )
            }
        }
        
        // --- Back Button ---
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { vibrate(); onBackClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.back),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
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
    onMoveDown: () -> Unit,
    itemHeightPx: Float,
    isDraggingUpdate: (Boolean) -> Unit,
    offsetYUpdate: (Float) -> Unit,
    vibrateFunc: (Boolean) -> Unit
) {
    var offsetY by remember(contact.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(offsetY) { offsetYUpdate(offsetY) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.background), // Opaque background for dragging
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            ContactAvatar(
                contact = contact,
                size = 56.dp,
                showFavoriteStar = true,
                modifier = Modifier.pointerInput(contact.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isDraggingUpdate(true)
                            vibrateFunc(true)
                        },
                        onDragEnd = {
                            isDraggingUpdate(false)
                            offsetY = 0f
                        },
                        onDragCancel = {
                            isDraggingUpdate(false)
                            offsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetY += dragAmount.y

                            val threshold = itemHeightPx * 0.7f
                            if (itemHeightPx > 0) {
                                if (offsetY > threshold && canMoveDown) {
                                    vibrateFunc(false)
                                    onMoveDown()
                                    offsetY -= itemHeightPx
                                } else if (offsetY < -threshold && canMoveUp) {
                                    vibrateFunc(false)
                                    onMoveUp()
                                    offsetY += itemHeightPx
                                }
                            }
                        }
                    )
                }
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
                val scrollState = rememberScrollState()
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
                 if(enabled) {
                    Modifier.pressClickEffect(
                        onClick = onClick,
                        onPressedChange = { isPressed = it }
                    )
                 } else Modifier
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
            .pressClickEffect(
                onClick = onClick,
                onPressedChange = { isPressed = it }
            ),
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
 * Option button for settings (like Dark Mode)
 */
@Composable
fun SettingsOptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isPressed -> if (isSelected) GreenCall.copy(alpha = 0.7f) else Color.Gray
        isSelected -> GreenCall
        else -> Color.LightGray
    }

    val textColor = if (isSelected) Color.White else Color.Black

    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .semantics {
                role = Role.RadioButton
                this.selected = isSelected
            }
            .pressClickEffect(
                onClick = onClick,
                onPressedChange = { isPressed = it }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Large toggle button for huge text/picture settings - triggers on press
 */
@Composable
fun BigToggleButton(
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
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Standard large button for settings actions
 */
@Composable
fun SettingsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(60.dp)
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

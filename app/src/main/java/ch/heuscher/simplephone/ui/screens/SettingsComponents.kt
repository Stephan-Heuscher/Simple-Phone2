package ch.heuscher.simplephone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.heuscher.simplephone.R
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.ContactAvatar
import ch.heuscher.simplephone.ui.components.pressClickEffect
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.HighContrastBlue

/**
 * Reusable composable components for the Settings screen.
 * Extracted from SettingsScreen.kt to improve maintainability and reuse.
 *
 * User benefit: Smaller, focused files make it easier to find and modify
 * individual settings components without scrolling through a 1400-line file.
 */

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
    isDragging: Boolean,
    onDraggingChange: (Boolean) -> Unit,
    offsetY: Float,
    onOffsetYChange: (Float) -> Unit,
    vibrateFunc: (Boolean) -> Unit
) {
    val currentOffsetY by rememberUpdatedState(offsetY)
    val currentOnOffsetYChange by rememberUpdatedState(onOffsetYChange)
    val currentOnDraggingChange by rememberUpdatedState(onDraggingChange)
    val currentVibrateFunc by rememberUpdatedState(vibrateFunc)
    val currentOnMoveUp by rememberUpdatedState(onMoveUp)
    val currentOnMoveDown by rememberUpdatedState(onMoveDown)
    val currentCanMoveUp by rememberUpdatedState(canMoveUp)
    val currentCanMoveDown by rememberUpdatedState(canMoveDown)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.background),
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
                        onDragStart = { offset ->
                            currentOnDraggingChange(true)
                            currentVibrateFunc(true)
                        },
                        onDragEnd = {
                            currentOnDraggingChange(false)
                            currentOnOffsetYChange(0f)
                        },
                        onDragCancel = {
                            currentOnDraggingChange(false)
                            currentOnOffsetYChange(0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffsetY = currentOffsetY + dragAmount.y
                            currentOnOffsetYChange(newOffsetY)

                            val threshold = itemHeightPx * 0.7f
                            if (itemHeightPx > 0) {
                                if (newOffsetY > threshold && currentCanMoveDown) {
                                    currentVibrateFunc(false)
                                    currentOnMoveDown()
                                    currentOnOffsetYChange(newOffsetY - itemHeightPx)
                                } else if (newOffsetY < -threshold && currentCanMoveUp) {
                                    currentVibrateFunc(false)
                                    currentOnMoveUp()
                                    currentOnOffsetYChange(newOffsetY + itemHeightPx)
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
                contentDescription = stringResource(R.string.cd_move_up, contact.name),
                onClick = onMoveUp,
                enabled = canMoveUp
            )

            BigArrowButton(
                icon = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_move_down, contact.name),
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
                contentDescription = label
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
                contentDescription = text
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

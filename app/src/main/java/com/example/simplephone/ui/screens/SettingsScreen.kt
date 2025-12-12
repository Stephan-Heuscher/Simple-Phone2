package com.example.simplephone.ui.screens

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
import com.example.simplephone.data.MockData
import com.example.simplephone.model.Contact
import com.example.simplephone.ui.components.ContactAvatar
import com.example.simplephone.ui.theme.HighContrastBlue

@Composable
fun SettingsScreen(
    filterHours: Int,
    onFilterChange: (Int) -> Unit,
    onBackClick: () -> Unit = {}
) {
    // Remember favorites order for reordering
    val favorites = remember { mutableStateListOf(*MockData.getFavoritesOrdered().toTypedArray()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Recents Filter Section ---
        item {
            Text(
                "Recents Filter",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Hide calls older than:",
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
                    onClick = { if (filterHours > 1) onFilterChange(filterHours - 1) },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    "$filterHours hours",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                BigIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "Increase hours",
                    onClick = { onFilterChange(filterHours + 1) },
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
                "Drag to reorder your favorites:",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }

        itemsIndexed(favorites) { index, contact ->
            FavoriteReorderRow(
                contact = contact,
                canMoveUp = index > 0,
                canMoveDown = index < favorites.size - 1,
                onMoveUp = {
                    if (index > 0) {
                        val item = favorites.removeAt(index)
                        favorites.add(index - 1, item)
                        MockData.moveFavoriteUp(contact.id)
                    }
                },
                onMoveDown = {
                    if (index < favorites.size - 1) {
                        val item = favorites.removeAt(index)
                        favorites.add(index + 1, item)
                        MockData.moveFavoriteDown(contact.id)
                    }
                }
            )
            if (index < favorites.size - 1) {
                HorizontalDivider()
            }
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

            Text(
                text = contact.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
        }

        // Right side: Big Up/Down arrows
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

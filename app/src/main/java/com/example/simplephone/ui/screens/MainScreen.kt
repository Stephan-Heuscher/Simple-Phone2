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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.HorizontalDivider
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
import com.example.simplephone.ui.theme.GreenCall

@Composable
fun MainScreen(
    onCallClick: (String) -> Unit,
    onContactClick: (String) -> Unit
) {
    // Get only the last MISSED call (not any call)
    val lastMissedCall = remember { MockData.getLastMissedCall() }
    val favorites = remember { MockData.getFavoritesOrdered() }
    val allContacts = remember { MockData.contacts.sortedBy { it.name } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // --- Last Missed Call Section (displayed like a contact) ---
        if (lastMissedCall != null) {
            item {
                SectionHeader(title = "Missed Call")
            }

            item {
                val contact = MockData.getContactById(lastMissedCall.contactId)
                if (contact != null) {
                    // Display as a simple contact row (no phone number or time shown)
                    ContactRow(
                        contact = contact,
                        onCallClick = { onCallClick(contact.number) },
                        showFavoriteStar = false // Don't show star for missed call section
                    )
                    HorizontalDivider(thickness = 2.dp)
                }
            }
        }

        // --- Favorites Section ---
        item {
            SectionHeader(title = "Favorites")
        }

        if (favorites.isEmpty()) {
            item {
                Text(
                    "No favorites",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            items(favorites) { contact ->
                ContactRow(
                    contact = contact,
                    onCallClick = { onCallClick(contact.number) },
                    showFavoriteStar = true
                )
                HorizontalDivider()
            }
        }

        // --- Phone Book Section ---
        item {
            SectionHeader(title = "Phone Book")
        }

        items(allContacts) { contact ->
            ContactRow(
                contact = contact,
                onCallClick = { onCallClick(contact.number) },
                showFavoriteStar = true
            )
            HorizontalDivider()
        }
    }
}

/**
 * A single contact row with avatar, name, and green call button.
 * Accessible: reacts on press, large touch targets.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ContactRow(
    contact: Contact,
    onCallClick: () -> Unit,
    showFavoriteStar: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .semantics {
                contentDescription = "Contact ${contact.name}, tap green button to call"
                role = Role.Button
            }
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        // Full row press calls the contact
                        onCallClick()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        true
                    }
                    else -> false
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                size = 64.dp,
                showFavoriteStar = showFavoriteStar
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = contact.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Right side: Green call button
        GreenCallIcon(
            onClick = onCallClick,
            contentDescription = "Call ${contact.name}"
        )
    }
}

/**
 * Green circular call button - triggers on press for accessibility
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GreenCallIcon(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Int = 56
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(if (isPressed) GreenCall.copy(alpha = 0.7f) else GreenCall)
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
            imageVector = Icons.Filled.Call,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

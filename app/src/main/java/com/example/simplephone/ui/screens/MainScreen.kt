package com.example.simplephone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import com.example.simplephone.data.MockData
import com.example.simplephone.model.CallLogEntry
import com.example.simplephone.model.Contact
import com.example.simplephone.ui.components.ContactAvatar
import com.example.simplephone.ui.theme.GreenCall

@Composable
fun MainScreen(
    onCallClick: (String) -> Unit,
    onContactClick: (String) -> Unit,
    missedCalls: List<CallLogEntry> = emptyList(),
    missedCallsHours: Int = 24,
    useHugeText: Boolean = false,
    contacts: List<Contact> = MockData.contacts
) {
    val favorites = remember(contacts) { contacts.filter { it.isFavorite }.sortedBy { it.sortOrder } }
    val allContacts = remember(contacts) { contacts.sortedBy { it.name } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // --- Missed Calls Section (always shown) ---
        item {
            SectionHeader(title = "Missed Calls (last $missedCallsHours hrs)")
        }

        if (missedCalls.isEmpty()) {
            // Show green background with "No missed calls" message
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenCall.copy(alpha = 0.3f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No missed calls",
                        style = if (useHugeText) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = GreenCall
                    )
                }
                HorizontalDivider(thickness = 2.dp)
            }
        } else {
            items(missedCalls) { callEntry ->
                // Try to find contact by number
                val contact = allContacts.find { it.number == callEntry.contactId }
                    ?: Contact(
                        id = callEntry.id,
                        name = callEntry.contactId, // Show number as name
                        number = callEntry.contactId
                    )
                ContactRow(
                    contact = contact,
                    onCallClick = { onCallClick(contact.number) },
                    showFavoriteStar = false,
                    useHugeText = useHugeText
                )
                HorizontalDivider()
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
                    showFavoriteStar = true,
                    useHugeText = useHugeText
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
                showFavoriteStar = true,
                useHugeText = useHugeText
            )
            HorizontalDivider()
        }
    }
}

/**
 * A single contact row with avatar, name, and green call button.
 * Accessible: reacts on press, large touch targets.
 */
@Composable
fun ContactRow(
    contact: Contact,
    onCallClick: () -> Unit,
    showFavoriteStar: Boolean = true,
    modifier: Modifier = Modifier,
    useHugeText: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Contact ${contact.name}, tap picture or green button to call"
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Avatar (clickable) + Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Avatar is clickable
            ClickableAvatar(
                contact = contact,
                size = 64.dp,
                showFavoriteStar = showFavoriteStar,
                onClick = onCallClick
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Contact name with horizontal scrolling for long names
            // Text size is 25% bigger (headlineMedium vs headlineSmall), or huge if setting is on
            val textStyle = if (useHugeText) {
                MaterialTheme.typography.displayMedium // Same visual weight as avatar
            } else {
                MaterialTheme.typography.headlineMedium // 25% bigger than headlineSmall
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                val scrollState = androidx.compose.foundation.rememberScrollState()
                Text(
                    text = contact.name,
                    style = textStyle,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                )
            }
        }

        // Right side: Green call button (clickable)
        GreenCallIcon(
            onClick = onCallClick,
            contentDescription = "Call ${contact.name}"
        )
    }
}

/**
 * Clickable avatar that triggers on press
 */
@Composable
fun ClickableAvatar(
    contact: Contact,
    size: Dp,
    showFavoriteStar: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(
                role = Role.Button,
                onClickLabel = "Tap to call ${contact.name}"
            ) {
                onClick()
            }
    ) {
        ContactAvatar(
            contact = contact,
            size = size,
            showFavoriteStar = showFavoriteStar
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
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(GreenCall)
            .pointerInteropFilter { event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    onClick()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Call,
            contentDescription = contentDescription,
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

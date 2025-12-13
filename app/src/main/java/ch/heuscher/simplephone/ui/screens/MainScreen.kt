package ch.heuscher.simplephone.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import ch.heuscher.simplephone.data.MockData
import ch.heuscher.simplephone.model.CallLogEntry
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.ContactAvatar
import ch.heuscher.simplephone.ui.components.VerticalScrollbar
import ch.heuscher.simplephone.ui.components.HorizontalScrollbar
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.utils.vibrate

@Composable
fun MainScreen(
    onCallClick: (String) -> Unit,
    onContactClick: (String) -> Unit,
    onDialerClick: () -> Unit = {},
    missedCalls: List<CallLogEntry> = emptyList(),
    missedCallsHours: Int = 24,
    useHugeText: Boolean = false,
    contacts: List<Contact> = MockData.contacts,
    isDefaultDialer: Boolean = true,
    onSetDefaultDialer: () -> Unit = {},
    useHapticFeedback: Boolean = false
) {
    val favorites = remember(contacts) { contacts.filter { it.isFavorite }.sortedBy { it.sortOrder } }
    val allContacts = remember(contacts) { contacts.sortedBy { it.name } }
    
    // Search state
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    // Filtered contacts based on search
    val filteredContacts = remember(allContacts, missedCalls, searchQuery) {
        if (searchQuery.isBlank()) {
            allContacts
        } else {
            // Get contacts matching search
            val matchingContacts = allContacts.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                contact.number.contains(searchQuery)
            }
            
            // Get missed calls matching search (that are not already in contacts)
            val matchingMissedCalls = missedCalls.filter { call ->
                // Check if number matches search
                val numberMatches = call.contactId.contains(searchQuery)
                
                // Check if this number is already in our contacts list (to avoid duplicates)
                val isKnownContact = allContacts.any { 
                    it.number.replace(Regex("[^0-9]"), "") == call.contactId.replace(Regex("[^0-9]"), "") 
                }
                
                numberMatches && !isKnownContact
            }.map { call ->
                Contact(
                    id = call.id,
                    name = call.contactId, // Show number as name for unknown contacts
                    number = call.contactId,
                    isFavorite = false
                )
            }.distinctBy { it.number } // Avoid duplicate unknown numbers
            
            matchingContacts + matchingMissedCalls
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Search Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dialer Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(
                        onClick = { 
                            if (useHapticFeedback) vibrate(context)
                            onDialerClick() 
                        },
                        role = Role.Button
                    )
                    .semantics { contentDescription = "Open Dialer" },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top row (1, 2, 3) - Bigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f)
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("1", style = textStyle)
                        Text("2", style = textStyle)
                        Text("3", style = textStyle)
                    }
                    
                    // Bottom row (4, 5, 6) - Cut off
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        val textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("4", style = textStyle)
                        Text("5", style = textStyle)
                        Text("6", style = textStyle)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                placeholder = { 
                    Text(
                        "Search...",
                        style = MaterialTheme.typography.bodyLarge
                    ) 
                },
                leadingIcon = null,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            if (useHapticFeedback) vibrate(context)
                            searchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                )
            )
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {

            // --- Default Dialer Warning Banner ---
            if (!isDefaultDialer) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFF6B00))
                        .clickable { 
                            if (useHapticFeedback) vibrate(context)
                            onSetDefaultDialer() 
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⚠️ Not set as default phone app",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Tap here to enable call handling & notifications",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- Missed Calls Section (only shown when not searching) ---
        if (searchQuery.isBlank()) {
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
                            color = GreenCall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider(thickness = 2.dp)
                }
            } else {
                items(missedCalls) { callEntry ->
                    // Try to find contact by number
                    // Normalize numbers for comparison (remove spaces, dashes, etc.)
                    val normalizedCallNumber = callEntry.contactId.replace(Regex("[^0-9+]"), "")
                    
                    val contact = allContacts.find { 
                        val normalizedContactNumber = it.number.replace(Regex("[^0-9+]"), "")
                        // Check if one ends with the other (to handle country codes roughly)
                        if (normalizedCallNumber.length > 6 && normalizedContactNumber.length > 6) {
                            normalizedCallNumber.endsWith(normalizedContactNumber) || normalizedContactNumber.endsWith(normalizedCallNumber)
                        } else {
                            normalizedCallNumber == normalizedContactNumber
                        }
                    } ?: Contact(
                            id = callEntry.id,
                            name = callEntry.contactId, // Show number as name
                            number = callEntry.contactId
                        )
                    ContactRow(
                        contact = contact,
                        onCallClick = { 
                            if (useHapticFeedback) vibrate(context)
                            onCallClick(contact.number) 
                        },
                        showFavoriteStar = false,
                        useHugeText = useHugeText
                    )
                    HorizontalDivider()
                }
            }
        }

        // --- Favorites Section (only shown when not searching) ---
        if (searchQuery.isBlank()) {
            item {
                SectionHeader(title = "Favorites")
            }

            if (favorites.isEmpty()) {
                item {
                    Text(
                        "No favorites",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                items(favorites) { contact ->
                    ContactRow(
                        contact = contact,
                        onCallClick = { 
                            if (useHapticFeedback) vibrate(context)
                            onCallClick(contact.number) 
                        },
                        showFavoriteStar = true,
                        useHugeText = useHugeText
                    )
                    HorizontalDivider()
                }
            }
        }

        // --- Phone Book Section (or Search Results) ---
        item {
            SectionHeader(title = if (searchQuery.isBlank()) "Phone Book" else "Search Results")
        }
        

        // Show search results count if searching
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "${filteredContacts.size} contact${if (filteredContacts.size != 1) "s" else ""} found",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

            items(filteredContacts) { contact ->
                ContactRow(
                    contact = contact,
                    onCallClick = { 
                        if (useHapticFeedback) vibrate(context)
                        onCallClick(contact.number) 
                    },
                    showFavoriteStar = true,
                    useHugeText = useHugeText
                )
                HorizontalDivider()
            }
        }
        
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            listState = listState
        )
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
                Column {
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
                    HorizontalScrollbar(
                        scrollState = scrollState,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
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
            .clickable { 
                android.util.Log.d("MainScreen", "Avatar clicked for ${contact.name}")
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
            .clickable {
                android.util.Log.d("MainScreen", "Green call button clicked")
                onClick()
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
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

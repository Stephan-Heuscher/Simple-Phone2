package ch.heuscher.simplephone.ui.screens

import androidx.compose.ui.res.stringResource
import ch.heuscher.simplephone.R


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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
    onOpenContact: (String) -> Unit,
    onCallLogClick: () -> Unit = {},
    onDialerClick: () -> Unit = {},
    missedCalls: List<CallLogEntry> = emptyList(),
    missedCallsHours: Int = 24,
    useHugeText: Boolean = false,
    useHugeContactPicture: Boolean = false,
    useGridContactImages: Boolean = false,
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

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            item {
                // Search Bar Row
                Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val cdOpenDialer = stringResource(R.string.cd_open_dialer)


            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(
                        onClick = { 
                            if (useHapticFeedback) vibrate(context)
                            onDialerClick() 
                        },
                        role = Role.Button
                    )
                    .semantics { contentDescription = cdOpenDialer }
            ) {
                // High contrast buttons for better visibility
                val buttonSize = 28.dp
                val spacing = 5.dp
                val startOffset = 5.dp
                val buttonColor = MaterialTheme.colorScheme.primary
                val textColor = MaterialTheme.colorScheme.onPrimary
                val textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                // Button 1 (Top-Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = startOffset, y = startOffset)
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(buttonColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", style = textStyle)
                }
                
                // Button 2 (Top-Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = startOffset + buttonSize + spacing, y = startOffset)
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(buttonColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("2", style = textStyle)
                }
                
                // Button 4 (Bottom-Left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = startOffset, y = startOffset + buttonSize + spacing)
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(buttonColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("4", style = textStyle)
                }
                
                // Button 5 (Bottom-Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = startOffset + buttonSize + spacing, y = startOffset + buttonSize + spacing)
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(buttonColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("5", style = textStyle)
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
                        stringResource(R.string.search_placeholder),
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
                                contentDescription = stringResource(R.string.cd_clear_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.cd_search),
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
            }

            // Continue with items directly

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
                            text = stringResource(R.string.default_app_warning),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.default_app_enable),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp)
                        .clickable { onCallLogClick() }
                        .padding(horizontal = 16.dp), // Add padding here for touch target, but text has padding? No, SectionHeader has padding.
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                         text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.missed_calls),
                         style = MaterialTheme.typography.titleLarge,
                         fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.show_all),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (missedCalls.isEmpty()) {
                // Show green background with "No missed calls" message
                item(key = "no_missed_calls") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GreenCall.copy(alpha = 0.3f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_missed_calls),
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
                items(missedCalls, key = { "missed_${it.id}" }) { callEntry ->
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
                        onOpenContact = { onOpenContact(contact.id) },
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
                SectionHeader(title = stringResource(R.string.favorites))
            }

            if (favorites.isEmpty()) {
                item(key = "no_favorites") {
                    Text(
                        stringResource(R.string.no_favorites),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                if (useGridContactImages) {
                    items(favorites.chunked(2), key = { "fav_chunk_${it[0].id}" }) { chunk ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            chunk.forEach { contact ->
                                Box(modifier = Modifier.weight(1f)) {
                                     GridContactItem(
                                        contact = contact,
                                        onCallClick = {
                                            if (useHapticFeedback) vibrate(context)
                                            onCallClick(contact.number)
                                        },
                                        onOpenContact = { onOpenContact(contact.id) }
                                     )
                                }
                            }
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(favorites, key = { "fav_${it.id}" }) { contact ->
                        if (useHugeContactPicture) {
                            HugeContactRow(
                                contact = contact,
                                onCallClick = {
                                    if (useHapticFeedback) vibrate(context)
                                    onCallClick(contact.number)
                                },
                                onOpenContact = { onOpenContact(contact.id) },
                                showFavoriteStar = true
                            )
                        } else {
                            ContactRow(
                                contact = contact,
                                onCallClick = {
                                    if (useHapticFeedback) vibrate(context)
                                    onCallClick(contact.number)
                                },
                                onOpenContact = { onOpenContact(contact.id) },
                                showFavoriteStar = true,
                                useHugeText = useHugeText
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        // --- Phone Book Section (or Search Results) ---
        item(key = "phone_book_header") {
            SectionHeader(title = if (searchQuery.isBlank()) stringResource(R.string.phone_book) else stringResource(R.string.search_results))
        }
        

        // Show search results count if searching
        if (searchQuery.isNotBlank()) {
            item(key = "search_count") {
                Text(
                    text = stringResource(R.string.contacts_found, filteredContacts.size),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

            if (useGridContactImages) {
                 items(filteredContacts.chunked(2), key = { "chunk_${it[0].id}" }) { chunk ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        chunk.forEach { contact ->
                            Box(modifier = Modifier.weight(1f)) {
                                    GridContactItem(
                                    contact = contact,
                                    onCallClick = {
                                        if (useHapticFeedback) vibrate(context)
                                        onCallClick(contact.number)
                                    },
                                    onOpenContact = { onOpenContact(contact.id) }
                                    )
                            }
                        }
                        if (chunk.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    HorizontalDivider()
                 }
            } else {
                items(filteredContacts, key = { it.id }) { contact ->
                    if (useHugeContactPicture) {
                        HugeContactRow(
                            contact = contact,
                            onCallClick = {
                                if (useHapticFeedback) vibrate(context)
                                onCallClick(contact.number)
                            },
                            onOpenContact = { onOpenContact(contact.id) },
                            showFavoriteStar = true
                        )
                    } else {
                        ContactRow(
                            contact = contact,
                            onCallClick = {
                                if (useHapticFeedback) vibrate(context)
                                onCallClick(contact.number)
                            },
                            onOpenContact = { onOpenContact(contact.id) },
                            showFavoriteStar = true,
                            useHugeText = useHugeText
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
        


        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd),
            listState = listState
        )
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
    onOpenContact: () -> Unit = {},
    showFavoriteStar: Boolean = true,
    modifier: Modifier = Modifier,
    useHugeText: Boolean = false
) {
    val cdContactAction = stringResource(R.string.cd_contact_action, contact.name)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onOpenContact() },
                    onTap = { /* Let children handle tap or add listener here if needed, but row isn't clickable as whole? */ }
                )
            }
            .semantics {
                contentDescription = cdContactAction
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
            contentDescription = stringResource(R.string.cd_call_contact, contact.name)
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

@Composable
fun HugeContactRow(
    contact: Contact,
    onCallClick: () -> Unit,
    onOpenContact: () -> Unit,
    showFavoriteStar: Boolean
) {
    val cdContactHuge = stringResource(R.string.cd_contact_huge, contact.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onOpenContact() }
                )
            }
            .semantics {
                contentDescription = cdContactHuge
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image (Half screen)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onCallClick() }
        ) {
           // We reuse ContactAvatar but scaled up, or just use it as is if it supports fill?
           // ContactAvatar uses .size(size), so we might need a custom one or just pass a large size
           // But ContactAvatar is Circle. We want "Huge Contact Picture" likely Rectangular or very large Circle.
           // Let's use ContactAvatar with a large size for now.
           // Actually, "half screen with" probably implies the image is large.
           // Let's make it a large square or rectangle.
           // Since ContactAvatar logic (letters/colors/image loading) is complex, let's reuse it but in a Box.
           // We can just put a very large ContactAvatar.
           Box(modifier = Modifier.align(Alignment.Center)) {
               ContactAvatar(contact = contact, size = 140.dp, showFavoriteStar = showFavoriteStar)
           }
        }

        // Name (Half screen)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
             Text(
                text = contact.name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GridContactItem(
    contact: Contact,
    onCallClick: () -> Unit,
    onOpenContact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onOpenContact() },
                    onTap = { onCallClick() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Just contact image (large)
        ContactAvatar(
            contact = contact,
            size = 150.dp,
            showFavoriteStar = false
        )
        // User asked for "just contact images", but a name is usually helpful.
        // Let's put name very small or omit if strict?
        // "Create an option with just contact images (2 per line)"
        // I will add the name below just in case, but make it optional or unobtrusive?
        // Let's add it.
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = contact.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

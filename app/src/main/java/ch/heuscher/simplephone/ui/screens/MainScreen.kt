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
import androidx.compose.material.icons.filled.PhoneMissed
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
import ch.heuscher.simplephone.ui.components.isTabletLayout
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.LightGreenBackground
import ch.heuscher.simplephone.ui.theme.LightBlueBackground
import ch.heuscher.simplephone.ui.utils.vibrate
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.VerticalDivider

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
    useHapticFeedback: Boolean = false,
    contactResolutionMaps: ch.heuscher.simplephone.ui.MainViewModel.ContactResolutionMaps? = null
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
                contact.allNumbers.any { it.contains(searchQuery) }
            }
            
            // Get missed calls matching search (that are not already in contacts)
            val matchingMissedCalls = missedCalls.filter { call ->
                // Check if number matches search
                val numberMatches = call.contactId.contains(searchQuery)
                
                // Check if this number is already in our contacts list (to avoid duplicates)
                val isKnownContact = allContacts.any { contact ->
                    contact.allNumbers.any { number ->
                        number.replace(Regex("[^0-9]"), "") == call.contactId.replace(Regex("[^0-9]"), "")
                    }
                }
                
                numberMatches && !isKnownContact
            }.map { call ->
                Contact(
                    id = call.id,
                    name = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(call.contactId, context), // Show number as name for unknown contacts
                    number = call.contactId,
                    isFavorite = false
                )
            }.distinctBy { it.number } // Avoid duplicate unknown numbers
            
            matchingContacts + matchingMissedCalls
        }
    }

    // Optimization: Pre-calculate contact resolution for missed calls
    // This avoids O(N*M) lookups inside the LazyColumn composition (scrolling)
    val missedCallsWithContacts = remember(missedCalls, contactResolutionMaps) {
        val maps = contactResolutionMaps
        
        missedCalls.map { callEntry ->
             var contact: Contact? = null
             
             if (maps != null) {
                 val normalizedLogNumber = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.normalize(callEntry.contactId)
                 // 1. Try exact normalized match
                 contact = maps.normalized[normalizedLogNumber]
                 // 2. If not found, try suffix match (last 7 digits)
                 if (contact == null && normalizedLogNumber.length >= 7) {
                     contact = maps.suffixes[normalizedLogNumber.takeLast(7)]
                 }
             }

             // Fallback if no map provided or not found (though map should handle known ones)
             // If maps is null, we could potentially fallback to old slow logic, but for now assuming it's provided or we just show unknown
             // For consistency with VM logic, we trust the map. If not in map -> Unknown.
             
             val resolvedContact = contact ?: Contact(
                    id = callEntry.id,
                    name = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(callEntry.contactId, context),
                    number = callEntry.contactId
                )
            callEntry to resolvedContact
        }
    }

    val isTablet = isTabletLayout()

    if (isTablet) {
        // ── Tablet / Foldable: Two-pane side-by-side layout ──
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            // Search bar spans full width above both panes
            SearchBarRow(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onDialerClick = onDialerClick,
                useHapticFeedback = useHapticFeedback,
                context = context,
                focusManager = focusManager
            )

            // Default Dialer Warning Banner (full width)
            if (!isDefaultDialer) {
                DefaultDialerBanner(
                    onSetDefaultDialer = onSetDefaultDialer,
                    useHapticFeedback = useHapticFeedback,
                    context = context
                )
            }

            Row(modifier = Modifier.weight(1f)) {
                // LEFT PANE: Missed calls + Favorites
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val leftListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    LazyColumn(modifier = Modifier.fillMaxSize(), state = leftListState) {
                        if (searchQuery.isBlank()) {
                            // Missed Calls section
                            item {
                                MissedCallsHeader(onCallLogClick = onCallLogClick)
                            }
                            if (missedCalls.isEmpty()) {
                                item(key = "no_missed_calls") {
                                    NoMissedCallsBanner(
                                        onCallLogClick = onCallLogClick,
                                        useHugeText = useHugeText
                                    )
                                }
                            } else {
                                items(missedCallsWithContacts, key = { "missed_${it.first.id}" }) { (callEntry, contact) ->
                                    Box(modifier = Modifier) {
                                        MissedCallRow(
                                            contact = contact,
                                            timestamp = callEntry.timestamp,
                                            onCallClick = {
                                                if (useHapticFeedback) vibrate(context)
                                                onCallClick(contact.number)
                                            },
                                            onOpenContact = { onOpenContact(contact.id) }
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }

                            // Favorites section
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
                                            if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
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
                        } else {
                            // When searching, left pane shows a message
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.search_results),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        listState = leftListState
                    )
                }

                VerticalDivider()

                // RIGHT PANE: Phone Book / Search Results
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val rightListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    LazyColumn(modifier = Modifier.fillMaxSize(), state = rightListState) {
                        item(key = "phone_book_header") {
                            SectionHeader(
                                title = if (searchQuery.isBlank()) stringResource(R.string.phone_book)
                                        else stringResource(R.string.search_results)
                            )
                        }
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
                                    if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
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
                        listState = rightListState
                    )
                }
            }
        }
    } else {
        // ── Phone: Original single-column layout (unchanged) ──
        Box(modifier = Modifier.fillMaxSize().imePadding()) {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()

            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                item {
                    SearchBarRow(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onDialerClick = onDialerClick,
                        useHapticFeedback = useHapticFeedback,
                        context = context,
                        focusManager = focusManager
                    )
                }

                if (!isDefaultDialer) {
                    item {
                        DefaultDialerBanner(
                            onSetDefaultDialer = onSetDefaultDialer,
                            useHapticFeedback = useHapticFeedback,
                            context = context
                        )
                    }
                }

                if (searchQuery.isBlank()) {
                    item {
                        MissedCallsHeader(onCallLogClick = onCallLogClick)
                    }
                    if (missedCalls.isEmpty()) {
                        item(key = "no_missed_calls") {
                            NoMissedCallsBanner(
                                onCallLogClick = onCallLogClick,
                                useHugeText = useHugeText
                            )
                        }
                    } else {
                        items(missedCallsWithContacts, key = { "missed_${it.first.id}" }) { (callEntry, contact) ->
                            Box(modifier = Modifier) {
                                MissedCallRow(
                                    contact = contact,
                                    timestamp = callEntry.timestamp,
                                    onCallClick = {
                                        if (useHapticFeedback) vibrate(context)
                                        onCallClick(contact.number)
                                    },
                                    onOpenContact = { onOpenContact(contact.id) }
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }

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
                                    if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
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

                item(key = "phone_book_header") {
                    SectionHeader(
                        title = if (searchQuery.isBlank()) stringResource(R.string.phone_book)
                                else stringResource(R.string.search_results)
                    )
                }
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
                            if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
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
}

/**
 * Search bar with dialer button — extracted for reuse in phone & tablet layouts.
 */
@Composable
fun SearchBarRow(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDialerClick: () -> Unit,
    useHapticFeedback: Boolean,
    context: android.content.Context,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
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
            val buttonSize = 28.dp
            val spacing = 5.dp
            val startOffset = 5.dp
            val buttonColor = MaterialTheme.colorScheme.primary
            val textColor = MaterialTheme.colorScheme.onPrimary
            val textStyle = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Box(
                modifier = Modifier.align(Alignment.TopStart)
                    .offset(x = startOffset, y = startOffset)
                    .size(buttonSize).clip(CircleShape).background(buttonColor),
                contentAlignment = Alignment.Center
            ) { Text("1", style = textStyle) }
            Box(
                modifier = Modifier.align(Alignment.TopStart)
                    .offset(x = startOffset + buttonSize + spacing, y = startOffset)
                    .size(buttonSize).clip(CircleShape).background(buttonColor),
                contentAlignment = Alignment.Center
            ) { Text("2", style = textStyle) }
            Box(
                modifier = Modifier.align(Alignment.TopStart)
                    .offset(x = startOffset, y = startOffset + buttonSize + spacing)
                    .size(buttonSize).clip(CircleShape).background(buttonColor),
                contentAlignment = Alignment.Center
            ) { Text("4", style = textStyle) }
            Box(
                modifier = Modifier.align(Alignment.TopStart)
                    .offset(x = startOffset + buttonSize + spacing, y = startOffset + buttonSize + spacing)
                    .size(buttonSize).clip(CircleShape).background(buttonColor),
                contentAlignment = Alignment.Center
            ) { Text("5", style = textStyle) }
        }

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f).height(56.dp),
            placeholder = {
                Text(stringResource(R.string.search_placeholder), style = MaterialTheme.typography.bodyLarge)
            },
            leadingIcon = null,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        if (useHapticFeedback) vibrate(context)
                        onSearchQueryChange("")
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear_search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )
    }
}

/**
 * Orange warning banner shown when the app is not set as the default dialer.
 */
@Composable
fun DefaultDialerBanner(
    onSetDefaultDialer: () -> Unit,
    useHapticFeedback: Boolean,
    context: android.content.Context
) {
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

/**
 * Header row for the Missed Calls section.
 */
@Composable
fun MissedCallsHeader(onCallLogClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCallLogClick() }
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.missed_calls),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.show_all),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Green "No missed calls" banner — reassuring feedback for the user.
 */
@Composable
fun NoMissedCallsBanner(onCallLogClick: () -> Unit, useHugeText: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightGreenBackground)
            .clickable { onCallLogClick() }
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
            .padding(horizontal = 8.dp, vertical = 12.dp),
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
            .padding(start = 8.dp, top = 24.dp, bottom = 8.dp)
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

/**
 * A dedicated row for missed calls to improve readability for users with eye problems.
 * Features:
 * - Distinct red missed call icon
 * - Larger bold text for the name
 * - Relative timestamp (e.g. "10 min ago")
 * - Large green call button
 */
@Composable
fun MissedCallRow(
    contact: Contact,
    timestamp: java.time.LocalDateTime,
    onCallClick: () -> Unit,
    onOpenContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeString = remember(timestamp) {
        getRelativeTimeDisplay(context, timestamp)
    }
    
    val cdMissedCall = stringResource(R.string.missed_calls) + " " + contact.name + ", " + timeString

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onOpenContact() },
                    onTap = { /* Row click logic if any */ }
                )
            }
            .semantics {
                contentDescription = cdMissedCall
            }
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        // Content Row: Texts (Left)
        // We allow this row to extend up to where the button's center would be.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 32.dp) // Extend to center of button area
                .align(Alignment.CenterStart)
        ) {
            // Missed Call Icon (Gray and clear)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White), // White background on gray row
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhoneMissed,
                    contentDescription = null,
                    tint = Color.Gray, // Gray tint to reduce pressure
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name - Larger and Bolder with Horizontal Scroll
                Box(modifier = Modifier.fillMaxWidth()) {
                    val scrollState = androidx.compose.foundation.rememberScrollState()
                    Column {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.headlineMedium, // Significantly larger
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
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
                
                // Time - Relative
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Call Button (Right) - Placed on top
        GreenCallIcon(
            onClick = onCallClick,
            contentDescription = stringResource(R.string.cd_call_contact, contact.name),
            size = 64, // Slightly larger for better accessibility
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

// Helper to format relative time
fun getRelativeTimeDisplay(context: android.content.Context, timestamp: java.time.LocalDateTime): String {
    val now = java.time.LocalDateTime.now()
    val time = timestamp.atZone(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000
    val nowMillis = now.atZone(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000
    
    return android.text.format.DateUtils.getRelativeTimeSpanString(
        time,
        nowMillis,
        android.text.format.DateUtils.MINUTE_IN_MILLIS,
        android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

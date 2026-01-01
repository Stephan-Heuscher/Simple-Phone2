package ch.heuscher.simplephone.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.heuscher.simplephone.R
import ch.heuscher.simplephone.data.CallLogRepository
import ch.heuscher.simplephone.model.CallLogEntry
import ch.heuscher.simplephone.model.CallType
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.ContactAvatar
import ch.heuscher.simplephone.ui.components.VerticalScrollbar
import ch.heuscher.simplephone.ui.components.HorizontalScrollbar
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.RedHangup
import ch.heuscher.simplephone.ui.utils.vibrate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(
    onCallClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onAddContact: (String) -> Unit = {},
    callLogRepository: CallLogRepository,
    contacts: List<Contact>,
    useHugeText: Boolean = false,
    useHapticFeedback: Boolean = true
) {
    val context = LocalContext.current
    var callLogs by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        callLogs = callLogRepository.getAllCallLogs()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.call_history),
                        style = if (useHugeText) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (useHapticFeedback) vibrate(context)
                        onBackClick() 
                    }) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(if (useHugeText) 32.dp else 24.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val listState = rememberLazyListState()
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                items(callLogs) { log ->
                    // Find contact
                    val normalizedLogNumber = log.contactId.replace(Regex("[^0-9+]"), "")
                    val foundContact = contacts.find { 
                        val normalizedContact = it.number.replace(Regex("[^0-9+]"), "")
                        if (normalizedLogNumber.length > 6 && normalizedContact.length > 6) {
                            normalizedLogNumber.endsWith(normalizedContact) || normalizedContact.endsWith(normalizedLogNumber)
                        } else {
                            normalizedLogNumber == normalizedContact
                        }
                    }
                    
                    // Check if this is a known contact or unknown number
                    val isKnownContact = foundContact != null
                    val contact = foundContact ?: Contact(
                        id = log.id,
                        name = log.contactId, // Number as name for unknown
                        number = log.contactId
                    )

                    CallLogItem(
                        log = log,
                        contact = contact,
                        isKnownContact = isKnownContact,
                        onCallClick = { 
                            if (useHapticFeedback) vibrate(context)
                            onCallClick(contact.number)
                        },
                        onAddContact = { 
                            if (!isKnownContact) {
                                onAddContact(contact.number)
                            }
                        },
                        useHugeText = useHugeText
                    )
                    HorizontalDivider()
                }
            }
            
            // Persistent vertical scrollbar
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd),
                listState = listState
            )
        }
    }
}

@Composable
fun CallLogItem(
    log: CallLogEntry,
    contact: Contact,
    isKnownContact: Boolean,
    onCallClick: () -> Unit,
    onAddContact: () -> Unit = {},
    useHugeText: Boolean
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onAddContact() }
                )
            }
            .padding(if (useHugeText) 20.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on type - larger when useHugeText
        val iconSize = if (useHugeText) 36.dp else 24.dp
        val (icon, tint) = when (log.type) {
            CallType.MISSED -> Icons.Default.CallMissed to RedHangup
            CallType.INCOMING -> Icons.Default.CallReceived to Color.Blue
            CallType.OUTGOING -> Icons.Default.CallMade to GreenCall
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
        
        Spacer(modifier = Modifier.width(if (useHugeText) 20.dp else 16.dp))
        
        // Avatar with favorite star for known contacts - same size as MainScreen
        val avatarSize = if (useHugeText) 80.dp else 64.dp
        ContactAvatar(
            contact = contact,
            size = avatarSize,
            showFavoriteStar = contact.isFavorite // Show star for favorites
        )
        
        Spacer(modifier = Modifier.width(if (useHugeText) 20.dp else 16.dp))
        
        // Info column
        Column(modifier = Modifier.weight(1f)) {
            // Name with horizontal scroll for long names (2 lines max)
            val nameScrollState = rememberScrollState()
            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = contact.name,
                        style = if (useHugeText) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(nameScrollState)
                    )
                    // Show scrollbar indicator if content is scrollable
                    HorizontalScrollbar(
                        scrollState = nameScrollState,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            // Only show phone number if this is an UNKNOWN contact
            if (!isKnownContact) {
                // Don't show number again if name IS the number
            } else {
                // For known contacts, optionally show number (smaller text)
                Text(
                    text = contact.number,
                    style = if (useHugeText) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Date and duration row
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dateStr = try {
                    val zdt = log.timestamp.atZone(ZoneId.systemDefault())
                    DateUtils.getRelativeTimeSpanString(
                        zdt.toInstant().toEpochMilli(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                } catch (e: Exception) {
                    log.timestamp.toString()
                }
                
                Text(
                    text = dateStr,
                    style = if (useHugeText) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (log.type != CallType.MISSED && log.duration > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        style = if (useHugeText) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Format duration
                    val durationStr = if (log.duration < 60) {
                        "${log.duration}s"
                    } else if (log.duration < 3600) {
                        "${log.duration / 60}m ${log.duration % 60}s"
                    } else {
                        "${log.duration / 3600}h ${(log.duration % 3600) / 60}m"
                    }
                    
                    Text(
                        text = durationStr,
                        style = if (useHugeText) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Green call button - same as MainScreen
        GreenCallIcon(
            onClick = onCallClick,
            contentDescription = "Call ${contact.name}",
            size = if (useHugeText) 72 else 56
        )
    }
}

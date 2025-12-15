package ch.heuscher.simplephone.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.heuscher.simplephone.data.CallLogRepository
import ch.heuscher.simplephone.model.CallLogEntry
import ch.heuscher.simplephone.model.CallType
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.ContactAvatar
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.RedHangup
import ch.heuscher.simplephone.ui.utils.vibrate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(
    onCallClick: (String) -> Unit,
    onBackClick: () -> Unit,
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
    
    // Placeholder UI until data is loaded
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(ch.heuscher.simplephone.R.string.call_history)) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (useHapticFeedback) vibrate(context)
                        onBackClick() 
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(callLogs) { log ->
                 // Find contact
                val normalizedLogNumber = log.contactId.replace(Regex("[^0-9+]"), "")
                val contact = contacts.find { 
                    val normalizedContact = it.number.replace(Regex("[^0-9+]"), "")
                    if (normalizedLogNumber.length > 6 && normalizedContact.length > 6) {
                        normalizedLogNumber.endsWith(normalizedContact) || normalizedContact.endsWith(normalizedLogNumber)
                    } else {
                        normalizedLogNumber == normalizedContact
                    }
                } ?: Contact(
                    id = log.id,
                    name = log.contactId, // Number as name
                    number = log.contactId
                )

                CallLogItem(
                    log = log,
                    contact = contact,
                    onClick = { 
                         if (useHapticFeedback) vibrate(context)
                         onCallClick(contact.number)
                    },
                    useHugeText = useHugeText
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun CallLogItem(
    log: CallLogEntry,
    contact: Contact,
    onClick: () -> Unit,
    useHugeText: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on type
        val (icon, tint) = when (log.type) {
            CallType.MISSED -> Icons.Default.CallMissed to RedHangup
            CallType.INCOMING -> Icons.Default.CallReceived to Color.Blue
            CallType.OUTGOING -> Icons.Default.CallMade to GreenCall
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null, // decorative
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Avatar
        ContactAvatar(
            contact = contact,
            size = 48.dp,
            showFavoriteStar = false
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = if (useHugeText) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.number,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Date formatting
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

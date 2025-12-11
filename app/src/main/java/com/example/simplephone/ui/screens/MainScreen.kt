package com.example.simplephone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simplephone.data.MockData
import com.example.simplephone.model.CallType
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(
    onCallClick: (String) -> Unit,
    onContactClick: (String) -> Unit
) {
    val lastCall = remember { MockData.getRecents().maxByOrNull { it.timestamp } }
    val favorites = remember { MockData.contacts.filter { it.isFavorite } }
    val allContacts = remember { MockData.contacts.sortedBy { it.name } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        
        // --- Last Call Section ---
        item {
            SectionHeader(title = "Last Call")
        }

        if (lastCall != null) {
            item {
                val contact = MockData.getContactById(lastCall.contactId)
                val name = contact?.name ?: "Unknown"
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCallClick(contact?.number ?: "") }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (lastCall.type) {
                            CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
                            CallType.OUTGOING -> Icons.Filled.Call
                            CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed
                        }
                        val tint = if (lastCall.type == CallType.MISSED) Color.Red else MaterialTheme.colorScheme.onSurface

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.padding(end = 16.dp).size(40.dp)
                        )

                        Column {
                            Text(name, style = MaterialTheme.typography.headlineSmall)
                            Text(contact?.number ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Text(
                        lastCall.timestamp.format(formatter),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                HorizontalDivider()
            }
        } else {
            item {
                Text(
                    "No recent calls",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCallClick(contact.number) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp).size(32.dp)
                    )
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                HorizontalDivider()
            }
        }

        // --- Phone Book Section ---
        item {
            SectionHeader(title = "Phone Book")
        }

        items(allContacts) { contact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContactClick(contact.id) }
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.displaySmall
                )
            }
            HorizontalDivider()
        }
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

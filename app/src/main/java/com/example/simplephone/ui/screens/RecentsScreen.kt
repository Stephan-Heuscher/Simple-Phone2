package com.example.simplephone.ui.screens

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.simplephone.data.MockData
import com.example.simplephone.model.CallType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun RecentsScreen(
    filterHours: Int = 2 // Default configurable
) {
    val recents = remember { 
        MockData.getRecents().filter { 
            it.timestamp.isAfter(LocalDateTime.now().minusHours(filterHours.toLong())) 
        } 
    }

    if (recents.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No recent calls", style = MaterialTheme.typography.displaySmall)
            Text("(Last $filterHours hours)", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(recents) { call ->
                val contact = MockData.getContactById(call.contactId)
                val name = contact?.name ?: "Unknown"
                val formatter = DateTimeFormatter.ofPattern("HH:mm")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         val icon = when (call.type) {
                            CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
                            CallType.OUTGOING -> Icons.Filled.Call
                            CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed
                        }
                        val tint = if (call.type == CallType.MISSED) Color.Red else MaterialTheme.colorScheme.onSurface
                        
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.padding(end = 16.dp).size(40.dp)
                        )
                        
                        Column {
                            Text(name, style = MaterialTheme.typography.headlineSmall) // Big Name
                            Text(contact?.number ?: "Unknown", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Text(
                        call.timestamp.format(formatter),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

package com.example.simplephone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.simplephone.data.MockData

@Composable
fun PhoneBookScreen(
    onContactClick: (String) -> Unit
) {
    val contacts = remember { MockData.contacts.sortedBy { it.name } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(contacts) { contact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContactClick(contact.id) }
                    .padding(24.dp), // Check huge padding for touch target
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Initial Circle could go here, but keeping it simple accessible text first
                 Text(
                    text = contact.name,
                    style = MaterialTheme.typography.displaySmall // Very large text
                )
            }
            HorizontalDivider()
        }
    }
}

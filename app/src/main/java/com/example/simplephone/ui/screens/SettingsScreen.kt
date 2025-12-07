package com.example.simplephone.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    filterHours: Int,
    onFilterChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Recents Filter", style = MaterialTheme.typography.headlineLarge)
        Text("Hide calls older than:", style = MaterialTheme.typography.bodyLarge)
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 32.dp).fillMaxWidth()
        ) {
            IconButton(
                onClick = { if (filterHours > 1) onFilterChange(filterHours - 1) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease", modifier = Modifier.fillMaxSize())
            }
            
            Text(
                "$filterHours hours", 
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(
                onClick = { onFilterChange(filterHours + 1) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.fillMaxSize())
            }
        }
    }
}

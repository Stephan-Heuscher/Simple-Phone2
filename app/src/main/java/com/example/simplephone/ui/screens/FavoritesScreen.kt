package com.example.simplephone.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.simplephone.data.MockData
import com.example.simplephone.ui.components.BigButton

@Composable
fun FavoritesScreen(
    onCallClick: (String) -> Unit,
    isExpandedScreen: Boolean = false // Passed from WindowSizeClass
) {
    val favorites = remember { MockData.contacts.filter { it.isFavorite } }
    
    // Adaptive columns: 2 for phone, 4 for tablet/unfolded
    val columns = if (isExpandedScreen) 4 else 2 

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(favorites) { contact ->
            BigButton(
                text = contact.name,
                icon = Icons.Filled.Person,
                onClick = { onCallClick(contact.number) },
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

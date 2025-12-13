package ch.heuscher.simplephone.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ch.heuscher.simplephone.data.MockData

@Composable
fun PhoneBookScreen(
    onContactClick: (String) -> Unit,
    onCallClick: (String) -> Unit = {}
) {
    val contacts = remember { MockData.contacts.sortedBy { it.name } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(contacts) { contact ->
            // Reuse ContactRow from MainScreen for consistent look
            ContactRow(
                contact = contact,
                onCallClick = { onCallClick(contact.number) },
                showFavoriteStar = true
            )
            HorizontalDivider()
        }
    }
}

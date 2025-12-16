package ch.heuscher.simplephone.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ch.heuscher.simplephone.data.MockData

import ch.heuscher.simplephone.model.Contact

@Composable
fun PhoneBookScreen(
    contacts: List<Contact>,
    onContactClick: (String) -> Unit,
    onCallClick: (String) -> Unit = {},
    onEditClick: (String) -> Unit
) {
    val sortedContacts = remember(contacts) { contacts.sortedBy { it.name } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sortedContacts) { contact ->
            // Reuse ContactRow from MainScreen for consistent look
            ContactRow(
                contact = contact,
                onCallClick = { onCallClick(contact.number) },
                onEditClick = { onEditClick(contact.id) },
                showFavoriteStar = true
            )
            HorizontalDivider()
        }
    }
}

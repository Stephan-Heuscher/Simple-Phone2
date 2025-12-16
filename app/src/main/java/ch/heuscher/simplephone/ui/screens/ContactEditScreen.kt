package ch.heuscher.simplephone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditScreen(
    contact: Contact? = null,
    initialNumber: String? = null,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ContactRepository(context) }
    
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var number by remember { mutableStateOf(contact?.number ?: initialNumber ?: "") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (contact == null) "Add Contact" else "Edit Contact") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (contact == null) {
                            repository.addContact(name, number)
                        } else {
                            repository.updateContact(contact.id, name, number)
                        }
                        onSave()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = number,
                onValueChange = { number = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

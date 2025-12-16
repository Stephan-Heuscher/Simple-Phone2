package ch.heuscher.simplephone.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.model.Contact
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditScreen(
    contact: Contact? = null,
    initialNumber: String? = null,
    useHugeText: Boolean = false,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ContactRepository(context) }
    
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var number by remember { mutableStateOf(contact?.number ?: initialNumber ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(contact?.imageUri?.let { Uri.parse(it) }) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = it }
    }
    
    val avatarSize = if (useHugeText) 160.dp else 120.dp
    val textStyle = if (useHugeText) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge
    val labelStyle = if (useHugeText) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (contact == null) "Add Contact" else "Edit Contact",
                        style = if (useHugeText) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            modifier = Modifier.size(if (useHugeText) 32.dp else 24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (contact == null) {
                            repository.addContact(name, number, imageUri?.toString())
                        } else {
                            repository.updateContact(contact.id, name, number, imageUri?.toString())
                        }
                        onSave()
                    }) {
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = "Save",
                            modifier = Modifier.size(if (useHugeText) 32.dp else 24.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(if (useHugeText) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (useHugeText) 24.dp else 16.dp)
        ) {
            // Contact Image with edit overlay
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Contact Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(avatarSize / 2),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Camera overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change Photo",
                        modifier = Modifier.size(if (useHugeText) 48.dp else 32.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Text(
                text = "Tap to change photo",
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(if (useHugeText) 16.dp else 8.dp))
            
            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", style = labelStyle) },
                textStyle = textStyle,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Phone number field - read only
            OutlinedTextField(
                value = number,
                onValueChange = { },
                label = { Text("Phone Number", style = labelStyle) },
                textStyle = textStyle,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                enabled = false
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save button at bottom
            Button(
                onClick = {
                    if (contact == null) {
                        repository.addContact(name, number, imageUri?.toString())
                    } else {
                        repository.updateContact(contact.id, name, number, imageUri?.toString())
                    }
                    onSave()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (useHugeText) 72.dp else 56.dp)
            ) {
                Text(
                    text = if (contact == null) "Add Contact" else "Save Changes",
                    style = if (useHugeText) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

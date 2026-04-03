package ch.heuscher.simplephone.watch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SyncedContact(val id: String, val name: String, val number: String, val photoBitmap: Bitmap? = null)

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private val dataClient by lazy { Wearable.getDataClient(this) }
    private var contactsState = mutableStateListOf<SyncedContact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SimplePhoneWatchApp(this, contactsState)
            }
        }
        loadInitialContacts()
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(this)
    }

    private fun loadInitialContacts() {
        dataClient.dataItems.addOnSuccessListener { items ->
            for (item in items) {
                if (item.uri.path == "/contacts") {
                    updateContactsFromDataItem(item)
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.dataItem.uri.path == "/contacts") {
                updateContactsFromDataItem(event.dataItem)
            }
        }
    }

    private fun updateContactsFromDataItem(dataItem: com.google.android.gms.wearable.DataItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                val favoritesArray = dataMap.getDataMapArrayList("favorites")

                val newContacts = favoritesArray?.mapNotNull { map ->
                    val id = map.getString("id")
                    val name = map.getString("name")
                    val number = map.getString("number")
                    val asset = map.getAsset("photo")

                    var bitmap: Bitmap? = null
                    if (asset != null) {
                        bitmap = loadBitmapFromAsset(asset)
                    }

                    if (id != null && name != null && number != null) {
                        SyncedContact(id, name, number, bitmap)
                    } else null
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    contactsState.clear()
                    contactsState.addAll(newContacts)
                }
            } catch (e: Exception) {
                Log.e("WatchMainActivity", "Failed to parse contacts", e)
            }
        }
    }

    private fun loadBitmapFromAsset(asset: Asset): Bitmap? {
        if (asset.fd == null) {
            // Need to fetch it blocking
            try {
                val assetInputStream = Tasks.await(dataClient.getFdForAsset(asset)).inputStream
                if (assetInputStream != null) {
                    return BitmapFactory.decodeStream(assetInputStream)
                }
            } catch (e: Exception) {
                Log.e("WatchMainActivity", "Error fetching asset blocking", e)
            }
        }
        return null
    }
}

@Composable
fun SimplePhoneWatchApp(context: Context, contacts: List<SyncedContact>) {
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(32.dp)) }

        if (contacts.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.watch_no_favorites),
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(contacts) { contact ->
                ContactButton(contact = contact) {
                    makeCall(context, contact.number)
                }
            }
        }

        item {
            ActionButton(text = stringResource(R.string.watch_emergency_call), color = Color(0xFFE53935)) {
                makeCall(context, context.getString(R.string.watch_emergency_number))
            }
        }
        
        item {
            ActionButton(text = stringResource(R.string.watch_find_phone), color = Color(0xFFFB8C00)) {
                findMyPhone(context)
            }
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

private fun findMyPhone(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes)
            val messageClient = Wearable.getMessageClient(context)
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/find_my_phone", ByteArray(0))
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "Failed to send find phone message", e)
        }
    }
}

@Composable
fun ContactButton(contact: SyncedContact, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1E88E5)),
        modifier = Modifier
            .fillMaxWidth(1f) // Use full width
            .padding(vertical = 8.dp)
            .height(84.dp) // Drastically increase height
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (contact.photoBitmap != null) {
                Image(
                    bitmap = contact.photoBitmap.asImageBitmap(),
                    contentDescription = "Contact photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp) // Much larger photo
                        .clip(CircleShape)
                )
            } else {
                // Fallback Avatar
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = contact.name,
                color = Color.White,
                fontSize = 20.sp, // Readable text size
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        modifier = Modifier
            .fillMaxWidth(1f) // Use full width
            .padding(vertical = 8.dp)
            .height(84.dp) // Match height
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun makeCall(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

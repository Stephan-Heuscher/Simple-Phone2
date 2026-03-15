package ch.heuscher.simplephone.watch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

data class SyncedContact(val id: String, val name: String, val number: String)

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
        try {
            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
            val favoritesArray = dataMap.getDataMapArrayList("favorites")
            
            val newContacts = favoritesArray?.mapNotNull { map ->
                val id = map.getString("id")
                val name = map.getString("name")
                val number = map.getString("number")
                if (id != null && name != null && number != null) {
                    SyncedContact(id, name, number)
                } else null
            } ?: emptyList()

            runOnUiThread {
                contactsState.clear()
                contactsState.addAll(newContacts)
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "Failed to parse contacts", e)
        }
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
        item { Spacer(modifier = Modifier.height(24.dp)) }
        
        if (contacts.isEmpty()) {
            item {
                Text(
                    text = "Keine Favoriten",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(contacts) { contact ->
                ActionButton(text = contact.name, color = Color(0xFF1E88E5)) {
                    makeCall(context, contact.number)
                }
            }
        }

        item {
            ActionButton(text = "Notruf", color = Color(0xFFE53935)) {
                makeCall(context, "112")
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = color),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 6.dp)
            .height(64.dp)
    ) {
        Text(
            text = text, 
            color = Color.White, 
            fontSize = 20.sp, 
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


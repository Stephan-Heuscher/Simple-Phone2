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
    private var isContactsLoading = mutableStateOf(true)

    private fun saveContactsToPrefs(contacts: List<SyncedContact>) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = getSharedPreferences("simple_phone_watch", Context.MODE_PRIVATE)
            val jsonArray = org.json.JSONArray()
            for (contact in contacts) {
                val jsonObject = org.json.JSONObject()
                jsonObject.put("id", contact.id)
                jsonObject.put("name", contact.name)
                jsonObject.put("number", contact.number)
                
                if (contact.photoBitmap != null) {
                    val outputStream = java.io.ByteArrayOutputStream()
                    contact.photoBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    val base64String = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.DEFAULT)
                    jsonObject.put("photoBase64", base64String)
                }
                
                jsonArray.put(jsonObject)
            }
            prefs.edit().putString("cached_contacts", jsonArray.toString()).apply()
        }
    }

    private fun loadContactsFromPrefs(): Boolean {
        val prefs = getSharedPreferences("simple_phone_watch", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("cached_contacts", null) ?: return false
        try {
            val jsonArray = org.json.JSONArray(jsonString)
            val cachedContacts = mutableListOf<SyncedContact>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                var photoBitmap: Bitmap? = null
                if (jsonObject.has("photoBase64")) {
                    try {
                        val base64String = jsonObject.getString("photoBase64")
                        val decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                        photoBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (e: Exception) {
                        Log.e("WatchMainActivity", "Failed to decode base64 bitmap", e)
                    }
                }

                cachedContacts.add(
                    SyncedContact(
                        jsonObject.getString("id"),
                        jsonObject.getString("name"),
                        jsonObject.getString("number"),
                        photoBitmap
                    )
                )
            }
            contactsState.clear()
            contactsState.addAll(cachedContacts)
            return true
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "Failed to load cached contacts", e)
        }
        return false
    }

    private val defaultDialerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.d("WatchMainActivity", "Default dialer role granted")
        } else {
            Log.w("WatchMainActivity", "Default dialer role denied or ignored")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure we are the default dialer to fully replace the stock watch phone app functions
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
                if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER) &&
                    !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER)
                ) {
                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                    defaultDialerLauncher.launch(intent)
                }
            } else {
                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                if (telecomManager.defaultDialerPackage != packageName) {
                    val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    intent.putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                    defaultDialerLauncher.launch(intent)
                }
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "Failed to ask for default dialer", e)
        }

        if (loadContactsFromPrefs()) {
            isContactsLoading.value = false
        }
        setContent {
            MaterialTheme {
                SimplePhoneWatchApp(this, contactsState, isContactsLoading.value)
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
            var found = false
            for (item in items) {
                if (item.uri.path == "/contacts") {
                    found = true
                    updateContactsFromDataItem(item)
                } else if (item.uri.path == "/settings") {
                    updateSettingsFromDataItem(item)
                }
            }
            if (!found && contactsState.isEmpty()) {
                isContactsLoading.value = false
            }
        }.addOnFailureListener {
            if (contactsState.isEmpty()) {
                isContactsLoading.value = false
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val path = event.dataItem.uri.path
            if (path == "/contacts") {
                updateContactsFromDataItem(event.dataItem)
            } else if (path == "/settings") {
                updateSettingsFromDataItem(event.dataItem)
            }
        }
    }

    private fun updateSettingsFromDataItem(dataItem: com.google.android.gms.wearable.DataItem) {
        try {
            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
            val prefs = getSharedPreferences("simple_phone_watch", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("setting_confirm_before_call", dataMap.getBoolean("confirm_before_call", false))
                putBoolean("setting_silence_call_on_touch", dataMap.getBoolean("silence_call_on_touch", false))
                putBoolean("setting_block_unknown_callers", dataMap.getBoolean("block_unknown_callers", false))
                putBoolean("setting_use_haptic_feedback", dataMap.getBoolean("use_haptic_feedback", true))
                apply()
            }
            Log.d("WatchMainActivity", "Updated settings from phone")
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "Failed to update settings", e)
        }
    }

    private fun updateContactsFromDataItem(dataItem: com.google.android.gms.wearable.DataItem) {
        try {
            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val favoritesArray = dataMap.getDataMapArrayList("favorites")

                    // FIRST PASS: Load names and numbers instantly so the UI can draw right away
                    val newContacts = favoritesArray?.mapNotNull { map ->
                        val id = map.getString("id")
                        val name = map.getString("name")
                        val number = map.getString("number")
                        val asset = map.getAsset("photo")

                        if (id != null && name != null && number != null) {
                            Pair(SyncedContact(id, name, number, null), asset)
                        } else null
                    } ?: emptyList()

                    val parsedContacts = newContacts.map { it.first }
                    
                    withContext(Dispatchers.Main) {
                        // Only clear out and recreate if there are actual updates.
                        // We avoid flickering the screen if it's already populated by cache.
                        if (contactsState.isEmpty()) {
                            contactsState.addAll(parsedContacts)
                            isContactsLoading.value = false
                        }
                    }

                    // SECOND PASS: Download high-res photos asynchronously without blocking the UI
                    val updatedContacts = parsedContacts.toMutableList()
                    var anyUpdates = false
                    
                    newContacts.forEachIndexed { index, pair ->
                        val asset = pair.second
                        if (asset != null) {
                            val bitmap = loadBitmapFromAsset(asset)
                            if (bitmap != null) {
                                updatedContacts[index] = updatedContacts[index].copy(photoBitmap = bitmap)
                                anyUpdates = true
                                withContext(Dispatchers.Main) {
                                    if (index < contactsState.size) {
                                        contactsState[index] = contactsState[index].copy(photoBitmap = bitmap)
                                    } else {
                                        contactsState.add(updatedContacts[index])
                                    }
                                }
                            }
                        }
                    }
                    
                    // Save to preferences whether or not there were photo updates, to keep it in sync.
                    saveContactsToPrefs(updatedContacts)
                    
                } catch (e: Exception) {
                    Log.e("WatchMainActivity", "Failed to parse contacts inside coroutine", e)
                    withContext(Dispatchers.Main) { if(contactsState.isEmpty()) isContactsLoading.value = false }
                }
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "Failed to get DataMap from DataItem", e)
            if(contactsState.isEmpty()) isContactsLoading.value = false
        }
    }

    private fun loadBitmapFromAsset(asset: Asset): Bitmap? {
        if (asset.fd == null) {
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
fun SimplePhoneWatchApp(context: Context, contacts: List<SyncedContact>, isLoading: Boolean) {
    var showConfirmDialog by remember { mutableStateOf<String?>(null) }
    
    val callPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(context, "Permission granted. Tap contact again.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun smartCall(number: String, contactName: String? = null, force: Boolean = false) {
        val prefs = context.getSharedPreferences("simple_phone_watch", Context.MODE_PRIVATE)
        val confirmReq = prefs.getBoolean("setting_confirm_before_call", false)
        
        if (confirmReq && !force) {
            showConfirmDialog = number
            return
        }

        // Immediately open the call screen with the name we already know
        val callIntent = Intent(context, WatchCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("CALLER_NAME", contactName ?: number)
            putExtra("CALLER_NUMBER", number)
            putExtra("IS_OUTGOING", true)
        }
        context.startActivity(callIntent)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                // isNearby == true means Bluetooth (direct) connection — audio routing works.
                // isNearby == false means WLAN/cloud relay only — BT audio can't be routed,
                // so we must dial directly on the watch for the user to hear anything.
                val nearbyNode = nodes.firstOrNull { it.isNearby }
                if (nearbyNode != null) {
                    // Phone is connected via Bluetooth, route through phone
                    val messageClient = Wearable.getMessageClient(context)
                    messageClient.sendMessage(nearbyNode.id, "/initiate_call", number.toByteArray(Charsets.UTF_8))
                } else {
                    // No Bluetooth connection (WLAN only or no phone at all) — dial directly
                    withContext(Dispatchers.Main) {
                        val hasTelephony = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY)
                        if (!hasTelephony) {
                            android.widget.Toast.makeText(context, context.getString(R.string.watch_error_no_telephony), android.widget.Toast.LENGTH_LONG).show()
                            return@withContext
                        }
                        
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            callPermissionLauncher.launch(android.Manifest.permission.CALL_PHONE)
                        } else {
                            try {
                                val dialIntent = Intent(Intent.ACTION_CALL).apply {
                                    data = Uri.parse("tel:$number")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(dialIntent)
                            } catch (e: Exception) {
                                Log.e("WatchMainActivity", "Failed to start direct call", e)
                                android.widget.Toast.makeText(context, context.getString(R.string.watch_error_no_telephony), android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WatchMainActivity", "Failed to smart call", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Call error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Confirmation Dialog
    androidx.wear.compose.material.dialog.Dialog(
        showDialog = showConfirmDialog != null,
        onDismissRequest = { showConfirmDialog = null }
    ) {
        androidx.wear.compose.material.dialog.Alert(
            title = {
                Text(
                    text = "Call ${showConfirmDialog ?: ""}?",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        ) {
            item {
                Button(
                    onClick = {
                        val num = showConfirmDialog
                        showConfirmDialog = null
                        if (num != null) smartCall(num, force = true)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF43A047))
                ) { Text("Call", color = Color.White) }
            }
            item {
                Button(
                    onClick = { showConfirmDialog = null },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935))
                ) { Text("Cancel", color = Color.White) }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 32.dp),
        // Use negative spacing so they underlap slightly
        verticalArrangement = Arrangement.spacedBy((-16).dp)
    ) {
        item { Spacer(modifier = Modifier.height(32.dp)) }

        if (contacts.isEmpty()) {
            if (isLoading) {
                item {
                    androidx.wear.compose.material.CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        indicatorColor = Color(0xFF1E88E5)
                    )
                }
            } else {
                item {
                    Text(
                        text = stringResource(R.string.watch_no_favorites),
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(contacts) { contact ->
                ContactButton(contact = contact) {
                    smartCall(contact.number, contact.name)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            ActionButton(text = stringResource(R.string.watch_emergency_call), color = Color(0xFFE53935)) {
                smartCall(context.getString(R.string.watch_emergency_number))
            }
        }
        
        item {
            ActionButton(text = stringResource(R.string.watch_find_phone), color = Color(0xFFFB8C00)) {
                findMyPhone(context)
            }
        }
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

private fun initiatePhoneCall(context: Context, number: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = Tasks.await(nodeClient.connectedNodes)
            val messageClient = Wearable.getMessageClient(context)
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/initiate_call", number.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Log.e("WatchMainActivity", "Failed to send initiate call message", e)
        }
    }
}

@Composable
fun ContactButton(contact: SyncedContact, onClick: () -> Unit) {
    // Massive full-width card that shows a big image
    androidx.wear.compose.material.Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.85f) // A bit less wide
            .height(140.dp) // Huge height
            .padding(horizontal = 0.dp),
        contentPadding = PaddingValues(0.dp),
        backgroundPainter = androidx.wear.compose.material.CardDefaults.cardBackgroundPainter(
            startBackgroundColor = Color(0xFF1E88E5),
            endBackgroundColor = Color(0xFF1565C0)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (contact.photoBitmap != null) {
                Image(
                    bitmap = contact.photoBitmap.asImageBitmap(),
                    contentDescription = "Contact photo",
                    contentScale = ContentScale.Crop, // Crop to use whole width & height
                    alignment = androidx.compose.ui.BiasAlignment(0f, 1.0f), // Shifted crop focus proportionally downwards
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dark gradient overlay so the text is extremely readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 50f
                        )
                    )
            )

            // Huge text overlapping the massive image
            Text(
                text = contact.name,
                color = Color.White,
                fontSize = 28.sp, // Bigger text
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
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


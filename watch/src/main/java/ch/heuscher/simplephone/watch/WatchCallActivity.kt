package ch.heuscher.simplephone.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WatchCallActivity : androidx.fragment.app.FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {

    private var isCallActive = true
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val bringToFrontRunnable = Runnable {
        if (isCallActive && !isDestroyed && !isFinishing) {
            val bringToFrontIntent = Intent(this@WatchCallActivity, WatchCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(bringToFrontIntent)
        }
    }

    private val _callState = mutableIntStateOf(0) // Default 0
    private val _callerName = mutableStateOf("")
    private val _contactPhoto = mutableStateOf<Bitmap?>(null)
    private val _audioRoute = mutableIntStateOf(1) // Default to EARPIECE (1)
    private val _volumePercent = mutableIntStateOf(0)
    private val _isMuted = mutableStateOf(false)
    private val _watchInitiated = mutableStateOf(false)
    
    private val _isAmbient = mutableStateOf(false)
    private val _ambientUpdateTrigger = mutableIntStateOf(0)
    private lateinit var ambientController: AmbientModeSupport.AmbientController

    private val syncStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val jsonPayload = intent?.getStringExtra("SYNC_STATE_JSON") ?: return
            handleSyncStateJson(jsonPayload)
            
            if (isCallActive && !isDestroyed && !isFinishing) {
                val bringToFrontIntent = Intent(context, WatchCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(bringToFrontIntent)
            }
        }
    }

    private fun handleSyncStateJson(jsonString: String) {
        try {
            val json = org.json.JSONObject(jsonString)
            val newState = json.getInt("callState")
            _callState.intValue = newState
            
            val newName = json.getString("callerName")
            if (newName.isNotEmpty() && newName != _callerName.value) {
                _callerName.value = newName
                updatePhoto(newName)
            } else if (newName.isEmpty()) {
                val number = json.getString("callerNumber")
                if (number.isNotEmpty() && number != _callerName.value) {
                    _callerName.value = number
                }
            }
            
            _audioRoute.intValue = json.getInt("audioRoute")
            _volumePercent.intValue = json.getInt("volumePercent")
            _isMuted.value = json.getBoolean("isMuted")
            _watchInitiated.value = json.getBoolean("watchInitiated")
            
            if (newState == android.telecom.Call.STATE_DISCONNECTED) {
                isCallActive = false
                finish()
            }
        } catch (e: Exception) {
            Log.e("WatchCallActivity", "Failed to parse sync state", e)
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                _isAmbient.value = true
            }
            override fun onExitAmbient() {
                _isAmbient.value = false
            }
            override fun onUpdateAmbient() {
                _ambientUpdateTrigger.intValue++
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        ambientController = AmbientModeSupport.attach(this)
        isCallActive = true
        
        handleIntent(intent)

        // Register sync receiver
        registerReceiver(syncStateReceiver, IntentFilter("ch.heuscher.simplephone.watch.SYNC_CALL_STATE"), RECEIVER_NOT_EXPORTED)

        // Request initial status
        sendMessageToPhone("/request_audio_status")

        setContent {
            MaterialTheme {
                WatchCallScreen(
                    callerName = _callerName.value,
                    contactPhoto = _contactPhoto.value,
                    callState = _callState.intValue,
                    isAmbient = _isAmbient.value,
                    audioRoute = _audioRoute.intValue,
                    volumePercent = _volumePercent.intValue,
                    isMuted = _isMuted.value,
                    ambientUpdateTrigger = _ambientUpdateTrigger.intValue,
                    onAccept = {
                        // Immediately show answered state visually while request goes out
                        _callState.intValue = android.telecom.Call.STATE_ACTIVE
                        sendMessageToPhone("/answer_call")
                    },
                    onSilence = {
                        sendMessageToPhone("/silence_ringer")
                    },
                    onReject = {
                        isCallActive = false
                        sendMessageToPhone("/reject_call")
                        finish()
                    },
                    onHangup = {
                        isCallActive = false
                        sendMessageToPhone("/end_call")
                        finish()
                    },
                    onVolumeUp = {
                        sendMessageToPhone("/volume_up")
                    },
                    onVolumeDown = {
                        sendMessageToPhone("/volume_down")
                    },
                    onToggleMute = {
                        sendMessageToPhone("/toggle_mute")
                    },
                    onSetAudioRoute = { route ->
                        sendMessageToPhone("/set_audio_route", route.toString())
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        sendMessageToPhone("/request_audio_status")
    }

    private fun handleIntent(intent: Intent) {
        val jsonPayload = intent.getStringExtra("SYNC_STATE_JSON")
        if (jsonPayload != null) {
            handleSyncStateJson(jsonPayload)
        } else {
            // No sync state JSON means this wasn't launched by the phone app.
            // This can happen if the system or another path starts the activity.
            // Without accurate state data we should not show the call UI.
            Log.w("WatchCallActivity", "No SYNC_STATE_JSON in intent — finishing")
            isCallActive = false
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(bringToFrontRunnable)
    }

    override fun onStop() {
        super.onStop()
        val state = _callState.intValue
        if (isCallActive && !isFinishing &&
            (state == android.telecom.Call.STATE_ACTIVE || state == android.telecom.Call.STATE_RINGING)) {
            // System dialer likely took over. Re-assert focus after a short delay,
            // but only if there's genuinely an active or ringing call.
            handler.postDelayed(bringToFrontRunnable, 500)
        }
    }

    private fun updatePhoto(name: String) {
        val prefs = getSharedPreferences("simple_phone_watch", Context.MODE_PRIVATE)
        val cachedJson = prefs.getString("cached_contacts", null)
        if (cachedJson != null) {
            try {
                val array = org.json.JSONArray(cachedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("name") == name) {
                        val base64 = obj.optString("photoBase64", "")
                        if (base64.isNotEmpty()) {
                            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                            _contactPhoto.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                        return
                    }
                }
            } catch (e: Exception) {
                Log.e("WatchCallActivity", "Failed to parse cache for photo", e)
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            val prefs = getSharedPreferences("simple_phone_watch", Context.MODE_PRIVATE)
            if (prefs.getBoolean("setting_silence_call_on_touch", false) && _callState.intValue == android.telecom.Call.STATE_RINGING) {
                sendMessageToPhone("/silence_ringer")
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()
        isCallActive = false
        try {
            unregisterReceiver(syncStateReceiver)
        } catch (e: Exception) {
            // Might not be registered
        }
    }

    private fun sendMessageToPhone(path: String, payload: String = "") {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodeClient = Wearable.getNodeClient(this@WatchCallActivity)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                val messageClient = Wearable.getMessageClient(this@WatchCallActivity)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, payload.toByteArray())
                }
            } catch (e: Exception) {
                Log.e("WatchCallActivity", "Failed to send $path", e)
            }
        }
    }
}

@Composable
fun WatchCallScreen(
    callerName: String, 
    contactPhoto: Bitmap?, 
    callState: Int, 
    isAmbient: Boolean = false,
    audioRoute: Int = 1,
    volumePercent: Int = 0,
    isMuted: Boolean = false,
    ambientUpdateTrigger: Int = 0,
    onAccept: () -> Unit, 
    onSilence: () -> Unit, 
    onReject: () -> Unit, 
    onHangup: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit,
    onToggleMute: () -> Unit,
    onSetAudioRoute: (Int) -> Unit
) {
    if (isAmbient) {
        val trigger = ambientUpdateTrigger
        val currentTime = remember(trigger) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
        
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Thin
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = callerName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    val isIncoming = callState == android.telecom.Call.STATE_RINGING

    if (isIncoming) {
        // Incoming call: top avatar/name, bottom action buttons
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Top 2/3: Contact Photo / Name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                contentAlignment = Alignment.Center
            ) {
                if (contactPhoto != null) {
                    Image(
                        bitmap = contactPhoto.asImageBitmap(),
                        contentDescription = "Contact photo",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = callerName.take(1).uppercase(),
                            color = Color.White.copy(alpha = 0.2f),
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Name overlay with gradient for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 100f
                            )
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = callerName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
                    )
                }
            }

            // Bottom 1/3: Buttons side-by-side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Left: Silence (Blue)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(Color(0xFF1E88E5))
                        .clickable { onSilence() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.watch_silence),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Right: Accept (Green)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(Color(0xFF43A047))
                        .clickable { onAccept() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.watch_accept),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        // Active call: full-bleed photo like ContactButton, with disconnect button
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Big contact photo filling the screen
            if (contactPhoto != null) {
                Image(
                    bitmap = contactPhoto.asImageBitmap(),
                    contentDescription = "Contact photo",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alignment = androidx.compose.ui.BiasAlignment(0f, -0.3f),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback: large initial letter
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = callerName.take(1).uppercase(),
                        color = Color.White.copy(alpha = 0.15f),
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Dark gradient overlay from bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.95f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Bottom content: name + status + controls + hangup button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Name
                Text(
                    text = callerName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // Status with Volume and Mute
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isMuted) "MUTED" else "Vol: $volumePercent%",
                        color = if (isMuted) Color.Red else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = if (isMuted) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Audio route indicator
                    val routeLabel = when(audioRoute) {
                        2 -> "BT"
                        4 -> "SPEAKER"
                        else -> "PHONE"
                    }
                    Text(
                        text = "[$routeLabel]",
                        color = Color.Cyan,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BT Button
                    Button(
                        onClick = { onSetAudioRoute(2) },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (audioRoute == 2) Color.Blue else Color.DarkGray
                        )
                    ) {
                        Text("BT", fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Mute Button
                    Button(
                        onClick = { onToggleMute() },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isMuted) Color.Red else Color.DarkGray
                        )
                    ) {
                        Text(if (isMuted) "UNMUTE" else "MUTE", fontSize = 8.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Red Hangup Button
                    Button(
                        onClick = { onHangup() },
                        modifier = Modifier.size(44.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935))
                    ) {
                        Text(
                            text = "✕",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // Speaker Button
                    Button(
                        onClick = { onSetAudioRoute(8) },
                        modifier = Modifier.size(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (audioRoute == 8) Color.Green else Color.DarkGray
                        )
                    ) {
                        Text("SPK", fontSize = 10.sp)
                    }
                }
            }

            // Left Half: Volume Down (Huge invisible tap target for accessibility)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.3f)
                    .align(Alignment.CenterStart)
                    .clickable { onVolumeDown() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "-", 
                    fontSize = 48.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f), 
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Right Half: Volume Up (Huge invisible tap target for accessibility)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.3f)
                    .align(Alignment.CenterEnd)
                    .clickable { onVolumeUp() },
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "+", 
                    fontSize = 48.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.4f), 
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

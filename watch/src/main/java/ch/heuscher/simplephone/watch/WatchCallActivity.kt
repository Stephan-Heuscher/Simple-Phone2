package ch.heuscher.simplephone.watch

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.util.*

class WatchCallActivity : androidx.fragment.app.FragmentActivity() {

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

    private val _callState = mutableIntStateOf(0)
    private val _callerName = mutableStateOf("")
    private val _callerNumber = mutableStateOf("")
    private val _contactPhoto = mutableStateOf<Bitmap?>(null)
    private val _audioRoute = mutableIntStateOf(1)
    private val _volumePercent = mutableIntStateOf(0)
    private val _isMuted = mutableStateOf(false)
    private val _watchInitiated = mutableStateOf(false)
    private val _isOutgoing = mutableStateOf(false)

    private fun handleSyncStateJson(jsonString: String) {
        try {
            val json = org.json.JSONObject(jsonString)
            val newState = json.getInt("callState")
            _callState.intValue = newState
            
            val newName = json.optString("callerName", "")
            val number = json.optString("callerNumber", "")
            _callerNumber.value = number
            
            if (newName.isNotEmpty() && newName != _callerName.value) {
                _callerName.value = newName
                updatePhoto(newName, number)
            } else if (newName.isEmpty()) {
                if (number.isNotEmpty() && number != _callerName.value) {
                    _callerName.value = number
                    updatePhoto("", number)
                }
            }
            
            _audioRoute.intValue = json.optInt("audioRoute", 1)
            _volumePercent.intValue = json.optInt("volumePercent", 0)
            _isMuted.value = json.optBoolean("isMuted", false)
            _watchInitiated.value = json.optBoolean("watchInitiated", false)
            _isOutgoing.value = json.optBoolean("isOutgoing", false)
            
            if (newState == android.telecom.Call.STATE_DISCONNECTED) {
                isCallActive = false
                finish()
            }
        } catch (e: Exception) {
            Log.e("WatchCallActivity", "Failed to parse sync state", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isCallActive = true
        
        handleIntent(intent)

        setContent {
            MaterialTheme {
                WatchCallScreen(
                    callerName = _callerName.value,
                    callerNumber = _callerNumber.value,
                    contactPhoto = _contactPhoto.value,
                    callState = _callState.intValue,
                    volumePercent = _volumePercent.intValue,
                    watchInitiated = _watchInitiated.value,
                    isOutgoing = _isOutgoing.value,
                    onAccept = {
                        _callState.intValue = android.telecom.Call.STATE_ACTIVE
                        sendMessageToPhone("/answer_call")
                    },
                    onSilence = {
                        sendMessageToPhone("/silence_ringer")
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
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val jsonPayload = intent.getStringExtra("SYNC_STATE_JSON")
        if (jsonPayload != null) {
            handleSyncStateJson(jsonPayload)
            return
        }

        // Outgoing call launched directly from MainActivity — render placeholder
        // immediately while we wait for the phone's first /sync_call_state.
        val callerName = intent.getStringExtra("CALLER_NAME")
        if (callerName != null) {
            val callerNumber = intent.getStringExtra("CALLER_NUMBER") ?: ""
            val isOutgoing = intent.getBooleanExtra("IS_OUTGOING", false)
            _callerName.value = callerName
            _callerNumber.value = callerNumber
            _isOutgoing.value = isOutgoing
            _watchInitiated.value = isOutgoing
            _callState.intValue = if (isOutgoing) android.telecom.Call.STATE_DIALING else android.telecom.Call.STATE_NEW
            updatePhoto(callerName, callerNumber)
        } else {
            Log.w("WatchCallActivity", "No SYNC_STATE_JSON in intent — requesting status")
        }
        sendMessageToPhone("/request_audio_status")
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
            handler.postDelayed(bringToFrontRunnable, 500)
        }
    }

    private fun updatePhoto(name: String, number: String) {
        val prefs = getSharedPreferences("simple_phone_watch", Context.MODE_PRIVATE)
        val cachedJson = prefs.getString("cached_contacts", null)
        if (cachedJson != null) {
            try {
                val array = org.json.JSONArray(cachedJson)
                val cleanNumber = number.replace(Regex("[^0-9]"), "")
                
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val contactName = obj.optString("name", "")
                    val contactNumber = obj.optString("number", "").replace(Regex("[^0-9]"), "")
                    
                    if ((name.isNotEmpty() && contactName == name) || 
                        (cleanNumber.isNotEmpty() && contactNumber == cleanNumber) ||
                        (cleanNumber.length >= 7 && contactNumber.isNotEmpty() && cleanNumber.endsWith(contactNumber))) {
                        
                        if (_callerName.value == _callerNumber.value || _callerName.value.isEmpty()) {
                            _callerName.value = contactName
                        }
                        
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
    }

    private fun sendMessageToPhone(path: String, payload: String = "") {
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
    callerNumber: String,
    contactPhoto: Bitmap?,
    callState: Int,
    volumePercent: Int,
    watchInitiated: Boolean,
    isOutgoing: Boolean,
    onAccept: () -> Unit,
    onSilence: () -> Unit,
    onHangup: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit
) {
    val isIncoming = callState == android.telecom.Call.STATE_RINGING
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(lastInteractionTime) {
        if (lastInteractionTime > 0) {
            kotlinx.coroutines.delay(6000)
            lastInteractionTime = 0L
        }
    }
    val volumeOverlayVisible = lastInteractionTime > 0

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Background photo or initial
        if (contactPhoto != null) {
            Image(
                bitmap = contactPhoto.asImageBitmap(),
                contentDescription = "Contact photo",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alignment = androidx.compose.ui.BiasAlignment(0f, if (isIncoming) 0f else -0.3f),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (callerName.takeIf { it.isNotEmpty() } ?: "?").take(1).uppercase(),
                    color = Color.White.copy(alpha = 0.2f),
                    fontSize = if (isIncoming) 80.sp else 120.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Gradient overlay for text readability
        Box(
            modifier = Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.95f)),
                    startY = 100f
                )
            )
        )

        if (isIncoming) {
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = callerName.ifEmpty { callerNumber },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Text(
                    text = stringResource(R.string.watch_incoming_call),
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Silence Button
                    Button(
                        onClick = onSilence,
                        modifier = Modifier.size(56.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3)) // Bright Blue
                    ) {
                        Text(text = stringResource(R.string.watch_silence), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Accept Button
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.size(64.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF43A047))
                    ) {
                        Text(text = "✓", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = callerName.ifEmpty { callerNumber },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                val statusText = if (callState == android.telecom.Call.STATE_DIALING) {
                    stringResource(R.string.watch_calling)
                } else {
                    "" // User requested no call timer, so just blank or caller name
                }
                
                if (statusText.isNotEmpty()) {
                    Text(
                        text = statusText,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(28.dp))
                }

                Button(
                    onClick = onHangup,
                    modifier = Modifier.size(64.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935))
                ) {
                    Text(text = "✕", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Visible volume edges
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.25f).align(Alignment.CenterStart).clickable { onVolumeDown(); lastInteractionTime = System.currentTimeMillis() }) {
                Box(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight(0.4f).width(4.dp).background(Color.White.copy(alpha = 0.2f), shape = CircleShape))
                Text(text = "-", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Center).padding(start = 8.dp))
            }
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.25f).align(Alignment.CenterEnd).clickable { onVolumeUp(); lastInteractionTime = System.currentTimeMillis() }) {
                Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(0.4f).width(4.dp).background(Color.White.copy(alpha = 0.2f), shape = CircleShape))
                Text(text = "+", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Center).padding(end = 8.dp))
            }
            
            // Volume overlay
            if (volumeOverlayVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .background(Color.Black.copy(alpha = 0.85f), CircleShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$volumePercent%",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Visual volume bar
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (volumePercent / 100f).coerceIn(0f, 1f))
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

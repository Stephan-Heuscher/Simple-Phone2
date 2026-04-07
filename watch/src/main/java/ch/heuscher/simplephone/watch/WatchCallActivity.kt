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

    private val endCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finish()
        }
    }

    private var _isAnswered = mutableStateOf(false)

    private val answerCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _isAnswered.value = true
        }
    }

    private val callInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val name = intent?.getStringExtra("CALLER_NAME") ?: return
            _callerName.value = name
            // Re-lookup photo
            updatePhoto(name)
        }
    }

    private val _callerName = mutableStateOf("")
    private val _contactPhoto = mutableStateOf<Bitmap?>(null)
    
    private val _isAmbient = mutableStateOf(false)
    private val _ambientUpdateTrigger = mutableIntStateOf(0)
    private lateinit var ambientController: AmbientModeSupport.AmbientController

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
        
        val initialName = intent.getStringExtra("CALLER_NAME") ?: getString(R.string.watch_call_label)
        _callerName.value = initialName
        val isOutgoing = intent.getBooleanExtra("IS_OUTGOING", false)
        if (isOutgoing) {
            _isAnswered.value = true
        }

        // Register receivers
        registerReceiver(endCallReceiver, IntentFilter("ch.heuscher.simplephone.watch.CALL_ENDED"), RECEIVER_NOT_EXPORTED)
        registerReceiver(answerCallReceiver, IntentFilter("ch.heuscher.simplephone.watch.CALL_ANSWERED"), RECEIVER_NOT_EXPORTED)
        registerReceiver(callInfoReceiver, IntentFilter("ch.heuscher.simplephone.watch.CALL_INFO"), RECEIVER_NOT_EXPORTED)

        // Try to find contact photo
        updatePhoto(initialName)

        setContent {
            MaterialTheme {
                WatchCallScreen(
                    callerName = _callerName.value,
                    contactPhoto = _contactPhoto.value,
                    isAnswered = _isAnswered.value,
                    isAmbient = _isAmbient.value,
                    ambientUpdateTrigger = _ambientUpdateTrigger.intValue,
                    onAccept = {
                        _isAnswered.value = true
                        sendMessageToPhone("/answer_call")
                    },
                    onSilence = {
                        sendMessageToPhone("/silence_ringer")
                    },
                    onReject = {
                        sendMessageToPhone("/reject_call")
                        finish()
                    },
                    onHangup = {
                        sendMessageToPhone("/end_call")
                        finish()
                    }
                )
            }
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
            if (prefs.getBoolean("setting_silence_call_on_touch", false) && !_isAnswered.value) {
                sendMessageToPhone("/silence_ringer")
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(endCallReceiver)
        unregisterReceiver(answerCallReceiver)
        unregisterReceiver(callInfoReceiver)
    }

    private fun sendMessageToPhone(path: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nodeClient = Wearable.getNodeClient(this@WatchCallActivity)
                val nodes = Tasks.await(nodeClient.connectedNodes)
                val messageClient = Wearable.getMessageClient(this@WatchCallActivity)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, path, ByteArray(0))
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
    isAnswered: Boolean, 
    isAmbient: Boolean = false,
    ambientUpdateTrigger: Int = 0,
    onAccept: () -> Unit, 
    onSilence: () -> Unit, 
    onReject: () -> Unit, 
    onHangup: () -> Unit
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

    if (!isAnswered) {
        // Incoming call: split screen - top silence, bottom accept with name
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Top Half: Blue (Silence Ringtone)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1E88E5))
                    .clickable { onSilence() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.watch_silence),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Half: Green (Accept)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF43A047))
                    .clickable { onAccept() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = callerName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.watch_accept),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
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

            // Bottom content: name + status + hangup button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Name
                Text(
                    text = callerName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                // Status
                Text(
                    text = "On call",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Red Hangup Button
                Button(
                    onClick = { onHangup() },
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935))
                ) {
                    Text(
                        text = "✕",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

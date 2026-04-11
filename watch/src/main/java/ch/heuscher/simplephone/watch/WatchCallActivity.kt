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

    private val endCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isCallActive = false
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

    private val audioStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val route = intent?.getIntExtra("AUDIO_ROUTE", 1) ?: 1
            _audioRoute.intValue = route
            
            // Try to bring this activity to front if it was superseded by system dialer
            if (isCallActive && !isDestroyed && !isFinishing) {
                val bringToFrontIntent = Intent(context, WatchCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(bringToFrontIntent)
            }
        }
    }

    private val _callerName = mutableStateOf("")
    private val _contactPhoto = mutableStateOf<Bitmap?>(null)
    private val _audioRoute = mutableIntStateOf(1) // Default to EARPIECE (1)
    
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
        isCallActive = true
        
        val initialName = intent.getStringExtra("CALLER_NAME") ?: getString(R.string.watch_call_label)
        _callerName.value = initialName
        val isOutgoing = intent.getBooleanExtra("IS_OUTGOING", false)
        if (isOutgoing) {
            _isAnswered.value = true
        }
        val initialRoute = intent.getIntExtra("AUDIO_ROUTE", 1)
        _audioRoute.intValue = initialRoute

        // Register receivers
        registerReceiver(endCallReceiver, IntentFilter("ch.heuscher.simplephone.watch.CALL_ENDED"), RECEIVER_NOT_EXPORTED)
        registerReceiver(answerCallReceiver, IntentFilter("ch.heuscher.simplephone.watch.CALL_ANSWERED"), RECEIVER_NOT_EXPORTED)
        registerReceiver(callInfoReceiver, IntentFilter("ch.heuscher.simplephone.watch.CALL_INFO"), RECEIVER_NOT_EXPORTED)
        registerReceiver(audioStateReceiver, IntentFilter("ch.heuscher.simplephone.watch.AUDIO_STATE"), RECEIVER_NOT_EXPORTED)

        // Try to find contact photo
        updatePhoto(initialName)

        setContent {
            MaterialTheme {
                WatchCallScreen(
                    callerName = _callerName.value,
                    contactPhoto = _contactPhoto.value,
                    isAnswered = _isAnswered.value,
                    isAmbient = _isAmbient.value,
                    audioRoute = _audioRoute.intValue,
                    ambientUpdateTrigger = _ambientUpdateTrigger.intValue,
                    onAccept = {
                        _isAnswered.value = true
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
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(bringToFrontRunnable)
    }

    override fun onStop() {
        super.onStop()
        if (isCallActive && !isFinishing) {
            // System dialer likely took over. Re-assert focus after a short delay.
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
        unregisterReceiver(audioStateReceiver)
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
    isAnswered: Boolean, 
    isAmbient: Boolean = false,
    audioRoute: Int = 1,
    ambientUpdateTrigger: Int = 0,
    onAccept: () -> Unit, 
    onSilence: () -> Unit, 
    onReject: () -> Unit, 
    onHangup: () -> Unit,
    onVolumeUp: () -> Unit,
    onVolumeDown: () -> Unit
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

                // Status
                Text(
                    text = "On call",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                // Audio Route Controls removed for accessibility. Volume controls are now on the screen edges.
                Spacer(modifier = Modifier.height(8.dp))

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
            }

            // Left Half: Volume Down (Huge invisible tap target for accessibility)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .align(Alignment.CenterStart)
                    .clickable { onVolumeDown() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "-", 
                    fontSize = 48.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f), 
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            // Right Half: Volume Up (Huge invisible tap target for accessibility)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .align(Alignment.CenterEnd)
                    .clickable { onVolumeUp() },
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "+", 
                    fontSize = 48.sp, 
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f), 
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
    }
}

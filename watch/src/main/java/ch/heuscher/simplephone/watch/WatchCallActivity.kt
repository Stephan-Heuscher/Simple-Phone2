package ch.heuscher.simplephone.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

class WatchCallActivity : ComponentActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
fun WatchCallScreen(callerName: String, contactPhoto: Bitmap?, isAnswered: Boolean, onAccept: () -> Unit, onSilence: () -> Unit, onReject: () -> Unit, onHangup: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!isAnswered) {
            // Top Half: Blue (Silence Ringtone)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1E88E5))
                    .clickable {
                        onSilence()
                    },
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
                    .clickable {
                        onAccept()
                    },
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
        } else {
            // REDESIGNED: Compact friendlier UI for round watch
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Avatar (48dp - compact for round screen)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E88E5)),
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
                        Text(
                            text = callerName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Name
                Text(
                    text = callerName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                // Status
                Text(
                    text = "On call",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Red Hangup Button (48dp)
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

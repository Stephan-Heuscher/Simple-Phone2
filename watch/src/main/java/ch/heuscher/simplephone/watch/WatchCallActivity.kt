package ch.heuscher.simplephone.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val callerName = intent.getStringExtra("CALLER_NAME") ?: "Anruf"

        // Register receivers
        registerReceiver(endCallReceiver, IntentFilter("ch.heuscher.simplephone.watch.CALL_ENDED"), RECEIVER_NOT_EXPORTED)
        registerReceiver(answerCallReceiver, IntentFilter("ch.heuscher.simplephone.watch.CALL_ANSWERED"), RECEIVER_NOT_EXPORTED)

        setContent {
            MaterialTheme {
                WatchCallScreen(
                    callerName = callerName,
                    isAnswered = _isAnswered.value,
                    onAccept = {
                        _isAnswered.value = true
                        sendMessageToPhone("/answer_call")
                    },
                    onReject = {
                        sendMessageToPhone("/reject_call")
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(endCallReceiver)
        unregisterReceiver(answerCallReceiver)
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
fun WatchCallScreen(callerName: String, isAnswered: Boolean, onAccept: () -> Unit, onReject: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top Half (or Full Screen if answered): Red (Reject/End Call)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFE53935))
                .clickable {
                    onReject()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isAnswered) {
                     Text(
                        text = callerName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = "Auflegen",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Half: Green (Accept) - only show if not answered yet
        if (!isAnswered) {
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
                        text = "Annehmen",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

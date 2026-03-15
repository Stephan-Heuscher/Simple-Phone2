package ch.heuscher.simplephone.watch

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SimplePhoneWatchApp(this)
            }
        }
    }
}

@Composable
fun SimplePhoneWatchApp(context: Context) {
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            ActionButton(text = "Sarah", color = Color(0xFF1E88E5)) {
                makeCall(context, "0791234567")
            }
        }
        item {
            ActionButton(text = "Tom", color = Color(0xFF1E88E5)) {
                makeCall(context, "0791234568")
            }
        }
        item {
            ActionButton(text = "Doktor", color = Color(0xFF1E88E5)) {
                makeCall(context, "0441234569")
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


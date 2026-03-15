package ch.heuscher.simplephone.watch

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
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SimplePhoneWatchApp()
            }
        }
    }
}

@Composable
fun SimplePhoneWatchApp() {
    var timeText by remember { mutableStateOf(getCurrentTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timeText = getCurrentTime()
        }
    }

    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = timeText,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp, top = 24.dp)
            )
        }
        item {
            ActionButton(text = "Kontakte", color = Color(0xFF1E88E5)) {
                // TODO: Open contacts
            }
        }
        item {
            ActionButton(text = "Wählen", color = Color(0xFF43A047)) {
                // TODO: Open dialer
            }
        }
        item {
            ActionButton(text = "Notruf", color = Color(0xFFE53935)) {
                // TODO: SOS Action
            }
        }
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

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

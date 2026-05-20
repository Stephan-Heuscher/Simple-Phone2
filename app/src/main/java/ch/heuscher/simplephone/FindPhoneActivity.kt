package ch.heuscher.simplephone

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.heuscher.simplephone.ui.theme.SimplePhoneTheme
import kotlinx.coroutines.delay

class FindPhoneActivity : ComponentActivity() {

    private var currentRingtone: Ringtone? = null
    private var originalVolume: Int = -1
    private var vibrator: Vibrator? = null
    private var isAlarmActive by mutableStateOf(true)

    // Trigger state change to reset the 60s timer
    private var sessionTrigger by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen using same pattern as IncomingCallActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Get Vibrator service
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        startAlarm()

        setContent {
            SimplePhoneTheme(darkThemeOption = 2) { // Dark theme option 2 (forced dark/vibrant)
                FindPhoneScreen(
                    isAlarmActive = isAlarmActive,
                    sessionTrigger = sessionTrigger,
                    onStopClicked = {
                        stopAlarm()
                        finish()
                    },
                    onTimeout = {
                        stopAlarm()
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("FindPhoneActivity", "onNewIntent: Resetting find phone timer and ensuring alarm is playing")
        
        // Reset timer by incrementing session trigger
        sessionTrigger++
        
        // Ensure alarm is active and playing
        if (!isAlarmActive) {
            isAlarmActive = true
            startAlarm()
        }
    }

    private fun startAlarm() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // Max out alarm volume
        try {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (e: Exception) {
            Log.e("FindPhoneActivity", "Failed to max out stream volume", e)
        }

        // Play high-priority alarm sound
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            
            currentRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            currentRingtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            currentRingtone?.play()
        } catch (e: Exception) {
            Log.e("FindPhoneActivity", "Failed to play ringtone", e)
        }

        // Vibrate with a strong pulsing pattern (500ms on, 500ms off)
        try {
            val pattern = longArrayOf(0, 500, 500, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 1)) // 1 repeats from index 1
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 1)
            }
        } catch (e: Exception) {
            Log.e("FindPhoneActivity", "Failed to start vibration", e)
        }
    }

    private fun stopAlarm() {
        isAlarmActive = false
        
        // Stop Ringtone
        try {
            currentRingtone?.stop()
        } catch (e: Exception) {
            Log.e("FindPhoneActivity", "Failed to stop ringtone", e)
        }

        // Cancel vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("FindPhoneActivity", "Failed to cancel vibration", e)
        }

        // Restore original stream volume
        try {
            if (originalVolume != -1) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
            }
        } catch (e: Exception) {
            Log.e("FindPhoneActivity", "Failed to restore stream volume", e)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}

@Composable
fun FindPhoneScreen(
    isAlarmActive: Boolean,
    sessionTrigger: Int,
    onStopClicked: () -> Unit,
    onTimeout: () -> Unit
) {
    // 60 seconds auto-dismiss timer
    LaunchedEffect(sessionTrigger) {
        delay(60000)
        onTimeout()
    }

    // Set up flashing screen color animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulsingBackgroundColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFFFFD0), // Soft yellow-white
        targetValue = Color(0xFFFFD54F),  // Warm bright yellow
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsingBackground"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAlarmActive) pulsingBackgroundColor else Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Pulsing Alarm Icon
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = if (isAlarmActive) Color(0xFFE65100) else Color.Gray,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Big friendly callout text
            Text(
                text = stringResource(R.string.find_phone_here_i_am),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAlarmActive) Color(0xFF3E2723) else Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Big premium "STOP" button
            Button(
                onClick = onStopClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD84315), // Deep vibrant red-orange
                    contentColor = Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(width = 240.dp, height = 88.dp)
            ) {
                Text(
                    text = stringResource(R.string.find_phone_stop),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

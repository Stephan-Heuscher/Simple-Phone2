package com.example.simplephone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.simplephone.data.MockData
import com.example.simplephone.model.AudioOutput
import com.example.simplephone.model.Contact
import com.example.simplephone.ui.components.AppScaffold
import com.example.simplephone.ui.components.Screen
import com.example.simplephone.ui.screens.FavoritesScreen
import com.example.simplephone.ui.screens.InCallScreen
import com.example.simplephone.ui.screens.MainScreen
import com.example.simplephone.ui.screens.PhoneBookScreen
import com.example.simplephone.ui.screens.RecentsScreen
import com.example.simplephone.ui.screens.SettingsScreen
import com.example.simplephone.ui.theme.SimplePhoneTheme

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val CALL_PHONE_PERMISSION_REQUEST = 1001
    }
    
    private var pendingPhoneNumber: String? = null
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimplePhoneTheme {
                val windowSize = calculateWindowSizeClass(this)
                SimplePhoneApp(
                    widthSizeClass = windowSize.widthSizeClass,
                    onMakeCall = { phoneNumber -> initiatePhoneCall(phoneNumber) }
                )
            }
        }
    }
    
    private fun initiatePhoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            == PackageManager.PERMISSION_GRANTED) {
            makePhoneCall(phoneNumber)
        } else {
            pendingPhoneNumber = phoneNumber
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PHONE_PERMISSION_REQUEST
            )
        }
    }
    
    private fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPhoneNumber?.let { makePhoneCall(it) }
            }
            pendingPhoneNumber = null
        }
    }
}

@Composable
fun SimplePhoneApp(
    widthSizeClass: WindowWidthSizeClass,
    onMakeCall: (String) -> Unit
) {
    val navController = rememberNavController()
    var filterHours by remember { mutableStateOf(2) } // State lifted up
    
    // In-call state
    var isInCall by remember { mutableStateOf(false) }
    var currentCallContact by remember { mutableStateOf<Contact?>(null) }
    var currentAudioOutput by remember { mutableStateOf(AudioOutput.EARPIECE) }

    // Determine current screen title for TopBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTitle = when(currentRoute) {
        Screen.Settings.route -> Screen.Settings.title
        Screen.InCall.route -> "Calling..."
        else -> "Simple Phone"
    }

    // Function to handle call initiation
    val handleCall: (String) -> Unit = { phoneNumber ->
        // Find contact by phone number
        val contact = MockData.contacts.find { it.number == phoneNumber }
            ?: Contact(id = "unknown", name = phoneNumber, number = phoneNumber)
        currentCallContact = contact
        isInCall = true
        navController.navigate(Screen.InCall.route)
        onMakeCall(phoneNumber)
    }
    
    // Function to handle hangup
    val handleHangup: () -> Unit = {
        isInCall = false
        currentCallContact = null
        navController.popBackStack()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AppScaffold(navController = navController, currentScreenTitle = currentTitle) {
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    MainScreen(
                        onCallClick = handleCall,
                        onContactClick = { contactId -> 
                            val contact = MockData.getContactById(contactId)
                            contact?.let { handleCall(it.number) }
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        filterHours = filterHours,
                        onFilterChange = { filterHours = it },
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.InCall.route) {
                    currentCallContact?.let { contact ->
                        InCallScreen(
                            contact = contact,
                            callDuration = "00:00",
                            currentAudioOutput = currentAudioOutput,
                            availableAudioOutputs = listOf(
                                AudioOutput.EARPIECE,
                                AudioOutput.SPEAKER,
                                AudioOutput.BLUETOOTH,
                                AudioOutput.HEARING_AID
                            ),
                            onHangup = handleHangup,
                            onAudioOutputChange = { currentAudioOutput = it }
                        )
                    }
                }
            }
        }
    }
}

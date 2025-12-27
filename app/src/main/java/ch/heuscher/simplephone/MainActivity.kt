package ch.heuscher.simplephone

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import ch.heuscher.simplephone.ui.components.pressClickEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.data.MockData
import ch.heuscher.simplephone.data.SettingsRepository
import ch.heuscher.simplephone.model.AudioOutput
import ch.heuscher.simplephone.model.CallLogEntry
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.AppScaffold
import ch.heuscher.simplephone.ui.components.Screen
import ch.heuscher.simplephone.ui.screens.FavoritesScreen
import ch.heuscher.simplephone.ui.screens.InCallScreen
import ch.heuscher.simplephone.ui.screens.MainScreen
import ch.heuscher.simplephone.ui.screens.DialerScreen
import ch.heuscher.simplephone.ui.screens.OnboardingScreen
import ch.heuscher.simplephone.ui.screens.PhoneBookScreen
import ch.heuscher.simplephone.ui.screens.RecentsScreen
import ch.heuscher.simplephone.ui.screens.SettingsScreen
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.HighContrastBlue
import ch.heuscher.simplephone.ui.theme.SimplePhoneTheme
import java.util.Locale

import ch.heuscher.simplephone.widget.FavoritesWidget

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val CALL_PHONE_PERMISSION_REQUEST = 1001
        private const val CONTACTS_PERMISSION_REQUEST = 1002
        private const val REQUEST_CODE_SET_DEFAULT_DIALER = 1003
    }
    
    private var pendingPhoneNumber: String? = null
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var contactRepository: ContactRepository
    private var textToSpeech: TextToSpeech? = null
    
    private lateinit var powerManager: android.os.PowerManager
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        


        
        settingsRepository = SettingsRepository(this)
        contactRepository = ContactRepository(this)
        
        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
        
        // Request permissions at startup
        requestPermissionsIfNeeded()
        
        setContent {
            val darkModeOption = remember { mutableStateOf(settingsRepository.darkModeOption) }
            var isDefaultDialer by remember { mutableStateOf(checkIsDefaultDialer()) }
            var showOnboarding by remember { mutableStateOf(!settingsRepository.onboardingCompleted) }
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        isDefaultDialer = checkIsDefaultDialer()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            SimplePhoneTheme(darkThemeOption = darkModeOption.value) {
                if (showOnboarding) {
                    OnboardingScreen(
                        onComplete = {
                            settingsRepository.onboardingCompleted = true
                            showOnboarding = false
                        },
                        useHapticFeedback = settingsRepository.useHapticFeedback
                    )
                } else {
                    val windowSize = calculateWindowSizeClass(this)
                    SimplePhoneApp(
                        widthSizeClass = windowSize.widthSizeClass,
                        onMakeCall = { phoneNumber, contactName -> 
                            if (settingsRepository.useVoiceAnnouncements) {
                                textToSpeech?.speak("Calling $contactName", TextToSpeech.QUEUE_FLUSH, null, null)
                            }
                            initiatePhoneCall(phoneNumber) 
                        },
                        settingsRepository = settingsRepository,
                        contactRepository = contactRepository,
                        onDarkModeOptionChange = { darkModeOption.value = it },
                        isDefaultDialer = isDefaultDialer,
                        onSetDefaultDialer = { requestDefaultDialerRole() }
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Prompt user to set this app as default phone app if not already
        // We don't do this automatically on resume anymore to avoid annoying the user
        // offerReplacingDefaultDialer()
        
        // Refresh widget in case contacts or permissions changed
        FavoritesWidget.sendRefreshBroadcast(this)
        
        // Release wake lock when returning to app (call ended or minimized)

    }
    
    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
    
    private fun requestDefaultDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER) &&
                !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_DIALER)) {
                val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
            } else {
                // Already default or role not available
                android.widget.Toast.makeText(this, "Already default dialer or not available", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            offerReplacingDefaultDialer()
        }
    }
    
    private fun requestPermissionsIfNeeded() {
        val permissionsNeeded = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CALL_LOG)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
        }
        
        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                CONTACTS_PERMISSION_REQUEST
            )
        }
    }
    
    private fun offerReplacingDefaultDialer() {
        if (!checkIsDefaultDialer()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    android.util.Log.d("MainActivity", "Requesting to be default dialer")
                    val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                        putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                    }
                    @Suppress("DEPRECATION")
                    startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Failed to show default dialer prompt", e)
                }
            }
        } else {
            android.util.Log.d("MainActivity", "Already default dialer")
            android.widget.Toast.makeText(this, "Already default dialer", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkIsDefaultDialer(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val telecomManager = getSystemService(android.telecom.TelecomManager::class.java)
            return telecomManager?.defaultDialerPackage == packageName
        }
        return false
    }
    
    private fun initiatePhoneCall(phoneNumber: String) {
        android.util.Log.d("MainActivity", "Initiating phone call to $phoneNumber")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            == PackageManager.PERMISSION_GRANTED) {
            makePhoneCall(phoneNumber)
        } else {
            android.util.Log.d("MainActivity", "Requesting CALL_PHONE permission")
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

        // Cancel missed call notification if exists
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val notificationId = phoneNumber.replace(Regex("[^0-9]"), "").hashCode()
        notificationManager.cancel(notificationId)
        

        
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
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                android.widget.Toast.makeText(this, "App is now the default dialer!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(this, "Default dialer request declined.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun SimplePhoneApp(
    widthSizeClass: WindowWidthSizeClass,
    onMakeCall: (String, String) -> Unit,
    settingsRepository: SettingsRepository,
    contactRepository: ContactRepository,
    onDarkModeOptionChange: (Int) -> Unit = {},
    isDefaultDialer: Boolean = false,
    onSetDefaultDialer: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    
    // Settings state
    var displayMode by remember { mutableStateOf(settingsRepository.displayMode) }
    // Derived display values for existing components
    val useHugeText = displayMode == 1
    val useHugeContactPicture = displayMode == 2
    val useGridContactImages = displayMode == 3
    var missedCallsHours by remember { mutableStateOf(settingsRepository.missedCallsHours) }
    var darkModeOption by remember { mutableStateOf(settingsRepository.darkModeOption) }
    var confirmBeforeCall by remember { mutableStateOf(settingsRepository.confirmBeforeCall) }
    var useHapticFeedback by remember { mutableStateOf(settingsRepository.useHapticFeedback) }
    var useVoiceAnnouncements by remember { mutableStateOf(settingsRepository.useVoiceAnnouncements) }
    var isDemoMode by remember { mutableStateOf(settingsRepository.isDemoMode) }

    var answerOnSpeakerIfFlat by remember { mutableStateOf(settingsRepository.answerOnSpeakerIfFlat) }

    // Triple tap logic
    var titleTapCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastTitleTapTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    fun onTitleClick() {
        val now = System.currentTimeMillis()
        if (now - lastTitleTapTime < 500) {
            titleTapCount++
        } else {
            titleTapCount = 1
        }
        lastTitleTapTime = now

        if (titleTapCount >= 3) {
            isDemoMode = !isDemoMode
            settingsRepository.isDemoMode = isDemoMode
            titleTapCount = 0
            android.widget.Toast.makeText(context, if (isDemoMode) "Demo Mode Enabled" else "Demo Mode Disabled", android.widget.Toast.LENGTH_SHORT).show()
            // Refresh widget
            FavoritesWidget.sendRefreshBroadcast(context)
        }
    }
    
    // Permission denied messages
    var showPermissionMessage by remember { mutableStateOf(false) }
    var permissionMessageText by remember { mutableStateOf("") }
    
    // Call confirmation dialog state
    var showCallConfirmDialog by remember { mutableStateOf(false) }
    var pendingCallContact by remember { mutableStateOf<Contact?>(null) }
    
    // Contacts and call logs from device
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var missedCalls by remember { mutableStateOf<List<CallLogEntry>>(emptyList()) }
    
    // Load contacts and call logs
    fun loadData() {
        val loadedContacts = if (isDemoMode) MockData.demoContacts else contactRepository.getContacts()
        
        // Apply saved favorites order
        val savedOrder = settingsRepository.getFavoritesOrder()
        val orderedContacts = if (savedOrder.isNotEmpty()) {
            loadedContacts.map { contact ->
                val savedIndex = savedOrder.indexOf(contact.id)
                if (savedIndex >= 0) contact.copy(sortOrder = savedIndex) else contact.copy(sortOrder = Int.MAX_VALUE)
            }.sortedWith(compareBy({ !it.isFavorite }, { it.sortOrder }, { it.name }))
        } else {
            loadedContacts
        }
        
        contacts = orderedContacts
        if (loadedContacts.isEmpty() && !isDemoMode) {
            showPermissionMessage = true
            permissionMessageText = "Please grant contacts permission to see your contacts"
        }
        missedCalls = contactRepository.getMissedCallsInLastHours(missedCallsHours)
    }

    LaunchedEffect(Unit) {
        loadData()
        
        // Register content observer for contact changes
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                loadData()
            }
        }
        context.contentResolver.registerContentObserver(
            android.provider.ContactsContract.Contacts.CONTENT_URI, 
            true, 
            observer
        )
        
        // Also listen for call log changes
        context.contentResolver.registerContentObserver(
            android.provider.CallLog.Calls.CONTENT_URI,
            true,
            observer
        )
    }

    LaunchedEffect(isDemoMode) {
        loadData()
    }
    
    // Refresh missed calls when hours change
    LaunchedEffect(missedCallsHours) {
        missedCalls = contactRepository.getMissedCallsInLastHours(missedCallsHours)
    }
    
    // In-call state
    var isInCall by remember { mutableStateOf(false) }
    var currentCallContact by remember { mutableStateOf<Contact?>(null) }
    var currentAudioOutput by remember { mutableStateOf(AudioOutput.EARPIECE) }

    // Determine current screen title for TopBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTitle = when(currentRoute) {
        Screen.Settings.route -> Screen.Settings.title
        Screen.Dialer.route -> Screen.Dialer.title
        Screen.InCall.route -> "Calling..."
        else -> "Simple Phone"
    }

    // Function to actually make the call
    val executeCall: (Contact) -> Unit = { contact ->
        if (useHapticFeedback) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        // Just make the call directly - system dialer will handle the UI
        onMakeCall(contact.number, contact.name)
    }

    // Function to handle call initiation (with optional confirmation)
    val handleCall: (String) -> Unit = { phoneNumber ->
        val contact = contacts.find { it.number == phoneNumber }
            ?: Contact(id = "unknown", name = phoneNumber, number = phoneNumber)
        
        if (useHapticFeedback) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        
        if (confirmBeforeCall) {
            pendingCallContact = contact
            showCallConfirmDialog = true
        } else {
            executeCall(contact)
        }
    }
    
    // Function to handle hangup
    val handleHangup: () -> Unit = {
        isInCall = false
        currentCallContact = null
        navController.popBackStack()
    }
    
    // Call confirmation dialog
    if (showCallConfirmDialog && pendingCallContact != null) {
        CallConfirmationDialog(
            contactName = pendingCallContact!!.name,
            onConfirm = {
                showCallConfirmDialog = false
                pendingCallContact?.let { executeCall(it) }
                pendingCallContact = null
            },
            onDismiss = {
                showCallConfirmDialog = false
                pendingCallContact = null
            }
        )
    }
    
    // Permission message dialog
    if (showPermissionMessage) {
        PermissionMessageDialog(
            message = permissionMessageText,
            onDismiss = { showPermissionMessage = false }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AppScaffold(
            navController = navController, 
            currentScreenTitle = currentTitle,
            onTitleClick = { if (currentTitle == "Simple Phone") onTitleClick() }
        ) {
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) {
                    MainScreen(
                        onCallClick = handleCall,
                        onContactClick = { contactId -> 
                            val contact = contacts.find { it.id == contactId }
                            contact?.let { handleCall(it.number) }
                        },
                        onCallLogClick = { navController.navigate(Screen.CallLog.route) },
                        onDialerClick = { navController.navigate(Screen.Dialer.route) },
                        onEditClick = { contactId, number ->
                            val route = "contact_edit?contactId=${contactId ?: ""}&number=${number ?: ""}"
                            navController.navigate(route)
                        },
                        missedCalls = missedCalls,
                        missedCallsHours = missedCallsHours,
                        useHugeText = useHugeText,
                        useHugeContactPicture = useHugeContactPicture,
                        useGridContactImages = useGridContactImages,
                        contacts = contacts,
                        isDefaultDialer = isDefaultDialer,
                        onSetDefaultDialer = onSetDefaultDialer,
                        useHapticFeedback = useHapticFeedback
                    )
                }
                composable(Screen.Dialer.route) {
                    DialerScreen(
                        onCallClick = handleCall,
                        useHapticFeedback = useHapticFeedback
                    )
                }
                composable(Screen.CallLog.route) {
                    ch.heuscher.simplephone.ui.screens.CallLogScreen(
                        onCallClick = handleCall,
                        onBackClick = { navController.popBackStack() },
                        onEditContactClick = { contactId, number ->
                            val route = "contact_edit?contactId=${contactId ?: ""}&number=$number"
                            navController.navigate(route)
                        },
                        callLogRepository = ch.heuscher.simplephone.data.CallLogRepository(context),
                        contacts = contacts,
                        useHugeText = useHugeText,
                        useHapticFeedback = useHapticFeedback
                    )
                }
                composable(
                    route = Screen.ContactEdit.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("contactId") {
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                        },
                        androidx.navigation.navArgument("number") {
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                        }
                    )
                ) { backStackEntry ->
                    val contactId = backStackEntry.arguments?.getString("contactId")
                    val number = backStackEntry.arguments?.getString("number")
                    val contact = contacts.find { it.id == contactId }
                    
                    ch.heuscher.simplephone.ui.screens.ContactEditScreen(
                        contact = contact,
                        initialNumber = number,
                        useHugeText = useHugeText,
                        onSave = {
                            loadData() // Refresh data
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        displayMode = displayMode,
                        onDisplayModeChange = {
                            displayMode = it
                            settingsRepository.displayMode = it
                        },
                        missedCallsHours = missedCallsHours,
                        onMissedCallsHoursChange = {
                            missedCallsHours = it
                            settingsRepository.missedCallsHours = it
                        },
                        darkModeOption = darkModeOption,
                        onDarkModeOptionChange = {
                            darkModeOption = it
                            settingsRepository.darkModeOption = it
                            onDarkModeOptionChange(it)
                        },
                        blockUnknownCallers = settingsRepository.blockUnknownCallers,
                        onBlockUnknownCallersChange = {
                            settingsRepository.blockUnknownCallers = it
                        },
                        answerOnSpeakerIfFlat = answerOnSpeakerIfFlat,
                        onAnswerOnSpeakerIfFlatChange = {
                             answerOnSpeakerIfFlat = it
                             settingsRepository.answerOnSpeakerIfFlat = it
                        },
                        confirmBeforeCall = confirmBeforeCall,
                        onConfirmBeforeCallChange = {
                            confirmBeforeCall = it
                            settingsRepository.confirmBeforeCall = it
                        },
                        useHapticFeedback = useHapticFeedback,
                        onHapticFeedbackChange = {
                            useHapticFeedback = it
                            settingsRepository.useHapticFeedback = it
                        },
                        useVoiceAnnouncements = useVoiceAnnouncements,
                        onVoiceAnnouncementsChange = {
                            useVoiceAnnouncements = it
                            settingsRepository.useVoiceAnnouncements = it
                        },
                        favorites = contacts.filter { it.isFavorite },
                        onFavoritesReorder = { newOrder ->
                            // Save the order to preferences
                            settingsRepository.saveFavoritesOrder(newOrder.map { it.id })
                            // Update the contacts list with new favorites order
                            contacts = contacts.map { contact ->
                                val newIndex = newOrder.indexOfFirst { it.id == contact.id }
                                if (newIndex >= 0) contact.copy(sortOrder = newIndex) else contact
                            }.sortedWith(compareBy({ !it.isFavorite }, { it.sortOrder }))
                            
                            // Update widget
                            FavoritesWidget.sendRefreshBroadcast(context)
                        },
                        isDefaultDialer = isDefaultDialer,
                        onSetDefaultDialer = onSetDefaultDialer,
                        onBackClick = { navController.popBackStack() }
                    )
                }
                composable(Screen.InCall.route) {
                    currentCallContact?.let { contact ->
                        // Detect available audio outputs - refresh on each recomposition
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val availableOutputs = remember(currentCallContact) {
                            mutableListOf<AudioOutput>().apply {
                                add(AudioOutput.EARPIECE) // Always available
                                add(AudioOutput.SPEAKER) // Always available
                                
                                // Check for Bluetooth devices
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                                    if (devices.any { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || 
                                                       it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }) {
                                        add(AudioOutput.BLUETOOTH)
                                    }
                                    if (devices.any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                                                       it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }) {
                                        add(AudioOutput.WIRED_HEADSET)
                                    }
                                    if (devices.any { it.type == AudioDeviceInfo.TYPE_HEARING_AID }) {
                                        add(AudioOutput.HEARING_AID)
                                    }
                                }
                            }
                        }
                        
                        // Always show at least EARPIECE and SPEAKER
                        val outputsToShow = if (availableOutputs.size >= 2) availableOutputs else {
                            mutableListOf(AudioOutput.EARPIECE, AudioOutput.SPEAKER)
                        }
                        
                        InCallScreen(
                            contact = contact,
                            currentAudioOutput = currentAudioOutput,
                            availableAudioOutputs = outputsToShow,
                            onHangup = handleHangup,
                            onAudioOutputChange = { output ->
                                currentAudioOutput = output
                                // Apply audio routing
                                when (output) {
                                    AudioOutput.SPEAKER -> {
                                        audioManager.mode = AudioManager.MODE_IN_CALL
                                        @Suppress("DEPRECATION")
                                        audioManager.isSpeakerphoneOn = true
                                        @Suppress("DEPRECATION")
                                        audioManager.stopBluetoothSco()
                                        audioManager.isBluetoothScoOn = false
                                    }
                                    AudioOutput.BLUETOOTH -> {
                                        audioManager.mode = AudioManager.MODE_IN_CALL
                                        @Suppress("DEPRECATION")
                                        audioManager.isSpeakerphoneOn = false
                                        @Suppress("DEPRECATION")
                                        audioManager.startBluetoothSco()
                                        audioManager.isBluetoothScoOn = true
                                    }
                                    else -> { // Earpiece, Wired Headset, Hearing Aid
                                        audioManager.mode = AudioManager.MODE_IN_CALL
                                        @Suppress("DEPRECATION")
                                        audioManager.isSpeakerphoneOn = false
                                        @Suppress("DEPRECATION")
                                        audioManager.stopBluetoothSco()
                                        audioManager.isBluetoothScoOn = false
                                    }
                                }
                            },
                            onDtmfClick = { digit ->
                                ch.heuscher.simplephone.call.CallService.sendDtmf(digit)
                            },
                            useHapticFeedback = useHapticFeedback
                        )
                    }
                }
            }
        }
    }
}

/**
 * Large, accessible call confirmation dialog
 */
@Composable
fun CallConfirmationDialog(
    contactName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Call $contactName?",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // No button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(HighContrastBlue)
                            .pressClickEffect(
                                onClick = onDismiss,
                                onPressedChange = {} // We don't track press for color change here? But wait, background is static HighContrastBlue
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    // Yes button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GreenCall)
                            .pressClickEffect(
                                onClick = onConfirm,
                                onPressedChange = {}
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Yes",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Permission message dialog for friendly error messages
 */
@Composable
fun PermissionMessageDialog(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Permission Needed",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HighContrastBlue),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text(
                            "OK",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}

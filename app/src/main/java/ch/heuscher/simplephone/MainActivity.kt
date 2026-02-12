package ch.heuscher.simplephone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.app.ActivityCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.heuscher.simplephone.data.AppSettings
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.data.SettingsRepository
import ch.heuscher.simplephone.model.AudioOutput
import ch.heuscher.simplephone.model.Contact
import ch.heuscher.simplephone.ui.components.AppScaffold
import ch.heuscher.simplephone.ui.components.Screen
import ch.heuscher.simplephone.ui.screens.MainScreen
import ch.heuscher.simplephone.ui.screens.DialerScreen
import ch.heuscher.simplephone.ui.screens.OnboardingScreen
import ch.heuscher.simplephone.ui.screens.SettingsScreen
import ch.heuscher.simplephone.ui.theme.SimplePhoneTheme
import ch.heuscher.simplephone.ui.MainViewModel
import java.util.Locale
import ch.heuscher.simplephone.widget.FavoritesWidget
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import ch.heuscher.simplephone.ui.components.pressClickEffect
import ch.heuscher.simplephone.ui.theme.GreenCall
import ch.heuscher.simplephone.ui.theme.HighContrastBlue

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
        contactRepository = ContactRepository(this) // Kept for Activity usage if any, but VM handles data
        
        // Trigger initial sync to ensure device is registered in Firestore (Gentle Phone)
        lifecycleScope.launch {
            settingsRepository.syncRemoteSettings()
        }
        
        // Initialize View Model
        val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        // Handle incoming intent (e.g. from tel: links)
        handleIntent(intent)

        // Initialize Text-to-Speech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
        
        // Request permissions at startup ONLY if onboarding is completed
        // We check the pure property here, but inside composable we'll observe flow
        if (settingsRepository.onboardingCompleted) {
            requestPermissionsIfNeeded()
        }
        
        // Notify VM about permissions (optimistic or check)
        val hasContactsPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissionsState(hasContactsPerm)
        
        setContent {
            // Observe settings flow for reactive UI updates
            val settings by settingsRepository.settings.collectAsState()
            
            var isDefaultDialer by remember { mutableStateOf(checkIsDefaultDialer()) }
            val showOnboarding = !settings.onboardingCompleted
            
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        isDefaultDialer = checkIsDefaultDialer()
                        // Refresh logic if needed
                        viewModel.refresh()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            SimplePhoneTheme(darkThemeOption = settings.darkModeOption) {
                if (showOnboarding) {
                    OnboardingScreen(
                        onComplete = {
                            settingsRepository.onboardingCompleted = true
                            // showOnboarding updates automatically via flow
                            requestDefaultDialerRole()
                            // Permissions requested via chain in requestDefaultDialerRole/onActivityResult
                        },
                        useHapticFeedback = settings.useHapticFeedback
                    )
                } else {
                    // Observe configuration changes to trigger recomposition
                    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                    // Calculate window size class based on current configuration/activity state
                    val screenWidthDp = configuration.screenWidthDp
                    val screenHeightDp = configuration.screenHeightDp
                    
                    val dim1 = if (screenWidthDp > 0) screenWidthDp.toFloat() else 1f
                    val dim2 = if (screenHeightDp > 0) screenHeightDp.toFloat() else 1f
                    val longDim = kotlin.math.max(dim1, dim2)
                    val shortDim = kotlin.math.min(dim1, dim2)
                    
                    val elongationRatio = longDim / shortDim
                    
                    val widthSizeClass = when {
                        elongationRatio < 2.0f -> androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded
                        else -> androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact
                    }
                    
                    // == ZOOM FACTOR LOGIC ==
                    var currentZoomFactor by remember(widthSizeClass) { 
                        mutableStateOf(settingsRepository.getZoomFactor(widthSizeClass)) 
                    }
                    
                    val currentDensity = LocalDensity.current
                    val customDensity = remember(currentDensity, currentZoomFactor) {
                        androidx.compose.ui.unit.Density(
                            density = currentDensity.density * currentZoomFactor,
                            fontScale = currentDensity.fontScale * currentZoomFactor
                        )
                    }
                    
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalDensity provides customDensity
                    ) {
                        SimplePhoneApp(
                            viewModel = viewModel,
                            widthSizeClass = widthSizeClass,
                            onOpenContact = { id -> openNativeContactApp(id) },
                            onAddContact = { number -> openNativeContactEditor(number) },
                            onMakeCall = { phoneNumber, contactName -> 
                                if (settings.useVoiceAnnouncements) {
                                    textToSpeech?.speak(getString(R.string.calling_announcement, contactName), TextToSpeech.QUEUE_FLUSH, null, null)
                                }
                                initiatePhoneCall(phoneNumber) 
                            },
                            settingsRepository = settingsRepository,
                            settings = settings,
                            isDefaultDialer = isDefaultDialer,
                            onSetDefaultDialer = { requestDefaultDialerRole() },
                            currentZoomFactor = currentZoomFactor,
                            onZoomChange = { newZoom -> 
                                currentZoomFactor = newZoom
                                settingsRepository.setZoomFactor(widthSizeClass, newZoom)
                            },
                            onShowOnboarding = { settingsRepository.onboardingCompleted = false }
                        )
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Prompt user to set this app as default phone app if not already, but ONLY if onboarding is finished
        if (settingsRepository.onboardingCompleted) {
             requestDefaultDialerRole()
        }
        
        // Refresh widget in case contacts or permissions changed
        FavoritesWidget.sendRefreshBroadcast(this)
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
                requestPermissionsIfNeeded()
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
            requestPermissionsIfNeeded()
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
    
    private fun openNativeContactApp(contactId: String) {
        try {
            val id = contactId.toLongOrNull()
            if (id != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
                intent.data = uri
                startActivity(intent)
            } else {
                 android.widget.Toast.makeText(this, getString(R.string.toast_open_contact_error), android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error opening contact", e)
            android.widget.Toast.makeText(this, getString(R.string.toast_open_contact_exception), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNativeContactEditor(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber)
            }
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error opening contact editor", e)
            android.widget.Toast.makeText(this, getString(R.string.toast_open_contact_exception), android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun makePhoneCall(phoneNumber: String) {
        val normalizedNumber = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.normalize(phoneNumber)
        
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.fromParts("tel", normalizedNumber, null)
        }

        // Cancel missed call notification if exists
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val notificationId = normalizedNumber.hashCode() 
        notificationManager.cancel(notificationId)
        
        startActivity(intent)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CALL_PHONE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPhoneNumber?.let { makePhoneCall(it) }
                val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
                viewModel.updatePermissionsState(true)
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
                android.widget.Toast.makeText(this, getString(R.string.toast_default_dialer_set), android.widget.Toast.LENGTH_SHORT).show()
                val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
                val hasContactsPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                viewModel.updatePermissionsState(hasContactsPerm)
            } else {
                android.widget.Toast.makeText(this, getString(R.string.toast_default_dialer_declined), android.widget.Toast.LENGTH_SHORT).show()
            }
            requestPermissionsIfNeeded()
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_DIAL) {
            if (intent.data?.scheme == "tel") {
                val number = intent.data?.schemeSpecificPart
                val viewModel = ViewModelProvider(this)[MainViewModel::class.java]
                viewModel.setPendingDialerNumber(number)
            }
        }
    }
}

@Composable
fun SimplePhoneApp(
    viewModel: MainViewModel,
    widthSizeClass: WindowWidthSizeClass,
    onOpenContact: (String) -> Unit,
    onAddContact: (String) -> Unit,
    onMakeCall: (String, String) -> Unit,
    settingsRepository: SettingsRepository,
    settings: AppSettings,
    isDefaultDialer: Boolean = false,
    onSetDefaultDialer: () -> Unit = {},
    currentZoomFactor: Float = 1.0f,
    onZoomChange: (Float) -> Unit = {},
    onShowOnboarding: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    
    // Derived display values
    val useHugeText = settings.displayMode == SettingsRepository.DISPLAY_MODE_LARGE_TEXT
    val useHugeContactPicture = settings.displayMode == SettingsRepository.DISPLAY_MODE_BIG_PHOTOS
    val useGridContactImages = settings.displayMode == SettingsRepository.DISPLAY_MODE_GRID

    // Triple tap logic
    var titleTapCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastTitleTapTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    // Demo mode confirmation dialog state
    var showDemoModeDialog by remember { mutableStateOf(false) }

    fun onTitleClick() {
        val now = System.currentTimeMillis()
        if (now - lastTitleTapTime < 500) {
            titleTapCount++
        } else {
            titleTapCount = 1
        }
        lastTitleTapTime = now

        if (titleTapCount >= 3) {
            titleTapCount = 0
            if (!settings.isDemoMode) {
                showDemoModeDialog = true
            } else {
                settingsRepository.isDemoMode = false
                viewModel.refresh()
                android.widget.Toast.makeText(context, context.getString(R.string.demo_mode_disabled), android.widget.Toast.LENGTH_SHORT).show()
                FavoritesWidget.sendRefreshBroadcast(context)
            }
        }
    }
    
    // Permission denied messages
    var showPermissionMessage by remember { mutableStateOf(false) }
    var permissionMessageText by remember { mutableStateOf("") }
    
    // Call confirmation dialog state
    var showCallConfirmDialog by remember { mutableStateOf(false) }
    var pendingCallContact by remember { mutableStateOf<Contact?>(null) }
    
    val contacts by viewModel.contacts.collectAsState()
    val missedCalls by viewModel.missedCalls.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val contactResolutionMaps by viewModel.contactResolutionMaps.collectAsState()
    
    // Refresh missed calls when hours change
    LaunchedEffect(settings.missedCallsHours) {
        viewModel.refresh()
    }
    
    val pendingDialerNumber by viewModel.pendingDialerNumber.collectAsState()

    // Navigate to dialer when a number is pending (from intent)
    LaunchedEffect(pendingDialerNumber) {
        if (pendingDialerNumber != null) {
            navController.navigate(Screen.Dialer.route) {
                launchSingleTop = true
            }
        }
    }
    
    // In-call state
    var isInCall by remember { mutableStateOf(false) }
    var currentCallContact by remember { mutableStateOf<Contact?>(null) }
    var currentAudioOutput by remember { mutableStateOf(AudioOutput.EARPIECE) }

    // Determine current screen title for TopBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTitle = when(currentRoute) {
        Screen.Settings.route -> stringResource(Screen.Settings.titleRes)
        Screen.Dialer.route -> stringResource(Screen.Dialer.titleRes)
        Screen.InCall.route -> stringResource(R.string.calling_title)
        else -> stringResource(R.string.app_name)
    }

    // Function to actually make the call
    val executeCall: (Contact) -> Unit = { contact ->
        if (settings.useHapticFeedback) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        onMakeCall(contact.number, contact.name)
    }

    // Function to handle call initiation (with optional confirmation)
    val handleCall: (String) -> Unit = { phoneNumber ->
        // Use helper to find contact robustly
        val contact = contacts.find { 
             ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.areNumbersSame(it.number, phoneNumber, context)
        } ?: Contact(id = "unknown", name = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.format(phoneNumber), number = phoneNumber)
        
        if (settings.useHapticFeedback) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        
        if (settings.confirmBeforeCall) {
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

    // Demo Mode Confirmation Dialog
    if (showDemoModeDialog) {
        var secondsRemaining by remember { androidx.compose.runtime.mutableIntStateOf(15) }
        
        LaunchedEffect(Unit) {
            while (secondsRemaining > 0) {
                kotlinx.coroutines.delay(1000)
                secondsRemaining--
            }
            showDemoModeDialog = false
        }
        
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDemoModeDialog = false },
            title = { Text(stringResource(R.string.demo_mode_dialog_title)) },
            text = { Text(stringResource(R.string.demo_mode_dialog_message, secondsRemaining)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        settingsRepository.isDemoMode = true
                        viewModel.refresh()
                        showDemoModeDialog = false
                        android.widget.Toast.makeText(context, context.getString(R.string.demo_mode_enabled), android.widget.Toast.LENGTH_SHORT).show()
                        FavoritesWidget.sendRefreshBroadcast(context)
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDemoModeDialog = false }
                ) {
                    Text(stringResource(R.string.no))
                }
            }
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
                        onOpenContact = onOpenContact,
                        onContactClick = { contactId -> 
                            val contact = contacts.find { it.id == contactId }
                            contact?.let { handleCall(it.number) }
                        },
                        onCallLogClick = { navController.navigate(Screen.CallLog.route) },
                        onDialerClick = { navController.navigate(Screen.Dialer.route) },
                        missedCalls = missedCalls,
                        missedCallsHours = settings.missedCallsHours,
                        useHugeText = useHugeText,
                        useHugeContactPicture = useHugeContactPicture,
                        useGridContactImages = useGridContactImages,
                        contacts = contacts,
                        isDefaultDialer = isDefaultDialer,
                        onSetDefaultDialer = onSetDefaultDialer,
                        useHapticFeedback = settings.useHapticFeedback,
                        contactResolutionMaps = contactResolutionMaps
                    )
                }
                composable(Screen.Dialer.route) {
                    DialerScreen(
                        onCallClick = handleCall,
                        useHapticFeedback = settings.useHapticFeedback,
                        initialNumber = pendingDialerNumber,
                        onInitialNumberConsumed = { viewModel.consumePendingDialerNumber() }
                    )
                }
                composable(Screen.CallLog.route) {
                    ch.heuscher.simplephone.ui.screens.CallLogScreen(
                        onCallClick = handleCall,
                        onBackClick = { navController.popBackStack() },
                        onOpenContact = onOpenContact,
                        onAddContact = onAddContact,
                        callLogs = recents,
                        contacts = contacts,
                        useHugeText = useHugeText,
                        useHapticFeedback = settings.useHapticFeedback,
                        contactResolutionMaps = contactResolutionMaps
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        displayMode = settings.displayMode,
                        onDisplayModeChange = { settingsRepository.displayMode = it },
                        missedCallsHours = settings.missedCallsHours,
                        onMissedCallsHoursChange = { settingsRepository.missedCallsHours = it },
                        darkModeOption = settings.darkModeOption,
                        onDarkModeOptionChange = { settingsRepository.darkModeOption = it },
                        blockUnknownCallers = settings.blockUnknownCallers,
                        onBlockUnknownCallersChange = { settingsRepository.blockUnknownCallers = it },
                        lastBlockedNumber = settings.lastBlockedNumber,

                        confirmBeforeCall = settings.confirmBeforeCall,
                        onConfirmBeforeCallChange = { settingsRepository.confirmBeforeCall = it },
                        useHapticFeedback = settings.useHapticFeedback,
                        onHapticFeedbackChange = { settingsRepository.useHapticFeedback = it },
                        useVoiceAnnouncements = settings.useVoiceAnnouncements,
                        onVoiceAnnouncementsChange = { settingsRepository.useVoiceAnnouncements = it },
                        favorites = contacts.filter { it.isFavorite },
                        onFavoritesReorder = { newOrder ->
                            viewModel.onFavoritesReorder(newOrder)
                            FavoritesWidget.sendRefreshBroadcast(context)
                        },
                        isDefaultDialer = isDefaultDialer,
                        onSetDefaultDialer = onSetDefaultDialer,
                        onBackClick = { navController.popBackStack() },
                        currentZoomFactor = currentZoomFactor,
                        onZoomChange = onZoomChange,
                        currentWidthSizeClass = widthSizeClass,
                        onShowOnboarding = onShowOnboarding,
                        simplifiedContactCallScreen = settings.simplifiedContactCallScreen,
                        onSimplifiedContactCallScreenChange = { settingsRepository.simplifiedContactCallScreen = it },
                        silenceCallOnTouch = settings.silenceCallOnTouch,
                        onSilenceCallOnTouchChange = { settingsRepository.silenceCallOnTouch = it },
                        ringtoneSilenceTimeout = settings.ringtoneSilenceTimeout,
                        onRingtoneSilenceTimeoutChange = { settingsRepository.ringtoneSilenceTimeout = it },
                        // Gentle Phone Specific
                        pairingCode = settingsRepository.getPairingCode(),
                        showPairingCode = settingsRepository.isRemoteSettingsEnabled()
                    )
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
                    text = stringResource(R.string.call_confirmation_title, contactName),
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
                                onPressedChange = {}
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.no),
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
                            stringResource(R.string.yes),
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
                    text = stringResource(R.string.permission_needed_title),
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
                            stringResource(R.string.ok_button),
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

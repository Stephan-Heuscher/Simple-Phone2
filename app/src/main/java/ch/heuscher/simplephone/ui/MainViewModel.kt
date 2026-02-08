package ch.heuscher.simplephone.ui

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.heuscher.simplephone.data.ContactRepository
import ch.heuscher.simplephone.data.SettingsRepository
import ch.heuscher.simplephone.model.CallLogEntry
import ch.heuscher.simplephone.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val contactRepository = ContactRepository(application)
    private val settingsRepository = SettingsRepository(application)

    data class ContactResolutionMaps(
        val normalized: Map<String, Contact>,
        val suffixes: Map<String, Contact>
    )

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _contactResolutionMaps = MutableStateFlow(ContactResolutionMaps(emptyMap(), emptyMap()))
    val contactResolutionMaps: StateFlow<ContactResolutionMaps> = _contactResolutionMaps.asStateFlow()

    private val _missedCalls = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val missedCalls: StateFlow<List<CallLogEntry>> = _missedCalls.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recents = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val recents: StateFlow<List<CallLogEntry>> = _recents.asStateFlow()
    
    // Permission state tracked in VM to avoid loading data before permitted
    private val _hasPermissions = MutableStateFlow(false)

    private val _pendingDialerNumber = MutableStateFlow<String?>(null)
    val pendingDialerNumber: StateFlow<String?> = _pendingDialerNumber.asStateFlow()

    fun setPendingDialerNumber(number: String?) {
        _pendingDialerNumber.value = number
    }

    fun consumePendingDialerNumber() {
        _pendingDialerNumber.value = null
    }

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            loadData()
        }
    }

    private var isObserverRegistered = false

    init {
        // Try to register immediately
        registerObservers()
    }

    private fun registerObservers() {
        if (isObserverRegistered) return
        
        try {
            getApplication<Application>().contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                contentObserver
            )
            getApplication<Application>().contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                contentObserver
            )
            isObserverRegistered = true
        } catch (e: SecurityException) {
            // Permissions not granted yet. Will retry when permissions are updated.
        } catch (e: Exception) {
            // Log other errors if needed
        }
    }

    fun updatePermissionsState(granted: Boolean) {
        _hasPermissions.value = granted
        if (granted) {
            registerObservers()
            loadData()
        }
    }

    fun loadData() {
        val isDemo = settingsRepository.isDemoMode
        if (!_hasPermissions.value && !isDemo) return

        viewModelScope.launch {
            _isLoading.value = true
            
            // Fix blocking I/O: Run on IO dispatcher
            val loadedContacts = withContext(Dispatchers.IO) {
                val isDemo = settingsRepository.isDemoMode // Accessing shared prefs is fast, but okay to be here
                if (isDemo) {
                    ch.heuscher.simplephone.data.MockData.demoContacts
                } else {
                    contactRepository.getContacts()
                }
            }

            // Apply Sort Order (Computing on background thread is good)
            val sortedContacts = withContext(Dispatchers.Default) {
                val savedOrder = settingsRepository.getFavoritesOrder()
                if (savedOrder.isNotEmpty()) {
                    loadedContacts.map { contact ->
                        val savedIndex = savedOrder.indexOf(contact.id)
                        if (savedIndex >= 0) contact.copy(sortOrder = savedIndex) else contact.copy(sortOrder = Int.MAX_VALUE)
                    }.sortedWith(compareBy({ !it.isFavorite }, { it.sortOrder }, { it.name }))
                } else {
                    loadedContacts
                }
            }
            
            _contacts.value = sortedContacts

            // Generate lookup maps for consistent resolution across the app
            // We use the sorted list so that if duplicates exist, the first one (based on sort order) "wins"
            // if we populate the map in order (put won't overwrite if we check absolute existence, 
            // but HashMap usually overwrites. So we should iterate in reverse if we want top priority to stay?
            // Or just iterate normally and putIfAbsent?
            // Actually, simply `put` overwrites, so the LAST one stays.
            // We want the FIRST one (highest priority/favorite) to be the one found.
            // So we should iterate list REVERSED, or use putIfAbsent equivalent.
            // Let's use REVERSE iteration so the first items in the sorted list end up overwriting the lower ones?
            // No, if we iterate normal list: Item A (High Prio), Item B (Low Prio).
            // Put A. Map[Number] = A.
            // Put B. Map[Number] = B. 
            // Result: B. (Wrong).
            // So we must iterate REVERSED.
            
            val newResolutionMaps = withContext(Dispatchers.Default) {
                val normalizedMap = HashMap<String, Contact>()
                val suffixMap = HashMap<String, Contact>()
                
                // Iterate in reverse order so that higher priority contacts (at the start of the list)
                // overwrite lower priority ones in the map.
                sortedContacts.asReversed().forEach { contact ->
                    contact.allNumbers.forEach { number ->
                        val normalized = ch.heuscher.simplephone.ui.utils.PhoneNumberHelper.normalize(number)
                        if (normalized.isNotEmpty()) {
                            normalizedMap[normalized] = contact
                            if (normalized.length >= 7) {
                                suffixMap[normalized.takeLast(7)] = contact
                            }
                        }
                    }
                }
                ContactResolutionMaps(normalizedMap, suffixMap)
            }
            _contactResolutionMaps.value = newResolutionMaps
            
            // Upload favorites to cloud if enabled
            val favorites = sortedContacts.filter { it.isFavorite }
            withContext(Dispatchers.IO) {
                settingsRepository.uploadFavorites(favorites)
            }

            // Load Missed Calls
            // Load Missed Calls (only if permissions granted, or mock them if demo mode extended to calls)
            // For now, only load real calls if permitted, otherwise empty list check happening in repo anyway?
            // Safer to check permission here or let repo handle it?
            // Repo crashes if permisson denied usually.
            
            val loadedMissedCalls = if (_hasPermissions.value) {
                withContext(Dispatchers.IO) {
                    contactRepository.getMissedCallsInLastHours(settingsRepository.missedCallsHours)
                }
            } else {
                emptyList()
            }
            _missedCalls.value = loadedMissedCalls

            // Load All Recents (Call Log)
            val loadedRecents = withContext(Dispatchers.IO) {
                 if (settingsRepository.isDemoMode) {
                     ch.heuscher.simplephone.data.MockData.getRecents()
                 } else if (_hasPermissions.value) {
                     contactRepository.getCallLogs()
                 } else {
                     emptyList()
                 }
            }
            _recents.value = loadedRecents
            
            _isLoading.value = false
        }
    }

    fun onFavoritesReorder(newOrder: List<Contact>) {
        viewModelScope.launch(Dispatchers.IO) {
             settingsRepository.saveFavoritesOrder(newOrder.map { it.id })
             // Reload to apply sort
             loadData()
        }
    }
    
    fun toggleDemoMode() {
        val newState = !settingsRepository.isDemoMode
        settingsRepository.isDemoMode = newState
        loadData()
    }
    
    fun refresh() {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        if (isObserverRegistered) {
            try {
                getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
